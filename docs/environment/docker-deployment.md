# Docker 部署指南

本文档说明如何使用 Docker 与 Docker Compose 部署音乐平台后端及依赖服务。

## 前置条件

- Docker Engine 24.0+
- Docker Compose 2.20+
- 已配置好环境变量文件

## 镜像构建

### 后端镜像

在项目根目录创建 `musicServer/Dockerfile`：

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/musicServer-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

构建命令：

```bash
cd musicServer
mvn clean package -DskipTests
docker build -t vibe-music-server:latest .
```

### 前端镜像（可选）

如使用 Nginx 托管前端，可构建静态资源镜像：

```dockerfile
FROM nginx:alpine
COPY dist/ /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

## Docker Compose 示例

```yaml
version: "3.8"

services:
  mysql:
    image: mysql:8.0
    container_name: vibe-mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${SPRING_DATASOURCE_PASSWORD}
      MYSQL_DATABASE: vibe_music
    volumes:
      - mysql_data:/var/lib/mysql
    ports:
      - "3306:3306"
    networks:
      - vibe-net

  redis:
    image: redis:7-alpine
    container_name: vibe-redis
    command: redis-server --requirepass ${SPRING_REDIS_PASSWORD}
    volumes:
      - redis_data:/data
    ports:
      - "6379:6379"
    networks:
      - vibe-net

  minio:
    image: minio/minio:latest
    container_name: vibe-minio
    environment:
      MINIO_ROOT_USER: ${MINIO_ACCESSKEY}
      MINIO_ROOT_PASSWORD: ${MINIO_SECRETKEY}
    command: server /data --console-address ":9001"
    volumes:
      - minio_data:/data
    ports:
      - "9000:9000"
      - "9001:9001"
    networks:
      - vibe-net

  server:
    image: vibe-music-server:latest
    container_name: vibe-music-server
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://vibe-mysql:3306/vibe_music?useUnicode=true&characterEncoding=utf-8&useSSL=false
      - SPRING_DATASOURCE_USERNAME=root
      - SPRING_DATASOURCE_PASSWORD=${SPRING_DATASOURCE_PASSWORD}
      - SPRING_REDIS_HOST=vibe-redis
      - SPRING_REDIS_PASSWORD=${SPRING_REDIS_PASSWORD}
      - MINIO_ENDPOINT=http://vibe-minio:9000
      - JWT_SECRET=${JWT_SECRET}
      - DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY}
    ports:
      - "8080:8080"
    depends_on:
      - mysql
      - redis
      - minio
    networks:
      - vibe-net

  nginx:
    image: nginx:alpine
    container_name: vibe-nginx
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/conf.d/default.conf
      - ./dist:/usr/share/nginx/html
    depends_on:
      - server
    networks:
      - vibe-net

volumes:
  mysql_data:
  redis_data:
  minio_data:

networks:
  vibe-net:
    driver: bridge
```

## 启动命令

```bash
# 准备 .env 文件
cp .env.example .env

# 启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f server

# 停止服务
docker-compose down
```

## 初始化 MinIO Bucket

首次启动后，需要创建 bucket：

```bash
docker exec -it vibe-minio mc alias set local http://localhost:9000 ${MINIO_ACCESSKEY} ${MINIO_SECRETKEY}
docker exec -it vibe-minio mc mb local/vibe-music-data
```

## 更新部署

```bash
# 重新构建后端镜像
cd musicServer
mvn clean package -DskipTests
docker build -t vibe-music-server:latest .

# 滚动更新
docker-compose up -d --no-deps --build server
```

## 常见问题

| 问题 | 原因 | 解决 |
|------|------|------|
| 后端无法连接 MySQL | 容器网络未就绪 | 等待 MySQL 完全启动后重启 server |
| MinIO 文件无法访问 | Bucket 未创建或权限不足 | 创建 bucket 并检查策略 |
| 时区不一致 | 容器默认时区非中国 | 挂载 `/etc/localtime` 或设置 `TZ` 环境变量 |
