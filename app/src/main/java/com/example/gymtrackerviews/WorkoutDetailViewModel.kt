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

    private val _startRestTimeInMillis = MutableStateFlow(90000L) // 1:30 por defecto (90000 Long)
    val startRestTimeInMillis: StateFlow<Long> = _startRestTimeInMillis.asStateFlow()

    private val _timerValue = MutableStateFlow(_startRestTimeInMillis.value) // Inicializar con el valor de startRestTime
    val timerValue: StateFlow<Long> = _timerValue.asStateFlow()

    private val timerStepMillis: Long = 15000L
    val minRestTimeMillis: Long = 15000L
    val maxRestTimeMillis: Long = 300000L

    init {
        Log.i("WorkoutDetailVM", "ViewModel initialized for workoutId: $workoutId")
        loadWorkoutDetails()
        loadWorkoutSets()
        Log.d("WorkoutDetailVM", "Initial timerValue: ${_timerValue.value}, startRestTime: ${_startRestTimeInMillis.value}")
    }

    private fun loadWorkoutDetails() {
        viewModelScope.launch {
            workoutDao.getWorkoutFlowById(workoutId).collectLatest {
                _workout.value = it
                Log.d("WorkoutDetailVM", "Workout details loaded: ${it?.name}")
                // Si el workout está finalizado, asegurar que el timer no esté corriendo
                if (it?.endTime != null && _isTimerRunning.value) {
                    pauseTimer()
                    Log.d("WorkoutDetailVM", "Workout is finished, pausing timer if it was running.")
                }
            }
        }
    }

    private fun loadWorkoutSets() {
        viewModelScope.launch {
            workoutSetDao.getSetsForWorkout(workoutId).collectLatest {
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
                    endTime = Date(),
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
                    timestamp = Date()
                )
                workoutSetDao.insertSet(newSet)
                Log.d("WorkoutDetailVM", "Nueva serie añadida: $exerciseName")
            }
        }
    }

    fun updateSet(workoutSet: WorkoutSet) {
        viewModelScope.launch {
            if (_workout.value?.endTime == null) {
                workoutSetDao.updateSet(workoutSet)
                Log.d("WorkoutDetailVM", "Serie actualizada: ${workoutSet.exerciseName}")
            }
        }
    }

    fun deleteWorkoutSet(workoutSet: WorkoutSet) {
        viewModelScope.launch {
            if (_workout.value?.endTime == null) {
                workoutSetDao.deleteSet(workoutSet)
                Log.d("WorkoutDetailVM", "Serie borrada: ${workoutSet.exerciseName}")
            }
        }
    }

    fun toggleTimer() {
        Log.d("WorkoutDetailVM", "toggleTimer called. Workout finished: ${(_workout.value?.endTime != null)}. Timer running: ${_isTimerRunning.value}")
        if (_workout.value?.endTime != null) {
            Log.d("WorkoutDetailVM", "Workout already finished, not starting timer.")
            return
        }
        if (_isTimerRunning.value) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        Log.d("WorkoutDetailVM", "startTimer called. Current isTimerRunning: ${_isTimerRunning.value}, workout finished: ${(_workout.value?.endTime != null)}, timerValue: ${_timerValue.value}")
        if (_isTimerRunning.value || _workout.value?.endTime != null) {
            Log.w("WorkoutDetailVM", "startTimer: Not starting, already running or workout finished.")
            return
        }
        if (_timerValue.value <= 0) {
            Log.w("WorkoutDetailVM", "startTimer: Not starting, timerValue is 0 or less. Resetting to startRestTime.")
            _timerValue.value = _startRestTimeInMillis.value // Asegurar que hay tiempo para iniciar
            if (_timerValue.value <= 0) { // Si startRestTime también es 0, no iniciar
                Log.e("WorkoutDetailVM", "startTimer: startRestTimeInMillis is also 0. Cannot start timer.")
                return
            }
        }

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(_timerValue.value, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timerValue.value = millisUntilFinished
                // Log.v("WorkoutDetailVM", "Timer tick: ${millisUntilFinished / 1000}s left") // Log muy verboso
            }
            override fun onFinish() {
                Log.d("WorkoutDetailVM", "Timer finished.")
                _isTimerRunning.value = false
                _timerValue.value = _startRestTimeInMillis.value
            }
        }.start()
        _isTimerRunning.value = true
        Log.i("WorkoutDetailVM", "Timer actually started with duration: ${_timerValue.value / 1000}s")
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        _isTimerRunning.value = false
        Log.i("WorkoutDetailVM", "Timer paused. Current timeLeft: ${_timerValue.value / 1000}s")
    }

    fun resetTimer() {
        Log.d("WorkoutDetailVM", "resetTimer called. Workout finished: ${(_workout.value?.endTime != null)}")
        if (_workout.value?.endTime != null) return
        pauseTimer()
        _timerValue.value = _startRestTimeInMillis.value
        Log.i("WorkoutDetailVM", "Timer reset to: ${_timerValue.value / 1000}s")
    }

    fun decreaseTimer() {
        Log.d("WorkoutDetailVM", "decreaseTimer called. isTimerRunning: ${_isTimerRunning.value}, workout finished: ${(_workout.value?.endTime != null)}")
        if (_isTimerRunning.value || _workout.value?.endTime != null) return
        val newStartTime = (_startRestTimeInMillis.value - timerStepMillis).coerceAtLeast(minRestTimeMillis)
        if (newStartTime != _startRestTimeInMillis.value) {
            _startRestTimeInMillis.value = newStartTime
            _timerValue.value = _startRestTimeInMillis.value
            Log.i("WorkoutDetailVM", "Timer decreased. New start time: ${newStartTime / 1000}s")
        }
    }

    fun increaseTimer() {
        Log.d("WorkoutDetailVM", "increaseTimer called. isTimerRunning: ${_isTimerRunning.value}, workout finished: ${(_workout.value?.endTime != null)}")
        if (_isTimerRunning.value || _workout.value?.endTime != null) return
        val newStartTime = (_startRestTimeInMillis.value + timerStepMillis).coerceAtMost(maxRestTimeMillis)
        if (newStartTime != _startRestTimeInMillis.value) {
            _startRestTimeInMillis.value = newStartTime
            _timerValue.value = _startRestTimeInMillis.value
            Log.i("WorkoutDetailVM", "Timer increased. New start time: ${newStartTime / 1000}s")
        }
    }

    override fun onCleared() {
        super.onCleared()
        countDownTimer?.cancel()
        Log.d("WorkoutDetailVM", "ViewModel cleared, timer cancelled.")
    }
}
