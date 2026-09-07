package cn.net.zhu.seckill.business.helper;

import cn.net.zhu.seckill.business.util.SnowFlakeIdWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


/**
 * 统一封装ID生成服务
 *
 * @author 一只朱
 * @date 2026-09-07 16:19
 *
 * "Run the code. Run the world."
 */

@RequiredArgsConstructor
@Component
public class IdGenerateHelper {

    private final SnowFlakeIdWorker snowFlakeIdWorker;

    /**
     * 生成分布式ID
     *
     * @return 分布式ID
     */
    public Long nextId() {
        return snowFlakeIdWorker.nextId();
    }
}
