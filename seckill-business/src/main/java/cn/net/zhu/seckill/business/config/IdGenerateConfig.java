package cn.net.zhu.seckill.business.config;

import cn.net.zhu.seckill.business.util.SnowFlakeIdWorker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *  雪花ID生成器类
 *
 * @author 一只朱
 * @date 2026-08-23 02:47
 *
 * "Run the code. Run the world."
 */
@Configuration
public class IdGenerateConfig {

    @Value("${snowflake.worker-id:1}")
    private long workerId;

    @Value("${snowflake.datacenter-id:1}")
    private long datacenterId;

    @Bean
    public SnowFlakeIdWorker snowFlakeIdWorker() {
        return new SnowFlakeIdWorker(workerId, datacenterId);
    }
}