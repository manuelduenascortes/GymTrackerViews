package com.example.gymtrackerviews // Tu paquete

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gymtrackerviews.databinding.ItemWorkoutBinding // Importa el ViewBinding del item_workout.xml
import java.text.SimpleDateFormat // Para dar formato a la fecha
import java.util.Locale // Para el formato de fecha según el idioma/región

// Heredamos de ListAdapter, que es eficiente para listas que cambian.
// Necesita el tipo de dato (Workout) y el tipo del ViewHolder (WorkoutViewHolder).
// También necesita un 'DiffUtil.ItemCallback' para saber cómo comparar items.
class WorkoutAdapter : ListAdapter<Workout, WorkoutAdapter.WorkoutViewHolder>(WorkoutDiffCallback()) {

    // --- ViewHolder ---
    // Clase interna que contiene las referencias a las vistas de UNA fila (item_workout.xml).
    // Recibe el 'binding' generado por ViewBinding para ese layout.
    inner class WorkoutViewHolder(private val binding: ItemWorkoutBinding) : RecyclerView.ViewHolder(binding.root) {

        // Función para asignar los datos de un Workout a las vistas de esta fila.
        fun bind(workout: Workout) {
            binding.textViewWorkoutId.text = "Workout ID: ${workout.id}"

            // Formateamos el objeto Date a un String legible (ej. "24 abr 2025, 13:30:00")
            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
            binding.textViewWorkoutStartTime.text = "Inicio: ${dateFormat.format(workout.startTime)}"
        }
    }

    // --- Métodos obligatorios del ListAdapter ---

    // Se llama cuando el RecyclerView necesita crear una nueva fila (ViewHolder).
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        // "Inflamos" (creamos) la vista desde el XML item_workout.xml usando ViewBinding.
        val binding = ItemWorkoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        // Creamos y devolvemos una instancia del ViewHolder, pasándole el binding.
        return WorkoutViewHolder(binding)
    }

    // Se llama cuando el RecyclerView quiere mostrar los datos de un elemento en una fila específica.
    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        // Obtenemos el objeto Workout correspondiente a esta posición en la lista.
        val workout = getItem(position)
        // Llamamos a la función 'bind' del ViewHolder para que ponga los datos en las vistas.
        holder.bind(workout)
    }
}

// --- DiffUtil Callback ---
// Ayuda al ListAdapter a calcular de forma eficiente qué elementos de la lista han cambiado,
// para actualizar solo lo necesario, en lugar de redibujar toda la lista.
class WorkoutDiffCallback : DiffUtil.ItemCallback<Workout>() {
    // Comprueba si dos elementos representan el MISMO objeto (normalmente por ID).
    override fun areItemsTheSame(oldItem: Workout, newItem: Workout): Boolean {
        return oldItem.id == newItem.id
    }

    // Comprueba si los CONTENIDOS de dos elementos son iguales.
    // Como Workout es una 'data class', la comparación '==' ya revisa todos los campos.
    override fun areContentsTheSame(oldItem: Workout, newItem: Workout): Boolean {
        return oldItem == newItem
    }
}