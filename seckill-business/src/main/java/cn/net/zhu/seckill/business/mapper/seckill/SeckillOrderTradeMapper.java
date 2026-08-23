package cn.net.zhu.seckill.business.mapper.seckill;

import cn.net.zhu.seckill.business.entity.seckill.SeckillOrderTradeConditionEntity;
import cn.net.zhu.seckill.business.entity.seckill.SeckillOrderTradeEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 秒杀订单交易 Mapper（秒杀订单数据库增删改查、条件分页查询）
 * @author 一只朱
 * @date 2026-08-24 03:06
 *
 * "Run the code. Run the world."
 */

@Mapper
public interface SeckillOrderTradeMapper {
    /**
     * 根据主键id查询秒杀订单
     * @param id 订单id
     * @return 秒杀订单交易实体
     */
    SeckillOrderTradeEntity findById(Long id);

    /**
     * 根据订单编号查询秒杀订单
     * @param code 订单编号
     * @return 秒杀订单交易实体
     */
    SeckillOrderTradeEntity findByCode(String code);

    /**
     * 新增秒杀订单
     * @param entity 秒杀订单交易实体
     * @return 受影响行数
     */
    int insert(SeckillOrderTradeEntity entity);

    /**
     * 更新秒杀订单
     * @param entity 秒杀订单交易实体
     * @return 受影响行数
     */
    int update(SeckillOrderTradeEntity entity);

    /**
     * 条件查询秒杀订单列表（分页数据）
     * @param condition 查询条件实体
     * @return 秒杀订单集合
     */
    List<SeckillOrderTradeEntity> searchByCondition(SeckillOrderTradeConditionEntity condition);

    /**
     * 条件查询秒杀订单总条数（分页统计）
     * @param condition 查询条件实体
     * @return 数据总条数
     */
    int searchCount(SeckillOrderTradeConditionEntity condition);
}
