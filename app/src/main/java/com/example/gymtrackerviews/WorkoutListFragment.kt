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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("WorkoutListFragment", "--- onCreateView START ---")
        _binding = FragmentWorkoutListBinding.inflate(inflater, container, false)
        Log.d("WorkoutListFragment", "--- onCreateView END ---")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("WorkoutListFragment", "--- onViewCreated START ---")
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeWorkoutList() // Cambiado nombre aquí

        binding.fabNewWorkout.setOnClickListener {
            Log.d("WorkoutListFragment", ">>> CLIC EN FAB detectado!")
            viewModel.insertNewWorkout()
        }
        Log.d("WorkoutListFragment", "--- onViewCreated END ---")
    }

    private fun setupRecyclerView() {
        workoutAdapter = WorkoutAdapter(
            onItemClick = { workout ->
                Log.d("WorkoutListFragment", "Item Clicked - Workout ID: ${workout.id}")
                try {
                    val action = WorkoutListFragmentDirections.actionWorkoutListFragmentToWorkoutDetailFragment(workout.id)
                    findNavController().navigate(action)
                    Log.d("WorkoutListFragment", "Navigation action triggered.")
                } catch (e: Exception) {
                    Log.e("WorkoutListFragment", "Navigation failed.", e)
                    Toast.makeText(context, "Error al navegar", Toast.LENGTH_SHORT).show()
                }
            },
            onDeleteClick = { workout ->
                Log.d("WorkoutListFragment", "Delete Clicked - Workout ID: ${workout.id}")
                showDeleteWorkoutConfirmationDialog(workout)
            }
        )

        binding.recyclerViewWorkouts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = workoutAdapter
        }
        Log.d("WorkoutListFragment", "RecyclerView setup complete.")
    }

    private fun showDeleteWorkoutConfirmationDialog(workoutToDelete: Workout) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Borrado")
            .setMessage("¿Seguro que quieres borrar este workout (ID: ${workoutToDelete.id}) y todas sus series asociadas?")
            .setPositiveButton("Borrar") { _, _ ->
                viewModel.deleteWorkout(workoutToDelete)
                Toast.makeText(context, "Workout borrado", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Función para observar la lista desde el ViewModel y gestionar visibilidad
    private fun observeWorkoutList() {
        Log.d("WorkoutListFragment", "Starting to observe ViewModel workouts...")
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allWorkouts.collectLatest { workoutsList ->
                    Log.d("WorkoutListFragment", "Workout list updated from ViewModel. Count: ${workoutsList.size}")

                    // --- Lógica de Visibilidad ---
                    if (workoutsList.isEmpty()) {
                        binding.recyclerViewWorkouts.visibility = View.GONE // Oculta RecyclerView
                        binding.textViewEmptyList.visibility = View.VISIBLE // Muestra mensaje
                    } else {
                        binding.recyclerViewWorkouts.visibility = View.VISIBLE // Muestra RecyclerView
                        binding.textViewEmptyList.visibility = View.GONE // Oculta mensaje
                    }
                    // --- Fin Lógica de Visibilidad ---

                    if(::workoutAdapter.isInitialized){
                        workoutAdapter.submitList(workoutsList)
                    } else {
                        Log.e("WorkoutListFragment", "WorkoutAdapter not initialized when trying to submit list.")
                    }
                } // Fin collectLatest
            } // Fin repeatOnLifecycle
        } // Fin launch
    } // Fin observeWorkoutList


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d("WorkoutListFragment", "View destroyed, binding set to null.")
    }
}