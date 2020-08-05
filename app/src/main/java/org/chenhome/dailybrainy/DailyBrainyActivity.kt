package org.chenhome.dailybrainy

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import org.chenhome.dailybrainy.ui.ViewChallengesFrag

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
