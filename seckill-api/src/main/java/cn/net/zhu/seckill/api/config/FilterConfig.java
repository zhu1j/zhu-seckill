package cn.net.zhu.seckill.api.config;

import cn.net.zhu.seckill.business.filter.JwtTokenFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *  过滤配置类
 *
 * @author 一只朱
 * @date 2026-08-23 02:59
 *
 * "Run the code. Run the world."
 */

@Configuration
public class FilterConfig {
    @Bean
    public FilterRegistrationBean<JwtTokenFilter> registrationBean() {
        FilterRegistrationBean<JwtTokenFilter> bean = new FilterRegistrationBean<JwtTokenFilter>(new JwtTokenFilter());
        bean.addUrlPatterns("/");
        return bean;
    }
}
