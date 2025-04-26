package com.example.gymtrackerviews // Tu paquete

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter // Para AutoCompleteTextView
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible // Para controlar visibilidad fácilmente
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gymtrackerviews.databinding.FragmentWorkoutDetailBinding // Binding del Fragment
import com.example.gymtrackerviews.databinding.DialogAddSetBinding // Binding del Dialogo
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class WorkoutDetailFragment : Fragment() {

    private var _binding: FragmentWorkoutDetailBinding? = null
    private val binding get() = _binding!!

    // Obtenemos la BD una vez para pasar DAOs a la Factory
    private val database by lazy { AppDatabase.getDatabase(requireContext().applicationContext) }
    // Obtenemos el ViewModel usando la Factory
    private val viewModel: WorkoutDetailViewModel by viewModels {
        WorkoutDetailViewModel.Factory(database.workoutDao(), database.workoutSetDao())
    }
    // Adapter para la lista de series
    private lateinit var workoutSetAdapter: WorkoutSetAdapter

    // --- onCreateView con firma correcta ---
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("WorkoutDetailFragment", "--- onViewCreated START (Workout ID from ViewModel: ${viewModel.workoutId}) ---")
        super.onViewCreated(view, savedInstanceState)

        // Mostramos el ID del workout (obtenido del ViewModel)
        binding.textViewDetailTitle.text = "Detalles del Workout ID: ${viewModel.workoutId}"

        // Configuramos RecyclerView y empezamos a observar datos
        setupSetsRecyclerView()
        observeViewModelData()

        // Listener para el botón "Añadir Serie"
        binding.buttonAddSet.setOnClickListener {
            Log.d("WorkoutDetailFragment", "Add Set button clicked")
            showAddOrEditSetDialog(null) // null indica que es para añadir
        }

        // Listener para el botón "Finalizar Workout"
        binding.buttonFinishWorkout.setOnClickListener {
            Log.d("WorkoutDetailFragment", "Finish Workout button clicked for workout ID: ${viewModel.workoutId}")
            viewModel.finishWorkout() // Llama al ViewModel
            Toast.makeText(context, "Workout finalizado", Toast.LENGTH_SHORT).show()
        }
        Log.d("WorkoutDetailFragment", "--- onViewCreated END ---")
    }

    // Observa todos los StateFlows del ViewModel
    private fun observeViewModelData() {
        Log.d("WorkoutDetailFragment", "Starting to observe ViewModel data")
        viewLifecycleOwner.lifecycleScope.launch {
            // Usamos repeatOnLifecycle para observar solo cuando el fragment está visible
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 1. Observar detalles del Workout
                launch {
                    viewModel.workoutDetails.collectLatest { workout ->
                        if (workout != null) {
                            Log.d("WorkoutDetailFragment", "Workout details updated from ViewModel: $workout")
                            val dateFormat = java.text.SimpleDateFormat("dd MMM yy, HH:mm:ss", java.util.Locale.getDefault())
                            binding.textViewDetailStartTime.text = "Iniciado: ${dateFormat.format(workout.startTime)}"
                        } else {
                            Log.w("WorkoutDetailFragment", "Workout details from ViewModel are null.")
                            binding.textViewDetailStartTime.text = "Iniciado: (Cargando...)"
                        }
                    }
                }
                // 2. Observar lista de Series y gestionar visibilidad
                launch {
                    viewModel.workoutSets.collectLatest { setsList ->
                        Log.d("WorkoutDetailFragment", "Sets list updated from ViewModel. Count: ${setsList.size}")
                        // Gestión de visibilidad de lista/mensaje vacío
                        val isEmpty = setsList.isEmpty()
                        binding.recyclerViewSets.isVisible = !isEmpty
                        binding.textViewEmptySets.isVisible = isEmpty

                        // Actualizar adapter
                        if(::workoutSetAdapter.isInitialized) {
                            workoutSetAdapter.submitList(setsList)
                        } else {
                            Log.e("WorkoutDetailFragment", "WorkoutSetAdapter not initialized when trying to submit list.")
                        }
                    }
                }
                // 3. Observar estado 'finalizado' y gestionar botones
                launch {
                    viewModel.isWorkoutFinished.collectLatest { isFinished ->
                        Log.d("WorkoutDetailFragment", "Workout finished state updated: $isFinished")
                        // Ocultar botón "Finalizar" si ya está finalizado
                        binding.buttonFinishWorkout.isVisible = !isFinished
                        // Deshabilitar botón "Añadir Serie" si ya está finalizado
                        binding.buttonAddSet.isEnabled = !isFinished
                    }
                }
            } // Fin repeatOnLifecycle
        } // Fin launch principal
    }

    // Configura el RecyclerView y su adapter (con lambdas para clicks)
    private fun setupSetsRecyclerView() {
        workoutSetAdapter = WorkoutSetAdapter(
            onItemClick = { setToEdit ->
                Log.d("WorkoutDetailFragment", "Item Clicked - Set ID: ${setToEdit.id}")
                showAddOrEditSetDialog(setToEdit) // Llama al diálogo en modo Edición
            },
            onDeleteClick = { setToDelete ->
                Log.d("WorkoutDetailFragment", "Delete Clicked - Set ID: ${setToDelete.id}")
                showDeleteConfirmationDialog(setToDelete) // Llama al diálogo de borrado
            }
        )
        binding.recyclerViewSets.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = workoutSetAdapter
        }
        Log.d("WorkoutDetailFragment", "Sets RecyclerView setup complete.")
    }

    // Muestra el diálogo para añadir o editar una serie
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

        // Si editamos, rellenamos campos
        if (isEditing && existingSet != null) {
            dialogBinding.editTextExerciseName.setText(existingSet.exerciseName, false)
            dialogBinding.editTextReps.setText(existingSet.repetitions.toString())
            dialogBinding.editTextWeight.setText(existingSet.weight.toString())
            Log.d("WorkoutDetailFragment", "Dialog pre-filled for editing set ID: ${existingSet.id}")
        }

        // Construimos y mostramos el diálogo
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(if (isEditing) "Editar Serie" else "Añadir Nueva Serie")
        builder.setView(dialogBinding.root)
        builder.setPositiveButton(if (isEditing) "Actualizar" else "Guardar") { dialog, _ ->
            // Lógica de guardar/actualizar
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

    // Muestra diálogo para confirmar borrado de serie
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

    // Limpia el binding cuando la vista se destruye
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d("WorkoutDetailFragment", "--- onDestroyView ---")
    }
} // --- FIN de la clase WorkoutDetailFragment ---
