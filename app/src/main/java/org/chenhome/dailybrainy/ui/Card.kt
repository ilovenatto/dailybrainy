package org.chenhome.dailybrainy.ui.story

import org.chenhome.dailybrainy.repo.game.Sketch

// Callback for when user wants to view sketch
class SketchCardListener(val onViewListener: (Sketch) -> Unit) {
    fun onView(sketch: Sketch) {
        onViewListener(sketch)
    }
}