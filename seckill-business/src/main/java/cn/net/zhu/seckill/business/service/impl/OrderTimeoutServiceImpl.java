package cn.net.zhu.seckill.business.service.impl;

import cn.hutool.json.JSONUtil;
import cn.net.zhu.seckill.business.entity.mq.OrderTimeoutMessage;
import cn.net.zhu.seckill.business.entity.seckill.SeckillOrderTradeEntity;
import cn.net.zhu.seckill.business.mapper.seckill.SeckillOrderTradeMapper;
import cn.net.zhu.seckill.business.service.OrderTimeoutService;
import cn.net.zhu.seckill.business.service.StockConsistencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Objects;

/**
 * 订单超时关闭服务实现类
 * 基于RocketMQ延迟消息实现秒杀订单超时未支付自动关闭；
 * sendOrderTimeoutMessage：下单成功发送延迟消息；
 * handleOrderTimeout：消费者消费延迟消息，校验订单状态，关闭待支付订单，恢复库存；
 * 开源RocketMQ只支持固定延时等级，不支持任意时间延时。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTimeoutServiceImpl implements OrderTimeoutService {

    /**
     * RocketMQ操作模板，发送延迟消息
     */
    private final RocketMQTemplate rocketMQTemplate;
    /**
     * 订单交易Mapper，查询更新订单状态
     */
    private final SeckillOrderTradeMapper seckillOrderTradeMapper;
    /**
     * 库存一致性服务，订单超时取消后恢复DB+Redis库存
     */
    private final StockConsistencyService stockConsistencyService;

    /**
     * 订单超时消息Topic，配置文件可配置，默认ORDER_TIMEOUT_TOPIC
     */
    @Value("${seckill.orderTimeoutTopic:ORDER_TIMEOUT_TOPIC}")
    private String orderTimeoutTopic;

    /**
     * 发送订单超时延迟消息
     * 将订单信息封装为OrderTimeoutMessage，序列化为JSON；根据超时分钟换算RocketMQ delayLevel；
     * 异步发送消息，通过SendCallback接收发送成功/失败回调记录日志；
     * @param order 订单实体
     * @param timeoutMinutes 超时时间（分钟）
     */
    @Override
    public void sendOrderTimeoutMessage(SeckillOrderTradeEntity order, Integer timeoutMinutes) {
        OrderTimeoutMessage message = new OrderTimeoutMessage();
        message.setOrderId(order.getId());
        message.setOrderCode(order.getCode());
        message.setUserId(order.getUserId());
        message.setUserName(order.getUserName());
        message.setSeckillProductId(order.getSeckillProductId());
        message.setProductId(order.getProductId());
        message.setOrderTime(order.getOrderTime());
        message.setTimeoutMinutes(timeoutMinutes);
        message.setMessageCreateTime(new Date());

        // 根据传入超时分钟，计算RocketMQ固定延时等级
        int delayLevel = calculateDelayLevel(timeoutMinutes);

        // 异步发送延迟消息，3s发送超时，指定延时等级
        rocketMQTemplate.asyncSend(orderTimeoutTopic,
                MessageBuilder.withPayload(JSONUtil.toJsonStr(message)).build(),
                new SendCallback() {
                    @Override
                    public void onSuccess(org.apache.rocketmq.client.producer.SendResult result) {
                        log.info("超时消息发送成功: orderCode={}, msgId={}", order.getCode(), result.getMsgId());
                    }

                    @Override
                    public void onException(Throwable e) {
                        log.error("超时消息发送失败: orderCode={}", order.getCode(), e);
                    }
                }, 3000L, delayLevel);
    }

    /**
     * 消费端处理订单超时逻辑
     * MQ消费者收到延迟消息调用该方法；
     * 1.查询订单；2.校验订单状态必须为待支付(orderStatus=1)；3.二次校验时间是否真正超时；
     * 4.更新订单状态为已取消；5.调用库存一致性服务恢复库存；
     * @param orderCode 业务订单编号
     * @return true：执行了超时关闭；false：订单不存在/状态不符/未到超时时间，跳过处理
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleOrderTimeout(String orderCode) {
        log.info("处理订单超时: orderCode={}", orderCode);
        SeckillOrderTradeEntity order = seckillOrderTradeMapper.findByCode(orderCode);
        if (Objects.isNull(order)) {
            log.warn("订单不存在: orderCode={}", orderCode);
            return false;
        }

        // 只有待支付订单(1)才允许超时关闭
        if (!Objects.equals(order.getOrderStatus(), 1)) {
            log.info("订单状态不是待支付，无需处理: orderCode={}, status={}", orderCode, order.getOrderStatus());
            return false;
        }

        // 二次时间校验：代码硬编码写死15分钟阈值，没有使用消息内timeoutMinutes
        long now = System.currentTimeMillis();
        if (order.getOrderTime() != null) {
            long orderTimeMs = order.getOrderTime().getTime();
            if (now - orderTimeMs < 15 * 60 * 1000) {
                log.info("订单尚未超时: orderCode={}", orderCode);
                return false;
            }
        }

        // 更新订单状态：已取消
        order.setOrderStatus(4);
        order.setUpdateTime(new Date());
        seckillOrderTradeMapper.update(order);

        // 恢复库存（DB库存+Redis预扣库存）
        stockConsistencyService.restoreStock(order.getSeckillProductId(), order.getQuantity());
        log.info("订单超时取消成功: orderCode={}", orderCode);
        return true;
    }

    /**
     * RocketMQ开源版延时等级换算
     * 开源RocketMQ不支持自定义任意延迟时间，只能使用预设等级：
     * 1s,5s,10s,30s,1m,2m,3m,4m,5m,6m,7m,8m,9m,10m,20m,30m,1h,2h
     * @param timeoutMinutes 业务超时分钟
     * @return rocketMQ delayLevel
     */
    private int calculateDelayLevel(Integer timeoutMinutes) {
        if (timeoutMinutes <= 1) return 5;      // 1min
        if (timeoutMinutes <= 5) return 9;      // 5min
        if (timeoutMinutes <= 10) return 14;    // 10min
        if (timeoutMinutes <= 15) return 15;    // 20min
        if (timeoutMinutes <= 30) return 16;    // 30min
        return 17;                              // 1h
    }
}

/*
====================业务总结====================
1、模块职责：订单超时关闭实现；RocketMQ延迟消息方案；下单发送延时消息；消费消息关闭待支付订单+回滚库存。
2、调用链路：
订单创建成功 → OrderService → sendOrderTimeoutMessage() 异步发送RocketMQ延迟消息；
MQ消费者监听 ORDER_TIMEOUT_TOPIC → 解析消息拿到orderCode → handleOrderTimeout(orderCode)；
handleOrderTimeout：校验订单状态、时间，更新订单为取消，调用stockConsistencyService.restoreStock恢复库存。
3、核心流程：
① sendOrderTimeoutMessage：组装OrderTimeoutMessage，根据超时分钟映射RocketMQ delayLevel，异步发送；发送成功/失败打印日志；
② handleOrderTimeout：
 ‑ 查询订单；不存在直接返回false；
 ‑ 非待支付直接跳过；
 ‑ 二次校验时间；
 ‑ 事务内更新订单状态为已取消；恢复库存；返回true。
③ calculateDelayLevel：业务分钟转RocketMQ固定延时等级。
4、技术设计亮点：
‑ 使用RocketMQ延迟消息，不用定时任务轮库，减轻DB压力；
‑ asyncSend异步发送，不阻塞下单主流程；SendCallback记录发送结果；
‑ handleOrderTimeout做多层校验：订单存在校验、状态校验、时间二次校验，天然支持消息重复消费幂等；
‑ 抽离StockConsistencyService统一处理库存恢复，DB与Redis库存一致性封装；
‑ Topic配置化，yaml配置覆盖。
5、风险点 & 潜在坑：
‑ 严重不一致：sendOrderTimeoutMessage传入timeoutMinutes，但是handleOrderTimeout硬编码15分钟做时间判断，没有使用消息体里timeoutMinutes；如果业务改成30分钟超时，消费端依旧按15分钟判断，逻辑bug。
‑ RocketMQ开源只有固定延时等级，业务设置12分钟超时，会映射到20分钟延时等级，消息实际20分钟后才到达，和业务预期不一致。
‑ asyncSend发送失败只是打error日志，没有重试、没有落库本地消息表；消息丢失订单永远不会关闭，库存泄露。
‑ 没有消息幂等表；依靠订单状态做幂等；如果消息重复消费没问题，但订单数据被人为篡改会出问题。
‑ handleOrderTimeout入参只传orderCode；消费者需要自己解析MQ消息拿到orderCode；当前类没有消费者代码。
‑ restoreStock抛出异常会触发事务回滚：订单状态回滚，但MQ消息会重试，不断重复消费。
‑ 消息体全部字段序列化JSON，没有做压缩；大消息占用带宽。
6、模块关联：依赖RocketMQTemplate；OrderTimeoutMessage MQ消息体；SeckillOrderTradeMapper；StockConsistencyService。
7、生产注意事项：
‑ handleOrderTimeout时间校验，优先使用消息体中的timeoutMinutes，不要硬编码15分钟；
‑ RocketMQ开源延时等级限制要文档说明，业务超时时间建议对齐RocketMQ预设档位；
‑ 发送失败增加本地消息表/可靠消息方案，补偿发送；增加定时任务兜底扫描超时待支付订单，应对MQ消息丢失；
‑ restoreStock要做幂等，防止重复恢复库存造成超卖；
‑ 消费者侧做好消息重试、死信队列配置；处理失败消息进入死信人工排查；
‑ 增加监控告警：消息发送失败、死信队列消息告警。
*/

