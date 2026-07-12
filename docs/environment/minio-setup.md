# MinIO 对象存储配置指南

本文档说明 MinIO 的安装、配置、Bucket 创建及在项目中使用方法。

## MinIO 简介

MinIO 是一个高性能的对象存储系统，兼容 Amazon S3 API。本项目使用 MinIO 存储歌曲音频、封面图片、用户头像、轮播图等静态资源。

## 安装方式

### Docker 安装（推荐）

```bash
docker run -d \
  -p 9000:9000 \
  -p 9001:9001 \
  --name vibe-minio \
  -v /data/minio:/data \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  minio/minio server /data --console-address ":9001"
```

### 二进制安装

1. 下载 MinIO 二进制文件
2. 设置可执行权限
3. 启动服务：

```bash
./minio server /data --console-address ":9001"
```

## 访问控制台

- API 地址：`http://localhost:9000`
- 控制台地址：`http://localhost:9001`
- 默认账号：`minioadmin`
- 默认密码：`minioadmin`

## 创建 Bucket

### 通过控制台创建

1. 登录 MinIO 控制台
2. 点击左侧 Buckets
3. 点击 Create Bucket
4. 输入 Bucket 名称：`vibe-music-data`
5. 确认创建

### 通过 mc 命令创建

```bash
mc alias set local http://localhost:9000 minioadmin minioadmin
mc mb local/vibe-music-data
```

## 权限配置

### 公开读取（可选）

如需通过 URL 直接访问资源，可设置 Bucket Policy：

```bash
mc policy set download local/vibe-music-data
```

或在控制台中设置 Access Policy 为 Public。

### 私有 + 预签名 URL（推荐）

保持 Bucket 私有，通过服务端生成预签名 URL 访问资源。

## 项目配置

在 `application.yml` 中配置 MinIO：

```yaml
minio:
  endpoint: ${MINIO_ENDPOINT:http://127.0.0.1:9000}
  accessKey: ${MINIO_ACCESSKEY:minioadmin}
  secretKey: ${MINIO_SECRETKEY:minioadmin}
  bucket: ${MINIO_BUCKET:vibe-music-data}
```

## 存储目录规划

建议在 Bucket 内按资源类型分目录存储：

```
vibe-music-data/
├── user-avatar/          # 用户头像
├── artist-avatar/        # 歌手头像
├── song-cover/           # 歌曲封面
├── playlist-cover/       # 歌单封面
├── banner/               # 轮播图
└── audio/                # 歌曲音频
```

## 文件命名规范

- 使用 `UUID` 或 `资源类型_资源ID_时间戳` 命名
- 避免使用中文字符与特殊符号
- 保留原始扩展名

## 清理策略

- 定期清理未引用的孤文件
- 可配置 MinIO 生命周期策略自动删除过期临时文件
- 删除数据库记录时不自动删除物理文件，需额外脚本处理

## 常见问题

| 问题 | 原因 | 解决 |
|------|------|------|
| 上传失败 | Bucket 不存在 | 创建对应 Bucket |
| 访问 403 | Bucket 权限不足 | 检查 Access Policy 或预签名 URL |
| 文件无法预览 | URL 不正确 | 确认 MinIO endpoint 可被前端访问 |
| 控制台无法登录 | 账号密码错误 | 检查启动时的环境变量 |
