package com.example.gymtrackerviews // Tu paquete

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class WorkoutDetailFragment : Fragment() {

    private var _binding: FragmentWorkoutDetailBinding? = null
    private val binding get() = _binding!!

    private val database by lazy { AppDatabase.getDatabase(requireContext().applicationContext) }
    private val viewModel: WorkoutDetailViewModel by viewModels {
        WorkoutDetailViewModel.Factory(database.workoutDao(), database.workoutSetDao())
    }
    // El adapter se inicializa en onViewCreated, antes de observar datos
    private lateinit var workoutSetAdapter: WorkoutSetAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("WorkoutDetailFragment", "--- onCreateView START ---")
        _binding = FragmentWorkoutDetailBinding.inflate(inflater, container, false)
        Log.d("WorkoutDetailFragment", "--- onCreateView END ---")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("WorkoutDetailFragment", "--- onViewCreated START (Workout ID from ViewModel: ${viewModel.workoutId}) ---")
        super.onViewCreated(view, savedInstanceState) // Llamada a super importante

        binding.textViewDetailTitle.text = "Detalles del Workout ID: ${viewModel.workoutId}"

        // --- IMPORTANTE: Configurar RecyclerView PRIMERO ---
        // Aseguramos que el adapter esté listo antes de que la observación intente usarlo
        setupSetsRecyclerView()
        // ---------------------------------------------------

        // Empezamos a observar los datos del ViewModel
        observeViewModelData()

        // Listener para el botón "Añadir Serie"
        binding.buttonAddSet.setOnClickListener {
            Log.d("WorkoutDetailFragment", "Add Set button clicked")
            // Verificamos si está habilitado antes de mostrar
            if (binding.buttonAddSet.isEnabled) {
                showAddOrEditSetDialog(null)
            } else {
                Toast.makeText(context, "El workout ya está finalizado.", Toast.LENGTH_SHORT).show()
            }
        }

        // Listener para el botón "Finalizar Workout"
        binding.buttonFinishWorkout.setOnClickListener {
            Log.d("WorkoutDetailFragment", "Finish Workout button clicked for workout ID: ${viewModel.workoutId}")
            if (binding.buttonFinishWorkout.isVisible) {
                viewModel.finishWorkout()
                Toast.makeText(context, "Workout finalizado", Toast.LENGTH_SHORT).show()
            }
        }
        Log.d("WorkoutDetailFragment", "--- onViewCreated END ---")
    }

    // Observa todos los StateFlows del ViewModel
    private fun observeViewModelData() {
        Log.d("WorkoutDetailFragment", "Starting to observe ViewModel data")
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 1. Observar detalles del Workout
                launch {
                    viewModel.workoutDetails.collectLatest { workout ->
                        if (workout != null) {
                            Log.d("WorkoutDetailFragment", "Workout details updated: $workout")
                            val dateFormat = java.text.SimpleDateFormat("dd MMM yy, HH:mm:ss", java.util.Locale.getDefault())
                            binding.textViewDetailStartTime.text = "Iniciado: ${dateFormat.format(workout.startTime)}"
                            // Mostrar/Ocultar y formatear End Time
                            if (workout.endTime != null) {
                                binding.textViewDetailEndTime.text = "Finalizado: ${dateFormat.format(workout.endTime)}"
                                binding.textViewDetailEndTime.isVisible = true
                            } else {
                                binding.textViewDetailEndTime.isVisible = false
                            }
                        } else {
                            Log.w("WorkoutDetailFragment", "Workout details from ViewModel are null.")
                            binding.textViewDetailStartTime.text = "Iniciado: (Cargando...)"
                            binding.textViewDetailEndTime.isVisible = false
                        }
                    }
                }
                // 2. Observar lista de Series y gestionar visibilidad
                launch {
                    viewModel.workoutSets.collectLatest { setsList ->
                        Log.d("WorkoutDetailFragment", "Sets list updated. Count: ${setsList.size}")
                        val isEmpty = setsList.isEmpty()
                        binding.recyclerViewSets.isVisible = !isEmpty
                        binding.textViewEmptySets.isVisible = isEmpty
                        // El adapter ya debería estar inicializado por setupSetsRecyclerView()
                        workoutSetAdapter.submitList(setsList)
                    }
                }
                // 3. Observar estado 'finalizado' y gestionar botones
                launch {
                    viewModel.isWorkoutFinished.collectLatest { isFinished ->
                        Log.d("WorkoutDetailFragment", "Workout finished state updated: $isFinished")
                        binding.buttonFinishWorkout.isVisible = !isFinished
                        binding.buttonAddSet.isEnabled = !isFinished
                    }
                }
            } // Fin repeatOnLifecycle
        } // Fin launch principal
    }

    // Configura el RecyclerView de series y su adapter (AHORA INICIALIZA adapter aquí)
    private fun setupSetsRecyclerView() {
        // Creamos e inicializamos el adapter aquí
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
        // Aplicamos configuración al RecyclerView
        binding.recyclerViewSets.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = workoutSetAdapter // Asignamos el adapter ya creado
        }
        Log.d("WorkoutDetailFragment", "Sets RecyclerView setup complete.")
    }

    // Muestra el diálogo para añadir o editar una serie (sin cambios internos)
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

    // Muestra diálogo para confirmar borrado de serie (sin cambios internos)
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

    // --- onDestroyView CORREGIDO ---
    override fun onDestroyView() {
        super.onDestroyView() // <-- ¡¡LLAMADA A SUPER AÑADIDA!! ---
        _binding = null // Limpiamos el binding para evitar fugas de memoria
        Log.d("WorkoutDetailFragment", "--- onDestroyView ---")
    }
    // --- FIN onDestroyView ---

} // --- FIN de la clase WorkoutDetailFragment ---
