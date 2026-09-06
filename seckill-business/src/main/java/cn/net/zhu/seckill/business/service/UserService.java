package cn.net.zhu.seckill.business.service;

import cn.net.zhu.seckill.business.entity.auth.AuthUserEntity;
import cn.net.zhu.seckill.business.entity.auth.CaptchaEntity;
import cn.net.zhu.seckill.business.entity.auth.TokenEntity;

/**
 * 用户服务接口
 *
 * @author 一只朱
 * @date 2026-09-05 13:06
 *
 * "Run the code. Run the world."
 */

public interface UserService {
    /**
     * 校验验证码是否正确
     *
     * @param uuid uuid
     * @param code 验证码
     */
    void checkCode(String uuid, String code);
    /**
     * 删除缓存中的验证码
     *
     * @param uuid uuid
     */
    void deleteCode(String uuid);
    /**
     * 用户登录接口
     *
     * @param authUserEntity 用户信息
     * @return token信息
     */
    TokenEntity login(AuthUserEntity authUserEntity);
    /**
     * 获取验证码
     *
     * @return 验证码实体
     */
    CaptchaEntity getCode();
}
