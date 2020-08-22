package org.chenhome.dailybrainy.ui

import android.content.Context
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import org.chenhome.dailybrainy.repo.PlayerSession
import org.chenhome.dailybrainy.repo.UserRepo

class NewGameVM @ViewModelInject constructor(
    val userRepo: UserRepo,
    @ApplicationContext context: Context
) : ViewModel() {
    // Current player
    // TODO: 8/22/20  Use normal 2-way data binding to get value
    // val playerName : String
    // val imgFn : String
    private var _player: MutableLiveData<PlayerSession> = MutableLiveData(PlayerSession())
    val player: LiveData<PlayerSession>
        get() = _player

    /**
     * Called by Fragment to persist the new game in remote database
     *
     * @param challengeGuid Challenge that this [Game] is for.
     * @return gameGuid of the newly created game
     */
    // TODO: 8/22/20
    /*
    fun persistNewGame(challengeGuid: String) : String {
        // Create game
        val gameGuid = brainyRepo.insertGame(challengeGuid, userRepo.currentPlayerGuid)

        // Create player
        _player.value?.gameGuid = gameGuid
        _player.value?.userGuid = userRepo.currentPlayerGuid
        return brainyRepo.insertPlayer(_player.value)
    }*/

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

}