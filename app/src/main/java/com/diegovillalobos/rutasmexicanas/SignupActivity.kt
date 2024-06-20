package com.diegovillalobos.rutasmexicanas

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.diegovillalobos.rutasmexicanas.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()  // Inicializa Firestore

        binding.continueBtn.setOnClickListener {
            val name = binding.name.text.toString().trim()
            val lastname = binding.lastname.text.toString().trim()
            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString().trim()
            val confirmPassword = binding.confirmPassword.text.toString().trim()
            val userType = binding.userTypeSpinner.selectedItem.toString()
            val initialRating = 0.0 // Valoración inicial

            if (name.isEmpty() || lastname.isEmpty() || email.isEmpty() || userType == "Seleccione tipo de usuario") {
                Toast.makeText(this, "Por favor, complete todos los campos y/o seleccione un tipo de usuario", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Las contraseñas no coinciden.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val firebaseUser = auth.currentUser // Aquí obtenemos la instancia del usuario actual
                        val userProfile = hashMapOf(
                            "name" to name,
                            "lastname" to lastname,
                            "email" to email,
                            "userType" to userType, // Asegúrate de obtener este valor del Spinner en tu layout
                            "rating" to initialRating
                        )

                        firebaseUser?.let { currentUser ->
                            db.collection("users").document(currentUser.uid)
                                .set(userProfile)
                                .addOnSuccessListener {
                                    Log.d(TAG, "UserProfile created successfully")
                                    navigateToAppropriateActivity(currentUser.uid)
                                }
                                .addOnFailureListener { e ->
                                    Log.w(TAG, "Error writing document", e)
                                    Toast.makeText(baseContext, "Failed to create user profile.", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.exception)
                        Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        binding.move.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun navigateToAppropriateActivity(userId: String) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val userType = document.getString("userType") ?: "Pasajero" // Use "userType" to match the key used in the document
                if (userType == "Conductor") {
                    val intent = Intent(this, DriverActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }
    }
}