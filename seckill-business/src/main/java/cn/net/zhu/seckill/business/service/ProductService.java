package cn.net.zhu.seckill.business.service;

import cn.net.zhu.seckill.business.entity.ResponsePageEntity;
import cn.net.zhu.seckill.business.entity.product.EsSeckillProductConditionEntity;
import cn.net.zhu.seckill.business.entity.seckill.EsSeckillProductEntity;
import cn.net.zhu.seckill.business.entity.seckill.SeckillProductDetailPageEntity;

/**
 * 秒杀商品服务接口
 *
 * @author 一只朱
 * @date 2026-09-07 15:55
 *
 * "Run the code. Run the world."
 */

public interface ProductService {

    /**
     * 搜索秒杀商品
     *
     * @param seckillProductConditionEntity 查询条件
     * @return 秒杀商品
     */
    ResponsePageEntity<EsSeckillProductEntity> searchProductList(EsSeckillProductConditionEntity seckillProductConditionEntity);

    /**
     * 获取商品信息
     *
     * @param id 秒杀商品ID
     * @return 商品信息
     */
    SeckillProductDetailPageEntity getProductInfo(Long id);

    /**
     * 获取秒杀商品详情
     *
     * @param id 秒杀商品ID
     * @return 商品详情
     */
    SeckillProductDetailPageEntity getProductDetail(Long id);

    /**
     * 修改ES中商品库存
     *
     * @param id    秒杀商品ID
     * @param stock 库存
     */
    void updateEsProductStock(Long id, Integer stock);

    /**
     * 从ES中根据ID获取秒杀商品信息
     *
     * @param id 秒杀商品ID
     * @return ES秒杀商品实体
     */
    EsSeckillProductEntity getProductFromES(Long id);
}