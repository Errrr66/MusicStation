# 首页数据看板说明

本文档说明管理后台首页（Welcome）数据看板的展示内容与图表含义。

## 页面入口

- 路径：`src/views/welcome`
- 页面：`index.vue`
- 路由：首页 / 欢迎页

## 数据指标卡

页面顶部展示四大核心指标：

| 指标 | 说明 | 数据来源 |
|------|------|----------|
| USERS | 平台用户总数 | `/admin/getAllUsersCount` |
| ARTISTS | 平台歌手总数 | `/admin/getAllArtistsCount` |
| SONGS | 平台歌曲总数 | `/admin/getAllSongsCount` |
| PLAYLISTS | 平台歌单总数 | `/admin/getAllPlaylistsCount` |

每个指标卡附带趋势折线图，展示近 7 个时间点的模拟/真实变化趋势。

## 图表分析

### 折线图（ChartLine）

- 展示平台用户增长趋势
- 采用 Matrix 点阵风格绘制
- 使用 Morandi 色系区分数据系列

### 柱状图（ChartBar）

- 展示不同国家/地区的艺人分布
- 支持按地区维度对比
- 采用 Canvas 渲染以提升性能

### 饼图 1（ChartPie1）

- 展示歌曲风格分布
- 包含：欧美流行、华语流行、粤语流行、韩国流行、古典、嘻哈说唱、摇滚、电子、节奏布鲁斯、轻音乐

### 饼图 2（ChartPie2）

- 展示歌手性别分布
- 包含：男、女

## 性能优化

- 所有图表已统一转换为 Canvas 渲染
- 动画使用 30fps 节流
- 页面不可见时通过 `IntersectionObserver` 暂停动画
- 标签页隐藏时通过 `visibilitychange` 停止渲染

## 使用建议

1. 首页数据看板用于快速了解平台整体运营情况
2. 图表颜色采用 Matrix 设计系统，确保视觉一致性
3. 如需导出数据，可跳转至各管理模块的列表页查看详情

## 相关文档

- 设计系统参考 [frontend/design-system.md](../frontend/design-system.md)
- 后端统计接口参考 [backend/api-reference.md](../backend/api-reference.md)
