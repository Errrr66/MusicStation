package com.example.music.mapper;

import com.example.music.model.entity.Playlist;
import com.example.music.model.vo.PlaylistDetailVO;
import com.example.music.model.vo.PlaylistVO;
import com.example.music.model.vo.SongVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>

 */
@Mapper
public interface PlaylistMapper extends BaseMapper<Playlist> {

    // 根据歌单id获取歌单详情
    PlaylistDetailVO getPlaylistDetailById(Long playlistId);

    // 查询歌单下的歌曲列表（独立查询，避免笛卡尔积）
    List<SongVO> listPlaylistSongs(@Param("playlistId") Long playlistId);

    // 获取用户收藏歌单的风格
    List<String> getFavoritePlaylistStyles(List<Long> favoritePlaylistIds);

    // 根据风格推荐歌单（排除已收藏歌单）
    List<PlaylistVO> getRecommendedPlaylistsByStyles(List<Long> sortedStyleIds, List<Long> favoritePlaylistIds, int limit);

    // 随机推荐歌单（避免 ORDER BY RAND() 性能问题，改用 id DESC）
    @Select("SELECT " +
            "p.id AS playlistId, " +
            "p.title AS title, " +
            "p.cover_url AS coverUrl " +
            "FROM tb_playlist p " +
            "ORDER BY p.id DESC " +
            "LIMIT #{limit}")
    List<PlaylistVO> getRandomPlaylists(int limit);

    // 根据用户收藏的歌单id列表获取歌单列表
    IPage<PlaylistVO> getPlaylistsByIds(
            Long userId,
            Page<PlaylistVO> page,
            @Param("playlistIds") List<Long> playlistIds,
            @Param("title") String title,
            @Param("style") String style);

    // Agent RAG: 按关键词搜索歌单
    List<PlaylistVO> searchPlaylistsByKeyword(@Param("keyword") String keyword,
                                              @Param("limit") int limit);
}
