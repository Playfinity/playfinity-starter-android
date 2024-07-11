package io.playfinity.sdk.sample

import android.Manifest
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
import android.os.Build
import android.os.Build.VERSION
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AppCompatActivity
import io.playfinity.sdk.sample.Utils.toHexString

@SuppressLint("MissingPermission")
class MainActivity : AppCompatActivity() {

    private companion object {
        private const val TAG = "MainActivity"
    }

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var bluetoothGatt: BluetoothGatt? = null

    private var isScanningInitializing = false
    private var isScanningInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        init()
        startScanningWithPermissions()
    }

    override fun onResume() {
        super.onResume()

        startScanningWithPermissions()
    }

    override fun onStop() {
        super.onStop()

        stopBleScanning()
    }

    private fun init() {

        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager

        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
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
                    VERSION.SDK_INT >= Build.VERSION_CODES.M && VERSION.SDK_INT <= Build.VERSION_CODES.P -> {
                        requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }

                    VERSION.SDK_INT > Build.VERSION_CODES.P && VERSION.SDK_INT < Build.VERSION_CODES.S -> {
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

    private fun connectToDevice(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
    }

    @Suppress("DEPRECATION")
    private fun setCharacteristicNotification(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        enabled: Boolean
    ) {
        gatt.setCharacteristicNotification(characteristic, enabled)

        val descriptor: BluetoothGattDescriptor? =
            characteristic.getDescriptor(SensorDescriptors.notifications)

        descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        gatt.writeDescriptor(descriptor)
    }

    private fun processBluetoothData(data: ByteArray) {
        Log.i(TAG, "processBluetoothData: ${data.toHexString()}")
    }

    private val requestPermissionLauncher =
        registerForActivityResult(RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startScanningWithPermissions()
            }
        }

    private fun attemptBleScanning() {

        // Stop service if BLE scanning is not possible.
        if (!hasAllRequiredBlePermissionsAndServices()) {
            Log.w(TAG, "Permissions not granted. Skipping BLE scanning.")
            return
        }

        // Skip scanning if it already started.
        if (isScanningInProgress || isScanningInitializing) {
            Log.w(TAG, "Skipping BLE initialization because it's already scanning.")
            return
        }

        isScanningInitializing = true

        val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        bluetoothLeScanner?.startScan(bleCallback)
    }

    private fun stopBleScanning() {
        isScanningInitializing = false

        // Stop service if BLE scanning is not possible.
        if (!hasAllRequiredBlePermissionsAndServices()) {
            Log.w(TAG, "Permissions not granted.")
            return
        }

        bluetoothLeScanner?.stopScan(bleCallback)
    }

    private val bleCallback: ScanCallback = object : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device: BluetoothDevice = result.device
            Log.i(TAG, "Device found: ${device.name}")

            if (Utils.isPlayfinityDevice(device.name)) {
                Log.i(TAG, "Found football device: ${device.name}")

                stopBleScanning()
                connectToDevice(device)
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server")
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

                gatt.services.forEach { service ->
                    service.characteristics.forEach { char ->
                        Log.i(TAG, "${service.uuid}, ${char.uuid}")
                    }
                }

                val service: BluetoothGattService? = gatt.getService(SensorServices.movement)

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
}
