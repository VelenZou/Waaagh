package org.waaagh.feignclient.config;

import org.waaagh.feignclient.feign.client.OrderClient;
import org.waaagh.feignclient.feign.client.UserClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WaaaghWebMvcConfig implements WebMvcConfigurer {

    @Autowired
    UserClient  userClient;
    @Autowired
    OrderClient orderClient;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new WaaaghInterceptor());
    }

}
