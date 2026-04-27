package com.llorente.tfg_gpsreminders.utils

import java.util.Locale

object LocationUtils {

    private const val MANUAL_SELECTION_TEXT = "Selección personalizada"

    fun formatCoordinate(value: Double?): String {
        return if (value != null) {
            String.format(Locale.US, "%.4f", value)
        } else {
            "-"
        }
    }

    fun formatCoordinates(latitude: Double?, longitude: Double?): String {
        return if (latitude != null && longitude != null) {
            "Latitud: ${formatCoordinate(latitude)}\nLongitud: ${formatCoordinate(longitude)}"
        } else {
            "Sin ubicación asociada"
        }
    }

    fun formatCoordinatesInline(latitude: Double?, longitude: Double?): String {
        return if (latitude != null && longitude != null) {
            "Lat: ${formatCoordinate(latitude)}, Lon: ${formatCoordinate(longitude)}"
        } else {
            "Sin ubicación asociada"
        }
    }

    fun normalizePlaceName(placeName: String?): String? {
        val cleaned = placeName?.trim()

        return when {
            cleaned.isNullOrBlank() -> null
            cleaned.equals("Desconocido", ignoreCase = true) -> MANUAL_SELECTION_TEXT
            cleaned.equals("Ubicación seleccionada", ignoreCase = true) -> MANUAL_SELECTION_TEXT
            cleaned.equals("Selección personalizada", ignoreCase = true) -> MANUAL_SELECTION_TEXT
            else -> cleaned
        }
    }

    fun buildLocationLabel(
        placeName: String?,
        address: String?,
        latitude: Double?,
        longitude: Double?,
        emptyText: String = "No seleccionado"
    ): String {
        val normalizedPlaceName = normalizePlaceName(placeName)
        val cleanedAddress = address?.trim()

        return when {
            !normalizedPlaceName.isNullOrBlank() -> normalizedPlaceName
            !cleanedAddress.isNullOrBlank() && !cleanedAddress.equals("Desconocido", ignoreCase = true) -> cleanedAddress
            latitude != null && longitude != null -> MANUAL_SELECTION_TEXT
            else -> emptyText
        }
    }
}
