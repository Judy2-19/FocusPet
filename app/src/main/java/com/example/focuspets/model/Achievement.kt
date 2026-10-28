package com.example.focuspets.model

/**
 * Achievement badge displayed on Statistics. id: stable identifier for future persistent unlocks;
 * emoji: unlocked icon; title: name; desc: unlocked description; target: required streak/session
 * count; progress: clamped to [0, target]; unlocked: whether the threshold is met.
 */
data class AchievementBadge(
    val id: String,
    val emoji: String,
    val title: String,
    val desc: String,
    val target: Int,
    val progress: Int,
    val unlocked: Boolean
)
