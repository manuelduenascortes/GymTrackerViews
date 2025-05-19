package com.example.gymtrackerviews // O tu paquete de modelos/entidades

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String, // Nombre del ejercicio, no debería ser nulo

    @ColumnInfo(name = "muscle_group")
    val muscleGroup: String? = null, // Grupo muscular, puede ser opcional

    @ColumnInfo(name = "description")
    val description: String? = null, // Descripción opcional del ejercicio

    // Podrías añadir más campos en el futuro, como:
    // val defaultImageResId: Int? = null, // Para una imagen por defecto
    // val isCustom: Boolean = false // Para distinguir entre predefinidos y creados por el usuario
)
