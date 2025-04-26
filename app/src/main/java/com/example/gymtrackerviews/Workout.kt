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
    val startTime: Date, // La hora de inicio sigue siendo obligatoria

    // 👇 --- CAMPO AÑADIDO --- 👇
    // Guardará la hora de fin. Es 'Date?' (nullable) porque puede no haber terminado.
    @ColumnInfo(name = "end_time")
    val endTime: Date? = null
    // 👆 --- FIN CAMPO AÑADIDO --- 👆
)