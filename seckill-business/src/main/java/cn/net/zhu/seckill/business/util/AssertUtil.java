package cn.net.zhu.seckill.business.util;

import cn.net.zhu.seckill.business.exception.BusinessException;
import org.springframework.util.StringUtils;

import java.util.Collection;

/**
 *  断言工具类
 *
 * 这是项目里**业务断言工具**，专门做参数校验，校验不通过直接抛出自定义业务异常 `BusinessException`，统一异常处理器就能捕获，返回给前端错误码 + 提示信息。
 * 特点：全部是**静态方法**，私有构造 `private AssertUtil(){}`，禁止 new 对象，直接类名。方法调用。
 * `ASSERT_ERROR_CODE = 1`：断言失败统一错误码。
 * @author 一只朱
 * @date 2026-08-23 15:24
 *
 * "Run the code. Run the world."
 */

public class AssertUtil {
    public static final int ASSERT_ERROR_CODE = 1;

    private AssertUtil(){}

    //要求条件必须为 true；条件为 false 就抛业务异常
    public static void isTrue(boolean condition, String message){
        if (!condition) throw new BusinessException(ASSERT_ERROR_CODE,message);
    }

    //对象不能为 null，如果是 null 抛异常
    //⚠️注意：只判断`null`，对象是空字符串`""`、空集合`[]`不会拦截。
    public static void notNull(Object obj,String message) {
        if (obj == null) throw new BusinessException(ASSERT_ERROR_CODE,message);
    }

    //字符串不能是 null，也不能是空字符串
    public static void hasLength(String str, String message) {
        /**
         * Spring 工具 `StringUtils.hasLength()`：
         * - `true`：字符串不为 null，并且不是空串`""`
         * - `false`：`null` / `""`
         */
        if (!StringUtils.hasLength(str)) throw new BusinessException(ASSERT_ERROR_CODE,message);
    }

    //集合不能为 null，也不能是空集合(集合非 null、至少有 1 个元素)
    public static void notEmpty(Collection<?> col, String message) {
        if (col == null || col.isEmpty()) throw new BusinessException(ASSERT_ERROR_CODE,message);
    }

    /** 秒杀项目业务场景示例
     * // 1.校验id不能null
     * AssertUtil.notNull(seckillId,"秒杀id不能为空");
     * // 2.校验手机号
     * AssertUtil.hasLength(phone,"手机号不能为空");
     * // 3.校验库存>0
     * AssertUtil.isTrue(stock > 0,"商品库存不足");
     * //4.校验选中商品列表
     * AssertUtil.notEmpty(goodsIdList,"请选择要秒杀的商品");
     */
}
