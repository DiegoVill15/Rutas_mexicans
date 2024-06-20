package com.diegovillalobos.rutasmexicanas.models

data class Reservation(
    val id: String = "",
    val origen: String = "",
    val destino: String = "",
    val fechaHora: String = "",
    val precioTotal: Double = 0.0,
    val numeroAsientos: Int = 0,
    val direccionSalida: String = "",
    val conductorId: String = "",
    var ratingGiven: Boolean = false, // Asegúrate de incluir este campo
    var rating: Float? = null // Almacena la calificación dada, si aplica
)

