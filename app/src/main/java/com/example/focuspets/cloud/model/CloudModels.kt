package com.example.focuspets.cloud.model

/** 匿名注册响应 */
data class AnonResponse(val uid: String, val displayName: String)

/** 云端用户完整对象（GET 返回） */
data class CloudUser(
    val uid: String,
    val displayName: String,
    val totalPoints: Int,
    val collection: List<Int>,
    val wardrobe: List<String>,
    val mood: String,
    val updatedAt: Long
)

/** 上报/合并时发送的负载（不含 uid / updatedAt） */
data class CloudUserPayload(
    val displayName: String,
    val totalPoints: Int,
    val collection: List<Int>,
    val wardrobe: List<String>,
    val mood: String,
    /** 当前展示的宠物 id（1~9，用于排行榜头像兜底） */
    val currentPetId: Int = 1,
    /** 当前宠物立绘路径（assets 内；猫/狗带装扮，其余为空串） */
    val petAvatar: String = ""
)

/** 排行榜单项 */
data class LeaderboardEntry(
    val rank: Int,
    val uid: String,
    val displayName: String,
    val totalPoints: Int,
    /** 当前宠物立绘路径（assets 内，如 cat/pink_dress_blue_crown.png）；猫/狗有，其余为空 */
    val petAvatar: String? = null,
    /** 当前宠物 id（无立绘的宠物用 emoji 兜底，1~9） */
    val petId: Int? = null
)

/** 自己的名次信息 */
data class MeRank(val rank: Int?, val totalPoints: Int)

/** 排行榜响应 */
data class LeaderboardResponse(val list: List<LeaderboardEntry>, val me: MeRank?)
