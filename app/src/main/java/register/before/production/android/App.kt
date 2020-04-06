package register.before.production.android

import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.playfinity.sdk.PFICallback
import io.playfinity.sdk.PlayfinitySDK
import io.playfinity.sdk.PlayfinitySDKBuilder
import io.playfinity.sdk.device.SensorType
import io.playfinity.sdk.errors.PlayfinityThrowable
import timber.log.Timber

class App : Application(), PFICallback {

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

    fun getPfiSdkLiveData(): LiveData<PlayfinitySDK?> =
            pfiLiveData

    override fun onPlayfinityReady(sdk: PlayfinitySDK) {

        // Notify subscribers.
        pfiLiveData.postValue(sdk)
    }

    override fun onPlayfinityError(playfinityThrowable: PlayfinityThrowable) {

        // Log an error.
        Timber.e("Failed to initialize Playfinity SDK. Error ${playfinityThrowable.type}")

        // Notify subscribers.
        pfiLiveData.postValue(null)
    }

    private fun connectPfiSDk() {
        PlayfinitySDKBuilder()
                .enableLogging(BuildConfig.DEBUG)
                .addCallback(this)
                .build(this, SensorType.Ball)
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
