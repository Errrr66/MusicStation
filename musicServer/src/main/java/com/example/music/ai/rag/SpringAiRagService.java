package com.example.music.ai.rag;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 基于 Spring AI 的 RAG 服务
 * <p>
 * 使用 {@link SimpleVectorStore}（内存实现）做语义检索，后续可切换 PgVector/Redis VectorStore。
 * 与现有 {@code AgentRagService}（bi-gram 字符相似度）并存，互不影响：
 * 现有 AgentRagService 保持不变，本服务通过 Spring AI Advisor 机制注入检索上下文。
 */
@Service
public class SpringAiRagService {

    private static final Logger log = LoggerFactory.getLogger(SpringAiRagService.class);

    private final EmbeddingModel embeddingModel;
    private volatile VectorStore vectorStore;
    private volatile boolean initialized = false;

    public SpringAiRagService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @PostConstruct
    public void init() {
        try {
            this.vectorStore = SimpleVectorStore.builder(embeddingModel).build();
            this.initialized = true;
            log.info("SpringAiRagService 初始化完成（SimpleVectorStore，内存）");
        } catch (Exception e) {
            log.warn("SpringAiRagService 初始化失败，RAG 检索将降级为空结果: {}", e.getMessage());
            this.initialized = false;
        }
    }

    /** 是否可用（向量库已初始化且有数据时返回 true） */
    public boolean isAvailable() {
        return initialized && vectorStore != null;
    }

    /** 添加文档到向量库 */
    public void addDocuments(List<Document> documents) {
        if (!isAvailable() || documents == null || documents.isEmpty()) {
            return;
        }
        try {
            vectorStore.add(documents);
        } catch (Exception e) {
            log.warn("SpringAiRagService 添加文档失败: {}", e.getMessage());
        }
    }

    /**
     * 语义检索
     *
     * @param query 查询文本
     * @param topK  返回条数
     * @return 按相关性排序的检索结果
     */
    public List<RetrievalResult> retrieve(String query, int topK) {
        if (!isAvailable() || query == null || query.isBlank() || topK <= 0) {
            return List.of();
        }
        try {
            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(topK)
                            .build()
            );
            if (docs == null || docs.isEmpty()) {
                return List.of();
            }
            return docs.stream().map(d -> new RetrievalResult(
                    d.getText(),
                    d.getMetadata(),
                    d.getScore() != null ? d.getScore() : 0.0
            )).toList();
        } catch (Exception e) {
            log.warn("SpringAiRagService 检索失败: {}", e.getMessage());
            return List.of();
        }
    }

    /** 检索结果记录 */
    public record RetrievalResult(String content, Map<String, Object> metadata, double score) {
    }
}
