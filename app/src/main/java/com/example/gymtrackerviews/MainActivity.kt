package com.example.gymtrackerviews // Tu paquete

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
// Importa navigateUp si no está ya (generalmente se resuelve automáticamente)
// import androidx.navigation.ui.navigateUp
import com.example.gymtrackerviews.databinding.ActivityMainBinding // Tu ViewBinding

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

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.splashFragment,
                R.id.loginFragment,
                R.id.workoutListFragment
            )
            // No se pasa DrawerLayout aquí si no lo tienes
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        Log.d("MainActivity", "Activity Creada. Toolbar y NavController configurados.")
    }

    override fun onSupportNavigateUp(): Boolean {
        // CAMBIO: Usar la versión más simple de navigateUp()
        // Esto debería funcionar si no hay un DrawerLayout involucrado.
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
