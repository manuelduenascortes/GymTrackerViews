package com.example.gymtrackerviews // Tu paquete

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.gymtrackerviews.databinding.ActivityMainBinding // Tu ViewBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Esta Activity ya no hace nada más. La navegación y la lógica
        // ocurren dentro de los Fragments.
        Log.d("MainActivity", "Activity Creada. NavHostFragment gestiona los fragments.")
    }
}