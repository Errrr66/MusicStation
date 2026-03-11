主要更新内容如下 (v1.0.3 - 2026-03-08):
新增功能 (Added)
全新登录体验:
重构了登录页面，采用了现代化的分屏设计。
新增 互动动画角色 (Animated Characters)，复刻了输入的交互效果（眼球跟随、密码窥视、打字互动等）。
引入了 InteractiveHoverButton 悬停交互按钮。
全局圆角 UI 系统:
确立了全站统一的 10px 基础圆角风格。
样式变更 (Changed)
Element Plus 组件深度定制:
通过重写 SCSS 变量和样式，强制以下组件应用圆角：
数据展示: 表格 (el-table)、卡片 (el-card)、标签 (el-tag)、树形控件 (el-tree)、分页 (el-pagination)。
表单组件: 输入框 (el-input)、选择器 (el-select)、按钮 (el-button)、多选框 (el-checkbox)、日期选择器 (el-picker)。
反馈组件: 对话框 (el-dialog)、通知 (el-notification)、警告 (el-alert)、气泡卡片 (el-popover)。
导航组件: 下拉菜单 (el-dropdown)、选项卡 (el-tabs)。
布局与容器优化:
列表页顶部的 搜索栏 (.search-form) 现在拥有圆角背景。
表格上方的 工具栏容器 (PureTableBar) 也已适配圆角风格。
配置调整:
调整了 main.ts 中的样式加载顺序，确保自定义主题优先级高于 Element Plus 默认样式。
