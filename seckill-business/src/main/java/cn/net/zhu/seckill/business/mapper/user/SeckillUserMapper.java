package cn.net.zhu.seckill.business.mapper.user;

import cn.net.zhu.seckill.business.entity.user.SeckillUserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户Mapper接口
 * 操作用户表：查询、新增、更新、逻辑删除、更新登录时间
 *
 * @author 一只朱
 * @date 2026-09-06 16:39
 *
 * "Run the code. Run the world."
 */

@Mapper
public interface SeckillUserMapper {

    /**
     * 根据ID查询用户
     *
     * @param id 用户ID
     * @return 用户信息
     */
    SeckillUserEntity findById(@Param("id") Long id);

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    SeckillUserEntity findByUsername(@Param("username") String username);

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号
     * @return 用户信息
     */
    SeckillUserEntity findByPhone(@Param("phone") String phone);

    /**
     * 插入用户
     *
     * @param user 用户信息
     * @return 影响行数
     */
    int insert(SeckillUserEntity user);

    /**
     * 更新用户
     *
     * @param user 用户信息
     * @return 影响行数
     */
    int update(SeckillUserEntity user);

    /**
     * 根据ID列表批量逻辑删除用户
     *
     * @param ids 用户ID列表
     * @param entity 更新实体，携带逻辑删除字段（deleteFlag、deleteTime等）
     * @return 影响行数
     */
    int deleteByIds(@Param("ids") List<Long> ids, @Param("entity") SeckillUserEntity entity);

    /**
     * 更新用户最后登录时间
     *
     * @param id 用户ID
     * @return 影响行数
     */
    int updateLastLoginTime(@Param("id") Long id);
}

/*
====================业务总结====================
1、模块职责：MyBatis Mapper；秒杀系统用户表CRUD；支持主键、用户名、手机号查询；新增、全量更新、批量逻辑删除、单独更新登录时间。

2、调用链路：
‑ findById：根据用户ID获取用户信息；
‑ findByUsername：登录场景，用户名查用户；
‑ findByPhone：手机号登录、手机号注册查重；
‑ insert：用户注册；
‑ update：用户信息修改；
‑ deleteByIds：批量逻辑删除，传入id集合+更新实体携带删除标记；
‑ updateLastLoginTime：用户登录成功更新last_login_time。

3、接口说明：
‑ 三个查询方法：ID、用户名、手机号；用户名、手机号业务上应该建唯一索引，防止重复；
‑ insert 返回int受影响行数，用于判断注册是否成功；
‑ update 全字段更新，需要注意MyBatis xml里是否做动态字段更新，否则会把null覆盖数据库字段；
‑ deleteByIds：逻辑删除，不是物理delete；传入ids集合批量，entity用来携带deleteFlag、deleteTime；
‑ updateLastLoginTime：独立方法，只更新登录时间，避免大对象更新，轻量高效。

4、技术设计亮点：
‑ @Mapper注解，Spring扫描生成代理对象；
‑ @Param注解绑定参数名，适配xml；
‑ 区分普通update和专项updateLastLoginTime；高频登录场景只更新时间，不碰其他字段；
‑ 批量逻辑删除，支持多选ID批量处理。

5、风险点 & 潜在坑：
‑ findByUsername / findByPhone：数据库必须加唯一唯一索引；否则出现重复用户；代码层没有做强制约束。
‑ update方法：如果xml写的是 set 全部字段，对象内null字段会把数据库值覆盖为null；需要<if>动态标签。
‑ deleteByIds入参设计特殊：ids是要删除的id集合，entity放删除字段；xml要取出entity内部属性；容易写错OGNL表达式 `#{entity.deleteFlag}`。
‑ deleteByIds只做逻辑删除；查询方法 findById/findByUsername/findByPhone **没有自动过滤已逻辑删除用户**；xml没有加 where delete_flag = 0；业务代码需要自己过滤已删除用户，否则会查到已删除账号。
‑ 没有分页查询列表方法；需要后台用户列表时要新增list查询。
‑ 没有防SQL注入风险，使用MyBatis #{} 占位符，本身安全。

6、模块关联：对应实体 SeckillUserEntity；xml映射文件 SeckillUserMapper.xml；被 SeckillUserService 调用。

7、生产注意事项：
‑ 用户表 username、phone 建立唯一索引，数据库层面防止重复注册；
‑ 查询方法xml全部追加条件 `where delete_flag = 0`，隔离逻辑删除数据；
‑ update 方法xml必须使用动态<if>，只更新非null字段，禁止全字段set；
‑ deleteByIds xml示例片段参考：
UPDATE seckill_user
SET delete_flag = #{entity.deleteFlag}, delete_time = #{entity.deleteTime}
WHERE id IN
<foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
‑ 登录查询优先使用id，用户名/手机号只用于登录校验；业务逻辑尽量使用userId做主键；
‑ updateLastLoginTime可以加乐观锁或者直接sql set last_login_time = now()，减少传参。
*/

