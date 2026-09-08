package com.example.focuspets.model

import android.content.Context

/**
 * 小狗「妆扮」系统的单一数据源（镜像 CatWardrobe）：
 * - 价格 / 目录常量
 * - 当前装备状态（SharedPreferences，纯 UI 偏好，不影响积分）
 * - 立绘资源路径解析（基于 assets/dog/ 实际生成的文件，自动兼容各颜色配齐程度不同的素材）
 *
 * 与小猫的区别：
 * 1) 多了一个「尾巴长度」维度（长尾 / 短尾），免费自选；
 * 2) 配饰链为 领带 → 西服/礼服 → 学士帽（长尾穿「礼服=dress」，短尾穿「西服=suit」）；
 * 3) 颜色把猫的「蓝」换成了「彩色」。
 *
 * 购买记录（影响积分的部分）在 Room 的 wardrobe_purchases 表里，这里只存"穿哪件"，通过 itemId 关联。
 */
object DogWardrobe {

    /** 小狗在 pets 表中的固定 id */
    const val DOG_PET_ID = 2

    private const val PREFS = "dog_wardrobe"
    private const val KEY_TAIL = "equipped_tail"
    private const val KEY_COLOR = "equipped_color"
    private const val KEY_TIE = "equipped_tie"
    private const val KEY_OUTFIT = "equipped_outfit"
    private const val KEY_CAP = "equipped_cap"

    // ---- 价格（参考小猫：裙 120、冠 180；狗的不更便宜也不过分贵）----
    const val TAIL_COST = 0            // 尾巴免费自选
    const val COLOR_COST = 50
    const val TIE_COST = 120
    const val OUTFIT_COST = 150
    const val CAP_COST = 180

    const val DEFAULT_TAIL = "long"
    const val DEFAULT_COLOR = "gray"

    val TAILS = listOf("long", "short")
    val COLORS = listOf("gray", "white", "pink", "yellow", "black", "colorful")

    // ---- itemId 约定（与 Room wardrobe_purchases.item_id 一致）----
    fun colorItemId(color: String) = "dog_color_$color"
    const val TIE_ITEM_ID = "dog_tie"
    const val OUTFIT_ITEM_ID = "dog_outfit"
    const val CAP_ITEM_ID = "dog_cap"

    fun colorLabel(color: String) = when (color) {
        "gray" -> "灰色"
        "white" -> "白色"
        "pink" -> "粉色"
        "yellow" -> "黄色"
        "black" -> "黑色"
        "colorful" -> "彩色"
        else -> color
    }

    fun tailLabel(tail: String) = when (tail) {
        "long" -> "长尾巴"
        "short" -> "短尾巴"
        else -> tail
    }

    // 配饰配色 token：文件名中明确写出颜色，便于以后扩展（如 blue_tie / 其他裙色）
    private const val TIE_TOKEN = "red_tie"               // 红领带
    private const val CAP_TOKEN = "academic_cap"          // 学院帽
    private const val SHORT_OUTFIT_TOKEN = "black_suit"   // 短尾·黑西服
    private const val LONG_OUTFIT_TOKEN = "red_dress"     // 长尾·红礼裙

    /** 长尾穿礼服、短尾穿西服（返回带颜色的 token） */
    private fun outfitToken(tail: String) =
        if (tail == "long") LONG_OUTFIT_TOKEN else SHORT_OUTFIT_TOKEN

    // ---- 装备状态读写 ----

    fun getEquippedTail(context: Context): String =
        prefs(context).getString(KEY_TAIL, DEFAULT_TAIL) ?: DEFAULT_TAIL

    fun setEquippedTail(context: Context, tail: String) {
        prefs(context).edit().putString(KEY_TAIL, tail).apply()
    }

    fun getEquippedColor(context: Context): String =
        prefs(context).getString(KEY_COLOR, DEFAULT_COLOR) ?: DEFAULT_COLOR

    fun setEquippedColor(context: Context, color: String) {
        prefs(context).edit().putString(KEY_COLOR, color).apply()
    }

    fun isTieEquipped(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TIE, false)

    fun setTieEquipped(context: Context, equipped: Boolean) {
        prefs(context).edit().putBoolean(KEY_TIE, equipped).apply()
    }

    fun isOutfitEquipped(context: Context): Boolean =
        prefs(context).getBoolean(KEY_OUTFIT, false)

    fun setOutfitEquipped(context: Context, equipped: Boolean) {
        prefs(context).edit().putBoolean(KEY_OUTFIT, equipped).apply()
    }

    fun isCapEquipped(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CAP, false)

    fun setCapEquipped(context: Context, equipped: Boolean) {
        prefs(context).edit().putBoolean(KEY_CAP, equipped).apply()
    }

    /**
     * 换颜色 / 尾巴后，若某些已穿配饰在当前组合下没有对应立绘（素材未生成），
     * 自动摘掉，避免"买了却看不见"的脏状态。
     */
    fun pruneInvalid(context: Context) {
        val tail = getEquippedTail(context)
        val color = getEquippedColor(context)
        if (isCapEquipped(context) && !capChangesImage(context, tail, color)) {
            setCapEquipped(context, false)
        }
        if (isOutfitEquipped(context) && !outfitChangesImage(context, tail, color)) {
            setOutfitEquipped(context, false)
        }
    }

    // ---- 立绘资源（assets/dog/ 实际文件名集合，惰性缓存）----

    @Volatile
    private var assetSet: Set<String>? = null

    private fun assetSet(context: Context): Set<String> {
        if (assetSet == null) {
            val files = try {
                context.assets.list("dog")?.toSet().orEmpty()
            } catch (_: Exception) {
                emptySet()
            }
            assetSet = files
        }
        return assetSet!!
    }

    /**
     * 给定各维度，解析最终立绘路径；若精确组合不存在，逐级回退（摘帽 → 脱衣 → 摘领带），
     * 最后回退到同尾巴灰狗 / 长尾灰狗，保证永远能加载到一张图。
     */
    fun assetPathFor(
        context: Context,
        tail: String,
        color: String,
        tie: Boolean,
        outfit: Boolean,
        cap: Boolean
    ): String {
        val set = assetSet(context)
        val ot = outfitToken(tail)
        val base = listOf(tail, color)
        // 若「纯服饰（不含帽）」没有独立立绘（只有"服饰+帽"同框素材），
        // 则「穿西服/礼服」应自动带帽，避免出现"只有领带"的脏状态。
        val pureOutfit = (base + listOf(TIE_TOKEN, ot)).joinToString("_") + ".png"
        val needsCapForSuit = outfit && pureOutfit !in set
        val useCap = cap || needsCapForSuit
        // 从最完整到最精简逐级回退：摘帽 → 脱衣 → 摘领带 → 默认
        val candidates = listOf(
            buildList { addAll(base); if (tie) add(TIE_TOKEN); if (outfit) add(ot); if (useCap) add(CAP_TOKEN) },
            buildList { addAll(base); if (tie) add(TIE_TOKEN); if (outfit) add(ot) },
            buildList { addAll(base); if (tie) add(TIE_TOKEN) },
            base
        )
        for (c in candidates) {
            val name = c.joinToString("_") + ".png"
            if (name in set) return "dog/$name"
        }
        if ("${tail}_gray.png" in set) return "dog/${tail}_gray.png"
        return "dog/long_gray.png"
    }

    /** 当前尾巴/颜色下，是否存在「纯西服/礼服（不含帽）」的独立立绘 */
    fun hasIndependentSuit(context: Context, tail: String, color: String): Boolean {
        val ot = outfitToken(tail)
        return "${tail}_${color}_${TIE_TOKEN}_$ot.png" in assetSet(context)
    }

    /** 当前装备组合对应的立绘路径 */
    fun assetPath(context: Context): String = assetPathFor(
        context,
        getEquippedTail(context),
        getEquippedColor(context),
        isTieEquipped(context),
        isOutfitEquipped(context),
        isCapEquipped(context)
    )

    /**
     * 在当前尾巴 / 颜色下，穿上「西服/礼服」是否会真正改变立绘（用于 UI 启用 / 禁用）。
     * 例如彩色狗、粉色短尾没有对应服饰立绘，会回退成"只有领带"，于是视为不可用。
     */
    fun outfitChangesImage(context: Context, tail: String, color: String): Boolean {
        val withOutfit = assetPathFor(context, tail, color, tie = true, outfit = true, cap = false)
        val without = assetPathFor(context, tail, color, tie = true, outfit = false, cap = false)
        return withOutfit != without
    }

    /** 在当前尾巴 / 颜色下，戴上「学士帽」是否会真正改变立绘 */
    fun capChangesImage(context: Context, tail: String, color: String): Boolean {
        val withCap = assetPathFor(context, tail, color, tie = true, outfit = true, cap = true)
        val without = assetPathFor(context, tail, color, tie = true, outfit = true, cap = false)
        return withCap != without
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * 重置进度时清空「穿在身上」的装扮状态（SharedPreferences 整体清空，回到默认
     * 长尾 / 灰色 / 无配饰）。与 Room 的购买记录（wardrobe_purchases）一起复位，
     * 否则会出现"已重置但宠物还穿着已不存在的装扮"的脏状态。
     */
    fun clearEquipped(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
