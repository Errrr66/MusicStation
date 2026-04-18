# Agent 与检索能力总览

## 功能边界

- 提供 Agent 对话编排、意图识别、RAG 检索增强。
- 提供同步/流式回复与健康检查观测能力。

## 服务入口

- `POST /chat/agent`
- `POST /chat/agent/stream`
- `GET /chat/health`
- `POST /chat/rag/evaluate`

## 前置条件

- `deepseek` 配置有效且可访问。
- `tts.api-url` 服务可用（语音场景）。
- `agent.rag.*` 检索参数已配置。

## 关键流程

1. 接收请求并识别意图。
2. 根据意图决定是否触发 RAG。
3. 执行检索、评分、引用构建。
4. 生成回答并按需返回音频。
5. 前端展示引用、卡片和流式状态。

## 数据与策略

- 检索模式：`keyword` / `vector` / `hybrid`。
- 支持二阶段 rerank、语义向量与回退策略。
- 通过 `/chat/health` 暴露关键运行状态。

## 配置项（重点）

- `agent.rag.*`
- `agent.search.*`
- `deepseek.*`
- `tts.api-url`

## 验证步骤

1. 调用 `POST /chat/agent`，确认返回 `answer` 与结构化数据。
2. 调用 `POST /chat/agent/stream`，确认流式事件顺序正确。
3. 调用 `GET /chat/health`，确认 `ragEnabled`、`ragMode`、`providers` 字段可见。
4. 调用 `POST /chat/rag/evaluate`，确认可返回评估指标。

## 已知限制与待确认

- 语义向量能力依赖外部服务，未配置时会回退轻量策略。
- 评估样本集规模与自动化回归覆盖仍待持续完善。

## 关联文档

- [Agent 接口文档](../../server/agent-api.md)
- [RAG 实现详解](../../server/rag-implementation.md)
- [AI Agent 运行手册](../../operations/ai-agent-runbook.md)
