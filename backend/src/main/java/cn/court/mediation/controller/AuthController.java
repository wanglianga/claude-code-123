package cn.court.mediation.controller;

import cn.court.mediation.common.ApiResponse;
import cn.court.mediation.common.BizException;
import cn.court.mediation.entity.User;
import cn.court.mediation.repository.UserRepository;
import cn.court.mediation.security.AuthTokenStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final AuthTokenStore tokenStore;

    public AuthController(UserRepository userRepository, AuthTokenStore tokenStore) {
        this.userRepository = userRepository;
        this.tokenStore = tokenStore;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BizException("账号不存在"));
        if (!user.getPassword().equals(password)) {
            throw new BizException("密码错误");
        }
        String token = tokenStore.issue(user.getId());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", userView(user));
        return ApiResponse.ok(result);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            tokenStore.revoke(auth.substring(7));
        }
        return ApiResponse.ok();
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(HttpServletRequest request) {
        return ApiResponse.ok(userView((User) request.getAttribute("currentUser")));
    }

    static Map<String, Object> userView(User u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("realName", u.getRealName());
        m.put("phone", u.getPhone());
        m.put("role", u.getRole().name());
        m.put("roleLabel", u.getRole().getLabel());
        m.put("expertise", u.getExpertise());
        m.put("mediationRoom", u.getMediationRoom());
        m.put("capacity", u.getCapacity());
        return m;
    }
}
