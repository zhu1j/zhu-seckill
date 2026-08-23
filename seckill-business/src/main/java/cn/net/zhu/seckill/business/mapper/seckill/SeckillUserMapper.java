package cn.net.zhu.seckill.business.mapper.seckill;

import cn.net.zhu.seckill.business.entity.user.SeckillUserEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 秒杀用户 Mapper（秒杀系统用户数据查询、新增、更新操作）
 * @author 一只朱
 * @date 2026-08-24 03:33
 *
 * "Run the code. Run the world."
 */

@Mapper
public interface SeckillUserMapper {
    /**
     * 根据主键id查询用户信息
     * @param id 用户id
     * @return 秒杀用户实体
     */
    SeckillUserEntity findById(Long id);

    /**
     * 根据用户名查询用户信息
     * @param username 用户名
     * @return 秒杀用户实体
     */
    SeckillUserEntity findByUsername(String username);

    /**
     * 根据手机号查询用户信息
     * @param phone 手机号码
     * @return 秒杀用户实体
     */
    SeckillUserEntity findByPhone(String phone);

    /**
     * 新增秒杀系统用户
     * @param entity 秒杀用户实体
     * @return 受影响行数
     */
    int insert(SeckillUserEntity entity);

    /**
     * 更新用户最后登录时间
     * @param id 用户id
     * @return 受影响行数
     */
    int updateLastLoginTime(Long id);
}
