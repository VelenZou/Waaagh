package org.waaagh.feignclient.feign.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;

//@Component
public class UserInterceptor implements RequestInterceptor {

  public UserInterceptor() {
    System.out.println("UserInterceptor...");
  }

  @Override
  public void apply(RequestTemplate template) {
    System.out.println("UserInterceptor...");
  }
}
