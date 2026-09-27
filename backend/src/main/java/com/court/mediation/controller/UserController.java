package com.court.mediation.controller;

import com.court.mediation.common.BizException;
import com.court.mediation.common.R;
import com.court.mediation.entity.User;
import com.court.mediation.repo.UserRepository;
import com.court.mediation.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    /** 用户/调解员管理列表，可按角色过滤 */
    @GetMapping
    public R<List<Map<String, Object>>> list(@RequestParam(required = false) String role) {
        List<User> users = role == null || role.isBlank()
                ? userRepository.findAll()
                : userRepository.findByRoleAndEnabledTrue(role);
        return R.ok(users.stream().map(AuthService::userInfo).toList());
    }

    @PostMapping
    public R<Map<String, Object>> create(@RequestBody User u) {
        if (u.getUsername() == null || u.getUsername().isBlank()) throw new BizException("账号不能为空");
        if (userRepository.existsByUsername(u.getUsername())) throw new BizException("账号已存在");
        if (u.getPassword() == null || u.getPassword().isBlank()) u.setPassword("123456");
        if (u.getRole() == null) u.setRole("PARTY");
        if (u.getEnabled() == null) u.setEnabled(true);
        if (u.getRemote() == null) u.setRemote(false);
        return R.ok(AuthService.userInfo(userRepository.save(u)));
    }

    @PutMapping("/{id}")
    public R<Map<String, Object>> update(@PathVariable Long id, @RequestBody User u) {
        User db = userRepository.findById(id).orElseThrow(() -> new BizException("用户不存在"));
        if (u.getRealName() != null) db.setRealName(u.getRealName());
        if (u.getPassword() != null && !u.getPassword().isBlank()) db.setPassword(u.getPassword());
        if (u.getRole() != null) db.setRole(u.getRole());
        if (u.getPhone() != null) db.setPhone(u.getPhone());
        if (u.getTitle() != null) db.setTitle(u.getTitle());
        if (u.getSpecialties() != null) db.setSpecialties(u.getSpecialties());
        if (u.getRemote() != null) db.setRemote(u.getRemote());
        if (u.getEnabled() != null) db.setEnabled(u.getEnabled());
        return R.ok(AuthService.userInfo(userRepository.save(db)));
    }
}
