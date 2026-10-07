package org.waaagh.feignclient.feign.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.waaagh.cloudfeignapi.User;

/**
 * {@link UserClientConstantPath} 的 fallback 工厂（让示例工程保持可运行；不影响插件验证）。
 */
public class ConstantPathFallbackFactory implements FallbackFactory<UserClientConstantPath> {

    @Override
    public UserClientConstantPath create(Throwable cause) {
        return new UserClientConstantPath() {
            @Override
            public User constantPath(Long id) {
                return null;
            }
        };
    }
}
