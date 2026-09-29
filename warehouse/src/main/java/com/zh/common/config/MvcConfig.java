package com.zh.common.config;

import com.zh.utils.JwtTool;
import com.zh.utils.LoginInterceptor;
import com.zh.utils.RefreshTokenInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class MvcConfig implements WebMvcConfigurer {

    private final JwtTool jwtTool;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1.刷新拦截器：所有请求先尝试解析token存入UserHolder
        registry.addInterceptor(new RefreshTokenInterceptor(jwtTool, stringRedisTemplate))
                .addPathPatterns("/**")
                .order(0);
        // 2.登录拦截器：只负责校验UserHolder是否有用户
        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(
                        "/auth/login"
                ).order(1);
    }
}
