package cn.court.mediation.security;

import cn.court.mediation.entity.User;
import cn.court.mediation.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * 轻量令牌鉴权（演示用）：
 * 登录成功后服务端生成随机 token 与用户绑定；前端在 Authorization 头携带 Bearer token。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String TOKEN_MAP_ATTR = "authTokens";

    private final AuthTokenStore tokenStore;
    private final UserRepository userRepository;

    public AuthInterceptor(AuthTokenStore tokenStore, UserRepository userRepository) {
        this.tokenStore = tokenStore;
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return reject(response, "未登录或登录已失效");
        }
        String token = auth.substring(7);
        Optional<Long> userIdOpt = tokenStore.resolve(token);
        if (userIdOpt.isEmpty()) {
            return reject(response, "登录令牌无效，请重新登录");
        }
        Optional<User> userOpt = userRepository.findById(userIdOpt.get());
        if (userOpt.isEmpty()) {
            return reject(response, "用户不存在");
        }
        request.setAttribute("currentUser", userOpt.get());
        return true;
    }

    private boolean reject(HttpServletResponse response, String message) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\",\"data\":null}");
        return false;
    }
}
