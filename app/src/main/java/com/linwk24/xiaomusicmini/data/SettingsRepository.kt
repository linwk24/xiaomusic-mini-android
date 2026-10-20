package com.linwk24.xiaomusicmini.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val serverKey = stringPreferencesKey("server_url")
    private val outputKey = stringPreferencesKey("output_mode")
    private val phoneModeKey = stringPreferencesKey("phone_mode")

    val serverUrl: Flow<String> = context.settingsDataStore.data.map {
        it[serverKey] ?: DEFAULT_SERVER
    }

    val outputMode: Flow<OutputMode> = context.settingsDataStore.data.map {
        runCatching { OutputMode.valueOf(it[outputKey] ?: OutputMode.SPEAKER.name) }
            .getOrDefault(OutputMode.SPEAKER)
    }

    suspend fun setServerUrl(url: String) {
        context.settingsDataStore.edit { it[serverKey] = url }
    }

    val phoneMode: Flow<String> = context.settingsDataStore.data.map {
        it[phoneModeKey] ?: "all"
    }

    suspend fun setOutputMode(mode: OutputMode) {
        context.settingsDataStore.edit { it[outputKey] = mode.name }
    }

    suspend fun setPhoneMode(mode: String) {
        context.settingsDataStore.edit { it[phoneModeKey] = mode }
    }

    companion object {
        const val DEFAULT_SERVER = "http://192.168.5.1:8091"
    }
}
