package com.playfinity.recorder.presentation

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.playfinity.recorder.R
import com.playfinity.recorder.presentation.adapter.EventLogAdapter
import com.playfinity.recorder.presentation.adapter.EventLogEntry
import com.playfinity.recorder.utils.SensorCharacteristics
import com.playfinity.recorder.utils.SensorDescriptors
import com.playfinity.recorder.utils.SensorServices
import com.playfinity.recorder.utils.Utils
import com.playfinity.recorder.utils.Utils.toHexString
import com.playfinity.recorder.utils.hasAllRequiredBlePermissionsAndServices

@SuppressLint("MissingPermission")
class FootballActivity : PlayfinityActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var bluetoothGatt: BluetoothGatt? = null

    private var isScanningInitializing = false
    private var isScanningInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_football)
        setupLogAdapter()
        init()
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsAndServices()
    }

    override fun onStop() {
        super.onStop()

        stopBleScanning()
    }

    override fun onReadyToScan() {

        // Skip scanning if it already started.
        if (isScanningInProgress || isScanningInitializing) {
            return
        }

        isScanningInitializing = true

        addLog("Starting BLE scanner.")

        val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        bluetoothLeScanner?.startScan(bleCallback)
    }

    private fun init() {
        addLog("Initializing BLE adapter...")

        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager

        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    }

    @SuppressLint("SetTextI18n")
    private fun connectToDevice(device: BluetoothDevice) {
        addLog("Connecting to ${device.name}")

        findViewById<TextView>(R.id.sensorNameView).text = "Firmware: ${device.name}"
        findViewById<TextView>(R.id.sensorMacView).text = "MAC: ${device.address}"
        findViewById<TextView>(R.id.sensorRawDataView).text = ""

        bluetoothGatt = device.connectGatt(this, false, gattCallback)
    }

    @Suppress("DEPRECATION")
    private fun setCharacteristicNotification(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        enabled: Boolean
    ) {
        addLog("Enabling notifications...")

        gatt.setCharacteristicNotification(characteristic, enabled)

        val descriptor: BluetoothGattDescriptor? =
            characteristic.getDescriptor(SensorDescriptors.notifications)

        descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        gatt.writeDescriptor(descriptor)
    }

    private fun processBluetoothData(data: ByteArray) {
        val rawData = data.toHexString()

        runOnUiThread {
            findViewById<TextView>(R.id.sensorRawDataView).text = rawData
        }
    }

    private fun stopBleScanning() {
        isScanningInitializing = false

        // Stop service if BLE scanning is not possible.
        if (!hasAllRequiredBlePermissionsAndServices()) {
            return
        }

        addLog("BLE scanner stopped.")

        bluetoothLeScanner?.stopScan(bleCallback)
    }

    private val bleCallback: ScanCallback = object : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device: BluetoothDevice = result.device

            if (Utils.isPlayfinityDevice(device.name)) {
                addLog("Found Playfinity device.")
                stopBleScanning()
                connectToDevice(device)
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                addLog("Device connected.")
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                addLog("Device disconnected.")
                onReadyToScan()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

                val service: BluetoothGattService? = gatt.getService(SensorServices.movement)

                addLog("Services discovered.")

                service?.let {
                    val characteristic: BluetoothGattCharacteristic? =
                        it.getCharacteristic(SensorCharacteristics.movementData)
                    characteristic?.let { char ->
                        setCharacteristicNotification(gatt, char, true)
                    }
                }
            }
        }

        @Deprecated("Deprecated in Java")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            characteristic?.value?.let { processBluetoothData(it) }
        }
    }

    private val eventLogAdapter = EventLogAdapter(mutableListOf())

    private fun setupLogAdapter() {
        val rv = findViewById<RecyclerView>(R.id.eventLogList)

        rv.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        rv.adapter = eventLogAdapter
    }

    private fun addLog(title: String, desc: String = "") {
        runOnUiThread {
            eventLogAdapter.insertRow(EventLogEntry(title, desc))

            val rv = findViewById<RecyclerView>(R.id.eventLogList)
            rv.post {
                rv.scrollToPosition(eventLogAdapter.itemCount - 1)
            }
        }
    }
}
