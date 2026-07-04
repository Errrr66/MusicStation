package com.example.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.music.model.entity.UserFollow;
import com.example.music.model.vo.UserSimpleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserFollowMapper extends BaseMapper<UserFollow> {

    @Select("SELECT COUNT(1) FROM tb_user_follow WHERE user_id = #{userId} AND follow_user_id = #{targetUserId}")
    Long existsFollow(@Param("userId") Long userId, @Param("targetUserId") Long targetUserId);

    @Select("SELECT COUNT(1) FROM tb_user_follow WHERE user_id = #{userId}")
    Long countFollowing(@Param("userId") Long userId);

    @Select("SELECT COUNT(1) FROM tb_user_follow WHERE follow_user_id = #{userId}")
    Long countFollowers(@Param("userId") Long userId);

    /**
     * 批量查询某用户关注的所有目标用户 ID 集合
     */
    @Select("SELECT follow_user_id FROM tb_user_follow WHERE user_id = #{userId}")
    List<Long> listFollowingIds(@Param("userId") Long userId);

    /**
     * 批量查询关注某用户的所有粉丝 ID 集合
     */
    @Select("SELECT user_id FROM tb_user_follow WHERE follow_user_id = #{userId}")
    List<Long> listFollowersIds(@Param("userId") Long userId);

    @Select("""
            SELECT u.id AS userId,
                   u.username,
                   u.user_avatar AS userAvatar,
                   u.introduction
            FROM tb_user_follow uf
            JOIN tb_user u ON uf.follow_user_id = u.id
            WHERE uf.user_id = #{userId}
            ORDER BY uf.create_time DESC
            """)
    List<UserSimpleVO> listFollowing(@Param("userId") Long userId);

    @Select("""
            SELECT u.id AS userId,
                   u.username,
                   u.user_avatar AS userAvatar,
                   u.introduction
            FROM tb_user_follow uf
            JOIN tb_user u ON uf.user_id = u.id
            WHERE uf.follow_user_id = #{userId}
            ORDER BY uf.create_time DESC
            """)
    List<UserSimpleVO> listFollowers(@Param("userId") Long userId);

    @Select("""
            SELECT u.id AS userId,
                   u.username,
                   u.user_avatar AS userAvatar,
                   u.introduction
            FROM tb_user u
            WHERE u.username LIKE CONCAT('%', #{keyword}, '%')
            ORDER BY u.id DESC
            LIMIT #{limit}
            """)
    List<UserSimpleVO> searchUsers(@Param("keyword") String keyword, @Param("limit") Integer limit);
}

