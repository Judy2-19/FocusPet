package com.example.focuspets.model

/**
 * Read-only catalog mapping nine fixed pet IDs to display data. Cats/dogs use assets and
 * CloudSyncManager uploads their current outfit paths; IDs 3-9 use leaderboard emoji fallbacks.
 * Display data follows AppDatabase migrations.
 */
object PetCatalog {

    /** ID-to-emoji fallback; normally used by IDs 3-9. */
    private val EMOJI = mapOf(
        1 to "\uD83D\uDC38",   // Cat: prefer artwork, with emoji as fallback.
        2 to "\uD83D\uDC36",   // Dog: prefer artwork, with emoji as fallback.
        3 to "\uD83C\uDF37",   // Wood spirit.
        4 to "⭐",        // Starlight beast.
        5 to "⚡",        // Thunder dog.
        6 to "❄️",        // Ice fox.
        7 to "\uD83C\uDF91",   // Shadow dragon.
        8 to "\uD83E\uDD8C",   // Sacred deer.
        9 to "\uD83C\uDF0C"    // Creator deity.
    )

    /** Pets with asset artwork: cat and dog. */
    fun hasArtwork(id: Int): Boolean = id == 1 || id == 2

    /** Emoji fallback for pets without artwork or failed image loading. */
    fun emoji(id: Int?): String = if (id == null) "\uD83D\uDC3E" else (EMOJI[id] ?: "\uD83D\uDC3E")
}
