package cn.net.zhu.seckill.job.consumer;

import cn.hutool.json.JSONUtil;
import cn.net.zhu.seckill.business.entity.seckill.UserSeckillProductEntity;
import cn.net.zhu.seckill.business.service.UserSeckillProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 秒杀下单消息消费者
 * 接收前端秒杀请求投递过来的MQ消息；解析消息体，调用UserSeckillProductService执行业务创建秒杀订单；
 * 异常抛出RuntimeException触发RocketMQ重试；消费失败达到重试上限进入死信队列。
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
        topic = "${seckill.seckillProductTopic:SECKILL_PRODUCT_TOPIC}",
        consumerGroup = "${seckill.seckillProductGroup:SECKILL_PRODUCT_GROUP}"
)
public class UserSeckillProductConsumer implements RocketMQListener<MessageExt> {

    private final UserSeckillProductService userSeckillProductService;

    /**
     * MQ消息回调入口
     * @param message RocketMQ原始消息对象MessageExt
     */
    @Override
    public void onMessage(MessageExt message) {
        try {
            String content = new String(message.getBody());
            log.info("收到秒杀消息: {}", content);
            // JSON反序列化为用户秒杀实体
            UserSeckillProductEntity entity = JSONUtil.toBean(content, UserSeckillProductEntity.class);
            // 执行业务逻辑：创建秒杀订单
            userSeckillProductService.createOrder(entity);
        } catch (Exception e) {
            log.error("处理秒杀消息失败", e);
            // 抛出异常，RocketMQ会进行消息重试
            throw new RuntimeException("处理秒杀消息失败", e);
        }
    }
}

/*
====================业务总结====================
1、模块职责：RocketMQ消费者；秒杀异步下单消费端；削峰填谷，把高并发秒杀请求放入MQ异步消费，保护数据库。

2、调用链路：
网关/秒杀接口 → Producer发送消息到 SECKILL_PRODUCT_TOPIC
→ UserSeckillProductConsumer#onMessage()
→ UserSeckillProductService#createOrder(entity) 执行创建订单、扣库存。

3、核心流程：
① 收到原始MessageExt消息；取出byte[] body转字符串；
② hutool JSONUtil反序列化得到UserSeckillProductEntity；
③ 调用createOrder执行业务下单；
④ 捕获全部异常，打印错误日志，抛出RuntimeException，触发RocketMQ重试机制。

4、技术设计亮点：
‑ 配置化topic、consumerGroup，yaml配置覆盖，便于多环境切换；
‑ 消费MessageExt原始消息，可以拿到msgId、reconsumeTimes等元信息；
‑ 异常抛出触发MQ重试，失败不丢弃消息；
‑ 独立consumer组件，业务逻辑全部下沉UserSeckillProductService，消费层只做消息解析转发。

5、风险点 & 潜在坑：
‑ JSON反序列化异常会抛出异常触发重试；消息体格式错误（脏消息）会无限重试直到最大重试次数，最后进入死信队列；没有做消息格式校验。
‑ 缺少幂等控制：同一个消息重复投递，会重复调用createOrder，造成重复下单，生成多条订单，超卖。
‑ 没有使用message.getMsgId做幂等去重；没有消费记录表。
‑ 全部异常一律重试；业务异常（例如用户限购、库存不足）不应该重试，当前代码也会走重试，浪费MQ资源。
‑ 没有打印msgId、reconsumeTimes日志，排查重复消费问题不方便。
‑ 没有对body判空；空消息body会直接抛出异常触发重试。
‑ 消费者没有设置最大重试次数，依赖RocketMQ默认配置。

6、模块关联：依赖 UserSeckillProductService；接收Producer投递的秒杀消息；Topic：SECKILL_PRODUCT_TOPIC。

7、生产注意事项：
‑ 增加msgId日志输出：log.info("收到秒杀消息 msgId={}, reconsumeTimes={}, content={}", message.getMsgId(), message.getReconsumeTimes(), content);
‑ 增加幂等：使用msgId做幂等key，Redis/DB消费记录，消费过直接return，不执行业务；防止重复消费生成重复订单。
‑ 区分异常类型：
  • 系统异常(DB、Redis异常)：抛出异常允许重试；
  • 业务异常（库存不足、用户限购、重复下单）：不要抛出异常，直接消费成功return，避免无效重试。
‑ 增加body非空校验；脏消息直接丢弃或者转入死信。
‑ 配置死信队列DLQ，消费失败达到上限消息进入死信，后台告警人工处理。
‑ 监控consumer堆积，消息堆积告警，防止秒杀流量过大消费跟不上。
‑ 调整RocketMQ消费者最大重试次数，不要无限制重试。
*/

