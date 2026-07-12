# 数据库设计文档

本文档说明音乐平台后端的数据库表结构、ORM 映射关系与关键实体说明。

## 技术栈

- **数据库**：MySQL 8.0+
- **连接池**：Alibaba Druid
- **ORM**：MyBatis-Plus
- **缓存**：Redis
- **对象存储**：MinIO

## 命名规范

- 表名统一使用 `tb_` 前缀
- 主键字段名为 `id`，自增策略
- 下划线命名映射到 Java 驼峰命名（`map-underscore-to-camel-case: true`）

## 核心实体

### 用户（`tb_user`）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 用户 ID |
| username | VARCHAR | 用户名（4-16 位字母/数字/下划线/连字符） |
| password | VARCHAR | 密码（BCrypt/MD5） |
| phone | VARCHAR | 手机号 |
| email | VARCHAR | 邮箱 |
| user_avatar | VARCHAR | 头像 URL |
| introduction | VARCHAR | 简介（≤100 字） |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| status | TINYINT | 状态：0-启用，1-禁用 |

### 歌曲（`tb_song`）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 歌曲 ID |
| artist_id | BIGINT | 歌手 ID |
| name | VARCHAR | 歌曲名称 |
| album | VARCHAR | 专辑名称 |
| lyric | TEXT | 歌词 |
| duration | VARCHAR | 时长 |
| style | VARCHAR | 风格 |
| cover_url | VARCHAR | 封面 URL |
| audio_url | VARCHAR | 音频 URL |
| release_time | DATE | 发行时间 |

### 歌手（`tb_artist`）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 歌手 ID |
| name | VARCHAR | 歌手姓名 |
| gender | TINYINT | 性别：0-男，1-女 |
| avatar | VARCHAR | 头像 URL |
| birth | DATE | 出生日期 |
| area | VARCHAR | 地区 |
| introduction | TEXT | 简介 |

### 歌单（`tb_playlist`）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 歌单 ID |
| title | VARCHAR | 歌单标题 |
| cover_url | VARCHAR | 封面 URL |
| introduction | VARCHAR | 简介 |
| style | VARCHAR | 风格 |

### 其他实体

| 实体 | 表名 | 说明 |
|------|------|------|
| Admin | tb_admin | 管理员账号 |
| Banner | tb_banner | 首页轮播图 |
| Comment | tb_comment | 歌曲/歌单评论 |
| Feedback | tb_feedback | 用户反馈 |
| Genre | tb_genre | 音乐流派 |
| PlaylistBinding | tb_playlist_binding | 歌单-歌曲关联 |
| PrivateMessage | tb_private_message | 用户私信 |
| Style | tb_style | 音乐风格 |
| UserFavorite | tb_user_favorite | 用户收藏 |
| UserFollow | tb_user_follow | 用户关注关系 |

## 关键 VO/DTO 说明

- `SongDetailVO`、`CommentVO` 使用 `@JsonIgnoreProperties({"handler", "objectFactory"})` 防止 MyBatis 代理序列化问题
- `PrivateMessageDTO` 的 `content` 字段限制最大 2000 字符
- 业务层校验：文本消息 content 非空，歌曲/歌单分享需携带非空 ID

## Redis 使用

- Token 会话缓存
- 验证码缓存
- Agent 艺术家别名索引
- Spring Cache 默认有效期：10 分钟

## 数据库连接配置

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: ${SPRING_DATASOURCE_URL:jdbc:mysql://127.0.0.1:3306/vibe_music?useUnicode=true&characterEncoding=utf-8&useSSL=false}
    username: ${SPRING_DATASOURCE_USERNAME:root}
    password: ${SPRING_DATASOURCE_PASSWORD:123456}
    type: com.alibaba.druid.pool.DruidDataSource
```

## MinIO 配置

用于存储歌曲音频、封面、头像等静态资源：

```yaml
minio:
  endpoint: ${MINIO_ENDPOINT:http://127.0.0.1:9000}
  accessKey: ${MINIO_ACCESSKEY:minioadmin}
  secretKey: ${MINIO_SECRETKEY:minioadmin}
  bucket: ${MINIO_BUCKET:vibe-music-data}
```
