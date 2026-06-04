package com.example.ui.effects

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.withFrameMillis
import kotlinx.coroutines.isActive
import kotlin.math.sin
import kotlin.math.cos
import kotlin.random.Random

enum class WeatherVisualType {
    SUNNY, CLOUDY, FOGGY, RAINY, SNOWY, THUNDERSTORM
}

// Low-overhead particle system to prevent allocations at 60+ FPS
private class RainDrop(
    var x: Float,
    var y: Float,
    var length: Float,
    var speed: Float,
    var alpha: Float,
    var width: Float,
    var isForeground: Boolean
) {
    fun update(height: Float, widthPx: Float, windSlant: Float) {
        y += speed
        x += windSlant * (speed * 0.1f)
        if (y > height || x < -50f || x > widthPx + 50f) {
            y = -length - Random.nextFloat() * 100f
            x = Random.nextFloat() * widthPx
        }
    }
}

private class Snowflake(
    var x: Float,
    var y: Float,
    var radius: Float,
    var speed: Float,
    var angle: Float,
    var wanderSpeed: Float,
    var alpha: Float,
    var sparklePhase: Float,
    var sparkleSpeed: Float
) {
    fun update(height: Float, widthPx: Float, timeSec: Float) {
        y += speed
        angle += wanderSpeed
        x += sin(angle) * 0.8f
        sparklePhase += sparkleSpeed
        if (y > height) {
            y = -radius - 10f
            x = Random.nextFloat() * widthPx
        }
    }
}

private class CloudLayer(
    var x: Float,
    val y: Float,
    val scale: Float,
    val speed: Float,
    val alpha: Float,
    val baseRadius: Float
) {
    class CloudPuff(
        val relX: Float,
        val relY: Float,
        val radiusRatio: Float,
        val anglePhase: Float,
        val phaseSpeed: Float
    )

    // Detached, carefully organic coordinates for 9 sub-puffs
    val puffs = listOf(
        CloudPuff(0f, 0f, 1.0f, Random.nextFloat() * 6.282f, 0.45f),
        CloudPuff(-baseRadius * 0.65f, -baseRadius * 0.04f, 0.82f, Random.nextFloat() * 6.282f, 0.58f),
        CloudPuff(baseRadius * 0.65f, -baseRadius * 0.04f, 0.82f, Random.nextFloat() * 6.282f, 0.52f),
        CloudPuff(-baseRadius * 1.25f, baseRadius * 0.08f, 0.62f, Random.nextFloat() * 6.282f, 0.75f),
        CloudPuff(baseRadius * 1.25f, baseRadius * 0.08f, 0.62f, Random.nextFloat() * 6.282f, 0.68f),
        CloudPuff(-baseRadius * 1.70f, baseRadius * 0.22f, 0.45f, Random.nextFloat() * 6.282f, 0.92f),
        CloudPuff(baseRadius * 1.70f, baseRadius * 0.22f, 0.45f, Random.nextFloat() * 6.282f, 0.82f),
        CloudPuff(-baseRadius * 0.35f, -baseRadius * 0.32f, 0.70f, Random.nextFloat() * 6.282f, 0.38f),
        CloudPuff(baseRadius * 0.35f, -baseRadius * 0.32f, 0.70f, Random.nextFloat() * 6.282f, 0.41f)
    )

    var time = Random.nextFloat() * 1000f

    fun update(widthPx: Float) {
        x += speed
        time += 0.016f // step time-phase forward
        val bound = baseRadius * 3.8f * scale
        if (x > widthPx + bound) {
            x = -bound
        }
    }
}

private class SunDust(
    var x: Float,
    var y: Float,
    var radius: Float,
    var speedY: Float,
    var speedX: Float,
    var alpha: Float,
    var angleOffset: Float,
    var wanderPhase: Float
) {
    fun update(height: Float, widthPx: Float) {
        y -= speedY
        wanderPhase += 0.015f
        x += speedX + sin(wanderPhase) * 0.3f
        if (y < -20f) {
            y = height + 20f
            x = Random.nextFloat() * widthPx
        }
        if (x < -20f) x = widthPx + 10f
        if (x > widthPx + 20f) x = -10f
    }
}

private class GlassRipple(
    var x: Float,
    var y: Float,
    var radius: Float,
    var maxRadius: Float,
    var thickness: Float,
    var alpha: Float,
    var speed: Float,
    var active: Boolean
) {
    fun update() {
        if (!active) return
        radius += speed
        if (radius >= maxRadius) {
            active = false
        } else {
            alpha = (1f - (radius / maxRadius)).coerceIn(0f, 1f) * 0.35f
        }
    }

    fun spawn(newX: Float, newY: Float) {
        x = newX
        y = newY
        radius = 1f
        maxRadius = 25f + Random.nextFloat() * 35f
        thickness = 1f + Random.nextFloat() * 1.5f
        speed = 1.2f + Random.nextFloat() * 1.5f
        alpha = 0.35f
        active = true
    }
}

@Composable
fun WeatherBackground(
    weatherType: WeatherVisualType,
    isDay: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Cinematic, realistic high-fidelity sky base gradients
    val startColor = if (isDay) {
        when (weatherType) {
            WeatherVisualType.SUNNY -> Color(0xFF0F2B48)       // Realistic atmospheric deep upper-air sky blue
            WeatherVisualType.CLOUDY -> Color(0xFF3A4454)      // Soft, neutral slate blue-cloud atmosphere
            WeatherVisualType.FOGGY -> Color(0xFF424A54)       // Overcast dense mist dome
            WeatherVisualType.RAINY -> Color(0xFF181C25)       // Saturated storm blue-gray
            WeatherVisualType.SNOWY -> Color(0xFF050E20)       // Deep subzero polar dusk sky
            WeatherVisualType.THUNDERSTORM -> Color(0xFF090B10) // Darkest atmospheric electric indigo core
        }
    } else {
        when (weatherType) {
            WeatherVisualType.SUNNY -> Color(0xFF030712)       // Deep cosmic space black
            WeatherVisualType.CLOUDY -> Color(0xFF080D1A)      // Indigo-black nocturnal cloudscape
            WeatherVisualType.FOGGY -> Color(0xFF111622)       // Overcast night mist
            WeatherVisualType.RAINY -> Color(0xFF04060B)       // Wet obsidian storm night
            WeatherVisualType.SNOWY -> Color(0xFF010515)       // Polar subzero night
            WeatherVisualType.THUNDERSTORM -> Color(0xFF070410) // Nocturnal thunderstorm backdrop
        }
    }

    val endColor = if (isDay) {
        when (weatherType) {
            WeatherVisualType.SUNNY -> Color(0xFFF9A8D4)       // Warm Horizon Ray scattering sky gradient
            WeatherVisualType.CLOUDY -> Color(0xFF7E8B9B)      // Softly scattered cloudbase horizon-light
            WeatherVisualType.FOGGY -> Color(0xFF8E9AA6)       // Misty ground fog diffusion gray
            WeatherVisualType.RAINY -> Color(0xFF2E3846)       // Rain-curtained wet asphalt grey
            WeatherVisualType.SNOWY -> Color(0xFF39516E)       // Glistening snow horizon scattering
            WeatherVisualType.THUNDERSTORM -> Color(0xFF1E1428) // Electric purple cloud discharge horizon
        }
    } else {
        when (weatherType) {
            WeatherVisualType.SUNNY -> Color(0xFF0F172A)       // Rich deep slate night bottom
            WeatherVisualType.CLOUDY -> Color(0xFF1E293B)      // Cloudy night floor
            WeatherVisualType.FOGGY -> Color(0xFF2D3748)       // Misty ground glow
            WeatherVisualType.RAINY -> Color(0xFF111827)       // Rain-curtained night asphalt
            WeatherVisualType.SNOWY -> Color(0xFF1E3A8A)       // Polar blue horizon glow
            WeatherVisualType.THUNDERSTORM -> Color(0xFF1F1235) // Deep electric magenta-indigo bottom
        }
    }

    val animatedStartColor by animateColorAsState(targetValue = startColor, animationSpec = tween(1500), label = "AtmosphereStart")
    val animatedEndColor by animateColorAsState(targetValue = endColor, animationSpec = tween(1500), label = "AtmosphereEnd")

    val gradientBrush = remember(animatedStartColor, animatedEndColor) {
        Brush.verticalGradient(
            colors = listOf(animatedStartColor, animatedEndColor)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBrush)
    ) {
        when (weatherType) {
            WeatherVisualType.SUNNY -> {
                if (isDay) SunnyVisuals() else NightVisuals()
            }
            WeatherVisualType.CLOUDY -> CloudyVisuals(isDay = isDay)
            WeatherVisualType.FOGGY -> FoggyVisuals()
            WeatherVisualType.RAINY -> RainyVisuals()
            WeatherVisualType.SNOWY -> SnowyVisuals()
            WeatherVisualType.THUNDERSTORM -> ThunderstormVisuals()
        }
    }
}

@Composable
fun SunnyVisuals() {
    val infiniteTransition = rememberInfiniteTransition(label = "SunnyLoop")

    // Slow ambient rotation for realistic sun flare diffraction
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(120000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "DiffractionRotation"
    )

    // Gentle sun hovering to create a cinematic handheld camera organic movement
    val translationPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CameraHover"
    )

    // Soft core corona breath (less than 5% amplitude for high realism)
    val coronaScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SunCoronaBreath"
    )

    // Setup sparkling gold ambient dust specks floating through sunlight rays
    val dustParticles = remember {
        mutableStateListOf<SunDust>().apply {
            repeat(40) {
                add(
                    SunDust(
                        x = Random.nextFloat() * 1080f,
                        y = Random.nextFloat() * 1920f,
                        radius = 2f + Random.nextFloat() * 5f,
                        speedY = 0.3f + Random.nextFloat() * 0.7f,
                        speedX = -0.2f + Random.nextFloat() * 0.4f,
                        alpha = 0.1f + Random.nextFloat() * 0.35f,
                        angleOffset = Random.nextFloat() * 360f,
                        wanderPhase = Random.nextFloat() * 100f
                    )
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameMillis {
                dustParticles.forEach { it.update(2400f, 1200f) }
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Calculate slow organic camera drift offsets
        val floatOffsetSunX = sin(translationPhase).toFloat() * 15f
        val floatOffsetSunY = cos(translationPhase).toFloat() * 10f

        // Let's place the legendary sun at 82% Width and 18% Height of the canvas
        val centerX = w * 0.82f + floatOffsetSunX
        val centerY = h * 0.18f + floatOffsetSunY
        val baseRadius = 75.dp.toPx() * coronaScale

        // ——————————————————————————————————————————————————————————
        // 1. ATMOSPHERIC SCATTERING / PRIMARY SOLAR GLARE
        // ——————————————————————————————————————————————————————————
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFF7ED).copy(alpha = 0.40f),
                    Color(0xFFFEF08A).copy(alpha = 0.18f),
                    Color(0xFFF97316).copy(alpha = 0.05f),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY),
                radius = baseRadius * 5.5f
            ),
            radius = baseRadius * 5.5f,
            center = Offset(centerX, centerY)
        )

        // ——————————————————————————————————————————————————————————
        // 2. DELICATE ANAMORPHIC DIFFRACTION RAYS (Extremely elegant & thin)
        // ——————————————————————————————————————————————————————————
        withTransform({
            rotate(rotation, Offset(centerX, centerY))
        }) {
            val rayCount = 16
            for (i in 0 until rayCount) {
                val angle = (360f / rayCount) * i
                val rayLength = baseRadius * (2.2f + 0.35f * sin((i * 4.5f + translationPhase).toDouble()).toFloat())
                val angleRad = Math.toRadians(angle.toDouble())

                // Very thin golden ambient soft volumetric blades
                val cornerHalfWidth = baseRadius * 0.04f
                val perpAngle1 = Math.toRadians(angle - 90.0)
                val perpAngle2 = Math.toRadians(angle + 90.0)

                val path = Path().apply {
                    moveTo(
                        centerX + (cornerHalfWidth * cos(perpAngle1)).toFloat(),
                        centerY + (cornerHalfWidth * sin(perpAngle1)).toFloat()
                    )
                    lineTo(
                        centerX + (rayLength * cos(angleRad)).toFloat(),
                        centerY + (rayLength * sin(angleRad)).toFloat()
                    )
                    lineTo(
                        centerX + (cornerHalfWidth * cos(perpAngle2)).toFloat(),
                        centerY + (cornerHalfWidth * sin(perpAngle2)).toFloat()
                    )
                    close()
                }

                drawPath(
                    path = path,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFEF3C7).copy(alpha = 0.12f),
                            Color(0xFFFBBF24).copy(alpha = 0.02f),
                            Color.Transparent
                        ),
                        center = Offset(centerX, centerY),
                        radius = rayLength
                    )
                )
            }
        }

        // ——————————————————————————————————————————————————————————
        // 3. SOLAR DISC CORE (Brilliant pure light core)
        // ——————————————————————————————————————————————————————————
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White,
                    Color(0xFFFFFBEB),
                    Color(0xFFFEF08A).copy(alpha = 0.95f),
                    Color(0xFFF59E0B).copy(alpha = 0.50f),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY),
                radius = baseRadius * 1.2f
            ),
            radius = baseRadius * 1.2f,
            center = Offset(centerX, centerY)
        )

        // ——————————————————————————————————————————————————————————
        // 4. CINEMATIC LENS FLARES (Traced along vector to bottom-left)
        // ——————————————————————————————————————————————————————————
        // Draw flares aligned towards the opposite screen corner to simulate beautiful glass reflections
        val targetX = w * 0.15f
        val targetY = h * 0.85f
        val vectorX = targetX - centerX
        val vectorY = targetY - centerY

        // Flare 1: Delicate coral-red wide ambient ring
        val fx1X = centerX + vectorX * 0.22f
        val fx1Y = centerY + vectorY * 0.22f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFF43F5E).copy(alpha = 0.06f),
                    Color(0xFFFDA4AF).copy(alpha = 0.03f),
                    Color.Transparent
                ),
                center = Offset(fx1X, fx1Y),
                radius = baseRadius * 1.6f
            ),
            radius = baseRadius * 1.6f,
            center = Offset(fx1X, fx1Y)
        )

        // Flare 2: Brilliant tiny warm amber bead
        val fx2X = centerX + vectorX * 0.42f
        val fx2Y = centerY + vectorY * 0.42f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFEF08A).copy(alpha = 0.18f),
                    Color(0xFFF59E0B).copy(alpha = 0.06f),
                    Color.Transparent
                ),
                center = Offset(fx2X, fx2Y),
                radius = baseRadius * 0.22f
            ),
            radius = baseRadius * 0.22f,
            center = Offset(fx2X, fx2Y)
        )

        // Flare 3: Wide cinematic rainbow rainbow-magenta prism glow
        val fx3X = centerX + vectorX * 0.62f
        val fx3Y = centerY + vectorY * 0.62f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF818CF8).copy(alpha = 0.04f),
                    Color(0xFFC084FC).copy(alpha = 0.03f),
                    Color(0xFFF472B6).copy(alpha = 0.02f),
                    Color.Transparent
                ),
                center = Offset(fx3X, fx3Y),
                radius = baseRadius * 2.5f
            ),
            radius = baseRadius * 2.5f,
            center = Offset(fx3X, fx3Y)
        )

        // Flare 4: Cool green-blue soft dispersion disk
        val fx4X = centerX + vectorX * 0.84f
        val fx4Y = centerY + vectorY * 0.84f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF14B8A6).copy(alpha = 0.05f),
                    Color(0xFF0EA5E9).copy(alpha = 0.03f),
                    Color.Transparent
                ),
                center = Offset(fx4X, fx4Y),
                radius = baseRadius * 1.1f
            ),
            radius = baseRadius * 1.1f,
            center = Offset(fx4X, fx4Y)
        )

        // ——————————————————————————————————————————————————————————
        // 5. FLOATING SUNLIGHT GOLD COMPASS DUST RAYS
        // ——————————————————————————————————————————————————————————
        dustParticles.forEach { dust ->
            // Scale and twinkle factor based on coordinates
            val twinkle = (0.35f + 0.65f * sin((dust.y * 0.01f + dust.wanderPhase)).toFloat()).coerceIn(0.1f, 1.0f)
            val currentAlpha = dust.alpha * twinkle * (dust.y / h).coerceIn(0.2f, 1.0f) // fade near top edge
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFEF3C7).copy(alpha = currentAlpha),
                        Color(0xFFF59E0B).copy(alpha = currentAlpha * 0.4f),
                        Color.Transparent
                    ),
                    center = Offset(dust.x, dust.y),
                    radius = dust.radius * 2.0f
                ),
                radius = dust.radius * 2.0f,
                center = Offset(dust.x, dust.y)
            )
        }
    }
}

@Composable
fun CloudyVisuals(isDay: Boolean = true) {
    val infiniteTransition = rememberInfiniteTransition(label = "CloudyLoop")

    // Slow orbital camera parallax simulation
    val parallaxOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CloudParallax"
    )

    // Parallel parallax layers with customized sizes and parameters
    val clouds = remember {
        mutableStateListOf(
            CloudLayer(100f, 260f, 0.75f, 0.15f, 0.40f, 160f),  // High thin backing layer
            CloudLayer(700f, 340f, 1.15f, 0.22f, 0.65f, 180f),  // Main volume layer mid depth
            CloudLayer(-150f, 480f, 0.95f, 0.32f, 0.55f, 170f), // Secondary drifting foreground volume
            CloudLayer(400f, 640f, 1.40f, 0.12f, 0.70f, 220f)   // Grand heavy bottom-lit layer
        )
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameMillis {
                clouds.forEach { it.update(1200f) }
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val camX = sin(parallaxOffset).toFloat() * 12f
        val camY = cos(parallaxOffset).toFloat() * 8f

        // Draw a soft glowing sun or moon rays hiding deep behind clouds to create realistic ray filtering
        val sunBehindX = w * 0.76f + camX
        val sunBehindY = h * 0.18f + camY
        val haloColors = if (isDay) {
            listOf(
                Color(0xFFFEF08A).copy(alpha = 0.22f),
                Color(0xFFF97316).copy(alpha = 0.06f),
                Color.Transparent
            )
        } else {
            listOf(
                Color(0xFFF1F5F9).copy(alpha = 0.18f),
                Color(0xFFCBD5E1).copy(alpha = 0.04f),
                Color.Transparent
            )
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = haloColors,
                center = Offset(sunBehindX, sunBehindY),
                radius = 320.dp.toPx()
            ),
            radius = 320.dp.toPx(),
            center = Offset(sunBehindX, sunBehindY)
        )

        // Render layered volumetric clouds
        clouds.forEach { cloud ->
            val actualX = cloud.x + camX * (cloud.scale * 0.4f)
            val actualY = cloud.y + camY * (cloud.scale * 0.3f)

            // Step 1: Broad multi-scale volumetric atmospheric ambient occlusion shadow (extremely soft, giant radius to avoid bubble edges)
            cloud.puffs.forEach { puff ->
                val waveX = sin(cloud.time * puff.phaseSpeed + puff.anglePhase) * 22f * cloud.scale
                val waveY = cos(cloud.time * puff.phaseSpeed * 0.8f + puff.anglePhase) * 14f * cloud.scale
                val dyRadius = sin(cloud.time * puff.phaseSpeed * 0.5f + puff.anglePhase) * 0.09f

                val nodeCenter = Offset(
                    actualX + puff.relX * cloud.scale + waveX,
                    actualY + puff.relY * cloud.scale + waveY
                )
                val r = cloud.baseRadius * puff.radiusRatio * (1f + dyRadius) * cloud.scale

                // Broad shadow foundation (massive scale, very transparent)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0F172A).copy(alpha = cloud.alpha * 0.22f),
                            Color(0xFF1E293B).copy(alpha = cloud.alpha * 0.08f),
                            Color.Transparent
                        ),
                        center = nodeCenter + Offset(-25f * cloud.scale, 45f * cloud.scale),
                        radius = r * 2.2f
                    ),
                    radius = r * 2.2f,
                    center = nodeCenter + Offset(-25f * cloud.scale, 45f * cloud.scale)
                )
            }

            // Step 2: Main Cloud Body with high soft blend (radial gradient with massive feathering)
            cloud.puffs.forEach { puff ->
                val waveX = sin(cloud.time * puff.phaseSpeed + puff.anglePhase) * 22f * cloud.scale
                val waveY = cos(cloud.time * puff.phaseSpeed * 0.8f + puff.anglePhase) * 14f * cloud.scale
                val dyRadius = sin(cloud.time * puff.phaseSpeed * 0.5f + puff.anglePhase) * 0.09f

                val nodeCenter = Offset(
                    actualX + puff.relX * cloud.scale + waveX,
                    actualY + puff.relY * cloud.scale + waveY
                )
                val r = cloud.baseRadius * puff.radiusRatio * (1f + dyRadius) * cloud.scale

                // Soft body core
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFF8FAFC).copy(alpha = cloud.alpha * 0.85f),
                            Color(0xFFE2E8F0).copy(alpha = cloud.alpha * 0.70f),
                            Color(0xFF94A3B8).copy(alpha = cloud.alpha * 0.35f),
                            Color(0xFF475569).copy(alpha = cloud.alpha * 0.10f),
                            Color.Transparent
                        ),
                        center = nodeCenter,
                        radius = r * 1.5f
                    ),
                    radius = r * 1.5f,
                    center = nodeCenter
                )
            }

            // Step 3: Beautiful sunlight backscattering / silver lining rim (highly transparent, shifted top-right)
            cloud.puffs.forEach { puff ->
                val waveX = sin(cloud.time * puff.phaseSpeed + puff.anglePhase) * 22f * cloud.scale
                val waveY = cos(cloud.time * puff.phaseSpeed * 0.8f + puff.anglePhase) * 14f * cloud.scale
                val dyRadius = sin(cloud.time * puff.phaseSpeed * 0.5f + puff.anglePhase) * 0.09f

                val nodeCenter = Offset(
                    actualX + puff.relX * cloud.scale + waveX,
                    actualY + puff.relY * cloud.scale + waveY
                )
                val r = cloud.baseRadius * puff.radiusRatio * (1f + dyRadius) * cloud.scale

                // Soft silver lining highlight
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFFBEB).copy(alpha = cloud.alpha * 0.55f),
                            Color(0xFFFEF3C7).copy(alpha = cloud.alpha * 0.20f),
                            Color.Transparent
                        ),
                        center = nodeCenter - Offset(r * 0.35f, r * 0.35f),
                        radius = r * 1.2f
                    ),
                    radius = r * 1.2f,
                    center = nodeCenter - Offset(r * 0.35f, r * 0.35f)
                )
            }
        }
    }
}

@Composable
fun FoggyVisuals() {
    val infiniteTransition = rememberInfiniteTransition(label = "FoggyLoop")

    // Slow shifting waves for realistic mist layers
    val wavePhase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "MistWaveY"
    )

    val wavePhase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(26000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "MistWaveX"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Define multiple rich volumetric horizontal mist bands with sine-wave vertical variance
        val fogBands = listOf(
            Triple(h * 0.32f, wavePhase1, Color(0xFFE2E8F0).copy(alpha = 0.14f)),
            Triple(h * 0.48f, wavePhase2, Color(0xFFCBD5E1).copy(alpha = 0.18f)),
            Triple(h * 0.64f, wavePhase1 * 1.3f, Color(0xFF94A3B8).copy(alpha = 0.22f)),
            Triple(h * 0.80f, wavePhase2 * 0.8f, Color(0xFF64748B).copy(alpha = 0.26f)),
            Triple(h * 0.95f, wavePhase1 * 0.6f, Color(0xFF475569).copy(alpha = 0.32f))
        )

        fogBands.forEach { (midY, phase, color) ->
            val path = Path().apply {
                moveTo(0f, midY)
                val segmentCount = 20
                val step = w / segmentCount
                for (i in 0..segmentCount) {
                    val currX = i * step
                    // Realistic double-sine organic waving
                    val dY = sin(i * 0.42f + phase).toFloat() * 45f + cos(i * 0.25f - phase * 0.5f).toFloat() * 18f
                    val targetY = midY + dY
                    if (i == 0) moveTo(currX, targetY) else lineTo(currX, targetY)
                }
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }

            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(color, color.copy(alpha = 0.0f)),
                    startY = midY - 120f,
                    endY = h
                )
            )
        }

        // Add soft foggy vignette on the sides to amplify depth
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF475569).copy(alpha = 0.16f),
                    Color.Transparent,
                    Color(0xFF475569).copy(alpha = 0.16f),
                )
            )
        )
    }
}

@Composable
fun RainyVisuals() {
    // Large pool of raindrops with distinct layers: Background (faint & slow) vs Foreground (thick & fast)
    val rainDrops = remember {
        mutableStateListOf<RainDrop>().apply {
            repeat(130) {
                val isForeground = Random.nextFloat() > 0.6f
                add(
                    RainDrop(
                        x = Random.nextFloat() * 1200f,
                        y = Random.nextFloat() * 2200f - 1100f,
                        length = if (isForeground) 55f + Random.nextFloat() * 25f else 30f + Random.nextFloat() * 20f,
                        speed = if (isForeground) 48f + Random.nextFloat() * 18f else 28f + Random.nextFloat() * 10f,
                        alpha = if (isForeground) 0.32f + Random.nextFloat() * 0.28f else 0.10f + Random.nextFloat() * 0.18f,
                        width = if (isForeground) 1.8f + Random.nextFloat() * 1.5f else 0.8f + Random.nextFloat() * 0.8f,
                        isForeground = isForeground
                    )
                )
            }
        }
    }

    // Interactive glass water ripples mapping phone droplets
    val glassRipples = remember {
        List(18) {
            GlassRipple(
                x = 0f, y = 0f, radius = 0f, maxRadius = 0f, thickness = 0f, alpha = 0f, speed = 0f, active = false
            )
        }
    }

    LaunchedEffect(Unit) {
        var frameCount = 0
        while (isActive) {
            withFrameMillis {
                frameCount++
                
                // Constantly drive falling raindrops with realistic leftwards storm wind slant (-2.8)
                rainDrops.forEach { it.update(2400f, 1200f, -2.8f) }

                // Drive active ripples on glass
                glassRipples.forEach { if (it.active) it.update() }

                // Spawn new glass screen splashes periodically to mimic raindrops crashing on front viewport glass
                if (frameCount % 6 == 0) {
                    val deadRipple = glassRipples.firstOrNull { !it.active }
                    deadRipple?.spawn(
                        newX = Random.nextFloat() * 1080f,
                        newY = hScaleEstimate() * Random.nextFloat()
                    )
                }
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Constant wind offset slant angle calculations
        val slantX = -2.8f * 3.8f

        // Draw deep backing vertical rain curtain mist / volumetric haze
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF1E293B).copy(alpha = 0.40f),
                    Color(0xFF334155).copy(alpha = 0.25f),
                    Color.Transparent
                )
            )
        )

        // Draw raindrops layered safely via foreground flags
        rainDrops.forEach { drop ->
            // Boundary validation
            if (drop.x < -100f) drop.x = w + Random.nextFloat() * 100f
            if (drop.x > w + 100f) drop.x = -Random.nextFloat() * 100f
            if (drop.y > h) {
                drop.y = -drop.length - Random.nextFloat() * 80f
                drop.x = Random.nextFloat() * w
            }

            // Draw rain strings with a subtle color offset to replicate sky reflections
            drawLine(
                color = if (drop.isForeground) Color(0xFFE2E8F0).copy(alpha = drop.alpha) else Color(0xFF94A3B8).copy(alpha = drop.alpha),
                start = Offset(drop.x, drop.y),
                end = Offset(drop.x + slantX, drop.y + drop.length),
                strokeWidth = drop.width
            )
        }

        // Draw amazing glass water splashes / screen ripples
        glassRipples.forEach { ripple ->
            if (ripple.active) {
                // Outermost ring wave
                drawCircle(
                    color = Color.White.copy(alpha = ripple.alpha),
                    radius = ripple.radius,
                    center = Offset(ripple.x, ripple.y),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = ripple.thickness)
                )
                // Small inner soft glow to solidify raindrop landing point
                drawCircle(
                    color = Color.White.copy(alpha = ripple.alpha * 0.45f),
                    radius = ripple.radius * 0.35f,
                    center = Offset(ripple.x, ripple.y)
                )
            }
        }
    }
}

@Composable
fun SnowyVisuals() {
    val infiniteTransition = rememberInfiniteTransition(label = "SnowyLoop")

    // Ambient polar breeze gust mapping
    val polarBreezePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PolarWindBreeze"
    )

    val snowFlakes = remember {
        mutableStateListOf<Snowflake>().apply {
            repeat(110) {
                add(
                    Snowflake(
                        x = Random.nextFloat() * 1200f,
                        y = Random.nextFloat() * 2200f - 1100f,
                        radius = 2.5f + Random.nextFloat() * 7.5f,
                        speed = 1.6f + Random.nextFloat() * 3.4f,
                        angle = Random.nextFloat() * 360f,
                        wanderSpeed = 0.015f + Random.nextFloat() * 0.025f,
                        alpha = 0.25f + Random.nextFloat() * 0.65f,
                        sparklePhase = Random.nextFloat() * 100f,
                        sparkleSpeed = 0.01f + Random.nextFloat() * 0.03f
                    )
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        val timeDelta = 0.016f
        var timeElapsed = 0f
        while (isActive) {
            withFrameMillis {
                timeElapsed += timeDelta
                snowFlakes.forEach { it.update(2400f, 1200f, timeElapsed) }
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Horizontal polar cross-breeze offset calculation
        val breezeX = sin(polarBreezePhase).toFloat() * 1.5f

        // Draw snowy winter twilight atmosphere overlay
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF0F172A).copy(alpha = 0.35f)
                )
            )
        )

        snowFlakes.forEach { flake ->
            if (flake.x < -10f) flake.x = w + 10f
            if (flake.x > w + 10f) flake.x = -10f
            if (flake.y > h) {
                flake.y = -flake.radius - 12f
                flake.x = Random.nextFloat() * w
            }

            // Real-time calculated breeze drift dynamic inject
            flake.x += breezeX * (flake.radius * 0.12f)

            // Sparkle glowing scaling cycle
            val crystalGlint = (0.5f + 0.5f * sin(flake.sparklePhase)).toFloat()
            val finalAlpha = flake.alpha * (0.6f + 0.4f * crystalGlint)

            // Draw a soft concentric halo representing frozen condensation vapor around snowflake
            drawCircle(
                color = Color.White.copy(alpha = finalAlpha * 0.28f),
                radius = flake.radius * 2.3f,
                center = Offset(flake.x, flake.y)
            )

            // Center dense white ice core
            drawCircle(
                color = Color.White.copy(alpha = finalAlpha),
                radius = flake.radius,
                center = Offset(flake.x, flake.y)
            )

            // For larger flakes, draw realistic delicate micro crystal star needles
            if (flake.radius > 6.0f) {
                val needleLen = flake.radius * 1.8f
                // Simple 4-point cross layout for stunning realism
                drawLine(
                    color = Color.White.copy(alpha = finalAlpha * 0.65f),
                    start = Offset(flake.x, flake.y - needleLen),
                    end = Offset(flake.x, flake.y + needleLen),
                    strokeWidth = 1.2f
                )
                drawLine(
                    color = Color.White.copy(alpha = finalAlpha * 0.65f),
                    start = Offset(flake.x - needleLen, flake.y),
                    end = Offset(flake.x + needleLen, flake.y),
                    strokeWidth = 1.2f
                )
            }
        }
    }
}

@Composable
fun ThunderstormVisuals() {
    val rainDrops = remember {
        mutableStateListOf<RainDrop>().apply {
            repeat(140) {
                val isForeground = Random.nextFloat() > 0.55f
                add(
                    RainDrop(
                        x = Random.nextFloat() * 1200f,
                        y = Random.nextFloat() * 2200f - 1100f,
                        length = if (isForeground) 55f + Random.nextFloat() * 30f else 30f + Random.nextFloat() * 20f,
                        speed = if (isForeground) 52f + Random.nextFloat() * 20f else 33f + Random.nextFloat() * 12f,
                        alpha = if (isForeground) 0.30f + Random.nextFloat() * 0.30f else 0.10f + Random.nextFloat() * 0.15f,
                        width = if (isForeground) 1.8f + Random.nextFloat() * 1.5f else 0.8f + Random.nextFloat() * 0.8f,
                        isForeground = isForeground
                    )
                )
            }
        }
    }

    // Advanced dynamic double-fork lightning simulator state
    var lightningAlpha by remember { mutableStateOf(0f) }
    var lightningFrameCountdown by remember { mutableStateOf(Random.nextInt(40, 160)) }
    var lightningPoints by remember { mutableStateOf<List<Pair<Offset, Offset>>>(emptyList()) }
    var activeFlashWaveCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameMillis {
                // Heavy wind slanted speed update
                rainDrops.forEach { it.update(2400f, 1200f, -4.2f) }

                // Handle organic multi-step lightning propagation
                if (lightningFrameCountdown > 0) {
                    lightningFrameCountdown--
                    if (lightningAlpha > 0f) {
                        // Decay over frames
                        lightningAlpha -= 0.08f
                        if (lightningAlpha < 0) lightningAlpha = 0f
                    }
                } else {
                    // Trigger double multi-stage electric arc discharge!
                    activeFlashWaveCount = if (Random.nextFloat() > 0.5f) 2 else 1
                    lightningAlpha = 1.0f
                    val boltPoints = mutableListOf<Pair<Offset, Offset>>()

                    // Form an organic multi-branched jagged lightning tree
                    var startX = 150f + Random.nextFloat() * 800f
                    var startY = 10f
                    val destY = 1700f

                    while (startY < destY) {
                        val branchLength = 50f + Random.nextFloat() * 110f
                        val angleOffset = -1.21f + Random.nextFloat() * 2.42f // jagged deviations
                        val endX = startX + tanAngleConversion(angleOffset) * branchLength
                        val endY = startY + branchLength

                        boltPoints.add(Pair(Offset(startX, startY), Offset(endX, endY)))

                        // 35% probability of branching a sub-lightning path
                        if (Random.nextFloat() < 0.35f) {
                            var subX = endX
                            var subY = endY
                            repeat(3) {
                                val slen = 35f + Random.nextFloat() * 60f
                                val sEndX = subX + (-1.5f + Random.nextFloat() * 3f) * slen * 0.3f
                                val sEndY = subY + slen
                                boltPoints.add(Pair(Offset(subX, subY), Offset(sEndX, sEndY)))
                                subX = sEndX
                                subY = sEndY
                            }
                        }

                        startX = endX
                        startY = endY
                    }

                    lightningPoints = boltPoints
                    // Reset frame delay to next violent sound barrier punch
                    lightningFrameCountdown = Random.nextInt(120, 320)
                }
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val windOffsetSlantX = -4.2f * 3.8f

        // ——————————————————————————————————————————————————————————
        // 1. SKY FLASH AMBIENT LIGHTING
        // ——————————————————————————————————————————————————————————
        if (lightningAlpha > 0f) {
            // Screen flash wave
            drawRect(
                color = Color(0xFFC0BDF5).copy(alpha = lightningAlpha * 0.40f)
            )
        }

        // ——————————————————————————————————————————————————————————
        // 2. STRIKING BOLTS & DOUBLE-GAUSSIAN GLOW BULBS
        // ——————————————————————————————————————————————————————————
        if (lightningAlpha > 0f && lightningPoints.isNotEmpty()) {
            lightningPoints.forEach { (start, end) ->
                // Outer massive atmospheric corona glow (electric magenta-indigo discharge aura)
                drawLine(
                    color = Color(0xFFA855F7).copy(alpha = lightningAlpha * 0.48f),
                    start = start,
                    end = end,
                    strokeWidth = 16f * lightningAlpha
                )
                // Middle vibrant white-violet electric light
                drawLine(
                    color = Color(0xFFC084FC).copy(alpha = lightningAlpha * 0.75f),
                    start = start,
                    end = end,
                    strokeWidth = 7f * lightningAlpha
                )
                // Pure incandescent core line
                drawLine(
                    color = Color.White,
                    start = start,
                    end = end,
                    strokeWidth = 2.5f * lightningAlpha
                )
            }
        }

        // ——————————————————————————————————————————————————————————
        // 3. ELECTRIC WIND-DRIVEN HEAVY PRECIPITATION
        // ——————————————————————————————————————————————————————————
        rainDrops.forEach { drop ->
            if (drop.x < -100f) drop.x = w + Random.nextFloat() * 100f
            if (drop.x > w + 100f) drop.x = -Random.nextFloat() * 100f
            if (drop.y > h) {
                drop.y = -drop.length - Random.nextFloat() * 80f
                drop.x = Random.nextFloat() * w
            }

            // Realism modifier: raindrops glisten, reflecting the immense electric sky flash
            val lightningFlashHighlightMultiplier = if (lightningAlpha > 0.1f) 2.2f else 1.0f
            val baseColor = if (drop.isForeground) Color(0xFFE2E8F0) else Color(0xFF475569)
            val dropAlpha = (drop.alpha * lightningFlashHighlightMultiplier).coerceIn(0.05f, 0.95f)

            drawLine(
                color = baseColor.copy(alpha = dropAlpha),
                start = Offset(drop.x, drop.y),
                end = Offset(drop.x + windOffsetSlantX, drop.y + drop.length),
                strokeWidth = drop.width * (if (lightningAlpha > 0.1f) 1.25f else 1.0f)
            )
        }
    }
}

// Inline constant height scale boundaries approximation
private fun hScaleEstimate(): Float = 2200f

// Safe tan implementation for custom angle mathematics
private fun tanAngleConversion(angle: Float): Float {
    return sin(angle) / cos(angle).coerceAtLeast(0.1f)
}

private class CosmicStar(
    var x: Float,
    var y: Float,
    var radius: Float,
    var alpha: Float,
    var twinkleSpeed: Float,
    var twinklePhase: Float
) {
    fun update() {
        twinklePhase += twinkleSpeed
    }
}

@Composable
fun NightVisuals() {
    val infiniteTransition = rememberInfiniteTransition(label = "NightLoop")
    val translationPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "MoonHover"
    )

    val stars = remember {
        mutableStateListOf<CosmicStar>().apply {
            repeat(65) {
                add(
                    CosmicStar(
                        x = Random.nextFloat() * 1080f,
                        y = Random.nextFloat() * 2000f,
                        radius = 0.8f + Random.nextFloat() * 2.5f,
                        alpha = 0.20f + Random.nextFloat() * 0.70f,
                        twinkleSpeed = 0.02f + Random.nextFloat() * 0.04f,
                        twinklePhase = Random.nextFloat() * 100f
                    )
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameMillis {
                stars.forEach { it.update() }
            }
        }
    }

    val startColor = Color(0xFF030712) // matching deep space night top

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Draw twinkling stars
        stars.forEach { star ->
            val twinkle = (0.25f + 0.75f * sin(star.twinklePhase)).coerceIn(0.1f, 1f)
            drawCircle(
                color = Color.White.copy(alpha = star.alpha * twinkle),
                radius = star.radius,
                center = Offset(star.x, star.y)
            )
        }

        // Draw stellar indigo nebulas
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF6366F1).copy(alpha = 0.07f), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = w * 0.75f
            ),
            radius = w * 0.75f,
            center = Offset(w * 0.5f, h * 0.5f)
        )

        // Floating moon camera drift offset
        val moonDriftX = sin(translationPhase).toFloat() * 12f
        val moonDriftY = cos(translationPhase).toFloat() * 8f

        val centerX = w * 0.82f + moonDriftX
        val centerY = h * 0.18f + moonDriftY
        val baseRadius = 55.dp.toPx()

        // 1. Soft lunar atmospheric corona backlight glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFF1F5F9).copy(alpha = 0.22f),
                    Color(0xFFCBD5E1).copy(alpha = 0.06f),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY),
                radius = baseRadius * 2.8f
            ),
            radius = baseRadius * 2.8f,
            center = Offset(centerX, centerY)
        )

        // 2. Glowing silver moon body disc
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color(0xFFF8FAFC), Color(0xFFE2E8F0)),
                center = Offset(centerX, centerY),
                radius = baseRadius
            ),
            radius = baseRadius,
            center = Offset(centerX, centerY)
        )

        // 3. Subtract shadow circle to carve a brilliant crescent
        drawCircle(
            color = startColor,
            radius = baseRadius * 0.95f,
            center = Offset(centerX - baseRadius * 0.35f, centerY - baseRadius * 0.15f)
        )
    }
}
