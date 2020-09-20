package org.chenhome.dailybrainy.repo

import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.database.FirebaseDatabase
import org.chenhome.dailybrainy.repo.game.GameStub
import org.chenhome.dailybrainy.repo.helper.ChallengeObserver
import org.chenhome.dailybrainy.repo.helper.GameStubObserver
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Singleton that offers observable domain objects like [Challenge] to clients. These domain objects will
 * notify their observers when their state has changed. Obtain instance by injecting with Hilt.
 * `@Inject val brainyRepo:BrainRepo`
 *
 *
 * This singleton's setup and teardown lifecycle is dictated by [ProcessLifecycleOwner]. Data is only available
 * after the lifecycle has begun.
 *
 */
@Singleton
class BrainyRepo @Inject constructor(
    val userRepo: UserRepo, // Injected
) : LifecycleObserver {
    private val fireDb: FirebaseDatabase = FirebaseDatabase.getInstance()

    /**
     * Remote data being observed
     */
    private val challengeObs =
        ChallengeObserver()
    private val gameStubObs =
        GameStubObserver(userRepo.currentPlayerGuid)

    init {
        Timber.d("Challenge and GameStub remote observers registered")
        ProcessLifecycleOwner.get().lifecycle.run {
            addObserver(challengeObs)
            addObserver(gameStubObs)
        }


    }

    /**
     * Insert player session bound to existing Game. Players live at
     * `/playersessions/<gameguid>/<player guid>/<player obj>
     *
     * @param gameGuid
     * @param player
     * @return new player session's Guid. Else null if insertion failed
     */
    suspend fun insertPlayerSession(
        gameGuid: String,
        player: PlayerSession,
    ): String? {
        try {// Insert session
            val sessionRef = fireDb.getReference(DbFolder.PLAYERSESSION.path)
                .child(gameGuid)
                .push()

            // Return player Guid if insertion successful, else null
            return suspendCoroutine<String?> { cont ->
                sessionRef.key?.let { playerGuid ->
                    player.guid = playerGuid
                    // rest is filled out by the user in the UI

                    if (player.isValid()) {
                        sessionRef.setValue(player) { error, _ ->
                            error?.let {
                                Timber.w("Unable to insert player session $player in location $sessionRef")
                                cont.resume(null)
                            } ?: run {
                                Timber.d("Successfully inserted new session $playerGuid")
                                cont.resume(playerGuid)
                            }
                        }
                    } else {
                        Timber.w("Invalid player $player. Unable to insert")
                        cont.resume(null)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e("Unable to insert player $player, $e")
            return null
        }
    }

    /**
     * Inserts new game into remote database. Also inserts a new PlayerSession.
     * Typically launched from a viewModelScope context.
     *
     * @return gameGuid of newly created game. Else null if insertion failed
     */
    suspend fun insertNewGame(
        challengeGuid: String,
        player: PlayerSession,
        userGuid: String,
    ): String? {
        try {// Insert game
            val gameRef = fireDb.getReference(DbFolder.GAMES.path).push()
            val gameGuid = suspendCoroutine<String?> { cont ->
                gameRef.key?.let { gameGuid ->
                    val game = Game(gameGuid, challengeGuid, userGuid)
                    gameRef.setValue(game) { error, _ ->
                        error?.let {
                            Timber.w("Unable to insert new game $error")
                            cont.resume(null)
                        } ?: cont.resume(gameGuid)
                    }
                } ?: cont.resume(null)
            } ?: return null

            // Insert session
            player.gameGuid = gameGuid
            player.userGuid = userGuid
            // rest is filled out by the user in the UI (presumably)
            val playerGuid = insertPlayerSession(gameGuid, player)

            // return gameGuid, else null if operation failed
            return suspendCoroutine<String?> { cont ->
                playerGuid?.let {
                    Timber.d("Successfully inserted new game $gameGuid for player $playerGuid")
                    cont.resume(gameGuid)
                } ?: run {
                    // delete obsolete game
                    Timber.w("Invalid player $player. Removing obsolete game.")
                    gameRef.removeValue { error, _ ->
                        error?.let { Timber.w("Unable to remove obsolete game $gameRef") }
                    }
                    cont.resume(null)
                }
            }
        } catch (e: Exception) {
            Timber.e("Unable to insert new game for player $player and $challengeGuid, $e")
            return null
        }
    }


    /**
     * List of challenges offered by DailyBrainy. They only change
     * when the app has published new challenges.
     */
    val challenges: LiveData<List<Challenge>> = challengeObs._challenges
    val lessons: LiveData<List<Challenge>> = challengeObs._lessons

    val todayLesson: LiveData<Challenge> = challengeObs._todayLesson
    val todayChallenge: LiveData<Challenge> = challengeObs._todayChallenge

    /**
     * List of [GameStub] started by the current user. [GameStub] are
     * game session that the user has participated in.
     */
    val gameStubs: LiveData<List<GameStub>> = gameStubObs._gameStubs

}
