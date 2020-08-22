package org.chenhome.dailybrainy.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import dagger.hilt.android.AndroidEntryPoint
import org.chenhome.dailybrainy.R
import timber.log.Timber

@AndroidEntryPoint
class ViewChallengesFrag : Fragment() {

    companion object {
        fun newInstance() = ViewChallengesFrag()
    }

    private val viewChallengesVM: ViewChallengesVM by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // TODO: 8/22/20 remove this test later


        Timber.d("userguid ${viewChallengesVM.userRepo.currentPlayerGuid}")
        viewChallengesVM.games.observe(viewLifecycleOwner, Observer {
            Timber.d("Observing gamestub $it")
        })
        viewChallengesVM.challenges.observe(viewLifecycleOwner, Observer {
            Timber.d("Observing challenges $it")
        })
        return inflater.inflate(R.layout.view_challenges_frag, container, false)
    }

}