package com.example.music.interceptor;


import com.example.music.config.RolePermissionManager;
import com.example.music.constant.JwtClaimsConstant;
import com.example.music.constant.MessageConstant;
import com.example.music.constant.PathConstant;
import com.example.music.util.JwtUtil;
import com.example.music.util.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    private static final PathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final List<String> ALLOWED_PATHS = Arrays.asList(
            PathConstant.PLAYLIST_DETAIL_PATH,
            PathConstant.ARTIST_DETAIL_PATH,
            PathConstant.SONG_LIST_PATH,
            PathConstant.SONG_DETAIL_PATH,
            PathConstant.SOCIAL_PROFILE_PATH,
            PathConstant.SOCIAL_FOLLOWERS_PATH,
            PathConstant.SOCIAL_FOLLOWING_PATH
    );

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RolePermissionManager rolePermissionManager;

    public void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8"); // 设置字符编码为UTF-8
        response.setContentType("application/json;charset=UTF-8"); // 设置响应的Content-Type
        response.getWriter().write(message);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 允许 CORS 预检请求（OPTIONS 方法）直接通过
        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            response.setStatus(HttpServletResponse.SC_OK);
            return true; // 直接放行，确保 CORS 预检请求不会被拦截
        }

        String token = resolveToken(request.getHeader("Authorization"));
        String path = request.getRequestURI();
        boolean isAllowedPath = isAllowedPath(path);

        if (token == null || token.isEmpty()) {
            if (isAllowedPath) {
                return true; // 允许未登录用户访问这些路径
            }

            sendErrorResponse(response, 401, MessageConstant.NOT_LOGIN); // 缺少令牌
            return false;
        }

        try {
            Map<String, Object> claims = validateAndParseClaims(token);
            // 把业务数据存储到ThreadLocal中
            ThreadLocalUtil.set(claims);

            // 公开接口允许匿名访问，登录后仅用于返回个性化字段，不做权限拦截
            if (isAllowedPath) {
                return true;
            }

            String role = (String) claims.get(JwtClaimsConstant.ROLE);
            if (rolePermissionManager.hasPermission(role, path)) {
                return true;
            } else {
                sendErrorResponse(response, 403, MessageConstant.NO_PERMISSION); // 无权限访问
                return false;
            }
        } catch (Exception e) {
            // 公开接口即使 token 异常也允许按匿名态访问
            if (isAllowedPath) {
                return true;
            }
            sendErrorResponse(response, 401, MessageConstant.SESSION_EXPIRED); // 令牌无效
            return false;
        }
    }

    private String resolveToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isEmpty()) {
            return authorizationHeader;
        }
        if (authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return authorizationHeader;
    }

    private boolean isAllowedPath(String path) {
        return ALLOWED_PATHS.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    private Map<String, Object> validateAndParseClaims(String token) {
        ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
        String redisToken = operations.get(token);
        if (redisToken == null) {
            throw new RuntimeException("token expired");
        }
        return JwtUtil.parseToken(token);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清空ThreadLocal中的数据
        ThreadLocalUtil.remove();
    }
}
