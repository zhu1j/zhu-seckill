package cn.net.zhu.seckill.api.config;

import cn.net.zhu.seckill.business.entity.auth.CaptchaEntity;
import cn.net.zhu.seckill.business.entity.product.EsSeckillProductConditionEntity;
import cn.net.zhu.seckill.business.service.ProductService;
import cn.net.zhu.seckill.business.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * API接口预热配置
 * 在应用启动后自动调用关键接口进行预热，减少首次访问延迟
 *
 * @author 一只朱
 * @date 2026-09-07 16:26
 *
 * "Run the code. Run the world."
 */
 
@Slf4j
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class ApiWarmupConfig implements ApplicationRunner {

    private final ProductService productService;
    private final UserService userService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("开始执行API接口预热...");
        warmupApis();
    }

    @Async
    public void warmupApis() {
        try {
            // 延迟5秒后开始预热，确保应用完全启动
            Thread.sleep(5000);
            
            log.info("开始预热关键API接口...");
            
            // 1. 预热验证码接口
            warmupCaptchaApi();
            
            // 2. 预热商品搜索接口
            warmupProductSearchApi();
            
            // 3. 预热商品详情接口
            warmupProductDetailApi();
            
            log.info("API接口预热完成");
            
        } catch (Exception e) {
            log.error("API接口预热失败", e);
        }
    }

    /**
     * 预热验证码接口
     */
    private void warmupCaptchaApi() {
        try {
            log.info("预热验证码接口...");
            CaptchaEntity captcha = userService.getCode();
            if (captcha != null) {
                log.info("验证码接口预热成功");
            }
        } catch (Exception e) {
            log.warn("验证码接口预热失败: {}", e.getMessage());
        }
    }

    /**
     * 预热商品搜索接口
     */
    private void warmupProductSearchApi() {
        try {
            log.info("预热商品搜索接口...");
            EsSeckillProductConditionEntity condition = new EsSeckillProductConditionEntity();
            condition.setPageNo(1);
            condition.setPageSize(10);
            
            productService.searchProductList(condition);
            log.info("商品搜索接口预热成功");
        } catch (Exception e) {
            log.warn("商品搜索接口预热失败: {}", e.getMessage());
        }
    }

    /**
     * 预热商品详情接口
     */
    private void warmupProductDetailApi() {
        try {
            log.info("预热商品详情接口...");
            // 使用一个常见的商品ID进行预热，如果不存在也不会报错
            Long testProductId = 1875105846680662016L;
            productService.getProductDetail(testProductId);
            log.info("商品详情接口预热成功");
        } catch (Exception e) {
            log.warn("商品详情接口预热失败: {}", e.getMessage());
        }
    }
}