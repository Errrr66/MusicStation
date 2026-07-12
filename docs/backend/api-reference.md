# 后端 API 接口文档

本文档汇总音乐平台后端服务（musicServer）对外暴露的 RESTful API 接口。

## 基础信息

- **基础路径**：`http://localhost:8080`（默认）
- **认证方式**：HTTP Header `Authorization: Bearer <JWT Token>`
- **内容类型**：`application/json`
- **统一响应格式**：
  ```json
  {
    "code": 0,
    "message": "success",
    "data": {}
  }
  ```

## 认证说明

- 未标注“需认证”的接口可匿名访问
- 需认证接口必须在请求头携带有效的 JWT Token
- `/admin/**` 接口需管理员角色（`ROLE_ADMIN`）
- `/user/**`、`/playlist/**`、`/song/**` 等接口需普通用户角色（`ROLE_USER`）
- Token 有效期为 6 小时，同时会在 Redis 中缓存以支持主动吊销

## 管理员接口（`/admin`）

| 方法 | 路径 | 说明 | 需认证 |
|------|------|------|--------|
| POST | `/admin/login` | 管理员登录 | 否 |
| POST | `/admin/logout` | 管理员登出 | 是 |
| GET | `/admin/getAllUsersCount` | 获取用户总数 | 是 |
| POST | `/admin/getAllUsers` | 分页获取用户列表 | 是 |
| POST | `/admin/addUser` | 新增用户 | 是 |
| PUT | `/admin/updateUser` | 更新用户信息 | 是 |
| PATCH | `/admin/updateUserStatus/{id}/{status}` | 更新用户状态 | 是 |
| DELETE | `/admin/deleteUser/{id}` | 删除用户 | 是 |
| DELETE | `/admin/deleteUsers` | 批量删除用户 | 是 |
| GET | `/admin/getAllArtistsCount` | 获取歌手总数 | 是 |
| POST | `/admin/getAllArtists` | 分页获取歌手列表 | 是 |
| POST | `/admin/addArtist` | 新增歌手 | 是 |
| PUT | `/admin/updateArtist` | 更新歌手信息 | 是 |
| PATCH | `/admin/updateArtistAvatar/{id}` | 更新歌手头像 | 是 |
| DELETE | `/admin/deleteArtist/{id}` | 删除歌手 | 是 |
| DELETE | `/admin/deleteArtists` | 批量删除歌手 | 是 |
| GET | `/admin/getAllSongsCount` | 获取歌曲总数 | 是 |
| GET | `/admin/getAllArtistNames` | 获取所有歌手名称 | 是 |
| POST | `/admin/getAllSongsByArtist` | 按歌手分页查询歌曲 | 是 |
| POST | `/admin/addSong` | 新增歌曲 | 是 |
| PUT | `/admin/updateSong` | 更新歌曲信息 | 是 |
| PATCH | `/admin/updateSongCover/{id}` | 更新歌曲封面 | 是 |
| PATCH | `/admin/updateSongAudio/{id}` | 更新歌曲音频并提取歌词 | 是 |
| DELETE | `/admin/deleteSong/{id}` | 删除歌曲 | 是 |
| DELETE | `/admin/deleteSongs` | 批量删除歌曲 | 是 |
| GET | `/admin/getAllPlaylistsCount` | 获取歌单总数 | 是 |
| POST | `/admin/getAllPlaylists` | 分页获取歌单列表 | 是 |
| POST | `/admin/addPlaylist` | 新增歌单 | 是 |
| PUT | `/admin/updatePlaylist` | 更新歌单信息 | 是 |
| PATCH | `/admin/updatePlaylistCover/{id}` | 更新歌单封面 | 是 |
| DELETE | `/admin/deletePlaylist/{id}` | 删除歌单 | 是 |
| DELETE | `/admin/deletePlaylists` | 批量删除歌单 | 是 |
| POST | `/admin/getAllBanners` | 分页获取轮播图 | 是 |
| POST | `/admin/addBanner` | 新增轮播图 | 是 |
| PATCH | `/admin/updateBanner/{id}` | 更新轮播图 | 是 |
| PATCH | `/admin/updateBannerStatus/{id}` | 更新轮播图状态 | 是 |
| DELETE | `/admin/deleteBanner/{id}` | 删除轮播图 | 是 |
| DELETE | `/admin/deleteBanners` | 批量删除轮播图 | 是 |
| POST | `/admin/getAllFeedbacks` | 分页获取用户反馈 | 是 |
| DELETE | `/admin/deleteFeedback/{id}` | 删除反馈 | 是 |
| DELETE | `/admin/deleteFeedbacks` | 批量删除反馈 | 是 |
| GET | `/admin/rag/health` | RAG 健康状态 | 是 |
| POST | `/admin/rag/artist-alias/refresh` | 刷新艺术家别名索引 | 是 |
| POST | `/admin/rag/debug-retrieve` | RAG 检索调试 | 是 |
| POST | `/admin/rag/evaluate` | RAG 评估 | 是 |

## 用户接口（`/user`）

| 方法 | 路径 | 说明 | 需认证 |
|------|------|------|--------|
| GET | `/user/sendVerificationCode` | 发送邮箱验证码 | 否 |
| POST | `/user/register` | 用户注册 | 否 |
| POST | `/user/login` | 用户登录 | 否 |
| GET | `/user/getUserInfo` | 获取当前用户信息 | 是 |
| PUT | `/user/updateUserInfo` | 更新用户信息 | 是 |
| PATCH | `/user/updateUserAvatar` | 更新用户头像 | 是 |
| PATCH | `/user/updateUserPassword` | 修改密码 | 是 |
| PATCH | `/user/resetUserPassword` | 重置密码 | 否 |
| POST | `/user/logout` | 用户登出 | 是 |
| DELETE | `/user/deleteAccount` | 注销账号 | 是 |

## 歌曲接口（`/song`）

| 方法 | 路径 | 说明 | 需认证 |
|------|------|------|--------|
| POST | `/song/getAllSongs` | 分页获取歌曲列表 | 否 |
| GET | `/song/getRecommendedSongs` | 获取推荐歌曲 | 否 |
| GET | `/song/getSongDetail/{id}` | 获取歌曲详情 | 否 |
| PATCH | `/song/fillLyric/{id}` | 补全指定歌曲歌词 | 是 |
| POST | `/song/fillMissingLyrics` | 批量补全缺失歌词 | 是 |

## 歌手接口（`/artist`）

| 方法 | 路径 | 说明 | 需认证 |
|------|------|------|--------|
| POST | `/artist/getAllArtists` | 分页获取歌手列表 | 否 |
| GET | `/artist/getRandomArtists` | 获取随机歌手 | 是 |
| GET | `/artist/getArtistDetail/{id}` | 获取歌手详情 | 否 |

## 歌单接口（`/playlist`）

| 方法 | 路径 | 说明 | 需认证 |
|------|------|------|--------|
| POST | `/playlist/getAllPlaylists` | 分页获取歌单列表 | 否 |
| GET | `/playlist/getRecommendedPlaylists` | 获取推荐歌单 | 否 |
| GET | `/playlist/getPlaylistDetail/{id}` | 获取歌单详情 | 否 |

## 收藏接口（`/favorite`）

| 方法 | 路径 | 说明 | 需认证 |
|------|------|------|--------|
| POST | `/favorite/getFavoriteSongs` | 分页获取收藏歌曲 | 是 |
| POST | `/favorite/collectSong` | 收藏歌曲 | 是 |
| DELETE | `/favorite/cancelCollectSong` | 取消收藏歌曲 | 是 |
| POST | `/favorite/getFavoritePlaylists` | 分页获取收藏歌单 | 是 |
| POST | `/favorite/collectPlaylist` | 收藏歌单 | 是 |
| DELETE | `/favorite/cancelCollectPlaylist` | 取消收藏歌单 | 是 |

## 评论接口（`/comment`）

| 方法 | 路径 | 说明 | 需认证 |
|------|------|------|--------|
| POST | `/comment/addSongComment` | 新增歌曲评论 | 是 |
| POST | `/comment/addPlaylistComment` | 新增歌单评论 | 是 |
| PATCH | `/comment/likeComment/{id}` | 点赞评论 | 是 |
| PATCH | `/comment/cancelLikeComment/{id}` | 取消点赞 | 是 |
| DELETE | `/comment/deleteComment/{id}` | 删除评论 | 是 |

## 社交接口（`/social`）

| 方法 | 路径 | 说明 | 需认证 |
|------|------|------|--------|
| GET | `/social/profile/{userId}` | 获取用户主页 | 否 |
| POST | `/social/follow/{targetUserId}` | 关注用户 | 是 |
| DELETE | `/social/follow/{targetUserId}` | 取消关注 | 是 |
| GET | `/social/followers/{userId}` | 获取粉丝列表 | 否 |
| GET | `/social/following/{userId}` | 获取关注列表 | 否 |
| GET | `/social/conversations` | 获取会话列表 | 是 |
| GET | `/social/messages/{friendId}` | 获取私信记录 | 是 |
| POST | `/social/messages` | 发送私信 | 是 |

## AI 对话接口（`/chat`）

| 方法 | 路径 | 说明 | 需认证 |
|------|------|------|--------|
| GET | `/chat/health` | 健康检查 | 否 |
| POST | `/chat/ask` | 普通对话 | 是 |
| POST | `/chat/agent` | 音乐 Agent 对话 | 是 |
| POST | `/chat/agent/stream` | 伪流式 Agent（SSE） | 是 |
| POST | `/chat/agent/stream/v2` | 真流式 Agent（SSE） | 是 |
| POST | `/chat/agent/savePlaylist` | 保存 Agent 生成歌单 | 是 |
| POST | `/chat/agent/artist-alias/refresh` | 刷新艺术家别名索引 | 是 |
| POST | `/chat/rag/evaluate` | RAG 评估 | 是 |

## 其他公开接口

| 方法 | 路径 | 说明 | 需认证 |
|------|------|------|--------|
| GET | `/search/all` | 综合搜索 | 否 |
| GET | `/banner/getBannerList` | 获取轮播图列表 | 否 |
| POST | `/feedback/addFeedback` | 提交用户反馈 | 是 |

## 返回码说明

| HTTP 状态码 | 含义 |
|-------------|------|
| 200 | 请求成功 |
| 401 | 未登录或 Token 无效/过期 |
| 403 | 无权限访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
