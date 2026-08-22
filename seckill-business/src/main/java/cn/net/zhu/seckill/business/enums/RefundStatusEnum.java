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
 @Getter
 @AllArgsConstructor
public enum RefundStatusEnum {
     APPLYING(1,"申请中"),
     APPROVED(2,"审核通过"),
     REJECTED(3,"审核拒绝"),
     REFUNDING(4,"退款中"),
     REFUNDED(5,"退款成功"),
     REFUND_FAILED(6,"退款失败");

     private final int value;
     private final String desc;
}
