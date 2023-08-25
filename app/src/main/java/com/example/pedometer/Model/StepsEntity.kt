package com.example.pedometer.Model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "steps")
data class StepsEntity(
    @PrimaryKey(autoGenerate = false)
    var date: Long = 0L, // 날짜를 Long 타입으로 저장
    @ColumnInfo(name="todaySteps")
    var todaySteps: Int?,
    @ColumnInfo(name="goalSteps")
    var goalSteps: Int?
)
