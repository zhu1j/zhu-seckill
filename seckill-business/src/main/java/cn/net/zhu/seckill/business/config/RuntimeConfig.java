package cn.net.zhu.seckill.business.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 *  运行时配置类（MyBatis Mapper扫描）
 *
 * @author 一只朱
 * @date 2026-08-23 02:44
 *
 * "Run the code. Run the world."
 */

@MapperScan(basePackages = "cn.net.zhu.seckill.business.mapper")
@Configuration
public class RuntimeConfig {
}
