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

// Asegúrate que hereda de Fragment()
class WorkoutDetailFragment : Fragment() { // <--- Llave de apertura de la CLASE

    private var _binding: FragmentWorkoutDetailBinding? = null
    private val binding get() = _binding!!

    private val args: WorkoutDetailFragmentArgs by navArgs()
    private lateinit var database: AppDatabase
    private val workoutId by lazy { args.workoutId }
    private lateinit var workoutSetAdapter: WorkoutSetAdapter

    // --- onCreateView CON LA FIRMA CORRECTA Y COMPLETA ---
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("WorkoutDetailFragment", "--- onCreateView START ---")
        _binding = FragmentWorkoutDetailBinding.inflate(inflater, container, false)
        try {
            database = AppDatabase.getDatabase(requireContext().applicationContext)
            Log.d("WorkoutDetailFragment", "Database instance obtained.")
        } catch (e: Exception) {
            Log.e("WorkoutDetailFragment", "Error getting database instance", e)
            Toast.makeText(requireContext(), "Error crítico al iniciar la base de datos", Toast.LENGTH_LONG).show()
        }
        Log.d("WorkoutDetailFragment", "--- onCreateView END ---")
        return binding.root
    } // --- FIN onCreateView ---


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
        } // <-- Cierre del listener del botón
        Log.d("WorkoutDetailFragment", "--- onViewCreated END ---")
    } // --- FIN onViewCreated ---

    private fun showAddSetDialog() {
        val dialogBinding = DialogAddSetBinding.inflate(LayoutInflater.from(requireContext()))
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Añadir Nueva Serie")
        builder.setView(dialogBinding.root)

        builder.setPositiveButton("Guardar") { dialog, _ -> // Inicio lambda botón Guardar
            val exerciseName = dialogBinding.editTextExerciseName.text.toString().trim()
            val repsString = dialogBinding.editTextReps.text.toString()
            val weightString = dialogBinding.editTextWeight.text.toString()

            if (exerciseName.isEmpty() || repsString.isEmpty() || weightString.isEmpty()) {
                Toast.makeText(context, "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            } // Fin if validación

            try { // Inicio try conversión números
                val reps = repsString.toInt()
                val weight = weightString.toDouble()
                val newSet = WorkoutSet(
                    workoutId = workoutId,
                    exerciseName = exerciseName,
                    repetitions = reps,
                    weight = weight
                )
                viewLifecycleOwner.lifecycleScope.launch { // Inicio corutina
                    try { // Inicio try inserción BD
                        val setId = database.workoutSetDao().insertSet(newSet)
                        Log.d("WorkoutDetailFragment", "Set inserted with ID: $setId")
                        Toast.makeText(context, "Serie guardada (ID: $setId)", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) { // Inicio catch inserción BD
                        Log.e("WorkoutDetailFragment", "Error inserting set", e)
                        Toast.makeText(context, "Error al guardar la serie", Toast.LENGTH_SHORT).show()
                    } // Fin catch inserción BD
                } // Fin corutina
                dialog.dismiss()

            } catch (e: NumberFormatException) { // Inicio catch conversión números
                Toast.makeText(context, "Introduce números válidos para Reps y Peso", Toast.LENGTH_SHORT).show()
            } // Fin catch conversión números
        } // Fin lambda botón Guardar

        builder.setNegativeButton("Cancelar") { dialog, _ -> // Inicio lambda botón Cancelar
            dialog.dismiss()
        } // Fin lambda botón Cancelar
        builder.create().show()
    } // --- FIN showAddSetDialog ---

    private fun observeWorkoutDetails() {
        Log.d("WorkoutDetailFragment", "Starting to observe workout details for ID: $workoutId")
        viewLifecycleOwner.lifecycleScope.launch { // Inicio corutina
            try { // Inicio try
                database.workoutDao().getWorkoutFlowById(workoutId).collectLatest { workout -> // Inicio collectLatest
                    if (workout != null) { // Inicio if
                        Log.d("WorkoutDetailFragment", "Workout details received: $workout")
                        val dateFormat = SimpleDateFormat("dd MMM yy, HH:mm:ss", Locale.getDefault())
                        binding.textViewDetailStartTime.text = "Iniciado: ${dateFormat.format(workout.startTime)}"
                    } else { // Inicio else
                        Log.w("WorkoutDetailFragment", "Workout with ID $workoutId not found.")
                        binding.textViewDetailStartTime.text = "Iniciado: (No encontrado)"
                    } // Fin else
                } // Fin collectLatest
            } catch (e: Exception) { // Inicio catch
                Log.e("WorkoutDetailFragment", "Error observing workout details", e)
                binding.textViewDetailStartTime.text = "Iniciado: (Error al cargar)"
            } // Fin catch
        } // Fin corutina
    } // --- FIN observeWorkoutDetails ---

    private fun setupSetsRecyclerView() {
        workoutSetAdapter = WorkoutSetAdapter { setToDelete -> // Inicio lambda delete click
            showDeleteConfirmationDialog(setToDelete)
        } // Fin lambda delete click
        binding.recyclerViewSets.apply { // Inicio apply
            layoutManager = LinearLayoutManager(requireContext())
            adapter = workoutSetAdapter
        } // Fin apply
        Log.d("WorkoutDetailFragment", "Sets RecyclerView setup complete.")
    } // --- FIN setupSetsRecyclerView ---

    private fun showDeleteConfirmationDialog(setToDelete: WorkoutSet) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Borrado")
            .setMessage("¿Seguro que quieres borrar esta serie?\n(${setToDelete.exerciseName}: ${setToDelete.repetitions} reps @ ${setToDelete.weight} kg)")
            .setPositiveButton("Borrar") { _, _ -> // Inicio lambda Borrar
                deleteSet(setToDelete)
            } // Fin lambda Borrar
            .setNegativeButton("Cancelar", null)
            .show()
    } // --- FIN showDeleteConfirmationDialog ---

    private fun deleteSet(setToDelete: WorkoutSet) {
        viewLifecycleOwner.lifecycleScope.launch { // Inicio corutina
            try { // Inicio try
                database.workoutSetDao().deleteSet(setToDelete)
                Log.d("WorkoutDetailFragment", "Set deleted: $setToDelete")
                Toast.makeText(context, "Serie borrada", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) { // Inicio catch
                Log.e("WorkoutDetailFragment", "Error deleting set", e)
                Toast.makeText(context, "Error al borrar la serie", Toast.LENGTH_SHORT).show()
            } // Fin catch
        } // Fin corutina
    } // --- FIN deleteSet ---

    private fun observeWorkoutSets() {
        Log.d("WorkoutDetailFragment", "Starting to observe sets for Workout ID: $workoutId")
        viewLifecycleOwner.lifecycleScope.launch { // Inicio corutina
            try { // Inicio try
                database.workoutSetDao().getSetsForWorkout(workoutId).collectLatest { setsList -> // Inicio collectLatest
                    Log.d("WorkoutDetailFragment", "Sets list updated. Count: ${setsList.size}")
                    if(::workoutSetAdapter.isInitialized) { // Inicio if initialized
                        workoutSetAdapter.submitList(setsList)
                    } else { // Inicio else
                        Log.e("WorkoutDetailFragment", "WorkoutSetAdapter not initialized when trying to submit list.")
                    } // Fin else
                } // Fin collectLatest
            } catch (e: Exception) { // Inicio catch
                Log.e("WorkoutDetailFragment", "Error observing workout sets", e)
            } // Fin catch
        } // Fin corutina
    } // --- FIN observeWorkoutSets ---

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d("WorkoutDetailFragment", "--- onDestroyView ---")
    } // --- FIN onDestroyView ---

} // <--- ¡¡ASEGÚRATE DE QUE ESTA ÚLTIMA LLAVE ESTÁ PRESENTE!! ---