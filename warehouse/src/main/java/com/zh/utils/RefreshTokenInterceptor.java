package com.zh.utils;

import cn.hutool.core.util.StrUtil;
import com.zh.common.exception.UnauthorizedException;
import com.zh.domain.dto.UserDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import static com.zh.utils.RedisConstants.SESSION_KEY_PREFIX;

@RequiredArgsConstructor
public class RefreshTokenInterceptor implements HandlerInterceptor {

    private final JwtTool jwtTool;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1.从请求头获取token（前端需携带 authorization 头）
        String token = request.getHeader("authorization");
        // 2.没有token则放行，交给LoginInterceptor决定是否拦截
        if (StrUtil.isBlank(token)) {
            return true;
        }
        // 3.解析token，成功则保存用户到ThreadLocal
        try {
            Long userId = jwtTool.parseToken(token);
            // 4.校验Redis会话并取出会话中的用户展示名（登出后会话被删除，JWT即使未过期也视为未登录）
            String sessionUserName = stringRedisTemplate.opsForValue().get(SESSION_KEY_PREFIX + token);
            if (StrUtil.isBlank(sessionUserName)) {
                return true;
            }
            UserDTO userDTO = new UserDTO();
            userDTO.setId(userId);
            userDTO.setUsername(sessionUserName);
            UserHolder.saveUser(userDTO);
        } catch (UnauthorizedException e) {
            // token无效：不保存用户，放行后由LoginInterceptor返回401
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 4.请求结束清理ThreadLocal，防止线程复用导致数据串号
        UserHolder.removeUser();
    }
}
