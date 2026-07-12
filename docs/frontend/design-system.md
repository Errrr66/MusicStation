# 前端设计系统文档

本文档说明音乐平台前端统一遵循的 Matrix 设计规范，包括色彩、字体、布局与组件使用规范。

## 设计理念

Matrix 设计系统以复古点阵显示器为灵感，采用深色背景、高对比度文字、点阵图表与 monospace 字体，营造科技感与音乐氛围。

## 色彩规范

### 管理端色彩变量

变量定义位于 `musicAdmin/src/style/mr-variables.scss`：

| 变量 | 默认值 | 用途 |
|------|--------|------|
| `--mr-bg-base` | `#050505` | 页面主背景 |
| `--mr-bg-elevated` | `#111` | 卡片/输入框背景 |
| `--mr-color-primary` | `#fff` | 主文字/主按钮 |
| `--mr-border-subdued` | `rgba(255,255,255,0.1)` | 边框色 |
| `--mr-status-success` | `#0f0` | 成功状态 |
| `--mr-status-danger` | `#f00` | 危险/错误状态 |
| `--mr-status-warning` | `#ff0` | 警告状态 |

### 使用约束

- 业务代码禁止使用 `.spotify-*` 类名
- 禁止使用硬编码颜色如 `#1db954`
- 禁止使用 `html.dark` 选择器覆盖样式
- 统一通过 CSS 变量适配深浅主题

## 字体规范

### 全局字体栈

定义位于 `musicAdmin/src/style/reset.scss`：

```css
font-family: "JetBrains Mono", "Fira Code", "SF Mono", "Menlo", "Consolas", monospace;
```

### 规范要求

- 使用全局变量 `--mr-font-family`
- 业务组件中不得重复声明 `font-family`
- 标题与正文统一使用 monospace 字体栈

## 布局规范

### 登录页

- 桌面端：左视觉区 + 右登录卡片
- 比例：桌面 6:4，平板 5:5，移动端纯黑背景
- 登录卡片最大宽度 460px，居中对齐
- 输入框圆角、主按钮白色

### 管理端内部页

- 顶部搜索表单
- 中部 `PureTableBar` + 数据表格
- 表格列居中，表头使用 Matrix 背景色
- 移动端自动换行适配

## 组件规范

### 按钮

- 主按钮：`type="primary"`，白色背景或品牌色
- 危险操作：二次确认弹窗
- 文字按钮：行内修改/删除使用 `link` 样式

### 输入框

- 统一使用 `el-input--large`
- 背景使用 `--mr-bg-elevated`
- 聚焦状态使用品牌色边框

### 表格

- 使用 `pure-table` 组件
- 开启 `adaptive` 自适应高度
- 分页默认居中
- 选中行支持批量操作

### 弹窗

- 移动端弹窗宽度使用动态绑定 `:width`，避免 scoped 样式限制
- 表单弹窗统一使用 `el-dialog` + `el-form`

## 性能规范

- 页面首次加载时间 ≤ 2 秒
- 图表组件优先使用 Canvas 替代 SVG
- 动画使用 `requestAnimationFrame` 并做 30fps 节流
- 离开视口的动画使用 `IntersectionObserver` 暂停
- 标签页切换使用 `visibilitychange` 控制动画

## 响应式断点

| 断点 | 宽度 | 说明 |
|------|------|------|
| 移动端 | ≤ 480px | 单列布局，全宽卡片 |
| 平板 | ≤ 768px | 调整间距与字体 |
| 桌面 | > 768px | 标准左右/上下布局 |

## 组件文档

- [Matrix 组件](./components/matrix.md)
- [Orb 组件](./components/orb.md)
- [登录页设计](./design/login-design.md)
