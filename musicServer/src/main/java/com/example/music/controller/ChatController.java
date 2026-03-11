package com.example.music.controller;

import com.example.music.model.dto.ChatRequestDTO;
import com.example.music.result.Result;
import com.example.music.service.DeepSeekService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final DeepSeekService deepSeekService;

    @Autowired
    public ChatController(DeepSeekService deepSeekService) {
        this.deepSeekService = deepSeekService;
    }

    @PostMapping("/ask")
    public Result<String> ask(@RequestBody ChatRequestDTO chatRequestDTO) {
        if (chatRequestDTO.getMessage() == null || chatRequestDTO.getMessage().isEmpty()) {
            return Result.error("Message cannot be empty");
        }
        String response = deepSeekService.chat(chatRequestDTO);
        return Result.success("Success", response);
    }
}

