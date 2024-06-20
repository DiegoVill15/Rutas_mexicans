package com.diegovillalobos.rutasmexicanas

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.GravityCompat
import com.diegovillalobos.rutasmexicanas.databinding.ActivityPrivacyPolicyBinding
import com.diegovillalobos.rutasmexicanas.databinding.ActivityTermsAndConditionsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PrivacyPolicyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPrivacyPolicyBinding
    private var userType: String? = null
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacyPolicyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val textViewTerms = findViewById<TextView>(R.id.textViewPrivacity)
        val termsText = getString(R.string.privacity_text)
        textViewTerms.text = HtmlCompat.fromHtml(termsText, HtmlCompat.FROM_HTML_MODE_LEGACY)

        auth = FirebaseAuth.getInstance()
        updateNavHeader() // Llamar después de establecer el contenido de la vista

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle("Aviso de privacidad")

        val toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        setupDrawerMenu()

        // Obtiene el tipo de usuario desde Firestore
        FirebaseAuth.getInstance().currentUser?.uid?.let { userId ->
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    userType = document.getString("userType")
                }
                .addOnFailureListener { e ->
                    Log.e("TermsAndConditionsActivity", "Error obteniendo el tipo de usuario", e)
                }
        }
    }

    private fun setupDrawerMenu() {
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    navigateBasedOnUserType(R.id.nav_home)
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, UserProfileActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_reservaciones -> {
                    navigateBasedOnUserType(R.id.nav_reservaciones)
                }
                R.id.nav_terms -> {
                    val intent = Intent(this, TermsAndConditionsActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_privacy -> {
                    val intent = Intent(this, PrivacyPolicyActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_logout -> {
                    auth.signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun updateNavHeader() {
        val headerView = binding.navView.getHeaderView(0)
        val emailTextView = headerView.findViewById<TextView>(R.id.email)
        val ratingTextView = headerView.findViewById<TextView>(R.id.rating)
        val nameTextView = headerView.findViewById<TextView>(R.id.nombre_apellidos)

        val user = auth.currentUser
        emailTextView.text = user?.email ?: "user@example.com"

        val db = FirebaseFirestore.getInstance()
        user?.let {
            db.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val name = document.getString("name")
                        val lastname = document.getString("lastname")
                        val rating = document.getDouble("rating") ?: 0.0
                        val fullName = "$name $lastname"
                        nameTextView.text = fullName
                        ratingTextView.text = "Rating: $rating"
                    }
                }
        }
    }

    private fun navigateBasedOnUserType(actionId: Int) {
        val intent = when (userType) {
            "Conductor" -> when (actionId) {
                R.id.nav_home -> Intent(this, DriverActivity::class.java)
                R.id.nav_reservaciones -> Intent(this, DriverTripsActivity::class.java)
                else -> null
            }
            "Pasajero" -> when (actionId) {
                R.id.nav_home -> Intent(this, MainActivity::class.java)
                R.id.nav_reservaciones -> Intent(this, UserReservationsActivity::class.java)
                else -> null
            }
            else -> null // Si no hay tipo de usuario, no hacer nada o manejar de otro modo
        }
        intent?.let { startActivity(it) }
    }
}


