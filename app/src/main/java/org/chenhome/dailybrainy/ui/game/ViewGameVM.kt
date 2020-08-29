package org.chenhome.dailybrainy.ui.game

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.chenhome.dailybrainy.repo.FullGame
import org.chenhome.dailybrainy.repo.FullGameObserver

/**
 * Offers current game data and any current players in that game.
 * Instantiate with[ViewGameVMFactory], not with Hilt.
 */
class ViewGameVM(
    val gameGuid: String,
    val context: Context,
) : ViewModel() {

    // Observer gets initialized as soon as ViewGameVM is instantiated
    private val fullGameObs = FullGameObserver(context, gameGuid)

    /**
     * Full Game state. Read-only
     */
    val fullGame: LiveData<FullGame> = fullGameObs.fullGame


    override fun onCleared() {
        super.onCleared()
        fullGameObs.onDestroy()
    }
}

/**
 * @property gameGuid Guid identifier for game that's being shown
 */
class ViewGameVMFactory(
    private val context: Context,
    private val gameGuid: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ViewGameVM::class.java)) return ViewGameVM(gameGuid,
            context) as T
        throw IllegalArgumentException("Unsupported VM class $modelClass")
    }
}