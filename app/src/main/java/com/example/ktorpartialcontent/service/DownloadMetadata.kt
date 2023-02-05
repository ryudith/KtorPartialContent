package com.example.ktorpartialcontent.service

import android.content.Context
import android.os.Environment
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DownloadMetadata (
    val context: Context,
    var url: String,
    var fileName: String,
    var filePath: String = Environment.DIRECTORY_DOWNLOADS,  // default save to Download folder
    var customFileName: Boolean = false,  // flag for testing with content-disposition
    var isComplete: Boolean = false,
    var totalBytes: Long = 0,
    var downloadedBytes: Long = 0,
    var chunkSize: Long = 0,  // flag for partial content
) {
    suspend fun saveToFile (data: ByteArray)
    {
        val file = File(Environment.getExternalStoragePublicDirectory(filePath).absolutePath, fileName)
        val fileOutputStream = context.contentResolver.openOutputStream(file.toUri(), "wa")
        if (fileOutputStream != null)
        {
            withContext(Dispatchers.IO) {
                fileOutputStream.write(data)
                fileOutputStream.close()
            }
        }
    }
}