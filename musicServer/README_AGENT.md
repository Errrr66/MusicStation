# Music AI Agent API

本文档描述当前 `musicServer` AI Agent 的最终能力与接口契约。

- 运行手册：`docs/AI_AGENT_RUNBOOK.md`
- 本次变更记录：`docs/CHANGELOG_2026-04-06.md`

## 1) 能力概览

- Agent 同步与流式回复（SSE）
- RAG 检索增强（keyword/vector/hybrid）
- 引用信息回传（`citations`）
- 可选 TTS 语音返回（`audio`）
- 健康检查接口（`/chat/health`）

## 2) 接口总览

- `POST /chat/ask`：基础对话
- `POST /chat/agent`：Agent 同步回复
- `POST /chat/agent/stream`：Agent 流式回复（SSE）
- `POST /chat/agent/savePlaylist`：保存 AI 生成歌单
- `GET /chat/health`：健康状态与检索统计

## 3) Agent Request

```json
{
  "message": "推荐适合夜晚听的歌",
  "messages": [{ "role": "user", "content": "推荐适合夜晚听的歌" }],
  "nowPlaying": {
    "songId": "101",
    "title": "Song A",
    "artist": "Artist A",
    "album": "Album A"
  },
  "limit": 8,
  "enableVoice": true,
  "enableRag": true
}
```

## 4) Agent Response (简化)

```json
{
  "code": 0,
  "message": "Success",
  "data": {
    "answer": "...",
    "audio": "https://...mp3",
    "intent": "RECOMMEND",
    "playerCommand": "play_target",
    "toolTrace": [],
    "songs": [],
    "playlists": [],
    "citations": [
      {
        "sourceType": "song",
        "sourceId": "12",
        "title": "歌曲《...》- ...",
        "snippet": "...",
        "reason": "匹配用户问题关键词（hybrid）"
      }
    ],
    "musicArchive": {}
  }
}
```

## 5) Stream Event Contract

`POST /chat/agent/stream` 返回 SSE 事件：

- `event: start`
- `event: delta`
- `event: done`
- `event: error`

## 6) Health Contract

`GET /chat/health` 返回：

- `ragEnabled`
- `ragMode`
- `ragLastRetrieval`（strategy/mode/queryCount/candidateCount/citationCount）
- `providers.deepseekConfigured`
- `providers.ttsConfigured`

## 7) 关键配置

`application.yml`：

- `deepseek.*`
- `tts.api-url`
- `agent.search.*`
- `agent.rag.enabled`
- `agent.rag.top-k`
- `agent.rag.max-snippet-length`
- `agent.rag.mode`
- `agent.rag.keyword-weight`
- `agent.rag.vector-weight`

## 8) 验证命令

```bat
cd /d "E:\IntelliJ IDEA\workspace\music\musicServer"
mvnw.cmd -q -DskipTests compile

cd /d "E:\IntelliJ IDEA\workspace\music\musicClient"
pnpm type-check
```
