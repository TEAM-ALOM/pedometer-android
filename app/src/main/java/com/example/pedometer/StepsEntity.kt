package com.example.pedometer

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "steps")
data class StepsEntity(
    @PrimaryKey(autoGenerate = false)
    var date: String="",
    var todaySteps: Int?,
    var goalSteps: Int?
)