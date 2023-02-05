package com.example.ktorpartialcontent.service

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay

class ClientHelper
{
    companion object
    {
        private var instance: ClientHelper? = null
        fun createInstance (): ClientHelper
        {
            if (instance == null)
            {
                instance = ClientHelper()
                instance!!.initialize()
            }

            return instance!!
        }
    }

    private var client: HttpClient? = null
    private fun initialize ()
    {
        client = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json()
            }
        }
    }

    suspend fun download (downloadMetadata: DownloadMetadata): String?
    {
        try
        {
            if (downloadMetadata.chunkSize == 0L)
            {
                return withoutPartialContent(downloadMetadata)
            }
            else
            {
                return withPartialContent(downloadMetadata)
            }
        }
        catch (e: Exception)
        {
            return null
        }
    }

    private suspend fun withoutPartialContent (downloadMetadata: DownloadMetadata): String?
    {
        try
        {
            downloadMetadata.saveToFile(runRequest(downloadMetadata))
            return "Content-Length: ${downloadMetadata.totalBytes}"
        }
        catch (e: Exception)
        {
            return null
        }
    }

    private suspend fun withPartialContent (downloadMetadata: DownloadMetadata): String?
    {
        try
        {
            var downloadPercent: Double
            while (!downloadMetadata.isComplete)
            {
                val fileContent = runRequest(downloadMetadata)
                if (fileContent.size > 0)
                {
                    downloadMetadata.saveToFile(fileContent)
                }

                // flag download finish
                if (fileContent.size == 0 || downloadMetadata.downloadedBytes >= downloadMetadata.totalBytes)
                {
                    downloadMetadata.isComplete = true
                    downloadMetadata.downloadedBytes = downloadMetadata.totalBytes
                }
                else
                {
                    downloadMetadata.downloadedBytes += fileContent.size  // counter bytes
                }

                // simulate pause-resume after 60% download
                downloadPercent = downloadMetadata.downloadedBytes / downloadMetadata.totalBytes.toDouble() * 100
                if (downloadPercent > 60)
                {
                    Log.d("DEBUG_DATA", "Pause partial content for 5s")
                    delay(5000)
                }
            }

            return "total bytes ${downloadMetadata.totalBytes} - downloaded bytes ${downloadMetadata.downloadedBytes}"
        }
        catch (e: Exception)
        {
            return null
        }
    }

    private suspend fun runRequest (downloadMetadata: DownloadMetadata): ByteArray
    {
        val content = client!!.get {
            url(downloadMetadata.url)

            // flag for testing with Content-Disposition
            if (downloadMetadata.customFileName)
            {
                headers {
                    append("Custom-Name", downloadMetadata.customFileName.toString())
                }
            }

            // flag for partial content
            if (downloadMetadata.chunkSize > 0)
            {
                // can not use onDownload for counter because run async
                headers {
                    append(HttpHeaders.Range, "bytes=${downloadMetadata.downloadedBytes}-${downloadMetadata.downloadedBytes + downloadMetadata.chunkSize}")
                }
            }
            else
            {
                onDownload { bytesSentTotal, contentLength ->
                    downloadMetadata.downloadedBytes = bytesSentTotal
                    downloadMetadata.totalBytes = contentLength
                }
            }
        }

        val contentHeaders = content.headers
        val contentDisposition = contentHeaders[HttpHeaders.ContentDisposition]?.let { ContentDisposition(it) }
        if (contentDisposition != null)
        {
            val tmp = contentDisposition.disposition.split("=")
            if (tmp.size > 1)
            {
                downloadMetadata.fileName = tmp[1].replace("\"", "")
            }
        }

        if (downloadMetadata.chunkSize > 0)
        {
            // set content size
            if (downloadMetadata.totalBytes == 0L && contentHeaders.contains(HttpHeaders.ContentRange) &&
                    contentHeaders[HttpHeaders.ContentRange] != null)
            {
                val tmp = contentHeaders[HttpHeaders.ContentRange]!!.split("/")
                if (tmp.size == 2)
                {
                    val fileSize = tmp[1].toLongOrNull()
                    if (fileSize == null)
                    {
                        downloadMetadata.isComplete = true  // if no content size, then mark complete
                    }
                    else
                    {
                        downloadMetadata.totalBytes = fileSize
                    }
                }
            }
        }

        return content.body()
    }









}