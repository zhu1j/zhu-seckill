package cn.net.zhu.seckill.api.config;

import cn.net.zhu.seckill.business.json.JacksonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 *  Web 配置类
 *
 * @author 一只朱
 * @date 2026-08-23 03:05
 *
 * "Run the code. Run the world."
 */
 @Configuration
public class WebConfig implements WebMvcConfigurer {
 @Bean
 public MappingJackson2HttpMessageConverter getMappingJackson2HttpMessageConverter() {
  return new MappingJackson2HttpMessageConverter(new JacksonMapper());
 }
}
