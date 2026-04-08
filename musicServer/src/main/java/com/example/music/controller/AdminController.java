package com.example.music.controller;


import com.example.music.model.dto.*;
import com.example.music.model.entity.Artist;
import com.example.music.model.entity.Playlist;
import com.example.music.model.vo.ArtistNameVO;
import com.example.music.model.vo.ChatHealthVO;
import com.example.music.model.vo.RagDebugRetrieveVO;
import com.example.music.model.vo.RagEvalVO;
import com.example.music.model.vo.SongAdminVO;
import com.example.music.model.vo.UserManagementVO;
import com.example.music.result.PageResult;
import com.example.music.result.Result;
import com.example.music.service.*;
import com.example.music.util.BindingResultUtil;
import jakarta.validation.Valid;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;


/**
 * <p>
 * 前端控制器
 * </p>
  
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private IAdminService adminService;
    @Autowired
    private IUserService userService;
    @Autowired
    private IArtistService artistService;
    @Autowired
    private ISongService songService;
    @Autowired
    private IPlaylistService playlistService;
    @Autowired
    private MinioService minioService;
    @Autowired
    private AgentRagService agentRagService;
    @Autowired
    private ArtistAliasResolver artistAliasResolver;
    @Autowired
    private SemanticEmbeddingService semanticEmbeddingService;

    @Value("${agent.rag.enabled:true}")
    private boolean ragEnabled;

    @Value("${agent.rag.mode:hybrid}")
    private String ragMode;

    @Value("${deepseek.api-key:}")
    private String deepseekApiKey;

    @Value("${tts.api-url:}")
    private String ttsApiUrl;

    /**
     * 注册管理员
     *
     * @param adminDTO      管理员信息
     * @param bindingResult 绑定结果
     * @return 结果
     */
    @PostMapping("/register")
    public Result register(@RequestBody @Valid AdminDTO adminDTO, BindingResult bindingResult) {
        // 校验失败时，返回错误信息
        String errorMessage = BindingResultUtil.handleBindingResultErrors(bindingResult);
        if (errorMessage != null) {
            return Result.error(errorMessage);
        }

        return adminService.register(adminDTO);
    }

    /**
     * 登录管理员
     *
     * @param adminDTO      管理员信息
     * @param bindingResult 绑定结果
     * @return 结果
     */
    @PostMapping("/login")
    public Result login(@RequestBody @Valid AdminDTO adminDTO, BindingResult bindingResult) {
        // 校验失败时，返回错误信息
        String errorMessage = BindingResultUtil.handleBindingResultErrors(bindingResult);
        if (errorMessage != null) {
            return Result.error(errorMessage);
        }

        return adminService.login(adminDTO);
    }

    /**
     * 登出
     *
     * @param token 认证token
     * @return 结果
     */
    @PostMapping("/logout")
    public Result logout(@RequestHeader("Authorization") String token) {
        return adminService.logout(token);
    }

    /**********************************************************************************************/

    /**
     * 获取所有用户数量
     *
     * @return 用户数量
     */
    @GetMapping("/getAllUsersCount")
    public Result<Long> getAllUsersCount() {
        return userService.getAllUsersCount();
    }

    /**
     * 获取所有用户信息
     *
     * @param userSearchDTO 用户搜索条件
     * @return 结果
     */
    @PostMapping("/getAllUsers")
    public Result<PageResult<UserManagementVO>> getAllUsers(@RequestBody UserSearchDTO userSearchDTO) {
        return userService.getAllUsers(userSearchDTO);
    }

    /**
     * 新增用户
     *
     * @param userAddDTO 用户注册信息
     * @return 结果
     */
    @PostMapping("/addUser")
    public Result addUser(@RequestBody @Valid UserAddDTO userAddDTO, BindingResult bindingResult) {
        // 校验失败时，返回错误信息
        String errorMessage = BindingResultUtil.handleBindingResultErrors(bindingResult);
        if (errorMessage != null) {
            return Result.error(errorMessage);
        }

        return userService.addUser(userAddDTO);
    }

    /**
     * 更新用户信息
     *
     * @param userDTO 用户信息
     * @return 结果
     */
    @PutMapping("/updateUser")
    public Result updateUser(@RequestBody @Valid UserDTO userDTO, BindingResult bindingResult) {
        // 校验失败时，返回错误信息
        String errorMessage = BindingResultUtil.handleBindingResultErrors(bindingResult);
        if (errorMessage != null) {
            return Result.error(errorMessage);
        }

        return userService.updateUser(userDTO);
    }

    /**
     * 更新用户状态
     *
     * @param userId     用户id
     * @param userStatus 用户状态
     * @return 结果
     */
    @PatchMapping("/updateUserStatus/{id}/{status}")
    public Result updateUserStatus(@PathVariable("id") Long userId, @PathVariable("status") Integer userStatus) {
        return userService.updateUserStatus(userId, userStatus);
    }

    /**
     * 删除用户
     *
     * @param userId 用户id
     * @return 结果
     */
    @DeleteMapping("/deleteUser/{id}")
    public Result deleteUser(@PathVariable("id") Long userId) {
        return userService.deleteUser(userId);
    }

    /**
     * 批量删除用户
     *
     * @param userIds 用户id列表
     * @return 结果
     */
    @DeleteMapping("/deleteUsers")
    public Result deleteUsers(@RequestBody List<Long> userIds) {
        return userService.deleteUsers(userIds);
    }

    /**********************************************************************************************/

    /**
     * 获取所有歌手数量
     *
     * @param gender 性别
     * @param area   地区
     * @return 歌手数量
     */
    @GetMapping("/getAllArtistsCount")
    public Result<Long> getAllArtistsCount(@RequestParam(required = false) Integer gender, @RequestParam(required = false) String area) {
        return artistService.getAllArtistsCount(gender, area);
    }

    /**
     * 获取所有歌手信息
     *
     * @param artistDTO 歌手搜索条件
     * @return 结果
     */
    @PostMapping("/getAllArtists")
    public Result<PageResult<Artist>> getAllArtists(@RequestBody ArtistDTO artistDTO) {
        return artistService.getAllArtistsAndDetail(artistDTO);
    }

    /**
     * 新增歌手
     *
     * @param artistAddDTO 歌手信息
     * @return 结果
     */
    @PostMapping("/addArtist")
    public Result addArtist(@RequestBody ArtistAddDTO artistAddDTO) {
        return artistService.addArtist(artistAddDTO);
    }

    /**
     * 更新歌手信息
     *
     * @param artistUpdateDTO 歌手信息
     * @return 结果
     */
    @PutMapping("/updateArtist")
    public Result updateArtist(@RequestBody ArtistUpdateDTO artistUpdateDTO) {
        return artistService.updateArtist(artistUpdateDTO);
    }

    /**
     * 更新歌手头像
     *
     * @param artistId 歌手id
     * @param avatar   头像
     * @return 结果
     */
    @PatchMapping("/updateArtistAvatar/{id}")
    public Result updateArtistAvatar(@PathVariable("id") Long artistId, @RequestParam("avatar") MultipartFile avatar) {
        String avatarUrl = minioService.uploadFile(avatar, "artists");  // 上传到 artists 目录
        return artistService.updateArtistAvatar(artistId, avatarUrl);
    }

    /**
     * 删除歌手
     *
     * @param artistId 歌手id
     * @return 结果
     */
    @DeleteMapping("/deleteArtist/{id}")
    public Result deleteArtist(@PathVariable("id") Long artistId) {
        return artistService.deleteArtist(artistId);
    }

    /**
     * 批量删除歌手
     *
     * @param artistIds 歌手id列表
     * @return 结果
     */
    @DeleteMapping("/deleteArtists")
    public Result deleteArtists(@RequestBody List<Long> artistIds) {
        return artistService.deleteArtists(artistIds);
    }

    /**********************************************************************************************/

    /**
     * 获取所有歌曲的数量
     *
     * @param style 歌曲风格
     * @return 歌曲数量
     */
    @GetMapping("/getAllSongsCount")
    public Result<Long> getAllSongsCount(@RequestParam(required = false) String style) {
        return songService.getAllSongsCount(style);
    }

    /**
     * 获取所有歌手id和名称
     *
     * @return 结果
     */
    @GetMapping("/getAllArtistNames")
    public Result<List<ArtistNameVO>> getAllArtistNames() {
        return artistService.getAllArtistNames();
    }

    /**
     * 根据歌手id获取其歌曲信息
     *
     * @param songDTO 歌曲搜索条件
     * @return 结果
     */
    @PostMapping("/getAllSongsByArtist")
    public Result<PageResult<SongAdminVO>> getAllSongsByArtist(@RequestBody SongAndArtistDTO songDTO) {
        return songService.getAllSongsByArtist(songDTO);
    }

    /**
     * 添加歌曲信息
     *
     * @param songAddDTO 歌曲信息
     * @return 结果
     */
    @PostMapping("/addSong")
    public Result addSong(@RequestBody SongAddDTO songAddDTO) {
        return songService.addSong(songAddDTO);
    }

    /**
     * 修改歌曲信息
     *
     * @param songUpdateDTO 歌曲信息
     * @return 结果
     */
    @PutMapping("/updateSong")
    public Result UpdateSong(@RequestBody SongUpdateDTO songUpdateDTO) {
        return songService.updateSong(songUpdateDTO);
    }

    /**
     * 更新歌曲封面
     *
     * @param songId 歌曲id
     * @param cover  封面
     * @return 结果
     */
    @PatchMapping("/updateSongCover/{id}")
    public Result updateSongCover(@PathVariable("id") Long songId, @RequestParam("cover") MultipartFile cover) {
        String coverUrl = minioService.uploadFile(cover, "songCovers");  // 上传到 songCovers 目录
        return songService.updateSongCover(songId, coverUrl);
    }

    /**
     * 更新歌曲音频
     *
     * @param songId 歌曲id
     * @param audio  音频
     * @return 结果
     */
    @PatchMapping("/updateSongAudio/{id}")
    public Result updateSongAudio(@PathVariable("id") Long songId, @RequestParam("audio") MultipartFile audio) {
        String lyrics = extractLyrics(audio);
        String audioUrl = minioService.uploadFile(audio, "songs");  // 上传到 songs 目录
        return songService.updateSongAudio(songId, audioUrl, lyrics);
    }

    /**
     * 从音频文件中提取歌词
     *
     * @param file 音频文件
     * @return 歌词
     */
    private String extractLyrics(MultipartFile file) {
        File tempFile = null;
        try {
            // Determine suffix
            String originalFilename = file.getOriginalFilename();
            String suffix = ".mp3"; // Default
            if (originalFilename != null && originalFilename.contains(".")) {
                suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // Create a temp file
            tempFile = File.createTempFile("upload-audio-", suffix);
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(file.getBytes());
            }

            AudioFile audioFile = AudioFileIO.read(tempFile);
            Tag tag = audioFile.getTag();
            if (tag != null) {
                return tag.getFirst(FieldKey.LYRICS);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
        return null;
    }

    /**
     * 删除歌曲
     *
     * @param songId 歌曲id
     * @return 结果
     */
    @DeleteMapping("/deleteSong/{id}")
    public Result deleteSong(@PathVariable("id") Long songId) {
        return songService.deleteSong(songId);
    }

    /**
     * 批量删除歌曲
     *
     * @param songIds 歌曲id列表
     * @return 结果
     */
    @DeleteMapping("/deleteSongs")
    public Result deleteSongs(@RequestBody List<Long> songIds) {
        return songService.deleteSongs(songIds);
    }

    /**********************************************************************************************/

    /**
     * 获取所有歌单数量
     *
     * @param style 歌单风格
     * @return 歌单数量
     */
    @GetMapping("/getAllPlaylistsCount")
    public Result<Long> getAllPlaylistsCount(@RequestParam(required = false) String style) {
        return playlistService.getAllPlaylistsCount(style);
    }

    /**
     * 获取所有歌单信息
     *
     * @param playlistDTO 歌单搜索条件
     * @return 结果
     */
    @PostMapping("/getAllPlaylists")
    public Result<PageResult<Playlist>> getAllPlaylists(@RequestBody PlaylistDTO playlistDTO) {
        return playlistService.getAllPlaylistsInfo(playlistDTO);
    }

    /**
     * 新增歌单
     *
     * @param playlistAddDTO 歌单信息
     * @return 结果
     */
    @PostMapping("/addPlaylist")
    public Result addPlaylist(@RequestBody PlaylistAddDTO playlistAddDTO) {
        return playlistService.addPlaylist(playlistAddDTO);
    }

    /**
     * 更新歌单信息
     *
     * @param playlistUpdateDTO 歌单信息
     * @return 结果
     */
    @PutMapping("/updatePlaylist")
    public Result updatePlaylist(@RequestBody PlaylistUpdateDTO playlistUpdateDTO) {
        return playlistService.updatePlaylist(playlistUpdateDTO);
    }

    /**
     * 更新歌单封面
     *
     * @param playlistId 歌单id
     * @param cover      封面
     * @return 结果
     */
    @PatchMapping("/updatePlaylistCover/{id}")
    public Result updatePlaylistCover(@PathVariable("id") Long playlistId, @RequestParam("cover") MultipartFile cover) {
        String coverUrl = minioService.uploadFile(cover, "playlists");  // 上传到 playlists 目录
        return playlistService.updatePlaylistCover(playlistId, coverUrl);
    }

    /**
     * 删除歌单
     *
     * @param playlistId 歌单id
     * @return 结果
     */
    @DeleteMapping("/deletePlaylist/{id}")
    public Result deletePlaylist(@PathVariable("id") Long playlistId) {
        return playlistService.deletePlaylist(playlistId);
    }

    /**
     * 批量删除歌单
     *
     * @param playlistIds 歌单id列表
     * @return 结果
     */
    @DeleteMapping("/deletePlaylists")
    public Result deletePlaylists(@RequestBody List<Long> playlistIds) {
        return playlistService.deletePlaylists(playlistIds);
    }

    /**********************************************************************************************/

    @GetMapping("/rag/health")
    public Result<ChatHealthVO> ragHealth() {
        AgentRagService.RetrievalHealth retrievalHealth = agentRagService.getLastRetrievalHealth();
        ChatHealthVO data = ChatHealthVO.builder()
                .ragEnabled(ragEnabled)
                .ragMode(isBlank(ragMode) ? "hybrid" : ragMode)
                .ragLastRetrieval(ChatHealthVO.RagRetrievalVO.builder()
                        .strategy(retrievalHealth.strategy())
                        .mode(retrievalHealth.mode())
                        .queryCount(retrievalHealth.queryCount())
                        .candidateCount(retrievalHealth.candidateCount())
                        .citationCount(retrievalHealth.citationCount())
                        .enabled(retrievalHealth.enabled())
                        .updatedAtEpochMs(retrievalHealth.updatedAtEpochMs())
                        .build())
                .providers(ChatHealthVO.ProviderStatusVO.builder()
                        .deepseekConfigured(hasValue(deepseekApiKey))
                        .ttsConfigured(hasValue(ttsApiUrl))
                        .semanticConfigured(semanticEmbeddingService.isConfigured())
                        .build())
                .serverTime(java.time.LocalDateTime.now().toString())
                .build();
        return Result.success("Success", data);
    }

    @PostMapping("/rag/artist-alias/refresh")
    public Result<Map<String, Object>> refreshRagArtistAliasIndex() {
        return Result.success("Success", artistAliasResolver.refreshAliasIndex());
    }

    @PostMapping("/rag/debug-retrieve")
    public Result<RagDebugRetrieveVO> debugRagRetrieve(@RequestBody RagDebugRetrieveRequestDTO requestDTO) {
        String query = requestDTO == null || isBlank(requestDTO.getQuery()) ? "" : requestDTO.getQuery().trim();
        if (query.isEmpty()) {
            return Result.error("query 不能为空");
        }
        String intent = requestDTO == null || isBlank(requestDTO.getIntent()) ? "CHAT" : requestDTO.getIntent().trim().toUpperCase();
        int topK = Math.max(1, Math.min(requestDTO == null || requestDTO.getTopK() == null ? 8 : requestDTO.getTopK(), 20));
        boolean enableRag = requestDTO == null || requestDTO.getEnableRag() == null || requestDTO.getEnableRag();

        AgentRagService.RagContext context = agentRagService.retrieve(query, intent, null, enableRag, topK);
        AgentRagService.RetrievalHealth health = agentRagService.getLastRetrievalHealth();

        RagDebugRetrieveVO data = RagDebugRetrieveVO.builder()
                .query(query)
                .intent(intent)
                .ragEnabled(enableRag)
                .promptContext(context.promptContext())
                .citations(context.citations())
                .retrievalHealth(RagDebugRetrieveVO.AgentRagServiceHealthVO.builder()
                        .strategy(health.strategy())
                        .mode(health.mode())
                        .queryCount(health.queryCount())
                        .candidateCount(health.candidateCount())
                        .citationCount(health.citationCount())
                        .enabled(health.enabled())
                        .updatedAtEpochMs(health.updatedAtEpochMs())
                        .build())
                .build();
        return Result.success("Success", data);
    }

    @PostMapping("/rag/evaluate")
    public Result<RagEvalVO> evaluateRag(@RequestBody RagEvalRequestDTO requestDTO) {
        return Result.success("Success", agentRagService.evaluate(requestDTO));
    }

    private boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}
