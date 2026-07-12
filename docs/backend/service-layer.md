# 后端服务层与异常处理

本文档说明音乐平台后端的服务层组织、核心 Service 职责、统一响应结果与异常处理规范。

## 服务层组织

后端采用 **接口 + 实现** 的分层设计，所有业务逻辑集中在 `service` 层，Controller 仅负责参数接收与结果返回。

```
com.example.music.service/
├── IUserService.java                   # 用户服务接口
├── impl/UserServiceImpl.java           # 用户服务实现
├── IAdminService.java                  # 管理员服务接口
├── impl/AdminServiceImpl.java          # 管理员服务实现
├── IArtistService.java                 # 歌手服务接口
├── impl/ArtistServiceImpl.java         # 歌手服务实现
├── ISongService.java                   # 歌曲服务接口
├── impl/SongServiceImpl.java           # 歌曲服务实现
├── IPlaylistService.java               # 歌单服务接口
├── impl/PlaylistServiceImpl.java       # 歌单服务实现
├── IBannerService.java                 # 轮播图服务接口
├── impl/BannerServiceImpl.java         # 轮播图服务实现
├── IFeedbackService.java               # 反馈服务接口
├── impl/FeedbackServiceImpl.java       # 反馈服务实现
├── IUserFavoriteService.java           # 收藏服务接口
├── impl/UserFavoriteServiceImpl.java   # 收藏服务实现
├── ICommentService.java                # 评论服务接口
├── impl/CommentServiceImpl.java        # 评论服务实现
├── ISocialService.java                 # 社交服务接口
├── impl/SocialServiceImpl.java         # 社交服务实现
├── MinioService.java                   # MinIO 文件服务
├── impl/MinioServiceImpl.java
├── EmailService.java                   # 邮件服务
├── impl/EmailServiceImpl.java
├── DeepSeekService.java                # 旧版 DeepSeek 服务
├── MusicAgentService.java              # 旧版音乐 Agent 服务
├── AgentRagService.java                # 旧版 RAG 服务
├── ArtistAliasResolver.java            # 艺术家别名解析
└── SemanticEmbeddingService.java       # 语义 Embedding 服务
```

## 核心 Service 职责

### 用户服务（UserService）

- 用户注册、登录、登出
- 用户信息查询与更新
- 头像、密码修改
- 邮箱验证码发送

### 管理员服务（AdminService）

- 管理员登录、登出
- 用户/歌手/歌曲/歌单/轮播图/反馈的 CRUD
- 批量删除操作
- 状态更新（启用/禁用）

### 歌手服务（ArtistService）

- 歌手 CRUD
- 头像上传
- 歌手详情与列表查询

### 歌曲服务（SongService）

- 歌曲 CRUD
- 封面/音频上传
- 歌词补全（AI）
- 按歌手查询歌曲

### 歌单服务（PlaylistService）

- 歌单 CRUD
- 封面上传
- 推荐歌单查询

### MinIO 服务（MinioService）

- 文件上传（图片、音频）
- 文件 URL 生成
- 临时文件清理

## 统一响应结果（`Result<T>`）

后端所有接口统一返回 `Result<T>` 对象：

```json
{
   "code": 0,
   "message": "操作成功",
   "data": {}
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | Integer | 0-成功，1-失败 |
| message | String | 提示信息 |
| data | T | 响应数据 |

### 常用静态方法

- `Result.success(data)` — 成功并返回数据
- `Result.success()` — 成功无数据
- `Result.success(message, data)` — 成功并自定义提示
- `Result.error(message)` — 失败并返回错误信息
- `Result.error()` — 失败使用默认提示

## 异常处理规范

### 当前模式

项目当前主要采用**服务层手动返回错误结果**的方式处理业务异常，Controller 直接透传 `Result.error(message)`。

### 推荐优化方向

为进一步统一异常处理，建议引入全局异常处理：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

     @ExceptionHandler(BusinessException.class)
     public Result<Void> handleBusinessException(BusinessException e) {
         return Result.error(e.getMessage());
     }

     @ExceptionHandler(MethodArgumentNotValidException.class)
     public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
         String message = e.getBindingResult().getFieldErrors().stream()
             .map(FieldError::getDefaultMessage)
             .collect(Collectors.joining(", "));
         return Result.error(message);
     }

     @ExceptionHandler(Exception.class)
     public Result<Void> handleException(Exception e) {
         log.error("系统异常", e);
         return Result.error("系统繁忙，请稍后重试");
     }
}
```

### 业务异常类建议

```java
public class BusinessException extends RuntimeException {
     public BusinessException(String message) {
         super(message);
     }
}
```

## 事务控制

- 涉及多表写入的业务方法建议使用 `@Transactional`
- 例如：删除用户时同时清理关联数据
- 当前实现中部分批量删除操作已使用事务

## 日志规范

- 使用 SLF4J + Logback 记录日志
- 关键路径：登录、登出、敏感操作、AI 调用、文件上传
- 生产环境建议关闭 `DEBUG` 级别日志
- MyBatis SQL 日志仅在开发环境开启

## 相关文档

- [API 接口文档](./api-reference.md)
- [数据库设计文档](./database.md)
- [安全策略文档](./security.md)
