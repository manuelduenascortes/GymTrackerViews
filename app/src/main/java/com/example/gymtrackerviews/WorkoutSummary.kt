package com.example.gymtrackerviews // Tu paquete

import androidx.room.Embedded // Para incluir campos de otra entidad
import androidx.room.Relation

// Data class para contener un Workout y su número de series
// No es una @Entity, es solo para agrupar resultados de una consulta
data class WorkoutSummary(
    // Incluye todos los campos del objeto Workout directamente
    @Embedded
    val workout: Workout,

    // Campo calculado para el número de series
    // El nombre 'setCount' debe coincidir con el alias en la consulta SQL del DAO
    val setCount: Int
)
