package com.example.spark.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {

    // ─── Auth ─────────────────────────────────────────────────────────────

    @POST("api/v2/register")
    suspend fun register(@Body body: AuthRequest): Response<RegisterResponse>

    @POST("api/v2/login")
    suspend fun login(@Body body: AuthRequest): Response<TokenResponse>

    // ─── Profile ──────────────────────────────────────────────────────────

    @GET("api/v2/profile")
    suspend fun getProfile(
        @Header("Authorization") bearer: String,
    ): Response<ProfileResponse>

    @PATCH("api/v2/profile")
    suspend fun updateProfile(
        @Header("Authorization") bearer: String,
        @Body body: ProfileUpdateRequest,
    ): Response<ProfileResponse>

    // ─── Challenges ───────────────────────────────────────────────────────

    @GET("api/v2/challenges")
    suspend fun getChallenges(
        @Header("Authorization") bearer: String,
    ): Response<ChallengeStateBulkResponse>

    @PUT("api/v2/challenges/{id}")
    suspend fun updateChallenge(
        @Header("Authorization") bearer: String,
        @Path("id") challengeId: Int,
        @Body body: ChallengeStateUpdateRequest,
    ): Response<ChallengeStateResponse>
}
