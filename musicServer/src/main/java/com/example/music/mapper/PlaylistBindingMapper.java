package com.example.music.mapper;

import com.example.music.model.entity.PlaylistBinding;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
  
 */
@Mapper
public interface PlaylistBindingMapper extends BaseMapper<PlaylistBinding> {

    int batchInsertBindings(@Param("playlistId") Long playlistId,
                            @Param("songIds") List<Long> songIds);

}
