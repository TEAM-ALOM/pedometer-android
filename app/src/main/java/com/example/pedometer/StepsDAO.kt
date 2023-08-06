package com.example.pedometer

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query

@Dao
interface StepsDAO {
    @Insert(onConflict = REPLACE)
    fun insert(steps : StepsEntity)

    @Query("SELECT * FROM steps")
    fun getAll() : List<StepsEntity>

    @Delete
    fun delete(steps: StepsEntity)
}