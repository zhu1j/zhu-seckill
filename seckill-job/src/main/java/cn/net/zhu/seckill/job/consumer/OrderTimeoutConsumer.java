package cn.net.zhu.seckill.job.consumer;

import cn.hutool.json.JSONUtil;
import cn.net.zhu.seckill.business.entity.mq.OrderTimeoutMessage;
import cn.net.zhu.seckill.business.service.OrderTimeoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 订单超时延迟消息消费者
 * 消费RocketMQ延迟消息，接收下单时投递的订单超时消息；解析消息拿到orderCode，调用OrderTimeoutService执行订单超时关闭逻辑；
 * 消息异常抛出RuntimeException触发MQ重试；达到重试上限进入死信队列。
 *
 * @author 一只朱
 * @date 2026-09-06 16:08
 *
 * "Run the code. Run the world."
 */

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "${seckill.orderTimeoutTopic:ORDER_TIMEOUT_TOPIC}",
        consumerGroup = "${seckill.orderTimeoutGroup:SECKILL_ORDER_TIMEOUT_GROUP}"
)
public class OrderTimeoutConsumer implements RocketMQListener<MessageExt> {

    private final OrderTimeoutService orderTimeoutService;

    /**
     * MQ消息消费入口
     * @param message RocketMQ原始消息对象
     */
    @Override
    public void onMessage(MessageExt message) {
        try {
            String content = new String(message.getBody());
            log.info("收到订单超时消息: {}", content);
            // JSON反序列化为订单超时消息体
            OrderTimeoutMessage timeoutMessage = JSONUtil.toBean(content, OrderTimeoutMessage.class);
            // 传入业务订单号执行超时关闭逻辑
            orderTimeoutService.handleOrderTimeout(timeoutMessage.getOrderCode());
        } catch (Exception e) {
            log.error("处理订单超时消息失败", e);
            // 抛出异常，RocketMQ触发消息重试
            throw new RuntimeException("处理订单超时消息失败", e);
        }
    }
}

/*
====================业务总结====================
1、模块职责：订单超时延迟消息消费端；消费ORDER_TIMEOUT_TOPIC延迟消息；驱动订单超时关闭、释放库存。

2、调用链路：
下单成功 → OrderTimeoutService.sendOrderTimeoutMessage() 发送RocketMQ延迟消息
→ OrderTimeoutConsumer#onMessage()
→ 解析得到OrderTimeoutMessage
→ OrderTimeoutService.handleOrderTimeout(orderCode)：校验订单状态，关闭待支付订单，恢复库存。

3、核心流程：
① 获取原始MessageExt消息，body转字符串；
② hutool JSONUtil反序列化得到OrderTimeoutMessage；
③ 取出orderCode交给handleOrderTimeout处理；
④ 捕获全部异常，打印错误日志，抛出运行时异常，RocketMQ执行重试。

4、技术设计亮点：
‑ topic、consumerGroup配置化，yaml可配置，适配多环境；
‑ 消费原生MessageExt，可以获取msgId、重试次数等元信息；
‑ 消费层只负责消息解析转发，业务逻辑全部下沉OrderTimeoutService；
‑ 异常抛出触发MQ重试，消息不会直接丢失。

5、风险点 & 潜在坑：
‑ 脏消息风险：消息体JSON格式错误，反序列化异常，会一直重试直到最大重试次数；没有脏消息过滤逻辑。
‑ 缺少msgId、reconsumeTimes日志，排查重复消费、消息堆积问题不方便。
‑ handleOrderTimeout内部依靠订单状态做幂等；但没有消费记录表；极端情况下消息重复消费+订单数据被篡改会出现问题。
‑ 全部异常统一重试：业务类异常（订单不存在、订单已支付）也会抛出异常触发重试，属于无效重试，浪费MQ资源。
‑ 没有body空值判断；body为空直接抛异常触发重试。
‑ 消息体OrderTimeoutMessage携带timeoutMinutes，但consumer只取出orderCode传给handleOrderTimeout；
  handleOrderTimeout旧代码硬编码15分钟时间判断，没有使用消息内timeoutMinutes，前后逻辑不一致bug。
‑ 没有配置死信队列，重试耗尽消息直接丢弃，丢失订单超时关闭能力，库存泄露。

6、模块关联：OrderTimeoutService；消息体OrderTimeoutMessage；对应生产者OrderTimeoutServiceImpl。

7、生产注意事项：
‑ 日志补充msgId、重试次数：
log.info("收到订单超时消息 msgId={},reconsumeTimes={},content={}",message.getMsgId(),message.getReconsumeTimes(),content);
‑ body增加非空校验，空消息直接return消费成功，丢弃脏消息。
‑ 区分异常类型：
  系统异常(Redis/DB异常)：抛出异常允许重试；
  业务异常(订单不存在、订单已支付、已关闭)：打印日志，不要抛出异常，直接消费成功return，避免无效重试。
‑ handleOrderTimeout要使用消息体里timeoutMinutes做时间校验，消除硬编码15分钟。
‑ 配置死信队列DLQ，重试耗尽消息进入死信，增加监控告警。
‑ 增加定时任务兜底扫描超时待支付订单，补偿MQ消息丢失场景。
‑ 监控消费堆积告警，防止消息消费能力跟不上消息量。
‑ 配置合理最大重试次数，避免无限重试。
*/

