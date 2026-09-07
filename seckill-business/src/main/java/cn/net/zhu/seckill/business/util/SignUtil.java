package cn.net.zhu.seckill.business.util;


import cn.hutool.core.bean.BeanUtil;
import cn.net.zhu.seckill.business.entity.SignEntity;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 *
 * @author 一只朱
 * @date 2026-09-07 16:05
 *
 * "Run the code. Run the world."
 */

public abstract class SignUtil {

    private static final int SIGN_TIME_OUT = 15 * 60 * 1000;

    private SignUtil() {
    }

    /**
     * 生成签名
     *
     * @param signEntity 实体类
     * @param secretKey  密钥
     */
    public static void makeSign(SignEntity signEntity, String secretKey) {
        long timestamp = System.currentTimeMillis();
        Map<String, Object> paramMap = BeanUtil.beanToMap(signEntity, false, true);
        paramMap.put("timestamp", timestamp);
        String sign = getSign(paramMap, secretKey);
        signEntity.setTimestamp(timestamp);
        signEntity.setSign(sign);
    }

    /**
     * 校验签名
     *
     * @param signEntity 参数
     * @param secretKey  密钥
     */
    public static void checkSign(SignEntity signEntity, String secretKey) {
        String sign = signEntity.getSign();
        signEntity.setSign(null);
        Map<String, Object> paramMap = BeanUtil.beanToMap(signEntity, false, true);
        checkSign(paramMap, sign, secretKey);
    }

    /**
     * 校验签名
     *
     * @param paramMap  参数
     * @param secretKey 密钥
     */
    public static void checkSign(Map<String, Object> paramMap, String sign, String secretKey) {
        Long timestamp = (Long) paramMap.get("timestamp");

        AssertUtil.notNull(timestamp, "timestamp不能为空");
        AssertUtil.isTrue(StringUtils.hasLength(sign), "sign不能为空");
        AssertUtil.isTrue(System.currentTimeMillis() - timestamp < SIGN_TIME_OUT, "该请求已过期");

        String newSign = getSign(paramMap, secretKey);
        AssertUtil.isTrue(sign.equals(newSign), "签名错误");
    }

    /**
     * 获取签名方法
     *
     * @param param     参数
     * @param secretKey 密钥
     * @return 签名
     */
    public static String getSign(Map<String, Object> param, String secretKey) {
        List<String> keys = new ArrayList<>();
        param.forEach((k, v) -> {
            if (Objects.isNull(v)) {
                return;
            }

            if (v instanceof String) {
                if (StringUtils.hasLength((String) v)) {
                    keys.add(k);
                }
            } else {
                keys.add(k);
            }
        });
        Collections.sort(keys);
        StringBuilder signBuilder = new StringBuilder();
        for (String key : keys) {
            signBuilder.append(key).append("=").append(param.get(key)).append("&");
        }
        signBuilder.append("secretKey=").append(secretKey);
        return Md5Util.md5(signBuilder.toString());
    }

}
