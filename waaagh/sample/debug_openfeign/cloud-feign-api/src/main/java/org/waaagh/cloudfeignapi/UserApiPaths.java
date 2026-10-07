package org.waaagh.cloudfeignapi;

/**
 * 客户端与服务端共用的路径常量（放在 api 模块，双方都引用）。
 * <p>
 * 用于验证插件对「路径写在常量里」的解析：{@code @FeignClient#path} 与
 * {@code @GetMapping} 引用常量时，跳转与 Copy URL 仍应拼出完整路径。
 * 对照用例见 sample 根目录 METHOD_MATCHING.md。
 */
public interface UserApiPaths {

    /** 客户端基础路径，被 @FeignClient#path 引用。 */
    String CLIENT_BASE = "/hello/world";

    /** 方法路径，被客户端 @GetMapping 与服务端 Controller 方法共同引用。 */
    String CONSTANT_PATH = "/user/const/{id}";
}
