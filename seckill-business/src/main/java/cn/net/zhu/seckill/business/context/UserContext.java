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
    private static final ThreadLocal<UserEntity> USER_HOLDER = new TransmittableThreadLocal<>();

    public static UserEntity getCurrentUser() {
        return USER_HOLDER.get();
    }

    public static void setCurrentUser(UserEntity user) {
        USER_HOLDER.set(user);
    }

    public static void remove() {
        USER_HOLDER.remove();
    }
}
