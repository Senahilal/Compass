package com.example.compassanddigitallevel

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.compassanddigitallevel.ui.theme.CompassAndDigitalLevelTheme
import kotlin.math.roundToInt

class MainActivity : ComponentActivity(), SensorEventListener  {

    private lateinit var sensorManager: SensorManager

    //Use the magnetometer and accelerometer to calculate the compass heading.
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    //Use the gyroscope to detect tilt and display a digital level with roll and pitch values.
    private var gyroscope: Sensor? = null


    private var compassHeading by mutableStateOf(0f)
    private var roll by mutableStateOf(0f)
    private var pitch by mutableStateOf(0f)

    private var _accuracy by mutableStateOf("Unknown")


    //x, y and z values of acceleration (force applied to the device)
    //x axis: Side to Side; y-axis: Front to Back; z-axis:Up and Down
    private var accelerometerValues: FloatArray? = null

    //x - horizontal magnetic field in the left-right direction (east + / west -)
    //y - horizontal magnetic field in the front-back direction (north + / south -)
    //z - vertical magnetic field (up/down)
    private var magnetometerValues: FloatArray? = null




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Sensor Manager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        setContent {
            CompassAndDigitalLevelTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ){
                    CompassScreen(compassHeading)
                    DigitalLevelScreen(roll, pitch)
                }

            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {

                Sensor.TYPE_ACCELEROMETER -> accelerometerValues = it.values.clone()
                Sensor.TYPE_MAGNETIC_FIELD -> magnetometerValues = it.values.clone()

                //for tilt detection
                Sensor.TYPE_GYROSCOPE -> {
                    // converting rad/s to degrees/s
                    pitch = it.values[1] * (180 / Math.PI).toFloat() //forward/backward
                    roll = it.values[2] * (180 / Math.PI).toFloat()  //left/right

                }
            }

            if (accelerometerValues != null && magnetometerValues != null) {
                val rotationMatrix = FloatArray(9)
                val orientationAngles = FloatArray(3)

                if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerValues, magnetometerValues)) {
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)

                    //rad -> degrees (-180° to 180° degrees)
                    val heading = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()

                    // Normalize to 0-360 degrees
                    compassHeading = (heading + 360) % 360
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        _accuracy = when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "High"
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Medium"
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "Low"
            SensorManager.SENSOR_STATUS_UNRELIABLE -> "Unreliable"
            else -> "Unknown"
        }
    }
}

@Composable
fun CompassScreen(compassHeading: Float) {
    Column(
        modifier = Modifier
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Compass Heading: ${compassHeading}°",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Red side points to North",
            style = MaterialTheme.typography.bodyLarge
        )

//        //rotate the png based on heading -red side:north
//        Image(
//            painter = painterResource(id = R.drawable.compass_needle),
//            contentDescription = "Compass Needle",
//            modifier = Modifier
//                .size(200.dp)
//                .rotate(-compassHeading) // Rotate the compass needle
//        )

        Box(
            contentAlignment = Alignment.Center, // Center the needle over the compass
            modifier = Modifier.size(200.dp)
        ) {
            // Background compass image
            Image(
                painter = painterResource(id = R.drawable.compass),
                contentDescription = "Compass",
                modifier = Modifier.fillMaxSize() // Fill the available space
            )

            //rotate the png based on heading -red side:north
            Image(
                painter = painterResource(id = R.drawable.compass_needle),
                contentDescription = "Compass Needle",
                modifier = Modifier
                    .size(200.dp)
                    .rotate(-compassHeading) // Rotate the compass needle dynamically
            )
        }
    }
}


//display a digital level with roll and pitch values.
@Composable
fun DigitalLevelScreen(roll: Float, pitch: Float) {
    Column(
        modifier = Modifier
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Digital Level",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        Text(
            text = "Roll: ${roll}°",
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = "Pitch: ${pitch}°",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}



@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CompassAndDigitalLevelTheme {

    }
}