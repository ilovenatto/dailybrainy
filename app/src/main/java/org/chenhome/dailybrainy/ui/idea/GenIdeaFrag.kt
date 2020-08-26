package org.chenhome.dailybrainy.ui.idea

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import org.chenhome.dailybrainy.R

class GenIdeaFrag : Fragment() {

    companion object {
        fun newInstance() = GenIdeaFrag()
    }

    private lateinit var viewModel: GenIdeaVM

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.gen_idea_frag, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(GenIdeaVM::class.java)
        // TODO: Use the ViewModel
    }

}