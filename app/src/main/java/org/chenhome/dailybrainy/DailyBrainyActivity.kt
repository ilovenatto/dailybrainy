package org.chenhome.dailybrainy

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint

// Required at Activity level for fragments to use Hilt
@AndroidEntryPoint
class DailyBrainyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dailybrainy_act)
    }
}
