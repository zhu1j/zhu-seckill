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
    private Long orderId; //订单id
    private String orderCode; //订单编号
    private Long userId; //用户id
    private String userName; //用户账号名称
    private Long seckillProductId; //秒杀商品id
    private Long productId; //原商品id
    private Date orderTime; //下单时间
    private Integer timeoutMinutes; //超时时间(分钟)
    private Date messageCreateTime; //消息创建时间
}
