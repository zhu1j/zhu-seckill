package cn.net.zhu.seckill.business.entity.product;

import cn.net.zhu.seckill.business.entity.RequestConditionEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * ES秒杀商品查询条件实体
 * 继承RequestConditionEntity，封装ES搜索的过滤条件；用于秒杀商品列表ES查询入参；
 * 该项目是知识星球：java突击队 的内部项目
 *
 *
 * @author 一只朱
 * @date 2026-08-23 15:07
 *
 * "Run the code. Run the world."
 */
@ApiModel("秒杀商品查询条件实体")
@Data
public class ESSeckillProductConditionEntity extends RequestConditionEntity {

    /**
     * 秒杀商品ID
     */
    @ApiModelProperty("ID")
    private Long id;

    /**
     * 普通商品ID
     */
    @ApiModelProperty("商品ID")
    private Long productId;

    /**
     * 秒杀商品名称，支持ES模糊检索
     */
    @ApiModelProperty("商品名称")
    private String name;

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
     * 秒杀活动开始时间
     */
    @ApiModelProperty("秒杀开始时间")
    private Date startTime;

    /**
     * 秒杀活动结束时间
     */
    @ApiModelProperty("秒杀结束时间")
    private Date endTime;

    /**
     * 秒杀状态 (0‑未开始, 1‑进行中, 2‑已结束)
     */
    @ApiModelProperty("秒杀状态")
    private Integer seckillStatus;

    /**
     * 查询时间范围‑开始，用于过滤时间区间
     */
    @ApiModelProperty("查询开始时间")
    private Date queryStartTime;

    /**
     * 查询时间范围‑结束，用于过滤时间区间
     */
    @ApiModelProperty("查询结束时间")
    private Date queryEndTime;
}

/*
====================业务总结====================
1、模块职责：ES查询条件DTO；继承 RequestConditionEntity（一般封装分页、排序参数）；
组装各类过滤条件，传给ES，构建DSL语句，查询秒杀商品列表。

2、调用链路：
Controller接收前端查询参数 → 封装 ESSeckillProductConditionEntity → Service 根据该实体拼装ES查询DSL → ES返回商品列表。

3、字段分类：
1）主键业务字段：id(秒杀商品ID)、productId(源商品ID)、name(商品名称模糊检索)
2）库存价格：withHoldQuantity预扣库存、remainQuantity实际库存、price秒杀价
3）活动时间：startTime、endTime 秒杀本身起止时间；
4）状态：seckillStatus 0未开始/1进行中/2已结束；
5）查询区间：queryStartTime / queryEndTime，用于时间范围筛选；
6）父类RequestConditionEntity：通常包含 pageNum、pageSize、sortField、sortOrder分页排序。

4、技术设计亮点：
‑ 继承复用分页排序条件，不需要重复写分页字段；
‑ swagger完整注解，接口文档清晰；
‑ 把所有ES过滤条件收拢到一个DTO，Service层直接读取字段构建DSL；职责清晰。

5、风险点 & 潜在坑：
‑ startTime / endTime：是秒杀活动本身时间；queryStartTime / queryEndTime是查询过滤区间，两个语义容易混淆，写DSL时容易用错字段。
‑ name做模糊检索：ES中要注意分词匹配；直接传入完整字符串会做term精确匹配，达不到模糊搜索效果。
‑ seckillStatus是业务计算状态，ES索引不一定实时存储该字段；该条件不能直接term过滤，需要后端根据startTime、endTime实时计算；直接拿这个字段去ES查询会查不到数据。
‑ BigDecimal价格类型：ES内部没有BigDecimal，存储一般用long分；DTO用BigDecimal接收，转换时容易出现精度bug。
‑ Date类型：前端传时间字符串序列化/反序列化时区问题；ES存储UTC，应用本地时区转换出错导致时间筛选范围错乱。
‑ 继承RequestConditionEntity，如果父类有多余参数，全部带入查询条件，拼装DSL时要做判断，null字段不要拼接过滤条件。
‑ 没有参数校验；例如pageSize过大，不做限制，会导致ES深度分页性能问题。

6、模块关联：父类 RequestConditionEntity；用于ESSeckillProductService，构建Elasticsearch DSL查询。

7、生产注意事项：
‑ seckillStatus不要直接作为ES过滤条件；优先使用startTime、endTime做时间范围过滤，内存计算状态；
‑ name模糊搜索使用match分词查询，不要使用term；
‑ 价格ES存储用long（单位分），DTO做BigDecimal和long转换，处理精度；
‑ 时间字段统一时区，建议UTC；
‑ 拼装DSL的时候判断字段不为null才追加过滤条件；null字段跳过；
‑ 对pageSize做最大值限制，防止深度分页；
‑ 区分两组时间：秒杀活动时间(startTime/endTime) 和 查询过滤时间(queryStartTime/queryEndTime)，代码注释写清楚，避免DSL写错。
*/

