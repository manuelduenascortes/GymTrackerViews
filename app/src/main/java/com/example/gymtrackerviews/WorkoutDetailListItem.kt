package com.example.gymtrackerviews // Tu paquete

// Clase sellada para representar los diferentes tipos de items en la lista de detalle
sealed class WorkoutDetailListItem {
    // Data class para representar una cabecera (nombre del ejercicio)
    data class HeaderItem(val exerciseName: String) : WorkoutDetailListItem() {
        // Sobreescribimos id para DiffUtil
        override val id: String = "header_$exerciseName"
    }

    // Data class para representar una fila de serie (contiene el objeto WorkoutSet)
    data class SetItem(val workoutSet: WorkoutSet) : WorkoutDetailListItem() {
        // Sobreescribimos id para DiffUtil
        override val id: String = "set_${workoutSet.id}"
    }

    // Propiedad abstracta para el ID único (requerido por la sealed class)
    // DiffUtil usará esto para identificar items de forma única.
    abstract val id: String
}
