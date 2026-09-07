package cn.net.zhu.seckill.business.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 签名实体
 *
 * @author 一只朱
 * @date 2026-09-07 15:29
 *
 * "Run the code. Run the world."
 */

@NoArgsConstructor
@AllArgsConstructor
@Data
public class SignEntity implements Serializable {

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 签名
     */
    private String sign;
}
