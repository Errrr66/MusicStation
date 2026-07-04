package com.example.music.mapper;

import com.example.music.model.entity.Comment;
import com.example.music.model.vo.CommentVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>

 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    /**
     * 原子地增加评论点赞数
     *
     * @param commentId 评论ID
     * @return 受影响行数
     */
    @Update("UPDATE tb_comment SET like_count = like_count + 1 WHERE id = #{commentId}")
    int incrementLikeCount(@Param("commentId") Long commentId);

    /**
     * 原子地减少评论点赞数（不低于 0）
     *
     * @param commentId 评论ID
     * @return 受影响行数
     */
    @Update("UPDATE tb_comment SET like_count = GREATEST(like_count - 1, 0) WHERE id = #{commentId}")
    int decrementLikeCount(@Param("commentId") Long commentId);

    /**
     * 查询某歌曲的评论列表（独立查询，避免笛卡尔积）
     */
    @Select("SELECT c.id AS commentId, c.user_id AS userId, u.username AS username, " +
            "u.user_avatar AS userAvatar, c.content AS content, c.create_time AS createTime, c.like_count AS likeCount " +
            "FROM tb_comment c LEFT JOIN tb_user u ON c.user_id = u.id " +
            "WHERE c.song_id = #{songId} AND c.type = 0 ORDER BY c.create_time DESC")
    List<CommentVO> listSongComments(@Param("songId") Long songId);

    /**
     * 查询某歌单的评论列表（独立查询，避免笛卡尔积）
     */
    @Select("SELECT c.id AS commentId, c.user_id AS userId, u.username AS username, " +
            "u.user_avatar AS userAvatar, c.content AS content, c.create_time AS createTime, c.like_count AS likeCount " +
            "FROM tb_comment c LEFT JOIN tb_user u ON c.user_id = u.id " +
            "WHERE c.playlist_id = #{playlistId} AND c.type = 1 ORDER BY c.create_time DESC")
    List<CommentVO> listPlaylistComments(@Param("playlistId") Long playlistId);

}
