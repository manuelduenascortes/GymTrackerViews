package com.example.gymtrackerviews // Tu paquete

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast // <--- ¡¡IMPORT AÑADIDO!! ---
import androidx.navigation.fragment.navArgs
import com.example.gymtrackerviews.databinding.FragmentWorkoutDetailBinding // Tu ViewBinding
import java.text.SimpleDateFormat
import java.util.Locale

class WorkoutDetailFragment : Fragment() {

    private var _binding: FragmentWorkoutDetailBinding? = null
    private val binding get() = _binding!!

    private val args: WorkoutDetailFragmentArgs by navArgs()

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
        Log.d("WorkoutDetailFragment", "--- onViewCreated START ---")
        super.onViewCreated(view, savedInstanceState)

        val workoutId = args.workoutId
        Log.d("WorkoutDetailFragment", "Received workoutId: $workoutId")

        binding.textViewDetailTitle.text = "Detalles del Workout ID: $workoutId"

        // TODO: Obtener los datos del Workout con este ID desde la BD
        // TODO: Configurar el RecyclerView para las series (recyclerViewSets)
        // TODO: Configurar el botón "Añadir Serie" (buttonAddSet)

        binding.buttonAddSet.setOnClickListener {
            Log.d("WorkoutDetailFragment", "Add Set button clicked for workoutId: $workoutId")
            // TODO: Navegar a pantalla/dialog de añadir serie o mostrar campos aquí
            // Ahora sí debería reconocer Toast:
            Toast.makeText(context, "Funcionalidad Añadir Serie (Próximamente)", Toast.LENGTH_SHORT).show()
        }
        Log.d("WorkoutDetailFragment", "--- onViewCreated END ---")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d("WorkoutDetailFragment", "--- onDestroyView ---")
    }
}