package cn.net.zhu.seckill.business.mapper.seckill;

import cn.net.zhu.seckill.business.entity.seckill.SeckillRefundLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 秒杀退款操作日志 Mapper（退款操作日志数据库写入）
 * @author 一只朱
 * @date 2026-08-24 03:32
 *
 * "Run the code. Run the world."
 */

@Mapper
public interface SeckillRefundLogMapper {
    /**
     * 新增退款操作日志
     * @param entity 退款操作日志实体
     * @return 受影响行数
     */
    int insert(SeckillRefundLogEntity entity);
}
