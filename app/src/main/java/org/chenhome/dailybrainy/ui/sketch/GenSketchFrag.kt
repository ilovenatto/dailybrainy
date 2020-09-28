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
import org.chenhome.dailybrainy.repo.game.Sketch
import org.chenhome.dailybrainy.ui.*
import timber.log.Timber

@AndroidEntryPoint
class GenSketchFrag : Fragment() {

    companion object {
        const val NUM_POPULARIDEAS = 2
    }

    private val args: GenSketchFragArgs by navArgs()
    private val vm: SketchVM by viewModels {
        GameVMFactory(requireContext(), args.gameGuid)
    }

    private val playerAdap = PlayerSheetAdapter()
    private val ideaAdap = IdeaAdapter(false)

    private val sketchAdap = SketchAdapter(SketchVHListener(
        {
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
        with(binding.avatars) {
            listPlayers.adapter = playerAdap.playerAdapter
            listThumbs.adapter = playerAdap.thumbAdapter
        }

        binding.listSketches.adapter = sketchAdap

        binding.lifecycleOwner = viewLifecycleOwner
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.executePendingBindings()

        vm.fullGame.observe(viewLifecycleOwner, {
            it?.let { game ->
                Timber.d("Got challenge ${game.challenge.title}")
                ideaAdap.ideas = game.mostPopularIdeas(Idea.Origin.BRAINSTORM, NUM_POPULARIDEAS)
                playerAdap.setGame(it)
                sketchAdap.sketches = game.ideas(Idea.Origin.SKETCH).map { idea -> Sketch(idea) }
            }
        })
        initNavObserver(vm.navToCamera, vm.navToViewSketch)
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
        if (requestCode != Idea.Origin.SKETCH.ordinal
            || resultCode != Activity.RESULT_OK
        ) {
            Timber.w("Unable to capture image")
            return
        }
        // save photo in remote database.. which will eventually make it back to local filesystem
        // TODO: 9/19/20 show error
        vm.uploadSketch(Idea.Origin.SKETCH)
    }

    private fun initNavObserver(
        navToCamera: LiveData<Event<Idea.Origin>>,
        navToViewSketch: LiveData<Event<Sketch>>,
    ) {
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
                            startActivityForResult(intent, Idea.Origin.SKETCH.ordinal)
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



}

