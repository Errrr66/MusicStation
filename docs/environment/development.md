# 开发环境配置

本文档说明项目在本地开发时的环境要求、依赖安装与启动方式。

## 系统要求

- **操作系统**：Windows 10/11、macOS、Linux
- **JDK**：17+
- **Node.js**：18+
- **包管理器**：pnpm 8+（推荐）、npm 9+
- **数据库**：MySQL 8.0+
- **缓存**：Redis 6.0+
- **对象存储**：MinIO（可选，本地开发可用默认值）

## 后端（musicServer）

### 启动前准备

1. 创建 MySQL 数据库 `vibe_music`
2. 启动 Redis 服务
3. 启动 MinIO 服务并创建 bucket `vibe-music-data`

### 配置方式

- 主配置文件：`src/main/resources/application.yml`
- 敏感信息通过环境变量注入，本地开发可使用默认值
- 推荐在 IDE 运行配置中设置环境变量：
  - `SPRING_DATASOURCE_PASSWORD`
  - `SPRING_REDIS_PASSWORD`
  - `JWT_SECRET`
  - `DEEPSEEK_API_KEY`

### 启动方式

1. 使用 IntelliJ IDEA 打开 `musicServer` 项目
2. 找到 `MusicApplication` 主类
3. 右键运行 `main` 方法
4. 默认访问地址：`http://localhost:8080`

### 常用命令

```bash
# Maven 编译
mvn clean compile

# 运行测试
mvn test

# 打包
mvn clean package -DskipTests
```

## 前端

### 管理端（musicAdmin）

```bash
cd musicAdmin
pnpm install
pnpm run dev
```

- 默认访问地址：`http://localhost:8089`
- 类型检查：`pnpm run typecheck`
- 生产构建：`pnpm run build`

### 客户端（musicClient）

```bash
cd musicClient
pnpm install
pnpm run dev
```

- 默认访问地址：`http://localhost:8090`

## 本地服务启动顺序

1. MySQL
2. Redis
3. MinIO
4. musicServer（后端）
5. musicAdmin / musicClient（前端）

## 调试技巧

- 后端开启 MyBatis SQL 日志：`application.yml` 中 `log-impl: org.apache.ibatis.logging.stdout.StdOutImpl`
- 前端使用浏览器 Vue/React DevTools 进行组件调试
- AI 模块调试可访问 `/chat/health` 与 `/admin/rag/health`
