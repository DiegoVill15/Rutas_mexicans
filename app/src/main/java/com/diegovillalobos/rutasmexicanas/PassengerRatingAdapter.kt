package com.diegovillalobos.rutasmexicanas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PassengerRatingAdapter(private val passengerList: List<Pair<String, String>>) : RecyclerView.Adapter<PassengerRatingAdapter.ViewHolder>() {
    private val ratings = mutableMapOf<String, Float>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_passenger_rating, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (userId, name) = passengerList[position]
        holder.bind(userId, name)
    }

    override fun getItemCount() = passengerList.size

    fun getRatings(): Map<String, Float> = ratings

    fun areAllRated(): Boolean = ratings.size == passengerList.size && ratings.values.all { it > 0 }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.textViewPassengerName)
        private val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBarPassenger)

        fun bind(userId: String, name: String) {
            nameTextView.text = name
            ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
                ratings[userId] = rating
            }
        }
    }
}
