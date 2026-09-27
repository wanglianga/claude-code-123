package com.court.mediation.service;

import com.court.mediation.common.BizException;
import com.court.mediation.entity.User;
import com.court.mediation.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    public Map<String, Object> login(String username, String password) {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new BizException("账号不存在"));
        if (!Boolean.TRUE.equals(u.getEnabled())) throw new BizException("账号已停用");
        if (!u.getPassword().equals(password)) throw new BizException("密码错误");

        String token = Base64.getUrlEncoder().withoutPadding()
                .encodeToString((u.getId() + "|" + u.getUsername() + "|" + u.getRole()).getBytes());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", userInfo(u));
        return result;
    }

    public static Map<String, Object> userInfo(User u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("realName", u.getRealName());
        m.put("role", u.getRole());
        m.put("phone", u.getPhone());
        m.put("title", u.getTitle());
        m.put("specialties", u.getSpecialties());
        return m;
    }
}
