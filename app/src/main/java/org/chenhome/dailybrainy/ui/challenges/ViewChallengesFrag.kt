package org.chenhome.dailybrainy.ui.challenges

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import org.chenhome.dailybrainy.R
import org.chenhome.dailybrainy.repo.Challenge
import timber.log.Timber

@AndroidEntryPoint // for injecting ViewModel "by viewModels()"
class ViewChallengesFrag : Fragment() {

    private val viewChallengesVM: ViewChallengesVM by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = inflater.inflate(R.layout.view_challenges_frag, container, false)
        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                val challAdapter = ViewChallengesAdapter(
                    ChallengeListener { guid, category ->
                        when (category) {
                            Challenge.Category.CHALLENGE -> viewChallengesVM.navToNewGame(guid)
                            Challenge.Category.LESSON -> viewChallengesVM.navToLesson(guid)
                        }
                    },
                    GameListener { gameGuid ->
                        viewChallengesVM.navToExistingGame(gameGuid)
                    })

                // Observe ViewModel data and update the adapter
                viewChallengesVM.challenges.observe(viewLifecycleOwner, Observer {
                    it?.let {
                        Timber.d("Observed ${it.size} challenges changed")
                        challAdapter.setChallenges(it)
                    }
                })
                viewChallengesVM.games.observe(viewLifecycleOwner, Observer {
                    it?.let {
                        Timber.d("Observed ${it.size} games changed")
                        challAdapter.setGames(it)
                    }
                })
                adapter = challAdapter


            }
        }

        // observe ViewModel
        viewChallengesVM.navToNewGame.observe(viewLifecycleOwner, {
            // navigate
            it.contentIfNotHandled()?.let { challengeGuid ->
                val dir =
                    ViewChallengesFragDirections.actionViewChallengesFragToNewGameFrag(challengeGuid)
                this.findNavController().navigate(dir)
            }
        })

        viewChallengesVM.navToLesson.observe(viewLifecycleOwner, {
            // navigate
            it.contentIfNotHandled()?.let { challengeGuid ->
                val dir =
                    ViewChallengesFragDirections.actionViewChallengesFragToLessonFrag(challengeGuid)
                this.findNavController().navigate(dir)
            }
        })

        viewChallengesVM.navToExistingGame.observe(viewLifecycleOwner, {
            // navigate
            it.contentIfNotHandled()?.let { gameGuid ->
                val dir =
                    ViewChallengesFragDirections.actionViewChallengesFragToViewGameFrag(gameGuid)
                this.findNavController().navigate(dir)
            }
        })
        return view
    }
}
