package com.example.gymtrackerviews // Tu paquete

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater // Import necesario
import android.view.View
import android.view.ViewGroup       // Import necesario
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gymtrackerviews.databinding.FragmentWorkoutDetailBinding
import com.example.gymtrackerviews.databinding.DialogAddSetBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

// --- INICIO DE LA CLASE WorkoutDetailFragment ---
class WorkoutDetailFragment : Fragment() {

    private var _binding: FragmentWorkoutDetailBinding? = null
    private val binding get() = _binding!!

    private val database by lazy { AppDatabase.getDatabase(requireContext().applicationContext) }
    private val viewModel: WorkoutDetailViewModel by viewModels {
        WorkoutDetailViewModel.Factory(database.workoutDao(), database.workoutSetDao())
    }
    private lateinit var workoutSetAdapter: WorkoutSetAdapter

    // --- onCreateView ---
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("WorkoutDetailFragment", "--- onCreateView START ---")
        _binding = FragmentWorkoutDetailBinding.inflate(inflater, container, false)
        Log.d("WorkoutDetailFragment", "--- onCreateView END ---")
        return binding.root
    }
    // --- FIN onCreateView ---

    // --- onViewCreated ---
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("WorkoutDetailFragment", "--- onViewCreated START (Workout ID from ViewModel: ${viewModel.workoutId}) ---")
        super.onViewCreated(view, savedInstanceState) // Llamada a super

        binding.textViewDetailTitle.text = "Detalles del Workout ID: ${viewModel.workoutId}"

        setupSetsRecyclerView()
        observeViewModelData()

        binding.buttonAddSet.setOnClickListener {
            Log.d("WorkoutDetailFragment", "Add Set button clicked")
            if (binding.buttonAddSet.isEnabled) {
                showAddOrEditSetDialog(null)
            } else {
                Toast.makeText(context, "El workout ya está finalizado.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonFinishWorkout.setOnClickListener {
            Log.d("WorkoutDetailFragment", "Finish Workout button clicked for workout ID: ${viewModel.workoutId}")
            if (binding.buttonFinishWorkout.isVisible) {
                viewModel.finishWorkout()
                Toast.makeText(context, "Workout finalizado", Toast.LENGTH_SHORT).show()
            }
        }
        Log.d("WorkoutDetailFragment", "--- onViewCreated END ---")
    }
    // --- FIN onViewCreated ---

    // --- observeViewModelData ---
    private fun observeViewModelData() {
        Log.d("WorkoutDetailFragment", "Starting to observe ViewModel data")
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observar detalles del Workout (fechas)
                launch {
                    viewModel.workoutDetails.collectLatest { workout ->
                        if (workout != null) {
                            Log.d("WorkoutDetailFragment", "Workout details updated (dates): $workout")
                            val dateFormat = java.text.SimpleDateFormat("dd MMM yy, HH:mm:ss", java.util.Locale.getDefault())
                            binding.textViewDetailStartTime.text = "Iniciado: ${dateFormat.format(workout.startTime)}"
                            if (workout.endTime != null) {
                                binding.textViewDetailEndTime.text = "Finalizado: ${dateFormat.format(workout.endTime)}"
                                binding.textViewDetailEndTime.isVisible = true
                            } else {
                                binding.textViewDetailEndTime.isVisible = false
                            }
                        } else {
                            Log.w("WorkoutDetailFragment", "Workout details from ViewModel are null (dates).")
                            binding.textViewDetailStartTime.text = "Iniciado: (Cargando...)"
                            binding.textViewDetailEndTime.isVisible = false
                        }
                    }
                }
                // Observar lista de Series
                launch {
                    viewModel.workoutSets.collectLatest { setsList ->
                        Log.d("WorkoutDetailFragment", "Sets list updated. Count: ${setsList.size}")
                        val isEmpty = setsList.isEmpty()
                        binding.recyclerViewSets.isVisible = !isEmpty
                        binding.textViewEmptySets.isVisible = isEmpty
                        if(::workoutSetAdapter.isInitialized) {
                            workoutSetAdapter.submitList(setsList)
                        }
                    }
                }
                // Observar estado 'finalizado'
                launch {
                    viewModel.isWorkoutFinished.collectLatest { isFinished ->
                        Log.d("WorkoutDetailFragment", "Workout finished state updated: $isFinished")
                        binding.buttonFinishWorkout.isVisible = !isFinished
                        binding.buttonAddSet.isEnabled = !isFinished
                        // Asegurarse que binding no es null antes de acceder
                        _binding?.let {
                            it.editTextWorkoutNotes.isEnabled = !isFinished
                        }
                    }
                }
                // Observar notas
                launch {
                    viewModel.workoutDetails
                        .map { it?.notes }
                        .distinctUntilChanged()
                        .collectLatest { notes ->
                            // Solo actualizamos si el texto es diferente y binding no es null
                            // y si el campo está habilitado (para no interferir si el usuario escribe mientras se deshabilita)
                            if (_binding != null && binding.editTextWorkoutNotes.isEnabled && binding.editTextWorkoutNotes.text.toString() != (notes ?: "")) {
                                Log.d("WorkoutDetailFragment", "Updating notes EditText: $notes")
                                binding.editTextWorkoutNotes.setText(notes ?: "")
                            }
                        }
                }
            } // Fin repeatOnLifecycle
        } // Fin launch principal
    }
    // --- FIN observeViewModelData ---

    // --- setupSetsRecyclerView ---
    private fun setupSetsRecyclerView() {
        workoutSetAdapter = WorkoutSetAdapter(
            onItemClick = { setToEdit ->
                Log.d("WorkoutDetailFragment", "Item Clicked - Set ID: ${setToEdit.id}")
                if (binding.buttonAddSet.isEnabled) {
                    showAddOrEditSetDialog(setToEdit)
                } else {
                    Toast.makeText(context, "Workout finalizado, no se puede editar.", Toast.LENGTH_SHORT).show()
                }
            },
            onDeleteClick = { setToDelete ->
                Log.d("WorkoutDetailFragment", "Delete Clicked - Set ID: ${setToDelete.id}")
                if (binding.buttonAddSet.isEnabled) {
                    showDeleteConfirmationDialog(setToDelete)
                } else {
                    Toast.makeText(context, "Workout finalizado, no se puede borrar.", Toast.LENGTH_SHORT).show()
                }
            }
        )
        binding.recyclerViewSets.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = workoutSetAdapter
        }
        Log.d("WorkoutDetailFragment", "Sets RecyclerView setup complete.")
    }
    // --- FIN setupSetsRecyclerView ---

    // --- showAddOrEditSetDialog ---
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

                if (isEditing && existingSet != null) {
                    val updatedSet = existingSet.copy(exerciseName = exerciseName, repetitions = reps, weight = weight)
                    viewModel.updateSet(updatedSet)
                    Log.d("WorkoutDetailFragment", "Called viewModel.updateSet for set ID: ${updatedSet.id}")
                    Toast.makeText(context, "Serie actualizada", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.insertSet(exerciseName, reps, weight)
                    Log.d("WorkoutDetailFragment", "Called viewModel.insertSet")
                    Toast.makeText(context, "Serie guardada", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            } catch (e: NumberFormatException) {
                Toast.makeText(context, "Introduce números válidos para Reps y Peso", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
        Log.d("WorkoutDetailFragment", "Add/Edit dialog shown. Editing: $isEditing")
    }
    // --- FIN showAddOrEditSetDialog ---

    // --- showDeleteConfirmationDialog ---
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

    // --- onPause ---
    override fun onPause() {
        super.onPause() // Llamar a super primero
        Log.d("WorkoutDetailFragment", "--- onPause START --- Attempting to call saveNotes...") // Log añadido
        saveNotes() // Llamamos a nuestra función para guardar
        Log.d("WorkoutDetailFragment", "--- onPause END --- Notes potentially saved.")
    }
    // --- FIN onPause ---

    // --- Función para guardar las notas actuales del EditText ---
    // 👇 ESTA FUNCIÓN DEBE ESTAR AQUÍ, DENTRO DE LA CLASE 👇
    private fun saveNotes() {
        // Comprobar si binding no es nulo (importante!)
        if (_binding != null) {
            // Solo guardar si el campo de notas está habilitado (para no guardar si se deshabilitó por finalizar workout)
            if (binding.editTextWorkoutNotes.isEnabled) {
                val notesFromEditText = binding.editTextWorkoutNotes.text.toString()
                // Podríamos añadir lógica para guardar solo si ha cambiado, pero por ahora guardamos siempre si está habilitado
                Log.d("WorkoutDetailFragment", "Saving notes: '$notesFromEditText'")
                viewModel.saveNotes(notesFromEditText) // Llama al ViewModel
            } else {
                Log.d("WorkoutDetailFragment", "Notes EditText is disabled, skipping save.")
            }
        } else {
            Log.w("WorkoutDetailFragment", "Binding is null in saveNotes, cannot save.")
        }
    }
    // 👆 --- FIN saveNotes --- 👆

    // --- onDestroyView ---
    override fun onDestroyView() {
        super.onDestroyView() // Llamada a super OBLIGATORIA
        _binding = null // Limpiamos el binding
        Log.d("WorkoutDetailFragment", "--- onDestroyView --- Binding set to null.")
    }
    // --- FIN onDestroyView ---

} // --- FIN de la clase WorkoutDetailFragment ---
