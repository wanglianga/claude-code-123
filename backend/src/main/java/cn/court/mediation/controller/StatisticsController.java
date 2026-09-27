package cn.court.mediation.controller;

import cn.court.mediation.common.ApiResponse;
import cn.court.mediation.entity.User;
import cn.court.mediation.enums.Role;
import cn.court.mediation.security.CurrentUser;
import cn.court.mediation.repository.UserRepository;
import cn.court.mediation.service.StatisticsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final UserRepository userRepository;

    public StatisticsController(StatisticsService statisticsService, UserRepository userRepository) {
        this.statisticsService = statisticsService;
        this.userRepository = userRepository;
    }

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.ok(statisticsService.dashboard());
    }

    @GetMapping("/todos")
    public ApiResponse<Map<String, Object>> todos(HttpServletRequest request) {
        return ApiResponse.ok(statisticsService.todos(CurrentUser.get(request)));
    }

    /** 调解员名册（窗口分流选择用） */
    @GetMapping("/mediators")
    public ApiResponse<List<User>> mediators() {
        return ApiResponse.ok(userRepository.findByRole(Role.MEDIATOR));
    }
}
