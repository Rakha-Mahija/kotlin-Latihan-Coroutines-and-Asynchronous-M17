# Jawaban Latihan M17: Kotlin Coroutines

Proyek: Coroutine Demo (`com.example.myfirstcomposeapp`).

> Catatan: jawaban soal 1, 3, 4 dan 5 disusun dari membaca kode dan konsep coroutine, bukan dari hasil menjalankan aplikasi. Silakan buktikan sendiri di emulator dan ganti bagian "Hasil pengamatan" dengan yang kamu lihat.

## 1. Tambah satu permintaan lagi

Sudah dikerjakan di kode:
- `TaskRepository.getCourseCount()` dengan `delay(1500L)` (data/TaskRepository.kt).
- Field `courseCount` di `TasksUiState`.
- `async` ketiga di `TasksViewModel.loadData()`, dan hasilnya tampil sebagai "Courses: 3".

**Apakah total waktu pemuatan masih sekitar 2 detik? Ya.**

Ketiga `async` dimulai bersamaan di dalam `coroutineScope`, jadi waktu totalnya mengikuti permintaan yang paling lama, bukan jumlah semuanya:

| Permintaan | Lama |
| --- | --- |
| `getTasks()` | 2000 ms |
| `getAnnouncement()` | 1000 ms |
| `getCourseCount()` | 1500 ms |
| **Bersamaan** | **max = 2000 ms** |
| Kalau satu per satu | 4500 ms |

Hasil pengamatan: _(isi sendiri)_

## 2. Tombol Retry

Sudah dikerjakan di `TasksScreen.kt`. Tombol **Retry** ada di sebelah pesan error, memanggil `loadData()`, dan hanya tampil kalau `errorMessage != null`.

Catatan: pesan "Loading cancelled" juga mengisi `errorMessage`, jadi Retry ikut muncul setelah Cancel.

## 3. Menangkap terlalu banyak

Percobaan: ganti `catch (e: IOException)` menjadi `catch (e: Exception)`, tekan **Load**, lalu segera **Cancel**.

**Pesan yang muncul:** bukan "Loading cancelled", melainkan "Failed to load: ..." diikuti teks dari exception pembatalan. Teks lengkapnya bisa dilihat langsung di layar.

**Kenapa berbeda:**
1. `cancelLoading()` memanggil `loadJob?.cancel()` lalu langsung mengisi `errorMessage = "Loading cancelled"`.
2. Coroutine yang dibatalkan melempar `CancellationException`.
3. `CancellationException` adalah turunan `Exception`, jadi sekarang **ikut tertangkap** oleh `catch (e: Exception)`.
4. Blok catch berjalan setelahnya dan **menimpa** pesan dengan "Failed to load: ...".

Dengan `catch (e: IOException)` pembatalan lolos dari catch, jadi pesan "Loading cancelled" tidak tertimpa. Pelajarannya: jangan menangkap `CancellationException`. Kalau terpaksa menangkap `Exception`, lempar ulang `CancellationException`-nya.

Hasil pengamatan: _(isi sendiri)_

## 4. Di mana macetnya?

**Percobaan A:** ganti `delay(2000L)` menjadi `Thread.sleep(2000L)` di dalam `withContext(Dispatchers.IO)`.

Layar **tidak macet**. Tombol Tap me tetap bereaksi dan angkanya terus bertambah. `Thread.sleep` memang memblokir thread, tetapi yang terblokir adalah thread IO, bukan main thread.

**Percobaan B:** hapus `withContext` dari `getTasks()`.

Layar **macet sekitar 2 detik**: Tap me tidak bereaksi dan indikator putar berhenti. `getTasks()` dipanggil dari `async` di dalam `viewModelScope`, yang berjalan di main thread. Tanpa `withContext(Dispatchers.IO)`, `Thread.sleep` ikut berjalan di main thread dan memblokirnya.

**Kenapa hasilnya berbeda:**
- `delay()` menangguhkan coroutine dan melepaskan thread, jadi tidak memblokir apa pun.
- `Thread.sleep()` menahan thread yang sedang dipakai.
- `withContext(Dispatchers.IO)` menentukan thread mana yang dipakai. Menjadikan fungsi `suspend` tidak otomatis memindahkan pekerjaan ke latar belakang.

Hasil pengamatan: _(isi sendiri)_

## 5. Pertanyaan konsep

**(a) Beda `launch` dan `async`**
- `launch` mengembalikan `Job`, tidak menghasilkan nilai. Dipakai untuk pekerjaan "jalankan saja", misalnya memperbarui state.
- `async` mengembalikan `Deferred<T>`, hasilnya diambil dengan `await()`. Dipakai ketika butuh hasil dari beberapa pekerjaan yang berjalan bersamaan.

**(b) Kenapa `GlobalScope` sebaiknya dihindari**
- `GlobalScope` tidak terikat ke lifecycle ViewModel, Activity atau layar. Coroutine di dalamnya terus berjalan sampai selesai atau sampai proses aplikasi mati.
- Kalau pengguna sudah meninggalkan layar, pekerjaannya tetap berjalan: memboroskan baterai dan memori, bisa memperbarui UI yang sudah tidak ada, dan sulit dibatalkan.
- `viewModelScope` otomatis dibatalkan saat ViewModel dihapus. Ini yang disebut structured concurrency.

**(c) Apa yang terjadi pada `getAnnouncement()` kalau `getTasks()` gagal**

Aturannya: `getTasks()` dan `getAnnouncement()` adalah child dari `coroutineScope`. Kalau satu child gagal, scope membatalkan child lainnya, lalu melempar exception-nya.

Di kode ini ada nuansa waktu. `getAnnouncement()` hanya butuh 1 detik, sedangkan `getTasks()` baru gagal di detik ke-2. Jadi pengumuman sudah **selesai** sebelum error terjadi. Karena `tasks.await()` melempar exception lebih dulu, hasil pengumuman **tidak pernah dipakai** dan tidak masuk ke state. Layar hanya menampilkan pesan error.

Kalau `getAnnouncement()` lebih lambat dari `getTasks()`, barulah ia benar-benar dibatalkan.

## 6. Tantangan: tombol Sort

Sudah dikerjakan: `TasksViewModel.sortTasks()` mengurutkan `tasks` berdasarkan judul di `withContext(Dispatchers.Default)`, lalu memperbarui state dengan hasilnya. Tombol **Sort** muncul setelah data dimuat dan dinonaktifkan selama loading.

Urutan setelah ditekan: Prepare quiz, Read chapter 5, Write lab report.

`Dispatchers.Default` dipilih karena pengurutan adalah pekerjaan CPU. `Dispatchers.IO` untuk menunggu jaringan atau berkas.

## Pengembangan bebas

Fokus pengembangan: membuat manfaat coroutine **terlihat** di layar, bukan hanya dijelaskan.

| Tambahan | Konsep | Cara kerja |
| --- | --- | --- |
| Timer langsung selama loading | `launch` kedua, `delay` | Coroutine timer berjalan bersamaan dengan pemuatan dan memperbarui `elapsedMs` tiap 100 ms. Berhenti lewat `ticker.cancel()` di blok `finally`. |
| Tombol **Sequential** | Beda `async` dan berurutan | Memuat tiga permintaan satu per satu: sekitar 4.5 detik, dibanding sekitar 2 detik untuk **Load**. |
| Tombol **Timeout** | `withTimeoutOrNull` | Membatasi waktu tunggu 1.5 detik. `getTasks()` butuh 2 detik, jadi selalu kena timeout dan tampil "Failed to load: Timed out after 1500 ms". Memakai `OrNull` supaya tidak melempar `TimeoutCancellationException`, yang termasuk `CancellationException`. |
| Kartu **Waktu pemuatan terakhir** | State yang diturunkan dari hasil | `durations` menyimpan waktu terakhir tiap mode, sehingga Concurrent dan Sequential bisa dibandingkan berdampingan. |

Refactor: `loadData`, `loadSequential` dan `loadWithTimeout` berbagi satu fungsi `startLoad()` untuk loading, timer, hasil, dan error. Perilaku `loadData()` untuk soal 1, 3 dan 5 tidak berubah.

Cara mencoba:
1. Tekan **Load**, lalu **Sequential**. Kartu perbandingan menampilkan kedua waktu.
2. Tekan **Timeout** dan amati pesan error.
3. Tekan **Load**, lalu **Cancel** di tengah jalan. Timer ikut berhenti.

Hasil pengamatan: _(isi sendiri)_
