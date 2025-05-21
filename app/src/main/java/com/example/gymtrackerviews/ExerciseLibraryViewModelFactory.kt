package com.example.gymtrackerviews.ui.library // TODO: Asegúrate que este paquete coincide con ExerciseLibraryViewModel.kt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gymtrackerviews.ExerciseDao // TODO: Ajusta el import si tu ExerciseDao está en otro paquete (ej. com.example.gymtrackerviews.data.ExerciseDao)
// Si ExerciseLibraryViewModel está en este mismo paquete, no necesitas importarlo explícitamente.
// Si está en otro, necesitarías: import com.example.gymtrackerviews.paquete.del.viewmodel.ExerciseLibraryViewModel

class ExerciseLibraryViewModelFactory(
    private val exerciseDao: ExerciseDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExerciseLibraryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExerciseLibraryViewModel(exerciseDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for ExerciseLibrary")
    }
}
