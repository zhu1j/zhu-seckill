package cn.net.zhu.seckill.business.entity.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *  验证码实体类
 *
 * @author 一只朱
 * @date 2026-08-23 15:05
 *
 * "Run the code. Run the world."
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaEntity {
    private String uuid;
    private String img;
}
