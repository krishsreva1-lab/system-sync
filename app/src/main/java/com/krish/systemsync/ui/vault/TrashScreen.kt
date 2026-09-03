package com.krish.systemsync.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.krish.systemsync.vault.TrashedFile
import com.krish.systemsync.vault.VaultViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit
) {
    val trash by viewModel.trash.collectAsState()
    var fileToPurge by remember { mutableStateOf<TrashedFile?>(null) }
    var confirmEmptyAll by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadTrash() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Secure Trash", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (trash.isNotEmpty()) {
                        TextButton(onClick = { confirmEmptyAll = true }) {
                            Text("Empty")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (trash.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.DeleteForever, contentDescription = null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))
                    Text("Trash is empty", style = MaterialTheme.typography.titleMedium)
                    Text("Deleted items appear here", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(trash, key = { it.trashName }) { item ->
                    TrashItemRow(
                        item = item,
                        onRestore = { viewModel.restoreFromTrash(item.trashName) },
                        onDeleteForever = { fileToPurge = item }
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        fileToPurge?.let { item ->
            AlertDialog(
                onDismissRequest = { fileToPurge = null },
                title = { Text("Delete Forever?") },
                text = { Text("\"${item.originalName}\" will be permanently deleted. This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.permanentlyDeleteFromTrash(item.trashName)
                        fileToPurge = null
                    }) { Text("Delete Forever") }
                },
                dismissButton = {
                    TextButton(onClick = { fileToPurge = null }) { Text("Cancel") }
                }
            )
        }

        if (confirmEmptyAll) {
            AlertDialog(
                onDismissRequest = { confirmEmptyAll = false },
                title = { Text("Empty Trash?") },
                text = { Text("All ${trash.size} item(s) will be permanently deleted. This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.emptyTrash()
                        confirmEmptyAll = false
                    }) { Text("Empty Trash") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmEmptyAll = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun TrashItemRow(item: TrashedFile, onRestore: () -> Unit, onDeleteForever: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.originalName, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(
                    "${item.size / 1024} KB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRestore) {
                Icon(Icons.Rounded.Restore, contentDescription = "Restore")
            }
            IconButton(onClick = onDeleteForever) {
                Icon(Icons.Rounded.DeleteForever, contentDescription = "Delete Forever", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
