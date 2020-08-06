package org.chenhome.dailybrainy.repo.local

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*


/**
 * Local DB for all DailyBrainy database objects
 *
 * Local DB acts as the cache for data that is either prepopulated from app resources
 * or from the network. The respository will consider BrainyDb the source of truth.
 */
@Database(
    version = 1,
    entities = [
        Challenge::class,
        Game::class,
        PlayerSession::class,
        Idea::class
    ],
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BrainyDb : RoomDatabase() {
    abstract val playerSessionDAO: PlayerSessionDAO
    abstract val gameDAO: GameDAO
    abstract val challengeDAO: ChallengeDAO
    abstract val ideaDAO: IdeaDAO

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
}


/**
 * Converters used by Room database to convert primitives to complex types
 */
private class Converters {
    @TypeConverter
    fun fromStepName(stepName: String): Challenge.Step {
        return Challenge.Step.valueOf(stepName)
    }

    @TypeConverter
    fun fromStep(step: Challenge.Step): String {
        return step.name
    }

    @TypeConverter
    fun fromPhaseName(phaseName: String): Challenge.Phase {
        return Challenge.Phase.valueOf(phaseName)
    }

    @TypeConverter
    fun fromPhase(phase: Challenge.Phase): String {
        return phase.name
    }

    @TypeConverter
    fun fromCategory(category: Challenge.Category): String {
        return category.name
    }

    @TypeConverter
    fun fromCategoryName(categoryName: String): Challenge.Category {
        return Challenge.Category.valueOf(categoryName)
    }

    @TypeConverter
    fun fromOrigin(origin: Idea.Origin): String {
        return origin.name
    }

    @TypeConverter
    fun fromOriginName(originName: String): Idea.Origin {
        return Idea.Origin.valueOf(originName)
    }
}

@Dao
interface ChallengeDAO {
    @Insert
    fun insert(challenge: Challenge): Long

    @Query("DELETE FROM challenge WHERE guid = :guid")
    fun delete(guid: String): Int

    @Update
    fun update(challege: Challenge): Int

    @Query("select * from challenge")
    fun getAll(): List<Challenge>

    @Query("select * from challenge where guid=:guid")
    fun get(guid: String): Challenge?
}

@Dao
interface GameDAO {
    @Insert
    fun insert(game: Game): Long

    @Query("DELETE FROM game WHERE guid = :guid")
    fun delete(guid: String): Int

    @Update
    fun update(game: Game): Int

    @Query("select * from game where guid=:guid")
    fun get(guid: String): Game?

    @Query("select * from game where playerGuid=:playerGuid")
    fun getByPlayerLive(playerGuid: String): LiveData<List<Game>>

    @Query("select * from game where playerGuid=:playerGuid")
    fun getByPlayer(playerGuid: String): List<Game>

}

@Dao
interface PlayerSessionDAO {
    @Insert
    fun insert(player: PlayerSession): Long

    @Query("DELETE FROM playersession WHERE guid = :guid")
    fun delete(guid: String): Int

    @Update()
    fun update(player: PlayerSession): Int

    @Query("select * from playersession where playerGuid=:playerGuid")
    fun getByPlayer(playerGuid: String): PlayerSession?

    @Query("select * from playersession where guid=:guid")
    fun get(guid: String): PlayerSession?

    @Query("select * from playersession where gameGuid=:gameGuid")
    fun getByGameLive(gameGuid: String): LiveData<List<PlayerSession>>
}


@Dao
interface IdeaDAO {
    @Insert
    fun insert(idea: Idea): Long

    @Query("delete from idea where guid=:guid")
    fun delete(guid: String): Int

    @Update()
    fun update(idea: Idea): Int

    @Query("select * from idea where guid=:guid")
    fun get(guid: String): Idea?

    @Query("select * from idea where origin=:origin and gameGuid=:gameGuid")
    fun getByOriginLive(gameGuid: String, origin: Idea.Origin): LiveData<List<Idea>>
}
