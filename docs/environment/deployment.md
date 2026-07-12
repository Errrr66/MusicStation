# 部署环境配置

本文档说明项目生产环境部署时的配置要求、构建流程与环境变量。

## 部署架构

```
[用户]
   │
[Nginx / 负载均衡]  ← HTTPS、静态资源、反向代理
   │
[musicAdmin] ─────┐
[musicClient] ────┼──→ [musicServer] → [MySQL / Redis / MinIO]
[AI Agent] ───────┘
```

## 必需环境变量

生产环境必须通过环境变量注入以下配置，禁止在配置文件中硬编码：

| 变量 | 说明 | 要求 |
|------|------|------|
| `JWT_SECRET` | JWT 签名密钥 | ≥64 位随机字符串，务必保密 |
| `DEEPSEEK_API_KEY` | DeepSeek/DashScope API Key | 有效的 API 密钥 |
| `SPRING_DATASOURCE_URL` | 数据库连接 URL | 生产数据库地址 |
| `SPRING_DATASOURCE_USERNAME` | 数据库用户名 | 非 root 的独立账号 |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码 | 强密码 |
| `SPRING_REDIS_HOST` | Redis 主机 | 生产 Redis 地址 |
| `SPRING_REDIS_PORT` | Redis 端口 | 默认 6379 |
| `SPRING_REDIS_PASSWORD` | Redis 密码 | 强密码 |

## 可选环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `SERVER_PORT` | 8080 | 后端服务端口 |
| `CORS_ALLOWED_ORIGINS` | `*` | 允许跨域来源，生产环境建议限制为具体域名 |
| `AI_CHAT_MODEL` | `deepseek-v4-flash` | AI 聊天模型 |
| `AI_CHAT_TEMPERATURE` | 0.7 | AI 温度参数 |
| `AI_CHAT_MAX_TOKENS` | 2048 | 最大 token 数 |
| `AI_RAG_AUTO_INDEX` | false | 启动时是否自动构建 RAG 索引 |
| `CHAT_STREAM_V2_ENABLED` | true | 是否启用真流式 SSE |
| `MINIO_ENDPOINT` | `http://127.0.0.1:9000` | MinIO 地址 |
| `MINIO_ACCESSKEY` | minioadmin | MinIO Access Key |
| `MINIO_SECRETKEY` | minioadmin | MinIO Secret Key |
| `MINIO_BUCKET` | vibe-music-data | MinIO Bucket 名称 |

## 后端部署

### 构建

```bash
cd musicServer
mvn clean package -DskipTests
```

构建产物：`target/musicServer-*.jar`

### 运行

```bash
java -jar musicServer-*.jar \
  --JWT_SECRET=your-64-char-secret \
  --DEEPSEEK_API_KEY=your-api-key \
  --SPRING_DATASOURCE_PASSWORD=your-db-password \
  --SPRING_REDIS_PASSWORD=your-redis-password
```

或使用 `.env` 文件与 systemd/docker 管理。

## 前端部署

### 管理端

```bash
cd musicAdmin
pnpm install
pnpm run build
```

构建产物：`dist/` 目录，部署到 Nginx 静态资源目录。

### 客户端

```bash
cd musicClient
pnpm install
pnpm run build
```

构建产物：`dist/` 目录，部署到 Nginx 静态资源目录。

## Nginx 配置建议

```nginx
server {
    listen 443 ssl;
    server_name your-domain.com;

    # SSL 证书配置
    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;

    # 安全响应头
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    # 前端静态资源
    location / {
        root /var/www/music-admin/dist;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 代理
    location /api/ {
        proxy_pass http://localhost:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

## 部署检查清单

- [ ] 已配置强密码与独立数据库账号
- [ ] `JWT_SECRET` 为 ≥64 位随机字符串
- [ ] Redis 已设置密码并限制访问
- [ ] MinIO bucket 已创建且权限正确
- [ ] 已启用 HTTPS
- [ ] 已配置 CORS 白名单
- [ ] 已关闭后端 SQL 控制台日志
- [ ] 已配置日志收集与监控告警
