package cn.net.zhu.seckill.business.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 *  ES 基础实体类
 *
 * @author 一只朱
 * @date 2026-08-23 15:02
 *
 * "Run the code. Run the world."
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class EsBaseEntity implements Serializable {

    /**
     * ID
     */
    private String id;

    /**
     * 数据
     */
    private Map<String, Object> data;
}
