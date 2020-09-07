package register.before.production.android.sound

import android.app.Application
import io.playfinity.sdk.core.sound.PlayfinitySoundManager
import io.playfinity.sdk.core.sound.PlayfinitySoundSettings
import io.playfinity.sdk.core.sound.PlayfinitySoundVolume
import register.before.production.android.AppType
import register.before.production.android.R

open class TrixSoundSkin(
        packageName: String,
        soundManager: PlayfinitySoundManager,
        application: Application) : DefaultSoundSkin(packageName, soundManager, application) {

    //region Type

    override val name: AppType? = AppType.Trix

    //endregion

    //region Sounds

    override val kick: PlayfinitySoundSettings? = createSoundSettings(R.raw.trix_kick, PlayfinitySoundVolume.high())
    override val bounce: PlayfinitySoundSettings? = createSoundSettings(R.raw.trix_bounce, PlayfinitySoundVolume.high())
    override val miss: PlayfinitySoundSettings? = createSoundSettings(R.raw.trix_miss, PlayfinitySoundVolume.high())

    //endregion
}
