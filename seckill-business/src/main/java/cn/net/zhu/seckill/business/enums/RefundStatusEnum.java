package cn.net.zhu.seckill.business.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *  退款状态枚举类
 *
 * @author 一只朱
 * @date 2026-08-22 19:19
 *
 * "Run the code. Run the world."
 */
public enum RefundStatusEnum {

    /**
     * 申请中
     */
    APPLYING(1, "申请中"),

    /**
     * 审核通过
     */
    APPROVED(2, "审核通过"),

    /**
     * 审核拒绝
     */
    REJECTED(3, "审核拒绝"),

    /**
     * 退款中
     */
    REFUNDING(4, "退款中"),

    /**
     * 退款成功
     */
    REFUNDED(5, "退款成功"),

    /**
     * 退款失败
     */
    REFUND_FAILED(6, "退款失败");

    private final Integer value;
    private final String desc;

    RefundStatusEnum(Integer value, String desc) {
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
     * @return 退货状态枚举
     */
    public static RefundStatusEnum getByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (RefundStatusEnum statusEnum : RefundStatusEnum.values()) {
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
        RefundStatusEnum statusEnum = getByValue(value);
        return statusEnum != null ? statusEnum.getDesc() : "未知状态";
    }
}
