package org.chenhome.dailybrainy.ui.sketch

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ApplicationComponent
import org.chenhome.dailybrainy.repo.FullGameRepo
import org.chenhome.dailybrainy.repo.Idea
import org.chenhome.dailybrainy.repo.UserRepo
import org.chenhome.dailybrainy.repo.game.FullGame
import org.chenhome.dailybrainy.ui.Event
import org.chenhome.dailybrainy.ui.GenerateVMHelper
import org.chenhome.dailybrainy.ui.VoteVMHelper
import timber.log.Timber

class SketchVM(
    context: Context,
    gameGuid: String,
) : ViewModel() {

    @EntryPoint
    @InstallIn(ApplicationComponent::class)
    interface SketchVMEP {
        fun userRepo(): UserRepo
    }

    private val userRepo = EntryPointAccessors.fromApplication(context,
        SketchVMEP::class.java)
        .userRepo()

    /**
     * Expose FullGame
     */
    internal val fullGameRepo = FullGameRepo(context, gameGuid)
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
        _navToNext.value = Event(true)
    }

    val generate = GenerateVMHelper()
    val vote = VoteVMHelper(fullGameRepo)

    fun captureSketch() {
        // create idea and post it remotely

        val idea = Idea(
            "",
            fullGame.value?.game?.guid ?: "",
            userRepo.currentPlayerGuid,
            Idea.Origin.SKETCH)
        // TODO: 9/2/20 remove this later .. test
        idea.imgFn = "challenges/challenge_cookout.png"
        fullGameRepo.insertRemote(idea)

        Timber.d("userid ${userRepo.currentPlayerGuid}")
    }
}
