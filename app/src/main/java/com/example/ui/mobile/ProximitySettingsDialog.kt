package com.example.ui.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Earthquake
import com.example.model.UserLocation
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.SeismicRed
import com.example.util.DistanceCalculator

data class CityPreset(
    val name: String,
    val lat: Double,
    val lon: Double,
    val region: String
)

val PRESET_CITIES = listOf(
    CityPreset("San Francisco, CA", 37.7749, -122.4194, "San Andreas Fault"),
    CityPreset("Los Angeles, CA", 34.0522, -118.2437, "SoCal Fault System"),
    CityPreset("Tokyo, Japan", 35.6762, 139.6503, "Japan Trench"),
    CityPreset("Anchorage, AK", 61.2181, -149.9003, "Aleutian Subduction"),
    CityPreset("Santiago, Chile", -33.4489, -70.6693, "Peru-Chile Trench"),
    CityPreset("Rome, Italy", 41.9028, 12.4964, "Apennines Fault"),
    CityPreset("Jakarta, Indonesia", -6.2088, 106.8456, "Sunda Megathrust"),
    CityPreset("Wellington, NZ", -41.2865, 174.7762, "Hikurangi Margin")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProximitySettingsDialog(
    userLocation: UserLocation,
    allQuakes: List<Earthquake>,
    soundAlertsEnabled: Boolean,
    onToggleSoundAlerts: () -> Unit,
    onSaveLocation: (name: String, lat: Double, lon: Double, radiusKm: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var radiusKm by remember { mutableFloatStateOf(userLocation.radiusKm.toFloat()) }
    var locationName by remember { mutableStateOf(userLocation.name) }
    var latitudeStr by remember { mutableStateOf(userLocation.latitude.toString()) }
    var longitudeStr by remember { mutableStateOf(userLocation.longitude.toString()) }

    val lat = latitudeStr.toDoubleOrNull() ?: userLocation.latitude
    val lon = longitudeStr.toDoubleOrNull() ?: userLocation.longitude

    // Live counter of quakes within selected radius
    val detectedQuakesCount = remember(lat, lon, radiusKm, allQuakes) {
        allQuakes.count { q ->
            DistanceCalculator.calculateDistanceKm(lat, lon, q.latitude, q.longitude) <= radiusKm
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = PulseCyan) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Radar, contentDescription = null, tint = PulseCyan, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.proximity_dialog_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.btn_cancel), tint = Color.LightGray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Radius Slider Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2D4A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.label_alert_radius, radiusKm.toInt()),
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.LightGray)
                        )
                        Surface(
                            color = PulseCyan.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${radiusKm.toInt()} km",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = PulseCyan
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = radiusKm,
                        onValueChange = { radiusKm = it },
                        valueRange = 50f..2000f,
                        steps = 38,
                        colors = SliderDefaults.colors(
                            thumbColor = PulseCyan,
                            activeTrackColor = PulseCyan,
                            inactiveTrackColor = Color.DarkGray
                        )
                    )

                    // Quick Step Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(100f, 250f, 500f, 1000f, 1500f).forEach { presetKm ->
                            FilterChip(
                                selected = radiusKm.toInt() == presetKm.toInt(),
                                onClick = { radiusKm = presetKm },
                                label = { Text("${presetKm.toInt()} km", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PulseCyan,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Detection Status Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (detectedQuakesCount > 0) SeismicRed.copy(alpha = 0.18f) else Color(0xFF1E2D4A),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (detectedQuakesCount > 0) SeismicRed.copy(alpha = 0.6f) else Color.Transparent
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = null,
                        tint = if (detectedQuakesCount > 0) SeismicRed else PulseCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (detectedQuakesCount > 0)
                            "$detectedQuakesCount tremor(s) currently within ${radiusKm.toInt()} km of $locationName"
                        else
                            "No tremors within ${radiusKm.toInt()} km of $locationName",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Preset Seismic Cities
            Text(
                text = "Preset Seismic Locations",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PRESET_CITIES) { city ->
                    val isSelected = locationName == city.name
                    Card(
                        modifier = Modifier
                            .clickable {
                                locationName = city.name
                                latitudeStr = city.lat.toString()
                                longitudeStr = city.lon.toString()
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) PulseCyan.copy(alpha = 0.25f) else Color(0xFF1E2D4A)
                        ),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, PulseCyan) else null,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = city.name,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) PulseCyan else Color.White
                                )
                            )
                            Text(
                                text = city.region,
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontSize = 10.sp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Custom Coordinates Inputs
            Text(
                text = "Custom Coordinates",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = locationName,
                onValueChange = { locationName = it },
                label = { Text(stringResource(R.string.label_location_name)) },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = PulseCyan) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PulseCyan,
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = latitudeStr,
                    onValueChange = { latitudeStr = it },
                    label = { Text(stringResource(R.string.label_latitude)) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PulseCyan,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = longitudeStr,
                    onValueChange = { longitudeStr = it },
                    label = { Text(stringResource(R.string.label_longitude)) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PulseCyan,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // TTS Spoken Audio Toggle in dialog
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = PulseCyan)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.enable_audio_narration),
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = stringResource(R.string.enable_audio_narration_desc),
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                        )
                    }
                }
                Switch(
                    checked = soundAlertsEnabled,
                    onCheckedChange = { onToggleSoundAlerts() },
                    colors = SwitchDefaults.colors(checkedThumbColor = PulseCyan, checkedTrackColor = Color(0xFF004F58))
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save & Apply Button
            Button(
                onClick = {
                    val finalLat = latitudeStr.toDoubleOrNull() ?: userLocation.latitude
                    val finalLon = longitudeStr.toDoubleOrNull() ?: userLocation.longitude
                    onSaveLocation(locationName.ifBlank { "Custom Location" }, finalLat, finalLon, radiusKm.toDouble())
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_location_button"),
                colors = ButtonDefaults.buttonColors(containerColor = PulseCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.btn_save_location), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
