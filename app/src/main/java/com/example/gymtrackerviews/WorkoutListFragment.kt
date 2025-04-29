package com.example.gymtrackerviews // Tu paquete

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater // Import necesario
import android.view.View
import android.view.ViewGroup       // Import necesario
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gymtrackerviews.databinding.FragmentWorkoutListBinding // Tu ViewBinding

import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WorkoutListFragment : Fragment() {

    private var _binding: FragmentWorkoutListBinding? = null
    private val binding get() = _binding!!

    private lateinit var workoutAdapter: WorkoutAdapter
    private val viewModel: WorkoutListViewModel by viewModels {
        WorkoutListViewModelFactory(AppDatabase.getDatabase(requireContext().applicationContext).workoutDao())
    }

    // --- onCreateView (Restaurado y Correcto) ---
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("WorkoutListFragment", "--- onCreateView START ---")
        _binding = FragmentWorkoutListBinding.inflate(inflater, container, false)
        Log.d("WorkoutListFragment", "--- onCreateView END ---")
        return binding.root // Devuelve la vista
    }
    // --- FIN onCreateView ---

    // --- onViewCreated (Restaurado) ---
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("WorkoutListFragment", "--- onViewCreated START ---")
        super.onViewCreated(view, savedInstanceState) // Llamada a super

        setupRecyclerView() // Configura adapter y RV
        observeWorkoutList() // Observa datos del ViewModel

        // Listener del FAB
        binding.fabNewWorkout.setOnClickListener {
            Log.d("WorkoutListFragment", ">>> CLIC EN FAB detectado!")
            viewModel.insertNewWorkout()
        }
        Log.d("WorkoutListFragment", "--- onViewCreated END ---")
    }
    // --- FIN onViewCreated ---

    // --- setupRecyclerView (Restaurado) ---
    private fun setupRecyclerView() {
        // Pasamos las dos lambdas al crear el adapter:
        workoutAdapter = WorkoutAdapter(
            // 1. Lambda para click en el item (navegar)
            onItemClick = { workout -> // Recibe Workout
                Log.d("WorkoutListFragment", "Item Clicked - Workout ID: ${workout.id}")
                try {
                    // Pasamos el ID del Workout a la acción de navegación
                    val action = WorkoutListFragmentDirections.actionWorkoutListFragmentToWorkoutDetailFragment(workout.id)
                    findNavController().navigate(action)
                    Log.d("WorkoutListFragment", "Navigation action triggered.")
                } catch (e: Exception) {
                    Log.e("WorkoutListFragment", "Navigation failed.", e)
                    Toast.makeText(context, "Error al navegar", Toast.LENGTH_SHORT).show()
                }
            },
            // 2. Lambda para click en el botón borrar del item
            onDeleteClick = { workout -> // Recibe Workout
                Log.d("WorkoutListFragment", "Delete Clicked - Workout ID: ${workout.id}")
                // Llamamos al diálogo pasándole el Workout
                showDeleteWorkoutConfirmationDialog(workout)
            }
        ) // Fin creación adapter

        binding.recyclerViewWorkouts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = workoutAdapter // Asignamos el adapter
        }
        Log.d("WorkoutListFragment", "RecyclerView setup complete.")
    }
    // --- FIN setupRecyclerView ---

    // --- showDeleteWorkoutConfirmationDialog (Restaurado) ---
    private fun showDeleteWorkoutConfirmationDialog(workoutToDelete: Workout) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Borrado")
            .setMessage("¿Seguro que quieres borrar este workout (ID: ${workoutToDelete.id}) y todas sus series asociadas?")
            .setPositiveButton("Borrar") { _, _ ->
                // Buscamos el WorkoutSummary correspondiente para pasarlo al ViewModel
                val summaryToDelete = viewModel.allWorkoutSummaries.value.find { it.workout.id == workoutToDelete.id }
                if (summaryToDelete != null) {
                    viewModel.deleteWorkout(summaryToDelete) // Llama al ViewModel con WorkoutSummary
                    Toast.makeText(context, "Workout borrado", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("WorkoutListFragment", "Could not find WorkoutSummary to delete for Workout ID: ${workoutToDelete.id}")
                    Toast.makeText(context, "Error al borrar workout", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    // --- FIN showDeleteWorkoutConfirmationDialog ---

    // --- observeWorkoutList (Restaurado) ---
    private fun observeWorkoutList() {
        Log.d("WorkoutListFragment", "Starting to observe ViewModel workout summaries...")
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observamos allWorkoutSummaries del ViewModel
                viewModel.allWorkoutSummaries.collectLatest { workoutSummaryList ->
                    Log.d("WorkoutListFragment", "Workout summary list updated. Count: ${workoutSummaryList.size}")

                    // Gestión de visibilidad
                    if (workoutSummaryList.isEmpty()) {
                        binding.recyclerViewWorkouts.visibility = View.GONE
                        binding.textViewEmptyList.visibility = View.VISIBLE
                    } else {
                        binding.recyclerViewWorkouts.visibility = View.VISIBLE
                        binding.textViewEmptyList.visibility = View.GONE
                    }

                    // Enviamos la List<WorkoutSummary> al adapter
                    if(::workoutAdapter.isInitialized){
                        workoutAdapter.submitList(workoutSummaryList)
                    } else {
                        Log.e("WorkoutListFragment", "WorkoutAdapter not initialized.")
                    }
                } // Fin collectLatest
            } // Fin repeatOnLifecycle
        } // Fin launch
    } // Fin observeWorkoutList

    // --- onDestroyView (Restaurado y Correcto) ---
    override fun onDestroyView() {
        super.onDestroyView() // LLAMADA A SUPER PRIMERO
        _binding = null // Limpiar binding
        Log.d("WorkoutListFragment", "View destroyed, binding set to null.")
    }
    // --- FIN onDestroyView ---

} // --- FIN de la clase WorkoutListFragment ---
