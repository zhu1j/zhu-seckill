package cn.net.zhu.seckill.business.filter;

import cn.net.zhu.seckill.business.context.UserContext;
import cn.net.zhu.seckill.business.entity.user.UserEntity;
import cn.net.zhu.seckill.business.helper.UserTokenHelper;
import cn.net.zhu.seckill.business.util.RedisUtil;
import cn.net.zhu.seckill.business.util.SpringUtil;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 *  JWT 令牌过滤器类
 *  (拦截所有请求，完成JWT令牌校验、Redis二次校验、用户上下文设置)
 *
 * @author 一只朱
 * @date 2026-08-23 03:02
 *
 * "Run the code. Run the world."
 */

@Slf4j
public class JwtTokenFilter implements Filter {

    public static final String LOGIN_AGAN_INFO = "您登录状态已过期，为了保护您的账户安全，请重新登录";
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;

        // 从请求头Authorization中获取token
        String token = request.getHeader("Authorization");

        // 兼容前端 "Basic@" 前缀写法：剥离前缀还原为原始JWT
        if (StringUtils.hasLength(token) && token.startsWith("Basic@")) {
            token = token.substring("Basic@".length());
        }

        // 无token或未登录产生的垃圾值直接放行（登录、注册等公开接口走这里）
        if (!StringUtils.hasLength(token) || "null".equals(token) || "undefined".equals(token)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        try {
            // 从Spring上下文获取工具Bean（Filter不属于Spring容器，不能直接@Autowired注入）
            UserTokenHelper userTokenHelper = SpringUtil.getBean(UserTokenHelper.class);
            RedisUtil redisUtil = SpringUtil.getBean(RedisUtil.class);

            // 解析JWT令牌，获取用户名(subject)
            String username = userTokenHelper.getUsernameFromToken(token);
            if (!StringUtils.hasLength(username)) {
                throw new RuntimeException("token无效");
            }

            // 根据用户名拼接key，读取Redis中保存的token
            String redisToken = redisUtil.get(userTokenHelper.getTokenKey(username));
            if (!StringUtils.hasLength(redisToken)) {
                throw new RuntimeException("token已过期");
            }

            // 比对前端传过来的token和Redis缓存token是否一致，去除可能存在的引号空格干扰
            if (!token.equals(redisToken.replace("\"", "").trim())) {
                throw new RuntimeException("token不匹配");
            }

            // 从Redis读取缓存的用户业务JSON，转为用户实体对象
            String userJson = redisUtil.get(userTokenHelper.getUserKey(username));
            UserEntity userEntity = JSON.parseObject(userJson, UserEntity.class);

            // 将当前登录用户存入ThreadLocal，后续Controller/Service直接获取登录用户
            UserContext.setCurrentUser(userEntity);

            // 校验全部通过，放行执行业务接口
            filterChain.doFilter(servletRequest, servletResponse);
        } catch (Exception e) {
            // JWT解析异常、Redis校验失败、token不匹配都会进入此处
            log.error("JWT认证失败: {}", e.getMessage());
            // 认证失败依旧放行，交给接口内部判断是否需要登录；也可此处直接返回401响应
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            // 【必须清理】清除ThreadLocal，防止线程复用造成用户信息泄露、串用户问题
            UserContext.remove();
        }
    }
}
/*
====================业务总结====================
1、该Filter是Servlet原生过滤器，对全部http请求进行拦截鉴权；
2、无token直接放行，留给登录注册等公开接口使用；
3、有token时：先解析JWT拿到用户名，再做Redis二次校验，弥补JWT无法主动作废的缺陷；
   - Redis不存在token → token过期/已登出
   - Redis内token和前端token不一致 → 异地挤下线、旧token失效
4、校验全部通过，把用户对象放入ThreadLocal，业务代码直接拿登录用户，不用重复解析token；
5、finally强制清空ThreadLocal，防止线程池复用线程引发用户上下文错乱；
6、缺陷：认证失败依旧放行，不会直接返回401；需要配合接口内部判断UserContext是否有用户，区分是否需要登录；
7、适用秒杀系统：支持主动登出（删除Redis key即可让token立刻失效）；单账号新登录会覆盖旧token，实现单设备登录，旧设备直接鉴权失败。
*/
