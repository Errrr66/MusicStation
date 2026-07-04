package com.example.music.ai;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 负载均衡的 ChatModel 选择器
 * 当前单 Provider（DashScope OpenAI 兼容），为多 Provider 扩展预留接口。
 */
@Component
public class LoadBalancedChatModel {

    private final List<ChatModel> models;
    private final AtomicInteger counter = new AtomicInteger(0);

    public LoadBalancedChatModel(List<ChatModel> models) {
        // 防御性拷贝，避免外部修改
        this.models = (models == null) ? new ArrayList<>() : new ArrayList<>(models);
    }

    /**
     * 选择一个 ChatModel。当前单 Provider 时直接返回唯一实例；
     * 多 Provider 时采用轮询策略。
     */
    public ChatModel select() {
        if (models.isEmpty()) {
            return null;
        }
        if (models.size() == 1) {
            return models.get(0);
        }
        int idx = Math.abs(counter.getAndIncrement() % models.size());
        return models.get(idx);
    }

    public int size() {
        return models.size();
    }
}
