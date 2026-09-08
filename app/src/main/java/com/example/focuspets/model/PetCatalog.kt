package com.example.focuspets.model

/**
 * 宠物目录（9 只固定编号，只读）：把宠物 id 映射到展示信息。
 * - 猫(1)/狗(2) 走 assets 立绘，由 CloudSyncManager 上报当前装扮路径；
 * - 其余 3~9 号没有立绘，只用 emoji 兜底显示在排行榜。
 * 数据以 AppDatabase 迁移后的实际展示为准（💧🔥🌿⭐⚡❄️🌑🦌🌌）。
 */
object PetCatalog {

    /** id → emoji（兜底用，仅 3~9 号走这里） */
    private val EMOJI = mapOf(
        1 to "\uD83D\uDC38",   // 🐱 猫（立绘优先，这里仅兜底）
        2 to "\uD83D\uDC36",   // 🐶 狗（立绘优先，这里仅兜底）
        3 to "\uD83C\uDF37",   // 🌿 木灵
        4 to "⭐",        // 星光兽
        5 to "⚡",        // 雷电犬
        6 to "❄️",        // 冰晶狐
        7 to "\uD83C\uDF91",   // 🌑 暗影龙
        8 to "\uD83E\uDD8C",   // 🦌 神圣鹿
        9 to "\uD83C\uDF0C"    // 🌌 创世神
    )

    /** 有 assets 立绘的宠物（猫/狗） */
    fun hasArtwork(id: Int): Boolean = id == 1 || id == 2

    /** emoji 兜底（用于无立绘的宠物；立绘加载失败也退回这个） */
    fun emoji(id: Int?): String = if (id == null) "\uD83D\uDC3E" else (EMOJI[id] ?: "\uD83D\uDC3E")
}
