package com.example.translatelanguages.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow


@Dao
interface DaoTranslate {
    @Insert (onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslation(entityTranslate: EntityTranslate)

    @Query("SELECT * FROM TranslateDB ORDER BY timesTampEntity DESC")
    fun getAllTranslation(): Flow<List<EntityTranslate>>

    @Query("DELETE FROM TranslateDB")
    suspend fun delete ()

    @Query("DELETE FROM TranslateDB WHERE id = :itemId")
    suspend fun deleteById(itemId : Int)

    @Query("SELECT * FROM TranslateDB WHERE originalTextEntity = :original AND fromLanguageEntity= :fromLang AND toLanguageEntity= :toLang LIMIT 1 ")
    suspend fun getTranslationExists(original :String
                                     , fromLang:String
                                     , toLang:String ): EntityTranslate?
}