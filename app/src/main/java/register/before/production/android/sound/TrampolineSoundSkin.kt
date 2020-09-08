package register.before.production.android.sound

import android.app.Application
import io.playfinity.sdk.core.device.SensorEvent
import io.playfinity.sdk.core.sound.PlayfinitySoundManager
import io.playfinity.sdk.core.sound.PlayfinitySoundSettings
import io.playfinity.sdk.core.sound.PlayfinitySoundVolume
import register.before.production.android.AppType
import register.before.production.android.R
import kotlin.math.abs

open class TrampolineSoundSkin(
        packageName: String,
        soundManager: PlayfinitySoundManager,
        application: Application) : DefaultSoundSkin(packageName, soundManager, application) {

    //region Type

    override val name: AppType? = AppType.Trampoline

    //endregion

    //region Sounds

    override val jump: PlayfinitySoundSettings? = createSoundSettings(R.raw.trampo_hopp, PlayfinitySoundVolume.high())

    override val voFlip: PlayfinitySoundSettings? = createSoundSettings(R.raw.vo_flip, PlayfinitySoundVolume.high())
    override val vo90degree: PlayfinitySoundSettings? = createSoundSettings(R.raw.vo_90, PlayfinitySoundVolume.high())
    override val vo180degree: PlayfinitySoundSettings? = createSoundSettings(R.raw.vo_180, PlayfinitySoundVolume.high())
    override val vo270degree: PlayfinitySoundSettings? = createSoundSettings(R.raw.vo_270, PlayfinitySoundVolume.high())
    override val vo360degree: PlayfinitySoundSettings? = createSoundSettings(R.raw.vo_360, PlayfinitySoundVolume.high())

    //endregion

    //region Event

    override fun eventSpeech(sensorEvent: SensorEvent?) {
        if (jumpEnds(sensorEvent?.eventType)) {
            val yawRotation = abs((sensorEvent?.yawRotation ?: 0).toDouble())
            val pitchRotation = abs((sensorEvent?.pitchRotation ?: 0).toDouble())

            // Play "Flip" sound when pitch exceeds 360 absolute value.
            if (pitchRotation >= 360) {
                play(voFlip)
            }

            // When absolute value of rotation exceeds 90, 180, 270 or 360 degrees play
            // appropriate vo sounds.
            when {
                yawRotation >= 360 -> {
                    play(vo360degree)
                }
                yawRotation >= 270 -> {
                    play(vo270degree)
                }
                yawRotation >= 180 -> {
                    play(vo180degree)
                }
                yawRotation >= 90 -> {
                    play(vo90degree)
                }
            }
        }
    }

    //endregion
}
