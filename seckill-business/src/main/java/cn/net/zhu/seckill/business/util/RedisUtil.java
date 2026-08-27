package cn.net.zhu.seckill.business.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 *  Redis 工具类
 *
 * @author 一只朱
 * @date 2026-08-23 15:23
 *
 * "Run the code. Run the world."
 */

@Slf4j
@Component
public class RedisUtil {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 带过期时间的设置
    public boolean set(String key, String value, long expireTime) {
        try {
            stringRedisTemplate.opsForValue().set(key, value,expireTime, TimeUnit.SECONDS);
            return true;
        } catch (Exception e){
            log.error("Redis set error: key={}",key,e);
            return false;
        }
    }

    //不带过期时间的设置(永久写入，无过期)
    public boolean set(String key,String value) {
        try {
            stringRedisTemplate.opsForValue().set(key, value);
            return true;
        }catch (Exception e){
            log.error("Redis set error: key={}",key,e);
            return false;
        }
    }

    //获取值
    public String get(String key){
        try {
            return stringRedisTemplate.opsForValue().get(key);
        }catch (Exception e) {
            log.error("Redis get error: key={}",key,e);
            return null;
        }
    }

    //原子递增
    public Long increment(String key,long value){
        try{
            return stringRedisTemplate.opsForValue().increment(key,value);
        } catch (Exception e){
            log.error("Redis increment error: key={}",key,e);
            return null;
        }
    }

    public Long increment(String key){
        return increment(key,1);
    }

    //原子递减（秒杀核心操作）
    public Long decrement(String key){
        return  increment(key,-1);
    }

    //删除键
    public void del(String key){
        try {
            if (StringUtils.hasLength(key)) {
                stringRedisTemplate.delete(key);
            }
        } catch (Exception e) {
            log.error("Redis del error: key={}",key,e);
        }
    }
}
