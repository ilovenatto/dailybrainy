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

    // List of Games owned by the current user
    val games: LiveData<List<GameStub>> = brainyRepo.gameStubs

    // List of challenges offered by DailyBrainy
    val challenges: LiveData<List<Challenge>> = brainyRepo.challenges

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
     * NAVIGATE: Navigate to Existing Game (gameGuid required)
     *
     * @param gameGuid Guid for existing [Game]
     */
    fun navToExistingGame(gameGuid: String) {
        _navToExistingGame.value = Event(gameGuid)
    }

    private val _navToExistingGame = MutableLiveData<Event<String>>()
    val navToExistingGame: LiveData<Event<String>>
        get() = _navToExistingGame


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