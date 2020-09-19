package org.chenhome.dailybrainy.repo.image

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.*
import java.nio.channels.FileChannel
import java.util.*
import javax.inject.Inject

data class LocalFolder(val context: Context, val location: File?)

/**
 * Facade over DailyBrainy's FileProvider, an app-specific location for local files.
 *
 * Used to:
 * - Offer local files for Camera app to write images to.
 * - Save and delete local files to app-specific location
 * - File is later retrieved via its URI using a content resolver
 */
class LocalImageRepo @Inject constructor(
    @ApplicationContext val context: Context,
) {
    val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Store local files using File references provided by this app's FileProvider.
     * FileProvider's configuration only manages files located in paths described
     * by
     * > xml/paths.xml
     * This FileProvider has made following paths available.
     */
    // Matches path from Context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val LOCALFOLDER_PICS =
        LocalFolder(
            context,
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        )

    // Matches path from Context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
    val LOCALFOLDER_DOWNLOADS =
        LocalFolder(
            context,
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        )


    /**
     * Offer File URI for Camera app (and other apps) to write a local file into
     * DailyBrainy's private file storage location.
     *
     * @return null if no Uri could be created
     */
    fun makeFileUri(folder: LocalFolder): Uri? {
        try {
            val tmpFile = File.createTempFile(UUID.randomUUID().toString(), null, folder.location)
            val uri =
                FileProvider.getUriForFile(context, context.applicationContext.packageName, tmpFile)
            Timber.d("Make temp file uri $uri in packge ${context.applicationContext.packageName}")
            return uri
        } catch (e: IOException) {
            Timber.w("Unable to create temp file: ${e.message}")
            return null
        }

    }

    /**
     * @param uri Uri returned by [makeFileUri]
     */
    fun deleteLocal(uri: Uri): Boolean =
        context.contentResolver.delete(uri, null, null) > 0

    fun isExist(uri: Uri): Boolean {
        try {
            val sizeBytes = context.contentResolver.openFileDescriptor(uri, "r")?.statSize
            Timber.d("File $uri is of size $sizeBytes")
            return if (sizeBytes == null) false else sizeBytes > 0
        } catch (e: FileNotFoundException) {
            Timber.w("This file doesn't exist: $uri")
            return false
        }
    }

    /**
     * Called in the IO thread
     * @param srcUri:Uri produced by [makeFileUri]
     * @return URI of the newly saved file. Else null if unable to save file
     */
    suspend fun saveLocal(srcUri: Uri, folder: LocalFolder): Uri? {
        return withContext<Uri?>(scope.coroutineContext) {
            var src: FileChannel? = null
            var dst: FileChannel? = null
            try {
                // Make destination file
                val dstUri = makeFileUri(folder) ?: return@withContext null

                src = FileInputStream(srcUri.toFile()).channel
                dst = FileOutputStream(
                    context.contentResolver
                        .openFileDescriptor(dstUri, "rw")?.fileDescriptor
                ).channel
                dst.transferFrom(src, 0, src.size())
                return@withContext dstUri
            } catch (e: IOException) {
                Timber.w("Unable to write file $srcUri, error: ${e.message}")
                return@withContext null
            } finally {
                src?.close()
                dst?.close()
            }

        }

    }

}

