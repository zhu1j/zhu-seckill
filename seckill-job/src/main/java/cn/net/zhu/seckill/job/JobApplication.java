package cn.net.zhu.seckill.job;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 秒杀系统 job服务启动类
 *
 * @author 一只朱
 * @date 2026-08-10 04:31
 *
 * "Run the code. Run the world."
 */
@ComponentScan(basePackages = "cn.net.zhu.seckill")
@SpringBootApplication
//SpringBoot 包扫描规则
//默认：启动类只会扫描【启动类所在包以及它下面所有子包】，不是扫描整个模块。
public class JobApplication {
    public static void main(String[] args){
        SpringApplication.run(JobApplication.class,args);
    }
}
