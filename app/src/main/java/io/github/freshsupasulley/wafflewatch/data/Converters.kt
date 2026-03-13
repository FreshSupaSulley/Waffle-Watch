package io.github.freshsupasulley.wafflewatch.data

import androidx.room.TypeConverter
import io.github.freshsupasulley.wafflewatch.model.LocationStatus

class Converters {
    @TypeConverter
    fun fromLocationStatus(status: LocationStatus): String = status.name

    @TypeConverter
    fun toLocationStatus(value: String): LocationStatus = LocationStatus.valueOf(value)
}
