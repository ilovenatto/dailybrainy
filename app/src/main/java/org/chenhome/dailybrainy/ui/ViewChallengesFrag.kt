package org.chenhome.dailybrainy.ui

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.chenhome.dailybrainy.R

class ViewChallengesFrag : Fragment() {

    companion object {
        fun newInstance() = ViewChallengesFrag()
    }

    private lateinit var viewChallengesVM : ViewChallengesVM

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.view_challenges_frag, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewChallengesVM = ViewModelProviders.of(this).get(ViewChallengesVM::class.java)
        // TODO: Use the ViewModel
    }

}