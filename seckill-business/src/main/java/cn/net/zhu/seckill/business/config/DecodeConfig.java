package cn.net.zhu.seckill.business.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 *  解码配置类（BCrypt密码加密）
 *
 * @author 一只朱
 * @date 2026-08-23 02:45
 *
 * "Run the code. Run the world."
 */
 @Configuration
public class DecodeConfig {
     @Bean
     public PasswordEncoder passwordEncoder() {
         return new BCryptPasswordEncoder();
     }
}
