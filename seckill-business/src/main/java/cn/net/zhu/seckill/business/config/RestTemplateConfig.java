package cn.net.zhu.seckill.business.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate配置类
 *
 * @author 一只朱
 * @date 2026-09-07 15:15
 *
 * "Run the code. Run the world."
 */

@Configuration
public class RestTemplateConfig {

    @Value("${seckill.api.restTemplate.connectTimeout:200000}")
    private int connectTimeout;

    @Value("${seckill.api.restTemplate.readTimeout:200000}")
    private int readTimeout;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory httpRequestFactory = new SimpleClientHttpRequestFactory();
        httpRequestFactory.setConnectTimeout(connectTimeout);
        httpRequestFactory.setReadTimeout(readTimeout);
        return new RestTemplate(httpRequestFactory);
    }
}

