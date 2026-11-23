package com.example.myfirstcomposeapp.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TasksScreen(
    modifier: Modifier = Modifier,
    viewModel: TasksViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TasksContent(
        state = state,
        onLoad = { viewModel.loadData() },
        onLoadWithError = { viewModel.loadData(shouldFail = true) },
        onSequential = viewModel::loadSequential,
        onTimeout = viewModel::loadWithTimeout,
        onCancel = viewModel::cancelLoading,
        onTap = viewModel::onTap,
        onBlock = viewModel::blockMainThread,
        onRetry = { viewModel.loadData() },
        onSort = viewModel::sortTasks,
        modifier = modifier
    )
}

@Composable
fun TasksContent(
    state: TasksUiState,
    onLoad: () -> Unit,
    onLoadWithError: () -> Unit,
    onSequential: () -> Unit,
    onTimeout: () -> Unit,
    onCancel: () -> Unit,
    onTap: () -> Unit,
    onBlock: () -> Unit,
    onRetry: () -> Unit,
    onSort: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Coroutine Demo", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Bandingkan waktu Load (async) dengan Sequential.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onLoad, enabled = !state.isLoading) {
                Text("Load")
            }
            OutlinedButton(onClick = onLoadWithError, enabled = !state.isLoading) {
                Text("Load (fail)")
            }
            OutlinedButton(onClick = onCancel, enabled = state.isLoading) {
                Text("Cancel")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onSequential, enabled = !state.isLoading) {
                Text("Sequential")
            }
            OutlinedButton(onClick = onTimeout, enabled = !state.isLoading) {
                Text("Timeout")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onTap) {
                Text("Tap me: ${state.tapCount}")
            }
            OutlinedButton(onClick = onBlock) {
                Text("Block UI")
            }
        }

        // Indikator putar + timer langsung selama memuat
        if (state.isLoading) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator()
                Text("${state.activeMode?.label ?: "Loading"}: ${formatSeconds(state.elapsedMs)}")
            }
        }

        // Tombol Retry hanya muncul kalau ada pesan error
        state.errorMessage?.let { message ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }

        // Perbandingan waktu pemuatan terakhir untuk tiap mode
        if (state.durations.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Waktu pemuatan terakhir",
                        style = MaterialTheme.typography.titleSmall
                    )
                    state.durations.forEach { (mode, ms) ->
                        Text("${mode.label}: ${formatSeconds(ms)}")
                    }
                }
            }
        }

        if (state.announcement.isNotEmpty()) {
            Text("Announcement: ${state.announcement}")
        }

        if (state.courseCount > 0) {
            Text("Courses: ${state.courseCount}")
        }

        if (state.tasks.isNotEmpty()) {
            OutlinedButton(onClick = onSort, enabled = !state.isLoading) {
                Text("Sort")
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.tasks, key = { it.id }) { task ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            task.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            task.course,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

// 2300 ms -> "2.3 s" (tanpa tergantung bahasa perangkat)
private fun formatSeconds(ms: Long): String = "${ms / 1000}.${(ms % 1000) / 100} s"
