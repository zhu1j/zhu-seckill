package cn.net.zhu.seckill.business.entity.seckill;

import cn.net.zhu.seckill.business.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

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

@Data
@EqualsAndHashCode(callSuper = true)
public class SeckillOrderTradeEntity extends BaseEntity {
    private String cover; //商品封面图地址
    private Long tradeId; //交易id
    private String code; //订单编号
    private Long userId; //用户id
    private String userName; //用户账号名称
    private Date orderTime; //下单时间
    private Integer orderStatus; //订单状态编码
    private String orderStatusString; //订单状态描述
    private Integer payStatus; //支付状态编码
    private BigDecimal totalAmount; //订单总金额
    private BigDecimal paymentAmount; //实际支付金额
    private Long seckillProductId; //秒杀商品id
    private Long productId; //原商品id
    private String productName; //商品名称
    private String model; //商品型号
    private BigDecimal price; //秒杀成交单价
    private BigDecimal costPrice; //商品成本价
    private Integer quantity; //购买数量
    private String remark; //订单备注
}
