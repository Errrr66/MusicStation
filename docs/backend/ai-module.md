# AI 模块设计文档

本文档说明音乐平台后端 AI 模块的架构、核心服务、RAG 检索与配置项。

## 架构概述

AI 模块基于 **Spring AI 1.0.0** 构建，通过 OpenAI 兼容协议对接阿里云 DashScope 提供的 DeepSeek 模型。系统同时保留旧版 `DeepSeekService` / `AgentRagService` / `MusicAgentService`，新旧服务共存，逐步迁移。

```
com.example.music.ai
├── AiService                     # 统一 AI 服务接口
├── SpringAiService               # Spring AI 实现（同步/流式/工具调用）
├── AiServiceProperties           # 配置属性绑定
├── ModelConfigManager            # 模型配置管理
├── LoadBalancedChatModel         # 负载均衡聊天模型
├── AiCallLogger                  # 全链路调用日志
├── StreamingChatService          # 真流式聊天服务
└── rag/
    ├── SpringAiRagService        # 基于 Embedding 的语义检索
    ├── DocumentChunker           # 文档分块
    ├── RagAdvisor                # RAG Advisor
    └── KnowledgeBaseInitializer  # 知识库初始化
```

## 核心接口（`AiService`）

| 方法 | 说明 |
|------|------|
| `chat(AiRequest)` | 同步对话，返回完整响应 |
| `chatStream(AiRequest)` | 流式对话，返回 `Stream<String>` 逐 token 输出 |
| `chatWithTools(AiRequest, tools)` | 带工具调用能力的对话（基础设施已就绪） |
| `isAvailable()` | 健康检查 |
| `embed(String)` | 生成文本 Embedding 向量 |

### 请求/响应记录

- `AiRequest`：包含 message、history、systemPrompt、options、requestId、userId
- `AiResponse`：包含 content、model、latencyMs、promptTokens、completionTokens、requestId、fallback

## SpringAiService

统一 AI 服务实现，特性包括：

- **模型接入**：通过 `ChatClient` 调用 DashScope `/chat/completions`
- **容错机制**：使用 Resilience4j 的 `@Retry` + `@CircuitBreaker`，失败时进入 fallback
- **流式输出**：`chatStream` 使用 `ChatClient.stream().content().toStream()` 实现真流式
- **调用日志**：`AiCallLogger` 记录请求、响应、流式分片与错误

## RAG 检索

### SpringAiRagService

- 基于 `SimpleVectorStore`（内存实现）
- 使用 `EmbeddingModel` 对查询文本进行向量化
- 支持 `retrieve(query, topK)` 语义检索
- 与旧 `AgentRagService` 并存，互不影响

### 文档处理

- `DocumentChunker`：按字符长度分块，支持重叠窗口
- `KnowledgeBaseInitializer`：启动时自动索引歌曲/歌手信息（由 `ai.rag.auto-index` 控制）

### 配置项

| 配置 | 默认值 | 说明 |
|------|--------|------|
| `ai.rag.auto-index` | false | 启动时是否自动构建索引 |
| `ai.rag.index-batch-size` | 100 | 每批写入向量库的文档数 |
| `ai.rag.chunk-size` | 500 | 单块文档字符长度 |
| `ai.rag.chunk-overlap` | 100 | 相邻分块重叠字符数 |
| `ai.rag.top-k` | 4 | 检索返回条数 |

## 聊天端点

| 端点 | 说明 |
|------|------|
| `POST /chat/ask` | 普通 DeepSeek 对话 |
| `POST /chat/agent` | 音乐 Agent 对话 |
| `POST /chat/agent/stream` | 伪流式 SSE |
| `POST /chat/agent/stream/v2` | 真流式 SSE（推荐） |
| `POST /chat/agent/savePlaylist` | 保存 Agent 推荐歌单 |

## AI 服务配置（`ai.service`）

| 配置 | 默认值 | 说明 |
|------|--------|------|
| `default-provider` | openai-compatible | 默认 Provider |
| `connect-timeout-seconds` | 10 | 连接超时 |
| `read-timeout-seconds` | 60 | 读取超时 |
| `retry.max-attempts` | 3 | 最大重试次数 |
| `retry.backoff-millis` | 1000 | 重试间隔 |
| `circuit-breaker.failure-rate-threshold` | 0.5 | 熔断失败率阈值 |
| `circuit-breaker.wait-duration-seconds` | 60 | 熔断开启后等待时间 |
| `fallback.enabled` | true | 是否启用降级响应 |
| `fallback.message` | AI 服务暂时不可用... | 降级提示文案 |

## 模型配置

通过 `application.yml` 中的 `spring.ai.openai` 配置：

| 配置 | 默认值 | 说明 |
|------|--------|------|
| `api-key` | `${DEEPSEEK_API_KEY}` | DashScope API Key |
| `base-url` | `https://dashscope.aliyuncs.com/compatible-mode/v1` | 兼容端点 |
| `chat.options.model` | `deepseek-v4-flash` | 聊天模型 |
| `chat.options.temperature` | 0.7 | 生成温度 |
| `chat.options.max-tokens` | 2048 | 最大 token 数 |
| `embedding.options.model` | `text-embedding-v3` | Embedding 模型 |
