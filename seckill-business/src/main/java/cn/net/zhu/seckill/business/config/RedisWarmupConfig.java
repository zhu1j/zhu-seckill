package cn.net.zhu.seckill.business.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis连接预热配置
 * 应用启动时预热Redis连接，避免首次访问慢的问题
 *
 * @author 一只朱
 * @date 2026-09-07 15:14
 *
 * "Run the code. Run the world."
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisWarmupConfig implements ApplicationRunner {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            log.info("开始预热Redis连接...");
            
            // 执行一个简单的ping操作来预热连接
            String pong = stringRedisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();
            
            // 执行一些基本操作来预热连接池
            String testKey = "warmup:test";
            stringRedisTemplate.opsForValue().set(testKey, "warmup", 10);
            String value = stringRedisTemplate.opsForValue().get(testKey);
            stringRedisTemplate.delete(testKey);
            
            log.info("Redis连接预热完成，ping响应: {}, 测试操作结果: {}", pong, value);
                
        } catch (Exception e) {
            log.warn("Redis连接预热失败，但不影响应用启动: {}", e.getMessage());
        }
    }
}