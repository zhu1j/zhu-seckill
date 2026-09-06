package cn.net.zhu.seckill.business.entity.seckill;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 用户秒杀商品查询实体
 * 查询条件DTO；用于查询用户秒杀记录、限购校验等查询入参；
 *
 * @author 一只朱
 * @date 2026-08-23 15:17
 *
 * "Run the code. Run the world."
 */
@Data
public class UserSeckillProductQueryEntity {

    /**
     * 秒杀商品ID，必填，JSR‑303参数校验不允许为空
     */
    @ApiModelProperty("秒杀商品ID")
    @NotNull(message = "seckillProductId不能为空")
    private Long seckillProductId;

    /**
     * 用户名称
     */
    private String userName;
}

/*
====================业务总结====================
1、模块职责：查询条件DTO；封装查询参数，用于查询用户的秒杀下单记录、做用户限购校验逻辑。

2、调用链路：
Controller接收请求 → 封装UserSeckillProductQueryEntity → 传入UserSeckillProductService，查询用户是否已经购买过该秒杀商品，实现限购。

3、字段说明：
‑ seckillProductId：秒杀商品主键，@NotNull校验，接口必须传入；
‑ userName：用户账号名称，作为查询条件。

4、技术设计亮点：
‑ 使用JSR‑303 @NotNull做参数校验，配合@Valid即可自动校验入参，不用手写if判空；
‑ swagger注解完善接口文档；
‑ DTO职责单一，只承载查询条件。

5、风险点 & 潜在坑：
‑ userName没有非空校验；业务上查询用户秒杀记录，用户标识不能为空；如果前端不传userName，会导致查询条件失效；
‑ 只用userName做用户标识，业务最好使用userId；用户名可修改，会出现数据错乱；
‑ 没有分页字段；如果后续需要批量查询历史记录，该DTO不支持分页。
‑ @NotNull仅校验不为null；如果前端传0，seckillProductId=0可以通过校验，会查出来无效数据。

6、模块关联：用于Controller → UserSeckillProductService，限购判断。

7、生产注意事项：
‑ 优先使用userId(Long)代替userName作为用户唯一标识；用户名可变更，不适合做业务查询条件；
‑ 如果userName必须使用，增加@NotNull校验；
‑ seckillProductId除了非空，建议增加自定义校验，校验ID大于0；
‑ 如果用于列表查询，补充pageNum、pageSize分页参数；
‑ Controller层方法参数前必须加 @Valid，否则 @NotNull 校验不会生效。
*/
