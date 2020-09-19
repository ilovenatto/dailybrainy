package org.chenhome.dailybrainy.ui.sketch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import org.chenhome.dailybrainy.databinding.ViewSketchFragBinding
import timber.log.Timber

/**
 * Renders the idea specified by the full path to Firebase database Idea entity.
 */
@AndroidEntryPoint
class ViewSketchFrag : Fragment() {

    private val args: ViewSketchFragArgs by navArgs()
    private val vm: SketchStubVM by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val binding = ViewSketchFragBinding
            .inflate(LayoutInflater.from(requireContext()), container, false)

        if (args.gameGuid.isNotEmpty() && args.ideaGuid.isNotEmpty()) {
            vm.loadSketchStub(args.gameGuid, args.ideaGuid)
            vm.sketchStub.observe(viewLifecycleOwner, {
                binding.vm = vm
                binding.executePendingBindings()
            })
        } else {
            Timber.w("No sketch firebase path specified. Unable to load remote sketch.")
        }
        return binding.root
    }

}