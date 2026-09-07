package cn.net.zhu.seckill.business.config;

import cn.net.zhu.seckill.business.config.properties.OrderApiProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 业务配置类
 *
 * @author 一只朱
 * @date 2026-09-07 15:12
 *
 * "Run the code. Run the world."
 */

@Data
@Component
@Slf4j
@ConfigurationProperties(prefix = "seckill")
public class BusinessConfig {

    /**
     * 订单API接口配置
     */
    private OrderApiProperties orderApiProperties = new OrderApiProperties();

}

