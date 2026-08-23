package cn.net.zhu.seckill.business.entity.auth;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 *  用户认证实体类
 *
 * @author 一只朱
 * @date 2026-08-23 15:03
 *
 * "Run the code. Run the world."
 */

@Data
public class AuthUserEntity {
    private String uuid;
    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;
    private String code;
    private String phone;

}
