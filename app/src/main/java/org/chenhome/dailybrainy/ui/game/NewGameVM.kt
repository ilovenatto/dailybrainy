package org.chenhome.dailybrainy.ui.game

import android.content.Context
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import org.chenhome.dailybrainy.repo.PlayerSession
import org.chenhome.dailybrainy.repo.image.AvatarImage
import org.chenhome.dailybrainy.ui.Event
import timber.log.Timber

class NewGameVM @ViewModelInject constructor(
    @ApplicationContext context: Context
) : ViewModel() {
    // Current player
    private var _player: MutableLiveData<PlayerSession> = MutableLiveData(PlayerSession())
    val player: LiveData<PlayerSession>
        get() = _player

    init {
        _player.value?.name = "foobar"
    }

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
    fun navToGame(gameGuid: String) {
        _navToGame.value = Event(gameGuid)
    }

    private val _navToGame = MutableLiveData<Event<String>>()
    val navToGame: LiveData<Event<String>>
        get() = _navToGame


    fun onAvatarSelected(avatarImage: AvatarImage) {
        Timber.d("Selecting avatar $avatarImage")
        _player.value?.imgFn = avatarImage.name
    }

}