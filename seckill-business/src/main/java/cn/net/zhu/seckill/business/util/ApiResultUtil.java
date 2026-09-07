package cn.net.zhu.seckill.business.util;

/**
 * api请求响应实体处理工具类
 *
 * @author 一只朱
 * @date 2026-09-07 16:04
 *
 * "Run the code. Run the world."
 */

public class ApiResultUtil {

    private ApiResultUtil() {
    }

    /**
     * 请求成功
     *
     * @param data 数据
     * @param <T>  数据类型
     * @return 接口相应实体
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(ApiResult.OK, null, data);
    }

    /**
     * 请求成功
     *
     * @param <T> 数据类型
     * @return 接口相应实体
     */
    public static <T> ApiResult<T> success() {
        return success(null);
    }

    /**
     * 请求成功
     *
     * @param code    返回码
     * @param message 返回信息
     * @param <T>     数据类型
     * @return 接口相应实体
     */
    public static <T> ApiResult<T> error(int code, String message) {
        return new ApiResult<>(code, message, null);
    }
}
