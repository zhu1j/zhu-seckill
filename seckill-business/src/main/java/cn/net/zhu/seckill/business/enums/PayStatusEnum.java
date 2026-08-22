package cn.net.zhu.seckill.business.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *  支付状态枚举类
 *
 * @author 一只朱
 * @date 2026-08-22 19:14
 *
 * "Run the code. Run the world."
 */

@Getter
@AllArgsConstructor
public enum PayStatusEnum {
    PENDING(1,"待支付"),
    PAID(2,"已支付"),
    REFUNDED(3,"退款");

    private final int value;
    private final String desc;

}
