package com.example.focuspets.cloud

import com.example.focuspets.cloud.model.LeaderboardResponse

/** Query /api/leaderboard and cache the last successful response. */
class LeaderboardRepository(private val api: CloudApi) {
    suspend fun getLeaderboard(limit: Int = 50): LeaderboardResponse {
        val resp = api.leaderboard(limit, CloudSyncManager.currentUid())
        // Cache successful results for offline fallback.
        CloudSyncManager.cacheLeaderboard(resp)
        return resp
    }
}
