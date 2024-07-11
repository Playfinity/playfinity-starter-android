package com.playfinity.recorder.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.Build.VERSION
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.playfinity.recorder.R
import com.playfinity.recorder.presentation.adapter.EventLogAdapter
import com.playfinity.recorder.presentation.adapter.EventLogEntry
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
import java.lang.Long.max

@SuppressLint("SetTextI18n")
class SmartBallActivity : AppCompatActivity(),
    PFICallback, DiscoverSensorListener, SensorEventsSubscriber, SensorRawDataSubscriber {

    private companion object {
        const val SUBSCRIBE_TO_RAW_DATA = false
    }

    private var rawPacketsProcessed = 0L
    private var rawPacketsTimestamp = 0L
    private var isScanningInitializing = false
    private var isScanningInProgress = false
    private var playfinitySDK: PlayfinitySDK? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_smartball)

        setupLogAdapter()
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

        findViewById<TextView>(R.id.sdkStatusView).text = "Status: Connected"

        startScanningWithPermissions()
        observeSensors()
    }

    override fun onPlayfinityError(pfiThrowable: PFIThrowable) {
        findViewById<TextView>(R.id.sdkStatusView).text = "Status: Error (${pfiThrowable.message})"
    }

    override fun onSensorDiscovered(sensor: Sensor) {
        findViewById<TextView>(R.id.sensorNameView).text = "Id: ${sensor.sensorId}"
        findViewById<TextView>(R.id.sensorFirmwareView).text = "Firmware: ${sensor.firmwareVersion}"
        findViewById<TextView>(R.id.sensorMacView).text = "MAC: ${sensor.macAddress}"

        when (SUBSCRIBE_TO_RAW_DATA) {
            true -> observeSensorRawData(sensor)
            else -> observeSensorEvents(sensor)
        }
    }

    override fun onSensorDiscoverError(pfiThrowable: PFIThrowable) {
        val message = pfiThrowable.cause?.localizedMessage
        findViewById<TextView>(R.id.sensorNameView).text = "Error: $message"
        findViewById<TextView>(R.id.sensorFirmwareView).text = "-"
        findViewById<TextView>(R.id.sensorMacView).text = "-"
    }

    override fun onSensorEvent(event: SensorEvent) {
        if (event.eventType != SensorEventType.Inair
            && event.eventType != SensorEventType.Jumping
        ) {
            eventLogAdapter.insertRow(buildLogRow(event))

            val rv = findViewById<RecyclerView>(R.id.eventLogList)
            rv.post {
                rv.scrollToPosition(eventLogAdapter.itemCount - 1)
            }
        }
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
            return
        }

        // Skip scanning if SDK is not ready.
        if (playfinitySdk == null) {
            return
        }

        // Skip scanning if it already started.
        if (isScanningInProgress || isScanningInitializing) {
            return
        }

        isScanningInitializing = true

        playfinitySdk
            .getBluetoothManager()
            .startScanner(false)
            .let { isScanningInProgress = true }
    }

    private fun stopBleScanning() {
        val playfinitySdk = playfinitySDK ?: return

        isScanningInitializing = false

        playfinitySdk
            .getBluetoothManager()
            .stopScanner()
            .let { isScanningInProgress = false }
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

    private val eventLogAdapter = EventLogAdapter(mutableListOf())

    private fun setupLogAdapter() {
        val rv = findViewById<RecyclerView>(R.id.eventLogList)

        rv.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        rv.adapter = eventLogAdapter
    }

    private fun buildLogRow(event: SensorEvent): EventLogEntry {
        val params = when (event.eventType) {
            SensorEventType.Thrown -> {
                "Speed ${event.speedKmh} km/h"
            }

            SensorEventType.Bounce,
            SensorEventType.Caught,
            SensorEventType.Miss -> {
                "Height ${event.heightBallEvent} cm, Airtime ${event.airTimeMilliseconds} ms"
            }

            SensorEventType.Jump -> {
                ""
            }

            SensorEventType.Land -> {
                "Rotation ${event.yawRotation}\u00B0, Pitch ${event.pitchRotation}\u00B0"
            }

            SensorEventType.Kick -> {
                "Speed ${event.speedKmh} km/h"
            }

            else -> {
                ""
            }
        }

        return EventLogEntry(event.eventType.name, params)
    }
}
