package com.example.spark.network

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// ─── UI-состояния ─────────────────────────────────────────────────────────────

sealed interface AuthState {
    data object Idle : AuthState
    data object Loading : AuthState
    data object Success : AuthState
    data class Error(val message: String) : AuthState
}

sealed interface DataState<out T> {
    data object Loading : DataState<Nothing>
    data class Ready<T>(val data: T) : DataState<T>
    data class Error(val message: String) : DataState<Nothing>
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

class SparkViewModel(
    private val repo: SparkRepository,
    private val tokenStore: TokenStore,
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _profile = MutableStateFlow<DataState<ProfileResponse>>(DataState.Loading)
    val profile: StateFlow<DataState<ProfileResponse>> = _profile.asStateFlow()

    private val _challenges = MutableStateFlow<DataState<List<ChallengeStateResponse>>>(DataState.Loading)
    val challenges: StateFlow<DataState<List<ChallengeStateResponse>>> = _challenges.asStateFlow()

    /** Сохранённый токен — null до загрузки из DataStore. */
    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    init {
        // Восстанавливаем сессию при старте
        viewModelScope.launch {
            val saved = tokenStore.tokenFlow.first()
            if (saved != null) {
                _token.value = saved
                loadAll(saved)
            }
        }
    }

    // ─── Auth ──────────────────────────────────────────────────────────────

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repo.register(email, password)
                .onSuccess { _authState.value = AuthState.Success }
                .onFailure { _authState.value = AuthState.Error(it.message ?: "Ошибка регистрации") }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repo.login(email, password)
                .onSuccess { resp ->
                    tokenStore.save(resp.accessToken)
                    _token.value = resp.accessToken
                    _authState.value = AuthState.Success
                    loadAll(resp.accessToken)
                }
                .onFailure { _authState.value = AuthState.Error(it.message ?: "Ошибка входа") }
        }
    }

    fun logout() {
        viewModelScope.launch {
            tokenStore.clear()
            _token.value = null
            _authState.value = AuthState.Idle
            _profile.value = DataState.Loading
            _challenges.value = DataState.Loading
        }
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    // ─── Profile ───────────────────────────────────────────────────────────

    fun loadProfile() {
        val t = _token.value ?: return
        viewModelScope.launch {
            _profile.value = DataState.Loading
            repo.getProfile(t)
                .onSuccess { _profile.value = DataState.Ready(it) }
                .onFailure { _profile.value = DataState.Error(it.message ?: "Ошибка профиля") }
        }
    }

    fun updateProfile(update: ProfileUpdateRequest) {
        val t = _token.value ?: return
        viewModelScope.launch {
            repo.updateProfile(t, update)
                .onSuccess { _profile.value = DataState.Ready(it) }
                .onFailure { /* профиль остаётся прежним, ошибка молчаливая */ }
        }
    }

    // ─── Challenges ────────────────────────────────────────────────────────

    fun loadChallenges() {
        val t = _token.value ?: return
        viewModelScope.launch {
            _challenges.value = DataState.Loading
            repo.getChallenges(t)
                .onSuccess { _challenges.value = DataState.Ready(it.states) }
                .onFailure { _challenges.value = DataState.Error(it.message ?: "Ошибка заданий") }
        }
    }

    fun updateChallenge(challengeId: Int, status: String, photoUrl: String? = null) {
        val t = _token.value ?: return
        viewModelScope.launch {
            repo.updateChallenge(t, challengeId, status, photoUrl)
                .onSuccess { updated ->
                    // Заменяем только изменившийся элемент в списке
                    val current = (_challenges.value as? DataState.Ready)?.data ?: emptyList()
                    val merged = current
                        .filter { it.challengeId != challengeId }
                        .plus(updated)
                        .sortedBy { it.challengeId }
                    _challenges.value = DataState.Ready(merged)
                }
                .onFailure { /* retry логику можно добавить позже */ }
        }
    }

    // ─── helpers ───────────────────────────────────────────────────────────

    private fun loadAll(token: String) {
        viewModelScope.launch {
            launch { repo.getProfile(token).onSuccess { _profile.value = DataState.Ready(it) } }
            launch {
                repo.getChallenges(token).onSuccess { _challenges.value = DataState.Ready(it.states) }
            }
        }
    }

    // ─── Factory ───────────────────────────────────────────────────────────

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SparkViewModel(SparkRepository(), TokenStore(context.applicationContext)) as T
    }
}
