package org.lyflexi.feignclient.feign.client;

import org.lyflexi.cloudfeignapi.TransportServiceApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * 运力 Feign 客户端：本身不声明任何端点方法，全部端点继承自 {@link TransportServiceApi}。
 * 用于验证插件对“Feign 接口继承父接口，父接口承载端点”的支持。
 */
@FeignClient(path = "/hello/world/transport", value = "cloud-feign-server", contextId = "transport")
public interface TransportService extends TransportServiceApi {
}
