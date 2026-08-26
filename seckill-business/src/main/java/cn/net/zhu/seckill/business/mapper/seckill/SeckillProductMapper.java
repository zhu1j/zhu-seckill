package cn.net.zhu.seckill.business.mapper.seckill;

import cn.net.zhu.seckill.business.entity.seckill.SeckillProductEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.xmlbeans.impl.xb.xmlconfig.Extensionconfig;

/**
 *  秒杀商品 Mapper（数据库CRUD、库存扣减恢复操作）
 * @author 一只朱
 * @date 2026-08-24 03:01
 *
 * "Run the code. Run the world."
 */

@Mapper
public interface SeckillProductMapper {
        /**
         * 根据主键id查询秒杀商品
         * @param id 秒杀商品id
         * @return 秒杀商品实体
         */
        SeckillProductEntity findById(Long id);

        /**
         * 新增秒杀商品
         * @param entity 秒杀商品实体
         * @return 受影响行数
         */
        int insert(SeckillProductEntity entity);

        /**
         * 更新秒杀商品
         * @param entity 秒杀商品实体
         * @return 受影响行数
         */
        int update(SeckillProductEntity entity);

        /**
         * 扣减预锁定库存（下单冻结库存）
         * 原子预扣库存
         * @param id 秒杀商品id
         * @return 受影响行数
         */
        int reduceWithHoldStock(Long id);

        /**
         * 恢复预锁定库存（订单超时/取消释放冻结库存）
         * 恢复预扣库存
         * @param id 秒杀商品id
         * @return 受影响行数
         */
        int restoreWithHoldStock(Long id);
}
