package cn.net.zhu.seckill.business.entity.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
@ApiModel("用户注册实体")
@Data
public class UserRegisterEntity {

    /**
     * 唯一标识（验证码UUID）
     */
    @NotBlank(message = "唯一标识不能为空")
    @ApiModelProperty("唯一标识")
    private String uuid;

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @ApiModelProperty("用户名")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @ApiModelProperty("密码")
    private String password;

    /**
     * 确认密码
     */
    @NotBlank(message = "确认密码不能为空")
    @ApiModelProperty("确认密码")
    private String confirmPassword;

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @ApiModelProperty("手机号")
    private String phone;

    /**
     * 手机验证码
     */
    @NotBlank(message = "手机验证码不能为空")
    @ApiModelProperty("手机验证码")
    private String phoneCode;

    /**
     * 图形验证码
     */
    @NotBlank(message = "验证码不能为空")
    @ApiModelProperty("验证码")
    private String code;

    /**
     * 邮箱（可选）
     */
    @ApiModelProperty("邮箱")
    private String email;

    /**
     * 昵称（可选）
     */
    @ApiModelProperty("昵称")
    private String nickname;
}
