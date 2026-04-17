# VIP Songs Import Tool

This tool imports local songs from NetEase download folders into this project database (`vibe_music`) and MinIO bucket (`vibe-music-data`).

## What It Does
- Reads audio from `.ncm`, `.mp3`, `.flac`
- Decrypts `.ncm` into playable audio bytes
- Reads matching `.lrc` lyric files
- Extracts metadata (song name, artist, album, duration, release date, cover)
- Uploads song audio to MinIO `songs/`
- Uploads song cover to MinIO `songCovers/`
- Creates artist (if missing) and artist avatar in MinIO `artists/`
- Inserts `tb_song` and `tb_genre` mappings
- Writes a JSONL manifest for audit and reruns 

## Files
- Script: `musicServer/tools/import_vip_songs.py`
- Requirements: `musicServer/tools/requirements-vip-import.txt`
- Manifest output: `musicServer/tools/import_manifest.jsonl`

## Install
```bash
python -m pip install -r "E:\IntelliJ IDEA\workspace\music\musicServer\tools\requirements-vip-import.txt"
```

## Dry Run (recommended first)
```bash
python "E:\IntelliJ IDEA\workspace\music\musicServer\tools\import_vip_songs.py" --dry-run --limit 20
```

## Real Import
```bash
python "E:\IntelliJ IDEA\workspace\music\musicServer\tools\import_vip_songs.py"
```

## Useful Options
- `--source-dir` custom source folder
- `--limit` import only first N tracks
- `--manifest` custom manifest path
- `--mysql-*` override DB settings
- `--minio-*` override MinIO settings
- `--dry-run` parse only, do not write DB/MinIO

## Safety Notes
- Duplicate detection key: `(artist_id, song_name, album)`
- On single-track failure, DB transaction is rolled back and uploaded objects are deleted
- Existing songs are skipped as `duplicate`

