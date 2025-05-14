package com.example.gymtrackerviews // O el paquete donde pongas tus ViewModels

import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale
import kotlin.math.max

// TODO: Asegúrate de que los imports para Workout, WorkoutSet, WorkoutDao, WorkoutSetDao son correctos
// import com.example.gymtrackerviews.Workout
// import com.example.gymtrackerviews.WorkoutSet
// import com.example.gymtrackerviews.WorkoutDao
// import com.example.gymtrackerviews.WorkoutSetDao


class WorkoutDetailViewModel(
    val workoutId: Long, // ID del workout actual
    private val workoutDao: WorkoutDao,
    private val workoutSetDao: WorkoutSetDao
) : ViewModel() {

    private val _workout = MutableStateFlow<Workout?>(null)
    val workout: StateFlow<Workout?> = _workout.asStateFlow()

    private val _workoutSets = MutableStateFlow<List<WorkoutSet>>(emptyList())
    val workoutSets: StateFlow<List<WorkoutSet>> = _workoutSets.asStateFlow()

    // --- Lógica del Temporizador ---
    private var countDownTimer: CountDownTimer? = null
    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _startRestTimeInMillis = MutableStateFlow(90000L)
    val startRestTimeInMillis: StateFlow<Long> = _startRestTimeInMillis.asStateFlow()

    private val _timerValue = MutableStateFlow(_startRestTimeInMillis.value)
    val timerValue: StateFlow<Long> = _timerValue.asStateFlow()

    private val timerStepMillis: Long = 15000L
    val minRestTimeMillis: Long = 15000L
    val maxRestTimeMillis: Long = 300000L

    init {
        loadWorkoutDetails()
        loadWorkoutSets()
    }

    private fun loadWorkoutDetails() {
        viewModelScope.launch {
            workoutDao.getWorkoutFlowById(workoutId).collectLatest {
                _workout.value = it
            }
        }
    }

    private fun loadWorkoutSets() {
        viewModelScope.launch {
            // CAMBIO AQUÍ: Usar el nombre correcto del método del DAO
            workoutSetDao.getSetsForWorkout(workoutId).collectLatest { // Antes era getSetsForWorkoutFlow
                _workoutSets.value = it.sortedBy { set -> set.timestamp }
            }
        }
    }

    fun updateWorkoutName(newName: String?) {
        viewModelScope.launch {
            val currentWorkout = _workout.value
            if (currentWorkout != null) {
                val nameToSave = if (newName.isNullOrBlank()) null else newName
                val updatedWorkout = currentWorkout.copy(name = nameToSave)
                workoutDao.updateWorkout(updatedWorkout)
                Log.d("WorkoutDetailVM", "Nombre del workout actualizado a: $nameToSave")
            }
        }
    }

    fun updateWorkoutNotes(notes: String?) {
        viewModelScope.launch {
            val currentWorkout = _workout.value
            if (currentWorkout != null && currentWorkout.endTime == null) {
                val notesToSave = if (notes.isNullOrBlank()) null else notes
                if (currentWorkout.notes != notesToSave) {
                    val updatedWorkout = currentWorkout.copy(notes = notesToSave)
                    workoutDao.updateWorkout(updatedWorkout)
                    Log.d("WorkoutDetailVM", "Notas del workout actualizadas.")
                }
            }
        }
    }

    fun finishWorkout(finalNotes: String?) {
        viewModelScope.launch {
            val currentWorkout = _workout.value
            if (currentWorkout != null && currentWorkout.endTime == null) {
                val notesToSave = if (finalNotes.isNullOrBlank()) null else finalNotes
                val finishedWorkout = currentWorkout.copy(
                    endTime = Date(), // endTime es Date
                    notes = notesToSave
                )
                workoutDao.updateWorkout(finishedWorkout)
                Log.d("WorkoutDetailVM", "Workout finalizado con ID: ${currentWorkout.id}")
                pauseTimer()
            }
        }
    }

    fun insertSet(exerciseName: String, reps: Int, weight: Double) {
        viewModelScope.launch {
            if (_workout.value?.endTime == null) {
                val newSet = WorkoutSet(
                    workoutId = workoutId,
                    exerciseName = exerciseName,
                    repetitions = reps,
                    weight = weight,
                    timestamp = Date() // CAMBIO AQUÍ: Usar Date() para el timestamp
                )
                workoutSetDao.insertSet(newSet)
                Log.d("WorkoutDetailVM", "Nueva serie añadida: $exerciseName")
            }
        }
    }

    fun updateSet(workoutSet: WorkoutSet) {
        viewModelScope.launch {
            if (_workout.value?.endTime == null) {
                workoutSetDao.updateSet(workoutSet) // Asumiendo que updateSet en DAO está bien
                Log.d("WorkoutDetailVM", "Serie actualizada: ${workoutSet.exerciseName}")
            }
        }
    }

    fun deleteWorkoutSet(workoutSet: WorkoutSet) {
        viewModelScope.launch {
            if (_workout.value?.endTime == null) {
                workoutSetDao.deleteSet(workoutSet) // Asumiendo que deleteSet en DAO está bien
                Log.d("WorkoutDetailVM", "Serie borrada: ${workoutSet.exerciseName}")
            }
        }
    }

    fun toggleTimer() {
        if (_workout.value?.endTime != null) return
        if (_isTimerRunning.value) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        if (_isTimerRunning.value || _workout.value?.endTime == null) return
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(_timerValue.value, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timerValue.value = millisUntilFinished
            }
            override fun onFinish() {
                _isTimerRunning.value = false
                _timerValue.value = _startRestTimeInMillis.value
                Log.d("WorkoutDetailVM", "Timer finished.")
            }
        }.start()
        _isTimerRunning.value = true
        Log.d("WorkoutDetailVM", "Timer started.")
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        _isTimerRunning.value = false
        Log.d("WorkoutDetailVM", "Timer paused.")
    }

    fun resetTimer() {
        if (_workout.value?.endTime != null) return
        pauseTimer()
        _timerValue.value = _startRestTimeInMillis.value
        Log.d("WorkoutDetailVM", "Timer reset.")
    }

    fun decreaseTimer() {
        if (_isTimerRunning.value || _workout.value?.endTime != null) return
        val newStartTime = (_startRestTimeInMillis.value - timerStepMillis).coerceAtLeast(minRestTimeMillis)
        if (newStartTime != _startRestTimeInMillis.value) {
            _startRestTimeInMillis.value = newStartTime
            _timerValue.value = _startRestTimeInMillis.value
            Log.d("WorkoutDetailVM", "Timer decreased. New start time: ${newStartTime / 1000}s")
        }
    }

    fun increaseTimer() {
        if (_isTimerRunning.value || _workout.value?.endTime != null) return
        val newStartTime = (_startRestTimeInMillis.value + timerStepMillis).coerceAtMost(maxRestTimeMillis)
        if (newStartTime != _startRestTimeInMillis.value) {
            _startRestTimeInMillis.value = newStartTime
            _timerValue.value = _startRestTimeInMillis.value
            Log.d("WorkoutDetailVM", "Timer increased. New start time: ${newStartTime / 1000}s")
        }
    }

    override fun onCleared() {
        super.onCleared()
        countDownTimer?.cancel()
        Log.d("WorkoutDetailVM", "ViewModel cleared, timer cancelled.")
    }
}
