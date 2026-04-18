package com.example.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.music.model.entity.PrivateMessage;
import com.example.music.model.vo.ConversationVO;
import com.example.music.model.vo.PrivateMessageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PrivateMessageMapper extends BaseMapper<PrivateMessage> {

    @Select("""
            SELECT pm.id,
                   pm.from_user_id AS fromUserId,
                   pm.to_user_id AS toUserId,
                   pm.message_type AS messageType,
                   pm.content,
                   pm.song_id AS songId,
                   s.name AS songName,
                   a.name AS songArtistName,
                   s.cover_url AS songCoverUrl,
                   pm.playlist_id AS playlistId,
                   p.title AS playlistTitle,
                   p.cover_url AS playlistCoverUrl,
                   pm.create_time AS createTime,
                   u.username AS fromUsername,
                   u.user_avatar AS fromUserAvatar
            FROM tb_private_message pm
            JOIN tb_user u ON u.id = pm.from_user_id
            LEFT JOIN tb_song s ON s.id = pm.song_id
            LEFT JOIN tb_artist a ON a.id = s.artist_id
            LEFT JOIN tb_playlist p ON p.id = pm.playlist_id
            WHERE (pm.from_user_id = #{userId} AND pm.to_user_id = #{friendId})
               OR (pm.from_user_id = #{friendId} AND pm.to_user_id = #{userId})
            ORDER BY pm.id DESC
            LIMIT #{offset}, #{size}
            """)
    List<PrivateMessageVO> listMessages(@Param("userId") Long userId,
                                        @Param("friendId") Long friendId,
                                        @Param("offset") Integer offset,
                                        @Param("size") Integer size);

    @Select("""
            SELECT friend.id AS friendUserId,
                   friend.username AS friendUsername,
                   friend.user_avatar AS friendAvatar,
                   last_pm.content AS lastMessage,
                   last_pm.message_type AS lastMessageType,
                   COALESCE(unread.unreadCount, 0) AS unreadCount,
                   last_pm.create_time AS updatedAt
            FROM (
                SELECT CASE WHEN pm.from_user_id = #{userId} THEN pm.to_user_id ELSE pm.from_user_id END AS friend_id,
                       MAX(pm.id) AS last_message_id
                FROM tb_private_message pm
                WHERE pm.from_user_id = #{userId} OR pm.to_user_id = #{userId}
                GROUP BY CASE WHEN pm.from_user_id = #{userId} THEN pm.to_user_id ELSE pm.from_user_id END
            ) t
            JOIN tb_private_message last_pm ON last_pm.id = t.last_message_id
            JOIN tb_user friend ON friend.id = t.friend_id
            LEFT JOIN (
                SELECT from_user_id,
                       COUNT(1) AS unreadCount
                FROM tb_private_message
                WHERE to_user_id = #{userId} AND read_status = 0
                GROUP BY from_user_id
            ) unread ON unread.from_user_id = t.friend_id
            ORDER BY last_pm.id DESC
            """)
    List<ConversationVO> listConversations(@Param("userId") Long userId);

    @Update("""
            UPDATE tb_private_message
            SET read_status = 1
            WHERE from_user_id = #{friendId} AND to_user_id = #{userId} AND read_status = 0
            """)
    void markAsRead(@Param("userId") Long userId, @Param("friendId") Long friendId);
}


