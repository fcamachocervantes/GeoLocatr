package com.csci448.geolocatr

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreManager (private val context: Context){
    companion object {
        private const val DATA_STORE_NAME = "mapDataStore"

        private val Context.dataStore: DataStore<Preferences>
                by preferencesDataStore(name = DATA_STORE_NAME)

        private val TRAFFIC_KEY = booleanPreferencesKey("traffic_key")
        private val COMPASS_KEY = booleanPreferencesKey("compass_key")
    }

    val trafficFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[TRAFFIC_KEY] ?: false
    }

    val compassFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[COMPASS_KEY] ?: true
    }

    suspend fun setTrafficFlow(newValue: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[TRAFFIC_KEY] = newValue
        }
    }

    suspend fun setCompassFlow(newValue: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[COMPASS_KEY] = newValue
        }
    }
}