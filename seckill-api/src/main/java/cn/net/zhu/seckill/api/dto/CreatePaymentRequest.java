package cn.net.zhu.seckill.api.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 创建支付单请求DTO
 * 用于接口 /seckill/payment/create 的请求体入参；
 * JSR‑303参数校验：订单编号非空、支付方式不能为null。
 *
 * @author 一只朱
 * @date 2026-09-06 11:14
 *
 * "Run the code. Run the world."
 */

@Data
public class CreatePaymentRequest {

    /**
     * 业务订单编号，对外业务号，非数据库主键ID
     */
    @NotBlank(message = "订单编号不能为空")
    private String orderCode;

    /**
     * 支付方式
     * 1:支付宝 2:微信 3:银行卡
     */
    @NotNull(message = "支付方式不能为空")
    private Integer paymentMethod;
}

/*
====================业务总结====================
1、模块职责：创建支付接口入参DTO，接收前端JSON请求参数。
2、调用链路：前端支付页面AJAX JSON请求 → CreatePaymentRequest → PaymentController#createPayment → SeckillPaymentService#createPayment。
3、核心流程：@Valid触发校验，orderCode为空串/空白、paymentMethod为null直接返回参数校验错误；校验通过后传给业务层创建支付流水。
4、技术设计亮点：
- @NotBlank校验字符串，拦截空字符串、全空格；@NotNull校验Integer不能为null；
- 使用业务orderCode，不传递订单数据库主键，避免暴露内部ID。
5、风险点 & 潜在坑：
- DTO只做格式校验，**不做业务权限校验**；Controller/Service层需要校验该订单属于当前登录用户，防止越权为别人订单创建支付单。
- 仅校验非空，没有校验paymentMethod取值范围（只能1/2/3），枚举值合法性校验下沉到Service层。
6、模块关联：对应 PaymentController#createPayment；依赖 spring‑validation JSR‑303校验。
7、生产注意事项：
- 业务层需要校验paymentMethod是否在合法枚举范围内；
- 必须增加登录鉴权，校验当前用户与订单归属一致，防止越权。
*/