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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
    // Usamos el NUEVO adapter
    private lateinit var workoutDetailAdapter: WorkoutDetailAdapter

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
        super.onViewCreated(view, savedInstanceState) // Llamada a super

        binding.textViewDetailTitle.text = "Detalles del Workout ID: ${viewModel.workoutId}"

        setupSetsRecyclerView() // Configura el NUEVO adapter
        observeViewModelData() // Observa los datos (incluyendo el mapa agrupado)

        binding.buttonAddSet.setOnClickListener {
            Log.d("WorkoutDetailFragment", "Add Set button clicked")
            if (binding.buttonAddSet.isEnabled) {
                showAddOrEditSetDialog(null)
            } else {
                Toast.makeText(context, "El workout ya está finalizado.", Toast.LENGTH_SHORT).show()
            }
        }

        // Listener botón Finalizar Workout (Llama a finishWorkout con notas)
        binding.buttonFinishWorkout.setOnClickListener {
            Log.d("WorkoutDetailFragment", "Finish Workout button clicked for workout ID: ${viewModel.workoutId}")
            if (binding.buttonFinishWorkout.isVisible) {
                // Obtenemos las notas ACTUALES del EditText
                val currentNotes = binding.editTextWorkoutNotes.text.toString()
                // Llamamos a finishWorkout PASANDO las notas
                viewModel.finishWorkout(currentNotes)
                Toast.makeText(context, "Workout finalizado", Toast.LENGTH_SHORT).show()
            }
        }
        Log.d("WorkoutDetailFragment", "--- onViewCreated END ---")
    }

    // Observa los datos del ViewModel
    private fun observeViewModelData() {
        Log.d("WorkoutDetailFragment", "Starting to observe ViewModel data")
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 1. Observar detalles del Workout (fechas y notas)
                launch {
                    viewModel.workoutDetails.collectLatest { workout ->
                        if (workout != null) {
                            Log.d("WorkoutDetailFragment", "Workout details updated (dates): $workout")
                            val dateFormat = java.text.SimpleDateFormat("dd MMM yy, HH:mm:ss", java.util.Locale.getDefault())
                            binding.textViewDetailStartTime.text = "Iniciado: ${dateFormat.format(workout.startTime)}"
                            binding.textViewDetailEndTime.isVisible = workout.endTime != null
                            workout.endTime?.let { binding.textViewDetailEndTime.text = "Finalizado: ${dateFormat.format(it)}" }
                            val notes = workout.notes
                            // Solo actualizamos si el texto es diferente y binding no es null
                            // y si el campo está habilitado
                            if (_binding != null && binding.editTextWorkoutNotes.isEnabled && binding.editTextWorkoutNotes.text.toString() != (notes ?: "")) {
                                Log.d("WorkoutDetailFragment", "Updating notes EditText: $notes")
                                binding.editTextWorkoutNotes.setText(notes ?: "")
                            }
                        } else {
                            Log.w("WorkoutDetailFragment", "Workout details from ViewModel are null (dates).")
                            binding.textViewDetailStartTime.text = "Iniciado: (Cargando...)"
                            binding.textViewDetailEndTime.isVisible = false
                        }
                    }
                }
                // 2. Observar el MAPA AGRUPADO y crear la lista plana para el adapter
                launch {
                    // Observamos groupedWorkoutSets
                    viewModel.groupedWorkoutSets.collectLatest { groupedSetsMap ->
                        Log.d("WorkoutDetailFragment", "Grouped sets map updated. Exercise count: ${groupedSetsMap.size}")
                        val detailListItems = mutableListOf<WorkoutDetailListItem>()
                        // Ordenar por nombre de ejercicio (opcional)
                        groupedSetsMap.toSortedMap().forEach { (exerciseName, sets) ->
                            detailListItems.add(WorkoutDetailListItem.HeaderItem(exerciseName))
                            // Ordenar series por timestamp (opcional, DAO ya lo hace)
                            sets.sortedBy { it.timestamp }.forEach { workoutSet ->
                                detailListItems.add(WorkoutDetailListItem.SetItem(workoutSet))
                            }
                        }

                        val isEmpty = detailListItems.isEmpty()
                        binding.recyclerViewSets.isVisible = !isEmpty
                        binding.textViewEmptySets.isVisible = isEmpty
                        Log.d("WorkoutDetailFragment", "Flat list created. Item count: ${detailListItems.size}. Is empty: $isEmpty")

                        // Usamos la variable correcta del adapter
                        if(::workoutDetailAdapter.isInitialized) {
                            workoutDetailAdapter.submitList(detailListItems) // Enviamos lista plana
                            Log.d("WorkoutDetailFragment", "Submitted flat list to adapter.")
                        } else {
                            Log.e("WorkoutDetailFragment", "WorkoutDetailAdapter not initialized!")
                        }
                    } // Fin collectLatest groupedWorkoutSets
                } // Fin launch series

                // 3. Observar estado 'finalizado'
                launch {
                    viewModel.isWorkoutFinished.collectLatest { isFinished ->
                        Log.d("WorkoutDetailFragment", "Observed isWorkoutFinished state: $isFinished")
                        // Asegurarse que binding no es null antes de modificar vistas
                        _binding?.let { safeBinding ->
                            safeBinding.buttonFinishWorkout.isVisible = !isFinished
                            safeBinding.buttonAddSet.isEnabled = !isFinished
                            safeBinding.editTextWorkoutNotes.isEnabled = !isFinished
                            Log.d("WorkoutDetailFragment", "Updated button/notes enabled state based on isFinished=$isFinished")
                        }
                    }
                }
            } // Fin repeatOnLifecycle
        } // Fin launch principal
    }

    // Configura el RecyclerView usando el NUEVO WorkoutDetailAdapter
    private fun setupSetsRecyclerView() {
        // Instanciamos el NUEVO adapter
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
        // Aplicamos config al RecyclerView
        binding.recyclerViewSets.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = workoutDetailAdapter // Asignamos el NUEVO adapter
        }
        Log.d("WorkoutDetailFragment", "Sets RecyclerView setup complete with WorkoutDetailAdapter.")
    }

    // Muestra el diálogo para añadir o editar una serie
    private fun showAddOrEditSetDialog(existingSet: WorkoutSet?) {
        // ... (código interno del diálogo sin cambios) ...
        Log.d("WorkoutDetailFragment", "showAddOrEditSetDialog called. Editing existingSet: ${existingSet?.id}")
        val dialogBinding = DialogAddSetBinding.inflate(LayoutInflater.from(requireContext()))
        val isEditing = existingSet != null
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

    // Función para guardar las notas actuales del EditText
    private fun saveNotes() {
        if (_binding != null) {
            if (binding.editTextWorkoutNotes.isEnabled) {
                val notesFromEditText = binding.editTextWorkoutNotes.text.toString()
                Log.d("WorkoutDetailFragment", "Saving notes: '$notesFromEditText'")
                viewModel.saveNotes(notesFromEditText) // Llama al ViewModel
            } else {
                Log.d("WorkoutDetailFragment", "Notes EditText is disabled, skipping save.")
            }
        } else {
            Log.w("WorkoutDetailFragment", "Binding is null in saveNotes, cannot save.")
        }
    }

    // --- onPause con llamada a super ---
    override fun onPause() {
        super.onPause() // <-- LLAMADA A SUPER PRIMERO Y OBLIGATORIA ---
        Log.d("WorkoutDetailFragment", "--- onPause START --- Attempting to call saveNotes...")
        saveNotes()
        Log.d("WorkoutDetailFragment", "--- onPause END --- Notes potentially saved.")
    }

    // --- onDestroyView con llamada a super ---
    override fun onDestroyView() {
        super.onDestroyView() // <-- LLAMADA A SUPER PRIMERO Y OBLIGATORIA ---
        _binding = null
        Log.d("WorkoutDetailFragment", "--- onDestroyView --- Binding set to null.")
    }

} // --- FIN de la clase WorkoutDetailFragment ---
