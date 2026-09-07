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

public enum PayStatusEnum {

    /**
     * 待支付
     */
    PENDING(1, "待支付"),

    /**
     * 已支付
     */
    PAID(2, "已支付"),

    /**
     * 退款
     */
    REFUNDED(3, "退款");

    private final Integer value;
    private final String desc;

    PayStatusEnum(Integer value, String desc) {
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
     * @return 支付状态枚举
     */
    public static PayStatusEnum getByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (PayStatusEnum statusEnum : PayStatusEnum.values()) {
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
        PayStatusEnum statusEnum = getByValue(value);
        return statusEnum != null ? statusEnum.getDesc() : "未知状态";
    }
}
