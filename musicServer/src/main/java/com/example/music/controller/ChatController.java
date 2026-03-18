package com.example.music.controller;

import com.example.music.model.dto.ChatRequestDTO;
import com.example.music.result.Result;
import com.example.music.service.DeepSeekService;
import com.example.music.service.SpeechService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final DeepSeekService deepSeekService;
    private final SpeechService speechService;

    @Autowired
    public ChatController(DeepSeekService deepSeekService, SpeechService speechService) {
        this.deepSeekService = deepSeekService;
        this.speechService = speechService;
    }

    @PostMapping("/ask")
    public Result<Map<String, String>> ask(@RequestBody ChatRequestDTO chatRequestDTO) {
        if ((chatRequestDTO.getMessage() == null || chatRequestDTO.getMessage().isEmpty()) &&
                (chatRequestDTO.getMessages() == null || chatRequestDTO.getMessages().isEmpty())) {
            return Result.error("Message cannot be empty");
        }

        // 调用 AI (Single Request Mode)
        String fullResponse = deepSeekService.chat(chatRequestDTO);
        System.out.println("DeepSeek Response: " + fullResponse);

        String chineseResponse = fullResponse;
        String japaneseResponse = "";

        // 解析 JSON
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, String> jsonMap = mapper.readValue(fullResponse, new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
            chineseResponse = jsonMap.get("chinese");
            japaneseResponse = jsonMap.get("japanese");
        } catch (Exception e) {
            // 解析失败（可能 AI 没有返回 JSON），则直接作为中文文本
            chineseResponse = fullResponse;
            japaneseResponse = "";
            System.err.println("JSON Parsing failed: " + e.getMessage());
        }

        // 兜底清洗，确保没有动作描写残留
        if (chineseResponse == null) chineseResponse = "";
        
        // 生成语音
        String audioUrl = "";
        if (japaneseResponse != null && !japaneseResponse.isEmpty()) {
             // 清洗：去掉可能存在的日文括号备注（双保险）
             String cleanJapanese = japaneseResponse.replaceAll("(?s)（.*?）", "").replaceAll("(?s)\\(.*?\\)", "").trim();
             if (!cleanJapanese.isEmpty()) {
                audioUrl = speechService.generateAndUploadSpeech(cleanJapanese, 6);
             }
        }

        Map<String, String> data = new HashMap<>();
        data.put("answer", chineseResponse);
        data.put("audio", audioUrl);

        return Result.success("Success", data);
    }
}
