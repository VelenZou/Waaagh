package com.waaagh.enums;

public enum SpringCloudClassAnnotation {
  /**
   * FeignClient
   */
  FEIGNCLIENT("org.springframework.cloud.openfeign.FeignClient", "");

  private final String qualifiedName;
  private final String desc;

  SpringCloudClassAnnotation(String qualifiedName, String desc) {
    this.qualifiedName = qualifiedName;
    this.desc = desc;
  }

  public static SpringCloudClassAnnotation getByShortName(String feignClient) {
    for (SpringCloudClassAnnotation springCloudClassAnnotation : SpringCloudClassAnnotation.values()) {
      if (springCloudClassAnnotation.getQualifiedName().endsWith(feignClient)) {
        return springCloudClassAnnotation;
      }
    }
    return null;
  }

  public String getQualifiedName() {
    return qualifiedName;
  }

  public String getDesc() {
    return desc;
  }
}
