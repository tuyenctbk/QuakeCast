package com.example.ui.globe

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DepthCategory
import com.example.model.Earthquake
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DeepDepthColor
import com.example.ui.theme.MidDepthColor
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.SeismicRed
import com.example.ui.theme.ShallowDepthColor
import kotlin.math.*

@Composable
fun Globe3DView(
    earthquakes: List<Earthquake>,
    selectedQuake: Earthquake?,
    onSelectQuake: (Earthquake) -> Unit,
    modifier: Modifier = Modifier,
    isAutoRotate: Boolean = true,
    isTvMode: Boolean = false,
    onToggleAutoRotate: (() -> Unit)? = null
) {
    var rotationYDeg by remember { mutableFloatStateOf(0f) }
    var rotationXDeg by remember { mutableFloatStateOf(15f) }
    var scaleFactor by remember { mutableFloatStateOf(1.0f) }

    // Auto-rotation infinite animation
    val infiniteTransition = rememberInfiniteTransition(label = "globe_rotation")
    val autoRotY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(45000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auto_rot_y"
    )

    // Seismic pulse animation
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    // Center rotation on selected quake if changed
    LaunchedEffect(selectedQuake) {
        selectedQuake?.let { quake ->
            rotationYDeg = (-quake.longitude).toFloat()
            rotationXDeg = (quake.latitude).toFloat().coerceIn(-65f, 65f)
        }
    }

    val currentRotY = if (isAutoRotate) (rotationYDeg + autoRotY) % 360f else rotationYDeg

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val baseRadius = min(size.width, size.height) * 0.38f * scaleFactor

                    // Find closest visible earthquake within tap tolerance
                    val hitTolerance = 32.dp.toPx()
                    var closestQuake: Earthquake? = null
                    var minDistance = Float.MAX_VALUE

                    for (quake in earthquakes) {
                        val p3d = project3D(quake.latitude, quake.longitude, baseRadius, currentRotY, rotationXDeg)
                        if (p3d.z > 0) {
                            val screenPos = Offset(center.x + p3d.x, center.y - p3d.y)
                            val dist = (tapOffset - screenPos).getDistance()
                            if (dist <= hitTolerance && dist < minDistance) {
                                minDistance = dist
                                closestQuake = quake
                            }
                        }
                    }

                    closestQuake?.let { onSelectQuake(it) }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    rotationYDeg = (rotationYDeg + dragAmount.x * 0.45f) % 360f
                    rotationXDeg = (rotationXDeg - dragAmount.y * 0.45f).coerceIn(-75f, 75f)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize().testTag("globe_3d_canvas")) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = min(size.width, size.height) * 0.38f * scaleFactor

            // 1. Space background stars
            drawSpaceBackground(size.width, size.height)

            // 2. 3D Earth Atmosphere Glow Outer Ring
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 0.12f),
                radius = baseRadius * 1.08f,
                center = center
            )
            drawCircle(
                color = Color(0xFF1E88E5).copy(alpha = 0.22f),
                radius = baseRadius * 1.03f,
                center = center
            )

            // 3. Dark Earth Sphere Base
            drawCircle(
                color = Color(0xFF0D172A),
                radius = baseRadius,
                center = center
            )

            // 4. Latitude / Longitude 3D Grid Lines
            draw3DGrid(center, baseRadius, currentRotY, rotationXDeg)

            // 5. Tectonic Plates / Ring of Fire Fault Lines
            draw3DTectonicPlates(center, baseRadius, currentRotY, rotationXDeg)

            // 6. Simplified 3D Coastlines
            draw3DCoastlines(center, baseRadius, currentRotY, rotationXDeg)

            // 7. Earthquakes as 3D Depth-Colored Pulse Rings
            draw3DEarthquakes(
                center = center,
                radius = baseRadius,
                rotY = currentRotY,
                rotX = rotationXDeg,
                earthquakes = earthquakes,
                selectedQuake = selectedQuake,
                pulseProgress = pulseProgress
            )

            // 8. Sphere Shading & Atmosphere Rim
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 0.25f),
                radius = baseRadius,
                center = center,
                style = Stroke(width = 2.5.dp.toPx())
            )
        }

        val bottomSpacing = if (selectedQuake != null && !isTvMode) 105.dp else 16.dp

        // Depth Color Legend Overlay (Bottom Left, Mobile only)
        if (!isTvMode) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, end = 16.dp, bottom = bottomSpacing),
                color = DarkSurface.copy(alpha = 0.90f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = "SEISMIC DEPTH",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = PulseCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendItem(color = ShallowDepthColor, label = "< 30 km (Shallow)")
                        LegendItem(color = MidDepthColor, label = "30-70 km (Mid)")
                        LegendItem(color = DeepDepthColor, label = "> 70 km (Deep)")
                    }
                }
            }
        }

        // Floating Control Buttons (Zoom, Reset, Auto-Rotate)
        if (!isTvMode) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = bottomSpacing),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onToggleAutoRotate != null) {
                    SmallFloatingActionButton(
                        onClick = onToggleAutoRotate,
                        containerColor = if (isAutoRotate) PulseCyan else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isAutoRotate) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Icon(
                            imageVector = if (isAutoRotate) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isAutoRotate) "Pause Rotation" else "Auto Rotate"
                        )
                    }
                }
                SmallFloatingActionButton(
                    onClick = { scaleFactor = (scaleFactor * 1.25f).coerceAtMost(2.5f) },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In")
                }
                SmallFloatingActionButton(
                    onClick = { scaleFactor = (scaleFactor / 1.25f).coerceAtLeast(0.6f) },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
                }
                SmallFloatingActionButton(
                    onClick = {
                        rotationYDeg = 0f
                        rotationXDeg = 15f
                        scaleFactor = 1.0f
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset View")
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = Color.LightGray, fontSize = 10.sp)
        )
    }
}

private fun DrawScope.drawSpaceBackground(width: Float, height: Float) {
    val starCount = 48
    for (i in 0 until starCount) {
        val starX = (sin(i * 137.5f) * 0.5f + 0.5f) * width
        val starY = (cos(i * 73.1f) * 0.5f + 0.5f) * height
        val starAlpha = (sin(i * 11f) * 0.3f + 0.5f).coerceIn(0.15f, 0.85f)
        drawCircle(
            color = Color.White.copy(alpha = starAlpha),
            radius = (i % 3 + 1).toFloat(),
            center = Offset(starX, starY)
        )
    }
}

private fun DrawScope.draw3DGrid(
    center: Offset,
    radius: Float,
    rotYDeg: Float,
    rotXDeg: Float
) {
    val gridColor = Color(0xFF233554).copy(alpha = 0.4f)
    val strokeWidth = 1.dp.toPx()

    // Parallels (Latitudes)
    for (lat in -60..60 step 30) {
        val path = Path()
        var first = true
        for (lon in -180..180 step 10) {
            val pt = project3D(lat.toDouble(), lon.toDouble(), radius, rotYDeg, rotXDeg)
            if (pt.z > 0) {
                val screenPt = Offset(center.x + pt.x, center.y - pt.y)
                if (first) {
                    path.moveTo(screenPt.x, screenPt.y)
                    first = false
                } else {
                    path.lineTo(screenPt.x, screenPt.y)
                }
            } else {
                first = true
            }
        }
        drawPath(path = path, color = gridColor, style = Stroke(width = strokeWidth))
    }

    // Meridians (Longitudes)
    for (lon in -180 until 180 step 30) {
        val path = Path()
        var first = true
        for (lat in -90..90 step 10) {
            val pt = project3D(lat.toDouble(), lon.toDouble(), radius, rotYDeg, rotXDeg)
            if (pt.z > 0) {
                val screenPt = Offset(center.x + pt.x, center.y - pt.y)
                if (first) {
                    path.moveTo(screenPt.x, screenPt.y)
                    first = false
                } else {
                    path.lineTo(screenPt.x, screenPt.y)
                }
            } else {
                first = true
            }
        }
        drawPath(path = path, color = gridColor, style = Stroke(width = strokeWidth))
    }
}

private fun DrawScope.draw3DTectonicPlates(
    center: Offset,
    radius: Float,
    rotYDeg: Float,
    rotXDeg: Float
) {
    // Pacific Ring of Fire & Major Tectonic Plate Boundary fault lines
    val faultLines = listOf(
        // Pacific Ring of Fire (Circum-Pacific Belt)
        listOf(
            Pair(-45.0, 168.0), Pair(-37.0, 178.0), Pair(-18.0, -174.0),
            Pair(-10.0, 160.0), Pair(0.0, 140.0), Pair(15.0, 145.0),
            Pair(35.0, 140.0), Pair(45.0, 150.0), Pair(55.0, 165.0),
            Pair(60.0, -165.0), Pair(55.0, -150.0), Pair(45.0, -130.0),
            Pair(35.0, -121.0), Pair(20.0, -105.0), Pair(0.0, -82.0),
            Pair(-20.0, -71.0), Pair(-40.0, -74.0), Pair(-55.0, -68.0)
        ),
        // Mid-Atlantic Ridge
        listOf(
            Pair(75.0, 5.0), Pair(65.0, -18.0), Pair(45.0, -28.0),
            Pair(25.0, -45.0), Pair(0.0, -25.0), Pair(-25.0, -15.0),
            Pair(-50.0, -5.0)
        ),
        // Alpine-Himalayan Belt
        listOf(
            Pair(36.0, -5.0), Pair(38.0, 15.0), Pair(39.0, 28.0),
            Pair(36.0, 48.0), Pair(32.0, 65.0), Pair(28.0, 85.0),
            Pair(22.0, 95.0), Pair(4.0, 98.0), Pair(-8.0, 115.0)
        )
    )

    val faultStroke = Stroke(
        width = 1.6.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
    )
    val faultColor = Color(0xFFFF9100).copy(alpha = 0.55f)

    for (line in faultLines) {
        val path = Path()
        var first = true
        for (pt in line) {
            val p3d = project3D(pt.first, pt.second, radius, rotYDeg, rotXDeg)
            if (p3d.z > 0) {
                val screenPt = Offset(center.x + p3d.x, center.y - p3d.y)
                if (first) {
                    path.moveTo(screenPt.x, screenPt.y)
                    first = false
                } else {
                    path.lineTo(screenPt.x, screenPt.y)
                }
            } else {
                first = true
            }
        }
        drawPath(path = path, color = faultColor, style = faultStroke)
    }
}

private fun DrawScope.draw3DCoastlines(
    center: Offset,
    radius: Float,
    rotYDeg: Float,
    rotXDeg: Float
) {
    val landColor = Color(0xFF00E5FF).copy(alpha = 0.45f)
    val stroke = Stroke(width = 1.5.dp.toPx())

    val coastPolys = listOf(
        // North America / Pacific West
        listOf(
            Pair(70.0, -165.0), Pair(60.0, -145.0), Pair(50.0, -125.0),
            Pair(35.0, -120.0), Pair(20.0, -105.0), Pair(8.0, -83.0)
        ),
        // South America West
        listOf(
            Pair(8.0, -78.0), Pair(-10.0, -78.0), Pair(-30.0, -72.0),
            Pair(-50.0, -75.0), Pair(-55.0, -67.0)
        ),
        // Asia East / Japan Trench / Kamchatka
        listOf(
            Pair(65.0, 170.0), Pair(55.0, 160.0), Pair(45.0, 142.0),
            Pair(35.0, 139.0), Pair(25.0, 122.0), Pair(10.0, 125.0), Pair(0.0, 115.0)
        ),
        // Australia / Indonesia
        listOf(
            Pair(-10.0, 140.0), Pair(-25.0, 150.0), Pair(-38.0, 145.0),
            Pair(-32.0, 115.0), Pair(-15.0, 125.0)
        ),
        // Europe / Mediterranean
        listOf(
            Pair(60.0, 10.0), Pair(50.0, 0.0), Pair(40.0, -5.0),
            Pair(36.0, 15.0), Pair(38.0, 24.0), Pair(35.0, 33.0)
        ),
        // Africa Outline
        listOf(
            Pair(36.0, 10.0), Pair(10.0, -15.0), Pair(-34.0, 18.0),
            Pair(12.0, 50.0), Pair(30.0, 32.0)
        )
    )

    for (poly in coastPolys) {
        val path = Path()
        var first = true
        for (pt in poly) {
            val p3d = project3D(pt.first, pt.second, radius, rotYDeg, rotXDeg)
            if (p3d.z > 0) {
                val screenPt = Offset(center.x + p3d.x, center.y - p3d.y)
                if (first) {
                    path.moveTo(screenPt.x, screenPt.y)
                    first = false
                } else {
                    path.lineTo(screenPt.x, screenPt.y)
                }
            } else {
                first = true
            }
        }
        drawPath(path = path, color = landColor, style = stroke)
    }
}

private fun DrawScope.draw3DEarthquakes(
    center: Offset,
    radius: Float,
    rotY: Float,
    rotX: Float,
    earthquakes: List<Earthquake>,
    selectedQuake: Earthquake?,
    pulseProgress: Float
) {
    for (quake in earthquakes) {
        val p3d = project3D(quake.latitude, quake.longitude, radius, rotY, rotX)
        if (p3d.z <= 0) continue // Skip back hemisphere

        val screenPos = Offset(center.x + p3d.x, center.y - p3d.y)
        val isSelected = selectedQuake?.id == quake.id

        // Depth color mapping
        val depthColor = when (quake.depthCategory) {
            DepthCategory.SHALLOW -> ShallowDepthColor
            DepthCategory.MID -> MidDepthColor
            DepthCategory.DEEP -> DeepDepthColor
        }

        val baseDotRadius: Float = (quake.magnitude.toFloat() * 2.2f).coerceIn(4f, 18f)

        // Draw Expanding Shockwave Pulse Ring
        val ringMaxRadius: Float = baseDotRadius * 2.8f
        val currentRingRadius: Float = baseDotRadius + (ringMaxRadius - baseDotRadius) * pulseProgress
        val ringAlpha: Float = (1f - pulseProgress).coerceIn(0f, 1f)

        drawCircle(
            color = depthColor.copy(alpha = ringAlpha * 0.8f),
            radius = currentRingRadius,
            center = screenPos,
            style = Stroke(width = 2.dp.toPx())
        )

        // Draw Solid Epicenter Dot
        drawCircle(
            color = depthColor,
            radius = baseDotRadius,
            center = screenPos
        )

        // If selected: draw targeted reticle & crosshair
        if (isSelected) {
            drawCircle(
                color = Color.White,
                radius = baseDotRadius + 8.dp.toPx(),
                center = screenPos,
                style = Stroke(width = 2.dp.toPx())
            )
            val notchLen = 6.dp.toPx()
            drawLine(
                color = Color.White,
                start = Offset(screenPos.x - baseDotRadius - notchLen, screenPos.y),
                end = Offset(screenPos.x - baseDotRadius, screenPos.y),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = Color.White,
                start = Offset(screenPos.x + baseDotRadius, screenPos.y),
                end = Offset(screenPos.x + baseDotRadius + notchLen, screenPos.y),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

private data class Point3D(val x: Float, val y: Float, val z: Float)

/**
 * 3D Orthographic spherical projection formula
 */
private fun project3D(
    latDeg: Double,
    lonDeg: Double,
    radius: Float,
    rotYDeg: Float,
    rotXDeg: Float
): Point3D {
    val lat = Math.toRadians(latDeg)
    val lon = Math.toRadians(lonDeg + rotYDeg.toDouble())
    val rotX = Math.toRadians(rotXDeg.toDouble())

    // Unit sphere coordinates (lon rotated around Y axis)
    val x0 = (radius.toDouble() * cos(lat) * sin(lon)).toFloat()
    val y0 = (radius.toDouble() * sin(lat)).toFloat()
    val z0 = (radius.toDouble() * cos(lat) * cos(lon)).toFloat()

    // Rotate around X axis by rotX
    val x = x0
    val y = (y0.toDouble() * cos(rotX) - z0.toDouble() * sin(rotX)).toFloat()
    val z = (y0.toDouble() * sin(rotX) + z0.toDouble() * cos(rotX)).toFloat()

    return Point3D(x, y, z)
}
