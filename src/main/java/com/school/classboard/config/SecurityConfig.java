package com.school.classboard.config;

import com.school.classboard.domain.SysUser;
import com.school.classboard.repository.SysUserRepository;
import com.school.classboard.ui.LoginView;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * 安全配置。
 *
 * <p>默认所有页面需要登录；标注 {@code @AnonymousAllowed} 的页面（班牌首页、公告、课表）允许匿名访问，
 * 供教室内的班牌终端直接展示。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends VaadinWebSecurity {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);
        setLoginView(http, LoginView.class);
        // 允许以 GET 方式退出登录，便于界面上的“退出登录”按钮直接跳转
        http.logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
                .logoutSuccessUrl("/login")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll());
    }

    @Override
    public void configure(WebSecurity web) {
        // 上传的图片需要匿名可访问（班牌、新闻页在未登录时也要显示）
        web.ignoring().requestMatchers("/uploads/**");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** 从数据库 sys_user 表读取账号 */
    @Bean
    public UserDetailsService userDetailsService(SysUserRepository repository) {
        return username -> {
            SysUser user = repository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("用户不存在：" + username));
            return User.withUsername(user.getUsername())
                    .password(user.getPassword())
                    .roles(user.getRole())
                    .disabled(!user.isEnabled())
                    .build();
        };
    }
}
