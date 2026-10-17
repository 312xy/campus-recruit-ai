package com.campusrecruit.ai.client;

import com.campusrecruit.ai.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 调用 DeepSeek 大模型的客户端（OpenAI 兼容接口）。
 *
 * 这是整个项目 AI 能力的入口：传入 system + user 两段提示词，返回模型回答。
 *
 * ---------- 人话讲解：一次 AI 调用到底发生了什么 ----------
 * 1. 我们拼一个 JSON 请求体，里面有 model（用哪个模型）、messages（对话内容）；
 * 2. 带着 Authorization: Bearer <你的Key> 这个请求头，用 HTTP POST 发给 DeepSeek 的服务器；
 * 3. DeepSeek 把模型生成的文本放在响应的 choices[0].message.content 里返回；
 * 4. 我们把这段文本取出来，交给调用方（比如简历解析服务）。
 *
 * 所以"接入大模型"本质上就是**一次普通的 HTTP 调用**，没有任何魔法。
 * 理解这一点，这个项目的 AI 部分你就能完全讲明白。
 *
 * ---------- 面试必懂点 ----------
 * - 为什么要设超时？大模型响应可能很慢（几秒到几十秒）。如果不设超时，
 *   一旦对方服务卡住，我们的 Tomcat 线程会被一直占着，请求量一上来就整个服务雪崩。
 *   这是"线上事故级"的常识点，能主动说出来很加分。
 * - 为什么要区分异常类型？网络超时（可重试）、余额不足/Key 错误（不可重试，
 *   要提示用户去充值或检查配置）—— 前端给的提示完全不同。
 * - 为什么 temperature 分两档？对话/生成用 0.7（自然、有变化），
 *   结构化抽取用 0.2（稳定、少乱想）。
 */
@Service
public class DeepSeekClient {

    private final AiProperties aiProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DeepSeekClient(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        // 配置超时：连接 10 秒，读取 120 秒（大模型生成慢，读取要给足时间）
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(120_000);
        this.restTemplate = new RestTemplate(factory);
    }

    /** 普通对话调用（temperature 0.7），适合生成类任务 */
    public String chat(String systemPrompt, String userPrompt) {
        return doChat(systemPrompt, userPrompt, false);
    }

    /**
     * JSON 模式调用（temperature 0.2 + response_format=json_object），适合抽取类任务。
     * 开了这个模式后，DeepSeek 会保证输出是合法 JSON，程序可以直接解析。
     */
    public String chatJson(String systemPrompt, String userPrompt) {
        return doChat(systemPrompt, userPrompt, true);
    }

    private String doChat(String systemPrompt, String userPrompt, boolean jsonMode) {
        // 1) 先检查 Key 有没有配。没配就直接给一句人能看懂的提示，
        //    而不是发一个注定 401 的请求，让用户对着一堆英文报错发懵。
        String apiKey = aiProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "未配置大模型 API Key：请设置环境变量 DEEPSEEK_API_KEY 后重启后端服务。");
        }

        // 2) 拼请求头：Content-Type 是 JSON，Authorization 带 Bearer Key
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey.trim());

        // 3) 拼请求体。用 HashMap 而不是 Map.of，因为 Map.of 不允许值为 null，
        //    而且我们要按条件往里面塞 response_format。
        Map<String, Object> body = new HashMap<>();
        body.put("model", aiProperties.getModel());
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        body.put("temperature", jsonMode ? 0.2 : 0.7);
        if (jsonMode) {
            // JSON 模式：要求模型必须输出合法 JSON
            body.put("response_format", Map.of("type", "json_object"));
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String url = aiProperties.getBaseUrl() + "/chat/completions";

        // 4) 发请求 + 把各种错误翻译成人话
        String responseBody;
        try {
            responseBody = restTemplate.postForObject(url, entity, String.class);
        } catch (HttpStatusCodeException e) {
            // 对方返回了 4xx/5xx。常见：401 Key 无效、402 余额不足、429 限流
            int code = e.getStatusCode().value();
            String hint = switch (code) {
                case 401 -> "API Key 无效或已过期，请检查 DEEPSEEK_API_KEY 是否正确";
                case 402 -> "账户余额不足，请到 DeepSeek 平台充值";
                case 429 -> "请求过于频繁（限流），请稍后再试";
                default -> "大模型服务返回错误（HTTP " + code + "）：" + brief(e.getResponseBodyAsString());
            };
            throw new RuntimeException("调用 AI 失败：" + hint);
        } catch (ResourceAccessException e) {
            // 网络层面问题：连不上、超时
            throw new RuntimeException("调用 AI 超时或网络不通，请检查本机网络后重试");
        }

        // 5) 从响应 JSON 里把模型回答取出来
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new RuntimeException("AI 返回内容解析失败：" + brief(responseBody));
        }
    }

    /**
     * 清洗大模型的返回内容，提取出纯 JSON 字符串。
     *
     * 人话讲解：就算开了 JSON 模式，模型偶尔还是会给 JSON 套一层 Markdown 代码块
     * （```json ... ```）或者前面加一句"好的，这是解析结果："。
     * 这类"脏输出"直接丢给 Jackson 会解析失败，所以这里先做一次清理。
     *
     * 面试必懂点：这属于"防御式编程"——不要假设外部依赖（哪怕是大模型 API）
     * 一定按你期望的格式返回。生产环境里 AI 输出不可控是常态，
     * 必须做兜底，否则线上就是一堆 500。
     */
    public static String extractJson(String raw) {
        if (raw == null) {
            return "{}";
        }
        String s = raw.trim();

        // 去掉 Markdown 代码块包裹
        if (s.startsWith("```")) {
            int firstLineEnd = s.indexOf('\n');
            if (firstLineEnd > 0) {
                s = s.substring(firstLineEnd + 1);
            }
            int fenceEnd = s.lastIndexOf("```");
            if (fenceEnd >= 0) {
                s = s.substring(0, fenceEnd);
            }
            s = s.trim();
        }

        // 只截取第一个 { 到最后一个 } 之间的内容
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            s = s.substring(start, end + 1);
        }
        return s;
    }

    /** 截断过长的错误信息，避免把整页 HTML 错误页塞进日志和前端 */
    private static String brief(String s) {
        if (s == null) {
            return "(无响应内容)";
        }
        String one = s.replaceAll("\\s+", " ").trim();
        return one.length() > 200 ? one.substring(0, 200) + "..." : one;
    }
}
