package cn.net.zhu.seckill.business.entity.seckill;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import javax.validation.constraints.NotEmpty;
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
    /**
     * 秒杀商品ID
     */
    @ApiModelProperty("秒杀商品ID")
    @NotNull(message = "seckillProductId不能为空")
    private Long seckillProductId;

    /**
     * 验证码uuid
     */
    @ApiModelProperty("验证码uuid")
    @NotEmpty(message = "uuid不能为空")
    private String uuid;

    /**
     * 验证码code
     */
    @ApiModelProperty("验证码code")
    @NotEmpty(message = "code不能为空")
    private String code;

    /**
     * 用户名称
     */
    private String userName;
}
