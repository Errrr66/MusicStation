# 客户端文档（musicClient）

## 2026-04-18 语音交互与 Orb 体验优化

- 聊天页新增 `TTS / 语音` 双模式切换。
- 仅在语音模式展示 Orb，可视化不再干扰文字聊天流。
- 语音输入改为实时问答链路（支持 barge-in 打断与续问）。
- Orb 接入真实音量采样：麦克风输入与 AI TTS 输出都会驱动变化。
- Orb 状态与颜色切换更丝滑（`listening / thinking / talking`）。
- 优化语音识别稳定性（安全启停、重复启动保护、短词噪声过滤）。
- 修复 AI 回答播放与输出分析兼容问题，保证“能听到 + 能看到”。

## 2026-04-06 最终体验层优化

- 聊天页新增健康状态栏（RAG、DeepSeek、TTS）。
- 聊天页支持健康检查手动刷新与详情折叠。
- 流式生成状态可视化（连接中/生成中/完成/失败）。
- 展示首字延迟与总耗时。
- 引用卡片支持点击跳转（歌单详情/曲库搜索）。

## 开发命令

```bat
cd /d "E:\IntelliJ IDEA\workspace\music\musicClient"
pnpm dev
pnpm type-check
pnpm build
```

## 关联文档

- `../README.md`
- `../operations/ai-agent-runbook.md`
- `../changelog/2026-04-06.md`
- `../changelog/2026-04-18.md`
- `../standards/markdown-style-guide.md`
