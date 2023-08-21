package com.example.pedometer.Model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import java.util.*

@Dao
interface StepsDAO {
    @Insert(onConflict = REPLACE)
    fun insert(steps : StepsEntity)

    @Query("SELECT * FROM steps")
    fun getAll() : List<StepsEntity>

    @Delete
    fun delete(steps: StepsEntity)
    @Query("SELECT * FROM steps WHERE date = :date")
    fun getByDate(date: Long): StepsEntity?

}