package org.chenhome.dailybrainy.repo

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.chenhome.dailybrainy.repo.local.*
import timber.log.Timber
import kotlin.random.Random

/**
 * Repository facade over local database and local image filestore. Local database
 * is backed by network store.
 *
 * Offers references to data. The data must be refreshed by calling {@link BrainyRepo.refresh()},
 * which will run off the main thread.
 */
class BrainyRepo (
    val context:Context
) {
    /**
     * Private
     */
    private val db: BrainyDb = BrainyDb.getDb(context)
    private val firebase: FirebaseDatabase = Firebase.database
    private val user: UserRepo = UserRepo(context)
    private val mapper:DomainObjectMapper = DomainObjectMapper(db, context)

    // necessary for INSERT, UPDATE, DELETE.
    // GET queries are done on background thread if they return LiveData
    private val scope:CoroutineScope = CoroutineScope(Dispatchers.IO)

    /**
     * Initialize on first reference to value.
     * Database READ calls can be made on UI thread
     */
    val myGames : LiveData<List<GameDb>> =
        db.gameDAO.getByPlayer(user.currentPlayerGuid) // current Guid lazily initialized also

    val todayLesson : LiveData<LessonDb>? =
        // pick randomly from list
        Transformations.map(db.lessonDAO.getAll()) {
            if (it.isEmpty()) null else it.get(Random.nextInt(0,it.size))
        }

    val todayChallenge : LiveData<ChallengeDb>? =
        Transformations.map(db.challengeDAO.getAll()) {
            if (it.isEmpty()) null else it.get(Random.nextInt(0, it.size))
        }


    /**
     * Refreshes local entities data from Firebase database. Does not refresh images, which live in Firebase Firestore
     *
     * @return number of entities that were updated or inserted.
     */
    // TODO: 8/3/20 utlize FireDatabaseRepo to intialize local db
/*    suspend fun refresh() : Int {
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
     * Instantiate Game, representing a newly inserted GameDb entity associated with an existing
     * challenge.
     *
     * @param challengeId existing challenge in db
     * @return Game instance representing newly inserted game in db. Null if insertion failed
     */
    suspend fun makeAndInsertGame(challengeId: Long) : Game? {
        return withContext(scope.coroutineContext) {
            val challenge = db.challengeDAO.get(challengeId)
            if (challenge == null || challenge.id != challengeId){
                return@withContext null
            }
            val game = GameDb(
                id = 0,
                guid = genGuid(),
                challengeId = challenge.id,
                pin = Game.genPin(),
                sessionStartMillisEpoch = System.currentTimeMillis(),
                currentStep = ChallengeDb.Step.GEN_IDEA
            )
            if (!game.canInsert()){
                return@withContext null
            }
            val gid = db.gameDAO.insert(game)
            if (gid == 0L) {
                Timber.w("Unable to insert game $game with challengeId: $challenge")
                return@withContext null
            }
            return@withContext mapper.toGame(game.copy(id = gid), challenge)
        }
    }

    /**
     * @return whether the update succeeded.
     * @param game must have it's {@link Game.id} set
     */
    suspend fun updateGame(game: Game): Boolean {
        return withContext(scope.coroutineContext) {
            val gameDb = mapper.toGameDb(game)
            if (!gameDb.canUpdate()) {
                Timber.w("Invalid Game. Cannot be updated $gameDb")
                return@withContext false
            }
            return@withContext db.gameDAO.update(gameDb) > 0L
        }
    }

    /**
     * Add idea to the game
     */
    suspend fun addIdea(idea: IdeaDb) : Boolean {
        return withContext(scope.coroutineContext) {
            if (idea.canInsert()) {
                db.ideaDAO.insert(idea) > 0L
            } else {
                Timber.w("Idea is not valid for insertion $idea")
                false
            }
        }
    }

    /**
     * Comprehensive data object describing the game state.
     *
     * @return Game constituted from database. Null if can't be found
     */
    fun getGame(gameId: Long) : Game? {
        val gameDb = db.gameDAO.get(gameId) ?: return null
        val challengeDb = db.challengeDAO.get(gameDb.challengeId) ?: return null
        return mapper.toGame(gameDb, challengeDb)
    }

    fun getPlayers(gameId:Long) : LiveData<List<PlayerDb>> {
        return db.playerDAO.getByGame(gameId)
    }

    fun getStoryboard(gameId: Long) : LiveData<StoryboardDb> {
        return db.storyboardDAO.getByGame(gameId)
    }

    fun getIdeas(phase: ChallengeDb.Phase, gameId: Long) : LiveData<List<IdeaDb>> {
        return db.ideaDAO.getByPhase(gameId,phase)
    }

}