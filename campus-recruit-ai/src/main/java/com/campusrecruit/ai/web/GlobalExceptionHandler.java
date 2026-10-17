package com.campusrecruit.ai.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Map;

/**
 * 全局异常处理器：把程序里的异常，统一转成前端好理解的 JSON。
 *
 * 人话讲解：
 * - 我们前面 register 时抛了 RuntimeException("用户名已存在")。如果不处理，
 *   前端会收到一堆看不懂的报错堆栈。这里拦截后，转成 {"error":"用户名已存在"} 并带状态码。
 * - 参数校验失败（比如密码太短）会抛 MethodArgumentNotValidException，也在这里兜底，
 *   返回第一条校验错误信息。
 * - 【兜底 handler】最后那个 handleUnknown 负责接住"我们没预料到的异常"。它返回 500
 *   且只给出泛化文案，不把堆栈吐给前端（堆栈里可能含 SQL、文件路径、内网地址等敏感信息，
 *   属于典型的信息泄露），同时用日志把完整堆栈留在服务器上供排查。
 *
 * 面试必懂点：
 * - @RestControllerAdvice = 所有 @RestController 的"全局切面"，集中处理异常，避免每个
 *   Controller 都写 try-catch（关注点分离）。
 * - 生产项目里通常还会区分"业务异常"和"系统异常"，分别返回 4xx 和 5xx，并打日志
 *   —— 这里已经按这个思路分层：RuntimeException(400 业务) / Exception(500 系统)。
 * - 【一个容易忽略的坑】返回体用 Map.of("error", e.getMessage()) 时，如果异常没有 message
 *   （e.getMessage() 返回 null），Map.of 会直接抛 NullPointerException —— 异常处理器
 *   自己炸掉，反而把真实错误盖住了。所以这里统一做了空值兜底。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常（我们用 RuntimeException 携带业务错误信息） */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", safeMessage(e, "请求处理失败")));
    }

    /** 参数校验失败（@Valid 触发的校验） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getDefaultMessage())
                .orElse("参数错误");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", msg));
    }

    /** 访问了不存在的接口：给中文提示，别让前端只收到英文的 Not Found */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoHandlerFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "接口不存在：" + e.getHttpMethod() + " " + e.getRequestURL()));
    }

    /** 上传文件超过 application.yml 里配置的大小上限 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleUploadTooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "文件太大了，请上传 2MB 以内的文件"));
    }

    /**
     * 最终兜底：任何没被上面接住的异常（比如空指针、数据库连接失败）。
     * 返回 500 + 泛化文案，真实堆栈只写服务器日志。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnknown(Exception e) {
        log.error("未预期的系统异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "服务器开小差了，请稍后重试"));
    }

    /** 取异常信息；没有 message 时用默认文案，避免 Map.of 因 null 值抛 NPE */
    private String safeMessage(Exception e, String fallback) {
        String msg = e.getMessage();
        return (msg == null || msg.isBlank()) ? fallback : msg;
    }
}
