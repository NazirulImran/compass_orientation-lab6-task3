package s72574.lab6.compass_orientation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), SensorEventListener {

    // Sensor Manager and Sensors (accelerometer and magnetometer)
    private lateinit var sensorManager: SensorManager //access all hardware
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    // UI Elements
    private lateinit var compassArrow: ImageView //get from drawable
    private lateinit var headingText: TextView

    // Step 2: Set Up Sensor Arrays
    // These hold the latest raw data from the hardware
    private var gravity = FloatArray(3)
    private var geomagnetic = FloatArray(3)

    // Check flags to ensure we have data from both sensors before calculating
    private var hasGravity = false
    private var hasGeomagnetic = false

    // Arrays for Rotation Matrix calculation
    private var rMat = FloatArray(9) // Rotation Matrix
    private var iMat = FloatArray(9) // Inclination Matrix
    private var orientation = FloatArray(3) // Output Orientation (Azimuth, Pitch, Roll)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI
        compassArrow = findViewById(R.id.compassArrow)
        headingText = findViewById(R.id.headingText)

        // Initialize Sensors
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    }

    // Step 5 (Lifecycle): Register Two Listeners
    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this) // Stop battery drain
    }

    // Step 3: The Math of Orientation
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        // 1. Update Data Arrays based on sensor type
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, gravity, 0, gravity.size)
            hasGravity = true
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, geomagnetic, 0, geomagnetic.size)
            hasGeomagnetic = true
        }

        // Only calculate if we have data from BOTH sensors
        if (hasGravity && hasGeomagnetic) {

            // 2. Compute Rotation Matrix
            // This fills 'rMat' with the math needed to translate sensor data to world coordinates
            val success = SensorManager.getRotationMatrix(rMat, iMat, gravity, geomagnetic)

            if (success) {
                // 3. Get Orientation
                // orientation[0] = Azimuth (Z-axis direction)
                SensorManager.getOrientation(rMat, orientation)

                // Step 4: Convert and Rotate
                val azimuthInRadians = orientation[0]

                // 1. Convert to Degrees
                var azimuthInDegrees = Math.toDegrees(azimuthInRadians.toDouble()).toFloat()

                // 2. Normalize (0 to 360)
                // The raw result can be negative (e.g., -90 for West). This fixes it.
                azimuthInDegrees = (azimuthInDegrees + 360) % 360

                // 3. Update the Arrow (Animation)
                // We use negative degrees because the phone rotates one way,
                // but the arrow must rotate the opposite way to stay pointing North.
                compassArrow.rotation = -azimuthInDegrees

                // Update Text
                headingText.text = "${azimuthInDegrees.toInt()}° North"
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}