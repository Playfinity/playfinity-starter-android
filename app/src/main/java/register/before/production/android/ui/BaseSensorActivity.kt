package register.before.production.android.ui

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import io.playfinity.sdk.PlayfinitySDK
import io.playfinity.sdk.SensorEvent
import io.playfinity.sdk.callbacks.DiscoverSensorListener
import io.playfinity.sdk.device.Sensor
import io.playfinity.sdk.device.SensorEventsSubscriber
import io.playfinity.sdk.errors.PlayfinityThrowable
import register.before.production.android.App
import register.before.production.android.extension.hasAllRequiredBlePermissionsAndServices
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

    override fun onResume() {
        super.onResume()

        // Start discovering process by checking
        // location permission and BT availability.
        onBleReady()
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

    //endregion

    //region Playfinity

    private var pfiSdk: PlayfinitySDK? = null

    private fun observePfiSdk() {
        App.getApp(this)
                .getPfiSdkLiveData()
                .observe(this, Observer { sdk ->
                    sdk?.let {
                        onPifSdkChanged(it)
                    }
                })
    }

    open fun onPifSdkChanged(sdk: PlayfinitySDK) {
        this.pfiSdk = sdk

        // Register sensor discover callback.
        sdk.getBluetoothManager().addSensorDiscoverListener(this)

        // Start discovering process by checking
        // location permission and BT availability.
        onBleReady()

        // Notify.
        onPfiSdkInitialized(sdk)
    }

    open fun onPfiSdkInitialized(sdk: PlayfinitySDK) {

    }

    fun clearSensorCache() {
        pfiSdk?.getBluetoothManager()?.clearSensorsCache()
    }

    //endregion

    //region Sensor

    abstract override fun onSensorDiscovered(sensor: Sensor)

    abstract override fun onSensorDiscoverError(playfinityThrowable: PlayfinityThrowable)

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
                                Timber.d("Clearing sensors cache.")
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

        // Check required scanning permissions.
        if (!canStartBleScanner()) {
            Timber.v("Cannot start BLE scanner. Has all required permissions: ${hasAllRequiredBlePermissionsAndServices()}")
            return
        }

        startBleScanning(findOnlyOadDevices)
    }

    private fun onBleStop() {
        stopBleScanning()
    }

    private var isScanning = false

    private fun startBleScanning(findOnlyOadDevices: Boolean) {

        // Log scanning start.
        Timber.v("Starting BLE scanning...")

        pfiSdk?.getBluetoothManager()
                ?.run {
                    isScanning = true
                    startScanner(findOnlyOadDevices)
                }
    }

    private fun stopBleScanning() {

        // Log scanning start.
        Timber.v("BLE scanning stopped.")

        pfiSdk?.getBluetoothManager()
                ?.run {
                    isScanning = false
                    stopScanner()
                }
    }

    //endregion

    //region Config

    protected var findOnlyOadDevices: Boolean = false

    companion object {
        //No-op.
    }

    //endregion
}
