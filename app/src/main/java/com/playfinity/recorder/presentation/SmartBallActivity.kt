package com.playfinity.recorder.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.Build.VERSION
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AppCompatActivity
import com.playfinity.recorder.R
import com.playfinity.recorder.utils.hasAllRequiredBlePermissionsAndServices
import com.playfinity.recorder.utils.hasBluetoothPermission
import com.playfinity.recorder.utils.hasLocationPermission
import com.playfinity.recorder.utils.isBluetoothEnabled
import com.playfinity.recorder.utils.isLocationEnabled
import com.playfinity.recorder.utils.isLocationRequired
import io.playfinity.sdk.PFICallback
import io.playfinity.sdk.PlayfinitySDK
import io.playfinity.sdk.PlayfinitySDKBuilder
import io.playfinity.sdk.core.bluetooth.BluetoothDataRaw
import io.playfinity.sdk.core.callback.DiscoverSensorListener
import io.playfinity.sdk.core.device.Sensor
import io.playfinity.sdk.core.device.SensorEvent
import io.playfinity.sdk.core.device.SensorEventType
import io.playfinity.sdk.core.device.SensorEventsSubscriber
import io.playfinity.sdk.core.device.SensorRawDataSubscriber
import io.playfinity.sdk.core.device.SensorType.Baseball
import io.playfinity.sdk.utility.error.PFIThrowable
import io.playfinity.sdk.utility.extention.toHexString
import java.lang.Long.max

@SuppressLint("SetTextI18n")
class SmartBallActivity : AppCompatActivity(),
    PFICallback, DiscoverSensorListener, SensorEventsSubscriber, SensorRawDataSubscriber {

    private companion object {
        private const val TAG = "SmartBallActivity"
        const val CONNECTED_MODE = false
    }

    private var rawPacketsProcessed = 0L
    private var rawPacketsTimestamp = 0L
    private var isScanningInitializing = false
    private var isScanningInProgress = false
    private var playfinitySDK: PlayfinitySDK? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize UI.
        setContentView(R.layout.activity_smartball)

        // Initialize Playfinity SDK.
        initializeSdk()
    }

    override fun onResume() {
        super.onResume()
        startScanningWithPermissions()
    }

    override fun onStop() {
        super.onStop()
        stopBleScanning()
    }

    override fun onPlayfinityReady(sdk: PlayfinitySDK) {
        this.playfinitySDK = sdk

        findViewById<TextView>(R.id.connectionStatus).text = "Status: Connected"

        // Start scanning devices.
        startScanningWithPermissions()

        // Testing sensor:
        observeSensors()

        // Log the fact.
        Log.d(TAG, "onPlayfinityReady() is called with $sdk")
    }

    override fun onPlayfinityError(pfiThrowable: PFIThrowable) {
        findViewById<TextView>(R.id.connectionStatus).text = "Status: Error (${pfiThrowable.message})"

        // Log an error.
        Log.e(TAG, pfiThrowable.message, pfiThrowable)
    }

    override fun onSensorDiscovered(sensor: Sensor) {
        findViewById<TextView>(R.id.sensorStatus).text = "SensorId: ${sensor.sensorId}"

        Log.d(TAG, "onSensorDiscovered() is called with ${sensor.sensorId}")

        when (CONNECTED_MODE) {
            true -> observeSensorRawData(sensor)
            else -> observeSensorEvents(sensor)
        }
    }

    override fun onSensorDiscoverError(pfiThrowable: PFIThrowable) {
        Log.d(TAG, "onSensorDiscoverError() | Reason: ${pfiThrowable.cause?.localizedMessage}")
    }

    override fun onSensorEvent(event: SensorEvent) {
        if (event.eventType != SensorEventType.Inair && event.eventType != SensorEventType.Jumping) {
            findViewById<TextView>(R.id.sensorEventStatus).text = "Event: [${event.identifier}] ${event.eventType}"
        }

        Log.d(TAG, "onSensorEvent() $event")
    }

    override fun onSensorRawData(rawData: BluetoothDataRaw) {
        var pps = 0L
        if (rawPacketsProcessed == 0L) {
            rawPacketsTimestamp = System.currentTimeMillis()
        } else {
            val timeDiff = max(1L, (System.currentTimeMillis() - rawPacketsTimestamp) / 1000L)
            pps = rawPacketsProcessed / timeDiff
        }

        rawPacketsProcessed++
        Log.d(TAG, "onSensorRawData() ${rawData.data.toHexString()}, pps = $pps")
    }

    private fun initializeSdk() {
        PlayfinitySDKBuilder()
            .addCallback(this)
            .enableLogging(true)
            .enableMultiSensorMode(false)
            .build(application, Baseball)
    }

    private fun startScanningWithPermissions() {
        when {
            !isBluetoothEnabled() -> {
                Toast.makeText(this, "Enable Bluetooth", Toast.LENGTH_LONG).show()
            }

            isLocationRequired() && !isLocationEnabled() -> {
                Toast.makeText(this, "Enable GPS", Toast.LENGTH_LONG).show()
            }

            !hasLocationPermission() -> {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && VERSION.SDK_INT <= Build.VERSION_CODES.P -> {
                        requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }

                    Build.VERSION.SDK_INT > Build.VERSION_CODES.P && VERSION.SDK_INT < Build.VERSION_CODES.S -> {
                        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }

                    else -> {}
                }
                Toast.makeText(this, "Grant Location permissions", Toast.LENGTH_LONG).show()
            }

            !hasBluetoothPermission() -> {
                Toast.makeText(this, "Grant Bluetooth permissions", Toast.LENGTH_LONG).show()
            }

            else -> attemptBleScanning()
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startScanningWithPermissions()
            }
        }

    private fun attemptBleScanning() {
        val playfinitySdk = playfinitySDK

        // Stop service if BLE scanning is not possible.
        if (!hasAllRequiredBlePermissionsAndServices()) {
            Log.w(TAG, "Permissions not granted. Skipping BLE scanning.")
            return
        }

        // Skip scanning if SDK is not ready.
        if (playfinitySdk == null) {
            Log.w(TAG, "Playfinity SDK not ready. Skipping BLE scanning.")
            return
        }

        // Skip scanning if it already started.
        if (isScanningInProgress || isScanningInitializing) {
            Log.w(TAG, "Skipping BLE initialization because it's already scanning.")
            return
        }

        isScanningInitializing = true

        playfinitySdk
            .getBluetoothManager()
            .startScanner(false)
            .let {
                Log.i(TAG, "Bluetooth LE scanner: STARTED")
                isScanningInProgress = true
            }
    }

    private fun stopBleScanning() {
        val playfinitySdk = playfinitySDK ?: return

        isScanningInitializing = false

        playfinitySdk
            .getBluetoothManager()
            .stopScanner()
            .let {
                Log.d(TAG, "Bluetooth LE scanner: STOPPED")
                isScanningInProgress = false
            }
    }

    private fun observeSensors() {
        playfinitySDK
            ?.getBluetoothManager()
            ?.addSensorDiscoverListener(this)
    }

    private fun observeSensorEvents(sensor: Sensor) {
        sensor.apply {
            unSubscribeEvents(this@SmartBallActivity)
            subscribeToEvents(this@SmartBallActivity)
        }
    }

    private fun observeSensorRawData(sensor: Sensor) {
        sensor.apply {
            unSubscribeRawData(this@SmartBallActivity)
            subscribeToRawData(this@SmartBallActivity)
        }
    }
}
