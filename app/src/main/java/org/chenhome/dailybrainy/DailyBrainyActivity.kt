package org.chenhome.dailybrainy

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.*
import org.chenhome.dailybrainy.repo.local.BrainyDb
import org.chenhome.dailybrainy.repo.local.ChallengeDb
import org.chenhome.dailybrainy.ui.ViewChallengesFrag
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class DailyBrainyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dailybrainy_act)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, ViewChallengesFrag.newInstance())
                .commitNow()
        }
    }
}
