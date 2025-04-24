package com.example.gymtrackerviews // Tu paquete

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gymtrackerviews.databinding.FragmentWorkoutListBinding // Tu ViewBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date

class WorkoutListFragment : Fragment() {

    private var _binding: FragmentWorkoutListBinding? = null
    private val binding get() = _binding!!

    private lateinit var workoutAdapter: WorkoutAdapter
    private lateinit var database: AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("WorkoutListFragment", "--- onCreateView START ---")
        _binding = FragmentWorkoutListBinding.inflate(inflater, container, false)
        try {
            database = AppDatabase.getDatabase(requireContext().applicationContext)
            Log.d("WorkoutListFragment", "Database instance obtained successfully.")
        } catch (e: Exception) {
            Log.e("WorkoutListFragment", "Error getting database instance", e)
            Toast.makeText(requireContext(), "Error crítico al iniciar la base de datos", Toast.LENGTH_LONG).show()
        }
        Log.d("WorkoutListFragment", "--- onCreateView END ---")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("WorkoutListFragment", "--- onViewCreated START ---")
        super.onViewCreated(view, savedInstanceState)

        if (::database.isInitialized) {
            setupRecyclerView()
            observeWorkouts()
        } else {
            Log.e("WorkoutListFragment", "Database was not initialized, skipping setup.")
        }

        binding.fabNewWorkout.setOnClickListener {
            Log.d("WorkoutListFragment", ">>> CLIC EN FAB detectado!")
            if (::database.isInitialized) {
                insertNewWorkout()
            } else {
                Log.e("WorkoutListFragment", "Database not initialized, cannot insert workout.")
                Toast.makeText(requireContext(), "Error: Base de datos no disponible", Toast.LENGTH_SHORT).show()
            }
        }
        Log.d("WorkoutListFragment", "--- onViewCreated END ---")
    }

    private fun setupRecyclerView() {
        workoutAdapter = WorkoutAdapter { workout ->
            Log.d("WorkoutListFragment", "Item Clicked - Workout ID: ${workout.id}")
            try {
                val action = WorkoutListFragmentDirections.actionWorkoutListFragmentToWorkoutDetailFragment(workout.id)
                findNavController().navigate(action)
                Log.d("WorkoutListFragment", "Navigation action triggered.")
            } catch (e: Exception) {
                Log.e("WorkoutListFragment", "Navigation failed.", e)
                Toast.makeText(context, "Error al navegar", Toast.LENGTH_SHORT).show()
            }
        }
        binding.recyclerViewWorkouts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = workoutAdapter
        }
        Log.d("WorkoutListFragment", "RecyclerView setup complete.")
    }

    private fun observeWorkouts() {
        viewLifecycleOwner.lifecycleScope.launch {
            Log.d("WorkoutListFragment", "Starting to observe workouts...")
            try {
                database.workoutDao().getAllWorkouts().collectLatest { workoutsList ->
                    Log.d("WorkoutListFragment", "Workout list updated. Count: ${workoutsList.size}")
                    workoutAdapter.submitList(workoutsList)
                }
            } catch (e: Exception) {
                Log.e("WorkoutListFragment", "Error observing workouts", e)
            }
        }
    }

    private fun insertNewWorkout() {
        val newWorkout = Workout(startTime = Date())
        Log.d("WorkoutListFragment", "Attempting to insert workout: $newWorkout")
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val generatedId = database.workoutDao().insertWorkout(newWorkout)
                Toast.makeText(requireContext(), "Nuevo workout guardado con ID: $generatedId", Toast.LENGTH_SHORT).show()
                Log.d("WorkoutListFragment", "Workout insertado con ID: $generatedId y fecha: ${newWorkout.startTime}")
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al guardar workout: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("WorkoutListFragment", "Error insertando workout", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d("WorkoutListFragment", "View destroyed, binding set to null.")
    }
}