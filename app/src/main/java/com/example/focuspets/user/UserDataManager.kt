package com.example.focuspets.user

import android.content.Context
import androidx.room.withTransaction
import com.example.focuspets.cloud.ApiClient
import com.example.focuspets.cloud.AuthManager
import com.example.focuspets.db.AppDatabase
import com.example.focuspets.model.CatWardrobe
import com.example.focuspets.model.DogWardrobe
import com.example.focuspets.db.InitialDataProvider
import com.example.focuspets.model.PetCareState
import com.example.focuspets.model.PetMood
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineExceptionHandler
import android.util.Log

/**
 * User data management for progress reset and sign-out. Reset must clear the cloud identity after
 * local tables; otherwise ensureLogin would fetch the same UID's mirror and restore old data. Use
 * AuthManager.logout to sync under a new anonymous identity. Writes run on IO; onDone handles UI
 * completion.
 */
object UserDataManager {

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, t ->
            Log.e("UserDataManager", "user op failed (non-fatal): ${t.message}", t)
        }
    )

    /**
     * Reset records, wardrobe, collection (keeping the gift pet), sync queue/meta, and mood to NORMAL;
     * sign out of cloud. Keep UI-only preferences such as background and onboarding.
     */
    fun resetProgress(context: Context, onDone: () -> Unit = {}) {
        scope.launch {
            runCatching {
                val db = AppDatabase.getInstance(context.applicationContext)
                db.withTransaction {
                    db.focusRecordDao().deleteAll()
                    db.wardrobeDao().deleteAll()
                    db.userCollectionDao().deleteExcept(InitialDataProvider.FREE_PET_ID)
                    db.syncDao().clearQueue()
                    db.syncDao().clearMeta()
                }
                PetCareState.setMood(context.applicationContext, PetMood.NORMAL)
                // Clear equipped-state SharedPreferences together with purchase records,
                // otherwise reset pets could still wear outfits that are no longer owned.
                DogWardrobe.clearEquipped(context.applicationContext)
                CatWardrobe.clearEquipped(context.applicationContext)
                // Disconnect the old anonymous cloud identity to prevent old progress from being restored.
                AuthManager(context.applicationContext, ApiClient.api).logout()
            }
            // Invoke the callback on the main thread so callers can show a Toast or finish the page.
            withContext(Dispatchers.Main) { onDone() }
        }
    }

    /**
     * Sign out only: retain all local progress, register a new UID next launch, and continue syncing local
     * data under that identity.
     */
    fun logout(context: Context, onDone: () -> Unit = {}) {
        scope.launch {
            runCatching {
                AuthManager(context.applicationContext, ApiClient.api).logout()
            }
            // Invoke the callback on the main thread so callers can show a Toast or finish the page.
            withContext(Dispatchers.Main) { onDone() }
        }
    }
}
