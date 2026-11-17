package com.example.translatelanguages.data.local.database


import android.content.Context
    import androidx.room.Database
    import androidx.room.Room
    import androidx.room.RoomDatabase

@Database(
    entities = [EntityTranslate::class],
    version = 1
    )
    abstract class AppDatabase : RoomDatabase() {

        abstract fun daoTranslate(): DaoTranslate

        companion object {
            @Volatile
            private var INSTANCE: AppDatabase? = null

            fun getInstance(context: Context): AppDatabase {
                return INSTANCE ?: synchronized(this) {
                    INSTANCE ?: Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "translator_db"
                    ).build().also { INSTANCE = it }
                }
            }
        }
    }

