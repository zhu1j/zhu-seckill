package cn.net.zhu.seckill.business.service.impl;

import cn.net.zhu.seckill.business.entity.seckill.SeckillProductEntity;
import cn.net.zhu.seckill.business.mapper.seckill.SeckillProductMapper;
import cn.net.zhu.seckill.business.service.ProductService;
import cn.net.zhu.seckill.business.service.StockConsistencyService;
import cn.net.zhu.seckill.business.util.BusinessKeyUtil;
import cn.net.zhu.seckill.business.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * 库存一致性服务实现类
 * 维护秒杀商品 DB预扣库存(withHoldQuantity)、Redis缓存库存、ES搜索库存三者一致性；
 * restoreStock：订单超时/退款恢复库存；reduceStock：下单扣减库存；
 * syncStock：以DB为准刷新Redis+ES；checkStockConsistency：比对DB‑Redis库存；
 * Redis/ES异常不回滚DB，通过自定义线程池异步补偿任务做重试。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockConsistencyServiceImpl implements StockConsistencyService {

    /**
     * 秒杀商品Mapper，操作DB预扣库存 withHoldQuantity
     */
    private final SeckillProductMapper seckillProductMapper;
    /**
     * 商品服务，用于更新ES库存索引
     */
    private final ProductService productService;
    /**
     * Redis工具类，操作缓存库存
     */
    private final RedisUtil redisUtil;
    /**
     * 库存补偿专用线程池，Redis/ES失败后异步重试补偿
     */
    @Qualifier("stockCompensationExecutor")
    private final Executor stockCompensationExecutor;

    /**
     * 恢复库存（订单超时关闭、退款场景）
     * 事务边界：DB恢复必须成功；Redis、ES异常不回滚DB，标记失败后走异步补偿；
     * @param seckillProductId 秒杀商品ID
     * @param quantity 恢复数量
     * @return true 执行完成；DB异常直接抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean restoreStock(Long seckillProductId, Integer quantity) {
        try {
            boolean redisOk = true;
            boolean esOk = true;

            // 1.DB恢复预扣库存，数据库操作在事务内，必须成功
            seckillProductMapper.restoreWithHoldStock(seckillProductId);

            // 2.Redis恢复库存，异常捕获，不影响DB事务提交
            try {
                String stockKey = BusinessKeyUtil.getProductStockKey(seckillProductId);
                redisUtil.increment(stockKey, quantity);
            } catch (Exception e) {
                log.error("Redis恢复库存失败", e);
                redisOk = false;
            }

            // 3.同步ES库存，异常捕获，不影响DB事务提交
            try {
                SeckillProductEntity product = seckillProductMapper.findById(seckillProductId);
                if (Objects.nonNull(product)) {
                    productService.updateEsProductStock(seckillProductId, product.getWithHoldQuantity());
                }
            } catch (Exception e) {
                log.error("ES同步库存失败", e);
                esOk = false;
            }

            // 4.Redis或者ES失败，提交DB事务之后异步线程补偿重试
            if (!redisOk || !esOk) {
                asyncCompensate(seckillProductId, quantity, !redisOk, !esOk);
            }
            return true;
        } catch (Exception e) {
            log.error("恢复库存失败", e);
            throw e; // DB操作异常抛出，触发事务回滚
        }
    }

    /**
     * 扣减库存（下单场景）
     * DB扣减预扣库存；Redis扣缓存；Redis异常只打日志，不回滚DB；无ES同步；返回true，无库存不足判断
     * @param seckillProductId 秒杀商品ID
     * @param quantity 扣减数量
     * @return true
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reduceStock(Long seckillProductId, Integer quantity) {
        // DB扣减预扣库存
        seckillProductMapper.reduceWithHoldStock(seckillProductId);
        try {
            String stockKey = BusinessKeyUtil.getProductStockKey(seckillProductId);
            redisUtil.increment(stockKey, -quantity);
        } catch (Exception e) {
            log.error("Redis减少库存失败", e);
        }
        return true;
    }

    /**
     * 全量同步库存：以DB预扣库存为准，覆盖Redis、刷新ES；用于预热、修复脏缓存
     * @param seckillProductId 秒杀商品ID
     * @return false商品不存在；true同步完成
     */
    @Override
    public boolean syncStock(Long seckillProductId) {
        SeckillProductEntity product = seckillProductMapper.findById(seckillProductId);
        if (Objects.isNull(product)) return false;

        // 覆盖写Redis库存
        String stockKey = BusinessKeyUtil.getProductStockKey(seckillProductId);
        redisUtil.set(stockKey, product.getWithHoldQuantity().toString());
        // 更新ES索引库存
        productService.updateEsProductStock(seckillProductId, product.getWithHoldQuantity());
        return true;
    }

    /**
     * 校验DB预扣库存与Redis缓存库存是否一致快照比对
     * @param seckillProductId 秒杀商品ID
     * @return true一致；false不一致/商品不存在/redis为空
     */
    @Override
    public boolean checkStockConsistency(Long seckillProductId) {
        SeckillProductEntity product = seckillProductMapper.findById(seckillProductId);
        if (Objects.isNull(product)) return false;

        String stockKey = BusinessKeyUtil.getProductStockKey(seckillProductId);
        String redisStock = redisUtil.get(stockKey);
        Integer dbStock = product.getWithHoldQuantity();
        return Objects.equals(dbStock, redisStock != null ? Integer.valueOf(redisStock) : null);
    }

    /**
     * 异步补偿任务：自定义线程池执行，sleep 1s延迟重试Redis/ES；只重试一次，失败打日志告警
     * @param seckillProductId 秒杀商品ID
     * @param quantity 变更数量
     * @param needRedis 是否需要补偿Redis
     * @param needEs 是否需要补偿ES
     */
    private void asyncCompensate(Long seckillProductId, Integer quantity,
                                 boolean needRedis, boolean needEs) {
        stockCompensationExecutor.execute(() -> {
            try {
                Thread.sleep(1000); // 延迟1秒重试
                if (needRedis) {
                    String stockKey = BusinessKeyUtil.getProductStockKey(seckillProductId);
                    redisUtil.increment(stockKey, quantity);
                    log.info("Redis库存补偿成功: productId={}", seckillProductId);
                }
                if (needEs) {
                    SeckillProductEntity product = seckillProductMapper.findById(seckillProductId);
                    if (Objects.nonNull(product)) {
                        productService.updateEsProductStock(seckillProductId, product.getWithHoldQuantity());
                    }
                    log.info("ES库存补偿成功: productId={}", seckillProductId);
                }
            } catch (Exception e) {
                log.error("库存补偿失败: productId={}", seckillProductId, e);
            }
        });
    }
}

/*
====================业务总结====================
1、模块职责：库存一致性实现；管理三方存储：DB预扣库存(withHoldQuantity)、Redis缓存库存、ES搜索库存；
DB优先；Redis/ES允许短暂不一致，依靠异步补偿修复；用于秒杀下单、订单超时、退款、缓存预热巡检。

2、调用链路：
‑ reduceStock：下单流程 OrderServiceImpl → reduceStock() DB扣预扣库存，Redis‑quantity；
‑ restoreStock：①OrderTimeoutServiceImpl订单超时；②退款完成；DB恢复预扣库存，Redis+quantity，更新ES；Redis/ES失败提交事务后异步补偿；
‑ syncStock：商品上架预热、脏缓存修复，DB覆盖Redis+ES；
‑ checkStockConsistency：定时任务巡检，比对DB与Redis库存；
‑ asyncCompensate：stockCompensationExecutor线程池，延迟1s重试一次Redis/ES。

3、核心流程：
restoreStock：
1.DB恢复预扣库存（事务内，失败回滚）
2.Redis恢复，异常捕获标记失败
3.ES更新，异常捕获标记失败
4.Redis/ES失败，提交事务后提交异步补偿任务；
asyncCompensate：sleep 1s，重试Redis、ES；补偿失败只打印日志，无二次重试。

reduceStock：
1.DB扣减预扣库存；
2.Redis扣减；Redis异常仅日志；直接返回true；**没有库存不足判断逻辑**。

syncStock：查询DB，直接set覆盖Redis库存，更新ES。

checkStockConsistency：读取DB withHoldQuantity，读取Redis字符串值，装箱Integer做equals比对。

4、技术设计亮点：
‑ 存储分层：DB作为真相源；Redis做缓存；ES用于搜索展示；
‑ 事务设计：DB操作在事务；Redis、ES外部资源异常不回滚数据库；用异步补偿做最终一致性；
‑ 独立线程池 stockCompensationExecutor，不和业务线程池混用，避免补偿任务拖垮业务；
‑ Redis/ES异常隔离，不阻塞主业务流程；
‑ syncStock提供一键修复入口；checkStockConsistency用于巡检告警。

5、风险点 & 潜在坑：
① reduceStock严重缺陷：
 ‑ seckillProductMapper.reduceWithHoldStock()看不到参数quantity；Mapper方法没有传入扣减数量，极有可能写死固定扣1；无法支持多件购买；
 ‑ DB扣减没有乐观锁判断库存是否充足；不管库存够不够直接扣；会出现负库存；
 ‑ 返回永远true，上层调用拿不到库存不足信号；超卖风险极高。

② restoreStock：
 ‑ restoreWithHoldStock同样没有传入quantity；DB直接恢复全部预扣库存，不是恢复本次quantity；多订单并发恢复会错乱；
 ‑ 幂等缺失：MQ消息重复调用restoreStock，DB会重复恢复库存，造成超卖。

③ 异步补偿：
 ‑ asyncCompensate仅重试1次；重试依旧失败就永久不一致；没有补偿记录表，没有定时任务扫描补偿失败项；
 ‑ Thread.sleep()在线程池内占用工作线程；大量失败会耗尽stockCompensationExecutor线程；
 ‑ 补偿只执行一次，失败无告警。

④ checkStockConsistency：
 ‑ Redis读取字符串转Integer；Redis存null直接返回false；并发时刻快照比对，瞬时不一致属于正常，不能直接判定故障；
 ‑ 没有校验ES和DB一致性，只校验DB‑Redis。

⑤ syncStock：秒杀流量高峰直接覆盖Redis，会冲掉正在执行的预扣操作，造成库存错乱。

6、模块关联：StockConsistencyService接口；SeckillProductMapper；ProductService；RedisUtil；自定义线程池 stockCompensationExecutor；
被 OrderServiceImpl、OrderTimeoutServiceImpl、退款服务调用。

7、生产注意事项：
‑ Mapper方法 reduceWithHoldStock、restoreWithHoldStock 必须传入quantity参数；DB update使用乐观锁 where with_hold_quantity >= #{quantity}；
‑ reduceStock 需要返回扣减结果，区分库存不足、成功；不能永远返回true；
‑ restoreStock 增加幂等控制，一张操作记录表记录订单‑商品‑数量，防止重复恢复；
‑ 放弃Thread.sleep；补偿任务发送RocketMQ重试消息，支持多次重试；增加补偿失败表，定时扫描告警；
‑ syncStock禁止秒杀高峰执行；只允许上架/低峰运维调用；
‑ checkStockConsistency巡检发现不一致只告警，不要自动修复；
‑ ES也纳入一致性巡检范围。
*/

