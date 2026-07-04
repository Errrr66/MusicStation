package com.example.music.util;

import java.util.Map;

/**
 * ThreadLocal 工具类
 */
public class ThreadLocalUtil {

    // 提供 ThreadLocal 对象，明确泛型类型，避免原始类型带来的强制转换与告警
    private static final ThreadLocal<Map<String, Object>> THREAD_LOCAL = new ThreadLocal<>();

    // 根据键获取值
    public static Map<String, Object> get() {
        return THREAD_LOCAL.get();
    }

    // 存储键值对
    public static void set(Map<String, Object> value) {
        THREAD_LOCAL.set(value);
    }

    // 安全获取：当 ThreadLocal 未设置时返回 null 而非抛出 NPE
    public static <T> T getValue(String key) {
        Map<String, Object> map = THREAD_LOCAL.get();
        if (map == null) {
            return null;
        }
        return (T) map.get(key);
    }

    // 清除ThreadLocal 防止内存泄漏
    public static void remove() {
        THREAD_LOCAL.remove();
    }
}
