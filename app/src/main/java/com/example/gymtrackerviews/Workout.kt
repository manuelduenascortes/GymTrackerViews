package com.example.gymtrackerviews // Tu paquete

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.util.Date

@Entity(tableName = "workouts")
data class Workout(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name") // NUEVO CAMPO PARA EL NOMBRE DEL ENTRENAMIENTO
    var name: String? = null,  // Lo hacemos nullable (opcional) por ahora

    @ColumnInfo(name = "start_time")
    val startTime: Date,

    @ColumnInfo(name = "end_time")
    val endTime: Date? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null
)
