package com.example.music.ai.rag;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档分块工具
 * <p>
 * 按字符长度分块，带重叠（overlap），用于将长文本（如歌词）切分为向量索引友好的片段。
 */
@Component
public class DocumentChunker {

    public static final int DEFAULT_CHUNK_SIZE = 500;
    public static final int DEFAULT_OVERLAP = 100;

    /**
     * 使用默认参数分块（chunkSize=500, overlap=100）
     */
    public List<Document> chunk(String text, Map<String, Object> metadata) {
        return chunk(text, metadata, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
    }

    /**
     * 按指定参数分块
     *
     * @param text      原始文本
     * @param metadata  附加到每个分块的元数据（会被复制，不会修改入参）
     * @param chunkSize 每块字符数
     * @param overlap   相邻块重叠字符数（必须小于 chunkSize）
     */
    public List<Document> chunk(String text, Map<String, Object> metadata, int chunkSize, int overlap) {
        List<Document> docs = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return docs;
        }
        int safeChunkSize = Math.max(50, chunkSize);
        int safeOverlap = Math.max(0, Math.min(overlap, safeChunkSize - 1));
        int start = 0;
        int idx = 0;
        Map<String, Object> baseMeta = metadata == null ? new HashMap<>() : new HashMap<>(metadata);
        while (start < text.length()) {
            int end = Math.min(start + safeChunkSize, text.length());
            String chunk = text.substring(start, end);
            Map<String, Object> meta = new HashMap<>(baseMeta);
            meta.put("chunkIndex", idx);
            meta.put("chunkStart", start);
            meta.put("chunkEnd", end);
            docs.add(new Document(chunk, meta));
            if (end >= text.length()) {
                break;
            }
            start += (safeChunkSize - safeOverlap);
            idx++;
        }
        return docs;
    }
}
