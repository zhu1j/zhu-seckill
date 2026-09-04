package cn.net.zhu.seckill.business.mapper.seckill;

import cn.net.zhu.seckill.business.entity.seckill.SeckillPaymentEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 秒杀支付记录 Mapper（支付记录数据库增改查操作）
 * @author 一只朱
 * @date 2026-08-24 03:29
 *
 * "Run the code. Run the world."
 */

@Mapper
public interface SeckillPaymentMapper {
    /**
     * 新增秒杀支付记录
     * @param entity 秒杀支付记录实体
     * @return 受影响行数
     */
    int insert(SeckillPaymentEntity entity);

    /**
     * 根据主键id查询支付记录
     * @param id 支付记录id
     * @return 秒杀支付记录实体
     */
    SeckillPaymentEntity findById(Long id);

    /**
     * 根据订单编号查询支付记录集合(降序展示)
     * @param orderCode 订单编号
     * @return 支付记录列表
     */
    List<SeckillPaymentEntity> findByOrderCode(String orderCode);

    /**
     * 更新支付状态
     * @param entity 秒杀支付记录实体
     * @return 受影响行数
     */
    int updatePaymentStatus(SeckillPaymentEntity entity);

    /**
     * 更新支付记录全量信息
     * @param entity 秒杀支付记录实体
     * @return 受影响行数
     */
    int update(SeckillPaymentEntity entity);
}
