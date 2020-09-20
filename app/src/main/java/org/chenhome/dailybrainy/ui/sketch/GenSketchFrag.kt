package org.chenhome.dailybrainy.ui.sketch

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
import org.chenhome.dailybrainy.databinding.GenSketchFragBinding
import org.chenhome.dailybrainy.repo.Idea
import org.chenhome.dailybrainy.repo.game.FullGame
import org.chenhome.dailybrainy.repo.game.Sketch
import org.chenhome.dailybrainy.ui.*
import org.chenhome.dailybrainy.ui.SketchAdapter.SketchVHListener
import timber.log.Timber

@AndroidEntryPoint
class GenSketchFrag : Fragment() {

    private val args: GenSketchFragArgs by navArgs()
    private val vm: SketchVM by viewModels {
        GameVMFactory(requireContext(), args.gameGuid)
    }

    private val playerAdap = PlayerAdapter()
    private val ideaAdap = IdeaAdapter()
    private val sketchAdap = SketchAdapter(SketchVHListener(
        { _ ->
            // do nothing on vote
        },
        { sketch ->
            vm.navToViewSketch(sketch)
        }), false
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val binding = GenSketchFragBinding.inflate(LayoutInflater.from(context), container, false)
        binding.vm = vm
        binding.listIdeas.adapter = ideaAdap
        binding.listPlayers.adapter = playerAdap
        binding.listSketches.adapter = sketchAdap
        binding.lifecycleOwner = viewLifecycleOwner
        binding.executePendingBindings()

        initAdapterObservers(vm.fullGame, ideaAdap, playerAdap, sketchAdap)
        initNavObserver(vm.navToNext, vm.navToCamera, vm.navToViewSketch)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        vm.generate.countdownTimer.start()
    }

    override fun onPause() {
        super.onPause()
        vm.generate.countdownTimer.cancel()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != IntentReq.REQ_IMAGE_CAPTURE
            || resultCode != Activity.RESULT_OK
        ) {
            Timber.w("Unable to capture image")
            return
        }
        // save photo in remote database.. which will eventually make it back to local filesystem
        // TODO: 9/19/20 show error
        vm.uploadSketch()
    }

    private fun initNavObserver(
        navToNext: LiveData<Event<Boolean>>,
        navToCamera: LiveData<Event<Boolean>>,
        navToViewSketch: LiveData<Event<Sketch>>,
    ) {
        navToNext.observe(viewLifecycleOwner, {
            it.contentIfNotHandled()?.run {
                findNavController().popBackStack()
            }
        })

        navToCamera.observe(viewLifecycleOwner, { event ->
            event.contentIfNotHandled()?.run {
                Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
                    if (intent
                            .resolveActivity(requireContext().packageManager)
                            .toString().isNotEmpty()
                    ) {
                        vm.genAndSetNewUri()
                        vm.sketchImageUri?.let { uri ->
                            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                            startActivityForResult(intent, IntentReq.REQ_IMAGE_CAPTURE)
                        } ?: Timber.w("Unable to launch capture image intent")
                    } else {
                        Timber.w("Unable to find activity to capture image")
                    }
                }
            }
        })

        navToViewSketch.observe(viewLifecycleOwner, {
            it.contentIfNotHandled()?.let { sketch ->
                findNavController().navigate(GenSketchFragDirections
                    .actionGenSketchFragToViewSketchFrag(
                        sketch.idea.gameGuid,
                        sketch.idea.guid))
            }
        })
    }


    private fun initAdapterObservers(
        fullGame: LiveData<FullGame>,
        ideaAdap: IdeaAdapter,
        playerAdap: PlayerAdapter,
        sketchAdap: SketchAdapter,
    ) {
        fullGame.observe(viewLifecycleOwner, { it ->
            it?.let { game ->
                Timber.d("Got challenge ${game.challenge.title}")
                ideaAdap.ideas = game.ideas(Idea.Origin.BRAINSTORM)
                playerAdap.players = game.players
                sketchAdap.sketches = game.ideas(Idea.Origin.SKETCH).map { idea -> Sketch(idea) }
            }
        })
    }

}

