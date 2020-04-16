package register.before.production.android.sound

import io.playfinity.sdk.SensorEvent
import io.playfinity.sdk.SensorEventType
import io.playfinity.sdk.sound.PlayfinitySoundSettings
import register.before.production.android.AppType

interface SoundSkin {

    //region Type

    val name: AppType?

    //endregion

    //region Sounds

    val kick: PlayfinitySoundSettings?
    val thrown: PlayfinitySoundSettings?
    val caught: PlayfinitySoundSettings?
    val bounce: PlayfinitySoundSettings?
    val miss: PlayfinitySoundSettings?
    val jump: PlayfinitySoundSettings?

    val voFlip: PlayfinitySoundSettings?
    val vo90degree: PlayfinitySoundSettings?
    val vo180degree: PlayfinitySoundSettings?
    val vo270degree: PlayfinitySoundSettings?
    val vo360degree: PlayfinitySoundSettings?

    //endregion

    //region Play

    fun play(sound: PlayfinitySoundSettings?)
    fun play(sound: List<PlayfinitySoundSettings?>)

    //endregion

    //region Stop

    fun stop(sound: PlayfinitySoundSettings?)

    //endregion

    //region Event

    fun eventSound(sensorEvent: SensorEvent?): PlayfinitySoundSettings?

    //endregion

    fun eventSpeech(sensorEvent: SensorEvent?)

    //endregion

    //region Limit

    fun throwEnds(eventType: SensorEventType?): Boolean

    fun jumpEnds(eventType: SensorEventType?): Boolean

    //endregion
}
