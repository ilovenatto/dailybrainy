package org.chenhome.dailybrainy.ui.challenges

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.chenhome.dailybrainy.repo.BrainyRepo
import org.chenhome.dailybrainy.repo.Challenge
import org.chenhome.dailybrainy.repo.game.GameStub
import org.chenhome.dailybrainy.ui.Event

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

    /**
     * NAVIGATE: Navigate to New Game (challengeGuid required)
     *
     * @param challengeGuid Guid for [Challenge] for new [Game]
     */
    fun navToNewGame(challengeGuid: String) {
        _navToNewGame.value = Event(challengeGuid)
    }

    private val _navToNewGame = MutableLiveData<Event<String>>()

    // Observed by Fragment
    val navToNewGame: LiveData<Event<String>>
        get() = _navToNewGame

    /**
     * navToJoinGame is a external immutable LiveData observable
     * by others
     */
    private var _navToJoinGame = MutableLiveData<Event<String>>()
    val navToJoinGame: LiveData<Event<String>>
        get() = _navToJoinGame

    fun navToJoinGame(challengeGuid: String) {
        _navToJoinGame.value = Event(challengeGuid)
    }

    /**
     * NAVIGATE: Navigate to Challenge (challengeGuid required)
     *
     * @param challengeGuid Guid for existing [Challenge]
     */
    fun navToLesson(challengeGuid: String) {
        _navToLesson.value = Event(challengeGuid)
    }

    private val _navToLesson = MutableLiveData<Event<String>>()
    val navToLesson: LiveData<Event<String>>
        get() = _navToLesson


}