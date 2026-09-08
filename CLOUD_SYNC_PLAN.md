# FocusPets 云端同步 / 排行榜 实施方案

> 状态：规划已确认（实施中）
> 后端演进：Firebase（国内网络不可达）→ LeanCloud 国内版（注册下线）→ **自建后端（本地/局域网）**
> 已确认方向：**自建后端** · **匿名一键登录** · **全量跨设备同步** · **全局总榜**
> 适用工程：Kotlin + MVVM + Room 单 Activity，包名 `com.example.focuspets`，minSdk 24

---

## 0. 一句话目标

给纯单机应用加「云端身份 + 可同步状态 + 排行榜视图」。因为第三方 BaaS（Firebase / LeanCloud）在当前网络都不可用，**改为自建一个跑在开发机/局域网的小后端**，Android 通过 HTTP 直连。架构接口（`AuthManager` / `CloudSyncManager`）保持抽象，将来换公网/VPS 或别的 BaaS 只改实现层。

---

## 1. 总体架构

```
┌────────── 设备端 (现有) ──────────┐         ┌──── 自建后端 (本地/局域网 HTTP) ────┐
│ ViewModel(LiveData)→Repository    │         │  Python stdlib http.server         │
│            → Room (本地真源)       │  HTTP    │  + sqlite3 (focuspets.db)          │
│ UI ◄── LiveData ◄─────────────────┘ ──────► │                                   │
│                                    ◄──────  │  /api/auth/anon                   │
│ [新增] CloudSyncManager            │  JSON    │  /api/users/{uid}                 │
│  (Retrofit 客户端 + 离线队列)       │         │  /api/leaderboard?limit=50        │
└────────────────────────────────────┘         └───────────────────────────────────┘
```

- 后端语言：本方案用 **Python 标准库 + sqlite3（零依赖，python 直接跑）**。若你想换成 Kotlin Ktor / Node，说一声，API 契约不变，我移植。
- 设计原则不变：**Room 仍是本地真源**，后端是镜像 + 跨设备副本。

---

## 2. 后端 API 契约（JSON over HTTP）

| 方法 | 路径 | 说明 | 请求体 / 参数 | 响应 |
| --- | --- | --- | --- | --- |
| POST | `/api/auth/anon` | 匿名注册，返回 uid | — | `{uid, displayName}` |
| GET | `/api/users/{uid}` | 拉取某用户云端状态 | — | `{uid, displayName, totalPoints, collection[], wardrobe[], mood, updatedAt}` |
| PUT | `/api/users/{uid}` | 上报/合并状态 | 部分字段（`displayName`/`totalPoints`/`collection`/`wardrobe`/`mood`） | 合并后的完整用户对象 |
| GET | `/api/leaderboard?limit=50&uid=` | 取榜单 Top N，可带 `uid` 算自己名次 | query | `{list:[{rank,uid,displayName,totalPoints}], me:{rank,totalPoints}\|null}` |

- 状态码：成功 200，无用户 404，其他 400/500。
- 已开启 CORS（`Access-Control-Allow-Origin: *`），方便模拟器/网页调试。
- `collection` / `wardrobe` 以 JSON 数组字符串存库。

---

## 3. 数据模型映射（Room → 后端 sqlite）

| 本地（Room） | 后端 `users` 表字段 | 说明 |
| --- | --- | --- |
| `focus_records` | `total_points` (聚合) | 专注结算时客户端算好增量上报 |
| `user_collection` | `collection` (JSON 数组) | 已解锁宠物 id |
| `wardrobe_purchases` | `wardrobe` (JSON 数组) | 已购妆扮 id |
| 心情 `SharedPreferences` | `mood` (String) | NORMAL/SICK/GLOW |
| （派生）可用积分 | `total_points` | `Σ专注分钟 − Σ解锁cost − Σ妆扮cost` |
| — | `display_name` | 默认 `专注者{4位}` |
| — | `updated_at` (ms) | 冲突解决 last-write-wins |

排行榜 = `SELECT uid, display_name, total_points FROM users ORDER BY total_points DESC, updated_at ASC LIMIT ?`。

---

## 4. Android 端改动清单

### 4.1 新增 `app/src/main/java/com/example/focuspets/cloud/`
- `CloudConfig.kt` — `BASE_URL`（来自 `local.properties` / `BuildConfig`，如 `http://10.0.2.2:8080/` 模拟器，或局域网 IP）。
- `ApiClient.kt` — Retrofit2 + OkHttp + Gson（`converter-gson`）。
- `AuthManager.kt` — 调 `POST /api/auth/anon` 拿 uid；本地用 `SharedPreferences` 缓存 uid（匿名即绑设备）。
- `CloudSyncManager.kt` — ① `PUT /api/users/{uid}` 增量推送；② 启动/联网 `GET` 拉取合并；③ 离线写队列（Room `sync_queue`）；④ last-write-wins。
- `LeaderboardRepository.kt` — 封装 `GET /api/leaderboard`，返回带"我的排名"的列表。
- `model/CloudUser.kt` / `model/LeaderboardEntry.kt` — Retrofit 数据类。

### 4.2 改动现有文件
- `PetRepository.kt` — 写操作成功后 `CloudSyncManager.enqueue(...)`。
- `MainActivity.kt` / `App` — 初始化 Retrofit + 匿名登录 + 首次拉取 + 网络恢复补传。
- 底部导航 — 新增「排行」Tab：`ui/leaderboard/LeaderboardFragment.kt` + `LeaderboardViewModel.kt`。

### 4.3 依赖（替换原 LeanCloud 段）
```kotlin
// app/build.gradle.kts
dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
```
> `dependencyResolutionManagement` 已含 `google()` / `mavenCentral()`，无需新增源。

### 4.4 AndroidManifest 明文流量
本地后端是 HTTP，Android 9+ 默认禁明文。在 `app/src/main/AndroidManifest.xml` 的 `<application>` 加：
```xml
android:usesCleartextTraffic="true"
```
（仅调试可接受；正式公网部署请改用 HTTPS + 网络安全配置。）

### 4.5 Room 迁移（version 3 → 4）
```
sync_meta (key TEXT PK, value TEXT)   -- lastSyncAt / pendingCount
sync_queue (id PK, op_type, payload, created_at)
```
`AppDatabase.version = 4` + `MIGRATION_3_4`；原有迁移不动。

---

## 5. 同步策略（离线优先）
同前：本地先落 Room → `enqueue` 进 `sync_queue` → 联网即 `PUT` 上报并清队列；断网留队列，启动/网络恢复补传。拉取以 `updated_at` 较新者覆盖。

---

## 6. 排行榜 UI
`GET /api/leaderboard?limit=50` 取 Top 50；列表（名次/昵称/积分），高亮"我的排名"；顶部显示我的总积分与名次；可改 `displayName`（走 `PUT`）。零数据占位："成为第一个上榜的专注者"。

---

## 7. 实施阶段
| 阶段 | 内容 | 阻塞点 |
| --- | --- | --- |
| **P0** | 后端服务器（见 `server/leaderboard_server.py`）+ 本地 curl 验证 | 无，已可跑 |
| **P1** | Android：`CloudConfig/ApiClient/AuthManager` + 匿名登录 + `users` 读写 | 依赖 P0 |
| **P2** | `CloudSyncManager` + `sync_queue` + 迁移 + `PetRepository` 接入 | 依赖 P1 |
| **P3** | 排行榜 `LeaderboardFragment/ViewModel/Repository` + 底部 Tab | 依赖 P1 |
| **P4** | 联调：模拟器 `10.0.2.2` / 真机局域网 IP 连后端；test 分支加清云端调试 | — |

---

## 8. 运行与连接（关键）

**起后端**（开发机）：
```bash
cd server
python leaderboard_server.py 8080
# 输出 FocusPets backend on http://0.0.0.0:8080
```
数据库 `focuspets.db` 自动生成在同目录。

**Android 连接方式**：
- 模拟器：`http://10.0.2.2:8080/`（10.0.2.2 = 宿主回环）。
- 真机同 WiFi：用开发机局域网 IP（Windows `ipconfig` 看 IPv4，如 `http://192.168.1.50:8080/`）。
- 在 `local.properties` 配 `CLOUD_BASE_URL=...`，经 `BuildConfig.CLOUD_BASE_URL` 注入 `CloudConfig`。

---

## 9. 风险与注意
- **局域网限制**：当前仅同网络/同机可达，不是真正公网跨设备。要真跨设备需把后端部署到 VPS（阿里云轻量/腾讯云等）并改 `BASE_URL` 为公网地址 + HTTPS。
- **无鉴权**：`PUT` 任何人可改任意 uid（局域网调试无所谓）；若部署公网，必须加 token/签名校验。
- **数据库位置**：`focuspets.db` 在后端进程目录，备份/迁移直接拷文件。
- **匿名换设备丢进度**：匿名 uid 存本机 `SharedPreferences`，换手机需"绑定账号"才能找回；本期只留接口。
- **test 分支**：调试满配会产生百万积分，test 分支对榜单加本地开关/排除，不污染 main。
- **密钥/URL 安全**：`CLOUD_BASE_URL` 走 `local.properties`，勿提交 Git。

---

## 10. 以后升级到公网（可选）
拿到 VPS 后：把 `server/leaderboard_server.py` 部署上去（或用 Ktor/Node 重写），配域名 + HTTPS，Android 端只改 `CLOUD_BASE_URL`。`AuthManager/CloudSyncManager` 接口不变。
