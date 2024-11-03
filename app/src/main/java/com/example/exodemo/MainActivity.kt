    package com.example.exodemo

    import android.os.Bundle
    import androidx.activity.ComponentActivity
    import androidx.activity.compose.setContent
    import androidx.compose.foundation.layout.Arrangement
    import androidx.compose.foundation.layout.Column
    import androidx.compose.foundation.layout.Spacer
    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.compose.foundation.layout.height
    import androidx.compose.material3.Button
    import androidx.compose.material3.MaterialTheme
    import androidx.compose.material3.Surface
    import androidx.compose.material3.Text
    import androidx.compose.runtime.Composable
    import androidx.compose.runtime.getValue
    import androidx.compose.runtime.mutableStateOf
    import androidx.compose.runtime.remember
    import androidx.compose.runtime.rememberCoroutineScope
    import androidx.compose.runtime.setValue
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.tooling.preview.Preview
    import androidx.compose.ui.unit.dp
    import androidx.core.net.toUri
    import androidx.work.OneTimeWorkRequestBuilder
    import androidx.work.WorkInfo
    import androidx.work.WorkManager
    import androidx.work.workDataOf
    import com.example.exodemo.ui.theme.ExoDemoTheme
    import com.google.android.exoplayer2.ExoPlayer
    import com.google.android.exoplayer2.MediaItem
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.launch
    import java.io.File
    import android.content.Context
    import android.net.Uri
    import android.util.Log
    import androidx.compose.foundation.layout.aspectRatio
    import androidx.compose.foundation.layout.fillMaxWidth
    import androidx.compose.runtime.DisposableEffect
    import androidx.compose.runtime.LaunchedEffect
    import androidx.compose.ui.platform.LocalContext
    import androidx.compose.ui.viewinterop.AndroidView
    import androidx.core.net.toUri
    import androidx.media3.exoplayer.DefaultLoadControl
    import androidx.media3.exoplayer.LoadControl
    import com.google.android.exoplayer2.PlaybackException
    import com.google.android.exoplayer2.Player
    import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
    import com.google.android.exoplayer2.ui.PlayerView

    class MainActivity : ComponentActivity() {
        private lateinit var exoPlayer: ExoPlayer
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContent {
                exoPlayer = ExoPlayer.Builder(this)
                    .build()
                exoPlayer.addListener(object : Player.Listener {
                    override fun onRenderedFirstFrame() {
                        Log.d("ExoPlayer", "First frame rendered")
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e("ExoPlayer", "Playback error: ${error.message}")
                    }
                })
                val context = LocalContext.current
                val workManager = WorkManager.getInstance(context)
                var downloadStatus by remember { mutableStateOf("Idle") }
                var isDownloaded by remember { mutableStateOf(false) }
                var showPlayer by remember { mutableStateOf(false) }

                val downloadRequest = OneTimeWorkRequestBuilder<VideoDownloadWorker>()
                    .setInputData(workDataOf("VIDEO_URL" to "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"))
                    .build()
                LaunchedEffect(workManager) {
                    workManager.getWorkInfoByIdLiveData(downloadRequest.id).observe(this@MainActivity) { workInfo ->
                        if (workInfo != null && workInfo.state.isFinished) {
                            if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                                downloadStatus = "Download Complete"
                                isDownloaded = true
                            } else {
                                downloadStatus = "Download Failed"
                            }
                        }
                    }
                }
                VideoDownloadScreen2(
                    downloadStatus = downloadStatus,
                    onDownloadClick = {
                        downloadStatus = "Downloading..."
                        workManager.enqueue(downloadRequest)
                    },
                    onPlayVideo = {
                        if (true) {
                            showPlayer = true
                            playVideo()
                        }
                    },
                    showPlayer = showPlayer
                )
    //            ExoDemoTheme {
    //                VideoDownloadScreen(
    //                    onDownloadClick = { startDownload() },
    //                    onPlayVideo = { playVideo() }
    //                )
    //            }
            }
        }
        //https://github.com/rockyhappy/Choco-Chip-Reader/assets/115190222/84cc7df5-f99e-4fc2-aeed-0f4380cc127a
        private fun startDownload() {
            val downloadRequest = OneTimeWorkRequestBuilder<VideoDownloadWorker>()
                .setInputData(workDataOf("VIDEO_URL" to "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"))
                .build()

            WorkManager.getInstance(this).enqueue(downloadRequest)

            WorkManager.getInstance(this).getWorkInfoByIdLiveData(downloadRequest.id).observe(this) { workInfo ->
                if (workInfo != null && workInfo.state.isFinished) {
                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                        // Trigger video playback once the download is successful
                        playVideo()
                    }
                }
            }
        }

        private fun playVideo() {
//            val videoUri = File(filesDir, "downloaded_video.mp4").toUri()
//            Log.d("MainActivity", "Playing video from: $videoUri")
//            exoPlayer.setMediaItem(MediaItem.fromUri(videoUri))
//            exoPlayer.prepare()
//            Log.d("MainActivity", "Playing video from: $videoUri")
//            exoPlayer.playWhenReady = true
            val videoFile = File(filesDir, "downloaded_video.mp4")
            if (videoFile.exists()) {
                val videoUri = videoFile.toUri()
                Log.d("MainActivity", "Playing video from: $videoUri")

                val mediaItem = MediaItem.Builder()
                    .setUri(videoUri)
                    .build()

                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            } else {
                Log.d("MainActivity", "Downloaded video file not found.")
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            exoPlayer.release()
        }
    }

    @Composable
    fun VideoDownloadScreen2(
        downloadStatus: String,
        onDownloadClick: () -> Unit,
        onPlayVideo: () -> Unit,
        showPlayer: Boolean,
    ) {
        val context = LocalContext.current
        val exoPlayer = remember {
            ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.fromUri(context.getVideoUri())
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
            }
        }
        DisposableEffect(Unit) {
            onDispose {
                exoPlayer.release() // Release the player when it's no longer needed
            }
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Download Status: $downloadStatus")

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onDownloadClick) {
                Text("Download Video")
            }

            Spacer(modifier = Modifier.height(16.dp))

//            Button(
//                onClick = {},
//                enabled = downloadStatus == "Download Complete"
//            ) {
//                Text("Play Video")
//            }

            if (true) {
                Spacer(modifier = Modifier.height(16.dp))
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16 / 9f),
                    factory = {
                        PlayerView(context).apply {
                            player = exoPlayer
                            useController = true
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    }
                )
            }
        }
    }

    fun Context.getVideoUri(): Uri = File(filesDir, "downloaded_video.mp4").toUri()