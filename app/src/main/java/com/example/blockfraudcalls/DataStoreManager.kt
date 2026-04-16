package com.example.blockfraudcalls

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.blockfraudcalls.model.WhitelistNumber
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer

val Context.dataStore by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {

    companion object {
        private val BLOCKED_NUMBER = stringPreferencesKey("user_text")
        private val WHITELIST_KEY = stringPreferencesKey("whitelist")
    }

    suspend fun saveBlockedNumber(value: String) {
        context.dataStore.edit { prefs ->
            prefs[BLOCKED_NUMBER] = value
        }
    }

    fun getBlockedNumber(): Flow<String> =
        context.dataStore.data.map { prefs ->
            prefs[BLOCKED_NUMBER] ?: ""
        }.distinctUntilChanged()

    suspend fun saveToWhitelist(list: List<WhitelistNumber>) {
        val json = Json.encodeToString(ListSerializer(WhitelistNumber.serializer()), list)
        context.dataStore.edit { prefs ->
            prefs[WHITELIST_KEY] = json
        }
    }

    fun getWhitelist(): Flow<List<WhitelistNumber>> =
        context.dataStore.data.map { prefs ->
            val json = prefs[WHITELIST_KEY]
            if (json.isNullOrEmpty()) {
                emptyList()
            } else {
                Json.decodeFromString(ListSerializer(WhitelistNumber.serializer()), json)
            }
        }.distinctUntilChanged()
}