package com.example.music.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SpeechService {

    private static final String TTS_API_URL = "http://127.0.0.1:5000/voice";
    private final MinioService minioService;

    @Autowired
    public SpeechService(MinioService minioService) {
        this.minioService = minioService;
    }

    /**
     * 调用语音合成 API 并上传到 MinIO
     *
     * @param text      要转换的文本 (建议日文)
     * @param modelId   模型 ID (Murasame 为 6)
     * @return 音频文件的 URL
     */
    public String generateAndUploadSpeech(String text, int modelId) {
        try {
            // URL 编码参数
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);

            // 构建请求 URL (GET 参数)
            String url = String.format("%s?text=%s&model_id=%d&encoding=utf-8&length=1.0",
                    TTS_API_URL, encodedText, modelId);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == 200) {
                try (InputStream inputStream = response.body()) {
                   // 上传到 MinIO
                   return minioService.uploadFile(inputStream, "speech.wav", "audio/wav", "speech");
                }
            } else {
                System.err.println("TTS 请求失败, 状态码: " + response.statusCode());
                return null;
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }
}

