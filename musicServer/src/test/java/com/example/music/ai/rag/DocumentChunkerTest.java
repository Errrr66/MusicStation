package com.example.music.ai.rag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DocumentChunker 单元测试。
 * 验证分块逻辑：正常分块、重叠、空文本、短文本。
 */
class DocumentChunkerTest {

    private final DocumentChunker chunker = new DocumentChunker();

    @Test
    void testChunkNormal() {
        String text = "a".repeat(250); // 250 chars
        List<Document> docs = chunker.chunk(text, Map.of("source", "test"), 100, 20);

        assertFalse(docs.isEmpty());
        // chunkSize=100, overlap=20, step=80
        // [0:100] -> [80:180] -> [160:250] = 3 chunks
        assertEquals(3, docs.size());
        assertEquals(100, docs.get(0).getText().length());
        assertEquals(100, docs.get(1).getText().length());
        assertEquals(90, docs.get(2).getText().length());
    }

    @Test
    void testChunkWithOverlap() {
        // 200 chars, chunkSize=100, overlap=20
        String text = "0123456789"; // 10 chars
        String full = text.repeat(20); // 200 chars
        List<Document> docs = chunker.chunk(full, null, 100, 20);

        assertEquals(3, docs.size());
        // 重叠部分：chunk0 的最后 20 字符 == chunk1 的前 20 字符
        String chunk0 = docs.get(0).getText();
        String chunk1 = docs.get(1).getText();
        String overlapFromChunk0 = chunk0.substring(chunk0.length() - 20);
        String overlapFromChunk1 = chunk1.substring(0, 20);
        assertEquals(overlapFromChunk0, overlapFromChunk1);

        // chunk1 和 chunk2 的重叠
        String chunk2 = docs.get(2).getText();
        assertEquals(chunk1.substring(chunk1.length() - 20), chunk2.substring(0, 20));
    }

    @Test
    void testChunkEmpty() {
        List<Document> docs = chunker.chunk("", null);
        assertNotNull(docs);
        assertTrue(docs.isEmpty());
    }

    @Test
    void testChunkNull() {
        List<Document> docs = chunker.chunk(null, null);
        assertNotNull(docs);
        assertTrue(docs.isEmpty());
    }

    @Test
    void testChunkBlankText() {
        List<Document> docs = chunker.chunk("   ", null);
        assertNotNull(docs);
        assertTrue(docs.isEmpty());
    }

    @Test
    void testChunkShortText() {
        String text = "short text"; // 10 chars
        List<Document> docs = chunker.chunk(text, null); // 默认 chunkSize=500

        assertEquals(1, docs.size());
        assertEquals(text, docs.get(0).getText());
    }

    @Test
    void testChunkShortTextWithCustomSize() {
        String text = "hello"; // 5 chars
        List<Document> docs = chunker.chunk(text, null, 100, 20);

        assertEquals(1, docs.size());
        assertEquals(text, docs.get(0).getText());
    }

    @Test
    void testChunkMetadataPropagated() {
        String text = "a".repeat(60);
        List<Document> docs = chunker.chunk(text, Map.of("title", "song1"), 50, 10);

        assertFalse(docs.isEmpty());
        for (Document doc : docs) {
            assertEquals("song1", doc.getMetadata().get("title"));
            assertNotNull(doc.getMetadata().get("chunkIndex"));
            assertNotNull(doc.getMetadata().get("chunkStart"));
            assertNotNull(doc.getMetadata().get("chunkEnd"));
        }
    }

    @Test
    void testChunkMetadataNotMutated() {
        Map<String, Object> meta = new java.util.HashMap<>(Map.of("key", "value"));
        chunker.chunk("a".repeat(120), meta, 50, 10);

        // 确保原始 metadata 未被修改
        assertEquals(1, meta.size());
        assertEquals("value", meta.get("key"));
    }

    @Test
    void testChunkIndexSequence() {
        String text = "a".repeat(250);
        List<Document> docs = chunker.chunk(text, null, 100, 20);

        assertEquals(3, docs.size());
        for (int i = 0; i < docs.size(); i++) {
            assertEquals(i, docs.get(i).getMetadata().get("chunkIndex"));
        }
    }

    @Test
    void testChunkWithDefaultParameters() {
        String text = "a".repeat(600); // > 500 (DEFAULT_CHUNK_SIZE)
        List<Document> docs = chunker.chunk(text, null);

        assertTrue(docs.size() > 1);
        // 默认 chunkSize=500, 每个 chunk 不超过 500 字符
        for (Document doc : docs) {
            assertTrue(doc.getText().length() <= 500);
        }
    }

    @Test
    void testChunkOverlapClampedToChunkSize() {
        // overlap >= chunkSize 时，safeOverlap = chunkSize - 1，step = 1
        String text = "a".repeat(200);
        List<Document> docs = chunker.chunk(text, null, 100, 200); // overlap > chunkSize

        assertNotNull(docs);
        assertFalse(docs.isEmpty());
        // step = chunkSize - (chunkSize-1) = 1，分块数 = 200 - 100 + 1 = 101
        // 验证不产生死循环，且分块数有限
        assertTrue(docs.size() <= 200);
    }
}
