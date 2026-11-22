package com.example.network

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.db.AppDatabase
import com.example.db.EarthquakeDao
import com.example.db.EarthquakeEntity
import com.example.model.Earthquake
import com.example.model.FeedTimeframe
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

data class FetchResult(
    val quakes: List<Earthquake>,
    val isFromCache: Boolean,
    val cacheTimestamp: Long? = null
)

class EarthquakeRepository(context: Context? = null) {

    private var earthquakeDao: EarthquakeDao? = context?.let {
        AppDatabase.getInstance(it.applicationContext).earthquakeDao()
    }

    fun initDatabase(context: Context) {
        if (earthquakeDao == null) {
            earthquakeDao = AppDatabase.getInstance(context.applicationContext).earthquakeDao()
        }
    }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        })
        .build()

    private val apiService: UsgsApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://earthquake.usgs.gov/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(UsgsApiService::class.java)
    }

    suspend fun fetchEarthquakesWithCacheStatus(timeframe: FeedTimeframe): FetchResult {
        return try {
            val response = apiService.getEarthquakes(timeframe.endpoint)
            val quakes = response.features?.mapNotNull { feature ->
                val props = feature.properties ?: return@mapNotNull null
                val geom = feature.geometry ?: return@mapNotNull null
                val coords = geom.coordinates ?: return@mapNotNull null

                if (coords.size < 3) return@mapNotNull null

                val lon = coords[0]
                val lat = coords[1]
                val depth = coords[2]

                Earthquake(
                    id = feature.id ?: "usgs_${System.currentTimeMillis()}_${(0..999).random()}",
                    magnitude = props.mag ?: 0.0,
                    place = props.place ?: "Unknown Location",
                    time = props.time ?: System.currentTimeMillis(),
                    url = props.url ?: "https://earthquake.usgs.gov",
                    detailUrl = props.detail ?: "",
                    felt = props.felt,
                    cdi = props.cdi,
                    mmi = props.mmi,
                    alert = props.alert,
                    status = props.status ?: "reviewed",
                    tsunami = props.tsunami ?: 0,
                    sig = props.sig ?: 0,
                    net = props.net ?: "us",
                    code = props.code ?: "",
                    latitude = lat,
                    longitude = lon,
                    depth = depth,
                    title = props.title ?: "M ${props.mag} - ${props.place}",
                    magType = props.magType ?: "mw"
                )
            }?.sortedByDescending { it.time } ?: emptyList()

            if (quakes.isNotEmpty()) {
                // Save to Room Database Cache asynchronously
                try {
                    earthquakeDao?.let { dao ->
                        val entities = quakes.map { EarthquakeEntity.fromDomainModel(it, timeframe.name) }
                        dao.insertEarthquakes(entities)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("EarthquakeRepository", "Failed to cache earthquakes in Room", e)
                }
                FetchResult(quakes = quakes, isFromCache = false)
            } else {
                loadFromCache(timeframe)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("EarthquakeRepository", "Network fetch failed, falling back to cache", e)
            // Network failure or offline -> fallback to Room DB local cache
            loadFromCache(timeframe)
        }
    }

    suspend fun fetchEarthquakes(timeframe: FeedTimeframe): Result<List<Earthquake>> {
        val result = fetchEarthquakesWithCacheStatus(timeframe)
        return Result.success(result.quakes)
    }

    private suspend fun loadFromCache(timeframe: FeedTimeframe): FetchResult {
        val cachedEntities = try {
            earthquakeDao?.getEarthquakesByTimeframe(timeframe.name)
                ?.ifEmpty { earthquakeDao?.getAllEarthquakes() }
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        return if (cachedEntities.isNotEmpty()) {
            val quakes = cachedEntities.map { it.toDomainModel() }
            val lastUpdated = cachedEntities.maxOfOrNull { it.lastUpdated }
            FetchResult(quakes = quakes, isFromCache = true, cacheTimestamp = lastUpdated)
        } else {
            FetchResult(quakes = emptyList(), isFromCache = false)
        }
    }
}
