package register.before.production.android.extension

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat


//region Permission

fun Context.hasPermission(permission: String): Boolean {
  return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

fun Context.hasLocationPermission(): Boolean {
  return when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
      hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
    Build.VERSION.SDK_INT > Build.VERSION_CODES.P -> {
      hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    else -> {
      true
    }
  }
}

fun Context.hasAllRequiredBlePermissionsAndServices(): Boolean {
  return when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
      isBluetoothEnabled() && isLocationEnabled() && hasLocationPermission()
    }
    else -> {
      isBluetoothEnabled()
    }
  }
}

//endregion

//region Service

@SuppressLint("MissingPermission")
fun Context.isBluetoothEnabled(): Boolean {

  // Retrieve Bluetooth Manager service.
  val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?

  return bluetoothManager?.adapter?.isEnabled ?: false
}

fun Context.isLocationEnabled(): Boolean {

  // Retrieve Location Manager service.
  val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

  // Return location state based on current software version.
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

    // Returns the current enabled/disabled status of location
    locationManager.isLocationEnabled
  } else {

    // Returns the current enabled/disabled status of location
    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
  }
}

//endregion
