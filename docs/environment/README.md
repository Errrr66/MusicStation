# 环境配置说明

本目录存放项目开发、部署与运行环境的配置说明与要求。

## 文档导航

| 文档 | 说明 |
|------|------|
| [development.md](./development.md) | 本地开发环境搭建、依赖安装与启动方式 |
| [deployment.md](./deployment.md) | 生产部署流程、环境变量与 Nginx 配置 |
| [runtime.md](./runtime.md) | 运行时资源要求、端口、性能指标与监控 |
| [docker-deployment.md](./docker-deployment.md) | 使用 Docker Compose 一键部署 |
| [minio-setup.md](./minio-setup.md) | MinIO 对象存储安装与 Bucket 配置 |
| [logging-monitoring.md](./logging-monitoring.md) | 日志配置、监控指标与告警规则 |
| [aliyun-deployment.md](./aliyun-deployment.md) | 基于项目现有配置的阿里云生产部署实战 |

## 核心环境变量

| 变量 | 说明 | 适用场景 |
|------|------|----------|
| `JWT_SECRET` | JWT 签名密钥（≥64 位） | 开发/生产 |
| `DEEPSEEK_API_KEY` | DeepSeek/DashScope API Key | 开发/生产 |
| `SPRING_DATASOURCE_URL` | 数据库连接 URL | 开发/生产 |
| `SPRING_DATASOURCE_USERNAME` | 数据库用户名 | 开发/生产 |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码 | 开发/生产 |
| `SPRING_REDIS_HOST` | Redis 主机 | 开发/生产 |
| `SPRING_REDIS_PORT` | Redis 端口 | 开发/生产 |
| `SPRING_REDIS_PASSWORD` | Redis 密码 | 开发/生产 |
| `CORS_ALLOWED_ORIGINS` | 跨域白名单 | 生产 |
| `AI_CHAT_MODEL` | AI 聊天模型 | 开发/生产 |
| `AI_CHAT_TEMPERATURE` | AI 温度参数 | 开发/生产 |
| `PERFORM_PERF_TEST` | 是否执行性能测试 | 测试 |

## 快速启动顺序

1. 启动 MySQL、Redis、MinIO
2. 启动 musicServer
3. 启动 musicAdmin / musicClient

详细步骤请参考 [development.md](./development.md)。
