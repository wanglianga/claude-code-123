package com.court.mediation.controller;

import com.court.mediation.common.R;
import com.court.mediation.entity.User;
import com.court.mediation.repo.UserRepository;
import com.court.mediation.security.CurrentUser;
import com.court.mediation.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        return R.ok(authService.login(body.get("username"), body.get("password")));
    }

    @GetMapping("/me")
    public R<Map<String, Object>> me() {
        User u = CurrentUser.get();
        return R.ok(AuthService.userInfo(u));
    }
}
