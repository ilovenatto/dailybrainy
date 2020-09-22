package org.chenhome.dailybrainy.repo.helper

import androidx.lifecycle.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.chenhome.dailybrainy.repo.Challenge
import org.chenhome.dailybrainy.repo.DbFolder
import org.chenhome.dailybrainy.repo.image.RemoteImage
import timber.log.Timber
import java.util.*

/**
 * Listens for Challenges offered by DailyBrainy in remote database. Challenges
 * will only change if app publishes new challenges (rarely).
 *
 * Lifecycle is dictated by a lifecycle owner. This instance should be added as an
 * observer of a lifecycle.
 */
internal class ChallengeObserver : ValueEventListener, LifecycleObserver {
    private val fireDb = FirebaseDatabase.getInstance()
    private val fireRef = fireDb.getReference(DbFolder.CHALLENGES.path)
    private val remoteImage = RemoteImage()
    private val scope = CoroutineScope(Dispatchers.IO)

    // mutable private challenges
    private var _allChall: MutableLiveData<List<Challenge>> = MutableLiveData(listOf())

    /**
     * List of challenges offered by DailyBrainy
     */
    val challenges = Transformations.map(_allChall) {
        it.filter { c ->
            c.category == Challenge.Category.CHALLENGE
        }
    }

    /**
     * List of lessons offered by DailyBrainy
     */
    val lessons = Transformations.map(_allChall) {
        it.filter { c ->
            c.category == Challenge.Category.LESSON
        }
    }

    /**
     * Today's challenge
     */
    val todayChallenge = Transformations.map(_allChall) {
        val challs = it.filter { c ->
            c.category == Challenge.Category.CHALLENGE
        }
        if (challs.isNotEmpty()) challs[indexOfToday(challs.size)]
        else null
    }

    /**
     * Today's lesson
     */
    val todayLesson = Transformations.map(_allChall) {
        val less = it.filter { c ->
            c.category == Challenge.Category.LESSON
        }
        if (less.isNotEmpty()) less[indexOfToday(less.size)]
        else null
    }


    override fun onCancelled(error: DatabaseError) {
        Timber.w("onCancelled $error")
    }

    override fun onDataChange(snapshot: DataSnapshot) {
        Timber.d("Challenges data has changed at location ${snapshot.key}")
        scope.launch {
            try {
                // Map of challengeGuid -> Challenge
                snapshot.getValue<Map<String, Challenge>>()?.let {
                    Timber.d("${it.size} challenges encountered. Replacing all existing challenges.")
                    _allChall.postValue(it.values.toList())

                    // After challenge stubs first published, update challenges with ImageURis, which take several seconds.
                    // then republish this change
                    val challenges = mutableListOf<Challenge>()
                    it.values.forEach { chall ->
                        chall.imageUri = remoteImage.getImageUri(chall.imgFn)
                        challenges.add(chall)
                    }
                    Timber.d("Publishing challenge imageUris")
                    _allChall.postValue(challenges)
                } ?: Timber.w("No challenges found in snapshot ${snapshot.key}")
            } catch (ex: Exception) {
                Timber.e("Unable to observe challenges $ex")
            }
        }

    }


    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun register() = fireRef.addValueEventListener(this)

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun refresh() = _allChall.notifyObserver()

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun deregister() = fireRef.removeEventListener(this)

    private fun indexOfToday(size: Int): Int =
        if (size >= 0) {
            Calendar.getInstance().get(Calendar.DAY_OF_WEEK) % size
        } else {
            -1
        }
}
