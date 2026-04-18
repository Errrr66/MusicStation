# VIP 歌曲导入工具

该工具用于把本地网易云下载目录中的歌曲导入项目数据库（`vibe_music`）和 MinIO 桶（`vibe-music-data`）。

## 功能说明

- 读取 `.ncm`、`.mp3`、`.flac` 音频文件。
- 将 `.ncm` 解密为可播放音频字节。
- 读取同名 `.lrc` 歌词文件。
- 提取元数据（歌名、歌手、专辑、时长、发行日期、封面）。
- 上传歌曲音频到 MinIO `songs/`。
- 上传歌曲封面到 MinIO `songCovers/`。
- 缺失歌手时自动创建，并上传头像到 MinIO `artists/`。
- 写入 `tb_song` 与 `tb_genre` 映射。
- 生成 JSONL 清单用于审计与重跑。

## 相关文件

- 脚本：`musicServer/tools/import_vip_songs.py`
- 依赖：`musicServer/tools/requirements-vip-import.txt`
- 清单：`musicServer/tools/import_manifest.jsonl`

## 安装依赖

```bash
python -m pip install -r "musicServer/tools/requirements-vip-import.txt"
```

## 预演（推荐先执行）

```bash
python "musicServer/tools/import_vip_songs.py" --dry-run --limit 20
```

## 正式导入

```bash
python "musicServer/tools/import_vip_songs.py"
```

## 常用参数

- `--source-dir`：自定义源目录。
- `--limit`：仅导入前 N 首。
- `--manifest`：自定义清单输出路径。
- `--mysql-*`：覆盖数据库配置。
- `--minio-*`：覆盖 MinIO 配置。
- `--dry-run`：仅解析，不写入 DB/MinIO。

## 安全说明

- 重复检测键：`(artist_id, song_name, album)`。
- 单首导入失败时，数据库事务会回滚并删除已上传对象。
- 已存在歌曲会标记为 `duplicate` 并跳过。

## 关联文档

- [导入与资源处理链路](../features/server/import-and-asset-pipeline.md)
- [服务端文档（musicServer）](music-server.md)
