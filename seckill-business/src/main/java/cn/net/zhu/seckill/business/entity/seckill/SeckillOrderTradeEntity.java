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
 *  秒杀订单交易 实体类
 *
 * @author 一只朱
 * @date 2026-08-23 15:11
 *
 * "Run the code. Run the world."
 */
@ApiModel("秒杀订单实体")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SeckillOrderTradeEntity extends BaseEntity {

    /**
     * 图片
     */
    private String cover;


    /**
     * 订单ID
     */
    @ApiModelProperty("订单ID")
    private Long tradeId;

    /**
     * 订单编码
     */
    @ApiModelProperty("订单编码")
    private String code;

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
     * 下单时间
     */
    @ApiModelProperty("下单时间")
    private Date orderTime;

    /**
     * 订单状态 1:下单 2:支付 3：完成 4：取消
     */
    @ApiModelProperty("订单状态 1:下单 2:支付 3：完成 4：取消")
    private Integer orderStatus;

    /**
     * 订单状态字符串
     */
    private String orderStatusString;

    /**
     * 支付状态 1:待支付 2:已支付 3：退款
     */
    @ApiModelProperty("支付状态 1:待支付 2:已支付 3：退款")
    private Integer payStatus;

    /**
     * 总金额
     */
    @ApiModelProperty("总金额")
    private BigDecimal totalAmount;

    /**
     * 付款金额
     */
    @ApiModelProperty("付款金额")
    private BigDecimal paymentAmount;

    /**
     * 秒杀商品ID
     */
    @ApiModelProperty("秒杀商品ID")
    private Long seckillProductId;

    /**
     * 商品ID
     */
    @ApiModelProperty("商品ID")
    private Long productId;

    /**
     * 商品名称
     */
    @ApiModelProperty("商品名称")
    private String productName;

    /**
     * 商品规格
     */
    @ApiModelProperty("商品规格")
    private String model;

    /**
     * 秒杀价
     */
    @ApiModelProperty("秒杀价")
    private BigDecimal price;

    /**
     * 原价
     */
    @ApiModelProperty("原价")
    private BigDecimal costPrice;

    /**
     * 数量
     */
    @ApiModelProperty("数量")
    private Integer quantity;

    /**
     * 备注
     */
    @ApiModelProperty("备注")
    private String remark;
}
