package com.example.translatelanguages.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first


val Context.dataStore by preferencesDataStore(name = "translate_setting")

class PreferencesTranslate(private val context: Context) {
    companion object {
        val FROM_LANG = stringPreferencesKey("from_lang")
        val TO_LANG   = stringPreferencesKey("to_lang")
    }
    suspend fun saveLanguage (from: String , to: String){
        context.dataStore.edit {
            prefs ->
            prefs[FROM_LANG] = from
            prefs[TO_LANG]   = to
        }

    }
    suspend fun getLanguage():Pair<String ?, String ?>{
        val prefs = context.dataStore.data.first()
        val from = prefs[FROM_LANG]
        val to = prefs[TO_LANG]
        return from to to
    }

}