package com.example.gymtrackerviews // Tu paquete

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater // Import necesario
import android.view.View
import android.view.ViewGroup       // Import necesario
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gymtrackerviews.databinding.FragmentWorkoutDetailBinding // Tu ViewBinding
import com.example.gymtrackerviews.databinding.DialogAddSetBinding // Binding del Dialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class WorkoutDetailFragment : Fragment() {

    private var _binding: FragmentWorkoutDetailBinding? = null
    private val binding get() = _binding!!

    private val args: WorkoutDetailFragmentArgs by navArgs()
    private lateinit var database: AppDatabase
    private val workoutId by lazy { args.workoutId }
    private lateinit var workoutSetAdapter: WorkoutSetAdapter

    // --- onCreateView CON LA FIRMA CORRECTA Y COMPLETA ---
    override fun onCreateView(
        inflater: LayoutInflater,    // <-- PARÁMETRO 1
        container: ViewGroup?,       // <-- PARÁMETRO 2
        savedInstanceState: Bundle?  // <-- PARÁMETRO 3
    ): View {                        // <-- TIPO DE RETORNO
        Log.d("WorkoutDetailFragment", "--- onCreateView START ---")
        _binding = FragmentWorkoutDetailBinding.inflate(inflater, container, false) // Se usan inflater y container
        try {
            database = AppDatabase.getDatabase(requireContext().applicationContext)
            Log.d("WorkoutDetailFragment", "Database instance obtained.")
        } catch (e: Exception) {
            Log.e("WorkoutDetailFragment", "Error getting database instance", e)
            Toast.makeText(requireContext(), "Error crítico al iniciar la base de datos", Toast.LENGTH_LONG).show()
        }
        Log.d("WorkoutDetailFragment", "--- onCreateView END ---")
        return binding.root
    }
    // --- FIN onCreateView ---


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("WorkoutDetailFragment", "--- onViewCreated START (Workout ID: $workoutId) ---")
        super.onViewCreated(view, savedInstanceState)

        binding.textViewDetailTitle.text = "Detalles del Workout ID: $workoutId"

        if (::database.isInitialized) {
            observeWorkoutDetails()
            setupSetsRecyclerView()
            observeWorkoutSets()
        } else {
            Log.e("WorkoutDetailFragment", "Database not initialized.")
        }

        binding.buttonAddSet.setOnClickListener {
            Log.d("WorkoutDetailFragment", "Add Set button clicked for workoutId: $workoutId")
            showAddSetDialog()
        }
        Log.d("WorkoutDetailFragment", "--- onViewCreated END ---")
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
                val newSet = WorkoutSet(
                    workoutId = workoutId,
                    exerciseName = exerciseName,
                    repetitions = reps,
                    weight = weight
                )
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val setId = database.workoutSetDao().insertSet(newSet)
                        Log.d("WorkoutDetailFragment", "Set inserted with ID: $setId")
                        Toast.makeText(context, "Serie guardada (ID: $setId)", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e("WorkoutDetailFragment", "Error inserting set", e)
                        Toast.makeText(context, "Error al guardar la serie", Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()

            } catch (e: NumberFormatException) {
                Toast.makeText(context, "Introduce números válidos para Reps y Peso", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun observeWorkoutDetails() {
        Log.d("WorkoutDetailFragment", "Starting to observe workout details for ID: $workoutId")
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                database.workoutDao().getWorkoutFlowById(workoutId).collectLatest { workout ->
                    if (workout != null) {
                        Log.d("WorkoutDetailFragment", "Workout details received: $workout")
                        val dateFormat = SimpleDateFormat("dd MMM yy, HH:mm:ss", Locale.getDefault())
                        binding.textViewDetailStartTime.text = "Iniciado: ${dateFormat.format(workout.startTime)}"
                    } else {
                        Log.w("WorkoutDetailFragment", "Workout with ID $workoutId not found.")
                        binding.textViewDetailStartTime.text = "Iniciado: (No encontrado)"
                    }
                }
            } catch (e: Exception) {
                Log.e("WorkoutDetailFragment", "Error observing workout details", e)
                binding.textViewDetailStartTime.text = "Iniciado: (Error al cargar)"
            }
        }
    }

    private fun setupSetsRecyclerView() {
        workoutSetAdapter = WorkoutSetAdapter()
        binding.recyclerViewSets.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = workoutSetAdapter
        }
        Log.d("WorkoutDetailFragment", "Sets RecyclerView setup complete.")
    }

    private fun observeWorkoutSets() {
        Log.d("WorkoutDetailFragment", "Starting to observe sets for Workout ID: $workoutId")
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                database.workoutSetDao().getSetsForWorkout(workoutId).collectLatest { setsList ->
                    Log.d("WorkoutDetailFragment", "Sets list updated. Count: ${setsList.size}")
                    workoutSetAdapter.submitList(setsList)
                }
            } catch (e: Exception) {
                Log.e("WorkoutDetailFragment", "Error observing workout sets", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d("WorkoutDetailFragment", "--- onDestroyView ---")
    }
}