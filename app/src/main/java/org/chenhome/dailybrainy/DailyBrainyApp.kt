package org.chenhome.dailybrainy

import android.app.Application
import timber.log.Timber

class DailyBrainyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Setup logging
        Timber.plant(Timber.DebugTree())
    }
}