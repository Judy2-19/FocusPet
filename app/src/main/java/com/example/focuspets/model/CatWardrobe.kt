package com.example.focuspets.model

import android.content.Context

/**
 * Single source for cat wardrobe prices, catalog, equipped-state preferences, and artwork resolution.
 * Room wardrobe_purchases stores spending; SharedPreferences stores only what is worn. Both use
 * itemId.
 */
object CatWardrobe {

    /** Fixed cat ID in the pets table. */
    const val CAT_PET_ID = 1

    private const val PREFS = "cat_wardrobe"
    private const val KEY_COLOR = "equipped_color"
    private const val KEY_DRESS = "equipped_dress"
    private const val KEY_CROWN = "equipped_crown"

    // ---- Prices ----
    const val COLOR_COST = 50
    const val DRESS_COST = 120
    const val CROWN_COST = 180

    const val DEFAULT_COLOR = "gray"

    val COLORS = listOf("gray", "blue", "pink", "yellow", "white", "black")
    val CROWNS = listOf("blue_crown", "pink_crown")

    /**
     * Available crown artwork per color. Doubao-generated assets do not cover every combination; expose
     * only existing variants to avoid missing images.
     */
    val CROWN_OPTIONS_BY_COLOR: Map<String, List<String>> = mapOf(
        "gray" to listOf("blue_crown", "pink_crown"),
        "blue" to listOf("blue_crown", "pink_crown"),
        "pink" to listOf("blue_crown", "pink_crown"),
        "yellow" to listOf("blue_crown", "pink_crown"),
        "black" to listOf("blue_crown", "pink_crown"),
        "white" to listOf("blue_crown", "pink_crown")
    )

    // ---- itemId convention: matches Room wardrobe_purchases.item_id ----
    fun colorItemId(color: String) = "color_$color"
    const val DRESS_ITEM_ID = "dress"
    fun crownItemId(crown: String) = "crown_$crown"

    fun colorLabel(color: String) = when (color) {
        "gray" -> com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_084)
        "blue" -> com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_038)
        "pink" -> com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_125)
        "yellow" -> com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_179)
        "white" -> com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_177)
        "black" -> com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_036)
        else -> color
    }

    fun crownLabel(crown: String) = when (crown) {
        "blue_crown" -> com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_037)
        "pink_crown" -> com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_124)
        else -> crown
    }

    // ---- Equipped-state access ----

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

    // ---- Artwork paths ----

    /**
     * Crowns require a dress because crown assets include the dress. Ignore the crown when no dress is
     * equipped.
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
     * Reset equipped preferences to gray with no dress/crown together with Room purchase records.
     */
    fun clearEquipped(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
