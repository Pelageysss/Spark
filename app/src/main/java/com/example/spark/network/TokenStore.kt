package com.example.spark.network

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "spark_prefs")

/**
 * Хранит JWT-токен в DataStore (переживает перезапуск приложения).
 * Токен — единственные данные на устройстве; всё остальное — в API.
 */
class TokenStore(private val context: Context) {

    companion object {
        private val KEY_TOKEN = stringPreferencesKey("access_token")
    }

    /** Flow текущего токена (null — не авторизован). */
    val tokenFlow: Flow<String?> = context.dataStore.data
        .map { it[KEY_TOKEN] }

    suspend fun save(token: String) {
        context.dataStore.edit { it[KEY_TOKEN] = token }
    }

    suspend fun clear() {
        context.dataStore.edit { it.remove(KEY_TOKEN) }
    }
}
