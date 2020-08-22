package org.chenhome.dailybrainy.ui

import android.content.Context
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import org.chenhome.dailybrainy.repo.BrainyRepo
import org.chenhome.dailybrainy.repo.Challenge
import org.chenhome.dailybrainy.repo.GameStub
import org.chenhome.dailybrainy.repo.UserRepo

/**
 * ViewModel for [ViewChallengesFrag].
 *
 * Lists current user's past games and currently available challenge and lessons.
 * Injectable within Fragment via [https://developer.android.com/reference/kotlin/androidx/fragment/app/package-summary#viewmodels]
 *
 */
class ViewChallengesVM @ViewModelInject constructor(
    val userRepo: UserRepo,
    @ApplicationContext context: Context
) : ViewModel() {

    private val brainyRepo: BrainyRepo = BrainyRepo.singleton(context)

    // List of Games owned by the current user
    val games: LiveData<List<GameStub>> = brainyRepo.gameStubs
    val challenges: LiveData<List<Challenge>> = brainyRepo.challenges
}