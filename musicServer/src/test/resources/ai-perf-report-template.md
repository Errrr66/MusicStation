# AI 服务性能测试报告

## 测试环境
- 日期: {DATE}
- Java: {JAVA_VERSION}
- Spring Boot: 3.4.2
- Spring AI: 1.0.0
- AI 模型: {MODEL_NAME}
- 部署方式: {DEPLOY_MODE}

## 测试场景

### 场景1: 单次对话
- 平均响应时间: {X}ms (目标: 比 DeepSeekService 降低 30%+)
- P50/P95/P99: {X}ms / {X}ms / {X}ms
- Token 使用: prompt={X}, completion={X}

### 场景2: 并发 50 请求
- 成功率: {X}%
- 平均响应时间: {X}ms
- QPS: {X}
- 降级次数: {X}

### 场景3: 流式响应首 token 延迟
- 首 token: {X}ms (目标: < 500ms)
- 完整响应: {X}ms
- chunk 数: {X}

### 场景4: RAG 检索
- 索引构建时间: {X}s
- 单次检索延迟: {X}ms
- Recall@4: {X}% (对比旧 bi-gram: {Y}%)
- 文档分块数: {X}

### 场景5: 稳定性测试(持续 30 分钟)
- 总请求数: {X}
- 错误率: {X}% (目标: < 0.1%)
- 内存使用: {X}MB
- 熔断触发次数: {X}

## Resilience4j 容错指标
- 重试触发次数: {X}
- 熔断器状态: {CLOSED/OPEN/HALF_OPEN}
- 降级触发次数: {X}
- 平均重试次数: {X}

## 资源消耗
- CPU 峰值: {X}%
- 内存峰值: {X}MB
- GC 次数: {X}
- 线程峰值: {X}

## 结论
- 可用性: {PASS/FAIL} (目标 99.9%)
- 并发能力: {PASS/FAIL} (目标提升 50%+)
- 性能提升: {X}% (目标提升 30%+)
- 降级机制: {PASS/FAIL} (异常时正确降级)
