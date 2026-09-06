package cn.net.zhu.seckill.business.helper;

import cn.net.zhu.seckill.business.util.RedisUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 *  JWT生成与解析
 *
 * @author 一只朱
 * @date 2026-09-05 14:23
 *
 * "Run the code. Run the world."
 */

@RequiredArgsConstructor
@Slf4j
@Component
public class UserTokenHelper {
    /**
     * Token Redis key前缀：token:
     */
    private static final String TOKEN_PREFIX = "token:";

    /**
     * 用户信息Redis key前缀：user:
     */
    private static final String USER_PREFIX = "user:";

    /**
     * Redis工具类，用于缓存token、用户业务数据
     */
    private final RedisUtil redisUtil;

    /**
     * JWT签名密钥，配置文件 mall.mgt.tokenSecret，默认值123456test
     * <p>注意：生产环境务必修改，密钥泄露会导致可以伪造任意JWT令牌</p>
     */
    @Getter
    @Value("${mall.mgt.tokenSecret:123456test}")
    private String tokenSecret;

    /**
     * Token过期时间，单位：秒；配置文件 mall.mgt.tokenExpireTimeInRecord，默认3600秒 = 1小时
     * <p>同时用于JWT过期时间、Redis缓存过期时间</p>
     */
    @Value("${mall.mgt.tokenExpireTimeInRecord:3600}")
    private int tokenExpireTimeInRecord;


    /**
     * 生成JWT并存入Redis
     * (生成JWT登录令牌，并将token与用户业务JSON数据存入Redis)
     * @param username 用户名，存入JWT的subject字段
     * @param json 用户业务信息JSON字符串
     * @return JWT令牌字符串
     */
    public String generateToken(String username, String json) {
        String token = Jwts.builder()
                .setSubject(username)
                .setExpiration(new Date(System.currentTimeMillis() + tokenExpireTimeInRecord * 1000L))
                .signWith(SignatureAlgorithm.HS512, tokenSecret)
                .compact();
        redisUtil.set(getTokenKey(username), token, tokenExpireTimeInRecord);
        redisUtil.set(getTokenKey(username),json,tokenExpireTimeInRecord);
        return token;

    }

    /**
     *  从JWT令牌中提取用户名
     * @param token jwt令牌
     * @return 用户名；token无效/无subject时返回null
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) return null;
        return claims.getSubject();
    }

    /**
     * 解析JWT令牌获取Claims
     * (从 Token 中获取载荷声明)
     * @param token jwt令牌
     * @return JWT的Claims声明对象
     */
    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(tokenSecret)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取token的Redis key
     * @param username 用户名
     * @return token:username
     */
    public String getTokenKey(String username) {
        return String.format("%s%s", TOKEN_PREFIX, username);
    }

    /**
     * 获取用户业务信息的Redis key
     * @param username 用户名
     * @return user:username
     */
    public String getUserKey(String username) {
        return String.format("%s%s", USER_PREFIX, username);
    }
}
