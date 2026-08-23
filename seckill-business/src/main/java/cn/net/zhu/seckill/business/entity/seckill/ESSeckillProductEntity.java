package cn.net.zhu.seckill.business.entity.seckill;

import cn.net.zhu.seckill.business.entity.EsBaseEntity;
import io.swagger.models.auth.In;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 *  ES 秒杀商品实体类（ES存储）
 *
 * @author 一只朱
 * @date 2026-08-23 15:10
 *
 * "Run the code. Run the world."
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class ESSeckillProductEntity extends EsBaseEntity {
    private String name; //商品名称
    private String brandName; //品牌名称
    private String model; //商品型号
    private Long productId; //关联原商品id
    private Integer withHoldQuantity; //预锁定/预占库存(下单后冻结待付款库存)
    private Integer remainQuantity; //剩余可售库存(可卖现货)
    private BigDecimal price; //秒杀活动价格
    private BigDecimal costPrice; //商品成本价
    private Date startTime; //秒杀开始时间
    private Date endTime; //秒杀结束时间
    private String cover; //商品封面图地址
}
