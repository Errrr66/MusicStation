package com.example.music.ai.rag;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.music.model.entity.Artist;
import com.example.music.model.entity.Song;
import com.example.music.mapper.ArtistMapper;
import com.example.music.mapper.SongMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库索引初始化器
 * <p>
 * 启动时将歌曲/歌手数据分块后索引到 Spring AI 向量库（SimpleVectorStore），
 * 为 {@link SpringAiRagService} / {@link RagAdvisor} 提供语义检索基础。
 * 失败不影响应用启动；可通过 {@code ai.rag.auto-index=false} 关闭。
 */
@Slf4j
@Component
public class KnowledgeBaseInitializer {

    @Autowired(required = false)
    private SpringAiRagService ragService;

    @Autowired
    private SongMapper songMapper;

    @Autowired
    private ArtistMapper artistMapper;

    @Autowired
    private DocumentChunker chunker;

    @Value("${ai.rag.auto-index:true}")
    private boolean autoIndex;

    @Value("${ai.rag.index-batch-size:100}")
    private int batchSize;

    @Value("${ai.rag.chunk-size:500}")
    private int chunkSize;

    @Value("${ai.rag.chunk-overlap:100}")
    private int overlap;

    @Value("${ai.rag.top-k:4}")
    private int topK;

    @PostConstruct
    public void initIndex() {
        if (!autoIndex || ragService == null || !ragService.isAvailable()) {
            log.info("RAG 自动索引已禁用或向量库不可用，跳过索引构建");
            return;
        }
        try {
            log.info("开始构建 RAG 知识库索引（chunkSize={}, overlap={}）...", chunkSize, overlap);
            int total = 0;

            // 1) 索引歌曲元数据 + 歌词
            List<Song> songs = songMapper.selectList(null);
            if (songs != null) {
                List<Document> docs = new ArrayList<>();
                for (Song song : songs) {
                    if (song == null) {
                        continue;
                    }
                    String content = buildSongContent(song);
                    if (content == null || content.isBlank()) {
                        continue;
                    }
                    docs.addAll(chunker.chunk(content, buildSongMetadata(song), chunkSize, overlap));
                    if (docs.size() >= batchSize) {
                        ragService.addDocuments(docs);
                        total += docs.size();
                        docs.clear();
                    }
                }
                if (!docs.isEmpty()) {
                    ragService.addDocuments(docs);
                    total += docs.size();
                }
            }

            // 2) 索引歌手简介
            List<Artist> artists = artistMapper.selectList(new QueryWrapper<Artist>().isNotNull("introduction"));
            if (artists != null) {
                List<Document> docs = new ArrayList<>();
                for (Artist artist : artists) {
                    if (artist == null) {
                        continue;
                    }
                    String content = buildArtistContent(artist);
                    if (content == null || content.isBlank()) {
                        continue;
                    }
                    docs.addAll(chunker.chunk(content, buildArtistMetadata(artist), chunkSize, overlap));
                    if (docs.size() >= batchSize) {
                        ragService.addDocuments(docs);
                        total += docs.size();
                        docs.clear();
                    }
                }
                if (!docs.isEmpty()) {
                    ragService.addDocuments(docs);
                    total += docs.size();
                }
            }

            log.info("RAG 知识库索引构建完成，共 {} 个文档块（topK={}）", total, topK);
        } catch (Exception e) {
            log.error("RAG 知识库索引构建失败，RAG 检索将降级", e);
        }
    }

    private String buildSongContent(Song song) {
        StringBuilder sb = new StringBuilder();
        if (song.getSongName() != null) {
            sb.append("歌曲名: ").append(song.getSongName()).append("\n");
        }
        if (song.getAlbum() != null && !song.getAlbum().isBlank()) {
            sb.append("专辑: ").append(song.getAlbum()).append("\n");
        }
        if (song.getStyle() != null && !song.getStyle().isBlank()) {
            sb.append("风格: ").append(song.getStyle()).append("\n");
        }
        if (song.getLyric() != null && !song.getLyric().isBlank()) {
            sb.append("歌词:\n").append(song.getLyric()).append("\n");
        }
        return sb.toString();
    }

    private String buildArtistContent(Artist artist) {
        StringBuilder sb = new StringBuilder();
        if (artist.getArtistName() != null) {
            sb.append("歌手: ").append(artist.getArtistName()).append("\n");
        }
        if (artist.getArea() != null && !artist.getArea().isBlank()) {
            sb.append("地区: ").append(artist.getArea()).append("\n");
        }
        if (artist.getIntroduction() != null && !artist.getIntroduction().isBlank()) {
            sb.append("简介: ").append(artist.getIntroduction()).append("\n");
        }
        return sb.toString();
    }

    private Map<String, Object> buildSongMetadata(Song song) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("type", "song");
        if (song.getSongId() != null) {
            meta.put("id", song.getSongId());
        }
        if (song.getSongName() != null) {
            meta.put("name", song.getSongName());
        }
        if (song.getArtistId() != null) {
            meta.put("artistId", song.getArtistId());
        }
        return meta;
    }

    private Map<String, Object> buildArtistMetadata(Artist artist) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("type", "artist");
        if (artist.getArtistId() != null) {
            meta.put("id", artist.getArtistId());
        }
        if (artist.getArtistName() != null) {
            meta.put("name", artist.getArtistName());
        }
        return meta;
    }
}
