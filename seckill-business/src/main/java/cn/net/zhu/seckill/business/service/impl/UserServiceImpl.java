package cn.net.zhu.seckill.business.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.net.zhu.seckill.business.entity.auth.AuthUserEntity;
import cn.net.zhu.seckill.business.entity.auth.CaptchaEntity;
import cn.net.zhu.seckill.business.entity.auth.TokenEntity;
import cn.net.zhu.seckill.business.entity.user.UserEntity;
import cn.net.zhu.seckill.business.exception.BusinessException;
import cn.net.zhu.seckill.business.helper.UserTokenHelper;
import cn.net.zhu.seckill.business.service.UserService;
import cn.net.zhu.seckill.business.util.AssertUtil;
import cn.net.zhu.seckill.business.util.PasswordUtil;
import cn.net.zhu.seckill.business.util.RedisUtil;
import com.alibaba.fastjson.JSON;
import com.wf.captcha.ArithmeticCaptcha;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;

/**
 * 用户服务实现类
 * (负责验证码生成校验、用户登录业务逻辑，秒杀系统登录模块)
 *
 * @author 一只朱
 * @date 2026-09-05 15:16
 *
 * "Run the code. Run the world."
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    /**
     * Redis工具类，缓存验证码、注册用户信息
     */
    private final RedisUtil redisUtil;

    /**
     * 密码工具类，RSA密码解密
     */
    private final PasswordUtil passwordUtil;

    /**
     * BCrypt密码加密比对工具
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * JWT Token工具帮助类，生成令牌
     */
    private final UserTokenHelper userTokenHelper;

    /**
     * Redis验证码key前缀：captcha:
     */
    private static final String CAPTCHA_PREFIX = "captcha:";

    /**
     * Redis注册用户缓存key前缀：registerUser:
     * 缓存注册后的用户信息，登录时直接读取，减轻DB压力（秒杀场景）
     */
    private static final String REGISTER_USER_PREFIX = "registerUser:";

    /**
     * 验证码过期时间，单位秒；配置 seckill.api.captchaExpireSecond，默认60秒
     */
    @Value("${seckill.api.captchaExpireSecond:60}")
    private int captchaExpireSecond;

    /**
     * Token过期时间，单位秒；配置 mall.mgt.tokenExpireTimeInRecord，默认3600秒(1小时)
     */
    @Value("${mall.mgt.tokenExpireTimeInRecord:3600}")
    private int tokenExpireTimeInRecord;

    /**
     * 校验图形验证码
     * @param uuid 验证码唯一标识
     * @param code 用户输入验证码
     */
    @Override
    public void checkCode(String uuid, String code) {
        // 参数非空校验
        AssertUtil.isTrue(StringUtils.hasLength(uuid), "验证码标识不能为空");
        AssertUtil.isTrue(StringUtils.hasLength(code), "验证码不能为空");
        // 根据uuid从Redis获取标准答案
        String redisCode = redisUtil.get(getCaptchaKey(uuid));
        AssertUtil.hasLength(redisCode, "验证码已过期");
        // 比对验证码，去除首尾空格
        if (!redisCode.trim().equals(code.trim())) {
            throw new BusinessException("验证码错误");
        }
    }

    /**
     * 删除Redis中的验证码
     * @param uuid 验证码唯一标识
     */
    @Override
    public void deleteCode(String uuid) {
        redisUtil.del(getCaptchaKey(uuid));
    }

    /**
     * 用户登录
     * @param authUserEntity 登录请求参数：用户名、RSA加密密码、验证码uuid、验证码
     * @return TokenEntity 返回令牌实体，包含用户名、token、权限集合、过期时间
     */
    @Override
    public TokenEntity login(AuthUserEntity authUserEntity) {
        try {
            // 1.校验算术图形验证码，防止暴力密码破解
            checkCode(authUserEntity.getUuid(), authUserEntity.getCode());

            // 2.从Redis缓存读取用户信息（秒杀设计：用户注册后预存入Redis，登录不走数据库）
            String username = authUserEntity.getUsername();
            String json = redisUtil.get(REGISTER_USER_PREFIX + username);
            if (!StringUtils.hasLength(json)) {
                throw new BusinessException("该用户不存在");
            }
            UserEntity userEntity = JSON.parseObject(json, UserEntity.class);

            // 3.RSA解密前端传过来的密码，再使用BCrypt比对密文
            String decodePassword = passwordUtil.decodeRsaPassword(authUserEntity);
            if (!passwordEncoder.matches(decodePassword, userEntity.getPassword())) {
                throw new BusinessException("密码错误");
            }

            // 4.校验全部通过，生成JWT令牌，同时把token、用户信息写入Redis
            String token = userTokenHelper.generateToken(username, JSON.toJSONString(userEntity));

            // 5.登录成功，销毁已使用验证码，防止重复使用
            redisUtil.del(getCaptchaKey(authUserEntity.getUuid()));

            // 返回token实体，权限集合暂时为空集合
            return new TokenEntity(username, token, new ArrayList<>(), tokenExpireTimeInRecord);
        } catch (BusinessException e) {
            // 业务异常直接抛出，交给全局异常处理器处理
            throw e;
        } catch (Exception e) {
            // 系统未知异常，打印日志，包装为业务异常抛出
            log.error("登录失败", e);
            throw new BusinessException("登录失败：" + e.getMessage());
        }
    }

    /**
     * 获取算术图形验证码
     * @return CaptchaEntity 验证码返回实体：uuid、base64图片
     */
    @Override
    public CaptchaEntity getCode() {
        // 创建算术验证码：宽111，高36，两位数算术运算
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(111, 36);
        captcha.setLen(2);
        // 获取运算结果答案
        String result = String.valueOf((int) Double.parseDouble(captcha.text()));
        // 生成唯一uuid作为验证码key标识
        String uuid = IdUtil.simpleUUID();
        // 验证码答案存入Redis，设置过期时间
        redisUtil.set(getCaptchaKey(uuid), result, captchaExpireSecond);
        // 返回uuid + base64图片，前端根据uuid提交验证码
        return new CaptchaEntity(uuid, captcha.toBase64());
    }

    /**
     * 拼接验证码Redis完整key
     * @param uuid 验证码唯一id
     * @return captcha:xxxxxx
     */
    private String getCaptchaKey(String uuid) {
        return String.format("%s%s", CAPTCHA_PREFIX, uuid);
    }
}

/*
====================业务总结====================
1、这是秒杀系统登录核心Service，包含获取验证码、校验验证码、登录三大能力；
2、验证码为算术验证码，uuid关联答案存Redis，设置较短过期时间，用于防密码暴力刷接口；
3、登录不查询数据库，直接读取Redis缓存的注册用户信息，适配秒杀高并发场景；
4、密码传输全程RSA加密，后端解密后再用BCrypt比对密文，密码不会明文在网络传输；
5、登录成功调用UserTokenHelper生成JWT，同时把token、用户信息写入Redis；使用完毕立即销毁验证码；
6、异常处理：业务异常直接抛出，其他异常捕获打印日志后包装抛出，统一交给全局异常处理器；
7、设计特点：
   - 验证码用完即删，不可重复使用；
   - 用户信息放Redis，减轻DB压力，适配秒杀流量；
   - 配合JwtTokenFilter过滤器完成后续请求鉴权；
8、潜在注意点：
   - registerUser:username 需要注册业务主动往Redis写用户数据，否则登录直接报用户不存在；
   - 当前登录逻辑：同一个账号新登录会覆盖Redis里面旧token，实现单端登录，旧设备失效。
*/