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
import com.example.gymtrackerviews.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth // Importación para ktx
import com.google.firebase.ktx.Firebase // Importación para ktx

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ya NO se oculta la Toolbar aquí

        auth = Firebase.auth

        binding.buttonRegister.setOnClickListener {
            val email = binding.editTextEmailRegister.text.toString().trim()
            val password = binding.editTextPasswordRegister.text.toString().trim()
            val confirmPassword = binding.editTextConfirmPasswordRegister.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                context?.let { ctx ->
                    Toast.makeText(ctx, "Por favor, completa todos los campos.", Toast.LENGTH_SHORT).show()
                }
                return@setOnClickListener
            }

            if (password.length < 6) {
                context?.let { ctx ->
                    Toast.makeText(ctx, "La contraseña debe tener al menos 6 caracteres.", Toast.LENGTH_SHORT).show()
                }
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                context?.let { ctx ->
                    Toast.makeText(ctx, "Las contraseñas no coinciden.", Toast.LENGTH_SHORT).show()
                }
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity()) { task ->
                    if (!isAdded || _binding == null) {
                        Log.w("RegisterFragment", "Fragment not added or view destroyed when registration completed.")
                        return@addOnCompleteListener
                    }

                    if (task.isSuccessful) {
                        Log.d("RegisterFragment", "createUserWithEmail:success")
                        val user = auth.currentUser
                        context?.let { ctx ->
                            Toast.makeText(ctx, "Registro exitoso: ${user?.email}", Toast.LENGTH_SHORT).show()
                        }

                        user?.sendEmailVerification()
                            ?.addOnCompleteListener(requireActivity()) { verificationTask ->
                                if (isAdded && _binding != null) {
                                    context?.let { ctx ->
                                        if (verificationTask.isSuccessful) {
                                            Log.d("RegisterFragment", "Verification email sent.")
                                            Toast.makeText(ctx, "Correo de verificación enviado.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Log.w("RegisterFragment", "sendEmailVerification failed.", verificationTask.exception)
                                            Toast.makeText(ctx, "Fallo al enviar correo de verificación: ${verificationTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    Log.w("RegisterFragment", "Fragment not added or view destroyed when email verification completed.")
                                }
                            }
                        if (isAdded) {
                            try {
                                findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                            } catch (e: IllegalStateException) {
                                Log.e("RegisterFragment", "Navigation failed after registration: ${e.message}")
                            }
                        }

                    } else {
                        Log.w("RegisterFragment", "createUserWithEmail:failure", task.exception)
                        context?.let { ctx ->
                            Toast.makeText(ctx, "Error en el registro: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
        }

        binding.textViewLoginLink.setOnClickListener {
            if (isAdded) {
                try {
                    findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                } catch (e: IllegalStateException) {
                    Log.e("RegisterFragment", "Navigation to login link failed: ${e.message}")
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
