package com.scott.app.ui.view.reusable

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import coil.size.Size
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player.REPEAT_MODE_OFF
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.scott.ezmessaging.extension.getLocationUri
import com.scott.ezmessaging.model.Message

@Composable
fun ShowMessageImage(
    modifier: Modifier = Modifier,
    message: Message.MmsMessage
) {
    if (message.uniqueId == "66328") {
        ExoplayerExample(message = message)
    } else {
        val context = LocalContext.current
        val imageLoader = ImageLoader.Builder(context).components {
            if (Build.VERSION.SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }.build()
        val imageBuilder =
            ImageRequest.Builder(context).data(message.getLocationUri()).apply(block = {
                size(Size.ORIGINAL)
            }).build()
        Image(
            painter = rememberAsyncImagePainter(imageBuilder, imageLoader),
            contentDescription = null,
            modifier = modifier
                .width(300.dp)
                .height(300.dp)
        )
    }
}

@Composable
fun ExoplayerExample(message: Message.MmsMessage) {

    val context = LocalContext.current

    val mediaItem = MediaItem.Builder()
        .setUri(message.getLocationUri())
        .build()
    val exoPlayer = remember(context, mediaItem) {
        ExoPlayer.Builder(context)
            .build()
            .also { exoPlayer ->
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = false
                exoPlayer.repeatMode = REPEAT_MODE_OFF
            }
    }

    // Set MediaSource to ExoPlayer
    LaunchedEffect(mediaItem) {
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
    }

    // Manage lifecycle events
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Use AndroidView to embed an Android View (PlayerView) into Compose
    AndroidView(
        factory = { ctx ->
            StyledPlayerView(ctx).apply {
                player = exoPlayer
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(600.dp) // Set your desired height
    )


}