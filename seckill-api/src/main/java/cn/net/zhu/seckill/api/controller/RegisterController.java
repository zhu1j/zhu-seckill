package cn.net.zhu.seckill.api.controller;

import cn.net.zhu.seckill.business.entity.user.UserRegisterEntity;
import cn.net.zhu.seckill.business.service.SeckillUserService;
import cn.net.zhu.seckill.business.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import javax.validation.Valid;

/**
 * 注册控制器
 * (秒杀系统用户注册相关接口，页面跳转、注册提交、用户名手机号查重、发送短信验证码)
 *
 * @author 一只朱
 * @date 2026-09-05 17:56
 *
 * "Run the code. Run the world."
 */

@Api(tags = "注册操作")
@RestController
@RequestMapping("")
@RequiredArgsConstructor
@Slf4j
public class RegisterController {

    /**
     * 秒杀用户业务服务，处理用户数据库注册、查询逻辑
     */
    private final SeckillUserService seckillUserService;

    /**
     * 用户工具服务，图形验证码校验、删除验证码
     */
    private final UserService userService;

    /**
     * 跳转注册页面
     * @return ModelAndView 返回register视图，跳转到注册页面模板
     */
    @ApiOperation("注册页面")
    @GetMapping("/register")
    public ModelAndView register() {
        return new ModelAndView("register");
    }

    /**
     * 执行用户注册
     * @param userRegisterEntity 注册请求参数：用户名、手机号、密码、图形验证码uuid、图形验证码、短信验证码
     * @return 注册成功提示字符串
     */
    @ApiOperation("用户注册")
    @PostMapping("/doRegister")
    public String doRegister(@Valid @RequestBody UserRegisterEntity userRegisterEntity) {
        // 校验算术图形验证码，防止恶意刷注册接口
        userService.checkCode(userRegisterEntity.getUuid(), userRegisterEntity.getCode());

        // 校验短信验证码
        if (!seckillUserService.validatePhoneCode(
                userRegisterEntity.getPhone(), userRegisterEntity.getPhoneCode())) {
            throw new RuntimeException("短信验证码错误");
        }

        // 调用业务层执行注册，写入数据库，同时将用户信息缓存进Redis供登录使用
        boolean success = seckillUserService.register(userRegisterEntity);

        // 注册完成，销毁已使用的图形验证码，禁止重复使用
        userService.deleteCode(userRegisterEntity.getUuid());

        if (!success) throw new RuntimeException("注册失败");
        return "注册成功";
    }

    /**
     * 检查用户名是否可用
     * @param username 待检测用户名
     * @return true：用户名不存在可以注册；false：用户名已存在
     */
    @ApiOperation("检查用户名是否存在")
    @GetMapping("/checkUsername")
    public Boolean checkUsername(@RequestParam String username) {
        return seckillUserService.findByUsername(username) == null;
    }

    /**
     * 检查手机号是否已注册
     * @param phone 待检测手机号
     * @return true：手机号未注册可以使用；false：手机号已存在
     */
    @ApiOperation("检查手机号是否已注册")
    @GetMapping("/checkPhone")
    public Boolean checkPhone(@RequestParam String phone) {
        return seckillUserService.findByPhone(phone) == null;
    }

    /**
     * 发送短信验证码（模拟实现）
     * @param phone 目标手机号
     * @return 发送成功提示
     */
    @ApiOperation("发送短信验证码")
    @PostMapping("/sendPhoneCode")
    public String sendPhoneCode(@RequestParam String phone) {
        // 模拟短信：取手机号后四位作为验证码，打印日志，不调用真实短信服务商
        String mockCode = phone.substring(phone.length() - 4);
        log.info("向手机号 {} 发送验证码：{}", phone, mockCode);
        return "验证码已发送";
    }
}

/*
====================业务总结====================
1、注册模块Controller，全部接口属于公开接口，不需要JWT鉴权；
2、接口清单：跳转注册页面、提交注册、用户名查重、手机号查重、发送模拟短信验证码；
3、注册双重验证码校验：图形算术验证码 + 短信验证码，用来防恶意批量注册；
4、注册流程：前端先校验用户名/手机号是否占用 → 获取图形验证码 → 获取短信验证码 → 提交doRegister；
5、注册成功后销毁图形验证码，避免验证码重复复用；业务层完成DB写入，同时把用户数据写入Redis，供登录接口直接读取，减轻DB压力；
6、短信验证码为模拟实现：验证码等于手机号后四位，日志打印，没有对接真实短信通道；
7、checkUsername/checkPhone返回true代表“不存在，可以注册”，前端据此做表单校验提示；
8、异常直接抛出RuntimeException，交给全局异常处理器统一处理返回错误信息；
9、和登录模块配合：注册完成之后，用户就可以直接走登录接口完成登录获取JWT令牌。
*/

