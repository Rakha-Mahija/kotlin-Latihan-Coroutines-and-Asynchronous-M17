package com.example.myfirstcomposeapp.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfirstcomposeapp.data.Task
import com.example.myfirstcomposeapp.data.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import kotlin.time.TimeSource

class TasksViewModel(
    private val repository: TaskRepository = TaskRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    // Hasil gabungan dari tiga permintaan
    private data class LoadedData(
        val tasks: List<Task>,
        val announcement: String,
        val courseCount: Int
    )

    // Tiga permintaan berjalan bersamaan: total waktu = permintaan terlama
    fun loadData(shouldFail: Boolean = false) {
        startLoad(LoadMode.CONCURRENT) { loadConcurrently(shouldFail) }
    }

    // Pembanding: tiga permintaan satu per satu, total waktu = jumlah semuanya
    fun loadSequential() {
        startLoad(LoadMode.SEQUENTIAL) {
            val tasks = repository.getTasks()
            val announcement = repository.getAnnouncement()
            val courseCount = repository.getCourseCount()
            LoadedData(tasks, announcement, courseCount)
        }
    }

    // Batasi waktu tunggu. getTasks() butuh 2 detik, jadi selalu kena timeout.
    // withTimeoutOrNull mengembalikan null saat waktu habis (tidak melempar).
    fun loadWithTimeout() {
        startLoad(LoadMode.TIMEOUT) {
            withTimeoutOrNull(TIMEOUT_MS) { loadConcurrently(shouldFail = false) }
                ?: throw IOException("Timed out after $TIMEOUT_MS ms")
        }
    }

    private suspend fun loadConcurrently(shouldFail: Boolean): LoadedData = coroutineScope {
        val tasks = async { repository.getTasks(shouldFail) }
        val announcement = async { repository.getAnnouncement() }
        val courseCount = async { repository.getCourseCount() }
        LoadedData(tasks.await(), announcement.await(), courseCount.await())
    }

    // Kerangka bersama: loading, timer, hasil, dan error
    private fun startLoad(mode: LoadMode, block: suspend () -> LoadedData) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, activeMode = mode, elapsedMs = 0L)
            }
            val start = TimeSource.Monotonic.markNow()

            // Coroutine kedua: memperbarui timer tiap 100 ms selama memuat
            val ticker = launch {
                while (true) {
                    delay(100L)
                    _uiState.update { it.copy(elapsedMs = start.elapsedNow().inWholeMilliseconds) }
                }
            }

            try {
                val data = block()
                val total = start.elapsedNow().inWholeMilliseconds
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        activeMode = null,
                        tasks = data.tasks,
                        announcement = data.announcement,
                        courseCount = data.courseCount,
                        elapsedMs = total,
                        durations = it.durations + (mode to total)
                    )
                }
            } catch (e: IOException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        activeMode = null,
                        errorMessage = "Failed to load: ${e.message}"
                    )
                }
            } finally {
                // Timer ikut berhenti apa pun hasilnya (sukses, error, atau dibatalkan)
                ticker.cancel()
            }
        }
    }

    fun cancelLoading() {
        loadJob?.cancel()
        _uiState.update {
            it.copy(isLoading = false, activeMode = null, errorMessage = "Loading cancelled")
        }
    }

    fun onTap() {
        _uiState.update { it.copy(tapCount = it.tapCount + 1) }
    }

    // Contoh buruk yang disengaja: memblokir main thread
    fun blockMainThread() {
        Thread.sleep(3000L)
    }

    // Urutkan berdasarkan judul di Dispatchers.Default
    fun sortTasks() {
        viewModelScope.launch {
            val sorted = withContext(Dispatchers.Default) {
                _uiState.value.tasks.sortedBy { it.title }
            }
            _uiState.update { it.copy(tasks = sorted) }
        }
    }

    private companion object {
        const val TIMEOUT_MS = 1500L
    }
}
