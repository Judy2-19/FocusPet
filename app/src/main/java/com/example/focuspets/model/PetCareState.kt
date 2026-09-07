package com.example.focuspets.model

import android.content.Context

/** 宠物心情三态：普通 / 生病（专注失败）/ 发光（连续成功 ≥ 3 次） */
enum class PetMood { NORMAL, SICK, GLOW }

/**
 * 宠物健康与心情状态（阶段三：饥饿 → 阶段四：升级为心情状态机）：
 * - 专注失败 → SICK（不扣积分，切表情/流泪状态，进度不受影响）
 * - 连续成功 3 次 → GLOW（开心/发光）
 * - 再次失败后重新从 0 计数
 * 单机小数据，SharedPreferences 足够，不值得加 Room 字段/迁移。
 */
object PetCareState {

    private const val PREFS = "pet_care"
    private const val KEY_HUNGRY = "pet_hungry"
    private const val KEY_CONSECUTIVE_SUCCESS = "consecutive_success"
    private const val GLOW_THRESHOLD = 3

    fun isHungry(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HUNGRY, false)

    fun getConsecutiveSuccess(context: Context): Int =
        prefs(context).getInt(KEY_CONSECUTIVE_SUCCESS, 0)

    /** 专注成功：连续计数 +1，恢复健康 */
    fun recordSuccess(context: Context) {
        prefs(context).edit()
            .putInt(KEY_CONSECUTIVE_SUCCESS, getConsecutiveSuccess(context) + 1)
            .putBoolean(KEY_HUNGRY, false)
            .apply()
    }

    /** 专注失败：连续计数清零，宠物进入生病状态 */
    fun recordFailure(context: Context) {
        prefs(context).edit()
            .putInt(KEY_CONSECUTIVE_SUCCESS, 0)
            .putBoolean(KEY_HUNGRY, true)
            .apply()
    }

    fun getMood(context: Context): PetMood = when {
        isHungry(context) -> PetMood.SICK
        getConsecutiveSuccess(context) >= GLOW_THRESHOLD -> PetMood.GLOW
        else -> PetMood.NORMAL
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
