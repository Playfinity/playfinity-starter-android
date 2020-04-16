package register.before.production.android.sound

import android.app.Application
import android.content.res.Resources
import android.net.Uri
import androidx.annotation.RawRes
import io.playfinity.sdk.SensorEvent
import io.playfinity.sdk.SensorEventType
import io.playfinity.sdk.sound.PlayfinitySoundManager
import io.playfinity.sdk.sound.PlayfinitySoundSettings
import io.playfinity.sdk.sound.PlayfinitySoundVolume
import register.before.production.android.AppType
import java.util.*

open class DefaultSoundSkin(
        private val packageName: String,
        private val soundManager: PlayfinitySoundManager,
        application: Application) : SoundSkin {

    //region Resources

    protected val resources: Resources = application.resources

    //endregion

    //region Type

    override val name: AppType? = null

    //endregion

    //region Sounds

    override val kick: PlayfinitySoundSettings? = null
    override val thrown: PlayfinitySoundSettings? = null
    override val caught: PlayfinitySoundSettings? = null
    override val bounce: PlayfinitySoundSettings? = null
    override val miss: PlayfinitySoundSettings? = null
    override val jump: PlayfinitySoundSettings? = null

    override val voFlip: PlayfinitySoundSettings? = null
    override val vo90degree: PlayfinitySoundSettings? = null
    override val vo180degree: PlayfinitySoundSettings? = null
    override val vo270degree: PlayfinitySoundSettings? = null
    override val vo360degree: PlayfinitySoundSettings? = null

    //endregion

    //region Play

    override fun play(sound: PlayfinitySoundSettings?) {
        sound?.let { soundManager.play(it) }
    }

    override fun play(sound: List<PlayfinitySoundSettings?>) {
        sound.forEach {
            if (it != null) {
                play(it)
            }
        }
    }

    //endregion

    //region Stop

    override fun stop(sound: PlayfinitySoundSettings?) {
        sound?.let { soundManager.stop(it) }
    }

    //endregion

    //region Event

    override fun eventSound(sensorEvent: SensorEvent?): PlayfinitySoundSettings? {
        return when (sensorEvent?.eventType) {
            SensorEventType.Thrown -> thrown
            SensorEventType.Caught -> caught
            SensorEventType.Bounce -> bounce
            SensorEventType.Kick -> kick
            SensorEventType.Miss -> miss
            SensorEventType.Jump -> jump
            else -> null
        }
    }

    override fun eventSpeech(sensorEvent: SensorEvent?) {
        //No-op.
    }

    //endregion

    //region Limit

    override fun throwEnds(eventType: SensorEventType?) =
            eventType == SensorEventType.Caught
                    || eventType == SensorEventType.Bounce
                    || eventType == SensorEventType.Miss

    override fun jumpEnds(eventType: SensorEventType?) =
            eventType == SensorEventType.Land

    //endregion

    //region Builder

    protected fun createSoundSettings(@RawRes soundId: Int,
                                      volume: PlayfinitySoundVolume,
                                      queuedPlay: Boolean = false,
                                      looped: Boolean = false,
                                      willBlockPlaybackUntilFinished: Boolean = false) =
            PlayfinitySoundSettings(
                    UUID.randomUUID().toString(),
                    Uri.parse("android.resource://$packageName/$soundId"),
                    soundId,
                    volume = volume,
                    queuedPlay = queuedPlay,
                    looped = looped,
                    willBlockPlaybackUntilFinished = willBlockPlaybackUntilFinished
            )

    //endregion
}
