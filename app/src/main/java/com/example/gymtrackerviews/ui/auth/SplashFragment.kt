package com.example.gymtrackerviews.ui.auth // Asegúrate que este es el paquete correcto

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gymtrackerviews.R
import com.example.gymtrackerviews.databinding.FragmentSplashBinding // Importa la clase de ViewBinding generada
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth

        // Usamos un Handler para esperar un breve momento antes de navegar.
        // Esto da tiempo a que Firebase inicialice y compruebe el usuario actual,
        // y también da una sensación de "carga" si la comprobación es muy rápida.
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isAdded) { // Comprobar si el fragment sigue añadido
                return@postDelayed
            }
            checkUserSession()
        }, 1500) // Espera 1.5 segundos (1500 milisegundos)
    }

    private fun checkUserSession() {
        if (auth.currentUser != null) {
            // Usuario ya ha iniciado sesión, navegar a WorkoutListFragment
            Log.d("SplashFragment", "User already logged in: ${auth.currentUser?.email}. Navigating to workout list.")
            try {
                findNavController().navigate(R.id.action_splashFragment_to_workoutListFragment)
            } catch (e: IllegalStateException) {
                Log.e("SplashFragment", "Navigation to workout list failed: ${e.message}")
                // Como fallback, si la navegación falla por alguna razón (ej. nav graph no listo), intentar ir al login
                navigateToLogin()
            }
        } else {
            // No hay usuario, navegar a LoginFragment
            Log.d("SplashFragment", "No user logged in. Navigating to login.")
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        if (!isAdded) return
        try {
            findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
        } catch (e: IllegalStateException) {
            Log.e("SplashFragment", "Navigation to login failed: ${e.message}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
