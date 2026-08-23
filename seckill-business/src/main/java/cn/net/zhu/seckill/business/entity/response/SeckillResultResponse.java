package cn.net.zhu.seckill.business.entity.response;

import cn.net.zhu.seckill.business.entity.seckill.SeckillOrderTradeEntity;
import lombok.Data;

/**
 *  秒杀结果响应 类
 *
 * @author 一只朱
 * @date 2026-08-23 15:09
 *
 * "Run the code. Run the world."
 */

@Data
public class SeckillResultResponse {
    private Integer status; //0:处理中 1:成功 2:失败
    private Integer progress; //0-100
    private String message;
    private SeckillOrderTradeEntity orderInfo;

    /**
     * 三个结果响应方法 分别对应：处理中、成功、失败
     */

    public static SeckillResultResponse processing(int progress, String message) {
        SeckillResultResponse r = new SeckillResultResponse();
        r.status = 0; r.progress = progress; r.message = message;
        return r;
    }

    public static SeckillResultResponse success(SeckillOrderTradeEntity order) {
        SeckillResultResponse r = new SeckillResultResponse();
        r.status = 1; r.progress = 100; r.message = "抢购成功"; r.orderInfo = order;
        return r;
    }

    public static SeckillResultResponse failure(String message){
        SeckillResultResponse r = new SeckillResultResponse();
        r.status = 2; r.progress = 100; r.message = message;
        return r;
    }
}
