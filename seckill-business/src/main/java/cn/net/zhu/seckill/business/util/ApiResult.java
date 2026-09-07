package cn.net.zhu.seckill.business.util;

import cn.hutool.http.HttpStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * api请求响应实体
 *
 * @author 一只朱
 * @date 2026-09-07 16:04
 *
 * "Run the code. Run the world."
 */

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ApiResult<T> {

    /**
     * 请求成功状态码
     */
    public static final int OK = HttpStatus.HTTP_OK;

    /**
     * 接口返回码
     */
    private int code;

    /**
     * 接口返回信息
     */
    private String message;

    /**
     * 数据
     */
    private T data;
}
