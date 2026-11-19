package com.example.translatelanguages.data.repository

import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await

class DownloadLanguageRepository {
    //انشا مترجم حسب لغت المصدر والهدف
    private fun createTranslator(
        from: String, to: String
    ) = Translation
        .getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(from)
                .setTargetLanguage(to)
                .build()
        )

    // فحص هل لغه معينة موجودة او لا
    suspend fun isModelDownloaded(
        language: String
    ): Boolean {
        val model = TranslateRemoteModel
            .Builder(language)
            .build()
        return RemoteModelManager
            .getInstance()
            .isModelDownloaded(model)
            .await()
    }

    // تنزيل نماذج اللغات المطلوبة عند الجاجة
    suspend fun downloadModel(from: String, to: String): Result<Unit> {
        val translator = createTranslator(from, to)
        return try {
            translator.downloadModelIfNeeded()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            translator.close()
        }
    }

    suspend fun areModelsDownloaded(
        from: String,
        to: String
    ): Result<Boolean> {
        return try {
            val fromDownloaded = isModelDownloaded(from)
            val toDownloaded = if (from == to) {
                fromDownloaded
            } else {
                isModelDownloaded(to)
            }
            Result.success(fromDownloaded && toDownloaded)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
