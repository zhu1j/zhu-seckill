package cn.net.zhu.seckill.business.entity.seckill;

import lombok.Data;

/**
 * 秒杀商品详情页面实体
 * 继承SeckillProductDetailEntity；在基础详情字段之上增加前端页面渲染所需倒计时、秒杀状态字段；
 * 用于详情页接口返回，给前端做倒计时、按钮置灰、可秒杀状态展示。
 *
 * @author 一只朱
 * @date 2026-08-23 15:14
 *
 * "Run the code. Run the world."
 */
@Data
public class SeckillProductDetailPageEntity extends SeckillProductDetailEntity {

    /**
     * 距离秒杀开始剩余秒数，用于前端倒计时；0代表无倒计时
     */
    private Long remainSeconds = 0L;

    /**
     * 秒杀状态
     * 0：未开始
     * 1：秒杀倒计时（还未到开始时间，展示倒计时）
     * 2：秒杀进行中
     * 3：秒杀已结束
     */
    private Integer seckillStatus = 0;
}

/*
====================业务总结====================
1、模块职责：页面VO实体（接口出参）；继承基础详情实体，扩展页面动态状态字段；
SeckillProductDetailEntity是数据库/基础详情；SeckillProductDetailPageEntity是面向前端页面的视图对象。

2、调用链路：
服务端查询SeckillProductDetailEntity → 根据当前系统时间、秒杀开始/结束时间计算remainSeconds、seckillStatus → 组装SeckillProductDetailPageEntity返回给前端。

3、字段说明：
‑ remainSeconds：剩余秒数，前端JS直接做倒计时组件渲染；默认0；
‑ seckillStatus：4种状态，驱动前端按钮逻辑：
 0‑未开始：页面普通展示；
 1‑倒计时：展示倒计时，秒杀按钮置灰不可点击；
 2‑进行中：秒杀按钮点亮，可以下单；
 3‑已结束：秒杀结束，按钮置灰。

4、技术设计亮点：
‑ 继承复用父类SeckillProductDetailEntity全部商品详情字段，不用重复定义；
‑ 默认值初始化，避免前端拿到null；
‑ 把时间计算逻辑放在后端，前端只负责渲染，减少前端时间不同步问题。

5、风险点 & 潜在坑：
‑ 状态是后端根据服务器时间计算；如果前端本地时间和服务端时间偏差过大，页面展示会有偏差；以服务端返回状态为准，前端不要自己算时间。
‑ remainSeconds只代表距离开始的秒数；秒杀结束后该字段语义失效；
‑ 没有Swagger注解，接口文档看不到字段说明；
‑ 魔法数字 0/1/2/3，没有枚举；代码可读性差；
‑ 继承实体，如果父类有数据库字段，VO会全部带出，部分无用字段也会返回前端。
‑ 没有校验：seckillStatus和remainSeconds可能出现状态矛盾（例如状态=1倒计时，但remainSeconds=0）。

6、模块关联：父类 SeckillProductDetailEntity；用于商品详情Controller接口返回。

7、生产注意事项：
‑ 增加@ApiModel、@ApiModelProperty注解完善接口文档；
‑ 建议定义枚举 SeckillPageStatusEnum，替换魔法数字；
‑ 后端计算状态时做好约束，保证seckillStatus与remainSeconds语义一致；
‑ 秒杀进行中也要叠加库存判断：即使status=2，如果库存为0，前端按钮也要置灰；该VO缺少库存状态字段；
‑ 避免VO直接继承数据库Entity，最佳实践是DTO拷贝，防止数据库敏感字段泄露；
‑ remainSeconds不要返回负数，计算小于0时置0。
*/
