package com.example.spark.network

/**
 * Единая точка доступа к API.
 * Все методы — suspend, возвращают Result<T> чтобы ViewModel не знал о Retrofit.
 */
class SparkRepository(
    private val api: ApiService = RetrofitClient.api,
) {

    private fun String.bearer() = "Bearer $this"

    // ─── Auth ─────────────────────────────────────────────────────────────

    suspend fun register(email: String, password: String): Result<RegisterResponse> = runCatching {
        val resp = api.register(AuthRequest(email, password))
        resp.body() ?: error("register failed: ${resp.code()} ${resp.errorBody()?.string()}")
    }

    suspend fun login(email: String, password: String): Result<TokenResponse> = runCatching {
        val resp = api.login(AuthRequest(email, password))
        resp.body() ?: error("login failed: ${resp.code()} ${resp.errorBody()?.string()}")
    }

    // ─── Profile ──────────────────────────────────────────────────────────

    suspend fun getProfile(token: String): Result<ProfileResponse> = runCatching {
        val resp = api.getProfile(token.bearer())
        resp.body() ?: error("getProfile failed: ${resp.code()}")
    }

    suspend fun updateProfile(token: String, update: ProfileUpdateRequest): Result<ProfileResponse> = runCatching {
        val resp = api.updateProfile(token.bearer(), update)
        resp.body() ?: error("updateProfile failed: ${resp.code()}")
    }

    // ─── Challenges ───────────────────────────────────────────────────────

    suspend fun getChallenges(token: String): Result<ChallengeStateBulkResponse> = runCatching {
        val resp = api.getChallenges(token.bearer())
        resp.body() ?: error("getChallenges failed: ${resp.code()}")
    }

    suspend fun updateChallenge(
        token: String,
        challengeId: Int,
        status: String,
        photoUrl: String? = null,
    ): Result<ChallengeStateResponse> = runCatching {
        val resp = api.updateChallenge(token.bearer(), challengeId, ChallengeStateUpdateRequest(status, photoUrl))
        resp.body() ?: error("updateChallenge failed: ${resp.code()}")
    }
}
