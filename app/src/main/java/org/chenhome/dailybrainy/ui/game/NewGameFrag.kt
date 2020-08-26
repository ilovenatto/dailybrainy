package org.chenhome.dailybrainy.ui.game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import org.chenhome.dailybrainy.R
import timber.log.Timber

class NewGameFrag : Fragment() {

    companion object {
        fun newInstance() = NewGameFrag()
    }

    private val viewModel: NewGameVM by viewModels()
    private val args: NewGameFragArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Timber.d("got args ${args.challengeGuid}")
        return inflater.inflate(R.layout.new_game_frag, container, false)
    }

}