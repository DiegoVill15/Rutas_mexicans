package com.diegovillalobos.rutasmexicanas

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.diegovillalobos.rutasmexicanas.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.diegovillalobos.rutasmexicanas.models.Trip
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var tripAdapter: TripAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var originSpinner: Spinner
    private lateinit var destinationSpinner: Spinner
    private lateinit var dateInput: EditText
    private var allTrips: ArrayList<Trip> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        setupDrawer()

        // Inicialización de componentes de la UI
        recyclerView = findViewById(R.id.tripsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        originSpinner = findViewById(R.id.spinnerOrigin)
        destinationSpinner = findViewById(R.id.spinnerDestination)
        dateInput = findViewById(R.id.dateInput) // Asegúrate de que este ID coincide con el XML

        setupDatePicker()  // Ahora puedes llamar a setupDatePicker

        loadSpinnerData()

        findViewById<Button>(R.id.searchButton).setOnClickListener {
            filterTrips()
        }

        val clearButton = findViewById<ImageButton>(R.id.clearFilterButton)
        clearButton.setOnClickListener {
            clearFilters()
        }

        auth = FirebaseAuth.getInstance()
        updateNavHeader()

    }

    override fun onResume() {
        super.onResume()
        loadTripsFromFirestore() // Asegura que los datos están actualizados cuando la actividad se reanuda.
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        dateInput.setOnClickListener {
            DatePickerDialog(this, DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                dateInput.setText(format.format(calendar.time))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
        dateInput.isFocusable = false
        dateInput.isCursorVisible = false
    }

    private fun loadSpinnerData() {
        val db = FirebaseFirestore.getInstance()
        val originSet = mutableSetOf<String>()
        val destinationSet = mutableSetOf<String>()

        db.collection("viajes").get().addOnSuccessListener { documents ->
            for (document in documents) {
                val trip = document.toObject(Trip::class.java)
                originSet.add(trip.origen)
                destinationSet.add(trip.destino)
            }
            val originAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                originSet.toList()
            )
            val destinationAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                destinationSet.toList()
            )

            originSpinner.adapter = originAdapter
            destinationSpinner.adapter = destinationAdapter
        }.addOnFailureListener { exception ->
            Log.d("MainActivity", "Error loading spinner data: ", exception)
        }
    }


    private fun filterTrips() {
        val selectedOrigin = originSpinner.selectedItem.toString()
        val selectedDestination = destinationSpinner.selectedItem.toString()
        val selectedDate = dateInput.text.toString()

        val filteredTrips = allTrips.filter { trip ->
            trip.origen == selectedOrigin && trip.destino == selectedDestination && trip.fechaHora.startsWith(selectedDate)
        }

        if (filteredTrips.isEmpty()) {
            Toast.makeText(this, "No se encontraron viajes con los criterios especificados.", Toast.LENGTH_LONG).show()
        }
        tripAdapter.updateData(ArrayList(filteredTrips))
    }

    private fun loadTripsFromFirestore() {
        val db = FirebaseFirestore.getInstance()
        db.collection("viajes")
            .get()
            .addOnSuccessListener { documents ->
                allTrips.clear()
                for (document in documents) {
                    val trip = document.toObject(Trip::class.java).apply {
                        id = document.id
                    }
                    fetchDriverDetails(trip, db) { updatedTrip ->
                        allTrips.add(updatedTrip)
                        if (allTrips.size == documents.size()) {
                            updateUI(allTrips)  // Llama a updateUI cuando todos los viajes han sido procesados
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.d("MainActivity", "Error getting documents: ", exception)
            }
    }

    private fun fetchDriverDetails(trip: Trip, db: FirebaseFirestore, callback: (Trip) -> Unit) {
        db.collection("users").document(trip.conductorId)
            .get()
            .addOnSuccessListener { userDoc ->
                trip.conductorRating = userDoc.getDouble("rating") ?: 0.0
                trip.driverName = userDoc.getString("name") ?: "Unknown Driver"
                callback(trip)
            }
            .addOnFailureListener { e ->
                Log.d("MainActivity", "Error fetching driver details", e)
                callback(trip)
            }
    }

    private fun updateUI(trips: List<Trip>) {
        val mutableTrips = if (trips is MutableList<Trip>) trips else trips.toMutableList()

        tripAdapter = TripAdapter(mutableTrips) { trip ->
            if (trip.plazasDisponibles > 0) {
                val intent = Intent(this, TripDetailActivity::class.java).apply {
                    putExtra("TRIP_EXTRA", trip)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "No hay plazas disponibles para este viaje.", Toast.LENGTH_SHORT).show()
            }
        }
        recyclerView.adapter = tripAdapter
    }


    private fun clearFilters() {

        // Actualizar el adaptador con todos los viajes sin filtrar
        tripAdapter.updateData(allTrips)
        recyclerView.adapter = tripAdapter


        dateInput.setText("")
        originSpinner.setSelection(0)
        destinationSpinner.setSelection(0)
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

