package cn.net.zhu.seckill.business.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *  订单状态枚举类
 *
 * @author 一只朱
 * @date 2026-08-22 19:03
 *
 * "Run the code. Run the world."
 */
@Getter
@AllArgsConstructor
public enum OrderStatusEnum {
    ORDERED(1,"下单"),
    PAID(2,"支付"),
    COMPLETED(3,"完成"),
    CANCELLED(4,"取消");

    private final int value;
    private final String desc;

    public static String getDescByValue(Integer value){
        if (value == null) return "未知";
        for (OrderStatusEnum e : values()) {
            if (e.value == value) return e.desc;
        }
        return "未知";
    }
}
