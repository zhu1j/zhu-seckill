package cn.net.zhu.seckill.business.entity.seckill;

import cn.net.zhu.seckill.business.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 *  秒杀退款实体类
 *
 * @author 一只朱
 * @date 2026-08-23 15:15
 *
 * "Run the code. Run the world."
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class SeckillRefundEntity extends BaseEntity {
    private String refundNo; //退款流水号
    private Long orderId; //订单id
    private String orderCode; //订单编号
    private Long paymentId; //支付记录id
    private String paymentNo; //支付流水号
    private Long userId; //用户id
    private String userName; //用户账号名称
    private BigDecimal refundAmount; //退款金额
    private String refundReason; //退款原因
    private Integer refundType; //退款类型编码
    private Integer refundStatus; //退款状态编码
    private Date applyTime; //退款申请时间
    private Date auditTime; //退款审核时间
    private Long auditUserId; //审核人id
    private String auditUserName; //审核人账号名称
    private String auditRemark; //审核备注
    private Date refundTime; //退款实际到账时间
    private String thirdPartyRefundNo; //第三方退款流水号
    private String remark; //扩展备注信息
}
