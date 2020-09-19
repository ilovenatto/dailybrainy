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
import org.chenhome.dailybrainy.repo.image.RemoteImage
import org.chenhome.dailybrainy.repo.image.RemoteImageFolder
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

    private val remoteImageRepo: RemoteImage = RemoteImage()


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
        _navToNext.value = Event(true)
    }

    val generate = GenerateVMHelper()
    val vote = VoteVMHelper(fullGameRepo)


    /**
     * navToCamera is a external immutable LiveData observable
     * by others
     */
    private var _navToCamera = MutableLiveData<Event<Boolean>>()
    val navToCamera: LiveData<Event<Boolean>>
        get() = _navToCamera

    fun navToCamera() {
        _navToCamera.value = Event(true)
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


    fun uploadSketch() {
        if (sketchImageUri == null || !localImageRepo.isExist(sketchImageUri!!)) {
            Timber.w("No image was saved by camera app. Unable to upload.")
            return
        }

        viewModelScope.launch {
            remoteImageRepo.upload(RemoteImageFolder.SKETCHES, sketchImageUri!!)?.let { ref ->
                Timber.d("Stored file to ${ref.path}")
                if (ref.path.isNotEmpty()) {
                    // create idea and post it remotely
                    val idea = Idea(
                        "", // gets set by FullGameRepo
                        fullGame.value?.game?.guid!!, // we better have a game guid
                        userRepo.currentPlayerGuid,
                        Idea.Origin.SKETCH)
                    idea.imgFn = ref.path
                    fullGameRepo.insertRemote(idea) // hopefully it worked
                } else {
                    Timber.w("Invalid remoate path ${ref.path}. Unable to upload sketch.")
                }
            } ?: Timber.w("Unable to upload sketch")
        }
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
