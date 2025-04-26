package com.example.gymtrackerviews // Asegúrate que es tu paquete

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter // Import para el AutoCompleteTextView
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gymtrackerviews.databinding.FragmentWorkoutDetailBinding // Binding del Fragment
import com.example.gymtrackerviews.databinding.DialogAddSetBinding // Binding del Dialogo
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class WorkoutDetailFragment : Fragment() {

    private var _binding: FragmentWorkoutDetailBinding? = null
    private val binding get() = _binding!!

    // Obtenemos la BD una vez para pasar DAOs a la Factory
    private val database by lazy { AppDatabase.getDatabase(requireContext().applicationContext) }
    // Obtenemos el ViewModel
    private val viewModel: WorkoutDetailViewModel by viewModels {
        WorkoutDetailViewModel.Factory(database.workoutDao(), database.workoutSetDao())
    }
    // Adapter para la lista de series
    private lateinit var workoutSetAdapter: WorkoutSetAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("WorkoutDetailFragment", "--- onCreateView START ---")
        _binding = FragmentWorkoutDetailBinding.inflate(inflater, container, false)
        Log.d("WorkoutDetailFragment", "--- onCreateView END ---")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("WorkoutDetailFragment", "--- onViewCreated START (Workout ID from ViewModel: ${viewModel.workoutId}) ---")
        super.onViewCreated(view, savedInstanceState)

        binding.textViewDetailTitle.text = "Detalles del Workout ID: ${viewModel.workoutId}"

        // Setup y observación
        setupSetsRecyclerView()
        observeViewModelData()

        // Listener para el botón principal de añadir
        binding.buttonAddSet.setOnClickListener {
            Log.d("WorkoutDetailFragment", "Add Set button clicked")
            showAddOrEditSetDialog(null) // null indica que es para añadir nuevo
        }
        Log.d("WorkoutDetailFragment", "--- onViewCreated END ---")
    }

    // Observa los datos del ViewModel (Workout y Lista de Sets)
    private fun observeViewModelData() {
        Log.d("WorkoutDetailFragment", "Starting to observe ViewModel data")
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Corutina para observar los detalles del workout
                launch {
                    viewModel.workoutDetails.collectLatest { workout ->
                        if (workout != null) {
                            Log.d("WorkoutDetailFragment", "Workout details updated from ViewModel: $workout")
                            val dateFormat = java.text.SimpleDateFormat("dd MMM yy, HH:mm:ss", java.util.Locale.getDefault())
                            binding.textViewDetailStartTime.text = "Iniciado: ${dateFormat.format(workout.startTime)}"
                        } else {
                            Log.w("WorkoutDetailFragment", "Workout details from ViewModel are null.")
                            binding.textViewDetailStartTime.text = "Iniciado: (Cargando...)"
                        }
                    }
                }
                // Corutina para observar la lista de series y gestionar visibilidad
                launch {
                    viewModel.workoutSets.collectLatest { setsList ->
                        Log.d("WorkoutDetailFragment", "Sets list updated from ViewModel. Count: ${setsList.size}")

                        // Gestión de visibilidad de lista/mensaje vacío
                        if (setsList.isEmpty()) {
                            binding.recyclerViewSets.visibility = View.GONE
                            binding.textViewEmptySets.visibility = View.VISIBLE
                        } else {
                            binding.recyclerViewSets.visibility = View.VISIBLE
                            binding.textViewEmptySets.visibility = View.GONE
                        }

                        // Actualizar adapter
                        if(::workoutSetAdapter.isInitialized) {
                            workoutSetAdapter.submitList(setsList)
                        }
                    }
                } // Fin launch series
            } // Fin repeatOnLifecycle
        } // Fin launch principal
    }

    // Configura el RecyclerView de series y su adapter (con lambdas para clicks)
    private fun setupSetsRecyclerView() {
        workoutSetAdapter = WorkoutSetAdapter(
            onItemClick = { setToEdit ->
                Log.d("WorkoutDetailFragment", "Item Clicked - Set ID: ${setToEdit.id}")
                showAddOrEditSetDialog(setToEdit) // Llama al diálogo en modo Edición
            },
            onDeleteClick = { setToDelete ->
                Log.d("WorkoutDetailFragment", "Delete Clicked - Set ID: ${setToDelete.id}")
                showDeleteConfirmationDialog(setToDelete) // Llama al diálogo de borrado
            }
        )
        binding.recyclerViewSets.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = workoutSetAdapter
        }
        Log.d("WorkoutDetailFragment", "Sets RecyclerView setup complete.")
    }

    // Muestra el diálogo para añadir o editar una serie
    private fun showAddOrEditSetDialog(existingSet: WorkoutSet?) {
        // Inflamos el layout del diálogo
        val dialogBinding = DialogAddSetBinding.inflate(LayoutInflater.from(requireContext()))
        val isEditing = existingSet != null // True si estamos editando, false si añadiendo

        // --- Configuración del AutoCompleteTextView ---
        try {
            val exercises: Array<String> = resources.getStringArray(R.array.default_exercise_list)
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, exercises)
            dialogBinding.editTextExerciseName.setAdapter(adapter) // Asignamos el adapter
            Log.d("WorkoutDetailFragment", "AutoCompleteTextView adapter set.")
        } catch (e: Exception) {
            Log.e("WorkoutDetailFragment", "Error setting AutoCompleteTextView adapter", e)
        }
        // --- Fin Configuración ---


        // Si estamos editando, rellenamos los campos
        if (isEditing && existingSet != null) {
            dialogBinding.editTextExerciseName.setText(existingSet.exerciseName, false) // false para no filtrar al inicio
            dialogBinding.editTextReps.setText(existingSet.repetitions.toString())
            dialogBinding.editTextWeight.setText(existingSet.weight.toString())
            Log.d("WorkoutDetailFragment", "Dialog pre-filled for editing set ID: ${existingSet.id}")
        }

        // Construimos el diálogo
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(if (isEditing) "Editar Serie" else "Añadir Nueva Serie")
        builder.setView(dialogBinding.root) // Usamos la vista inflada

        // Botón Positivo (Guardar/Actualizar)
        builder.setPositiveButton(if (isEditing) "Actualizar" else "Guardar") { dialog, _ ->
            val exerciseName = dialogBinding.editTextExerciseName.text.toString().trim()
            val repsString = dialogBinding.editTextReps.text.toString()
            val weightString = dialogBinding.editTextWeight.text.toString()

            // Validación simple
            if (exerciseName.isEmpty() || repsString.isEmpty() || weightString.isEmpty()) {
                Toast.makeText(context, "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            try {
                val reps = repsString.toInt()
                val weight = weightString.toDouble()

                if (isEditing && existingSet != null) {
                    // Modo Edición: Creamos copia con mismo ID y llamamos a update
                    val updatedSet = existingSet.copy(
                        exerciseName = exerciseName,
                        repetitions = reps,
                        weight = weight
                    )
                    viewModel.updateSet(updatedSet)
                    Log.d("WorkoutDetailFragment", "Called viewModel.updateSet for set ID: ${updatedSet.id}")
                    Toast.makeText(context, "Serie actualizada", Toast.LENGTH_SHORT).show()
                } else {
                    // Modo Añadir: Llamamos a insert
                    viewModel.insertSet(exerciseName, reps, weight)
                    Log.d("WorkoutDetailFragment", "Called viewModel.insertSet")
                    Toast.makeText(context, "Serie guardada", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss() // Cerramos el diálogo

            } catch (e: NumberFormatException) {
                Toast.makeText(context, "Introduce números válidos para Reps y Peso", Toast.LENGTH_SHORT).show()
            }
        } // Fin Botón Positivo

        // Botón Negativo (Cancelar)
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            Log.d("WorkoutDetailFragment", "Add/Edit dialog cancelled.")
            dialog.dismiss()
        }

        // Mostramos el diálogo
        builder.create().show()
        Log.d("WorkoutDetailFragment", "Add/Edit dialog shown. Editing: $isEditing")
    } // --- FIN showAddOrEditSetDialog ---


    // Muestra diálogo para confirmar borrado de serie
    private fun showDeleteConfirmationDialog(setToDelete: WorkoutSet) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Borrado")
            .setMessage("¿Seguro que quieres borrar esta serie?\n(${setToDelete.exerciseName}: ${setToDelete.repetitions} reps @ ${setToDelete.weight} kg)")
            .setPositiveButton("Borrar") { _, _ ->
                viewModel.deleteSet(setToDelete) // Llama al ViewModel
                Log.d("WorkoutDetailFragment", "Delete confirmed for set ID: ${setToDelete.id}. Called viewModel.deleteSet")
                Toast.makeText(context, "Serie borrada", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
        Log.d("WorkoutDetailFragment", "Delete confirmation dialog shown for set ID: ${setToDelete.id}")
    } // --- FIN showDeleteConfirmationDialog ---


    // Limpia el binding cuando la vista se destruye
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d("WorkoutDetailFragment", "--- onDestroyView ---")
    }
} // --- FIN de la clase WorkoutDetailFragment ---