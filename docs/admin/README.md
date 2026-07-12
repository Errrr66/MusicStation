# 管理端操作指南

本目录存放音乐管理后台（musicAdmin）的功能说明与操作手册，面向运营与管理人员。

## 功能模块

| 模块 | 说明 | 文档 |
|------|------|------|
| 登录与安全 | 账号登录、记住我、自动退出、密码策略 | [login-guide.md](./login-guide.md) |
| 首页看板 | 平台核心指标与数据图表 | [dashboard-guide.md](./dashboard-guide.md) |
| 用户管理 | 用户账号的增删改查与状态控制 | [user-management.md](./user-management.md) |
| 歌曲管理 | 歌曲资源维护、封面/音频上传、歌词预览 | [song-management.md](./song-management.md) |
| 歌单管理 | 歌单创建、编辑、封面管理与风格维护 | [playlist-management.md](./playlist-management.md) |
| 艺人管理 | 艺人信息维护、头像上传与类型管理 | [artist-management.md](./artist-management.md) |
| 轮播图管理 | 首页轮播图上传、启用/禁用与删除 | [banner-management.md](./banner-management.md) |
| 用户反馈 | 用户意见反馈查看与处理 | [feedback-management.md](./feedback-management.md) |

## 通用操作说明

### 列表页常见元素

- **搜索区域**：位于列表上方，支持按多个条件筛选
- **操作按钮**：新增、搜索、重置、批量删除
- **数据表格**：展示分页数据，支持列自定义
- **行级操作**：修改、删除、更多（上传、预览等）

### 表单页常见规则

- 红色星号（*）表示必填字段
- 系统自动生成的编号字段不可编辑
- 提交前会进行前端校验，校验失败会给出提示
- 新增/编辑成功后自动刷新列表并关闭弹窗

### 批量操作说明

1. 表格左侧勾选列支持单选与全选
2. 选中后顶部出现“已选 N 项”提示条
3. 点击“批量删除”前会弹出二次确认
4. 取消选择可清空当前选中状态

## 相关技术文档

- 前端实现参考 [frontend/README.md](../frontend/README.md)
- 后端接口参考 [backend/README.md](../backend/README.md)
- 环境配置参考 [environment/README.md](../environment/README.md)
