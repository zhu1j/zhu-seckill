package cn.net.zhu.seckill.business.entity.user;

import cn.net.zhu.seckill.business.entity.BaseEntity;
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

@Data
@EqualsAndHashCode(callSuper = true)
public class SeckillUserEntity extends BaseEntity {
    private String username;
    private String password;
    private String phone;
    private String email;
    private String nickname;
    private String avatar;
    private Integer status;
    private Date registerTime;
    private Date lastLoginTime;
}
