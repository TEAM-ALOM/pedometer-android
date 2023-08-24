package com.example.pedometer.Model

import androidx.room.*
import androidx.room.OnConflictStrategy.Companion.REPLACE
import java.util.*

@Dao
interface StepsDAO {
    @Insert(onConflict = REPLACE)
    fun insert(steps : StepsEntity)

    @Update
    fun update(steps: StepsEntity)

    @Query("SELECT * FROM steps")
    fun getAll() : List<StepsEntity>

    @Delete
    fun delete(steps: StepsEntity)
    @Query("SELECT * FROM steps WHERE date = :date")
    fun getByDate(date: Long): StepsEntity?
    @Query("SELECT * FROM steps WHERE date BETWEEN :startTime AND :endTime")
    fun getStepsBetweenDates(startTime: Long, endTime: Long): List<StepsEntity>

}