package com.example.focuspets.model

import android.content.Context

/**
 * Dog wardrobe source, mirroring CatWardrobe: prices/catalog, equipped preferences, and paths resolved
 * from available assets/dog files. Tail length is free; accessories follow tie → outfit → graduation
 * cap. Long tails use dresses, short tails use suits; multicolor replaces the cat's blue. Room stores
 * purchases and spending; preferences store equipment linked by itemId.
 */
object DogWardrobe {

    /** Fixed dog ID in the pets table. */
    const val DOG_PET_ID = 2

    private const val PREFS = "dog_wardrobe"
    private const val KEY_TAIL = "equipped_tail"
    private const val KEY_COLOR = "equipped_color"
    private const val KEY_TIE = "equipped_tie"
    private const val KEY_OUTFIT = "equipped_outfit"
    private const val KEY_CAP = "equipped_cap"

    // ---- Prices: comparable to cat dress 120/crown 180; neither cheaper nor excessively expensive ----
    const val TAIL_COST = 0            // Tail selection is free.
    const val COLOR_COST = 50
    const val TIE_COST = 120
    const val OUTFIT_COST = 150
    const val CAP_COST = 180

    const val DEFAULT_TAIL = "long"
    const val DEFAULT_COLOR = "gray"

    val TAILS = listOf("long", "short")
    val COLORS = listOf("gray", "white", "pink", "yellow", "black", "colorful")

    // ---- itemId convention: matches Room wardrobe_purchases.item_id ----
    fun colorItemId(color: String) = "dog_color_$color"
    const val TIE_ITEM_ID = "dog_tie"
    const val OUTFIT_ITEM_ID = "dog_outfit"
    const val CAP_ITEM_ID = "dog_cap"

    fun colorLabel(color: String) = when (color) {
        "gray" -> com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_084)
        "white" -> com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_177)
        "pink" -> com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_125)
        "yellow" -> com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_179)
        "black" -> com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_036)
        "colorful" -> com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_105)
        else -> color
    }

    fun tailLabel(tail: String) = when (tail) {
        "long" -> com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_098)
        "short" -> com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_138)
        else -> tail
    }

    // Accessory color tokens explicitly appear in filenames for future variants such as blue_tie or other dress colors.
    private const val TIE_TOKEN = "red_tie"               // Red tie.
    private const val CAP_TOKEN = "academic_cap"          // Graduation cap.
    private const val SHORT_OUTFIT_TOKEN = "black_suit"   // Short tail, black suit.
    private const val LONG_OUTFIT_TOKEN = "red_dress"     // Long tail, red dress.

    /** Long tails wear dresses; short tails wear suits. Return a color-qualified token. */
    private fun outfitToken(tail: String) =
        if (tail == "long") LONG_OUTFIT_TOKEN else SHORT_OUTFIT_TOKEN

    // ---- Equipped-state access ----

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
     * After changing color or tail, remove equipped accessories missing artwork for that combination,
     * avoiding invisible purchased outfits.
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

    // ---- Artwork: lazily cached filenames under assets/dog/ ----

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
     * Resolve the final path, falling back by removing cap, outfit, then tie. Finally use a gray dog with
     * the same tail or long tail so an image always loads.
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
        // If there is no standalone outfit image and only an outfit-with-cap asset exists,
        // automatically include the cap when equipping the outfit to avoid falling back to a tie-only image.
        val pureOutfit = (base + listOf(TIE_TOKEN, ot)).joinToString("_") + ".png"
        val needsCapForSuit = outfit && pureOutfit !in set
        val useCap = cap || needsCapForSuit
        // Fallback from most complete to simplest: remove cap, outfit, tie, then use the default.
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

    /** Whether this tail/color has standalone outfit artwork without a cap. */
    fun hasIndependentSuit(context: Context, tail: String, color: String): Boolean {
        val ot = outfitToken(tail)
        return "${tail}_${color}_${TIE_TOKEN}_$ot.png" in assetSet(context)
    }

    /** Artwork path for the current equipment combination. */
    fun assetPath(context: Context): String = assetPathFor(
        context,
        getEquippedTail(context),
        getEquippedColor(context),
        isTieEquipped(context),
        isOutfitEquipped(context),
        isCapEquipped(context)
    )

    /**
     * Whether equipping an outfit actually changes the image for this tail/color, for UI enablement.
     * Multicolor and pink short-tail variants without outfit art fall back to tie-only and are
     * unavailable.
     */
    fun outfitChangesImage(context: Context, tail: String, color: String): Boolean {
        val withOutfit = assetPathFor(context, tail, color, tie = true, outfit = true, cap = false)
        val without = assetPathFor(context, tail, color, tie = true, outfit = false, cap = false)
        return withOutfit != without
    }

    /** Whether equipping a graduation cap changes the image for this tail/color. */
    fun capChangesImage(context: Context, tail: String, color: String): Boolean {
        val withCap = assetPathFor(context, tail, color, tie = true, outfit = true, cap = true)
        val without = assetPathFor(context, tail, color, tie = true, outfit = true, cap = false)
        return withCap != without
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Reset equipped preferences to long tail, gray, and no accessories alongside Room purchase records,
     * preventing stale outfits after progress reset.
     */
    fun clearEquipped(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
