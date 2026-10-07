package org.waaagh.feignclient.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.waaagh.cloudfeignapi.InheritanceApi;

/**
 * 继承场景的 Feign 客户端：本身不声明端点方法，全部继承自 {@link InheritanceApi}。
 * <p>
 * 用于验证「@FeignClient 子接口继承父接口、父接口承载端点」的场景。
 * 完整路径通过本接口的 path=/hello/world/inheritance 计算：
 * 预期匹配 InheritanceServerController 的 getById（GET）与 save（POST）。
 */
@FeignClient(path = "/hello/world/inheritance", value = "cloud-feign-server", contextId = "inheritance")
public interface InheritanceFeignClient extends InheritanceApi {
}
