package cn.net.susan.seckill.business.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * JVM预热配置
 * 应用启动时进行JVM预热，包括类加载和JIT编译优化
 *
 * @author 一只朱
 * @date 2026-09-07 15:14
 *
 * "Run the code. Run the world."
 */

@Component
@Slf4j
@Order(1) // 确保在其他预热之前执行
public class JvmWarmupConfig implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("开始JVM预热...");
        
        // 异步执行预热，避免阻塞应用启动
        CompletableFuture.runAsync(this::performWarmup);
    }
    
    private void performWarmup() {
        try {
            // 1. 预热常用的类加载
            warmupClassLoading();
            
            // 2. 预热JIT编译器
            warmupJitCompiler();
            
            // 3. 预热垃圾收集器
            warmupGarbageCollector();
            
            log.info("JVM预热完成");
            
        } catch (Exception e) {
            log.warn("JVM预热过程中出现异常，但不影响应用启动: {}", e.getMessage());
        }
    }
    
    /**
     * 预热类加载
     */
    private void warmupClassLoading() {
        try {
            log.debug("开始预热类加载...");
            
            // 预加载常用的业务类
            Class.forName("cn.net.susan.seckill.business.service.impl.UserSeckillProductServiceImpl");
            Class.forName("cn.net.susan.seckill.business.service.impl.StockRepairServiceImpl");
            Class.forName("cn.net.susan.seckill.business.util.RedisUtil");
            Class.forName("cn.net.susan.seckill.business.util.BusinessKeyUtil");
            
            // 预加载常用的Spring类
            Class.forName("org.springframework.web.servlet.DispatcherServlet");
            Class.forName("org.springframework.data.redis.core.StringRedisTemplate");
            
            log.debug("类加载预热完成");
            
        } catch (Exception e) {
            log.warn("类加载预热失败: {}", e.getMessage());
        }
    }
    
    /**
     * 预热JIT编译器
     */
    private void warmupJitCompiler() {
        try {
            log.debug("开始预热JIT编译器...");
            
            // 执行一些计算密集型操作来触发JIT编译
            long startTime = System.currentTimeMillis();
            
            // 模拟热点代码执行
            for (int i = 0; i < 100000; i++) {
                // 字符串操作
                String test = "seckill_product_" + i;
                test.hashCode();
                
                // 数学运算
                Math.sqrt(i);
                Math.random();
                
                // 集合操作
                java.util.concurrent.ConcurrentHashMap<String, Object> map = new java.util.concurrent.ConcurrentHashMap<>();
                map.put("key" + i, "value" + i);
                map.get("key" + i);
            }
            
            long endTime = System.currentTimeMillis();
            log.debug("JIT编译器预热完成，耗时: {}ms", endTime - startTime);
            
        } catch (Exception e) {
            log.warn("JIT编译器预热失败: {}", e.getMessage());
        }
    }
    
    /**
     * 预热垃圾收集器
     */
    private void warmupGarbageCollector() {
        try {
            log.debug("开始预热垃圾收集器...");
            
            // 创建一些对象来触发垃圾收集
            for (int i = 0; i < 1000; i++) {
                byte[] temp = new byte[1024]; // 1KB
                temp = null; // 让对象变为垃圾
            }
            
            // 建议进行垃圾收集
            System.gc();
            
            // 等待一小段时间让GC完成
            TimeUnit.MILLISECONDS.sleep(100);
            
            log.debug("垃圾收集器预热完成");
            
        } catch (Exception e) {
            log.warn("垃圾收集器预热失败: {}", e.getMessage());
        }
    }
}