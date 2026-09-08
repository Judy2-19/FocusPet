package com.example.focuspets.cloud

import com.example.focuspets.cloud.model.LeaderboardResponse

/** 排行榜数据访问：封装对后端 /api/leaderboard 的查询，并缓存最后一次成功结果 */
class LeaderboardRepository(private val api: CloudApi) {
    suspend fun getLeaderboard(limit: Int = 50): LeaderboardResponse {
        val resp = api.leaderboard(limit, CloudSyncManager.currentUid())
        // 成功即缓存，供离线时回退显示
        CloudSyncManager.cacheLeaderboard(resp)
        return resp
    }
}
