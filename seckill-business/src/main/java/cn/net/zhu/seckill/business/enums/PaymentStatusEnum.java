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

public enum PaymentStatusEnum {

    /**
     * 待支付
     */
    PENDING(1, "待支付"),

    /**
     * 支付中
     */
    PROCESSING(2, "支付中"),

    /**
     * 支付成功
     */
    SUCCESS(3, "支付成功"),

    /**
     * 支付失败
     */
    FAILED(4, "支付失败"),

    /**
     * 已退款
     */
    REFUNDED(5, "已退款"),

    /**
     * 部分退款
     */
    PARTIAL_REFUNDED(6, "部分退款");

    private final Integer value;
    private final String desc;

    PaymentStatusEnum(Integer value, String desc) {
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
     * @return 支付记录状态枚举
     */
    public static PaymentStatusEnum getByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (PaymentStatusEnum statusEnum : PaymentStatusEnum.values()) {
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
        PaymentStatusEnum statusEnum = getByValue(value);
        return statusEnum != null ? statusEnum.getDesc() : "未知状态";
    }
}
