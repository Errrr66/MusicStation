package com.example.music.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.music.mapper.ArtistMapper;
import com.example.music.mapper.PlaylistMapper;
import com.example.music.mapper.SongMapper;
import com.example.music.mapper.UserFollowMapper;
import com.example.music.model.entity.Artist;
import com.example.music.model.vo.PlaylistVO;
import com.example.music.model.vo.SongVO;
import com.example.music.model.vo.UserSimpleVO;
import com.example.music.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/search")
public class SearchController {

    @Autowired
    private SongMapper songMapper;
    @Autowired
    private ArtistMapper artistMapper;
    @Autowired
    private PlaylistMapper playlistMapper;
    @Autowired
    private UserFollowMapper userFollowMapper;

    @GetMapping("/all")
    public Result<Map<String, Object>> searchAll(@RequestParam String keyword,
                                                 @RequestParam(defaultValue = "20") Integer limit) {
        int safeLimit = limit == null || limit < 1 ? 20 : Math.min(limit, 100);
        String safeKeyword = keyword == null ? "" : keyword.trim();
        if (safeKeyword.isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("songs", List.of());
            empty.put("artists", List.of());
            empty.put("playlists", List.of());
            empty.put("users", List.of());
            return Result.success(empty);
        }

        List<SongVO> songs = songMapper.searchSongsByKeyword(safeKeyword, safeLimit);

        QueryWrapper<Artist> artistQuery = new QueryWrapper<>();
        artistQuery.like("name", safeKeyword).last("LIMIT " + safeLimit);
        List<Artist> artists = artistMapper.selectList(artistQuery);

        List<PlaylistVO> playlists = playlistMapper.searchPlaylistsByKeyword(safeKeyword, safeLimit);
        List<UserSimpleVO> users = userFollowMapper.searchUsers(safeKeyword, safeLimit);

        Map<String, Object> data = new HashMap<>();
        data.put("songs", songs);
        data.put("artists", artists);
        data.put("playlists", playlists);
        data.put("users", users);
        return Result.success(data);
    }
}


