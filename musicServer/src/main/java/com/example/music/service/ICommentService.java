package com.example.music.service;

import com.example.music.model.dto.CommentPlaylistDTO;
import com.example.music.model.dto.CommentSongDTO;
import com.example.music.model.entity.Comment;
import com.example.music.result.Result;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 服务类
 * </p>
  
 */
public interface ICommentService extends IService<Comment> {

    // 新增歌曲评论
    Result addSongComment(CommentSongDTO commentSongDTO);

    // 新增歌单评论
    Result addPlaylistComment(CommentPlaylistDTO commentPlaylistDTO);

    // 点赞评论
    Result likeComment(Long commentId);

    // 取消点赞评论
    Result cancelLikeComment(Long commentId);

    // 删除评论
    Result deleteComment(Long commentId);

}
