package org.chenhome.dailybrainy.repo.local

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.chenhome.dailybrainy.R
import org.chenhome.dailybrainy.repo.Game

import timber.log.Timber
import java.util.*


/**
 * Local DB for all DailyBrainy database objects
 *
 * Local DB acts as the cache for data that is either prepopulated from app resources
 * or from the network. The respository will consider BrainyDb the source of truth.
 */
@Database(
    version = 1,
    entities = [
        ChallengeDb::class,
        GameDb::class,
        PlayerDb::class,
        StoryboardDb::class,
        IdeaDb::class,
        LessonDb::class
    ],
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BrainyDb : RoomDatabase() {
    abstract val storyboardDAO: StoryboardDAO
    abstract val playerDAO: PlayerDAO
    abstract val gameDAO: GameDAO
    abstract val challengeDAO: ChallengeDAO
    abstract val ideaDAO: IdeaDAO
    abstract val lessonDAO: LessonDAO

    // Singletone instance
    companion object {
        @Volatile
        var INSTANCE: BrainyDb? = null

        /**
         * Singleton getter
         */
        fun getDb(
            context: Context
        ): BrainyDb {
            val tmpInst = INSTANCE
            // use smart cast to check it's null and of the type, BrainyDb
            if (tmpInst != null) {
                return tmpInst
            }


            // Synchronize on static companion object (there's only one)
            synchronized(this) {
                val inst = Room.databaseBuilder(
                    context.applicationContext,
                    BrainyDb::class.java,
                    "dailybrainy_db"
                )
                  //  .addCallback(BrainyDbCallback(context))
                    .build()
                INSTANCE = inst
                return inst
            }
        }
    }

    object BrainyDbHelper {
        fun getChallengesFromJson(context: Context): List<ChallengeDb>? {
            // Get json file
            val challengeStr: String = context
                .resources
                .openRawResource(R.raw.brainydb_challenges)
                .bufferedReader()
                // closes the inputstream
                .use {
                    it.readText()
                }
            if (challengeStr.isNullOrEmpty()) {
                Timber.w("Unable preload Brainy db with challenges")
                return null
            }
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            val adapter: JsonAdapter<List<ChallengeDb>> = moshi
                .adapter(Types.newParameterizedType(List::class.java, ChallengeDb::class.java))

            // get result
            val result = adapter.fromJson(challengeStr)
            if (result.isNullOrEmpty()) {
                Timber.w("No challenges in json file. Unable to preload Brainy db")
                return null
            }
            Timber.i("Got Challenges from JSON: ${result.size}")
            return result
        }
    }
}


private class Converters {
    @TypeConverter
    fun fromStepName(stepName:String) : ChallengeDb.Step {
        return ChallengeDb.Step.valueOf(stepName)
    }

    @TypeConverter
    fun fromStep(step: ChallengeDb.Step) : String {
        return step.name
    }
    @TypeConverter
    fun fromPhaseName(phaseName:String) : ChallengeDb.Phase {
        return ChallengeDb.Phase.valueOf(phaseName)
    }

    @TypeConverter
    fun fromPhase(phase: ChallengeDb.Phase) : String {
        return phase.name
    }
}

@Dao
interface ChallengeDAO {
    @Insert
    fun insert(challengeDb: ChallengeDb): Long

    @Delete
    fun delete(challegeDb: ChallengeDb): Int

    @Update
    fun update(challegeDb: ChallengeDb): Int
    
    @Query("select * from challengedb")
    fun getAll(): LiveData<List<ChallengeDb>>

    @Query("select * from challengedb")
    fun getAllBlocking(): List<ChallengeDb>

    @Query("select * from challengedb where id=:challengeId")
    fun get(challengeId: Long): ChallengeDb
}

@Dao
abstract class GameDAO {
    @Insert
    abstract fun insert(game: GameDb): Long

    @Delete
    abstract fun delete(game: GameDb): Int

    @Update
    abstract fun update(game: GameDb): Int

    @Query("select * from gamedb where id=:gameId")
    abstract fun get(gameId: Long): GameDb

    /**
     * All games are universally known by all players. Filter down
     * to the games where this user is one of the players
     * @param playerGuid globally unique identifier for the player
     * @return Games that player has participated in
     */
    @Query("SELECT * FROM playerdb INNER JOIN gamedb ON gamedb.id = playerdb.gameId WHERE playerdb.guid = :playerGuid")
    abstract fun getByPlayer(playerGuid: String) : LiveData<List<GameDb>>
}

@Dao
interface PlayerDAO {
    @Insert
    fun insert(player: PlayerDb): Long

    @Delete
    fun delete(player: PlayerDb): Int

    @Update()
    fun update(player: PlayerDb): Int

    @Query("select * from playerdb where id=:playerId")
    fun get(playerId: Long): PlayerDb

    @Query("select * from playerdb where gameId=:gameId")
    fun getByGame(gameId: Long): LiveData<List<PlayerDb>>

    @Query("select * from playerdb where guid=:guid")
    fun getByGuid(guid: String) : PlayerDb


}


@Dao
interface StoryboardDAO {
    @Insert
    fun insert(story: StoryboardDb): Long

    @Delete
    fun delete(story: StoryboardDb): Int

    @Update()
    fun update(story: StoryboardDb): Int

    @Query("select * from storyboarddb where id=:storyboardId")
    fun get(storyboardId: Long): LiveData<StoryboardDb>

    @Query("select * from storyboarddb where gameId=:gameId")
    fun getByGame(gameId: Long): LiveData<StoryboardDb>

}

@Dao
interface IdeaDAO {
    @Insert
    fun insert(idea: IdeaDb): Long

    @Delete
    fun delete(idea: IdeaDb): Int

    @Update()
    fun update(idea: IdeaDb): Int

    @Query("select * from ideadb where id=:ideaId")
    fun get(ideaId: Long): IdeaDb

    @Query("select * from ideadb where phase=:phase and gameId=:gameId")
    fun getByPhase(gameId: Long, phase: ChallengeDb.Phase) : LiveData<List<IdeaDb>>

    @Query("select * from ideadb where gameId=:gameId")
    fun getAll(gameId: Long): List<IdeaDb>
}

@Dao
interface LessonDAO {
    @Insert
    fun insert(lesson: LessonDb): Long

    @Delete
    fun delete(lesson: LessonDb): Int

    @Update()
    fun update(lesson: LessonDb): Int

    @Query("select * from lessondb where id=:lessonId")
    fun get(lessonId: Long): LessonDb

    @Query("select * from lessondb")
    fun getAll(): LiveData<List<LessonDb>>
}