# Fv7 Tools Manager

Ekstensi App Inventor (`.aix`) — **12 komponen dalam 1 paket** (`com.fv7.tools`) untuk membangun aplikasi file manager, downloader, pemutar media, UI dinamis, penjadwal, dan notifikasi — semuanya bekerja dengan **sistem id** sehingga banyak pekerjaan bisa jalan bareng tanpa tabrakan.

**Unduh file `.aix` terbaru:** halaman [Releases](https://github.com/VB-7/MiT-File_Manager/releases) — buka rilis paling atas, link download ada di paling atas deskripsi rilis (file dilampirkan di chat rilis bila tersedia).

## Daftar isi

- [Perkenalan](#perkenalan)
- [Cara pasang](#cara-pasang)
- [Aturan main blok](#aturan-main-blok)
- [Sistem id](#sistem-id)
- [Izin Android](#izin-android)
- [Riwayat versi](#riwayat-versi)
- [Tools_Audio](#tools-audio)
- [Tools_Base](#tools-base)
- [Tools_Compress](#tools-compress)
- [Tools_Core](#tools-core)
- [Tools_Downloader](#tools-downloader)
- [Tools_File](#tools-file)
- [Tools_Image](#tools-image)
- [Tools_Layout](#tools-layout)
- [Tools_Sheet](#tools-sheet)
- [Tools_Style](#tools-style)
- [Tools_Task](#tools-task)
- [Tools_Video](#tools-video)

## Perkenalan

### Projek apa ini?

Fv7 Tools Manager adalah kumpulan 12 ekstensi App Inventor yang saling terhubung: File, Downloader, Compress (ZIP), Audio, Video, Image, Core (komponen dinamis), Layout, Style, Sheet (bottom-sheet + toast + notifikasi), Task (penjadwal + alarm), dan Base (fondasi utilitas + id). Satu file `.aix`, satu paket, satu sistem id.

### Untuk apa?

Untuk membuat aplikasi seperti file manager, pengunduh, galeri audio/video, toko tema/UI dinamis, pengingat/penjadwal — tanpa menulis kode Java. Contoh alur: `CallMakeDirectory` → `CallDownload(url, ...) → event DownloadFinished`.

### Kelebihan

- **Bisa jalan bareng (paralel ber-id):** tiap download/audio/video/gambar/tugas punya id angka sendiri; semua list id **urut pembuatan** (index 1 = pertama dibuat), jadi tidak nyasar saat dipakai dengan blok bawaan MIT.
- **Aturan blok konsisten:** `Is*` = boolean (colok ke `if`), `Call*` = kerja async, hasil selalu lewat event, `Set*` = aksi tanpa feedback/return.
- **Izin diurus blok:** operasi storage otomatis minta izin sendiri (Android 11+ dibukakan layar *Akses semua file*); blok pintar `CallEnsurePermission`/`CallEnsureStoragePermission` + 10 Helper `Permission...` tinggal colok.
- **Tiap event ada fallback polling:** `GetLast*`, `IsBusy`, `Get*Status/Result/Progress` — bila event terlewat, status tetap bisa dibaca.
- **Satu pintu download:** `CallDownload` menerima `http(s)`, `data:` URL (base64), `content:` URI, dan `file:`/path lokal; nama file bisa ditebak otomatis (`CallGuessFileName`).
- **Butuh Android 5.0 (API 21) ke atas.**

## Cara pasang

1. Unduh `.aix` dari halaman rilis (link di deskripsi rilis).
2. Di App Inventor: menu **Extension → Import extension → pilih file `.aix`** (atau import via URL mentah GitHub).
3. Seret komponen yang dibutuhkan ke Screen (mis. `Tools_File`, `Tools_Downloader`).
4. Pasang event `...Finished` untuk tiap `Call*` agar tahu hasilnya (success + message).

## Aturan main blok

- **`Is...`** → kembali **boolean**, langsung colok ke `if`. Contoh: `IsFileOrDirectoryExists`, `IsAllFilesAccessGranted`.
- **`Call...`** → **tanpa return** (void). Hasilnya keluar lewat **event** (`...Finished`, `GotText`, ...). Contoh: `CallDownload` → `DownloadFinished(id, success, savePath, fileName, message)`.
- **`Set...`** → aksi, **tanpa feedback dan tanpa return**. Contoh: `SetCancelDownload`, `SetShowToast`.
- **`Get...`** → baca langsung (return), untuk polling/status. **`Remove.../Delete...`** → lepas/bersihkan id.
- Tiap kegagalan juga tercatat di `ErrorOccurred` + bisa dibaca via `LastError(...)` / `GetLastErrorMessage()`.

## Sistem id

Buat id unik via `Tools_Base.CreateUniqueId` atau `CreateDownloadId`/`CreateImageId`/`CreateAudioId`/`CreateVideoId`/`CreateLayoutId`/`CreateSheetId`/`CreateTaskId` (atau per tipe lain). Id 0/-1 = tidak valid. Daftar id (`Get...Ids`) selalu **urut pembuatan**. Id yang masih dipakai operasi lain ditolak (`false` + pesan) — batalkan dulu via `SetCancel...`/`Remove...Id`.

## Izin Android

Operasi file otomatis meminta izin sebelum jalan (sudah granted = lanjut tanpa dialog). Android 11+ (API 30+): tulis storage bersama butuh **Akses semua file** — blok `OpenAllFilesAccessSettings` membuka layarnya, cek via `IsAllFilesAccessGranted`. Izin lain (kamera, mikrofon, notifikasi, media, lokasi) dicek/diminta via `IsPermissionGranted` / `CallEnsurePermission` / `CallEnsureStoragePermission`, atau colok Helper `Permission...` (tanpa ketik string). Hasil izin lewat event `GotPermission(success, permission, message)`.

## Riwayat versi

- **v17** — `CallDownload` 4 pintu (http/data:/content:/file:); `CallGuessFileName` + `FileNameGuessed`; versi anotasi 17 + catatan update per komponen; blok `GetWhatsNew`.
- **v16** — gate izin otomatis 16 operasi storage; `IsAllFilesAccessGranted`/`OpenAllFilesAccessSettings`; `Is/CallEnsure*Permission` + 10 Helper Permission; `MANAGE_EXTERNAL_STORAGE`.
- **v15** — aturan final: Is=boolean, Call=void+event, Set=tanpa feedback.
- **v14** — direktori Android (`GetAndroidStorageDirectory`, SD card, cache); notifikasi download per-id.
- **v13** — hapus parameter scope; root otomatis `/storage/emulated/0`.
- **v12** — audit penamaan + `SheetShown`, `RegisterSheet`, `IsExactAlarmAllowed`.
- **v11** — rombak `Tools_File` ke pola Is/Call/Set.
- **v8–v10** — tipe layout, helper kategori file, path cleaner.
- **v6–v7** — komponen Core/Layout/Style/Sheet + Task (penjadwal & alarm).
- **v4–v5** — sistem id ber-id + Base64 ke file.
- **v1–v3** — rilis awal paket.

## Tools_Audio

> Tools_Audio: putar BANYAK audio ber-id (background, tanpa tampilan), kontrol pause/resume/seek/maju/mundur, loop, volume. Satu id = satu audio, bisa jalan bareng. Id dikelola CreateAudioId / Tools_Base. List id URUT PEMBUATAN. (v17 2026-11-17: tanpa perubahan fungsi; penomoran versi.)

Versi anotasi: **17**. Jumlah blok: 20 fungsi, 5 properti, 4 event.

### Fungsi (Tools_Audio)

#### `CallPlayAudio`

CALL: Putar audio ber-id (background, tanpa tampilan). Bisa jalan bareng (satu id = satu audio). path file (/Download/..., absolut apa adanya), file:/// absolut, atau http(s) URL (streaming). id dari CreateAudioId / Tools_Base CreateUniqueId (reservasi diadopsi). Tampilan (Slider/Label) bikin sendiri dari AudioStarted/ProgressChanged/Finished. Sukses menembak AudioStarted; gagal via ErrorOccurred.

- **Input:** `path` (teks), `id` (angka)
- **Return:** void

#### `CreateAudioId`

Buat id audio unik baru (Number, siap pakai ke CallPlayAudio).

- **Input:** tidak ada
- **Return:** angka

#### `CurrentAudioId`

Id audio yang terakhir dibuat/di-start. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetActiveAudioIds`

Id audio yang sedang bunyi (URUT PEMBUATAN). Yang dijeda/berhenti TIDAK masuk sini.

- **Input:** tidak ada
- **Return:** list

#### `GetAudioDuration`

Durasi audio (ms). 0 bila belum siap. (per id)

- **Input:** `id` (angka)
- **Return:** angka

#### `GetAudioIds`

Semua id audio yang dipegang (URUT PEMBUATAN, index 1 = pertama dibuat).

- **Input:** tidak ada
- **Return:** list

#### `GetAudioPosition`

Posisi audio sekarang (ms). (per id)

- **Input:** `id` (angka)
- **Return:** angka

#### `GetFirstAudioId`

Id index 1 = PERTAMA DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetLastAudioId`

Id TERAKHIR DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetPreviousAudioId`

Id yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.

- **Input:** `id` (angka)
- **Return:** angka

#### `IsAudioIdActive`

True bila id audio ini sedang bunyi.

- **Input:** `id` (angka)
- **Return:** boolean

#### `IsAudioPlaying`

True bila audio id ini sedang bunyi.

- **Input:** `id` (angka)
- **Return:** boolean

#### `LastError`

Fallback polling untuk event ErrorOccurred: pesan error terakhir operasi ini (kosong bila belum ada).

- **Input:** `operation` (teks)
- **Return:** teks

#### `RemoveAudioId`

Hapus id audio: hentikan bila bunyi + lepas id (otomatis beres-beres).

- **Input:** `id` (angka)
- **Return:** void

#### `SetBackwardAudio`

SET: Mundur audio sejauh SkipSeconds detik. (per id)

- **Input:** `id` (angka)
- **Return:** void

#### `SetForwardAudio`

SET: Maju audio sejauh SkipSeconds detik. (per id)

- **Input:** `id` (angka)
- **Return:** void

#### `SetPauseAudio`

SET: jeda audio yang sedang bunyi (per id). Tanpa feedback.

- **Input:** `id` (angka)
- **Return:** void

#### `SetResumeAudio`

SET: lanjutkan audio yang dijeda (per id). Tanpa feedback.

- **Input:** `id` (angka)
- **Return:** void

#### `SetSeekAudio`

SET: Lompat audio ke posisi (ms). Bisa dipasang ke Slider. (per id)

- **Input:** `id` (angka), `position` (angka)
- **Return:** void

#### `SetStopAudio`

SET: Hentikan audio (per id). Putar lagi via CallPlayAudio.

- **Input:** `id` (angka)
- **Return:** void

### Properti (Tools_Audio)

#### `AudioLooping`

Audio mengulang otomatis bila selesai. Default false.

- **Tipe:** boolean
- **Akses:** read-write

#### `MediaVolume`

Volume audio 0-100 (default 100, berlaku semua id).

- **Tipe:** angka
- **Akses:** read-write

#### `OpAudio`

Nama operasi Audio (untuk if di ErrorOccurred).

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `ProgressInterval`

Jeda event AudioProgressChanged dalam milidetik (default 500).

- **Tipe:** angka
- **Akses:** read-write

#### `SkipSeconds`

Lompatan Maju/Mundur dalam detik (default 10).

- **Tipe:** angka
- **Akses:** read-write

### Event (Tools_Audio)

#### `AudioFinished`

Audio selesai (tidak loop). Putar lagi via CallPlayAudio.

- **Parameter:** `id` (angka)

#### `AudioProgressChanged`

Progres audio berubah: id + posisi + durasi (ms) + persen 0-100.

- **Parameter:** `id` (angka), `position` (angka), `duration` (angka), `percent` (angka)

#### `AudioStarted`

Audio mulai bunyi: id + durasi (ms) diketahui.

- **Parameter:** `id` (angka), `duration` (angka)

#### `ErrorOccurred`

SATU-SATUNYA error Tools_Audio.

- **Parameter:** `component` (teks), `id` (angka), `message` (teks)

## Tools_Base

> Fondasi Tools: id pool lintas extension + helper Tag untuk id komponen LUAR (auto-konversi teks angka) + util dasar (JSON, URI, waktu, byte, mime). Id unik global via CreateUniqueId, atau per tipe via CreateDownloadId/CreateImageId/CreateAudioId/CreateVideoId. Semua list id URUT PEMBUATAN (index 1 = pertama dibuat). (v17 2026-11-17: ToolsVersion 17; blok baru GetWhatsNew = catatan update yang bisa dibaca lewat blok.)

Versi anotasi: **17**. Jumlah blok: 98 fungsi, 2 properti, 1 event.

### Fungsi (Tools_Base)

#### `BusyIds`

Semua id sibuk lintas tipe (URUT PEMBUATAN, index 1 = pertama dibuat). Kosong = tidak ada.

- **Input:** tidak ada
- **Return:** list

#### `ClearTag`

Buang SELURUH isi satu tag.

- **Input:** `tag` (teks)
- **Return:** void

#### `ConvertBytes`

Ubah angka byte jadi teks (B/KB/MB/GB).

- **Input:** `bytes` (angka)
- **Return:** teks

#### `CreateTagId`

Buat id unik baru di tag ini (Number, angka terkecil yang belum dipakai, langsung di-reservasi). Untuk saat kamulah yang menentukan id-nya (mis. bikin tab sendiri). Beda dengan SaveTagId yang mencatat id dari luar.

- **Input:** `tag` (teks)
- **Return:** angka

#### `CreateUniqueId`

Buat id unik global baru (Number). Reservasi otomatis, siap pakai ke CallDownload / CallShowImage / CallPlayAudio / CallPlayVideoIn (reservasi otomatis diadopsi). Urutan pembuatan = urutan list.

- **Input:** tidak ada
- **Return:** angka

#### `CurrentAudioId`

Id audio terakhir dibuat/di-start. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `CurrentDownloadId`

Id unduhan terakhir dibuat/di-start. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `CurrentDynamicId`

Id dinamis terakhir dibuat. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `CurrentImageId`

Id gambar terakhir dibuat/di-start. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `CurrentLayoutId`

Id layout terakhir dibuat. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `CurrentSheetId`

Id sheet terakhir dibuat. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `CurrentSliderId`

Id slider terakhir dibuat. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `CurrentTaskId`

Id tugas terakhir dibuat. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `CurrentVideoId`

Id video terakhir dibuat/di-start. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `FormatTime`

Ubah milidetik jadi teks jam (m:ss / h:mm:ss) untuk Label.

- **Input:** `millis` (angka)
- **Return:** teks

#### `GetActiveAudioIds`

Id audio yang sedang bunyi (URUT PEMBUATAN).

- **Input:** tidak ada
- **Return:** list

#### `GetActiveDownloadIds`

Id unduhan yang sedang jalan (URUT PEMBUATAN). Yang dijeda TIDAK masuk sini.

- **Input:** tidak ada
- **Return:** list

#### `GetActiveDynamicIds`

Id dinamis yang aktif (URUT PEMBUATAN).

- **Input:** tidak ada
- **Return:** list

#### `GetActiveImageIds`

Id gambar yang sedang tampil/loading (URUT PEMBUATAN).

- **Input:** tidak ada
- **Return:** list

#### `GetActiveLayoutIds`

Id layout yang aktif (URUT PEMBUATAN).

- **Input:** tidak ada
- **Return:** list

#### `GetActiveSheetIds`

Id sheet yang aktif (URUT PEMBUATAN).

- **Input:** tidak ada
- **Return:** list

#### `GetActiveSliderIds`

Id slider yang aktif (URUT PEMBUATAN).

- **Input:** tidak ada
- **Return:** list

#### `GetActiveTaskIds`

Id tugas/alarm yang aktif (URUT PEMBUATAN).

- **Input:** tidak ada
- **Return:** list

#### `GetActiveVideoIds`

Id video yang sedang main (URUT PEMBUATAN).

- **Input:** tidak ada
- **Return:** list

#### `GetAllTags`

Semua nama tag yang masih punya isi (URUT DIBUAT).

- **Input:** tidak ada
- **Return:** list

#### `GetAudioIds`

Semua id audio (URUT PEMBUATAN, index 1 = pertama dibuat).

- **Input:** tidak ada
- **Return:** list

#### `GetDownloadIds`

Semua id unduhan (URUT PEMBUATAN, index 1 = pertama dibuat).

- **Input:** tidak ada
- **Return:** list

#### `GetDynamicIds`

Semua id dinamis Tools_Core (URUT PEMBUATAN, index 1 = pertama dibuat).

- **Input:** tidak ada
- **Return:** list

#### `GetFirstAudioId`

Id audio index 1 = PERTAMA DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetFirstDownloadId`

Id unduhan index 1 = PERTAMA DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetFirstDynamicId`

Id dinamis index 1 = PERTAMA DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetFirstImageId`

Id gambar index 1 = PERTAMA DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetFirstLayoutId`

Id layout index 1 = PERTAMA DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetFirstSheetId`

Id sheet index 1 = PERTAMA DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetFirstSliderId`

Id slider index 1 = PERTAMA DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetFirstTagId`

Id index 1 = PERTAMA DISIMPAN di tag ini. -1 bila kosong.

- **Input:** `tag` (teks)
- **Return:** angka

#### `GetFirstTaskId`

Id tugas index 1 = PERTAMA DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetFirstVideoId`

Id video index 1 = PERTAMA DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetImageIds`

Semua id gambar (URUT PEMBUATAN, index 1 = pertama dibuat).

- **Input:** tidak ada
- **Return:** list

#### `GetLastAudioId`

Id audio TERAKHIR DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetLastDownloadId`

Id unduhan TERAKHIR DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetLastDynamicId`

Id dinamis TERAKHIR DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetLastImageId`

Id gambar TERAKHIR DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetLastLayoutId`

Id layout TERAKHIR DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetLastSheetId`

Id sheet TERAKHIR DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetLastSliderId`

Id slider TERAKHIR DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetLastTagId`

Id TERAKHIR DISIMPAN di tag ini. -1 bila kosong.

- **Input:** `tag` (teks)
- **Return:** angka

#### `GetLastTaskId`

Id tugas TERAKHIR DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetLastVideoId`

Id video TERAKHIR DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetLayoutIds`

Semua id layout Tools_Layout (URUT PEMBUATAN, index 1 = pertama dibuat).

- **Input:** tidak ada
- **Return:** list

#### `GetMimeType`

Tebak mime dari nama file/ekstensi. Tidak perlu file-nya ada.

- **Input:** `fileName` (teks)
- **Return:** teks

#### `GetPreviousAudioId`

Id audio yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.

- **Input:** `id` (angka)
- **Return:** angka

#### `GetPreviousDownloadId`

Id unduhan yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.

- **Input:** `id` (angka)
- **Return:** angka

#### `GetPreviousDynamicId`

Id dinamis yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.

- **Input:** `id` (angka)
- **Return:** angka

#### `GetPreviousImageId`

Id gambar yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.

- **Input:** `id` (angka)
- **Return:** angka

#### `GetPreviousLayoutId`

Id layout yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.

- **Input:** `id` (angka)
- **Return:** angka

#### `GetPreviousSheetId`

Id sheet yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.

- **Input:** `id` (angka)
- **Return:** angka

#### `GetPreviousSliderId`

Id slider yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.

- **Input:** `id` (angka)
- **Return:** angka

#### `GetPreviousTagId`

Id yang disimpan TEPAT SEBELUM id ini di tag itu. id boleh Number / teks angka. -1 bila tidak ada.

- **Input:** `id` (any), `tag` (teks)
- **Return:** angka

#### `GetPreviousTaskId`

Id tugas yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.

- **Input:** `id` (angka)
- **Return:** angka

#### `GetPreviousVideoId`

Id video yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.

- **Input:** `id` (angka)
- **Return:** angka

#### `GetSheetIds`

Semua id sheet Tools_Sheet (URUT PEMBUATAN, index 1 = pertama dibuat).

- **Input:** tidak ada
- **Return:** list

#### `GetSliderIds`

Semua id slider Tools_Image (URUT PEMBUATAN, index 1 = pertama dibuat).

- **Input:** tidak ada
- **Return:** list

#### `GetTagIdCount`

Jumlah id tercatat di tag ini.

- **Input:** `tag` (teks)
- **Return:** angka

#### `GetTagIds`

Semua id di tag ini (Number semua, URUT MENYIMPAN, index 1 = pertama disimpan).

- **Input:** `tag` (teks)
- **Return:** list

#### `GetTaskIds`

Semua id tugas/alarm Tools_Task (URUT PEMBUATAN, index 1 = pertama dibuat).

- **Input:** tidak ada
- **Return:** list

#### `GetVideoIds`

Semua id video (URUT PEMBUATAN, index 1 = pertama dibuat).

- **Input:** tidak ada
- **Return:** list

#### `GetWhatsNew`

Catatan update paket Tools (dibaca lewat blok, karena panel info extension tidak bisa diubah manual).

- **Input:** tidak ada
- **Return:** teks

#### `IsAudioIdActive`

True bila id audio ini sedang bunyi.

- **Input:** `id` (angka)
- **Return:** boolean

#### `IsDownloadIdActive`

True bila id unduhan ini sedang jalan.

- **Input:** `id` (angka)
- **Return:** boolean

#### `IsDynamicIdActive`

True bila id dinamis ini masih ada.

- **Input:** `id` (angka)
- **Return:** boolean

#### `IsIdBusy`

True bila id sedang dipakai operasi lain.

- **Input:** `id` (angka)
- **Return:** boolean

#### `IsImageIdActive`

True bila id gambar ini sedang tampil/loading.

- **Input:** `id` (angka)
- **Return:** boolean

#### `IsLayoutIdActive`

True bila id layout ini masih ada.

- **Input:** `id` (angka)
- **Return:** boolean

#### `IsSheetIdActive`

True bila id sheet ini masih terdaftar.

- **Input:** `id` (angka)
- **Return:** boolean

#### `IsSliderIdActive`

True bila id slider ini masih ada.

- **Input:** `id` (angka)
- **Return:** boolean

#### `IsTagIdSaved`

True bila id ini tercatat di tag itu (teks angka ikut cocok: "5" = 5).

- **Input:** `id` (any), `tag` (teks)
- **Return:** boolean

#### `IsTaskIdActive`

True bila id tugas ini aktif (jalan/jeda/alarm terpasang).

- **Input:** `id` (angka)
- **Return:** boolean

#### `IsVideoIdActive`

True bila id video ini sedang main.

- **Input:** `id` (angka)
- **Return:** boolean

#### `JsonDecode`

Decode teks JSON jadi dictionary/list. Gagal = "" + error.

- **Input:** `json` (teks)
- **Return:** any

#### `JsonEncode`

Encode list/dictionary/teks jadi JSON.

- **Input:** `value` (any)
- **Return:** teks

#### `LastError`

Fallback polling untuk event ErrorOccurred: pesan error terakhir operasi ini (Id/Json/Tag, kosong bila belum ada).

- **Input:** `operation` (teks)
- **Return:** teks

#### `ReleaseId`

Lepas paksa id yang macet. Jangan dipakai saat operasinya masih jalan (pakai Cancel/Stop/Remove dulu).

- **Input:** `id` (angka)
- **Return:** void

#### `RemoveAudioId`

Hapus id audio: hentikan bila bunyi + lepas id.

- **Input:** `id` (angka)
- **Return:** void

#### `RemoveDownloadId`

Hapus id unduhan: batalkan bila jalan + lepas id.

- **Input:** `id` (angka)
- **Return:** void

#### `RemoveDynamicId`

Hapus id dinamis: lepas view + lepas id.

- **Input:** `id` (angka)
- **Return:** void

#### `RemoveImageId`

Hapus id gambar: lepas dari layout + lepas id.

- **Input:** `id` (angka)
- **Return:** void

#### `RemoveLayoutId`

Hapus id layout: lepas dari induk (anak dinamis di dalamnya ikut beres) + lepas id.

- **Input:** `id` (angka)
- **Return:** void

#### `RemoveSheetId`

Hapus id sheet: tutup diam-diam + lepas id.

- **Input:** `id` (angka)
- **Return:** void

#### `RemoveSliderId`

Hapus id slider: lepas dari layout (autoplay mati) + lepas id.

- **Input:** `id` (angka)
- **Return:** void

#### `RemoveTagId`

Hapus satu id dari tag (hanya lepas catatan, komponen luar tidak diusik). Tag yang kosong ikut dibuang. Tidak tercatat via ErrorOccurred.

- **Input:** `id` (any), `tag` (teks)
- **Return:** void

#### `RemoveTaskId`

Hapus id tugas: batalkan jadwal + cabut alarm sistem + lepas id.

- **Input:** `id` (angka)
- **Return:** void

#### `RemoveVideoId`

Hapus id video: hentikan + lepas layout + lepas id.

- **Input:** `id` (angka)
- **Return:** void

#### `SaveTagId`

Simpan id komponen LUAR ke tag (mis. WebView pihak ketiga). id boleh Number ATAU teks angka ("5" = 5, otomatis dikonversi). Id dobel = tetap satu. Gagal (tag kosong / bukan angka) via ErrorOccurred.

- **Input:** `id` (any), `tag` (teks)
- **Return:** void

#### `SuggestId`

Id yang disarankan: angka terkecil >= 1 yang belum dipakai siapa pun (tidak di-reservasi, langsung pakai).

- **Input:** tidak ada
- **Return:** angka

#### `ToolsVersion`

Versi paket Tools (sama untuk semua Tools_* dalam satu .aix).

- **Input:** tidak ada
- **Return:** angka

#### `UriDecode`

Decode teks URL-encoded.

- **Input:** `text` (teks)
- **Return:** teks

#### `UriEncode`

Encode teks untuk URL (spasi jadi %20).

- **Input:** `text` (teks)
- **Return:** teks

### Properti (Tools_Base)

#### `OpId`

Nama operasi Id (untuk if di ErrorOccurred).

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `OpJson`

Nama operasi Json (untuk if di ErrorOccurred).

- **Tipe:** teks
- **Akses:** baca saja (getter)

### Event (Tools_Base)

#### `ErrorOccurred`

SATU-SATUNYA error Tools_Base. component = nama operasi (bandingkan dengan blok Op...).

- **Parameter:** `component` (teks), `id` (angka), `message` (teks)

## Tools_Compress

> Tools_Compress: bikin ZIP, ekstrak ZIP, intip isi ZIP, dan deteksi jenis arsip (zip/rar/7z/tar/gz/...) dari magic-bytes. RAR/7z terdeteksi namun belum bisa dibuka (butuh library luar) — ZIP penuh bisa. (v16: gate izin otomatis. v17 2026-11-17: penomoran versi.)

Versi anotasi: **17**. Jumlah blok: 7 fungsi, 2 properti, 7 event.

### Fungsi (Tools_Compress)

#### `CallUnzip`

CALL: ekstrak ZIP ke folder. Struktur folder di dalam ZIP dijaga. Batalkan via SetCancelZip. Pantau via UnzipStarted/ProgressChanged/Finished.

- **Input:** `zipPath` (teks), `destFolder` (teks)
- **Return:** void

#### `CallZipFiles`

CALL: bikin ZIP dari BANYAK file sekaligus (tanpa loop). paths = list path sumber. destZip = path file zip tujuan. Mulai = menembak Started; gagal via ErrorOccurred. Pantau via ZipStarted/ProgressChanged/Finished. Satu proses ZIP per saat.

- **Input:** `paths` (any), `destZip` (teks)
- **Return:** void

#### `DetectArchiveType`

Deteksi jenis file dari ISINYA (bukan ekstensi). Kembali: zip, rar, 7z, gz, bz2, xz, tar, apk, pdf, png, jpg, gif, mp3, mp4, atau unknown. Instan, tanpa id.

- **Input:** `path` (teks)
- **Return:** teks

#### `IsZipBusy`

True bila ada proses ZIP/Unzip berjalan.

- **Input:** tidak ada
- **Return:** boolean

#### `LastError`

Fallback polling untuk event ErrorOccurred: pesan error terakhir operasi ini (Zip/Unzip, kosong bila belum ada).

- **Input:** `operation` (teks)
- **Return:** teks

#### `ListZipContents`

Daftar isi ZIP (nama entri). Instan. Kosong bila bukan ZIP.

- **Input:** `zipPath` (teks)
- **Return:** list

#### `SetCancelZip`

SET: batalkan proses ZIP/Unzip berjalan. Tanpa feedback.

- **Input:** tidak ada
- **Return:** void

### Properti (Tools_Compress)

#### `OpUnzip`

Nama operasi Unzip (untuk if di ErrorOccurred).

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `OpZip`

Nama operasi Zip (untuk if di ErrorOccurred).

- **Tipe:** teks
- **Akses:** baca saja (getter)

### Event (Tools_Compress)

#### `ErrorOccurred`

SATU-SATUNYA error Tools_Compress.

- **Parameter:** `component` (teks), `id` (angka), `message` (teks)

#### `UnzipFinished`

Ekstrak selesai: success + folder + jumlah ok/gagal.

- **Parameter:** `success` (boolean), `destFolder` (teks), `doneCount` (angka), `failCount` (angka), `message` (teks)

#### `UnzipProgressChanged`

Progres ekstrak berubah: index/total + nama entri + persen.

- **Parameter:** `index` (angka), `total` (angka), `entryName` (teks), `entryPercent` (angka)

#### `UnzipStarted`

Ekstrak ZIP dimulai: nama ZIP + total entri + folder tujuan.

- **Parameter:** `zipName` (teks), `total` (angka), `destFolder` (teks)

#### `ZipFinished`

Bikin ZIP selesai: success + path ZIP + fileName + jumlah ok/gagal.

- **Parameter:** `success` (boolean), `zipPath` (teks), `fileName` (teks), `doneCount` (angka), `failCount` (angka), `message` (teks)

#### `ZipProgressChanged`

Progres bikin ZIP berubah: index/total + nama file + persen.

- **Parameter:** `index` (angka), `total` (angka), `fileName` (teks), `filePercent` (angka)

#### `ZipStarted`

Bikin ZIP dimulai: total file + path ZIP tujuan.

- **Parameter:** `total` (angka), `zipPath` (teks)

## Tools_Core

> Tools_Core: mesin komponen dinamis generik ber-id. Bikin komponen MIT apa pun saat aplikasi jalan (Button, Label, TextBox, Image, CheckBox, Switch, Slider, Spinner, ListView, DatePicker, TimePicker, Horizontal/VerticalArrangement, Horizontal/VerticalScrollArrangement) ke dalam layout, lengkap dengan event Click/LongClick/ComponentCreated ber-id, plus SetProperty/GetProperty/Invoke generik. Id dikelola CreateDynamicId. List id URUT PEMBUATAN. Catatan: event klik komponen dinamis dilayani Tools_Core (blok Any generik MIT tidak ikut terpicu untuknya). (v17 2026-11-17: tanpa perubahan fungsi; penomoran versi.)

Versi anotasi: **17**. Jumlah blok: 20 fungsi, 1 properti, 5 event.

### Fungsi (Tools_Core)

#### `CallCreateDynamicButton`

CALL: Jalan pintas: bikin Button dinamis ber-id lengkap dengan teks, warna, ukuran font, tebal, visibilitas. Sama seperti Create + Set berulang, tapi satu blok.

- **Input:** `container` (component), `id` (angka), `text` (teks), `textColor` (angka), `backgroundColor` (angka), `fontSize` (angka), `bold` (boolean), `visible` (boolean)
- **Return:** void

#### `CallCreateDynamicComponent`

Bikin komponen MIT dinamis ber-id ke dalam layout (Arrangement MIT / dinamis). typeName: Button, Label, TextBox, Image, CheckBox, Switch, Slider, Spinner, ListView, DatePicker, TimePicker, HorizontalArrangement, VerticalArrangement, HorizontalScrollArrangement, VerticalScrollArrangement. id = penanda (minta ke CreateDynamicId, reservasi otomatis diadopsi). Sukses menembak ComponentCreated; klik/longklik menembak CoreClicked/CoreLongClicked; gagal via ErrorOccurred.

- **Input:** `typeName` (teks), `container` (component), `id` (angka)
- **Return:** void

#### `CallCreateDynamicImage`

CALL: Jalan pintas: bikin Image (gambar MIT) dinamis ber-id dengan picture + visibilitas. Isi picture seperti Image.Picture biasa (path/asset/URL).

- **Input:** `container` (component), `id` (angka), `picturePath` (teks), `visible` (boolean)
- **Return:** void

#### `CallCreateDynamicLabel`

CALL: Jalan pintas: bikin Label dinamis ber-id lengkap dengan teks, warna, ukuran font, tebal, visibilitas.

- **Input:** `container` (component), `id` (angka), `text` (teks), `textColor` (angka), `backgroundColor` (angka), `fontSize` (angka), `bold` (boolean), `visible` (boolean)
- **Return:** void

#### `CallCreateDynamicTextBox`

CALL: Jalan pintas: bikin TextBox dinamis ber-id lengkap dengan hint, warna, ukuran font, tebal, visibilitas.

- **Input:** `container` (component), `id` (angka), `hint` (teks), `textColor` (angka), `backgroundColor` (angka), `fontSize` (angka), `bold` (boolean), `visible` (boolean)
- **Return:** void

#### `CreateDynamicId`

Buat id dinamis unik baru (Number, siap pakai ke CreateDynamicComponent).

- **Input:** tidak ada
- **Return:** angka

#### `CurrentDynamicId`

Id dinamis terakhir dibuat. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetActiveDynamicIds`

Id dinamis yang aktif (URUT PEMBUATAN).

- **Input:** tidak ada
- **Return:** list

#### `GetDynamicComponent`

Ambil komponen dinamis ber-id (untuk blok Any generik MIT / blok yang butuh soket komponen). null bila tidak ada.

- **Input:** `id` (angka)
- **Return:** any

#### `GetDynamicIds`

Semua id dinamis (URUT PEMBUATAN, index 1 = pertama dibuat).

- **Input:** tidak ada
- **Return:** list

#### `GetDynamicProperty`

Baca properti komponen dinamis ber-id (teks). Kosong bila tidak ada/gagal (lihat ErrorOccurred).

- **Input:** `id` (angka), `name` (teks)
- **Return:** teks

#### `GetFirstDynamicId`

Id index 1 = PERTAMA DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetLastDynamicId`

Id TERAKHIR DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetPreviousDynamicId`

Id yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.

- **Input:** `id` (angka)
- **Return:** angka

#### `InvokeDynamic`

Panggil fungsi komponen dinamis ber-id (nama fungsi MIT + list argumen). Sinkron: hasil langsung dikembalikan sebagai teks (kosong bila void/gagal; gagal via ErrorOccurred).

- **Input:** `id` (angka), `name` (teks), `args` (list)
- **Return:** teks

#### `IsDynamicIdActive`

True bila id dinamis ini masih ada.

- **Input:** `id` (angka)
- **Return:** boolean

#### `LastError`

Fallback polling untuk event ErrorOccurred: pesan error terakhir operasi ini (kosong bila belum ada).

- **Input:** `operation` (teks)
- **Return:** teks

#### `RemoveDynamicId`

Hapus komponen dinamis: lepas view dari layout + lepas id (otomatis beres-beres).

- **Input:** `id` (angka)
- **Return:** void

#### `SetClearDynamicComponents`

SET: Hapus SEMUA komponen dinamis Tools_Core (id ikut dilepas).

- **Input:** tidak ada
- **Return:** void

#### `SetDynamicProperty`

SET: Set properti komponen dinamis ber-id (nama properti MIT, mis. Text, BackgroundColor, FontSize). Gagal via ErrorOccurred.

- **Input:** `id` (angka), `name` (teks), `value` (any)
- **Return:** void

### Properti (Tools_Core)

#### `OpCore`

Nama operasi Core (untuk if di ErrorOccurred).

- **Tipe:** teks
- **Akses:** baca saja (getter)

### Event (Tools_Core)

#### `ComponentCreated`

Komponen dinamis jadi: id + nama tipe.

- **Parameter:** `id` (angka), `typeName` (teks)

#### `CoreClicked`

Komponen dinamis di-klik: id.

- **Parameter:** `id` (angka)

#### `CoreDoubleClicked`

Komponen dinamis di-klik 2x cepat: id.

- **Parameter:** `id` (angka)

#### `CoreLongClicked`

Komponen dinamis di-long-klik: id.

- **Parameter:** `id` (angka)

#### `ErrorOccurred`

SATU-SATUNYA error Tools_Core. component = nama operasi (bandingkan dengan blok OpCore).

- **Parameter:** `component` (teks), `id` (angka), `message` (teks)

## Tools_Downloader

> Tools_Downloader: unduh paralel ber-id (pause/resume/cancel per id, Started/ProgressChanged/Finished) + unduh batch list URL. Id dikelola Tools_Base (CreateUniqueId) atau CreateDownloadId. List id URUT PEMBUATAN. (v17 2026-11-17: CallDownload 4 pintu http/data:/content:/file:; blok mandiri CallGuessFileName + event FileNameGuessed; v16: gate izin otomatis + SetDownloadNotification.)

Versi anotasi: **17**. Jumlah blok: 22 fungsi, 1 properti, 7 event.

### Fungsi (Tools_Downloader)

#### `CallDownload`

CALL: unduh ke folder (otomatis di-root /storage/emulated/0; absolut apa adanya). url = http(s) ATAU data:URL (base64/url-encoded, mis. data:image/png;base64,...) ATAU content: URI Android (dari file picker) ATAU file: URL / path lokal (disalin). blob: TIDAK didukung (itu objek browser; ubah dulu jadi base64 via JavaScript, lalu kirim sebagai data:URL atau ke CallSaveBase64ToFile). fileName kosong = nama otomatis. id = angka bebas yang belum dipakai (minta ke Tools_Base CreateUniqueId atau CreateDownloadId). Hasil lewat DownloadStarted/ProgressChanged/Finished (bawa id).

- **Input:** `url` (teks), `fileName` (teks), `folder` (teks), `id` (angka)
- **Return:** void

#### `CallDownloadList`

CALL: unduh BANYAK URL sekaligus (tanpa loop manual). fileNames boleh kosong (= otomatis semua). folder seperti CallDownload. Tanpa return: pantau tiap id lewat DownloadStarted/DownloadProgressChanged/DownloadFinished. Ringkasan lewat DownloadBatchFinished. Satu batch per saat.

- **Input:** `urls` (any), `fileNames` (any), `folder` (teks)
- **Return:** void

#### `CallGuessFileName`

CALL: tebak nama file dari URL saja (1 input, mandiri — bisa disandingkan komponen lain, mis. Web bawaan MIT untuk rename). http(s) = ambil header asli server (ikuti redirect); data:/content:/file:/path = tebak lokal tanpa internet. Hasil lewat FileNameGuessed (url + fileName + mimeType).

- **Input:** `url` (teks)
- **Return:** void

#### `CreateDownloadId`

Buat id unduhan unik baru (Number, siap pakai ke CallDownload). Urutan pembuatan menentukan urutan list.

- **Input:** tidak ada
- **Return:** angka

#### `CurrentDownloadId`

Id unduhan yang terakhir dibuat/di-start. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetActiveDownloadIds`

Id unduhan yang sedang jalan (URUT PEMBUATAN). Yang dijeda TIDAK masuk sini.

- **Input:** tidak ada
- **Return:** list

#### `GetBatchProgress`

Fallback polling untuk event batch: list [batchJalan, total, selesai, ok].

- **Input:** tidak ada
- **Return:** list

#### `GetDownloadIds`

Semua id unduhan yang dipegang (URUT PEMBUATAN, index 1 = pertama dibuat). Termasuk yang dijeda.

- **Input:** tidak ada
- **Return:** list

#### `GetDownloadProgress`

Fallback polling untuk event DownloadProgressChanged: list [persen 0-100, byte diterima, byte total].

- **Input:** `id` (angka)
- **Return:** list

#### `GetDownloadResult`

Fallback polling untuk event DownloadFinished: list [success, savePath, fileName, message]. Tetap ada setelah selesai sampai RemoveDownloadId.

- **Input:** `id` (angka)
- **Return:** list

#### `GetDownloadStatus`

Fallback polling untuk event Started/Progress/Finished: status unduhan ber-id. Salah satu: jalan, jeda, selesai, gagal, batal, atau tidak ada.

- **Input:** `id` (angka)
- **Return:** teks

#### `GetFileName`

GET: nama file dari URL, mime, dan content-disposition (dipakai otomatis bila fileName CallDownload dikosongkan).

- **Input:** `url` (teks), `mimeType` (teks), `contentDisposition` (teks)
- **Return:** teks

#### `GetFirstDownloadId`

Id index 1 = PERTAMA DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetLastDownloadId`

Id TERAKHIR DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetPreviousDownloadId`

Id yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.

- **Input:** `id` (angka)
- **Return:** angka

#### `IsDownloadIdActive`

True bila id unduhan ini sedang jalan (tidak dijeda).

- **Input:** `id` (angka)
- **Return:** boolean

#### `LastError`

Fallback polling untuk event ErrorOccurred: pesan error terakhir operasi ini (kosong bila belum ada).

- **Input:** `operation` (teks)
- **Return:** teks

#### `RemoveDownloadId`

Hapus id unduhan: batalkan bila jalan + lepas id (otomatis beres-beres).

- **Input:** `id` (angka)
- **Return:** void

#### `SetCancelDownload`

SET: batalkan unduhan berjalan (per id). File sebagian dihapus. Tanpa feedback.

- **Input:** `id` (angka)
- **Return:** void

#### `SetDownloadNotification`

SET: tampilkan progres unduhan di bar notifikasi Android (per id, otomatis: nama file + persen). show=true = tampil + update tiap Started/Progress/Finished; show=false = matikan + hapus notif id itu. Pasang sesudah CallDownload, atau di dalam event DownloadStarted/ProgressChanged (id sudah terdaftar; di Finished kurang relevan). Tiap id hasil CallDownloadList bisa dipasang satu-satu, agar sistem bisa bedakan Download vs DownloadList. Android 13+ butuh izin POST_NOTIFICATIONS (minta via blok GetPermission Tools_File). Tanpa feedback.

- **Input:** `id` (angka), `show` (boolean)
- **Return:** void

#### `SetPauseDownload`

SET: jeda unduhan berjalan (per id). Lanjut via SetResumeDownload. Tanpa feedback.

- **Input:** `id` (angka)
- **Return:** void

#### `SetResumeDownload`

SET: lanjutkan unduhan yang dijeda (per id). Tanpa feedback.

- **Input:** `id` (angka)
- **Return:** void

### Properti (Tools_Downloader)

#### `OpDownload`

Nama operasi Download (untuk if di ErrorOccurred).

- **Tipe:** teks
- **Akses:** baca saja (getter)

### Event (Tools_Downloader)

#### `DownloadBatchFinished`

Batch unduhan selesai semua: total + jumlah ok + folder.

- **Parameter:** `total` (angka), `okCount` (angka), `folder` (teks)

#### `DownloadBatchStarted`

Batch unduhan dimulai: total URL + folder tujuan.

- **Parameter:** `total` (angka), `folder` (teks)

#### `DownloadFinished`

Unduhan selesai: success pertama, fileName paling belakang. success=false = gagal/dibatalkan (lihat message + ErrorOccurred).

- **Parameter:** `id` (angka), `success` (boolean), `savePath` (teks), `fileName` (teks), `message` (teks)

#### `DownloadProgressChanged`

Progres unduhan berubah: persen 0-100 + byte.

- **Parameter:** `id` (angka), `percent` (angka), `receivedBytes` (angka), `totalBytes` (angka)

#### `DownloadStarted`

Unduhan dimulai: siapkan tampilan (nama + path tujuan).

- **Parameter:** `id` (angka), `fileName` (teks), `savePath` (teks)

#### `ErrorOccurred`

SATU-SATUNYA error Tools_Downloader. component = nama operasi (bandingkan dengan blok OpDownload).

- **Parameter:** `component` (teks), `id` (angka), `message` (teks)

#### `FileNameGuessed`

Hasil CallGuessFileName: url asal + fileName tebakan + mimeType (kosong bila tak diketahui).

- **Parameter:** `url` (teks), `fileName` (teks), `mimeType` (teks)

## Tools_File

> Tools_File (sistem draft): awalan Is = boolean (colok ke if), awalan Call = async + hasil lewat event, awalan Set = tanpa feedback. Semua path otomatis di-root ke /storage/emulated/0 ("/Download/a.jpg" jadi "/storage/emulated/0/Download/a.jpg"; absolut apa adanya). Salin/pindah digabung satu blok CallCopyOrMoveFile (isMove) + info duplikat. (v17 2026-11-17: penomoran versi. v16: gate izin otomatis 16 operasi + blok pintar CallEnsure*/Is* + 10 Helper Permission + all-files access Android 11+.)

Versi anotasi: **17**. Jumlah blok: 46 fungsi, 26 properti, 20 event.

### Fungsi (Tools_File)

#### `CallCopyOrMoveFile`

CALL: salin ATAU pindah file (isMove=true = pindah). Path otomatis di-root /storage/emulated/0. Nama tujuan boleh beda. Bila tujuan SUDAH ADA: tidak disalin, Finished(success=true, isDuplicated=true) -> rename manual ala contoh. Asset hanya bisa jadi sumber. Satu job per saat.

- **Input:** `fromFileName` (teks), `toFileName` (teks), `isMove` (boolean)
- **Return:** void

#### `CallCreateEmptyFile`

CALL: buat file kosong (folder induk dibuat otomatis). Sudah ada = success. Hasil lewat EmptyFileCreated.

- **Input:** `path` (teks)
- **Return:** void

#### `CallDeleteFile`

CALL: hapus file/folder (folder rekursif). Tidak ada = success. Hasil lewat FileDeleted.

- **Input:** `path` (teks)
- **Return:** void

#### `CallDeleteList`

CALL: hapus BANYAK path sekaligus (tanpa loop manual). paths = list / teks koma. Hasil lewat FileListDeleted (total + terhapus).

- **Input:** `paths` (any)
- **Return:** void

#### `CallDeleteNomediaFile`

CALL: hapus file .nomedia di folder. Tidak ada = success. Hasil lewat NomediaFileDeleted.

- **Input:** `folder` (teks)
- **Return:** void

#### `CallEnsurePermission`

CALL: pastikan 1 izin beres TANPA rakit manual — blok ini yang nentuin caranya (cek OS dulu): sudah granted = langsung GotPermission(true); storage Android 11+ belum = OTOMATIS buka Settings + GotPermission(false + pesan); lainnya = dialog izin + GotPermission. Tanpa return. Colok blok Permission... ATAU teks string izinnya.

- **Input:** `permission` (teks)
- **Return:** void

#### `CallEnsureStoragePermission`

CALL: pastikan izin tulis storage beres (tanpa input apa pun) — hasil lewat GotPermission. Storage Android 11+ belum granted = otomatis buka Settings.

- **Input:** tidak ada
- **Return:** void

#### `CallMakeDirectory`

CALL: buat folder (termasuk induknya). Sudah ada = success. Hasil lewat DirectoryCreated.

- **Input:** `folder` (teks)
- **Return:** void

#### `CallMakeNomediaFile`

CALL: buat file .nomedia di dalam folder (sembunyikan dari galeri/musik). Hasil lewat NomediaFileCreated.

- **Input:** `folder` (teks)
- **Return:** void

#### `CallReadText`

CALL: baca file teks (UTF-8) di background. Hasil lewat GotText (content = isi file). Mulai lewat ReadStarted.

- **Input:** `path` (teks)
- **Return:** void

#### `CallRenameFile`

CALL: ganti nama FILE (tanpa pindah folder). newName = nama saja. Hasil lewat FileRenamed (path = nama baru).

- **Input:** `path` (teks), `newName` (teks)
- **Return:** void

#### `CallRenameFolder`

CALL: ganti nama FOLDER (tanpa pindah). newName = nama saja. Hasil lewat FolderRenamed.

- **Input:** `path` (teks), `newName` (teks)
- **Return:** void

#### `CallSaveBase64ToFile`

CALL: simpan base64 LANGSUNG ke storage (tanpa validasi gambar, lebih cepat). Hasil lewat Base64FileSaved. Polling: GetLastBase64FilePath.

- **Input:** `base64` (teks), `path` (teks)
- **Return:** void

#### `CallSaveFile`

CALL: tulis teks ke file (UTF-8). append=true = sambung di akhir. Hasil lewat FileSaved.

- **Input:** `path` (teks), `content` (teks), `append` (boolean)
- **Return:** void

#### `CallSetHiddenDirectory`

CALL: jadikan directory hidden (nama jadi .nama). Sudah hidden = success. Hasil lewat DirectoryHidden.

- **Input:** `directory` (teks)
- **Return:** void

#### `FileSize`

Ukuran file dalam byte. Folder = total isi rekursif. path seperti IsFileOrDirectoryExists.

- **Input:** `path` (teks)
- **Return:** angka

#### `FileTypeOf`

Kategori file dari nama/path: image, audio, video, text, archive, document, apk, atau unknown. Berdasarkan ekstensi (tak peduli huruf besar). Bandingkan dengan blok TypeImage/dll. Olah string saja.

- **Input:** `pathOrName` (teks)
- **Return:** teks

#### `FreeSpace`

Sisa ruang kosong (byte) di kartu SD (/storage/emulated/0).

- **Input:** tidak ada
- **Return:** angka

#### `GetAndroidSdCardDirectory`

Root kartu SD LEPASAN (removable, mis. /storage/XXXX-XXXX). Kosong bila tidak ada / tidak terbaca. Path SD dipakai apa adanya (tidak di-root ulang).

- **Input:** tidak ada
- **Return:** teks

#### `GetAndroidSpecificDirectory`

Folder khusus aplikasi (tanpa izin tambahan, aman Android 11+). Cth: /storage/emulated/0/Android/data/<paket>/files. Hasilnya bisa langsung dipakai sebagai path blok lain.

- **Input:** tidak ada
- **Return:** teks

#### `GetAndroidStorageDirectory`

Root kartu SD utama (/storage/emulated/0) — awal semua path strategi baru.

- **Input:** tidak ada
- **Return:** teks

#### `GetCacheDir`

Path folder cache aplikasi (untuk file sementara). Tanpa izin tambahan.

- **Input:** tidak ada
- **Return:** teks

#### `GetExtension`

Ekstensi tanpa titik (huruf kecil). Cth: foto.JPG -> jpg. Olah string saja.

- **Input:** `path` (teks)
- **Return:** teks

#### `GetExtensionsForType`

Daftar ekstensi (tanpa titik) yang masuk kategori type. Tipe tak dikenal = list kosong + error. Olah string saja.

- **Input:** `type` (teks)
- **Return:** list

#### `GetFileName`

Nama file dari sebuah path (buang foldernya). Olah string saja, tanpa scope.

- **Input:** `path` (teks)
- **Return:** teks

#### `GetFullPath`

Path absolut dari input (di-root ke /storage/emulated/0; absolut/URL/aset apa adanya).

- **Input:** `path` (teks)
- **Return:** teks

#### `GetLastBase64FilePath`

Fallback polling untuk Base64FileSaved: path file base64 terakhir (kosong bila gagal/belum ada).

- **Input:** tidak ada
- **Return:** teks

#### `GetParentFolder`

Folder induk dari sebuah path. Cth: /a/b/c.jpg -> /a/b. Olah string saja.

- **Input:** `path` (teks)
- **Return:** teks

#### `GetPermission`

Minta izin Android (cth: android.permission.READ_EXTERNAL_STORAGE). Hasil lewat GotPermission. Pasang need_permission dari event lain ke sini. Android 11+: izin storage dialihkan ke 'Akses semua file' (pakai OpenAllFilesAccessSettings; cek IsAllFilesAccessGranted).

- **Input:** `permission` (teks)
- **Return:** void

#### `IsAllFilesAccessGranted`

True bila aplikasi boleh tulis SEMUA file bersama. Android 11+ = 'Akses semua file' aktif; Android 10 ke bawah selalu true (izin tulis biasa cukup via GetPermission).

- **Input:** tidak ada
- **Return:** boolean

#### `IsBusy`

True bila ada salinan/pindahan CallCopyOrMoveFile yang masih berjalan.

- **Input:** tidak ada
- **Return:** boolean

#### `IsFileOrDirectoryExists`

Apakah file ATAU folder ada di jalur (otomatis di-root /storage/emulated/0; absolut apa adanya). Aset (//...) bisa dicek.

- **Input:** `path` (teks)
- **Return:** boolean

#### `IsHiddenDirectory`

Apakah directory ada di jalur DAN hidden (nama diawali titik). Boleh tulis dengan/tanpa titik. path seperti IsFileOrDirectoryExists.

- **Input:** `directory` (teks)
- **Return:** boolean

#### `IsNomediaExists`

Apakah file .nomedia ada di dalam folder. path seperti IsFileOrDirectoryExists.

- **Input:** `folder` (teks)
- **Return:** boolean

#### `IsPermissionGranted`

True bila izin ini sudah granted. Untuk izin storage di Android 11+ = cek 'Akses semua file'. Android 6 ke bawah selalu true (izin sejak install). Colok blok Permission... ATAU teks string izinnya.

- **Input:** `permission` (teks)
- **Return:** boolean

#### `IsSdCardExists`

True bila kartu SD LEPASAN (removable) terpasang dan terbaca.

- **Input:** tidak ada
- **Return:** boolean

#### `IsStoragePermissionGranted`

True bila izin tulis storage sudah beres (tanpa input apa pun). Android 11+ = 'Akses semua file' aktif; Android 10 ke bawah = izin tulis granted.

- **Input:** tidak ada
- **Return:** boolean

#### `LastError`

Fallback polling untuk event ErrorOccurred: pesan error terakhir operasi ini (Directory/File/Read/Base64/CopyOrMove/Pick/Open/Permission, kosong bila belum ada).

- **Input:** `operation` (teks)
- **Return:** teks

#### `ListFiles`

Daftar file (path lengkap). filter = ekstensi tanpa titik (boleh koma: jpg,png; kosong = semua). recursive = sertakan isi subfolder. path seperti IsFileOrDirectoryExists.

- **Input:** `path` (teks), `filter` (teks), `recursive` (boolean)
- **Return:** list

#### `ListFilesByType`

Daftar file (path lengkap) yang kategorinya cocok. type = image/audio/video/text/archive/document/apk (boleh alias: gambar/musik/vidio/teks/arsip/dokumen) atau salah satu blok TypeImage/dll. recursive = sertakan isi subfolder. path seperti IsFileOrDirectoryExists.

- **Input:** `folder` (teks), `type` (teks), `recursive` (boolean)
- **Return:** list

#### `ListFolders`

Daftar subfolder (path lengkap, tanpa rekursif). path seperti IsFileOrDirectoryExists.

- **Input:** `folder` (teks)
- **Return:** list

#### `OpenAllFilesAccessSettings`

Buka layar Settings 'Akses semua file' untuk aplikasi ini (Android 11+). WAJIB agar tulis ke /storage/emulated/0 & SD card jalan. Setelah kembali, cek lewat IsAllFilesAccessGranted. Tanpa feedback.

- **Input:** tidak ada
- **Return:** void

#### `OpenFile`

Buka file dengan aplikasi lain (musik, gambar, PDF, ...). path seperti IsFileOrDirectoryExists. Hasil lewat AfterOpenFile. Asset disalin ke cache dulu.

- **Input:** `path` (teks)
- **Return:** void

#### `OpenFilePicker`

Buka pemilih file. mimeType contoh: image/* (kosong = semua). Hasil lewat AfterFilePicked (uri + path + nama file). Satu picker per saat.

- **Input:** `mimeType` (teks)
- **Return:** void

#### `SetCancelCopyOrMove`

SET: batalkan salinan/pindahan berjalan. Tanpa feedback (file sebagian dihapus).

- **Input:** tidak ada
- **Return:** void

#### `TotalSpace`

Total kapasitas (byte) kartu SD (/storage/emulated/0). -1 bila gagal.

- **Input:** tidak ada
- **Return:** angka

### Properti (Tools_File)

#### `OpBase64`

Nama operasi Base64 (untuk if di ErrorOccurred).

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `OpCopyOrMove`

Nama operasi salin/pindah (untuk if di ErrorOccurred).

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `OpDirectory`

Nama operasi direktori (untuk if di ErrorOccurred).

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `OpFile`

Nama operasi File umum (untuk if di ErrorOccurred).

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `OpOpen`

Nama operasi Open (untuk if di ErrorOccurred).

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `OpPermission`

Nama operasi Permission (untuk if di ErrorOccurred).

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `OpPick`

Nama operasi Pick (untuk if di ErrorOccurred).

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `OpRead`

Nama operasi Read (untuk if di ErrorOccurred).

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `PermissionCamera`

Helper read-only: izin kamera. Colok ke IsPermissionGranted / CallEnsurePermission (tanpa ketik string).

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `PermissionLocationCoarse`

Helper read-only: izin lokasi kasar. Colok ke IsPermissionGranted / CallEnsurePermission.

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `PermissionLocationFine`

Helper read-only: izin lokasi akurat. Colok ke IsPermissionGranted / CallEnsurePermission.

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `PermissionMicrophone`

Helper read-only: izin mikrofon (rekam suara). Colok ke IsPermissionGranted / CallEnsurePermission.

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `PermissionNotifications`

Helper read-only: izin notifikasi (Android 13+). Colok ke IsPermissionGranted / CallEnsurePermission.

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `PermissionReadAudio`

Helper read-only: izin baca audio (Android 13+). Colok ke IsPermissionGranted / CallEnsurePermission.

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `PermissionReadImages`

Helper read-only: izin baca gambar (Android 13+). Colok ke IsPermissionGranted / CallEnsurePermission.

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `PermissionReadStorage`

Helper read-only: izin baca storage. Colok ke IsPermissionGranted / CallEnsurePermission.

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `PermissionReadVideo`

Helper read-only: izin baca video (Android 13+). Colok ke IsPermissionGranted / CallEnsurePermission.

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `PermissionWriteStorage`

Helper read-only: izin tulis storage. Colok ke IsPermissionGranted / CallEnsurePermission.

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `TypeApk`

Pembanding read-only kategori: apk.

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `TypeArchive`

Pembanding read-only kategori: archive.

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `TypeAudio`

Pembanding read-only kategori: audio.

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `TypeDocument`

Pembanding read-only kategori: document.

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `TypeImage`

Pembanding read-only kategori: image.

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `TypeText`

Pembanding read-only kategori: text.

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `TypeUnknown`

Pembanding read-only kategori: unknown.

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `TypeVideo`

Pembanding read-only kategori: video.

- **Tipe:** teks
- **Akses:** baca saja (getter)

### Event (Tools_File)

#### `AfterFilePicked`

Hasil OpenFilePicker: success pertama, uri, path absolut, fileName paling belakang. success=false = dibatalkan user.

- **Parameter:** `success` (boolean), `uri` (teks), `path` (teks), `fileName` (teks)

#### `AfterOpenFile`

Hasil OpenFile: success + pesan.

- **Parameter:** `success` (boolean), `message` (teks)

#### `Base64FileSaved`

Hasil CallSaveBase64ToFile: path + success + message + needPermission.

- **Parameter:** `path` (teks), `success` (boolean), `message` (teks), `needPermission` (teks)

#### `CopyOrMoveFinished`

Salin/pindah selesai: success + path + nama file + isDuplicated (true = tujuan sudah ada, tidak ditimpa, rename manual).

- **Parameter:** `success` (boolean), `path` (teks), `fileName` (teks), `isDuplicated` (boolean)

#### `CopyOrMoveProgressChanged`

Progres salin/pindah: persen 0-100 + byte total & berjalan.

- **Parameter:** `progress` (angka), `byteTotal` (angka), `byteProgress` (angka)

#### `CopyOrMoveStarted`

Salin/pindah dimulai: path sumber + tujuan + nama file + isMoved.

- **Parameter:** `fromPath` (teks), `toPath` (teks), `fileName` (teks), `isMoved` (boolean)

#### `DirectoryCreated`

Hasil CallMakeDirectory: path + success + message + needPermission.

- **Parameter:** `path` (teks), `success` (boolean), `message` (teks), `needPermission` (teks)

#### `DirectoryHidden`

Hasil CallSetHiddenDirectory: path + success + message + needPermission (isi = izin yang harus diminta bila gagal karena izin).

- **Parameter:** `path` (teks), `success` (boolean), `message` (teks), `needPermission` (teks)

#### `EmptyFileCreated`

Hasil CallCreateEmptyFile: path + success + message + needPermission.

- **Parameter:** `path` (teks), `success` (boolean), `message` (teks), `needPermission` (teks)

#### `ErrorOccurred`

SATU-SATUNYA error Tools_File. component = nama operasi (bandingkan dengan blok Op...). id selalu 0 (tanpa id).

- **Parameter:** `component` (teks), `id` (angka), `message` (teks)

#### `FileDeleted`

Hasil CallDeleteFile: path + success + message + needPermission.

- **Parameter:** `path` (teks), `success` (boolean), `message` (teks), `needPermission` (teks)

#### `FileListDeleted`

Hasil CallDeleteList: success + total + terhapus + message + needPermission.

- **Parameter:** `success` (boolean), `total` (angka), `deleted` (angka), `message` (teks), `needPermission` (teks)

#### `FileRenamed`

Hasil CallRenameFile: path (nama baru) + success + message + needPermission.

- **Parameter:** `path` (teks), `success` (boolean), `message` (teks), `needPermission` (teks)

#### `FileSaved`

Hasil CallSaveFile: path + success + message + needPermission.

- **Parameter:** `path` (teks), `success` (boolean), `message` (teks), `needPermission` (teks)

#### `FolderRenamed`

Hasil CallRenameFolder: path (nama baru) + success + message + needPermission.

- **Parameter:** `path` (teks), `success` (boolean), `message` (teks), `needPermission` (teks)

#### `GotPermission`

Hasil GetPermission: success + izin + pesan. Pasang need_permission dari event lain ke GetPermission.

- **Parameter:** `success` (boolean), `permission` (teks), `message` (teks)

#### `GotText`

Hasil CallReadText: success pertama, path absolut, fileName paling belakang, content = isi file.

- **Parameter:** `success` (boolean), `path` (teks), `fileName` (teks), `content` (teks)

#### `NomediaFileCreated`

Hasil CallMakeNomediaFile: path + success + message + needPermission.

- **Parameter:** `path` (teks), `success` (boolean), `message` (teks), `needPermission` (teks)

#### `NomediaFileDeleted`

Hasil CallDeleteNomediaFile: path + success + message + needPermission.

- **Parameter:** `path` (teks), `success` (boolean), `message` (teks), `needPermission` (teks)

#### `ReadStarted`

Bacaan dimulai (CallReadText): persiapan (tampilkan loading).

- **Parameter:** `path` (teks)

## Tools_Image

> Tools_Image: tampilkan BANYAK gambar ber-id ke layout (path/URL/base64 mandiri) + gambar dari base64 + info ukuran. Id dikelola CreateImageId / Tools_Base. Sukses tampil tanpa event (langsung kelihatan); gagal via ErrorOccurred. List id URUT PEMBUATAN. (v17 2026-11-17: tanpa perubahan fungsi; penomoran versi.)

Versi anotasi: **17**. Jumlah blok: 35 fungsi, 3 properti, 10 event.

### Fungsi (Tools_Image)

#### `CallBase64ToPicture`

CALL: decode base64 jadi gambar di cache. Path hasil langsung bisa dipasang ke Image.Picture. Hasil lewat AfterPictureBase64Ready. Satu proses per saat.

- **Input:** `base64` (teks)
- **Return:** void

#### `CallCreateImageSlider`

CALL: bikin slider gambar ber-id (geser kanan-kiri bila horizontal=true, atas-bawah bila false — lebih dari Iago/Boson yang cuma kiri-kanan). Isi via SetSliderImages. Geser menembak SliderChanged; baca kapan saja via GetSliderIndex.

- **Input:** `container` (component), `id` (angka), `horizontal` (boolean)
- **Return:** void

#### `CallShowImage`

CALL: tampilkan gambar ber-id ke layout (Arrangement MIT / dinamis). Bisa jalan bareng (satu id = satu gambar). source kosong = pakai path (otomatis di-root /storage/emulated/0). source bisa: path file, file:/// absolut, http(s) URL (di-cache otomatis), atau base64 (mentah / data:...). id = penanda (minta ke CreateImageId / Tools_Base CreateUniqueId, reservasi otomatis diadopsi). Sukses menembak ImageShown; gagal via ErrorOccurred.

- **Input:** `container` (component), `source` (teks), `path` (teks), `id` (angka)
- **Return:** void

#### `CreateImageId`

Buat id gambar unik baru (Number, siap pakai ke CallShowImage).

- **Input:** tidak ada
- **Return:** angka

#### `CreateSliderId`

Buat id slider unik baru (Number, siap pakai ke CallCreateImageSlider).

- **Input:** tidak ada
- **Return:** angka

#### `CurrentImageId`

Id gambar yang terakhir dibuat/di-start. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `CurrentSliderId`

Id slider terakhir dibuat. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetActiveImageIds`

Id gambar yang sedang tampil/loading (URUT PEMBUATAN).

- **Input:** tidak ada
- **Return:** list

#### `GetActiveSliderIds`

Id slider yang aktif (URUT PEMBUATAN).

- **Input:** tidak ada
- **Return:** list

#### `GetFirstImageId`

Id index 1 = PERTAMA DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetFirstSliderId`

Id index 1 = PERTAMA DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetImageIds`

Semua id gambar yang tampil (URUT PEMBUATAN, index 1 = pertama dibuat).

- **Input:** tidak ada
- **Return:** list

#### `GetImageSize`

Lebar dan tinggi gambar (pixel). Kembali: list [lebar, tinggi]. [0, 0] bila bukan gambar / tidak ada.

- **Input:** `path` (teks)
- **Return:** list

#### `GetLastImageId`

Id TERAKHIR DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetLastPicturePath`

Fallback polling untuk event AfterPictureBase64Ready: path gambar terakhir yang jadi (kosong bila belum ada).

- **Input:** tidak ada
- **Return:** teks

#### `GetLastSliderId`

Id TERAKHIR DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetPreviousImageId`

Id yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.

- **Input:** `id` (angka)
- **Return:** angka

#### `GetPreviousSliderId`

Id yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.

- **Input:** `id` (angka)
- **Return:** angka

#### `GetSliderCount`

Jumlah halaman slider ber-id.

- **Input:** `id` (angka)
- **Return:** angka

#### `GetSliderIds`

Semua id slider (URUT PEMBUATAN, index 1 = pertama dibuat).

- **Input:** tidak ada
- **Return:** list

#### `GetSliderIndex`

Fallback polling untuk event SliderChanged: index halaman aktif (0 = pertama).

- **Input:** `id` (angka)
- **Return:** angka

#### `GetZoom`

Fallback polling untuk event ZoomChanged: skala zoom gambar ber-id (1.0 = pas).

- **Input:** `id` (angka)
- **Return:** angka

#### `IsImageIdActive`

True bila id gambar ini sedang tampil/loading.

- **Input:** `id` (angka)
- **Return:** boolean

#### `IsSliderIdActive`

True bila id slider ini masih ada.

- **Input:** `id` (angka)
- **Return:** boolean

#### `LastError`

Fallback polling untuk event ErrorOccurred: pesan error terakhir operasi ini (Image/Base64/Slider, kosong bila belum ada).

- **Input:** `operation` (teks)
- **Return:** teks

#### `RemoveImageId`

Hapus id gambar: lepas gambar dari layout + lepas id (otomatis beres-beres).

- **Input:** `id` (angka)
- **Return:** void

#### `RemoveSliderId`

Hapus slider: lepas dari layout + lepas id (otomatis beres-beres, autoplay mati).

- **Input:** `id` (angka)
- **Return:** void

#### `SetClearImage`

SET: hapus SEMUA gambar Tools_Image dari layout itu (komponen lain tidak diusik, id ikut dilepas). Tanpa feedback.

- **Input:** `container` (component)
- **Return:** void

#### `SetResetZoom`

SET: kembalikan zoom gambar ber-id ke pas (1.0). Tanpa feedback.

- **Input:** `id` (angka)
- **Return:** void

#### `SetSliderAutoPlay`

SET: Auto-putar slider tiap intervalMs (0 = mati).

- **Input:** `id` (angka), `intervalMs` (angka)
- **Return:** void

#### `SetSliderGoTo`

SET: lompat ke halaman index (0 = pertama). Tanpa feedback.

- **Input:** `id` (angka), `index` (angka)
- **Return:** void

#### `SetSliderImages`

SET: isi slider ber-id dengan daftar gambar (list teks: path file/asset, URL http, atau base64 — sama fleksibelnya dengan ShowImage; path dipakai bila item kosong). Maks 20 gambar. Gagal via ErrorOccurred.

- **Input:** `sliderId` (angka), `paths` (any)
- **Return:** void

#### `SetSliderNext`

SET: geser slider ke halaman berikut (berputar). Tanpa feedback.

- **Input:** `id` (angka)
- **Return:** void

#### `SetSliderPrevious`

SET: geser slider ke halaman sebelum (berputar). Tanpa feedback.

- **Input:** `id` (angka)
- **Return:** void

#### `SetZoomEnabled`

SET: Aktif/matikan cubit-zoom (pinch) + geser gambar ber-id. Aktif memakai mode MATRIX (klik tetap jalan untuk tap tanpa zoom). Perubahan skala menembak ZoomChanged; baca kapan saja via GetZoom.

- **Input:** `id` (angka), `enabled` (boolean)
- **Return:** void

### Properti (Tools_Image)

#### `OpBase64`

Nama operasi Base64 (untuk if di ErrorOccurred).

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `OpImage`

Nama operasi Image (untuk if di ErrorOccurred).

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `OpSlider`

Nama operasi Slider (untuk if di ErrorOccurred).

- **Tipe:** teks
- **Akses:** baca saja (getter)

### Event (Tools_Image)

#### `AfterPictureBase64Ready`

Hasil CallBase64ToPicture: success pertama, path absolut (cache), fileName paling belakang. Path siap ke Image.Picture.

- **Parameter:** `success` (boolean), `path` (teks), `fileName` (teks), `message` (teks)

#### `ErrorOccurred`

SATU-SATUNYA error Tools_Image. component = nama operasi (bandingkan dengan blok Op...).

- **Parameter:** `component` (teks), `id` (angka), `message` (teks)

#### `ImageClicked`

Gambar ber-id di-klik: id.

- **Parameter:** `id` (angka)

#### `ImageLongClicked`

Gambar ber-id di-long-klik: id.

- **Parameter:** `id` (angka)

#### `ImageShown`

Hasil CallShowImage: gambar ber-id tampil. id + path tampil.

- **Parameter:** `id` (angka), `path` (teks)

#### `PictureStarted`

Proses base64-gambar dimulai.

- **Parameter:** tidak ada

#### `SliderChanged`

Halaman slider pindah: id slider + index (0 = pertama). Fallback: GetSliderIndex.

- **Parameter:** `sliderId` (angka), `index` (angka)

#### `SliderClicked`

Halaman slider di-klik: id slider + index.

- **Parameter:** `sliderId` (angka), `index` (angka)

#### `SliderCreated`

Slider jadi: id + arah (true = kanan-kiri).

- **Parameter:** `id` (angka), `horizontal` (boolean)

#### `ZoomChanged`

Skala zoom gambar berubah (akhir gesture): id + skala (1.0 = pas). Fallback: GetZoom.

- **Parameter:** `id` (angka), `scale` (angka)

## Tools_Layout

> Tools_Layout: bikin layout dinamis ber-id (Vertical, Horizontal, VerticalScroll, HorizontalScroll) di dalam layout yang sudah ada atau buatan Tools_Core/Tools_Layout. Id dikelola CreateLayoutId. List id URUT PEMBUATAN. (v17 2026-11-17: tanpa perubahan fungsi; penomoran versi.)

Versi anotasi: **17**. Jumlah blok: 17 fungsi, 5 properti, 2 event.

### Fungsi (Tools_Layout)

#### `CallCreateHorizontalLayout`

CALL: Bikin HorizontalArrangement dinamis ber-id di dalam layout induk.

- **Input:** `container` (component), `id` (angka)
- **Return:** void

#### `CallCreateHorizontalScrollLayout`

CALL: Bikin HorizontalScrollArrangement dinamis ber-id (bisa scroll kiri-kanan) di dalam layout induk.

- **Input:** `container` (component), `id` (angka)
- **Return:** void

#### `CallCreateVerticalLayout`

CALL: Bikin VerticalArrangement dinamis ber-id di dalam layout induk. Sukses menembak LayoutCreated; gagal via ErrorOccurred.

- **Input:** `container` (component), `id` (angka)
- **Return:** void

#### `CallCreateVerticalScrollLayout`

CALL: Bikin VerticalScrollArrangement dinamis ber-id (bisa scroll atas-bawah) di dalam layout induk.

- **Input:** `container` (component), `id` (angka)
- **Return:** void

#### `CreateLayoutId`

Buat id layout unik baru (Number, siap pakai).

- **Input:** tidak ada
- **Return:** angka

#### `CurrentLayoutId`

Id layout terakhir dibuat. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetActiveLayoutIds`

Id layout yang aktif (URUT PEMBUATAN).

- **Input:** tidak ada
- **Return:** list

#### `GetFirstLayoutId`

Id index 1 = PERTAMA DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetLastLayoutId`

Id TERAKHIR DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetLayoutComponent`

Ambil layout dinamis ber-id (untuk dipakai sebagai wadah blok lain). null bila tidak ada.

- **Input:** `id` (angka)
- **Return:** any

#### `GetLayoutIds`

Semua id layout (URUT PEMBUATAN, index 1 = pertama dibuat).

- **Input:** tidak ada
- **Return:** list

#### `GetLayoutType`

Tipe layout ber-id ("VerticalArrangement" dkk, sama dengan typeName di LayoutCreated). Bandingkan dengan blok TypeVertical/TypeHorizontal/TypeVerticalScroll/TypeHorizontalScroll. Kosong bila id tidak ada.

- **Input:** `id` (angka)
- **Return:** teks

#### `GetPreviousLayoutId`

Id yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.

- **Input:** `id` (angka)
- **Return:** angka

#### `IsLayoutIdActive`

True bila id layout ini masih ada.

- **Input:** `id` (angka)
- **Return:** boolean

#### `LastError`

Fallback polling untuk event ErrorOccurred: pesan error terakhir operasi ini (kosong bila belum ada).

- **Input:** `operation` (teks)
- **Return:** teks

#### `RemoveLayoutId`

Hapus layout dinamis: lepas dari induk + lepas id (isi ikut hilang dari layar).

- **Input:** `id` (angka)
- **Return:** void

#### `SetClearLayouts`

SET: Hapus SEMUA layout dinamis Tools_Layout (id ikut dilepas).

- **Input:** tidak ada
- **Return:** void

### Properti (Tools_Layout)

#### `OpLayout`

Nama operasi Layout (untuk if di ErrorOccurred).

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `TypeHorizontal`

Pembanding read-only: tipe HorizontalArrangement.

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `TypeHorizontalScroll`

Pembanding read-only: tipe HorizontalScrollArrangement.

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `TypeVertical`

Pembanding read-only: tipe VerticalArrangement.

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `TypeVerticalScroll`

Pembanding read-only: tipe VerticalScrollArrangement.

- **Tipe:** teks
- **Akses:** baca saja (getter)

### Event (Tools_Layout)

#### `ErrorOccurred`

SATU-SATUNYA error Tools_Layout. component = nama operasi (bandingkan dengan blok OpLayout).

- **Parameter:** `component` (teks), `id` (angka), `message` (teks)

#### `LayoutCreated`

Layout dinamis jadi: id + nama tipe.

- **Parameter:** `id` (angka), `typeName` (teks)

## Tools_Sheet

> Tools_Sheet: bottom-sheet dialog ber-id (pakai Arrangement sebagai konten), toast, dan notifikasi bar status Android. Sheet: RegisterSheet -> ShowSheet/DismissSheet + event SheetHidden, animasi geser otomatis. Notifikasi: channel otomatis, butuh izin POST_NOTIFICATIONS di Android 13+ (pakai RequestNotificationPermission atau izinkan manual). Id dikelola CreateSheetId. List id URUT PEMBUATAN. (v17 2026-11-17: tanpa perubahan fungsi; penomoran versi.)

Versi anotasi: **17**. Jumlah blok: 20 fungsi, 9 properti, 3 event.

### Fungsi (Tools_Sheet)

#### `CallDismissSheet`

CALL: Tutup sheet ber-id (animasi keluar). Hasil lewat SheetHidden (registrasi tetap, bisa CallShowSheet lagi).

- **Input:** `id` (angka)
- **Return:** void

#### `CallShowSheet`

CALL: Tampilkan sheet ber-id (animasi geser naik otomatis). Hasil lewat SheetShown.

- **Input:** `id` (angka)
- **Return:** void

#### `CreateSheetId`

Buat id sheet unik baru (Number, siap pakai ke RegisterSheet).

- **Input:** tidak ada
- **Return:** angka

#### `CurrentSheetId`

Id sheet terakhir dibuat. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetActiveSheetIds`

Id sheet yang aktif (URUT PEMBUATAN).

- **Input:** tidak ada
- **Return:** list

#### `GetFirstSheetId`

Id index 1 = PERTAMA DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetLastSheetId`

Id TERAKHIR DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetPreviousSheetId`

Id yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.

- **Input:** `id` (angka)
- **Return:** angka

#### `GetSheetIds`

Semua id sheet (URUT PEMBUATAN, index 1 = pertama dibuat).

- **Input:** tidak ada
- **Return:** list

#### `IsSheetIdActive`

True bila id sheet ini masih terdaftar.

- **Input:** `id` (angka)
- **Return:** boolean

#### `IsSheetShowing`

True bila sheet ber-id sedang tampil.

- **Input:** `id` (angka)
- **Return:** boolean

#### `LastError`

Fallback polling untuk event ErrorOccurred: pesan error terakhir operasi ini (kosong bila belum ada).

- **Input:** `operation` (teks)
- **Return:** teks

#### `RegisterSheet`

Daftarkan bottom-sheet ber-id. content = Arrangement (dipindah ke dialog saat tampil, dikembalikan saat tutup). gravity angka: 80=bawah, 48=atas, 17=tengah, 3=kiri, 5=kanan. widthDp/heightDp <= 0 = menyesuaikan (lebar penuh bila gravity atas/bawah). dimPercent 0-100 (kegelapan latar). animationType teks: pakai properti SlideFromBottom/SlideFromTop/SlideFromLeft/SlideFromRight/FadeAnim/NoAnim. animationDurationMs 0 = 250. cancelable = bisa tutup via tombol back/luar. swipeToDismiss = geser ke arah tutup untuk menutup. Sukses tanpa event; gagal via ErrorOccurred.

- **Input:** `id` (angka), `content` (component), `gravity` (angka), `widthDp` (angka), `heightDp` (angka), `dimPercent` (angka), `animationType` (teks), `animationDurationMs` (angka), `cancelable` (boolean), `swipeToDismiss` (boolean)
- **Return:** void

#### `RemoveSheetId`

Hapus sheet ber-id: tutup diam-diam (tanpa SheetHidden), kembalikan konten ke layout asal, lepas id.

- **Input:** `id` (angka)
- **Return:** void

#### `SetCancelAllNotifications`

SET: Hapus SEMUA notifikasi Tools_Sheet.

- **Input:** tidak ada
- **Return:** void

#### `SetCancelNotification`

SET: Hapus notifikasi ber-id.

- **Input:** `id` (angka)
- **Return:** void

#### `SetClearSheets`

SET: Hapus SEMUA sheet Tools_Sheet (tutup diam-diam, konten dikembalikan, id dilepas).

- **Input:** tidak ada
- **Return:** void

#### `SetRequestNotificationPermission`

SET: Minta izin notifikasi (Android 13+). Best-effort: bila ditolak, izinkan manual di Setelan aplikasi.

- **Input:** tidak ada
- **Return:** void

#### `SetShowNotification`

SET: Tampilkan notifikasi bar status: id (angka unik, mis. 1), judul, isi. Tap notifikasi membuka aplikasi. Gagal (mis. izin belum ada) via ErrorOccurred.

- **Input:** `id` (angka), `title` (teks), `text` (teks)
- **Return:** void

#### `SetShowToast`

SET: Tampilkan toast (pesan singkat bawah layar). longDuration = tampil lebih lama. Gagal via ErrorOccurred.

- **Input:** `message` (teks), `longDuration` (boolean)
- **Return:** void

### Properti (Tools_Sheet)

#### `FadeAnim`

Tipe animasi: fade.

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `NoAnim`

Tipe animasi: tanpa animasi.

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `OpNotification`

Nama operasi Notification (untuk if di ErrorOccurred).

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `OpSheet`

Nama operasi Sheet (untuk if di ErrorOccurred).

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `OpToast`

Nama operasi Toast (untuk if di ErrorOccurred).

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `SlideFromBottom`

Tipe animasi: geser dari bawah.

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `SlideFromLeft`

Tipe animasi: geser dari kiri.

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `SlideFromRight`

Tipe animasi: geser dari kanan.

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `SlideFromTop`

Tipe animasi: geser dari atas.

- **Tipe:** teks
- **Akses:** baca saja (getter)

### Event (Tools_Sheet)

#### `ErrorOccurred`

SATU-SATUNYA error Tools_Sheet (Sheet, Toast, Notification). component = nama operasi (bandingkan dengan blok OpSheet/OpToast/OpNotification).

- **Parameter:** `component` (teks), `id` (angka), `message` (teks)

#### `SheetHidden`

Sheet ber-id tertutup (back/luar/Dismiss): id. Registrasi tetap, tampilkan lagi via CallShowSheet.

- **Parameter:** `id` (angka)

#### `SheetShown`

Sheet ber-id tampil (hasil CallShowSheet): id.

- **Parameter:** `id` (angka)

## Tools_Style

> Tools_Style: percantik komponen/wadah yang kelihatan (Button, Label, Arrangement, gambar Tools_Image, komponen dinamis, dsb). Radius, border, gradien, elevation, padding, margin, opacity, background warna/gambar, plus animasi fade/slide/zoom. Tanpa id (stateless, langsung ke komponen). Semua prosedur void; gagal via ErrorOccurred. Catatan: background bawaan komponen (mis. Button) diganti drawable baru. (v17 2026-11-17: tanpa perubahan fungsi; penomoran versi.)

Versi anotasi: **17**. Jumlah blok: 25 fungsi, 8 properti, 3 event.

### Fungsi (Tools_Style)

#### `CallAnimateFade`

CALL: Animasi fade: startPercent ke endPercent (0-100). Mis. 0->100 muncul, 100->0 hilang. Menembak AnimationStarted/AnimationEnded.

- **Input:** `comp` (component), `durationMs` (angka), `startPercent` (angka), `endPercent` (angka)
- **Return:** void

#### `CallAnimateRotation`

CALL: Animasi putar (derajat, searah jarum jam). durationMs mis. 300.

- **Input:** `comp` (component), `durationMs` (angka), `degrees` (angka)
- **Return:** void

#### `CallAnimateScale`

CALL: Animasi skala: ke toXPercent/toYPercent (100 = normal). durationMs mis. 300.

- **Input:** `comp` (component), `durationMs` (angka), `toXPercent` (angka), `toYPercent` (angka)
- **Return:** void

#### `CallAnimateSlideIn`

CALL: Animasi geser-masuk: direction = left/right/top/bottom. durationMs mis. 300.

- **Input:** `comp` (component), `direction` (teks), `durationMs` (angka)
- **Return:** void

#### `IsAnimationRunning`

Fallback polling untuk event animasi: true bila komponen sedang dianimasikan Tools_Style.

- **Input:** `comp` (component)
- **Return:** boolean

#### `LastError`

Fallback polling untuk event ErrorOccurred: pesan error terakhir operasi ini (kosong bila belum ada).

- **Input:** `operation` (teks)
- **Return:** teks

#### `SetBackgroundColor`

SET: Atur warna background polos. Background bawaan diganti.

- **Input:** `comp` (component), `color` (angka)
- **Return:** void

#### `SetBackgroundImage`

SET: Atur gambar background (file/asset, path di-root /storage/emulated/0). Cocok untuk wadah.

- **Input:** `comp` (component), `path` (teks)
- **Return:** void

#### `SetBorder`

SET: Atur garis tepi: warna + tebal (dp). Background bawaan diganti.

- **Input:** `comp` (component), `color` (angka), `widthDp` (angka)
- **Return:** void

#### `SetCircular`

SET: Jadikan lingkaran/oval (cocok untuk foto profil). Untuk lingkaran sempurna pakai komponen persegi.

- **Input:** `comp` (component)
- **Return:** void

#### `SetCircularBorder`

SET: Lingkaran + garis tepi warna (cocok untuk foto profil).

- **Input:** `comp` (component), `color` (angka), `widthDp` (angka)
- **Return:** void

#### `SetCornerRadius`

SET: Atur sudut membulat (radius, dp). Background bawaan diganti.

- **Input:** `comp` (component), `radiusDp` (angka)
- **Return:** void

#### `SetElevation`

SET: Atur bayangan/elevasi (dp). Efek terlihat di Android 5.0+.

- **Input:** `comp` (component), `dp` (angka)
- **Return:** void

#### `SetGradient`

SET: Atur gradien banyak warna (list warna) + arah angka: 0=atas-bawah, 1=kananATAS-kiriBAWAH, 2=kanan-kiri, 3=kananBAWAH-kiriATAS, 4=bawah-atas, 5=kiriBAWAH-kananATAS, 6=kiri-kanan, 7=kiriATAS-kananBAWAH. Background bawaan diganti.

- **Input:** `comp` (component), `colors` (any), `orientation` (angka)
- **Return:** void

#### `SetMargin`

SET: Atur jarak luar/margin (dp): kiri, atas, kanan, bawah. Hanya bila induk mendukung margin.

- **Input:** `comp` (component), `leftDp` (angka), `topDp` (angka), `rightDp` (angka), `bottomDp` (angka)
- **Return:** void

#### `SetOpacity`

SET: Atur transparansi 0-100 (100 = penuh).

- **Input:** `comp` (component), `percent` (angka)
- **Return:** void

#### `SetPadding`

SET: Atur jarak dalam/padding (dp): kiri, atas, kanan, bawah.

- **Input:** `comp` (component), `leftDp` (angka), `topDp` (angka), `rightDp` (angka), `bottomDp` (angka)
- **Return:** void

#### `SetRadialGradient`

SET: Atur gradien radial: warna tengah, warna tepi, radius (dp). Background bawaan diganti.

- **Input:** `comp` (component), `centerColor` (angka), `edgeColor` (angka), `radiusDp` (angka)
- **Return:** void

#### `SetResetEffects`

SET: Kembalikan efek sementara: hapus animasi, alpha/rotasi/skala/posisi ke normal. Background tidak diubah.

- **Input:** `comp` (component)
- **Return:** void

#### `SetRipple`

SET: Efek ripple (gelombang sentuh) warna tertentu. Background bawaan diganti RippleDrawable.

- **Input:** `comp` (component), `color` (angka)
- **Return:** void

#### `SetRotation`

SET: Atur putaran komponen (derajat, searah jarum jam).

- **Input:** `comp` (component), `degrees` (angka)
- **Return:** void

#### `SetShape`

SET: Atur bentuk background angka: pakai properti ShapeRectangle/ShapeOval/ShapeLine/ShapeRing.

- **Input:** `comp` (component), `shape` (angka)
- **Return:** void

#### `SetStroke`

SET: Garis tepi putus-putus: warna, tebal (dp), panjang garis (dp), jarak garis (dp). dashed=false = garis penuh.

- **Input:** `comp` (component), `dashed` (boolean), `widthDp` (angka), `color` (angka), `dashLengthDp` (angka), `dashGapDp` (angka)
- **Return:** void

#### `SetTextSpace`

SET: Atur spasi teks (khusus Label/TextBox/Button): jarak baris (dp) + jarak huruf (em). Selain teks: error.

- **Input:** `comp` (component), `lineSpaceDp` (angka), `letterSpace` (angka)
- **Return:** void

#### `SetVisibility`

SET: Atur visibilitas angka: pakai properti VisibilityVisible/VisibilityInvisible/VisibilityGone.

- **Input:** `comp` (component), `visibility` (angka)
- **Return:** void

### Properti (Tools_Style)

#### `OpStyle`

Nama operasi Style (untuk if di ErrorOccurred).

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `ShapeLine`

Bentuk: garis.

- **Tipe:** angka
- **Akses:** baca saja (getter)

#### `ShapeOval`

Bentuk: oval/lingkaran.

- **Tipe:** angka
- **Akses:** baca saja (getter)

#### `ShapeRectangle`

Bentuk: persegi.

- **Tipe:** angka
- **Akses:** baca saja (getter)

#### `ShapeRing`

Bentuk: cincin.

- **Tipe:** angka
- **Akses:** baca saja (getter)

#### `VisibilityGone`

Visibilitas: hilang (tempat ikut hilang).

- **Tipe:** angka
- **Akses:** baca saja (getter)

#### `VisibilityInvisible`

Visibilitas: tak terlihat (tempat tetap ada).

- **Tipe:** angka
- **Akses:** baca saja (getter)

#### `VisibilityVisible`

Visibilitas: terlihat.

- **Tipe:** angka
- **Akses:** baca saja (getter)

### Event (Tools_Style)

#### `AnimationEnded`

Animasi Tools_Style selesai: komponen + tipe. Fallback: IsAnimationRunning.

- **Parameter:** `comp` (component), `animationType` (teks)

#### `AnimationStarted`

Animasi Tools_Style mulai: komponen + tipe (fade/slide/scale/rotation). Fallback: IsAnimationRunning.

- **Parameter:** `comp` (component), `animationType` (teks)

#### `ErrorOccurred`

SATU-SATUNYA error Tools_Style. component = nama operasi (bandingkan dengan blok OpStyle). Id selalu 0 (stateless).

- **Parameter:** `component` (teks), `id` (angka), `message` (teks)

## Tools_Task

> Tools_Task: penjadwal full-stack ber-id. Tugas dalam-aplikasi (sekali/berulang/terjadwal) + alarm sistem via AlarmManager (buka aplikasi saat bunyi) + notifikasi ongoing + wake-lock + variabel/extra per tugas + persistensi alarm (RestoreAlarms). Id dikelola CreateTaskId / Tools_Base. Tanpa Service/receiver khusus (aman manifest, lolos Play). Setiap event ada fallback polling (GetTaskStatus/Result/Progress, GetAlarmInfo, CheckStartupAlarm). List id URUT PEMBUATAN. (v17 2026-11-17: tanpa perubahan fungsi; penomoran versi.)

Versi anotasi: **17**. Jumlah blok: 50 fungsi, 4 properti, 6 event.

### Fungsi (Tools_Task)

#### `CallCancelAllTasks`

CALL: Batalkan SEMUA tugas dalam-aplikasi (alarm sistem tidak ikut; pakai CallCancelAllAlarms bila perlu). Tiap tugas yang jalan menembak TaskCancelled.

- **Input:** tidak ada
- **Return:** void

#### `CallCancelTask`

CALL: Batalkan tugas ber-id (jadwal dalam-aplikasi saja). Hasil lewat TaskCancelled.

- **Input:** `id` (angka)
- **Return:** void

#### `CallCheckStartupAlarm`

CALL: WAJIB dipanggil di Screen.Initialize (dan timer bila perlu): cek apakah aplikasi dibuka oleh alarm Tools_Task. Tanpa return: bila dibuka oleh alarm dan datanya baru, menembak AlarmFired.

- **Input:** tidak ada
- **Return:** void

#### `CallRestoreAlarms`

CALL: Pasang ulang SEMUA alarm tersimpan (mis. habis reboot: panggil di Screen.Initialize). Tanpa return.

- **Input:** tidak ada
- **Return:** void

#### `CallScheduleAt`

CALL: Jadwalkan tugas pada jam epoch mili-detik (lihat NowMillis). Lewat = langsung jalan.

- **Input:** `id` (angka), `epochMillis` (angka), `payload` (teks)
- **Return:** void

#### `CallScheduleOnce`

CALL: Jadwalkan tugas sekali jalan: id + jeda mili-detik + payload teks (JSON via Tools_Base bila perlu). Menembak TaskStarted lalu TaskFinished. Sukses menembak Started; gagal via ErrorOccurred.

- **Input:** `id` (angka), `delayMillis` (angka), `payload` (teks)
- **Return:** void

#### `CallScheduleRepeating`

CALL: Jadwalkan tugas berulang: id + jeda awal + interval + jumlah ulang (mis. 5) atau -1 = selamanya + payload. Tiap tick menembak TaskTick; selesai menembak TaskFinished.

- **Input:** `id` (angka), `initialDelayMillis` (angka), `intervalMillis` (angka), `repeatCount` (angka), `payload` (teks)
- **Return:** void

#### `CallSetAlarm`

CALL: Pasang alarm SISTEM sekali bunyi: id + epoch mili-detik + payload + exact (true = tepat waktu bila izin ada). Bunyi = membuka aplikasi; baca via CheckStartupAlarm/AlarmFired. Tersimpan otomatis (survive reboot via RestoreAlarms).

- **Input:** `id` (angka), `epochMillis` (angka), `payload` (teks), `exact` (boolean)
- **Return:** void

#### `CallSetRepeatingAlarm`

CALL: Pasang alarm SISTEM berulang: id + epoch pertama + interval + payload. Interval <1 detik dibulatkan ke 1 detik (hemat baterai, aturan Android).

- **Input:** `id` (angka), `firstEpochMillis` (angka), `intervalMillis` (angka), `payload` (teks)
- **Return:** void

#### `ClearTaskExtras`

Hapus SEMUA extra tugas ber-id.

- **Input:** `id` (angka)
- **Return:** void

#### `ClearVars`

Hapus SEMUA variabel global.

- **Input:** tidak ada
- **Return:** void

#### `CreateTaskId`

Buat id tugas unik baru (Number, siap pakai ke CallSchedule*/CallSetAlarm). Urutan pembuatan menentukan urutan list.

- **Input:** tidak ada
- **Return:** angka

#### `CurrentTaskId`

Id tugas terakhir dibuat/di-start. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetActiveTaskIds`

Id tugas/alarm yang aktif (jalan/jeda/terpasang, URUT PEMBUATAN).

- **Input:** tidak ada
- **Return:** list

#### `GetAlarmInfo`

Fallback polling alarm: list [epochMillis, intervalMillis (0 = sekali), payload]. Kosong = tidak ada.

- **Input:** `id` (angka)
- **Return:** list

#### `GetAllVarNames`

Semua nama variabel global (URUT DIBUAT).

- **Input:** tidak ada
- **Return:** list

#### `GetFirstTaskId`

Id index 1 = PERTAMA DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetLastFiredPayload`

Payload tugas terakhir yang selesai/bunyi.

- **Input:** tidak ada
- **Return:** teks

#### `GetLastFiredTaskId`

Id tugas dalam-aplikasi terakhir yang selesai/bunyi. -1 bila belum ada.

- **Input:** tidak ada
- **Return:** angka

#### `GetLastFiredTime`

Waktu epoch tugas terakhir selesai/bunyi. 0 bila belum ada.

- **Input:** tidak ada
- **Return:** angka

#### `GetLastTaskId`

Id TERAKHIR DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetPendingAlarmIds`

Semua id alarm sistem yang terpasang (URUT PEMBUATAN).

- **Input:** tidak ada
- **Return:** list

#### `GetPendingTaskIds`

Alias GetActiveTaskIds: id yang masih pending.

- **Input:** tidak ada
- **Return:** list

#### `GetPreviousTaskId`

Id yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.

- **Input:** `id` (angka)
- **Return:** angka

#### `GetTaskExtra`

Baca extra tugas ber-id (default bila tidak ada).

- **Input:** `id` (angka), `key` (teks), `defaultValue` (teks)
- **Return:** teks

#### `GetTaskExtraKeys`

Semua kunci extra tugas ber-id.

- **Input:** `id` (angka)
- **Return:** list

#### `GetTaskIds`

Semua id tugas/alarm (URUT PEMBUATAN, index 1 = pertama dibuat).

- **Input:** tidak ada
- **Return:** list

#### `GetTaskProgress`

Fallback polling untuk TaskTick: list [tickCount, repeatCount, lastTickAt, nextFireAt].

- **Input:** `id` (angka)
- **Return:** list

#### `GetTaskResult`

Fallback polling untuk TaskFinished: list [success, payload, message]. Tetap ada sampai RemoveTaskId.

- **Input:** `id` (angka)
- **Return:** list

#### `GetTaskStatus`

Fallback polling untuk event Task*: status tugas ber-id. Salah satu: antri, jalan, jeda, selesai, batal, gagal, alarm, atau tidak ada.

- **Input:** `id` (angka)
- **Return:** teks

#### `GetVar`

Baca variabel global (default bila tidak ada).

- **Input:** `name` (teks), `defaultValue` (teks)
- **Return:** teks

#### `IsAlarmSet`

True bila alarm sistem ber-id terpasang.

- **Input:** `id` (angka)
- **Return:** boolean

#### `IsAwake`

True bila wake-lock sedang dipegang.

- **Input:** tidak ada
- **Return:** boolean

#### `IsExactAlarmAllowed`

True bila boleh pasang exact-alarm (Android <12 selalu true).

- **Input:** tidak ada
- **Return:** boolean

#### `IsTaskIdActive`

True bila id tugas ini aktif (jalan/jeda/alarm terpasang).

- **Input:** `id` (angka)
- **Return:** boolean

#### `LastError`

Fallback polling untuk event ErrorOccurred: pesan error terakhir operasi ini (kosong bila belum ada).

- **Input:** `operation` (teks)
- **Return:** teks

#### `NowMillis`

Waktu sekarang epoch mili-detik (untuk CallScheduleAt/SetAlarm).

- **Input:** tidak ada
- **Return:** angka

#### `RemoveTaskExtra`

Hapus satu extra tugas ber-id.

- **Input:** `id` (angka), `key` (teks)
- **Return:** void

#### `RemoveTaskId`

Hapus id tugas: batalkan jadwal + cabut alarm sistem + lepas id (otomatis beres-beres).

- **Input:** `id` (angka)
- **Return:** void

#### `RemoveVar`

Hapus satu variabel global.

- **Input:** `name` (teks)
- **Return:** void

#### `SetCancelAlarm`

SET: Cabut alarm sistem ber-id (tugas dalam-aplikasi dengan id sama tidak ikut; pakai CancelTask bila perlu).

- **Input:** `id` (angka)
- **Return:** void

#### `SetCancelAllAlarms`

SET: Cabut SEMUA alarm sistem Tools_Task.

- **Input:** tidak ada
- **Return:** void

#### `SetHideOngoingNotification`

SET: Hapus notifikasi ongoing ber-id.

- **Input:** `id` (angka)
- **Return:** void

#### `SetKeepAwake`

SET: Jaga CPU tetap hidup (wake-lock parsial) agar tugas tidak tidur. timeoutMillis 0 = sampai SetKeepAwake(false). Pakai seperlunya (boros baterai).

- **Input:** `on` (boolean), `timeoutMillis` (angka)
- **Return:** void

#### `SetPauseTask`

SET: Jeda tugas ber-id (berlaku untuk once/repeating yang belum selesai). Lanjut via SetResumeTask.

- **Input:** `id` (angka)
- **Return:** void

#### `SetRequestExactAlarmPermission`

SET: Minta izin exact-alarm (Android 12+). Best-effort: bila ditolak, alarm jalan inexact (tetap bunyi, bisa meleset).

- **Input:** tidak ada
- **Return:** void

#### `SetResumeTask`

SET: Lanjutkan tugas yang dijeda (SetPauseTask).

- **Input:** `id` (angka)
- **Return:** void

#### `SetShowOngoingNotification`

SET: Tampilkan notifikasi ongoing (tetap) agar tugas penting tidak dibunuh sistem. Tap membuka aplikasi.

- **Input:** `id` (angka), `title` (teks), `text` (teks)
- **Return:** void

#### `SetTaskExtra`

SET: Simpan extra teks untuk tugas ber-id (mis. url, nama file). Menimpa kunci yang sama. Tanpa feedback.

- **Input:** `id` (angka), `key` (teks), `value` (teks)
- **Return:** void

#### `SetVar`

SET: Simpan variabel teks global (bisa dibaca tugas mana pun + blok lain).

- **Input:** `name` (teks), `value` (teks)
- **Return:** void

### Properti (Tools_Task)

#### `OpAlarm`

Nama operasi Alarm (untuk if di ErrorOccurred).

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `OpNotification`

Nama operasi Notification (untuk if di ErrorOccurred).

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `OpTask`

Nama operasi Task (untuk if di ErrorOccurred).

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `OpVar`

Nama operasi Var (untuk if di ErrorOccurred).

- **Tipe:** teks
- **Akses:** baca saja (getter)

### Event (Tools_Task)

#### `AlarmFired`

Alarm sistem bunyi (aplikasi dibuka oleh alarm): id + payload. Selalu cek juga via CheckStartupAlarm di Initialize.

- **Parameter:** `id` (angka), `payload` (teks)

#### `ErrorOccurred`

SATU-SATUNYA error Tools_Task. component = nama operasi (bandingkan dengan blok OpTask/OpAlarm/OpVar/OpNotification).

- **Parameter:** `component` (teks), `id` (angka), `message` (teks)

#### `TaskCancelled`

Tugas dibatalkan: id.

- **Parameter:** `id` (angka)

#### `TaskFinished`

Tugas selesai: success + payload + message.

- **Parameter:** `id` (angka), `success` (boolean), `payload` (teks), `message` (teks)

#### `TaskStarted`

Tugas dalam-aplikasi dimulai: id + payload.

- **Parameter:** `id` (angka), `payload` (teks)

#### `TaskTick`

Tugas berulang tick ke-n: id + tick + payload.

- **Parameter:** `id` (angka), `tick` (angka), `payload` (teks)

## Tools_Video

> Tools_Video: putar BANYAK video ber-id di dalam layout (Arrangement MIT / dinamis), kontrol ala VLC (pause/resume/seek/maju/mundur/double-tap), loop, volume. Satu id = satu video, bisa jalan bareng. Id dikelola CreateVideoId / Tools_Base. List id URUT PEMBUATAN. (v17 2026-11-17: tanpa perubahan fungsi; penomoran versi.)

Versi anotasi: **17**. Jumlah blok: 21 fungsi, 7 properti, 4 event.

### Fungsi (Tools_Video)

#### `CallPlayVideoIn`

Putar video ber-id di dalam layout (Arrangement MIT / dinamis). Bisa jalan bareng (satu id = satu video). path file (/Download/..., absolut apa adanya), atau http(s) URL (streaming). id dari CreateVideoId / Tools_Base CreateUniqueId. Atur tinggi arrangement (mis. 300px) agar video kelihatan. Double-tap: kiri = mundur, kanan = maju, tengah = pause/resume. Sukses menembak VideoStarted; gagal via ErrorOccurred.

- **Input:** `container` (component), `path` (teks), `id` (angka)
- **Return:** void

#### `CreateVideoId`

Buat id video unik baru (Number, siap pakai ke CallPlayVideoIn).

- **Input:** tidak ada
- **Return:** angka

#### `CurrentVideoId`

Id video yang terakhir dibuat/di-start. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetActiveVideoIds`

Id video yang sedang main (URUT PEMBUATAN). Yang dijeda/berhenti TIDAK masuk sini.

- **Input:** tidak ada
- **Return:** list

#### `GetFirstVideoId`

Id index 1 = PERTAMA DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetLastVideoId`

Id TERAKHIR DIBUAT. -1 bila kosong.

- **Input:** tidak ada
- **Return:** angka

#### `GetPreviousVideoId`

Id yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.

- **Input:** `id` (angka)
- **Return:** angka

#### `GetVideoDuration`

Durasi video (ms). 0 bila belum siap. (per id)

- **Input:** `id` (angka)
- **Return:** angka

#### `GetVideoIds`

Semua id video yang dipegang (URUT PEMBUATAN, index 1 = pertama dibuat).

- **Input:** tidak ada
- **Return:** list

#### `GetVideoPosition`

Posisi video sekarang (ms). (per id)

- **Input:** `id` (angka)
- **Return:** angka

#### `IsVideoIdActive`

True bila id video ini sedang main.

- **Input:** `id` (angka)
- **Return:** boolean

#### `IsVideoPlaying`

True bila video id ini sedang main.

- **Input:** `id` (angka)
- **Return:** boolean

#### `LastError`

Fallback polling untuk event ErrorOccurred: pesan error terakhir operasi ini (kosong bila belum ada).

- **Input:** `operation` (teks)
- **Return:** teks

#### `RemoveVideoId`

Hapus id video: hentikan + lepas dari layout + lepas id (otomatis beres-beres).

- **Input:** `id` (angka)
- **Return:** void

#### `SetBackwardVideo`

SET: Mundur video sejauh SkipSeconds detik. (per id)

- **Input:** `id` (angka)
- **Return:** void

#### `SetClearVideo`

SET: Hapus SEMUA video Tools_Video dari layout itu (id ikut dilepas, komponen lain tidak diusik). Video ikut di-pause.

- **Input:** `container` (component)
- **Return:** void

#### `SetForwardVideo`

SET: Maju video sejauh SkipSeconds detik. (per id)

- **Input:** `id` (angka)
- **Return:** void

#### `SetPauseVideo`

SET: Jeda video yang sedang main. (per id)

- **Input:** `id` (angka)
- **Return:** void

#### `SetResumeVideo`

SET: Lanjutkan video yang dijeda. (per id)

- **Input:** `id` (angka)
- **Return:** void

#### `SetSeekVideo`

SET: Lompat video ke posisi (ms). Bisa dipasang ke Slider. (per id)

- **Input:** `id` (angka), `position` (angka)
- **Return:** void

#### `SetStopVideo`

SET: Hentikan video + lepas dari layout (per id). Id tetap tercatat (Play lagi langsung bisa). Bersih total via RemoveVideoId.

- **Input:** `id` (angka)
- **Return:** void

### Properti (Tools_Video)

#### `DoubleTapSeek`

Double-tap video: kiri = mundur, kanan = maju, tengah = pause/resume. Default true.

- **Tipe:** boolean
- **Akses:** read-write

#### `MediaVolume`

Volume video 0-100 (default 100).

- **Tipe:** angka
- **Akses:** read-write

#### `OpVideo`

Nama operasi Video (untuk if di ErrorOccurred).

- **Tipe:** teks
- **Akses:** baca saja (getter)

#### `ProgressInterval`

Jeda event VideoProgressChanged dalam milidetik (default 500).

- **Tipe:** angka
- **Akses:** read-write

#### `ShowVideoController`

Tampilkan kontrol bawaan video. Default true.

- **Tipe:** boolean
- **Akses:** read-write

#### `SkipSeconds`

Lompatan Maju/Mundur + double-tap dalam detik (default 10).

- **Tipe:** angka
- **Akses:** read-write

#### `VideoLooping`

Video mengulang otomatis bila selesai. Default false.

- **Tipe:** boolean
- **Akses:** read-write

### Event (Tools_Video)

#### `ErrorOccurred`

SATU-SATUNYA error Tools_Video.

- **Parameter:** `component` (teks), `id` (angka), `message` (teks)

#### `VideoFinished`

Video selesai (tidak loop). Putar lagi via CallPlayVideoIn.

- **Parameter:** `id` (angka)

#### `VideoProgressChanged`

Progres video berubah: id + posisi + durasi (ms) + persen 0-100.

- **Parameter:** `id` (angka), `position` (angka), `duration` (angka), `percent` (angka)

#### `VideoStarted`

Video mulai main: id + durasi (ms) diketahui.

- **Parameter:** `id` (angka), `duration` (angka)

---

*Dokumen ini digenerate otomatis dari `components.json` v17 — tiap blok di atas persis seperti di dalam `.aix`.*
