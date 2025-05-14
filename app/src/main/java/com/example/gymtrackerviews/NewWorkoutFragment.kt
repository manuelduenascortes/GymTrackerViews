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
class NewWorkoutFragment : Fragment() {

    private var _binding: FragmentNewWorkoutBinding? = null
    private val binding get() = _binding!!

    // Inyectamos el WorkoutListViewModel.
    // La factory y GymTrackerApplication deben estar correctamente configuradas.
    private val workoutListViewModel: WorkoutListViewModel by viewModels {
        // TODO: Verifica que esta forma de obtener el DAO es correcta para tu proyecto.
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

            // Usamos lifecycleScope para llamar a la función suspend del ViewModel
            viewLifecycleOwner.lifecycleScope.launch {
                // Llamamos al método del ViewModel que inserta con nombre y devuelve ID
                val newWorkoutId = workoutListViewModel.insertNewWorkoutWithNameAndGetId(workoutName) // Pasamos el nombre

                if (newWorkoutId != -1L && isAdded) { // Comprueba que se insertó y el fragment está activo
                    Log.d("NewWorkoutFragment", "Nuevo workout creado con ID: $newWorkoutId y nombre: '$workoutName'. Navegando a detalle.")

                    // Navegar a WorkoutDetailFragment usando Safe Args
                    // Asegúrate de que la acción en nav_graph.xml desde newWorkoutFragment
                    // hacia workoutDetailFragment se llame 'action_newWorkoutFragment_to_workoutDetailFragment'
                    // y que acepte un argumento 'workoutId' de tipo long.
                    val action = NewWorkoutFragmentDirections.actionNewWorkoutFragmentToWorkoutDetailFragment(newWorkoutId)
                    findNavController().navigate(action)

                } else if (isAdded) {
                    // Mostrar error solo si el fragment está activo y el contexto existe
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
