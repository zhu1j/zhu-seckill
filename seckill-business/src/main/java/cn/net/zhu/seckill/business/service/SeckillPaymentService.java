package cn.net.zhu.seckill.business.service;

import cn.net.zhu.seckill.business.entity.seckill.SeckillPaymentEntity;
import java.util.List;

/**
 * 秒杀支付记录服务接口
 * 管理订单的支付流水；创建支付单、处理第三方回调支付结果、查询支付记录、取消支付；
 * 一个订单可以有多条支付流水（重复发起支付场景）；支付完成后要更新订单支付状态。
 */
public interface SeckillPaymentService {

    /**
     * 创建支付单
     * 根据业务订单号与支付方式生成一条待支付的支付流水记录
     * @param orderCode 业务订单编号
     * @param paymentMethod 支付方式
     * @return SeckillPaymentEntity 生成的支付流水实体
     */
    SeckillPaymentEntity createPayment(String orderCode, Integer paymentMethod);

    /**
     * 处理第三方支付回调，完成支付
     * 更新支付流水为已支付，同时驱动更新对应订单的支付状态；
     * @param paymentId 支付流水主键ID
     * @param thirdPartyTransactionNo 第三方平台交易号
     * @return boolean true支付处理成功；false失败（状态不允许、不存在等）
     */
    boolean processPayment(Long paymentId, String thirdPartyTransactionNo);

    /**
     * 根据支付流水主键ID查询支付单详情
     * @param paymentId 支付记录主键
     * @return SeckillPaymentEntity 支付流水实体，查不到返回null
     */
    SeckillPaymentEntity getPaymentDetail(Long paymentId);

    /**
     * 根据业务订单号查询该订单全部支付流水（支持多次支付尝试）
     * @param orderCode 业务订单编号
     * @return List<SeckillPaymentEntity> 该订单下所有支付记录集合
     */
    List<SeckillPaymentEntity> getPaymentsByOrderCode(String orderCode);

    /**
     * 取消支付流水
     * 将支付单置为取消状态；不会变更订单主状态；用于订单取消时同步作废对应的支付流水
     * @param paymentId 支付流水主键ID
     * @return boolean true取消成功；false不存在或状态不允许取消
     */
    boolean cancelPayment(Long paymentId);
}

/*
====================业务总结====================
1、模块职责：支付流水领域接口；维护订单与第三方之间的支付记录；一个订单允许多条支付流水，适配用户多次发起支付。
2、调用链路：
支付页面 → Controller → SeckillPaymentService#createPayment 创建支付单；
第三方支付回调接口 → processPayment() 完成支付，同步更新订单状态；
订单取消流程 → cancelPayment() 作废对应支付流水。
3、核心流程：
① createPayment：订单发起支付时生成待支付流水；
② processPayment：接收第三方交易号，标记支付成功，联动更新订单为已支付；
③ getPaymentDetail / getPaymentsByOrderCode：查询支付流水；
④ cancelPayment：作废支付流水，不修改订单，仅处理支付记录自身状态。
4、技术设计亮点：
- 订单与支付记录一对多：允许同一订单多次发起支付；
- 分离支付流水与订单主表：订单负责订单状态，SeckillPaymentEntity专门记录第三方交易信息；
- processPayment接收第三方交易号留存对账凭证。
5、风险点 & 潜在坑：
- processPayment 需要幂等，防止第三方重复回调造成订单重复置为已支付；
- cancelPayment只作废支付流水，不会修改订单状态，业务调用方要保证订单‑支付流水状态一致性；
- 需要处理并发：同时回调、同时取消订单+支付回调同时到达的竞态问题。
6、模块关联：关联 SeckillOrderTradeService；支付完成后需要更新订单支付状态；第三方回调依赖本接口。
7、生产注意事项：
- processPayment 必须增加幂等校验，第三方交易号需要唯一索引；
- 支付、订单状态变更建议使用数据库事务保证一致性；
- 对账场景依靠 thirdPartyTransactionNo 与第三方平台核对资金。
*/
