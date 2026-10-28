package com.example.focuspets.cloud.model

/** Anonymous registration response. */
data class AnonResponse(val uid: String, val displayName: String)

/** Complete cloud user object returned by GET. */
data class CloudUser(
    val uid: String,
    val displayName: String,
    val totalPoints: Int,
    val collection: List<Int>,
    val wardrobe: List<String>,
    val mood: String,
    val updatedAt: Long
)

/** Upload/merge payload, excluding uid and updatedAt. */
data class CloudUserPayload(
    val displayName: String,
    val totalPoints: Int,
    val collection: List<Int>,
    val wardrobe: List<String>,
    val mood: String,
    /** Displayed pet ID (1-9), used as the leaderboard avatar fallback. */
    val currentPetId: Int = 1,
    /** Current pet artwork path within assets; includes cat/dog outfits, otherwise empty. */
    val petAvatar: String = ""
)

/** Leaderboard entry. */
data class LeaderboardEntry(
    val rank: Int,
    val uid: String,
    val displayName: String,
    val totalPoints: Int,
    /** Current artwork path within assets, e.g. cat/pink_dress_blue_crown.png; empty for pets other than cats/dogs. */
    val petAvatar: String? = null,
    /** Current pet ID (1-9); pets without artwork use an emoji fallback. */
    val petId: Int? = null
)

/** Current user's rank information. */
data class MeRank(val rank: Int?, val totalPoints: Int)

/** Leaderboard response. */
data class LeaderboardResponse(val list: List<LeaderboardEntry>, val me: MeRank?)
