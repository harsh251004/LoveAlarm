package com.secure.lovealarm.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings",
    corruptionHandler = ReplaceFileCorruptionHandler { _ -> preferencesOf() }
)

private val AMOLED_DARK_MODE_KEY = booleanPreferencesKey("amoled_dark_mode")

val Context.amoledDarkMode: Flow<Boolean>
    get() = appSettingsDataStore.data
        .catch { _ -> emit(preferencesOf()) }
        .map { prefs -> prefs[AMOLED_DARK_MODE_KEY] ?: false }

suspend fun Context.setAmoledDarkMode(enabled: Boolean) {
    appSettingsDataStore.edit { prefs -> prefs[AMOLED_DARK_MODE_KEY] = enabled }
}
