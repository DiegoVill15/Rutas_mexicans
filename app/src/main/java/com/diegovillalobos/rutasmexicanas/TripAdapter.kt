package com.diegovillalobos.rutasmexicanas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.diegovillalobos.rutasmexicanas.models.Trip

class TripAdapter(
    private var trips: MutableList<Trip>, // Asegúrate de que esto es MutableList
    private val onTripClick: (Trip) -> Unit
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_trip, parent, false)
        return TripViewHolder(view, onTripClick)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(trips[position])
    }

    override fun getItemCount(): Int = trips.size


    fun updateData(newTrips: List<Trip>) {
        trips = ArrayList(newTrips)  // Crea una nueva lista basada en newTrips
        notifyDataSetChanged()
    }


    class TripViewHolder(itemView: View, private val onTripClick: (Trip) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val originTextView: TextView = itemView.findViewById(R.id.textOrigin)
        private val destinationTextView: TextView = itemView.findViewById(R.id.textDestination)
        private val departureTimeTextView: TextView = itemView.findViewById(R.id.textDepartureTime)
        private val seatsTextView: TextView = itemView.findViewById(R.id.textSeatsAvailable)
        private val priceTextView: TextView = itemView.findViewById(R.id.textPrice)
        private val driverNameTextView: TextView = itemView.findViewById(R.id.textDriverName)
        private val ratingTextView: TextView = itemView.findViewById(R.id.textDriverRating)

        fun bind(trip: Trip) {
            originTextView.text = trip.origen
            destinationTextView.text = trip.destino
            departureTimeTextView.text = trip.fechaHora
            seatsTextView.text = "${trip.plazasDisponibles}"
            priceTextView.text = "$${trip.precio}"
            driverNameTextView.text = trip.driverName
            ratingTextView.text = "${trip.conductorRating} ★"
            itemView.setOnClickListener { onTripClick(trip) }

            if (trip.plazasDisponibles > 0) {
                itemView.setOnClickListener { onTripClick(trip) }
            } else {
                itemView.isClickable = false
                itemView.alpha = 0.5f // Opcional: Cambia la apariencia para indicar que no se puede seleccionar
            }
        }
    }
}