package cn.net.zhu.seckill.business.entity.mq;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 *  订单超时消息实体类（MQ延迟消息，处理秒杀订单超时未支付关闭逻辑）
 *
 * @author 一只朱
 * @date 2026-08-23 15:06
 *
 * "Run the code. Run the world."
 */

@Data
public class OrderTimeoutMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单编码
     */
    private String orderCode;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 秒杀商品ID
     */
    private Long seckillProductId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 订单创建时间
     */
    private Date orderTime;

    /**
     * 超时时间（分钟）
     */
    private Integer timeoutMinutes;

    /**
     * 消息创建时间
     */
    private Date messageCreateTime;
}
