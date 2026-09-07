package cn.net.zhu.seckill.business.util;


import cn.net.zhu.seckill.business.exception.BusinessException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author 一只朱
 * @date 2026-09-07 16:06
 *
 * "Run the code. Run the world."
 */

public abstract class Md5Util {

    private static final int MD5_LOOP_COUNT = 16;

    private Md5Util() {

    }


    /**
     * 获取md5值
     *
     * @param value 字符串
     * @return md5值
     */
    public static String md5(String value) {
        String string = null;
        char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
            md.update(value.getBytes());
            byte[] bytes = md.digest();
            char[] str = new char[2 * 16];
            int k = 0;

            for (int i = 0; i < MD5_LOOP_COUNT; i++) {
                byte b = bytes[i];
                //高4位
                str[k++] = hexArray[b >>> 4 & 0xf];
                //低4位
                str[k++] = hexArray[b & 0xf];
            }
            string = new String(str);
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException("生成MD5失败");
        }
        return string;
    }
}
