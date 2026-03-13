package io.github.freshsupasulley.wafflewatch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.freshsupasulley.wafflewatch.model.WaffleHouseLocation

@Dao
interface LocationDao {
    @Query("SELECT * FROM locations")
    suspend fun getAll(): List<WaffleHouseLocation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(locations: List<WaffleHouseLocation>)

    @Query("DELETE FROM locations")
    suspend fun deleteAll()
}
