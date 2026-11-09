package com.example.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EarthquakeDao {
    @Query("SELECT * FROM cached_earthquakes ORDER BY time DESC")
    fun getAllEarthquakesFlow(): Flow<List<EarthquakeEntity>>

    @Query("SELECT * FROM cached_earthquakes WHERE cachedTimeframe = :timeframe ORDER BY time DESC")
    suspend fun getEarthquakesByTimeframe(timeframe: String): List<EarthquakeEntity>

    @Query("SELECT * FROM cached_earthquakes ORDER BY time DESC")
    suspend fun getAllEarthquakes(): List<EarthquakeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEarthquakes(quakes: List<EarthquakeEntity>)

    @Query("DELETE FROM cached_earthquakes WHERE cachedTimeframe = :timeframe")
    suspend fun clearTimeframe(timeframe: String)

    @Query("DELETE FROM cached_earthquakes")
    suspend fun clearAll()
}
