package com.diegovillalobos.rutasmexicanas

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import com.diegovillalobos.rutasmexicanas.databinding.ActivityDriverTripsBinding
import com.diegovillalobos.rutasmexicanas.models.DriverTrip
import com.google.firebase.firestore.FirebaseFirestore
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth


class DriverTripsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDriverTripsBinding
    private lateinit var tripsAdapter: DriverTripsAdapter
    private var tripsList = mutableListOf<DriverTrip>()
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        binding = ActivityDriverTripsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurando la ActionBar con el DrawerLayout
        setSupportActionBar(binding.toolbar)
        setupDrawer()

        auth = FirebaseAuth.getInstance()
        updateNavHeader() // Llamar después de establecer el contenido de la vista

        setupRecyclerView()
        loadTrips()
    }

    private fun setupRecyclerView() {
        tripsAdapter = DriverTripsAdapter(tripsList)
        binding.tripsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@DriverTripsActivity)
            adapter = tripsAdapter
        }
    }

    private fun loadTrips() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(userId).collection("viajesConductor")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d("DriverTripsActivity", "No trips found for user: $userId")
                    Toast.makeText(this, "No se encontraron viajes.", Toast.LENGTH_SHORT).show()
                } else {
                    documents.forEach { document ->
                        val trip = document.toObject(DriverTrip::class.java)
                        tripsList.add(trip)
                        Log.d("DriverTripsActivity", "Loaded trip: ${trip.destino}")
                    }
                    tripsAdapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener { e ->
                Log.e("DriverTripsActivity", "Error loading trips", e)
                Toast.makeText(this, "Error al cargar los viajes: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setupDrawer() {
        val toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, DriverActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, UserProfileActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_reservaciones -> {
                    val intent = Intent(this, DriverTripsActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_terms -> {
                    val intent = Intent(this, TermsAndConditionsActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_privacy -> {
                    val intent = Intent(this, DriverTripsActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_logout -> {
                    auth.signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
            binding.drawerLayout.closeDrawers() // Cerrar el drawer cuando se selecciona un item
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

}

