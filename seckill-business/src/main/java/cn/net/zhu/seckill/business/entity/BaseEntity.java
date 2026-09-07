package cn.net.zhu.seckill.business.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 基础实体类（所有数据库实体类的父类）
 *
 * @author 一只朱
 * @date 2026-08-11 04:03
 *
 * "Run the code. Run the world."
 */

@AllArgsConstructor
@NoArgsConstructor
@Data
public class BaseEntity implements Serializable {

    /**
     * 系统ID
     */
    @ExcelProperty(value = "系统ID", index = -1)
    private Long id;

    /**
     * 创建人ID
     */
    @ExcelProperty(value = "创建人ID", index = -2)
    private Long createUserId;

    /**
     * 创建人名称
     */
    @ExcelProperty(value = "创建人名称", index = -3)
    private String createUserName;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间", index = -4)
    private Date createTime;

    /**
     * 修改人ID
     */
    @ExcelProperty(value = "修改人ID", index = -5)
    private Long updateUserId;

    /**
     * 修改人名称
     */
    @ExcelProperty(value = "修改人名称", index = -6)
    private String updateUserName;

    /**
     * 修改时间
     */
    @ExcelProperty(value = "修改时间", index = -7)
    private Date updateTime;

    /**
     * 是否删除
     */
    @ExcelProperty(value = "是否删除", index = -8)
    private Integer isDel;
}

