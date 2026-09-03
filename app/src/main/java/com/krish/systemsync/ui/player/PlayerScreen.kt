package com.krish.systemsync.ui.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.krish.systemsync.vault.FileType
import com.krish.systemsync.vault.VaultFile
import com.krish.systemsync.vault.VaultViewModel
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.roundToLong

private val SPEED_OPTIONS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: VaultViewModel,
    queue: List<VaultFile>,
    startIndex: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var currentIndex by remember { mutableIntStateOf(startIndex.coerceIn(0, (queue.size - 1).coerceAtLeast(0))) }
    val currentFile = queue.getOrNull(currentIndex)

    var resolvedFile by remember(currentIndex) { mutableStateOf<File?>(null) }
    var loadError by remember(currentIndex) { mutableStateOf(false) }

    LaunchedEffect(currentIndex, currentFile) {
        resolvedFile = null
        loadError = false
        val f = currentFile ?: return@LaunchedEffect
        runCatching { viewModel.preparePlaybackFile(f) }
            .onSuccess { resolvedFile = it }
            .onFailure { loadError = true }
    }

    val isMedia = currentFile?.type == FileType.VIDEO || currentFile?.type == FileType.AUDIO

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }
    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }

    // Load new source into the player whenever the resolved file changes.
    LaunchedEffect(resolvedFile) {
        val file = resolvedFile
        if (file != null && isMedia) {
            exoPlayer.setMediaItem(MediaItem.fromUri(file.toURI().toString()))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            exoPlayer.setPlaybackSpeed(playbackSpeed)
        } else {
            exoPlayer.stop()
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    durationMs = exoPlayer.duration.coerceAtLeast(0L)
                }
                if (state == Player.STATE_ENDED) {
                    if (currentIndex < queue.lastIndex) currentIndex++ else isPlaying = false
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    // Poll playback position for the seek bar.
    LaunchedEffect(exoPlayer, isMedia) {
        while (isMedia) {
            if (!isSeeking) {
                positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                durationMs = exoPlayer.duration.coerceAtLeast(0L)
            }
            delay(300)
        }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        currentFile?.name ?: "",
                        maxLines = 1,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    currentFile == null || loadError -> Text(
                        "Couldn't open this file",
                        color = Color.White
                    )
                    currentFile.type == FileType.IMAGE && resolvedFile != null -> AsyncImage(
                        model = resolvedFile,
                        contentDescription = currentFile.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    currentFile.type == FileType.VIDEO && resolvedFile != null -> AndroidView(
                        factory = {
                            PlayerView(context).apply {
                                player = exoPlayer
                                useController = false
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    currentFile.type == FileType.AUDIO -> AudioArt()
                    else -> CircularProgressIndicator(color = Color.White)
                }
            }

            if (isMedia && currentFile != null) {
                PlayerControls(
                    isPlaying = isPlaying,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    playbackSpeed = playbackSpeed,
                    hasPrev = currentIndex > 0,
                    hasNext = currentIndex < queue.lastIndex,
                    onPlayPause = {
                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                    },
                    onSeekBack = {
                        exoPlayer.seekTo((exoPlayer.currentPosition - 10_000).coerceAtLeast(0))
                    },
                    onSeekForward = {
                        exoPlayer.seekTo((exoPlayer.currentPosition + 10_000).coerceAtMost(durationMs))
                    },
                    onSeekTo = { fraction ->
                        val target = (fraction * durationMs).roundToLong()
                        exoPlayer.seekTo(target)
                        positionMs = target
                    },
                    onSeekingChange = { isSeeking = it },
                    onPrev = { if (currentIndex > 0) currentIndex-- },
                    onNext = { if (currentIndex < queue.lastIndex) currentIndex++ },
                    onSpeedChange = {
                        playbackSpeed = it
                        exoPlayer.setPlaybackSpeed(it)
                    }
                )
            } else if (currentFile?.type == FileType.IMAGE) {
                ImageNavControls(
                    hasPrev = currentIndex > 0,
                    hasNext = currentIndex < queue.lastIndex,
                    onPrev = { if (currentIndex > 0) currentIndex-- },
                    onNext = { if (currentIndex < queue.lastIndex) currentIndex++ }
                )
            }
        }
    }
}

@Composable
private fun AudioArt() {
    Box(
        modifier = Modifier
            .size(220.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1E1E1E)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(72.dp))
    }
}

@Composable
private fun ImageNavControls(hasPrev: Boolean, hasNext: Boolean, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrev, enabled = hasPrev) {
            Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", tint = if (hasPrev) Color.White else Color.Gray)
        }
        IconButton(onClick = onNext, enabled = hasNext) {
            Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = if (hasNext) Color.White else Color.Gray)
        }
    }
}

@Composable
private fun PlayerControls(
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    playbackSpeed: Float,
    hasPrev: Boolean,
    hasNext: Boolean,
    onPlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekTo: (Float) -> Unit,
    onSeekingChange: (Boolean) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSpeedChange: (Float) -> Unit
) {
    var showSpeedMenu by remember { mutableStateOf(false) }
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Slider(
            value = progress,
            onValueChange = {
                onSeekingChange(true)
                onSeekTo(it)
            },
            onValueChangeFinished = { onSeekingChange(false) },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTime(positionMs), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
            Text(formatTime(durationMs), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                TextButton(onClick = { showSpeedMenu = true }) {
                    Text("${playbackSpeed}x", color = Color.White)
                }
                DropdownMenu(expanded = showSpeedMenu, onDismissRequest = { showSpeedMenu = false }) {
                    SPEED_OPTIONS.forEach { speed ->
                        DropdownMenuItem(
                            text = { Text("${speed}x") },
                            onClick = {
                                onSpeedChange(speed)
                                showSpeedMenu = false
                            }
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev, enabled = hasPrev) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", tint = if (hasPrev) Color.White else Color.Gray)
                }
                IconButton(onClick = onSeekBack) {
                    Icon(Icons.Rounded.Replay10, contentDescription = "Back 10s", tint = Color.White)
                }
                FilledIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White)
                ) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = onSeekForward) {
                    Icon(Icons.Rounded.Forward10, contentDescription = "Forward 10s", tint = Color.White)
                }
                IconButton(onClick = onNext, enabled = hasNext) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = if (hasNext) Color.White else Color.Gray)
                }
            }

            Spacer(Modifier.width(48.dp)) // balance the speed button on the left
        }
        Spacer(Modifier.height(8.dp))
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
