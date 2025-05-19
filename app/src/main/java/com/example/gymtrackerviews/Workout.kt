package com.example.gymtrackerviews // O tu paquete de modelos/entidades

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.util.Date

@Entity(tableName = "workouts")
data class Workout(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    var name: String? = null,

    // NUEVO CAMPO PARA EL GRUPO MUSCULAR PRINCIPAL
    @ColumnInfo(name = "main_muscle_group")
    var mainMuscleGroup: String? = null,

    @ColumnInfo(name = "start_time")
    val startTime: Date,

    @ColumnInfo(name = "end_time")
    val endTime: Date? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null
)
