package cn.court.mediation.security;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 内存令牌存储（演示环境；重启需重新登录） */
@Component
public class AuthTokenStore {
    private final ConcurrentHashMap<String, Long> tokenToUser = new ConcurrentHashMap<>();

    public String issue(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokenToUser.put(token, userId);
        return token;
    }

    public Optional<Long> resolve(String token) {
        return Optional.ofNullable(tokenToUser.get(token));
    }

    public void revoke(String token) {
        if (token != null) {
            tokenToUser.remove(token);
        }
    }
}
