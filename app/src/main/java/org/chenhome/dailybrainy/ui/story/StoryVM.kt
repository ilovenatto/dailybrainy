package org.chenhome.dailybrainy.ui.story

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ApplicationComponent
import kotlinx.coroutines.launch
import org.chenhome.dailybrainy.repo.FullGameRepo
import org.chenhome.dailybrainy.repo.Idea
import org.chenhome.dailybrainy.repo.UserRepo
import org.chenhome.dailybrainy.repo.game.FullGame
import org.chenhome.dailybrainy.ui.Event
import timber.log.Timber

class StoryVM(
    context: Context,
    gameGuid: String,
) : ViewModel() {

    @EntryPoint
    @InstallIn(ApplicationComponent::class)
    interface StoryVMEP {
        fun userRepo(): UserRepo
    }

    private val userRepo = EntryPointAccessors.fromApplication(context,
        StoryVMEP::class.java)
        .userRepo()


    /**
     * Expose FullGame
     */
    private val fullGameRepo = FullGameRepo(context, gameGuid)
    val fullGame: LiveData<FullGame> = fullGameRepo.fullGame

    override fun onCleared() {
        fullGameRepo.onClear()
    }

    /**
     * navToNext
     */
    private var _navToNext = MutableLiveData<Event<Boolean>>()
    val navToNext: LiveData<Event<Boolean>>
        get() = _navToNext

    fun navToNext() {
        viewModelScope.launch {
            fullGame.value?.game?.let {
                fullGameRepo.updateRemote(it)
                _navToNext.value = Event(true)
            }
        }
    }

    fun captureSetting() = captureSketch(Idea.Origin.STORY_SETTING)
    fun captureSolution() = captureSketch(Idea.Origin.STORY_SOLUTION)
    fun captureResolution() = captureSketch(Idea.Origin.STORY_RESOLUTION)

    fun captureSketch(origin: Idea.Origin) {
        // create idea and post it remotely
        val idea = Idea(
            "",
            fullGame.value?.game?.guid ?: "",
            userRepo.currentPlayerGuid,
            origin)
        // TODO: 9/2/20 remove this later .. test
        idea.imgFn = "challenges/challenge_cookout.png"
        fullGameRepo.insertRemote(idea)

        Timber.d("userid ${userRepo.currentPlayerGuid}")
    }
}
