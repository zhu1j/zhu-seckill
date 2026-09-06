package cn.net.zhu.seckill.business.service.impl;

import cn.hutool.json.JSONUtil;
import cn.net.zhu.seckill.business.context.UserContext;
import cn.net.zhu.seckill.business.entity.response.SeckillResultResponse;
import cn.net.zhu.seckill.business.entity.seckill.*;
import cn.net.zhu.seckill.business.entity.user.UserEntity;
import cn.net.zhu.seckill.business.exception.BusinessException;
import cn.net.zhu.seckill.business.helper.IdGenerateHelper;
import cn.net.zhu.seckill.business.mapper.seckill.SeckillOrderTradeMapper;
import cn.net.zhu.seckill.business.mapper.seckill.SeckillProductMapper;
import cn.net.zhu.seckill.business.mapper.user.SeckillUserMapper;
import cn.net.zhu.seckill.business.service.OrderTimeoutService;
import cn.net.zhu.seckill.business.service.ProductService;
import cn.net.zhu.seckill.business.service.UserSeckillProductService;
import cn.net.zhu.seckill.business.util.BusinessKeyUtil;
import cn.net.zhu.seckill.business.util.OrderCodeUtil;
import cn.net.zhu.seckill.business.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户秒杀下单服务实现类
 * (异步秒杀核心实现；前端提交秒杀请求，前置多层校验，Redis预扣库存，投递RocketMQ异步下单；
 * MQ消费者消费消息执行真实创建订单、DB扣库存；前端轮询Redis进度状态获取秒杀结果；
 * 多层限流防护：登录校验、每日参与次数限制、时间窗口、本地标记、Redis库存防超卖、防重复抢购。)
 *
 * @author 一只朱
 * @date 2026-09-06 10:48
 *
 * "Run the code. Run the world."
 */

/** todo 这是这个项目最复杂的文件
 * 关键实现逻辑：
 * doSeckillProduct:
 *   1. 检查登录
 *   2. 检查用户当天秒杀次数（Redis计数）
 *   3. 检查商品时间窗口
 *   4. 检查本地低库存标记 (ConcurrentHashMap)
 *   5. 检查Redis库存
 *   6. 检查是否已购买
 *   7. Redis原子DECR库存
 *   8. 增加用户秒杀计数
 *   9. 异步发送RocketMQ消息
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSeckillProductServiceImpl implements UserSeckillProductService {

    /**
     * Redis工具类，存放库存、用户限购、秒杀进度、用户已购标记
     */
    private final RedisUtil redisUtil;

    /**
     * 商品服务，读取ES商品详情信息
     */
    private final ProductService productService;

    /**
     * RocketMQ模板，发送秒杀下单消息，实现流量削峰异步化
     */
    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 秒杀商品Mapper，执行数据库真实预扣库存
     */
    private final SeckillProductMapper seckillProductMapper;

    /**
     * 秒杀订单Mapper，订单数据入库查询
     */
    private final SeckillOrderTradeMapper seckillOrderTradeMapper;

    /**
     * 秒杀用户Mapper，查询用户数据库信息
     */
    private final SeckillUserMapper seckillUserMapper;

    /**
     * ID生成工具，生成雪花算法主键ID
     */
    private final IdGenerateHelper idGenerateHelper;

    /**
     * 订单超时服务，发送订单超时延时消息，用于未支付订单自动关闭
     */
    private final OrderTimeoutService orderTimeoutService;

    /**
     * 秒杀RocketMQ Topic名称，配置文件读取
     */
    @Value("${seckill.seckillProductTopic:SECKILL_PRODUCT_TOPIC}")
    private String seckillProductTopic;

    /**
     * 订单超时未支付时间，单位分钟，默认15分钟
     */
    @Value("${seckill.orderTimeoutMinutes:15}")
    private int orderTimeoutMinutes;

    /**
     * 本地低库存标记缓存，JVM内存ConcurrentHashMap；
     * 减少大量请求打到Redis；标记商品售罄之后直接本地拦截；
     * 注意：此标记仅本地生效，多实例环境不同步，仅做优化，不能作为唯一售罄判断依据。
     */
    private static final ConcurrentHashMap<Long, Boolean> LOCAL_LOW_STOCK = new ConcurrentHashMap<>();

    /**
     * 提交秒杀请求，异步秒杀入口
     * 做全部前置校验，Redis原子预扣库存，投递MQ消息；不真实创建订单；
     * 前端提交后需要轮询getResultWithProgress获取最终秒杀结果
     * @param entity 用户秒杀请求实体，包含商品ID、验证码相关参数
     */
    @Override
    public void doSeckillProduct(UserSeckillProductEntity entity) {
        // 1.登录校验：从ThreadLocal上下文获取当前登录用户
        UserEntity currentUser = UserContext.getCurrentUser();
        if (Objects.isNull(currentUser)) {
            throw new BusinessException(403, "请先登录");
        }
        entity.setUserName(currentUser.getUsername());

        // 2.每日参与秒杀次数校验，每个用户每日最多参与10次秒杀
        checkUserSeckillCount(currentUser.getUsername());

        // 3.获取商品详情，校验秒杀时间窗口：必须处于秒杀进行中时间段
        SeckillProductDetailPageEntity productInfo = productService.getProductDetail(
                entity.getSeckillProductId());
        checkTime(productInfo);

        // 4.JVM本地低库存标记快速拦截，减少Redis请求压力（优化手段）
        if (Boolean.TRUE.equals(LOCAL_LOW_STOCK.get(entity.getSeckillProductId()))) {
            throw new BusinessException("商品库存不足");
        }

        // 5.Redis读取商品库存，判断是否售罄
        String stockKey = BusinessKeyUtil.getProductStockKey(entity.getSeckillProductId());
        String stockValue = redisUtil.get(stockKey);
        if (!StringUtils.hasLength(stockValue) || Integer.parseInt(stockValue) <= 0) {
            // 设置本地标记，后续请求直接拦截
            LOCAL_LOW_STOCK.put(entity.getSeckillProductId(), true);
            throw new BusinessException("商品已售罄");
        }

        // 6.防重复抢购校验：Redis标记用户是否已经秒杀过该商品
        String userProductKey = BusinessKeyUtil.getUserSeckillProductKey(
                entity.getSeckillProductId(), currentUser.getUsername());
        if (StringUtils.hasLength(redisUtil.get(userProductKey))) {
            throw new BusinessException("您已参与过该商品的秒杀");
        }

        // 7.★核心★ ：Redis原子递减扣减库存；decrement原子操作，防止并发超卖
        Long remainStock = redisUtil.decrement(stockKey);
        if (remainStock == null || remainStock < 0) {
            // 库存扣减出现负数，需要回滚库存计数，标记商品售罄
            redisUtil.increment(stockKey);
            LOCAL_LOW_STOCK.put(entity.getSeckillProductId(), true);
            redisUtil.set(BusinessKeyUtil.getProductOverKey(entity.getSeckillProductId()), "1");
            throw new BusinessException("商品已售罄");
        }

        try {
            // 8.增加用户当日秒杀参与计数
            incrementUserSeckillCount(currentUser.getUsername());
            // 9.写入Redis秒杀进度状态：状态1，进度25%，抢购中
            setSeckillProcessStatus(entity.getSeckillProductId(), currentUser.getUsername(),
                    1, 25, "抢购中，正在处理...");
            // 10.★核心★：发送RocketMQ异步消息，交给消费者做真实下单
            send(seckillProductTopic, entity);
        } catch (Exception e) {
            // MQ发送失败，必须回滚Redis预扣库存，否则库存数据不一致
            redisUtil.increment(stockKey);
            throw new BusinessException("抢购失败，请重试");
        }
    }

    /**
     * 校验用户当日秒杀参与次数，上限10次
     * @param userName 登录用户名
     */
    private void checkUserSeckillCount(String userName) {
        String countKey = BusinessKeyUtil.getUserSeckillCountKey(userName);
        String countStr = redisUtil.get(countKey);
        if (StringUtils.hasLength(countStr) && Integer.parseInt(countStr) >= 10) {
            throw new BusinessException("您今天已参与10次秒杀，明天再来吧");
        }
    }

    /**
     * 用户当日秒杀计数+1；key过期时间24小时，实现按自然日限制
     * @param userName 登录用户名
     */
    private void incrementUserSeckillCount(String userName) {
        String countKey = BusinessKeyUtil.getUserSeckillCountKey(userName);
        String countStr = redisUtil.get(countKey);
        if (StringUtils.hasLength(countStr)) {
            redisUtil.increment(countKey);
        } else {
            redisUtil.set(countKey, "1", 24 * 60 * 60);
        }
    }

    /**
     * 校验秒杀时间窗口：未开始、已结束直接抛异常
     * @param productInfo 商品详情VO
     */
    private void checkTime(SeckillProductDetailPageEntity productInfo) {
        if (productInfo == null) {
            throw new BusinessException("商品不存在");
        }
        Date now = new Date();
        if (productInfo.getStartTime().after(now)) {
            throw new BusinessException("秒杀尚未开始");
        }
        if (!productInfo.getEndTime().after(now)) {
            throw new BusinessException("秒杀已结束");
        }
    }

    /**
     * 查询秒杀进度与结果；前端轮询调用
     * @param queryEntity 查询条件：商品id、用户信息
     * @return SeckillResultResponse 返回排队中/处理中/成功/失败，携带进度百分比
     */
    @Override
    public SeckillResultResponse getResultWithProgress(UserSeckillProductQueryEntity queryEntity) {
        // 登录校验
        UserEntity currentUser = UserContext.getCurrentUser();
        if (Objects.isNull(currentUser)) {
            return SeckillResultResponse.failure("请先登录");
        }
        // 查询Redis秒杀进度状态key
        String statusKey = BusinessKeyUtil.getSeckillProcessStatusKey(
                queryEntity.getSeckillProductId(), currentUser.getUsername());
        String statusValue = redisUtil.get(statusKey);
        if (!StringUtils.hasLength(statusValue)) {
            // 无状态记录，返回等待处理中
            return SeckillResultResponse.processing(0, "等待处理中...");
        }
        // redis存储格式：status:progress:message
        String[] parts = statusValue.split(":");
        int status = Integer.parseInt(parts[0]);
        int progress = Integer.parseInt(parts[1]);
        String message = parts.length > 2 ? parts[2] : "";

        if (status == 4) {
            // 状态4：秒杀流程已完成；查询Redis拿到订单ID，查询数据库订单实体返回
            String userProductKey = BusinessKeyUtil.getUserSeckillProductKey(
                    queryEntity.getSeckillProductId(), currentUser.getUsername());
            String orderId = redisUtil.get(userProductKey);
            if (StringUtils.hasLength(orderId)) {
                SeckillOrderTradeEntity order = seckillOrderTradeMapper.findById(
                        Long.valueOf(orderId));
                return SeckillResultResponse.success(order);
            }
        }
        if (status == -1) {
            // 状态‑1：秒杀失败
            return SeckillResultResponse.failure(message);
        }
        // 返回处理中以及当前进度
        return SeckillResultResponse.processing(progress, message);
    }

    /**
     * MQ消费者回调方法，执行真实创建订单，DB扣库存；
     * doSeckillProduct只是预扣Redis库存，真实落库在这里执行
     * @param entity 用户秒杀请求实体
     */
    @Override
    public void createOrder(UserSeckillProductEntity entity) {
        Long seckillProductId = entity.getSeckillProductId();
        String userName = entity.getUserName();

        // 更新进度：状态2，50%，正在创建订单
        setSeckillProcessStatus(seckillProductId, userName, 2, 50, "正在创建订单...");

        // 从ES读取商品基础信息
        EsSeckillProductEntity esProduct = productService.getProductFromES(seckillProductId);
        if (esProduct == null) {
            throw new BusinessException("商品信息不存在");
        }

        // 更新进度：状态3，75%，正在扣减数据库库存
        setSeckillProcessStatus(seckillProductId, userName, 3, 75, "正在扣减库存...");

        // DB层面预扣锁定库存
        seckillProductMapper.reduceWithHoldStock(seckillProductId);

        // 查询数据库用户信息
        cn.net.zhu.seckill.business.entity.user.SeckillUserEntity user =
                seckillUserMapper.findByUsername(userName);

        // 组装订单实体
        SeckillOrderTradeEntity order = new SeckillOrderTradeEntity();
        order.setId(idGenerateHelper.nextId());
        order.setCode(OrderCodeUtil.generateOrderCode());
        order.setUserId(user.getId());
        order.setUserName(userName);
        order.setSeckillProductId(seckillProductId);
        order.setProductId(esProduct.getProductId());
        order.setProductName(esProduct.getName());
        order.setModel(esProduct.getModel());
        order.setPrice(esProduct.getPrice());
        order.setCostPrice(esProduct.getCostPrice());
        order.setQuantity(1);
        order.setTotalAmount(esProduct.getPrice());
        order.setPaymentAmount(esProduct.getPrice());
        order.setOrderStatus(1);  // 下单状态
        order.setPayStatus(1);    // 待支付
        order.setOrderTime(new Date());

        // 订单插入数据库
        seckillOrderTradeMapper.insert(order);

        // Redis写入用户购买标记，value存储订单ID，用于轮询接口查询订单
        String userProductKey = BusinessKeyUtil.getUserSeckillProductKey(seckillProductId, userName);
        redisUtil.set(userProductKey, order.getId().toString());

        // 发送订单超时延时消息；15分钟未支付自动关闭订单、回滚库存
        orderTimeoutService.sendOrderTimeoutMessage(order, orderTimeoutMinutes);

        // 更新进度：状态4，100% 抢购成功
        setSeckillProcessStatus(seckillProductId, userName, 4, 100, "抢购成功！");
        log.info("用户 {} 秒杀商品 {} 成功，订单号：{}", userName, seckillProductId, order.getCode());
    }

    /**
     * 设置秒杀进度状态存入Redis；key5分钟过期，防止Redis垃圾数据堆积
     * @param productId 秒杀商品id
     * @param userName 用户名
     * @param status 业务状态码
     * @param progress 进度百分比
     * @param message 状态提示文本
     */
    private void setSeckillProcessStatus(Long productId, String userName,
                                         int status, int progress, String message) {
        String key = BusinessKeyUtil.getSeckillProcessStatusKey(productId, userName);
        String value = status + ":" + progress + ":" + message;
        redisUtil.set(key, value, 300);
    }

    /**
     * RocketMQ消息发送；优先异步发送，失败降级同步发送；30秒超时
     * @param topic mq主题
     * @param message 消息实体对象
     */
    public void send(String topic, Object message) {
        String jsonMessage = JSONUtil.toJsonStr(message);
        try {
            rocketMQTemplate.asyncSend(topic, jsonMessage, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.info("MQ发送成功: msgId={}, queue={}",
                            sendResult.getMsgId(), sendResult.getMessageQueue().getQueueId());
                }

                @Override
                public void onException(Throwable throwable) {
                    log.error("MQ异步发送失败，尝试同步发送: {}", throwable.getMessage());
                    try {
                        rocketMQTemplate.syncSend(topic, jsonMessage);
                        log.info("MQ同步发送成功");
                    } catch (Exception ex) {
                        log.error("MQ同步发送也失败", ex);
                    }
                }
            }, 30000);
        } catch (Exception e) {
            log.error("MQ消息发送异常", e);
            throw new BusinessException("消息发送失败");
        }
    }
}

/*
====================业务总结====================
1、模块职责：异步秒杀核心业务实现类；实现高并发秒杀削峰；请求入口做前置校验，MQ异步完成真实下单；
2、调用链路：
前端排队页面 → doSeckillProduct() 多层校验+Redis预扣库存 → RocketMQ发送消息；
前端循环轮询 getResultWithProgress() 查询Redis进度状态；
MQ消费者监听Topic → createOrder() 执行DB扣库存、生成订单、发送订单超时消息。
3、核心流程：
① doSeckillProduct：登录校验、每日10次限购校验、秒杀时间校验、本地标记+Redis库存校验、防重复抢购；Redis原子decrement预扣库存；投递MQ；MQ异常回滚Redis库存；
② getResultWithProgress：读取Redis进度字符串 status:progress:message；状态4代表完成，查询数据库订单返回；‑1代表失败；其余为处理中；
③ createOrder：消费者侧执行；更新各阶段进度；DB预扣锁定库存；插入订单记录；写入用户已购标记；发送延时消息用于超时未支付关闭订单；标记100%成功。
4、技术设计亮点：
- 多级拦截：JVM本地标记做第一层快速拦截，降低Redis压力；Redis原子decrement保证预扣库存不超卖；
- 异步削峰：RocketMQ异步下单，把秒杀写压力削峰，保护MySQL；异步发送失败降级同步发送；
- 进度轮询：Redis存储秒杀进度状态，前端http轮询获取结果，不使用长连接；
- 限购：用户每日最多参与10次秒杀，Redis key24小时自动过期；
- 订单超时：MQ延时消息实现15分钟未支付自动关闭订单；
- 异常容错：MQ发送失败必须回滚Redis预扣库存，防止库存丢失。
5、风险点 & 潜在坑：
- LOCAL_LOW_STOCK是JVM本地HashMap，多实例集群下不同步，只做优化，不能作为售罄唯一判断；
- Redis预扣库存和DB真实库存存在短暂不一致，最终由订单超时机制做兜底；
- MQ消息重复消费风险：createOrder需要做幂等；否则会生成重复订单；
- Redis进度状态5分钟过期，如果消费处理超过5分钟，前端查不到状态；
- 库存回滚逻辑只覆盖MQ发送异常；消费者内部异常需要额外补偿逻辑；
- ES仅用于商品信息展示，真实库存以MySQL为准。
6、模块关联：依赖ProductService、RocketMQ、Redis、多张mapper；和消费者配合执行createOrder；对接OrderTimeoutService处理超时订单。
7、生产注意事项：
- MQ消息丢失会导致Redis库存扣了但是没有生成订单，需要定时任务做库存对账补偿；
- 前端轮询频率需要限制，避免大量轮询请求打满服务；
- Redis中秒杀相关key需要设置过期时间，避免内存持续膨胀。
*/
