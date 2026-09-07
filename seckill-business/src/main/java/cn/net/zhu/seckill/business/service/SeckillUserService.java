package cn.net.zhu.seckill.business.service;

import cn.net.zhu.seckill.business.entity.user.SeckillUserEntity;
import cn.net.zhu.seckill.business.entity.user.UserRegisterEntity;

/**
 * 秒杀用户服务接口
 *
 * @author 一只朱
 * @date 2026-09-07 10:30
 *
 * "Run the code. Run the world."
 */

public interface SeckillUserService {

    /**
     * 用户注册
     *
     * @param userRegisterEntity 用户注册信息
     * @return 注册结果
     */
    boolean register(UserRegisterEntity userRegisterEntity);

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    SeckillUserEntity findByUsername(String username);

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号
     * @return 用户信息
     */
    SeckillUserEntity findByPhone(String phone);

    /**
     * 根据ID查询用户
     *
     * @param id 用户ID
     * @return 用户信息
     */
    SeckillUserEntity findById(Long id);

    /**
     * 更新最后登录时间
     *
     * @param id 用户ID
     * @return 更新结果
     */
    boolean updateLastLoginTime(Long id);

    /**
     * 验证手机验证码（Mock实现）
     *
     * @param phone 手机号
     * @param code 验证码
     * @return 验证结果
     */
    boolean validatePhoneCode(String phone, String code);
}
