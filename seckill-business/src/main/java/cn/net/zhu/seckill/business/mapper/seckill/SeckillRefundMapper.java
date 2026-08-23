package cn.net.zhu.seckill.business.mapper.seckill;

import cn.net.zhu.seckill.business.entity.seckill.SeckillRefundEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 秒杀退款记录 Mapper（退款申请数据库增改查操作）
 * @author 一只朱
 * @date 2026-08-24 03:31
 *
 * "Run the code. Run the world."
 */

@Mapper
public interface SeckillRefundMapper {
    /**
     * 新增秒杀退款记录
     * @param entity 秒杀退款记录实体
     * @return 受影响行数
     */
    int insert(SeckillRefundEntity entity);

    /**
     * 根据主键id查询退款记录
     * @param id 退款记录id
     * @return 秒杀退款记录实体
     */
    SeckillRefundEntity findById(Long id);

    /**
     * 根据订单编号查询退款记录
     * @param orderCode 订单编号
     * @return 秒杀退款记录实体
     */
    SeckillRefundEntity findByOrderCode(String orderCode);

    /**
     * 根据订单id查询退款记录集合
     * @param orderId 订单id
     * @return 退款记录列表
     */
    List<SeckillRefundEntity> findByOrderId(Long orderId);

    /**
     * 根据用户id查询用户所有退款记录
     * @param userId 用户id
     * @return 退款记录列表
     */
    List<SeckillRefundEntity> findByUserId(Long userId);

    /**
     * 更新退款状态
     * @param entity 秒杀退款记录实体
     * @return 受影响行数
     */
    int updateRefundStatus(SeckillRefundEntity entity);

    /**
     * 更新退款记录全量信息
     * @param entity 秒杀退款记录实体
     * @return 受影响行数
     */
    int update(SeckillRefundEntity entity);
}
