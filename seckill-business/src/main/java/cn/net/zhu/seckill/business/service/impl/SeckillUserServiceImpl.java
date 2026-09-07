package cn.net.zhu.seckill.business.service.impl;

import cn.net.zhu.seckill.business.entity.user.SeckillUserEntity;
import cn.net.zhu.seckill.business.entity.user.UserRegisterEntity;
import cn.net.zhu.seckill.business.exception.BusinessException;
import cn.net.zhu.seckill.business.helper.IdGenerateHelper;
import cn.net.zhu.seckill.business.mapper.user.SeckillUserMapper;
import cn.net.zhu.seckill.business.service.SeckillUserService;
import cn.net.zhu.seckill.business.util.AssertUtil;
import cn.net.zhu.seckill.business.util.FillUserUtil;
import cn.net.zhu.seckill.business.util.PasswordUtil;
import cn.net.zhu.seckill.business.util.RedisUtil;
import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;

import static cn.net.zhu.seckill.business.util.AssertUtil.ASSERT_ERROR_CODE;

/**
 * 秒杀用户服务实现类
 *
 * @author 一只朱
 * @date 2026-09-07 10:31
 *
 * "Run the code. Run the world."
 */

@RequiredArgsConstructor
@Slf4j
@Service
public class SeckillUserServiceImpl implements SeckillUserService {

    private static final String REGISTER_USER_PREFIX = "registerUser:";
    private static final String PHONE_CODE_PREFIX = "phoneCode:";
    
    private final SeckillUserMapper seckillUserMapper;
    private final RedisUtil redisUtil;
    private final PasswordEncoder passwordEncoder;
    private final PasswordUtil passwordUtil;
    private final IdGenerateHelper idGenerateHelper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean register(UserRegisterEntity userRegisterEntity) {
        // 参数校验
        validateRegisterParams(userRegisterEntity);
        
        // 检查用户名是否已存在
        SeckillUserEntity existUser = seckillUserMapper.findByUsername(userRegisterEntity.getUsername());
        AssertUtil.isTrue(existUser == null, "用户名已存在");
        
        // 检查手机号是否已存在
        existUser = seckillUserMapper.findByPhone(userRegisterEntity.getPhone());
        AssertUtil.isTrue(existUser == null, "手机号已被注册");
        
        // 验证手机验证码
        AssertUtil.isTrue(validatePhoneCode(userRegisterEntity.getPhone(), userRegisterEntity.getPhoneCode()), 
                "手机验证码错误");
        
        try {
            // 解密密码 - 创建临时AuthUserEntity用于解密
            String decodePassword = userRegisterEntity.getPassword();
            if (StringUtils.hasLength(decodePassword)) {
                // 如果密码是加密的，需要解密
                // 这里简化处理，实际项目中可能需要判断是否加密
                try {
                    // 创建临时对象用于解密
                    cn.net.zhu.seckill.business.entity.auth.AuthUserEntity tempAuth = 
                        new cn.net.zhu.seckill.business.entity.auth.AuthUserEntity();
                    tempAuth.setPassword(userRegisterEntity.getPassword());
                    decodePassword = passwordUtil.decodeRsaPassword(tempAuth);
                } catch (Exception e) {
                    // 如果解密失败，可能密码未加密，直接使用原密码
                    log.warn("密码解密失败，使用原密码：{}", e.getMessage());
                }
            }
            
            // 创建用户实体
            SeckillUserEntity userEntity = new SeckillUserEntity();
            BeanUtils.copyProperties(userRegisterEntity, userEntity);
            userEntity.setPassword(passwordEncoder.encode(decodePassword));
            userEntity.setStatus(1); // 正常状态
            userEntity.setRegisterTime(new Date());
            
            // 填充基础字段
            FillUserUtil.fillCreateDefaultUserInfo(userEntity);
            userEntity.setId(idGenerateHelper.nextId());
            
            // 保存到数据库
            int result = seckillUserMapper.insert(userEntity);
            AssertUtil.isTrue(result > 0, "用户注册失败");
            
            // 保存到Redis缓存（兼容现有登录逻辑）
            saveUserToCache(userEntity);
            
            log.info("用户注册成功，用户名：{}", userRegisterEntity.getUsername());
            return true;
            
        } catch (Exception e) {
            log.error("用户注册失败：", e);
            if (e instanceof BusinessException) {
                throw e;
            }
            throw new BusinessException(ASSERT_ERROR_CODE, "用户注册失败");
        }
    }

    @Override
    public SeckillUserEntity findByUsername(String username) {
        AssertUtil.isTrue(StringUtils.hasLength(username), "用户名不能为空");
        return seckillUserMapper.findByUsername(username);
    }

    @Override
    public SeckillUserEntity findByPhone(String phone) {
        AssertUtil.isTrue(StringUtils.hasLength(phone), "手机号不能为空");
        return seckillUserMapper.findByPhone(phone);
    }

    @Override
    public SeckillUserEntity findById(Long id) {
        AssertUtil.isTrue(id != null && id > 0, "用户ID不能为空");
        return seckillUserMapper.findById(id);
    }

    @Override
    public boolean updateLastLoginTime(Long id) {
        AssertUtil.isTrue(id != null && id > 0, "用户ID不能为空");
        return seckillUserMapper.updateLastLoginTime(id) > 0;
    }

    @Override
    public boolean validatePhoneCode(String phone, String code) {
        // Mock实现：简单的验证码验证
        // 在实际项目中，这里应该调用短信服务提供商的API进行验证
        if (!StringUtils.hasLength(phone) || !StringUtils.hasLength(code)) {
            return false;
        }
        
        // Mock验证码规则：手机号后4位作为验证码
        String mockCode = phone.substring(phone.length() - 4);
        return mockCode.equals(code);
    }

    /**
     * 验证注册参数
     */
    private void validateRegisterParams(UserRegisterEntity userRegisterEntity) {
        AssertUtil.isTrue(userRegisterEntity != null, "注册信息不能为空");
        AssertUtil.isTrue(StringUtils.hasLength(userRegisterEntity.getUsername()), "用户名不能为空");
        AssertUtil.isTrue(StringUtils.hasLength(userRegisterEntity.getPassword()), "密码不能为空");
        AssertUtil.isTrue(StringUtils.hasLength(userRegisterEntity.getConfirmPassword()), "确认密码不能为空");
        AssertUtil.isTrue(StringUtils.hasLength(userRegisterEntity.getPhone()), "手机号不能为空");
        AssertUtil.isTrue(StringUtils.hasLength(userRegisterEntity.getPhoneCode()), "手机验证码不能为空");
        AssertUtil.isTrue(StringUtils.hasLength(userRegisterEntity.getCode()), "图形验证码不能为空");
        
        // 验证手机号格式
        AssertUtil.isTrue(userRegisterEntity.getPhone().matches("^1[3-9]\\d{9}$"), "手机号格式不正确");
        
        // 验证用户名长度
        AssertUtil.isTrue(userRegisterEntity.getUsername().length() >= 3 && 
                userRegisterEntity.getUsername().length() <= 20, "用户名长度应在3-20个字符之间");
    }

    /**
     * 保存用户信息到Redis缓存（兼容现有登录逻辑）
     */
    private void saveUserToCache(SeckillUserEntity userEntity) {
        try {
            // 构造兼容现有登录逻辑的用户信息
            String userJson = JSON.toJSONString(userEntity);
            String cacheKey = REGISTER_USER_PREFIX + userEntity.getUsername();
            
            // 设置缓存，过期时间为7天
            redisUtil.set(cacheKey, userJson, 7 * 24 * 60 * 60);
            
            log.info("用户信息已保存到缓存，用户名：{}", userEntity.getUsername());
        } catch (Exception e) {
            log.error("保存用户信息到缓存失败：", e);
            // 不抛出异常，避免影响注册流程
        }
    }
}