package org.chenhome.dailybrainy.ui.game

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import org.chenhome.dailybrainy.R
import org.chenhome.dailybrainy.repo.*
import org.chenhome.dailybrainy.repo.helper.notifyObserver
import org.chenhome.dailybrainy.repo.image.AvatarImage
import org.chenhome.dailybrainy.ui.Event
import org.chenhome.dailybrainy.ui.UiError
import timber.log.Timber

class NewGameVM @ViewModelInject constructor(
    private val userRepo: UserRepo, // injected by Hilt
    val brainyRepo: BrainyRepo, // injected by Hilt
) : ViewModel() {

    // Current player, changeable by View, via 2-way data binding
    var player: MutableLiveData<PlayerSession> = MutableLiveData(PlayerSession())

    // Whether all form values are set
    val valid = MediatorLiveData<Boolean>().apply {
        addSource(player) {
            value = it.name.isNotEmpty() &&
                    (it.imgFn?.isNotEmpty() ?: false)
        }
    }

    /**
     * NAVIGATE: Navigate to New Game
     *
     * @param challengeGuid Guid for [Challenge]
     */
    fun onNavNewGame(challengeGuid: String) {
        player.value?.let { player ->
            viewModelScope.launch {
                brainyRepo.insertNewGame(challengeGuid, player, userRepo.currentPlayerGuid)
                    ?.let { gameGuid ->
                        _navToGame.value = Event(gameGuid)
                    } ?: showError(R.string.error_creategame)
            }
        } ?: Timber.w("Empty player.Unable to insert new game")
    }

    /**
     * NAVIGATE: Navigate to Existing Game (gameGuid required)
     *
     * @param gameGuid Guid for existing [Game]
     */
    fun onNavExistingGame(gameGuid: String) {
        player.value?.let { player ->
            viewModelScope.launch {
                // set player values
                player.gameGuid = gameGuid
                player.userGuid = userRepo.currentPlayerGuid

                brainyRepo.insertPlayerSession(gameGuid, player)?.let {
                    _navToGame.value = Event(gameGuid)
                } ?: showError(R.string.error_creategame)
            }
        } ?: Timber.w("Unable to join existing game")
    }

    private val _navToGame = MutableLiveData<Event<String>>()
    val navToGame: LiveData<Event<String>>
        get() = _navToGame


    /**
     * UI should show the specified error.
     */
    fun showError(descResId: Int) {
        // default to "Cancel" action
        _showError.value = Event(UiError(descResId, android.R.string.cancel))
    }

    private var _showError = MutableLiveData<Event<UiError>>()
    val showError: LiveData<Event<UiError>>
        get() = _showError


    fun onAvatarSelected(avatarImage: AvatarImage) {
        player.value?.imgFn = avatarImage.name
        player.notifyObserver()
    }

}