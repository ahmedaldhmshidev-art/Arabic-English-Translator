package com.example.translatelanguages.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity (tableName = "TranslateDB")
data class EntityTranslate
    (
    @PrimaryKey(autoGenerate = true) val id : Int =  0,
            val originalTextEntity : String,
            val translatedTextEntity : String,
            val fromLanguageEntity : String,
            val toLanguageEntity : String,
            val timesTampEntity : Long = System.currentTimeMillis()
            )