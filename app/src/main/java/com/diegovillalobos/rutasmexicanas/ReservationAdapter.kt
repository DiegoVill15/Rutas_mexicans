package com.diegovillalobos.rutasmexicanas.adapters

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.diegovillalobos.rutasmexicanas.R
import com.diegovillalobos.rutasmexicanas.models.Reservation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class ReservationAdapter(private var reservations: List<Reservation>) :
    RecyclerView.Adapter<ReservationAdapter.ReservationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reservation, parent, false)
        return ReservationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReservationViewHolder, position: Int) {
        holder.bind(reservations[position])
    }

    override fun getItemCount() = reservations.size

    fun updateData(newReservations: List<Reservation>) {
        reservations = newReservations
        notifyDataSetChanged()
    }

    class ReservationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val fechaHoraTextView: TextView = itemView.findViewById(R.id.fechaHoraTextView)
        private val origenTextView: TextView = itemView.findViewById(R.id.origenTextView)
        private val destinoTextView: TextView = itemView.findViewById(R.id.destinoTextView)
        private val precioTotalTextView: TextView = itemView.findViewById(R.id.precioTotalTextView)
        private val numeroAsientosTextView: TextView = itemView.findViewById(R.id.numeroAsientosTextView)
        private val direccionSalidaTextView: TextView = itemView.findViewById(R.id.direccionSalidaTextView)
        private val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBar)

        fun bind(reservation: Reservation) {
            val boldStyle = StyleSpan(Typeface.BOLD) // Estilo en negrita

            // Fecha de Salida
            val fechaHoraText = SpannableString("Fecha de Salida: ${reservation.fechaHora}")
            fechaHoraText.setSpan(boldStyle, 0, "Fecha de Salida:".length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            fechaHoraTextView.text = fechaHoraText

            // Origen
            val origenText = SpannableString("Origen: ${reservation.origen}")
            origenText.setSpan(boldStyle, 0, "Origen:".length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            origenTextView.text = origenText

            // Destino
            val destinoText = SpannableString("Destino: ${reservation.destino}")
            destinoText.setSpan(boldStyle, 0, "Destino:".length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            destinoTextView.text = destinoText

            // Total
            val precioTotalText = SpannableString("Total: $${String.format("%.2f", reservation.precioTotal)}")
            precioTotalText.setSpan(boldStyle, 0, "Total:".length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            precioTotalTextView.text = precioTotalText

            // Asientos
            val numeroAsientosText = SpannableString("Asientos: ${reservation.numeroAsientos}")
            numeroAsientosText.setSpan(boldStyle, 0, "Asientos:".length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            numeroAsientosTextView.text = numeroAsientosText

            // Dirección
            val direccionSalidaText = SpannableString("Dirección: ${reservation.direccionSalida}")
            direccionSalidaText.setSpan(boldStyle, 0, "Dirección:".length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            direccionSalidaTextView.text = direccionSalidaText

            // Configura la calificación y la barra de calificación
            reservation.rating?.let { rating ->
                ratingBar.rating = rating
                ratingBar.isEnabled = false
            } ?: run {
                ratingBar.rating = 0f
                ratingBar.isEnabled = true
                ratingBar.onRatingBarChangeListener = RatingBar.OnRatingBarChangeListener { _, rating, _ ->
                    updateConductorRating(reservation.id, reservation.conductorId, rating)
                    ratingBar.isEnabled = false // Deshabilita la barra después de calificar
                }
            }
        }


        private fun updateConductorRating(reservationId: String, conductorId: String, rating: Float) {
            val db = FirebaseFirestore.getInstance()

            db.collection("users").document(conductorId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val currentRating = document.getDouble("rating") ?: 0.0
                        val ratingCount = document.getLong("ratingCount") ?: 0

                        // Calcular el nuevo promedio
                        val newRating = ((currentRating * ratingCount) + rating) / (ratingCount + 1)
                        val formattedRating = String.format(Locale.getDefault(), "%.2f", newRating).toDouble()
                        val newRatingCount = ratingCount + 1

                        // Actualizar la calificación y el número de calificaciones en Firestore
                        db.collection("users").document(conductorId)
                            .update("rating", formattedRating, "ratingCount", newRatingCount)
                            .addOnSuccessListener {
                                Log.d("ReservationAdapter", "Calificación del conductor actualizada.")
                            }
                            .addOnFailureListener { e ->
                                Log.e("ReservationAdapter", "Error al actualizar la calificación del conductor", e)
                            }
                    }
                }

            // Actualizar el rating en la reserva
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            db.collection("users").document(userId).collection("reservas").document(reservationId)
                .update("rating", rating)
                .addOnSuccessListener {
                    Log.d("ReservationAdapter", "Calificación de la reserva almacenada.")
                }
                .addOnFailureListener { e ->
                    Log.e("ReservationAdapter", "Error al almacenar la calificación de la reserva", e)
                }
        }
    }

}


