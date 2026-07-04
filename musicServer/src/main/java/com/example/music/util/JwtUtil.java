package com.example.music.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.example.music.constant.JwtClaimsConstant;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret:}")
    private String secretConfig;

    private static String SECRET_KEY;

    @PostConstruct
    public void init() {
        SECRET_KEY = secretConfig;
    }

    // 设置 JWT 的过期时间 6 小时
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 6;

    /**
     * 生成 JWT token
     *
     * @param claims 自定义的业务数据
     * @return JWT token
     */
    public static String generateToken(Map<String, Object> claims) {
        return JWT.create()
                .withClaim("claims", claims) // 自定义的业务数据
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // 设置过期时间
                .sign(Algorithm.HMAC256(SECRET_KEY)); // 使用 HMAC256 算法加密
    }

    /**
     * 解析 JWT token
     *
     * @param token JWT token
     * @return 自定义的业务数据
     */
    public static Map<String, Object> parseToken(String token) {
        return JWT.require(Algorithm.HMAC256(SECRET_KEY))
                .build()
                .verify(token)
                .getClaim("claims")
                .asMap();
    }

    /**
     * 从请求头中提取用于缓存 Key 的用户标识（避免把完整 JWT 放入缓存 Key）
     *
     * @param request HttpServletRequest
     * @return 用户标识（userId 字符串），未登录返回 "guest"
     */
    public static String extractUserIdForCache(HttpServletRequest request) {
        if (request == null) {
            return "guest";
        }
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token == null || token.isEmpty()) {
            return "guest";
        }
        try {
            Map<String, Object> map = parseToken(token);
            Object userId = map.get(JwtClaimsConstant.USER_ID);
            return userId == null ? "guest" : userId.toString();
        } catch (Exception e) {
            return "guest";
        }
    }

}
