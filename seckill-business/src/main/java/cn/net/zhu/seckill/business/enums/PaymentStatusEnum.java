package cn.net.zhu.seckill.business.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *  付款状态枚举类
 *
 * @author 一只朱
 * @date 2026-08-22 19:03
 *
 * "Run the code. Run the world."
 */

@Getter
@AllArgsConstructor
public enum PaymentStatusEnum {
    PENDING(1,"待支付"),
    PROCESSING(2,"支付中"),
    SUCCESS(3,"支付成功"),
    FAILED(4,"支付失败"),
    REFUNDED(5,"已退款");

    private final int value;
    private final String desc;

}
