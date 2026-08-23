package cn.net.zhu.seckill.business.entity.seckill;

import cn.net.zhu.seckill.business.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

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

@Data
@EqualsAndHashCode(callSuper = true)
public class SeckillPaymentEntity extends BaseEntity {
    private String paymentNo; //支付流水号
    private Long orderId; //订单id
    private String orderCode; //订单编号
    private Long userId; //用户id
    private String userName; //用户账号名称
    private BigDecimal paymentAmount; //支付金额
    private Integer paymentMethod; //支付方式编码
    private Integer paymentStatus; //支付状态编码
    private String thirdPartyTransactionNo; //第三方支付交易号
    private Date paymentTime; //支付完成时间
    private BigDecimal refundAmount; //退款金额
    private Date refundTime; //退款完成时间
    private String remark; //支付备注信息
}
