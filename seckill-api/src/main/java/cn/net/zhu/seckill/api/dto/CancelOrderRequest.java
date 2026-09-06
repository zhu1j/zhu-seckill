package cn.net.zhu.seckill.api.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

/**
 * 取消订单请求DTO
 * 接收前端AJAX取消订单接口入参；使用JSR‑303参数校验，订单编号不能为空。
 *
 * @author 一只朱
 * @date 2026-09-06 11:04
 *
 * "Run the code. Run the world."
 */

@Data
public class CancelOrderRequest {

    /**
     * 业务订单编码（对外业务号，非数据库主键ID）
     */
    @NotBlank(message = "订单编号不能为空")
    private String orderCode;
}

/*
====================业务总结====================
1、模块职责：接口入参DTO，用于 /order/cancel 接口接收JSON请求体。
2、调用链路：前端AJAX JSON请求 → CancelOrderRequest → OrderController#cancelOrder → SeckillOrderTradeService。
3、核心流程：@Valid触发校验，orderCode为空直接返回参数校验错误；校验通过后把orderCode传给业务层执行取消订单。
4、技术设计亮点：
- 使用 @NotBlank 做非空校验，拦截空字符串、全空格；
- 传输业务orderCode，不传递数据库主键id，避免暴露内部主键。
5、风险点 & 潜在坑：
- DTO只做格式校验，**不做业务权限校验**；业务层/Controller需要校验该订单属于当前登录用户，防止越权取消他人订单。
- 仅校验非空，不校验orderCode格式合法性，格式校验下沉到Mapper查询。
6、模块关联：对应 OrderController#cancelOrder；JSR‑303校验依赖spring‑validation。
7、生产注意事项：接口层仅做参数格式校验；权限、订单状态、库存回滚均由业务层处理。
*/

