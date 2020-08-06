package org.chenhome.dailybrainy.repo

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.chenhome.dailybrainy.repo.local.*
import timber.log.Timber

/**
 * Repository facade over local database and local image filestore. Local database
 * is backed by network store.
 *
 * Offers references to data. The data must be refreshed by calling [refresh]
 * which will run off the main thread.
 */
class BrainyRepo (
    val context:Context
) {
    /**
     * Private
     */
    private val db: BrainyDb = BrainyDb.getDb(context)
    private val user: UserRepo = UserRepo(context)

    // necessary for INSERT, UPDATE, DELETE.
    // GET queries are done on background thread if they return LiveData
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)

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
     * Refreshes local entities data from Firebase database. Does not refresh images, which live in Firebase Firestore
     *
     * @return number of entities that were updated or inserted.
     */
    // TODO: 8/3/20 utlize `RemoteDb {` to intialize local db
    /*
    suspend fun refresh() : Int {
        return withContext(scope.coroutineContext) {
            // register remote data listeners if not yet registered
            if ()

            // refresh data
            var numUpdated = 0


        }
    }*/


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
    suspend fun insertIdea(gameGuid: String, idea: Idea): Idea? {
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
    suspend fun insertNewGame(challengeGuid: String): Game? {
        return withContext(scope.coroutineContext) {
            val challenge = db.challengeDAO.get(challengeGuid)
            if (challenge == null) {
                Timber.w("No such challenge $challengeGuid")
                return@withContext null
            }
            val game = Game(
                genGuid(),
                challengeGuid,
                user.currentPlayerGuid,
                genPin(),
                System.currentTimeMillis(),
                Challenge.Step.GEN_IDEA,
                null,
                null
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

}