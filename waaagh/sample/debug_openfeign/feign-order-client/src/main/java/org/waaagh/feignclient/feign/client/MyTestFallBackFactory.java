package org.waaagh.feignclient.feign.client;

import org.waaagh.cloudfeignapi.User;

public class MyTestFallBackFactory implements
    org.springframework.cloud.openfeign.FallbackFactory<UserClient3> {

  @Override
  public UserClient3 create(Throwable cause) {
    return new UserClient3() {
      @Override
      public User parallelScan8(Long id) {
        return null;
      }
    };
  }
}
