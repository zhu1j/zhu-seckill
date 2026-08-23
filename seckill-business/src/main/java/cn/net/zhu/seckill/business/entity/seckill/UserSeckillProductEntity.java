package cn.net.zhu.seckill.business.entity.seckill;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 *  用户秒杀商品实体类（秒杀请求入参）
 *
 * @author 一只朱
 * @date 2026-08-23 15:17
 *
 * "Run the code. Run the world."
 */

@Data
public class UserSeckillProductEntity {
    @NotNull(message = "秒杀商品ID不能为空")
    private Long seckillProductId;
    @NotNull(message = "验证码标识不能为空")
    private String uuid;
    @NotNull(message = "验证码不能为空")
    private String code;
    private String userName;
}
