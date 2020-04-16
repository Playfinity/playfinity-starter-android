package register.before.production.android.sound

import android.app.Application
import io.playfinity.sdk.sound.PlayfinitySoundManager
import io.playfinity.sdk.sound.PlayfinitySoundSettings
import io.playfinity.sdk.sound.PlayfinitySoundVolume
import register.before.production.android.AppType
import register.before.production.android.R

open class BallSoundSkin(
        packageName: String,
        soundManager: PlayfinitySoundManager,
        application: Application) : DefaultSoundSkin(packageName, soundManager, application) {

    //region Type

    override val name: AppType? = AppType.Ball

    //endregion

    //region Sounds

    override val thrown: PlayfinitySoundSettings? = createSoundSettings(R.raw.standard_generic_throw, PlayfinitySoundVolume.high())
    override val caught: PlayfinitySoundSettings? = createSoundSettings(R.raw.standard_generic_catch, PlayfinitySoundVolume.high())
    override val miss: PlayfinitySoundSettings? = createSoundSettings(R.raw.standard_generic_miss, PlayfinitySoundVolume.high())

    //endregion
}
