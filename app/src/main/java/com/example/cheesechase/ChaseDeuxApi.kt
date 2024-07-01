package com.example.cheesechase
import retrofit2.Call
import retrofit2.http.GET

interface ChaseDeuxApi {
    @GET("/obstacleLimit")
    fun getObstacleLimit(): Call<ObstacleLimitResponse>

    @GET("/image")
    fun getImages(): Call<ImagesResponse>
}

data class ObstacleLimitResponse(val limit: Int)
data class ImagesResponse(val jerry: String, val tom: String, val obstacle: String, val background: String)