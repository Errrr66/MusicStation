package com.example.music.mapper;

import com.example.music.model.entity.Artist;
import com.example.music.model.vo.ArtistDetailVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
  
 */
@Mapper
public interface ArtistMapper extends BaseMapper<Artist> {

    // 根据id查询歌手详情
    ArtistDetailVO getArtistDetailById(Long artistId);

    // 根据歌手名称列表解析歌手 id（精确匹配）
    List<Long> findArtistIdsByNames(@Param("names") List<String> names);

    // 根据关键词模糊解析歌手 id（用于别名/大小写/中英文容错）
    List<Long> findArtistIdsByKeyword(@Param("keyword") String keyword,
                                      @Param("limit") int limit);

}
