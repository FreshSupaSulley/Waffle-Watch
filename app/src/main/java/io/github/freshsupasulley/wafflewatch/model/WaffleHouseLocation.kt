package io.github.freshsupasulley.wafflewatch.model

import org.json.JSONObject

enum class LocationStatus { GREEN, YELLOW, RED }

data class WaffleHouseLocation(
    val locationId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val status: LocationStatus,
    val formattedHours: String,
)

fun parseLocations(json: String): List<WaffleHouseLocation> {
    val root = JSONObject(json)
    val locationsArray = root.getJSONObject("pageProps").getJSONArray("locations")
    return buildList {
        for (i in 0 until locationsArray.length()) {
            val loc = locationsArray.getJSONObject(i)
            val storeCode = loc.optString("storeCode", "")
            val status = when (storeCode.toIntOrNull()?.rem(3)) {
                0 -> LocationStatus.GREEN
                1 -> LocationStatus.YELLOW
                else -> LocationStatus.RED
            }

            val addressLines = loc.optJSONArray("addressLines")
            val firstLine = if (addressLines != null && addressLines.length() > 0) {
                addressLines.getString(0)
            } else ""
            val city = loc.optString("city", "")
            val state = loc.optString("state", "")
            val address = listOf(firstLine, city, state)
                .filter { it.isNotEmpty() }
                .joinToString(", ")

            val formattedHoursArr = loc.optJSONArray("formattedBusinessHours")
            val formattedHours = if (formattedHoursArr != null && formattedHoursArr.length() > 0) {
                formattedHoursArr.getString(0)
            } else "Hours unavailable"

            add(
                WaffleHouseLocation(
                    locationId = storeCode,
                    name = loc.optString("businessName", "Waffle House"),
                    address = address,
                    latitude = loc.optDouble("latitude", 0.0),
                    longitude = loc.optDouble("longitude", 0.0),
                    status = status,
                    formattedHours = formattedHours,
                )
            )
        }
    }
}
