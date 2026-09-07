package cn.net.zhu.seckill.business.entity.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 *  令牌实体类
 *
 * @author 一只朱
 * @date 2026-08-23 15:05
 *
 * "Run the code. Run the world."
 */

@Data
@AllArgsConstructor
public class TokenEntity {
    /**
     * 用户名称
     */
    private String username;

    /**
     * token
     */
    private String token;

    /**
     * 角色信息
     */
    private List<String> roles;

    /**
     * 过期时间
     */
    private int expiresIn;
}
