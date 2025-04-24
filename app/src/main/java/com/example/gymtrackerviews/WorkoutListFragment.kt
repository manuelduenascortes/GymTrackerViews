package com.example.gymtrackerviews // Tu paquete

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater // Import necesario
import android.view.View
import android.view.ViewGroup       // Import necesario
import android.widget.Toast
import androidx.appcompat.app.AlertDialog // Importamos AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gymtrackerviews.databinding.FragmentWorkoutListBinding // Tu ViewBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WorkoutListFragment : Fragment() { // Asegúrate que hereda de Fragment

    private var _binding: FragmentWorkoutListBinding? = null
    private val binding get() = _binding!!

    private lateinit var workoutAdapter: WorkoutAdapter
    private val viewModel: WorkoutListViewModel by viewModels {
        WorkoutListViewModelFactory(AppDatabase.getDatabase(requireContext().applicationContext).workoutDao())
    }

    // --- onCreateView CON LA FIRMA CORRECTA Y COMPLETA ---
    override fun onCreateView(
        inflater: LayoutInflater,    // <-- PARÁMETRO 1
        container: ViewGroup?,       // <-- PARÁMETRO 2
        savedInstanceState: Bundle?  // <-- PARÁMETRO 3
    ): View {                        // <-- TIPO DE RETORNO
        Log.d("WorkoutListFragment", "--- onCreateView START ---")
        // Usamos inflater y container aquí
        _binding = FragmentWorkoutListBinding.inflate(inflater, container, false)
        Log.d("WorkoutListFragment", "--- onCreateView END ---")
        // Devolvemos la vista raíz
        return binding.root
    }
    // --- FIN onCreateView ---

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("WorkoutListFragment", "--- onViewCreated START ---")
        super.onViewCreated(view, savedInstanceState)

        // Llamamos a configurar RecyclerView (que ahora crea el adapter con las 2 lambdas)
        setupRecyclerView()
        // Empezamos a observar la lista desde el ViewModel
        observeWorkoutList()

        // Listener del FAB (llama al ViewModel)
        binding.fabNewWorkout.setOnClickListener {
            Log.d("WorkoutListFragment", ">>> CLIC EN FAB detectado!")
            viewModel.insertNewWorkout()
        }
        Log.d("WorkoutListFragment", "--- onViewCreated END ---")
    }

    private fun setupRecyclerView() {
        // Pasamos las dos lambdas al crear el adapter:
        workoutAdapter = WorkoutAdapter(
            // 1. Lambda para click en el item (navegar)
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
            // 2. Lambda para click en el botón borrar del item
            onDeleteClick = { workout ->
                Log.d("WorkoutListFragment", "Delete Clicked - Workout ID: ${workout.id}")
                showDeleteWorkoutConfirmationDialog(workout) // Llamamos al diálogo de confirmación
            }
        ) // Fin creación adapter

        binding.recyclerViewWorkouts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = workoutAdapter
        }
        Log.d("WorkoutListFragment", "RecyclerView setup complete.")
    }

    // Función para mostrar confirmación de borrado de Workout
    private fun showDeleteWorkoutConfirmationDialog(workoutToDelete: Workout) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Borrado")
            // Advertimos que se borrarán también las series
            .setMessage("¿Seguro que quieres borrar este workout (ID: ${workoutToDelete.id}) y todas sus series asociadas?")
            .setPositiveButton("Borrar") { _, _ ->
                // Si confirma, llamamos al ViewModel para borrar
                viewModel.deleteWorkout(workoutToDelete)
                Toast.makeText(context, "Workout borrado", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Función para observar la lista desde el ViewModel
    private fun observeWorkoutList() {
        Log.d("WorkoutListFragment", "Starting to observe ViewModel workouts...")
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allWorkouts.collectLatest { workoutsList ->
                    Log.d("WorkoutListFragment", "Workout list updated from ViewModel. Count: ${workoutsList.size}")
                    // Asegurarnos que el adapter está inicializado
                    if(::workoutAdapter.isInitialized){
                        workoutAdapter.submitList(workoutsList)
                    }
                }
            }
        }
    }

    // Se llama cuando la vista se destruye
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Importante para evitar fugas de memoria en Fragments
        Log.d("WorkoutListFragment", "View destroyed, binding set to null.")
    }
} // <-- Llave de cierre final de la clase