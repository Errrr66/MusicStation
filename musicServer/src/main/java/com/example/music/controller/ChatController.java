package com.example.music.controller;

import com.example.music.model.dto.ChatRequestDTO;
import com.example.music.model.dto.AgentChatRequestDTO;
import com.example.music.model.dto.AgentPlaylistSaveDTO;
import com.example.music.model.vo.AgentChatResponseVO;
import com.example.music.model.vo.AgentPlaylistSaveVO;
import com.example.music.result.Result;
import com.example.music.service.DeepSeekService;
import com.example.music.service.MusicAgentService;
import com.example.music.service.SpeechService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private static final ExecutorService STREAM_EXECUTOR = Executors.newCachedThreadPool();

    private final DeepSeekService deepSeekService;
    private final SpeechService speechService;
    private final MusicAgentService musicAgentService;

    @Autowired
    public ChatController(DeepSeekService deepSeekService,
                          SpeechService speechService,
                          MusicAgentService musicAgentService) {
        this.deepSeekService = deepSeekService;
        this.speechService = speechService;
        this.musicAgentService = musicAgentService;
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

    @PostMapping("/agent")
    public Result<AgentChatResponseVO> askAgent(@RequestBody AgentChatRequestDTO requestDTO,
                                                HttpServletRequest request) {
        if ((requestDTO.getMessage() == null || requestDTO.getMessage().isEmpty())
                && (requestDTO.getMessages() == null || requestDTO.getMessages().isEmpty())) {
            return Result.error("Message cannot be empty");
        }
        return Result.success("Success", musicAgentService.handle(requestDTO, request));
    }

    @PostMapping("/agent/stream")
    public SseEmitter askAgentStream(@RequestBody AgentChatRequestDTO requestDTO,
                                     HttpServletRequest request) {
        SseEmitter emitter = new SseEmitter(120000L);

        if ((requestDTO.getMessage() == null || requestDTO.getMessage().isEmpty())
                && (requestDTO.getMessages() == null || requestDTO.getMessages().isEmpty())) {
            try {
                emitter.send(SseEmitter.event().name("error").data("Message cannot be empty"));
            } catch (Exception ignored) {
            }
            emitter.complete();
            return emitter;
        }

        emitter.onTimeout(emitter::complete);
        emitter.onCompletion(() -> {
        });

        CompletableFuture.runAsync(() -> {
            try {
                emitter.send(SseEmitter.event().name("start").data("ok"));
                AgentChatResponseVO responseVO = musicAgentService.handle(requestDTO, request);
                String answer = responseVO.getAnswer() == null ? "" : responseVO.getAnswer();
                for (String chunk : splitAnswer(answer, 12)) {
                    emitter.send(SseEmitter.event().name("delta").data(chunk));
                }
                String donePayload = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(responseVO);
                emitter.send(SseEmitter.event().name("done").data(donePayload));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data("Stream failed: " + e.getMessage()));
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
            }
        }, STREAM_EXECUTOR);

        return emitter;
    }

    @PostMapping("/agent/savePlaylist")
    public Result<AgentPlaylistSaveVO> saveAgentPlaylist(@RequestBody AgentPlaylistSaveDTO saveDTO,
                                                         HttpServletRequest request) {
        return Result.success("Success", musicAgentService.saveAgentPlaylist(saveDTO, request));
    }

    private List<String> splitAnswer(String text, int chunkSize) {
        java.util.ArrayList<String> chunks = new java.util.ArrayList<>();
        if (text == null || text.isEmpty() || chunkSize <= 0) {
            return chunks;
        }
        for (int i = 0; i < text.length(); i += chunkSize) {
            int end = Math.min(i + chunkSize, text.length());
            chunks.add(text.substring(i, end));
        }
        return chunks;
    }
}
