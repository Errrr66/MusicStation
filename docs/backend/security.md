# 安全策略文档

本文档说明音乐平台后端的安全机制，包括认证、授权、密码策略、JWT 与 Redis 会话管理。

## 认证流程

1. 用户/管理员调用登录接口（`/user/login` 或 `/admin/login`）
2. 服务端校验用户名与密码（BCrypt / 兼容旧 MD5）
3. 校验通过后生成 JWT Token 并写入 Redis
4. 前端在后续请求中通过 `Authorization: Bearer <token>` 携带 Token
5. `LoginInterceptor` 拦截请求，校验 Token 有效性与权限

## JWT 机制

- **实现**：`com.example.music.util.JwtUtil`
- **算法**：HMAC256
- **有效期**：6 小时
- **Secret**：通过环境变量 `JWT_SECRET` 注入，生产环境必须为 ≥64 位随机字符串
- **Claims**：包含 `userId`、`role` 等业务数据
- **缓存**：Token 同时存入 Redis，支持服务端主动吊销

## 登录拦截器（`LoginInterceptor`）

- 默认拦截所有请求
- 放行的公开路径在 `WebConfig` 中配置
- 额外允许的匿名路径在 `ALLOWED_PATHS` 中维护：
  - 歌曲/歌手/歌单列表与详情
  - 用户主页、粉丝/关注列表
- 支持 CORS 预检请求（OPTIONS）直接放行
- Token 解析异常时，公开接口仍按匿名态访问

## 权限模型

基于 `role-path-permissions` 配置：

| 角色 | 允许路径前缀 |
|------|--------------|
| `ROLE_ADMIN` | `/admin/` |
| `ROLE_USER` | `/user/`、`/playlist/`、`/artist/`、`/song/`、`/favorite/`、`/chat/`、`/social/`、`/search/`、`/comment/`、`/banner/`、`/feedback/` |

未命中权限规则的请求返回 `403 Forbidden`。

## 密码安全

- **实现**：`com.example.music.util.PasswordUtils`
- **新密码**：使用 BCrypt 加密存储
- **兼容旧密码**：自动识别 32 位十六进制 MD5 密码，登录时允许通过并自动迁移
- **复杂度要求**（用户注册）：8-18 位，必须包含数字、字母、符号中的至少两种

## 关键安全配置项

```yaml
jwt:
  secret: ${JWT_SECRET}

cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:*}

role-path-permissions:
  permissions:
    ROLE_ADMIN:
      - "/admin/"
    ROLE_USER:
      - "/user/"
      - "/playlist/"
      # ...
```

## 安全建议

1. 生产环境必须将 `JWT_SECRET` 设置为 ≥64 位随机字符串，禁止硬编码
2. 数据库、Redis、MinIO、邮件密码均通过环境变量注入
3. 生产环境启用 HTTPS，并在 Nginx 配置安全响应头
4. 定期轮换 API Key（DeepSeek/DashScope）
5. 敏感操作（删除、批量删除）建议增加操作日志审计
