package com.diegovillalobos.rutasmexicanas.models

import android.os.Parcel
import android.os.Parcelable

data class Trip(
    var id: String = "",
    val origen: String = "",
    val destino: String = "",
    val fechaHora: String = "",
    var plazasDisponibles: Int = 0,
    val precio: Double = 0.0,
    val estado: String = "",
    val conductorId: String = "",
    var conductorRating: Double = 0.0,
    var driverName: String = "",
    var direccionSalida: String = "",
    var asientos: MutableList<String> = mutableListOf() // Cambio a MutableList para permitir modificaciones

) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readString() ?: "",
        origen = parcel.readString() ?: "",
        destino = parcel.readString() ?: "",
        fechaHora = parcel.readString() ?: "",
        plazasDisponibles = parcel.readInt(),
        precio = parcel.readDouble(),
        estado = parcel.readString() ?: "",
        conductorId = parcel.readString() ?: "",
        conductorRating = parcel.readDouble(),
        driverName = parcel.readString() ?: "",
        direccionSalida = parcel.readString() ?: "",
        asientos = mutableListOf<String>().apply {
            parcel.readStringList(this)
        }
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(origen)
        parcel.writeString(destino)
        parcel.writeString(fechaHora)
        parcel.writeInt(plazasDisponibles)
        parcel.writeDouble(precio)
        parcel.writeString(estado)
        parcel.writeString(conductorId)
        parcel.writeDouble(conductorRating)
        parcel.writeString(driverName)
        parcel.writeString(direccionSalida)
        parcel.writeStringList(asientos)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Trip> {
        override fun createFromParcel(parcel: Parcel): Trip {
            return Trip(parcel)
        }

        override fun newArray(size: Int): Array<Trip?> {
            return arrayOfNulls(size)
        }
    }
}