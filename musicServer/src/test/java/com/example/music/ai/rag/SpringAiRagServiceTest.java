package com.example.music.ai.rag;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SpringAiRagService 单元测试。
 * <p>
 * 使用 mock EmbeddingModel 构造服务，然后通过反射注入 mock VectorStore，
 * 避免 SimpleVectorStore 内部 embedding 调用的不可控性。
 */
class SpringAiRagServiceTest {

    private EmbeddingModel embeddingModel;
    private SpringAiRagService ragService;
    private VectorStore mockVectorStore;

    @BeforeEach
    void setUp() {
        embeddingModel = mock(EmbeddingModel.class);
        ragService = new SpringAiRagService(embeddingModel);
        ragService.init();

        // 用 mock VectorStore 替换 SimpleVectorStore，实现可控测试
        mockVectorStore = mock(VectorStore.class);
        ReflectionTestUtils.setField(ragService, "vectorStore", mockVectorStore);
        ReflectionTestUtils.setField(ragService, "initialized", true);
    }

    @Test
    void testIsAvailable() {
        assertTrue(ragService.isAvailable());
    }

    @Test
    void testIsAvailableWhenNotInitialized() {
        ReflectionTestUtils.setField(ragService, "initialized", false);
        assertFalse(ragService.isAvailable());
    }

    @Test
    void testIsAvailableWhenVectorStoreNull() {
        ReflectionTestUtils.setField(ragService, "vectorStore", null);
        assertFalse(ragService.isAvailable());
    }

    @Test
    void testRetrieveEmptyQuery() {
        List<SpringAiRagService.RetrievalResult> results = ragService.retrieve("", 5);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testRetrieveNullQuery() {
        List<SpringAiRagService.RetrievalResult> results = ragService.retrieve(null, 5);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testRetrieveBlankQuery() {
        List<SpringAiRagService.RetrievalResult> results = ragService.retrieve("   ", 5);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testRetrieveInvalidTopK() {
        assertTrue(ragService.retrieve("query", 0).isEmpty());
        assertTrue(ragService.retrieve("query", -1).isEmpty());
    }

    @Test
    void testAddAndRetrieve() {
        Document doc1 = new Document("Java is a programming language", Map.of("source", "wiki"));
        Document doc2 = new Document("Spring is a Java framework", Map.of("source", "wiki"));

        when(mockVectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(doc1, doc2));

        // 添加文档
        ragService.addDocuments(List.of(doc1, doc2));
        verify(mockVectorStore).add(List.of(doc1, doc2));

        // 检索
        List<SpringAiRagService.RetrievalResult> results = ragService.retrieve("Java", 2);
        assertEquals(2, results.size());
        assertNotNull(results.get(0).content());
        assertNotNull(results.get(0).metadata());
    }

    @Test
    void testRetrieveReturnsEmptyWhenNoMatch() {
        when(mockVectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of());

        List<SpringAiRagService.RetrievalResult> results = ragService.retrieve("unknown", 5);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testAddDocumentsNull() {
        ragService.addDocuments(null);
        verifyNoInteractions(mockVectorStore);
    }

    @Test
    void testAddDocumentsEmpty() {
        ragService.addDocuments(List.of());
        verifyNoInteractions(mockVectorStore);
    }

    @Test
    void testAddDocumentsWhenNotAvailable() {
        ReflectionTestUtils.setField(ragService, "initialized", false);
        ragService.addDocuments(List.of(new Document("text", Map.of())));
        verifyNoInteractions(mockVectorStore);
    }

    @Test
    void testRetrieveWhenNotAvailable() {
        ReflectionTestUtils.setField(ragService, "initialized", false);
        List<SpringAiRagService.RetrievalResult> results = ragService.retrieve("query", 5);
        assertNotNull(results);
        assertTrue(results.isEmpty());
        verifyNoInteractions(mockVectorStore);
    }

    @Test
    void testRetrieveReturnsEmptyOnException() {
        when(mockVectorStore.similaritySearch(any(SearchRequest.class)))
                .thenThrow(new RuntimeException("search failed"));
        List<SpringAiRagService.RetrievalResult> results = ragService.retrieve("query", 5);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testAddDocumentsHandlesException() {
        doThrow(new RuntimeException("add failed"))
                .when(mockVectorStore).add(any(List.class));

        // 不应抛出异常
        assertDoesNotThrow(() ->
                ragService.addDocuments(List.of(new Document("text", Map.of()))));
    }
}
