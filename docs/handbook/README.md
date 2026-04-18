# 文档手册

统一文档管理目录（项目唯一文档中心）。

## 目录导航

- [文档规范](standards/markdown-style-guide.md)：Markdown 编写与维护约定。
- [变更记录](changelog/)：按日期管理版本变更。
- [功能文档](features/README.md)：按业务能力拆分的功能说明与接口入口。
- [运行手册](operations/ai-agent-runbook.md)：AI Agent 运行与排障。
- [服务端文档](server/)：接口、RAG、导入工具与服务概览。
- [客户端文档](client/)：用户端、管理端与组件说明。
- [历史归档](archive/notice-legacy.md)：保留历史资料用于追溯。

## 角色导航

- **前端开发**：
  - [客户端文档](client/music-client.md)
  - [管理端文档](client/music-admin.md)
  - [组件说明：ReCountTo](client/recountto-component.md)
- **后端开发**：
  - [服务端总览](server/music-server.md)
  - [Agent 接口文档](server/agent-api.md)
  - [RAG 实现详解](server/rag-implementation.md)
  - [VIP 导入工具](server/vip-import-tool.md)
- **运维 / 支撑**：
  - [AI Agent 运行手册](operations/ai-agent-runbook.md)
  - [变更记录 2026-04-18](changelog/2026-04-18.md)
  - [变更记录 2026-04-06](changelog/2026-04-06.md)
- **产品 / 测试**：
  - [功能文档总览](features/README.md)
  - [变更记录 2026-04-18](changelog/2026-04-18.md)
  - [变更记录 2026-04-06](changelog/2026-04-06.md)

## 维护说明

- 新增文档时，优先放入对应子目录，并同步更新本索引。
- 旧文档进入 `archive/`，避免散落在业务目录。
