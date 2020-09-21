package org.chenhome.dailybrainy.repo.game

import android.content.Context
import android.net.Uri
import org.chenhome.dailybrainy.repo.Challenge
import org.chenhome.dailybrainy.repo.Game
import org.chenhome.dailybrainy.repo.Idea
import org.chenhome.dailybrainy.repo.PlayerSession
import timber.log.Timber
import java.util.*

/**
 * Domain objects that align more closely with how the UI renders the data.
 * Typically composed of Data objects.
 */

/**
 * Represents the basic details of a user's
 * participation in a Game.
 */
data class GameStub(
    val game: Game,
    val playerSession: PlayerSession,
) {
    fun date(context: Context): String? =
        game.sessionStartMillis?.let { millis ->
            android.text.format.DateFormat.getDateFormat(context).format(Date(millis))
        } ?: null

    // Can get set at a later time
    var challenge: Challenge? = null
}

/**
 * Data object representing the full and comprehensive state
 * of the game, which includes the ideas generated during the game
 * and the players that participated.
 */
data class FullGame(
    var game: Game = Game(),
    var players: MutableList<PlayerSession> = mutableListOf(),
    var challenge: Challenge = Challenge(), // current challenge
) {
    private var _ideas: MutableList<Idea> = mutableListOf()

    fun mostPopularSketchUri(origin: Idea.Origin): Uri? =
        ideas(origin).maxByOrNull { it.votes }?.let { Sketch(it) }?.imgUri
            ?: null

    fun mostPopularSketch(origin: Idea.Origin): Sketch? =
        ideas(origin).maxByOrNull { it.votes }?.let { Sketch(it) }
            ?: null


    fun ideas(origin: Idea.Origin): List<Idea> = _ideas.filter { it.origin == origin }

    fun add(idea: Idea) {
        // doesn't exist
        if (_ideas.firstOrNull { idea.guid == it.guid } == null) {
            _ideas.add(idea)
        }
    }

    fun update(idea: Idea) {
        // doesn't exist
        val index = _ideas.indexOfFirst { idea.guid == it.guid }
        if (index >= 0) {
            _ideas[index] = idea
        }
    }

    fun remove(idea: Idea) {
        _ideas.removeIf { idea.guid == it.guid }
    }
}

/**
 * Idea decorated with an image URI
 */
data class Sketch(
    val idea: Idea,
) {
    val imgUri: Uri?
        get() = idea.imgUri?.let {
            val uri = Uri.parse(it)
            Timber.d("Parsed as uri $uri")
            uri
        }
}

