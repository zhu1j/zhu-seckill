package cn.net.zhu.seckill.business.entity.user;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 *  用户注册实体类
 *
 * @author 一只朱
 * @date 2026-08-23 15:20
 *
 * "Run the code. Run the world."
 */

@Data
public class UserRegisterEntity {
    private String uuid;
    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$")
    private String phone;
    @NotBlank(message = "验证码不能为空")
    private String phoneCode;
    @NotBlank(message = "图形验证码不能为空")
    private String code;
    private String email;
    private String nickname;
}
