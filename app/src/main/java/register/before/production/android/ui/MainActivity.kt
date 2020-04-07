package register.before.production.android.ui

import android.os.Bundle
import io.playfinity.sdk.SensorEvent
import io.playfinity.sdk.SensorEventType
import io.playfinity.sdk.device.Sensor
import io.playfinity.sdk.errors.PlayfinityThrowable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import register.before.production.android.AppExecutors
import register.before.production.android.R
import timber.log.Timber


class MainActivity : BaseSensorActivity() {

    //region Lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)
    }

    override fun onPause() {
        super.onPause()

        if (isFinishing) {
            recycleSensor()
        }
    }

    //endregion

    //region Ui

    private fun setupSensorUi(sensor: Sensor) {
        sensorNameView?.text = getString(R.string.label_sensor_name, sensor.givenName)
        sensorMacView?.text = getString(R.string.label_sensor_mac, sensor.macAddress)
        sensorFirmwareView?.text = getString(R.string.label_sensor_firmware, sensor.firmwareVersion)
    }

    private fun setupEventUi(event: SensorEvent) {
        lastEventTypeView?.text = getString(R.string.label_event_type, event.eventType.name)
    }

    override fun onPifSdkLoadingStatus(status: Int) {
        sdkStatusView?.text =
                when (status) {
                    PFI_STATUS_LOADING -> {
                        getString(R.string.label_sdk_connecting)
                    }
                    PFI_STATUS_SUCCESS -> {
                        getString(R.string.label_sdk_connected)
                    }
                    PFI_STATUS_ERROR -> {
                        getString(R.string.label_sdk_not_connected)
                    }
                    else -> ""
                }
    }

    //endregion

    //region Sensor

    private var activeSensor: Sensor? = null

    override fun onSensorDiscovered(sensor: Sensor) {
        activeSensor = sensor

        setupSensor(sensor)
    }

    override fun onSensorDiscoverError(playfinityThrowable: PlayfinityThrowable) {
        Timber.e("onSensorDiscoverError: ${playfinityThrowable.type}")
    }

    private fun setupSensor(sensor: Sensor) {
        setupSensorUi(sensor)

        sensor.subscribeToEvents(this)
    }

    private fun recycleSensor() {
        activeSensor?.unSubscribeEvents(this)
    }

    //endregion

    //region Event

    override fun onSensorEvent(event: SensorEvent) {
        if (event.eventType != SensorEventType.Inair) {
            Timber.d("onSensorEvent: $event")
            setupEventUi(event)
            processEventSound(event)
        }
    }

    //endregion

    //region Sound

    private fun processEventSound(event: SensorEvent) {

    }

    //endregion

    //region Companion

    companion object {
        val executors = AppExecutors()
    }

    //endregion
}
