package com.playfinity.starter.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.Build.VERSION
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AppCompatActivity
import com.playfinity.starter.utils.hasBluetoothPermission
import com.playfinity.starter.utils.hasLocationPermission
import com.playfinity.starter.utils.isBluetoothEnabled
import com.playfinity.starter.utils.isLocationEnabled
import com.playfinity.starter.utils.isLocationRequired

@SuppressLint("MissingPermission")
abstract class PlayfinityActivity : AppCompatActivity() {

    abstract fun onReadyToScan()

    protected fun checkPermissionsAndServices() {
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

            else -> onReadyToScan()
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                checkPermissionsAndServices()
            }
        }
}
