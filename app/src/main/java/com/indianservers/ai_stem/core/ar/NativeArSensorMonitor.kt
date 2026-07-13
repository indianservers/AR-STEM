package com.indianservers.ai_stem.core.ar

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import android.util.Log
import kotlin.math.PI
import kotlin.math.sqrt

class NativeArSensorMonitor(
    context: Context,
    private val onSample: (NativeArSensorSample) -> Unit
) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var linearAccelerationMagnitude = 0f
    private var gyroscopeMagnitude = 0f
    private var pitchDegrees: Float? = null
    private var rollDegrees: Float? = null
    private var magneticMagnitude: Float? = null
    private var ambientLightLux: Float? = null
    private var lastEmitMs = 0L

    fun start() {
        register(Sensor.TYPE_LINEAR_ACCELERATION, SensorManager.SENSOR_DELAY_GAME)
        register(Sensor.TYPE_ACCELEROMETER, SensorManager.SENSOR_DELAY_GAME)
        register(Sensor.TYPE_GYROSCOPE, SensorManager.SENSOR_DELAY_GAME)
        register(Sensor.TYPE_GYROSCOPE_UNCALIBRATED, SensorManager.SENSOR_DELAY_GAME)
        register(Sensor.TYPE_ROTATION_VECTOR, SensorManager.SENSOR_DELAY_GAME)
        register(Sensor.TYPE_MAGNETIC_FIELD, SensorManager.SENSOR_DELAY_UI)
        register(Sensor.TYPE_LIGHT, SensorManager.SENSOR_DELAY_UI)
        Log.d("AiStemAR", "Native sensor monitor started")
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        Log.d("AiStemAR", "Native sensor monitor stopped")
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_LINEAR_ACCELERATION -> linearAccelerationMagnitude = event.values.magnitude3()
            Sensor.TYPE_ACCELEROMETER -> if (linearAccelerationMagnitude == 0f) {
                linearAccelerationMagnitude = (event.values.magnitude3() - EARTH_GRAVITY).coerceAtLeast(0f)
            }
            Sensor.TYPE_GYROSCOPE, Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> gyroscopeMagnitude = event.values.magnitude3()
            Sensor.TYPE_ROTATION_VECTOR -> updateOrientation(event.values)
            Sensor.TYPE_MAGNETIC_FIELD -> magneticMagnitude = event.values.magnitude3()
            Sensor.TYPE_LIGHT -> ambientLightLux = event.values.firstOrNull()
        }
        emitThrottled()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun register(type: Int, delay: Int) {
        sensorManager.getDefaultSensor(type)?.let { sensor ->
            sensorManager.registerListener(this, sensor, delay)
            Log.d("AiStemAR", "Registered native sensor type=$type name=${sensor.name}")
        } ?: Log.d("AiStemAR", "Native sensor type=$type unavailable")
    }

    private fun updateOrientation(values: FloatArray) {
        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, values)
        SensorManager.getOrientation(rotationMatrix, orientation)
        pitchDegrees = orientation[1].toDegrees()
        rollDegrees = orientation[2].toDegrees()
    }

    private fun emitThrottled() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastEmitMs < EMIT_INTERVAL_MS) return
        lastEmitMs = now
        onSample(
            NativeArSensorSample(
                linearAccelerationMagnitude = linearAccelerationMagnitude,
                gyroscopeMagnitude = gyroscopeMagnitude,
                pitchDegrees = pitchDegrees,
                rollDegrees = rollDegrees,
                magneticMagnitude = magneticMagnitude,
                ambientLightLux = ambientLightLux,
                sensorAgeMillis = 0L
            )
        )
    }

    private fun FloatArray.magnitude3(): Float {
        val x = getOrNull(0) ?: 0f
        val y = getOrNull(1) ?: 0f
        val z = getOrNull(2) ?: 0f
        return sqrt(x * x + y * y + z * z)
    }

    private fun Float.toDegrees(): Float = (this * 180f / PI.toFloat())

    private companion object {
        const val EMIT_INTERVAL_MS = 80L
        const val EARTH_GRAVITY = 9.80665f
    }
}
