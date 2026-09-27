package cn.court.mediation.security;

import cn.court.mediation.entity.User;

/** 从请求属性中读取当前登录用户 */
public class CurrentUser {
    public static User get(jakarta.servlet.http.HttpServletRequest request) {
        return (User) request.getAttribute("currentUser");
    }

    public static Long id(jakarta.servlet.http.HttpServletRequest request) {
        User u = get(request);
        return u == null ? null : u.getId();
    }

    public static String name(jakarta.servlet.http.HttpServletRequest request) {
        User u = get(request);
        return u == null ? "系统" : u.getRealName();
    }
}
