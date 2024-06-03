package com.scott.app.ui.view.reusable

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import coil.size.Size
import com.scott.ezmessaging.extension.getLocationUri
import com.scott.ezmessaging.model.Message

@Composable
fun ShowMessageImage(
    modifier: Modifier = Modifier,
    message: Message.MmsMessage
) {
    val context = LocalContext.current
    val imageLoader = ImageLoader.Builder(context).components {
        if (Build.VERSION.SDK_INT >= 28) {
            add(ImageDecoderDecoder.Factory())
        } else {
            add(GifDecoder.Factory())
        }
    }.build()
    val imageBuilder = ImageRequest.Builder(context).data(message.getLocationUri()).apply(block = {
        size(Size.ORIGINAL)
    }).build()
    Image(
        painter = rememberAsyncImagePainter(imageBuilder, imageLoader), contentDescription = null, modifier = modifier
            .width(300.dp)
            .height(300.dp)
    )
}