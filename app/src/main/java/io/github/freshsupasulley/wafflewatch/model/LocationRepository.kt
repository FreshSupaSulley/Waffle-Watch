package io.github.freshsupasulley.wafflewatch.model

import io.github.freshsupasulley.wafflewatch.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

interface LocationRepository {
    suspend fun fetchLocations(): List<WaffleHouseLocation>

    companion object {
        fun create(): LocationRepository = RemoteLocationRepository(BuildConfig.LOCATIONS_SERVER_URL)
    }
}

class RemoteLocationRepository(private val serverUrl: String) : LocationRepository {
    override suspend fun fetchLocations() = withContext(Dispatchers.IO) {
        val conn = URL(serverUrl).openConnection() as HttpURLConnection
        try {
            conn.apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 15_000
                connect()
            }
            parseLocations(conn.inputStream.bufferedReader().readText())
        } finally {
            conn.disconnect()
        }
    }
}
