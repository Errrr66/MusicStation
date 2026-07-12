# 前端文档

本目录存放与前端相关的所有文档，包括页面设计规范、可复用组件说明、架构说明以及管理端功能模块描述。

## 技术栈

| 项目 | 技术栈 | 用途 |
|------|--------|------|
| 管理端（musicAdmin） | Vue 3 + Vite + TypeScript + Element Plus + Pinia | 管理后台 |
| 客户端（musicClient） | Vue 3 + TypeScript + Vite + Tailwind CSS + Element Plus | 用户客户端 |
| 公共设计系统 | Matrix 黑白灰点阵风格 | 视觉规范 |

## 子目录

| 目录 | 说明 |
|------|------|
| [design/](./design/) | 页面视觉与交互设计文档 |
| [components/](./components/) | 可复用 UI 组件使用说明与 API 参考 |

## 文档导航

| 文档 | 说明 |
|------|------|
| [architecture.md](./architecture.md) | 前端项目架构、目录组织与开发规范 |
| [design-system.md](./design-system.md) | Matrix 设计系统：色彩、字体、布局、组件规范 |
| [design/login-design.md](./design/login-design.md) | Lithos 地质主题登录页设计说明 |
| [components/matrix.md](./components/matrix.md) | 复古点阵显示组件 |
| [components/orb.md](./components/orb.md) | 3D 音频响应球体组件 |
| [client-guide.md](./client-guide.md) | 用户客户端页面与功能说明 |

## 开发规范

- 统一使用 Matrix 设计变量，禁止硬编码 Spotify 风格颜色
- 字体统一使用全局 monospace 字体栈
- 管理端业务组件遵循 `views/<module>/` 目录组织
- 移动端弹窗宽度使用动态绑定 `:width`
- 页面加载时间严格控制在 2 秒以内
