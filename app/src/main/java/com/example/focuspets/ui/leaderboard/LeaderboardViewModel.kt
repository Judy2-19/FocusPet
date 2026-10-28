package com.example.focuspets.ui.leaderboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.focuspets.cloud.ApiClient
import com.example.focuspets.cloud.CloudSyncManager
import com.example.focuspets.cloud.LeaderboardRepository
import com.example.focuspets.cloud.model.LeaderboardEntry
import com.example.focuspets.cloud.model.MeRank
import kotlinx.coroutines.launch

data class LeaderboardResult(
    val list: List<LeaderboardEntry>,
    val me: MeRank?,
    /** True means data came from the offline cache because the backend was unreachable. */
    val fromCache: Boolean = false
)

class LeaderboardViewModel : ViewModel() {

    private val repo = LeaderboardRepository(ApiClient.api)

    private val _board = MutableLiveData<LeaderboardResult?>()
    val board: LiveData<LeaderboardResult?> = _board

    private val _status = MutableLiveData("")
    val status: LiveData<String> = _status

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _status.postValue(com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_095))
            try {
                val resp = repo.getLeaderboard(50)
                _board.postValue(LeaderboardResult(resp.list, resp.me, fromCache = false))
                _status.postValue("")
            } catch (e: Exception) {
                // Fall back to the last cache instead of showing only a loading error when the backend is unreachable.
                val cached = CloudSyncManager.loadCachedLeaderboard()
                if (cached != null) {
                    _board.postValue(LeaderboardResult(cached.list, cached.me, fromCache = true))
                    _status.postValue(com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_182 ,(cached.list.size).toString()))
                } else {
                    _status.postValue(com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_167 ,(e.message).toString()))
                }
            }
        }
    }

    fun rename(name: String) {
        CloudSyncManager.rename(name)
        refresh()
    }
}
