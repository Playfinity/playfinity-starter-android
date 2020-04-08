package register.before.production.android.ui

import android.os.Bundle
import androidx.recyclerview.widget.DividerItemDecoration
import io.playfinity.sdk.SensorEvent
import io.playfinity.sdk.SensorEventType
import io.playfinity.sdk.device.Sensor
import io.playfinity.sdk.errors.PlayfinityThrowable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import register.before.production.android.App
import register.before.production.android.AppType
import register.before.production.android.BuildConfig
import register.before.production.android.R
import register.before.production.android.sound.SoundSkin
import register.before.production.android.ui.adapter.EventLogAdapter
import register.before.production.android.ui.adapter.EventLogEntry
import timber.log.Timber


class MainActivity : BaseSensorActivity() {

    //region Lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        setupUi()

        printDebugInfo()
    }

    override fun onPause() {
        super.onPause()

        if (isFinishing) {
            recycleSensor()
        }
    }

    //endregion

    //region Ui

    private fun setupUi() {

        // Prepare log adapter.
        setupLogAdapter()

        // Reset Ui.
        setupSensorUi(null)
    }

    private fun setupSensorUi(sensor: Sensor?) {
        val naLabel = getString(R.string.common_not_available)
        sensorNameView?.text =
                getString(R.string.label_sensor_name, sensor?.givenName ?: naLabel)
        sensorMacView?.text =
                getString(R.string.label_sensor_mac, sensor?.macAddress ?: naLabel)
        sensorFirmwareView?.text =
                getString(R.string.label_sensor_firmware, sensor?.firmwareVersion ?: naLabel)
    }

    private fun setupEventCounter(label: String) {
        counterView?.text = getString(R.string.label_counter, label)
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
            processEvent(event)
            processEventSound(event)
        }
    }

    private fun processEvent(event: SensorEvent) {
        when (App.getAppType()) {
            AppType.Ball -> {
                processBallEvents(event)
            }
            AppType.Trampoline -> {
                processTrampolineEvents(event)
            }
            AppType.Trix -> {
                processTrixEvents(event)
            }
        }
    }

    private fun processBallEvents(event: SensorEvent) {
        if (event.eventType == SensorEventType.Thrown) {
            throws++
            eventLogAdapter.clear()
        }

        if (event.eventType == SensorEventType.Thrown
                || event.eventType == SensorEventType.Bounce
                || event.eventType == SensorEventType.Caught
                || event.eventType == SensorEventType.Miss) {

            // Build and insert log entry.
            val eventRow = buildLogRow(event)
            eventRow?.let {
                eventLogAdapter.insertRow(it)
            }
        }

        setupEventCounter(getString(R.string.counter_throws, throws))
    }

    private fun processTrampolineEvents(event: SensorEvent) {
        if (event.eventType == SensorEventType.Jump) {
            jumps++
            eventLogAdapter.clear()
        }

        if (event.eventType == SensorEventType.Jump
                || event.eventType == SensorEventType.Land) {

            // Build and insert log entry.
            val eventRow = buildLogRow(event)
            eventRow?.let {
                eventLogAdapter.insertRow(it)
            }
        }

        setupEventCounter(getString(R.string.counter_jumps, jumps))
    }

    private fun processTrixEvents(event: SensorEvent) {
        if (event.eventType == SensorEventType.Kick) {
            kicks++
            eventLogAdapter.clear()
        }

        if (event.eventType == SensorEventType.Kick
                || event.eventType == SensorEventType.Bounce
                || event.eventType == SensorEventType.Miss) {

            // Build and insert log entry.
            val eventRow = buildLogRow(event)
            eventRow?.let {
                eventLogAdapter.insertRow(it)
            }
        }

        setupEventCounter(getString(R.string.counter_kicks, kicks))
    }

    private fun processEventSound(event: SensorEvent) {
        soundSkin.let { skin ->
            val elements = mutableListOf(
                    skin.eventSound(event))

            val sounds = listOfNotNull(*elements.toTypedArray())
            sounds.distinct().forEach { skin.play(it) }

            skin.eventSpeech(event)
        }
    }

    //endregion

    //region Adapter

    private val eventLogAdapter = EventLogAdapter(mutableListOf())

    private fun setupLogAdapter() {
        eventLogList.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        eventLogList.adapter = eventLogAdapter
    }

    private fun buildLogRow(event: SensorEvent): EventLogEntry? {
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

    //endregion

    //region Sound

    private val soundSkin: SoundSkin by lazy {
        App.getApp(this).getSoundSkin()
    }

    //endregion

    //region Debug

    private fun printDebugInfo() {
        Timber.i("================================")
        Timber.i("Type: ${App.getAppType().name}")
        Timber.i("Id: ${BuildConfig.APPLICATION_ID}")
        Timber.i("Flavor: ${BuildConfig.FLAVOR}")
        Timber.i("Version: ${BuildConfig.VERSION_NAME}")
        Timber.i("Playfinity API URL: ${getString(R.string.PFI_API_URL)}")
        Timber.i("Cloud API URL: ${getString(R.string.PFI_CLOUD_API_URL)}")
        Timber.i("================================")
    }

    //endregion

    //region Companion

    private var throws = 0
    private var jumps = 0
    private var kicks = 0

    companion object {
        //No-op.
    }

    //endregion
}
