package com.example.gymtrackerviews // O el paquete donde pongas tus ViewModels y Factories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gymtrackerviews.ExerciseDao // Asegúrate que el import es correcto
import com.example.gymtrackerviews.WorkoutDao
import com.example.gymtrackerviews.WorkoutSetDao

// import com.example.gymtrackerviews.WorkoutDetailViewModel // Asegúrate que este import es correcto si no está en el mismo paquete

class WorkoutDetailViewModelFactory(
    private val workoutId: Long,
    private val workoutDao: WorkoutDao,
    private val workoutSetDao: WorkoutSetDao,
    private val exerciseDao: ExerciseDao // <<< NUEVO: ExerciseDao añadido
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // V PASAMOS exerciseDao al ViewModel V
            return WorkoutDetailViewModel(workoutId, workoutDao, workoutSetDao, exerciseDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}