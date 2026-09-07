package cn.net.zhu.seckill.business.entity.auth;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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

@ApiModel("权限用户实体")
@Data
public class AuthUserEntity {

    /**
     * 唯一标识
     */
    @NotBlank(message = "唯一标识不能为空")
    @ApiModelProperty("唯一标识")
    private String uuid;

    /**
     * 用户名称
     */
    @NotBlank(message = "用户名称不能为空")
    @ApiModelProperty("用户名称")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @ApiModelProperty("密码")
    @NotBlank
    private String password;

    /**
     * 验证码
     */
    @NotBlank(message = "验证码不能为空")
    @ApiModelProperty("验证码")
    private String code;

    /**
     * 手机号
     */
    @ApiModelProperty("手机号")
    private String phone;
}
