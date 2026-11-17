package com.example.translatelanguages.data.repository

import android.content.Context
import com.example.translatelanguages.data.local.preferences.PreferencesTranslate
import com.example.translatelanguages.data.local.database.AppDatabase
import com.example.translatelanguages.data.local.database.EntityTranslate
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class RepositoryTranslate(context: Context) {
    //  DataStore للغات
    private val preferencesTranslate = PreferencesTranslate(context)

    //  Room Database + DAO
    private val db = AppDatabase.getInstance(context)
    private val dao = db.daoTranslate()
    //  ترجمة النص باستخدام ML Kit
    suspend fun translate(text: String, from: String, to: String): Result<String> {
        return try {
            // إعداد خيارات المترجم
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(from)
                .setTargetLanguage(to)
                .build()
            val translator = Translation.getClient(options)
            // تنزيل النموذج إذا لم يكن موجود
            translator.downloadModelIfNeeded().await()
            // ترجمة النص
            val translated = translator.translate(text).await()
            val exists = dao.getTranslationExists(text , from , to)
            if ( exists == null && isValidTranslation(text , translated))
            {
                // بعد الترجمة وتحقيق الشروط : حفظ النص في Room
            saveTranslationToRoom(text, translated, from, to)
            }
            Result.success(translated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isValidTranslation(text: String, translated: String): Boolean {
//لاتحفض اذا كان النص المدخل اقل من حرفين
        if (text.length < 2 ) return false
//لاتحفض اذا المخرج نفس النص المدخل
        if (text.trim().lowercase() == translated.trim().lowercase()) return false
//        لاتحفض اذا المخرج فاضي
        if (translated.isBlank()) return false

        return true
    }
    //  حفظ الترجمة في Room
    private suspend fun saveTranslationToRoom(
        original: String,
        translated: String,
        fromLang: String,
        toLang: String
    ) {
        val entity = EntityTranslate(
            originalTextEntity = original,
            translatedTextEntity = translated,
            fromLanguageEntity = fromLang,
            toLanguageEntity = toLang
        )
        dao.insertTranslation(entity)
    }
    //  DataStore: حفظ واسترجاع آخر لغتين
    suspend fun saveRepo(from: String, to: String) {
        preferencesTranslate.saveLanguage(from, to)
    }
    suspend fun getSaveLanguageRepo(): Pair<String?, String?> {
        return preferencesTranslate.getLanguage()
    }
    //  جلب كل الترجمات من Room
    fun getAllTranslations(): Flow<List<EntityTranslate>> {
        return dao.getAllTranslation()
    }
    //  مسح السجل
    suspend fun clearHistory() {
        dao.delete()
    }
    suspend fun deleteById(id: Int) {
        dao.deleteById(id)
    }
}
