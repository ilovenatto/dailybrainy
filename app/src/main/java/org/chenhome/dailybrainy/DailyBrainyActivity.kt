package org.chenhome.dailybrainy

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import org.chenhome.dailybrainy.repo.UserRepo
import org.chenhome.dailybrainy.ui.ViewChallengesFrag
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class DailyBrainyActivity : AppCompatActivity() {

    // field injection
    @Inject
    lateinit var userRepo: UserRepo
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dailybrainy_act)
        Timber.d("User guid ${userRepo.currentPlayerGuid}")
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, ViewChallengesFrag.newInstance())
                .commitNow()
        }
    }
}
