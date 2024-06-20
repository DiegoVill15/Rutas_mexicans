package com.diegovillalobos.rutasmexicanas

import android.app.AlertDialog
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.diegovillalobos.rutasmexicanas.models.DriverTrip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import java.util.concurrent.CountDownLatch

class DriverTripsAdapter(private var trips: List<DriverTrip>) : RecyclerView.Adapter<DriverTripsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_trip_driver, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val trip = trips[position]
        holder.bind(trip)
    }

    override fun getItemCount() = trips.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(trip: DriverTrip) {
            val originTextView = itemView.findViewById<TextView>(R.id.textViewOrigin)
            val destinationTextView = itemView.findViewById<TextView>(R.id.textViewDestination)
            val dateTimeTextView = itemView.findViewById<TextView>(R.id.textViewDateTime)
            val availableSeatsTextView = itemView.findViewById<TextView>(R.id.textViewAvailableSeats)
            val priceTextView = itemView.findViewById<TextView>(R.id.textViewPrice)
            val passengerDetailsText = itemView.findViewById<TextView>(R.id.textViewPassengerDetails)

            Log.d("Debug", "Creando/Recuperando Trip: ID = ${trip.id}")

            originTextView.text = createBoldText("Origen: ", trip.origen)
            destinationTextView.text = createBoldText("Destino: ", trip.destino)
            dateTimeTextView.text = createBoldText("Fecha y Hora: ", trip.fechaHora)
            availableSeatsTextView.text = createBoldText("Asientos disponibles: ", trip.plazasDisponibles.toString())
            priceTextView.text = createBoldText("Precio: ", "$${trip.precio}")

            val detailsHeader = "Detalles de los pasajeros:\n"
            val spannableHeader = SpannableString(detailsHeader)
            spannableHeader.setSpan(StyleSpan(Typeface.BOLD), 0, detailsHeader.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            passengerDetailsText.text = spannableHeader

            if (trip.pasajeros.isEmpty()) {
                passengerDetailsText.append("Sin pasajeros")
            } else {
                trip.pasajeros.forEach { (userId, seats) ->
                    fetchPassengerDetails(userId) { name ->
                        val passengerInfo = "$name: $seats asientos\n"
                        passengerDetailsText.append(passengerInfo)
                    }
                }
            }

            val rateButton = itemView.findViewById<Button>(R.id.buttonRatePassengers)
            if (trip.isRated) {
                rateButton.isEnabled = false  // Desactiva el botón si el viaje ya fue calificado
                rateButton.alpha = 0.5f       // Opaca el botón para indicar visualmente que está desactivado
            } else {
                rateButton.isEnabled = true
                rateButton.alpha = 1.0f
                rateButton.setOnClickListener {
                    ratePassengers(trip)
                }
            }

        }

        private fun createBoldText(label: String, value: String): SpannableString {
            val spannable = SpannableString("$label$value")
            spannable.setSpan(StyleSpan(Typeface.BOLD), 0, label.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            return spannable
        }

        private fun fetchPassengerDetails(userId: String, callback: (String) -> Unit) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(userId).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name") ?: "No Name"
                    val lastname = document.getString("lastname") ?: "No Lastname"
                    val fullName = "$name $lastname"
                    callback(fullName)
                } else {
                    Log.d("DriverTripsAdapter", "No se encontraron detalles para el pasajero con ID: $userId")
                }
            }.addOnFailureListener { e ->
                Log.e("DriverTripsAdapter", "Error al obtener detalles del pasajero: $e")
            }
        }

        private fun ratePassengers(trip: DriverTrip) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val tripRef = FirebaseFirestore.getInstance().collection("users").document(userId).collection("viajesConductor").document(trip.id)

            tripRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val isRated = document.getBoolean("isRated") ?: false
                    Log.d("DriverTripsAdapter", "Attempting to rate: isRated = $isRated")
                    if (isRated) {
                        Toast.makeText(itemView.context, "Este viaje ya ha sido calificado.", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    val passengerList = mutableListOf<Pair<String, String>>()
                    trip.pasajeros.forEach { (userId, _) ->
                        FirebaseFirestore.getInstance().collection("users").document(userId).get().addOnSuccessListener { userDoc ->
                            if (userDoc.exists()) {
                                val name = userDoc.getString("name") ?: "No Name"
                                val lastname = userDoc.getString("lastname") ?: "No Lastname"
                                val fullName = "$name $lastname"
                                passengerList.add(userId to fullName)
                            }
                            if (passengerList.size == trip.pasajeros.size) {
                                showRatingDialog(passengerList, trip)
                            }
                        }
                    }
                } else {
                    Log.e("DriverTripsAdapter", "No se encontró el documento del viaje.")
                }
            }.addOnFailureListener { e ->
                Log.e("DriverTripsAdapter", "Error al obtener los detalles del viaje", e)
            }
        }

        private fun showRatingDialog(passengerList: List<Pair<String, String>>, trip: DriverTrip) {
            // Inflar vista y configurar RecyclerView para el diálogo
            val dialogView = LayoutInflater.from(itemView.context).inflate(R.layout.dialog_rate_passenger, null)
            val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewPassengerRatings)
            val adapter = PassengerRatingAdapter(passengerList)
            recyclerView.layoutManager = LinearLayoutManager(itemView.context)
            recyclerView.adapter = adapter

            // Crear y mostrar diálogo
            val dialog = AlertDialog.Builder(itemView.context)
                .setView(dialogView)
                .setPositiveButton("Enviar") { _, _ ->
                    if (adapter.areAllRated()) {
                        val ratings = adapter.getRatings()
                        submitRatings(trip, ratings)
                    } else {
                        Toast.makeText(itemView.context, "Por favor, califica a todos los pasajeros.", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .create()
            dialog.show()
        }

        private fun submitRatings(trip: DriverTrip, ratings: Map<String, Float>) {
            val db = FirebaseFirestore.getInstance()
            val batch = db.batch()

            ratings.forEach { (userId, newRating) ->
                val userRef = db.collection("users").document(userId)
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(userRef)
                    val oldRating = snapshot.getDouble("rating") ?: 0.0
                    val ratingCount = snapshot.getLong("ratingCount") ?: 0

                    val newAverage = if (ratingCount > 0) {
                        ((oldRating * ratingCount + newRating) / (ratingCount + 1)).coerceAtMost(5.0)
                    } else {
                        newRating.toDouble()
                    }

                    val roundedAverage = String.format(Locale.US, "%.2f", newAverage).toDouble()

                    transaction.update(userRef, "rating", roundedAverage)
                    transaction.update(userRef, "ratingCount", ratingCount + 1)
                }.addOnSuccessListener {
                    Toast.makeText(itemView.context, "Calificaciones enviadas correctamente.", Toast.LENGTH_SHORT).show()
                    markTripAsRated(trip)
                }.addOnFailureListener { e ->
                    Log.e("DriverTripsAdapter", "Error al enviar calificaciones: $e")
                    Toast.makeText(itemView.context, "Error al enviar calificaciones.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        private fun markTripAsRated(trip: DriverTrip) {
            val db = FirebaseFirestore.getInstance()
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val tripRef = db.collection("users").document(userId).collection("viajesConductor").document(trip.id)

            if (trip.id.isNullOrEmpty()) {
                Log.e("DriverTripsAdapter", "Trip ID is null or empty")
                return  // Termina la ejecución si no hay un ID válido
            }

            tripRef.update("isRated", true)
                .addOnSuccessListener {
                    Log.d("DriverTripsAdapter", "Trip marked as rated")
                }
                .addOnFailureListener { e ->
                    Log.e("DriverTripsAdapter", "Error marking trip as rated", e)
                }
        }



    }
}