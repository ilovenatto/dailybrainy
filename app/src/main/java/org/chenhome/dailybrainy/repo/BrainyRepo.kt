package org.chenhome.dailybrainy.repo

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.chenhome.dailybrainy.repo.local.*
import org.chenhome.dailybrainy.repo.remote.RemoteDb
import timber.log.Timber

/**
 * Repository facade over local database and local image filestore. Local database
 * is backed by network store.
 *
 * As soon as the singleton is instantiated, data will begin to be synced to the local db.
 * On instantiation, remote and local db listeners will be registered and cause data to be synced
 * between these sources.
 */
class BrainyRepo
private constructor(
    val context: Context
) {
    /**
     * Private
     */
    private val db: LocalDb = LocalDb.singleton(context)
    private val user: UserRepo = UserRepo(context)
    private val remoteDb: RemoteDb = RemoteDb // singleton

    // necessary for INSERT, UPDATE, DELETE.
    // GET queries are done on background thread if they return LiveData
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)


    /**
     * On instantiation of the singleton, register remote db listners.
     * This will cause the data to be downloaded from remote db and written to local db
     */
    init {
        // Registration can be done on UI thread.
        Timber.d("Listening for remote db changes")
        remoteDb.registerRemoteObservers(context, ProcessLifecycleOwner.get())
    }


    /**
     * Access singleton with
     * `BrainyRepo.singleton(context)`
     */
    companion object : SingletonHolder<BrainyRepo, Context>({
        BrainyRepo(it)
    })

    /**
     * Registers listeners of the local database for the target game.
     * When the local data changes, update the remote database.
     *
     * Also register listeners of the remote game for changes and update the local db.
     *
     * Observers are registered lifecycle owner starts and deregistered when lifecycle is destroyed.
     *
     * @param gameGuid Game to look for in local database and observe for changes
     * @param gameLifecycle the currently running game's lifecycle
     */
    fun registerGameObservers(gameGuid: String, gameLifecycle: LifecycleOwner) {
        Timber.d("Registering observers for local game $gameGuid")
        // Listen to current game
        db.gameDAO.getLive(gameGuid)
            .observe(gameLifecycle, Observer { game ->
                Timber.d("Observed local game changed to: $game")
                remoteDb.updateRemote(game)
            })

        // Listen to ideas
        db.ideaDAO.getNewIdeasByGameLive(gameGuid)
            .observe(gameLifecycle, Observer { ideas ->
                Timber.d("Observed ${ideas.size} new ideas for game $gameGuid")
                remoteDb.addRemote(ideas)
            })

        // listen to remote game state, such as the game and ideas generated within that game
        remoteDb.registerRemoteGameObservers(context, gameGuid, gameLifecycle)
    }

    /**
     * @return challenge that this player hasn't encountered before. Null if one can't be found
     */
    suspend fun getNeverPlayed(category: Challenge.Category): Challenge? {
        return withContext(scope.coroutineContext) {
            val encountered = mutableSetOf<String>()
            db.gameDAO.getByPlayer(user.currentPlayerGuid).forEach {
                encountered.add(it.challengeGuid)
            }
            try {
                return@withContext db.challengeDAO.getAll().first {
                    !encountered.contains(it.guid)
                            && it.category == category
                }
            } catch (e: NoSuchElementException) {
                return@withContext null
            }
        }
    }

    /**
     * @return whether repo deleted local database, and user preferences
     */
    suspend fun nukeEverything() {
        withContext(scope.coroutineContext) {
            db.clearAllTables()
            user.clearUserPrefs()
        }
    }

    /**
     * Convenience method for inserting child Idea to specified Game
     * @return newly inserted Idea with its properties in the correct state. Else null
     * if insertion failed
     */
    suspend fun insertLocalIdea(gameGuid: String, idea: Idea): Idea? {
        return withContext(scope.coroutineContext) {
            val corrected = idea.copy(
                gameGuid = gameGuid,
                playerGuid = user.currentPlayerGuid
            )
            db.ideaDAO.insert(corrected).let {
                if (it <= 0) {
                    Timber.w("Unable to insert new ida $corrected")
                    return@withContext null
                }
            }
            return@withContext corrected
        }
    }

    /**
     * Instantiate Game, representing a newly inserted Game entity associated with an existing
     * challenge.
     *
     * @param challengeGuid existing challenge in db
     * @return Game instance representing newly inserted game in db. Null if insertion failed
     */
    suspend fun insertLocalGame(challengeGuid: String): Game? {
        return withContext(scope.coroutineContext) {
            val challenge = db.challengeDAO.get(challengeGuid)
            if (challenge == null) {
                Timber.w("No such challenge $challengeGuid")
                return@withContext null
            }
            val game = Game(
                guid = genGuid(),
                fireGuid = null,
                challengeGuid = challengeGuid,
                playerGuid = user.currentPlayerGuid,
                pin = genPin(),
                sessionStartMillis = System.currentTimeMillis(),
                currentStep = Challenge.Step.GEN_IDEA,
                storyTitle = null,
                storyDesc = null
            )
            db.gameDAO.insert(game).run {
                if (this == 0L) {
                    Timber.w("Unable to insert game $game with challengeId: $challenge")
                    return@withContext null
                }
            }
            return@withContext game
        }
    }

    /**
     * @param game Game with new state
     * @return whether local update succeeded
     */
    suspend fun updateLocalGame(game: Game): Boolean =
        withContext(scope.coroutineContext) {
            return@withContext db.gameDAO.update(game) == 1
        }
}
