package com.example.music.controller;

import com.example.music.model.dto.PrivateMessageDTO;
import com.example.music.model.vo.ConversationVO;
import com.example.music.model.vo.PrivateMessageVO;
import com.example.music.model.vo.UserProfileVO;
import com.example.music.model.vo.UserSimpleVO;
import com.example.music.result.Result;
import com.example.music.service.ISocialService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/social")
public class SocialController {

    @Autowired
    private ISocialService socialService;

    @GetMapping("/profile/{userId}")
    public Result<UserProfileVO> getProfile(@PathVariable Long userId) {
        return socialService.getUserProfile(userId);
    }

    @PostMapping("/follow/{targetUserId}")
    public Result<String> follow(@PathVariable Long targetUserId) {
        return socialService.followUser(targetUserId);
    }

    @DeleteMapping("/follow/{targetUserId}")
    public Result<String> unfollow(@PathVariable Long targetUserId) {
        return socialService.unfollowUser(targetUserId);
    }

    @GetMapping("/followers/{userId}")
    public Result<List<UserSimpleVO>> followers(@PathVariable Long userId) {
        return socialService.getFollowers(userId);
    }

    @GetMapping("/following/{userId}")
    public Result<List<UserSimpleVO>> following(@PathVariable Long userId) {
        return socialService.getFollowing(userId);
    }

    @GetMapping("/conversations")
    public Result<List<ConversationVO>> conversations() {
        return socialService.getConversations();
    }

    @GetMapping("/messages/{friendId}")
    public Result<List<PrivateMessageVO>> messages(@PathVariable Long friendId,
                                                   @RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "30") Integer pageSize) {
        return socialService.getMessages(friendId, pageNum, pageSize);
    }

    @PostMapping("/messages")
    public Result<String> sendMessage(@RequestBody @Valid PrivateMessageDTO dto) {
        return socialService.sendMessage(dto);
    }
}


