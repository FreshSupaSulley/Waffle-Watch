package io.github.freshsupasulley.wafflewatch.model

import org.json.JSONObject

enum class LocationStatus { GREEN, YELLOW, RED }

data class LocationsResponse(val timestamp: Long, val locations: List<WaffleHouseLocation>)

data class WaffleHouseLocation(
    val locationId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val status: LocationStatus,
    val formattedHours: String,
)

fun parseLocations(json: String): LocationsResponse {
    val root = JSONObject(json)
    val timestamp = root.getLong("timestamp")
    val array = root.getJSONArray("locations")
    val locations = buildList {
        for (i in 0 until array.length()) {
            val loc = array.getJSONObject(i)
            val code = loc.optString("code", "")
            val status = when (loc.optString("status", "")) {
                "open" -> LocationStatus.GREEN
                else   -> LocationStatus.RED
            }
            add(WaffleHouseLocation(
                locationId = code,
                name = "Waffle House #$code",
                address = loc.optString("addr", ""),
                latitude = loc.optDouble("lat", 0.0),
                longitude = loc.optDouble("long", 0.0),
                status = status,
                formattedHours = loc.optString("bizHours", "Hours unavailable"),
            ))
        }
    }
    return LocationsResponse(timestamp, locations)
}
