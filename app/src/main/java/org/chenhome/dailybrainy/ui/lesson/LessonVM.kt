package org.chenhome.dailybrainy.ui.lesson

import android.net.Uri
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import org.chenhome.dailybrainy.repo.Challenge
import org.chenhome.dailybrainy.repo.DbFolder
import org.chenhome.dailybrainy.repo.game.Lesson
import org.chenhome.dailybrainy.ui.Event
import timber.log.Timber

class LessonVM @ViewModelInject constructor() : ViewModel() {

    private val fireDb = FirebaseDatabase.getInstance()

    /**
     * lesson is a external immutable LiveData observable
     * by others
     */
    private var _lesson = MutableLiveData<Lesson>()
    val lesson: LiveData<Lesson>
        get() = _lesson

    /**
     * Must be called in order to load the lesson
     *
     * @param challengeGuid
     */
    fun loadLesson(challengeGuid: String) {
        // only load once
        if (_lesson.value == null
            && challengeGuid.isNotEmpty()
        ) {
            fireDb.getReference(DbFolder.CHALLENGES.path)
                .child(challengeGuid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        snapshot.getValue<Challenge>()?.let {
                            _lesson.value = Lesson(it)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) = Timber.d("Cancelled")
                })
        }
    }

    /**
     * navToYoutube is a external immutable LiveData observable
     * by others
     */
    private var _navToYoutube = MutableLiveData<Event<Uri?>>()
    val navToYoutube: LiveData<Event<Uri?>>
        get() = _navToYoutube

    fun navToYoutube() {
        _navToYoutube.value = Event(_lesson.value?.toYoutubeUri())
    }

    /**
     * thumbUri is a external immutable LiveData observable
     * by others. Uri of this lesson's youtube video
     */
    val thumbUri: LiveData<Uri> = Transformations.map(_lesson) {
        it.toYoutubeThumbUri()
    }
}