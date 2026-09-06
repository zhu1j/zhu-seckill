package cn.net.zhu.seckill.business.service;

import cn.net.zhu.seckill.business.entity.seckill.SeckillRefundEntity;
import java.util.List;

/**
 * 秒杀退款服务接口
 * 管理订单退款全流程：用户申请退款、管理员审核退款、执行退款打款、查询退款记录、取消退款申请；
 * 一个订单可有多条退款记录；退款需要依赖已支付订单，审核通过后执行真实退款逻辑。
 *
 * @author 一只朱
 * @date 2026-09-06 14:57
 *
 * "Run the code. Run the world."
 */

public interface SeckillRefundService {

    /**
     * 用户发起退款申请
     * @param orderCode 业务订单编号
     * @param refundReason 退款原因描述
     * @param refundType 退款类型：全额退款 / 部分退款
     * @return SeckillRefundEntity 创建的退款申请记录
     */
    SeckillRefundEntity applyRefund(String orderCode, String refundReason, Integer refundType);

    /**
     * 管理员审核退款申请
     * @param refundId 退款记录主键ID
     * @param auditResult 审核结果：同意 / 驳回
     * @param auditRemark 审核备注
     * @param auditUserId 审核人用户ID
     * @param auditUserName 审核人用户名
     * @return boolean true审核操作成功；false审核失败（记录不存在、状态不允许审核）
     */
    boolean auditRefund(Long refundId, Integer auditResult, String auditRemark,
                        Long auditUserId, String auditUserName);

    /**
     * 执行退款，调用第三方退款接口完成资金退回
     * 审核通过之后调用；更新退款记录为退款完成，同步更新订单状态，联动处理库存
     * @param refundId 退款记录主键ID
     * @return boolean true退款处理成功；false失败（状态非法、第三方异常）
     */
    boolean processRefund(Long refundId);

    /**
     * 根据退款主键ID查询退款单详情
     * @param refundId 退款记录主键
     * @return SeckillRefundEntity 退款实体，查不到返回null
     */
    SeckillRefundEntity getRefundDetail(Long refundId);

    /**
     * 根据业务订单号查询该订单全部退款记录
     * @param orderCode 业务订单编码
     * @return List<SeckillRefundEntity> 订单下所有退款流水集合
     */
    List<SeckillRefundEntity> getRefundsByOrderCode(String orderCode);

    /**
     * 根据用户ID查询该用户所有退款记录
     * @param userId 用户ID
     * @return List<SeckillRefundEntity> 用户退款流水集合
     */
    List<SeckillRefundEntity> getRefundsByUserId(Long userId);

    /**
     * 取消退款申请
     * 仅待审核状态退款可以取消；作废退款申请，不会变更订单状态
     * @param refundId 退款记录主键ID
     * @return boolean true取消成功；false状态不允许或记录不存在
     */
    boolean cancelRefund(Long refundId);
}

/*
====================业务总结====================
1、模块职责：退款领域服务接口；覆盖退款完整业务链路：申请→审核→执行退款→查询→取消申请；订单与退款一对多，支持多次退款。
2、调用链路：
用户端 → applyRefund() 提交退款申请；
管理后台 → auditRefund() 审核退款；审核通过后调用 processRefund() 执行第三方退款；
业务查询：getRefundDetail / getRefundsByOrderCode / getRefundsByUserId；
用户取消退款申请 → cancelRefund()。
3、核心流程：
① applyRefund：基于已支付订单生成待审核退款记录；
② auditRefund：管理员审核，同意或驳回退款申请；
③ processRefund：审核通过后执行退款，对接第三方退款接口，更新退款、订单状态，回滚库存；
④ 查询接口：按退款ID、订单号、用户ID查询退款流水；
⑤ cancelRefund：仅作废待审核退款申请，不修改订单。
4、技术设计亮点：
- 订单与退款一对多，支持部分退款、多次退款场景；
- 业务流程拆分为申请‑审核‑执行三阶段，适配后台人工审核流程；
- 审核信息完整留存：审核人ID、用户名、审核备注，便于追溯。
5、风险点 & 潜在坑：
- processRefund 需要幂等，防止第三方重复回调造成重复退款；
- 退款必须校验订单已支付，未支付订单不能发起退款；
- 退款成功后，需要同步完成：退款流水、订单状态、支付流水、库存、Redis缓存多端状态一致性；
- cancelRefund只能作废待审核申请，已审核/已退款记录不可取消。
6、模块关联：关联 SeckillOrderTradeService、SeckillPaymentService；退款依赖已支付订单与支付流水；processRefund对接第三方退款SDK。
7、生产注意事项：
- 退款、审核接口增加权限控制：普通用户不能调用auditRefund；
- 第三方退款调用做好异常捕获、重试、幂等；
- 退款记录、第三方退款交易号建立索引，用于对账。
*/

