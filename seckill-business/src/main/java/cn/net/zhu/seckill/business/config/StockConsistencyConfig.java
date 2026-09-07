package cn.net.zhu.seckill.business.config;

import org.checkerframework.checker.units.qual.C;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 *  库存补偿线程池类
 *
 * @author 一只朱
 * @date 2026-08-23 02:49
 *
 * "Run the code. Run the world."
 */

@Configuration
public class StockConsistencyConfig {
    /**
     * 库存补偿任务执行器
     */
    @Bean("stockCompensationExecutor")
    public Executor stockCompensationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数
        executor.setCorePoolSize(2);

        // 最大线程数
        executor.setMaxPoolSize(5);

        // 队列容量
        executor.setQueueCapacity(100);

        // 线程名前缀
        executor.setThreadNamePrefix("stock-compensation-");

        // 拒绝策略：由调用线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 线程空闲时间（秒）
        executor.setKeepAliveSeconds(60);

        // 允许核心线程超时
        executor.setAllowCoreThreadTimeOut(true);

        // 等待任务完成后关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间（秒）
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }

}
