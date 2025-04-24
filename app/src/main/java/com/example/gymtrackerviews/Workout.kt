package com.example.gymtrackerviews // Asegúrate que es tu paquete

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.util.Date // Necesitamos importar la clase Date

// @Entity le dice a Room que esta clase representa una tabla llamada 'workouts'.
@Entity(tableName = "workouts")
data class Workout(
    // @PrimaryKey marca este campo como la clave primaria de la tabla.
    // autoGenerate = true hace que Room genere automáticamente un ID único (1, 2, 3...) para cada nuevo workout.
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // Usamos Long para IDs autogenerados

    // @ColumnInfo le da un nombre específico a la columna en la base de datos.
    // Si no lo pones, Room usa el nombre de la variable (startTime).
    @ColumnInfo(name = "start_time")
    val startTime: Date // Guardaremos la fecha y hora de inicio del entrenamiento
)