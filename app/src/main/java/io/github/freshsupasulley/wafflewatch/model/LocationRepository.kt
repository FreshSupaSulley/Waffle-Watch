package io.github.freshsupasulley.wafflewatch.model

import android.content.Context
import io.github.freshsupasulley.wafflewatch.BuildConfig
import io.github.freshsupasulley.wafflewatch.data.AppDatabase
import io.github.freshsupasulley.wafflewatch.data.LocationDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

interface LocationRepository {
    suspend fun fetchLocations(): LocationsResponse
    suspend fun getCachedLocations(): List<WaffleHouseLocation>
    suspend fun getCachedTimestamp(): Long?
    suspend fun clearCache()

    companion object {
        fun create(context: Context): LocationRepository {
            val dao = AppDatabase.getInstance(context).locationDao()
            val prefs = context.getSharedPreferences("waffle_watch_prefs", Context.MODE_PRIVATE)
            return CachingLocationRepository(
                remote = RemoteLocationRepository(BuildConfig.LOCATIONS_SERVER_URL),
                dao = dao,
                prefs = prefs,
            )
        }
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

    override suspend fun getCachedLocations(): List<WaffleHouseLocation> = emptyList()
    override suspend fun getCachedTimestamp(): Long? = null
    override suspend fun clearCache() {}
}

class CachingLocationRepository(
    private val remote: RemoteLocationRepository,
    private val dao: LocationDao,
    private val prefs: android.content.SharedPreferences,
) : LocationRepository {

    override suspend fun fetchLocations(): LocationsResponse {
        val response = remote.fetchLocations()
        withContext(Dispatchers.IO) {
            dao.deleteAll()
            dao.insertAll(response.locations)
            prefs.edit().putLong(KEY_TIMESTAMP, response.timestamp).apply()
        }
        return response
    }

    override suspend fun getCachedLocations(): List<WaffleHouseLocation> {
        return withContext(Dispatchers.IO) { dao.getAll() }
    }

    override suspend fun getCachedTimestamp(): Long? {
        val ts = prefs.getLong(KEY_TIMESTAMP, -1L)
        return if (ts == -1L) null else ts
    }

    override suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            dao.deleteAll()
            prefs.edit().remove(KEY_TIMESTAMP).apply()
        }
    }

    companion object {
        private const val KEY_TIMESTAMP = "cached_timestamp"
    }
}
