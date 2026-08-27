package cn.net.zhu.seckill.business.util;

import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.net.zhu.seckill.business.entity.auth.AuthUserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 *  密码工具类
 * @author 一只朱
 * @date 2026-08-27 22:09
 *
 * "Run the code. Run the world."
 */

@Component
public class PasswordUtil {
    @Value("${seckill.password.privateKey}")
    private String privateKey;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // BCrypt加密
    public String encode(String password) {
        return passwordEncoder.encode(password);
    }

    // RSA解密
    public String decodeRsaPassword(AuthUserEntity authUserEntity) {
        if (privateKey == null || privateKey.isEmpty()) {
            return authUserEntity.getPassword(); //无密钥时直接返回原密码
        }

        RSA rsa = new RSA(privateKey,null);
        byte[] decrypt = rsa.decrypt(authUserEntity.getPassword(), KeyType.PrivateKey);
        return new String(decrypt);
    }
}
