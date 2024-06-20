package com.diegovillalobos.rutasmexicanas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserProfileActivity : AppCompatActivity() {
    private lateinit var nameEditText: TextInputEditText
    private lateinit var lastnameEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var saveButton: Button
    private lateinit var closeButton: ImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var userType: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        // Inicialización de componentes
        initViews()

        // Carga de datos del usuario
        loadUserData()

        saveButton.setOnClickListener {
            saveUserData()
        }

        closeButton.setOnClickListener {
            navigateBasedOnUserType()
        }
    }

    private fun initViews() {
        nameEditText = findViewById(R.id.nameEditText)
        lastnameEditText = findViewById(R.id.lastnameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        saveButton = findViewById(R.id.saveButton)
        closeButton = findViewById(R.id.close_button)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }

    private fun loadUserData() {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    nameEditText.setText(document.getString("name"))
                    lastnameEditText.setText(document.getString("lastname"))
                    userType = document.getString("userType") ?: ""
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al cargar los datos del usuario: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun saveUserData() {
        val user = auth.currentUser ?: return
        val credential = EmailAuthProvider.getCredential(user.email!!, passwordEditText.text.toString())

        user.reauthenticate(credential)
            .addOnSuccessListener {
                // Actualizar datos en Firestore
                updateUserInFirestore(user.uid)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error de autenticación: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateUserInFirestore(userId: String) {
        val updates = mapOf(
            "name" to nameEditText.text.toString(),
            "lastname" to lastnameEditText.text.toString()
        ) as Map<String, Any>  // Asegúrate de castear a Map<String, Any>

        db.collection("users").document(userId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Datos del usuario actualizados", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al actualizar datos: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }


    private fun navigateBasedOnUserType() {
        val intent = when (userType) {
            "Conductor" -> Intent(this, DriverActivity::class.java)
            else -> Intent(this, MainActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}
