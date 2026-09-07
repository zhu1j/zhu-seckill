package cn.net.zhu.seckill.business.config.properties;

import lombok.Data;

/**
 * 订单API接口配置
 *
 * @author 一只朱
 * @date 2026-09-07 15:12
 *
 * "Run the code. Run the world."
 */

@Data
public class OrderApiProperties {

    /**
     * host地址
     */
    private String host;

    /**
     * 创建订单url
     */
    private String createOrderUrl;

    /**
     * 查询订单状态
     */
    private String queryOrderStatus;

    /**
     * 密钥
     */
    private String secretKey;
}
