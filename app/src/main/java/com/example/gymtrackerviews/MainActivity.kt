package com.example.gymtrackerviews // Tu paquete

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View // Importar View para View.GONE y View.VISIBLE
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.gymtrackerviews.databinding.ActivityMainBinding
import com.example.gymtrackerviews.R // Asegúrate de que R está importado

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Define los destinos de nivel superior Y los que no deben tener Toolbar
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.splashFragment,
                R.id.loginFragment,
                R.id.registerFragment,
                R.id.workoutListFragment // WorkoutList es un destino de nivel superior CON Toolbar
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Listener para cambios de destino para controlar la visibilidad de la Toolbar
        navController.addOnDestinationChangedListener { _, destination, _ ->
            Log.d("MainActivity", "Navigating to destination: ${destination.label}, ID: ${destination.id}")
            if (destination.id == R.id.loginFragment ||
                destination.id == R.id.registerFragment ||
                destination.id == R.id.splashFragment) {
                Log.d("MainActivity", "Hiding AppBarLayout for ${destination.label}")
                binding.appBarLayout.visibility = View.GONE
            } else {
                Log.d("MainActivity", "Showing AppBarLayout for ${destination.label}")
                binding.appBarLayout.visibility = View.VISIBLE
            }
        }
        Log.d("MainActivity", "Activity Creada. Toolbar y NavController configurados.")
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
