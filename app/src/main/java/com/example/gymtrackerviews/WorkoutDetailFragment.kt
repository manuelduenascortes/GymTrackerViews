package com.example.gymtrackerviews // Tu paquete

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater // Import necesario
import android.view.View
import android.view.ViewGroup       // Import necesario
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity // Import para la Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController // Import para Toolbar
import androidx.navigation.ui.AppBarConfiguration     // Import para Toolbar
import androidx.navigation.ui.NavigationUI           // Import para Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gymtrackerviews.databinding.FragmentWorkoutDetailBinding
import com.example.gymtrackerviews.databinding.DialogAddSetBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import android.view.MenuItem // Import para onOptionsItemSelected

class WorkoutDetailFragment : Fragment() {

    private var _binding: FragmentWorkoutDetailBinding? = null
    private val binding get() = _binding!!

    private val database by lazy { AppDatabase.getDatabase(requireContext().applicationContext) }
    private val viewModel: WorkoutDetailViewModel by viewModels {
        WorkoutDetailViewModel.Factory(database.workoutDao(), database.workoutSetDao())
    }
    private lateinit var workoutDetailAdapter: WorkoutDetailAdapter

    // --- onCreateView CON FIRMA Y RETURN CORRECTOS ---
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View { // Debe devolver View
        Log.d("WorkoutDetailFragment", "--- onCreateView START ---")
        _binding = FragmentWorkoutDetailBinding.inflate(inflater, container, false)
        Log.d("WorkoutDetailFragment", "--- onCreateView END ---")
        return binding.root // Devuelve la vista inflada
    }
    // --- FIN onCreateView ---

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("WorkoutDetailFragment", "--- onViewCreated START (Workout ID from ViewModel: ${viewModel.workoutId}) ---")
        super.onViewCreated(view, savedInstanceState) // Llamada a super

        // --- Configuración de la Toolbar ---
        val navController = findNavController()
        // Usamos solo el NavController para que el botón atrás siempre aparezca
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        NavigationUI.setupWithNavController(binding.toolbarDetail, navController, appBarConfiguration)
        (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbarDetail)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayShowHomeEnabled(true)
        setHasOptionsMenu(true) // Habilitar manejo de opciones de menú (botón atrás)
        // --- Fin Configuración Toolbar ---


        // Ya no usamos este TextView para el título principal
        // binding.textViewDetailTitle.text = "Detalles del Workout ID: ${viewModel.workoutId}"

        setupSetsRecyclerView() // Configura adapter y RV
        observeViewModelData() // Empieza a observar

        // Listener botón Añadir Serie
        binding.buttonAddSet.setOnClickListener {
            Log.d("WorkoutDetailFragment", "Add Set button clicked")
            if (binding.buttonAddSet.isEnabled) {
                showAddOrEditSetDialog(null)
            } else {
                Toast.makeText(context, "El workout ya está finalizado.", Toast.LENGTH_SHORT).show()
            }
        }

        // Listener botón Finalizar Workout
        binding.buttonFinishWorkout.setOnClickListener {
            Log.d("WorkoutDetailFragment", "Finish Workout button clicked for workout ID: ${viewModel.workoutId}")
            if (binding.buttonFinishWorkout.isVisible) {
                saveNotes() // Guardar notas ANTES de finalizar
                Log.d("WorkoutDetailFragment", "Notes saved before finishing.")
                viewModel.finishWorkout(binding.editTextWorkoutNotes.text.toString()) // Pasamos las notas actuales
                Toast.makeText(context, "Workout finalizado", Toast.LENGTH_SHORT).show()
            }
        }
        Log.d("WorkoutDetailFragment", "--- onViewCreated END ---")
    }

    // --- Manejar el click en el botón Atrás/Up de la Toolbar ---
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Verificamos si el item pulsado es el botón Home/Up (la flecha atrás)
        if (item.itemId == android.R.id.home) {
            // Usamos el NavController para navegar hacia atrás
            findNavController().navigateUp()
            return true // Indicamos que hemos manejado el evento
        }
        return super.onOptionsItemSelected(item)
    }
    // --- Fin onOptionsItemSelected ---

    // Observa los datos del ViewModel
    private fun observeViewModelData() {
        Log.d("WorkoutDetailFragment", "Starting to observe ViewModel data")
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 1. Observar detalles del Workout (fechas, notas y TÍTULO TOOLBAR)
                launch {
                    viewModel.workoutDetails.collectLatest { workout ->
                        if (workout != null) {
                            Log.d("WorkoutDetailFragment", "Workout details updated: $workout")
                            val dateFormat = java.text.SimpleDateFormat("dd MMM yy, HH:mm", java.util.Locale.getDefault()) // Formato más corto para título
                            val dateFormatFull = java.text.SimpleDateFormat("dd MMM yy, HH:mm:ss", java.util.Locale.getDefault())

                            // Poner la fecha de inicio en el título de la Toolbar
                            (activity as? AppCompatActivity)?.supportActionBar?.title = "Workout ${dateFormat.format(workout.startTime)}"

                            // Mostramos fecha inicio y fin
                            binding.textViewDetailStartTime.text = "Iniciado: ${dateFormatFull.format(workout.startTime)}"
                            binding.textViewDetailEndTime.isVisible = workout.endTime != null
                            workout.endTime?.let { binding.textViewDetailEndTime.text = "Finalizado: ${dateFormatFull.format(it)}" }

                            // Cargar notas
                            val notes = workout.notes
                            if (_binding != null && binding.editTextWorkoutNotes.isEnabled && binding.editTextWorkoutNotes.text.toString() != (notes ?: "")) {
                                binding.editTextWorkoutNotes.setText(notes ?: "")
                            }
                        } else {
                            (activity as? AppCompatActivity)?.supportActionBar?.title = "Cargando Workout..."
                            binding.textViewDetailStartTime.text = "Iniciado: (Cargando...)"
                            binding.textViewDetailEndTime.isVisible = false
                        }
                    }
                }
                // 2. Observar lista de Series
                launch {
                    viewModel.groupedWorkoutSets.collectLatest { groupedSetsMap ->
                        Log.d("WorkoutDetailFragment", "Grouped sets map updated. Exercise count: ${groupedSetsMap.size}")
                        val detailListItems = mutableListOf<WorkoutDetailListItem>()
                        groupedSetsMap.toSortedMap().forEach { (exerciseName, sets) ->
                            detailListItems.add(WorkoutDetailListItem.HeaderItem(exerciseName))
                            sets.sortedBy { it.timestamp }.forEach { workoutSet ->
                                detailListItems.add(WorkoutDetailListItem.SetItem(workoutSet))
                            }
                        }
                        val isEmpty = detailListItems.isEmpty()
                        binding.recyclerViewSets.isVisible = !isEmpty
                        binding.textViewEmptySets.isVisible = isEmpty
                        if(::workoutDetailAdapter.isInitialized) {
                            workoutDetailAdapter.submitList(detailListItems)
                        }
                    }
                }
                // 3. Observar estado 'finalizado'
                launch {
                    viewModel.isWorkoutFinished.collectLatest { isFinished ->
                        Log.d("WorkoutDetailFragment", "Observed isWorkoutFinished state: $isFinished")
                        _binding?.let { safeBinding ->
                            safeBinding.buttonFinishWorkout.isVisible = !isFinished
                            safeBinding.buttonAddSet.isEnabled = !isFinished
                            safeBinding.editTextWorkoutNotes.isEnabled = !isFinished
                            Log.d("WorkoutDetailFragment", "Updated button/notes enabled state based on isFinished=$isFinished")
                        }
                    }
                }
            } // Fin repeatOnLifecycle
        } // Fin launch principal
    }

    // Configura el RecyclerView
    private fun setupSetsRecyclerView() {
        workoutDetailAdapter = WorkoutDetailAdapter(
            onSetClick = { setToEdit ->
                Log.d("WorkoutDetailFragment", "Set Item Clicked - Set ID: ${setToEdit.id}")
                if (binding.buttonAddSet.isEnabled) { showAddOrEditSetDialog(setToEdit) }
                else { Toast.makeText(context, "Workout finalizado, no se puede editar.", Toast.LENGTH_SHORT).show() }
            },
            onSetDeleteClick = { setToDelete ->
                Log.d("WorkoutDetailFragment", "Set Delete Clicked - Set ID: ${setToDelete.id}")
                if (binding.buttonAddSet.isEnabled) { showDeleteConfirmationDialog(setToDelete) }
                else { Toast.makeText(context, "Workout finalizado, no se puede borrar.", Toast.LENGTH_SHORT).show() }
            }
        )
        binding.recyclerViewSets.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = workoutDetailAdapter
        }
        Log.d("WorkoutDetailFragment", "Sets RecyclerView setup complete with WorkoutDetailAdapter.")
    }

    // Muestra el diálogo para añadir o editar una serie
    private fun showAddOrEditSetDialog(existingSet: WorkoutSet?) {
        // ... (código interno del diálogo sin cambios) ...
    }

    // Muestra diálogo para confirmar borrado de serie
    private fun showDeleteConfirmationDialog(setToDelete: WorkoutSet) {
        // ... (código interno del diálogo sin cambios) ...
    }

    // Función para guardar las notas actuales del EditText
    private fun saveNotes() {
        if (_binding != null) {
            if (binding.editTextWorkoutNotes.isEnabled) {
                val notesFromEditText = binding.editTextWorkoutNotes.text.toString()
                Log.d("WorkoutDetailFragment", "Saving notes: '$notesFromEditText'")
                viewModel.saveNotes(notesFromEditText) // Llama al ViewModel
            } else {
                Log.d("WorkoutDetailFragment", "Notes EditText is disabled, skipping save.")
            }
        } else {
            Log.w("WorkoutDetailFragment", "Binding is null in saveNotes, cannot save.")
        }
    }

    // --- onPause ---
    override fun onPause() {
        super.onPause() // Llamada a super OBLIGATORIA
        Log.d("WorkoutDetailFragment", "--- onPause START --- Attempting to call saveNotes...")
        saveNotes()
        Log.d("WorkoutDetailFragment", "--- onPause END --- Notes potentially saved.")
    }
    // --- FIN onPause ---

    // --- onDestroyView ---
    override fun onDestroyView() {
        super.onDestroyView() // Llamada a super OBLIGATORIA
        _binding = null // Limpiamos el binding
        Log.d("WorkoutDetailFragment", "--- onDestroyView --- Binding set to null.")
    }
    // --- FIN onDestroyView ---

} // --- FIN de la clase WorkoutDetailFragment ---
