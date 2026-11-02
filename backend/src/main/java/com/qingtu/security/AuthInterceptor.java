package com.qingtu.security;

import com.qingtu.common.BusinessException;
import com.qingtu.common.RequireRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || !(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        String token = resolveToken(request);
        if (token == null || token.isBlank()) {
            throw new BusinessException(401, "请先登录");
        }

        LoginUser user;
        try {
            user = jwtUtil.parseToken(token);
        } catch (Exception e) {
            throw new BusinessException(401, "登录状态已失效，请重新登录");
        }
        UserContext.set(user);

        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        if (requireRole != null && !Arrays.asList(requireRole.value()).contains(user.getRole())) {
            throw new BusinessException(403, "无权访问该功能");
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return header;
    }
}
