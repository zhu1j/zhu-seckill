package cn.net.zhu.seckill.business.service;

import cn.net.zhu.seckill.business.entity.ResponsePageEntity;
import cn.net.zhu.seckill.business.entity.product.ESSeckillProductConditionEntity;
import cn.net.zhu.seckill.business.entity.seckill.EsSeckillProductEntity;
import cn.net.zhu.seckill.business.entity.seckill.SeckillProductDetailPageEntity;

/**
 * 秒杀商品服务接口
 * 基于ElasticSearch实现商品检索、查询详情、ES库存更新，秒杀商品读主走ES，减轻MySQL数据库压力
 */
public interface ProductService {

    /**
     * 分页搜索秒杀商品列表
     * @param condition 查询条件实体：关键词、价格区间、分页参数等
     * @return ResponsePageEntity<ESSeckillProductEntity> 分页结果，ES查询出来的秒杀商品数据
     */
    ResponsePageEntity<EsSeckillProductEntity> searchProductList(
            ESSeckillProductConditionEntity condition);

    /**
     * 获取商品信息（用于秒杀列表简要信息）
     * @param id 商品id
     * @return SeckillProductDetailPageEntity 商品页面详情VO
     */
    SeckillProductDetailPageEntity getProductInfo(Long id);

    /**
     * 获取商品详情页完整数据
     * @param id 商品id
     * @return SeckillProductDetailPageEntity 商品完整详情VO，用于商品详情页面渲染
     */
    SeckillProductDetailPageEntity getProductDetail(Long id);

    /**
     * 更新ES中的商品库存数量
     * @param id 商品id
     * @param stock 需要更新的库存值
     */
    void updateEsProductStock(Long id, Integer stock);

    /**
     * 直接从ES获取秒杀商品原始实体
     * @param id 商品id
     * @return ESSeckillProductEntity ES存储的商品原始对象
     */
    EsSeckillProductEntity getProductFromES(Long id);
}

/*
====================业务总结====================
1、秒杀商品顶层接口，**查询全部走ElasticSearch**，把高并发读流量剥离MySQL，是秒杀系统优化点；
2、searchProductList：条件+分页检索ES，返回商品列表页数据；
3、getProductInfo / getProductDetail：两个获取详情方法，区分简要信息和完整详情页面数据；
4、getProductFromES：直接读取ES原始实体，供内部业务调用；
5、updateEsProductStock：更新ES库存；秒杀扣减库存时，除操作数据库，同步维护ES库存，保证页面展示库存准确；
6、设计意图：前端列表、详情页流量全部打到ES；MySQL只承担下单、订单写操作；
7、注意：ES库存为展示用，不能作为扣减库存的权威依据，真实库存以MySQL/Redis为准，ES仅做页面展示。
*/

