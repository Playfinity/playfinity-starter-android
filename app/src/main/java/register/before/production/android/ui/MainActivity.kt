package register.before.production.android.ui

import android.os.Bundle
import io.playfinity.sdk.SensorEvent
import io.playfinity.sdk.device.Sensor
import io.playfinity.sdk.errors.PlayfinityThrowable
import register.before.production.android.AppExecutors
import register.before.production.android.R
import timber.log.Timber


class MainActivity : BaseSensorActivity() {

    //region Lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        setupUi()
    }

    override fun onStop() {
        super.onStop()

        recycle()
    }

    private fun recycle() {
        if (isFinishing) {
            activeSensor?.let { unSubscribeToSensorEvents(it) }
        }
    }

    //endregion

    //region Ui

    private fun setupUi() {

    }

    //endregion

    //region Sensor

    private var activeSensor: Sensor? = null

    override fun onSensorDiscovered(sensor: Sensor) {
        activeSensor = sensor

        subscribeToSensorEvents(sensor)

        Timber.d("onSensorDiscovered: ${sensor.givenName}")
    }

    override fun onSensorDiscoverError(playfinityThrowable: PlayfinityThrowable) {
        Timber.e("onSensorDiscoverError: ${playfinityThrowable.type}")
    }

    override fun onSensorEvent(event: SensorEvent) {
        Timber.d("onSensorEvent: $event")
    }

    private fun subscribeToSensorEvents(sensor: Sensor) {
        sensor.subscribeToEvents(this)
    }

    private fun unSubscribeToSensorEvents(sensor: Sensor) {
        sensor.unSubscribeEvents(this)
    }

    //endregion

    //region Companion

    companion object {
        val executors = AppExecutors()
    }

    //endregion
}
