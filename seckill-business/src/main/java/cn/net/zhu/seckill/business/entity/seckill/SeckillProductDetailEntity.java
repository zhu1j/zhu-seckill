package cn.net.zhu.seckill.business.entity.seckill;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 秒杀商品详情实体
 * 继承ESSeckillProductEntity；在ES索引基础信息之上扩展页面详情字段：商品详情、轮播图；
 * 该项目是知识星球：java突击队 的内部项目
 *
 * @author 一只朱
 * @date 2026-09-06 16:32
 *
 * "Run the code. Run the world."
 */

@ApiModel("秒杀商品详情实体")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SeckillProductDetailEntity extends EsSeckillProductEntity {

    /**
     * 商品详情，富文本HTML详情
     */
    @ApiModelProperty("商品详情")
    private String detail;

    /**
     * 轮播图图片地址列表
     */
    @ApiModelProperty("轮播图")
    private List<String> swiper;
}

/*
====================业务总结====================
1、模块职责：秒杀商品详情DTO；继承ESSeckillProductEntity，复用ES搜索索引的基础字段（标题、价格、时间、库存等）；
额外补充详情页专属字段：detail富文本详情、swiper轮播图；用于商品详情查询。

2、继承关系链：
ESSeckillProductEntity（ES索引基础字段）
  ↑
SeckillProductDetailEntity（增加detail、swiper）
  ↑
SeckillProductDetailPageEntity（再扩展remainSeconds、seckillStatus页面状态）

3、字段说明：
‑ detail：商品富文本详情，一般为HTML字符串，前端直接渲染；
‑ swiper：轮播图url集合，前端渲染轮播组件。

4、技术设计亮点：
‑ 使用继承复用上层ES实体全部属性，避免重复写字段；
‑ 完整swagger注解，接口文档清晰；
‑ 提供全参、无参构造，序列化/反序列化友好。

5、风险点 & 潜在坑：
‑ 继承ESSeckillProductEntity，会把ES索引所有字段全部继承下来；如果ES实体里有内部索引字段，会一并对外输出；
‑ detail为富文本HTML；如果内容没有做过滤，存在XSS安全风险；
‑ swiper集合为null时，前端直接遍历会空指针；建议初始化默认空集合 `private List<String> swiper = Collections.emptyList();`
‑ 继承结构层级深：后续修改父类字段会影响所有子类，改动影响范围大；
‑ ESSeckillProductEntity本身是ES映射实体，直接拿来做接口出参，耦合ES结构；ES索引字段变更会直接影响接口返回。

6、模块关联：
父类：ESSeckillProductEntity；子类：SeckillProductDetailPageEntity；
接口层查询商品详情，组装该实体返回前端。

7、生产注意事项：
‑ 富文本detail后端做XSS过滤，防止恶意脚本注入；
‑ swiper字段初始化空集合，避免返回null；
‑ 不建议深度继承；如果后续ES结构经常变动，建议改为对象拷贝，不要直接继承ES映射实体；
‑ 注意控制返回字段，不要把ES内部字段透传给前端；
‑ 大的detail富文本可以考虑做压缩，减少接口响应体积。
*/

