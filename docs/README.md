# 项目文档总览

本文档目录按照项目架构与功能模块进行分类组织，便于团队成员快速查阅与后续维护。

## 目录结构

```
docs/
├── README.md                          # 本文档：总览与导航
├── frontend/                          # 前端相关文档
│   ├── README.md
│   ├── architecture.md                # 前端架构
│   ├── design-system.md               # Matrix 设计系统
│   ├── client-guide.md                # 客户端用户指南
│   ├── design/                        # 页面与交互设计文档
│   │   └── login-design.md
│   └── components/                    # 可复用组件文档
│       ├── matrix.md
│       └── orb.md
├── backend/                           # 后端服务文档
│   ├── README.md
│   ├── api-reference.md               # API 接口文档
│   ├── ai-module.md                   # AI 模块设计
│   ├── security.md                    # 安全策略
│   ├── database.md                    # 数据库设计
│   └── service-layer.md               # 服务层与异常处理
├── admin/                             # 管理端操作指南
│   ├── README.md
│   ├── login-guide.md                 # 登录指南
│   ├── dashboard-guide.md             # 首页数据看板
│   ├── user-management.md             # 用户管理
│   ├── song-management.md             # 歌曲管理
│   ├── playlist-management.md         # 歌单管理
│   ├── artist-management.md           # 艺人管理
│   ├── banner-management.md           # 轮播图管理
│   └── feedback-management.md         # 用户反馈
└── environment/                       # 环境配置说明
    ├── README.md
    ├── development.md                 # 开发环境
    ├── deployment.md                  # 部署环境
    ├── runtime.md                     # 运行环境
    ├── docker-deployment.md           # Docker 部署
    ├── minio-setup.md                 # MinIO 配置
    └── logging-monitoring.md          # 日志与监控
```

## 快速导航

### 按角色

| 角色 | 推荐文档 |
|------|----------|
| 前端开发 | [frontend/architecture.md](./frontend/architecture.md)、[frontend/design-system.md](./frontend/design-system.md) |
| 后端开发 | [backend/api-reference.md](./backend/api-reference.md)、[backend/ai-module.md](./backend/ai-module.md)、[backend/security.md](./backend/security.md)、[backend/service-layer.md](./backend/service-layer.md) |
| 运维/部署 | [environment/deployment.md](./environment/deployment.md)、[environment/docker-deployment.md](./environment/docker-deployment.md)、[environment/minio-setup.md](./environment/minio-setup.md)、[environment/logging-monitoring.md](./environment/logging-monitoring.md) |
| 运营/管理员 | [admin/login-guide.md](./admin/login-guide.md)、[admin/dashboard-guide.md](./admin/dashboard-guide.md)、[admin/user-management.md](./admin/user-management.md) |

### 按模块

| 模块 | 入口 |
|------|------|
| 前端 | [frontend/README.md](./frontend/README.md) |
| 后端 | [backend/README.md](./backend/README.md) |
| 管理端 | [admin/README.md](./admin/README.md) |
| 环境配置 | [environment/README.md](./environment/README.md) |

## 文档规范

- 统一使用 Markdown（`.md`）格式
- 文件名采用小写连字符（kebab-case）命名
- 每个分类目录下使用 `README.md` 作为索引入口
- 图片、示例代码等静态资源与文档放在同一目录下的 `assets/` 子目录中

## 最近更新

- 后端 API 接口文档：覆盖全部 Controller 端点
- AI 模块文档：Spring AI + RAG 架构与配置
- 安全策略文档：JWT、BCrypt、权限模型
- 数据库文档：核心实体与表结构
- 服务层文档：Service 职责、统一响应、异常处理建议
- 管理端操作手册：登录、首页看板、用户、歌曲、歌单、艺人、轮播图、反馈
- 客户端用户指南：首页、歌手、歌单、AI 助手、音乐库、私信等
- 环境配置文档：开发、部署、运行、Docker、MinIO、日志监控
