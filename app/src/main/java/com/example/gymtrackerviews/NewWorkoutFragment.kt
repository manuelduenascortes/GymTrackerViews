package com.example.gymtrackerviews // Asegúrate que este es tu paquete principal

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.gymtrackerviews.databinding.FragmentNewWorkoutBinding
import kotlinx.coroutines.launch

// TODO: Asegúrate de importar tu WorkoutListViewModel, su Factory, y GymTrackerApplication
// si están en paquetes diferentes y si la factory los necesita.
// import com.example.gymtrackerviews.WorkoutListViewModel
// import com.example.gymtrackerviews.WorkoutListViewModelFactory
// import com.example.gymtrackerviews.GymTrackerApplication

/**
 * Fragment para crear un nuevo entrenamiento.
 */
class NewWorkoutFragment : Fragment() { // El nombre de la clase debe ser NewWorkoutFragment

    private var _binding: FragmentNewWorkoutBinding? = null
    private val binding get() = _binding!!

    private val workoutListViewModel: WorkoutListViewModel by viewModels {
        val application = requireActivity().application as GymTrackerApplication
        WorkoutListViewModelFactory(application.database.workoutDao())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewWorkoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonStartAndAddSeries.setOnClickListener {
            val workoutName = binding.editTextWorkoutName.text.toString().trim()

            viewLifecycleOwner.lifecycleScope.launch {
                val newWorkoutId = workoutListViewModel.insertNewWorkoutWithNameAndGetId(workoutName)

                if (newWorkoutId != -1L && isAdded) {
                    Log.d("NewWorkoutFragment", "Nuevo workout creado con ID: $newWorkoutId y nombre: '$workoutName'. Navegando a detalle.")

                    val action = NewWorkoutFragmentDirections.actionNewWorkoutFragmentToWorkoutDetailFragment(newWorkoutId)
                    findNavController().navigate(action)

                } else if (isAdded) {
                    context?.let { ctx ->
                        Toast.makeText(ctx, "Error al crear el entrenamiento", Toast.LENGTH_SHORT).show()
                    }
                    Log.e("NewWorkoutFragment", "Error al crear el entrenamiento, ID devuelto: $newWorkoutId")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
