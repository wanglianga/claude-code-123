package com.court.mediation.security;

import com.court.mediation.common.BizException;
import com.court.mediation.entity.User;
import com.court.mediation.repo.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 简化的 Token 认证：登录成功后颁发 userId:username:role 形式的 Base64 token。
 * 演示系统不引入完整安全框架，前端在 Header 中携带 X-Token。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = request.getHeader("X-Token");
        if (token == null || token.isBlank()) {
            token = request.getParameter("token");
        }
        if (token == null || token.isBlank()) {
            throw new BizException("未登录或登录已过期");
        }
        String decoded;
        try {
            decoded = new String(java.util.Base64.getUrlDecoder().decode(token));
        } catch (IllegalArgumentException e) {
            throw new BizException("无效的登录凭证");
        }
        String[] parts = decoded.split("\\|");
        if (parts.length < 3) {
            throw new BizException("无效的登录凭证");
        }
        Long userId = Long.valueOf(parts[0]);
        User user = userRepository.findById(userId)
                .filter(User::getEnabled)
                .orElseThrow(() -> new BizException("用户不存在或已停用"));
        CurrentUser.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        CurrentUser.clear();
    }
}
