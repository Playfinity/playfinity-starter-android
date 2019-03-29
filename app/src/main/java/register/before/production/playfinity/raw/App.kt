package register.before.production.playfinity.raw

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.os.Build
import io.playfinity.sdk.PFICallback
import io.playfinity.sdk.PlayfinitySDK
import io.playfinity.sdk.PlayfinitySDKBuilder
import io.playfinity.sdk.device.SensorType
import io.playfinity.sdk.errors.PlayfinityThrowable

class App : Application(), PFICallback {

    private var playfinitySDK: PlayfinitySDK? = null
    private val pfiSDKLiveData: MutableLiveData<PlayfinitySDK?> = MutableLiveData()


    fun getPlayfinitySdkLiveData(): LiveData<PlayfinitySDK?> =
            pfiSDKLiveData

    override fun onCreate() {
        super.onCreate()

        connectPfiSDk()
    }

    override fun onPlayfinityReady(sdk: PlayfinitySDK) {
        playfinitySDK = sdk
        pfiSDKLiveData.postValue(sdk)
    }

    override fun onPlayfinityError(playfinityThrowable: PlayfinityThrowable) {
        playfinitySDK = null
        pfiSDKLiveData.postValue(null)
    }

    private fun connectPfiSDk() {
        PlayfinitySDKBuilder()
                .enableLogging(BuildConfig.DEBUG)
                .addCallback(this)
                .build(this, SensorType.Ball)
    }

    companion object {
        fun getApp(context: Context) = context.applicationContext as App

        fun isAtLestM() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }
}