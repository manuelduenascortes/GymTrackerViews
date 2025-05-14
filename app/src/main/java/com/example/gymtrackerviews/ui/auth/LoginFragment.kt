package com.example.gymtrackerviews.ui.auth // Asegúrate que este es el paquete correcto

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gymtrackerviews.R
import com.example.gymtrackerviews.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth // Importación para ktx
import com.google.firebase.ktx.Firebase // Importación para ktx

/**
 * Un Fragment para manejar el inicio de sesión de usuarios existentes.
 */
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    // Declarar una instancia de FirebaseAuth
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar FirebaseAuth
        auth = Firebase.auth

        // Comprobar si el usuario ya ha iniciado sesión (opcional, pero buena UX)
        // Si el usuario ya está logueado, podríamos llevarlo directamente a la lista de workouts.
        // Esto se haría normalmente en un "Splash Screen" o en la MainActivity,
        // pero lo pongo aquí como ejemplo conceptual.
        /*
        if (auth.currentUser != null) {
            if (isAdded) { // Comprobar si el fragment está añadido
                try {
                    findNavController().navigate(R.id.action_loginFragment_to_workoutListFragment)
                } catch (e: IllegalStateException) {
                    Log.e("LoginFragment", "Navigation to workout list (already logged in) failed: ${e.message}")
                }
            }
            return // Salir de onViewCreated si ya está logueado y navegó
        }
        */

        binding.buttonLogin.setOnClickListener {
            val email = binding.editTextEmailLogin.text.toString().trim()
            val password = binding.editTextPasswordLogin.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                context?.let { ctx ->
                    Toast.makeText(ctx, "Por favor, completa todos los campos.", Toast.LENGTH_SHORT).show()
                }
                return@setOnClickListener
            }

            // Aquí podrías mostrar un ProgressBar
            // if (isAdded) binding.progressBarLogin.visibility = View.VISIBLE // Necesitarías añadir un ProgressBar a tu XML

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity()) { task ->
                    if (!isAdded || _binding == null) {
                        Log.w("LoginFragment", "Fragment not added or view destroyed when login completed.")
                        return@addOnCompleteListener
                    }

                    // Ocultar ProgressBar
                    // binding.progressBarLogin.visibility = View.GONE

                    if (task.isSuccessful) {
                        // Inicio de sesión exitoso
                        Log.d("LoginFragment", "signInWithEmail:success")
                        val user = auth.currentUser
                        context?.let { ctx ->
                            Toast.makeText(ctx, "Inicio de sesión exitoso: ${user?.email}", Toast.LENGTH_SHORT).show()
                        }

                        // Navegar a la pantalla principal de la aplicación (WorkoutListFragment)
                        if (isAdded) {
                            try {
                                findNavController().navigate(R.id.action_loginFragment_to_workoutListFragment)
                            } catch (e: IllegalStateException) {
                                Log.e("LoginFragment", "Navigation to workout list failed: ${e.message}")
                            }
                        }
                    } else {
                        // Si el inicio de sesión falla, muestra un mensaje al usuario.
                        Log.w("LoginFragment", "signInWithEmail:failure", task.exception)
                        context?.let { ctx ->
                            Toast.makeText(ctx, "Error en el inicio de sesión: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
        }

        binding.textViewRegisterLink.setOnClickListener {
            if (isAdded) {
                try {
                    findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
                } catch (e: IllegalStateException) {
                    Log.e("LoginFragment", "Navigation to register link failed: ${e.message}")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
