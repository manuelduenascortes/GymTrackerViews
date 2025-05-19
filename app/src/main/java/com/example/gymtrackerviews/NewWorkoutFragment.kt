package com.example.gymtrackerviews // Asegúrate que este es tu paquete principal

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox // IMPORTANTE para referenciar los CheckBox
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.gymtrackerviews.databinding.FragmentNewWorkoutBinding
import kotlinx.coroutines.launch

// TODO: Asegúrate de importar tu WorkoutListViewModel, su Factory, y GymTrackerApplication
// import com.example.gymtrackerviews.WorkoutListViewModel
// import com.example.gymtrackerviews.WorkoutListViewModelFactory
// import com.example.gymtrackerviews.GymTrackerApplication

class NewWorkoutFragment : Fragment() {

    private var _binding: FragmentNewWorkoutBinding? = null
    private val binding get() = _binding!!

    private val workoutListViewModel: WorkoutListViewModel by viewModels {
        val application = requireActivity().application as GymTrackerApplication
        WorkoutListViewModelFactory(application.database.workoutDao())
    }

    private val muscleGroupCheckBoxes = mutableListOf<CheckBox>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewWorkoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // CAMBIO: Eliminados los operadores de llamada segura '?.'
        // Asumimos que si los IDs existen en el XML, binding.checkboxPecho (etc.) no será nulo.
        // Si un CheckBox no existiera en el XML y no usáramos '?.', la app crashearía.
        // Pero como los definimos en el XML, deberían existir.
        muscleGroupCheckBoxes.add(binding.checkboxPecho)
        muscleGroupCheckBoxes.add(binding.checkboxEspalda)
        muscleGroupCheckBoxes.add(binding.checkboxPiernas)
        muscleGroupCheckBoxes.add(binding.checkboxHombros)
        muscleGroupCheckBoxes.add(binding.checkboxBiceps)
        muscleGroupCheckBoxes.add(binding.checkboxTriceps)
        // Añade más si los tienes en el XML, asegurándote de que los IDs coincidan.
        // Si alguno de estos IDs no existe en tu fragment_new_workout.xml, la app crasheará aquí.
        // En ese caso, deberías usar binding.checkboxID?.let { muscleGroupCheckBoxes.add(it) }
        // o asegurarte de que el ID en el XML y en el Kotlin coincidan.


        binding.buttonStartAndAddSeries.setOnClickListener {
            val workoutName = binding.editTextWorkoutName.text.toString().trim()

            val selectedMuscleGroups = mutableListOf<String>()
            for (checkbox in muscleGroupCheckBoxes) {
                if (checkbox.isChecked) {
                    selectedMuscleGroups.add(checkbox.text.toString())
                }
            }

            val muscleGroupToSave = if (selectedMuscleGroups.isNotEmpty()) {
                selectedMuscleGroups.joinToString(", ")
            } else {
                null
            }

            viewLifecycleOwner.lifecycleScope.launch {
                val newWorkoutId = workoutListViewModel.insertNewWorkoutWithNameAndGetId(workoutName, muscleGroupToSave)

                if (newWorkoutId != -1L && isAdded) {
                    Log.d("NewWorkoutFragment", "Nuevo workout creado con ID: $newWorkoutId, Nombre: '$workoutName', Grupos: '$muscleGroupToSave'. Navegando a detalle.")

                    val action = NewWorkoutFragmentDirections.actionNewWorkoutFragmentToWorkoutDetailFragment(newWorkoutId)
                    try {
                        findNavController().navigate(action)
                    } catch (e: IllegalStateException) {
                        Log.e("NewWorkoutFragment", "Navigation failed: ${e.message}", e)
                        context?.let { Toast.makeText(it, "Error al navegar: ${e.localizedMessage}", Toast.LENGTH_LONG).show() }
                    }

                } else if (isAdded) {
                    context?.let { ctx ->
                        Toast.makeText(ctx, "Error al crear el entrenamiento.", Toast.LENGTH_SHORT).show()
                    }
                    Log.e("NewWorkoutFragment", "Error al crear el entrenamiento, ID devuelto: $newWorkoutId")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        muscleGroupCheckBoxes.clear()
        _binding = null
    }
}
