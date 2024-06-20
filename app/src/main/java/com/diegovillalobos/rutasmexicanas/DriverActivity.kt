package com.diegovillalobos.rutasmexicanas

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.diegovillalobos.rutasmexicanas.databinding.ActivityDriverBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.appcompat.app.ActionBarDrawerToggle
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class DriverActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDriverBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurando la ActionBar con el DrawerLayout
        setSupportActionBar(binding.toolbar)
        setupDrawer()

        auth = FirebaseAuth.getInstance()
        updateNavHeader() // Llamar después de establecer el contenido de la vista

        initializeUi()  // Asegúrate de llamar a esta función aquí

        // Listener para el botón de registrar viaje
        binding.registerTripButton.setOnClickListener {
            registerTravel()
        }
    }


    private fun registerTravel() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        if (userId.isEmpty()) {
            Toast.makeText(this, "Error: Usuario no identificado.", Toast.LENGTH_SHORT).show()
            return
        }

        // Ocultar el teclado antes de procesar el registro
        hideKeyboard()

        val origin = binding.originSpinner.selectedItem.toString()
        val destination = binding.destinationSpinner.selectedItem.toString()
        val dateTime = binding.dateTimeEditText.text.toString().trim()
        val seats = binding.seatsEditText.text.toString().toIntOrNull() ?: 0
        val price = binding.priceEditText.text.toString().toDoubleOrNull() ?: 0.0
        val status = binding.statusSpinner.selectedItem.toString()
        val direction = binding.directionEditText.text.toString().trim()

        // Verificaciones adicionales antes de proceder
        if (origin == destination) {
            Toast.makeText(this, "El origen y el destino no pueden ser iguales.", Toast.LENGTH_SHORT).show()
            return
        }

        // Intenta parsear la fecha y verifica que sea al menos una hora en el futuro
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        try {
            val tripDate = sdf.parse(dateTime)
            val currentDate = Calendar.getInstance().time
            if (tripDate == null || tripDate.before(currentDate) || (tripDate.time - currentDate.time) < 3600000) {
                Toast.makeText(this, "La fecha del viaje debe ser al menos una hora más tarde que el tiempo actual.", Toast.LENGTH_SHORT).show()
                return
            }
        } catch (e: ParseException) {
            Toast.makeText(this, "Formato de fecha y hora no válido.", Toast.LENGTH_SHORT).show()
            return
        }

        if (seats <= 0 || seats > 6) {
            Toast.makeText(this, "El número de plazas debe ser entre 1 y 6.", Toast.LENGTH_SHORT).show()
            return
        }

        val seatList = List(seats) { "libre" }
        val travelData = hashMapOf(
            "origen" to origin,
            "destino" to destination,
            "fechaHora" to dateTime,
            "plazasDisponibles" to seats,
            "precio" to price,
            "estado" to status,
            "conductorId" to userId,
            "direccionSalida" to direction,
            "asientos" to seatList,
            "isRated" to false // Añadir este campo
        )

        FirebaseFirestore.getInstance().collection("viajes")
            .add(travelData)
            .addOnSuccessListener { documentReference ->
                val viajeId = documentReference.id
                travelData["id"] = viajeId // Añadir el ID del documento a los datos del viaje
                FirebaseFirestore.getInstance().collection("users").document(userId).collection("viajesConductor").document(viajeId)
                    .set(travelData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Viaje registrado exitosamente.", Toast.LENGTH_SHORT).show()
                        clearFields()
                        Log.d("DriverActivity", "Viaje ID almacenado con éxito: $viajeId")
                    }
                    .addOnFailureListener { e ->
                        Log.e("DriverActivity", "Error al almacenar el ID del viaje", e)
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al registrar el viaje: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }




    private fun clearFields() {
        binding.dateTimeEditText.setText("")
        binding.seatsEditText.setText("")
        binding.priceEditText.setText("")
        binding.directionEditText.setText("")
        binding.originSpinner.setSelection(0)
        binding.destinationSpinner.setSelection(0)
        binding.statusSpinner.setSelection(0)
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }


    private fun initializeUi() {
        // Configuración de DatePicker y TimePicker
        binding.dateTimeEditText.setOnClickListener {
            showDateTimeDialog(binding.dateTimeEditText)
        }
    }

    private fun showDateTimeDialog(dateEditText: EditText) {
        val calendar = Calendar.getInstance()
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, monthOfYear)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            // Ahora muestra el TimePicker
            val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)

                val myFormat = "dd/MM/yyyy HH:mm" // Cambia esto según necesites
                val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
                dateEditText.setText(sdf.format(calendar.time))
            }

            TimePickerDialog(this, timeSetListener, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        DatePickerDialog(this, dateSetListener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)).show()
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