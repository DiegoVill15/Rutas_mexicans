package com.diegovillalobos.rutasmexicanas

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.diegovillalobos.rutasmexicanas.models.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class TripDetailActivity : AppCompatActivity() {
    private lateinit var gridLayout: GridLayout
    private lateinit var reserveButton: Button
    private lateinit var cashCheckBox: CheckBox
    private lateinit var seatsTextView: TextView
    private var totalPrice: Double = 0.0
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_detail)

        auth = FirebaseAuth.getInstance()
        val trip = intent.getParcelableExtra<Trip>("TRIP_EXTRA") ?: return

        // Inicialización de vistas
        initViews(trip)

        gridLayout = findViewById<GridLayout>(R.id.seatGridLayout) // Asegúrate de que esta línea está antes de llamar a setupSeatGrid(trip)
        setupSeatGrid(trip)
    }


    private fun initViews(trip: Trip) {
        val originTextView = findViewById<TextView>(R.id.originTextView)
        val destinationTextView = findViewById<TextView>(R.id.destinationTextView)
        val dateTimeTextView = findViewById<TextView>(R.id.dateTimeTextView)
        seatsTextView = findViewById<TextView>(R.id.seatsTextView)
        val priceTextView = findViewById<TextView>(R.id.priceTextView)
        val statusTextView = findViewById<TextView>(R.id.statusTextView)
        val direccionSalidaTextView = findViewById<TextView>(R.id.direccionSalidaTextView) // Asegúrate de que este ID está correcto

        originTextView.text = Html.fromHtml("<b>Origen:</b> ${trip.origen}")
        destinationTextView.text = Html.fromHtml("<b>Destino:</b> ${trip.destino}")
        dateTimeTextView.text = Html.fromHtml("<b>Fecha:</b> ${trip.fechaHora}")
        seatsTextView.text = Html.fromHtml("<b>Asientos disponibles:</b> ${trip.plazasDisponibles}")
        priceTextView.text = Html.fromHtml("<b>Precio:</b> \$${trip.precio}")
        statusTextView.text = Html.fromHtml("<b>Estado:</b> ${trip.estado}")
        direccionSalidaTextView.text = Html.fromHtml("<b>Dirección de Salida:</b> ${trip.direccionSalida}") // Verifica que esta línea esté correctamente asignando el valor

        val reserveButton = findViewById<Button>(R.id.btnBook)
        reserveButton.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                reserveTrip(trip, userId)
            } else {
                Toast.makeText(this, "Usuario no identificado", Toast.LENGTH_SHORT).show()
            }
        }

        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener {
            finish()  // Finaliza la actividad actual y regresa a la actividad anterior en la pila
        }
    }


    private fun setupSeatGrid(trip: Trip) {
        gridLayout.removeAllViews()
        gridLayout.columnCount = 2

        trip.asientos.forEachIndexed { index, estado ->
            val seatImage = ImageView(this).apply {
                layoutParams = GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f),
                    GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f)
                ).apply {
                    width = 200
                    height = 200
                    rightMargin = 10
                    topMargin = 10
                    bottomMargin = 10
                    leftMargin = 10
                }
                setImageResource(if (estado == "libre") R.drawable.ic_seat_available else R.drawable.ic_seat_occupied)
                setOnClickListener { toggleSeat(index, trip) }
            }
            gridLayout.addView(seatImage)
        }
    }


    private fun toggleSeat(index: Int, trip: Trip) {
        val seatImage = gridLayout.getChildAt(index) as ImageView
        val estado = trip.asientos[index]

        when (estado) {
            "libre" -> {
                trip.asientos[index] = "seleccionado"
                seatImage.setImageResource(R.drawable.ic_seat_selected)
                totalPrice += trip.precio
            }
            "seleccionado" -> {
                trip.asientos[index] = "libre"
                seatImage.setImageResource(R.drawable.ic_seat_available)
                totalPrice -= trip.precio
            }
            "ocupado" -> {
                // No hacer nada si el asiento está ocupado
            }
        }

        // Actualizar el TextView del precio total
        val totalPriceTextView = findViewById<TextView>(R.id.totalprice)
        totalPriceTextView.text = "Total a pagar: $${totalPrice}"

        // Actualizar los datos en Firestore
        val db = FirebaseFirestore.getInstance()
        db.collection("viajes").document(trip.id)
            .update("asientos", trip.asientos)
            .addOnSuccessListener {
                Log.d("TripDetailActivity", "Asientos actualizados correctamente en Firestore.")
            }
            .addOnFailureListener { e ->
                Log.e("TripDetailActivity", "Error al actualizar asientos en Firestore: ${e.message}")
            }
    }

    private fun reserveTrip(trip: Trip, userId: String) {
        val selectedSeats = trip.asientos.count { it == "seleccionado" }
        val paymentMethodRadioGroup = findViewById<RadioGroup>(R.id.paymentMethodRadioGroup)

        if (selectedSeats <= 0) {
            Toast.makeText(this, "Por favor, selecciona al menos un asiento.", Toast.LENGTH_SHORT).show()
            return
        }

        if (paymentMethodRadioGroup.checkedRadioButtonId == -1) {
            Toast.makeText(this, "Por favor, selecciona un método de pago.", Toast.LENGTH_SHORT).show()
            return
        }

        val newAvailableSeats = trip.plazasDisponibles - selectedSeats
        if (newAvailableSeats < 0) {
            Toast.makeText(this, "No hay plazas disponibles", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedSeats = trip.asientos.map { if (it == "seleccionado") "ocupado" else it }
        val db = FirebaseFirestore.getInstance()

        db.collection("viajes").document(trip.id)
            .update(mapOf(
                "plazasDisponibles" to newAvailableSeats,
                "asientos" to updatedSeats
            ))
            .addOnSuccessListener {
                val reservationData = mapOf(
                    "fechaHora" to trip.fechaHora,
                    "origen" to trip.origen,
                    "destino" to trip.destino,
                    "precioTotal" to selectedSeats * trip.precio,
                    "numeroAsientos" to selectedSeats,
                    "direccionSalida" to trip.direccionSalida,
                    "conductorId" to trip.conductorId
                )

                db.collection("users").document(userId).collection("reservas")
                    .add(reservationData)

                val passengerRef = db.collection("users").document(trip.conductorId).collection("viajesConductor").document(trip.id)

                db.runTransaction { transaction ->
                    val snapshot = transaction.get(passengerRef)
                    val passengerMap = snapshot.get("pasajeros") as? MutableMap<String, Any> ?: mutableMapOf()

                    val currentSeats = (passengerMap[userId] as? Number)?.toInt() ?: 0
                    passengerMap[userId] = currentSeats + selectedSeats

                    transaction.update(passengerRef, "pasajeros", passengerMap)
                }.addOnSuccessListener {
                    Toast.makeText(this, "Viaje reservado con éxito", Toast.LENGTH_LONG).show()
                    finish()
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Error al actualizar pasajero: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al actualizar viaje: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }





}
