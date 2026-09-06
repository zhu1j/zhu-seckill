package cn.net.zhu.seckill.business.service;

import cn.net.zhu.seckill.business.entity.seckill.SeckillOrderTradeEntity;

/**
 * 订单超时服务接口
 * 处理秒杀订单超时未支付能力：发送延迟消息；消费延迟消息执行订单超时关闭逻辑；
 * 秒杀订单下单成功后，开启超时倒计时；超时未支付则关闭订单、释放库存。
 *
 * @author 一只朱
 * @date 2026-09-06 15:47
 *
 * "Run the code. Run the world."
 */

public interface OrderTimeoutService {

    /**
     * 发送订单超时延迟消息
     * 下单成功调用；发送延迟消息，经过timeoutMinutes分钟后消息可达MQ消费者；
     * @param order 订单实体
     * @param timeoutMinutes 超时时间，单位：分钟，例如30代表30分钟未支付则关闭订单
     */
    void sendOrderTimeoutMessage(SeckillOrderTradeEntity order, Integer timeoutMinutes);

    /**
     * 处理订单超时逻辑
     * MQ消费者收到延迟消息后执行；校验订单当前状态，如果仍然未支付，则关闭订单，释放库存；
     * 如果订单已经支付/已经取消，直接忽略本次超时消息（幂等）
     * @param orderCode 业务订单编号
     * @return boolean true 执行了订单超时关闭逻辑；false 订单状态不满足，跳过处理
     */
    boolean handleOrderTimeout(String orderCode);
}

/*
====================业务总结====================
1、模块职责：订单超时关闭领域接口；基于延迟消息实现秒杀订单自动关单；两个核心能力：发送延迟消息、消费消息处理超时。
2、调用链路：
订单创建成功 → OrderService → sendOrderTimeoutMessage() 发送延迟消息；
MQ延迟队列消费者监听到消息 → handleOrderTimeout(orderCode) 执行关单+释放库存。
3、核心流程：
① sendOrderTimeoutMessage：拿到订单信息，向MQ投递延迟消息；消息延时时长由timeoutMinutes控制；
② handleOrderTimeout：消费消息；根据orderCode查询订单；校验订单状态是否为【待支付】；
  ‑ 待支付：执行关闭订单，修改订单状态，释放DB库存、还原Redis预扣库存；返回true；
  ‑ 已支付 / 已关闭：直接返回false，不做任何处理，保证消息幂等。
4、技术设计亮点：
‑ 使用延迟消息解耦，不需要定时轮询全量订单库，减少DB压力；
‑ sendOrderTimeoutMessage只负责发消息，业务处理全部下沉handleOrderTimeout；
‑ handleOrderTimeout入参使用业务orderCode，不使用数据库主键ID；
‑ 天然预留幂等设计：订单状态不符合直接跳过，支持消息重复投递。
5、风险点 & 潜在坑：
‑ 延迟消息丢失风险：消息丢失则订单永远不会自动关闭，会占住库存；需要补偿机制（定时扫描兜底）；
‑ 消息重复消费：依赖handleOrderTimeout内部状态校验做幂等；
‑ 消息到达时间只是近似值，不能做到绝对精准；
‑ sendOrderTimeoutMessage发送MQ本身可能失败，需要处理发送失败降级；
‑ handleOrderTimeout内部必须事务，保证订单状态更新、库存释放原子。
6、模块关联：实现类 OrderTimeoutServiceImpl；依赖MQ(RocketMQ延迟消息/RabbitMQ延迟插件)；关联 SeckillOrderTradeService、库存操作；下单业务调用 sendOrderTimeoutMessage。
7、生产注意事项：
‑ MQ消息务必做可靠投递；发送失败记录日志，后台补偿；
‑ 增加定时任务兜底：周期扫描待支付、超过超时时间的订单，补偿关闭；应对消息丢失场景；
‑ handleOrderTimeout必须加事务；更新订单、释放库存要原子；
‑ 做好日志打印：订单号、处理结果，方便排查超时关单问题；
‑ 不要在sendOrderTimeoutMessage做业务逻辑，只负责投递消息。
*/

