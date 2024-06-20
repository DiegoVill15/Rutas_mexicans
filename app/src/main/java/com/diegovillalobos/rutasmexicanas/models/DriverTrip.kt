package com.diegovillalobos.rutasmexicanas.models

data class DriverTrip(
    var id: String = "",  // Asegúrate de que id sea mutable
    val origen: String = "",
    val destino: String = "",
    val fechaHora: String = "",
    val plazasDisponibles: Int = 0,
    val precio: Double = 0.0,
    var isRated: Boolean = false,
    val pasajeros: Map<String, Int> = emptyMap()
)

