package org.chenhome.dailybrainy.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import org.chenhome.dailybrainy.R
import org.chenhome.dailybrainy.repo.Challenge
import timber.log.Timber

/**
 * Used as a wrapper for data that is exposed via a LiveData that represents an event.
 *
 * Thanks to Jose Alcerra [https://medium.com/androiddevelopers/livedata-with-snackbar-navigation-and-other-events-the-singleliveevent-case-ac2622673150]
 */
open class Event<out T>(private val content: T) {
    var hasBeenHandled = false
        private set // Allow external read but not write

    /**
     * Returns the content and prevents its use again.
     */
    fun contentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    /**
     * Returns the content, even if it's already been handled.
     */
    fun peekContent(): T = content
}

/**
 * An [Observer] for [Event]s, simplifying the pattern of checking if the [Event]'s content has
 * already been handled.
 * [onEventUnhandledContent] is *only* called if the [Event]'s contents has not been handled.
 *
 * Usage like
 * `someEvent.observe(this@MyActivity, EventObserver(::someFunctionInFragment))`
 */
class EventObserver<T>(private val onEventUnhandledContent: (T) -> Unit) : Observer<Event<T>> {
    override fun onChanged(event: Event<T>?) {
        event?.contentIfNotHandled()?.let { value ->
            onEventUnhandledContent(value)
        }
    }
}

data class UiError(
    val titleResId: Int, // String resource id describing the error
    val actionResId: Int, // String resource id describing the action one can take
)

/**
 * Binding adapter for "app:imageUrl=.." for <ImageView> XML elements.
 * Used during XML binding
 *
 * @param imgView
 * @param imgUri
 */
@BindingAdapter("imageUrl") // creates new attribute
fun bindImage(imgView: ImageView, imgUri: Uri?) {
    Timber.d("Binding $imgUri to ${imgView}")
    imgUri?.let {
        val imgUrl = imgUri
            .buildUpon()
            .scheme("https")
            .build()
        Timber.d("Loading image $imgUrl")
        Glide.with(imgView.context)
            .load(imgUrl)
            .apply(RequestOptions() // set error and loading placeholders
                .placeholder(R.drawable.ic_broken_image)
                .error(R.drawable.ic_broken_image))
            .into(imgView)
    }
}

// Dummy class to mark a Placeholder data item
data class PlaceholderDummy(
    val title: String,
    val desc: String, // desc optional
)

// UI value holder for Game step information
data class GameStep
    (
    val step: Challenge.Step,
    val isComplete: Boolean,
    val isCurrentStep: Boolean,
    val numStuff: Int,
) {
    fun iconDrawable(context: Context): Drawable? {
        if (isCurrentStep) return context.getDrawable(R.drawable.current_step)
        return if (isComplete) context.getDrawable(R.drawable.baseline_check_circle_24)
        else context.getDrawable(R.drawable.baseline_check_circle_outline_24)
    }

    fun description(context: Context): String {
        return when (step) {
            Challenge.Step.GEN_IDEA -> context.getString(R.string.num_ideas, numStuff)
            Challenge.Step.VOTE_IDEA -> context.getString(R.string.num_votes, numStuff)
            Challenge.Step.GEN_SKETCH -> context.getString(R.string.num_sketches, numStuff)
            Challenge.Step.VOTE_SKETCH -> context.getString(R.string.num_votes, numStuff)
            Challenge.Step.CREATE_STORYBOARD -> context.getString(R.string.num_votes, numStuff)
            Challenge.Step.VIEW_STORYBOARD -> context.getString(R.string.num_storypanels, numStuff)
        }
    }
}