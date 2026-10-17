package com.campusrecruit.ai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * 静态资源与 SPA（单页应用）路由配置。
 *
 * <p><b>这个类解决什么问题？</b>
 * <p>prod profile 下前端产物被打进同一个 jar（前后端同域部署），此时会有两类请求：
 * <ul>
 *   <li>访问 {@code /assets/index-xxx.js} → 应该返回真实的静态文件；</li>
 *   <li>访问 {@code /login} 或 {@code /membership} → 这些是 <b>Vue Router 的前端路由</b>，
 *       服务器上不存在对应文件。正确行为是返回 {@code index.html}，
 *       让前端 JS 启动后根据 URL 自己渲染出登录页 / 会员页。</li>
 * </ul>
 *
 * <p><b>为什么不能用 addViewController？</b>
 * <p>直觉做法是给每个前端路由注册 viewController（{@code registry.addViewController("/login")}）。
 * 但两个问题让它不可行：
 * <ol>
 *   <li><b>路径是无限的</b>：Vue Router 以后加一个 {@code /job/123} 详情页，就得回来补一条，
 *       漏一条那个页面在「刷新」或「直接输网址」时就会失败。</li>
 *   <li><b>只注册 {@code "/"} 不管用</b>：只加 {@code addViewController("/")} 时，
 *       访问 {@code /login} 会落到默认静态资源处理器，找不到名为 "login" 的文件，
 *       抛 {@code NoResourceFoundException}，被全局异常处理器兜成 <b>500</b>
 *       —— 这正是本项目实测踩到的坑（打包后才发现，开发期前端跑 Vite 所以看不到）。</li>
 * </ol>
 *
 * <p><b>正确解法：自定义 PathResourceResolver 做兜底。</b>
 * <p>先按正常流程找静态文件，找到就直接返回（js/css/图片都走这条）；
 * 找不到时不再抛异常，而是返回 {@code index.html} 交给前端路由处理。
 * 这是 Spring 官方文档给 SPA 推荐的标准做法。
 *
 * <p><b>面试可讲点：</b>
 * <ul>
 *   <li>这套机制在 Nginx 里的等价写法是 {@code try_files $uri $uri/ /index.html;}
 *       —— 本质都是「URL 找不到实体文件就兜底返回入口 HTML」。</li>
 *   <li><b>API 路径必须排除</b>：否则访问一个写错的 {@code /api/xxx} 会拿到一坨 HTML
 *       而不是 404 JSON，前端按 JSON 解析时反而报出难以定位的错。</li>
 *   <li>安全性不受影响：兜底返回的是公开的前端入口文件，真正的数据全由 {@code /api/**}
 *       保护（见 SecurityConfig），前端文件本身不含敏感信息。</li>
 * </ul>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/", "classpath:/public/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        // 1) 先尝试按路径找真实文件（/assets/xxx.js、favicon.ico 等）
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }

                        // 2) API 路径不兜底：让它们正常 404，保持 JSON 错误格式
                        if (resourcePath.startsWith("api/")) {
                            return null;
                        }

                        // 3) 前端路由（/login、/home、/membership…）→ 返回 index.html
                        Resource index = new ClassPathResource("/static/index.html");
                        return (index.exists() && index.isReadable()) ? index : null;
                    }
                });
    }
}
