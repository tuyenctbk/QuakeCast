package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Earthquake

@Entity(tableName = "cached_earthquakes")
data class EarthquakeEntity(
    @PrimaryKey val id: String,
    val magnitude: Double,
    val place: String,
    val time: Long,
    val url: String,
    val detailUrl: String,
    val felt: Int?,
    val cdi: Double?,
    val mmi: Double?,
    val alert: String?,
    val status: String,
    val tsunami: Int,
    val sig: Int,
    val net: String,
    val code: String,
    val latitude: Double,
    val longitude: Double,
    val depth: Double,
    val title: String,
    val magType: String,
    val cachedTimeframe: String,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): Earthquake {
        return Earthquake(
            id = id,
            magnitude = magnitude,
            place = place,
            time = time,
            url = url,
            detailUrl = detailUrl,
            felt = felt,
            cdi = cdi,
            mmi = mmi,
            alert = alert,
            status = status,
            tsunami = tsunami,
            sig = sig,
            net = net,
            code = code,
            latitude = latitude,
            longitude = longitude,
            depth = depth,
            title = title,
            magType = magType
        )
    }

    companion object {
        fun fromDomainModel(quake: Earthquake, timeframe: String): EarthquakeEntity {
            return EarthquakeEntity(
                id = quake.id,
                magnitude = quake.magnitude,
                place = quake.place,
                time = quake.time,
                url = quake.url,
                detailUrl = quake.detailUrl,
                felt = quake.felt,
                cdi = quake.cdi,
                mmi = quake.mmi,
                alert = quake.alert,
                status = quake.status,
                tsunami = quake.tsunami,
                sig = quake.sig,
                net = quake.net,
                code = quake.code,
                latitude = quake.latitude,
                longitude = quake.longitude,
                depth = quake.depth,
                title = quake.title,
                magType = quake.magType,
                cachedTimeframe = timeframe
            )
        }
    }
}
