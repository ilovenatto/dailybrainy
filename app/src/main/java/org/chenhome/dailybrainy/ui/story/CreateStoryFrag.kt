package org.chenhome.dailybrainy.ui.story

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import org.chenhome.dailybrainy.databinding.CreateStoryFragBinding
import org.chenhome.dailybrainy.repo.Idea
import org.chenhome.dailybrainy.repo.game.FullGame
import org.chenhome.dailybrainy.repo.game.Sketch
import org.chenhome.dailybrainy.ui.Event
import org.chenhome.dailybrainy.ui.GameVMFactory
import org.chenhome.dailybrainy.ui.PlayerAdapter
import org.chenhome.dailybrainy.ui.SketchAdapter
import org.chenhome.dailybrainy.ui.sketch.SketchVM
import timber.log.Timber

@AndroidEntryPoint
class CreateStoryFrag : Fragment() {

    private val args: CreateStoryFragArgs by navArgs()
    private val vm: SketchVM by viewModels {
        GameVMFactory(requireContext(), args.gameGuid)
    }

    private val sketchListener = SketchAdapter.SketchVHListener(
        { sketch -> // onvote
            vm.vote.incrementVoteRemotely(sketch.idea)
        }, { sketch -> // onview
            vm.navToViewSketch(sketch)
        })

    private val playerAdap = PlayerAdapter()
    private val settingAdap = SketchAdapter(sketchListener, true)
    private val solutionAdap = SketchAdapter(sketchListener, true)
    private val resolutionAdap = SketchAdapter(sketchListener, true)
    private lateinit var binding: CreateStoryFragBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = CreateStoryFragBinding.inflate(inflater, container, false)

        binding.vm = vm
        binding.listPlayers.adapter = playerAdap
        binding.listSetting.adapter = settingAdap
        binding.listSolution.adapter = solutionAdap
        binding.listResolution.adapter = resolutionAdap

        binding.lifecycleOwner = viewLifecycleOwner
        binding.executePendingBindings()

        initAdapterObservers(vm.fullGame, playerAdap, settingAdap, solutionAdap, resolutionAdap)
        initNavObserver(vm.navToNext, vm.navToViewSketch, vm.navToCamera)
        return binding.root

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK
            || (requestCode < 0 || requestCode >= Idea.Origin.values().size)
        ) {
            Timber.w("Unable to capture image")
            return
        }
        // already checked array bounds
        vm.uploadSketch(Idea.Origin.values()[requestCode])
    }


    private fun initAdapterObservers(
        fullGame: LiveData<FullGame>,
        playerAdap: PlayerAdapter,
        settingAdap: SketchAdapter,
        solutionAdap: SketchAdapter,
        resolutionAdap: SketchAdapter,
    ) {
        fullGame.observe(viewLifecycleOwner, {
            it?.let { game ->
                playerAdap.players = game.players
                settingAdap.sketches = game.ideas(Idea.Origin.STORY_SETTING).map { Sketch(it) }
                solutionAdap.sketches = game.ideas(Idea.Origin.STORY_SOLUTION).map { Sketch(it) }
                resolutionAdap.sketches =
                    game.ideas(Idea.Origin.STORY_RESOLUTION).map { Sketch(it) }
            }
        })


    }

    private fun initNavObserver(
        navToNext: LiveData<Event<Boolean>>,
        navToViewSketch: LiveData<Event<Sketch>>,
        navToCamera: LiveData<Event<Idea.Origin>>,
    ) {
        navToNext.observe(viewLifecycleOwner, {
            it.contentIfNotHandled()?.run {
                findNavController().popBackStack()
            }
        })
        navToViewSketch.observe(viewLifecycleOwner, {
            it.contentIfNotHandled()?.let { sketch ->
                findNavController()
                    .navigate(CreateStoryFragDirections.actionCreateStoryFragToViewSketchFrag(
                        sketch.idea.gameGuid,
                        sketch.idea.guid))
            }
        })

        navToCamera.observe(viewLifecycleOwner, { event ->
            event.contentIfNotHandled()?.let { origin ->
                Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
                    if (intent
                            .resolveActivity(requireContext().packageManager)
                            .toString().isNotEmpty()
                    ) {
                        vm.genAndSetNewUri()
                        vm.sketchImageUri?.let { uri ->
                            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                            // use Origin's ordinal as the Intent request code
                            startActivityForResult(intent, origin.ordinal)
                        } ?: Timber.w("Unable to launch capture image intent")
                    } else {
                        Timber.w("Unable to find activity to capture image")
                    }
                }
            }
        })
    }

}