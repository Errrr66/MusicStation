# 前端架构文档

本文档说明音乐平台前端项目的整体架构、技术选型与目录组织。

## 项目结构

音乐平台包含两个前端项目：

| 项目 | 技术栈 | 用途 |
|------|--------|------|
| musicAdmin | Vue 3 + Vite + TypeScript + Element Plus + Pinia | 管理后台 |
| musicClient | React 18 + TypeScript + Vite + Tailwind CSS | 用户客户端 |

## 管理端（musicAdmin）

### 技术栈

- **框架**：Vue 3（Composition API）
- **构建工具**：Vite
- **类型系统**：TypeScript
- **UI 组件库**：Element Plus
- **状态管理**：Pinia
- **路由**：Vue Router
- **HTTP 请求**：Axios
- **图标**：Iconify + `@iconify/vue`
- **样式**：SCSS + Matrix 设计变量

### 目录结构

```
musicAdmin/src/
├── api/                    # 接口请求封装
├── components/             # 全局组件
│   ├── RePureTableBar/     # 表格工具栏
│   ├── ReCol/              # 响应式列组件
│   └── ReIcon/             # 图标组件
├── layout/                 # 布局组件
│   ├── components/         # 侧边栏、内容区、标签页、页脚
│   └── hooks/              # 布局相关 Hooks
├── router/                 # 路由配置
│   ├── modules/            # 路由模块
│   └── index.ts
├── store/                  # Pinia 状态管理
├── style/                  # 全局样式与变量
│   ├── mr-variables.scss   # Matrix 设计变量
│   └── reset.scss
├── utils/                  # 工具函数
├── views/                  # 业务页面
│   ├── login/              # 登录页
│   ├── welcome/            # 首页数据看板
│   ├── user/               # 用户管理
│   ├── song/               # 歌曲管理
│   ├── playlist/           # 歌单管理
│   ├── artist/             # 艺人管理
│   ├── banner/             # 轮播图管理
│   └── feedback/           # 用户反馈
└── App.vue
```

### 页面开发规范

- 每个业务模块独立目录：`views/<module>/`
- 列表页：`index.vue`
- 表单弹窗：`form/index.vue`
- 业务 Hook：`utils/hook.tsx` 或 `hooks.ts`
- 表单校验：`utils/rule.ts`
- 类型定义：`utils/types.ts`

### 表格页通用模式

1. 顶部搜索表单（`el-form`）
2. `PureTableBar` 工具栏（标题、刷新、列自定义、新增按钮）
3. 选中条与批量删除
4. `pure-table` 表格（分页、行操作）

### 布局系统

- `layout/index.vue` 为主布局入口
- 左侧侧边栏导航
- 顶部标签页（Tag）
- 右侧内容区渲染 `RouterView`

## 客户端（musicClient）

### 技术栈

- **框架**：React 18
- **构建工具**：Vite
- **类型系统**：TypeScript
- **样式方案**：Tailwind CSS
- **状态管理**：Zustand / Redux（根据实际项目）
- **路由**：React Router

### 目录结构

```
musicClient/src/
├── api/                    # 接口请求
├── assets/                 # 静态资源
├── components/             # 公共组件
├── pages/                  # 页面组件
├── router/                 # 路由配置
├── store/                  # 状态管理
├── styles/                 # 全局样式
└── utils/                  # 工具函数
```

## 前后端交互

- 统一使用 Axios 发送 HTTP 请求
- 请求头携带 `Authorization: Bearer <token>`
- 响应拦截器统一处理 401/403 状态码
- 文件上传使用 `multipart/form-data`

## 设计系统

详见 [design-system.md](./design-system.md)。
