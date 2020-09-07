package register.before.production.android.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import io.playfinity.sdk.PlayfinitySDK
import io.playfinity.sdk.core.callback.DiscoverSensorListener
import io.playfinity.sdk.core.device.Sensor
import io.playfinity.sdk.core.device.SensorEvent
import io.playfinity.sdk.core.device.SensorEventsSubscriber
import io.playfinity.sdk.utility.error.PFIThrowable
import register.before.production.android.App
import register.before.production.android.extension.hasAllRequiredBlePermissionsAndServices
import register.before.production.android.extension.hasLocationPermission
import register.before.production.android.extension.isBluetoothEnabled
import register.before.production.android.extension.isLocationEnabled
import timber.log.Timber


abstract class BaseSensorActivity : AppCompatActivity(),
        DiscoverSensorListener,
        SensorEventsSubscriber {

    //region Lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Observe BT state.
        registerBleStateReceiver()

        // Observe SDK.
        observePfiSdk()
    }

    override fun onPause() {
        super.onPause()

        // Stop BT scanning.
        onBleStop()

        if (isFinishing) {
            unregisterBleReceiver()

            // Register sensor discover callback.
            pfiSdk?.getBluetoothManager()?.removeSensorDiscoverListener(this)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_ENABLE_BT -> {
                if (isBluetoothEnabled()) {
                    onBleReady()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onBleReady()
                }
            }
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    //endregion

    //region Playfinity

    private var pfiSdk: PlayfinitySDK? = null

    private fun observePfiSdk() {

        onPifSdkLoadingStatus(PFI_STATUS_LOADING)

        App.getApp(this)
                .getPfiSdkLiveData()
                .observe(this, Observer {
                    onPifSdkChanged(it)
                })
    }

    private fun onPifSdkChanged(sdk: PlayfinitySDK?) {
        this.pfiSdk = sdk

        if (sdk != null) {

            // Register sensor discover callback.
            sdk.getBluetoothManager().addSensorDiscoverListener(this)

            // Start discovering process by checking
            // location permission and BT availability.
            onBleReady()

            onPifSdkLoadingStatus(PFI_STATUS_SUCCESS)

            // Log SDK initialize status.
            Timber.v("Playfinity SDK successfully initialized.")

        } else {

            onPifSdkLoadingStatus(PFI_STATUS_ERROR)

            // Log SDK initialize status.
            Timber.e("Failed to initialize Playfinity SDK.")
        }
    }

    abstract fun onPifSdkLoadingStatus(status: Int)

    fun clearSensorCache() {
        pfiSdk?.getBluetoothManager()?.clearSensorsCache()
    }

    //endregion

    //region Sensor

    abstract override fun onSensorDiscovered(sensor: Sensor)

    abstract override fun onSensorDiscoverError(pfiThrowable: PFIThrowable)

    abstract override fun onSensorEvent(event: SensorEvent)

    //endregion

    //region Bluetooth

    private fun registerBleStateReceiver() {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bleStateReceiver, filter)
    }

    private val bleStateReceiver: BroadcastReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val action = intent.action

                    if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                        val state = intent.getIntExtra(
                                BluetoothAdapter.EXTRA_STATE,
                                BluetoothAdapter.ERROR
                        )
                        when (state) {
                            BluetoothAdapter.STATE_ON -> {

                                // Start discovering process by checking
                                // location permission and BT availability.
                                onBleReady()
                            }
                            BluetoothAdapter.STATE_OFF -> {
                                clearSensorCache()
                            }
                        }
                    }
                }
            }

    private fun unregisterBleReceiver() = try {
        unregisterReceiver(bleStateReceiver)
    } catch (e: IllegalArgumentException) {
        // No-op.
    }

    private fun canStartBleScanner(): Boolean {
        return hasAllRequiredBlePermissionsAndServices() && !isScanning
    }

    fun onBleReady() {

        // Conditionally start BLE scanning.
        if (canStartBleScanner()) {
            startBleScanning(findOnlyOadDevices)
        }

        // Otherwise, check for missing permissions or services.
        else {
            when {
                !isBluetoothEnabled() -> {

                    // Show a system activity that allows the user to turn on Bluetooth.
                    startActivityForResult(
                            Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT)

                    Timber.v("Bluetooth disabled. Requesting user to turn it on.")

                    return
                }
                !isLocationEnabled() -> {

                    // Show a system activity that allows the user to turn on GPS.
                    startActivity(
                            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))

                    Timber.v("Location services disabled. Requesting user to turn GPS on.")

                    return
                }
                !hasLocationPermission() -> {

                    Timber.v("Location permission not granted. Requesting user to grant location permission.")

                    // Note: If your app targets Android 9 (API level 28) or lower, you can declare the
                    // ACCESS_COARSE_LOCATION permission instead of the ACCESS_FINE_LOCATION permission.
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                        ActivityCompat.requestPermissions(this,
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                REQUEST_LOCATION_PERMISSION)
                    } else {
                        ActivityCompat.requestPermissions(this,
                                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                                REQUEST_LOCATION_PERMISSION)
                    }

                    return
                }
            }
        }
    }

    private fun onBleStop() {
        stopBleScanning()
    }

    private var isScanning = false

    private fun startBleScanning(findOnlyOadDevices: Boolean) {

        // Log scanning start.
        Timber.v("Starting BLE scanning...")

        // Start BLE scanning.
        pfiSdk?.getBluetoothManager()
                ?.run {
                    isScanning = true
                    startScanner(findOnlyOadDevices)
                }
    }

    private fun stopBleScanning() {

        // Log scanning end.
        Timber.v("BLE scanning stopped.")

        // Stop BLE scanning.
        pfiSdk?.getBluetoothManager()
                ?.run {
                    isScanning = false
                    stopScanner()
                }
    }

    //endregion

    //region Config

    private var findOnlyOadDevices: Boolean = false

    companion object {

        private const val REQUEST_ENABLE_BT = 1000
        private const val REQUEST_LOCATION_PERMISSION = 2000

        const val PFI_STATUS_LOADING = 1
        const val PFI_STATUS_SUCCESS = 2
        const val PFI_STATUS_ERROR = 3
    }

    //endregion
}
