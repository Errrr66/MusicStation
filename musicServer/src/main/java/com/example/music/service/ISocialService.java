package com.example.music.service;

import com.example.music.model.dto.PrivateMessageDTO;
import com.example.music.model.vo.ConversationVO;
import com.example.music.model.vo.PrivateMessageVO;
import com.example.music.model.vo.UserProfileVO;
import com.example.music.model.vo.UserSimpleVO;
import com.example.music.result.Result;

import java.util.List;

public interface ISocialService {

    Result<UserProfileVO> getUserProfile(Long targetUserId);

    Result<String> followUser(Long targetUserId);

    Result<String> unfollowUser(Long targetUserId);

    Result<List<UserSimpleVO>> getFollowers(Long userId);

    Result<List<UserSimpleVO>> getFollowing(Long userId);

    Result<List<ConversationVO>> getConversations();

    Result<List<PrivateMessageVO>> getMessages(Long friendId, Integer pageNum, Integer pageSize);

    Result<String> sendMessage(PrivateMessageDTO dto);
}


