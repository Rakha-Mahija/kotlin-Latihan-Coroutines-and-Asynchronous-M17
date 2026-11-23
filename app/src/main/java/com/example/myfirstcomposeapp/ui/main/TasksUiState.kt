package com.example.myfirstcomposeapp.ui.main

import com.example.myfirstcomposeapp.data.Task

// Cara memuat data yang bisa dibandingkan waktunya
enum class LoadMode(val label: String) {
    CONCURRENT("Concurrent (async)"),
    SEQUENTIAL("Sequential"),
    TIMEOUT("Timeout 1.5 s")
}

data class TasksUiState(
    val isLoading: Boolean = false,
    val tasks: List<Task> = emptyList(),
    val announcement: String = "",
    val courseCount: Int = 0,
    val errorMessage: String? = null,
    val tapCount: Int = 0,
    // Mode yang sedang berjalan, null kalau tidak sedang memuat
    val activeMode: LoadMode? = null,
    // Waktu berjalan (ms) selama memuat, diperbarui oleh coroutine timer
    val elapsedMs: Long = 0L,
    // Waktu pemuatan terakhir yang berhasil untuk tiap mode
    val durations: Map<LoadMode, Long> = emptyMap()
)
