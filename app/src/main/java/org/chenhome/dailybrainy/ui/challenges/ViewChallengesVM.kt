package org.chenhome.dailybrainy.ui.challenges

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import org.chenhome.dailybrainy.repo.BrainyRepo
import org.chenhome.dailybrainy.repo.Challenge
import org.chenhome.dailybrainy.repo.game.GameStub

/**
 * ViewModel for [ViewChallengesFrag].
 *
 * Lists current user's past games and currently available challenge and lessons.
 * Injectable within Fragment via [https://developer.android.com/reference/kotlin/androidx/fragment/app/package-summary#viewmodels]
 *
 */
class ViewChallengesVM @ViewModelInject constructor(
    brainyRepo: BrainyRepo, // injected
) : ViewModel() {


    val challenges: LiveData<List<Challenge>> = brainyRepo.challenges

    // List of Games owned by the current user
    val games: LiveData<List<GameStub>> = brainyRepo.myGameStubs

    // List of challenges offered by DailyBrainy
    val todayLesson: LiveData<Challenge?> = brainyRepo.todayLesson
    val todayChallenge: LiveData<Challenge?> = brainyRepo.todayChallenge
}


