package org.chenhome.dailybrainy.ui.story

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ApplicationComponent
import org.chenhome.dailybrainy.repo.FullGameRepo
import org.chenhome.dailybrainy.repo.UserRepo
import org.chenhome.dailybrainy.repo.game.FullGame
import org.chenhome.dailybrainy.repo.game.Sketch
import org.chenhome.dailybrainy.ui.Event

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
     * navToViewSketch is a external immutable LiveData observable
     * by others
     */
    private var _navToViewSketch = MutableLiveData<Event<Sketch>>()
    val navToViewSketch: LiveData<Event<Sketch>>
        get() = _navToViewSketch

    fun navToViewSketch(sketch: Sketch) {
        _navToViewSketch.value = Event(sketch)
    }
}
