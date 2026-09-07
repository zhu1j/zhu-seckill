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
public enum OrderStatusEnum {
    ORDERED(1,"下单"),
    PAID(2,"支付"),
    COMPLETED(3,"完成"),
    CANCELLED(4,"取消");

    private final int value;
    private final String desc;

    OrderStatusEnum(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }
    public Integer getValue() {
        return value;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 根据值获取枚举
     *
     * @param value 状态值
     * @return 订单状态枚举
     */
    public static OrderStatusEnum getByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (OrderStatusEnum statusEnum : OrderStatusEnum.values()) {
            if (statusEnum.getValue().equals(value)) {
                return statusEnum;
            }
        }
        return null;
    }

    /**
     * 根据值获取描述
     *
     * @param value 状态值
     * @return 状态描述
     */
    public static String getDescByValue(Integer value) {
        OrderStatusEnum statusEnum = getByValue(value);
        return statusEnum != null ? statusEnum.getDesc() : "未知状态";
    }
}
