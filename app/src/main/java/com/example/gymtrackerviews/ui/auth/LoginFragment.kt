package com.example.gymtrackerviews.ui.auth // Asegúrate que este es el paquete correcto

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
// import androidx.appcompat.app.AppCompatActivity // Ya no es necesario si solo se usaba para la toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gymtrackerviews.R
import com.example.gymtrackerviews.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth // Importación para ktx
import com.google.firebase.ktx.Firebase // Importación para ktx

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

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

        // Ya NO se oculta la Toolbar aquí

        auth = Firebase.auth

        binding.buttonLogin.setOnClickListener {
            val email = binding.editTextEmailLogin.text.toString().trim()
            val password = binding.editTextPasswordLogin.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                context?.let { ctx ->
                    Toast.makeText(ctx, "Por favor, completa todos los campos.", Toast.LENGTH_SHORT).show()
                }
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity()) { task ->
                    if (!isAdded || _binding == null) {
                        Log.w("LoginFragment", "Fragment not added or view destroyed when login completed.")
                        return@addOnCompleteListener
                    }

                    if (task.isSuccessful) {
                        Log.d("LoginFragment", "signInWithEmail:success")
                        val user = auth.currentUser
                        context?.let { ctx ->
                            Toast.makeText(ctx, "Inicio de sesión exitoso: ${user?.email}", Toast.LENGTH_SHORT).show()
                        }
                        if (isAdded) {
                            try {
                                findNavController().navigate(R.id.action_loginFragment_to_workoutListFragment)
                            } catch (e: IllegalStateException) {
                                Log.e("LoginFragment", "Navigation to workout list failed: ${e.message}")
                            }
                        }
                    } else {
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
        // Ya NO se muestra la Toolbar aquí
        _binding = null
    }
}
