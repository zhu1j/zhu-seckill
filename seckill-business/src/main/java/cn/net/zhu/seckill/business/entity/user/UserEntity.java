package cn.net.zhu.seckill.business.entity.user;

import cn.net.zhu.seckill.business.entity.auth.AuthUserEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *  用户实体类
 *
 * @author 一只朱
 * @date 2026-08-23 15:19
 *
 * "Run the code. Run the world."
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class UserEntity extends AuthUserEntity {
    private Long id;
}
