package cn.net.zhu.seckill.business.helper;

import cn.net.zhu.seckill.business.util.SnowFlakeIdWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdGenerateHelper {
    private final SnowFlakeIdWorker snowFlakeIdWorker;

    public Long nextId(){
        return snowFlakeIdWorker.nextId();
    }
}
