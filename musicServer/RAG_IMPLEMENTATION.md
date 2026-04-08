# MusicServer RAG 实现详解

本文档基于当前项目代码，说明 `musicServer` 中 AI 助手 RAG（Retrieval-Augmented Generation）的完整实现方式。

## 1. 实现目标

本项目的 RAG 目标不是做通用百科检索，而是做“音乐域增强回答”：

- 当用户是音乐相关意图时，优先从本地曲库/歌单检索证据
- 把检索结果拼接进 Prompt，让大模型按证据回答
- 在前端返回可点击的引用（`citations`）与卡片（歌曲/歌单）
- 对常识问题做意图门控，避免无意义检索

## 2. 代码入口与职责分层

### 2.1 入口层（Controller）

- `src/main/java/com/example/music/controller/ChatController.java`
  - `POST /chat/agent`
  - `POST /chat/agent/stream`
  - `GET /chat/health`

Controller 只负责接收请求、调用服务、返回结果，不做检索细节。

### 2.2 编排层（Agent Orchestrator）

- `src/main/java/com/example/music/service/MusicAgentService.java`

职责：

- 意图识别（搜索/推荐/建歌单/播放控制/普通聊天）
- 决定是否启用 RAG（`shouldUseRagForInput`）
- 调用 RAG 服务检索上下文
- 合并工具结果与引用
- 组装最终 Prompt 给大模型生成回答

### 2.3 检索层（RAG Retriever）

- `src/main/java/com/example/music/service/AgentRagService.java`

职责：

- 生成查询词（用户输入 + 可选当前播放）
- 从本地数据源检索候选（歌曲/歌单/风格）
- 对候选计算分数（keyword / vector / hybrid）
- 选 Top-K 作为引用，并构造 Prompt 上下文

## 3. 请求处理主流程

1. 前端请求 `/chat/agent` 或 `/chat/agent/stream`
2. `MusicAgentService.handle(...)` 先识别 `Intent`
3. 根据意图执行工具动作（本地搜索、推荐、建歌单等）
4. 对非播放器控制类请求，执行 RAG 门控：
   - 常识问题：不检索，直接常规回答
   - 音乐问题：调用 `AgentRagService.retrieve(...)`
5. 将 `ragContext.promptContext` 拼到大模型 Prompt
6. 返回：
   - `answer`（最终文本）
   - `songs/playlists`（结构化结果）
   - `citations`（RAG 引用）
   - `toolTrace/musicArchive`（可观测诊断）

## 4. RAG 门控策略（避免误检索）

门控位置：`MusicAgentService.handle(...)`

关键逻辑：

- `shouldUseRagForInput(input, intent)`
- `isMusicDomainQuery(input)`
- `isLikelyKnowledgeQuestion(input)`

行为：

- `Intent.PLAYER_CONTROL`：不做 RAG
- `Intent.CHAT`：只有在音乐域问题时才做 RAG
- 其余音乐意图：默认启用 RAG

这样可以保证“常识问题正常回答，音乐问题才检索增强”。

## 5. 检索数据源与召回逻辑

RAG 当前全部基于本地库，不依赖外部知识库。

### 5.1 歌曲检索

- `SongMapper.searchSongsByKeyword(keyword, limit)`
- `SongMapper.searchSongsByStyleKeyword(styleKeyword, limit)`

### 5.2 歌单检索

- `PlaylistMapper.searchPlaylistsByKeyword(keyword, limit)`

### 5.3 查询词构造

`AgentRagService` 会构造多组 query：

- 原始用户句子
- 清洗后的紧凑句子
- 动作语句抽取词（如“播放 Yellow”抽出 `Yellow`）
- 引号/书名号关键词（如《Yellow》）
- 必要时可回退当前播放上下文（受意图限制）

## 6. 排序与打分机制

`AgentRagService` 支持三种模式：

- `keyword`
- `vector`
- `hybrid`（默认）

配置项：

- `agent.rag.mode`
- `agent.rag.keyword-weight`
- `agent.rag.vector-weight`
- `agent.rag.top-k`

### 6.1 keyword 分

通过关键词包含关系 + 候选排名位置计算。

### 6.2 vector 分（轻量实现）

不是外部向量库，而是本地近似：

- 文本归一化
- 字符 bi-gram 向量化
- 余弦相似度

### 6.3 hybrid 分

`total = keywordWeight * keywordScore + vectorWeight * vectorScore`

最后按 `totalScore` 降序取 Top-K。

## 7. Prompt 拼接与答案生成

### 7.1 Prompt Context 构造

`AgentRagService.buildPromptContext(...)` 输出结构化文本，例如：

- `RAG资料（按相关性排序）`
- 每条资料含：类型、标题、摘要
- 回答约束：资料不足时明确不确定，禁止编造

### 7.2 最终生成

`MusicAgentService.buildAgentNarration(...)` 将以下信息合并后给 LLM：

- `intent`
- 用户输入
- 工具摘要
- 当前播放（可选）
- RAG 上下文（可选）

模型返回 JSON：

- `chinese`
- `japanese`（用于 TTS）

## 8. 引用（citations）如何返回前端

来源有两类：

1. RAG 检索引用（`AgentRagService.retrieve`）
2. 工具结果补充引用（`MusicAgentService.buildSongCitationsFromResults`）

合并逻辑：`mergeCitations(...)`

- 去重（`sourceType + sourceId + title`）
- 按上限截断
- 随响应返回 `citations`

前端在 `musicClient/src/pages/chat/index.vue` 中显示“参考资料”卡片并支持跳转。

## 9. 健康检查与可观测性

接口：`GET /chat/health`

返回关键字段：

- `ragEnabled`
- `ragMode`
- `ragLastRetrieval`
  - `strategy`
  - `mode`
  - `queryCount`
  - `candidateCount`
  - `citationCount`
  - `updatedAtEpochMs`

此外，`toolTrace` 与 `musicArchive` 也会记录每轮的策略与命中情况。

## 10. 与“严格歌手模式”的关系

虽然严格歌手模式不属于 RAG 核心，但影响检索质量：

- `MusicAgentService` 中歌手相关意图会优先按 `artist_id` 严格命中本地歌曲
- 新增 `ArtistAliasResolver` 自动构建歌手别名索引（内存 + Redis）
- 提供 `POST /chat/agent/artist-alias/refresh` 手动刷新

这保证了“来一份 Coldplay 歌单”不会混入其他歌手。

## 11. 关键配置（application.yml）

- `agent.rag.enabled`
- `agent.rag.top-k`
- `agent.rag.max-snippet-length`
- `agent.rag.mode`
- `agent.rag.keyword-weight`
- `agent.rag.vector-weight`

歌手别名相关：

- `agent.artist-alias.enabled`
- `agent.artist-alias.refresh-on-startup`
- `agent.artist-alias.redis-key`
- `agent.artist-alias.redis-ttl-seconds`
- `agent.artist-alias.override-mappings`

## 12. 当前边界与后续建议

当前实现优势：

- 全链路已可用
- 本地数据闭环稳定
- 可观测性较好

可继续增强：

- 引入真实向量引擎（Milvus/pgvector/ES vector）替代轻量 bi-gram
- 加入离线评测集（Recall@K、MRR）量化 RAG 改动收益
- 增加管理台“RAG 命中诊断面板”用于线上调优

---

如果你希望，我可以再补一版“按你当前数据库实际样本演示的检索案例文档”（例如 Coldplay、Yellow、风格歌单三条真实请求的检索与排序过程）。

