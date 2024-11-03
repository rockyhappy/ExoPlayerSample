package com.example.exodemo

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class VideoDownloadWorker2(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val videoUrl = inputData.getString("VIDEO_URL") ?: return Result.failure()
        val file = File(applicationContext.filesDir, "downloaded_video.mp4")

        return try {
            val client = OkHttpClient()
            val request = Request.Builder().url(videoUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return Result.failure()
                file.outputStream().use { output ->
                    response.body?.byteStream()?.copyTo(output)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
