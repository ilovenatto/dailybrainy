package org.chenhome.dailybrainy.ui.lesson

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import org.chenhome.dailybrainy.databinding.ViewLessonFragBinding
import org.chenhome.dailybrainy.ui.bindImage
import timber.log.Timber

@AndroidEntryPoint
class ViewLessonFrag : Fragment() {

    private val vm: LessonVM by viewModels()
    private val args: ViewLessonFragArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val binding = ViewLessonFragBinding.inflate(inflater, container, false)

        vm.loadLesson(args.challengeGuid)
        vm.lesson.observe(viewLifecycleOwner, { lesson ->
            binding.imageLesson.let {
                bindImage(it, lesson.challenge.imageUri)
            }
        })

        binding.vm = vm
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.lifecycleOwner = viewLifecycleOwner
        binding.executePendingBindings()

        vm.navToYoutube.observe(viewLifecycleOwner, {
            it.contentIfNotHandled()?.let { uri ->
                Intent(Intent.ACTION_VIEW).let { intent ->
                    intent.data = uri
                    startActivityForResult(intent, REQ_VID_VIEW)
                }
            } ?: Timber.w("No Youtube URL to open")
        })

        return binding.root
    }

    companion object {
        private const val REQ_VID_VIEW = 1
    }


}