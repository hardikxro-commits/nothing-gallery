package com.nothing.vault.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt
import com.nothing.vault.data.model.VaultPhoto
import com.nothing.vault.data.repository.VaultRepository
import com.nothing.vault.ui.theme.TextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PhotoViewerScreen(
    photo: VaultPhoto,
    repository: VaultRepository,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    onSwipeNext: (() -> Unit)? = null,
    onSwipePrev: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var hasFullRes by remember { mutableStateOf(false) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var showControls by remember { mutableStateOf(true) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    val controlsAlpha by animateFloatAsState(
        targetValue = if (showControls) 1f else 0f,
        animationSpec = tween(300),
        label = "controlsAlpha"
    )

    var loadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(photo.id) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
        dragOffsetX = 0f
        dragOffsetY = 0f
        loadFailed = false
        hasFullRes = false
        bitmap = null

        val cached = repository.getCachedBitmap(photo.id)
        if (cached != null) {
            bitmap = cached
            hasFullRes = true
        } else {
            val thumb = withContext(Dispatchers.IO) {
                repository.getThumbnailBitmap(photo)
            }
            if (thumb != null) {
                bitmap = thumb
            }
            val full = withContext(Dispatchers.IO) {
                repository.decryptPhotoToBitmap(photo)
            }
            if (full != null) {
                bitmap = full
                hasFullRes = true
            } else if (thumb == null) {
                loadFailed = true
            }
        }
    }

    val swipeThreshold = with(density) { 200.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { showControls = !showControls }
    ) {
        if (bitmap == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (loadFailed) {
                        Text(
                            text = "Could not load photo",
                            color = TextPrimary.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    } else {
                        CircularProgressIndicator(color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }

        bitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        }
                    }
            )
        }

        if (!hasFullRes && bitmap != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(bottom = 80.dp),
                    color = Color.White.copy(alpha = 0.5f),
                    strokeWidth = 2.dp
                )
            }
        }

        // Swipe overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(dragOffsetX.roundToInt(), dragOffsetY.roundToInt()) }
                .pointerInput(onBack, onSwipeNext, onSwipePrev, swipeThreshold) {
                    detectDragGestures(
                        onDragStart = { dragOffsetX = 0f; dragOffsetY = 0f },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (scale == 1f) {
                                dragOffsetX += dragAmount.x
                                dragOffsetY += dragAmount.y
                            }
                        },
                        onDragEnd = {
                            if (scale == 1f) {
                                val dx = dragOffsetX
                                val dy = dragOffsetY
                                when {
                                    abs(dx) > abs(dy) * 2f && abs(dx) > swipeThreshold && dx < 0f -> onSwipeNext?.invoke()
                                    abs(dx) > abs(dy) * 2f && abs(dx) > swipeThreshold && dx > 0f -> onSwipePrev?.invoke()
                                    abs(dy) > swipeThreshold -> onBack()
                                }
                            }
                            dragOffsetX = 0f
                            dragOffsetY = 0f
                        },
                        onDragCancel = {
                            dragOffsetX = 0f
                            dragOffsetY = 0f
                        }
                    )
                }
        )

        // Controls overlay
        if (showControls) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = controlsAlpha * 0.3f))
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = TextPrimary
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    IconButton(
                        onClick = {
                            val success = repository.recoverPhoto(photo)
                            Toast.makeText(
                                context,
                                if (success) "Saved to Pictures/recovere" else "Recover failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50).copy(alpha = 0.8f))
                    ) {
                        Icon(
                            Icons.Filled.Download,
                            contentDescription = "Recover",
                            tint = TextPrimary
                        )
                    }
                    IconButton(
                        onClick = {
                            repository.deletePhoto(photo)
                            onDeleted()
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Red.copy(alpha = 0.8f))
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = TextPrimary
                        )
                    }
                }
            }
        }
    }
}
