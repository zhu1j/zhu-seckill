package cn.net.zhu.seckill.business.service.impl;

import cn.net.zhu.seckill.business.entity.seckill.*;
import cn.net.zhu.seckill.business.helper.IdGenerateHelper;
import cn.net.zhu.seckill.business.mapper.seckill.*;
import cn.net.zhu.seckill.business.service.SeckillRefundService;
import cn.net.zhu.seckill.business.service.StockConsistencyService;
import cn.net.zhu.seckill.business.util.OrderCodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 秒杀退款服务实现类
 * 完整实现退款业务流程：用户申请退款、管理员审核、执行退款、取消退款申请；
 * 内置退款操作日志记录 recordRefundLog；审核通过后自动调用 processRefund 执行退款；
 * 模拟第三方退款接口，退款成功同步更新退款单、支付流水、订单状态。
 *
 * @author 一只朱
 * @date 2026-09-06 15:00
 *
 * "Run the code. Run the world."
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillRefundServiceImpl implements SeckillRefundService {

    /**
     * 退款主表Mapper
     */
    private final SeckillRefundMapper seckillRefundMapper;
    /**
     * 退款操作日志Mapper，记录每一步退款操作流水
     */
    private final SeckillRefundLogMapper seckillRefundLogMapper;
    /**
     * 订单Mapper，查询、更新订单状态
     */
    private final SeckillOrderTradeMapper seckillOrderTradeMapper;
    /**
     * 支付流水Mapper，退款时更新支付单状态为已退款
     */
    private final SeckillPaymentMapper seckillPaymentMapper;
    /**
     * 雪花ID生成工具，生成退款、退款日志主键
     */
    private final IdGenerateHelper idGenerateHelper;
    /**
     * 库存一致性服务，退款成功恢复DB+Redis库存
     */
    private final StockConsistencyService stockConsistencyService;

    /**
     * 用户发起退款申请
     * 校验订单必须存在且为已支付；校验不存在处理中的退款申请；生成退款记录，状态为申请中；
     * 同时写入退款操作日志；退款金额直接取订单实付金额；
     * @param orderCode 业务订单编号
     * @param refundReason 用户退款原因
     * @param refundType 退款类型：全额/部分退款
     * @return SeckillRefundEntity 新建退款记录
     * @throws RuntimeException 订单不存在、订单未支付、已有处理中退款申请抛出异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeckillRefundEntity applyRefund(String orderCode, String refundReason, Integer refundType) {
        // 查询订单，必须已支付才可申请退款
        SeckillOrderTradeEntity order = seckillOrderTradeMapper.findByCode(orderCode);
        if (Objects.isNull(order)) throw new RuntimeException("订单不存在");
        if (!Objects.equals(order.getPayStatus(), 2)) throw new RuntimeException("订单未支付，无法申请退货");

        // 校验该订单不能同时存在处理中的退款申请（申请中/审核通过）
//        SeckillRefundEntity existing = seckillRefundMapper.findByOrderCode(orderCode);
//        if (existing != null && (existing.getRefundStatus() == 1 || existing.getRefundStatus() == 2)) {
//            throw new RuntimeException("该订单已有退货申请在处理中");
//        }
        // 遍历该订单所有退款记录，只要有一条在处理中就拦下（注意 anyMatch：空集合返回false放行）
        List<SeckillRefundEntity> existingList = seckillRefundMapper.findByOrderCode(orderCode);
        boolean processing = existingList.stream()
                .anyMatch(r -> Objects.equals(r.getRefundStatus(), 1) || Objects.equals(r.getRefundStatus(), 2));
        if (processing) {
            throw new RuntimeException("该订单已有退货申请在处理中");
        }

        // 组装退款实体
        SeckillRefundEntity refund = new SeckillRefundEntity();
        refund.setId(idGenerateHelper.nextId());
        refund.setRefundNo(OrderCodeUtil.generateOrderCode());
        refund.setOrderId(order.getId());
        refund.setOrderCode(orderCode);
        refund.setUserId(order.getUserId());
        refund.setUserName(order.getUserName());
        refund.setRefundAmount(order.getPaymentAmount());
        refund.setRefundReason(refundReason);
        refund.setRefundType(refundType);
        refund.setRefundStatus(1); // 申请中
        refund.setApplyTime(new Date());
        refund.setCreateTime(new Date());
        // 创建人信息，满足表NOT NULL约束
        refund.setCreateUserId(order.getUserId());
        refund.setCreateUserName(order.getUserName());

        // 关联该订单最近一笔支付流水，退款成功时联动更新支付状态为已退款
        List<SeckillPaymentEntity> payments = seckillPaymentMapper.findByOrderCode(orderCode);
        if (payments != null && !payments.isEmpty()) {
            refund.setPaymentId(payments.get(0).getId());
            refund.setPaymentNo(payments.get(0).getPaymentNo());
        }
        seckillRefundMapper.insert(refund);

        // 记录退款操作日志：用户提交退款申请
        recordRefundLog(refund.getId(), refund.getRefundNo(), 1, "申请退货",
                refund.getUserId(), refund.getUserName(), null, null);
        log.info("用户[{}]申请退货成功，订单号：{}", order.getUserName(), orderCode);
        return refund;
    }

    /**
     * 管理员审核退款申请
     * 仅【申请中(1)】状态允许审核；填写审核结果、审核人信息更新退款单；写入审核日志；
     * 如果审核结果为通过(2)，同步调用 processRefund() 执行退款流程；
     * @param refundId 退款记录主键ID
     * @param auditResult 审核结果：2通过，3拒绝
     * @param auditRemark 审核备注信息
     * @param auditUserId 审核管理员ID
     * @param auditUserName 审核管理员用户名
     * @return true 审核操作成功
     * @throws RuntimeException 退款记录不存在、状态非申请中抛出异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean auditRefund(Long refundId, Integer auditResult, String auditRemark,
                               Long auditUserId, String auditUserName) {
        SeckillRefundEntity refund = seckillRefundMapper.findById(refundId);
        if (Objects.isNull(refund)) throw new RuntimeException("退货记录不存在");
        if (!Objects.equals(refund.getRefundStatus(), 1)) throw new RuntimeException("退货申请状态不正确");

        // 更新退款单审核信息
        refund.setRefundStatus(auditResult); // 2:通过 3:拒绝
        refund.setAuditUserId(auditUserId);
        refund.setAuditUserName(auditUserName);
        refund.setAuditRemark(auditRemark);
        refund.setAuditTime(new Date());
        refund.setUpdateTime(new Date());
        seckillRefundMapper.update(refund);

        String desc = auditResult == 2 ? "审核通过" : "审核拒绝";
        recordRefundLog(refundId, refund.getRefundNo(), auditResult, desc,
                auditUserId, auditUserName, null, null);

        // 审核通过，自动执行退款
        if (auditResult == 2) {
            processRefund(refundId);
        }
        return true;
    }

    /**
     * 执行退款流程
     * 状态流转：审核通过 → 退款中 → 退款成功；模拟生成第三方退款流水号；
     * 更新退款单、支付流水状态为已退款；更新订单为已取消、已退款状态；写入退款成功日志；
     * 发生异常时将退款单置为退款失败(6)；
     * @param refundId 退款记录主键ID
     * @return true退款成功；false执行异常失败
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean processRefund(Long refundId) {
        try {
            SeckillRefundEntity refund = seckillRefundMapper.findById(refundId);
            if (Objects.isNull(refund)) throw new RuntimeException("退货记录不存在");

            // 更新为退款中状态
            refund.setRefundStatus(4);
            refund.setUpdateTime(new Date());
            seckillRefundMapper.updateRefundStatus(refund);

            // 模拟调用第三方退款接口，生成第三方退款单号
            String thirdPartyRefundNo = "RF" + System.currentTimeMillis();
            refund.setRefundStatus(5);
            refund.setRefundTime(new Date());
            refund.setThirdPartyRefundNo(thirdPartyRefundNo);
            refund.setUpdateTime(new Date());
            seckillRefundMapper.update(refund);

            // 更新对应支付流水状态为已退款
            if (Objects.nonNull(refund.getPaymentId())) {
                SeckillPaymentEntity payment = seckillPaymentMapper.findById(refund.getPaymentId());
                if (Objects.nonNull(payment)) {
                    payment.setPaymentStatus(5); // 已退款
                    payment.setRefundAmount(refund.getRefundAmount());
                    payment.setRefundTime(new Date());
                    payment.setUpdateTime(new Date());
                    seckillPaymentMapper.updatePaymentStatus(payment);
                }
            }

            // 更新订单：支付状态=已退款，订单状态=已取消
            SeckillOrderTradeEntity order = seckillOrderTradeMapper.findById(refund.getOrderId());
            if (Objects.nonNull(order)) {
                order.setPayStatus(3);   // 退款
                order.setOrderStatus(4); // 已取消
                seckillOrderTradeMapper.update(order);

                // 退款成功恢复库存（DB预扣库存+Redis缓存库存），防止库存泄漏
                stockConsistencyService.restoreStock(order.getSeckillProductId(), order.getQuantity());
            }

            recordRefundLog(refundId, refund.getRefundNo(), 5, "退款成功-" + thirdPartyRefundNo,
                    refund.getUserId(), refund.getUserName(), null, null);
            log.info("退款处理成功，退货ID：{}", refundId);
            return true;
        } catch (Exception e) {
            log.error("退款处理失败", e);
            // 异常标记退款失败
            SeckillRefundEntity refund = seckillRefundMapper.findById(refundId);
            if (Objects.nonNull(refund)) {
                refund.setRefundStatus(6);
                refund.setUpdateTime(new Date());
                seckillRefundMapper.updateRefundStatus(refund);
            }
            return false;
        }
    }

    /**
     * 根据退款主键查询退款详情
     * @param refundId 退款记录主键
     * @return SeckillRefundEntity 退款实体，查不到返回null
     */
    @Override
    public SeckillRefundEntity getRefundDetail(Long refundId) {
        return seckillRefundMapper.findById(refundId);
    }

    /**
     * 根据业务订单号查询该订单全部退款记录
     * @param orderCode 业务订单编号
     * @return List<SeckillRefundEntity> 退款流水集合
     */
    @Override
    public List<SeckillRefundEntity> getRefundsByOrderCode(String orderCode) {
        return seckillRefundMapper.findByOrderCode(orderCode);
    }

    /**
     * 根据用户ID查询用户全部退款记录
     * @param userId 用户ID
     * @return List<SeckillRefundEntity> 用户退款流水集合
     */
    @Override
    public List<SeckillRefundEntity> getRefundsByUserId(Long userId) {
        return seckillRefundMapper.findByUserId(userId);
    }

    /**
     * 用户主动取消退款申请
     * 仅申请中(1)状态允许取消；退款单状态置为拒绝，备注标记用户主动取消；写入操作日志；
     * @param refundId 退款记录主键ID
     * @return true取消成功
     * @throws RuntimeException 退款记录不存在、状态非申请中抛出异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelRefund(Long refundId) {
        SeckillRefundEntity refund = seckillRefundMapper.findById(refundId);
        if (Objects.isNull(refund)) throw new RuntimeException("退货记录不存在");
        if (!Objects.equals(refund.getRefundStatus(), 1)) throw new RuntimeException("退货申请状态不正确");

        refund.setRefundStatus(3); // 拒绝
        refund.setAuditRemark("用户主动取消");
        refund.setUpdateTime(new Date());
        seckillRefundMapper.update(refund);

        recordRefundLog(refundId, refund.getRefundNo(), 3, "用户取消",
                refund.getUserId(), refund.getUserName(), null, null);
        return true;
    }

    /**
     * 私有工具方法：记录退款操作日志
     * 退款每一步操作（申请、审核、退款成功、用户取消）都会落库日志，用于审计追溯
     * @param refundId 退款单ID
     * @param refundNo 退款业务单号
     * @param operationType 操作类型编码
     * @param operationDesc 操作描述文本
     * @param operatorId 操作人ID
     * @param operatorName 操作人名称
     * @param requestData 请求报文（可为null）
     * @param responseData 响应报文（可为null）
     */
    private void recordRefundLog(Long refundId, String refundNo, Integer operationType,
                                 String operationDesc, Long operatorId, String operatorName,
                                 String requestData, String responseData) {
        SeckillRefundLogEntity logEntity = new SeckillRefundLogEntity();
        logEntity.setId(idGenerateHelper.nextId());
        logEntity.setRefundId(refundId);
        logEntity.setRefundNo(refundNo);
        logEntity.setOperationType(operationType);
        logEntity.setOperationDesc(operationDesc);
        logEntity.setOperatorId(operatorId);
        logEntity.setOperatorName(operatorName);
        logEntity.setRequestData(requestData);
        logEntity.setResponseData(responseData);
        logEntity.setCreateTime(new Date());
        seckillRefundLogMapper.insert(logEntity);
    }
}

/*
====================业务总结====================
1、模块职责：退款业务完整实现；申请退款、审核退款、执行退款、取消退款申请；配套退款操作日志；审核通过自动触发退款执行；联动更新退款单、支付流水、订单状态。
2、调用链路：
用户端 → applyRefund() 创建退款申请；
管理后台 → auditRefund() 审核退款，审核通过内部调用 processRefund()；
processRefund()：模拟第三方退款，更新退款/支付/订单状态；
查询接口：按退款ID、订单号、用户ID查询退款记录；
用户端 → cancelRefund() 取消待审核退款申请。
3、核心流程：
① applyRefund：校验订单已支付、无处理中退款；生成退款记录+操作日志；
② auditRefund：仅申请中可审核；填写审核信息落库；审核通过自动执行退款；
③ processRefund：事务内状态流转；模拟第三方退款；更新退款单、支付单、订单；异常捕获标记退款失败；
④ cancelRefund：仅作废待审核退款申请；
⑤ recordRefundLog：私有方法，全链路记录退款操作审计日志。
4、技术设计亮点：
- @Transactional保证退款各步骤数据库原子性；
- 退款全流程落操作日志，便于问题排查与审计；
- 状态机强校验：每个操作只允许对应状态执行；
- 审核通过后内部自动调用processRefund，简化后台调用；
- 退款失败捕获异常，自动回写退款失败状态，避免状态悬挂。
5、风险点 & 潜在坑：
- auditRefund 事务内部直接调用 processRefund；processRefund异常会导致auditRefund整体事务回滚；退款失败后审核记录也会回滚丢失；
- processRefund缺少幂等；第三方重复回调会重复退款；thirdPartyRefundNo没有做唯一防重；
- **退款成功后代码没有回滚商品库存，没有恢复Redis库存，缓存DB不一致**；
- applyRefund校验：findByOrderCode只查一条退款记录，不支持真正的多次部分退款；
- refund.getPaymentId为空时，不会更新支付流水状态；缺少保护逻辑；
- 异常抛出RuntimeException，依赖全局异常处理器捕获；
- cancelRefund只是作废退款申请，订单、支付状态完全不变。
6、模块关联：依赖 SeckillRefundMapper、SeckillRefundLogMapper、SeckillOrderTradeMapper、SeckillPaymentMapper；IdGenerateHelper生成主键；OrderCodeUtil生成退款业务编号。
7、生产注意事项：
- auditRefund与processRefund不建议放在同一个事务；审核操作落库成功，退款异步任务执行；防止退款第三方异常把审核记录回滚；
- processRefund必须做幂等，thirdPartyRefundNo建立唯一索引；
- 退款成功后必须补充：商品DB库存回滚、Redis库存恢复；
- 真实环境不能模拟第三方退款，调用真实退款SDK，处理回调；
- 审核接口增加权限控制，普通用户禁止调用auditRefund；
- 部分退款场景需要改造applyRefund逻辑，支持同一订单多条并行退款。
*/

