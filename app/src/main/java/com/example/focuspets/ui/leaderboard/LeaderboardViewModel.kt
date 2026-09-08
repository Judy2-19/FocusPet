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
    /** true = 来自离线缓存（后端连不上） */
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
            _status.postValue("加载中…")
            try {
                val resp = repo.getLeaderboard(50)
                _board.postValue(LeaderboardResult(resp.list, resp.me, fromCache = false))
                _status.postValue("")
            } catch (e: Exception) {
                // 后端连不上：回退到上次缓存，而不是显示"加载失败"
                val cached = CloudSyncManager.loadCachedLeaderboard()
                if (cached != null) {
                    _board.postValue(LeaderboardResult(cached.list, cached.me, fromCache = true))
                    _status.postValue("⚠ 后端未连接，显示上次缓存（共 ${cached.list.size} 条）")
                } else {
                    _status.postValue("排行榜加载失败：${e.message}（请确认后端服务已启动）")
                }
            }
        }
    }

    fun rename(name: String) {
        CloudSyncManager.rename(name)
        refresh()
    }
}
