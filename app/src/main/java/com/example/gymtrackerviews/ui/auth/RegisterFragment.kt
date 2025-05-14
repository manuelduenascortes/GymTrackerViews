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
import com.example.gymtrackerviews.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth // Importación para ktx
import com.google.firebase.ktx.Firebase // Importación para ktx

/**
 * Un Fragment para manejar el registro de nuevos usuarios.
 */
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    // Declarar una instancia de FirebaseAuth
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

        // Inicializar FirebaseAuth
        auth = Firebase.auth

        binding.buttonRegister.setOnClickListener {
            val email = binding.editTextEmailRegister.text.toString().trim()
            val password = binding.editTextPasswordRegister.text.toString().trim()
            val confirmPassword = binding.editTextConfirmPasswordRegister.text.toString().trim()

            // Usar 'context?.let' para los Toasts iniciales para mayor seguridad
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

            // Aquí podrías mostrar un ProgressBar si lo tuvieras en tu XML
            // if (isAdded) binding.progressBarRegister.visibility = View.VISIBLE

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity()) { task -> // Usar requireActivity() como LifecycleOwner es una buena práctica
                    // Asegurarse de que el fragment todavía está añadido y su vista (_binding) existe
                    // antes de interactuar con la UI o navegar.
                    if (!isAdded || _binding == null) {
                        Log.w("RegisterFragment", "Fragment not added or view destroyed when registration completed.")
                        return@addOnCompleteListener
                    }

                    // Ocultar ProgressBar
                    // binding.progressBarRegister.visibility = View.GONE

                    if (task.isSuccessful) {
                        Log.d("RegisterFragment", "createUserWithEmail:success")
                        val user = auth.currentUser
                        // Usar context?.let para el Toast
                        context?.let { ctx ->
                            Toast.makeText(ctx, "Registro exitoso: ${user?.email}", Toast.LENGTH_SHORT).show()
                        }

                        user?.sendEmailVerification()
                            ?.addOnCompleteListener(requireActivity()) { verificationTask -> // Usar requireActivity() también aquí
                                // De nuevo, comprobar si el fragment está activo y el contexto existe antes de mostrar el Toast
                                if (isAdded && _binding != null) { // Comprobamos _binding para asegurarnos que la vista sigue ahí
                                    context?.let { ctx -> // Usamos context?.let
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
                        // Navegar a la pantalla de Login
                        // La navegación también debe estar protegida
                        if (isAdded) { // Comprobamos isAdded antes de intentar navegar
                            try {
                                findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                            } catch (e: IllegalStateException) {
                                Log.e("RegisterFragment", "Navigation failed after registration: ${e.message}")
                                // Esto puede pasar si se intenta navegar cuando el NavController no está listo o el fragment ya no está en el backstack correcto.
                            }
                        }

                    } else {
                        Log.w("RegisterFragment", "createUserWithEmail:failure", task.exception)
                        // Mostrar error solo si el fragment está activo y el contexto existe
                        context?.let { ctx ->
                            Toast.makeText(ctx, "Error en el registro: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
        }

        binding.textViewLoginLink.setOnClickListener {
            // Comprobar si el fragment está añadido antes de navegar
            if (isAdded) { // Comprobamos isAdded antes de intentar navegar
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
        _binding = null
    }
}
