package com.ecommerce.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  public static final String API_V1_PREFIX = "/api/v1";

  @Override
  public void configurePathMatch(PathMatchConfigurer configurer) {
    configurer.addPathPrefix(
        API_V1_PREFIX,
        c -> c.isAnnotationPresent(RestController.class)
            && c.getPackageName().startsWith("com.ecommerce"));
  }
}
