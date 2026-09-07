package cn.net.zhu.seckill.business.entity.seckill;

import cn.net.zhu.seckill.business.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

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

@ApiModel("秒杀退款实体")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SeckillRefundEntity extends BaseEntity {

    /**
     * 退货流水号
     */
    @ApiModelProperty("退货流水号")
    private String refundNo;

    /**
     * 订单ID
     */
    @ApiModelProperty("订单ID")
    private Long orderId;

    /**
     * 订单编码
     */
    @ApiModelProperty("订单编码")
    private String orderCode;

    /**
     * 支付ID
     */
    @ApiModelProperty("支付ID")
    private Long paymentId;

    /**
     * 支付流水号
     */
    @ApiModelProperty("支付流水号")
    private String paymentNo;

    /**
     * 用户ID
     */
    @ApiModelProperty("用户ID")
    private Long userId;

    /**
     * 用户名称
     */
    @ApiModelProperty("用户名称")
    private String userName;

    /**
     * 退货金额
     */
    @ApiModelProperty("退货金额")
    private BigDecimal refundAmount;

    /**
     * 退货原因
     */
    @ApiModelProperty("退货原因")
    private String refundReason;

    /**
     * 退货类型 1:仅退款 2:退货退款
     */
    @ApiModelProperty("退货类型 1:仅退款 2:退货退款")
    private Integer refundType;

    /**
     * 退货状态 1:申请中 2:审核通过 3:审核拒绝 4:退款中 5:退款成功 6:退款失败 7:已完成
     */
    @ApiModelProperty("退货状态 1:申请中 2:审核通过 3:审核拒绝 4:退款中 5:退款成功 6:退款失败 7:已完成")
    private Integer refundStatus;

    /**
     * 申请时间
     */
    @ApiModelProperty("申请时间")
    private Date applyTime;

    /**
     * 审核时间
     */
    @ApiModelProperty("审核时间")
    private Date auditTime;

    /**
     * 审核人ID
     */
    @ApiModelProperty("审核人ID")
    private Long auditUserId;

    /**
     * 审核人名称
     */
    @ApiModelProperty("审核人名称")
    private String auditUserName;

    /**
     * 审核备注
     */
    @ApiModelProperty("审核备注")
    private String auditRemark;

    /**
     * 退款时间
     */
    @ApiModelProperty("退款时间")
    private Date refundTime;

    /**
     * 第三方退款流水号
     */
    @ApiModelProperty("第三方退款流水号")
    private String thirdPartyRefundNo;

    /**
     * 备注
     */
    @ApiModelProperty("备注")
    private String remark;
}