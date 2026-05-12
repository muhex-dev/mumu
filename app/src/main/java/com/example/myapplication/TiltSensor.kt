package com.example.myapplication

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * TiltSensor — reads the accelerometer and exposes smooth tilt values.
 *
 * tiltX:  -1f = phone tilted fully left,  +1f = fully right
 * tiltY:  -1f = phone tilted fully forward, +1f = fully back
 *
 * Values are low-pass filtered for smoothness.
 */
class TiltSensor(private val context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE)
            as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // Raw smoothed gravity components
    private var gravX = 0f
    private var gravY = 0f

    // Low-pass filter alpha — lower = smoother but slower
    private val alpha = 0.12f

    private val _tiltX = MutableStateFlow(0f)
    private val _tiltY = MutableStateFlow(0f)

    val tiltX: StateFlow<Float> = _tiltX
    val tiltY: StateFlow<Float> = _tiltY

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

            // Low-pass filter to smooth out jitter
            gravX = alpha * event.values[0] + (1 - alpha) * gravX
            gravY = alpha * event.values[1] + (1 - alpha) * gravY

            // Normalise to -1..+1 range
            // Max accelerometer value when fully tilted ≈ 9.8 (gravity)
            // We use ±5f as the comfortable tilt range so it's responsive
            _tiltX.value = (-gravX / 5f).coerceIn(-1f, 1f)
            _tiltY.value = ( gravY / 5f).coerceIn(-1f, 1f)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(
                listener, it,
                SensorManager.SENSOR_DELAY_UI   // 60ms interval — smooth, not battery-heavy
            )
        }
    }

    fun stop() {
        sensorManager.unregisterListener(listener)
    }
}

/**
 * Composable helper that provides tilt values and
 * automatically starts/stops with the lifecycle.
 */
@Composable
fun rememberTiltState(): Pair<Float, Float> {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val sensor = remember { TiltSensor(context) }

    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }

    // Collect sensor values
    LaunchedEffect(sensor) {
        sensor.tiltX.collect { tiltX = it }
    }
    LaunchedEffect(sensor) {
        sensor.tiltY.collect { tiltY = it }
    }

    // Tie to lifecycle — stop when app goes background
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START  -> sensor.start()
                Lifecycle.Event.ON_STOP   -> sensor.stop()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        sensor.start()
        onDispose {
            lifecycle.removeObserver(observer)
            sensor.stop()
        }
    }

    return Pair(tiltX, tiltY)
}