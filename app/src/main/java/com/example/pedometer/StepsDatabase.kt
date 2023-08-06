package com.example.pedometer

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = arrayOf(StepsEntity::class), version=1)
abstract class StepsDatabase : RoomDatabase() {
    abstract fun stepsDAO() : StepsDAO

    companion object{
        var INSTANCE : StepsDatabase? = null

        fun getInstance(context: Context) : StepsDatabase? {
            if(INSTANCE == null){
                synchronized(StepsDatabase::class){
                    INSTANCE = Room.databaseBuilder(context.applicationContext,
                        StepsDatabase::class.java, "steps.db")
                        .fallbackToDestructiveMigration()
                        .build()
                }
            }
            return INSTANCE
        }
    }
}