package cn.net.zhu.seckill.business.util;


import cn.net.zhu.seckill.business.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * 重试工具类
 *
 * @author 一只朱
 * @date 2026-09-07 16:05
 *
 * "Run the code. Run the world."
 */

@Slf4j
public abstract class ReTryUtil {

    private static final int DEFAULT_COUNT = 3;

    private ReTryUtil() {

    }

    /**
     * 重试3次数
     *
     * @param supplier 操作
     * @param <T>      泛型
     * @return 返回值
     */
    public static <T> T retry(Supplier<T> supplier) {
        return retry(supplier, DEFAULT_COUNT);
    }

    /**
     * 重试指定次数
     *
     * @param supplier   操作
     * @param retryCount 重试次数
     * @param <T>        泛型
     * @return 返回值
     */
    public static <T> T retry(Supplier<T> supplier, int retryCount) {
        int count = 0;
        while (count < retryCount) {
            try {
                return supplier.get();
            } catch (Exception e) {
                log.warn("失败原因：", e);
                count++;

                try {
                    Thread.sleep(50);
                    log.info(String.format("开始第%s次重试", count));
                } catch (InterruptedException ex) {
                    log.info(String.format("开始第%s次重试，唤醒失败", count));
                }
            }
        }

        if (count >= retryCount) {
            throw new BusinessException(String.format("经过%s次重试之后，该操作还是失败", count));
        }
        return null;
    }
}
