package org.waaagh.feignclient.feign.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;

//@Component
public class OrderInterceptor implements RequestInterceptor {

  public OrderInterceptor() {
    System.out.println("OrderInterceptor...");
  }

  @Override
  public void apply(RequestTemplate template) {
    System.out.println("OrderInterceptor...");
  }
}
