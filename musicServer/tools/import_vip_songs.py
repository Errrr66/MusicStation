#!/usr/bin/env python3
"""Batch import local songs (ncm/mp3/flac + lrc) into MinIO and MySQL for this project."""

from __future__ import annotations

import argparse
import base64
import datetime as dt
import hashlib
import io
import json
import os
import re
import struct
import sys
import uuid
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Dict, List, Optional, Sequence, Tuple

import pymysql
from Crypto.Cipher import AES
from minio import Minio
from minio.error import S3Error
from mutagen import File as MutagenFile
from mutagen.flac import FLAC
from mutagen.id3 import ID3
from PIL import Image, ImageDraw, ImageFont

CORE_KEY = bytes.fromhex("687A4852416D736F356B496E62617857")
META_KEY = bytes.fromhex("2331346C6A6B5F215C5D2630553C2728")

AUDIO_EXTENSIONS = {".ncm", ".mp3", ".flac"}


def pkcs7_unpad(data: bytes) -> bytes:
    if not data:
        return data
    pad = data[-1]
    if pad < 1 or pad > 16:
        return data
    if data[-pad:] != bytes([pad]) * pad:
        return data
    return data[:-pad]


def aes_ecb_decrypt(key: bytes, data: bytes) -> bytes:
    cipher = AES.new(key, AES.MODE_ECB)
    return cipher.decrypt(data)


def build_key_box(key_data: bytes) -> List[int]:
    box = list(range(256))
    c = 0
    key_len = len(key_data)
    for i in range(256):
        c = (box[i] + c + key_data[i % key_len]) & 0xFF
        box[i], box[c] = box[c], box[i]
    return box


def decode_ncm_file(path: Path) -> Tuple[bytes, Dict[str, Any], bytes, str]:
    with path.open("rb") as f:
        header = f.read(8)
        if header != b"CTENFDAM":
            raise ValueError(f"{path} is not an NCM file")

        f.read(2)
        key_len = struct.unpack("<I", f.read(4))[0]
        key_data = bytearray(f.read(key_len))
        for i in range(key_len):
            key_data[i] ^= 0x64
        # Decrypted key block is PKCS7 padded and prefixed with "neteasecloudmusic" (17 bytes).
        key_plain = pkcs7_unpad(aes_ecb_decrypt(CORE_KEY, bytes(key_data)))
        key_data = bytearray(key_plain[17:])
        if not key_data:
            raise ValueError(f"{path} has empty key data after decrypt")
        key_box = build_key_box(key_data)

        meta_len = struct.unpack("<I", f.read(4))[0]
        meta = {}
        if meta_len > 0:
            meta_blob = bytearray(f.read(meta_len))
            for i in range(meta_len):
                meta_blob[i] ^= 0x63
            decoded = base64.b64decode(bytes(meta_blob)[22:])
            plain = pkcs7_unpad(aes_ecb_decrypt(META_KEY, decoded))[6:]
            try:
                meta = json.loads(plain.decode("utf-8"))
            except json.JSONDecodeError:
                meta = {}

        f.read(4)  # crc32
        f.read(5)
        image_size = struct.unpack("<I", f.read(4))[0]
        image_data = f.read(image_size) if image_size > 0 else b""

        encrypted = f.read()

    out = bytearray(len(encrypted))
    for i, val in enumerate(encrypted):
        j = (i + 1) & 0xFF
        out[i] = val ^ key_box[(key_box[j] + key_box[(key_box[j] + j) & 0xFF]) & 0xFF]

    fmt = str(meta.get("format") or "mp3").lower()
    if fmt not in {"mp3", "flac"}:
        fmt = "mp3"
    return bytes(out), meta, image_data, fmt


def parse_filename_stem(stem: str) -> Tuple[List[str], str]:
    if " - " in stem:
        left, right = stem.split(" - ", 1)
        artists = [a.strip() for a in left.split(",") if a.strip()]
        title = right.strip()
        return artists or ["Unknown Artist"], title or stem
    return ["Unknown Artist"], stem


def normalize_artists(raw: str) -> List[str]:
    parts = re.split(r"[,/&;|]", raw)
    artists = [p.strip() for p in parts if p.strip()]
    return artists or ["Unknown Artist"]


def first_non_empty(values: Sequence[Optional[str]]) -> str:
    for value in values:
        if value:
            v = value.strip()
            if v:
                return v
    return ""


def parse_release_date(value: str) -> Optional[dt.date]:
    if not value:
        return None
    value = value.strip()
    for fmt in ("%Y-%m-%d", "%Y/%m/%d", "%Y.%m.%d", "%Y-%m", "%Y/%m", "%Y"):
        try:
            parsed = dt.datetime.strptime(value, fmt)
            if fmt == "%Y":
                return dt.date(parsed.year, 1, 1)
            if fmt in {"%Y-%m", "%Y/%m"}:
                return dt.date(parsed.year, parsed.month, 1)
            return parsed.date()
        except ValueError:
            continue
    return None


def detect_chinese(text: str) -> bool:
    return bool(re.search(r"[\u4e00-\u9fff]", text or ""))


def guess_styles(title: str, artists: Sequence[str], album: str, lyric: str, default_style: str) -> List[str]:
    text = f"{title} {' '.join(artists)} {album} {lyric[:300]}".lower()
    styles: List[str] = []

    if any(k in text for k in ["live", "rock", "punk", "metal"]):
        styles.append("摇滚")
    if any(k in text for k in ["r&b", "rhythm", "blues", "soul"]):
        styles.append("节奏布鲁斯")
    if any(k in text for k in ["ost", "soundtrack", "from "]):
        styles.append("原声带")
    if any(k in text for k in ["electro", "edm", "house", "trance"]):
        styles.append("电子")

    if detect_chinese(title + album + " ".join(artists)):
        styles.append("华语流行")
    elif re.search(r"[\u3040-\u30ff\u31f0-\u31ff]", title + album):
        styles.append("日本流行")
    elif re.search(r"[\uac00-\ud7af]", title + album):
        styles.append("韩国流行")
    else:
        styles.append("欧美流行")

    if not styles:
        styles = [default_style]

    # Keep order and de-duplicate.
    seen = set()
    deduped: List[str] = []
    for style in styles:
        if style not in seen:
            seen.add(style)
            deduped.append(style)
    return deduped


def load_lrc(lrc_path: Path) -> str:
    if not lrc_path.exists():
        return ""
    for enc in ("utf-8-sig", "utf-8", "gbk", "latin-1"):
        try:
            return lrc_path.read_text(encoding=enc).strip()
        except UnicodeDecodeError:
            continue
    return ""


def sanitize_filename(name: str) -> str:
    cleaned = re.sub(r'[\\/:*?"<>|]+', "_", name)
    return re.sub(r"\s+", " ", cleaned).strip() or "unknown"


def make_placeholder_cover(title: str, artist: str) -> bytes:
    img = Image.new("RGB", (512, 512), color=(28, 28, 32))
    draw = ImageDraw.Draw(img)
    font = ImageFont.load_default()
    line1 = (title[:28] + "...") if len(title) > 28 else title
    line2 = (artist[:28] + "...") if len(artist) > 28 else artist
    draw.text((32, 210), line1 or "Untitled", fill=(230, 230, 230), font=font)
    draw.text((32, 245), line2 or "Unknown Artist", fill=(140, 180, 255), font=font)
    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=90)
    return buf.getvalue()


def parse_mutagen(audio_path: Path) -> Dict[str, Any]:
    data: Dict[str, Any] = {
        "title": "",
        "artist": "",
        "album": "",
        "date": "",
        "duration": "",
        "cover": b"",
    }

    try:
        audio = MutagenFile(str(audio_path), easy=True)
        if audio:
            data["title"] = first_non_empty(audio.get("title", [""]))
            data["artist"] = first_non_empty(audio.get("artist", [""]))
            data["album"] = first_non_empty(audio.get("album", [""]))
            data["date"] = first_non_empty(audio.get("date", [""]) + audio.get("year", [""]))
            if getattr(audio, "info", None) and getattr(audio.info, "length", None):
                data["duration"] = f"{audio.info.length:.2f}"
    except Exception:
        pass

    ext = audio_path.suffix.lower()
    if ext == ".mp3":
        try:
            tags = ID3(str(audio_path))
            if "APIC:" in tags:
                data["cover"] = tags["APIC:"].data
            else:
                for key in tags.keys():
                    if key.startswith("APIC"):
                        data["cover"] = tags[key].data
                        break
        except Exception:
            pass
    elif ext == ".flac":
        try:
            tags = FLAC(str(audio_path))
            if tags.pictures:
                data["cover"] = tags.pictures[0].data
        except Exception:
            pass

    return data


@dataclass
class TrackPayload:
    source: Path
    audio_bytes: bytes
    audio_ext: str
    song_name: str
    artists: List[str]
    album: str
    lyric: str
    duration: str
    release_time: dt.date
    styles: List[str]
    cover_bytes: bytes


def collect_tracks(source_dir: Path, temp_dir: Path, limit: int, default_style: str) -> List[TrackPayload]:
    tracks: List[TrackPayload] = []
    candidates = sorted([p for p in source_dir.iterdir() if p.suffix.lower() in AUDIO_EXTENSIONS], key=lambda p: p.name.lower())

    for src in candidates:
        if 0 < limit <= len(tracks):
            break

        try:
            stem = src.stem
            file_artists, file_title = parse_filename_stem(stem)
            lrc_text = load_lrc(src.with_suffix(".lrc"))

            audio_bytes: bytes
            cover_bytes: bytes = b""
            mutagen_data: Dict[str, Any] = {}
            ncm_meta: Dict[str, Any] = {}

            if src.suffix.lower() == ".ncm":
                audio_bytes, ncm_meta, ncm_cover, fmt = decode_ncm_file(src)
                cover_bytes = ncm_cover
                temp_audio = temp_dir / f"{src.stem}.{fmt}"
                temp_audio.write_bytes(audio_bytes)
                mutagen_data = parse_mutagen(temp_audio)
                audio_ext = f".{fmt}"
            else:
                audio_bytes = src.read_bytes()
                mutagen_data = parse_mutagen(src)
                audio_ext = src.suffix.lower()

            title = first_non_empty([
                mutagen_data.get("title", ""),
                ncm_meta.get("musicName", "") if ncm_meta else "",
                file_title,
            ])

            if ncm_meta and ncm_meta.get("artist"):
                artists = [item[0] for item in ncm_meta.get("artist", []) if item and item[0]]
            else:
                artists = normalize_artists(first_non_empty([mutagen_data.get("artist", ""), ",".join(file_artists)]))

            album = first_non_empty([
                mutagen_data.get("album", ""),
                ncm_meta.get("album", "") if ncm_meta else "",
                "Unknown Album",
            ])

            release = parse_release_date(first_non_empty([
                mutagen_data.get("date", ""),
                ncm_meta.get("publishTime", "") if ncm_meta else "",
            ]))
            if release is None:
                release = dt.datetime.fromtimestamp(src.stat().st_mtime).date()

            duration = first_non_empty([mutagen_data.get("duration", ""), "0.00"])

            if not cover_bytes:
                cover_bytes = mutagen_data.get("cover", b"")
            if not cover_bytes:
                cover_bytes = make_placeholder_cover(title, artists[0])

            styles = guess_styles(title, artists, album, lrc_text, default_style)

            tracks.append(
                TrackPayload(
                    source=src,
                    audio_bytes=audio_bytes,
                    audio_ext=audio_ext,
                    song_name=title,
                    artists=artists,
                    album=album,
                    lyric=lrc_text,
                    duration=duration,
                    release_time=release,
                    styles=styles,
                    cover_bytes=cover_bytes,
                )
            )
        except Exception as exc:
            print(f"[WARN] skip unreadable file: {src} ({exc})")

    return tracks


class Importer:
    def __init__(self, args: argparse.Namespace):
        self.args = args
        self.db = pymysql.connect(
            host=args.mysql_host,
            port=args.mysql_port,
            user=args.mysql_user,
            password=args.mysql_password,
            database=args.mysql_db,
            charset="utf8mb4",
            autocommit=False,
        )
        self.minio = Minio(
            endpoint=args.minio_endpoint,
            access_key=args.minio_access_key,
            secret_key=args.minio_secret_key,
            secure=args.minio_secure,
        )
        self.endpoint_for_url = args.minio_public_endpoint.rstrip("/")
        self.bucket = args.minio_bucket
        self.manifest_path = Path(args.manifest)
        self.manifest_path.parent.mkdir(parents=True, exist_ok=True)

        if not self.minio.bucket_exists(self.bucket):
            raise RuntimeError(f"MinIO bucket does not exist: {self.bucket}")

        self.style_id_map = self._load_style_map()

    def _load_style_map(self) -> Dict[str, int]:
        with self.db.cursor() as cur:
            cur.execute("SELECT id, name FROM tb_style")
            rows = cur.fetchall()
        return {name: sid for sid, name in rows}

    def _artist_area(self, artist_name: str) -> Optional[str]:
        if detect_chinese(artist_name):
            return "中国"
        return None

    def _upload_bytes(self, folder: str, object_name: str, data: bytes, content_type: str) -> Tuple[str, str]:
        key = f"{folder}/{uuid.uuid4()}-{object_name}"
        self.minio.put_object(
            bucket_name=self.bucket,
            object_name=key,
            data=io.BytesIO(data),
            length=len(data),
            content_type=content_type,
        )
        url = f"{self.endpoint_for_url}/{self.bucket}/{key}"
        return key, url

    def _delete_object_quiet(self, key: str) -> None:
        try:
            self.minio.remove_object(self.bucket, key)
        except S3Error:
            pass

    def import_track(self, track: TrackPayload) -> str:
        artist_name = track.artists[0]
        with self.db.cursor() as cur:
            cur.execute("SELECT id, avatar FROM tb_artist WHERE name=%s ORDER BY id LIMIT 1", (artist_name,))
            row = cur.fetchone()

            artist_id: int
            avatar_url: Optional[str] = None
            if row:
                artist_id = int(row[0])
                avatar_url = row[1]
            else:
                cur.execute(
                    "INSERT INTO tb_artist(name, gender, avatar, birth, area, introduction) VALUES(%s, NULL, NULL, NULL, %s, %s)",
                    (artist_name, self._artist_area(artist_name), f"Auto imported artist: {artist_name}"),
                )
                artist_id = int(cur.lastrowid)

            cur.execute(
                "SELECT id FROM tb_song WHERE artist_id=%s AND name=%s AND album=%s ORDER BY id LIMIT 1",
                (artist_id, track.song_name, track.album),
            )
            exists = cur.fetchone()
            if exists:
                return "duplicate"

            uploaded_keys: List[str] = []
            try:
                display_name = sanitize_filename(f"{artist_name} - {track.song_name}{track.audio_ext}")
                content_type = "audio/mpeg" if track.audio_ext == ".mp3" else "audio/flac"
                audio_key, audio_url = self._upload_bytes("songs", display_name, track.audio_bytes, content_type)
                uploaded_keys.append(audio_key)

                cover_name = sanitize_filename(f"{artist_name}-{track.song_name}.jpg")
                cover_key, cover_url = self._upload_bytes("songCovers", cover_name, track.cover_bytes, "image/jpeg")
                uploaded_keys.append(cover_key)

                if not avatar_url:
                    avatar_name = sanitize_filename(f"{artist_name}.jpg")
                    avatar_key, avatar_url_new = self._upload_bytes("artists", avatar_name, track.cover_bytes, "image/jpeg")
                    uploaded_keys.append(avatar_key)
                    cur.execute("UPDATE tb_artist SET avatar=%s WHERE id=%s", (avatar_url_new, artist_id))

                cur.execute(
                    "INSERT INTO tb_song(artist_id, name, album, lyric, duration, style, cover_url, audio_url, release_time) "
                    "VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s)",
                    (
                        artist_id,
                        track.song_name,
                        track.album,
                        track.lyric or None,
                        track.duration,
                        ",".join(track.styles),
                        cover_url,
                        audio_url,
                        track.release_time,
                    ),
                )
                song_id = int(cur.lastrowid)

                style_ids = [self.style_id_map[s] for s in track.styles if s in self.style_id_map]
                for sid in style_ids:
                    cur.execute("INSERT IGNORE INTO tb_genre(song_id, style_id) VALUES(%s,%s)", (song_id, sid))

                self.db.commit()
                return "imported"
            except Exception:
                self.db.rollback()
                for key in reversed(uploaded_keys):
                    self._delete_object_quiet(key)
                raise

    def write_manifest(self, payload: Dict[str, Any]) -> None:
        with self.manifest_path.open("a", encoding="utf-8") as f:
            f.write(json.dumps(payload, ensure_ascii=False) + "\n")

    def close(self) -> None:
        self.db.close()


def build_arg_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(description="Import songs into project MySQL + MinIO")
    p.add_argument("--source-dir", default=r"E:\CloudMusic\VipSongsDownload")
    p.add_argument("--temp-dir", default=r"E:\IntelliJ IDEA\workspace\music\musicServer\tools\.tmp_audio")
    p.add_argument("--manifest", default=r"E:\IntelliJ IDEA\workspace\music\musicServer\tools\import_manifest.jsonl")
    p.add_argument("--limit", type=int, default=0, help="0 means no limit")
    p.add_argument("--dry-run", action="store_true")
    p.add_argument("--default-style", default="欧美流行")

    p.add_argument("--mysql-host", default="127.0.0.1")
    p.add_argument("--mysql-port", type=int, default=3306)
    p.add_argument("--mysql-user", default="root")
    p.add_argument("--mysql-password", default="123456")
    p.add_argument("--mysql-db", default="vibe_music")

    p.add_argument("--minio-endpoint", default="127.0.0.1:9000")
    p.add_argument("--minio-public-endpoint", default="http://127.0.0.1:9000")
    p.add_argument("--minio-access-key", default="minioadmin")
    p.add_argument("--minio-secret-key", default="minioadmin")
    p.add_argument("--minio-bucket", default="vibe-music-data")
    p.add_argument("--minio-secure", action="store_true")

    return p


def main() -> int:
    args = build_arg_parser().parse_args()

    source_dir = Path(args.source_dir)
    if not source_dir.exists():
        print(f"Source dir does not exist: {source_dir}", file=sys.stderr)
        return 2

    temp_dir = Path(args.temp_dir)
    temp_dir.mkdir(parents=True, exist_ok=True)

    tracks = collect_tracks(source_dir, temp_dir, args.limit, args.default_style)
    print(f"Collected tracks: {len(tracks)}")

    if args.dry_run:
        for t in tracks[:10]:
            print(f"[DRY] {t.source.name} => {t.artists[0]} | {t.song_name} | {t.album} | {','.join(t.styles)}")
        return 0

    importer = Importer(args)
    counters = {"imported": 0, "duplicate": 0, "failed": 0}

    try:
        for idx, track in enumerate(tracks, start=1):
            digest = hashlib.sha1(track.audio_bytes).hexdigest()
            try:
                result = importer.import_track(track)
                counters[result] = counters.get(result, 0) + 1
                importer.write_manifest(
                    {
                        "index": idx,
                        "source": str(track.source),
                        "artist": track.artists,
                        "song": track.song_name,
                        "album": track.album,
                        "styles": track.styles,
                        "duration": track.duration,
                        "release_time": track.release_time.isoformat(),
                        "audio_sha1": digest,
                        "result": result,
                        "time": dt.datetime.now().isoformat(timespec="seconds"),
                    }
                )
                print(f"[{idx}/{len(tracks)}] {result.upper()} - {track.source.name}")
            except Exception as exc:
                counters["failed"] += 1
                importer.write_manifest(
                    {
                        "index": idx,
                        "source": str(track.source),
                        "song": track.song_name,
                        "result": "failed",
                        "error": str(exc),
                        "time": dt.datetime.now().isoformat(timespec="seconds"),
                    }
                )
                print(f"[{idx}/{len(tracks)}] FAILED - {track.source.name} - {exc}")

        print("Import done:", counters)
        return 0 if counters["failed"] == 0 else 1
    finally:
        importer.close()


if __name__ == "__main__":
    raise SystemExit(main())

