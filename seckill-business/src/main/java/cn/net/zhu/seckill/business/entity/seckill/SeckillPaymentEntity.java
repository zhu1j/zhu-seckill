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
 *  秒杀付款实体类
 *
 * @author 一只朱
 * @date 2026-08-23 15:12
 *
 * "Run the code. Run the world."
 */
@ApiModel("秒杀支付实体")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SeckillPaymentEntity extends BaseEntity {

    /**
     * 支付流水号
     */
    @ApiModelProperty("支付流水号")
    private String paymentNo;

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
     * 支付金额
     */
    @ApiModelProperty("支付金额")
    private BigDecimal paymentAmount;

    /**
     * 支付方式 1:支付宝 2:微信 3:银行卡
     */
    @ApiModelProperty("支付方式 1:支付宝 2:微信 3:银行卡")
    private Integer paymentMethod;

    /**
     * 支付状态 1:待支付 2:支付中 3:支付成功 4:支付失败 5:已退款 6:部分退款
     */
    @ApiModelProperty("支付状态 1:待支付 2:支付中 3:支付成功 4:支付失败 5:已退款 6:部分退款")
    private Integer paymentStatus;

    /**
     * 第三方交易流水号
     */
    @ApiModelProperty("第三方交易流水号")
    private String thirdPartyTransactionNo;

    /**
     * 支付时间
     */
    @ApiModelProperty("支付时间")
    private Date paymentTime;

    /**
     * 退款金额
     */
    @ApiModelProperty("退款金额")
    private BigDecimal refundAmount;

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