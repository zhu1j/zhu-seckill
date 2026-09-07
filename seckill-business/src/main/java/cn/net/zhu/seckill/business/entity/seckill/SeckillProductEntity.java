package cn.net.zhu.seckill.business.entity.seckill;

import cn.net.zhu.seckill.business.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.models.auth.In;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

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

@ApiModel("秒杀商品实体")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SeckillProductEntity extends BaseEntity {

    /**
     * 分类名称
     */
    @ApiModelProperty("分类名称")
    private String categoryName;

    /**
     * 单位名称
     */
    @ApiModelProperty("单位名称")
    private String unitName;

    /**
     * 品牌名称
     */
    @ApiModelProperty("品牌名称")
    private String brandName;

    /**
     * 商品名称
     */
    @ApiModelProperty("商品名称")
    private String name;

    /**
     * 规格
     */
    @ApiModelProperty("规格")
    private String model;

    /**
     * 原价
     */
    @ApiModelProperty("原价")
    private BigDecimal costPrice;

    /**
     * 商品ID
     */
    @NotNull(message = "商品ID不能为空")
    @ApiModelProperty("商品ID")
    private Long productId;

    /**
     * 预扣库存
     */
    @NotNull(message = "预扣库存不能为空")
    @ApiModelProperty("预扣库存")
    private Integer withHoldQuantity;

    /**
     * 实际剩余库存
     */
    @NotNull(message = "实际剩余库存不能为空")
    @ApiModelProperty("实际剩余库存")
    private Integer remainQuantity;

    /**
     * 秒杀价格
     */
    @NotNull(message = "秒杀价格不能为空")
    @ApiModelProperty("秒杀价格")
    private BigDecimal price;

    /**
     *  秒杀开始时间
     */
    @NotNull(message = "秒杀开始时间不能为空")
    @ApiModelProperty("秒杀开始时间")
    private Date startTime;

    /**
     *  秒杀结束时间
     */
    @NotNull(message = "秒杀结束时间不能为空")
    @ApiModelProperty("秒杀结束时间")
    private Date endTime;
}

