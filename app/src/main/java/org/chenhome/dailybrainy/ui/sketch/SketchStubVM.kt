package org.chenhome.dailybrainy.ui.sketch

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import org.chenhome.dailybrainy.repo.DbFolder
import org.chenhome.dailybrainy.repo.Idea
import org.chenhome.dailybrainy.repo.game.Sketch
import org.chenhome.dailybrainy.ui.Event
import timber.log.Timber

class SketchStubVM @ViewModelInject constructor() : ViewModel() {

    private val fireDb: FirebaseDatabase = FirebaseDatabase.getInstance()

    /**
     * sketchStub is a external immutable LiveData observable
     * by others
     */
    private var _sketchStub = MutableLiveData<Sketch>()
    val sketchStub: LiveData<Sketch>
        get() = _sketchStub

    /**
     * navToNext is a external immutable LiveData observable
     * by others
     */
    private var _navToNext = MutableLiveData<Event<Boolean>>()
    val navToNext: LiveData<Event<Boolean>>
        get() = _navToNext

    fun navToNext() {
        _navToNext.value = Event(true)
    }

    fun loadSketchStub(gameGuid: String, ideaGuid: String) {
        if (_sketchStub.value == null
            && gameGuid.isNotEmpty()
            && ideaGuid.isNotEmpty()
        ) {
            Timber.d("Loading $gameGuid, $ideaGuid")
            // only load once
            fireDb.getReference(DbFolder.IDEAS.path)
                .child(gameGuid)
                .child(ideaGuid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        snapshot.getValue<Idea>()?.also {
                            Timber.d("Got remote sketch $it")
                            _sketchStub.value = Sketch(it)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) = Timber.d("Cancelled")
                })

        }
    }
}