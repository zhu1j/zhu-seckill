package cn.net.zhu.seckill.api.controller;

import cn.net.zhu.seckill.business.context.UserContext;
import cn.net.zhu.seckill.business.entity.user.UserEntity;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Objects;

/**
 * 用户信息控制器
 * (获取当前登录用户接口，依赖JwtTokenFilter过滤器完成鉴权，从ThreadLocal获取登录用户上下文)
 *
 * @author 一只朱
 * @date 2026-09-05 18:08
 *
 * "Run the code. Run the world."
 */

@Api(tags = "用户操作")
@RestController
@RequestMapping("")
public class UserController {

    /**
     * 获取当前登录用户信息
     * @return UserEntity 当前登录用户；未登录返回null；返回时清空密码字段，防止密码泄露到前端
     */
    @GetMapping("/getUserInfo")
    public UserEntity getUserInfo() {
        // 从ThreadLocal上下文取出登录用户（由JwtTokenFilter过滤器解析token后设置）
        UserEntity currentUser = UserContext.getCurrentUser();
        if (Objects.nonNull(currentUser)) {
            // 置空密码，避免密码明文返回给前端，安全防护
            currentUser.setPassword(null);
        }
        return currentUser;
    }
}

/*
====================业务总结====================
1、获取登录用户信息接口，需要携带有效JWT Token请求头，经过JwtTokenFilter过滤器鉴权；
2、用户对象存放在ThreadLocal的UserContext，过滤器校验成功后写入，finally会清理；
3、如果未登录：过滤器不会设置用户，接口直接返回null，前端据此判断跳转登录页；
4、安全处理：返回用户实体前强制把password置空，防止敏感密码字段泄露；
5、调用链路：前端请求携带Authorization token → JwtTokenFilter解析+Redis校验 → 设置UserContext → /getUserInfo读取上下文返回用户信息；
6、注意：该接口本身没有写鉴权判断，依靠过滤器；未登录直接返回null，不会返回401状态码，前端需要做null判断。
*/

