package cn.net.zhu.seckill.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 第一次提交，搭建框架，这是一个测试API 类
 */
@RestController
@RequestMapping("")
public class TestController {
    @GetMapping("/test")
    public String test() {
        return "success";
    }
}
