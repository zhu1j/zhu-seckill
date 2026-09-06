package cn.net.zhu.seckill.api.controller;

import cn.net.zhu.seckill.business.entity.seckill.SeckillRefundEntity;
import cn.net.zhu.seckill.business.service.SeckillRefundService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 秒杀退货退款控制器
 * 对外暴露退款相关API；用户申请退款、管理员审核退款、执行退款、取消退款申请、多维度查询退款记录；
 * Swagger生成接口文档；所有接口返回JSON；
 * /audit 为管理后台接口；/apply /cancel 为普通用户接口；/process 一般用于后台或者第三方回调触发。
 *
 * @author 一只朱
 * @date 2026-09-06 15:39
 *
 * "Run the code. Run the world."
 */

@Api(tags = "秒杀退货管理")
@RestController
@RequestMapping("/seckill/refund")
@RequiredArgsConstructor
public class RefundController {

    /**
     * 退款业务服务，封装退款全部业务逻辑
     */
    private final SeckillRefundService seckillRefundService;

    /**
     * 用户申请退货退款
     * @param orderCode 业务订单编号
     * @param refundReason 退款原因
     * @param refundType 退款类型：全额/部分退款
     * @return SeckillRefundEntity 创建的退款申请记录
     */
    @ApiOperation("申请退货")
    @PostMapping("/apply")
    public SeckillRefundEntity applyRefund(@RequestParam String orderCode,
                                           @RequestParam String refundReason,
                                           @RequestParam Integer refundType) {
        return seckillRefundService.applyRefund(orderCode, refundReason, refundType);
    }

    /**
     * 管理员审核退货退款申请
     * 审核通过内部会自动调用processRefund执行退款；审核拒绝直接结束流程
     * @param refundId 退款记录主键ID
     * @param auditResult 审核结果：2通过，3拒绝
     * @param auditRemark 审核备注，选填
     * @param auditUserId 审核管理员用户ID
     * @param auditUserName 审核管理员用户名
     * @return Boolean true审核操作成功
     */
    @ApiOperation("审核退货申请")
    @PostMapping("/audit")
    public Boolean auditRefund(@RequestParam Long refundId,
                               @RequestParam Integer auditResult,
                               @RequestParam(required = false) String auditRemark,
                               @RequestParam Long auditUserId,
                               @RequestParam String auditUserName) {
        return seckillRefundService.auditRefund(refundId, auditResult, auditRemark,
                auditUserId, auditUserName);
    }

    /**
     * 执行退款
     * 审核通过后调用；模拟调用第三方退款接口，更新退款单、支付流水、订单状态
     * @param refundId 退款记录主键ID
     * @return Boolean true退款成功；false退款失败
     */
    @ApiOperation("处理退款")
    @PostMapping("/process")
    public Boolean processRefund(@RequestParam Long refundId) {
        return seckillRefundService.processRefund(refundId);
    }

    /**
     * 查询退货退款单详情
     * @param refundId 退款记录主键ID
     * @return SeckillRefundEntity 退款实体，查不到返回null
     */
    @ApiOperation("查询退货详情")
    @GetMapping("/detail/{refundId}")
    public SeckillRefundEntity getRefundDetail(@PathVariable Long refundId) {
        return seckillRefundService.getRefundDetail(refundId);
    }

    /**
     * 根据业务订单号查询该订单全部退款记录
     * @param orderCode 业务订单编号
     * @return List<SeckillRefundEntity> 退款流水集合
     */
    @ApiOperation("根据订单编号查询退货记录")
    @GetMapping("/order/{orderCode}")
    public List<SeckillRefundEntity> getRefundsByOrderCode(@PathVariable String orderCode) {
        return seckillRefundService.getRefundsByOrderCode(orderCode);
    }

    /**
     * 根据用户ID查询该用户所有退款记录
     * @param userId 用户ID
     * @return List<SeckillRefundEntity> 用户退款流水集合
     */
    @ApiOperation("根据用户ID查询退货记录")
    @GetMapping("/user/{userId}")
    public List<SeckillRefundEntity> getRefundsByUserId(@PathVariable Long userId) {
        return seckillRefundService.getRefundsByUserId(userId);
    }

    /**
     * 用户取消待审核退货退款申请
     * 仅申请中状态允许取消；作废退款申请，不修改订单、支付状态
     * @param refundId 退款记录主键ID
     * @return Boolean true取消成功
     */
    @ApiOperation("取消退货申请")
    @PostMapping("/cancel/{refundId}")
    public Boolean cancelRefund(@PathVariable Long refundId) {
        return seckillRefundService.cancelRefund(refundId);
    }
}

/*
====================业务总结====================
1、模块职责：退款模块对外API控制器；提供退款申请、审核、执行退款、取消、多维度查询接口；Swagger文档；接口层只做参数接收转发，业务逻辑下沉Service。
2、调用链路：
用户前端 → /seckill/refund/apply 提交退款申请；
管理后台 → /seckill/refund/audit 审核退款；审核通过内部触发退款；
后台/测试 → /seckill/refund/process 手动执行退款；
查询接口：/detail /order/{orderCode} /user/{userId}；
用户前端 → /seckill/refund/cancel/{refundId} 取消退款申请。
3、核心流程：
① /apply：接收orderCode、refundReason、refundType，调用service创建退款申请；
② /audit：后台传入审核信息，执行审核逻辑，审核通过自动执行退款；
③ /process：独立执行退款；
④ 查询接口：按退款ID、订单号、用户ID查询退款流水；
⑤ /cancel/{refundId}：取消待审核退款申请。
4、技术设计亮点：
- Swagger注解完善，方便前后端联调；
- 请求参数区分 @RequestParam、@PathVariable；REST风格URL；
- Controller层薄，所有业务逻辑全部下沉Service层。
5、风险点 & 潜在坑：
- 无参数校验（没有DTO，全部@RequestParam）：参数为空、refundType/auditResult非法值直接传入Service；
- **缺少鉴权权限控制**：
    - /apply：未校验当前登录用户是否属于该订单，可越权替别人申请退款；
    - /audit：任何人都可以调用审核接口，没有管理员权限拦截；
    - /user/{userId}：可以查任意用户退款记录；
    - /cancel：可以取消别人的退款申请；
- /audit接口内部service会同步调用processRefund，同步阻塞第三方退款逻辑，生产环境高耗时；
- service抛出RuntimeException，依赖全局异常处理器统一封装错误返回；
- /process接口对外暴露，生产环境应限制访问，不允许前端直接调用。
6、模块关联：依赖 SeckillRefundService；没有使用DTO，直接接收url参数；对应退款管理后台页面。
7、生产注意事项：
- 入参封装DTO，增加JSR‑303参数校验；
- 全部接口增加登录鉴权：用户接口校验归属；审核接口增加管理员角色校验；
- audit审核成功不要同步调用processRefund；改为MQ/线程池异步执行退款；避免第三方超时导致审核事务回滚；
- /process接口不要暴露给前端；增加IP白名单或者内部接口鉴权；
- 返回不要直接返回SeckillRefundEntity实体，使用VO输出，屏蔽内部数据库字段。
*/
