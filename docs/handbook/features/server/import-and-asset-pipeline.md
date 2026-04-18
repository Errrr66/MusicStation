# 导入与资源处理链路

## 功能边界

- 支持本地歌曲导入、音频解析、资源上传到对象存储。
- 支持导入清单追踪、失败回滚与重复跳过策略。
- 支持 `--dry-run` 预演，降低正式导入风险。

## 服务入口

- 导入脚本：`musicServer/tools/import_vip_songs.py`
- 依赖文件：`musicServer/tools/requirements-vip-import.txt`
- 导入清单：`musicServer/tools/import_manifest.jsonl`

## 接口清单（重点）

- 脚本入口：`python "musicServer/tools/import_vip_songs.py"`
- 预演入口：`python "musicServer/tools/import_vip_songs.py" --dry-run`

## 前置条件

- 数据库与 MinIO 可用，且目标库/桶存在。
- 本地 Python 环境可执行脚本依赖。
- 导入源目录可读，音频文件与歌词文件命名规范。

## 关键流程

1. 扫描源目录中的音频与歌词文件。
2. 解析元数据并准备数据库写入实体。
3. 上传音频/封面/歌手头像到 MinIO 对应目录。
4. 写入歌曲、风格映射等业务数据。
5. 写入导入清单，便于审计与重跑。

## 安全与容错

- 采用重复键检测避免重复导入。
- 单曲失败执行事务回滚并清理已上传对象。
- 支持 `--dry-run` 进行无副作用预演。
- 建议使用最小权限数据库账号和对象存储凭据。
- 正式导入前先限制 `--limit` 小批量验证，避免全量误导入。

## 配置项（重点）

- `--source-dir`：自定义源目录。
- `--limit`：限制本次导入曲目数量。
- `--manifest`：自定义导入清单路径。
- `--mysql-*`：覆盖数据库连接参数。
- `--minio-*`：覆盖对象存储连接参数。
- `--dry-run`：仅解析与校验，不落库不上传。

## 常用命令

### 1) 安装依赖

```bash
python -m pip install -r "musicServer/tools/requirements-vip-import.txt"
```

### 2) 预演（推荐）

```bash
python "musicServer/tools/import_vip_songs.py" --dry-run --limit 20
```

### 3) 小批量正式导入

```bash
python "musicServer/tools/import_vip_songs.py" --limit 20
```

### 4) 全量导入

```bash
python "musicServer/tools/import_vip_songs.py"
```

### 5) 失败重跑（示例）

```bash
python "musicServer/tools/import_vip_songs.py" --manifest "musicServer/tools/import_manifest.jsonl"
```

## 验证步骤

1. 导入前：确认数据库连接、MinIO 连接、源目录可读。
2. 导入中：观察脚本日志，确认无连续失败与权限异常。
3. 导入后：
   - 核对 `tb_song` 新增数量与预期是否一致。
   - 核对 MinIO `songs/`、`songCovers/`、`artists/` 对象是否同步写入。
4. 清单审计：检查 `import_manifest.jsonl` 的 `duplicate`、`success`、`failed` 统计。

## 已知限制与待确认

- 超大批量导入时，建议按目录分批执行以便回溯。
- 网络抖动可能导致对象上传超时，建议在低峰期执行导入。
- 当前文档未覆盖自动告警与监控接入（待补充）。

## 关联文档

- [VIP 歌曲导入工具](../../server/vip-import-tool.md)
- [服务端文档（musicServer）](../../server/music-server.md)
- [变更记录 2026-04-06](../../changelog/2026-04-06.md)
