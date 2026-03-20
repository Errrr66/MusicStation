好的，为了在您的 Java 项目中调用这个语音模型，我为您准备了一个完整的方案。
1. 核心架构
您描述的流程如下： Java后端 -> DeepSeek (获取回复) -> Style-Bert-VITS2 (生成语音) -> 前端播放
目前 Style-Bert-VITS2 的 API 服务已经为您启动，监听在 http://127.0.0.1:5000。您只需要在 Java 代码中调用这个 HTTP 接口即可。
2. Java 调用示例代码
我已为您在项目根目录下生成了一个示例文件 SpeechClient.java。您可以直接参考或复制到您的项目中。
主要逻辑：
构建 HTTP GET 请求，指向 `http://127.0.0.1:5000/voice`。
参数 text 填入您想让它说的话（建议让 DeepSeek 返回日文，或者先翻译成日文）。
参数 model_id 填 6（这是为您查到的 Murasame 模型的 ID）。
将返回的二进制流保存为 .wav 文件。
// SpeechClient.java 部分代码预览
public static void generateSpeech(String text, int modelId, String outputFileName) {
    // ... 构建 URL ...
    String url = String.format("%s?text=%s&model_id=%d&encoding=utf-8", 
                                "http://127.0.0.1:5000/voice", encodedText, modelId);
    
    // ... 发送请求并保存文件 ...
}
3. 具体实施步骤
步骤 A: 确保 API 服务运行
之前的操作中，我已经帮您启动了 server_fastapi.py，它比网页版更适合 API 调用。如果不小心关闭了，请在终端（激活虚拟环境后）运行：
cd Style-Bert-VITS2
python server_fastapi.py
(当前状态：已在后台运行)
步骤 B: 在 Java 中集成 DeepSeek 和 TTS
在您的 Java 业务逻辑中，应该像下面这样串联：
用户输入：接收前端传来的中文文本（例如："你好"）。
调用 LLM (DeepSeek)：
Prompt 建议："你是一个日本动漫角色，请用日文回答用户的中文提问。用户说：{用户输入}"
假设 DeepSeek 返回："こんにちは、何か用？"
调用 TTS (Style-Bert-VITS2)：
使用 SpeechClient.generateSpeech("こんにちは、何か用？", 6, "reply.wav")。
返回前端：
将 DeepSeek 返回的日文文本（用于显示字幕）。
将生成的 reply.wav 音频文件（或其 URL）。
(可选) 如果需要中文翻译，可以让 DeepSeek 返回 JSON 格式：{"jp": "...", "cn": "..."}，分别用于 TTS 和字幕显示。
步骤 C: 模型 ID 说明
经过查询，您的环境中 Murasame 模型的 ID 是 6。 如果未来添加了其他模型，可以访问 http://127.0.0.1:5000/models/info 查看对应的 ID。
您可以查看项目根目录下的 SpeechClient.java 文件获取完整的 Java 实现。