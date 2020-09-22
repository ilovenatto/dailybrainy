package org.chenhome.dailybrainy.ui.idea

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.chenhome.dailybrainy.repo.FullGameRepo
import org.chenhome.dailybrainy.repo.Idea
import org.chenhome.dailybrainy.repo.UserRepo
import org.chenhome.dailybrainy.repo.game.FullGame
import org.chenhome.dailybrainy.ui.Event
import org.chenhome.dailybrainy.ui.GenerateVMHelper
import org.chenhome.dailybrainy.ui.VoteVMHelper
import timber.log.Timber

class IdeaVM(
    context: Context,
    gameGuid: String,
) : ViewModel() {


    /**
     * Expose FullGame
     */
    private val fullGameRepo = FullGameRepo(context, gameGuid)
    val fullGame: LiveData<FullGame> = fullGameRepo.fullGame

    override fun onCleared() {
        fullGameRepo.onClear()
    }

    /**
     * Navigate to next
     */
    private var _navToNext = MutableLiveData<Event<Boolean>>()
    val navToNext: LiveData<Event<Boolean>>
        get() = _navToNext

    fun navToNext() {
        _navToNext.value = Event(true)
    }

    /**
     * Compose helpers for other UI functionality
     */
    val generate = GenerateVMHelper()
    val vote = VoteVMHelper(fullGameRepo)

    var newIdea: MutableLiveData<String> = MutableLiveData("")
    val myPlayerGuid = UserRepo(context).currentPlayerGuid

    /**
     * addIdea is a external immutable LiveData observable
     * by others
     */
    fun addIdea() {
        if (newIdea.value?.isEmpty() != false) {
            Timber.w("No idea to add")
            return
        }


        // init the idea
        fullGame.value?.let {
            val idea = Idea(
                guid = "",
                gameGuid = it.game.guid,
                playerGuid = myPlayerGuid,
                playerName = "",
                origin = Idea.Origin.BRAINSTORM,
                votes = 0,
                title = newIdea.value,
                imgFn = null,
                imgUri = null
            )
            // attempt to insert async
            viewModelScope.launch {
                fullGameRepo.insertRemote(idea)
            }
        }
        // clear the edit text
        newIdea.value = ""
    }


}
