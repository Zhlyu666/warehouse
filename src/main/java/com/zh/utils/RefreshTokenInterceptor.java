package com.zh.utils;

import com.zh.common.exception.UnauthorizedException;
import com.zh.domain.dto.UserDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.HandlerInterceptor;

@RequiredArgsConstructor
public class RefreshTokenInterceptor implements HandlerInterceptor {

    private final JwtTool jwtTool;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1.从请求头获取token（前端需携带 authorization 头）
        String token = request.getHeader("authorization");
        // 2.没有token则放行，交给LoginInterceptor决定是否拦截
        if (token == null || token.isBlank()) {
            return true;
        }
        // 3.解析token，成功则保存用户到ThreadLocal
        try {
            Long userId = jwtTool.parseToken(token);
            UserDTO userDTO = new UserDTO();
            userDTO.setId(userId);
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
