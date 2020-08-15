package org.chenhome.dailybrainy.repo.image

import android.content.Context
import android.net.Uri
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


/**
 * Folder names under the root Firebase Storage root for
 * this app's account
 */
enum class RemoteImageFolder {
    CHALLENGES,
    IDEAS,
}

/**
 * Facade over Firebase Storage.
 * Used to upload images.
 * Depend on Glide to retrieve and cache images locally using Glide cache.
 *
 * App layer should use Glide to retrieve remote images. App layer
 * references images by
 */
class RemoteImage(
    val context: Context
) {
    val fstorage: FirebaseStorage = Firebase.storage
    val scope = CoroutineScope(Dispatchers.IO)

    /**
     * @param fileUri local file reference offered by this app's FileProvider.
     * Use {@link LocalImageRepo.makeFileUri()}
     * @return StorageReference to newly uploaded file. Returns null if upload fails
     */
    suspend fun upload(folder: RemoteImageFolder, fileUri: Uri): StorageReference? {
        return withContext(scope.coroutineContext) {
            suspendCoroutine<StorageReference?> { continuation ->
                val ref = fstorage
                    .getReference(
                        folder.name
                                + "/"
                                + fileUri.lastPathSegment
                    )

                Timber.d("Uploading file $fileUri to $ref")
                val upload = ref.putFile(fileUri)
                upload.addOnSuccessListener {
                    Timber.d("Put file to $it.storage")
                    continuation.resume(it.storage)
                }
                upload.addOnFailureListener {
                    Timber.w("Got failure on upload")
                    continuation.resume(null)
                }
            }

        }
    }

    /**
     * The fullRemotePath is stored in the database. Each path typically represents an image. Use this method
     * to validate that the fullRemotePath is a valid path that references an actual remote asset.
     *
     * @return non-null StorageReference if {@param fullRemotePath} is a valid path. else null.
     *
     * @param fullRemotePath corresponds to the value from this
     * method [getPath()](https://developers.google.com/android/reference/com/google/firebase/storage/StorageReference#getPath())
     */
    suspend fun getValidStorageRef(fullRemotePath: String): StorageReference? {
        return withContext(scope.coroutineContext) {
            suspendCoroutine<StorageReference?> { cont ->
                val ref = fstorage.getReference(fullRemotePath)
                ref.metadata
                    .addOnSuccessListener {
                        Timber.d("Identified valid file ${it.path} of size ${it.sizeBytes}")
                        cont.resume(ref)
                    }
                    .addOnFailureListener {
                        Timber.d("Unable to find remote file with path $fullRemotePath")
                        cont.resume(null)
                    }
            }
        }
    }

    /**
     * @param target Firebase remote asset that will be deleted
     * @return whether delete succeeded
     */
    suspend fun deleteRemote(target: StorageReference): Boolean {
        return withContext(scope.coroutineContext) {
            suspendCoroutine<Boolean> { cont ->
                target.delete()
                    .addOnSuccessListener {
                        Timber.d("Deleted remote file $target")
                        cont.resume(true)
                    }
                    .addOnFailureListener {
                        Timber.w("Unable to delete remote file $target")
                        cont.resume(false)
                    }
            }
        }
    }
}
