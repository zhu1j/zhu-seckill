package cn.net.zhu.seckill.business.entity.seckill;

import cn.net.zhu.seckill.business.entity.BaseEntity;
import io.swagger.models.auth.In;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

/**
 *  秒杀商品实体类（秒杀业务实体）
 *
 * @author 一只朱
 * @date 2026-08-23 15:13
 *
 * "Run the code. Run the world."
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class SeckillProductEntity extends BaseEntity {
    @NotNull
    private Long productId; //关联原商品 id
    @NotNull
    private Integer withHoldQuantity; //预锁定/预占库存(下单后冻结待付款库存)
    @NotNull
    private Integer remainQuantity; //剩余可售库存(可卖现货)
    @NotNull
    private BigDecimal price; //秒杀活动价格
    @NotNull
    private Date startTime; //秒杀开始时间
    @NotNull
    private Date endTime; //秒杀结束时间

}
