package cn.net.zhu.seckill.business.entity.seckill;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * ES 公共实体
 *
 * @author 一只朱
 * @date 2026-09-06 17:21
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
    private  String  id;

    /**
     * 数据
     */
    private Map<String, Object> data;
}

