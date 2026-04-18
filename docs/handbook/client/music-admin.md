# 管理端文档（musicAdmin）

## 版本信息

- 版本：`v1.0.3`
- 日期：`2026-03-08`

## 更新概览

### 新增功能

- 全新登录体验：
  - 重构登录页面为现代化分屏布局。
  - 新增互动动画角色（眼球跟随、密码窥视、打字互动等）。
  - 引入 `InteractiveHoverButton` 悬停交互按钮。
- 全局圆角 UI 系统：
  - 确立全站统一的 `10px` 基础圆角风格。

### 样式调整

- Element Plus 组件深度定制，统一圆角外观：
  - 数据展示：`el-table`、`el-card`、`el-tag`、`el-tree`、`el-pagination`
  - 表单组件：`el-input`、`el-select`、`el-button`、`el-checkbox`、`el-picker`
  - 反馈组件：`el-dialog`、`el-notification`、`el-alert`、`el-popover`
  - 导航组件：`el-dropdown`、`el-tabs`
- 布局与容器优化：
  - 列表页顶部搜索栏（`.search-form`）增加圆角背景。
  - 表格上方工具栏容器（`PureTableBar`）适配圆角风格。

### 配置调整

- 调整 `main.ts` 样式加载顺序，确保自定义主题优先级高于 Element Plus 默认样式。

## 关联文档

- `../README.md`
- `../standards/markdown-style-guide.md`
