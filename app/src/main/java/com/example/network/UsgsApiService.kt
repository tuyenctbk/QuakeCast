package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path

@JsonClass(generateAdapter = true)
data class GeoJsonProperties(
    @Json(name = "mag") val mag: Double?,
    @Json(name = "place") val place: String?,
    @Json(name = "time") val time: Long?,
    @Json(name = "url") val url: String?,
    @Json(name = "detail") val detail: String?,
    @Json(name = "felt") val felt: Int?,
    @Json(name = "cdi") val cdi: Double?,
    @Json(name = "mmi") val mmi: Double?,
    @Json(name = "alert") val alert: String?,
    @Json(name = "status") val status: String?,
    @Json(name = "tsunami") val tsunami: Int?,
    @Json(name = "sig") val sig: Int?,
    @Json(name = "net") val net: String?,
    @Json(name = "code") val code: String?,
    @Json(name = "title") val title: String?,
    @Json(name = "magType") val magType: String?
)

@JsonClass(generateAdapter = true)
data class GeoJsonGeometry(
    @Json(name = "type") val type: String?,
    @Json(name = "coordinates") val coordinates: List<Double>? // [longitude, latitude, depth]
)

@JsonClass(generateAdapter = true)
data class GeoJsonFeature(
    @Json(name = "id") val id: String?,
    @Json(name = "properties") val properties: GeoJsonProperties?,
    @Json(name = "geometry") val geometry: GeoJsonGeometry?
)

@JsonClass(generateAdapter = true)
data class GeoJsonFeatureCollection(
    @Json(name = "type") val type: String?,
    @Json(name = "features") val features: List<GeoJsonFeature>?
)

interface UsgsApiService {
    @GET("earthquakes/feed/v1.0/summary/{feed}")
    suspend fun getEarthquakes(@Path("feed") feedEndpoint: String): GeoJsonFeatureCollection
}
