package cn.net.zhu.seckill.business.util;

import com.alibaba.fastjson.JSON;
import org.apache.commons.text.StringEscapeUtils;

/**
 * JSON 工具
 *
 * @author 一只朱
 * @date 2026-09-07 16:03
 *
 * "Run the code. Run the world."
 */

public abstract class JsonUtil {

    private JsonUtil() {

    }

    /**
     * 将Redis中的json字符串转换成实体对象
     *
     * @param json   json字符串
     * @param tClass 实体对象
     * @param <T>    泛型
     * @return 实体对象
     */
    public static <T> T parseRedisEntity(String json, Class<T> tClass) {
        String subJson = parseUserJson(json);
        subJson = StringEscapeUtils.unescapeJava(subJson);
        subJson = subJson.substring(0, subJson.length() - 1);
        return JSON.parseObject(subJson, tClass);
    }

    private static String parseUserJson(String userJson) {
        //去掉多余的双引号
        return userJson.substring(1).substring(0, userJson.length() - 1);
    }
}
