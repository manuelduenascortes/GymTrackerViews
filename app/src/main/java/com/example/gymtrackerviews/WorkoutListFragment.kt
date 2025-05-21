package com.example.gymtrackerviews // O el paquete donde tengas tus Fragments principales

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gymtrackerviews.databinding.FragmentWorkoutListBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.collectLatest
// import kotlinx.coroutines.flow.distinctUntilChanged // No parece usarse
// import kotlinx.coroutines.flow.map // No parece usarse
import kotlinx.coroutines.launch

import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider

// TODO: Asegúrate de que los imports para R, GymTrackerApplication, WorkoutListViewModelFactory,
// WorkoutAdapter y WorkoutListViewModel sean correctos.
// import com.example.gymtrackerviews.R
// import com.example.gymtrackerviews.GymTrackerApplication
// import com.example.gymtrackerviews.WorkoutListViewModelFactory
// import com.example.gymtrackerviews.WorkoutAdapter
// import com.example.gymtrackerviews.WorkoutListViewModel


class WorkoutListFragment : Fragment() {

    private var _binding: FragmentWorkoutListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WorkoutListViewModel by viewModels {
        val application = requireActivity().application as GymTrackerApplication
        WorkoutListViewModelFactory(application.database.workoutDao())
    }

    private lateinit var workoutAdapter: WorkoutAdapter
    private lateinit var auth: FirebaseAuth

    private lateinit var muscleGroupFilterAdapter: ArrayAdapter<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkoutListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = Firebase.auth
        setupRecyclerView()
        setupFab()
        setupFilter()
        observeViewModel()
        setupMenu()
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.workout_list_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_logout -> {
                        signOut()
                        true
                    }
                    R.id.action_statistics -> {
                        if (isAdded) {
                            try {
                                findNavController().navigate(R.id.action_workoutListFragment_to_statisticsFragment)
                            } catch (e: IllegalStateException) {
                                Log.e("WorkoutListFragment", "Navigation to statistics failed: ${e.message}")
                            }
                        }
                        true
                    }
                    // NUEVO CASE PARA BIBLIOTECA DE EJERCICIOS
                    R.id.action_exercise_library -> {
                        if (isAdded) {
                            try {
                                findNavController().navigate(R.id.action_workoutListFragment_to_exerciseLibraryFragment)
                            } catch (e: IllegalStateException) {
                                Log.e("WorkoutListFragment", "Navigation to exercise library failed: ${e.message}")
                            }
                        }
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun signOut() {
        auth.signOut()
        if (isAdded) {
            try {
                viewModel.setMuscleGroupFilter(null)
                findNavController().navigate(R.id.action_workoutListFragment_to_splashFragment)
            } catch (e: IllegalStateException) {
                Log.e("WorkoutListFragment", "Navigation to splash failed after logout: ${e.message}")
            }
        }
    }

    private fun setupRecyclerView() {
        workoutAdapter = WorkoutAdapter(
            onItemClick = { workoutSummary ->
                val action = WorkoutListFragmentDirections.actionWorkoutListFragmentToWorkoutDetailFragment(workoutSummary.workout.id)
                if (isAdded) {
                    try {
                        findNavController().navigate(action)
                    } catch (e: IllegalStateException) {
                        Log.e("WorkoutListFragment", "Navigation to detail failed: ${e.message}")
                    }
                }
            },
            onDeleteClick = { workoutSummary ->
                viewModel.deleteWorkout(workoutSummary)
                context?.let { ctx ->
                    Toast.makeText(ctx, "Workout borrado: ${workoutSummary.workout.id}", Toast.LENGTH_LONG).show()
                }
            }
        )
        binding.recyclerViewWorkouts.apply {
            adapter = workoutAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupFab() {
        binding.fabNewWorkout.setOnClickListener {
            Log.d("WorkoutListFragment", "FAB clicked, navigating to NewWorkoutFragment.")
            if (isAdded) {
                try {
                    findNavController().navigate(R.id.action_workoutListFragment_to_newWorkoutFragment)
                } catch (e: IllegalStateException) {
                    Log.e("WorkoutListFragment", "Navigation to new workout screen failed: ${e.message}")
                }
            }
        }
    }

    private fun setupFilter() {
        val muscleGroups = resources.getStringArray(R.array.default_muscle_groups)
        muscleGroupFilterAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, muscleGroups)
        binding.autoCompleteTextViewMuscleGroupFilter.setAdapter(muscleGroupFilterAdapter)

        binding.autoCompleteTextViewMuscleGroupFilter.setOnItemClickListener { parent, _, position, _ ->
            val selectedMuscleGroup = parent.getItemAtPosition(position) as String
            viewModel.setMuscleGroupFilter(selectedMuscleGroup)
        }

        binding.buttonClearFilter.setOnClickListener {
            binding.autoCompleteTextViewMuscleGroupFilter.setText("", false)
            viewModel.setMuscleGroupFilter(null)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.muscleGroupFilter.collectLatest { currentFilter ->
                    if (binding.autoCompleteTextViewMuscleGroupFilter.text.toString() != (currentFilter ?: "")) {
                        binding.autoCompleteTextViewMuscleGroupFilter.setText(currentFilter ?: "", false)
                    }
                }
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allWorkoutSummaries.collectLatest { summaries ->
                    Log.d("WorkoutListFragment", "Workout summary list updated (filter: ${viewModel.muscleGroupFilter.value}). Count: ${summaries.size}")
                    workoutAdapter.submitList(summaries)
                    if (summaries.isEmpty()) {
                        binding.textViewEmptyList.visibility = View.VISIBLE
                        if (viewModel.muscleGroupFilter.value != null) {
                            binding.textViewEmptyList.text = "No hay workouts para el grupo muscular '${viewModel.muscleGroupFilter.value}'."
                        } else {
                            binding.textViewEmptyList.text = getString(R.string.no_workouts_message)
                        }
                    } else {
                        binding.textViewEmptyList.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
