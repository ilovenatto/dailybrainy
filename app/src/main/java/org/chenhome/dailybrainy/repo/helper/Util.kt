package org.chenhome.dailybrainy.repo.helper

import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.runBlocking
import org.chenhome.dailybrainy.repo.DbFolder
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * @param T type of the singleton
 * @param A parameter that's being injected
 * @constructor
 *
 * @param creator lambda that instantiates the singleton type
 */
open class SingletonHolder<out T : Any, in A>(creator: (A) -> T) {
    private var creator: ((A) -> T)? = creator
    @Volatile
    private var instance: T? = null

    fun singleton(arg: A): T {
        val i = instance
        // use smart cast to check it's null and of the right singleton type
        if (i != null) return i

        // Synchronize on static companion object (there's only one)
        return synchronized(this) {
            val i2 = instance
            if (i2 != null) {
                i2
            } else {
                val created = creator!!(arg)
                instance = created
                creator = null
                created
            }
        }
    }
}

/**
 * Helper to notify observers of [MutableLiveData] that it's internal data structure
 * has changed
 *
 * https://stackoverflow.com/questions/47941537/notify-observer-when-item-is-added-to-list-of-livedata?noredirect=1&lq=1
 *
 * @param T
 */
fun <T> MutableLiveData<T>.notifyObserver() {
    // Setting [MutableLiveData.value] notifies observers of that LiveData
    this.value = this.value
}


fun nukeRemoteDb() {
    val fireDb = FirebaseDatabase.getInstance()
    // remove all games and ideas
    runBlocking {
        suspendCoroutine<Unit> {
            fireDb.getReference(DbFolder.GAMES.path).setValue(null)
            fireDb.getReference(DbFolder.IDEAS.path).setValue(null)
            fireDb.getReference(DbFolder.PLAYERSESSION.path).setValue(null)
            it.resume(Unit)
        }
    }
}
