package com.example.focuspets.model

/**
 * 成就徽章：统计页「连续打卡成就徽章」的展示单元。
 *
 * @param id       稳定标识（用于后续扩展持久化解锁态）
 * @param emoji    解锁后展示的图标
 * @param title    徽章名称
 * @param desc     解锁后展示的描述
 * @param target   解锁所需阈值（连续天数或专注次数）
 * @param progress 当前进度（已夹紧到 [0, target]）
 * @param unlocked 当前是否已达阈值
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
