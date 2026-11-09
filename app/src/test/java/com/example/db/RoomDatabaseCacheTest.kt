package com.example.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.model.Earthquake
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomDatabaseCacheTest {

    private lateinit var database: AppDatabase
    private lateinit var earthquakeDao: EarthquakeDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        earthquakeDao = database.earthquakeDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndQueryCachedEarthquake() = runBlocking {
        val sampleQuake = Earthquake(
            id = "test_quake_001",
            magnitude = 6.4,
            place = "50 km N of Tokyo, Japan",
            time = System.currentTimeMillis(),
            url = "https://earthquake.usgs.gov",
            detailUrl = "",
            felt = 500,
            cdi = 5.8,
            mmi = 6.2,
            alert = "yellow",
            status = "reviewed",
            tsunami = 1,
            sig = 650,
            net = "us",
            code = "test001",
            latitude = 35.6762,
            longitude = 139.6503,
            depth = 35.0,
            title = "M 6.4 - Tokyo, Japan",
            magType = "mww"
        )

        val entity = EarthquakeEntity.fromDomainModel(sampleQuake, "PAST_DAY")
        earthquakeDao.insertEarthquakes(listOf(entity))

        val cached = earthquakeDao.getEarthquakesByTimeframe("PAST_DAY")
        assertEquals(1, cached.size)

        val domain = cached.first().toDomainModel()
        assertEquals("test_quake_001", domain.id)
        assertEquals(6.4, domain.magnitude, 0.01)
        assertEquals("50 km N of Tokyo, Japan", domain.place)
        assertEquals(35.0, domain.depth, 0.01)
        assertEquals(true, domain.hasTsunamiAlert)
    }

    @Test
    fun clearCacheByTimeframe() = runBlocking {
        val quake1 = Earthquake(
            id = "q1",
            magnitude = 4.2,
            place = "San Francisco, CA",
            time = System.currentTimeMillis(),
            url = "",
            detailUrl = "",
            latitude = 37.77,
            longitude = -122.41,
            depth = 10.0,
            title = "M 4.2 - San Francisco"
        )

        earthquakeDao.insertEarthquakes(listOf(EarthquakeEntity.fromDomainModel(quake1, "PAST_HOUR")))
        assertEquals(1, earthquakeDao.getEarthquakesByTimeframe("PAST_HOUR").size)

        earthquakeDao.clearTimeframe("PAST_HOUR")
        assertEquals(0, earthquakeDao.getEarthquakesByTimeframe("PAST_HOUR").size)
    }
}
