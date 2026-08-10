package cn.net.zhu.seckill.business.entity;

import io.swagger.models.auth.In;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 分页响应类
 *
 * @author 一只朱
 * @date 2026-08-11 04:07
 *
 * "Run the code. Run the world."
 */

@Data
public class ResponsePageEntity<T> {
    private Integer pageNo;
    private Integer pageSize;
    private Integer totalPage;
    private Long totalCount;
    private List<T> data;

    public static <T> ResponsePageEntity<T> buildEmpty() {
        ResponsePageEntity<T> entity = new ResponsePageEntity<>();
        entity.setData(new ArrayList<>());
        entity.setTotalCount(0L);
        entity.setTotalPage(0);
        return entity;
    }
}
