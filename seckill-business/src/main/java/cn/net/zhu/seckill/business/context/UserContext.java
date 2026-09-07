package cn.net.zhu.seckill.business.context;

import cn.net.zhu.seckill.business.entity.user.UserEntity;
import com.alibaba.ttl.TransmittableThreadLocal;

/**
 *  用户上下文类
 *
 * @author 一只朱
 * @date 2026-08-23 02:33
 *
 * "Run the code. Run the world."
 */

public class UserContext {
    private static final TransmittableThreadLocal<UserEntity> THREAD_LOCAL = new TransmittableThreadLocal<>();

    /**
     * 获取当前用户信息
     *
     * @return 当前用户信息
     */
    public static UserEntity getCurrentUser() {
        return THREAD_LOCAL.get();
    }

    /**
     * 设置当前用户信息
     *
     * @param userEntity 当前用户信息
     */
    public static void setCurrentUser(UserEntity userEntity) {
        THREAD_LOCAL.set(userEntity);
    }

    /**
     * 清空当前用户信息
     */
    public static void remove() {
        THREAD_LOCAL.remove();
    }
}
