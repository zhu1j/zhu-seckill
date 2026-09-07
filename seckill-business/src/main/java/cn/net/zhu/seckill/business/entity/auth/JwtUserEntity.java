package cn.net.zhu.seckill.business.entity.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

/**
 *  JWT用户实体类
 *
 * @author 一只朱
 * @date 2026-09-07 10:33
 *
 * "Run the code. Run the world."
 */

@NoArgsConstructor
@AllArgsConstructor
@Data
public class JwtUserEntity implements UserDetails {

    private Long id;
    private String username;
    @JsonIgnore
    private String password;
    private List<SimpleGrantedAuthority> authorities;
    /**
     * 角色信息
     */
    private List<String> roles;

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
