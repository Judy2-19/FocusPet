package com.example.focuspets.model

import android.content.Context

/** Pet moods: normal, sick after focus failure, glowing after at least three consecutive successes. */
enum class PetMood { NORMAL, SICK, GLOW }

/**
 * Pet health/mood state evolved from hunger to a state machine: failure sets SICK without deducting
 * points or progress; three consecutive successes set GLOW; failure resets the streak.
 * SharedPreferences suffices for this small local state without Room migrations.
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

    /** Success increments the streak and restores health. */
    fun recordSuccess(context: Context) {
        prefs(context).edit()
            .putInt(KEY_CONSECUTIVE_SUCCESS, getConsecutiveSuccess(context) + 1)
            .putBoolean(KEY_HUNGRY, false)
            .apply()
    }

    /** Failure clears the streak and makes the pet sick. */
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

    /** Apply cloud mood using the same logic as the debug override. */
    fun setMood(context: Context, mood: PetMood) = debugForceMood(context, mood)

    /**
     * Debug/test branch mood override: SICK sets hungry=true and streak=0; GLOW sets hungry=false and
     * streak=threshold; NORMAL sets hungry=false and streak=0.
     */
    fun debugForceMood(context: Context, mood: PetMood) {
        prefs(context).edit().apply {
            when (mood) {
                PetMood.SICK -> {
                    putBoolean(KEY_HUNGRY, true)
                    putInt(KEY_CONSECUTIVE_SUCCESS, 0)
                }
                PetMood.GLOW -> {
                    putBoolean(KEY_HUNGRY, false)
                    putInt(KEY_CONSECUTIVE_SUCCESS, GLOW_THRESHOLD)
                }
                PetMood.NORMAL -> {
                    putBoolean(KEY_HUNGRY, false)
                    putInt(KEY_CONSECUTIVE_SUCCESS, 0)
                }
            }
            apply()
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
