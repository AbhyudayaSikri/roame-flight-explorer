package travel.roame.takehome.android

import android.app.Application
import android.os.StrictMode

class FlightExplorerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Debug builds log (never crash on) disk or network access on the main thread and leaked
        // closeables, for QA T1: `adb logcat -s StrictMode` during a full pass.
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build(),
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .detectLeakedSqlLiteObjects()
                    .detectActivityLeaks()
                    .penaltyLog()
                    .build(),
            )
        }
    }
}
