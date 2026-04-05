# Music AI Agent API (Updated)

本文档汇总当前 AI 助手能力、接口、已修复问题与验收方式，供开发和联调使用。

## 1) 本次更新总结

- 已完成 Agent 化：支持本地曲库优先、全网补全、播放控制、歌单自动生成与保存。
- 已完成流式回复：`/chat/agent/stream`（SSE）支持 `start/delta/done/error`。
- 已完成实时语音：前端支持连续语音识别 + 自动发送 + 流式回答。
- 已完成听歌识曲接入：识曲入口已放到 AI 助手输入区侧边。
- 已修复关键问题：
  - “播放指定歌曲却只播放当前歌曲”已修复（区分点播与纯控制命令）。
  - “AI 歌单保存后收藏列表看不到”已修复（保存后自动写入用户收藏并刷新）。
  - “聊天中空回复框/多余头像”已修复（占位消息不渲染空泡泡与头像）。

## 2) 接口总览

- `POST /chat/ask`：基础对话（非 Agent）
- `POST /chat/agent`：Agent 同步回复
- `POST /chat/agent/stream`：Agent 流式回复（SSE）
- `POST /chat/agent/savePlaylist`：保存 AI 生成歌单

## 3) `POST /chat/agent`

### Request Example

```json
{
  "message": "推荐周杰伦的歌",
  "messages": [
    { "role": "user", "content": "推荐周杰伦的歌" }
  ],
  "nowPlaying": {
    "songId": "101",
    "title": "Song A",
    "artist": "Artist A",
    "album": "Album A"
  },
  "limit": 8,
  "enableVoice": true
}
```

### Response (simplified)

```json
{
  "code": 0,
  "message": "Success",
  "data": {
    "answer": "...",
    "audio": "https://...mp3",
    "intent": "RECOMMEND",
    "playerCommand": "play_target",
    "toolTrace": [
      { "tool": "recommend_local_keyword", "status": "ok", "summary": "..." }
    ],
    "songs": [],
    "playlists": [],
    "musicArchive": {}
  }
}
```

## 4) `POST /chat/agent/stream` (SSE)

### Event Contract

- `event: start`：流式开始
- `event: delta`：增量文本片段
- `event: done`：最终完整 `AgentChatResponseVO`（JSON 字符串）
- `event: error`：错误信息

### Client Notes

- 请求头需带 `Accept: text/event-stream`。
- 前端已实现 `AbortController` 中断，支持“停止生成/继续生成”。
- SSE 解析需支持多行 `data:`，避免 `done` JSON 截断。

## 5) `POST /chat/agent/savePlaylist`

### Request Example

```json
{
  "title": "AI歌单: 深夜轻音乐",
  "introduction": "由 AI 助手自动生成",
  "style": "AI推荐",
  "coverUrl": "https://example.com/cover.jpg",
  "tracks": [
    { "songId": 12, "songName": "Song A", "artistName": "Artist A" },
    { "songId": 22, "songName": "Song B", "artistName": "Artist B" }
  ]
}
```

### Success Response Example

```json
{
  "code": 0,
  "message": "Success",
  "data": {
    "playlistId": 101,
    "title": "[AI] AI歌单: 深夜轻音乐",
    "songCount": 2
  }
}
```

### Save Strategy

- 自动标题去重：`[AI] xxx`、`[AI] xxx (2)`、`[AI] xxx (3)`...
- 同步保存曲目绑定：`tb_playlist_binding`
- 自动加入用户收藏：`tb_user_favorite(type=1)`

## 6) 推荐与检索策略（当前）

- 本地曲库优先（MinIO 存储资源）。
- 用户输入含歌手/关键词时，优先走本地关键词命中。
- 本地不足时自动补全外网（iTunes）。
- 外网搜索带超时、熔断、Redis 缓存与回退。

## 7) 配置项

`application.yml` 关键项：

- `deepseek.api-key: ${DEEPSEEK_API_KEY:...}`
- `deepseek.base-url`
- `deepseek.model`
- `tts.api-url: ${TTS_API_URL:http://127.0.0.1:5000/voice}`
- `agent.search.*`（超时、缓存、熔断）

## 8) 常见问题

### Q1: `401 Unauthorized: [nobody]`

- 原因：模型 API key 未配置或与 `deepseek.base-url` 不匹配。
- 处理：设置 `DEEPSEEK_API_KEY` 后重启后端。

### Q2: 推荐某歌手时本地明明有歌却没命中

- 已优化：会从用户语句中提取关键词（如“推荐周杰伦的歌” -> “周杰伦”）并优先本地检索。

## 9) 验收命令

```bat
cd /d "e:\IntelliJ IDEA\workspace\music\musicServer"
mvnw.cmd -q -DskipTests compile

cd /d "e:\IntelliJ IDEA\workspace\music\musicClient"
npm run type-check
```

## 10) 错误码

| HTTP | `code` | Meaning | Typical Trigger |
| --- | --- | --- | --- |
| 200 | 0 | Success | 正常处理 |
| 200 | 1 | Business failure | 参数校验失败/业务规则不满足 |
| 401 | - | Unauthorized | Token 缺失或失效（接口被拦截） |
| 403 | - | Forbidden | 角色权限不足 |
| 500 | - | Server error | 服务内部异常 |



