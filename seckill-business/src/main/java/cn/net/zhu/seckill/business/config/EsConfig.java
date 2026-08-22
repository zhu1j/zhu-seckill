package cn.net.zhu.seckill.business.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 *  ES 客户端类
 *
 * @author 一只朱
 * @date 2026-08-23 02:54
 *
 * "Run the code. Run the world."
 */

@Configuration
public class EsConfig {
    @Value("${spring.elasticsearch.host:127.0.0.1}")
    private String host;
    @Value("${spring.elasticsearch.port:9200}")
    private int port;

    @Bean
    public RestHighLevelClient restHighLevelClient(){
        return new RestHighLevelClient(
                RestClient.builder(
                        Arrays.stream(host.split(","))
                                .map(s -> new HttpHost(s,port))
                                .toArray(HttpHost[]::new)
                )
        );
    }

}
