package org.chenhome.dailybrainy.ui.sketch

import android.content.Context
import android.net.Uri
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
import org.chenhome.dailybrainy.repo.game.Sketch
import org.chenhome.dailybrainy.repo.image.LocalImageRepo
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
        fun localImage(): LocalImageRepo
    }

    private val userRepo = EntryPointAccessors.fromApplication(context,
        SketchVMEP::class.java)
        .userRepo()

    private val localImageRepo: LocalImageRepo = EntryPointAccessors.fromApplication(context,
        SketchVMEP::class.java)
        .localImage()

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

    fun navToNext(isUpdateGame: Boolean) {
        if (isUpdateGame) {
            viewModelScope.launch {
                fullGame.value?.game?.let {
                    fullGameRepo.updateRemote(it)
                    _navToNext.value = Event(true)
                }
            }
        } else {
            _navToNext.value = Event(true)
        }
    }

    val generate = GenerateVMHelper()
    val vote = VoteVMHelper(fullGameRepo)


    /**
     * navToCamera is a external immutable LiveData observable
     * by others
     */
    private var _navToCamera = MutableLiveData<Event<Idea.Origin>>()
    val navToCamera: LiveData<Event<Idea.Origin>>
        get() = _navToCamera

    fun navToCamera(origin: Idea.Origin) {
        _navToCamera.value = Event(origin)
    }

    /**
     * Always regenerate a new URI every time this value is retrieved.
     */
    var sketchImageUri: Uri? = null

    /**
     * Generate and set new URI value for [sketchImageUri]
     */
    fun genAndSetNewUri() {
        sketchImageUri = localImageRepo.makeFileUri(localImageRepo.LOCALFOLDER_PICS)
    }


    /**
     * @return whether upload succeeded or not
     */
    fun uploadSketch(origin: Idea.Origin) {
        sketchImageUri?.let { uri ->
            fullGameRepo.insertRemoteSketch(origin, uri, userRepo.currentPlayerGuid)
        } ?: Timber.w("Unable to upload sketch with $sketchImageUri")
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
