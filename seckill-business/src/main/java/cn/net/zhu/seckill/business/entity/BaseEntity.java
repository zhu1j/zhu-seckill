package cn.net.zhu.seckill.business.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 基础实体类（所有数据库实体类的父类）
 *
 * @author 一只朱
 * @date 2026-08-11 04:03
 *
 * "Run the code. Run the world."
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseEntity {
    private Long id;
    private Long createUserId;
    private String createUserName;
    private Date createTime;
    private Long updateUserId;
    private String updateUserName;
    private Date updateTime;
    private Integer isDel;
}
