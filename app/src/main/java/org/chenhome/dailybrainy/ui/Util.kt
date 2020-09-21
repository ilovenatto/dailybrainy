package org.chenhome.dailybrainy.ui

import android.net.Uri
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import org.chenhome.dailybrainy.R
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
fun bindImage(imgView: ImageView, imgUri: Uri?): Unit {
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