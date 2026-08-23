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
    private String username;
    private String token;
    private List<String> roles;
    private int expiresIn;
}
