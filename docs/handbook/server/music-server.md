# 服务端文档（musicServer）

## 文档入口

- 运行手册：`../operations/ai-agent-runbook.md`
- 变更记录（2026-04-06）：`../changelog/2026-04-06.md`
- 变更记录（2026-04-18）：`../changelog/2026-04-18.md`
- AI Agent 接口：`agent-api.md`
- RAG 实现详解：`rag-implementation.md`

## 运行环境

| 组件 | 启动方式 | 访问地址 |
| --- | --- | --- |
| MinIO | 在 `minio/bin` 目录执行：`minio.exe server "项目路径\minio\data"` | - |
| Redis | 启动本地或部署环境 Redis 服务 | - |
| 管理端 | 在 `musicAdmin` 执行：`pnpm dev` | `http://localhost:8089` |
| 前端 | 在 `musicClient` 执行：`pnpm dev` | `http://localhost:8090` |
| 后端 | 运行 `VibeMusicApplication` 的 `main` 方法 | - |

## 核心能力

### 后端能力

- 集成 DeepSeek（OpenAI 兼容协议）。
- 提供 Agent 同步接口与 SSE 流式接口。
- 支持 RAG 检索增强与引用回传。
- 支持可选 TTS 语音输出。
- 提供健康检查接口：`GET /chat/health`。

### 前端协同能力

- 聊天页支持流式状态、健康检查、引用跳转。
- 支持 AI 语音回复与语音可视化能力。

## 历史更新摘要

### 2026-03-04（v1.0.1）

- 新增 DeepSeek 服务集成。
- 新增 `ChatController` 与聊天接口能力。
- 新增 `RestTemplateConfig`。
- 完善统一返回模型 `Result<T>` 的泛型支持。

### 2026-03-10

- 新增 MP3 歌词提取与 LRC 展示能力。
- 音乐详情抽屉支持歌词自动滚动与点击定位。
- 优化音乐抽屉 UI（头部信息精简、主题切换图标统一）。
- 新增 `jaudiotagger` 依赖用于音频元数据提取。

## 术语说明（历史补充）

- `ID3 tags`：译为“ID3 标签”。
- `LRC format`：译为“LRC 格式”。
- `endpoint`：译为“接口”。
