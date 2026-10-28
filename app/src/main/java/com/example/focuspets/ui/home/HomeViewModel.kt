package com.example.focuspets.ui.home

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.focuspets.db.PetRepository
import com.example.focuspets.db.dao.PetWithState
import com.example.focuspets.db.entity.PetEntity
import com.example.focuspets.model.PetCareState
import com.example.focuspets.model.PetMood

/**
 * Home UI state: selectedPet defaults to the first unlocked pet and changes on tap/swipe;
 * nextLockedPet is the cheapest locked pet for progress; mood is SICK/NORMAL/GLOW.
 */
data class HomeUiState(
    val selectedPet: PetEntity,
    val unlockedCount: Int,
    val availablePoints: Int,
    val nextLockedPet: PetEntity?,
    val progressPercent: Int,
    val remainingPoints: Int,
    val mood: PetMood
)

class HomeViewModel(private val repository: PetRepository) : ViewModel() {

    /** Selected index within unlocked pets; zero is the initial pet. */
    private val selectedIndex = MutableLiveData(0)

    /** Home pet ID pending restoration after process restart, persisted in SettingsManager; -1 means none. */
    private var pendingPetId = -1

    /** Restore a specified pet on the next emit to preserve selection across restarts. */
    fun selectPetById(id: Int) {
        pendingPetId = id
    }

    /** Mood refreshed by the Fragment after focus success/failure. */
    private val mood = MutableLiveData(PetMood.NORMAL)

    /** Latest unlocked count for wrapping pet selection. */
    private var unlockedCount = 1

    /**
     * Combine four sources into UiState: Room catalog unlock state and available points, selected index,
     * and pet mood.
     */
    val uiState: LiveData<HomeUiState> = MediatorLiveData<HomeUiState>().apply {
        var pets: List<PetWithState>? = null
        var points: Int? = null
        fun emit() {
            val p = pets ?: return
            val pt = points ?: return
            val unlocked = p.filter { it.isUnlocked }.map { it.pet }
            if (unlocked.isEmpty()) return
            unlockedCount = unlocked.size
            // Restore the last Home pet after process restart using SettingsManager.
            if (pendingPetId >= 0) {
                var idx = unlocked.indexOfFirst { it.id == pendingPetId }
                if (idx < 0) idx = 0
                pendingPetId = -1
                if (idx != selectedIndex.value ?: 0) {
                    selectedIndex.value = idx
                    return          // Changing the index emits again to render the new selection.
                }
            }
            val index = (selectedIndex.value ?: 0).coerceIn(0, unlocked.size - 1)

            // Progress targets the cheapest locked pet; the list is sorted by unlock_cost ascending.
            val nextLocked = p.firstOrNull { !it.isUnlocked }?.pet

            value = HomeUiState(
                selectedPet = unlocked[index],
                unlockedCount = unlocked.size,
                availablePoints = pt,
                nextLockedPet = nextLocked,
                progressPercent = if (nextLocked == null) 100
                else (pt * 100 / nextLocked.unlockCost).coerceAtMost(100),
                remainingPoints = if (nextLocked == null) 0
                else (nextLocked.unlockCost - pt).coerceAtLeast(0),
                mood = mood.value ?: PetMood.NORMAL
            )
        }
        addSource(repository.getPetsWithState()) { pets = it; emit() }
        addSource(repository.getAvailablePoints()) { points = it; emit() }
        addSource(selectedIndex) { emit() }
        addSource(mood) { emit() }
    }

    /** Tap or swipe left to cycle to the next unlocked pet. */
    fun selectNext() {
        val count = unlockedCount.coerceAtLeast(1)
        selectedIndex.value = ((selectedIndex.value ?: 0) + 1).mod(count)
    }

    /** Swipe right for the previous pet. */
    fun selectPrev() {
        val count = unlockedCount.coerceAtLeast(1)
        selectedIndex.value = (((selectedIndex.value ?: 0) - 1) + count).mod(count)
    }

    /** Refresh mood after completion/failure or returning to the page. */
    fun refreshMood(context: Context) {
        mood.value = PetCareState.getMood(context)
    }
}

class HomeViewModelFactory(private val repository: PetRepository) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        HomeViewModel(repository) as T
}
