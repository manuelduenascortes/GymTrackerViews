package com.example.gymtrackerviews // Tu paquete

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
// Quitamos import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gymtrackerviews.databinding.FragmentWorkoutDetailBinding
import com.example.gymtrackerviews.databinding.DialogAddSetBinding
import kotlinx.coroutines.flow.collectLatest // <--- ¡¡IMPORT AÑADIDO!! ---
import kotlinx.coroutines.launch
// Quitamos imports innecesarios como Date, SimpleDateFormat, Locale si no se usan aquí

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
        // Usamos el ID del ViewModel
        Log.d("WorkoutDetailFragment", "--- onViewCreated START (Workout ID from ViewModel: ${viewModel.workoutId}) ---")
        super.onViewCreated(view, savedInstanceState)

        binding.textViewDetailTitle.text = "Detalles del Workout ID: ${viewModel.workoutId}"

        setupSetsRecyclerView()
        observeViewModelData() // Llamamos a la función que observa el ViewModel

        binding.buttonAddSet.setOnClickListener {
            Log.d("WorkoutDetailFragment", "Add Set button clicked for workoutId: ${viewModel.workoutId}")
            showAddSetDialog()
        }
        Log.d("WorkoutDetailFragment", "--- onViewCreated END ---")
    }

    private fun observeViewModelData() {
        Log.d("WorkoutDetailFragment", "Starting to observe ViewModel data")
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Corutina para observar los detalles del workout
                launch {
                    viewModel.workoutDetails.collectLatest { workout -> // Ahora debería reconocer collectLatest
                        if (workout != null) {
                            Log.d("WorkoutDetailFragment", "Workout details updated from ViewModel: $workout")
                            // Necesitamos importar SimpleDateFormat y Locale aquí si los usamos
                            val dateFormat = java.text.SimpleDateFormat("dd MMM yy, HH:mm:ss", java.util.Locale.getDefault())
                            binding.textViewDetailStartTime.text = "Iniciado: ${dateFormat.format(workout.startTime)}"
                        } else {
                            Log.w("WorkoutDetailFragment", "Workout details from ViewModel are null.")
                            binding.textViewDetailStartTime.text = "Iniciado: (Cargando...)"
                        }
                    }
                }
                // Corutina para observar la lista de series
                launch {
                    viewModel.workoutSets.collectLatest { setsList -> // Ahora debería reconocer collectLatest
                        Log.d("WorkoutDetailFragment", "Sets list updated from ViewModel. Count: ${setsList.size}")
                        if(::workoutSetAdapter.isInitialized) {
                            workoutSetAdapter.submitList(setsList)
                        }
                    }
                }
            } // Fin repeatOnLifecycle
        } // Fin launch principal
    }


    private fun setupSetsRecyclerView() {
        workoutSetAdapter = WorkoutSetAdapter { setToDelete ->
            showDeleteConfirmationDialog(setToDelete)
        }
        binding.recyclerViewSets.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = workoutSetAdapter
        }
        Log.d("WorkoutDetailFragment", "Sets RecyclerView setup complete.")
    }

    private fun showAddSetDialog() {
        val dialogBinding = DialogAddSetBinding.inflate(LayoutInflater.from(requireContext()))
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Añadir Nueva Serie")
        builder.setView(dialogBinding.root)

        builder.setPositiveButton("Guardar") { dialog, _ ->
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
                viewModel.insertSet(exerciseName, reps, weight) // Llama al ViewModel
                Log.d("WorkoutDetailFragment", "Called viewModel.insertSet")
                dialog.dismiss()
            } catch (e: NumberFormatException) {
                Toast.makeText(context, "Introduce números válidos para Reps y Peso", Toast.LENGTH_SHORT).show()
            }
        } // Fin lambda botón Guardar

        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    } // --- FIN showAddSetDialog ---

    private fun showDeleteConfirmationDialog(setToDelete: WorkoutSet) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Borrado")
            .setMessage("¿Seguro que quieres borrar esta serie?\n(${setToDelete.exerciseName}: ${setToDelete.repetitions} reps @ ${setToDelete.weight} kg)")
            .setPositiveButton("Borrar") { _, _ ->
                viewModel.deleteSet(setToDelete) // Llama al ViewModel
                Log.d("WorkoutDetailFragment", "Called viewModel.deleteSet")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    } // --- FIN showDeleteConfirmationDialog ---

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d("WorkoutDetailFragment", "--- onDestroyView ---")
    }
}