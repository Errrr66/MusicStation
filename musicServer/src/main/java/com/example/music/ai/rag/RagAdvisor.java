package com.example.music.ai.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RAG Advisor：在 ChatClient 调用前注入向量检索到的上下文
 * <p>
 * 基于 Spring AI 1.0.0 {@link BaseAdvisor} 机制：
 * <ul>
 *   <li>{@link #before} 在请求前执行，按用户问题检索向量库，将命中文本写入 context 的 "rag_context"</li>
 *   <li>{@link #after} 默认透传响应</li>
 * </ul>
 * 通过 {@code ChatClient.prompt().advisors(ragAdvisor)} 启用，不影响现有 AgentRagService。
 */
@Component
public class RagAdvisor implements BaseAdvisor {

    private static final Logger log = LoggerFactory.getLogger(RagAdvisor.class);
    public static final String CONTEXT_KEY = "rag_context";
    public static final String CONTEXT_USER_QUERY = "user_query";

    private final SpringAiRagService ragService;
    private final int topK;

    @Autowired
    public RagAdvisor(SpringAiRagService ragService) {
        this(ragService, 4);
    }

    public RagAdvisor(SpringAiRagService ragService, int topK) {
        this.ragService = ragService;
        this.topK = Math.max(1, topK);
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        if (!ragService.isAvailable()) {
            return request;
        }
        // 优先从 context 取显式传入的 query，否则从 prompt 的 UserMessage 提取
        String query = extractFromContext(request);
        if (query == null || query.isBlank()) {
            query = extractUserQuery(request);
        }
        if (query == null || query.isBlank()) {
            return request;
        }
        try {
            List<SpringAiRagService.RetrievalResult> results = ragService.retrieve(query, topK);
            if (results == null || results.isEmpty()) {
                return request;
            }
            StringBuilder ctx = new StringBuilder("RAG资料（按相关性排序）:\n");
            for (int i = 0; i < results.size(); i++) {
                ctx.append(i + 1).append(". ").append(results.get(i).content()).append("\n");
            }
            ctx.append("\n回答约束: 优先依据RAG资料给出回答；若资料不足请明确说明不确定，不要编造事实。");
            request.context().put(CONTEXT_KEY, ctx.toString());
        } catch (Exception e) {
            log.warn("RagAdvisor 检索失败，跳过 RAG 增强: {}", e.getMessage());
        }
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain advisorChain) {
        return response;
    }

    @Override
    public String getName() {
        return "RagAdvisor";
    }

    @Override
    public int getOrder() {
        // 在记忆 Advisor 之后、模型调用之前执行
        return Ordered.HIGHEST_PRECEDENCE + 1000;
    }

    private String extractFromContext(ChatClientRequest request) {
        Object value = request.context().get(CONTEXT_USER_QUERY);
        return value == null ? null : value.toString();
    }

    private String extractUserQuery(ChatClientRequest request) {
        try {
            return request.prompt().getInstructions().stream()
                    .filter(m -> m.getMessageType() == MessageType.USER)
                    .map(m -> m.getText())
                    .reduce((first, second) -> second) // 取最后一条 user 消息
                    .orElse("");
        } catch (Exception e) {
            return "";
        }
    }
}
