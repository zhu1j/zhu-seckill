package cn.net.zhu.seckill.business.entity;

import io.swagger.models.auth.In;
import lombok.Data;

/**
 * 分页请求基类
 *
 * @author 一只朱
 * @date 2026-08-11 04:04
 *
 * "Run the code. Run the world."
 */

@Data
public class RequestPageEntity {
    private Integer pageNo = 1;
    private Integer pageSize = 10;
    private String sortField;

    //这个方法返回的是 跳过多少条记录
    public int getPageBegin() {
        return (pageNo - 1)* pageSize;
    }
}
