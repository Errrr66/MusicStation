# TTS 集成历史归档说明

本文档由项目根目录 `notice.md` 归档而来，保留早期 TTS 集成草案，供历史追溯。

## 1. 核心架构

流程：`Java 后端 -> DeepSeek（文本回复）-> Style-Bert-VITS2（语音生成）-> 前端播放`。

当时约定的本地 TTS API：`http://127.0.0.1:5000`。

## 2. Java 调用要点

- 请求地址：`http://127.0.0.1:5000/voice`
- 参数：
  - `text`：待合成文本（可使用 DeepSeek 生成后的文本）
  - `model_id`：示例为 `6`（Murasame）
- 返回：二进制音频流，保存为 `.wav`

```java
public static void generateSpeech(String text, int modelId, String outputFileName) {
    String url = String.format(
        "%s?text=%s&model_id=%d&encoding=utf-8",
        "http://127.0.0.1:5000/voice",
        encodedText,
        modelId
    );
    // ...发送请求并保存文件...
}
```

## 3. 实施步骤

### 步骤 A：确保 API 服务运行

```bash
cd Style-Bert-VITS2
python server_fastapi.py
```

### 步骤 B：在 Java 中串联 LLM 与 TTS

1. 接收前端中文输入。
2. 调用 DeepSeek 获取回复（可按业务提示词约束输出风格）。
3. 调用 `SpeechClient.generateSpeech(...)` 生成音频。
4. 返回文本与音频 URL 给前端。

可选结构化返回：

```json
{
  "jp": "...",
  "cn": "..."
}
```

### 步骤 C：模型 ID

- 历史记录中，`Murasame` 的 `model_id` 为 `6`。
- 可通过 `http://127.0.0.1:5000/models/info` 查询当前模型 ID。

## 4. 备注

- 本文档为历史草案归档，保留用于追溯。
- 实际生产配置请以当前服务代码与部署配置为准。
