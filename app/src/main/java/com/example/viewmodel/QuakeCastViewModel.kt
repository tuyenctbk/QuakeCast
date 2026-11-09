package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.Earthquake
import com.example.model.FeedTimeframe
import com.example.model.MagFilter
import com.example.model.UserLocation
import com.example.network.EarthquakeRepository
import com.example.util.DistanceCalculator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuakeUiState(
    val quakes: List<Earthquake> = emptyList(),
    val filteredQuakes: List<Earthquake> = emptyList(),
    val significantQuakes: List<Earthquake> = emptyList(), // top 10 M>=4.5
    val timeframe: FeedTimeframe = FeedTimeframe.PAST_DAY,
    val magFilter: MagFilter = MagFilter.ALL,
    val selectedQuake: Earthquake? = null,
    val userLocation: UserLocation = UserLocation("San Francisco, CA", 37.7749, -122.4194, 500.0),
    val proximityAlerts: List<Pair<Earthquake, Double>> = emptyList(), // Quake + DistanceKm
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isTvMode: Boolean = false,
    val tvIndex: Int = 0,
    val isAutoRotate: Boolean = true,
    val errorMessage: String? = null,
    val tsunamiOnly: Boolean = false,
    val soundAlertsEnabled: Boolean = true,
    val isOfflineMode: Boolean = false,
    val lastCacheTime: Long? = null
)

class QuakeCastViewModel(
    private val repository: EarthquakeRepository = EarthquakeRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuakeUiState())
    val uiState: StateFlow<QuakeUiState> = _uiState.asStateFlow()

    private var autoCycleJob: Job? = null

    init {
        loadEarthquakes()
        startAutoRefresh()
    }

    fun initDatabase(context: Context) {
        repository.initDatabase(context)
    }

    fun loadEarthquakes(timeframe: FeedTimeframe = _uiState.value.timeframe) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, timeframe = timeframe, errorMessage = null) }
            val result = repository.fetchEarthquakesWithCacheStatus(timeframe)
            val quakes = result.quakes

            _uiState.update { state ->
                val filtered = applyFilters(quakes, state.magFilter, state.searchQuery, state.tsunamiOnly)
                val sigs = quakes.filter { it.magnitude >= 4.5 }.take(10)
                val significantList = if (sigs.isNotEmpty()) sigs else quakes.take(10)
                val selected = state.selectedQuake ?: significantList.firstOrNull() ?: quakes.firstOrNull()
                val tvIdx = if (selected != null) significantList.indexOfFirst { it.id == selected.id }.coerceAtLeast(0) else 0
                val alerts = calculateProximityAlerts(quakes, state.userLocation)

                state.copy(
                    quakes = quakes,
                    filteredQuakes = filtered,
                    significantQuakes = significantList,
                    selectedQuake = selected,
                    tvIndex = tvIdx,
                    proximityAlerts = alerts,
                    isLoading = false,
                    isOfflineMode = result.isFromCache,
                    lastCacheTime = result.cacheTimestamp
                )
            }
        }
    }

    fun setMagFilter(filter: MagFilter) {
        _uiState.update { state ->
            val filtered = applyFilters(state.quakes, filter, state.searchQuery, state.tsunamiOnly)
            state.copy(magFilter = filter, filteredQuakes = filtered)
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            val filtered = applyFilters(state.quakes, state.magFilter, query, state.tsunamiOnly)
            state.copy(searchQuery = query, filteredQuakes = filtered)
        }
    }

    fun setTsunamiOnly(tsunamiOnly: Boolean) {
        _uiState.update { state ->
            val filtered = applyFilters(state.quakes, state.magFilter, state.searchQuery, tsunamiOnly)
            state.copy(tsunamiOnly = tsunamiOnly, filteredQuakes = filtered)
        }
    }

    fun selectQuake(quake: Earthquake?) {
        _uiState.update { state ->
            val tvIdx = state.significantQuakes.indexOfFirst { it.id == quake?.id }
            state.copy(
                selectedQuake = quake,
                tvIndex = if (tvIdx >= 0) tvIdx else state.tvIndex
            )
        }
    }

    fun setTvMode(enabled: Boolean) {
        _uiState.update { it.copy(isTvMode = enabled, isAutoRotate = enabled) }
        if (enabled) {
            startTvAutoCycle()
        } else {
            autoCycleJob?.cancel()
        }
    }

    fun navigateTvNext() {
        _uiState.update { state ->
            if (state.significantQuakes.isEmpty()) return@update state
            val nextIdx = (state.tvIndex + 1) % state.significantQuakes.size
            val nextQuake = state.significantQuakes[nextIdx]
            state.copy(tvIndex = nextIdx, selectedQuake = nextQuake)
        }
    }

    fun navigateTvPrevious() {
        _uiState.update { state ->
            if (state.significantQuakes.isEmpty()) return@update state
            val prevIdx = if (state.tvIndex - 1 < 0) state.significantQuakes.size - 1 else state.tvIndex - 1
            val prevQuake = state.significantQuakes[prevIdx]
            state.copy(tvIndex = prevIdx, selectedQuake = prevQuake)
        }
    }

    fun updateUserLocation(name: String, lat: Double, lon: Double, radiusKm: Double) {
        val newLoc = UserLocation(name, lat, lon, radiusKm)
        _uiState.update { state ->
            val alerts = calculateProximityAlerts(state.quakes, newLoc)
            state.copy(userLocation = newLoc, proximityAlerts = alerts)
        }
    }

    fun toggleAutoRotate() {
        _uiState.update { it.copy(isAutoRotate = !it.isAutoRotate) }
    }

    fun toggleSoundAlerts() {
        _uiState.update { it.copy(soundAlertsEnabled = !it.soundAlertsEnabled) }
    }

    private fun startTvAutoCycle() {
        autoCycleJob?.cancel()
        autoCycleJob = viewModelScope.launch {
            while (true) {
                delay(8000)
                if (_uiState.value.isTvMode && _uiState.value.isAutoRotate) {
                    navigateTvNext()
                }
            }
        }
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(60_000) // Auto refresh every 60 seconds
                loadEarthquakes(_uiState.value.timeframe)
            }
        }
    }

    private fun applyFilters(
        quakes: List<Earthquake>,
        magFilter: MagFilter,
        query: String,
        tsunamiOnly: Boolean
    ): List<Earthquake> {
        return quakes.filter { q ->
            q.magnitude >= magFilter.minMag &&
            (!tsunamiOnly || q.hasTsunamiAlert) &&
            (query.isBlank() || q.place.contains(query, ignoreCase = true) || q.title.contains(query, ignoreCase = true))
        }
    }

    private fun calculateProximityAlerts(
        quakes: List<Earthquake>,
        userLoc: UserLocation
    ): List<Pair<Earthquake, Double>> {
        val alerts = mutableListOf<Pair<Earthquake, Double>>()
        for (q in quakes) {
            val dist = DistanceCalculator.calculateDistanceKm(
                userLoc.latitude, userLoc.longitude,
                q.latitude, q.longitude
            )
            if (dist <= userLoc.radiusKm) {
                alerts.add(Pair(q, dist))
            }
        }
        return alerts.sortedBy { it.second }
    }
}
