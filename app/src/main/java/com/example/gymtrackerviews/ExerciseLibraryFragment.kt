package com.example.gymtrackerviews.ui.library // TODO: Ajusta este paquete si es diferente

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager // <<<--- IMPORT AÑADIDO AQUÍ
// import androidx.navigation.fragment.findNavController // Descomenta si lo usas
import com.example.gymtrackerviews.Exercise
import com.example.gymtrackerviews.R // Para R.array.default_muscle_groups
import com.example.gymtrackerviews.GymTrackerApplication
import com.example.gymtrackerviews.adapter.ExerciseAdapter
import com.example.gymtrackerviews.databinding.DialogAddExerciseBinding
import com.example.gymtrackerviews.databinding.FragmentExerciseLibraryBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// NO DEBE HABER OTRA DECLARACIÓN DE CLASE ExerciseLibraryFragment EN ESTE ARCHIVO
class ExerciseLibraryFragment : Fragment() {

    private var _binding: FragmentExerciseLibraryBinding? = null
    private val binding get() = _binding!!

    // El ViewModel y la Factory deben ser encontrados ahora si los paquetes e imports son correctos
    private val viewModel: ExerciseLibraryViewModel by viewModels {
        val application = requireActivity().application as GymTrackerApplication
        ExerciseLibraryViewModelFactory(application.database.exerciseDao())
    }

    private lateinit var exerciseAdapter: ExerciseAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExerciseLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
        setupFab()
    }

    private fun setupRecyclerView() {
        exerciseAdapter = ExerciseAdapter(
            onItemClick = { exercise ->
                Log.d("ExerciseLibrary", "Clicked on exercise: ${exercise.name}")
                showAddOrEditExerciseDialog(exercise)
            },
            onDeleteClick = { exercise ->
                Log.d("ExerciseLibrary", "Delete clicked for exercise: ${exercise.name}")
                showDeleteExerciseConfirmationDialog(exercise)
            }
        )
        binding.recyclerViewExercises.apply {
            adapter = exerciseAdapter
            layoutManager = LinearLayoutManager(requireContext()) // Aquí se usa LinearLayoutManager
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allExercises.collectLatest { exercises ->
                    Log.d("ExerciseLibrary", "Exercises updated: ${exercises.size} items")
                    exerciseAdapter.submitList(exercises)
                    binding.textViewEmptyExerciseList.visibility = if (exercises.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun setupFab() {
        binding.fabAddExercise.setOnClickListener {
            showAddOrEditExerciseDialog(null)
        }
    }

    private fun showAddOrEditExerciseDialog(exerciseToEdit: Exercise?) {
        if (!isAdded || context == null) return

        val dialogBinding = DialogAddExerciseBinding.inflate(LayoutInflater.from(requireContext()))
        val isEditing = exerciseToEdit != null
        val dialogTitle = if (isEditing) "Editar Ejercicio" else "Añadir Nuevo Ejercicio"

        if (isEditing && exerciseToEdit != null) {
            dialogBinding.editTextExerciseNameDialog.setText(exerciseToEdit.name)
            dialogBinding.autoCompleteMuscleGroupDialog.setText(exerciseToEdit.muscleGroup ?: "", false)
            dialogBinding.editTextExerciseDescriptionDialog.setText(exerciseToEdit.description ?: "")
        }

        try {
            val muscleGroups = resources.getStringArray(R.array.default_muscle_groups)
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, muscleGroups)
            dialogBinding.autoCompleteMuscleGroupDialog.setAdapter(adapter)
        } catch (e: Exception) {
            Log.e("ExerciseLibraryFragment", "Error setting muscle group adapter", e)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(dialogTitle)
            .setView(dialogBinding.root)
            .setPositiveButton(if (isEditing) "Actualizar" else "Guardar") { dialog, _ ->
                val name = dialogBinding.editTextExerciseNameDialog.text.toString().trim()
                val muscleGroup = dialogBinding.autoCompleteMuscleGroupDialog.text.toString().trim()
                val description = dialogBinding.editTextExerciseDescriptionDialog.text.toString().trim()

                if (name.isBlank()) {
                    Toast.makeText(context, "El nombre del ejercicio no puede estar vacío.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (isEditing && exerciseToEdit != null) {
                    val updatedExercise = exerciseToEdit.copy(
                        name = name,
                        muscleGroup = muscleGroup.ifEmpty { null },
                        description = description.ifEmpty { null }
                    )
                    viewModel.updateExercise(updatedExercise)
                    Toast.makeText(context, "Ejercicio '${updatedExercise.name}' actualizado", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.insertExercise(name, muscleGroup.ifEmpty { null }, description.ifEmpty { null })
                    Toast.makeText(context, "Ejercicio '$name' añadido", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun showDeleteExerciseConfirmationDialog(exercise: Exercise) {
        if (!isAdded || context == null) return
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Borrado")
            .setMessage("¿Seguro que quieres borrar el ejercicio '${exercise.name}'?")
            .setPositiveButton("Borrar") { _, _ ->
                viewModel.deleteExercise(exercise)
                Toast.makeText(context, "Ejercicio '${exercise.name}' borrado", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
