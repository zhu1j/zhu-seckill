package cn.net.zhu.seckill.api.controller;

import cn.net.zhu.seckill.api.dto.CreatePaymentRequest;
import cn.net.zhu.seckill.business.entity.seckill.SeckillPaymentEntity;
import cn.net.zhu.seckill.business.service.SeckillPaymentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 秒杀支付接口控制器
 * 对外暴露支付流水相关API；创建支付单、处理支付回调、查询支付流水、取消支付；
 * 使用Swagger注解生成接口文档；全部为JSON接口；
 * processPayment模拟第三方支付回调接口，真实环境一般由第三方平台POST回调调用。
 *
 * @author 一只朱
 * @date 2026-09-06 11:12
 *
 * "Run the code. Run the world."
 */

@Api(tags = "秒杀支付管理")
@RestController
@RequestMapping("/seckill/payment")
@RequiredArgsConstructor
public class PaymentController {

    /**
     * 支付流水业务服务，封装支付单创建、支付处理、取消、查询逻辑
     */
    private final SeckillPaymentService seckillPaymentService;

    /**
     * 创建支付记录
     * 根据订单号与支付方式生成待支付支付流水；订单必须为待支付状态
     * @param request 创建支付请求DTO，@Valid完成参数校验
     * @return SeckillPaymentEntity 返回新建的支付流水实体
     */
    @ApiOperation("创建支付记录")
    @PostMapping("/create")
    public SeckillPaymentEntity createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        return seckillPaymentService.createPayment(request.getOrderCode(), request.getPaymentMethod());
    }

    /**
     * 处理支付（模拟第三方回调）
     * 传入支付流水ID与第三方交易号，执行业务支付逻辑，更新支付流水与订单状态
     * @param paymentId 支付流水主键ID
     * @param thirdPartyTransactionNo 第三方平台交易号，可为空
     * @return Boolean true支付处理成功；false处理失败
     */
    @ApiOperation("处理支付")
    @PostMapping("/process")
    public Boolean processPayment(@RequestParam Long paymentId,
                                  @RequestParam(required = false) String thirdPartyTransactionNo) {
        return seckillPaymentService.processPayment(paymentId, thirdPartyTransactionNo);
    }

    /**
     * 根据支付流水主键查询支付详情
     * @param paymentId 支付记录主键ID，路径变量
     * @return SeckillPaymentEntity 支付流水实体，查不到返回null
     */
    @ApiOperation("查询支付详情")
    @GetMapping("/detail/{paymentId}")
    public SeckillPaymentEntity getPaymentDetail(@PathVariable Long paymentId) {
        return seckillPaymentService.getPaymentDetail(paymentId);
    }

    /**
     * 根据业务订单号查询该订单下全部支付流水
     * 同一订单可有多条支付记录，对应多次发起支付
     * @param orderCode 业务订单编号
     * @return List<SeckillPaymentEntity> 支付流水集合
     */
    @ApiOperation("根据订单编号查询支付记录")
    @GetMapping("/order/{orderCode}")
    public List<SeckillPaymentEntity> getPaymentsByOrderCode(@PathVariable String orderCode) {
        return seckillPaymentService.getPaymentsByOrderCode(orderCode);
    }

    /**
     * 取消支付流水
     * 仅待支付状态的支付单允许取消，只作废支付流水，不修改订单主状态
     * @param paymentId 支付流水主键ID
     * @return Boolean true取消成功
     */
    @ApiOperation("取消支付")
    @PostMapping("/cancel/{paymentId}")
    public Boolean cancelPayment(@PathVariable Long paymentId) {
        return seckillPaymentService.cancelPayment(paymentId);
    }
}

/*
====================业务总结====================
1、模块职责：支付模块对外API控制器；提供支付单CRUD接口；Swagger自动生成接口文档；所有接口返回JSON数据。
2、调用链路：
前端支付页面 → /seckill/payment/create 创建支付单；
模拟第三方回调 → /seckill/payment/process 完成支付；
订单取消业务 → /seckill/payment/cancel/{paymentId} 作废支付流水；
前端查询 → /detail /order/{orderCode} 查询支付流水。
3、核心流程：
① /create：接收orderCode、paymentMethod参数校验，调用service创建待支付支付单；
② /process：接收paymentId、第三方交易号，执行业务支付，联动更新订单状态；
③ /detail /order/{orderCode}：单条、列表查询支付流水；
④ /cancel/{paymentId}：取消待支付支付流水。
4、技术设计亮点：
- Swagger @Api、@ApiOperation 标注接口，便于前后端联调；
- @Valid校验RequestBody入参；路径变量、请求参数区分不同传参场景；
- REST风格URL设计；接口只做参数转发，业务逻辑下沉Service层。
5、风险点 & 潜在坑：
- 缺少登录鉴权：未校验当前用户是否属于该订单/支付流水，存在越权访问风险；
- /process为模拟回调接口；真实第三方回调需要增加签名校验、IP白名单、幂等处理；
- service层抛出RuntimeException，依赖全局异常处理器统一返回错误JSON；
- cancelPayment只作废支付流水，不会回滚库存、不会修改订单状态，调用方需要保证状态一致性。
6、模块关联：依赖 SeckillPaymentService；入参DTO：CreatePaymentRequest；对接支付页面payment.html。
7、生产注意事项：
- 全部接口增加用户权限校验，禁止访问他人订单的支付流水；
- 第三方回调接口不要对外暴露，增加安全防护；
- processPayment接口必须做幂等，防止第三方重复回调；
- 生产环境禁用直接调用 /process 模拟支付的能力。
*/