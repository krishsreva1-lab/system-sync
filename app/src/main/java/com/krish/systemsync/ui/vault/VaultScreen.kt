package com.krish.systemsync.ui.vault

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.krish.systemsync.vault.FileType
import com.krish.systemsync.vault.StorageMode
import com.krish.systemsync.vault.VaultEvent
import com.krish.systemsync.vault.VaultFile
import com.krish.systemsync.vault.VaultViewModel
import com.krish.systemsync.ui.components.GlassSurface
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    viewModel: VaultViewModel,
    biometricFileAccess: Boolean = false,
    onVerifyBiometric: (onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit = { success, _ -> success() },
    onNavigateToNotes: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onOpenPlayer: (queue: List<VaultFile>, startIndex: Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val files by viewModel.files.collectAsState()
    val event by viewModel.events.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val resetTrigger by viewModel.resetTrigger.collectAsState()
    var selectedCategory by remember { mutableStateOf<FileType?>(null) }
    
    LaunchedEffect(resetTrigger) {
        selectedCategory = null
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showImportOptions by remember { mutableStateOf(false) }
    var fileForMenu by remember { mutableStateOf<VaultFile?>(null) }
    var fileToRename by remember { mutableStateOf<VaultFile?>(null) }
    var fileToDelete by remember { mutableStateOf<VaultFile?>(null) }

    // Consent flow for removing the original public file (Android 11+ MediaStore.createDeleteRequest)
    val deleteConsentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onOriginalDeleteConfirmed()
        } else {
            viewModel.consumeEvent()
            scope.launch { snackbarHostState.showSnackbar("File hidden, but the original couldn't be removed") }
        }
    }

    var pendingImportMode by remember { mutableStateOf(StorageMode.MAXIMUM_PRIVACY) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        uris.forEach { uri ->
            val name = queryDisplayName(context, uri) ?: uri.lastPathSegment ?: "file_${System.currentTimeMillis()}"
            viewModel.importFile(uri, name, pendingImportMode)
        }
        scope.launch { snackbarHostState.showSnackbar("Importing ${uris.size} file(s)...") }
    }

    LaunchedEffect(event) {
        when (val e = event) {
            is VaultEvent.Success -> {
                snackbarHostState.showSnackbar(e.message)
                viewModel.consumeEvent()
            }
            is VaultEvent.NeedsDeleteConsent -> {
                deleteConsentLauncher.launch(
                    IntentSenderRequest.Builder(e.result.intentSender).build()
                )
            }
            is VaultEvent.Error -> {
                snackbarHostState.showSnackbar("Error: ${e.message}")
                viewModel.consumeEvent()
            }
            null -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(selectedCategory?.name ?: "Secure Vault", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedCategory != null) selectedCategory = null else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showImportOptions = true },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Import", tint = Color.White)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (selectedCategory == null) {
                VaultMainContent(
                    modifier = Modifier.fillMaxSize(),
                    files = files,
                    onCategoryClick = { selectedCategory = it },
                    onNavigateToNotes = onNavigateToNotes,
                    onNavigateToTrash = onNavigateToTrash
                )
            } else {
                val categoryFiles = files.filter { it.type == selectedCategory }
                VaultCategoryContent(
                    modifier = Modifier.fillMaxSize(),
                    files = categoryFiles,
                    onFileClick = { file ->
                        val index = categoryFiles.indexOf(file)
                        if (index != -1) {
                            if (biometricFileAccess) {
                                onVerifyBiometric(
                                    { onOpenPlayer(categoryFiles, index) },
                                    { err -> scope.launch { snackbarHostState.showSnackbar("Authentication required: $err") } }
                                )
                            } else {
                                onOpenPlayer(categoryFiles, index)
                            }
                        }
                    },
                    onFileLongClick = { fileForMenu = it }
                )
            }
        }

        if (showImportOptions) {
            ImportOptionsDialog(
                onDismiss = { showImportOptions = false },
                onSelectMode = { mode ->
                    pendingImportMode = mode
                    showImportOptions = false
                    importLauncher.launch(arrayOf("*/*"))
                }
            )
        }

        fileForMenu?.let { file ->
            FileActionMenu(
                file = file,
                onDismiss = { fileForMenu = null },
                onExport = {
                    fileForMenu = null
                    scope.launch {
                        snackbarHostState.showSnackbar("Export feature available via player or long-press")
                    }
                },
                onRename = {
                    val f = fileForMenu
                    fileForMenu = null
                    fileToRename = f
                },
                onDelete = {
                    val f = fileForMenu
                    fileForMenu = null
                    fileToDelete = f
                }
            )
        }

        fileToRename?.let { file ->
            RenameDialog(
                initialName = file.name,
                onDismiss = { fileToRename = null },
                onConfirm = { newName ->
                    viewModel.renameFile(file.name, newName, file.mode)
                    fileToRename = null
                }
            )
        }

        fileToDelete?.let { file ->
            AlertDialog(
                onDismissRequest = { fileToDelete = null },
                title = { Text("Delete File?") },
                text = { Text("This file will be permanently deleted.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteFile(file.name, file.mode, permanent = false)
                        fileToDelete = null
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { fileToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun VaultMainContent(
    modifier: Modifier = Modifier,
    files: List<VaultFile>,
    onCategoryClick: (FileType) -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToTrash: () -> Unit
) {
    val imageCount = files.count { it.type == FileType.IMAGE }
    val videoCount = files.count { it.type == FileType.VIDEO }
    val audioCount = files.count { it.type == FileType.AUDIO }
    val docCount = files.count { it.type == FileType.DOCUMENT }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            VaultCategoryCard(
                title = "Images",
                count = imageCount,
                icon = Icons.Rounded.Image,
                modifier = Modifier.weight(1f),
                onClick = { onCategoryClick(FileType.IMAGE) }
            )
            VaultCategoryCard(
                title = "Videos",
                count = videoCount,
                icon = Icons.Rounded.VideoLibrary,
                modifier = Modifier.weight(1f),
                onClick = { onCategoryClick(FileType.VIDEO) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            VaultCategoryCard(
                title = "Audio",
                count = audioCount,
                icon = Icons.Rounded.AudioFile,
                modifier = Modifier.weight(1f),
                onClick = { onCategoryClick(FileType.AUDIO) }
            )
            VaultCategoryCard(
                title = "Documents",
                count = docCount,
                icon = Icons.Rounded.Description,
                modifier = Modifier.weight(1f),
                onClick = { onCategoryClick(FileType.DOCUMENT) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            VaultCategoryCard(
                title = "Secure Notes",
                count = null,
                icon = Icons.AutoMirrored.Filled.Note,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToNotes
            )
            VaultCategoryCard(
                title = "Trash Bin",
                count = null,
                icon = Icons.Rounded.Delete,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToTrash
            )
        }
    }
}

@Composable
fun VaultCategoryCard(
    title: String,
    count: Int?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassSurface(
        modifier = modifier.aspectRatio(1.2f),
        shape = RoundedCornerShape(28.dp)
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (count != null) {
                        Text(
                            "$count items",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VaultCategoryContent(
    modifier: Modifier = Modifier,
    files: List<VaultFile>,
    onFileClick: (VaultFile) -> Unit,
    onFileLongClick: (VaultFile) -> Unit
) {
    if (files.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No files in this category", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(files, key = { it.name }) { file ->
            GlassSurface(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .combinedClickable(
                        onClick = { onFileClick(file) },
                        onLongClick = { onFileLongClick(file) }
                    ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            when (file.type) {
                                FileType.IMAGE -> Icons.Rounded.Image
                                FileType.VIDEO -> Icons.Rounded.VideoLibrary
                                FileType.AUDIO -> Icons.Rounded.AudioFile
                                FileType.DOCUMENT -> Icons.Rounded.Description
                                else -> Icons.Rounded.InsertDriveFile
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(4.dp)
                    ) {
                        Text(
                            text = file.name,
                            color = Color.White,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImportOptionsDialog(
    onDismiss: () -> Unit,
    onSelectMode: (StorageMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Security Mode") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Choose how you want to store these files in the vault:")
                Spacer(Modifier.height(4.dp))
                Text("• Maximum Privacy: Fully encrypted with AES-256 and removed from gallery.", style = MaterialTheme.typography.bodySmall)
                Text("• Standard Hidden: Moved to hidden app folder (.nomedia) and removed from gallery.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = { onSelectMode(StorageMode.MAXIMUM_PRIVACY) }) {
                Text("Maximum Privacy")
            }
        },
        dismissButton = {
            TextButton(onClick = { onSelectMode(StorageMode.STANDARD_HIDDEN) }) {
                Text("Standard Hidden")
            }
        }
    )
}

@Composable
fun FileActionMenu(
    file: VaultFile,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column {
                ListItem(
                    headlineContent = { Text("Export / Restore") },
                    leadingContent = { Icon(Icons.Rounded.Download, contentDescription = null) },
                    modifier = Modifier.clickable { onExport() }
                )
                ListItem(
                    headlineContent = { Text("Rename") },
                    leadingContent = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                    modifier = Modifier.clickable { onRename() }
                )
                ListItem(
                    headlineContent = { Text("Move to Trash") },
                    leadingContent = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable { onDelete() }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun RenameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename File") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            Button(onClick = { if (text.isNotBlank()) onConfirm(text) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun queryDisplayName(context: android.content.Context, uri: Uri): String? {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val idx = queryIndex(it, android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx != -1) return it.getString(idx)
        }
    }
    return null
}

private fun queryIndex(cursor: android.database.Cursor, columnName: String): Int {
    return cursor.getColumnIndex(columnName)
}
