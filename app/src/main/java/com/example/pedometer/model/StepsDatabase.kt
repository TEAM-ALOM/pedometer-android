package com.example.pedometer.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [StepsEntity::class], version = 2, exportSchema = false)
abstract class StepsDatabase : RoomDatabase() {

    abstract fun stepsDAO(): StepsDAO

    companion object {
        @Volatile
        private var INSTANCE: StepsDatabase? = null

        fun getInstance(context: Context): StepsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StepsDatabase::class.java,
                    "steps_database"
                )
                    .fallbackToDestructiveMigration() // 파괴적 마이그레이션 사용
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
