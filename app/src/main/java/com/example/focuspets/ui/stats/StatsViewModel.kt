package com.example.focuspets.ui.stats

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.focuspets.db.DayBar
import com.example.focuspets.db.PetRepository
import com.example.focuspets.model.AchievementBadge

class StatsViewModel(private val repo: PetRepository) : ViewModel() {
    val today: LiveData<Int> = repo.getTodayMinutes()
    val total: LiveData<Int> = repo.getTotalMinutes()
    val sessions: LiveData<Int> = repo.getSessionCount()
    val streak: LiveData<Int> = repo.getStreak()
    val last7: LiveData<List<DayBar>> = repo.getLast7Days()

    /** Heatmap minute matrix: seven weekdays (Monday-Sunday) by 24 hours. */
    val heatmap: LiveData<Array<IntArray>> = repo.getHeatmap()

    /** Streak achievement badges. */
    val achievements: LiveData<List<AchievementBadge>> = repo.getAchievements()
}

class StatsViewModelFactory(private val repo: PetRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = StatsViewModel(repo) as T
}
