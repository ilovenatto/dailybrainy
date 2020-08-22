package org.chenhome.dailybrainy.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import org.chenhome.dailybrainy.R

class NewGameFrag : Fragment() {

    companion object {
        fun newInstance() = NewGameFrag()
    }

    private val viewModel: NewGameVM by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.new_game_fragment, container, false)
    }

}