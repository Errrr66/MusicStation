# 后端文档

本目录存放后端服务相关文档，包括服务接口说明、核心模块设计、安全策略与数据模型。

## 技术栈

- 框架：Spring Boot 3.4.x
- 数据库：MySQL 8.0+ / Redis
- ORM：MyBatis-Plus
- 对象存储：MinIO
- AI：Spring AI 1.0.0 + DeepSeek/DashScope
- 安全：JWT + BCrypt + Redis Session

## 文档导航

| 文档 | 说明 |
|------|------|
| [api-reference.md](./api-reference.md) | 完整 RESTful API 接口列表与认证说明 |
| [ai-module.md](./ai-module.md) | Spring AI、RAG、Agent 模块设计 |
| [security.md](./security.md) | JWT、密码策略、接口鉴权与安全防护 |
| [database.md](./database.md) | 数据库表结构、MyBatis 映射与 Redis 使用 |
| [service-layer.md](./service-layer.md) | 服务层组织、统一响应与异常处理规范 |

## 关键环境变量

详见 [environment/README.md](../environment/README.md)。
