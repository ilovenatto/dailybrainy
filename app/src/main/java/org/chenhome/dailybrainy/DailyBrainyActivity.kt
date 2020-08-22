package org.chenhome.dailybrainy

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import org.chenhome.dailybrainy.ui.ViewChallengesFrag

@AndroidEntryPoint
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
