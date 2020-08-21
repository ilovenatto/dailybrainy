package org.chenhome.dailybrainy

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class DailyBrainyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Setup logging
        Timber.plant(Timber.DebugTree())
    }
}