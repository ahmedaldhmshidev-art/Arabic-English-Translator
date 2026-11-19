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

    private val preferencesTranslate =
        PreferencesTranslate(context.applicationContext)

    private val db =
        AppDatabase.getInstance(context.applicationContext)

    private val dao = db.daoTranslate()

    // ترجمة النص وحفظه عند استيفاء الشروط
    suspend fun translate(
        text: String,
        from: String,
        to: String
    ): Result<String> {

        if (text.isBlank()) {
            return Result.failure(
                IllegalArgumentException("النص فارغ")
            )
        }

        if (from == to) {
            return Result.failure(
                IllegalArgumentException("لغة المصدر والهدف متطابقتان")
            )
        }

        val translator = Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(from)
                .setTargetLanguage(to)
                .build()
        )

        return try {
            val translated = translator.translate(text).await()

            if (isValidTranslation(text, translated)) {

                val exists = dao.getTranslationExists(
                    text.trim(),
                    from,
                    to
                )

                if (exists == null) {
                    saveTranslationToRoom(
                        text.trim(),
                        translated.trim(),
                        from,
                        to
                    )
                }
            }

            Result.success(translated)

        } catch (e: Exception) {

            Result.failure(e)

        } finally {

            translator.close()
        }
    }

    // التحقق من صلاحية الترجمة للحفظ
    private fun isValidTranslation(
        text: String,
        translated: String
    ): Boolean {
        val original = text.trim()
        val result = translated.trim()
        if (original.length < 2) return false
        if (result.isBlank()) return false
        if (original.equals(result, ignoreCase = true)) return false

        return true
    }

    // حفظ الترجمة في Room
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

    // حفظ آخر لغتين في DataStore
    suspend fun saveRepo(from: String, to: String) {
        preferencesTranslate.saveLanguage(from, to)
    }

    // استرجاع آخر لغتين من DataStore
    suspend fun getSaveLanguageRepo(): Pair<String?, String?> {
        return preferencesTranslate.getLanguage()
    }

    // جلب جميع الترجمات من Room
    fun getAllTranslations(): Flow<List<EntityTranslate>> {
        return dao.getAllTranslation()
    }

    // حذف ترجمة حسب ID
    suspend fun deleteById(id: Int) {
        dao.deleteById(id)
    }

    // مسح سجل الترجمات بالكامل
    suspend fun clearHistory() {
        dao.delete()
    }
}