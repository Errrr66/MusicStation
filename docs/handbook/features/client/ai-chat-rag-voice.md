# AI 聊天、RAG 与语音交互

## 功能边界

- 提供文本聊天、流式回复、引用展示。
- 支持 RAG 检索增强与健康检查可视化。
- 支持语音输入、TTS 播放与 Orb 可视化联动。

## 用户入口 / 角色

- 入口页面：`musicClient/src/pages/chat/index.vue`。
- 使用角色：普通登录用户。

## 页面与交互

- 文本模式：消息流、引用卡片、工具轨迹、流式状态。
- 语音模式：麦克风实时输入、AI 语音播放、Orb 状态可视化。
- 健康信息：RAG/DeepSeek/TTS 状态与最近检索指标展示。

## 接口清单（重点）

- `POST /chat/agent`
- `POST /chat/agent/stream`
- `GET /chat/health`
- `POST /chat/agent/savePlaylist`

## 前置条件

- 用户登录状态有效，前端可携带 `Authorization`。
- 后端 `deepseek` 与 `tts` 配置可用。
- 浏览器允许麦克风权限（语音模式）。

## 关键流程

1. 用户发送文本或语音转写结果。
2. 后端识别意图，按需执行 RAG 检索。
3. 前端接收流式增量内容并实时渲染。
4. 若返回音频地址，触发 TTS 播放并驱动 Orb 输出振幅。

## 配置项（重点）

- `agent.rag.*`
- `deepseek.*`
- `tts.api-url`

## 验证步骤

1. 文本模式提问，确认流式响应与引用卡片正常显示。
2. 语音模式提问，确认识别、回答与 TTS 播放正常。
3. 观察 Orb 在 `listening / thinking / talking` 状态下的变化。
4. 点击健康检查，确认 RAG 与服务状态可见。

## 已知限制与待确认

- 浏览器语音识别能力受内核支持与权限策略影响。
- 不同浏览器对音频分析器挂载行为可能不同（已做兜底策略）。

## 关联文档

- [AI Agent 运行手册](../../operations/ai-agent-runbook.md)
- [Agent 接口文档](../../server/agent-api.md)
- [RAG 实现详解](../../server/rag-implementation.md)
- [变更记录 2026-04-18](../../changelog/2026-04-18.md)
