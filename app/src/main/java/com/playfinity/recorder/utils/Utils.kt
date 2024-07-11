package com.playfinity.recorder.utils

import java.util.regex.Pattern

object Utils {

    private const val PF2_BOOT_PATTERN = "PF2 Boot (.+) v\\d+\\.\\d+(\\.\\d+)?"
    private const val PF2_APP_PATTERN = "PF2 App (.+) v\\d+\\.\\d+(\\.\\d+)?"

    internal fun isPlayfinityDevice(deviceName: String?): Boolean =
        isPfBoot(deviceName ?: "") || isPfApp(deviceName ?: "")

    /**
     * OAD Device name.
     */
    private fun isPfBoot(deviceName: String): Boolean {
        return Pattern.matches(PF2_BOOT_PATTERN, deviceName)
    }

    /**
     * Device name used in firmware 1.6+.
     */
    private fun isPfApp(deviceName: String): Boolean {
        return Pattern.matches(PF2_APP_PATTERN, deviceName)
    }

    fun ByteArray.toHexString() = joinToString(" ") { String.format("%02x", it) }
}
