package com.example.spark.network

import com.google.gson.annotations.SerializedName

// ─── Auth ─────────────────────────────────────────────────────────────────────

data class AuthRequest(
    val email: String,
    val password: String,
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
)

data class UserResponse(
    val id: Int,
    val email: String,
)

data class RegisterResponse(
    val message: String,
    val user: UserResponse,
)

// ─── Profile ─────────────────────────────────────────────────────────────────

data class ProfileResponse(
    val email: String,
    val courage: Int,
    val completed: Int,
    val skipped: Int,
    val streak: Int,
    val level: String,
    @SerializedName("notifications_enabled") val notificationsEnabled: Boolean,
)

data class ProfileUpdateRequest(
    val courage: Int? = null,
    val completed: Int? = null,
    val skipped: Int? = null,
    val streak: Int? = null,
    val level: String? = null,
    @SerializedName("notifications_enabled") val notificationsEnabled: Boolean? = null,
)

// ─── Challenges ──────────────────────────────────────────────────────────────

data class ChallengeStateResponse(
    @SerializedName("challenge_id") val challengeId: Int,
    val status: String,       // open | active | checking | done | skipped
    @SerializedName("photo_url") val photoUrl: String?,
)

data class ChallengeStateBulkResponse(
    val states: List<ChallengeStateResponse>,
)

data class ChallengeStateUpdateRequest(
    val status: String,
    @SerializedName("photo_url") val photoUrl: String? = null,
)
