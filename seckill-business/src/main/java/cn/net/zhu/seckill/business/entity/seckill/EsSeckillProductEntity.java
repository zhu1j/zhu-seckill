package cn.net.zhu.seckill.business.entity.seckill;

import cn.net.zhu.seckill.business.entity.EsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

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

@ApiModel("秒杀商品实体")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class EsSeckillProductEntity extends EsBaseEntity {

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
    @ApiModelProperty("商品ID")
    private Long productId;

    /**
     * 预扣库存
     */
    @ApiModelProperty("预扣库存")
    private Integer withHoldQuantity;

    /**
     * 实际剩余库存
     */
    @ApiModelProperty("实际剩余库存")
    private Integer remainQuantity;

    /**
     * 秒杀价格
     */
    @ApiModelProperty("秒杀价格")
    private BigDecimal price;

    /**
     * 秒杀开始时间
     */
    @ApiModelProperty("秒杀开始时间")
    private Date startTime;

    /**
     * 秒杀结束时间
     */
    @ApiModelProperty("秒杀结束时间")
    private Date endTime;

    /**
     * 封面图片
     */
    @ApiModelProperty("封面图片")
    private String cover;
}
