package com.playfinity.recorder.utils

import java.util.UUID


internal object SensorServices {
    val movement: UUID = UUID.fromString("f000c0c0-0451-4000-b000-000000000000")
}

internal object SensorCharacteristics {
    val movementData: UUID = UUID.fromString("f000c0c2-0451-4000-b000-000000000000")
}

internal object SensorDescriptors {
    val notifications: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
}
