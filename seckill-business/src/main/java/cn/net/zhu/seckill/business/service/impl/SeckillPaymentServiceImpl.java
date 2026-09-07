package cn.net.zhu.seckill.business.service.impl;

import cn.net.zhu.seckill.business.entity.seckill.SeckillOrderTradeEntity;
import cn.net.zhu.seckill.business.entity.seckill.SeckillPaymentEntity;
import cn.net.zhu.seckill.business.helper.IdGenerateHelper;
import cn.net.zhu.seckill.business.mapper.seckill.SeckillOrderTradeMapper;
import cn.net.zhu.seckill.business.mapper.seckill.SeckillPaymentMapper;
import cn.net.zhu.seckill.business.service.SeckillPaymentService;
import cn.net.zhu.seckill.business.util.OrderCodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 秒杀支付流水服务实现类
 * 负责支付单创建、第三方支付回调处理、支付流水查询、取消支付流水；
 * 支付单与订单一对多，同一订单可多次发起支付；processPayment模拟第三方回调完成支付并联动更新订单状态。
 *
 * @author 一只朱
 * @date 2026-09-06 11:10
 *
 * "Run the code. Run the world."
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillPaymentServiceImpl implements SeckillPaymentService {

    /**
     * 支付流水Mapper，操作支付记录表
     */
    private final SeckillPaymentMapper seckillPaymentMapper;

    /**
     * 订单Mapper，查询&更新订单主表状态
     */
    private final SeckillOrderTradeMapper seckillOrderTradeMapper;

    /**
     * ID生成工具，生成支付记录主键
     */
    private final IdGenerateHelper idGenerateHelper;

    /**
     * 创建支付流水记录
     * 校验订单必须存在且为待支付状态(1)；生成唯一支付单号，插入支付流水，状态初始为待支付；
     * @param orderCode 业务订单号
     * @param paymentMethod 支付方式
     * @return SeckillPaymentEntity 新建支付流水实体
     * @throws RuntimeException 订单不存在、订单状态非待支付抛出异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeckillPaymentEntity createPayment(String orderCode, Integer paymentMethod) {
        // 查询订单，只有待支付订单才允许创建支付单
        SeckillOrderTradeEntity order = seckillOrderTradeMapper.findByCode(orderCode);
        if (Objects.isNull(order)) throw new RuntimeException("订单不存在");
        if (!Objects.equals(order.getOrderStatus(), 1)) throw new RuntimeException("订单状态不正确");

        // 组装支付流水实体
        SeckillPaymentEntity payment = new SeckillPaymentEntity();
        payment.setId(idGenerateHelper.nextId());
        payment.setPaymentNo(OrderCodeUtil.generateOrderCode());
        payment.setOrderId(order.getId());
        payment.setOrderCode(orderCode);
        payment.setUserId(order.getUserId());
        payment.setUserName(order.getUserName());
        payment.setPaymentAmount(order.getPaymentAmount());
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentStatus(1); // 待支付
        payment.setCreateTime(new Date());
        // 创建人信息，满足表NOT NULL约束
        payment.setCreateUserId(order.getUserId());
        payment.setCreateUserName(order.getUserName());
        seckillPaymentMapper.insert(payment);
        log.info("创建支付记录成功，订单号：{}", orderCode);
        return payment;
    }

    /**
     * 处理第三方支付回调，完成支付
     * 仅待支付状态(1)的支付单允许处理；先更新为支付中，再更新为支付成功，保存第三方交易号；联动更新订单状态为已支付；
     * 发生异常时将支付流水置为支付失败；
     * @param paymentId 支付流水主键ID
     * @param thirdPartyTransactionNo 第三方平台交易流水号，用于对账
     * @return true 支付处理成功；false 处理异常失败
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean processPayment(Long paymentId, String thirdPartyTransactionNo) {
        try {
            SeckillPaymentEntity payment = seckillPaymentMapper.findById(paymentId);
            if (Objects.isNull(payment)) throw new RuntimeException("支付记录不存在");
            if (!Objects.equals(payment.getPaymentStatus(), 1)) throw new RuntimeException("支付状态不正确");

            // 更新为支付中（模拟调用第三方支付）
            payment.setPaymentStatus(2);
            payment.setUpdateTime(new Date());
            seckillPaymentMapper.updatePaymentStatus(payment);

            // 模拟第三方返回支付成功，更新支付流水
            payment.setPaymentStatus(3);
            payment.setThirdPartyTransactionNo(thirdPartyTransactionNo);
            payment.setPaymentTime(new Date());
            payment.setUpdateTime(new Date());
            seckillPaymentMapper.updatePaymentStatus(payment);

            // 更新关联订单状态：订单状态=已支付，支付状态=已支付
            SeckillOrderTradeEntity order = seckillOrderTradeMapper.findById(payment.getOrderId());
            if (Objects.nonNull(order)) {
                order.setOrderStatus(2);  // 已支付
                order.setPayStatus(2);    // 已支付
                seckillOrderTradeMapper.update(order);
            }
            log.info("支付处理成功，支付ID：{}", paymentId);
            return true;
        } catch (Exception e) {
            log.error("支付处理失败", e);
            // 异常时把支付流水标记为支付失败
            SeckillPaymentEntity payment = seckillPaymentMapper.findById(paymentId);
            if (Objects.nonNull(payment)) {
                payment.setPaymentStatus(4);
                payment.setUpdateTime(new Date());
                seckillPaymentMapper.updatePaymentStatus(payment);
            }
            return false;
        }
    }

    /**
     * 根据支付流水主键查询支付详情
     * @param paymentId 支付记录主键
     * @return SeckillPaymentEntity 支付流水实体，查不到返回null
     */
    @Override
    public SeckillPaymentEntity getPaymentDetail(Long paymentId) {
        return seckillPaymentMapper.findById(paymentId);
    }

    /**
     * 根据业务订单号查询该订单全部支付流水记录
     * @param orderCode 业务订单编码
     * @return List<SeckillPaymentEntity> 订单下所有支付流水集合
     */
    @Override
    public List<SeckillPaymentEntity> getPaymentsByOrderCode(String orderCode) {
        return seckillPaymentMapper.findByOrderCode(orderCode);
    }

    /**
     * 取消支付流水
     * 仅待支付状态(1)可以取消；将支付流水置为取消/失败状态；**不会修改订单主状态**
     * @param paymentId 支付流水主键ID
     * @return true 取消成功
     * @throws RuntimeException 支付记录不存在、状态非待支付抛出异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelPayment(Long paymentId) {
        SeckillPaymentEntity payment = seckillPaymentMapper.findById(paymentId);
        if (Objects.isNull(payment)) throw new RuntimeException("支付记录不存在");
        if (!Objects.equals(payment.getPaymentStatus(), 1)) throw new RuntimeException("支付状态不正确");

        payment.setPaymentStatus(4);  // 支付失败/取消
        payment.setUpdateTime(new Date());
        seckillPaymentMapper.updatePaymentStatus(payment);
        return true;
    }
}

/*
====================业务总结====================
1、模块职责：支付流水业务实现；创建支付单、模拟第三方回调完成支付、查询、取消支付流水；订单与支付单一对多，支持多次发起支付。
2、调用链路：
支付页面 → createPayment() 创建待支付流水；
第三方回调接口 → processPayment()，更新支付流水+订单状态；
订单取消流程 → cancelPayment()，作废未支付的支付流水。
3、核心流程：
① createPayment：校验订单状态，生成支付流水，待支付；
② processPayment：事务内流转状态：待支付→支付中→支付成功；保存第三方交易号；更新订单为已支付；异常捕获置支付失败；
③ getPaymentDetail / getPaymentsByOrderCode：查询支付流水；
④ cancelPayment：仅作废支付流水，不修改订单。
4、技术设计亮点：
- @Transactional保证创建、支付处理、取消的数据库原子性；
- 状态机校验：只有待支付才能创建/处理/取消；
- 支付失败捕获异常自动回写支付状态，避免状态悬挂；
- 订单与支付单解耦，订单存主业务状态，支付单留存第三方对账信息。
5、风险点 & 潜在坑：
- processPayment**没有做幂等控制**；第三方重复回调会重复执行业务，造成订单状态重复更新；thirdPartyTransactionNo无幂等判断；
- 模拟支付中状态，真实项目需要对接真实第三方SDK；
- cancelPayment只作废支付流水，**不会更新订单状态、不会回滚库存**，调用方需要保证订单‑支付‑库存三方状态一致；
- 异常直接抛出RuntimeException，上层需要全局异常处理器捕获；
- processPayment内部catch异常之后，事务不会回滚，只会把支付单标记失败；需要注意数据库事务行为。
6、模块关联：依赖SeckillPaymentMapper、SeckillOrderTradeMapper；和SeckillOrderTradeService联动；IdGenerateHelper生成主键，OrderCodeUtil生成支付业务编号。
7、生产注意事项：
- processPayment必须增加幂等，thirdPartyTransactionNo建立唯一索引，回调先判断是否已处理；
- 真实环境支付中状态需要外部第三方回调驱动，不能本地模拟直接走到成功；
- 订单取消场景：cancelOrder之后必须调用cancelPayment作废对应支付流水；
- 支付回调接口做好防重、签名校验。
*/

