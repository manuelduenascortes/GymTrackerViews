package com.example.gymtrackerviews // Tu paquete

// --- IMPORTS ---
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton // Import MaterialButton
import com.example.gymtrackerviews.databinding.FragmentWorkoutDetailBinding
import com.example.gymtrackerviews.databinding.DialogAddSetBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import android.view.MenuItem
import kotlin.math.max
// --- FIN IMPORTS ---

// ================================================================
// --- INICIO DE LA CLASE WorkoutDetailFragment ---
// ================================================================
class WorkoutDetailFragment : Fragment() { // <-- Llave de apertura de la CLASE

    // --- Propiedades ---
    private var _binding: FragmentWorkoutDetailBinding? = null
    private val binding get() = _binding!!

    private val database by lazy { AppDatabase.getDatabase(requireContext().applicationContext) }
    private val viewModel: WorkoutDetailViewModel by viewModels {
        WorkoutDetailViewModel.Factory(database.workoutDao(), database.workoutSetDao())
    }
    private lateinit var workoutDetailAdapter: WorkoutDetailAdapter

    private var countDownTimer: CountDownTimer? = null
    private var timerIsRunning = false
    private var startRestTimeInMillis: Long = 90000
    private var timeLeftInMillis: Long = startRestTimeInMillis
    private val timerStepMillis: Long = 15000
    private val minRestTimeMillis: Long = 15000
    private val maxRestTimeMillis: Long = 300000
    // --- Fin Propiedades ---


    // --- onCreateView (COMPLETO Y CORRECTO) ---
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View { // Debe devolver View
        Log.d("WorkoutDetailFragment", "--- onCreateView START ---")
        _binding = FragmentWorkoutDetailBinding.inflate(inflater, container, false)
        Log.d("WorkoutDetailFragment", "--- onCreateView END ---")
        return binding.root // Devuelve la vista inflada
    }
    // --- FIN onCreateView ---

    // --- onViewCreated (COMPLETO) ---
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("WorkoutDetailFragment", "--- onViewCreated START (Workout ID from ViewModel: ${viewModel.workoutId}) ---")
        super.onViewCreated(view, savedInstanceState) // Llamada a super

        setupToolbar() // Configura la Toolbar
        setupSetsRecyclerView() // Configura el RecyclerView
        observeViewModelData() // Empieza a observar datos

        // Listeners Botones Temporizador
        binding.buttonTimerToggle.setOnClickListener {
            if (timerIsRunning) { pauseTimer() } else { startTimer() }
        }
        binding.buttonTimerReset.setOnClickListener { resetTimer() }
        binding.buttonTimerDecrease.setOnClickListener { adjustTimer(-timerStepMillis) }
        binding.buttonTimerIncrease.setOnClickListener { adjustTimer(timerStepMillis) }

        updateTimerUI() // UI inicial del timer

        // Listeners otros botones
        binding.buttonAddSet.setOnClickListener {
            Log.d("WorkoutDetailFragment", "Add Set button clicked. Is Enabled: ${binding.buttonAddSet.isEnabled}")
            if (binding.buttonAddSet.isEnabled) { showAddOrEditSetDialog(null) }
            else { Toast.makeText(context, "El workout ya está finalizado.", Toast.LENGTH_SHORT).show() }
        }
        binding.buttonFinishWorkout.setOnClickListener {
            Log.d("WorkoutDetailFragment", "Finish Workout button clicked for workout ID: ${viewModel.workoutId}")
            if (binding.buttonFinishWorkout.isVisible) {
                saveNotes() // Guardar notas ANTES de finalizar
                Log.d("WorkoutDetailFragment", "Notes saved before finishing.")
                viewModel.finishWorkout(binding.editTextWorkoutNotes.text.toString())
                Toast.makeText(context, "Workout finalizado", Toast.LENGTH_SHORT).show()
            }
        }

        Log.d("WorkoutDetailFragment", "--- onViewCreated END ---")
    }
    // --- FIN onViewCreated ---

    // --- setupToolbar (COMPLETO) ---
    private fun setupToolbar() {
        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        NavigationUI.setupWithNavController(binding.toolbarDetail, navController, appBarConfiguration)
        (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbarDetail)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayShowHomeEnabled(true)
        setHasOptionsMenu(true)
    }
    // --- FIN setupToolbar ---

    // --- onOptionsItemSelected (COMPLETO) ---
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            findNavController().navigateUp()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    // --- FIN onOptionsItemSelected ---

    // --- observeViewModelData (COMPLETO) ---
    private fun observeViewModelData() {
        Log.d("WorkoutDetailFragment", "Starting to observe ViewModel data")
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observar detalles del Workout
                launch {
                    viewModel.workoutDetails.collectLatest { workout ->
                        if (workout != null) {
                            val dateFormatTitle = java.text.SimpleDateFormat("dd MMM yy, HH:mm", java.util.Locale.getDefault())
                            (activity as? AppCompatActivity)?.supportActionBar?.title = "Workout ${dateFormatTitle.format(workout.startTime)}"
                            val dateFormatFull = java.text.SimpleDateFormat("dd MMM yy, HH:mm:ss", java.util.Locale.getDefault())
                            binding.textViewDetailEndTime.isVisible = workout.endTime != null
                            workout.endTime?.let { binding.textViewDetailEndTime.text = "Finalizado: ${dateFormatFull.format(it)}" }
                            val notes = workout.notes
                            if (_binding != null && binding.editTextWorkoutNotes.isEnabled && binding.editTextWorkoutNotes.text.toString() != (notes ?: "")) {
                                binding.editTextWorkoutNotes.setText(notes ?: "")
                            }
                        } else {
                            (activity as? AppCompatActivity)?.supportActionBar?.title = "Cargando Workout..."
                            binding.textViewDetailEndTime.isVisible = false
                        }
                    }
                }
                // Observar lista de Series
                launch {
                    viewModel.groupedWorkoutSets.collectLatest { groupedSetsMap ->
                        val detailListItems = mutableListOf<WorkoutDetailListItem>()
                        groupedSetsMap.toSortedMap().forEach { (exerciseName, sets) ->
                            detailListItems.add(WorkoutDetailListItem.HeaderItem(exerciseName))
                            sets.sortedBy { it.timestamp }.forEach { workoutSet ->
                                detailListItems.add(WorkoutDetailListItem.SetItem(workoutSet))
                            }
                        }
                        val isEmpty = detailListItems.isEmpty()
                        binding.recyclerViewSets.isVisible = !isEmpty
                        binding.textViewEmptySets.isVisible = isEmpty
                        if(::workoutDetailAdapter.isInitialized) {
                            workoutDetailAdapter.submitList(detailListItems)
                        }
                    }
                }
                // Observar estado 'finalizado'
                launch {
                    viewModel.isWorkoutFinished.collectLatest { isFinished ->
                        Log.d("WorkoutDetailFragment", "Observed isWorkoutFinished state: $isFinished")
                        _binding?.let { safeBinding ->
                            safeBinding.buttonFinishWorkout.isVisible = !isFinished
                            safeBinding.buttonAddSet.isEnabled = !isFinished
                            safeBinding.editTextWorkoutNotes.isEnabled = !isFinished
                            safeBinding.buttonTimerToggle.isEnabled = !isFinished
                            safeBinding.buttonTimerReset.isEnabled = !isFinished && timeLeftInMillis < startRestTimeInMillis
                            safeBinding.buttonTimerDecrease.isEnabled = !isFinished && !timerIsRunning
                            safeBinding.buttonTimerIncrease.isEnabled = !isFinished && !timerIsRunning
                            if(isFinished && timerIsRunning) { pauseTimer() }
                            Log.d("WorkoutDetailFragment", "Updated button/notes/timer enabled state based on isFinished=$isFinished")
                        }
                    }
                }
            } // Fin repeatOnLifecycle
        } // Fin launch principal
    }
    // --- FIN observeViewModelData ---

    // --- setupSetsRecyclerView (COMPLETO) ---
    private fun setupSetsRecyclerView() {
        workoutDetailAdapter = WorkoutDetailAdapter(
            onSetClick = { setToEdit ->
                Log.d("WorkoutDetailFragment", "Set Item Clicked - Set ID: ${setToEdit.id}")
                if (binding.buttonAddSet.isEnabled) { showAddOrEditSetDialog(setToEdit) }
                else { Toast.makeText(context, "Workout finalizado, no se puede editar.", Toast.LENGTH_SHORT).show() }
            },
            onSetDeleteClick = { setToDelete ->
                Log.d("WorkoutDetailFragment", "Set Delete Clicked - Set ID: ${setToDelete.id}")
                if (binding.buttonAddSet.isEnabled) { showDeleteConfirmationDialog(setToDelete) }
                else { Toast.makeText(context, "Workout finalizado, no se puede borrar.", Toast.LENGTH_SHORT).show() }
            }
        )
        binding.recyclerViewSets.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = workoutDetailAdapter
        }
        Log.d("WorkoutDetailFragment", "Sets RecyclerView setup complete with WorkoutDetailAdapter.")
    }
    // --- FIN setupSetsRecyclerView ---

    // --- showAddOrEditSetDialog (COMPLETO) ---
    private fun showAddOrEditSetDialog(existingSet: WorkoutSet?) {
        val dialogBinding = DialogAddSetBinding.inflate(LayoutInflater.from(requireContext()))
        val isEditing = existingSet != null

        // Configuración del AutoCompleteTextView
        try {
            val exercises: Array<String> = resources.getStringArray(R.array.default_exercise_list)
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, exercises)
            dialogBinding.editTextExerciseName.setAdapter(adapter)
            Log.d("WorkoutDetailFragment", "AutoCompleteTextView adapter set.")
        } catch (e: Exception) {
            Log.e("WorkoutDetailFragment", "Error setting AutoCompleteTextView adapter", e)
        }

        if (isEditing && existingSet != null) {
            dialogBinding.editTextExerciseName.setText(existingSet.exerciseName, false)
            dialogBinding.editTextReps.setText(existingSet.repetitions.toString())
            dialogBinding.editTextWeight.setText(existingSet.weight.toString())
            Log.d("WorkoutDetailFragment", "Dialog pre-filled for editing set ID: ${existingSet.id}")
        }

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(if (isEditing) "Editar Serie" else "Añadir Nueva Serie")
        builder.setView(dialogBinding.root)
        builder.setPositiveButton(if (isEditing) "Actualizar" else "Guardar") { dialog, _ ->
            val exerciseName = dialogBinding.editTextExerciseName.text.toString().trim()
            val repsString = dialogBinding.editTextReps.text.toString()
            val weightString = dialogBinding.editTextWeight.text.toString()

            if (exerciseName.isEmpty() || repsString.isEmpty() || weightString.isEmpty()) {
                Toast.makeText(context, "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            try {
                val reps = repsString.toInt()
                val weight = weightString.toDouble()

                if (reps <= 0) {
                    Toast.makeText(context, "Las repeticiones deben ser mayor que cero", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (weight < 0) {
                    Toast.makeText(context, "El peso no puede ser negativo", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (isEditing && existingSet != null) {
                    val updatedSet = existingSet.copy(exerciseName = exerciseName, repetitions = reps, weight = weight)
                    viewModel.updateSet(updatedSet)
                    Toast.makeText(context, "Serie actualizada", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.insertSet(exerciseName, reps, weight)
                    Toast.makeText(context, "Serie guardada", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            } catch (e: NumberFormatException) {
                Toast.makeText(context, "Introduce números válidos para Reps y Peso", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
        try {
            builder.create().show()
            Log.d("WorkoutDetailFragment", "Add/Edit dialog shown successfully. Editing: $isEditing")
        } catch (e: Exception) {
            Log.e("WorkoutDetailFragment", "Error showing Add/Edit dialog", e)
            Toast.makeText(context, "Error al mostrar el diálogo", Toast.LENGTH_SHORT).show()
        }
    }
    // --- FIN showAddOrEditSetDialog ---

    // --- showDeleteConfirmationDialog (COMPLETO) ---
    private fun showDeleteConfirmationDialog(setToDelete: WorkoutSet) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Borrado")
            .setMessage("¿Seguro que quieres borrar esta serie?\n(${setToDelete.exerciseName}: ${setToDelete.repetitions} reps @ ${setToDelete.weight} kg)")
            .setPositiveButton("Borrar") { _, _ ->
                viewModel.deleteSet(setToDelete)
                Log.d("WorkoutDetailFragment", "Delete confirmed for set ID: ${setToDelete.id}. Called viewModel.deleteSet")
                Toast.makeText(context, "Serie borrada", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
        Log.d("WorkoutDetailFragment", "Delete confirmation dialog shown for set ID: ${setToDelete.id}")
    }
    // --- FIN showDeleteConfirmationDialog ---

    // --- saveNotes (COMPLETO) ---
    private fun saveNotes() {
        if (_binding != null) {
            if (binding.editTextWorkoutNotes.isEnabled) {
                val notesFromEditText = binding.editTextWorkoutNotes.text.toString()
                Log.d("WorkoutDetailFragment", "Saving notes: '$notesFromEditText'")
                viewModel.saveNotes(notesFromEditText)
            } else {
                Log.d("WorkoutDetailFragment", "Notes EditText is disabled, skipping save.")
            }
        } else {
            Log.w("WorkoutDetailFragment", "Binding is null in saveNotes, cannot save.")
        }
    }
    // --- FIN saveNotes ---

    // --- onPause (COMPLETO) ---
    override fun onPause() {
        super.onPause() // Llamada a super OBLIGATORIA
        Log.d("WorkoutDetailFragment", "--- onPause START --- Attempting to call saveNotes...")
        saveNotes()
        Log.d("WorkoutDetailFragment", "--- onPause END --- Notes potentially saved.")
    }
    // --- FIN onPause ---

    // --- FUNCIONES TEMPORIZADOR (COMPLETAS) ---
    private fun startTimer() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimerUI()
            }
            override fun onFinish() {
                timerIsRunning = false
                timeLeftInMillis = startRestTimeInMillis
                updateTimerUI()
                context?.let { Toast.makeText(it, "¡Descanso terminado!", Toast.LENGTH_SHORT).show() }
            }
        }.start()
        timerIsRunning = true
        updateTimerUI()
        Log.d("WorkoutDetailFragment", "Timer started.")
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        timerIsRunning = false
        updateTimerUI()
        Log.d("WorkoutDetailFragment", "Timer paused.")
    }

    private fun resetTimer() {
        pauseTimer()
        timeLeftInMillis = startRestTimeInMillis
        updateTimerUI()
        Log.d("WorkoutDetailFragment", "Timer reset.")
    }

    private fun adjustTimer(changeMillis: Long) {
        if (!timerIsRunning) {
            var newStartTime = startRestTimeInMillis + changeMillis
            newStartTime = newStartTime.coerceIn(minRestTimeMillis, maxRestTimeMillis)
            if (newStartTime != startRestTimeInMillis) {
                startRestTimeInMillis = newStartTime
                timeLeftInMillis = startRestTimeInMillis
                updateTimerUI()
                Log.d("WorkoutDetailFragment", "Timer adjusted. New start time: ${startRestTimeInMillis / 1000}s")
            }
        } else {
            Log.d("WorkoutDetailFragment", "Cannot adjust timer while running.")
        }
    }

    private fun updateTimerUI() {
        if (_binding == null) return

        val minutes = (timeLeftInMillis / 1000) / 60
        val seconds = (timeLeftInMillis / 1000) % 60
        val timeFormatted = String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
        binding.textViewTimer.text = timeFormatted

        val iconResId = if (timerIsRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val iconDrawable = ContextCompat.getDrawable(requireContext(), iconResId)
        // Usamos setIcon en MaterialButton
        binding.buttonTimerToggle.setIcon(iconDrawable)

        val workoutFinished = !(binding.buttonAddSet.isEnabled)
        binding.buttonTimerReset.isEnabled = timeLeftInMillis < startRestTimeInMillis && !timerIsRunning && !workoutFinished
        binding.buttonTimerDecrease.isEnabled = !timerIsRunning && !workoutFinished
        binding.buttonTimerIncrease.isEnabled = !timerIsRunning && !workoutFinished
        if (startRestTimeInMillis <= minRestTimeMillis) binding.buttonTimerDecrease.isEnabled = false
        if (startRestTimeInMillis >= maxRestTimeMillis) binding.buttonTimerIncrease.isEnabled = false
    }
    // --- FIN FUNCIONES TEMPORIZADOR ---

    // --- onDestroyView (COMPLETO) ---
    override fun onDestroyView() {
        super.onDestroyView() // Llamada a super OBLIGATORIA
        countDownTimer?.cancel() // Cancelamos el timer
        _binding = null // Limpiamos el binding
        Log.d("WorkoutDetailFragment", "--- onDestroyView --- Timer cancelled. Binding set to null.")
    }
    // --- FIN onDestroyView ---

} // <--- ¡¡LLAVE DE CIERRE FINAL DE LA CLASE!! Asegúrate de que está y no hay nada después.
// ================================================================
// --- FIN DE LA CLASE WorkoutDetailFragment ---
// ================================================================
