package org.chenhome.dailybrainy.ui.sketch

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.chenhome.dailybrainy.repo.FullGameRepo
import org.chenhome.dailybrainy.repo.game.FullGame
import org.chenhome.dailybrainy.ui.Event
import org.chenhome.dailybrainy.ui.GenerateVMHelper
import org.chenhome.dailybrainy.ui.VoteVMHelper

class SketchVM(
    context: Context,
    gameGuid: String,
) : ViewModel() {

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

    }
}
