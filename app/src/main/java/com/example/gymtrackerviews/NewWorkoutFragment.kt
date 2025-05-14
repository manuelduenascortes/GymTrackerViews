package com.example.gymtrackerviews // Asegúrate que este es tu paquete principal

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels // Necesitarás esto para el ViewModel
import androidx.navigation.fragment.findNavController
import com.example.gymtrackerviews.databinding.FragmentNewWorkoutBinding
// TODO: Asegúrate de importar tu WorkoutListViewModel y su Factory si lo usas aquí
// import com.example.gymtrackerviews.WorkoutListViewModel
// import com.example.gymtrackerviews.WorkoutListViewModelFactory
// import com.example.gymtrackerviews.GymTrackerApplication // Si usas la clase Application

/**
 * Fragment para crear un nuevo entrenamiento.
 */
class NewWorkoutFragment : Fragment() {

    private var _binding: FragmentNewWorkoutBinding? = null
    private val binding get() = _binding!!

    // TODO: Si vas a crear el workout desde aquí, necesitarás el ViewModel
    // private val workoutListViewModel: WorkoutListViewModel by viewModels {
    //     val application = requireActivity().application as GymTrackerApplication
    //     WorkoutListViewModelFactory(application.database.workoutDao())
    // }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewWorkoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // CAMBIO AQUÍ: Usar el ID correcto del botón del XML
        binding.buttonStartAndAddSeries.setOnClickListener {
            val workoutName = binding.editTextWorkoutName.text.toString().trim()

            // TODO: Implementar la lógica para crear el Workout con el nombre
            // y luego navegar al WorkoutDetailFragment.
            // Esto probablemente implicará llamar a un método en tu ViewModel
            // que inserte el workout con el nombre y devuelva su ID.

            // Ejemplo de cómo podrías hacerlo (necesitarás el ViewModel configurado):
            /*
            viewLifecycleOwner.lifecycleScope.launch {
                // Asumiendo que tienes un método en tu ViewModel como:
                // suspend fun insertNewWorkoutWithNameAndGetId(name: String?): Long
                val newWorkoutId = workoutListViewModel.insertNewWorkoutWithNameAndGetId(workoutName.ifEmpty { null })
                if (newWorkoutId != -1L && isAdded) {
                    Log.d("NewWorkoutFragment", "Nuevo workout creado con ID: $newWorkoutId y nombre: $workoutName. Navegando a detalle.")
                    val action = NewWorkoutFragmentDirections.actionNewWorkoutFragmentToWorkoutDetailFragment(newWorkoutId)
                    findNavController().navigate(action)
                } else if (isAdded) {
                    Toast.makeText(context, "Error al crear el entrenamiento", Toast.LENGTH_SHORT).show()
                }
            }
            */

            // Por ahora, solo un Toast
            context?.let { ctx ->
                Toast.makeText(ctx, "Iniciar y Añadir Series (TODO) - Nombre: $workoutName", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
