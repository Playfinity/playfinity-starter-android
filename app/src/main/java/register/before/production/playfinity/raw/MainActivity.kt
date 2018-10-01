package register.before.production.playfinity.raw

import android.Manifest
import android.arch.lifecycle.Observer
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.transition.AutoTransition
import android.support.transition.TransitionManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import io.playfinity.sdk.PlayfinitySDK
import io.playfinity.sdk.bluetooth.BluetoothDataRaw
import io.playfinity.sdk.bluetooth.PFIBluetoothManager
import io.playfinity.sdk.callbacks.DiscoverSensorListener
import io.playfinity.sdk.device.Sensor
import io.playfinity.sdk.device.SensorRawDataSubscriber
import io.playfinity.sdk.errors.PlayfinityThrowable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.sensor_raw_data_list_view.*
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(),
        DiscoverSensorListener,
        SensorRawDataSubscriber {

    private var playfinitySDK: PlayfinitySDK? = null
    private val pfiBluetoothManager: PFIBluetoothManager?
        get() = playfinitySDK?.getBluetoothManager()

    private var sensor: Sensor? = null
    private var isConnectedToSensor = false
    private val adapter = RawDataListAdapter()

    private val handler = Handler()
    private val updateListTask = Runnable { updateListTaskJob() }
    private val rawDataItems = mutableListOf<RawDataListItem>()

    private var counter: Int = 0
        get() {
            field += 1
            executors.mainThread().execute {
                samples_tv.text = samples_text.format(field)
            }
            return field
        }
        set(value) {
            field = value
            executors.mainThread().execute {
                samples_tv.text = samples_text.format(field)
            }
        }
    private var collectRecords = true

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
                            BluetoothAdapter.STATE_ON -> onBleReady()
                        }
                    }
                }
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        status_tv.text = "Loading Playfinity SDK"
        App.getApp(this).getPlayfinitySdkLiveData().observe(this, Observer { playfinitySDK ->
            if (playfinitySDK != null) {
                this.playfinitySDK = playfinitySDK
                status_tv.text = "Playfinity SDK is Ready"
                handelSensorDiscovery()
            } else {
                status_tv.text = "Loading Playfinity SDK"
            }
        })


        setupUi()
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterBleReceiver()
    }

    override fun onResume() {
        super.onResume()

        if (canStartBleScanner()) {
            onBleReady()
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterBallScanner()
        handler.removeCallbacks(updateListTask)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            SHARE_REQUEST -> {
                collectRecords = true
                fab.isEnabled = true
                record_btn.isEnabled = true
                record_btn.text = "Record"
                send_btn.isEnabled = false
                progress_bar.visibility = View.GONE
                clearRecords()

                sensor?.subscribeToRawData(this)
            }
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onLocationPermissionGranted()
                } else {
                    onLocationPermissionDenied()
                }
            }
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    override fun onSensorDiscovered(sensor: Sensor) {
        pfiBluetoothManager?.removeSensorDiscoverListener(this)
        TransitionManager.beginDelayedTransition(root, AutoTransition().apply { duration = 200 })
        status_tv.text = "Sensor Found!"
        this.sensor = sensor
        sensor_header_layout.visibility = View.VISIBLE
        separator_1.visibility = View.VISIBLE
        sensor_name_tv.text = "Name: %s".format(sensor.givenName)
        sensor_firmware_tv.text = "Firmware: %s".format(sensor.firmwareVersion)
        counter = 0

        raw_data_list.visibility = View.VISIBLE
        fab.show()
    }

    override fun onSensorDiscoverError(playfinityThrowable: PlayfinityThrowable) {

    }

    override fun onSensorRawData(rawData: BluetoothDataRaw) {
        if (!collectRecords) {
            return
        }

        executors.workerThread().execute {
            rawDataItems.add(RawDataListItem(counter, rawData))
        }
    }

    private fun updateListTaskJob(postNext: Boolean = true) {
        adapter.submit(rawDataItems)
        (raw_data_list.layoutManager as LinearLayoutManager).scrollToPosition(adapter.itemCount - 1)
        rawDataItems.clear()

        if (postNext) {
            handler.postDelayed(updateListTask, updateListInterval)
        }
    }

    private fun setupUi() {
        fab.setOnClickListener {
            val foundSensor = sensor
            val bluetoothManager = pfiBluetoothManager
            if (foundSensor == null || bluetoothManager == null) {
                return@setOnClickListener
            }

            isConnectedToSensor = !isConnectedToSensor

            fab.setImageResource(if (isConnectedToSensor) R.drawable.ic_bluetooth_connected_white_24dp else R.drawable.ic_bluetooth_disabled_white_24dp)
            record_btn.isEnabled = isConnectedToSensor
            record_btn.text = "Record"
            send_btn.isEnabled = false
            list_header_group.visibility = View.GONE

            if (isConnectedToSensor) {
                status_tv.text = "Subscribed to Raw data"
                foundSensor.subscribeToRawData(this)
            } else {
                status_tv.text = "Unsubscribed Raw data"
                foundSensor.unSubscribeRawData(this)
                updateListTaskJob(false)
            }
        }

        val dividerItemDecoration = DividerItemDecoration(this, LinearLayoutManager.VERTICAL)

        raw_data_list.addItemDecoration(dividerItemDecoration)
        raw_data_list.adapter = adapter

        handler.postDelayed(updateListTask, updateListInterval)

        val header = RawDataListItem(0)
        ax.text = header.ax
        ay.text = header.ay
        az.text = header.az

        gx.text = header.gx
        gy.text = header.gy
        gz.text = header.gz

        baro.text = header.baro
        time.text = header.sensortime

        record_btn.setOnClickListener {
            record_btn.text = "Clear"
            clearRecords()
            send_btn.isEnabled = true
            list_header_group.visibility = View.VISIBLE
        }

        send_btn.setOnClickListener {
            //generate .txt from records
            collectRecords = false
            record_btn.isEnabled = false
            send_btn.isEnabled = false
            fab.isEnabled = false

            progress_bar.visibility = View.VISIBLE
            generateLogFile()
        }

        list_header_group.visibility = View.GONE
    }

    private fun generateLogFile() {
        try {
            val rawDataItems = adapter.getItems()
            val header = RawDataListItem(0)

            val records = mutableListOf<RawDataListItem>()
            records.add(header)
            records.addAll(rawDataItems)
            val logBuilder = createLogBuilder(records)
            val filecontents = logBuilder.toString().replace(".", ",")

            val imagePath = File(cacheDir, "files")
            imagePath.mkdirs()
            val file = File(imagePath, "logs.csv")
            val writer = FileWriter(file)
            writer.append(filecontents)
            writer.flush()
            writer.close()

            val logsURI = FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", file)
            shareLogs(logsURI)
        } catch (e: IOException) {
            //ignore
            fab.isEnabled = true
            record_btn.isEnabled = true
            record_btn.text = "Record"
            send_btn.isEnabled = false
            progress_bar.visibility = View.GONE
            clearRecords()
        }
    }

    private fun createLogBuilder(records: List<RawDataListItem>): StringBuilder {
        val logBuilder = StringBuilder()

        records.forEach {
            logBuilder
                    .append(it.ax)
                    .append(';')
                    .append(it.ay)
                    .append(';')
                    .append(it.az)
                    .append(';')
                    .append(it.gx)
                    .append(';')
                    .append(it.gy)
                    .append(';')
                    .append(it.gz)
                    .append(';')
                    .append(it.baro)
                    .append(';')
                    .append(it.sensortime)
                    .append(';')
                    .append('\n')
        }

        return logBuilder
    }

    private fun shareLogs(logsURI: Uri) {
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        shareIntent.putExtra(Intent.EXTRA_STREAM, logsURI)
        shareIntent.type = "text/*"
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Playfinity Raw Logs.csv")
        val message = "Logs from: ${Date().format("dd-MM-yyyy hh:mm:ss")}\n${sensor_name_tv.text}\n${sensor_firmware_tv.text}"
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, message)

        startActivityForResult(Intent.createChooser(shareIntent, "Send to"), SHARE_REQUEST)
    }

    private fun clearRecords() {
        handler.removeCallbacks(updateListTask)
        rawDataItems.clear()
        adapter.clear()
        counter = 0

        handler.postDelayed(updateListTask, updateListInterval)
    }

    private fun handelSensorDiscovery() {
        if (App.isAtLestM()) {
            if (hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                onLocationPermissionGranted()
            } else {
                requestLocationPermissions()
            }
        } else {
            onLocationPermissionGranted()
        }
    }

    /**
     * Stops ball discover event
     */
    private fun unregisterBallScanner() {
        sensor?.unSubscribeRawData(this)
        pfiBluetoothManager?.run {
            removeSensorDiscoverListener(this@MainActivity)
            stopScanner()
        }
    }

    /**
     * Checks if ble scanner can be started
     */
    private fun canStartBleScanner(): Boolean {
        return isBluetoothEnabled() && hasLocationPermissions()
    }

    private fun onLocationPermissionGranted() {
        if (isBluetoothEnabled()) {
            onBleReady()
        } else {
            onBluetoothNotEnabled()
        }
    }

    private fun hasLocationPermissions(): Boolean {
        return if (App.isAtLestM()) {
            hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        } else {
            true
        }
    }

    private fun onLocationPermissionDenied() {
        AlertDialog.Builder(this)
                .setTitle("Warning")
                .setMessage("Location permission\nis required for\nble scanning")
                .setPositiveButton("Got it") { _, _ ->

                }
                .create()
                .show()
    }

    private fun onBluetoothNotEnabled() {
        AlertDialog.Builder(this)
                .setTitle("Warning")
                .setMessage("You must turn on\nBluetooth to\nDiscover the\nball.")
                .setPositiveButton("Got it") { _, _ ->
                    val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
                    registerReceiver(bleStateReceiver, filter)
                }
                .create()
                .show()
    }

    private fun requestLocationPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION)) {
            Snackbar.make(
                    root,
                    "Location permission\nis required for\nble scanning",
                    Snackbar.LENGTH_LONG)
                    .setAction("Grant") { goToAppSettings() }
                    .show()
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST
            )
        }
    }

    private fun onBleReady() {
        if (!hasLocationPermissions()) {
            return
        }

        unregisterBleReceiver()

        executors.workerThread().execute {
            pfiBluetoothManager?.run {
                status_tv.text = "Looking for Sensors nearby"
                addSensorDiscoverListener(this@MainActivity)
                startScanner(false)
            }
        }
    }

    private fun unregisterBleReceiver() = try {
        unregisterReceiver(bleStateReceiver)
    } catch (e: IllegalArgumentException) {
        //ignore
    }

    private fun isBluetoothEnabled(): Boolean {
        val mBluetoothAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?)?.adapter
        return mBluetoothAdapter?.isEnabled ?: false
    }

    private fun goToAppSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        val executors = AppExecutors()

        private const val samples_text = "Samples: %d"
        private const val LOCATION_PERMISSION_REQUEST = 10
        private const val SHARE_REQUEST = 11
        private val updateListInterval = TimeUnit.SECONDS.toMillis(3)
    }
}
