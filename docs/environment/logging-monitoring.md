# 日志与监控指南

本文档说明项目的日志配置、关键监控指标与常见问题排查方法。

## 日志配置

### 后端日志

后端使用 SLF4J + Logback，配置文件位于 `musicServer/src/main/resources/logback-spring.xml`（如存在）或通过 `application.yml` 配置。

```yaml
logging:
  level:
    root: INFO
    com.example.music: DEBUG
    com.example.music.mapper: DEBUG  # 开发环境开启 SQL 日志
  file:
    name: logs/music-server.log
```

### 日志级别建议

| 环境 | root | mapper | 说明 |
|------|------|--------|------|
| 开发 | DEBUG | DEBUG | 方便调试 |
| 测试 | INFO | DEBUG | 保留问题排查能力 |
| 生产 | WARN | WARN | 减少日志量，仅保留错误 |

### 关键日志路径

- 登录/登出：`UserServiceImpl`、`AdminServiceImpl`
- 敏感操作：`AdminServiceImpl` 中的删除、批量删除
- AI 调用：`SpringAiService`、`AiCallLogger`
- 文件上传：`MinioServiceImpl`

## 日志轮转

生产环境建议按日期与大小滚动：

```xml
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/music-server.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>logs/music-server.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
        <maxHistory>30</maxHistory>
        <totalSizeCap>10GB</totalSizeCap>
    </rollingPolicy>
    <encoder>
        <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
</appender>
```

## 监控指标

### JVM 监控

- 堆内存使用率
- GC 频率与耗时
- 线程数
- CPU 使用率

### 数据库监控

- 慢查询数量
- 连接池使用率
- 主从延迟（如使用主从）

### Redis 监控

- 连接数
- 命中率
- 内存使用率
- QPS

### 业务监控

- 接口响应时间 P99
- 错误率
- 登录成功率
- AI 接口可用性

## 推荐工具

| 工具 | 用途 |
|------|------|
| Prometheus + Grafana | 指标采集与可视化 |
| ELK（Elasticsearch + Logstash + Kibana） | 日志收集与分析 |
| SkyWalking / Zipkin | 链路追踪 |
| Sentry | 前端错误收集 |

## 告警规则建议

| 规则 | 阈值 | 级别 |
|------|------|------|
| 后端接口错误率 | > 5% | P1 |
| 接口 P99 响应时间 | > 2s | P1 |
| JVM 堆内存使用率 | > 85% | P2 |
| MySQL 慢查询数 | > 10/min | P2 |
| AI 服务不可用 | 连续 3 次失败 | P2 |
| 磁盘使用率 | > 85% | P2 |

## 常见问题排查

### 接口响应慢

1. 查看慢 SQL 日志
2. 检查 Redis 命中率
3. 分析 JVM GC 情况
4. 确认 AI/外部服务响应时间

### 登录失败率高

1. 检查 JWT Secret 配置
2. 查看 Redis 连接是否正常
3. 确认用户账号状态

### AI 接口不可用

1. 访问 `/chat/health` 检查状态
2. 查看 DeepSeek API Key 是否有效
3. 检查网络连通性
4. 查看 Resilience4j 熔断状态

## 相关文档

- [部署环境](./deployment.md)
- [运行环境](./runtime.md)
- [Docker 部署](./docker-deployment.md)
