package com.diegovillalobos.rutasmexicanas

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.diegovillalobos.rutasmexicanas.adapters.ReservationAdapter
import com.diegovillalobos.rutasmexicanas.databinding.ActivityUserReservationsBinding
import com.diegovillalobos.rutasmexicanas.models.Reservation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserReservationsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ReservationAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var binding: ActivityUserReservationsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserReservationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        recyclerView = findViewById(R.id.reservationsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ReservationAdapter(listOf())
        recyclerView.adapter = adapter

        loadReservations()
        setSupportActionBar(binding.toolbar)
        setupDrawer()
        updateNavHeader()
    }

    private fun loadReservations() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("reservas")
            .get()
            .addOnSuccessListener { documents ->
                val reservations = documents.map { doc ->
                    val reservation = doc.toObject(Reservation::class.java).copy(id = doc.id)
                    reservation
                }
                adapter.updateData(reservations)
                binding.emptyView.visibility = if (reservations.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar las reservas: ${e.message}", Toast.LENGTH_LONG).show()
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
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, UserProfileActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_reservaciones -> {
                    val intent = Intent(this, UserReservationsActivity::class.java)
                    startActivity(intent)
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
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                // Añade más casos según tus ítems del menú
            }
            binding.drawerLayout.closeDrawers()
            true
        }
    }

    private fun updateNavHeader() {
        val headerView = binding.navView.getHeaderView(0)
        val usernameTextView = headerView.findViewById<TextView>(R.id.nombre_apellidos)
        val emailTextView = headerView.findViewById<TextView>(R.id.email)
        val ratingTextView = headerView.findViewById<TextView>(R.id.rating)

        val user = FirebaseAuth.getInstance().currentUser
        user?.let { firebaseUser ->
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(firebaseUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("name") ?: ""
                        val lastname = document.getString("lastname") ?: ""
                        val email = document.getString("email") ?: "No Email"
                        val rating = document.getDouble("rating") ?: 0.0

                        usernameTextView?.text = "$name $lastname"
                        emailTextView.text = email
                        ratingTextView.text = "★ $rating"

                        Log.d("MainActivity", "Rating retrieved: $rating")
                    } else {
                        Log.d("MainActivity", "No such document")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MainActivity", "Error fetching user details", e)
                }
        }
    }
}

