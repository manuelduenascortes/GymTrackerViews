package com.example.gymtrackerviews // Tu paquete

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
        super.onViewCreated(view, savedInstanceState)

        binding.textViewDetailTitle.text = "Detalles del Workout ID: ${viewModel.workoutId}"

        // Setup siempre, la observación controlará la visibilidad si la BD falla
        setupSetsRecyclerView()
        observeViewModelData() // Esta función ahora observa ambos flows

        binding.buttonAddSet.setOnClickListener {
            Log.d("WorkoutDetailFragment", "Add Set button clicked")
            showAddOrEditSetDialog(null)
        }
        Log.d("WorkoutDetailFragment", "--- onViewCreated END ---")
    }

    private fun observeViewModelData() {
        Log.d("WorkoutDetailFragment", "Starting to observe ViewModel data")
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Corutina para observar los detalles del workout
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
                // Corutina para observar la lista de series y gestionar visibilidad
                launch {
                    viewModel.workoutSets.collectLatest { setsList ->
                        Log.d("WorkoutDetailFragment", "Sets list updated from ViewModel. Count: ${setsList.size}")

                        // --- Lógica de Visibilidad ---
                        if (setsList.isEmpty()) {
                            binding.recyclerViewSets.visibility = View.GONE // Oculta RecyclerView
                            binding.textViewEmptySets.visibility = View.VISIBLE // Muestra mensaje
                        } else {
                            binding.recyclerViewSets.visibility = View.VISIBLE // Muestra RecyclerView
                            binding.textViewEmptySets.visibility = View.GONE // Oculta mensaje
                        }
                        // --- Fin Lógica de Visibilidad ---

                        if(::workoutSetAdapter.isInitialized) {
                            workoutSetAdapter.submitList(setsList)
                        }
                    }
                } // Fin launch series
            } // Fin repeatOnLifecycle
        } // Fin launch principal
    }


    private fun setupSetsRecyclerView() {
        workoutSetAdapter = WorkoutSetAdapter(
            onItemClick = { setToEdit ->
                Log.d("WorkoutDetailFragment", "Item Clicked - Set ID: ${setToEdit.id}")
                showAddOrEditSetDialog(setToEdit)
            },
            onDeleteClick = { setToDelete ->
                Log.d("WorkoutDetailFragment", "Delete Clicked - Set ID: ${setToDelete.id}")
                showDeleteConfirmationDialog(setToDelete)
            }
        )
        binding.recyclerViewSets.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = workoutSetAdapter
        }
        Log.d("WorkoutDetailFragment", "Sets RecyclerView setup complete.")
    }

    private fun showAddOrEditSetDialog(existingSet: WorkoutSet?) {
        val dialogBinding = DialogAddSetBinding.inflate(LayoutInflater.from(requireContext()))
        val isEditing = existingSet != null

        if (isEditing) {
            dialogBinding.editTextExerciseName.setText(existingSet?.exerciseName ?: "")
            dialogBinding.editTextReps.setText(existingSet?.repetitions?.toString() ?: "")
            dialogBinding.editTextWeight.setText(existingSet?.weight?.toString() ?: "")
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
                    val updatedSet = existingSet.copy(
                        exerciseName = exerciseName,
                        repetitions = reps,
                        weight = weight
                    )
                    viewModel.updateSet(updatedSet)
                    Log.d("WorkoutDetailFragment", "Called viewModel.updateSet")
                    Toast.makeText(context, "Serie actualizada", Toast.LENGTH_SHORT).show()

                } else {
                    viewModel.insertSet(exerciseName, reps, weight)
                    Log.d("WorkoutDetailFragment", "Called viewModel.insertSet")
                    Toast.makeText(context, "Serie guardada", Toast.LENGTH_SHORT).show() // Puedes añadir un Toast aquí si quieres
                }
                dialog.dismiss()

            } catch (e: NumberFormatException) {
                Toast.makeText(context, "Introduce números válidos para Reps y Peso", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }


    private fun showDeleteConfirmationDialog(setToDelete: WorkoutSet) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Borrado")
            .setMessage("¿Seguro que quieres borrar esta serie?\n(${setToDelete.exerciseName}: ${setToDelete.repetitions} reps @ ${setToDelete.weight} kg)")
            .setPositiveButton("Borrar") { _, _ ->
                viewModel.deleteSet(setToDelete)
                Log.d("WorkoutDetailFragment", "Called viewModel.deleteSet")
                Toast.makeText(context, "Serie borrada", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d("WorkoutDetailFragment", "--- onDestroyView ---")
    }
}