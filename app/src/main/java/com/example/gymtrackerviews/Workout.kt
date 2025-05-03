package com.example.gymtrackerviews // Tu paquete

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.util.Date

@Entity(tableName = "workouts")
data class Workout(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "start_time")
    val startTime: Date,

    // Campo para la hora de fin (puede ser null)
    @ColumnInfo(name = "end_time")
    val endTime: Date? = null,

    // Campo para notas (puede ser null)
    @ColumnInfo(name = "notes")
    val notes: String? = null
)
