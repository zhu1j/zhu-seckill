package cn.net.zhu.seckill.business.entity.seckill;

import cn.net.zhu.seckill.business.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 秒杀退货日志实体
 * 退款操作审计日志，记录退款全生命周期每一步操作；用于追溯、排查问题、对账；
 * 每一次退款状态变更都生成一条日志记录；
 *
 *  秒杀退款日志实体类
 *
 * @author 一只朱
 * @date 2026-08-23 15:16
 *
 * "Run the code. Run the world."
 */
@ApiModel("秒杀退货日志实体")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SeckillRefundLogEntity extends BaseEntity {

    /**
     * 退款主记录ID（关联 SeckillRefundEntity.id）
     */
    @ApiModelProperty("退货ID")
    private Long refundId;

    /**
     * 退款业务流水号，对外业务编号
     */
    @ApiModelProperty("退货流水号")
    private String refundNo;

    /**
     * 操作类型
     * 1:申请退货 2:审核通过 3:审核拒绝 4:退款成功 5:退款失败 6:完成退货
     */
    @ApiModelProperty("操作类型 1:申请退货 2:审核通过 3:审核拒绝 4:退款成功 5:退款失败 6:完成退货")
    private Integer operationType;

    /**
     * 操作文本描述，便于人阅读
     */
    @ApiModelProperty("操作描述")
    private String operationDesc;

    /**
     * 操作人ID；用户申请退款为用户ID；管理员审核为管理员ID
     */
    @ApiModelProperty("操作人ID")
    private Long operatorId;

    /**
     * 操作人名称
     */
    @ApiModelProperty("操作人名称")
    private String operatorName;

    /**
     * 请求报文，第三方接口调用时保存入参JSON字符串；可为null
     */
    @ApiModelProperty("请求数据")
    private String requestData;

    /**
     * 响应报文，第三方接口返回结果JSON字符串；可为null
     */
    @ApiModelProperty("响应数据")
    private String responseData;

    /**
     * 日志记录创建时间
     */
    @ApiModelProperty("创建时间")
    private Date createTime;
}

/*
====================业务总结====================
1、模块职责：退款审计日志实体；记录退款单每一次状态变更；用于问题排查、对账、审计追溯；
一条退款单可以对应多条退款日志，一对多关系。
2、调用链路：
SeckillRefundServiceImpl#recordRefundLog() → 组装SeckillRefundLogEntity → SeckillRefundLogMapper.insert()落库。
触发时机：申请退款、审核通过、审核拒绝、用户取消退款、退款成功、退款失败。
3、字段说明：
‑ refundId：关联退款主表主键；
‑ refundNo：业务退款单号，方便直接根据业务号检索日志；
‑ operationType：操作编码，便于程序筛选；operationDesc：可读文本，页面展示；
‑ operatorId/operatorName：记录操作人，区分普通用户、管理员；
‑ requestData / responseData：存储第三方退款接口请求响应JSON，排查第三方对接问题；
‑ createTime：日志发生时间。
4、技术设计亮点：
‑ 继承BaseEntity，复用基础主键字段；
‑ 同时保存refundId（数据库主键）与refundNo（业务号），两种维度都可以查询日志；
‑ 保存operator信息，完整记录是谁操作；
‑ 预留requestData、responseData字段，对接第三方支付退款时保存报文，定位线上问题非常关键；
‑ Swagger完整注解，便于接口文档。
5、风险点 & 潜在坑：
‑ requestData、responseData为String类型；存储JSON字符串，数据库字段长度要足够，防止报文截断；
‑ 没有更新时间，日志只新增不修改，符合审计日志设计原则；
‑ 业务层没有做索引提醒：业务上建议对 refundId、refundNo 建立数据库索引，提升查询性能；
‑ operationType是魔法数字，没有使用枚举类，代码可读性差。
6、模块关联：对应 SeckillRefundLogMapper；由 SeckillRefundServiceImpl#recordRefundLog 私有方法生成；和 SeckillRefundEntity 一对多。
7、生产注意事项：
‑ MySQL库：requestData、responseData建议使用TEXT类型，避免报文超长被截断；
‑ 给 refundId、refundNo 添加索引；
‑ 建议把operationType抽成枚举 RefundOperationTypeEnum，替换魔法数字；
‑ 审计日志禁止业务逻辑做update/delete，只允许insert写入。
*/

