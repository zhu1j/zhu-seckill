package cn.net.zhu.seckill.business.service;

/**
 * 库存一致性服务接口
 * 负责秒杀商品 DB数据库库存 与 Redis缓存库存的一致性操作；
 * 扣减库存、恢复库存、缓存同步、一致性校验；解决秒杀场景缓存与DB库存不一致问题。
 *
 * @author 一只朱
 * @date 2026-09-06 15:52
 *
 * "Run the code. Run the world."
 */

public interface StockConsistencyService {

    /**
     * 恢复库存
     * 订单超时关闭 / 退款成功后调用；DB库存 + quantity，Redis预扣库存回补；
     * @param seckillProductId 秒杀商品ID
     * @param quantity 需要恢复的数量
     * @return true 恢复成功；false 恢复失败
     */
    boolean restoreStock(Long seckillProductId, Integer quantity);

    /**
     * 扣减库存
     * 用户下单时扣减；先操作Redis预扣库存，再扣减DB真实库存；
     * @param seckillProductId 秒杀商品ID
     * @param quantity 需要扣减数量
     * @return true扣减成功；false扣减失败（库存不足等）
     */
    boolean reduceStock(Long seckillProductId, Integer quantity);

    /**
     * 同步库存
     * 以DB数据库库存为准，覆盖刷新Redis缓存库存；用于缓存失效、缓存脏数据修复、预热；
     * @param seckillProductId 秒杀商品ID
     * @return true同步成功
     */
    boolean syncStock(Long seckillProductId);

    /**
     * 校验库存一致性
     * 对比DB库存值与Redis缓存库存值是否相等；用于定时巡检、排查库存不一致问题；
     * @param seckillProductId 秒杀商品ID
     * @return true DB与Redis库存一致；false 不一致
     */
    boolean checkStockConsistency(Long seckillProductId);
}

/*
====================业务总结====================
1、模块职责：库存一致性顶层接口；管理秒杀DB与Redis双库存；提供扣减、恢复、同步、一致性校验4个核心能力；
隔离上层业务，上层订单、超时、退款模块不需要关心Redis和DB底层细节。

2、调用链路：
- reduceStock：下单流程 → OrderServiceImpl → reduceStock() 预扣库存；
- restoreStock：①订单超时关闭 OrderTimeoutServiceImpl ②退款成功 SeckillRefundServiceImpl；
- syncStock：商品上架、缓存丢失、定时修复脏缓存；
- checkStockConsistency：定时任务巡检，发现DB‑Redis库存不一致告警。

3、核心能力说明：
① reduceStock：下单扣减；秒杀预扣，防止超卖；要保证Redis、DB扣减逻辑，处理库存不足；
② restoreStock：回滚库存；订单超时、退款时归还库存，防止库存泄露；必须幂等，避免多次回补导致超卖；
③ syncStock：以DB为基准刷新Redis；修复缓存脏数据；商品发布后做缓存预热；
④ checkStockConsistency：比对DB库存和Redis缓存库存，返回是否一致，用于巡检告警。

4、技术设计亮点：
‑ 将DB+Redis库存操作收拢到独立Service，业务层解耦；订单、退款、超时模块只调用接口，不感知底层存储；
‑ 接口语义清晰，区分扣减、恢复、同步、校验，职责单一；
‑ seckillProductId作为唯一主键，不是普通productId，区分秒杀商品。

5、风险点 & 潜在坑：
‑ restoreStock必须幂等：消息重复消费多次调用，不能重复加库存，否则出现超卖；
‑ reduceStock要防超卖；DB扣减必须带where条件乐观锁（stock >= quantity）；
‑ syncStock直接覆盖Redis，如果有正在进行的预扣订单，会把预扣状态冲掉，要选低峰执行；
‑ checkStockConsistency只能做快照比对；并发下单瞬间DB‑Redis短暂不一致属于正常现象，不能直接判定故障；
‑ 接口只返回boolean，失败没有返回错误码，上层只能知道成功失败，不知道失败原因（库存不足/异常）。

6、模块关联：实现类 StockConsistencyServiceImpl；被 OrderServiceImpl、OrderTimeoutServiceImpl、SeckillRefundServiceImpl 调用；
依赖 SeckillProductMapper、RedisTemplate。

7、生产注意事项：
‑ restoreStock、reduceStock内部使用乐观锁更新DB库存，禁止直接 set stock = stock ± quantity；
‑ restoreStock 增加幂等控制（例如操作记录表，同一个订单只能恢复一次库存）；
‑ checkStockConsistency定时巡检，发现不一致发送告警，不要自动修复，防止并发下误修复；
‑ syncStock尽量在商品上架、凌晨低峰执行；秒杀流量高峰禁止调用syncStock；
‑ 建议接口增加返回对象，区分：库存不足、执行异常、成功，不要只用boolean。
*/
