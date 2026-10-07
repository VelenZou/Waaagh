package org.waaagh.feignclient.feign.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.waaagh.feignclient.feign.interceptor.UserInterceptor;

@Configuration
public class UserConfiguration {

  @Bean
  public UserInterceptor userInterceptor() {
    return new UserInterceptor();
  }
}