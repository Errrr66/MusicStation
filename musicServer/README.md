### 版本更新日志 (V1.0.1)
#### 更新日期：2026-03-04
#### 更新类型：功能新增 + 基础设施优化
#### 关联模块：后端（Spring Boot）、前端（Vue）、第三方服务集成

---

### 一、运行环境说明
| 组件         | 启动命令/方式                                                                 | 访问地址               |
|--------------|-----------------------------------------------------------------------------|----------------------|
| MinIO 服务器 | 在 minio/bin 目录执行：`minio.exe server "项目路径\minio\data"`              | -                    |
| Redis 服务   | 启动本地/部署环境Redis服务                                                   | -                    |
| 管理端       | 执行命令：`npx pnpm dev`                                                     | http://localhost:8089 |
| 前端         | 执行命令：`npm run dev`                                                      | http://localhost:8090 |
| 后端         | 运行 VibeMusicApplication 类的 main 方法                                     | -                    |

---

### 二、后端核心更新
#### 1. 第三方服务集成
- 新增 DeepSeek AI 服务集成能力，支持 OpenAI 兼容格式的 HTTP 通信
- 在 `application.yml` 中新增配置项：
  - DeepSeek API Key（接口鉴权）
  - DeepSeek Base URL（接口请求地址）
  - 模型名称（指定调用的AI模型）

#### 2. 核心类/接口新增
- 服务层：新增 `DeepSeekService` 类，封装与 DeepSeek API 的请求/响应处理逻辑
- 控制层：新增 `ChatController`，开放 `/chat/ask` 接口，接收前端消息并返回AI回复
- 配置类：新增 `RestTemplateConfig`，配置并注入 `RestTemplate` Bean，用于HTTP请求发送

#### 3. 数据模型更新
- DTO层：新增 `ChatRequestDTO`，标准化接收前端聊天请求参数
- API模型：新增 `ChatCompletionRequest`/`ChatCompletionResponse` 及其内部类，映射DeepSeek API的JSON结构

#### 4. 基础设施优化
- 优化 `Result<T>` 统一返回类：
  - 增加泛型方法支持，提升类型安全性
  - 修复类型转换告警，保证接口返回数据格式统一

---

### 三、前端核心更新
#### 1. 功能模块新增
- 新增 AI 问答功能模块，路径：`src/pages/chat/index.vue`
- 核心交互能力：
  - 消息列表自动滚动，保证最新消息可视
  - 区分用户（蓝色气泡）/AI（灰色气泡）消息样式
  - 新增“思考中...”加载状态反馈，提升用户体验

#### 2. 接口封装
- 在 `src/api/chat.ts` 中新增 `sendChatMessage` 方法，封装后端 `/chat/ask` 接口调用逻辑

#### 3. 路由与导航更新
- 侧边栏：在 `src/layout/components/aside/data.ts` 中新增“AI 问答”菜单项，配置图标及路由跳转规则
- 路由配置：在 `src/routers/index.ts` 中注册 `/chat` 路由，关联AI问答页面

---
### 变更日志
本文件将记录该项目中所有显著的变更。

[已发布] - 2026-03-10
#### 新增功能
- 歌词提取（后端）：通过管理界面上传 MP3 文件时，自动提取文件中嵌入的歌词（ID3 标签）。
- 歌词展示（前端）：
  - 在音乐详情抽屉组件中新增专属的「歌词」标签页；
  - 支持标准 LRC 格式解析；
  - 实现随音乐播放进度自动滚动歌词的功能；
  - 新增点击定位功能：点击歌词行可将播放进度跳转到对应时间点；
  - 采用渐变遮罩样式，实现类似网易云音乐的现代感「渐隐」效果。

#### 功能调整
- 音乐抽屉组件界面：
  - 移除抽屉头部的时钟/时间显示，优化界面简洁度；
  - 将底部纯文字样式的「深色模式」开关替换为太阳/月亮图标切换按钮，与主应用头部样式保持统一。
- 接口（后端）：
  - 更新 SongController 和 SongService 中的 updateSongAudio 接口，使其支持接收并保存歌词数据；
  - 增强 AdminController 中的文件上传逻辑，集成 jaudiotagger 工具提取元数据。

#### 依赖项
- 后端：在 pom.xml 中新增 jaudiotagger（v3.0.1）依赖，用于读取音频文件元数据。

### 总结
1. 核心新增能力：实现了 MP3 歌词提取、LRC 解析、播放同步滚动及点击定位等完整的歌词功能，界面视觉效果对标主流音乐应用；
2. 界面优化：精简音乐抽屉头部信息，统一深色模式切换的交互样式；
3. 技术调整：后端集成 jaudiotagger 依赖，更新接口以适配歌词数据的存储与处理。

### 翻译说明（补充）
1. 技术术语统一：
  - ID3 tags：译为「ID3 标签」（音频文件元数据标准，行业通用译法）；
  - LRC format：译为「LRC 格式」（歌词同步格式，保留缩写+格式）；
  - endpoint：译为「接口」（后端开发通用译法，替代「端点」更符合中文技术语境）；
  - pom.xml：保留文件名（Maven 项目核心配置文件，无需翻译）。
2. 界面术语适配：
  - Admin interface：译为「管理界面」（后台管理系统通用表述）；
  - music detail drawer：译为「音乐详情抽屉组件」（前端抽屉式弹窗，补充「组件」更清晰）；
  - gradient mask / fade-out effect：译为「渐变遮罩样式」「渐隐效果」（贴合视觉设计术语）。
3. 动作逻辑优化：
  - Automatically extracts：译为「自动提取」（突出功能主动性）；
  - click-to-seek feature：译为「点击定位功能」（替代「点击查找」，更符合播放控制场景）；
  - synchronized with music playback progress：译为「随音乐播放进度自动滚动」（简化冗余表述，保留核心逻辑）。
