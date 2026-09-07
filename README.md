# FocusPets · 专注养宠 + 图鉴收集

一款用「番茄钟专注」换取「宠物图鉴收集」的 Android 应用。每专注一分钟得 1 积分，用积分解锁萌宠，宠物还会根据你的专注表现切换心情。

> 包名：`com.example.focuspets` ｜ 语言：Kotlin ｜ 架构：单 Activity + 多 Fragment + MVVM + Room



---

## 一、技术栈

| 类别   | 方案                                                         |
| ---- | ---------------------------------------------------------- |
| 开发语言 | Kotlin 1.9.24                                              |
| UI   | ViewBinding + Material Design + RecyclerView               |
| 架构   | MVVM（ViewModel + LiveData + MediatorLiveData）              |
| 本地存储 | Room（SQLite 封装，KSP 注解处理）                                   |
| 计时核心 | Foreground Service + CountDownTimer + 包内定向广播               |
| 动画   | 属性动画（ObjectAnimator / AnimatorSet）+ 自定义 View + SoundPool 音效 |
| 构建   | Gradle 8.7 + AGP 8.5.2，compileSdk / targetSdk 34，minSdk 24 |

---

## 二、功能总览

应用分为三个底部 Tab：**首页（宠物陪伴）**、**专注（计时）**、**图鉴（收集）**。

```
┌─────────────┐   ┌─────────────┐   ┌─────────────┐
│   首页 Home  │   │  专注 Focus │   │  图鉴 Pokédex│
│  宠物互动    │   │  番茄钟计时  │   │  9 只宠物网格 │
│  心情三态    │   │  积分结算    │   │  解锁/详情   │
└─────────────┘   └─────────────┘   └─────────────┘
        ▲                │ 专注完成 +1 分
        │ 解锁后可选中     ▼
        └──────── 图鉴解锁消耗积分 ────────┘
```

---

## 三、宠物功能详解

### 1. 首页 · 宠物陪伴（`ui/home`）

- **双形态渲染**：1 号「小猫咪」用 `assets/cat/*.jpg` 立绘（`ImageView`），其余 8 只仍用 emoji（`TextView`），按当前选中宠物自动切换。
- **呼吸动画**：宠物以 1.0 ↔ 1.06 缩放无限往返（`AnimatorSet` + `REVERSE`），作用于当前可见的宠物视图。
- **切换宠物**：点击宠物 / 左右滑动，在「已解锁列表」中循环切换（`selectNext` / `selectPrev`）。
- **跳跃反馈**：每次切换触发 `OvershootInterpolator` 跳跃动画。
- **心情三态渲染**（见第四节）：根据 `PetMood` 显示不同文案、颜色、光环与生病贴纸。
- **进度条**：展示「距下一只可解锁宠物还差多少积分」，满配后显示「图鉴已全部收集完成」。

### 1.1 小猫咪活体行为（1 号宠物专属）

猫咪被选中且页面在前台时，每 4~9 秒随机触发一种行为，让它显得活灵活现：

| 行为     | 表现                                          |
| ------ | ------------------------------------------- |
| 🐾 喵喵叫 | `SoundPool` 播放 `res/raw/meow.wav`（音量 0.25，很轻）+ 冒出「喵~」气泡 |
| 🐾 侧壁跳跃 | 在屏幕左右侧壁之间连跳两次再回到中间（`TRANSLATION_X/Y` 抛物线）    |
| 🐾 凑近嗅探 | 放大到 1.22 倍并左右轻晃，结束后恢复并重启呼吸动画                |

> 行为调度在 `onPause` 停止、`onResume` 恢复，页面不可见时不会空跑。嗅探会临时取消呼吸动画，避免两者同时改 `SCALE` 打架。

### 1.2 小猫咪妆扮系统（积购买 · 可换装）

入口：首页「🐱 装扮小猫」按钮（仅 1 号小猫咪被选中时显示），打开 `CatWardrobeBottomSheet` 底部面板。

| 类别   | 条目                    | 单价         | 说明                   |
| ---- | --------------------- | ---------- | -------------------- |
| 皮肤颜色 | 灰（默认）/ 蓝 / 粉 / 黄 / 白 / 黑 | 50 积分（灰色免费） | 灰色默认拥有，其余需购买        |
| 衣服配件 | 红裙                    | 120 积分      | **必须先购买裙子**才能买王冠    |
| 王冠   | 蓝色王冠 / 粉色王冠           | 180 积分      | 需先拥有裙子；买后自动穿上裙子     |

规则与实现要点：

- **买断制、随时换**：可购买多个，已拥有的条目点击即穿上/脱下，不重复扣费。
- **皇冠依附裙子**：皇冠立绘都是「颜色-红裙-王冠」，所以脱下裙子会连带摘掉王冠；换到没有该王冠立绘的颜色时也会自动摘掉。
- **素材完整性**：豆包生成的立绘并非每种颜色都配齐全套 —— **白猫没有王冠立绘、灰猫只有蓝色王冠**。面板按 `CatWardrobe.CROWN_OPTIONS_BY_COLOR` 只展示有立绘的组合，避免加载到不存在的图。
- **立绘路径解析**：`cat/{颜色}.jpg` → `cat/{颜色}_dress.jpg` → `cat/{颜色}_dress_{王冠}.jpg`，由 `CatWardrobe.assetPathFor()` 统一生成。
- **水印处理**：原始素材左上角与右下角有「豆包AI生成」水印，已用 `process_cat_assets.py` 采样背景色后将四角区域涂平（输出在 `app/src/main/assets/cat/`，共 21 张）。
- **积分扣减**：购买写入 `wardrobe_purchases` 表，可用积分公式同步纳入该项消费（见第六节）。

### 2. 专注 · 番茄钟（`ui/focus` + `service`）

- **预设时长**：15 / 25 / 45 分钟（`MaterialButtonToggleGroup` 单选）。
- **自定义时长**：输入框可填 **1~180** 任意分钟数，点「应用」生效，同时取消预设按钮的选中态；计时中禁用输入。
- **进度条样式**：自定义 View `FocusProgressBar` —— 已过去时间为**橘色**、未走完为**白色**，交界处一条**黑色竖线**分割，且两端不带圆角（Material 原生 `LinearProgressIndicator` 的圆角会让两截变成各自独立的圆弧）。
- **前台服务计时**：`FocusService` 常驻通知栏，每秒广播剩余时间给 UI，切到后台也不会被系统杀掉中断计时。
- **后台判定（防作弊）**：App 切到后台后宽限 **5 秒**，超过则判定「专注失败」——不结算积分，宠物进入生病状态。
- **放弃**：计时中按钮变为「放弃专注」，确认后不结算、不惩罚。
- **积分结算**：倒计时自然完成 → 写入一条 `focus_records`（分钟数即积分数）→ 广播 `FINISHED` → 检查是否有新宠物积分达标并弹庆祝窗。

### 3. 图鉴 · 收集（`ui/collection`）

- **3 列网格**：`GridLayoutManager(3)`，9 张宠物卡片。
- **稀有度徽章 + 彩色边框**：普通（蓝）/ 稀有（紫）/ 传说（金），已解锁卡片高亮，未解锁显示 `❓` + 所需积分。
- **点击分发**：
  - 已解锁 → 底部弹窗看详情（emoji / 名称 / 稀有度 / 故事文案）。
  - 积分够 → 弹确认框「花费 N 积分解锁？」。
  - 积分不够 → 提示「还差 N 积分」。
- **解锁逻辑**：插入一条 `user_collection` 记录即可；可用积分是派生值（总专注分钟 − 已解锁 cost 之和），无需额外扣分字段，天然防超扣。

---

## 四、宠物心情状态机（`model/PetCareState`）

心情存于 `SharedPreferences`（单机小数据，无需进 Room），三态：

| 状态          | 触发条件                | 页面表现               |
| ----------- | ------------------- | ------------------ |
| `NORMAL` 普通 | 默认 / 专注失败后重新 normal | 「😊 陪伴中」，蓝色标签      |
| `SICK` 生病   | 专注失败（切后台超 5 秒）      | 宠物半透明 + 🤒 贴纸，红色标签 |
| `GLOW` 发光   | 连续专注成功 ≥ 3 次        | 金色光环呼吸脉冲，金色标签      |

> 连续成功计数在「失败」时清零；再次失败后重新从 0 累计。

---

## 五、宠物图鉴一览（9 只）

积分规则：**专注 1 分钟 = 1 积分**；解锁即消耗对应积分（已解锁宠物的 cost 之和从总分中扣除）。

| # | 名称  | emoji | 稀有度 | 解锁积分 | 简介             |
| - | --- | ----- | --- | ---- | -------------- |
| 1 | 小猫咪 | 🐱    | 普通  | 0    | 可换装的小猫咪，默认灰色（首只赠送） |
| 2 | 火苗崽 | 🔥    | 普通  | 50   | 充满热情的小家伙       |
| 3 | 木灵  | 🌿    | 普通  | 100  | 喜欢安静地睡觉        |
| 4 | 星光兽 | ⭐     | 稀有  | 300  | 只在深夜出现         |
| 5 | 雷电犬 | ⚡     | 稀有  | 500  | 行动迅捷如闪电        |
| 6 | 冰晶狐 | ❄️    | 稀有  | 700  | 高傲的冰雪贵族        |
| 7 | 暗影龙 | 🌑    | 传说  | 1200 | 拥有毁灭力量         |
| 8 | 神圣鹿 | 🦌    | 传说  | 1800 | 森林的守护神         |
| 9 | 创世神 | 🌌    | 传说  | 2500 | 集齐所有图鉴后的终极奖励   |

稀有度分布：**普通 ×3、稀有 ×3、传说 ×3**。

---

## 六、数据库结构（`db`）

```
pets             宠物图鉴（9 条预置，只读）
  ├ id (PK)  name  emoji  rarity  unlock_cost  description

user_collection  用户的收藏/解锁记录（解锁 = 插入一行）
  ├ pet_id (PK, FK→pets, ON DELETE CASCADE)  unlocked_at

focus_records    专注记录（积分来源）
  ├ id (PK)  focus_date  duration_minutes

wardrobe_purchases  小猫咪妆扮购买记录（v3 新增）
  ├ item_id (PK)  item_type  cost
```

- 派生积分：`SUM(focus_records.duration_minutes) − SUM(已解锁 pets.unlock_cost) − SUM(wardrobe_purchases.cost)`
- 数据库版本 `version = 3`：
  - `MIGRATION_1_2` —— 原地 UPDATE 宠物数据，保留用户专注记录。
  - `MIGRATION_2_3` —— 新建 `wardrobe_purchases` 表（猫咪妆扮消费）。
- 妆扮消费进 Room 而非 SharedPreferences，是为了让「可用积分」这个 LiveData 在买完皮肤后能自动刷新。

---

## 七、测试分支调试（test 分支专用）

`test` 分支内置一套调试工具（`debug/DebugHelper.kt`），**仅用于开发调试，合并回主分支时应整体删除**。

- **自动满配**：测试版首次启动会一次性解锁全部 9 只宠物（SharedPreferences 守卫，仅一次），让你直接浏览完整图鉴。
- **图鉴页按钮「🛠 一键满配 / 重置进度」**：在满配与「仅首只」之间一键切换，方便调试解锁流程与进度条。
- **首页按钮「🎭 调试心情」**：循环切换 `NORMAL → SICK → GLOW`，实时验证三种心情的渲染表现。

> 由于应用数据按 `applicationId` 持久化，从主分支装的旧库（仅 1 只解锁）在首次跑测试版时会被自动满配覆盖。若要重新测试「未解锁」初始态，点图鉴页的「重置进度」即可。

---

## 八、项目结构

```
app/src/main/java/com/example/focuspets/
├── MainActivity.kt              # 入口 + 底部导航 + test 分支自动满配
├── db/                          # Room：实体 / DAO / 数据库 / 预置数据
│   ├── AppDatabase.kt           # 版本、迁移、Seed 回调
│   ├── InitialDataProvider.kt   # 9 只预置宠物 + 首只赠送
│   ├── PetRepository.kt         # 解锁/积分/达标查询
│   ├── entity/                  # PetEntity / UserCollectionEntity / FocusRecordEntity
│   └── dao/                     # PetDao / UserCollectionDao / FocusRecordDao
├── model/
│   ├── PetCareState.kt          # 心情状态机（SharedPreferences）
│   └── CatWardrobe.kt           # 猫咪妆扮：价格/目录/装备状态/立绘路径
├── service/
│   └── FocusService.kt          # 前台计时服务
├── ui/
│   ├── home/                    # 首页宠物互动（含猫咪立绘 + 行为动画 + 音效）
│   ├── focus/                   # 专注计时页
│   │   └── FocusProgressBar.kt  # 自定义进度条：橘色已走 + 白色未走 + 黑色竖线
│   ├── collection/              # 图鉴网格 + 详情弹窗
│   └── wardrobe/
│       └── CatWardrobeBottomSheet.kt  # 妆扮面板（购买 / 穿戴）
└── debug/
    └── DebugHelper.kt           # test 分支调试工具
```

配套资源：

```
app/src/main/assets/cat/    # 21 张猫咪立绘（已去水印），命名：{颜色}[_dress][_{王冠}].jpg
app/src/main/res/raw/       # meow.wav：合成的小声喵叫
process_cat_assets.py       # 一次性脚本：去水印 + 重命名 + 生成 meow.wav
```

---

## 九、构建与运行

1. 用 Android Studio 打开本工程，连接已开启「开发者选项 → USB 调试」的手机。
2. 首次会下载依赖 + 构建 APK（耐心等几分钟，依赖已配国内镜像）。
3. 点 **Run（▶）** 安装并启动；后续改动点 Run（或 ⚡ Apply Changes 热更新）即可在手机看到新版。
4. 数据库结构变更时，需提升 `AppDatabase.version` 并编写对应 `Migration`，否则旧库会崩溃。

详见团队仓库提交记录与对话历史中的环境排障笔记。

```
```
