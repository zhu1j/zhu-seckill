package cn.net.zhu.seckill.business.entity.seckill;

import cn.net.zhu.seckill.business.entity.RequestPageEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 *  秒杀订单查询条件实体 实体类
 *
 * @author 一只朱
 * @date 2026-08-23 15:12
 *
 * "Run the code. Run the world."
 */

@ApiModel("秒杀订单查询条件实体")
@Data
public class SeckillOrderTradeConditionEntity extends RequestPageEntity {


    /**
     *  ID
     */
    @ApiModelProperty("ID")
    private Long id;

    /**
     *  订单ID
     */
    @ApiModelProperty("订单ID")
    private Long tradeId;

    /**
     *  订单编码
     */
    @ApiModelProperty("订单编码")
    private String code;

    /**
     *  用户ID
     */
    @ApiModelProperty("用户ID")
    private Long userId;

    /**
     *  用户名称
     */
    @ApiModelProperty("用户名称")
    private String userName;

    /**
     *  下单时间
     */
    @ApiModelProperty("下单时间")
    private Date orderTime;

    /**
     *  下单开始时间
     */
    @ApiModelProperty("下单开始时间")
    private Date orderTimeStart;

    /**
     *  下单结束时间
     */
    @ApiModelProperty("下单结束时间")
    private Date orderTimeEnd;

    /**
     *  订单状态 1:下单 2:支付 3：完成 4：取消
     */
    @ApiModelProperty("订单状态 1:下单 2:支付 3：完成 4：取消")
    private Integer orderStatus;

    /**
     *  支付状态 1:待支付 2:已支付 3：退款
     */
    @ApiModelProperty("支付状态 1:待支付 2:已支付 3：退款")
    private Integer payStatus;

    /**
     *  总金额
     */
    @ApiModelProperty("总金额")
    private BigDecimal totalAmount;

    /**
     *  付款金额
     */
    @ApiModelProperty("付款金额")
    private BigDecimal paymentAmount;

    /**
     *  秒杀商品ID
     */
    @ApiModelProperty("秒杀商品ID")
    private Long seckillProductId;

    /**
     *  商品ID
     */
    @ApiModelProperty("商品ID")
    private Long productId;

    /**
     *  商品名称
     */
    @ApiModelProperty("商品名称")
    private String productName;

    /**
     *  商品规格
     */
    @ApiModelProperty("商品规格")
    private String model;

    /**
     *  秒杀价
     */
    @ApiModelProperty("秒杀价")
    private BigDecimal price;

    /**
     *  原价
     */
    @ApiModelProperty("原价")
    private BigDecimal costPrice;

    /**
     *  数量
     */
    @ApiModelProperty("数量")
    private Integer quantity;

    /**
     *  备注
     */
    @ApiModelProperty("备注")
    private String remark;

    /**
     *  创建人ID
     */
    @ApiModelProperty("创建人ID")
    private Long createUserId;

    /**
     *  创建人名称
     */
    @ApiModelProperty("创建人名称")
    private String createUserName;

    /**
     *  创建日期
     */
    @ApiModelProperty("创建日期")
    private Date createTime;

    /**
     *  修改人ID
     */
    @ApiModelProperty("修改人ID")
    private Long updateUserId;

    /**
     *  修改人名称
     */
    @ApiModelProperty("修改人名称")
    private String updateUserName;

    /**
     *  修改时间
     */
    @ApiModelProperty("修改时间")
    private Date updateTime;

    /**
     *  是否删除 1：已删除 0：未删除
     */
    @ApiModelProperty("是否删除 1：已删除 0：未删除")
    private Integer isDel;
}

