package com.example.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.music.constant.JwtClaimsConstant;
import com.example.music.mapper.PlaylistMapper;
import com.example.music.mapper.PrivateMessageMapper;
import com.example.music.mapper.SongMapper;
import com.example.music.mapper.UserFavoriteMapper;
import com.example.music.mapper.UserFollowMapper;
import com.example.music.mapper.UserMapper;
import com.example.music.model.dto.PrivateMessageDTO;
import com.example.music.model.entity.PrivateMessage;
import com.example.music.model.entity.User;
import com.example.music.model.entity.UserFollow;
import com.example.music.model.vo.ConversationVO;
import com.example.music.model.vo.PlaylistVO;
import com.example.music.model.vo.PrivateMessageVO;
import com.example.music.model.vo.SongVO;
import com.example.music.model.vo.UserProfileVO;
import com.example.music.model.vo.UserSimpleVO;
import com.example.music.result.Result;
import com.example.music.service.ISocialService;
import com.example.music.util.ThreadLocalUtil;
import com.example.music.util.TypeConversionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class SocialServiceImpl implements ISocialService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserFollowMapper userFollowMapper;
    @Autowired
    private UserFavoriteMapper userFavoriteMapper;
    @Autowired
    private SongMapper songMapper;
    @Autowired
    private PlaylistMapper playlistMapper;
    @Autowired
    private PrivateMessageMapper privateMessageMapper;

    @Override
    public Result<UserProfileVO> getUserProfile(Long targetUserId) {
        User target = userMapper.selectById(targetUserId);
        if (target == null) {
            return Result.error("用户不存在");
        }

        Long loginUserId = getLoginUserId();
        boolean following = loginUserId != null && isFollowing(loginUserId, targetUserId);
        boolean mutualFollow = loginUserId != null && following && isFollowing(targetUserId, loginUserId);

        UserProfileVO profile = new UserProfileVO();
        profile.setUserId(target.getUserId());
        profile.setUsername(target.getUsername());
        profile.setUserAvatar(target.getUserAvatar());
        profile.setIntroduction(target.getIntroduction());
        profile.setFollowing(following);
        profile.setMutualFollow(mutualFollow);
        profile.setFollowerCount(userFollowMapper.countFollowers(targetUserId));
        profile.setFollowingCount(userFollowMapper.countFollowing(targetUserId));

        List<UserSimpleVO> followers = userFollowMapper.listFollowers(targetUserId);
        List<UserSimpleVO> followingUsers = userFollowMapper.listFollowing(targetUserId);
        enrichFollowFlags(followers, loginUserId);
        enrichFollowFlags(followingUsers, loginUserId);
        profile.setFollowers(followers);
        profile.setFollowingUsers(followingUsers);

        List<Long> favoriteSongIds = userFavoriteMapper.selectList(new QueryWrapper<com.example.music.model.entity.UserFavorite>()
                        .eq("user_id", targetUserId)
                        .eq("type", 0))
                .stream().map(com.example.music.model.entity.UserFavorite::getSongId)
                .filter(Objects::nonNull).toList();

        List<Long> favoritePlaylistIds = userFavoriteMapper.selectList(new QueryWrapper<com.example.music.model.entity.UserFavorite>()
                        .eq("user_id", targetUserId)
                        .eq("type", 1))
                .stream().map(com.example.music.model.entity.UserFavorite::getPlaylistId)
                .filter(Objects::nonNull).toList();

        List<SongVO> favoriteSongs = favoriteSongIds.isEmpty()
                ? Collections.emptyList()
                : songMapper.getSongsByIds(
                        new Page<>(1, 200),
                        favoriteSongIds,
                        loginUserId == null ? 0L : loginUserId,
                        null,
                        null,
                        null
                ).getRecords();

        List<PlaylistVO> favoritePlaylists = favoritePlaylistIds.isEmpty()
                ? Collections.emptyList()
                : playlistMapper.getPlaylistsByIds(
                        loginUserId == null ? 0L : loginUserId,
                        new Page<>(1, 200),
                        favoritePlaylistIds,
                        null,
                        null
                ).getRecords();

        profile.setFavoriteSongs(favoriteSongs);
        profile.setFavoritePlaylists(favoritePlaylists);
        return Result.success(profile);
    }

    @Override
    @Transactional
    public Result<String> followUser(Long targetUserId) {
        Long loginUserId = requireLoginUserId();
        if (loginUserId.equals(targetUserId)) {
            return Result.error("不能关注自己");
        }
        if (userMapper.selectById(targetUserId) == null) {
            return Result.error("用户不存在");
        }
        if (isFollowing(loginUserId, targetUserId)) {
            return Result.success("已关注");
        }

        UserFollow follow = new UserFollow();
        follow.setUserId(loginUserId);
        follow.setFollowUserId(targetUserId);
        follow.setCreateTime(LocalDateTime.now());
        userFollowMapper.insert(follow);
        return Result.success("关注成功");
    }

    @Override
    @Transactional
    public Result<String> unfollowUser(Long targetUserId) {
        Long loginUserId = requireLoginUserId();
        userFollowMapper.delete(new QueryWrapper<UserFollow>()
                .eq("user_id", loginUserId)
                .eq("follow_user_id", targetUserId));
        return Result.success("取消关注成功");
    }

    @Override
    public Result<List<UserSimpleVO>> getFollowers(Long userId) {
        Long loginUserId = getLoginUserId();
        List<UserSimpleVO> followers = userFollowMapper.listFollowers(userId);
        enrichFollowFlags(followers, loginUserId);
        return Result.success(followers);
    }

    @Override
    public Result<List<UserSimpleVO>> getFollowing(Long userId) {
        Long loginUserId = getLoginUserId();
        List<UserSimpleVO> following = userFollowMapper.listFollowing(userId);
        enrichFollowFlags(following, loginUserId);
        return Result.success(following);
    }

    @Override
    public Result<List<ConversationVO>> getConversations() {
        Long userId = requireLoginUserId();
        return Result.success(privateMessageMapper.listConversations(userId));
    }

    @Override
    public Result<List<PrivateMessageVO>> getMessages(Long friendId, Integer pageNum, Integer pageSize) {
        Long userId = requireLoginUserId();
        if (!isMutualFollow(userId, friendId)) {
            return Result.error("仅支持互相关注好友私信");
        }
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 30 : Math.min(pageSize, 100);
        int offset = (safePageNum - 1) * safePageSize;

        List<PrivateMessageVO> rows = privateMessageMapper.listMessages(userId, friendId, offset, safePageSize);
        List<PrivateMessageVO> messages = new ArrayList<>(rows);
        Collections.reverse(messages);
        privateMessageMapper.markAsRead(userId, friendId);
        return Result.success(messages);
    }

    @Override
    @Transactional
    public Result<String> sendMessage(PrivateMessageDTO dto) {
        Long userId = requireLoginUserId();
        if (dto.getToUserId() == null || userMapper.selectById(dto.getToUserId()) == null) {
            return Result.error("接收用户不存在");
        }
        if (!isMutualFollow(userId, dto.getToUserId())) {
            return Result.error("仅支持互相关注好友私信");
        }

        String messageType = dto.getMessageType();
        if (messageType == null || messageType.trim().isEmpty()) {
            messageType = "TEXT";
        }
        messageType = messageType.toUpperCase();

        if ("TEXT".equals(messageType) && (dto.getContent() == null || dto.getContent().trim().isEmpty())) {
            return Result.error("文本消息内容不能为空");
        }
        if ("SONG".equals(messageType) && dto.getSongId() == null) {
            return Result.error("分享歌曲不能为空");
        }
        if ("PLAYLIST".equals(messageType) && dto.getPlaylistId() == null) {
            return Result.error("分享歌单不能为空");
        }

        PrivateMessage message = new PrivateMessage();
        message.setFromUserId(userId);
        message.setToUserId(dto.getToUserId());
        message.setMessageType(messageType);
        message.setContent(dto.getContent());
        message.setSongId(dto.getSongId());
        message.setPlaylistId(dto.getPlaylistId());
        message.setReadStatus(0);
        message.setCreateTime(LocalDateTime.now());
        privateMessageMapper.insert(message);
        return Result.success("发送成功");
    }

    private void enrichFollowFlags(List<UserSimpleVO> list, Long loginUserId) {
        if (list == null || list.isEmpty()) {
            return;
        }
        // 批量查询当前登录用户关注的所有 userId，避免 N+1 查询
        Set<Long> loginFollowingSet;
        Set<Long> loginFollowerSet;
        if (loginUserId == null) {
            loginFollowingSet = Collections.emptySet();
            loginFollowerSet = Collections.emptySet();
        } else {
            loginFollowingSet = new HashSet<>(userFollowMapper.listFollowingIds(loginUserId));
            // 查询谁关注了当前登录用户，用于判断互相关注
            loginFollowerSet = new HashSet<>(userFollowMapper.listFollowersIds(loginUserId));
        }
        for (UserSimpleVO user : list) {
            if (loginUserId == null) {
                user.setFollowing(false);
                user.setMutualFollow(false);
                continue;
            }
            boolean following = loginFollowingSet.contains(user.getUserId());
            // 互相关注：我关注他 且 他关注我
            boolean mutual = following && loginFollowerSet.contains(user.getUserId());
            user.setFollowing(following);
            user.setMutualFollow(mutual);
        }
    }

    private Long requireLoginUserId() {
        Long userId = getLoginUserId();
        if (userId == null) {
            throw new IllegalStateException("未登录");
        }
        return userId;
    }

    private Long getLoginUserId() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        if (claims == null) {
            return null;
        }
        return TypeConversionUtil.toLong(claims.get(JwtClaimsConstant.USER_ID));
    }

    private boolean isFollowing(Long userId, Long targetUserId) {
        return userFollowMapper.existsFollow(userId, targetUserId) > 0;
    }

    private boolean isMutualFollow(Long userId, Long targetUserId) {
        return isFollowing(userId, targetUserId) && isFollowing(targetUserId, userId);
    }
}



