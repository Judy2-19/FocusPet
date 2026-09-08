package com.example.focuspets.cloud

import com.example.focuspets.BuildConfig
import com.example.focuspets.cloud.model.AnonResponse
import com.example.focuspets.cloud.model.CloudUser
import com.example.focuspets.cloud.model.CloudUserPayload
import com.example.focuspets.cloud.model.LeaderboardResponse
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** 与本地/局域网后端通信的 HTTP 接口，契约见 server/leaderboard_server.py */
interface CloudApi {

    @POST("/api/auth/anon")
    suspend fun anonLogin(): AnonResponse

    @GET("/api/users/{uid}")
    suspend fun getUser(@Path("uid") uid: String): CloudUser

    @PUT("/api/users/{uid}")
    suspend fun updateUser(@Path("uid") uid: String, @Body payload: CloudUserPayload): CloudUser

    @GET("/api/leaderboard")
    suspend fun leaderboard(
        @Query("limit") limit: Int,
        @Query("uid") uid: String?
    ): LeaderboardResponse
}

object ApiClient {
    val api: CloudApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.CLOUD_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(OkHttpClient.Builder().build())
            .build()
            .create(CloudApi::class.java)
    }
}
