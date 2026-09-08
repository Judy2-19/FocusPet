package com.example.focuspets.model

import android.content.Context

/**
 * 小猫咪「妆扮」系统的单一数据源：
 * - 价格/目录常量
 * - 当前装备状态（SharedPreferences，纯 UI 偏好，不影响积分）
 * - 立绘资源路径解析
 *
 * 注意：购买记录（影响积分的部分）在 Room 的 wardrobe_purchases 表里，
 * 这里只存"穿哪件"，两者通过 itemId 关联。
 */
object CatWardrobe {

    /** 小猫咪在 pets 表中的固定 id */
    const val CAT_PET_ID = 1

    private const val PREFS = "cat_wardrobe"
    private const val KEY_COLOR = "equipped_color"
    private const val KEY_DRESS = "equipped_dress"
    private const val KEY_CROWN = "equipped_crown"

    // ---- 价格 ----
    const val COLOR_COST = 50
    const val DRESS_COST = 120
    const val CROWN_COST = 180

    const val DEFAULT_COLOR = "gray"

    val COLORS = listOf("gray", "blue", "pink", "yellow", "white", "black")
    val CROWNS = listOf("blue_crown", "pink_crown")

    /**
     * 每个颜色实际拥有哪些王冠立绘。
     * 素材由豆包生成，并非每种颜色都配齐全套，只提供存在的组合，避免加载不到图。
     */
    val CROWN_OPTIONS_BY_COLOR: Map<String, List<String>> = mapOf(
        "gray" to listOf("blue_crown", "pink_crown"),
        "blue" to listOf("blue_crown", "pink_crown"),
        "pink" to listOf("blue_crown", "pink_crown"),
        "yellow" to listOf("blue_crown", "pink_crown"),
        "black" to listOf("blue_crown", "pink_crown"),
        "white" to listOf("blue_crown", "pink_crown")
    )

    // ---- itemId 约定（与 Room wardrobe_purchases.item_id 一致） ----
    fun colorItemId(color: String) = "color_$color"
    const val DRESS_ITEM_ID = "dress"
    fun crownItemId(crown: String) = "crown_$crown"

    fun colorLabel(color: String) = when (color) {
        "gray" -> "灰色"
        "blue" -> "蓝色"
        "pink" -> "粉色"
        "yellow" -> "黄色"
        "white" -> "白色"
        "black" -> "黑色"
        else -> color
    }

    fun crownLabel(crown: String) = when (crown) {
        "blue_crown" -> "蓝色王冠"
        "pink_crown" -> "粉色王冠"
        else -> crown
    }

    // ---- 装备状态读写 ----

    fun getEquippedColor(context: Context): String =
        prefs(context).getString(KEY_COLOR, DEFAULT_COLOR) ?: DEFAULT_COLOR

    fun setEquippedColor(context: Context, color: String) {
        prefs(context).edit().putString(KEY_COLOR, color).apply()
    }

    fun isDressEquipped(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DRESS, false)

    fun setDressEquipped(context: Context, equipped: Boolean) {
        prefs(context).edit().putBoolean(KEY_DRESS, equipped).apply()
    }

    fun getEquippedCrown(context: Context): String? =
        prefs(context).getString(KEY_CROWN, null)

    fun setEquippedCrown(context: Context, crown: String?) {
        prefs(context).edit().apply {
            if (crown == null) remove(KEY_CROWN) else putString(KEY_CROWN, crown)
            apply()
        }
    }

    // ---- 立绘路径 ----

    /**
     * 组合规则：皇冠必须搭配裙子（素材里皇冠图都是"颜色-红裙-王冠"），
     * 所以解析时若未穿裙子，皇冠自动忽略。
     */
    fun assetPathFor(color: String, dress: Boolean, crown: String?): String = when {
        dress && !crown.isNullOrBlank() -> "cat/${color}_dress_$crown.png"
        dress -> "cat/${color}_dress.png"
        else -> "cat/$color.png"
    }

    fun assetPath(context: Context): String = assetPathFor(
        getEquippedColor(context),
        isDressEquipped(context),
        getEquippedCrown(context)
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * 重置进度时清空「穿在身上」的装扮状态（SharedPreferences 整体清空，回到默认
     * 灰色 / 无裙无冠）。与 Room 的购买记录（wardrobe_purchases）一起复位。
     */
    fun clearEquipped(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
