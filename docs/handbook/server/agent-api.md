# AI Agent 接口文档

本文档描述当前 `musicServer` 的 AI Agent 能力与接口契约。

## 关联文档

- 运行手册：`../operations/ai-agent-runbook.md`
- 变更记录（2026-04-06）：`../changelog/2026-04-06.md`
- 变更记录（2026-04-18）：`../changelog/2026-04-18.md`

## 1) 能力概览

- Agent 同步回复与流式回复（SSE）
- RAG 检索增强（`keyword / vector / hybrid`）
- 意图门控（常识问题直答，音乐问题触发检索）
- 引用信息回传（`citations`）
- 可选 TTS 语音返回（`audio`）
- 健康检查接口（`/chat/health`）
- 严格歌手模式（按歌手 ID 命中本地曲库，不混入其他歌手）

## 2) 接口总览

- `POST /chat/ask`：基础对话
- `POST /chat/agent`：Agent 同步回复
- `POST /chat/agent/stream`：Agent 流式回复（SSE）
- `POST /chat/agent/savePlaylist`：保存 AI 生成歌单
- `POST /chat/agent/artist-alias/refresh`：手动刷新歌手别名索引
- `POST /chat/rag/evaluate`：离线评估 RAG（Recall@K / MRR / Hit Rate）
- `GET /chat/health`：健康状态与检索统计

## 3) Agent 请求示例

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

## 4) Agent 响应示例（简化）

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

## 5) 流式事件约定

`POST /chat/agent/stream` 返回 SSE 事件：

- `event: start`
- `event: delta`
- `event: done`
- `event: error`

## 6) 健康检查返回约定

`GET /chat/health` 返回：

- `ragEnabled`
- `ragMode`
- `ragLastRetrieval`（`strategy / mode / queryCount / candidateCount / citationCount`）
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
- `agent.rag.semantic-weight`
- `agent.rag.candidate-multiplier`
- `agent.rag.rerank.enabled`
- `agent.rag.rerank.weight`
- `agent.rag.semantic.enabled`
- `agent.rag.semantic.api-url`
- `agent.rag.semantic.api-key`
- `agent.rag.semantic.model`
- `agent.rag.semantic.max-text-length`
- `agent.artist-alias.enabled`
- `agent.artist-alias.refresh-on-startup`
- `agent.artist-alias.redis-key`
- `agent.artist-alias.redis-ttl-seconds`
- `agent.artist-alias.override-mappings`

## 8) 行为策略

- 常识/通用问答：默认不触发 RAG，直接由 LLM 常规回答。
- 音乐类问答：触发本地检索 + 可选 RAG，返回歌曲/歌单卡片与引用。
- 指定歌手歌单/推荐：优先走 `artist_id` 严格匹配，仅返回该歌手本地歌曲。
- 指定风格歌单/推荐：优先按风格字段与风格关联命中，不引入无关风格。
- 歌手别名匹配：启动时基于 `tb_artist` 自动构建别名索引（Redis + 内存双层缓存）。
- 排序策略：`hybrid` 粗排后进行二阶段 rerank 精排。
- 向量策略：支持外部 embedding API 语义打分，未配置时自动回退 bi-gram。

## 9) 别名索引刷新接口（最小示例）

请求：

```http
POST /chat/agent/artist-alias/refresh
Authorization: <token>
```

响应：

```json
{
  "code": 0,
  "message": "Success",
  "data": {
    "enabled": true,
    "aliasCount": 1280,
    "artistCount": 356,
    "redisKey": "agent:artist:alias:index:v1"
  }
}
```

## 10) RAG 评估接口（最小示例）

请求：

```json
{
  "topK": 4,
  "cases": [
    {
      "query": "播放 Yellow",
      "intent": "SEARCH_MUSIC",
      "relevant": [
        {
          "sourceType": "song",
          "sourceId": "123",
          "titleKeyword": "yellow",
          "artistKeyword": "coldplay"
        }
      ]
    }
  ]
}
```

响应关键字段：

- `recallAtK`
- `mrr`
- `hitRate`
- `softRecallAtK`
- `softMrr`
- `softHitRate`
- `details`

## 11) 验证命令

```bat
cd /d "E:\IntelliJ IDEA\workspace\music\musicServer"
mvnw.cmd -q -DskipTests compile

cd /d "E:\IntelliJ IDEA\workspace\music\musicClient"
pnpm type-check
```
