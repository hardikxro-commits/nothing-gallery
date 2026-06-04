package com.nothing.vault.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.vault.data.model.VaultPhoto
import com.nothing.vault.data.repository.VaultRepository
import com.nothing.vault.ui.theme.Accent
import com.nothing.vault.ui.theme.Background
import com.nothing.vault.ui.theme.TextMuted
import com.nothing.vault.ui.theme.TextPrimary
import com.nothing.vault.ui.theme.TextSecondary
import androidx.compose.material.icons.filled.Folder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultGalleryScreen(
    folderId: String,
    repository: VaultRepository,
    onPhotoClick: (VaultPhoto) -> Unit,
    onImport: () -> Unit,
    onLock: () -> Unit,
    onCreateFolder: () -> Unit
) {
    var photos by remember { mutableStateOf<List<VaultPhoto>>(emptyList()) }
    val folder = remember { repository.getFolders().firstOrNull { it.id == folderId } }
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    LaunchedEffect(folderId) {
        val loadedPhotos = withContext(Dispatchers.IO) {
            repository.getPhotos(folderId).also {
                repository.warmPhotoCache(it, centerIndex = 0, count = 60, preDecrypt = true)
            }
        }
        photos = loadedPhotos
    }

    LaunchedEffect(folderId) {
        snapshotFlow { gridState.firstVisibleItemIndex }
            .collect { index ->
                scope.launch(Dispatchers.IO) {
                    repository.warmPhotoCache(photos, centerIndex = index, count = 48)
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background)
            )

            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = folder?.name ?: "Vault",
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 20.sp
                            )
                            Text(
                                text = "${photos.size} photo${if (photos.size != 1) "s" else ""}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onLock) {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = "Lock",
                                tint = TextSecondary
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onCreateFolder) {
                            Icon(
                                Icons.Filled.Folder,
                                contentDescription = "New folder",
                                tint = TextSecondary
                            )
                        }
                        IconButton(onClick = onImport) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Import",
                                tint = Accent
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )

                if (photos.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No photos yet",
                                color = TextMuted,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Tap + to import photos",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(photos, key = { it.id }) { photo ->
                            ThumbnailItem(photo = photo, repository = repository, onClick = { onPhotoClick(photo) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThumbnailItem(
    photo: VaultPhoto,
    repository: VaultRepository,
    onClick: () -> Unit
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(photo.id) {
        val cached = repository.getCachedThumbnail(photo.id)
        if (cached != null) {
            bitmap = cached
        } else {
            val loaded = withContext(Dispatchers.IO) {
                repository.getThumbnailBitmap(photo)
            }
            bitmap = loaded
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.1f))
        )
    }
}
