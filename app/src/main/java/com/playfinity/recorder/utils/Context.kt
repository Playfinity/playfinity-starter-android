package com.playfinity.recorder.utils

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Build.VERSION
import androidx.core.content.ContextCompat


/**
 * Determine whether given permission has been granted.
 */
fun Context.hasPermission(permission: String): Boolean {
  return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

/**
 * Returns true if Location permissions required to discover a Playfinity Sensor are given.
 */
fun Context.hasLocationPermission(): Boolean {
  return when {
    VERSION.SDK_INT >= Build.VERSION_CODES.M && VERSION.SDK_INT <= Build.VERSION_CODES.P -> {
      hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
    VERSION.SDK_INT > Build.VERSION_CODES.P && VERSION.SDK_INT < Build.VERSION_CODES.S -> {
      hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    else -> {
      true
    }
  }
}

/**
 * Returns true if Bluetooth permissions required to discover a Playfinity Sensor are given.
 */
fun Context.hasBluetoothPermission(): Boolean {
  return when {
    VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      hasPermission(Manifest.permission.BLUETOOTH_CONNECT) && hasPermission(
          Manifest.permission.BLUETOOTH_SCAN)
    }
    else -> {
      true
    }
  }
}

/**
 * Returns true if all required providers and services to discover a Playfinity Sensor are up and running.
 */
fun Context.hasAllRequiredBlePermissionsAndServices(): Boolean {
  return when {

    // For Android 6 and below.
    VERSION.SDK_INT < Build.VERSION_CODES.M -> isBluetoothEnabled()

    // For Android 7 -> Android 11
    VERSION.SDK_INT >= Build.VERSION_CODES.M && VERSION.SDK_INT < Build.VERSION_CODES.S -> {
      isBluetoothEnabled() && isLocationEnabled() && hasLocationPermission()
    }

    // For Android 12 and above.
    else -> {
      isBluetoothEnabled() && hasBluetoothPermission()
    }
  }
}

/**
 * Return true if Bluetooth is currently enabled and ready for use.
 */
fun Context.isBluetoothEnabled(): Boolean {

  // Retrieve Bluetooth Manager service.
  val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?

  return bluetoothManager?.adapter?.isEnabled ?: false
}

/**
 * Returns the current enabled/disabled status of location provider.
 */
fun Context.isLocationEnabled(): Boolean {

  // Retrieve Location Manager service.
  val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

  // Return location state based on current software version.
  return if (VERSION.SDK_INT >= Build.VERSION_CODES.P) {

    // Returns the current enabled/disabled status of location
    locationManager.isLocationEnabled
  } else {

    // Returns the current enabled/disabled status of location
    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
  }
}

/**
 * Returns true if [LocationManager.GPS_PROVIDER] or [LocationManager.NETWORK_PROVIDER] service is required to pair a Playfinity Sensor.
 */
fun Context.isLocationRequired(): Boolean {
  return when {
    VERSION.SDK_INT >= Build.VERSION_CODES.M && VERSION.SDK_INT <= Build.VERSION_CODES.P -> {
      true
    }
    VERSION.SDK_INT > Build.VERSION_CODES.P && VERSION.SDK_INT < Build.VERSION_CODES.S -> {
      true
    }
    else -> {
      false
    }
  }
}
