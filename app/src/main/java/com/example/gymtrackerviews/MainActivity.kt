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

        // Define los destinos de nivel superior (donde no quieres el botón "atrás" automáticamente)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.splashFragment, // Splash no debería tener Toolbar visible de todas formas
                R.id.loginFragment,  // Login no tendrá Toolbar
                R.id.registerFragment, // Register no tendrá Toolbar
                R.id.workoutListFragment // WorkoutList es un destino de nivel superior con Toolbar
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        // <<< AÑADIDO: Listener para cambios de destino >>>
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment, R.id.registerFragment, R.id.splashFragment -> {
                    supportActionBar?.hide()
                    // Si tu Toolbar está dentro de un AppBarLayout, también puedes ocultar el AppBarLayout
                    binding.appBarLayout.visibility = View.GONE
                }
                else -> {
                    supportActionBar?.show()
                    binding.appBarLayout.visibility = View.VISIBLE
                }
            }
        }
        Log.d("MainActivity", "Activity Creada. Toolbar y NavController configurados.")
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
