package org.chenhome.dailybrainy.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.chenhome.dailybrainy.ui.challenges.JoinGameVM
import org.chenhome.dailybrainy.ui.game.ViewGameVM
import org.chenhome.dailybrainy.ui.idea.IdeaVM
import org.chenhome.dailybrainy.ui.sketch.SketchVM
import org.chenhome.dailybrainy.ui.story.StoryVM


class GameVMFactory(
    private val context: Context,
    private val gameGuid: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IdeaVM::class.java)) {
            return IdeaVM(context, gameGuid) as T
        }
        if (modelClass.isAssignableFrom(SketchVM::class.java)) {
            return SketchVM(context, gameGuid) as T
        }
        if (modelClass.isAssignableFrom(StoryVM::class.java)) {
            return StoryVM(context, gameGuid) as T
        }
        if (modelClass.isAssignableFrom(ViewGameVM::class.java))
            return ViewGameVM(gameGuid, context) as T

        throw IllegalArgumentException("Unsupported ViewModel type $modelClass")
    }
}

class ChallengeVMFactory(
    private val context: Context,
    private val challengeGuid: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JoinGameVM::class.java)) {
            return JoinGameVM(context, challengeGuid) as T
        }
        throw IllegalArgumentException("Unsupported ViewModel type $modelClass")
    }
}