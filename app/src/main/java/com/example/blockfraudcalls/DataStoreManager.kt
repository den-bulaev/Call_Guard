package com.example.blockfraudcalls

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property for DataStore
val Context.dataStore by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {

    companion object {
        private val USER_TEXT = stringPreferencesKey("user_text")
    }

    // Save text
    suspend fun saveText(value: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_TEXT] = value
        }
    }

    // Read text
    fun getText(): Flow<String> =
        context.dataStore.data.map { prefs ->
            prefs[USER_TEXT] ?: ""
        }
}