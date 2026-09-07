package cn.net.zhu.seckill.business.entity.user;

import cn.net.zhu.seckill.business.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 *  秒杀用户实体类
 *
 * @author 一只朱
 * @date 2026-08-23 15:19
 *
 * "Run the code. Run the world."
 */
@ApiModel("用户实体")
@Data
@EqualsAndHashCode(callSuper = true)
public class SeckillUserEntity extends BaseEntity {

    /**
     * 用户ID
     */
    @ApiModelProperty("用户ID")
    private Long id;

    /**
     * 用户名
     */
    @ApiModelProperty("用户名")
    private String username;

    /**
     * 密码（加密后）
     */
    @ApiModelProperty("密码")
    private String password;

    /**
     * 手机号
     */
    @ApiModelProperty("手机号")
    private String phone;

    /**
     * 邮箱
     */
    @ApiModelProperty("邮箱")
    private String email;

    /**
     * 昵称
     */
    @ApiModelProperty("昵称")
    private String nickname;

    /**
     * 头像URL
     */
    @ApiModelProperty("头像URL")
    private String avatar;

    /**
     * 用户状态 1:正常 2:禁用
     */
    @ApiModelProperty("用户状态")
    private Integer status;

    /**
     * 注册时间
     */
    @ApiModelProperty("注册时间")
    private Date registerTime;

    /**
     * 最后登录时间
     */
    @ApiModelProperty("最后登录时间")
    private Date lastLoginTime;
}
