# Remaining Chinese text

Branch: main. Scan covers Git-tracked UTF-8 text files only; untracked output/, tmp/, caches, binary assets, and Git history are excluded. Line numbers refer to the working tree after comment translation.

Translated 864 comment/docstring segments in 94 files. Verified that every character outside the parsed comment/docstring spans is identical to HEAD.

## Preserved Chinese in comments

Chinese path references are intentionally preserved. Surrounding explanations are English.

- `tools/cutout_cat_crowns.py:1`: FocusPets: cut out the three new cat crown images from 图片/初始-猫/皇冠 into transparent PNGs under assets/cat/. The background is beige #FEF8EA (254,248,234); reuse v2 edge decontamination and light feathering. Match CatWardrobe.assetPathFor with cat/<color>_dress_<crown-color>.png.
- `tools/cutout_dog.py:1`: FocusPets short-tail dog cutouts: read 图片/狗/短尾巴 and output transparent PNGs under assets/dog/. Use lightweight rembg u2netp, alpha decontamination, and gentle feathering for pale backgrounds. Name files tail_color[_red_tie][_black_suit|red_dress][_academic_cap]. Explicit color tokens allow future variants: short tails use black_suit, long tails red_dress, caps academic_cap. Deduplicate by filename, retaining the first image, including suit/cap folder duplicates. Process only 短尾巴; preserve existing long-tail artwork.
- `tools/cutout_dog.py:22`: # Keywords may occur in parent folder names, e.g. 红领带/黄.jpg.

## Other preserved Chinese

Documentation prose and displayed/runtime strings were outside the comment-only scope. This includes UI text, accessibility labels, pet catalog data, SQL string values, script matching keys, printed messages, and actual paths.

| File | Lines containing Chinese |
| --- | --- |
| `.gitignore` | 2, 3, 4, 5, 8, 16, 19, 20, 26, 32, 35, 44, 48, 55, 59, 60, 62, 65, 66, 69, 75 |
| `CLOUD_SYNC_PLAN.md` | 1, 3, 4, 5, 6, 10, 12, 16, 19, 21, 24, 25, 29, 30, 34, 36, 38, 39, 40, 41, 43, 44, 45, 49, 51, 53, 54, 55, 56, 57, 58, 59, 61, 65, 67, 68, 70, 71, 72, 73, 75, 76, 77, 78, 80, 89, 91, 92, 96, 98, 103, 107, 108, 112, 113, 117, 118, 120, 121, 122, 123, 124, 128, 130, 134, 136, 138, 139, 140, 141, 145, 146, 147, 148, 149, 150, 151, 155, 156 |
| `README.md` | 1, 3, 5, 11, 13, 15, 17, 18, 19, 20, 21, 25, 27, 31, 32, 33, 35, 36, 37, 42, 44, 46, 47, 48, 49, 50, 51, 53, 55, 57, 59, 60, 61, 63, 65, 67, 69, 71, 72, 73, 75, 77, 78, 79, 80, 81, 82, 84, 86, 88, 90, 91, 92, 94, 96, 97, 98, 99, 100, 101, 103, 105, 106, 107, 108, 109, 110, 111, 113, 115, 116, 117, 118, 119, 120, 121, 125, 127, 129, 131, 132, 133, 135, 139, 141, 143, 145, 146, 147, 148, 149, 150, 151, 152, 153, 155, 159, 162, 165, 168, 171, 175, 176, 177, 178, 179, 183, 185, 187, 188, 190, 192, 193, 194, 195, 197, 198, 200, 201, 205, 209, 210, 211, 212, 213, 217, 218, 219, 221, 223, 224, 225, 226, 228, 229, 231, 234, 237, 238, 239, 240, 241, 242, 247, 249, 250, 251, 252, 254, 258, 260, 262, 264, 265, 266, 267, 268, 269, 270, 271, 272, 273, 274, 275, 276, 278, 280, 282, 283, 284, 285, 286, 287, 288, 290, 294, 296, 298, 300, 302, 304, 305, 306, 307, 309, 311, 312, 314, 321, 323, 325, 327, 329, 330, 331, 333, 335, 337, 340, 343, 347, 349, 351, 352 |
| `app/src/main/AndroidManifest.xml` | 69 |
| `app/src/main/assets/card3d/card.html` | 6, 25, 27, 29, 30, 31, 43, 48, 50, 52, 172, 199, 201, 208, 211, 518, 519, 530, 548 |
| `app/src/main/java/com/example/focuspets/CrashActivity.kt` | 28, 31, 50, 54, 59 |
| `app/src/main/java/com/example/focuspets/MainActivity.kt` | 49, 52, 109 |
| `app/src/main/java/com/example/focuspets/badge/BadgeCelebration.kt` | 169, 172, 229, 230, 245, 246, 247, 252 |
| `app/src/main/java/com/example/focuspets/cloud/AuthManager.kt` | 31 |
| `app/src/main/java/com/example/focuspets/cloud/CloudSyncManager.kt` | 72 |
| `app/src/main/java/com/example/focuspets/db/AppDatabase.kt` | 135, 136, 137, 139, 140, 141, 143, 144, 145 |
| `app/src/main/java/com/example/focuspets/db/InitialDataProvider.kt` | 16, 17, 18, 19, 20, 21, 22, 23, 24 |
| `app/src/main/java/com/example/focuspets/db/PetRepository.kt` | 140, 141, 142, 143, 144, 145, 181, 185 |
| `app/src/main/java/com/example/focuspets/debug/DebugHelper.kt` | 112 |
| `app/src/main/java/com/example/focuspets/model/CatWardrobe.kt` | 49, 50, 51, 52, 53, 54, 59, 60 |
| `app/src/main/java/com/example/focuspets/model/DogWardrobe.kt` | 43, 44, 45, 46, 47, 48, 53, 54 |
| `app/src/main/java/com/example/focuspets/service/FocusService.kt` | 274, 276, 290, 291 |
| `app/src/main/java/com/example/focuspets/ui/collection/FragmentCollection.kt` | 56, 63, 82, 89, 107, 109, 110, 112, 113, 117, 119, 120, 122 |
| `app/src/main/java/com/example/focuspets/ui/collection/PetAdapter.kt` | 56, 57, 58 |
| `app/src/main/java/com/example/focuspets/ui/collection/PetCardIcon.kt` | 156, 157 |
| `app/src/main/java/com/example/focuspets/ui/focus/FocusCalendarView.kt` | 25 |
| `app/src/main/java/com/example/focuspets/ui/focus/FocusRecordsActivity.kt` | 180, 213, 223, 232, 242, 290, 312, 316, 323, 331, 333, 337, 345, 346, 373, 375, 386, 387, 394, 400, 406, 409, 433, 440, 449 |
| `app/src/main/java/com/example/focuspets/ui/focus/FragmentFocus.kt` | 75, 154, 237, 252, 258, 283, 286, 323, 324, 325, 331, 342, 352, 353, 354, 364, 366, 367, 368, 371, 379, 384, 397, 402, 447, 461, 462, 463, 469 |
| `app/src/main/java/com/example/focuspets/ui/focus/LegendAdapter.kt` | 46 |
| `app/src/main/java/com/example/focuspets/ui/focus/PieChartView.kt` | 68 |
| `app/src/main/java/com/example/focuspets/ui/focus/RecordAdapter.kt` | 40 |
| `app/src/main/java/com/example/focuspets/ui/home/HomeFragment.kt` | 118, 158, 159, 160, 162, 166, 398, 410, 576, 587, 601, 605, 611, 628, 646, 665, 666, 667, 670 |
| `app/src/main/java/com/example/focuspets/ui/leaderboard/LeaderboardAdapter.kt` | 42, 50, 51, 52 |
| `app/src/main/java/com/example/focuspets/ui/leaderboard/LeaderboardFragment.kt` | 41, 50, 51, 62, 64, 68 |
| `app/src/main/java/com/example/focuspets/ui/leaderboard/LeaderboardViewModel.kt` | 35, 45, 47 |
| `app/src/main/java/com/example/focuspets/ui/lock/LockActivity.kt` | 205, 219, 221, 223, 225, 227, 228, 270, 284, 285, 286, 287, 296, 297, 298, 299, 315, 325, 326, 327, 352, 357, 375, 381, 393 |
| `app/src/main/java/com/example/focuspets/ui/onboarding/OnboardingActivity.kt` | 139 |
| `app/src/main/java/com/example/focuspets/ui/settings/BgColorPickerDialog.kt` | 52, 53, 57, 65 |
| `app/src/main/java/com/example/focuspets/ui/settings/SettingsActivity.kt` | 55, 68, 94, 96, 105, 116, 118, 119, 122, 126, 136, 138, 139, 141, 142, 145, 156, 158, 159, 160, 161, 162, 163, 165, 166, 168, 272 |
| `app/src/main/java/com/example/focuspets/ui/stats/BadgeAdapter.kt` | 38 |
| `app/src/main/java/com/example/focuspets/ui/stats/FocusHeatmapView.kt` | 37 |
| `app/src/main/java/com/example/focuspets/ui/stats/FragmentStats.kt` | 64, 67, 81, 82, 138, 139, 146, 147 |
| `app/src/main/java/com/example/focuspets/ui/wardrobe/CatWardrobeBottomSheet.kt` | 40, 43, 99, 118, 155, 156, 157, 201, 211 |
| `app/src/main/java/com/example/focuspets/ui/wardrobe/DogWardrobeBottomSheet.kt` | 64, 110, 143, 156, 157, 158, 187, 188, 189, 228, 229, 230, 235, 236, 237 |
| `app/src/main/res/layout/activity_focus_records.xml` | 28, 37, 74, 98, 117, 151, 176, 200, 208, 216, 224, 233, 265, 274, 300, 309, 336, 353, 386, 405 |
| `app/src/main/res/layout/activity_lock.xml` | 35, 45, 57, 69, 76, 85, 100, 107, 114, 129, 146, 154, 164, 174, 183, 217, 227, 236 |
| `app/src/main/res/layout/activity_lock_whitelist.xml` | 22, 31, 41, 58 |
| `app/src/main/res/layout/activity_onboarding.xml` | 39, 49, 59, 77, 85, 90, 95, 100, 105, 127, 136, 170, 186 |
| `app/src/main/res/layout/activity_settings.xml` | 30, 38, 45, 72, 77, 85, 106, 111, 119, 126, 131, 145, 149, 157, 164, 185, 189, 200, 214, 218, 225, 235, 244, 253, 262, 280, 293, 314, 318, 326, 332, 346, 350, 357, 367, 376, 385, 392, 406, 410 |
| `app/src/main/res/layout/dialog_badge_poster.xml` | 27, 37 |
| `app/src/main/res/layout/dialog_bg_color.xml` | 13, 22, 40 |
| `app/src/main/res/layout/dialog_cat_wardrobe.xml` | 15, 24, 32, 47, 62, 78, 88 |
| `app/src/main/res/layout/dialog_dog_wardrobe.xml` | 15, 24, 32, 47, 62, 88 |
| `app/src/main/res/layout/fragment_collection.xml` | 15, 32, 42 |
| `app/src/main/res/layout/fragment_focus.xml` | 27, 37, 62, 71, 87, 108, 123, 130, 140, 157, 164, 171, 186, 203, 211, 221, 240, 249, 258 |
| `app/src/main/res/layout/fragment_home.xml` | 23, 32, 41, 53, 107, 129, 141, 162, 171, 179, 190, 201, 211 |
| `app/src/main/res/layout/fragment_leaderboard.xml` | 17, 26, 42, 52, 61, 78, 86 |
| `app/src/main/res/layout/fragment_stats.xml` | 17, 26, 46, 73, 82, 100, 109, 127, 136, 154, 163, 174, 183, 210, 225, 236, 246, 263, 283 |
| `app/src/main/res/layout/item_focus_record.xml` | 26, 45 |
| `app/src/main/res/layout/item_legend.xml` | 21, 29 |
| `app/src/main/res/layout/leaderboard_item.xml` | 51, 60 |
| `app/src/main/res/menu/bottom_nav.xml` | 7, 12, 17, 22, 27 |
| `app/src/main/res/values/strings.xml` | 2 |
| `developer/bug.md` | 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27 |
| `process_cat_assets.py` | 8, 13, 14, 15, 16, 17, 18, 22, 23, 29, 30, 31, 32, 33, 34, 36, 37, 38, 39, 40, 41, 43, 44, 45, 46, 47, 48, 49, 50, 51 |
| `server/leaderboard_server.py` | 22, 209 |
| `tools/cutout_cat_crowns.py` | 1, 9, 14, 15, 16, 41 |
| `tools/cutout_dog.py` | 1, 9, 15, 16, 22, 35, 37, 39, 41, 100 |
| `tools/cutout_v2.py` | 97, 109 |
| `tools/smoke_card.js` | 71, 73, 75, 78, 79, 81, 82, 89, 94, 99, 102, 103, 106 |

## Exact remaining lines

### .gitignore

```text
2: # FocusPets - Android 项目 .gitignore
3: # 原则：只提交「源码 + 构建脚本 + wrapper」，
4: #       一切机器本地生成物（构建产物、IDE 缓存、签名密钥）全部忽略。
5: # 注意：gitignore 不支持行内注释，# 必须写在行首。
8: # ---------- Gradle 构建产物 ----------
16: # SDK 路径（每台机器不同，绝不能提交）
19: # 注意：gradlew / gradlew.bat / gradle/wrapper/gradle-wrapper.jar
20: # 和 gradle-wrapper.properties 应该提交，clone 后才能直接构建。
26: # ---------- 签名与密钥（泄露 = App 可被冒名重签名） ----------
32: # ---------- 原生构建 / NDK ----------
35: # ---------- 打包产物 ----------
44: # ---------- 日志与崩溃报告 ----------
48: # ---------- 操作系统垃圾文件 ----------
55: # ---------- VS Code（若用 VS Code 打开过） ----------
59: # ---------- 本地工作数据（不属于工程，不上传） ----------
60: # WorkBuddy 会话/记忆
62: # 本地课程笔记（如需提交请删除下一行）
65: # ---------- 设计源图 / 抠图原始素材（体积大、非工程产物，不进仓库） ----------
66: # 图片/
69: # ---------- 生成预览 / 测试产物（不进仓库） ----------
75: # ---------- 本地数据库快照（二进制，无 diff 意义，不进仓库） ----------
```

### CLOUD_SYNC_PLAN.md

```text
1: # FocusPets 云端同步 / 排行榜 实施方案
3: > 状态：规划已确认（实施中）
4: > 后端演进：Firebase（国内网络不可达）→ LeanCloud 国内版（注册下线）→ **自建后端（本地/局域网）**
5: > 已确认方向：**自建后端** · **匿名一键登录** · **全量跨设备同步** · **全局总榜**
6: > 适用工程：Kotlin + MVVM + Room 单 Activity，包名 `com.example.focuspets`，minSdk 24
10: ## 0. 一句话目标
12: 给纯单机应用加「云端身份 + 可同步状态 + 排行榜视图」。因为第三方 BaaS（Firebase / LeanCloud）在当前网络都不可用，**改为自建一个跑在开发机/局域网的小后端**，Android 通过 HTTP 直连。架构接口（`AuthManager` / `CloudSyncManager`）保持抽象，将来换公网/VPS 或别的 BaaS 只改实现层。
16: ## 1. 总体架构
19: ┌────────── 设备端 (现有) ──────────┐         ┌──── 自建后端 (本地/局域网 HTTP) ────┐
21: │            → Room (本地真源)       │  HTTP    │  + sqlite3 (focuspets.db)          │
24: │ [新增] CloudSyncManager            │  JSON    │  /api/users/{uid}                 │
25: │  (Retrofit 客户端 + 离线队列)       │         │  /api/leaderboard?limit=50        │
29: - 后端语言：本方案用 **Python 标准库 + sqlite3（零依赖，python 直接跑）**。若你想换成 Kotlin Ktor / Node，说一声，API 契约不变，我移植。
30: - 设计原则不变：**Room 仍是本地真源**，后端是镜像 + 跨设备副本。
34: ## 2. 后端 API 契约（JSON over HTTP）
36: | 方法 | 路径 | 说明 | 请求体 / 参数 | 响应 |
38: | POST | `/api/auth/anon` | 匿名注册，返回 uid | — | `{uid, displayName}` |
39: | GET | `/api/users/{uid}` | 拉取某用户云端状态 | — | `{uid, displayName, totalPoints, collection[], wardrobe[], mood, updatedAt}` |
40: | PUT | `/api/users/{uid}` | 上报/合并状态 | 部分字段（`displayName`/`totalPoints`/`collection`/`wardrobe`/`mood`） | 合并后的完整用户对象 |
41: | GET | `/api/leaderboard?limit=50&uid=` | 取榜单 Top N，可带 `uid` 算自己名次 | query | `{list:[{rank,uid,displayName,totalPoints}], me:{rank,totalPoints}\|null}` |
43: - 状态码：成功 200，无用户 404，其他 400/500。
44: - 已开启 CORS（`Access-Control-Allow-Origin: *`），方便模拟器/网页调试。
45: - `collection` / `wardrobe` 以 JSON 数组字符串存库。
49: ## 3. 数据模型映射（Room → 后端 sqlite）
51: | 本地（Room） | 后端 `users` 表字段 | 说明 |
53: | `focus_records` | `total_points` (聚合) | 专注结算时客户端算好增量上报 |
54: | `user_collection` | `collection` (JSON 数组) | 已解锁宠物 id |
55: | `wardrobe_purchases` | `wardrobe` (JSON 数组) | 已购妆扮 id |
56: | 心情 `SharedPreferences` | `mood` (String) | NORMAL/SICK/GLOW |
57: | （派生）可用积分 | `total_points` | `Σ专注分钟 − Σ解锁cost − Σ妆扮cost` |
58: | — | `display_name` | 默认 `专注者{4位}` |
59: | — | `updated_at` (ms) | 冲突解决 last-write-wins |
61: 排行榜 = `SELECT uid, display_name, total_points FROM users ORDER BY total_points DESC, updated_at ASC LIMIT ?`。
65: ## 4. Android 端改动清单
67: ### 4.1 新增 `app/src/main/java/com/example/focuspets/cloud/`
68: - `CloudConfig.kt` — `BASE_URL`（来自 `local.properties` / `BuildConfig`，如 `http://10.0.2.2:8080/` 模拟器，或局域网 IP）。
70: - `AuthManager.kt` — 调 `POST /api/auth/anon` 拿 uid；本地用 `SharedPreferences` 缓存 uid（匿名即绑设备）。
71: - `CloudSyncManager.kt` — ① `PUT /api/users/{uid}` 增量推送；② 启动/联网 `GET` 拉取合并；③ 离线写队列（Room `sync_queue`）；④ last-write-wins。
72: - `LeaderboardRepository.kt` — 封装 `GET /api/leaderboard`，返回带"我的排名"的列表。
73: - `model/CloudUser.kt` / `model/LeaderboardEntry.kt` — Retrofit 数据类。
75: ### 4.2 改动现有文件
76: - `PetRepository.kt` — 写操作成功后 `CloudSyncManager.enqueue(...)`。
77: - `MainActivity.kt` / `App` — 初始化 Retrofit + 匿名登录 + 首次拉取 + 网络恢复补传。
78: - 底部导航 — 新增「排行」Tab：`ui/leaderboard/LeaderboardFragment.kt` + `LeaderboardViewModel.kt`。
80: ### 4.3 依赖（替换原 LeanCloud 段）
89: > `dependencyResolutionManagement` 已含 `google()` / `mavenCentral()`，无需新增源。
91: ### 4.4 AndroidManifest 明文流量
92: 本地后端是 HTTP，Android 9+ 默认禁明文。在 `app/src/main/AndroidManifest.xml` 的 `<application>` 加：
96: （仅调试可接受；正式公网部署请改用 HTTPS + 网络安全配置。）
98: ### 4.5 Room 迁移（version 3 → 4）
103: `AppDatabase.version = 4` + `MIGRATION_3_4`；原有迁移不动。
107: ## 5. 同步策略（离线优先）
108: 同前：本地先落 Room → `enqueue` 进 `sync_queue` → 联网即 `PUT` 上报并清队列；断网留队列，启动/网络恢复补传。拉取以 `updated_at` 较新者覆盖。
112: ## 6. 排行榜 UI
113: `GET /api/leaderboard?limit=50` 取 Top 50；列表（名次/昵称/积分），高亮"我的排名"；顶部显示我的总积分与名次；可改 `displayName`（走 `PUT`）。零数据占位："成为第一个上榜的专注者"。
117: ## 7. 实施阶段
118: | 阶段 | 内容 | 阻塞点 |
120: | **P0** | 后端服务器（见 `server/leaderboard_server.py`）+ 本地 curl 验证 | 无，已可跑 |
121: | **P1** | Android：`CloudConfig/ApiClient/AuthManager` + 匿名登录 + `users` 读写 | 依赖 P0 |
122: | **P2** | `CloudSyncManager` + `sync_queue` + 迁移 + `PetRepository` 接入 | 依赖 P1 |
123: | **P3** | 排行榜 `LeaderboardFragment/ViewModel/Repository` + 底部 Tab | 依赖 P1 |
124: | **P4** | 联调：模拟器 `10.0.2.2` / 真机局域网 IP 连后端；test 分支加清云端调试 | — |
128: ## 8. 运行与连接（关键）
130: **起后端**（开发机）：
134: # 输出 FocusPets backend on http://0.0.0.0:8080
136: 数据库 `focuspets.db` 自动生成在同目录。
138: **Android 连接方式**：
139: - 模拟器：`http://10.0.2.2:8080/`（10.0.2.2 = 宿主回环）。
140: - 真机同 WiFi：用开发机局域网 IP（Windows `ipconfig` 看 IPv4，如 `http://192.168.1.50:8080/`）。
141: - 在 `local.properties` 配 `CLOUD_BASE_URL=...`，经 `BuildConfig.CLOUD_BASE_URL` 注入 `CloudConfig`。
145: ## 9. 风险与注意
146: - **局域网限制**：当前仅同网络/同机可达，不是真正公网跨设备。要真跨设备需把后端部署到 VPS（阿里云轻量/腾讯云等）并改 `BASE_URL` 为公网地址 + HTTPS。
147: - **无鉴权**：`PUT` 任何人可改任意 uid（局域网调试无所谓）；若部署公网，必须加 token/签名校验。
148: - **数据库位置**：`focuspets.db` 在后端进程目录，备份/迁移直接拷文件。
149: - **匿名换设备丢进度**：匿名 uid 存本机 `SharedPreferences`，换手机需"绑定账号"才能找回；本期只留接口。
150: - **test 分支**：调试满配会产生百万积分，test 分支对榜单加本地开关/排除，不污染 main。
151: - **密钥/URL 安全**：`CLOUD_BASE_URL` 走 `local.properties`，勿提交 Git。
155: ## 10. 以后升级到公网（可选）
156: 拿到 VPS 后：把 `server/leaderboard_server.py` 部署上去（或用 Ktor/Node 重写），配域名 + HTTPS，Android 端只改 `CLOUD_BASE_URL`。`AuthManager/CloudSyncManager` 接口不变。
```

### README.md

```text
1: # FocusPets · 专注养宠 + 图鉴收集
3: 一款用「番茄钟专注」换取「宠物图鉴收集」的 Android 应用。每专注一分钟得 1 积分，用积分解锁萌宠，宠物还会根据你的专注表现切换心情。
5: > 包名：`com.example.focuspets` ｜ 语言：Kotlin ｜ 架构：单 Activity + 多 Fragment + MVVM + Room
11: ## 一、技术栈
13: | 类别   | 方案                                                         |
15: | 开发语言 | Kotlin 1.9.24                                              |
17: | 架构   | MVVM（ViewModel + LiveData + MediatorLiveData）              |
18: | 本地存储 | Room（SQLite 封装，KSP 注解处理）                                   |
19: | 计时核心 | Foreground Service + CountDownTimer + 包内定向广播               |
20: | 动画   | 属性动画（ObjectAnimator / AnimatorSet）+ 自定义 View + SoundPool 音效 |
21: | 构建   | Gradle 8.7 + AGP 8.5.2，compileSdk / targetSdk 34，minSdk 24 |
25: ## 二、功能总览
27: 应用分为三个底部 Tab：**首页（宠物陪伴）**、**专注（计时）**、**图鉴（收集）**。
31: │   首页 Home  │   │  专注 Focus │   │  图鉴 Pokédex│
32: │  宠物互动    │   │  番茄钟计时  │   │  9 只宠物网格 │
33: │  心情三态    │   │  积分结算    │   │  解锁/详情   │
35: ▲                │ 专注完成 +1 分
36: │ 解锁后可选中     ▼
37: └──────── 图鉴解锁消耗积分 ────────┘
42: ## 三、宠物功能详解
44: ### 1. 首页 · 宠物陪伴（`ui/home`）
46: - **双形态渲染**：1 号「小猫咪」与 2 号「小狗」均用 `assets/cat/*.jpg` / `assets/dog/*.png` 立绘（`ImageView`），其余 7 只仍用 emoji（`TextView`），按当前选中宠物自动切换。
47: - **呼吸动画**：宠物以 1.0 ↔ 1.06 缩放无限往返（`AnimatorSet` + `REVERSE`），作用于当前可见的宠物视图。
48: - **切换宠物**：点击宠物 / 左右滑动，在「已解锁列表」中循环切换（`selectNext` / `selectPrev`）。
49: - **跳跃反馈**：每次切换触发 `OvershootInterpolator` 跳跃动画。
50: - **心情三态渲染**（见第四节）：根据 `PetMood` 显示不同文案、颜色、光环与生病贴纸。
51: - **进度条**：展示「距下一只可解锁宠物还差多少积分」，满配后显示「图鉴已全部收集完成」。
53: ### 1.1 小猫咪活体行为（1 号宠物专属）
55: 猫咪被选中且页面在前台时，每 4~9 秒随机触发一种行为，让它显得活灵活现：
57: | 行为     | 表现                                          |
59: | 🐾 喵喵叫 | `SoundPool` 播放 `res/raw/meow.wav`（音量 0.25，很轻）+ 冒出「喵~」气泡 |
60: | 🐾 侧壁跳跃 | 在屏幕左右侧壁之间连跳两次再回到中间（`TRANSLATION_X/Y` 抛物线）    |
61: | 🐾 凑近嗅探 | 放大到 1.22 倍并左右轻晃，结束后恢复并重启呼吸动画                |
63: > 行为调度在 `onPause` 停止、`onResume` 恢复，页面不可见时不会空跑。嗅探会临时取消呼吸动画，避免两者同时改 `SCALE` 打架。
65: ### 1.2 小猫咪妆扮系统（积购买 · 可换装）
67: 入口：首页「🐱 装扮小猫」按钮（仅 1 号小猫咪被选中时显示），打开 `CatWardrobeBottomSheet` 底部面板。
69: | 类别   | 条目                    | 单价         | 说明                   |
71: | 皮肤颜色 | 灰（默认）/ 蓝 / 粉 / 黄 / 白 / 黑 | 50 积分（灰色免费） | 灰色默认拥有，其余需购买        |
72: | 衣服配件 | 红裙                    | 120 积分      | **必须先购买裙子**才能买王冠    |
73: | 王冠   | 蓝色王冠 / 粉色王冠           | 180 积分      | 需先拥有裙子；买后自动穿上裙子     |
75: 规则与实现要点：
77: - **买断制、随时换**：可购买多个，已拥有的条目点击即穿上/脱下，不重复扣费。
78: - **皇冠依附裙子**：皇冠立绘都是「颜色-红裙-王冠」，所以脱下裙子会连带摘掉王冠；换到没有该王冠立绘的颜色时也会自动摘掉。
79: - **素材完整性**：豆包生成的立绘并非每种颜色都配齐全套 —— **白猫没有王冠立绘、灰猫只有蓝色王冠**。面板按 `CatWardrobe.CROWN_OPTIONS_BY_COLOR` 只展示有立绘的组合，避免加载到不存在的图。
80: - **立绘路径解析**：`cat/{颜色}.jpg` → `cat/{颜色}_dress.jpg` → `cat/{颜色}_dress_{王冠}.jpg`，由 `CatWardrobe.assetPathFor()` 统一生成。
81: - **水印处理**：原始素材左上角与右下角有「豆包AI生成」水印，已用 `process_cat_assets.py` 采样背景色后将四角区域涂平（输出在 `app/src/main/assets/cat/`，共 21 张）。
82: - **积分扣减**：购买写入 `wardrobe_purchases` 表，可用积分公式同步纳入该项消费（见第六节）。
84: ### 1.3 小狗妆扮系统（积购买 · 可换装）
86: 入口：首页「🐶 装扮小狗」按钮（仅 2 号小狗被选中时显示），打开 `DogWardrobeBottomSheet` 底部面板。
88: | 类别   | 条目                     | 单价              | 说明                                          |
90: | 尾巴   | 长尾巴 / 短尾巴             | 免费自选            | 免费切换，决定本体与配饰树（长尾=礼服、短尾=西服）                  |
91: | 皮肤颜色 | 灰（默认）/ 白 / 粉 / 黄 / 黑 / 彩色 | 50 积分（灰色免费）      | 灰色默认拥有，其余需购买；把猫的"蓝"换成了"彩色"                 |
92: | 配饰   | 红领带 → 西服/礼服 → 学士帽       | 120 / 150 / 180 积分 | 必须按"领带 → 西服或礼服 → 学士帽"顺序购买                 |
94: 规则与实现要点：
96: - **买断制、随时换**：可购买多个，已拥有的条目点击即穿上/脱下，不重复扣费。
97: - **配饰依赖链**：领带最优先；西服/礼服需先有领带；学士帽需先有西服/礼服。脱下前置会自动连带摘掉后续配饰（如摘领带会连脱西服与学士帽）。
98: - **素材完整性**：不同颜色并非都配齐所有服饰（如彩色狗无裙/帽立绘、粉色短尾无西服），面板用 `DogWardrobe.outfitChangesImage()` / `capChangesImage()` 探测，没有立绘的按钮自动禁用并提示，避免"买了却看不到"。
99: - **立绘路径解析**：`dog/{尾巴}_{颜色}[_tie][_{礼服|西服}][_cap].png`，由 `DogWardrobe.assetPathFor()` 统一生成；若某个精确组合不存在（素材缺口），会逐级回退（摘帽 → 脱衣 → 摘领带）到能加载的最接近立绘，最后兜底为同尾巴灰狗。
100: - **活体行为**：小狗被选中且页面在前台时，随机触发「汪汪叫」（播放 `res/raw/bark.wav` + 冒出「汪~」气泡）或侧壁跳跃，与小猫行为调度共用同一套机制。
101: - **积分扣减**：购买写入 `wardrobe_purchases` 表（itemId 形如 `dog_color_white` / `dog_tie` / `dog_outfit` / `dog_cap`），可用积分公式同步纳入该项消费。
103: ### 2. 专注 · 番茄钟（`ui/focus` + `service`）
105: - **预设时长**：15 / 25 / 45 分钟（`MaterialButtonToggleGroup` 单选）。
106: - **自定义时长**：输入框可填 **1~180** 任意分钟数，点「应用」生效，同时取消预设按钮的选中态；计时中禁用输入。
107: - **进度条样式**：自定义 View `FocusProgressBar` —— 已过去时间为**橘色**、未走完为**白色**，交界处一条**黑色竖线**分割，且两端不带圆角（Material 原生 `LinearProgressIndicator` 的圆角会让两截变成各自独立的圆弧）。
108: - **前台服务计时**：`FocusService` 常驻通知栏，每秒广播剩余时间给 UI，切到后台也不会被系统杀掉中断计时。
109: - **后台判定（防作弊）**：App 切到后台后宽限 **5 秒**，超过则判定「专注失败」——不结算积分，宠物进入生病状态。
110: - **放弃**：计时中按钮变为「放弃专注」，确认后不结算、不惩罚。
111: - **积分结算**：倒计时自然完成 → 写入一条 `focus_records`（分钟数即积分数）→ 广播 `FINISHED` → 检查是否有新宠物积分达标并弹庆祝窗。
113: ### 3. 图鉴 · 收集（`ui/collection`）
115: - **3 列网格**：`GridLayoutManager(3)`，9 张宠物卡片。
116: - **稀有度徽章 + 彩色边框**：普通（蓝）/ 稀有（紫）/ 传说（金），已解锁卡片高亮，未解锁显示 `❓` + 所需积分。
117: - **点击分发**：
118: - 已解锁 → 底部弹窗看详情（emoji / 名称 / 稀有度 / 故事文案）。
119: - 积分够 → 弹确认框「花费 N 积分解锁？」。
120: - 积分不够 → 提示「还差 N 积分」。
121: - **解锁逻辑**：插入一条 `user_collection` 记录即可；可用积分是派生值（总专注分钟 − 已解锁 cost 之和），无需额外扣分字段，天然防超扣。
125: ## 四、宠物心情状态机（`model/PetCareState`）
127: 心情存于 `SharedPreferences`（单机小数据，无需进 Room），三态：
129: | 状态          | 触发条件                | 页面表现               |
131: | `NORMAL` 普通 | 默认 / 专注失败后重新 normal | 「😊 陪伴中」，蓝色标签      |
132: | `SICK` 生病   | 专注失败（切后台超 5 秒）      | 宠物半透明 + 🤒 贴纸，红色标签 |
133: | `GLOW` 发光   | 连续专注成功 ≥ 3 次        | 金色光环呼吸脉冲，金色标签      |
135: > 连续成功计数在「失败」时清零；再次失败后重新从 0 累计。
139: ## 五、宠物图鉴一览（9 只）
141: 积分规则：**专注 1 分钟 = 1 积分**；解锁即消耗对应积分（已解锁宠物的 cost 之和从总分中扣除）。
143: | # | 名称  | emoji | 稀有度 | 解锁积分 | 简介             |
145: | 1 | 小猫咪 | 🐱    | 普通  | 0    | 可换装的小猫咪，默认灰色（首只赠送） |
146: | 2 | 小狗   | 🐶    | 普通  | 50   | 忠诚的小伙伴（可换装：尾巴/颜色/领带/西服或礼服/学士帽） |
147: | 3 | 龙猫   | 🐹    | 普通  | 100  | 软糯的小毛球（暂未实装立绘）        |
148: | 4 | 星光兽 | ⭐     | 稀有  | 300  | 只在深夜出现         |
149: | 5 | 雷电犬 | ⚡     | 稀有  | 500  | 行动迅捷如闪电        |
150: | 6 | 冰晶狐 | ❄️    | 稀有  | 700  | 高傲的冰雪贵族        |
151: | 7 | 暗影龙 | 🌑    | 传说  | 1200 | 拥有毁灭力量         |
152: | 8 | 神圣鹿 | 🦌    | 传说  | 1800 | 森林的守护神         |
153: | 9 | 创世神 | 🌌    | 传说  | 2500 | 集齐所有图鉴后的终极奖励   |
155: 稀有度分布：**普通 ×3、稀有 ×3、传说 ×3**。
159: ## 六、数据库结构（`db`）
162: pets             宠物图鉴（9 条预置，只读）
165: user_collection  用户的收藏/解锁记录（解锁 = 插入一行）
168: focus_records    专注记录（积分来源）
171: wardrobe_purchases  小猫咪妆扮购买记录（v3 新增）
175: - 派生积分：`SUM(focus_records.duration_minutes) − SUM(已解锁 pets.unlock_cost) − SUM(wardrobe_purchases.cost)`
176: - 数据库版本 `version = 3`：
177: - `MIGRATION_1_2` —— 原地 UPDATE 宠物数据，保留用户专注记录。
178: - `MIGRATION_2_3` —— 新建 `wardrobe_purchases` 表（猫咪妆扮消费）。
179: - 妆扮消费进 Room 而非 SharedPreferences，是为了让「可用积分」这个 LiveData 在买完皮肤后能自动刷新。
183: ## 七、main / test 分支分工
185: | 分支     | 定位        | 内容                                    |
187: | `main` | 正式版本      | 正常解锁流程，**不含任何调试代码**（本文即 main 的状态）      |
188: | `test` | 调试版本      | 在 main 基础上叠加满配调试工具 + 大额测试积分           |
190: `test` 分支额外内置的调试工具（`debug/DebugHelper.kt`），**仅用于开发调试，不进 main**：
192: - **自动满配**：首次启动一次性解锁全部 9 只宠物（SharedPreferences 守卫，仅一次）。
193: - **图鉴页按钮「🛠 一键满配 / 重置进度」**：在满配与「仅首只」之间一键切换。
194: - **首页按钮「🎭 调试心情」**：循环切换 `NORMAL → SICK → GLOW`，验证三种心情渲染。
195: - **大额测试积分**：直接写入 `focus_records`，让可用积分达到百万级，方便测试妆扮商城。
197: > ⚠️ **切分支前务必先 commit**。未提交的改动存在工作区，会跟着 `git checkout` 一起漂移到另一个分支 ——
198: > 这正是「明明在 main 却也是满配」的原因。
200: > ⚠️ 应用数据按 `applicationId` 持久化：手机里已解锁的宠物不会因为换分支/改代码而消失。
201: > 从满配版切回 main 后，需要**卸载 App 或清除应用数据**才能看到正常的「仅 1 只」初始状态。
205: ## 八、项目结构
209: ├── MainActivity.kt              # 入口 + 底部导航 + test 分支自动满配
210: ├── db/                          # Room：实体 / DAO / 数据库 / 预置数据
211: │   ├── AppDatabase.kt           # 版本、迁移、Seed 回调
212: │   ├── InitialDataProvider.kt   # 9 只预置宠物 + 首只赠送
213: │   ├── PetRepository.kt         # 解锁/积分/达标查询
217: │   ├── PetCareState.kt          # 心情状态机（SharedPreferences）
218: │   ├── CatWardrobe.kt           # 猫咪妆扮：价格/目录/装备状态/立绘路径
219: │   └── DogWardrobe.kt           # 小狗妆扮：尾巴/颜色/配饰链 + 立绘路径（按实际素材回退）
221: │   └── FocusService.kt          # 前台计时服务
223: │   ├── home/                    # 首页宠物互动（含猫咪立绘 + 行为动画 + 音效）
224: │   ├── focus/                   # 专注计时页
225: │   │   └── FocusProgressBar.kt  # 自定义进度条：橘色已走 + 白色未走 + 黑色竖线
226: │   ├── collection/              # 图鉴网格 + 详情弹窗
228: │       ├── CatWardrobeBottomSheet.kt  # 小猫妆扮面板（购买 / 穿戴）
229: │       └── DogWardrobeBottomSheet.kt  # 小狗妆扮面板（尾巴/颜色/配饰链）
231: └── DebugHelper.kt           # test 分支调试工具
234: 配套资源：
237: app/src/main/assets/cat/    # 21 张猫咪立绘（已去水印），命名：{颜色}[_dress][_{王冠}].jpg
238: app/src/main/assets/dog/    # 42 张小狗立绘（透明 PNG，cutout_dog.py 抠图），命名：{尾巴}_{颜色}[_tie][_{礼服|西服}][_cap].png
239: app/src/main/res/raw/       # meow.wav：合成的小声喵叫；bark.wav：合成的小声狗叫
240: process_cat_assets.py       # 一次性脚本：去水印 + 重命名 + 生成 meow.wav
241: tools/cutout_dog.py         # 小狗抠图：从「图片/狗」生成 assets/dog/ 透明 PNG
242: tools/gen_bark.py           # 合成 bark.wav 的脚本
247: ## 九、构建与运行
249: 1. 用 Android Studio 打开本工程，连接已开启「开发者选项 → USB 调试」的手机。
250: 2. 首次会下载依赖 + 构建 APK（耐心等几分钟，依赖已配国内镜像）。
251: 3. 点 **Run（▶）** 安装并启动；后续改动点 Run（或 ⚡ Apply Changes 热更新）即可在手机看到新版。
252: 4. 数据库结构变更时，需提升 `AppDatabase.version` 并编写对应 `Migration`，否则旧库会崩溃。
254: 详见团队仓库提交记录与对话历史中的环境排障笔记。
258: ## 十、无障碍支持（深色模式 / TalkBack）
260: 深色模式与屏幕阅读器均采用「跟随系统 + 语义补全」的思路实现，不引入任何新的运行时依赖。
262: ### 1. 深色模式
264: - 主题本身就是 `Theme.Material3.DayNight.NoActionBar`，默认**自动跟随系统深色模式开关**。
265: - **支持手动切换（脱离系统跟随）**：设置页「外观 → 深色模式」提供三选一分段开关——`跟随系统` / `浅色` / `深色`。
266: - 选择会持久化到 `SharedPreferences`（键 `theme_mode`），并在 `FocusPetsApp.onCreate()` 首屏绘制前通过 `AppCompatDelegate.setDefaultNightMode()` 应用，避免闪屏。
267: - 切换即生效：设置页本身会随主题重建，全 App 立即统一深浅。
268: - 深色配色集中放在 `res/values-night/`：
269: - 深色配色集中放在 `res/values-night/`：
270: - `values-night/colors.xml` —— `page_background` / `text_primary` / `text_hint` / `cat_accent` / `rarity_*` 的深色取值；
271: - `values-night/themes.xml` —— 窗口底色指向深色资源；
272: - `drawable-night/bg_card.xml`、`drawable-night/bg_badge.xml` —— 卡片与徽章的深色底。
273: - **兼容用户自定义背景色**：色板原本是浅色柔和色（番茄 ToDo 风格），直接套进深色模式会破坏文字对比度。
274: `Backgrounds` 在夜间模式下调用 `util/ThemeExt.kt` 的 `toNightSurface()`，把任意背景色**压暗成深色表层并保留色相**，
275: 因此「换背景」选的蜜桃粉 / 天空蓝，在深色模式下依然是深色底 + 浅色字，不会刺眼。
276: - 代码里原本写死的浅色（图鉴卡片白底、柱状图标签灰字、详情文案灰字等）已改为引用颜色资源，随深浅色自动切换。
278: ### 2. TalkBack（屏幕阅读器）
280: | 位置 | 无障碍处理 |
282: | 首页宠物舞台 | 整个舞台作为一个可聚焦节点，朗读「宠物舞台：点击或左右滑动切换宠物」；内部立绘 / emoji / 光晕 / 生病贴纸 / 喵叫气泡标记为 `importantForAccessibility="no"`，避免重复播报 |
283: | 专注 / 锁机计时 | 剩余时间与进度条分别带 `contentDescription`「剩余专注时间」「专注进度」（**不加 live region**，避免每秒播报刷屏） |
284: | 图鉴卡片 | 动态生成描述：`小猫咪，普通，已解锁` / `冰晶狐，稀有，未解锁，需要 700 积分解锁` |
285: | 排行榜条目 | `第 3 名，专注者1234，200 分，这是你` |
286: | 宠物详情 | 大图 emoji 的 `contentDescription` 设为宠物名 |
287: | 背景色色板 | 每个色块带中文色名（暖米色 / 蜜桃粉 / …），可朗读、可选中 |
288: | 按钮 / 输入框 | 本身有文字或 `hint`，沿用系统默认朗读 |
290: > 新增工具方法集中在 `app/src/main/java/com/example/focuspets/util/ThemeExt.kt`：`Context.isNightMode()`、`Int.toNightSurface()`。
294: ## 十一、如何运行排行榜后端（本地后端 + 真机转发）
296: 排行榜与云端同步依赖一个极简本地后端 `server/leaderboard_server.py`：**零第三方依赖**（仅 Python 标准库 + `sqlite3`），负责匿名账号、状态镜像与排行榜。数据落地在 `server/focuspets.db`。
298: > 后端不是 App 运行的必需项：未启动时，排行榜页会自动回退到上一次成功拉取的缓存，App 不会崩溃。
300: ### 1. 一键启动（推荐，Windows）
302: 直接双击 `developer\start_backend.bat`（或在 Git Bash / CMD 中运行）。它会自动完成：
304: 1. 检查并杀掉占用 **8090** 端口的旧进程；
305: 2. **自动 `adb reverse tcp:8090 tcp:8090`**（需手机已通过 USB 调试连上电脑；连不上则跳过，不影响后端启动）；
306: 3. 在多个 Python 里挑一个能正常 `import` 标准库的（优先用 WorkBuddy 自带的托管 Python，规避系统 Python 标准库丢失的情况）；
307: 4. 运行 `python leaderboard_server.py 8090`。
309: 窗口保持打开即代表后端在跑；**关闭窗口即停止**。启动后控制台会打印：
311: - 模拟器访问：`http://10.0.2.2:8090/`
312: - 真机（USB）访问：`http://127.0.0.1:8090/`
314: ### 2. 手动启动
321: ### 3. 让 App 连上后端（关键）
323: App 的后端地址在**编译期**写死进 `BuildConfig.CLOUD_BASE_URL`，来源是项目根目录 `local.properties` 的 `CLOUD_BASE_URL`。因此改完地址必须**重新构建**才会生效（仅 Apply Changes 热更新不行）。
325: 按运行环境改 `local.properties`：
327: | 运行环境 | CLOUD_BASE_URL 取值 | 说明 |
329: | 安卓模拟器 | `http://10.0.2.2:8090/` | 10.0.2.2 是模拟器回环到宿主机的地址 |
330: | **真机 USB 调试（adb reverse）** | `http://127.0.0.1:8090/` | 配合 bat 的 `adb reverse`，把手机本机 8090 转发到电脑 8090，最稳、不受 WiFi 网段隔离影响 |
331: | 真机同一 WiFi | `http://<开发机局域网IP>:8090/` | 如 `http://192.168.1.23:8090/`，需手机与电脑同网段且防火墙放行 |
333: > 当前 `local.properties` 默认就是 `http://127.0.0.1:8090/`（对应「真机 + adb reverse」链路）。若切回模拟器，把它改成 `http://10.0.2.2:8090/` 并重新 build 即可。bat 与 `local.properties` 已对齐在 8090；若改端口，bat 的 `PORT` 与 `local.properties` 的地址要一起改。
335: 改完后重新构建安装：`./gradlew installDebug`（或 Android Studio 点 Run）。
337: ### 4. 验证后端在跑
340: # 排行榜（仅有过匿名注册的用户才会出现在榜上）
343: # 匿名注册一个新账号（App 首次启动会自动调用，这里仅供手动验证）
347: 能返回 JSON 即代表链路通。打开 App → 统计页 →「🏆 查看全球排行榜」即可看到榜单；若榜单为空，先在 App 内专注完成一次（产生积分并同步）即可上榜。
349: ### 5. 端口与数据库
351: - 默认端口 `8090`（bat 写死；手动启动时 `leaderboard_server.py` 的参数可改，但必须和 `local.properties` 里的地址一致）。
352: - 数据文件：`server/focuspets.db`（SQLite）。要清空榜单 / 账号，直接删这个文件，下次启动会自动重建空库。
```

### app/src/main/AndroidManifest.xml

```text
69: android:label="锁机白名单应用"
```

### app/src/main/assets/card3d/card.html

```text
6: <title>宠物卡牌</title>
25: <button id="close" title="关闭">✕</button>
27: <div id="hint">拖动旋转 · 正面轻点放大 · 转到背面轻点看简介</div>
29: <button data-r="COMMON">普通</button>
30: <button data-r="RARE">稀有</button>
31: <button data-r="LEGENDARY">传说</button>
43: if (h) { h.style.color = '#ff9a9a'; h.textContent = 'JS 错误: ' + m + ' (行 ' + l + ')'; }
48: COMMON:    { bg: ['#3a5f9e', '#24406e', '#12233f'], back: '#101d33', accent: '#a9c8ff', label: '普通',
50: RARE:      { bg: ['#6a3fb0', '#432a75', '#221040'], back: '#1d1036', accent: '#d9b3ff', label: '稀有',
52: LEGENDARY: { bg: ['#f6d271', '#e0a233', '#9c6a12'], back: '#2a1c05', accent: '#fff4c2', label: '传说',
172: x.fillText('未解锁', CW / 2, CH / 2 + 90);
199: x.fillText('宠 物 图 鉴', CW / 2, 604);
201: x.fillText('轻点卡片查看简介', CW / 2, 812);
208: var desc = (data.desc && String(data.desc).length) ? data.desc : '专注的好伙伴';
211: x.fillText('再点一下收起', CW / 2, 812);
518: id: 1, name: '小猫咪', rarity: rarity, locked: false, unlockCost: 0,
519: desc: '可换装的小猫咪，默认灰色',
530: h.textContent = 'three.js 未加载（检查 card3d/three.min.js 是否可访问）';
548: if (eh) { eh.style.color = '#ff9a9a'; eh.textContent = '初始化失败: ' + err.message; }
```

### app/src/main/java/com/example/focuspets/CrashActivity.kt

```text
28: ?: "（无堆栈信息）")
31: text = "😵 应用崩溃了\n\n下面这段可直接选中复制（或点「复制崩溃信息」按钮）：\n\n$trace"
50: text = "复制崩溃信息"
54: Toast.makeText(this@CrashActivity, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
59: text = "关闭应用"
```

### app/src/main/java/com/example/focuspets/MainActivity.kt

```text
49: .setTitle("上次崩溃信息（请截图发我）")
52: .setPositiveButton("知道了") { _, _ -> finish() }
109: this, "请先结束当前专注，再进入锁机模式", Toast.LENGTH_SHORT
```

### app/src/main/java/com/example/focuspets/badge/BadgeCelebration.kt

```text
169: putExtra(Intent.EXTRA_TITLE, "我的专注成就")
172: context.startActivity(Intent.createChooser(intent, "分享成就海报"))
229: drawCenter(c, "专注养宠", cx, cardT + 96f, 46f, Color.parseColor("#FFB300"), bold = true)
230: drawCenter(c, "FOCUS · 成就达成", cx, cardT + 146f, 30f, Color.parseColor("#BDBDBD"), bold = false)
245: val nick = if (nickname.isNotBlank()) nickname else "专注者"
246: drawCenter(c, "$nick 达成了这个成就", cx, divY + 84f, 46f, Color.parseColor("#212121"), bold = true)
247: drawCenter(c, "$petEmoji 和它的专注伙伴一起成长", cx, divY + 146f, 40f, Color.parseColor("#9E9E9E"), bold = false)
252: drawCenter(c, "每一次专注，都在陪它长大", cx, cardB - 36f, 30f, Color.parseColor("#BDBDBD"), bold = false)
```

### app/src/main/java/com/example/focuspets/cloud/AuthManager.kt

```text
31: fun displayName(): String = prefs.getString(KEY_NAME, "专注者") ?: "专注者"
```

### app/src/main/java/com/example/focuspets/cloud/CloudSyncManager.kt

```text
72: fun currentName(): String = if (::auth.isInitialized) auth.displayName() else "专注者"
```

### app/src/main/java/com/example/focuspets/db/AppDatabase.kt

```text
135: db.execSQL("UPDATE pets SET name='小水滴', emoji='💧', rarity='COMMON', unlock_cost=0, description='最基础的元素精灵' WHERE id=1")
136: db.execSQL("UPDATE pets SET name='火苗崽', emoji='🔥', rarity='COMMON', unlock_cost=50, description='充满热情的小家伙' WHERE id=2")
137: db.execSQL("UPDATE pets SET name='木灵', emoji='🌿', rarity='COMMON', unlock_cost=100, description='喜欢安静地睡觉' WHERE id=3")
139: db.execSQL("UPDATE pets SET name='星光兽', emoji='⭐', rarity='RARE', unlock_cost=300, description='只在深夜出现' WHERE id=4")
140: db.execSQL("UPDATE pets SET name='雷电犬', emoji='⚡', rarity='RARE', unlock_cost=500, description='行动迅捷如闪电' WHERE id=5")
141: db.execSQL("UPDATE pets SET name='冰晶狐', emoji='❄️', rarity='RARE', unlock_cost=700, description='高傲的冰雪贵族' WHERE id=6")
143: db.execSQL("UPDATE pets SET name='暗影龙', emoji='🌑', rarity='LEGENDARY', unlock_cost=1200, description='拥有毁灭力量' WHERE id=7")
144: db.execSQL("UPDATE pets SET name='神圣鹿', emoji='🦌', rarity='LEGENDARY', unlock_cost=1800, description='森林的守护神' WHERE id=8")
145: db.execSQL("UPDATE pets SET name='创世神', emoji='🌌', rarity='LEGENDARY', unlock_cost=2500, description='集齐所有图鉴后的终极奖励' WHERE id=9")
```

### app/src/main/java/com/example/focuspets/db/InitialDataProvider.kt

```text
16: PetEntity(1, "小猫咪", "🐱", Rarity.COMMON, 0, "可换装的小猫咪，默认灰色"),
17: PetEntity(2, "小狗", "🐶", Rarity.COMMON, 50, "忠诚的小伙伴（暂未实装立绘）"),
18: PetEntity(3, "龙猫", "🐹", Rarity.COMMON, 100, "软糯的小毛球（暂未实装立绘）"),
19: PetEntity(4, "星光兽", "⭐", Rarity.RARE, 300, "只在深夜出现"),
20: PetEntity(5, "雷电犬", "⚡", Rarity.RARE, 500, "行动迅捷如闪电"),
21: PetEntity(6, "冰晶狐", "❄️", Rarity.RARE, 700, "高傲的冰雪贵族"),
22: PetEntity(7, "暗影龙", "🌑", Rarity.LEGENDARY, 1200, "拥有毁灭力量"),
23: PetEntity(8, "神圣鹿", "🦌", Rarity.LEGENDARY, 1800, "森林的守护神"),
24: PetEntity(9, "创世神", "🌌", Rarity.LEGENDARY, 2500, "集齐所有图鉴后的终极奖励")
```

### app/src/main/java/com/example/focuspets/db/PetRepository.kt

```text
140: BadgeDef("first", "\uD83C\uDF31", "初次打卡", "完成第一次专注", 1, sessions),
141: BadgeDef("s3", "\uD83D\uDD25", "三日之约", "连续专注 3 天", 3, streak),
142: BadgeDef("s7", "\u2B50", "一周坚持", "连续专注 7 天", 7, streak),
143: BadgeDef("s14", "\uD83D\uDCE0", "双周达人", "连续专注 14 天", 14, streak),
144: BadgeDef("s30", "\uD83D\uDC51", "月度王者", "连续专注 30 天", 30, streak),
145: BadgeDef("s100", "\uD83C\uDFC6", "百日传奇", "连续专注 100 天", 100, streak),
181: val weekLabels = arrayOf("日", "一", "二", "三", "四", "五", "六")
185: val label = "周" + weekLabels[cal.get(Calendar.DAY_OF_WEEK) - 1]
```

### app/src/main/java/com/example/focuspets/debug/DebugHelper.kt

```text
112: content = "测试积分",
```

### app/src/main/java/com/example/focuspets/model/CatWardrobe.kt

```text
49: "gray" -> "灰色"
50: "blue" -> "蓝色"
51: "pink" -> "粉色"
52: "yellow" -> "黄色"
53: "white" -> "白色"
54: "black" -> "黑色"
59: "blue_crown" -> "蓝色王冠"
60: "pink_crown" -> "粉色王冠"
```

### app/src/main/java/com/example/focuspets/model/DogWardrobe.kt

```text
43: "gray" -> "灰色"
44: "white" -> "白色"
45: "pink" -> "粉色"
46: "yellow" -> "黄色"
47: "black" -> "黑色"
48: "colorful" -> "彩色"
53: "long" -> "长尾巴"
54: "short" -> "短尾巴"
```

### app/src/main/java/com/example/focuspets/service/FocusService.kt

```text
274: CHANNEL_ID, "专注计时", NotificationManager.IMPORTANCE_LOW
276: description = "番茄钟倒计时进行中"
290: .setContentTitle("专注中…不要切走哦 🐣")
291: .setContentText("剩余 ${formatMillis(remaining)}")
```

### app/src/main/java/com/example/focuspets/ui/collection/FragmentCollection.kt

```text
56: binding.tvPoints.text = "可用积分 $it"
63: binding.btnDebugUnlock.text = if (allUnlocked) "🛠 重置进度" else "🛠 一键满配"
82: Toast.makeText(requireContext(), "💰 积分已补到 100 万", Toast.LENGTH_SHORT).show()
89: "🎉 解锁成功：${pet.name}！",
107: .setTitle("解锁「${item.pet.name}」？")
109: "是否花费 ${item.pet.unlockCost} 积分解锁？\n" +
110: "当前可用积分：${item.availablePoints}"
112: .setPositiveButton("解锁") { _, _ -> viewModel.unlockPet(item.pet) }
113: .setNegativeButton("再想想", null)
117: .setTitle("尚未解锁")
119: "还差 ${item.pointsNeeded} 积分即可解锁「${item.pet.name}」，" +
120: "继续专注吧！"
122: .setPositiveButton("加油", null)
```

### app/src/main/java/com/example/focuspets/ui/collection/PetAdapter.kt

```text
56: Rarity.COMMON -> "普通" to R.color.rarity_common
57: Rarity.RARE -> "稀有" to R.color.rarity_rare
58: Rarity.LEGENDARY -> "传说" to R.color.rarity_legend
```

### app/src/main/java/com/example/focuspets/ui/collection/PetCardIcon.kt

```text
156: s.replace(Regex("（[^）]*(暂未实装|未实装|暂无立绘|待实装)[^）]*）"), "")
157: .replace(Regex("\\([^)]*(暂未实装|未实装|暂无立绘|待实装)[^)]*\\)"), "")
```

### app/src/main/java/com/example/focuspets/ui/focus/FocusCalendarView.kt

```text
25: private val weekLabels = listOf("日", "一", "二", "三", "四", "五", "六")
```

### app/src/main/java/com/example/focuspets/ui/focus/FocusRecordsActivity.kt

```text
180: "${cal.get(Calendar.YEAR)}年${cal.get(Calendar.MONTH) + 1}月"
213: Triple(start, end, SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINA).format(Date(start)))
223: val sf = SimpleDateFormat("M月d日", Locale.CHINA)
232: val label = "${cal.get(Calendar.YEAR)}年${cal.get(Calendar.MONTH) + 1}月"
242: val label = "${cal.get(Calendar.YEAR)}年"
290: binding.tvSummarySessions.text = "$it 次"
312: PieChartView.Slice(if (key.isEmpty()) "未命名" else key, c.minutes, color)
316: binding.pieChart.setData(slices, "总专注\n${formatMinutes(currentTotal)}")
323: LegendAdapter.LegendItem(color, if (key.isEmpty()) "未命名" else key, c.minutes, pct)
331: val content = if (raw.isEmpty()) "未命名" else raw
333: val time = SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date(r.createdAt))
337: binding.tvEventsTitle.text = "专注事件（${recs.size}）"
345: if (content == "未命名") {
346: Toast.makeText(this, "未命名内容不可改色", Toast.LENGTH_SHORT).show()
373: .setTitle("「$content」的颜色")
375: .setNegativeButton("取消", null)
386: min >= 60 -> "${min / 60} 小时 ${min % 60} 分"
387: else -> "$min 分钟"
394: Toast.makeText(this, "暂无数据可分享", Toast.LENGTH_SHORT).show()
400: Toast.makeText(this, "分享失败，请重试", Toast.LENGTH_SHORT).show()
406: putExtra(Intent.EXTRA_TITLE, "我的专注内容占比")
409: startActivity(Intent.createChooser(intent, "分享饼图"))
433: drawCenter(c, "专注内容占比", w / 2f, y + titleSize, titleSize, Color.parseColor("#212121"), true)
440: PieChartView.drawPie(c, cx, cy, pieR, slices, 0.52f, Color.WHITE, "总专注\n${formatMinutes(total)}")
449: drawRight(c, "$pct%  ·  ${s.value} 分", w - pad, ly + legendSize, legendSize, Color.parseColor("#757575"), false)
```

### app/src/main/java/com/example/focuspets/ui/focus/FragmentFocus.kt

```text
75: requireContext(), "没有通知权限，倒计时通知将无法显示", Toast.LENGTH_SHORT
154: binding.tvPointsFocus.text = "可用积分 $it"
237: Toast.makeText(requireContext(), "请输入 1~180 之间的分钟数", Toast.LENGTH_SHORT).show()
252: Toast.makeText(requireContext(), "请先填写专注内容再保存预设", Toast.LENGTH_SHORT).show()
258: requireContext(), "已保存预设：$content · ${selectedMinutes} 分钟", Toast.LENGTH_SHORT
283: text = "${p.content} · ${p.minutes}分"
286: closeIconContentDescription = "删除预设 ${p.content}"
323: .setTitle("放弃本次专注？")
324: .setMessage("已专注的时间不会计入积分")
325: .setPositiveButton("放弃") { _, _ ->
331: .setNegativeButton("继续专注", null)
342: requireContext(), "🍅 专注完成！+$minutes 积分", Toast.LENGTH_SHORT
352: .setTitle("😿 专注失败")
353: .setMessage("你切走超过了 5 秒，本次专注没有完成。\n宠物饿坏了，快完成一次专注哄哄它吧！")
354: .setPositiveButton("知道了", null)
364: val lines = pets.joinToString("\n") { "${it.emoji} ${it.name}（${it.unlockCost} 积分）" }
366: .setTitle("🎉 恭喜！有新宠物可以解锁啦")
367: .setMessage("这次专注攒够了积分！快去图鉴领取你的新伙伴：\n\n$lines")
368: .setPositiveButton("去图鉴看看") { _, _ ->
371: .setNegativeButton("稍后再说", null)
379: binding.btnStart.text = "放弃专注"
384: binding.tvSubtitle.text = "专注中…请保持 App 在前台"
397: binding.btnStart.text = "开始专注"
402: binding.tvSubtitle.text = "选择时长，开始一次专注"
447: closeIconContentDescription = "删除 $text"
461: .setTitle("删除专注内容？")
462: .setMessage("将删除所有「$content」的专注记录（含对应积分）。此操作不可撤销。")
463: .setPositiveButton("删除") { _, _ ->
469: .setNegativeButton("取消", null)
```

### app/src/main/java/com/example/focuspets/ui/focus/LegendAdapter.kt

```text
46: b.tvLegendMinutes.text = "${item.minutes} 分"
```

### app/src/main/java/com/example/focuspets/ui/focus/PieChartView.kt

```text
68: canvas.drawText("暂无数据", cx, cy - (fm.ascent + fm.descent) / 2f, p)
```

### app/src/main/java/com/example/focuspets/ui/focus/RecordAdapter.kt

```text
40: b.tvRecordMinutes.text = "${item.minutes} 分"
```

### app/src/main/java/com/example/focuspets/ui/home/HomeFragment.kt

```text
118: binding.tvTitle.text = "${CloudSyncManager.currentName()}的伙伴"
158: PetMood.SICK -> "生病"
159: PetMood.GLOW -> "发光"
160: PetMood.NORMAL -> "普通"
162: binding.btnDebugMood.text = "🎭 调试心情：" + moodLabel(PetCareState.getMood(requireContext()))
166: binding.btnDebugMood.text = "🎭 调试心情：" + moodLabel(mood)
398: binding.tvMeowBubble.text = "喵~"
410: binding.tvMeowBubble.text = "汪~"
576: binding.btnWardrobe.text = if (petIsCat) "🐱 装扮小猫" else "🐶 装扮小狗"
587: binding.tvCollectionStatus.text = "已收集 ${state.unlockedCount} / 9"
601: binding.tvProgressHint.text = "🎉 图鉴已全部收集完成！"
605: "距解锁 ${state.nextLockedPet.emoji} ${state.nextLockedPet.name} 还差 ${state.remainingPoints} 积分"
611: binding.tvMoodTag.text = "🤒 生病中…完成一次专注恢复"
628: binding.tvMoodTag.text = "✨ 容光焕发！"
646: binding.tvMoodTag.text = "😊 陪伴中"
665: PetMood.SICK -> "，当前生病中"
666: PetMood.GLOW -> "，当前容光焕发"
667: PetMood.NORMAL -> "，当前陪伴中"
670: "${state.selectedPet.name} ${state.selectedPet.emoji}$moodText，点击或左右滑动可切换宠物"
```

### app/src/main/java/com/example/focuspets/ui/leaderboard/LeaderboardAdapter.kt

```text
42: holder.points.text = "${item.totalPoints} 分"
50: append("第 ${item.rank} 名，${item.displayName}，${item.totalPoints} 分")
51: append("，宠物 ${PetCatalog.emoji(item.petId)}")
52: if (isMe) append("，这是你")
```

### app/src/main/java/com/example/focuspets/ui/leaderboard/LeaderboardFragment.kt

```text
41: binding.tvMyName.text = "昵称：${CloudSyncManager.currentName()}"
50: if (result.me?.rank != null) "我的排名：#${result.me.rank}" else "我的排名：未上榜"
51: binding.tvMyPoints.text = "我的积分：${result.me?.totalPoints ?: 0}"
62: .setTitle("修改昵称")
64: .setPositiveButton("确定") { _, _ ->
68: .setNegativeButton("取消", null)
```

### app/src/main/java/com/example/focuspets/ui/leaderboard/LeaderboardViewModel.kt

```text
35: _status.postValue("加载中…")
45: _status.postValue("⚠ 后端未连接，显示上次缓存（共 ${cached.list.size} 条）")
47: _status.postValue("排行榜加载失败：${e.message}（请确认后端服务已启动）")
```

### app/src/main/java/com/example/focuspets/ui/lock/LockActivity.kt

```text
205: Toast.makeText(this, "请输入 1~180 之间的分钟数", Toast.LENGTH_SHORT).show()
219: .setTitle("开始锁机专注？")
221: "开始后手机将被锁定在本页面，无法主动退出；专注结束自动解锁。\n" +
223: "已设置白名单：锁机期间可正常使用白名单内的应用，其余应用会被自动拉回。"
225: "未设置白名单：将使用系统级屏幕固定，把手机完全锁死在当前页面。"
227: .setPositiveButton("开始锁机") { _, _ -> beginLock() }
228: .setNegativeButton("不用了", null)
270: putExtra(FocusService.EXTRA_CONTENT, "锁机")
284: .setTitle("需要「使用情况访问」权限")
285: .setMessage("锁机白名单需要读取前台应用来判断是否放行。请在接下来的设置页中，找到「专注养宠」并开启「使用情况访问」权限，然后返回重新点击「开始锁机专注」。")
286: .setPositiveButton("去授权") { _, _ -> UsageStatsPermission.openSettings(this) }
287: .setNegativeButton("取消", null)
296: .setTitle("强制退出锁机？")
297: .setMessage("这是今天的最后一次强制退出机会，确定要解锁离开吗？")
298: .setPositiveButton("强制退出") { _, _ -> doForceExit() }
299: .setNegativeButton("继续专注", null)
315: binding.tvDoneSub.text = "+$minutes 积分"
325: .setTitle("😿 专注失败")
326: .setMessage("本次专注没有完成。")
327: .setPositiveButton("知道了") { _, _ ->
352: binding.btnForceExit.text = "强制退出（今日剩余 1 次）"
357: binding.tvSubtitle.text = "锁机专注中…保持手机在本页面"
375: binding.btnStart.text = "开始锁机专注"
381: binding.tvSubtitle.text = "选择时长，开始锁机专注"
393: if (petId == DogWardrobe.DOG_PET_ID) "专注中的小狗" else "专注中的小猫"
```

### app/src/main/java/com/example/focuspets/ui/onboarding/OnboardingActivity.kt

```text
139: binding.btnNext.text = if (last) "开始体验" else "下一步"
```

### app/src/main/java/com/example/focuspets/ui/settings/BgColorPickerDialog.kt

```text
52: "暖米色", "蜜桃粉", "天空蓝", "薄荷绿",
53: "薰衣草紫", "奶油黄", "樱花粉", "青瓷绿"
57: val label = labels.getOrElse(index) { "背景" }
65: contentDescription = "$label 背景色"
```

### app/src/main/java/com/example/focuspets/ui/settings/SettingsActivity.kt

```text
55: Toast.makeText(this, "已设置自定义提醒音频", Toast.LENGTH_SHORT).show()
68: binding.tvVersion.text = "版本 ${appVersionName()}"
94: "未登录（离线或同步未初始化）"
96: "匿名账号 ····${uid.takeLast(6)}"
105: hint = "2-12 个字符"
116: .setTitle("修改昵称")
118: .setNegativeButton("取消", null)
119: .setPositiveButton("保存") { _, _ ->
122: Toast.makeText(this, "昵称至少 2 个字符", Toast.LENGTH_SHORT).show()
126: Toast.makeText(this, "昵称已更新：$name", Toast.LENGTH_SHORT).show()
136: .setTitle("注销云端账号？")
138: "会清除本机绑定的匿名云端身份，下次启动将生成一个新的匿名账号。\n\n" +
139: "本机进度（专注记录、图鉴、妆扮）不会被删除。"
141: .setNegativeButton("取消", null)
142: .setPositiveButton("确认注销") { _, _ ->
145: Toast.makeText(this, "已注销，下次启动以新匿名账号同步", Toast.LENGTH_SHORT)
156: .setTitle("重置全部进度？")
158: "将清空：\n" +
159: "· 全部专注记录与积分\n" +
160: "· 已解锁的宠物图鉴（仅保留初始那只）\n" +
161: "· 猫咪妆扮购买记录\n" +
162: "· 宠物心情状态\n\n" +
163: "同时会注销云端账号，避免旧数据被同步回来。\n此操作不可撤销。"
165: .setNegativeButton("取消", null)
166: .setPositiveButton("确认重置") { _, _ ->
168: Toast.makeText(this, "进度已重置，回到初始状态", Toast.LENGTH_SHORT).show()
272: if (hasUri) "已选择自定义音频" else "尚未选择文件"
```

### app/src/main/java/com/example/focuspets/ui/stats/BadgeAdapter.kt

```text
38: b.tvDesc.text = if (item.unlocked) item.desc else "连续 ${item.target} 天解锁"
```

### app/src/main/java/com/example/focuspets/ui/stats/FocusHeatmapView.kt

```text
37: private val weekdayLabels = arrayOf("一", "二", "三", "四", "五", "六", "日")
```

### app/src/main/java/com/example/focuspets/ui/stats/FragmentStats.kt

```text
64: binding.tvSessions.text = "$it 次"
67: binding.tvStreak.text = "$it 天"
81: min >= 60 -> "${min / 60} 小时 ${min % 60} 分"
82: else -> "$min 分钟"
138: val msg = if (bar.minutes > 0) "${bar.label}：${bar.minutes} 分钟"
139: else "${bar.label}：暂无专注记录"
146: if (max == 0) "还没有专注记录，开始第一个番茄钟吧～"
147: else "近 7 天最高 ${max} 分钟 · 点击柱子看具体分钟数"
```

### app/src/main/java/com/example/focuspets/ui/wardrobe/CatWardrobeBottomSheet.kt

```text
40: _message.value = "🎉 购买成功！-$cost 积分"
43: _message.value = "积分不足，再专注一会儿吧"
99: binding.tvPoints.text = "可用积分 $points"
118: else -> "$label ${CatWardrobe.COLOR_COST}分"
155: isEquipped -> "✓ 红裙（点击脱下）"
156: isOwned -> "红裙（点击穿上）"
157: else -> "红裙 ${CatWardrobe.DRESS_COST}分"
201: if (dressOwned) "" else "需要先购买裙子才能买王冠"
211: else -> "$label ${CatWardrobe.CROWN_COST}分"
```

### app/src/main/java/com/example/focuspets/ui/wardrobe/DogWardrobeBottomSheet.kt

```text
64: binding.tvPoints.text = "可用积分 $points"
110: else -> "$label ${DogWardrobe.COLOR_COST}分"
143: val outfitName = if (tail == "long") "礼服" else "西服"
156: tieOn -> "✓ 红领带（点击脱下）"
157: tieOwned -> "红领带（点击穿上）"
158: else -> "红领带 ${DogWardrobe.TIE_COST}分"
187: outfitOn -> "✓ $outfitName（点击脱下）"
188: outfitOwned -> "$outfitName（点击穿上）"
189: else -> "$outfitName ${DogWardrobe.OUTFIT_COST}分"
228: !tieOwned -> "需要先购买红领带"
229: outfitChanges && !outfitOwned -> "需要先购买${outfitName}才能买学士帽"
230: !capChanges -> "当前颜色暂无学士帽立绘"
235: capOn -> "✓ 学士帽（点击脱下）"
236: capOwned -> "学士帽（点击穿上）"
237: else -> "学士帽 ${DogWardrobe.CAP_COST}分"
```

### app/src/main/res/layout/activity_focus_records.xml

```text
28: android:text="‹ 返回"
37: android:text="专注记录"
74: android:contentDescription="上个月"
98: android:contentDescription="下个月"
117: android:text="🔴 有记录的日期 · 点击查看当天专注事件"
151: android:contentDescription="上一时段"
176: android:contentDescription="下一时段"
200: android:text="日" />
208: android:text="周" />
216: android:text="月" />
224: android:text="年" />
233: android:text="回到今天"
265: android:text="本时段专注"
274: android:text="0 分钟"
300: android:text="专注次数"
309: android:text="0 次"
336: android:text="内容占比"
353: android:text="分享饼图"
386: android:text="专注事件"
405: android:text="这一时段还没有专注记录，去专注一次吧～"
```

### app/src/main/res/layout/activity_lock.xml

```text
35: android:text="🔒 锁机模式"
45: android:text="退出"
57: android:contentDescription="专注中的小猫" />
69: android:contentDescription="剩余专注时间" />
76: android:text="选择时长，开始锁机专注"
85: android:contentDescription="专注进度" />
100: android:text="15 分钟" />
107: android:text="25 分钟" />
114: android:text="45 分钟" />
129: android:hint="自定义分钟（1~180）">
146: android:text="应用" />
154: android:text="开始锁机专注"
164: android:text="📋 白名单应用（锁机期间可用）"
174: android:text="🔒 开始后手机将被锁定在本页面，无法主动退出（白名单内的应用仍可使用）；专注结束自动解锁。"
183: android:text="强制退出（今日剩余 1 次）"
217: android:text="专注完成！"
227: android:text="+25 积分"
236: android:text="解锁离开"
```

### app/src/main/res/layout/activity_lock_whitelist.xml

```text
22: android:text="‹ 返回"
31: android:text="锁机白名单应用"
41: android:text="勾选锁机期间仍允许使用的应用。开启白名单后，锁机将不再硬锁死手机：白名单内的应用可正常使用，打开白名单外的应用会被自动拉回锁机页。"
58: android:text="保存"
```

### app/src/main/res/layout/activity_onboarding.xml

```text
39: android:text="欢迎来到 专注养宠"
49: android:text="用每一次专注，点亮属于你的小宠物"
59: android:text="专注越久，积分越多，\n伙伴也会越来越开心 ✨"
77: android:text="怎么玩？"
85: android:text="🍅 在「专注」页开始一次专注计时" />
90: android:text="✅ 完成后获得积分，宠物心情变好" />
95: android:text="📖 用积分在「图鉴」解锁更多伙伴" />
100: android:text="🔒 用「锁机」强制专注，还能给小猫换装" />
105: android:text="🏆 在「排行」里和朋友比比专注时长" />
127: android:text="选一个喜欢的背景色"
136: android:text="之后随时可以在首页「换背景」里改"
170: android:text="跳过"
186: android:text="下一步"
```

### app/src/main/res/layout/activity_settings.xml

```text
30: android:contentDescription="返回"
38: android:text="设置"
45: <TextView style="@style/SettingsSectionTitle" android:text="账号" />
72: android:text="昵称（排行榜显示）" />
77: android:text="专注者" />
85: android:text="编辑"
106: android:text="云端账号" />
111: android:text="匿名账号" />
119: android:text="注销"
126: android:text="本应用为匿名登录，注销会清除本机绑定的云端身份，下次启动生成新的匿名账号（本地进度保留）。" />
131: <TextView style="@style/SettingsSectionTitle" android:text="数据与进度" />
145: android:text="重置进度" />
149: android:text="清空全部专注记录、已解锁图鉴、猫咪妆扮与心情状态，回到刚安装的状态；同时会注销云端账号，避免云端把旧数据同步回来。此操作不可撤销。" />
157: android:text="⚠️ 重置进度"
164: <TextView style="@style/SettingsSectionTitle" android:text="锁机" />
185: android:text="锁机时使用系统屏幕固定" />
189: android:text="开启后锁机会触发系统「应用已固定」提示（需手动确认）；关闭则用普通全屏锁定，无系统弹窗，但可按 Home 退出。" />
200: <TextView style="@style/SettingsSectionTitle" android:text="结束提醒" />
214: android:text="专注 / 锁机结束时" />
218: android:text="选择结束时如何提醒你（铃声、震动二选一，或两者皆无）。" />
225: android:contentDescription="结束提醒方式"
235: android:text="仅震动"
244: android:text="默认铃声"
253: android:text="自定义音频"
262: android:text="关闭"
280: android:text="选择音频文件"
293: <TextView style="@style/SettingsSectionTitle" android:text="引导" />
314: android:text="新手引导" />
318: android:text="重新查看三页使用说明" />
326: android:text="查看"
332: <TextView style="@style/SettingsSectionTitle" android:text="外观" />
346: android:text="深色模式" />
350: android:text="可手动切换浅色 / 深色，或跟随系统设置" />
357: android:contentDescription="深色模式切换：跟随系统、浅色或深色"
367: android:text="跟随系统"
376: android:text="浅色"
385: android:text="深色"
392: <TextView style="@style/SettingsSectionTitle" android:text="关于" />
406: android:text="专注养宠 v1.0" />
410: android:text="专注数据保存在本机；开启云端同步后会镜像到自建后端，用于跨设备与排行榜。" />
```

### app/src/main/res/layout/dialog_badge_poster.xml

```text
27: android:text="分享"
37: android:text="收下啦"
```

### app/src/main/res/layout/dialog_bg_color.xml

```text
13: android:text="选一个你喜欢的背景色 🎨"
22: android:text="番茄 ToDo 风格 · 之后也能在首页「换背景」重新选择"
40: android:text="稍后再说"
```

### app/src/main/res/layout/dialog_cat_wardrobe.xml

```text
15: android:text="🐱 装扮小猫咪"
24: android:text="可用积分 0"
32: android:text="皮肤颜色"
47: android:text="衣服配件"
62: android:text="王冠（需先购买裙子）"
78: android:text="当前颜色暂无王冠立绘，换一个颜色试试"
88: android:text="完成" />
```

### app/src/main/res/layout/dialog_dog_wardrobe.xml

```text
15: android:text="🐶 装扮小狗"
24: android:text="可用积分 0"
32: android:text="尾巴（免费自选）"
47: android:text="皮肤颜色"
62: android:text="配饰（先领带 → 西服/礼服 → 学士帽）"
88: android:text="完成" />
```

### app/src/main/res/layout/fragment_collection.xml

```text
15: android:text="可用积分 0"
32: android:text="🛠 一键满配"
42: android:text="💰 补满积分"
```

### app/src/main/res/layout/fragment_focus.xml

```text
27: android:text="专注"
37: android:text="📊 记录"
62: android:text="本次专注内容"
71: android:hint="写代码 / 看书 / 写报告…">
87: android:text="最近用过"
108: android:text="😿 宠物饿坏了…完成一次专注让它恢复健康"
123: android:contentDescription="剩余专注时间" />
130: android:text="选择时长，开始一次专注"
140: android:contentDescription="专注进度" />
157: android:text="15 分钟" />
164: android:text="25 分钟" />
171: android:text="45 分钟" />
186: android:hint="自定义分钟（1~180）">
203: android:text="应用" />
211: android:text="⭐ 存为预设"
221: android:text="常用预设"
240: android:text="开始专注"
249: android:text="可用积分 0"
258: android:text="🍅 专注期间请保持 App 在前台\n切后台超过 5 秒，本次专注将失败"
```

### app/src/main/res/layout/fragment_home.xml

```text
23: android:text="我的伙伴"
32: android:text="📖 图鉴"
41: android:text="⚙ 设置"
53: android:contentDescription="宠物舞台：点击或左右滑动切换宠物"
107: android:text="喵~"
129: android:text="小水滴"
141: android:text="😊 陪伴中"
162: android:text="距解锁 🐶 小狗 还差 50 积分"
171: android:text="已收集 1 / 9"
179: android:text="点击宠物切换 · 左右滑动也可以"
190: android:text="🐱 装扮小猫"
201: android:text="🎨 换背景"
211: android:text="🎭 调试心情：普通"
```

### app/src/main/res/layout/fragment_leaderboard.xml

```text
17: android:text="专注排行榜"
26: android:text="看看谁最专注，陪宠物长得最快"
42: android:text="我的排名：—"
52: android:text="我的积分：0"
61: android:text="昵称：专注者"
78: android:text="刷新" />
86: android:text="修改昵称" />
```

### app/src/main/res/layout/fragment_stats.xml

```text
17: android:text="专注统计"
26: android:text="每一次专注，都在陪它长大"
46: android:text="🏆 查看全球排行榜"
73: android:text="0 分钟"
82: android:text="今日专注"
100: android:text="0 分钟"
109: android:text="累计专注"
127: android:text="0 次"
136: android:text="专注次数"
154: android:text="0 天"
163: android:text="连续打卡"
174: android:text="专注时段热力图"
183: android:text="看看你一天里最专注的时段 🔥"
210: android:text="少"
225: android:text="多"
236: android:text="颜色越深 = 该时段专注越久"
246: android:text="连续打卡成就徽章"
263: android:text="近 7 天专注（分钟）"
283: android:text="数据来自专注记录"
```

### app/src/main/res/layout/item_focus_record.xml

```text
26: android:text="未命名"
45: android:text="0 分"
```

### app/src/main/res/layout/item_legend.xml

```text
21: android:text="内容"
29: android:text="0 分"
```

### app/src/main/res/layout/leaderboard_item.xml

```text
51: android:text="昵称"
60: android:text="0 分"
```

### app/src/main/res/menu/bottom_nav.xml

```text
7: android:title="首页" />
12: android:title="专注" />
17: android:title="锁机" />
22: android:title="图鉴" />
27: android:title="统计" />
```

### app/src/main/res/values/strings.xml

```text
2: <string name="app_name">专注养宠</string>
```

### developer/bug.md

```text
1: ### 修改bug
2: - 1.模型问题-卡牌猫尾巴部分消失对照小狗更改（）
3: - 2.锁机模式二次跳转（Judy-已解决）
4: - 3.专注时候进入记录界面会使得专注失败 （）
5: - 4.统计界面-近7天专注最多的一天会使得周几文字消失（）
6: - 5.白名单会使得手机系统跳出蓝色界面通知屏幕固定点击知道了屏幕直接锁定无法使用白名单软件（）
7: - 6.卡牌模型变小（）
8: - 7.饼图右下角总专注文字位置和饼图重叠（）
9: ### 添加新功能
10: - 1.自习室-后端连房间（）
11: - 2.解锁新宠物弹出卡牌界面
12: - 3.解锁同一宠物全部装扮恭喜界面
13: - 4.记录界面可删改记录事件
14: - 5.记录界面-饼图出现动画
15: - 6.更多成就系统左右滑动可看到
16: - 7.云端备份导出功能-首页设置界面内
17: - 8.放弃专注需要填写原因（）
18: - 9.记录界面可总结前一周的专注记录
19: ### 图片处理
20: - 1.小猫（已处理完成）
21: - 2.小狗（改版2.0）
22: - 3.龙猫（规划中）
23: ### 测试系统项目
24: - 冒烟测试
25: - 单元功能性测试
26: - 用户友好测试
27: - 可复用性测试
```

### process_cat_assets.py

```text
8: SRC_DIR = BASE_DIR / "图片" / "初始-猫"
13: "灰": "gray",
14: "蓝": "blue",
15: "粉": "pink",
16: "黄": "yellow",
17: "白": "white",
18: "黑": "black",
22: "蓝冠": "blue_crown",
23: "粉冠": "pink_crown",
29: ("不同颜色的猫/灰.jpg", "gray.jpg"),
30: ("不同颜色的猫/蓝.jpg", "blue.jpg"),
31: ("不同颜色的猫/粉.jpg", "pink.jpg"),
32: ("不同颜色的猫/黄.jpg", "yellow.jpg"),
33: ("不同颜色的猫/白.jpg", "white.jpg"),
34: ("不同颜色的猫/黑.jpg", "black.jpg"),
36: ("裙子/灰-红裙.jpg", "gray_dress.jpg"),
37: ("裙子/蓝-红裙.jpg", "blue_dress.jpg"),
38: ("裙子/粉-红裙.jpg", "pink_dress.jpg"),
39: ("裙子/黄-红裙.jpg", "yellow_dress.jpg"),
40: ("裙子/白-红裙.jpg", "white_dress.jpg"),
41: ("裙子/黑-红裙.jpg", "black_dress.jpg"),
43: ("皇冠/灰-红裙-蓝冠.jpg", "gray_dress_blue_crown.jpg"),
44: ("皇冠/蓝-红裙-蓝冠.jpg", "blue_dress_blue_crown.jpg"),
45: ("皇冠/蓝-红裙-粉冠.jpg", "blue_dress_pink_crown.jpg"),
46: ("皇冠/粉-红裙-蓝冠.jpg", "pink_dress_blue_crown.jpg"),
47: ("皇冠/粉-红裙-粉冠.jpg", "pink_dress_pink_crown.jpg"),
48: ("皇冠/黄-红裙-蓝冠.jpg", "yellow_dress_blue_crown.jpg"),
49: ("皇冠/黄-红裙-粉冠.jpg", "yellow_dress_pink_crown.jpg"),
50: ("皇冠/黑-红裙-蓝冠.jpg", "black_dress_blue_crown.jpg"),
51: ("皇冠/黑-红裙-粉冠.jpg", "black_dress_pink_crown.jpg"),
```

### server/leaderboard_server.py

```text
22: return "专注者" + str(random.randint(1000, 9999))
209: print(f"  DB: {DB_PATH}  |  模拟器连 http://10.0.2.2:{PORT}/  |  真机连 http://<本机局域网IP>:{PORT}/")
```

### tools/cutout_cat_crowns.py

```text
1: """ FocusPets: cut out the three new cat crown images from 图片/初始-猫/皇冠 into transparent PNGs under assets/cat/. The background is beige #FEF8EA (254,248,234); reuse v2 edge decontamination and light feathering. Match CatWardrobe.assetPathFor with cat/<color>_dress_<crown-color>.png."""
9: SRC = BASE / "图片" / "初始-猫" / "皇冠"
14: "白-红裙-蓝冠.jpg": "white_dress_blue_crown.png",
15: "白-红裙-粉冠.jpg": "white_dress_pink_crown.png",
16: "灰-红裙-粉冠.jpg": "gray_dress_pink_crown.png",
41: print("SKIP 缺失:", src_name)
```

### tools/cutout_dog.py

```text
1: """ FocusPets short-tail dog cutouts: read 图片/狗/短尾巴 and output transparent PNGs under assets/dog/. Use lightweight rembg u2netp, alpha decontamination, and gentle feathering for pale backgrounds. Name files tail_color[_red_tie][_black_suit|red_dress][_academic_cap]. Explicit color tokens allow future variants: short tails use black_suit, long tails red_dress, caps academic_cap. Deduplicate by filename, retaining the first image, including suit/cap folder duplicates. Process only 短尾巴; preserve existing long-tail artwork."""
9: IN = BASE / "图片" / "狗" / "短尾巴"
15: COLORS = [("白", "white"), ("灰", "gray"), ("黑", "black"),
16: ("黄", "yellow"), ("粉", "pink"), ("彩色", "colorful")]
22: text = f"{stem} {parent}"         # Keywords may occur in parent folder names, e.g. 红领带/黄.jpg.
35: if "领带" in text:
37: if "西服" in text:
39: elif "礼裙" in text:
41: if any(k in text for k in ("学士帽", "学院帽")):
100: print(f"\n共生成 {len(pngs)} 张短尾小狗透明 PNG -> {OUT}")
```

### tools/cutout_v2.py

```text
97: print(f"OK {jpg.name} -> {png.name}  边缘色块={report[-1][1]:.1f}%")
109: print("\n=== 黄色系边缘色块对比（应接近 0） ===")
```

### tools/smoke_card.js

```text
71: console.log('1) 脚本解析/执行 OK');
73: if (!loadHandler) { console.log('!! 未捕获到 load 处理器'); process.exit(1); }
75: console.log('2) load 处理器 OK（init + 兜底渲染）');
78: setSample({ id: 1, name: '小猫咪', rarity: 'LEGENDARY', locked: false, unlockCost: 0, desc: '可换装的小猫咪，默认灰色' });
79: console.log('3) setupCard(传说) OK');
81: setSample({ id: 9, name: '创世神', rarity: 'COMMON', locked: true, unlockCost: 2500, desc: '集齐所有图鉴后的终极奖励' });
82: console.log('4) setupCard(锁定) OK  ← 之前 data.locked 报错点');
89: setSample({ id: 4, name: '星光兽', rarity: 'RARE', locked: false, unlockCost: 300, desc: '只在深夜出现' });
94: console.log('5) 拖动旋转 60 帧 OK（y 无上限）');
99: console.log('6) 轻点 OK ← 覆盖 onTap（正面放大 / 背面展开简介）');
102: setSample({ id: 1, name: '测试', rarity: r, locked: false, unlockCost: 0, desc: '一句简介' });
103: console.log('7) 稀有度 ' + r + ' 构建 OK');
106: console.log('=== 全部通过 ✓ ===');
```

## Validation

- Non-comment/non-docstring content comparison against HEAD: passed for all 94 changed source files (line endings normalized for Git comparison).
- Python AST comparison excluding docstrings: passed.
- 3D card Node smoke test: passed.
- Offline Debug APK build: succeeded; existing deprecation warnings remain.
- git diff --check: passed.

