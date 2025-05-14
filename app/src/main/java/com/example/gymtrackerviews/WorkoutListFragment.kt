package com.example.gymtrackerviews // O el paquete donde tengas tus Fragments principales

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gymtrackerviews.databinding.FragmentWorkoutListBinding
// import com.google.android.material.snackbar.Snackbar // Descomenta si usas Snackbar en otro lado
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider

// TODO: IMPORTANTE - Asegúrate de que estas clases existen y los imports son correctos.
// Verifica los nombres y paquetes de WorkoutSummary, WorkoutAdapter, WorkoutListViewModel,
// WorkoutDao, AppDatabase, y GymTrackerApplication.
// import com.example.gymtrackerviews.WorkoutSummary
// import com.example.gymtrackerviews.WorkoutAdapter
// import com.example.gymtrackerviews.WorkoutListViewModel
// import com.example.gymtrackerviews.GymTrackerApplication
// import com.example.gymtrackerviews.WorkoutListViewModelFactory


class WorkoutListFragment : Fragment() {

    private var _binding: FragmentWorkoutListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WorkoutListViewModel by viewModels {
        val application = requireActivity().application as GymTrackerApplication
        WorkoutListViewModelFactory(application.database.workoutDao())
    }

    private lateinit var workoutAdapter: WorkoutAdapter
    private lateinit var auth: FirebaseAuth

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
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun signOut() {
        auth.signOut()
        if (isAdded) {
            try {
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
        // CAMBIO: Eliminado el operador de llamada segura '?.' porque fabNewWorkout no debería ser nulo
        // si está definido en fragment_workout_list.xml
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

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allWorkoutSummaries.collect { summaries ->
                    Log.d("WorkoutListFragment", "Workout summary list updated. Count: ${summaries.size}")
                    workoutAdapter.submitList(summaries)
                    // CAMBIO: Eliminado el operador de llamada segura '?.' porque textViewEmptyList no debería ser nulo
                    // si está definido en fragment_workout_list.xml
                    if (summaries.isEmpty()) {
                        binding.textViewEmptyList.visibility = View.VISIBLE
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
