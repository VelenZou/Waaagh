package org.waaagh.feignclient.feign.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.waaagh.feignclient.feign.interceptor.OrderInterceptor;

@Configuration
public class OrderConfiguration {

  @Bean
  public OrderInterceptor orderInterceptor() {
    return new OrderInterceptor();
  }
}
