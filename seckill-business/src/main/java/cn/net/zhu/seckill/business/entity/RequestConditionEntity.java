package cn.net.zhu.seckill.business.entity;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 请求条件实体
 * 继承 RequestPageEntity；封装通用查询条件：时间范围、模糊查询、Excel导出自定义表头；
 * 作为各类业务查询DTO的父类，复用分页+通用查询字段。
 *
 * @author 一只朱
 * @date 2026-09-06 17:20
 *
 * "Run the code. Run the world."
 */

@Data
public class RequestConditionEntity extends RequestPageEntity {

    /**
     * 创建日期范围，前端传时间数组，例如 ["2024‑01‑01","2024‑01‑31"]
     */
    @ApiModelProperty("创建日期范围")
    private List<String> betweenTime;

    /**
     * 创建开始时间，字符串格式时间
     */
    private String createBeginTime;

    /**
     * 创建结束时间，字符串格式时间
     */
    private String createEndTime;

    /**
     * 自定义excel表头列表，用于导出功能，自定义导出哪些列
     */
    @ApiModelProperty("自定义excel表头列表")
    private List<String> customizeColumnNameList;

    /**
     * 模糊查询关键词，通用blurry字段，用于多字段模糊检索
     */
    @ApiModelProperty("查询条件")
    private String blurry;
}

/*
====================业务总结====================
1、模块职责：通用查询父DTO；继承 RequestPageEntity（分页参数 pageNum、pageSize）；
封装项目通用查询能力：时间范围、模糊检索、Excel自定义导出表头；业务查询条件实体继承此类，复用通用字段。

2、继承链：
RequestPageEntity（分页：pageNum、pageSize）
  ↑
RequestConditionEntity（时间范围、blurry模糊、Excel表头）
  ↑
ESSeckillProductConditionEntity（ES商品查询条件）以及其他业务查询DTO

3、字段说明：
‑ betweenTime：前端一次性传入起止时间字符串数组；业务代码一般解析数组赋值给 createBeginTime、createEndTime；
‑ createBeginTime / createEndTime：创建时间区间过滤；String类型，接收前端时间字符串，后续转Date/LocalDateTime；
‑ customizeColumnNameList：Excel导出时，用户自定义需要导出的表头列集合；用于动态导出指定列；
‑ blurry：通用模糊查询字段，一般用于多字段like/match模糊搜索。

4、技术设计亮点：
‑ 分层继承，分页、通用查询条件抽离父类，各个业务查询DTO不用重复定义；
‑ 兼顾两种传参方式：betweenTime数组 和 createBeginTime/createEndTime 分开传，适配不同前端组件；
‑ 内置Excel导出相关字段，导出接口直接复用该DTO。

5、风险点 & 潜在坑：
‑ 两套时间参数并存 betweenTime 和 createBeginTime/createEndTime；业务代码需要做兼容解析，处理优先级，容易出现逻辑遗漏；
‑ 时间全部使用String接收；没有统一日期格式校验；前端传入非法时间字符串，解析会抛异常；
‑ blurry为通用模糊字段；MyBatis中要注意防止SQL注入，不能直接拼接${}；ES中使用match查询；
‑ customizeColumnNameList 如果直接把前端传入的值映射到Excel列名，存在注入风险；需要做白名单校验；
‑ 继承层级多；父类新增字段所有子类全部继承，接口出参/入参字段会变多；
‑ 缺少swagger注解部分字段(createBeginTime、createEndTime)，接口文档缺失说明。

6、模块关联：父类 RequestPageEntity；子类 ESSeckillProductConditionEntity；所有列表查询DTO。

7、生产注意事项：
‑ 时间参数处理优先级：优先解析 betweenTime，覆盖 createBeginTime、createEndTime；
‑ 增加日期格式校验，非法时间直接参数校验失败返回；
‑ blurry禁止MyBatis使用${}字符串拼接；MySQL使用concat('%',#{blurry},'%')；ES使用match；
‑ customizeColumnNameList必须做白名单过滤，只允许配置中存在的表头，防止任意列导出；
‑ 补全缺失 @ApiModelProperty；
‑ 分页父类RequestPageEntity需要做pageSize上限保护，防止超大pageSize压垮DB/ES；
‑ DTO继承层级不宜继续加深；业务复杂后建议改为组合代替继承。
*/


