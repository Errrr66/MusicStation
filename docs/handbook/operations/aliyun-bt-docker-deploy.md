# Aliyun 轻量服务器（Alibaba Cloud Linux 3）部署手册

本文档用于将本项目部署到：

- 服务器：`101.200.154.124`
- 域名：`paramus.fun`
- 面板：宝塔 Linux
- 运行方式：Docker + Nginx

## 1. 域名与网络

在域名解析中添加 A 记录到 `101.200.154.124`：

- `@` -> `101.200.154.124`
- `admin` -> `101.200.154.124`
- `api` -> `101.200.154.124`
- `oss` -> `101.200.154.124`

阿里云安全组开放：`22`、`80`、`443`。

## 2. 代码上传前已完成的配置改造

- `musicServer/src/main/resources/application.yml`
  - 所有数据库、Redis、MinIO、邮件、DeepSeek 等改为环境变量优先
- `musicServer/src/main/resources/application-prod.yml`
  - 新增生产配置（默认连接 `mysql` / `redis` / `minio` 容器）
- `musicClient/.env.production`
  - `VITE_APP_BASE_API=https://api.paramus.fun`
- `musicAdmin/.env.production`
  - `VITE_API_URL=https://api.paramus.fun`
- `musicAdmin/src/utils/http/index.ts`
  - `baseURL` 改为 `import.meta.env.VITE_API_URL`

## 3. 服务器目录

```bash
mkdir -p /www/projects/music/{source,deploy,data/mysql,data/redis,data/minio,logs}
```

将源码上传到：

- `/www/projects/music/source/musicClient`
- `/www/projects/music/source/musicAdmin`
- `/www/projects/music/source/musicServer`

将部署模板上传到：

- `/www/projects/music/deploy/.env`（由 `deploy/aliyun/.env.example` 复制并改密钥）
- `/www/projects/music/deploy/docker-compose.yml`（由 `deploy/aliyun/docker-compose.yml` 复制）

## 4. 后端打包与容器启动

```bash
cd /www/projects/music/source/musicServer
mvn -DskipTests clean package

cd /www/projects/music/deploy
docker compose up -d --build
docker compose ps
```

## 5. 数据库导入

```bash
docker exec -i music-mysql mysql -uroot -p你的Root密码 vibe_music < /www/projects/music/source/musicServer/sql/music.sql
docker exec -i music-mysql mysql -uroot -p你的Root密码 vibe_music < /www/projects/music/source/musicServer/sql/social_feature.sql
```

## 6. 前端构建与发布

```bash
cd /www/projects/music/source/musicClient
pnpm install
pnpm build
mkdir -p /www/wwwroot/paramus.fun
cp -rf dist/* /www/wwwroot/paramus.fun/

cd /www/projects/music/source/musicAdmin
pnpm install
pnpm build
mkdir -p /www/wwwroot/admin.paramus.fun
cp -rf dist/* /www/wwwroot/admin.paramus.fun/
```

## 7. Nginx 站点

宝塔中创建四个站点，并按 `deploy/aliyun/nginx/*.conf` 配置。

- `paramus.fun`
- `admin.paramus.fun`
- `api.paramus.fun`
- `oss.paramus.fun`

每个站点申请并启用 HTTPS 证书（Let's Encrypt），并开启强制 HTTPS。

## 8. 资源 URL 替换（必须）

将历史 `localhost:9000` / `127.0.0.1:9000` 资源地址替换为 `https://oss.paramus.fun`。

示例：

```sql
UPDATE tb_artist
SET avatar = REPLACE(avatar, 'http://localhost:9000', 'https://oss.paramus.fun')
WHERE avatar LIKE 'http://localhost:9000%';

UPDATE tb_artist
SET avatar = REPLACE(avatar, 'http://127.0.0.1:9000', 'https://oss.paramus.fun')
WHERE avatar LIKE 'http://127.0.0.1:9000%';
```

## 9. 验收

```bash
curl -I https://paramus.fun
curl -I https://admin.paramus.fun
curl -I https://api.paramus.fun
curl -I https://oss.paramus.fun

cd /www/projects/music/deploy
docker compose ps
docker logs --tail=100 music-server
```

若出现跨域问题，请在后端 CORS 允许：

- `https://paramus.fun`
- `https://admin.paramus.fun`

