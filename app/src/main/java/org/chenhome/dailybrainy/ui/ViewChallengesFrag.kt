package org.chenhome.dailybrainy.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import org.chenhome.dailybrainy.R
import timber.log.Timber

@AndroidEntryPoint // for injecting ViewModel "by viewModels()"
class ViewChallengesFrag : Fragment() {
    companion object {
        fun newInstance() = ViewChallengesFrag()
    }

    private val viewChallengesVM: ViewChallengesVM by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.view_challenges_frag, container, false)
        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                layoutManager = LinearLayoutManager(context)
                val challAdapter = ChallengeNGameAdapter()

                // Observe ViewModel data and update the adapter
                viewChallengesVM.challenges.observe(viewLifecycleOwner, Observer {

                    it?.let {
                        Timber.d("Observed challenges changed $it")
                        challAdapter.setChallenges(it)
                    }
                })
                viewChallengesVM.games.observe(viewLifecycleOwner, Observer {
                    it?.let {
                        Timber.d("Observed games changed $it")
                        challAdapter.setGames(it)
                    }
                })
                adapter = challAdapter
            }
        }
        return view
    }
}