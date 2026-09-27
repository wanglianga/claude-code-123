package com.court.mediation.security;

import com.court.mediation.entity.User;

/**
 * 基于 ThreadLocal 的当前登录用户上下文，由 AuthInterceptor 在请求进入时写入、结束时清理。
 */
public class CurrentUser {
    private static final ThreadLocal<User> HOLDER = new ThreadLocal<>();

    public static void set(User user) {
        HOLDER.set(user);
    }

    public static User get() {
        return HOLDER.get();
    }

    public static Long userId() {
        User u = HOLDER.get();
        return u == null ? null : u.getId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
