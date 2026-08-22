package cn.net.zhu.seckill.business.config;

import cn.net.zhu.seckill.business.util.SnowFlakeIdWorker;
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
    public SnowFlakeIdWorker snowFlakeIdWorker() {
        return new SnowFlakeIdWorker(0,0);
    }
}
