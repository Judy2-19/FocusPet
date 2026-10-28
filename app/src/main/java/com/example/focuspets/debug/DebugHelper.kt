package com.example.focuspets.debug

import android.content.Context
import com.example.focuspets.db.AppDatabase
import com.example.focuspets.db.InitialDataProvider
import com.example.focuspets.db.entity.FocusRecordEntity
import com.example.focuspets.db.entity.UserCollectionEntity
import com.example.focuspets.model.PetCareState
import com.example.focuspets.model.PetMood
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Debug tools intended for the test branch: unlock all nine pets and grant a large balance to inspect
 * rarity, moods, progress, and wardrobe purchases without earning points manually. The original
 * balance model derives points from focus duration minus pet and outfit costs; marked grant records
 * can be cleared. Intended to stay out of normal main behavior.
 */
object DebugHelper {

    // Guard background debug database writes with an exception handler so Room query failures do not kill the process.
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, t ->
            Log.e("DebugHelper", "debug op failed (non-fatal): ${t.message}", t)
        }
    )

    private const val PREFS = "debug_prefs"
    // Use a new key because older versions already set full_unlock_done; this allows the million-point top-up to run once again.
    private const val KEY_SETUP_DONE = "test_setup_v2"

    /** Target test balance: one million points. */
    const val TEST_POINTS = 1_000_000

    /** Date marker used to identify and delete injected test records precisely. */
    private const val TEST_GRANT_MARKER = "test-grant"

    /**
     * On the test build's first launch, unlock all pets and grant test points once. Inject points with
     * duration=0 so actual focus statistics stay unchanged. Additional top-ups remain available from
     * Collection.
     */
    fun ensureTestSetup(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_SETUP_DONE, false)) {
            scope.launch {
                unlockAllSync(context)
                topUpPointsSync(context)
            }
            prefs.edit().putBoolean(KEY_SETUP_DONE, true).apply()
        }
    }

    /** Unlock all nine pets idempotently. */
    fun unlockAll(context: Context) {
        scope.launch { unlockAllSync(context) }
    }

    /** Reset progress to the initial gift pet and remove injected points. */
    fun resetToInitial(context: Context) {
        scope.launch { resetToInitialSync(context) }
    }

    /** Top up available points to one million; grant only the difference, allowing repeated taps. */
    fun grantPoints(context: Context) {
        scope.launch { topUpPointsSync(context) }
    }

    /**
     * Cycle NORMAL → SICK → GLOW → NORMAL and return the new state for debug button feedback.
     */
    fun cycleMood(context: Context): PetMood {
        val next = when (PetCareState.getMood(context)) {
            PetMood.NORMAL -> PetMood.SICK
            PetMood.SICK -> PetMood.GLOW
            PetMood.GLOW -> PetMood.NORMAL
        }
        PetCareState.debugForceMood(context, next)
        return next
    }

    // ---------------- Internal implementation ----------------

    private suspend fun unlockAllSync(context: Context) {
        val db = AppDatabase.getInstance(context.applicationContext)
        db.userCollectionDao().unlockPets(
            InitialDataProvider.PRESET_PETS.map { UserCollectionEntity(petId = it.id) }
        )
    }

    private suspend fun resetToInitialSync(context: Context) {
        val db = AppDatabase.getInstance(context.applicationContext)
        db.userCollectionDao().deleteExcept(InitialDataProvider.FREE_PET_ID)
        db.focusRecordDao().deleteTestGrants(TEST_GRANT_MARKER)
    }

    /**
     * Read the available balance and grant only the difference up to TEST_POINTS. Inject through points
     * with duration=0 to avoid falsifying total focus time.
     */
    private suspend fun topUpPointsSync(context: Context) {
        val db = AppDatabase.getInstance(context.applicationContext)
        val current = db.userCollectionDao().getAvailablePointsOnce()
        if (current < TEST_POINTS) {
            db.focusRecordDao().insert(
                FocusRecordEntity(
                    focusDate = TEST_GRANT_MARKER,
                    content = com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_147),
                    durationMinutes = 0,
                    points = TEST_POINTS - current
                )
            )
        }
    }
}
