#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
FocusPets 本地/局域网后端：匿名账号 + 状态同步 + 排行榜。
零依赖，仅用 Python 标准库 + sqlite3。

运行:
    python leaderboard_server.py [port]        # 默认 8080
    python leaderboard_server.py 8080

Android 连接:
    模拟器        -> http://10.0.2.2:<port>/
    真机 USB 调试  -> 先执行 `adb reverse tcp:<port> tcp:<port>`，App 填 http://127.0.0.1:<port>/
                      （adb reverse 把手机本机回环转发到电脑，避开 WiFi 网段隔离，最稳）
    真机同 WiFi   -> http://<开发机局域网IP>:<port>/  （需手机与电脑在同一网段）
"""
import json
import sqlite3
import uuid
import random
import datetime
import sys
import urllib.parse
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

DB_PATH = "focuspets.db"
PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 8080


def now_ms() -> int:
    return int(datetime.datetime.now().timestamp() * 1000)


def random_name() -> str:
    return "专注者" + str(random.randint(1000, 9999))


def init_db() -> None:
    conn = sqlite3.connect(DB_PATH)
    conn.execute(
        """
        CREATE TABLE IF NOT EXISTS users (
            uid          TEXT PRIMARY KEY,
            display_name TEXT,
            total_points INTEGER DEFAULT 0,
            collection   TEXT DEFAULT '[]',
            wardrobe     TEXT DEFAULT '[]',
            mood         TEXT DEFAULT 'NORMAL',
            updated_at   INTEGER,
            current_pet  INTEGER DEFAULT 1,
            pet_avatar   TEXT DEFAULT ''
        )
        """
    )
    # 老库可能缺这两个字段，补列（幂等）
    for col, ddl in (
        ("current_pet", "ALTER TABLE users ADD COLUMN current_pet INTEGER DEFAULT 1"),
        ("pet_avatar", "ALTER TABLE users ADD COLUMN pet_avatar TEXT DEFAULT ''"),
    ):
        try:
            conn.execute(ddl)
        except sqlite3.OperationalError:
            pass
    conn.commit()
    conn.close()


def row_to_user(row) -> dict:
    return {
        "uid": row[0],
        "displayName": row[1],
        "totalPoints": row[2],
        "collection": json.loads(row[3]),
        "wardrobe": json.loads(row[4]),
        "mood": row[5],
        "updatedAt": row[6],
        "currentPet": row[7] if len(row) > 7 else 1,
        "petAvatar": row[8] if len(row) > 8 else "",
    }


class Handler(BaseHTTPRequestHandler):
    server_version = "FocusPetsBackend/1.0"

    # ---- 工具 ----
    def _send(self, code: int, obj) -> None:
        payload = json.dumps(obj, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)

    def _read_json(self) -> dict:
        length = int(self.headers.get("Content-Length", 0) or 0)
        if length == 0:
            return {}
        try:
            return json.loads(self.rfile.read(length).decode("utf-8") or "{}")
        except Exception:
            return {}

    def log_message(self, fmt, *args):
        sys.stderr.write("[backend] " + (fmt % args) + "\n")

    # ---- 路由 ----
    def do_OPTIONS(self):
        self._send(204, {})

    def do_POST(self):
        if self.path == "/api/auth/anon":
            uid = uuid.uuid4().hex
            name = random_name()
            conn = sqlite3.connect(DB_PATH)
            conn.execute(
                "INSERT INTO users(uid, display_name, updated_at) VALUES (?,?,?)",
                (uid, name, now_ms()),
            )
            conn.commit()
            conn.close()
            self._send(200, {"uid": uid, "displayName": name})
            return
        self._send(404, {"error": "not found", "path": self.path})

    def do_GET(self):
        if self.path.startswith("/api/users/"):
            uid = self.path.split("/")[-1].split("?")[0]
            conn = sqlite3.connect(DB_PATH)
            row = conn.execute(
                "SELECT uid, display_name, total_points, collection, wardrobe, mood, updated_at, "
                "current_pet, pet_avatar FROM users WHERE uid=?",
                (uid,),
            ).fetchone()
            conn.close()
            if not row:
                self._send(404, {"error": "no such user"})
                return
            self._send(200, row_to_user(row))
            return

        if self.path.startswith("/api/leaderboard"):
            qs = urllib.parse.parse_qs(urllib.parse.urlparse(self.path).query)
            limit = int(qs.get("limit", ["50"])[0])
            me_uid = qs.get("uid", [""])[0]
            conn = sqlite3.connect(DB_PATH)
            rows = conn.execute(
                "SELECT uid, display_name, total_points, current_pet, pet_avatar "
                "FROM users ORDER BY total_points DESC, updated_at ASC LIMIT ?",
                (limit,),
            ).fetchall()
            conn.close()
            listing = [
                {
                    "rank": i + 1,
                    "uid": r[0],
                    "displayName": r[1],
                    "totalPoints": r[2],
                    "petId": r[3],
                    "petAvatar": r[4],
                }
                for i, r in enumerate(rows)
            ]
            me = None
            if me_uid:
                for item in listing:
                    if item["uid"] == me_uid:
                        me = {"rank": item["rank"], "totalPoints": item["totalPoints"]}
                        break
                if me is None:
                    me = {"rank": None, "totalPoints": 0}
            self._send(200, {"list": listing, "me": me})
            return

        self._send(404, {"error": "not found", "path": self.path})

    def do_PUT(self):
        if self.path.startswith("/api/users/"):
            uid = self.path.split("/")[-1].split("?")[0]
            body = self._read_json()
            conn = sqlite3.connect(DB_PATH)
            row = conn.execute(
                "SELECT uid, display_name, total_points, collection, wardrobe, mood, updated_at, "
                "current_pet, pet_avatar FROM users WHERE uid=?",
                (uid,),
            ).fetchone()
            if not row:
                conn.close()
                self._send(404, {"error": "no such user"})
                return
            name = body.get("displayName", row[1])
            tp = int(body.get("totalPoints", row[2]))
            coll = body.get("collection", json.loads(row[3]))
            ward = body.get("wardrobe", json.loads(row[4]))
            mood = body.get("mood", row[5])
            cur_pet = int(body.get("currentPetId", row[7] if len(row) > 7 else 1))
            pet_av = body.get("petAvatar", row[8] if len(row) > 8 else "")
            conn.execute(
                "UPDATE users SET display_name=?, total_points=?, collection=?, wardrobe=?, mood=?, "
                "updated_at=?, current_pet=?, pet_avatar=? WHERE uid=?",
                (name, tp, json.dumps(coll, ensure_ascii=False), json.dumps(ward, ensure_ascii=False),
                 mood, now_ms(), cur_pet, pet_av, uid),
            )
            conn.commit()
            updated = conn.execute(
                "SELECT uid, display_name, total_points, collection, wardrobe, mood, updated_at, "
                "current_pet, pet_avatar FROM users WHERE uid=?",
                (uid,),
            ).fetchone()
            conn.close()
            self._send(200, row_to_user(updated))
            return
        self._send(404, {"error": "not found", "path": self.path})


def main() -> None:
    init_db()
    httpd = ThreadingHTTPServer(("0.0.0.0", PORT), Handler)
    print(f"FocusPets backend listening on http://0.0.0.0:{PORT}")
    print(f"  DB: {DB_PATH}  |  模拟器连 http://10.0.2.2:{PORT}/  |  真机连 http://<本机局域网IP>:{PORT}/")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nshutdown.")
        httpd.shutdown()


if __name__ == "__main__":
    main()
