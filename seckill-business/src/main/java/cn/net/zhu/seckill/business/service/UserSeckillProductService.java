package cn.net.zhu.seckill.business.service;

import cn.net.zhu.seckill.business.entity.response.SeckillResultResponse;
import cn.net.zhu.seckill.business.entity.seckill.UserSeckillProductEntity;
import cn.net.zhu.seckill.business.entity.seckill.UserSeckillProductQueryEntity;

/**
 * 用户秒杀下单服务接口
 * 处理秒杀发起、异步结果查询、创建订单核心能力；
 * 秒杀采用异步排队模式：doSeckillProduct提交秒杀请求入队列，轮询getResultWithProgress获取秒杀结果，最终createOrder生成订单。
 *
 * @author 一只朱
 * @date 2026-09-06 10:38
 *
 * "Run the code. Run the world."
 */

public interface UserSeckillProductService {

    /**
     * 提交秒杀请求
     * 将用户秒杀请求送入消息队列，异步处理，不同步返回秒杀结果；
     * 前端调用此方法后，需要轮询 {@link #getResultWithProgress(UserSeckillProductQueryEntity)} 获取最终秒杀结果
     * @param entity 用户秒杀请求实体：用户id、商品id、验证码相关信息
     */
    void doSeckillProduct(UserSeckillProductEntity entity);

    /**
     * 查询秒杀进度与结果
     * 前端轮询接口，查询当前用户针对该商品的秒杀状态：排队中、秒杀成功、秒杀失败
     * @param queryEntity 查询条件：用户id、商品id
     * @return SeckillResultResponse 秒杀结果响应，包含状态码、提示信息、订单号（成功时返回）
     */
    SeckillResultResponse getResultWithProgress(UserSeckillProductQueryEntity queryEntity);

    /**
     * 执行创建订单
     * 秒杀消费端消费消息成功后调用，生成真实业务订单，扣减数据库库存，保存订单记录
     * @param entity 用户秒杀请求实体，携带用户、商品信息
     */
    void createOrder(UserSeckillProductEntity entity);
}

/*
====================业务总结====================
1、模块职责：用户秒杀核心业务接口，实现异步秒杀流程，削峰填谷，抵御高并发流量；
2、调用链路：
   前端排队页面 → doSeckillProduct提交秒杀请求 → 送入MQ消息队列；
   前端循环轮询 getResultWithProgress 查询秒杀结果；
   MQ消费者消费消息 → 校验库存、扣减库存 → 调用 createOrder 创建数据库订单；
3、核心流程：
   ① doSeckillProduct：接收秒杀参数，做前置校验，请求投递MQ，立刻返回，不阻塞前端；
   ② getResultWithProgress：查询Redis中秒杀状态标记，返回排队中/成功/失败状态供前端展示；
   ③ createOrder：消费者侧执行，落库生成订单，操作MySQL真实库存；
4、技术设计亮点：
   - 异步化：秒杀请求不同步执行业务，借助MQ削峰，保护数据库；
   - 状态轮询：不使用长连接，前端http轮询获取秒杀结果，降低服务压力；
5、风险点 & 潜在坑：
   - doSeckillProduct仅提交任务，不代表秒杀成功；成功与否必须看轮询返回结果；
   - createOrder在消费者执行，需要做幂等，防止消息重复消费生成重复订单；
   - 需要控制前端轮询频率，避免轮询打满服务；
6、模块关联：对接Controller排队接口、MQ消费者、Redis状态存储、订单数据库；
7、生产注意事项：MQ消息丢失、重复消费需要做补偿与幂等处理；Redis存储秒杀中间状态需要设置过期时间。
*/
