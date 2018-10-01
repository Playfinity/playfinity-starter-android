package register.before.production.playfinity.raw

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Global executor pools for the whole application.
 *
 * Grouping tasks like this avoids the effects of task starvation (e.g. disk reads don't wait behind
 * webservice requests).
 */
class AppExecutors(private val mainThread: Executor,
                   private val workerThread: Executor) {

    constructor() : this(
            MainThreadExecutor(),
            Executors.newSingleThreadExecutor())

    fun mainThread(): Executor {
        return mainThread
    }

    fun workerThread(): Executor {
        return workerThread
    }

    private class MainThreadExecutor : Executor {
        private val mainThreadHandler = Handler(Looper.getMainLooper())

        override fun execute(command: Runnable) {
            mainThreadHandler.post(command)
        }
    }
}