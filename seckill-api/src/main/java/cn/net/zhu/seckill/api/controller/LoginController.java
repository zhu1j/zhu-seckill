package cn.net.zhu.seckill.api.controller;

import cn.net.zhu.seckill.business.entity.auth.AuthUserEntity;
import cn.net.zhu.seckill.business.entity.auth.CaptchaEntity;
import cn.net.zhu.seckill.business.entity.auth.TokenEntity;
import cn.net.zhu.seckill.business.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import javax.validation.Valid;

/**
 * 登录控制器
 * (秒杀系统登录相关接口，提供页面跳转、获取验证码、登录接口)
 *
 * @author 一只朱
 * @date 2026-09-05 16:52
 *
 * "Run the code. Run the world."
 */

@Api(tags = "登录操作")
@RestController
@RequestMapping("")
@RequiredArgsConstructor
public class LoginController {

    /**
     * 用户业务服务
     */
    private final UserService userService;

    /**
     * 用户登录接口
     * @param authUserEntity 登录请求体：用户名、RSA加密密码、验证码uuid、验证码
     * @return TokenEntity 返回token信息，前端拿到token放入请求头Authorization后续鉴权使用
     */
    @ApiOperation("用户登录")
    @PostMapping("/doLogin")
    public TokenEntity doLogin(@Valid @RequestBody AuthUserEntity authUserEntity) {
        return userService.login(authUserEntity);
    }

    /**
     * 跳转登录页面
     * @return ModelAndView 视图名称login，跳转到登录页面模板
     */
    @ApiOperation("登录页面")
    @GetMapping("/login")
    public ModelAndView login() {
        return new ModelAndView("login");
    }

    /**
     * 获取算术验证码接口
     * @return CaptchaEntity 返回验证码uuid与base64图片，前端回传uuid用于校验验证码
     */
    @ApiOperation("获取验证码")
    @GetMapping("/code")
    public CaptchaEntity getCode() {
        return userService.getCode();
    }
}

/*
====================业务总结====================
1、登录层Controller，对外暴露三个能力：跳转登录页面、获取验证码、执行登录；
2、/doLogin POST接口接收JSON参数，@Valid做请求参数校验，调用UserService完成登录逻辑，返回JWT‑Token给前端；
3、前端拿到Token后，后续业务请求把Token放到Http请求头 Authorization，交给JwtTokenFilter过滤器做鉴权校验；
4、/code 接口获取验证码：返回uuid + base64图片；前端登录时把uuid和用户输入验证码一并提交给登录接口；
5、/login 返回视图，用于页面跳转；
6、Swagger注解用于接口文档生成；@RequiredArgsConstructor构造器注入UserService；
7、整体链路：前端获取验证码 → 页面输入账号密码验证码 → 调用doLogin拿到token → 后续请求携带token访问秒杀接口；
8、注意：本控制器接口都属于公开接口，不需要经过JwtTokenFilter鉴权。
*/

