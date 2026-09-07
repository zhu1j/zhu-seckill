package cn.net.zhu.seckill.business.entity.seckill;

import cn.net.zhu.seckill.business.entity.SignEntity;
import lombok.Data;
import java.util.Date;
import java.math.BigDecimal;

/**
 *  用户秒杀商品交易实体类
 *
 * @author 一只朱
 * @date 2026-08-23 15:18
 *
 * "Run the code. Run the world."
 */

@Data
public class UserSeckillProductTradeEntity extends SignEntity {

    /**
     * 订单ID
     */
    private Long tradeId;

    /**
     * 订单code
     */
    private String code;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 总金额
     */
    private BigDecimal totalAmount;

    /**
     * 付款金额
     */
    private BigDecimal paymentAmount;

    /**
     * 秒杀商品ID
     */
    private Long seckillProductId;

    /**
     * 原始商品ID
     */
    private Long productId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品规格
     */
    private String model;

    /**
     * 秒杀价
     */
    private BigDecimal price;

    /**
     * 原价
     */
    private BigDecimal costPrice;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 下单时间
     */
    private Date orderTime;
}

