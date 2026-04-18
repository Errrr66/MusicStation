# AI Agent 运行手册

本文档说明 `music` 项目当前 AI Agent 的架构、运行检查与排障步骤。

## 关联文档

- [文档手册首页](../README.md)
- [变更记录 2026-04-06](../changelog/2026-04-06.md)
- [变更记录 2026-04-18](../changelog/2026-04-18.md)
- [Agent 接口文档](../server/agent-api.md)

## 适用范围

- 后端：`musicServer`
- 前端：`musicClient`
- 关注点：Agent 聊天、RAG 检索、SSE 流式响应、TTS、健康检查可视化

## 1. 当前架构

### 1.1 请求流程

1. 客户端调用 `POST /chat/agent` 或 `POST /chat/agent/stream`。
2. 后端在 `MusicAgentService` 中识别意图。
3. 可选触发 `AgentRagService` 执行 RAG 检索。
4. 将检索上下文拼接到提示词。
5. 大模型生成最终文本回复。
6. 可选执行 TTS 生成音频 URL。
7. 返回结果包含 `toolTrace`、`songs`、`playlists`、`citations`。

### 1.2 RAG 模式

配置位置：`musicServer/src/main/resources/application.yml`

- `keyword`：仅关键词排序
- `vector`：轻量向量近似排序
- `hybrid`：关键词 + 向量加权排序（默认）

### 1.3 健康检查接口

`GET /chat/health`

返回信息：

- RAG 开关与模式
- 最近一次检索统计（`queryCount`、`candidateCount`、`citationCount`）
- 服务提供方状态（`deepseekConfigured`、`ttsConfigured`）

该接口不返回密钥等敏感信息。

## 2. 关键配置

`musicServer/src/main/resources/application.yml`

- `deepseek.api-key`
- `deepseek.base-url`
- `deepseek.model`
- `tts.api-url`
- `agent.search.*`
- `agent.rag.enabled`
- `agent.rag.top-k`
- `agent.rag.max-snippet-length`
- `agent.rag.mode`
- `agent.rag.keyword-weight`
- `agent.rag.vector-weight`

## 3. 前端交互能力

涉及文件：`musicClient/src/pages/chat/index.vue`

- 流式状态栏（`connecting`、`streaming`、`done`、`error`）
- 首字延迟与总耗时展示
- 可折叠健康面板（RAG/服务状态）
- 引用卡片点击行为：
  - `playlist`：跳转 `/playlist/:id`
  - `song`：直接播放或降级到曲库搜索

## 4. API 契约重点

### 4.1 Agent 请求

`AgentChatRequestDTO` 支持字段：

- `message`
- `messages`
- `nowPlaying`
- `enableVoice`
- `enableRag`
- `limit`

### 4.2 Agent 响应

`AgentChatResponseVO` 包含字段：

- `answer`
- `audio`
- `intent`
- `playerCommand`
- `toolTrace`
- `songs`
- `playlists`
- `citations`
- `musicArchive`

## 5. 验证命令

在 Windows `cmd.exe` 中执行：

```bat
cd /d "E:\IntelliJ IDEA\workspace\music\musicServer"
mvnw.cmd -q -DskipTests compile
```

```bat
cd /d "E:\IntelliJ IDEA\workspace\music\musicClient"
pnpm type-check
pnpm build
```

## 6. 常见问题排查

### 6.1 未返回引用

- 检查 `/chat/health` 中 `ragEnabled`。
- 降低查询复杂度后重试。
- 确认本地数据存在可匹配歌曲/歌单。

### 6.2 流式中断

- 使用聊天界面的停止/继续控制。
- 在浏览器网络面板检查是否出现 `event: error`。
- 可改用非流式接口 `POST /chat/agent` 重试。

### 6.3 大模型不可用

- 检查 `/chat/health` 中 `deepseekConfigured`。
- 校验 `deepseek.api-key` 与 `deepseek.base-url`。

### 6.4 TTS 无音频

- 检查 `/chat/health` 中 `ttsConfigured`。
- 检查 `tts.api-url` 服务可用性。

## 7. 运维建议

- 密钥统一通过环境变量注入，避免硬编码到仓库。
- ` /chat/health` 的暴露范围按部署策略收敛。
- 线上默认建议使用 `hybrid` 模式，兼顾相关性与稳定性。
