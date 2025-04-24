package com.example.gymtrackerviews // Tu paquete

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager // Necesario para decirle al RecyclerView cómo ordenar los items
import kotlinx.coroutines.flow.collectLatest // Para coleccionar el Flow de forma eficiente
import kotlinx.coroutines.launch
import java.util.Date
import com.example.gymtrackerviews.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var database: AppDatabase
    // Declaramos una variable para nuestro adaptador
    private lateinit var workoutAdapter: WorkoutAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtenemos instancia de la BD (igual que antes)
        database = AppDatabase.getDatabase(applicationContext)

        // --- Configuramos el RecyclerView ---
        setupRecyclerView() // Llamamos a una nueva función para organizar esto

        // ---- Empezamos a observar los datos de la BD ----
        observeWorkouts() // Llamamos a una nueva función para esto

        // ---- Listener del FAB (igual que antes) ----
        binding.fabNewWorkout.setOnClickListener {
            insertNewWorkout()
        }

        Log.d("MainActivity", "MainActivity cargada y lista.")
    }

    // --- Nueva función para configurar el RecyclerView ---
    private fun setupRecyclerView() {
        // Creamos la instancia de nuestro WorkoutAdapter
        workoutAdapter = WorkoutAdapter()
        // Accedemos al RecyclerView desde el binding
        binding.recyclerViewWorkouts.apply {
            // 1. Establecemos el LayoutManager: Le dice cómo organizar los elementos.
            //    LinearLayoutManager los pone uno debajo del otro, como una lista normal.
            layoutManager = LinearLayoutManager(this@MainActivity)
            // 2. Establecemos el Adapter: Le decimos quién gestionará los datos y las vistas.
            adapter = workoutAdapter
        }
        Log.d("MainActivity", "RecyclerView configurado.")
    }

    // --- Nueva función para observar los datos de los Workouts ---
    private fun observeWorkouts() {
        // Lanzamos una corutina en el scope del ciclo de vida de la Activity
        lifecycleScope.launch {
            // Obtenemos el Flow de la lista de workouts desde el DAO
            // .collectLatest asegura que solo procesemos la última lista emitida si llegan muy rápido
            database.workoutDao().getAllWorkouts().collectLatest { workoutsList ->
                // Este bloque se ejecutará cada vez que la lista en la BD cambie.
                Log.d("MainActivity", "Lista de Workouts actualizada desde BD. Cantidad: ${workoutsList.size}")
                // Enviamos la nueva lista al adaptador para que actualice el RecyclerView.
                workoutAdapter.submitList(workoutsList)
            }
        }
    }

    // --- Función para insertar (igual que antes) ---
    private fun insertNewWorkout() {
        val newWorkout = Workout(startTime = Date())
        lifecycleScope.launch {
            try {
                val generatedId = database.workoutDao().insertWorkout(newWorkout)
                Toast.makeText(this@MainActivity, "Nuevo workout guardado con ID: $generatedId", Toast.LENGTH_SHORT).show()
                Log.d("MainActivity", "Workout insertado con ID: $generatedId y fecha: ${newWorkout.startTime}")
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error al guardar workout: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("MainActivity", "Error insertando workout", e)
            }
        }
    }
}