package org.waaagh.cloudfeignserver.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 演示「按子包自动加路径前缀」的配置：
 * {@code WebMvcConfigurer#configurePathMatch} +
 * {@code PathMatchConfigurer#addPathPrefix(prefix, predicate)}。
 * <p>
 * Spring 只会应用<b>第一个</b>命中的规则（注册顺序），最终完整路径 =
 * server.servlet.context-path + spring.mvc.servlet.path + 前缀 + 类上 @RequestMapping + 方法路径。
 * <p>
 * 插件会解析本文件里的 addPathPrefix 调用（lambda / HandlerTypePredicate / 常量引用），
 * 把前缀拼进 Controller 完整路径，因此 Feign 匹配与 Copy Controller URL 都会包含前缀。
 * 验证用例与预期结果见 sample 根目录 PATH_PREFIX_MATCHING.md。
 */
@Configuration
public class WaaaghPathPrefixConfig implements WebMvcConfigurer {

    /** 规则二的前缀用常量，顺带验证插件对常量引用的解析。 */
    private static final String V3_PREFIX = "/v3";

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // 规则一：controller.v2 子包（且带 @RestController）→ 前缀 /v2
        // 注意用的是 startsWith：controller.v2beta 这类同前缀包也会命中（与 Spring 行为一致）
        configurer.addPathPrefix("/v2",
                c -> c.isAnnotationPresent(RestController.class)
                        && c.getPackageName().startsWith("org.waaagh.cloudfeignserver.controller.v2"));

        // 规则二：HandlerTypePredicate.forBasePackage → 前缀 /v3
        String s = "org.waaagh.cloudfeignserver.controller.v3";
        configurer.addPathPrefix(V3_PREFIX,
                HandlerTypePredicate.forBasePackage(s));
    }
}
