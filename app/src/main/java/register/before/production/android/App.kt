package register.before.production.android

import android.app.Application
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.util.SparseArray
import androidx.core.util.containsKey
import androidx.lifecycle.MutableLiveData
import io.playfinity.sdk.PFICallback
import io.playfinity.sdk.PlayfinitySDK
import io.playfinity.sdk.PlayfinitySDKBuilder
import io.playfinity.sdk.core.sound.PlayfinitySoundManager
import io.playfinity.sdk.utility.error.PFIThrowable
import io.playfinity.sdk.core.device.SensorType
import register.before.production.android.sound.BallSoundSkin
import register.before.production.android.sound.SoundSkin
import register.before.production.android.sound.TrampolineSoundSkin
import register.before.production.android.sound.TrixSoundSkin

import timber.log.Timber

class App : Application(), PFICallback, PlayfinitySoundManager.PlayfinitySoundManagerListener {

    //region Lifecycle

    override fun onCreate() {
        super.onCreate()

        // Setup logger.
        setupTimber()

        // Initialize Playfinity SDK.
        connectPfiSDk()
    }

    //endregion

    //region Playfinity

    private val pfiLiveData: MutableLiveData<PlayfinitySDK?> = MutableLiveData()

    fun getPfiSdkLiveData(): MutableLiveData<PlayfinitySDK?> =
            pfiLiveData

    override fun onPlayfinityReady(sdk: PlayfinitySDK) {

        // Notify subscribers.
        pfiLiveData.postValue(sdk)
    }

    override fun onPlayfinityError(pfiThrowable: PFIThrowable) {

        // Log an error.
        Timber.e("Failed to initialize Playfinity SDK. Error ${pfiThrowable.type}")

        // Notify subscribers.
        pfiLiveData.postValue(null)
    }

    private fun connectPfiSDk() {
        val sensorType = when (getAppType()) {
            AppType.Trix -> SensorType.Football
            AppType.Trampoline -> SensorType.Trampoline
            else -> SensorType.Ball
        }

        PlayfinitySDKBuilder()
                .enableLogging(BuildConfig.DEBUG)
                .addCallback(this)
                .build(this, sensorType)
    }

    //endregion

    //region Sound

    private val soundManager: PlayfinitySoundManager by lazy {
        PlayfinitySoundManager.create(this, this)
    }

    private val soundSkinsArray: SparseArray<SoundSkin> = SparseArray()

    fun getSoundSkin(): SoundSkin {
        val type = getAppType()
        val key = type.ordinal
        return if (soundSkinsArray.containsKey(key)) {
            soundSkinsArray[key]
        } else {
            val soundSkin = when (type) {
                AppType.Ball -> BallSoundSkin(packageName, soundManager, this@App)
                AppType.Trampoline -> TrampolineSoundSkin(packageName, soundManager, this@App)
                AppType.Trix -> TrixSoundSkin(packageName, soundManager, this@App)
            }
            soundSkinsArray.put(key, soundSkin)
            soundSkin
        }
    }

    override fun onLanguageDataMissing() {
        val installIntent = Intent()
        installIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
        startActivity(installIntent)
    }

    //endregion

    //region Timber

    private fun setupTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    //endregion

    //region Companion

    companion object {

        fun getApp(context: Context) = context.applicationContext as App

        fun getAppType() =
                try {
                    AppType.valueOf(BuildConfig.APP_TYPE)
                } catch (e: IllegalArgumentException) {
                    AppType.Ball
                }
    }

    //endregion
}
