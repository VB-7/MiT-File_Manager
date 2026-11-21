// DIGENERATE OTOMATIS oleh tools/gen_facade.mjs — JANGAN EDIT MANUAL.
// Ubah 12 pemilik (Tools_Audio..Tools_Video), lalu jalan ulang --regen.
package com.fv7.tools;

import com.fv7.tools.helpers.Type;
import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.Options;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.AndroidViewComponent;
import com.google.appinventor.components.runtime.Component;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.util.YailList;

import java.util.List;

@DesignerComponent(
    version = 23,
    description = "Tools_All_in_One: SATU komponen berisi SEMUA blok "
        + "12 Tools_* (Audio/Base/Compress/Core/Downloader/File/Image/Layout/Sheet/"
        + "Style/Task/Video). Fasad: semua blok diteruskan ke mesin aslinya, semua "
        + "event dicerminkan ke sini. Cocok bila tidak mau banyak komponen di daftar "
        + "blok. JANGAN pasang bareng 12 Tools_* dalam satu aplikasi (event dobel + "
        + "boros memori). Konvensi awalan: Is = boolean (colok ke if), Call = async + "
        + "hasil lewat event, Set = tanpa feedback sukses (gagal via ErrorOccurred), "
        + "Get = nilai balik sinkron. Pilihan tetap (colok ke soket yang "
        + "cocok). FILE INI DIGENERATE OTOMATIS oleh tools/gen_facade.mjs — jangan "
        + "edit manual.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    androidMinSdk = 21,
    iconName = "aiwebres/tools_all_in_one.jpg",
    helpUrl = "https://github.com/VB-7/MiT-File_Manager")
@SimpleObject(external = true)
@UsesPermissions({
    "android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE",
    "android.permission.MANAGE_EXTERNAL_STORAGE",
    "android.permission.CAMERA",
    "android.permission.RECORD_AUDIO",
    "android.permission.POST_NOTIFICATIONS",
    "android.permission.READ_MEDIA_IMAGES",
    "android.permission.READ_MEDIA_VIDEO",
    "android.permission.READ_MEDIA_AUDIO",
    "android.permission.ACCESS_FINE_LOCATION",
    "android.permission.ACCESS_COARSE_LOCATION",
    "android.permission.INTERNET",
    "android.permission.WAKE_LOCK",
    "android.permission.RECEIVE_BOOT_COMPLETED",
    "android.permission.SCHEDULE_EXACT_ALARM"
})
public class Tools_All_in_One extends AndroidNonvisibleComponent {

  private final Tools_Audio audio;
  private final Tools_Base base;
  private final Tools_Compress compress;
  private final Tools_Core core;
  private final Tools_Downloader downloader;
  private final Tools_File file;
  private final Tools_Image image;
  private final Tools_Layout layout;
  private final Tools_Sheet sheet;
  private final Tools_Style style;
  private final Tools_Task task;
  private final Tools_Video video;

  public Tools_All_in_One(ComponentContainer container) {
    super(container.$form());
    audio = new Tools_Audio(container);
    base = new Tools_Base(container);
    compress = new Tools_Compress(container);
    core = new Tools_Core(container);
    downloader = new Tools_Downloader(container);
    file = new Tools_File(container);
    image = new Tools_Image(container);
    layout = new Tools_Layout(container);
    sheet = new Tools_Sheet(container);
    style = new Tools_Style(container);
    task = new Tools_Task(container);
    video = new Tools_Video(container);
    Shared.registerAllInOne(this);
  }

  // ================= Audio =================

  @SimpleFunction(description = "CALL: Putar audio ber-id (background, tanpa tampilan). Bisa jalan bareng "
      + "(satu id = satu audio). path file (/Download/... atau absolut apa adanya), "
      + "file:/// absolut, atau http(s) URL (streaming, izin INTERNET otomatis dibawa). "
      + "id dari CreateAudioId atau CreateUniqueId Tools_Base (reservasi diadopsi). "
      + "Id yang masih bunyi tidak bisa dipakai ulang (Stop/Pause dulu). Stop lalu Play "
      + "= mulai dari awal. Tampilan (Slider/Label) bikin sendiri dari "
      + "AudioStarted/ProgressChanged/Finished. Sukses menembak AudioStarted; gagal via "
      + "ErrorOccurred.")
  public void CallPlayAudio(final String path, final int id) {
    audio.CallPlayAudio(path, id);
  }

  @SimpleFunction(description = "SET: Jeda audio yang sedang bunyi. Tanpa feedback. "
      + "Cek status via IsAudioPaused.")
  public void SetPauseAudio(int id) {
    audio.SetPauseAudio(id);
  }

  @SimpleFunction(description = "SET: Lanjutkan audio yang dijeda. Tanpa feedback. "
      + "Cek status via IsAudioPaused.")
  public void SetResumeAudio(int id) {
    audio.SetResumeAudio(id);
  }

  @SimpleFunction(description = "SET: Hentikan audio (posisi kembali ke 0, tanpa event). "
      + "Putar lagi via CallPlayAudio (mulai dari awal). Cek via IsAudioIdActive.")
  public void SetStopAudio(int id) {
    audio.SetStopAudio(id);
  }

  @SimpleFunction(description = "SET: Lompat audio ke posisi milidetik. Bisa dipasang ke "
      + "Slider. Tanpa feedback.")
  public void SetSeekAudio(int id, int position) {
    audio.SetSeekAudio(id, position);
  }

  @SimpleFunction(description = "SET: Maju audio sejauh SkipSeconds DETIK. "
      + "Tanpa feedback.")
  public void SetForwardAudio(int id) {
    audio.SetForwardAudio(id);
  }

  @SimpleFunction(description = "SET: Maju audio sejauh seconds DETIK "
      + "(tanpa ubah SkipSeconds). Tanpa feedback.")
  public void SetForwardAudioBy(int id, int seconds) {
    audio.SetForwardAudioBy(id, seconds);
  }

  @SimpleFunction(description = "SET: Mundur audio sejauh SkipSeconds DETIK. "
      + "Tanpa feedback.")
  public void SetBackwardAudio(int id) {
    audio.SetBackwardAudio(id);
  }

  @SimpleFunction(description = "SET: Mundur audio sejauh seconds DETIK "
      + "(tanpa ubah SkipSeconds). Tanpa feedback.")
  public void SetBackwardAudioBy(int id, int seconds) {
    audio.SetBackwardAudioBy(id, seconds);
  }

  @SimpleFunction(description = "Posisi audio sekarang dalam milidetik. 0 bila belum siap.")
  public int GetAudioPosition(int id) {
    return audio.GetAudioPosition(id);
  }

  @SimpleFunction(description = "Durasi audio dalam milidetik. 0 bila belum siap.")
  public int GetAudioDuration(int id) {
    return audio.GetAudioDuration(id);
  }

  @SimpleFunction(description = "Buat id audio unik baru (Number, siap pakai ke CallPlayAudio).")
  public int CreateAudioId() {
    return audio.CreateAudioId();
  }

  @SimpleFunction(description = "Semua id audio yang dipegang (URUT PEMBUATAN, "
      + "index 1 = pertama dibuat).")
  public List<Integer> GetAudioIds() {
    return audio.GetAudioIds();
  }

  @SimpleFunction(description = "Id audio yang sedang bunyi (URUT PEMBUATAN). "
      + "Yang dijeda/berhenti TIDAK masuk sini.")
  public List<Integer> GetActiveAudioIds() {
    return audio.GetActiveAudioIds();
  }

  @SimpleFunction(description = "Id audio yang terakhir disentuh (dibuat atau di-start). "
      + "-1 bila kosong. Beda dengan GetLastAudioId (terakhir dibuat).")
  public int CurrentAudioId() {
    return audio.CurrentAudioId();
  }

  @SimpleFunction(description = "Id index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstAudioId() {
    return audio.GetFirstAudioId();
  }

  @SimpleFunction(description = "Id audio TERAKHIR DIBUAT. -1 bila kosong. Beda dengan "
      + "CurrentAudioId (terakhir disentuh).")
  public int GetLastAudioId() {
    return audio.GetLastAudioId();
  }

  @SimpleFunction(description = "Id yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.")
  public int GetPreviousAudioId(int id) {
    return audio.GetPreviousAudioId(id);
  }

  @SimpleFunction(description = "True bila id audio ini sedang bunyi. False = jeda, "
      + "berhenti, atau belum ada (bedakan jeda vs berhenti via IsAudioPaused).")
  public boolean IsAudioIdActive(int id) {
    return audio.IsAudioIdActive(id);
  }

  @SimpleFunction(description = "True bila id audio ini sedang dijeda (Pause tapi "
      + "belum Stop/selesai). False = bunyi/berhenti/belum ada.")
  public boolean IsAudioPaused(int id) {
    return audio.IsAudioPaused(id);
  }

  @SimpleFunction(description = "Volume khusus satu id (0-100). = volume global "
      + "bila belum diatur via SetAudioVolume.")
  public int GetAudioVolume(int id) {
    return audio.GetAudioVolume(id);
  }

  @SimpleFunction(description = "SET: Volume khusus satu id (0-100). Menimpa "
      + "volume global untuk id ini. Tanpa feedback.")
  public void SetAudioVolume(int id, int volume) {
    audio.SetAudioVolume(id, volume);
  }

  @SimpleFunction(description = "Loop khusus satu id. = loop global bila belum "
      + "diatur via SetAudioLooping.")
  public boolean GetAudioLooping(int id) {
    return audio.GetAudioLooping(id);
  }

  @SimpleFunction(description = "SET: Loop khusus satu id (true = mengulang bila "
      + "selesai). Menimpa loop global untuk id ini. Tanpa feedback.")
  public void SetAudioLooping(int id, boolean loop) {
    audio.SetAudioLooping(id, loop);
  }

  @SimpleFunction(description = "Kecepatan id ini (1.0 = normal). 1.0 bila belum "
      + "diatur via SetAudioSpeed.")
  public float GetAudioSpeed(int id) {
    return audio.GetAudioSpeed(id);
  }

  @SimpleFunction(description = "SET: Kecepatan khusus satu id (1.0 = normal, "
      + "0.5 = setengah, 2.0 = ganda). Butuh Android 6+. Tanpa feedback.")
  public void SetAudioSpeed(int id, float speed) {
    audio.SetAudioSpeed(id, speed);
  }

  @SimpleFunction(description = "Hapus id audio: hentikan bila bunyi + lepas id "
      + "(otomatis beres-beres).")
  public void RemoveAudioId(int id) {
    audio.RemoveAudioId(id);
  }

  @SimpleFunction(description = "SET: Hentikan SEMUA audio + buang semua id. "
      + "Tanpa feedback.")
  public void SetClearAudios() {
    audio.SetClearAudios();
  }

  @SimpleProperty(description = "Jeda event AudioProgressChanged dalam milidetik (default 500). "
      + "Lagu 3 menit = 360 event; HP kentang naikkan ke 1000.",
      category = PropertyCategory.BEHAVIOR)
  public int ProgressInterval() {
    return audio.ProgressInterval();
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER,
      defaultValue = "500")
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void ProgressInterval(int interval) {
    audio.ProgressInterval(interval);
    video.ProgressInterval(interval);
  }

  @SimpleProperty(description = "Lompatan Maju/Mundur dalam DETIK (bukan ms). Default 10. "
      + "Untuk lompatan sekali-sekali pakai SetForwardAudioBy/SetBackwardAudioBy.",
      category = PropertyCategory.BEHAVIOR)
  public int SkipSeconds() {
    return audio.SkipSeconds();
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER,
      defaultValue = "10")
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void SkipSeconds(int seconds) {
    audio.SkipSeconds(seconds);
    video.SkipSeconds(seconds);
  }

  @SimpleProperty(description = "Loop global (default false). Id yang sudah diatur via "
      + "SetAudioLooping memakai nilainya sendiri.",
      category = PropertyCategory.BEHAVIOR)
  public boolean AudioLooping() {
    return audio.AudioLooping();
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
      defaultValue = "False")
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void AudioLooping(boolean loop) {
    audio.AudioLooping(loop);
  }

  @SimpleProperty(description = "Volume global 0-100 (default 100, awal semua id). Id yang "
      + "sudah diatur via SetAudioVolume memakai nilainya sendiri.",
      category = PropertyCategory.BEHAVIOR)
  public int MediaVolume() {
    return audio.MediaVolume();
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER,
      defaultValue = "100")
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void MediaVolume(int volume) {
    audio.MediaVolume(volume);
    video.MediaVolume(volume);
  }

  @SimpleEvent(description = "Audio mulai bunyi: id + durasi (ms) diketahui.")
  public void AudioStarted(int id, int duration) {
    EventDispatcher.dispatchEvent(this, "AudioStarted", id, duration);
  }

  @SimpleEvent(description = "Progres audio berubah: id + posisi + durasi (ms) + persen 0-100.")
  public void AudioProgressChanged(int id, int position, int duration, int percent) {
    EventDispatcher.dispatchEvent(this, "AudioProgressChanged", id, position, duration, percent);
  }

  @SimpleEvent(description = "Audio selesai (tidak loop). Putar lagi via CallPlayAudio.")
  public void AudioFinished(int id) {
    EventDispatcher.dispatchEvent(this, "AudioFinished", id);
  }

  @SimpleEvent(description = "SATU-SATUNYA error gabungan 12 pemilik. "
      + "component = nama operasi (isi dengan OpAudio/OpJson/OpId/OpPermission/OpZip/OpUnzip/OpCore"
      + "/OpDownload/OpDirectory/OpFile/OpRead/OpBase64/OpCopyOrMove/OpPick/OpOpen/OpSlider/OpImage"
      + "/OpLayout/OpSheet/OpToast/OpNotification/OpStyle/OpTask/OpAlarm/OpVar/OpVideo untuk if)."
      + " Contoh: jika component = OpAudio maka tampilkan message. id = id terkait (0 bila statis)."
      + " ")
  public void ErrorOccurred(String component, int id, String message) {
    EventDispatcher.dispatchEvent(this, "ErrorOccurred", component, id, message);
  }

  @SimpleProperty(description = "Nama operasi Audio untuk if di ErrorOccurred. Contoh: "
      + "colok ke perbandingan = dengan component dari event ErrorOccurred.",
      category = PropertyCategory.BEHAVIOR)
  public String OpAudio() {
    return audio.OpAudio();
  }

  @SimpleFunction(description = "Fallback polling untuk event ErrorOccurred "
      + "gabungan 12 pemilik: pesan error terakhir operasi ini (teks kosong bila belum ada). Isi"
      + " operation dengan OpAudio/OpJson/OpId/OpPermission/OpZip/OpUnzip/OpCore/OpDownload/OpDirec"
      + "tory/OpFile/OpRead/OpBase64/OpCopyOrMove/OpPick/OpOpen/OpSlider/OpImage/OpLayout/OpSheet/O"
      + "pToast/OpNotification/OpStyle/OpTask/OpAlarm/OpVar/OpVideo untuk if. Contoh: simpan"
      + " LastError(OpAudio) ke variabel bila ErrorOccurred menembak. ")
  public String LastError(String operation) {
    return Shared.lastError(operation);
  }

  // ================= Base =================

  @SimpleFunction(description = "Buat id unik global baru (Number, reservasi otomatis). "
      + "Siap pakai ke blok Call... pemiliknya (Download/Image/Audio/Video/dll; "
      + "reservasi otomatis diadopsi). Urutan pembuatan = urutan list.")
  public int CreateUniqueId() {
    return base.CreateUniqueId();
  }

  @SimpleFunction(description = "BAHAYA: angka terkecil >= 1 yang belum dipakai siapa "
      + "pun, TAPI tidak di-reservasi (tidak dibooking). Dua pemanggil bisa "
      + "dapat angka SAMA = tabrakan. JANGAN dipakai dua tempat bersamaan; "
      + "kalau ragu pakai CreateUniqueId (reservasi otomatis).")
  public int SuggestId() {
    return base.SuggestId();
  }

  @SimpleFunction(description = "True bila id sedang dipakai operasi lain.")
  public boolean IsIdBusy(int id) {
    return base.IsIdBusy(id);
  }

  @SimpleFunction(description = "Semua id sibuk lintas tipe (URUT PEMBUATAN, "
      + "index 1 = pertama dibuat). Kosong = tidak ada.")
  public List<Integer> BusyIds() {
    return base.BusyIds();
  }

  @SimpleFunction(description = "SET: lepas paksa id yang macet (ruang UGD). "
      + "Resep urut: Cancel/Stop/Remove pemiliknya DULU, SetForceReleaseId "
      + "TERAKHIR. Id macet bila cancel tidak melepas / crash di tengah. "
      + "Tanpa feedback.")
  public void SetForceReleaseId(int id) {
    base.SetForceReleaseId(id);
  }

  @SimpleFunction(description = "Buat id unik baru di tag ini (Number, angka terkecil yang belum "
      + "dipakai, langsung di-reservasi). Untuk saat kamulah yang menentukan id-nya "
      + "(mis. bikin tab sendiri). Beda dengan RegisterTagId yang mencatat id dari luar.")
  public int CreateTagId(String tag) {
    return base.CreateTagId(tag);
  }

  @SimpleFunction(description = "Catat id komponen LUAR ke tag (mis. WebView pihak ketiga). "
      + "id boleh Number ATAU teks angka (\"5\" = 5, otomatis dikonversi). "
      + "Beda dengan CreateTagId yang MEMBUATKAN id baru. Id dobel = tetap "
      + "satu (idempoten). Gagal (tag kosong / bukan angka) via ErrorOccurred.")
  public void RegisterTagId(Object id, String tag) {
    base.RegisterTagId(id, tag);
  }

  @SimpleFunction(description = "Semua id di tag ini (Number semua, URUT MENYIMPAN, "
      + "index 1 = pertama disimpan).")
  public List<Integer> GetTagIds(String tag) {
    return base.GetTagIds(tag);
  }

  @SimpleFunction(description = "Id pada nomor urut index di tag ini "
      + "(1 = pertama disimpan). -1 bila tag kosong / index di luar. "
      + "Cth: GetTagIds lalu GetTagIdAt(tag, 2) = id kedua.")
  public int GetTagIdAt(String tag, int index) {
    return base.GetTagIdAt(tag, index);
  }

  @SimpleFunction(description = "Id index 1 = PERTAMA DISIMPAN di tag ini. -1 bila kosong.")
  public int GetFirstTagId(String tag) {
    return base.GetFirstTagId(tag);
  }

  @SimpleFunction(description = "Id TERAKHIR DISIMPAN di tag ini. -1 bila kosong.")
  public int GetLastTagId(String tag) {
    return base.GetLastTagId(tag);
  }

  @SimpleFunction(description = "Id yang disimpan TEPAT SEBELUM id ini di tag itu. "
      + "id boleh Number / teks angka. -1 bila tidak ada.")
  public int GetPreviousTagId(Object id, String tag) {
    return base.GetPreviousTagId(id, tag);
  }

  @SimpleFunction(description = "True bila id ini tercatat di tag itu "
      + "(teks angka ikut cocok: \"5\" = 5).")
  public boolean IsTagIdSaved(Object id, String tag) {
    return base.IsTagIdSaved(id, tag);
  }

  @SimpleFunction(description = "True bila tag ini ADA (masih menyimpan "
      + "id). Beda dengan IsTagIdSaved yang cek id di dalam satu tag.")
  public boolean IsTagIdExists(String tag) {
    return base.IsTagIdExists(tag);
  }

  @SimpleFunction(description = "Jumlah id tercatat di tag ini.")
  public int GetTagIdCount(String tag) {
    return base.GetTagIdCount(tag);
  }

  @SimpleFunction(description = "Hapus satu id dari tag (hanya lepas catatan, komponen luar "
      + "tidak diusik). Tag yang kosong ikut dibuang. Id yang memang tidak "
      + "tercatat = idempoten, TAPI tetap lapor via ErrorOccurred (\"tidak tercatat\") "
      + "agar salah ketik tag ketahuan; bukan diam.")
  public void RemoveTagId(Object id, String tag) {
    base.RemoveTagId(id, tag);
  }

  @SimpleFunction(description = "SET: hapus SELURUH isi satu tag. Tanpa feedback.")
  public void SetClearTag(String tag) {
    base.SetClearTag(tag);
  }

  @SimpleFunction(description = "Semua nama tag yang masih punya isi (URUT DIBUAT).")
  public List<String> GetAllTags() {
    return base.GetAllTags();
  }

  @SimpleFunction(description = "CALL: SATU PINTU minta 1 izin apa pun (manual + "
      + "luar: teks / blok Permission... / 1 entri list permission event lain). "
      + "Pintar OS: sudah granted = langsung GotPermission(true) tanpa dialog; "
      + "storage Android 11+ belum = OTOMATIS buka Settings + "
      + "GotPermission(false + pesan); lainnya = dialog izin (end-user yang "
      + "mutusin allow/deny) + GotPermission. permanentlyDenied=false = user "
      + "menolak (boleh tanya lagi); true = permanen (aktifkan manual di "
      + "Setelan). Tanpa return.")
  public void CallRequestPermission(final String permission) {
    base.CallRequestPermission(permission);
  }

  @SimpleFunction(description = "CALL: minta BANYAK izin sekaligus (list; contoh "
      + "langsung colok list permission dari event luar). GotPermission "
      + "ditembak PER IZIN (satu bentuk hasil). Entri kosong dilewati.")
  public void CallRequestPermissions(final YailList permissions) {
    base.CallRequestPermissions(permissions);
  }

  @SimpleFunction(description = "True bila 1 izin ini sudah granted (teks ATAU "
      + "blok Permission...). Storage Android 11+ = cek akses semua file. "
      + "Android 6 ke bawah selalu true (izin sejak install).")
  public boolean IsPermissionGranted(String permission) {
    return base.IsPermissionGranted(permission);
  }

  @SimpleFunction(description = "Daftar izin dari list yang SUDAH granted "
      + "(untuk global/var/alur). Kosong = tak ada yang beres.")
  public List<String> GetGrantedPermissions(YailList permissions) {
    return base.GetGrantedPermissions(permissions);
  }

  @SimpleFunction(description = "Daftar izin dari list yang BELUM granted / "
      + "ditolak (untuk global/var/alur). Kosong = semua beres.")
  public List<String> GetDeniedPermissions(YailList permissions) {
    return base.GetDeniedPermissions(permissions);
  }

  @SimpleEvent(description = "Hasil CallRequestPermission(s) Base (HANYA untuk "
      + "izin yang diminta via Base - tidak menembak untuk izin tools lain): "
      + "success + izin + pesan + permanentlyDenied. success=true = beres "
      + "(lanjut). false + permanentlyDenied=false = user menolak (boleh "
      + "tanya lagi) ATAU kasus sistem (storage 11+ / input salah - baca "
      + "message). false + true = permanen (aktifkan manual di Setelan).")
  public void GotPermission(boolean success, String permission, String message, boolean permanentlyDenied) {
    EventDispatcher.dispatchEvent(this, "GotPermission", success, permission, message, permanentlyDenied);
  }

  @SimpleProperty(description = "Pilihan read-only: izin kamera (CADANGAN - belum ada "
      + "bloknya). Colok ke CallRequestPermission(s) / IsPermissionGranted "
      + "di sini (tanpa ketik string).",
      category = PropertyCategory.BEHAVIOR)
  public String PermissionCamera() {
    return base.PermissionCamera();
  }

  @SimpleProperty(description = "Pilihan read-only: izin mikrofon, rekam "
      + "suara (CADANGAN - belum ada bloknya). Colok ke "
      + "CallRequestPermission(s) / IsPermissionGranted di sini.",
      category = PropertyCategory.BEHAVIOR)
  public String PermissionMicrophone() {
    return base.PermissionMicrophone();
  }

  @SimpleProperty(description = "Pilihan read-only: izin baca storage. "
      + "Colok ke CallRequestPermission(s) / IsPermissionGranted di sini.",
      category = PropertyCategory.BEHAVIOR)
  public String PermissionReadStorage() {
    return base.PermissionReadStorage();
  }

  @SimpleProperty(description = "Pilihan read-only: izin tulis storage. "
      + "Colok ke CallRequestPermission(s) / IsPermissionGranted di sini.",
      category = PropertyCategory.BEHAVIOR)
  public String PermissionWriteStorage() {
    return base.PermissionWriteStorage();
  }

  @SimpleProperty(description = "Pilihan read-only: izin notifikasi "
      + "(Android 13+). Colok ke CallRequestPermission(s) / "
      + "IsPermissionGranted di sini.",
      category = PropertyCategory.BEHAVIOR)
  public String PermissionNotifications() {
    return base.PermissionNotifications();
  }

  @SimpleProperty(description = "Pilihan read-only: izin baca gambar "
      + "(Android 13+). Colok ke CallRequestPermission(s) / "
      + "IsPermissionGranted di sini.",
      category = PropertyCategory.BEHAVIOR)
  public String PermissionReadImages() {
    return base.PermissionReadImages();
  }

  @SimpleProperty(description = "Pilihan read-only: izin baca video "
      + "(Android 13+). Colok ke CallRequestPermission(s) / "
      + "IsPermissionGranted di sini.",
      category = PropertyCategory.BEHAVIOR)
  public String PermissionReadVideo() {
    return base.PermissionReadVideo();
  }

  @SimpleProperty(description = "Pilihan read-only: izin baca audio "
      + "(Android 13+). Colok ke CallRequestPermission(s) / "
      + "IsPermissionGranted di sini.",
      category = PropertyCategory.BEHAVIOR)
  public String PermissionReadAudio() {
    return base.PermissionReadAudio();
  }

  @SimpleProperty(description = "Pilihan read-only: izin lokasi akurat (CADANGAN - "
      + "belum ada bloknya). Colok ke CallRequestPermission(s) / "
      + "IsPermissionGranted di sini.",
      category = PropertyCategory.BEHAVIOR)
  public String PermissionLocationFine() {
    return base.PermissionLocationFine();
  }

  @SimpleProperty(description = "Pilihan read-only: izin lokasi kasar (CADANGAN - "
      + "belum ada bloknya). Colok ke CallRequestPermission(s) / "
      + "IsPermissionGranted di sini.",
      category = PropertyCategory.BEHAVIOR)
  public String PermissionLocationCoarse() {
    return base.PermissionLocationCoarse();
  }

  @SimpleFunction(description = "Encode list/dictionary/teks/angka/boolean jadi JSON.")
  public String JsonEncode(Object value) {
    return base.JsonEncode(value);
  }

  @SimpleFunction(description = "Decode teks JSON jadi dictionary/list. "
      + "Gagal = \"\" + error (cek LastError(\"Json\"); pola guard: if hasil = \"\" berarti gagal).")
  public Object JsonDecode(String json) {
    return base.JsonDecode(json);
  }

  @SimpleFunction(description = "Encode teks untuk URL (spasi jadi %20).")
  public String UriEncode(String text) {
    return base.UriEncode(text);
  }

  @SimpleFunction(description = "Decode teks URL-encoded.")
  public String UriDecode(String text) {
    return base.UriDecode(text);
  }

  @SimpleFunction(description = "Ubah milidetik jadi teks jam (m:ss / h:mm:ss) untuk Label.")
  public String FormatTime(int millis) {
    return base.FormatTime(millis);
  }

  @SimpleFunction(description = "Ubah angka byte jadi teks (B/KB/MB/GB).")
  public String ConvertBytes(long bytes) {
    return base.ConvertBytes(bytes);
  }

  @SimpleFunction(description = "Tebak mime dari nama file/ekstensi. Tidak perlu file-nya ada.")
  public String GetMimeType(String fileName) {
    return base.GetMimeType(fileName);
  }

  @SimpleFunction(description = "Versi paket Tools (sama untuk semua Tools_* dalam satu .aix: "
      + "wajib SAMA semua per rilis; beda = bug kemasan).")
  public int ToolsVersion() {
    return base.ToolsVersion();
  }

  @SimpleFunction(description = "Catatan update paket Tools (dibaca lewat blok, "
      + "karena panel info extension tidak bisa diubah manual).")
  public String GetWhatsNew() {
    return base.GetWhatsNew();
  }

  @SimpleProperty(description = "Nama operasi Json (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpJson() {
    return base.OpJson();
  }

  @SimpleProperty(description = "Nama operasi Id (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpId() {
    return base.OpId();
  }

  @SimpleProperty(description = "Nama operasi Permission (untuk if di "
      + "ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpPermission() {
    return base.OpPermission();
  }

  // ================= Compress =================

  @SimpleFunction(description = "Deteksi jenis file dari ISINYA (bukan ekstensi). Kembali: zip, "
      + "rar, 7z, gz, bz2, xz, tar, apk, pdf, png, jpg, gif, mp3, mp4, atau unknown. "
      + "Instan, tanpa id. Pasangan GetZipEntries: hasil \"kosong\" + Detect = bukan-ZIP "
      + "berarti zip-nya kosong (bukan rusak).")
  public String DetectArchiveType(String path) {
    return compress.DetectArchiveType(path);
  }

  @SimpleFunction(description = "Daftar isi ZIP (nama entri). Instan. Kosong = bukan ZIP "
      + "ATAU zip kosong (bedakan via DetectArchiveType dulu).")
  public List<String> GetZipEntries(String zipPath) {
    return compress.GetZipEntries(zipPath);
  }

  @SimpleFunction(description = "CALL: bikin ZIP dari BANYAK file sekaligus (tanpa loop). paths = "
      + "list path FILE (folder dilewati, tidak rekursif; list kosong = Finished gagal "
      + "\"list kosong\"). destZip = path file zip tujuan (sudah ada = DITIMPA). "
      + "Mulai = menembak Started; gagal via ErrorOccurred. "
      + "Pantau via ZipStarted/ProgressChanged/Finished. Satu proses ZIP per saat.")
  public void CallZipFiles(Object paths, String destZip) {
    compress.CallZipFiles(paths, destZip);
  }

  @SimpleFunction(description = "CALL: ekstrak ZIP ke folder. Struktur folder di dalam ZIP dijaga. "
      + "destFolder sudah ada = DIGABUNG (file sama nama ditimpa). Bukan ZIP valid = "
      + "Finished gagal \"ekstrak gagal (bukan ZIP valid?)\". Batalkan via SetCancelZip "
      + "(file yang sudah terekstrak tetap ada). Pantau via "
      + "UnzipStarted/ProgressChanged/Finished.")
  public void CallUnzip(String zipPath, String destFolder) {
    compress.CallUnzip(zipPath, destFolder);
  }

  @SimpleFunction(description = "SET: batalkan proses ZIP/Unzip berjalan. Tanpa feedback "
      + "(tidak ada event Cancelled: cancel = Set yang pasti). Efek: berhenti; file ZIP "
      + "sebagian DIHAPUS, file yang sudah terekstrak tetap ada. Verifikasi via IsZipBusy "
      + "(false = sudah berhenti) lalu coba-lagi.")
  public void SetCancelZip() {
    compress.SetCancelZip();
  }

  @SimpleFunction(description = "True bila ada proses ZIP/Unzip berjalan.")
  public boolean IsZipBusy() {
    return compress.IsZipBusy();
  }

  @SimpleEvent(description = "Bikin ZIP dimulai: total file + path ZIP tujuan.")
  public void ZipStarted(int total, String zipPath) {
    EventDispatcher.dispatchEvent(this, "ZipStarted", total, zipPath);
  }

  @SimpleEvent(description = "Progres bikin ZIP berubah: index/total + nama file + percent (0-100).")
  public void ZipProgressChanged(int index, int total, String fileName, int percent) {
    EventDispatcher.dispatchEvent(this, "ZipProgressChanged", index, total, fileName, percent);
  }

  @SimpleEvent(description = "Bikin ZIP selesai: success + path ZIP + fileName + "
      + "jumlah ok/gagal.")
  public void ZipFinished(boolean success, String zipPath, String fileName, int doneCount, int failCount, String message) {
    EventDispatcher.dispatchEvent(this, "ZipFinished", success, zipPath, fileName, doneCount, failCount, message);
  }

  @SimpleEvent(description = "Ekstrak ZIP dimulai: nama ZIP + total entri + folder tujuan.")
  public void UnzipStarted(String zipName, int total, String destFolder) {
    EventDispatcher.dispatchEvent(this, "UnzipStarted", zipName, total, destFolder);
  }

  @SimpleEvent(description = "Progres ekstrak berubah: index/total + nama entri + percent (0-100).")
  public void UnzipProgressChanged(int index, int total, String entryName, int percent) {
    EventDispatcher.dispatchEvent(this, "UnzipProgressChanged", index, total, entryName, percent);
  }

  @SimpleEvent(description = "Ekstrak selesai: success + folder + jumlah ok/gagal.")
  public void UnzipFinished(boolean success, String destFolder, int doneCount, int failCount, String message) {
    EventDispatcher.dispatchEvent(this, "UnzipFinished", success, destFolder, doneCount, failCount, message);
  }

  @SimpleProperty(description = "Nama operasi Zip (untuk if di ErrorOccurred: "
      + "cth if component = OpZip berarti error bikin-ZIP).",
      category = PropertyCategory.BEHAVIOR)
  public String OpZip() {
    return compress.OpZip();
  }

  @SimpleProperty(description = "Nama operasi Unzip (untuk if di ErrorOccurred: "
      + "cth if component = OpUnzip berarti error ekstrak).",
      category = PropertyCategory.BEHAVIOR)
  public String OpUnzip() {
    return compress.OpUnzip();
  }

  // ================= Core =================

  @SimpleFunction(description = "Bikin komponen MIT dinamis ber-id ke dalam layout "
      + "(Arrangement MIT / dinamis). typeName: Button, Label, TextBox, Image, "
      + "CheckBox, Switch, Slider, Spinner, ListView, DatePicker, TimePicker, Space, "
      + "HorizontalArrangement, VerticalArrangement, HorizontalScrollArrangement, "
      + "VerticalScrollArrangement (tidak case-sensitive; salah = ErrorOccurred + "
      + "daftar valid). id = penanda (minta ke CreateDynamicId, "
      + "reservasi otomatis diadopsi). Sukses menembak ComponentCreated; "
      + "klik/longklik menembak CoreClicked/CoreLongClicked; gagal via "
      + "ErrorOccurred. Pemula mulai dari sini + Set satu-satu.")
  public void CallCreateDynamicComponent(String typeName, AndroidViewComponent container, final int id) {
    core.CallCreateDynamicComponent(typeName, container, id);
  }

  @SimpleFunction(description = "Ambil komponen dinamis ber-id (untuk dicolok ke "
      + "soket komponen / blok Any generik MIT saat SET/GET properti). null bila "
      + "tidak ada. Batas Any: Any untuk SET/GET boleh (colok hasil sini); Any "
      + "untuk EVENT klik TIDAK menyala pada komponen dinamis (pakai CoreClicked).")
  public Object GetDynamicComponent(int id) {
    return core.GetDynamicComponent(id);
  }

  @SimpleFunction(description = "SET: Set properti komponen dinamis ber-id "
      + "(nama properti MIT, mis. Text, BackgroundColor, FontSize). "
      + "Sukses diam; gagal via ErrorOccurred.")
  public void SetDynamicProperty(int id, String name, Object value) {
    core.SetDynamicProperty(id, name, value);
  }

  @SimpleFunction(description = "CALL: Jalan pintas: bikin Button dinamis ber-id "
      + "lengkap dengan teks, warna, ukuran font, tebal, visibilitas, "
      + "LEBAR + TINGGI (angka MIT: -2 isi, -1 otomatis, positif pixel). "
      + "Sama seperti Create + Set berulang, tapi satu blok. Hanya bila sudah "
      + "pede butuh sekaligus; pemula pakai Create + Set satu-satu.")
  public void CallCreateDynamicButton(AndroidViewComponent container, int id, String text, int textColor, int backgroundColor, float fontSize, boolean bold, boolean visible, int width, int height) {
    core.CallCreateDynamicButton(container, id, text, textColor, backgroundColor, fontSize, bold, visible, width, height);
  }

  @SimpleFunction(description = "CALL: Jalan pintas: bikin Label dinamis ber-id "
      + "lengkap dengan teks, warna, ukuran font, tebal, visibilitas, "
      + "LEBAR + TINGGI (angka MIT). Hanya bila sudah pede butuh sekaligus; "
      + "pemula pakai Create + Set satu-satu.")
  public void CallCreateDynamicLabel(AndroidViewComponent container, int id, String text, int textColor, int backgroundColor, float fontSize, boolean bold, boolean visible, int width, int height) {
    core.CallCreateDynamicLabel(container, id, text, textColor, backgroundColor, fontSize, bold, visible, width, height);
  }

  @SimpleFunction(description = "CALL: Jalan pintas: bikin TextBox dinamis ber-id "
      + "lengkap dengan hint, warna, ukuran font, tebal, visibilitas, "
      + "LEBAR + TINGGI (angka MIT). Hanya bila sudah pede butuh sekaligus; "
      + "pemula pakai Create + Set satu-satu.")
  public void CallCreateDynamicTextBox(AndroidViewComponent container, int id, String hint, int textColor, int backgroundColor, float fontSize, boolean bold, boolean visible, int width, int height) {
    core.CallCreateDynamicTextBox(container, id, hint, textColor, backgroundColor, fontSize, bold, visible, width, height);
  }

  @SimpleFunction(description = "CALL: Jalan pintas: bikin Image (gambar MIT) "
      + "dinamis ber-id dengan picture + visibilitas + LEBAR + TINGGI "
      + "(angka MIT). Isi picture seperti Image.Picture biasa "
      + "(path/asset/URL). Hanya bila sudah pede butuh sekaligus; "
      + "pemula pakai Create + Set satu-satu.")
  public void CallCreateDynamicImage(AndroidViewComponent container, int id, String picturePath, boolean visible, int width, int height) {
    core.CallCreateDynamicImage(container, id, picturePath, visible, width, height);
  }

  @SimpleFunction(description = "Baca properti komponen dinamis ber-id. SELALU teks "
      + "(angka pun jadi teks: FontSize = \"14.0\"; untuk hitung, konversi dulu). "
      + "Kosong bila tidak ada/gagal (lihat ErrorOccurred).")
  public String GetDynamicProperty(int id, String name) {
    return core.GetDynamicProperty(id, name);
  }

  @SimpleFunction(description = "CALL sinkron (pengecualian resmi keluarga Call): "
      + "panggil fungsi komponen dinamis ber-id, hasil LANGSUNG via return "
      + "(bukan event). name = nama fungsi MIT (tidak case-sensitive; cth: "
      + "RequestFocus untuk TextBox, Clear untuk TextBox/ListView; kosong bila "
      + "void/gagal; gagal via ErrorOccurred). args = list argumen (kosong bila "
      + "tanpa argumen).")
  public String CallInvokeDynamic(int id, String name, YailList args) {
    return core.CallInvokeDynamic(id, name, args);
  }

  @SimpleFunction(description = "SET: atur lebar + tinggi komponen dinamis ber-id "
      + "(semua tipe: Button/Label/TextBox/Image/dll). Angka SAMA seperti blok "
      + "bawaan MIT: -2 = isi penuh, -1 = otomatis, positif = pixel. "
      + "Sukses diam; gagal via ErrorOccurred.")
  public void SetDynamicSize(int id, int width, int height) {
    core.SetDynamicSize(id, width, height);
  }

  @SimpleFunction(description = "SET: tampil/sembunyi komponen dinamis ber-id. "
      + "false = GONE (hilang total). Sukses diam; gagal via ErrorOccurred.")
  public void SetDynamicVisible(int id, boolean visible) {
    core.SetDynamicVisible(id, visible);
  }

  @SimpleFunction(description = "SET: teks komponen dinamis ber-id "
      + "(Button/Label/TextBox/Hint). Tipe tak punya teks = ErrorOccurred "
      + "(TIDAK diam). Sukses diam.")
  public void SetDynamicText(int id, String text) {
    core.SetDynamicText(id, text);
  }

  @SimpleFunction(description = "SET: warna teks komponen dinamis ber-id. "
      + "Colok blok warna MIT. Tipe tak punya warna teks = ErrorOccurred "
      + "(TIDAK diam). Sukses diam.")
  public void SetDynamicTextColor(int id, int color) {
    core.SetDynamicTextColor(id, color);
  }

  @SimpleFunction(description = "SET: warna latar komponen dinamis ber-id. "
      + "Colok blok warna MIT. Sukses diam; gagal via ErrorOccurred.")
  public void SetDynamicBackgroundColor(int id, int color) {
    core.SetDynamicBackgroundColor(id, color);
  }

  @SimpleFunction(description = "True bila komponen dinamis ber-id sedang tampil "
      + "(Visible). False = sembunyi ATAU id tidak ada (ragu? cek IsDynamicIdActive).")
  public boolean IsDynamicVisible(int id) {
    return core.IsDynamicVisible(id);
  }

  @SimpleFunction(description = "Tipe komponen dinamis ber-id (\"Button\" dkk, "
      + "sama dengan typeName di ComponentCreated). Bandingkan dengan blok "
      + "TypeButton/TypeLabel/dll. Kosong bila id tidak ada.")
  public String GetDynamicType(int id) {
    return core.GetDynamicType(id);
  }

  @SimpleProperty(description = "Pembanding read-only: tipe Button. "
      + "Pilihan tetap, colok ke soket teks perbandingan "
      + "(cth: if GetDynamicType = TypeButton).",
      category = PropertyCategory.BEHAVIOR)
  public String TypeButton() {
    return core.TypeButton();
  }

  @SimpleProperty(description = "Pembanding read-only: tipe Label. "
      + "Pilihan tetap, colok ke soket teks perbandingan.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeLabel() {
    return core.TypeLabel();
  }

  @SimpleProperty(description = "Pembanding read-only: tipe TextBox. "
      + "Pilihan tetap, colok ke soket teks perbandingan.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeTextBox() {
    return core.TypeTextBox();
  }

  @SimpleProperty(description = "Pembanding read-only: tipe CheckBox. "
      + "Pilihan tetap, colok ke soket teks perbandingan.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeCheckBox() {
    return core.TypeCheckBox();
  }

  @SimpleProperty(description = "Pembanding read-only: tipe Switch. "
      + "Pilihan tetap, colok ke soket teks perbandingan.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeSwitch() {
    return core.TypeSwitch();
  }

  @SimpleProperty(description = "Pembanding read-only: tipe Slider. "
      + "Pilihan tetap, colok ke soket teks perbandingan.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeSlider() {
    return core.TypeSlider();
  }

  @SimpleProperty(description = "Pembanding read-only: tipe Spinner. "
      + "Pilihan tetap, colok ke soket teks perbandingan.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeSpinner() {
    return core.TypeSpinner();
  }

  @SimpleProperty(description = "Pembanding read-only: tipe ListView. "
      + "Pilihan tetap, colok ke soket teks perbandingan.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeListView() {
    return core.TypeListView();
  }

  @SimpleProperty(description = "Pembanding read-only: tipe DatePicker. "
      + "Pilihan tetap, colok ke soket teks perbandingan.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeDatePicker() {
    return core.TypeDatePicker();
  }

  @SimpleProperty(description = "Pembanding read-only: tipe TimePicker. "
      + "Pilihan tetap, colok ke soket teks perbandingan.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeTimePicker() {
    return core.TypeTimePicker();
  }

  @SimpleProperty(description = "Pembanding read-only: tipe Space. "
      + "Pilihan tetap, colok ke soket teks perbandingan.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeSpace() {
    return core.TypeSpace();
  }

  @SimpleProperty(description = "Pembanding read-only: tipe HorizontalArrangement. "
      + "Pilihan tetap, colok ke soket teks perbandingan.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeHorizontalArrangement() {
    return core.TypeHorizontalArrangement();
  }

  @SimpleProperty(description = "Pembanding read-only: tipe VerticalArrangement. "
      + "Pilihan tetap, colok ke soket teks perbandingan.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeVerticalArrangement() {
    return core.TypeVerticalArrangement();
  }

  @SimpleProperty(description = "Pembanding read-only: tipe HorizontalScrollArrangement. "
      + "Pilihan tetap, colok ke soket teks perbandingan.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeHorizontalScrollArrangement() {
    return core.TypeHorizontalScrollArrangement();
  }

  @SimpleProperty(description = "Pembanding read-only: tipe VerticalScrollArrangement. "
      + "Pilihan tetap, colok ke soket teks perbandingan.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeVerticalScrollArrangement() {
    return core.TypeVerticalScrollArrangement();
  }

  @SimpleFunction(description = "Lebar komponen dinamis ber-id (angka MIT: "
      + "-2 isi, -1 otomatis, positif pixel). -3 bila tidak ada/gagal.")
  public int GetDynamicWidth(int id) {
    return core.GetDynamicWidth(id);
  }

  @SimpleFunction(description = "Tinggi komponen dinamis ber-id (angka MIT). "
      + "-3 bila tidak ada/gagal.")
  public int GetDynamicHeight(int id) {
    return core.GetDynamicHeight(id);
  }

  @SimpleFunction(description = "Buat id dinamis unik baru (Number, siap pakai "
      + "ke CreateDynamicComponent).")
  public int CreateDynamicId() {
    return core.CreateDynamicId();
  }

  @SimpleFunction(description = "Semua id dinamis (URUT PEMBUATAN, "
      + "index 1 = pertama dibuat).")
  public List<Integer> GetDynamicIds() {
    return core.GetDynamicIds();
  }

  @SimpleFunction(description = "Id dinamis yang aktif (URUT PEMBUATAN).")
  public List<Integer> GetActiveDynamicIds() {
    return core.GetActiveDynamicIds();
  }

  @SimpleFunction(description = "Id dinamis terakhir dibuat. -1 bila kosong.")
  public int CurrentDynamicId() {
    return core.CurrentDynamicId();
  }

  @SimpleFunction(description = "Id index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstDynamicId() {
    return core.GetFirstDynamicId();
  }

  @SimpleFunction(description = "Id TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastDynamicId() {
    return core.GetLastDynamicId();
  }

  @SimpleFunction(description = "Id yang dibuat TEPAT SEBELUM id ini. "
      + "-1 bila tidak ada.")
  public int GetPreviousDynamicId(int id) {
    return core.GetPreviousDynamicId(id);
  }

  @SimpleFunction(description = "True bila id dinamis ini masih ada.")
  public boolean IsDynamicIdActive(int id) {
    return core.IsDynamicIdActive(id);
  }

  @SimpleFunction(description = "Hapus komponen dinamis: lepas view dari layout "
      + "+ lepas id (otomatis beres-beres).")
  public void RemoveDynamicId(int id) {
    core.RemoveDynamicId(id);
  }

  @SimpleFunction(description = "SET: Hapus SEMUA komponen dinamis Tools_Core "
      + "(id ikut dilepas).")
  public void SetClearDynamicComponents() {
    core.SetClearDynamicComponents();
  }

  @SimpleEvent(description = "Komponen dinamis jadi: id + nama tipe.")
  public void ComponentCreated(int id, String typeName) {
    EventDispatcher.dispatchEvent(this, "ComponentCreated", id, typeName);
  }

  @SimpleEvent(description = "Komponen dinamis di-klik: id. Resep daftar: "
      + "CoreClicked + GetDynamicProperty(id, \"Text\") = teks baris yang diklik.")
  public void CoreClicked(int id) {
    EventDispatcher.dispatchEvent(this, "CoreClicked", id);
  }

  @SimpleEvent(description = "Komponen dinamis di-long-klik: id.")
  public void CoreLongClicked(int id) {
    EventDispatcher.dispatchEvent(this, "CoreLongClicked", id);
  }

  @SimpleEvent(description = "Komponen dinamis di-klik 2x cepat: id.")
  public void CoreDoubleClicked(int id) {
    EventDispatcher.dispatchEvent(this, "CoreDoubleClicked", id);
  }

  @SimpleProperty(description = "Nama operasi Core (untuk if di ErrorOccurred: "
      + "cth if component = OpCore berarti error Core).",
      category = PropertyCategory.BEHAVIOR)
  public String OpCore() {
    return core.OpCore();
  }

  // ================= Downloader =================

  @SimpleFunction(description = "CALL: unduh ke folder. url = http(s) ATAU data:URL "
      + "(base64/url-encoded, mis. data:image/png;base64,...) ATAU content: URI "
      + "Android (dari file picker) ATAU file: URL / path lokal (disalin). "
      + "blob: TIDAK didukung (itu objek browser; ubah dulu jadi base64 via JavaScript, "
      + "lalu kirim sebagai data:URL atau ke CallSaveBase64ToFile Tools_File). "
      + "fileName kosong = nama otomatis (seperti CallGuessFileName). folder: boleh nama "
      + "relatif (Download/ = aman tanpa izin khusus); folder di luar itu butuh izin "
      + "MANAGE_EXTERNAL_STORAGE. Contoh aman: Download/. id dari CreateDownloadId atau "
      + "CreateUniqueId Tools_Base. Hasil lewat DownloadStarted/ProgressChanged/Finished "
      + "(bawa id). Gagal mulai (URL/id/folder salah) = Finished(success=false) + "
      + "ErrorOccurred.")
  public void CallDownload(String url, String fileName, String folder, int id) {
    downloader.CallDownload(url, fileName, folder, id);
  }

  @SimpleFunction(description = "CALL: unduh BANYAK URL sekaligus (tanpa loop manual). "
      + "urls = list teks URL (atau satu teks URL); fileNames = list nama (atau satu teks; "
      + "boleh kosong = otomatis semua; yang kurang dari jumlah URL = otomatis). "
      + "Contoh: urls = [url1, url2], fileNames = kosongkan. folder seperti CallDownload. "
      + "Tanpa return: id tiap URL ketahuan saat DownloadStarted menyala (cocokkan via "
      + "fileName/savePath yang dibawanya). Pantau tiap id lewat "
      + "DownloadStarted/DownloadProgressChanged/DownloadFinished. Ringkasan lewat "
      + "DownloadBatchFinished. Satu batch per saat. Tiap id batch bisa dipasang "
      + "SetDownloadNotification satu-satu (notif per file).")
  public void CallDownloadList(Object urls, Object fileNames, String folder) {
    downloader.CallDownloadList(urls, fileNames, folder);
  }

  @SimpleFunction(description = "SET: Batalkan unduhan berjalan. File sebagian dihapus. "
      + "Umpan balik via DownloadFinished(success=false). Tanpa event sendiri.")
  public void SetCancelDownload(int id) {
    downloader.SetCancelDownload(id);
  }

  @SimpleFunction(description = "SET: Jeda unduhan berjalan. Unduhan berhenti menulis, "
      + "lanjut dari posisi sama saat Resume (BUKAN ulang dari 0). Verifikasi via "
      + "IsDownloadPaused / GetDownloadStatus (status jadi jeda). Tanpa feedback.")
  public void SetPauseDownload(int id) {
    downloader.SetPauseDownload(id);
  }

  @SimpleFunction(description = "SET: Lanjutkan unduhan yang dijeda (lanjut dari posisi "
      + "jeda, tanpa unduh ulang). Verifikasi via IsDownloadPaused / GetDownloadStatus "
      + "(status kembali jalan). Tanpa feedback.")
  public void SetResumeDownload(int id) {
    downloader.SetResumeDownload(id);
  }

  @SimpleFunction(description = "True bila id unduhan ini sedang dijeda (Pause tapi "
      + "belum Cancel/selesai). False = jalan/selesai/gagal/batal/belum ada.")
  public boolean IsDownloadPaused(int id) {
    return downloader.IsDownloadPaused(id);
  }

  @SimpleFunction(description = "Persen unduhan id ini (0-100). = index 1 dari "
      + "GetDownloadProgress.")
  public int GetDownloadPercent(int id) {
    return downloader.GetDownloadPercent(id);
  }

  @SimpleFunction(description = "Path file tujuan id ini (kosong bila belum mulai). "
      + "= index 2 dari GetDownloadResult.")
  public String GetDownloadSavePath(int id) {
    return downloader.GetDownloadSavePath(id);
  }

  @SimpleFunction(description = "Nama file id ini (kosong bila belum mulai). "
      + "= index 3 dari GetDownloadResult.")
  public String GetDownloadFileName(int id) {
    return downloader.GetDownloadFileName(id);
  }

  @SimpleFunction(description = "Jumlah percobaan unduh id ini (tiap mulai/ulang +1, "
      + "termasuk CallRetryDownload; 0 bila id belum pernah mengunduh).")
  public int GetDownloadAttempts(int id) {
    return downloader.GetDownloadAttempts(id);
  }

  @SimpleFunction(description = "CALL: Unduh ulang id ini dari awal (byte dihitung ulang "
      + "dari 0). Untuk id yang gagal/dibatalkan/selesai. Id yang masih jalan harus "
      + "di-Cancel dulu. Hasil lewat DownloadFinished seperti biasa.")
  public void CallRetryDownload(int id) {
    downloader.CallRetryDownload(id);
  }

  @SimpleFunction(description = "Total URL batch yang sedang jalan (0 bila tidak ada batch).")
  public int GetBatchTotal() {
    return downloader.GetBatchTotal();
  }

  @SimpleFunction(description = "Jumlah URL batch yang sudah selesai (sukses/gagal).")
  public int GetBatchDone() {
    return downloader.GetBatchDone();
  }

  @SimpleFunction(description = "Jumlah URL batch yang sukses.")
  public int GetBatchOk() {
    return downloader.GetBatchOk();
  }

  @SimpleFunction(description = "Buat id unduhan unik baru (Number, siap pakai ke CallDownload). "
      + "Urutan pembuatan menentukan urutan list.")
  public int CreateDownloadId() {
    return downloader.CreateDownloadId();
  }

  @SimpleFunction(description = "Semua id unduhan yang dipegang (URUT PEMBUATAN, "
      + "index 1 = pertama dibuat). Termasuk yang dijeda.")
  public List<Integer> GetDownloadIds() {
    return downloader.GetDownloadIds();
  }

  @SimpleFunction(description = "Id unduhan yang sedang jalan (URUT PEMBUATAN). "
      + "Yang dijeda TIDAK masuk sini.")
  public List<Integer> GetActiveDownloadIds() {
    return downloader.GetActiveDownloadIds();
  }

  @SimpleFunction(description = "Id unduhan yang terakhir disentuh (dibuat atau di-start). "
      + "-1 bila kosong. Beda dengan GetLastDownloadId (terakhir dibuat).")
  public int CurrentDownloadId() {
    return downloader.CurrentDownloadId();
  }

  @SimpleFunction(description = "Id index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstDownloadId() {
    return downloader.GetFirstDownloadId();
  }

  @SimpleFunction(description = "Id unduhan TERAKHIR DIBUAT. -1 bila kosong. Beda dengan "
      + "CurrentDownloadId (terakhir disentuh).")
  public int GetLastDownloadId() {
    return downloader.GetLastDownloadId();
  }

  @SimpleFunction(description = "Id yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.")
  public int GetPreviousDownloadId(int id) {
    return downloader.GetPreviousDownloadId(id);
  }

  @SimpleFunction(description = "True bila id unduhan ini sedang jalan (tidak dijeda). "
      + "Bedakan jeda vs berhenti via IsDownloadPaused.")
  public boolean IsDownloadIdActive(int id) {
    return downloader.IsDownloadIdActive(id);
  }

  @SimpleFunction(description = "Hapus id unduhan: batalkan bila jalan + lepas id "
      + "(otomatis beres-beres).")
  public void RemoveDownloadId(int id) {
    downloader.RemoveDownloadId(id);
  }

  @SimpleFunction(description = "SET: tampilkan progres unduhan di bar notifikasi "
      + "Android (otomatis: nama file + persen). show=true = tampil + update "
      + "tiap Started/Progress/Finished; show=false = matikan + hapus notif id itu. "
      + "Pasang sesudah CallDownload, atau di dalam event DownloadStarted/"
      + "ProgressChanged (id sudah terdaftar; di Finished kurang relevan). "
      + "Tiap id hasil CallDownloadList bisa dipasang satu-satu (notif per file). "
      + "Android 13+ butuh izin POST_NOTIFICATIONS (minta via blok CallSetNotificationPermission Tools_Sheet "
      + "ATAU CallRequestPermission Tools_Base). Tanpa feedback.")
  public void SetDownloadNotification(int id, boolean show) {
    downloader.SetDownloadNotification(id, show);
  }

  @SimpleFunction(description = "Fallback polling untuk event Started/Progress/"
      + "Finished: status unduhan ber-id. Salah satu: jalan, jeda, selesai, "
      + "gagal, batal, atau tidak ada.")
  public String GetDownloadStatus(int id) {
    return downloader.GetDownloadStatus(id);
  }

  @SimpleFunction(description = "Fallback polling untuk event "
      + "DownloadProgressChanged: list [persen 0-100, byte diterima, byte total]. "
      + "Contoh ambil persen: select list item (hasil blok ini) index 1. "
      + "Jalan pintas: pakai GetDownloadPercent.")
  public List<Object> GetDownloadProgress(int id) {
    return downloader.GetDownloadProgress(id);
  }

  @SimpleFunction(description = "Fallback polling untuk event DownloadFinished: "
      + "list [success, savePath, fileName, message]. Contoh ambil nama: select list item "
      + "(hasil blok ini) index 3. Jalan pintas: GetDownloadSavePath/GetDownloadFileName. "
      + "Tetap ada setelah selesai sampai RemoveDownloadId.")
  public List<Object> GetDownloadResult(int id) {
    return downloader.GetDownloadResult(id);
  }

  @SimpleFunction(description = "Fallback polling untuk event batch: list "
      + "[batchJalan, total, selesai, ok]. Contoh ambil total: select list item "
      + "(hasil blok ini) index 2. Jalan pintas: GetBatchTotal/GetBatchDone/GetBatchOk.")
  public List<Object> GetBatchProgress() {
    return downloader.GetBatchProgress();
  }

  @SimpleFunction(description = "CALL: Tebak nama file dari URL saja (1 input, "
      + "mandiri — bisa disandingkan komponen lain, mis. Web bawaan MIT untuk "
      + "rename). http(s) = ambil header asli server (ikuti redirect); "
      + "data:/content:/file:/path = tebak lokal tanpa internet. Hasil lewat "
      + "FileNameGuessed (url + fileName + mimeType). Butuh potong path jadi nama? "
      + "pakai GetFileName Tools_File.")
  public void CallGuessFileName(final String url) {
    downloader.CallGuessFileName(url);
  }

  @SimpleEvent(description = "Hasil CallGuessFileName: url asal + fileName "
      + "tebakan + mimeType (kosong bila tak diketahui).")
  public void FileNameGuessed(String url, String fileName, String mimeType) {
    EventDispatcher.dispatchEvent(this, "FileNameGuessed", url, fileName, mimeType);
  }

  @SimpleEvent(description = "Unduhan dimulai: siapkan tampilan (nama + path tujuan).")
  public void DownloadStarted(int id, String fileName, String savePath) {
    EventDispatcher.dispatchEvent(this, "DownloadStarted", id, fileName, savePath);
  }

  @SimpleEvent(description = "Progres unduhan berubah: persen 0-100 + byte.")
  public void DownloadProgressChanged(int id, int percent, long receivedBytes, long totalBytes) {
    EventDispatcher.dispatchEvent(this, "DownloadProgressChanged", id, percent, receivedBytes, totalBytes);
  }

  @SimpleEvent(description = "Unduhan selesai: success pertama, fileName paling belakang. "
      + "success=false = gagal/dibatalkan (lihat message + ErrorOccurred).")
  public void DownloadFinished(int id, boolean success, String savePath, String fileName, String message) {
    EventDispatcher.dispatchEvent(this, "DownloadFinished", id, success, savePath, fileName, message);
  }

  @SimpleEvent(description = "Batch unduhan dimulai: total URL + folder tujuan.")
  public void DownloadBatchStarted(int total, String folder) {
    EventDispatcher.dispatchEvent(this, "DownloadBatchStarted", total, folder);
  }

  @SimpleEvent(description = "Batch unduhan selesai semua: total + jumlah ok + folder.")
  public void DownloadBatchFinished(int total, int okCount, String folder) {
    EventDispatcher.dispatchEvent(this, "DownloadBatchFinished", total, okCount, folder);
  }

  @SimpleProperty(description = "Nama operasi Download untuk if di ErrorOccurred. Contoh: "
      + "colok ke perbandingan = dengan component dari event ErrorOccurred.",
      category = PropertyCategory.BEHAVIOR)
  public String OpDownload() {
    return downloader.OpDownload();
  }

  // ================= File =================

  @SimpleFunction(description = "Apakah file ATAU folder ada di jalur "
      + "(otomatis di-root /storage/emulated/0; absolut apa adanya). "
      + "Aset (//...) bisa dicek.")
  public boolean IsFileOrDirectoryExists(String path) {
    return file.IsFileOrDirectoryExists(path);
  }

  @SimpleFunction(description = "Apakah file .nomedia ada di dalam folder. "
      + "path seperti IsFileOrDirectoryExists.")
  public boolean IsNomediaExists(String folder) {
    return file.IsNomediaExists(folder);
  }

  @SimpleFunction(description = "Apakah directory ada di jalur DAN hidden "
      + "(nama diawali titik). Boleh tulis dengan/tanpa titik. "
      + "path seperti IsFileOrDirectoryExists.")
  public boolean IsHiddenDirectory(String directory) {
    return file.IsHiddenDirectory(directory);
  }

  @SimpleFunction(description = "True bila salinan/pindahan CallCopyOrMoveFile "
      + "yang masih berjalan. Khusus operasi itu (slot copyormove).")
  public boolean IsCopyOrMoveBusy() {
    return file.IsCopyOrMoveBusy();
  }

  @SimpleFunction(description = "CALL: jadikan directory hidden (nama jadi "
      + ".nama). Sudah hidden = success. Hasil lewat DirectoryHidden.")
  public void CallSetHiddenDirectory(final String directory) {
    file.CallSetHiddenDirectory(directory);
  }

  @SimpleFunction(description = "CALL: buat file .nomedia di dalam folder "
      + "(sembunyikan dari galeri/musik). Hasil lewat NomediaFileCreated.")
  public void CallMakeNomediaFile(final String folder) {
    file.CallMakeNomediaFile(folder);
  }

  @SimpleFunction(description = "CALL: hapus file .nomedia di folder. "
      + "Tidak ada = success. Hasil lewat NomediaFileDeleted.")
  public void CallDeleteNomediaFile(final String folder) {
    file.CallDeleteNomediaFile(folder);
  }

  @SimpleFunction(description = "CALL: buat folder (termasuk induknya). "
      + "Sudah ada = success. Hasil lewat DirectoryCreated.")
  public void CallMakeDirectory(final String folder) {
    file.CallMakeDirectory(folder);
  }

  @SimpleFunction(description = "CALL: hapus file/folder (folder rekursif). "
      + "Tidak ada = success. Hasil lewat FileDeleted.")
  public void CallDeleteFile(final String path) {
    file.CallDeleteFile(path);
  }

  @SimpleFunction(description = "CALL: hapus BANYAK path sekaligus (tanpa loop "
      + "manual). paths = list, atau teks pisah koma (\"/a.jpg, /b.jpg\"; koma "
      + "boleh spasi). Hasil lewat FileListDeleted (total + terhapus).")
  public void CallDeleteList(final Object paths) {
    file.CallDeleteList(paths);
  }

  @SimpleFunction(description = "CALL: ganti nama FILE (tanpa pindah folder). "
      + "newName = nama saja. Hasil lewat FileRenamed (path = nama baru).")
  public void CallRenameFile(final String path, final String newName) {
    file.CallRenameFile(path, newName);
  }

  @SimpleFunction(description = "CALL: ganti nama FOLDER (tanpa pindah). "
      + "newName = nama saja. Hasil lewat FolderRenamed.")
  public void CallRenameFolder(final String path, final String newName) {
    file.CallRenameFolder(path, newName);
  }

  @SimpleFunction(description = "CALL: buat file kosong (folder induk dibuat "
      + "otomatis). Sudah ada = success. Hasil lewat EmptyFileCreated.")
  public void CallCreateEmptyFile(final String path) {
    file.CallCreateEmptyFile(path);
  }

  @SimpleFunction(description = "CALL: tulis teks ke file (UTF-8; timpa isi "
      + "lama). append=true = sambung di akhir (sama seperti CallAppendFile). "
      + "Hanya untuk teks; file biner rusak bila lewat sini. Hasil lewat FileSaved.")
  public void CallSaveFile(final String path, final String content, final boolean append) {
    file.CallSaveFile(path, content, append);
  }

  @SimpleFunction(description = "CALL: sambung teks di AKHIR file (UTF-8; "
      + "tidak menimpa isi lama). Sama seperti CallSaveFile(append=true). "
      + "File belum ada = dibuat baru. Hasil lewat FileSaved.")
  public void CallAppendFile(final String path, final String content) {
    file.CallAppendFile(path, content);
  }

  @SimpleFunction(description = "CALL: simpan base64 LANGSUNG ke storage "
      + "(tanpa validasi gambar, lebih cepat). Hasil lewat Base64FileSaved. "
      + "Polling: GetLastBase64FilePath.")
  public void CallSaveBase64ToFile(final String base64, final String path) {
    file.CallSaveBase64ToFile(base64, path);
  }

  @SimpleFunction(description = "Fallback polling untuk Base64FileSaved: "
      + "path file base64 terakhir (kosong bila gagal/belum ada).")
  public String GetLastBase64FilePath() {
    return file.GetLastBase64FilePath();
  }

  @SimpleFunction(description = "CALL: baca file teks (UTF-8) di background. "
      + "Hanya untuk teks UTF-8; file biner/encoding lain isinya bisa kacau "
      + "(biner = baca via base64/pustaka lain). Hasil lewat GotText "
      + "(content = isi file). Mulai lewat ReadStarted.")
  public void CallReadText(final String path) {
    file.CallReadText(path);
  }

  @SimpleFunction(description = "CALL: Salin ATAU pindah file (isMove=true = "
      + "pindah; salah centang = file pindah padahal mau salin — untuk aman pakai "
      + "CallCopyFile/CallMoveFile bila ragu). Path otomatis di-root "
      + "/storage/emulated/0. Nama tujuan boleh beda. Bila tujuan SUDAH ADA: tidak "
      + "disalin, Finished(success=false, isDuplicated=true) + ErrorOccurred. "
      + "Asset hanya bisa jadi sumber. Satu job per saat.")
  public void CallCopyOrMoveFile(final String fromFileName, final String toFileName, final boolean isMove) {
    file.CallCopyOrMoveFile(fromFileName, toFileName, isMove);
  }

  @SimpleFunction(description = "CALL: Salin file (tidak pindah). Sama seperti "
      + "CallCopyOrMoveFile(isMove=false), tapi tanpa risiko salah centang. "
      + "Bila tujuan SUDAH ADA: Finished(success=false, isDuplicated=true). "
      + "Hasil lewat CopyOrMoveFinished.")
  public void CallCopyFile(final String fromFileName, final String toFileName) {
    file.CallCopyFile(fromFileName, toFileName);
  }

  @SimpleFunction(description = "CALL: Pindah file (sumber ikut terhapus bila sukses). "
      + "Sama seperti CallCopyOrMoveFile(isMove=true), tapi tanpa risiko salah "
      + "centang. Bila tujuan SUDAH ADA: Finished(success=false, isDuplicated=true). "
      + "Hasil lewat CopyOrMoveFinished.")
  public void CallMoveFile(final String fromFileName, final String toFileName) {
    file.CallMoveFile(fromFileName, toFileName);
  }

  @SimpleFunction(description = "SET: batalkan salinan/pindahan berjalan. "
      + "Tanpa feedback (file sebagian dihapus). Rantai: CopyOrMoveFinished "
      + "(success=false, need='dibatalkan').")
  public void SetCancelCopyOrMove() {
    file.SetCancelCopyOrMove();
  }

  @SimpleFunction(description = "Nama file dari sebuah path (buang foldernya). "
      + "Olah string saja, tanpa scope. Butuh tebak nama dari URL? pakai "
      + "CallGuessFileName (Downloader).")
  public String GetFileName(String path) {
    return file.GetFileName(path);
  }

  @SimpleFunction(description = "Ekstensi tanpa titik (huruf kecil). "
      + "Cth: foto.JPG -> jpg. Olah string saja.")
  public String GetExtension(String path) {
    return file.GetExtension(path);
  }

  @SimpleFunction(description = "Folder induk dari sebuah path. "
      + "Cth: /a/b/c.jpg -> /a/b. Olah string saja.")
  public String GetParentFolder(String path) {
    return file.GetParentFolder(path);
  }

  @SimpleFunction(description = "Path absolut dari input (di-root ke "
      + "/storage/emulated/0; absolut/URL/aset apa adanya). Cth: "
      + "\"/Download/a.jpg\" -> \"/storage/emulated/0/Download/a.jpg\".")
  public String GetFullPath(String path) {
    return file.GetFullPath(path);
  }

  @SimpleFunction(description = "Ukuran file dalam byte (-1 bila tidak ada "
      + "/ gagal). Folder = total isi rekursif; folder besar = bisa lambat "
      + "(hitung rekursif, UI beku sesaat). path seperti IsFileOrDirectoryExists.")
  public long FileSize(String path) {
    return file.FileSize(path);
  }

  @SimpleFunction(description = "Waktu ubah terakhir file/folder (milidetik "
      + "epoch; 0 bila tidak ada/gagal). Bandingkan 2 file = mana yang baru. "
      + "Ubah ke tanggal pakai blok tanggal App Inventor.")
  public long GetLastModified(String path) {
    return file.GetLastModified(path);
  }

  @SimpleFunction(description = "True bila path ada DAN bisa dibaca aplikasi. "
      + "False = tidak ada, atau tidak ada izin baca (cek IsPermissionGranted "
      + "/ aktifkan 'Akses semua file').")
  public boolean IsFileReadable(String path) {
    return file.IsFileReadable(path);
  }

  @SimpleFunction(description = "Folder khusus aplikasi (tanpa izin tambahan, "
      + "aman Android 11+). Cth: /storage/emulated/0/Android/data/<paket>/"
      + "files. Hasilnya bisa langsung dipakai sebagai path blok lain. Pakai "
      + "ini agar TIDAK perlu izin storage sama sekali.")
  public String GetAndroidSpecificDirectory() {
    return file.GetAndroidSpecificDirectory();
  }

  @SimpleFunction(description = "Root kartu SD utama (/storage/emulated/0) — awal "
      + "semua path strategi baru.")
  public String GetAndroidStorageDirectory() {
    return file.GetAndroidStorageDirectory();
  }

  @SimpleFunction(description = "Root kartu SD LEPASAN (removable, mis. "
      + "/storage/XXXX-XXXX). Kosong bila tidak ada / tidak terbaca. Path SD "
      + "dipakai apa adanya (tidak di-root ulang).")
  public String GetAndroidSdCardDirectory() {
    return file.GetAndroidSdCardDirectory();
  }

  @SimpleFunction(description = "True bila kartu SD LEPASAN (removable) "
      + "terpasang dan terbaca.")
  public boolean IsSdCardExists() {
    return file.IsSdCardExists();
  }

  @SimpleFunction(description = "Path folder cache aplikasi (untuk file "
      + "sementara). Tanpa izin tambahan.")
  public String GetCacheDir() {
    return file.GetCacheDir();
  }

  @SimpleFunction(description = "Sisa ruang kosong (byte) di kartu SD "
      + "(/storage/emulated/0). -1 bila gagal dibaca.")
  public long FreeSpace() {
    return file.FreeSpace();
  }

  @SimpleFunction(description = "Total kapasitas (byte) kartu SD "
      + "(/storage/emulated/0). -1 bila gagal.")
  public long TotalSpace() {
    return file.TotalSpace();
  }

  @SimpleFunction(description = "GET: Daftar file (path lengkap). filter = "
      + "ekstensi tanpa titik (boleh koma: jpg,png; kosong = semua). "
      + "recursive = sertakan isi subfolder. path seperti IsFileOrDirectoryExists. "
      + "File besar + recursive = bisa lambat (UI beku sesaat); untuk daftar besar "
      + "pakai filter/tipe secukupnya.")
  public List<String> GetFiles(String path, String filter, boolean recursive) {
    return file.GetFiles(path, filter, recursive);
  }

  @SimpleFunction(description = "GET: Daftar subfolder (path lengkap, tanpa "
      + "rekursif). path seperti IsFileOrDirectoryExists.")
  public List<String> GetFolders(String folder) {
    return file.GetFolders(folder);
  }

  @SimpleFunction(description = "True bila NAMA PERSIS input sudah dipakai di "
      + "folder yang sama (nama.jpg vs nama.jpg = duplikat; nama (1).jpg "
      + "BUKAN duplikatnya — akhiran counter _N/(N) hanya untuk hitungan "
      + "Many). Inputnya sendiri dihitung, jadi file yang sudah ada selalu "
      + "true; gunanya untuk cek SEBELUM simpan (true = nama sudah "
      + "terpakai). Huruf besar-kecil diabaikan. Untuk banding ISI pakai "
      + "GetFileHash + blok = di if. Hanya 1 level folder, tanpa subfolder.")
  public boolean IsDuplicateFileOrDirectory(String fileOrDirectory) {
    return file.IsDuplicateFileOrDirectory(fileOrDirectory);
  }

  @SimpleFunction(description = "Berapa banyak keluarga counter nama input di "
      + "folder yang sama (nama.jpg, nama_1.jpg, nama (1).jpg, ...; termasuk "
      + "inputnya sendiri bila ada). 0 = tidak ada. Ini HITUNGAN Many — "
      + "untuk cek nama persis pakai IsDuplicateFileOrDirectory. Hanya 1 "
      + "level folder, tanpa subfolder.")
  public int GetManyDuplicateFileOrDirectory(String fileOrDirectory) {
    return file.GetManyDuplicateFileOrDirectory(fileOrDirectory);
  }

  @SimpleFunction(description = "Daftar path lengkap keluarga counter nama "
      + "input di folder yang sama, urut abjad (termasuk inputnya sendiri "
      + "bila ada; untuk cek nama persis lihat IsDuplicateFileOrDirectory). "
      + "Kosong = tidak ada; tanpa masuk subfolder.")
  public List<String> GetDuplicateFileOrDirectory(String fileOrDirectory) {
    return file.GetDuplicateFileOrDirectory(fileOrDirectory);
  }

  @SimpleFunction(description = "Hash isi file (sidik jari konten ala ZArchiver): "
      + "dua file dgn hash SAMA = isi SAMA walau nama beda (foto (3).jpg vs "
      + "foto (4).jpg). algorithm: colok Helper merah Type (dropdown MD5/SHA-1/ "
      + "SHA-224/SHA-256/SHA-384/SHA-512), atau tulis manual dgn blok teks "
      + "(cth sha256 — soketnya menerima keduanya). Bandingkan hasilnya dgn "
      + "blok = di if. Path seperti IsFileOrDirectoryExists (asset "
      + "didukung). File tidak ada/algoritma salah/baca gagal = teks kosong "
      + "+ ErrorOccurred.")
  public String GetFileHash(String path, @Options(Type.class) String algorithm) {
    return file.GetFileHash(path, algorithm);
  }

  @SimpleFunction(description = "Kategori file dari nama/path: image, audio, "
      + "video, text, archive, document, apk, atau unknown (boleh alias Indonesia "
      + "seperti GetFilesByType). Berdasarkan ekstensi (tak peduli huruf besar). "
      + "Bandingkan dengan blok TypeImage/dll. Olah string saja. Nama bisa dibohongi "
      + "(jpg padahal zip) — bila akurasi penting pakai DetectArchiveType "
      + "Tools_Compress.")
  public String FileTypeOf(String pathOrName) {
    return file.FileTypeOf(pathOrName);
  }

  @SimpleFunction(description = "GET: Daftar file (path lengkap) yang kategorinya "
      + "cocok. type = image/audio/video/text/archive/document/apk (boleh alias "
      + "Indonesia: gambar/musik/video/teks/arsip/dokumen; alias lama vidio tetap "
      + "diterima) atau salah satu blok TypeImage/dll. recursive = sertakan isi "
      + "subfolder. path seperti IsFileOrDirectoryExists. Pilih: nama bisa dibohongi "
      + "(jpg padahal zip) — bila akurasi penting pakai DetectArchiveType "
      + "Tools_Compress (baca isi); bila kecepatan penting pakai FileTypeOf "
      + "(baca nama).")
  public List<String> GetFilesByType(String folder, String type, boolean recursive) {
    return file.GetFilesByType(folder, type, recursive);
  }

  @SimpleFunction(description = "Daftar ekstensi (tanpa titik) yang masuk "
      + "kategori type. Tipe tak dikenal = list kosong + error. "
      + "Olah string saja.")
  public List<String> GetExtensionsForType(String type) {
    return file.GetExtensionsForType(type);
  }

  @SimpleProperty(description = "Pembanding read-only kategori: image.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeImage() {
    return file.TypeImage();
  }

  @SimpleProperty(description = "Pembanding read-only kategori: audio.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeAudio() {
    return file.TypeAudio();
  }

  @SimpleProperty(description = "Pembanding read-only kategori: video.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeVideo() {
    return file.TypeVideo();
  }

  @SimpleProperty(description = "Pembanding read-only kategori: text.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeText() {
    return file.TypeText();
  }

  @SimpleProperty(description = "Pembanding read-only kategori: archive.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeArchive() {
    return file.TypeArchive();
  }

  @SimpleProperty(description = "Pembanding read-only kategori: document.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeDocument() {
    return file.TypeDocument();
  }

  @SimpleProperty(description = "Pembanding read-only kategori: apk.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeApk() {
    return file.TypeApk();
  }

  @SimpleProperty(description = "Pembanding read-only kategori: unknown.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeUnknown() {
    return file.TypeUnknown();
  }

  @SimpleFunction(description = "True bila aplikasi boleh tulis SEMUA file "
      + "bersama. Android 11+ = 'Akses semua file' aktif; Android 10 ke "
      + "bawah selalu true (izin tulis biasa cukup via CallSetStoragePermission).")
  public boolean IsAllFilesAccessGranted() {
    return file.IsAllFilesAccessGranted();
  }

  @SimpleFunction(description = "Buka layar Settings 'Akses semua file' "
      + "untuk aplikasi ini (Android 11+). WAJIB agar tulis ke "
      + "/storage/emulated/0 & SD card jalan. Setelah kembali, cek lewat "
      + "IsAllFilesAccessGranted. Tanpa feedback.")
  public void OpenAllFilesAccessSettings() {
    file.OpenAllFilesAccessSettings();
  }

  @SimpleFunction(description = "True bila izin tulis storage sudah beres "
      + "(tanpa input apa pun). Android 11+ = 'Akses semua file' aktif; "
      + "Android 10 ke bawah = izin tulis granted.")
  public boolean IsStoragePermissionGranted() {
    return file.IsStoragePermissionGranted();
  }

  @SimpleFunction(description = "CALL: SATU PINTU izin File (spesifik storage, "
      + "tanpa soket). Pintar OS: sudah beres = langsung GotPermission(true) "
      + "tanpa dialog; Android 11+ belum = OTOMATIS buka Settings + "
      + "GotPermission(false + pesan); lainnya = dialog izin (end-user yang "
      + "mutusin) + GotPermission. permanentlyDenied=false = user menolak "
      + "(boleh tanya lagi); true = permanen (aktifkan manual di Setelan). "
      + "Tanpa return.")
  public void CallSetStoragePermission() {
    file.CallSetStoragePermission();
  }

  @SimpleFunction(description = "Buka pemilih file. mimeType contoh: image/* "
      + "(kosong = semua). Hasil lewat AfterFilePicked (uri + path + nama "
      + "file). Satu picker per saat.")
  public void OpenFilePicker(String mimeType) {
    file.OpenFilePicker(mimeType);
  }

  @SimpleFunction(description = "Buka file dengan aplikasi lain (musik, "
      + "gambar, PDF, ...). path seperti IsFileOrDirectoryExists. Hasil lewat "
      + "AfterOpenFile. Asset disalin ke cache dulu.")
  public void OpenFile(String path) {
    file.OpenFile(path);
  }

  @SimpleEvent(description = "Hasil CallSetHiddenDirectory: success pertama, path + "
      + "message + needPermission (isi = izin yang harus diminta bila "
      + "gagal karena izin).")
  public void DirectoryHidden(boolean success, String path, String message, String needPermission) {
    EventDispatcher.dispatchEvent(this, "DirectoryHidden", success, path, message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallMakeNomediaFile: success pertama, path + "
      + "message + needPermission.")
  public void NomediaFileCreated(boolean success, String path, String message, String needPermission) {
    EventDispatcher.dispatchEvent(this, "NomediaFileCreated", success, path, message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallDeleteNomediaFile: success pertama, path + "
      + "message + needPermission.")
  public void NomediaFileDeleted(boolean success, String path, String message, String needPermission) {
    EventDispatcher.dispatchEvent(this, "NomediaFileDeleted", success, path, message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallMakeDirectory: success pertama, path + "
      + "message + needPermission.")
  public void DirectoryCreated(boolean success, String path, String message, String needPermission) {
    EventDispatcher.dispatchEvent(this, "DirectoryCreated", success, path, message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallDeleteFile: success pertama, path + "
      + "message + needPermission.")
  public void FileDeleted(boolean success, String path, String message, String needPermission) {
    EventDispatcher.dispatchEvent(this, "FileDeleted", success, path, message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallDeleteList: success + total + "
      + "terhapus + message + needPermission.")
  public void FileListDeleted(boolean success, int total, int deleted, String message, String needPermission) {
    EventDispatcher.dispatchEvent(this, "FileListDeleted", success, total, deleted, message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallRenameFile: success pertama, path (nama baru) + "
      + "message + needPermission.")
  public void FileRenamed(boolean success, String path, String message, String needPermission) {
    EventDispatcher.dispatchEvent(this, "FileRenamed", success, path, message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallRenameFolder: success pertama, path (nama baru) + "
      + "message + needPermission.")
  public void FolderRenamed(boolean success, String path, String message, String needPermission) {
    EventDispatcher.dispatchEvent(this, "FolderRenamed", success, path, message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallCreateEmptyFile: success pertama, path + "
      + "message + needPermission.")
  public void EmptyFileCreated(boolean success, String path, String message, String needPermission) {
    EventDispatcher.dispatchEvent(this, "EmptyFileCreated", success, path, message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallSaveFile: success pertama, path + "
      + "message + needPermission.")
  public void FileSaved(boolean success, String path, String message, String needPermission) {
    EventDispatcher.dispatchEvent(this, "FileSaved", success, path, message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallSaveBase64ToFile: success pertama, path + "
      + "message + needPermission.")
  public void Base64FileSaved(boolean success, String path, String message, String needPermission) {
    EventDispatcher.dispatchEvent(this, "Base64FileSaved", success, path, message, needPermission);
  }

  @SimpleEvent(description = "Bacaan dimulai (CallReadText): persiapan "
      + "(tampilkan loading).")
  public void ReadStarted(String path) {
    EventDispatcher.dispatchEvent(this, "ReadStarted", path);
  }

  @SimpleEvent(description = "Hasil CallReadText: success pertama, path "
      + "absolut, fileName paling belakang, content = isi file.")
  public void GotText(boolean success, String path, String fileName, String content) {
    EventDispatcher.dispatchEvent(this, "GotText", success, path, fileName, content);
  }

  @SimpleEvent(description = "Salin/pindah dimulai: path sumber + tujuan + "
      + "nama file + isMoved.")
  public void CopyOrMoveStarted(String fromPath, String toPath, String fileName, boolean isMoved) {
    EventDispatcher.dispatchEvent(this, "CopyOrMoveStarted", fromPath, toPath, fileName, isMoved);
  }

  @SimpleEvent(description = "Progres salin/pindah: persen 0-100 + byte "
      + "total & berjalan.")
  public void CopyOrMoveProgressChanged(int progress, long byteTotal, long byteProgress) {
    EventDispatcher.dispatchEvent(this, "CopyOrMoveProgressChanged", progress, byteTotal, byteProgress);
  }

  @SimpleEvent(description = "Salin/pindah selesai: success + path + nama "
      + "file + isDuplicated (true = tujuan sudah ada, tidak ditimpa, "
      + "rename manual).")
  public void CopyOrMoveFinished(boolean success, String path, String fileName, boolean isDuplicated) {
    EventDispatcher.dispatchEvent(this, "CopyOrMoveFinished", success, path, fileName, isDuplicated);
  }

  @SimpleEvent(description = "Hasil OpenFilePicker: success pertama, uri, "
      + "path absolut, fileName paling belakang. success=false = "
      + "dibatalkan user.")
  public void AfterFilePicked(boolean success, String uri, String path, String fileName) {
    EventDispatcher.dispatchEvent(this, "AfterFilePicked", success, uri, path, fileName);
  }

  @SimpleEvent(description = "Hasil OpenFile: success + pesan.")
  public void AfterOpenFile(boolean success, String message) {
    EventDispatcher.dispatchEvent(this, "AfterOpenFile", success, message);
  }

  @SimpleProperty(description = "Nama operasi direktori (untuk if di "
      + "ErrorOccurred).", category = PropertyCategory.BEHAVIOR)
  public String OpDirectory() {
    return file.OpDirectory();
  }

  @SimpleProperty(description = "Nama operasi File umum (untuk if di "
      + "ErrorOccurred).", category = PropertyCategory.BEHAVIOR)
  public String OpFile() {
    return file.OpFile();
  }

  @SimpleProperty(description = "Nama operasi Read (untuk if di "
      + "ErrorOccurred).", category = PropertyCategory.BEHAVIOR)
  public String OpRead() {
    return file.OpRead();
  }

  @SimpleProperty(description = "Nama operasi Base64 (untuk if di "
      + "ErrorOccurred).", category = PropertyCategory.BEHAVIOR)
  public String OpBase64() {
    return file.OpBase64();
  }

  @SimpleProperty(description = "Nama operasi salin/pindah (untuk if di "
      + "ErrorOccurred).", category = PropertyCategory.BEHAVIOR)
  public String OpCopyOrMove() {
    return file.OpCopyOrMove();
  }

  @SimpleProperty(description = "Nama operasi Pick (untuk if di "
      + "ErrorOccurred).", category = PropertyCategory.BEHAVIOR)
  public String OpPick() {
    return file.OpPick();
  }

  @SimpleProperty(description = "Nama operasi Open (untuk if di "
      + "ErrorOccurred).", category = PropertyCategory.BEHAVIOR)
  public String OpOpen() {
    return file.OpOpen();
  }

  // ================= Image =================

  @SimpleFunction(description = "CALL: tampilkan gambar ber-id ke layout (Arrangement MIT / dinamis). "
      + "SATU pintu input: path file/asset, file:/// absolut, http(s) URL "
      + "(diunduh sekali lalu di-cache otomatis — tampil ulang = tanpa unduh), "
      + "base64 (mentah / data:...), atau content:// — "
      + "terdeteksi otomatis. Bisa jalan bareng (satu id = satu gambar). "
      + "id = penanda (minta ke CreateImageId / "
      + "Tools_Base CreateUniqueId, reservasi otomatis diadopsi). "
      + "width/height angka MIT: -2 = isi penuh, -1 = otomatis, positif = pixel. "
      + "fit true = seluruh gambar muat (tidak kepotong); false = penuhi tampilan "
      + "(bisa kepotong). Catatan kamera: foto hasil kamera bisa tampil miring "
      + "(orientasi Exif belum dibaca). Sukses menembak "
      + "ImageShown; gagal via ErrorOccurred.")
  public void CallShowImage(final AndroidViewComponent container, final String source, final int id, final int width, final int height, final boolean fit) {
    image.CallShowImage(container, source, id, width, height, fit);
  }

  @SimpleFunction(description = "SET: hapus SEMUA gambar Tools_Image dari layout itu "
      + "(komponen lain tidak diusik, id ikut dilepas). Tanpa feedback.")
  public void SetClearImage(final AndroidViewComponent container) {
    image.SetClearImage(container);
  }

  @SimpleFunction(description = "SET: Aktif/matikan cubit-zoom (pinch) + geser "
      + "gambar ber-id (mode cubit-geser; tap biasa tetap dihitung klik). "
      + "Perubahan skala menembak ZoomChanged; baca kapan saja "
      + "via GetZoom. Sukses diam; id tidak ada via ErrorOccurred.")
  public void SetZoomEnabled(final int id, final boolean enabled) {
    image.SetZoomEnabled(id, enabled);
  }

  @SimpleFunction(description = "Fallback polling untuk event ZoomChanged: "
      + "skala zoom gambar ber-id (1.0 = pas).")
  public float GetZoom(int id) {
    return image.GetZoom(id);
  }

  @SimpleFunction(description = "SET: kembalikan zoom gambar ber-id ke pas (1.0). Tanpa feedback.")
  public void SetResetZoom(final int id) {
    image.SetResetZoom(id);
  }

  @SimpleFunction(description = "SET: atur lebar + tinggi gambar ber-id. Angka "
      + "SAMA seperti blok bawaan MIT: -2 = isi penuh, -1 = otomatis, positif "
      + "= pixel. Persen tidak didukung blok ini; resep setengah layar: bungkus "
      + "dalam 2 Arrangement sejajar (masing-masing -2 = terbagi rata), gambar di "
      + "dalamnya -2. Sukses diam; id tidak ada via ErrorOccurred.")
  public void SetImageSize(final int id, final int width, final int height) {
    image.SetImageSize(id, width, height);
  }

  @SimpleFunction(description = "SET: tampil/sembunyi gambar ber-id. "
      + "false = GONE (hilang total). Sukses diam; id tidak ada via ErrorOccurred.")
  public void SetImageVisible(final int id, final boolean visible) {
    image.SetImageVisible(id, visible);
  }

  @SimpleFunction(description = "SET: mode pas gambar ber-id. fit true = "
      + "seluruh gambar muat di dalam (tidak kepotong); false = penuhi "
      + "tampilan (bisa kepotong). Mematikan zoom bila sedang aktif. "
      + "Sukses diam; id tidak ada via ErrorOccurred.")
  public void SetImageFit(final int id, final boolean fit) {
    image.SetImageFit(id, fit);
  }

  @SimpleFunction(description = "SET: ganti gambar ber-id yang SUDAH tampil "
      + "dengan sumber baru. SATU pintu input sama seperti CallShowImage: "
      + "path file/asset, file:/// absolut, http(s) URL (di-cache otomatis), "
      + "base64 (mentah / data:...), atau content:// — terdeteksi otomatis. "
      + "View, ukuran & mode fit TETAP (tidak dibuat ulang); zoom bila aktif "
      + "di-reset ke pas. Sukses menembak ImageShown; gagal via ErrorOccurred "
      + "(gambar lama tetap tampil).")
  public void SetImageSource(final int id, final String source) {
    image.SetImageSource(id, source);
  }

  @SimpleFunction(description = "Lebar gambar ber-id (angka MIT: -2 isi, "
      + "-1 otomatis, positif pixel). -3 bila id tidak ada. PENERJEMAH SATUAN: "
      + "untuk dipasang ke SetImageSize pakai blok ini (satuan MIT); GetImageSize "
      + "(pixel) hanya untuk hitung-hitungan.")
  public int GetImageWidth(int id) {
    return image.GetImageWidth(id);
  }

  @SimpleFunction(description = "Tinggi gambar ber-id (angka MIT). "
      + "-3 bila id tidak ada. Lihat penerjemah satuan di GetImageWidth.")
  public int GetImageHeight(int id) {
    return image.GetImageHeight(id);
  }

  @SimpleFunction(description = "True bila gambar ber-id sedang tampil "
      + "(visible). False bila tidak ada / sedang GONE.")
  public boolean IsImageVisible(int id) {
    return image.IsImageVisible(id);
  }

  @SimpleFunction(description = "True bila gambar ber-id masih loading (id aktif "
      + "tapi belum tampil: masih unduh/decode). False bila sudah tampil, gagal, "
      + "atau id tidak ada. Pasangan ImageShown.")
  public boolean IsImageLoading(int id) {
    return image.IsImageLoading(id);
  }

  @SimpleFunction(description = "CALL: bikin slider gambar ber-id (geser kanan-kiri "
      + "bila horizontal=true, atas-bawah bila false). paths = list gambar langsung "
      + "(path file/asset, URL http, atau base64; boleh list kosong = isi belakangan "
      + "via SetSliderImages). TANPA batas jumlah (dulu maks 20); tiap gambar "
      + "dikecilkan otomatis bila > 2048px, tapi puluhan gambar besar tetap makan "
      + "RAM — sesuaikan. URL diunduh sekali lalu di-cache (tak perlu unduh ulang; "
      + "bersihkan via SetClearImageCache). width/height = ukuran WADAH slider di "
      + "dalam container (angka MIT: -2 = isi penuh, -1 = otomatis, positif = "
      + "pixel); BUKAN ukuran isi gambar — tiap halaman menyesuaikan wadah. "
      + "fit true = gambar muat penuh (tidak kepotong); false = penuhi halaman "
      + "(bisa kepotong). INGAT: halaman slider mulai dari 0 "
      + "(bukan 1 seperti daftar id). Geser menembak "
      + "SliderChanged; baca kapan saja via GetSliderIndex.")
  public void CallCreateImageSlider(final AndroidViewComponent container, final int id, final boolean horizontal, final Object paths, final int width, final int height, final boolean fit) {
    image.CallCreateImageSlider(container, id, horizontal, paths, width, height, fit);
  }

  @SimpleFunction(description = "SET: isi slider ber-id dengan daftar gambar "
      + "(list teks: path file/asset, URL http, atau base64 — sama fleksibelnya "
      + "dengan ShowImage; path dipakai bila item kosong). TANPA batas jumlah. "
      + "Ukuran wadah & mode fit ikut saat pembuatan slider. Mode jendela "
      + "(SetSliderPreload): halaman dibuat dulu, decode-nya menyusul otomatis. "
      + "URL diunduh sekali lalu di-cache (tak perlu unduh ulang). "
      + "List kosong / slider tidak ada = ErrorOccurred. Gagal via ErrorOccurred.")
  public void SetSliderImages(final int sliderId, final Object paths) {
    image.SetSliderImages(sliderId, paths);
  }

  @SimpleFunction(description = "SET: geser slider ke halaman berikut (berputar). "
      + "Memicu SliderChanged bila halaman berubah (bukan tanpa feedback).")
  public void SetSliderNext(final int sliderId) {
    image.SetSliderNext(sliderId);
  }

  @SimpleFunction(description = "SET: geser slider ke halaman sebelum (berputar). "
      + "Memicu SliderChanged bila halaman berubah (bukan tanpa feedback).")
  public void SetSliderPrevious(final int sliderId) {
    image.SetSliderPrevious(sliderId);
  }

  @SimpleFunction(description = "SET: lompat ke halaman index (INGAT: 0 = pertama, "
      + "bukan 1). Memicu SliderChanged bila halaman berubah.")
  public void SetSliderGoTo(final int sliderId, final int index) {
    image.SetSliderGoTo(sliderId, index);
  }

  @SimpleFunction(description = "SET: tampil/sembunyi slider ber-id yang SUDAH "
      + "aktif. false = GONE (hilang total; wadah MIT bisa diisi konten lain "
      + "tanpa ketimpa, tanpa bikin layout baru). Sukses diam; id tidak ada "
      + "via ErrorOccurred.")
  public void SetSliderVisible(final int sliderId, final boolean visible) {
    image.SetSliderVisible(sliderId, visible);
  }

  @SimpleFunction(description = "SET: ubah ukuran WADAH slider ber-id yang SUDAH "
      + "aktif (angka MIT: -2 = isi penuh, -1 = otomatis, positif = pixel). "
      + "Isi tiap halaman menyesuaikan ulang. Sukses diam; id tidak ada via "
      + "ErrorOccurred.")
  public void SetSliderSize(final int sliderId, final int width, final int height) {
    image.SetSliderSize(sliderId, width, height);
  }

  @SimpleFunction(description = "SET: mode pas slider ber-id yang SUDAH aktif. "
      + "fit true = tiap gambar muat penuh di halamannya (tidak kepotong); "
      + "false = penuhi halaman (bisa kepotong). Sukses diam; id tidak ada via "
      + "ErrorOccurred.")
  public void SetSliderFit(final int sliderId, final boolean fit) {
    image.SetSliderFit(sliderId, fit);
  }

  @SimpleFunction(description = "SET: jarak antar halaman slider ber-id (pixel; "
      + "0 = rapat). Horizontal = jarak kiri-kanan, vertikal = jarak atas-bawah. "
      + "Paging geser ikut menyesuaikan. Area sela transparan (wadah di belakangnya "
      + "yang terlihat). Sukses diam; id tidak ada via ErrorOccurred.")
  public void SetSliderSpacing(final int sliderId, final int spacingPx) {
    image.SetSliderSpacing(sliderId, spacingPx);
  }

  @SimpleFunction(description = "SET: pilih/fokus halaman slider (0 = pertama). "
      + "Yang terpilih = berbayang + layar digeser ke sana (SliderChanged ikut "
      + "menembak). -1 = lepas pilihan. Di luar rentang via ErrorOccurred. "
      + "Dipakai misal thumbnail ditekan -> tampilkan di ShowImage + fokus di sini.")
  public void SetSliderSelected(final int sliderId, final int index) {
    image.SetSliderSelected(sliderId, index);
  }

  @SimpleFunction(description = "SET: mode hemat RAM slider (jendela preload). "
      + "jumlah = tetangga yang ikut di-decode di sekitar halaman aktif "
      + "(mis. 5 = aktif±5); sisanya dilepas dari RAM (byte-nya tetap aman di "
      + "cache disk, muat ulang murah). Pompa OTOMATIS tiap geser/GoTo/Next/"
      + "Previous/Selected/autoplay. 0 = mati (semua decode sekaligus seperti "
      + "dulu; hati-hati OOM bila puluhan gambar besar). Sukses diam; id tidak "
      + "ada via ErrorOccurred.")
  public void SetSliderPreload(final int sliderId, final int jumlah) {
    image.SetSliderPreload(sliderId, jumlah);
  }

  @SimpleFunction(description = "SET: ingat posisi slider (true = tiap pindah "
      + "halaman, index disimpan; habis isi slider otomatis lompat ke simpanan). "
      + "Hanya index yang persist (panggil tiap launch); daftar gambar tetap "
      + "dari SetSliderImages. Sukses diam; id tidak ada via ErrorOccurred.")
  public void SetSliderRememberPosition(final int sliderId, final boolean remember) {
    image.SetSliderRememberPosition(sliderId, remember);
  }

  @SimpleFunction(description = "SET: Auto-putar slider tiap intervalMs "
      + "(0 = mati). Tiap putaran memicu SliderChanged.")
  public void SetSliderAutoPlay(final int sliderId, final long intervalMs) {
    image.SetSliderAutoPlay(sliderId, intervalMs);
  }

  @SimpleFunction(description = "Fallback polling untuk event SliderChanged: "
      + "index halaman aktif (INGAT: 0 = pertama, bukan 1). -1 bila slider tidak ada.")
  public int GetSliderIndex(int sliderId) {
    return image.GetSliderIndex(sliderId);
  }

  @SimpleFunction(description = "Jumlah halaman slider ber-id (0 bila slider tidak ada).")
  public int GetSliderCount(int sliderId) {
    return image.GetSliderCount(sliderId);
  }

  @SimpleFunction(description = "Lebar WADAH slider ber-id (angka MIT: -2 = isi "
      + "penuh, -1 = otomatis, positif = pixel). -1 bila slider tidak ada. "
      + "Pasangan SetSliderSize.")
  public int GetSliderWidth(int sliderId) {
    return image.GetSliderWidth(sliderId);
  }

  @SimpleFunction(description = "Tinggi WADAH slider ber-id (angka MIT: -2 = isi "
      + "penuh, -1 = otomatis, positif = pixel). -1 bila slider tidak ada. "
      + "Pasangan SetSliderSize.")
  public int GetSliderHeight(int sliderId) {
    return image.GetSliderHeight(sliderId);
  }

  @SimpleFunction(description = "Mode pas slider ber-id (true = muat penuh; false "
      + "= penuhi halaman / slider tidak ada). Pasangan SetSliderFit.")
  public boolean GetSliderFit(int sliderId) {
    return image.GetSliderFit(sliderId);
  }

  @SimpleFunction(description = "Jarak antar halaman slider ber-id (pixel; -1 bila "
      + "slider tidak ada). Pasangan SetSliderSpacing.")
  public int GetSliderSpacing(int sliderId) {
    return image.GetSliderSpacing(sliderId);
  }

  @SimpleFunction(description = "True bila slider ber-id tampil (visible). False "
      + "bila GONE / tidak ada. Pasangan SetSliderVisible.")
  public boolean IsSliderVisible(int sliderId) {
    return image.IsSliderVisible(sliderId);
  }

  @SimpleFunction(description = "Index halaman slider yang terpilih/fokus "
      + "(-1 = tidak ada pilihan / slider tidak ada). Pasangan SetSliderSelected.")
  public int GetSliderSelected(int sliderId) {
    return image.GetSliderSelected(sliderId);
  }

  @SimpleFunction(description = "Sumber gambar halaman slider (path/URL/base64 "
      + "seperti saat dimasukkan; \"\" bila slider/index tidak ada). Pasangan "
      + "SliderClicked: dari index yang ditekan ambil sumbernya untuk "
      + "ditampilkan di ShowImage (SetImageSource).")
  public String GetSliderImage(int sliderId, int index) {
    return image.GetSliderImage(sliderId, index);
  }

  @SimpleFunction(description = "Jumlah gambar yang GAGAL dimuat pada pengisian "
      + "slider terakhir (mode jendela: terakumulasi tiap halaman dimuat; 0 = "
      + "semua ok / slider tidak ada). Pasangan GetSliderCount.")
  public int GetSliderFailed(int sliderId) {
    return image.GetSliderFailed(sliderId);
  }

  @SimpleFunction(description = "Jendela preload slider (tetangga aktif±N; 0 = "
      + "mati/semua sekaligus; -1 bila slider tidak ada). Pasangan SetSliderPreload.")
  public int GetSliderPreload(int sliderId) {
    return image.GetSliderPreload(sliderId);
  }

  @SimpleFunction(description = "True bila slider mengingat posisi (index "
      + "persist). False bila mati / tidak ada. Pasangan SetSliderRememberPosition.")
  public boolean IsSliderRememberPosition(int sliderId) {
    return image.IsSliderRememberPosition(sliderId);
  }

  @SimpleFunction(description = "True bila autoplay slider ber-id sedang jalan "
      + "(interval > 0 dan belum dimatikan).")
  public boolean IsSliderAutoPlay(int sliderId) {
    return image.IsSliderAutoPlay(sliderId);
  }

  @SimpleFunction(description = "Interval autoplay slider ber-id (milidetik; "
      + "0 = mati / slider tidak ada).")
  public long GetSliderInterval(int sliderId) {
    return image.GetSliderInterval(sliderId);
  }

  @SimpleFunction(description = "Buat id gambar unik baru (Number, siap pakai ke CallShowImage).")
  public int CreateImageId() {
    return image.CreateImageId();
  }

  @SimpleFunction(description = "Semua id gambar yang tampil (URUT PEMBUATAN, "
      + "index 1 = pertama dibuat). Beda dengan GetActiveImageIds: yang ini HANYA "
      + "yang sudah tampil; Aktif = termasuk yang masih loading.")
  public List<Integer> GetImageIds() {
    return image.GetImageIds();
  }

  @SimpleFunction(description = "Jumlah gambar ber-id yang sedang tampil "
      + "(0 bila tidak ada).")
  public int GetImageCount() {
    return image.GetImageCount();
  }

  @SimpleFunction(description = "Id gambar yang sedang tampil/loading (URUT PEMBUATAN). "
      + "Beda dengan GetImageIds: yang ini termasuk yang masih loading; Ids = "
      + "hanya yang sudah tampil.")
  public List<Integer> GetActiveImageIds() {
    return image.GetActiveImageIds();
  }

  @SimpleFunction(description = "Id gambar yang terakhir dibuat/di-start. -1 bila kosong.")
  public int CurrentImageId() {
    return image.CurrentImageId();
  }

  @SimpleFunction(description = "Id index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstImageId() {
    return image.GetFirstImageId();
  }

  @SimpleFunction(description = "Id TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastImageId() {
    return image.GetLastImageId();
  }

  @SimpleFunction(description = "Id yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.")
  public int GetPreviousImageId(int id) {
    return image.GetPreviousImageId(id);
  }

  @SimpleFunction(description = "True bila id gambar ini sedang tampil/loading.")
  public boolean IsImageIdActive(int id) {
    return image.IsImageIdActive(id);
  }

  @SimpleFunction(description = "Hapus id gambar: lepas gambar dari layout + lepas id "
      + "(otomatis beres-beres).")
  public void RemoveImageId(int id) {
    image.RemoveImageId(id);
  }

  @SimpleFunction(description = "Buat id slider unik baru (Number, siap pakai "
      + "ke CallCreateImageSlider).")
  public int CreateSliderId() {
    return image.CreateSliderId();
  }

  @SimpleFunction(description = "Semua id slider (URUT PEMBUATAN, "
      + "index 1 = pertama dibuat).")
  public List<Integer> GetSliderIds() {
    return image.GetSliderIds();
  }

  @SimpleFunction(description = "Id slider yang aktif (URUT PEMBUATAN).")
  public List<Integer> GetActiveSliderIds() {
    return image.GetActiveSliderIds();
  }

  @SimpleFunction(description = "Id slider terakhir dibuat. -1 bila kosong.")
  public int CurrentSliderId() {
    return image.CurrentSliderId();
  }

  @SimpleFunction(description = "Id index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstSliderId() {
    return image.GetFirstSliderId();
  }

  @SimpleFunction(description = "Id TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastSliderId() {
    return image.GetLastSliderId();
  }

  @SimpleFunction(description = "Id yang dibuat TEPAT SEBELUM id ini. "
      + "-1 bila tidak ada.")
  public int GetPreviousSliderId(int id) {
    return image.GetPreviousSliderId(id);
  }

  @SimpleFunction(description = "True bila id slider ini masih ada.")
  public boolean IsSliderIdActive(int id) {
    return image.IsSliderIdActive(id);
  }

  @SimpleFunction(description = "Hapus slider: lepas dari layout + lepas id "
      + "(otomatis beres-beres, autoplay mati).")
  public void RemoveSliderId(int sliderId) {
    image.RemoveSliderId(sliderId);
  }

  @SimpleFunction(description = "Fallback polling untuk event "
      + "AfterPictureBase64Ready: path gambar terakhir yang jadi "
      + "(kosong bila belum ada).")
  public String GetLastPicturePath() {
    return image.GetLastPicturePath();
  }

  @SimpleFunction(description = "CALL: decode base64 jadi gambar di cache. Path hasil langsung bisa "
      + "dipasang ke Image.Picture. Hasil lewat AfterPictureBase64Ready. "
      + "Satu proses per saat: panggilan kedua saat sibuk = DITOLAK "
      + "(AfterPictureBase64Ready success=false + ErrorOccurred).")
  public void CallBase64ToPicture(final String base64) {
    image.CallBase64ToPicture(base64);
  }

  @SimpleFunction(description = "Lebar dan tinggi gambar (pixel). SATU pintu: "
      + "path file/asset, http(s) URL (pakai cache bila pernah diunduh), base64, "
      + "atau content:// — terdeteksi "
      + "otomatis. Kembali: list [lebar, tinggi] (index 1 = lebar, 2 = tinggi). "
      + "[0, 0] bila bukan gambar / tidak ada. Pixel hanya untuk hitung-hitungan; "
      + "untuk dipasang ke SetImageSize pakai GetImageWidth/Height (satuan MIT).")
  public List<Integer> GetImageSize(String path) {
    return image.GetImageSize(path);
  }

  @SimpleFunction(description = "SET: hapus SEMUA cache unduhan URL gambar "
      + "(hemat unduh-ulang; batas atur via SetImageCacheMaxMB, terlama "
      + "terhapus otomatis). Pakai ini "
      + "bila gambar URL berubah tapi alamatnya sama (paksa unduh baru). "
      + "Sukses diam; gagal via ErrorOccurred.")
  public void SetClearImageCache() {
    image.SetClearImageCache();
  }

  @SimpleFunction(description = "SET: batas cache unduhan URL gambar (MB, "
      + "default 80). Di bawah 1 = kembali ke 80. Bila dikecilkan, kelebihan "
      + "langsung dipangkas (terlama dulu). Sukses diam; gagal via "
      + "ErrorOccurred.")
  public void SetImageCacheMaxMB(int mb) {
    image.SetImageCacheMaxMB(mb);
  }

  @SimpleFunction(description = "Batas cache unduhan URL gambar sekarang (MB, "
      + "default 80).")
  public int GetImageCacheMaxMB() {
    return image.GetImageCacheMaxMB();
  }

  @SimpleEvent(description = "Hasil CallShowImage/SetImageSource: gambar ber-id tampil. "
      + "id + path YANG JADI (URL/base64/content -> file cache; asset/file -> path itu; "
      + "penting untuk simpan/bagi gambar).")
  public void ImageShown(int id, String path) {
    EventDispatcher.dispatchEvent(this, "ImageShown", id, path);
  }

  @SimpleEvent(description = "Gambar ber-id di-klik: id.")
  public void ImageClicked(int id) {
    EventDispatcher.dispatchEvent(this, "ImageClicked", id);
  }

  @SimpleEvent(description = "Gambar ber-id di-long-klik: id.")
  public void ImageLongClicked(int id) {
    EventDispatcher.dispatchEvent(this, "ImageLongClicked", id);
  }

  @SimpleEvent(description = "Skala zoom gambar berubah (akhir gesture): "
      + "id + skala (1.0 = pas). Fallback: GetZoom.")
  public void ZoomChanged(int id, float scale) {
    EventDispatcher.dispatchEvent(this, "ZoomChanged", id, scale);
  }

  @SimpleEvent(description = "Slider jadi: id slider + arah (true = kanan-kiri).")
  public void SliderCreated(int sliderId, boolean horizontal) {
    EventDispatcher.dispatchEvent(this, "SliderCreated", sliderId, horizontal);
  }

  @SimpleEvent(description = "Halaman slider pindah: id slider + index "
      + "(INGAT: 0 = pertama, bukan 1). Dipicu geser manual, GoTo/Next/Previous, "
      + "dan autoplay. Fallback: GetSliderIndex.")
  public void SliderChanged(int sliderId, int index) {
    EventDispatcher.dispatchEvent(this, "SliderChanged", sliderId, index);
  }

  @SimpleEvent(description = "Halaman slider di-klik: id slider + index (0 = pertama).")
  public void SliderClicked(int sliderId, int index) {
    EventDispatcher.dispatchEvent(this, "SliderClicked", sliderId, index);
  }

  @SimpleEvent(description = "Proses base64-gambar dimulai (tanpa parameter "
      + "karena antrean serial: satu proses per saat, id tidak perlu).")
  public void PictureStarted() {
    EventDispatcher.dispatchEvent(this, "PictureStarted");
  }

  @SimpleEvent(description = "Hasil CallBase64ToPicture: success pertama, path absolut "
      + "(cache), fileName paling belakang. Path siap ke Image.Picture.")
  public void AfterPictureBase64Ready(boolean success, String path, String fileName, String message) {
    EventDispatcher.dispatchEvent(this, "AfterPictureBase64Ready", success, path, fileName, message);
  }

  @SimpleProperty(description = "Nama operasi Slider (untuk if di ErrorOccurred: "
      + "cth if component = OpSlider berarti error slider).",
      category = PropertyCategory.BEHAVIOR)
  public String OpSlider() {
    return image.OpSlider();
  }

  @SimpleProperty(description = "Nama operasi Image (untuk if di ErrorOccurred: "
      + "cth if component = OpImage berarti error gambar).",
      category = PropertyCategory.BEHAVIOR)
  public String OpImage() {
    return image.OpImage();
  }

  // ================= Layout =================

  @SimpleFunction(description = "CALL: Bikin VerticalArrangement dinamis ber-id di "
      + "dalam layout induk. width/height angka MIT (-2 isi, -1 otomatis, "
      + "positif pixel); visible false = GONE. Sukses menembak LayoutCreated; "
      + "gagal via ErrorOccurred.")
  public void CallCreateVerticalLayout(AndroidViewComponent container, int id, int width, int height, boolean visible, int backgroundColor) {
    layout.CallCreateVerticalLayout(container, id, width, height, visible, backgroundColor);
  }

  @SimpleFunction(description = "CALL: Bikin HorizontalArrangement dinamis ber-id di "
      + "dalam layout induk. width/height angka MIT (-2 isi, -1 otomatis, "
      + "positif pixel); visible false = GONE. Sukses menembak LayoutCreated; "
      + "gagal via ErrorOccurred.")
  public void CallCreateHorizontalLayout(AndroidViewComponent container, int id, int width, int height, boolean visible, int backgroundColor) {
    layout.CallCreateHorizontalLayout(container, id, width, height, visible, backgroundColor);
  }

  @SimpleFunction(description = "CALL: Bikin VerticalScrollArrangement dinamis "
      + "ber-id (bisa scroll atas-bawah) di dalam layout induk. width/height "
      + "angka MIT; visible false = GONE. Sukses menembak LayoutCreated; gagal "
      + "via ErrorOccurred.")
  public void CallCreateVerticalScrollLayout(AndroidViewComponent container, int id, int width, int height, boolean visible, int backgroundColor) {
    layout.CallCreateVerticalScrollLayout(container, id, width, height, visible, backgroundColor);
  }

  @SimpleFunction(description = "CALL: Bikin HorizontalScrollArrangement dinamis "
      + "ber-id (bisa scroll kiri-kanan) di dalam layout induk. width/height "
      + "angka MIT; visible false = GONE. Sukses menembak LayoutCreated; gagal "
      + "via ErrorOccurred.")
  public void CallCreateHorizontalScrollLayout(AndroidViewComponent container, int id, int width, int height, boolean visible, int backgroundColor) {
    layout.CallCreateHorizontalScrollLayout(container, id, width, height, visible, backgroundColor);
  }

  @SimpleFunction(description = "Ambil layout dinamis ber-id TANPA cek tipe "
      + "(untuk dibaca/dipegang; bebas). null bila tidak ada. Kontras: "
      + "GetLayoutAs(type, id) = AMAN (tipe wajib cocok; untuk dicolok ke soket "
      + "container yang menuntut tipe). Pakai As bila dicolok ke soket container.")
  public Object GetLayout(int id) {
    return layout.GetLayout(id);
  }

  @SimpleFunction(description = "Buat id layout unik baru (Number, siap pakai).")
  public int CreateLayoutId() {
    return layout.CreateLayoutId();
  }

  @SimpleFunction(description = "Semua id layout (URUT PEMBUATAN, "
      + "index 1 = pertama dibuat).")
  public List<Integer> GetLayoutIds() {
    return layout.GetLayoutIds();
  }

  @SimpleFunction(description = "Id layout yang aktif (URUT PEMBUATAN).")
  public List<Integer> GetActiveLayoutIds() {
    return layout.GetActiveLayoutIds();
  }

  @SimpleFunction(description = "Id layout terakhir dibuat. -1 bila kosong.")
  public int CurrentLayoutId() {
    return layout.CurrentLayoutId();
  }

  @SimpleFunction(description = "Id index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstLayoutId() {
    return layout.GetFirstLayoutId();
  }

  @SimpleFunction(description = "Id TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastLayoutId() {
    return layout.GetLastLayoutId();
  }

  @SimpleFunction(description = "Id yang dibuat TEPAT SEBELUM id ini. "
      + "-1 bila tidak ada.")
  public int GetPreviousLayoutId(int id) {
    return layout.GetPreviousLayoutId(id);
  }

  @SimpleFunction(description = "True bila id layout ini masih ada.")
  public boolean IsLayoutIdActive(int id) {
    return layout.IsLayoutIdActive(id);
  }

  @SimpleFunction(description = "Hapus layout dinamis: lepas dari induk + "
      + "lepas id (isi ikut hilang dari layar). CASCADING: isi dinamis di "
      + "dalamnya (Core/Image/Sheet/nested Tools_Layout) ikut dilepas id-nya. "
      + "Sukses menembak LayoutRemoved; id tidak ada = diam (tanpa event).")
  public void RemoveLayoutId(int id) {
    layout.RemoveLayoutId(id);
  }

  @SimpleFunction(description = "SET: Hapus SEMUA layout dinamis Tools_Layout "
      + "(id ikut dilepas; cascading seperti RemoveLayoutId). Menembak "
      + "LayoutRemoved SATU PER SATU id.")
  public void SetClearLayouts() {
    layout.SetClearLayouts();
  }

  @SimpleFunction(description = "SET: Hapus HANYA layout dinamis yang ada di "
      + "dalam container ini (wadah luar/arrangement biasa boleh; isi dinamis "
      + "di dalamnya cascading seperti RemoveLayoutId). Menembak LayoutRemoved "
      + "satu per satu id. Kontras: SetClearLayouts = sapu SEMUA.")
  public void SetClearLayoutsIn(AndroidViewComponent container) {
    layout.SetClearLayoutsIn(container);
  }

  @SimpleFunction(description = "SET: tampil/sembunyi layout dinamis ber-id. "
      + "type = colok TypeVertical/TypeHorizontal/TypeVerticalScroll/"
      + "TypeHorizontalScroll (WAJIB cocok dengan tipe aslinya, biar tidak "
      + "ketuker); id = id layout; visible false = GONE (hilang total, "
      + "ruang ikut ketutup). Tanpa feedback; cek dulu pakai IsLayoutExists "
      + "bila ragu.")
  public void SetLayoutVisible(String type, int id, boolean visible) {
    layout.SetLayoutVisible(type, id, visible);
  }

  @SimpleFunction(description = "True bila layout dinamis ber-id ini ADA dan "
      + "tipenya PERSIS sama dengan type yang diminta (colok TypeVertical/"
      + "dll). False bila id tidak ada ATAU tipe beda.")
  public boolean IsLayoutExists(String type, int id) {
    return layout.IsLayoutExists(type, id);
  }

  @SimpleFunction(description = "True bila layout dinamis ber-id ini ADA, "
      + "tipenya cocok (seperti IsLayoutExists), DAN sedang tampil (visible). "
      + "False bila tidak ada / tipe beda / sedang GONE. Kebalikan: IsLayoutGone.")
  public boolean IsLayoutVisible(String type, int id) {
    return layout.IsLayoutVisible(type, id);
  }

  @SimpleFunction(description = "True bila layout dinamis ber-id ini sedang GONE/"
      + "sembunyi. Kebalikan IsLayoutVisible: true juga bila id tidak ada / tipe "
      + "beda (ragu? cek IsLayoutExists dulu).")
  public boolean IsLayoutGone(String type, int id) {
    return layout.IsLayoutGone(type, id);
  }

  @SimpleFunction(description = "Tipe layout ber-id (\"VerticalArrangement\" "
      + "dkk, sama dengan typeName di LayoutCreated). Bandingkan dengan blok "
      + "TypeVertical/TypeHorizontal/TypeVerticalScroll/TypeHorizontalScroll. "
      + "Kosong bila id tidak ada.")
  public String GetLayoutType(int id) {
    return layout.GetLayoutType(id);
  }

  @SimpleProperty(description = "BLOK KUNING, pembanding read-only: tipe "
      + "VerticalArrangement (colok ke soket type).",
      category = PropertyCategory.BEHAVIOR)
  public String TypeVertical() {
    return layout.TypeVertical();
  }

  @SimpleProperty(description = "BLOK KUNING, pembanding read-only: tipe "
      + "HorizontalArrangement (colok ke soket type).",
      category = PropertyCategory.BEHAVIOR)
  public String TypeHorizontal() {
    return layout.TypeHorizontal();
  }

  @SimpleProperty(description = "BLOK KUNING, pembanding read-only: tipe "
      + "VerticalScrollArrangement (colok ke soket type).",
      category = PropertyCategory.BEHAVIOR)
  public String TypeVerticalScroll() {
    return layout.TypeVerticalScroll();
  }

  @SimpleProperty(description = "BLOK KUNING, pembanding read-only: tipe "
      + "HorizontalScrollArrangement (colok ke soket type).",
      category = PropertyCategory.BEHAVIOR)
  public String TypeHorizontalScroll() {
    return layout.TypeHorizontalScroll();
  }

  @SimpleFunction(description = "Ambil layout dinamis ber-id yang tipenya PERSIS "
      + "sama dengan type yang diminta (colok TypeVertical/dll; untuk dicolok ke "
      + "soket container, mis. Create button). AMAN vs GetLayout(id) yang bebas. "
      + "null bila id tidak ada ATAU tipe beda.")
  public Object GetLayoutAs(String type, int id) {
    return layout.GetLayoutAs(type, id);
  }

  @SimpleFunction(description = "SET: atur lebar + tinggi layout dinamis ber-id "
      + "(tipe wajib cocok). Angka SAMA seperti blok bawaan MIT: -2 = isi "
      + "penuh (fill parent), -1 = otomatis, positif = pixel. Tanpa feedback.")
  public void SetLayoutSize(String type, int id, int width, int height) {
    layout.SetLayoutSize(type, id, width, height);
  }

  @SimpleFunction(description = "SET: warna latar layout dinamis ber-id (tipe "
      + "wajib cocok). Colok blok warna MIT. Tanpa feedback.")
  public void SetLayoutBackgroundColor(String type, int id, int color) {
    layout.SetLayoutBackgroundColor(type, id, color);
  }

  @SimpleFunction(description = "SET: perataan isi layout dinamis ber-id (tipe "
      + "wajib cocok). Angka SAMA seperti blok bawaan MIT: horizontal 1=kiri, "
      + "2=tengah, 3=kanan; vertikal 1=atas, 2=tengah, 3=bawah. Tanpa feedback.")
  public void SetLayoutAlignment(String type, int id, int horizontal, int vertical) {
    layout.SetLayoutAlignment(type, id, horizontal, vertical);
  }

  @SimpleFunction(description = "Lebar layout dinamis ber-id (angka MIT: -2 isi, "
      + "-1 otomatis, positif pixel). -3 bila id tidak ada / tipe beda.")
  public int GetLayoutWidth(String type, int id) {
    return layout.GetLayoutWidth(type, id);
  }

  @SimpleFunction(description = "Tinggi layout dinamis ber-id (angka MIT). "
      + "-3 bila id tidak ada / tipe beda.")
  public int GetLayoutHeight(String type, int id) {
    return layout.GetLayoutHeight(type, id);
  }

  @SimpleEvent(description = "Layout dinamis jadi: id + nama tipe.")
  public void LayoutCreated(int id, String typeName) {
    EventDispatcher.dispatchEvent(this, "LayoutCreated", id, typeName);
  }

  @SimpleEvent(description = "Layout dinamis dilepas (RemoveLayoutId/ "
      + "SetClearLayouts/SetClearLayoutsIn): id yang hilang.")
  public void LayoutRemoved(int id) {
    EventDispatcher.dispatchEvent(this, "LayoutRemoved", id);
  }

  @SimpleProperty(description = "Nama operasi Layout (untuk if di "
      + "ErrorOccurred: cth if component = OpLayout berarti error Layout).",
      category = PropertyCategory.BEHAVIOR)
  public String OpLayout() {
    return layout.OpLayout();
  }

  // ================= Sheet =================

  @SimpleFunction(description = "Daftarkan bottom-sheet ber-id dengan gaya BAKU "
      + "(bawah, lebar penuh, dim 50, geser-bawah 250ms, bisa tutup back/luar, "
      + "geser-tutup aktif). content = Arrangement (dipindah ke dialog saat "
      + "tampil, dikembalikan saat tutup). Gaya cukup = langsung CallShowSheet; "
      + "mau gaya lain = SetSheetStyle dulu. Sukses tanpa event; gagal via "
      + "ErrorOccurred.")
  public void RegisterSheet(final int id, final AndroidViewComponent content) {
    sheet.RegisterSheet(id, content);
  }

  @SimpleFunction(description = "SET: ubah gaya sheet TERDAFTAR (boleh sebelum/sesudah "
      + "tampil; langsung kelihatan bila sedang tampil). gravity angka: 80=bawah, "
      + "48=atas, 17=tengah, 3=kiri, 5=kanan. widthPx/heightPx <= 0 = menyesuaikan "
      + "(lebar penuh bila gravity atas/bawah). dimPercent 0-100 (kegelapan latar). "
      + "animationType teks: pakai blok SlideFromBottom/SlideFromTop/"
      + "SlideFromLeft/SlideFromRight/FadeAnim/NoAnim "
      + "(salah ketik = slide-bawah). animationDurationMs 0 = 250. "
      + "cancelable = bisa tutup via tombol back/luar. swipeToDismiss = geser "
      + "ke arah tutup untuk menutup. Sukses diam; id belum register via "
      + "ErrorOccurred.")
  public void SetSheetStyle(final int id, final int gravity, final int widthPx, final int heightPx, final int dimPercent, final String animationType, final int animationDurationMs, final boolean cancelable, final boolean swipeToDismiss) {
    sheet.SetSheetStyle(id, gravity, widthPx, heightPx, dimPercent, animationType, animationDurationMs, cancelable, swipeToDismiss);
  }

  @SimpleFunction(description = "CALL: tampilkan sheet SEDERHANA dari Arrangement "
      + "dalam 1 blok (tanpa Register dulu): gaya baku bawah + full-width + "
      + "dim 50 + geser-bawah, id dibuat otomatis. Kembali: id sheet (tampil via "
      + "event SheetShown; tutup via CallDismissSheet; -1 bila gagal).")
  public int ShowSimpleSheet(final AndroidViewComponent content) {
    return sheet.ShowSimpleSheet(content);
  }

  @SimpleFunction(description = "CALL: Tampilkan sheet ber-id (animasi geser naik "
      + "otomatis). Hasil lewat SheetShown.")
  public void CallShowSheet(final int id) {
    sheet.CallShowSheet(id);
  }

  @SimpleFunction(description = "CALL: Tutup sheet ber-id (animasi keluar). "
      + "Hasil lewat SheetHidden. Dismiss = tutup TAPI registrasi tetap (bisa "
      + "CallShowSheet lagi); beda dengan RemoveSheetId (tutup + id dibuang) dan "
      + "SetClearSheets (buang semua).")
  public void CallDismissSheet(final int id) {
    sheet.CallDismissSheet(id);
  }

  @SimpleFunction(description = "True bila sheet ber-id sedang tampil. Kontras: "
      + "baru Register tapi belum Show -> IsSheetIdActive=true, IsSheetShowing=false.")
  public boolean IsSheetShowing(int id) {
    return sheet.IsSheetShowing(id);
  }

  @SimpleFunction(description = "Hapus sheet ber-id: tutup diam-diam (tanpa "
      + "SheetHidden), kembalikan konten ke layout asal, lepas id. Remove = tutup "
      + "+ id dibuang; beda dengan CallDismissSheet (registrasi tetap) dan "
      + "SetClearSheets (buang semua).")
  public void RemoveSheetId(final int id) {
    sheet.RemoveSheetId(id);
  }

  @SimpleFunction(description = "CALL: tampilkan toast (pesan singkat bawah layar). "
      + "longDuration = tampil lebih lama. Sukses menembak ToastShown; gagal via "
      + "ErrorOccurred. Toast TIDAK butuh izin notifikasi.")
  public void CallShowToast(final String message, final boolean longDuration) {
    sheet.CallShowToast(message, longDuration);
  }

  @SimpleFunction(description = "CALL: SET izin notifikasi (spesifik "
      + "POST_NOTIFICATIONS, tanpa soket). Android 13+ saja; di bawah itu "
      + "langsung GotPermission(true). Sudah granted = langsung "
      + "GotPermission(true) tanpa dialog; belum = dialog izin (end-user yang "
      + "mutusin) + GotPermission. permanentlyDenied=true = permanen "
      + "(aktifkan manual di Setelan). Tanpa return.")
  public void CallSetNotificationPermission() {
    sheet.CallSetNotificationPermission();
  }

  @SimpleFunction(description = "Buat id notifikasi unik baru (Number; minta ke sini, "
      + "JANGAN angka manual — id dobel = notifikasi lama tertimpa).")
  public int CreateNotificationId() {
    return sheet.CreateNotificationId();
  }

  @SimpleFunction(description = "CALL: tampilkan notifikasi bar status: id (minta ke "
      + "CreateNotificationId; manual boleh tapi dobel = tertimpa), judul, isi. "
      + "Tap notifikasi membuka aplikasi. Hanya NOTIFIKASI yang butuh izin "
      + "POST_NOTIFICATIONS (minta dulu via CallSetNotificationPermission; toast/sheet tidak). Sukses menembak NotificationShown; "
      + "gagal (mis. izin belum ada) via ErrorOccurred.")
  public void CallSetNotification(final int id, final String title, final String text) {
    sheet.CallSetNotification(id, title, text);
  }

  @SimpleFunction(description = "SET: Hapus notifikasi ber-id.")
  public void SetCancelNotification(int id) {
    sheet.SetCancelNotification(id);
  }

  @SimpleFunction(description = "SET: Hapus SEMUA notifikasi Tools_Sheet.")
  public void SetCancelAllNotifications() {
    sheet.SetCancelAllNotifications();
  }

  @SimpleProperty(description = "Tipe animasi: geser dari bawah.",
      category = PropertyCategory.BEHAVIOR)
  public String SlideFromBottom() {
    return sheet.SlideFromBottom();
  }

  @SimpleProperty(description = "Tipe animasi: geser dari atas.",
      category = PropertyCategory.BEHAVIOR)
  public String SlideFromTop() {
    return sheet.SlideFromTop();
  }

  @SimpleProperty(description = "Tipe animasi: geser dari kiri.",
      category = PropertyCategory.BEHAVIOR)
  public String SlideFromLeft() {
    return sheet.SlideFromLeft();
  }

  @SimpleProperty(description = "Tipe animasi: geser dari kanan.",
      category = PropertyCategory.BEHAVIOR)
  public String SlideFromRight() {
    return sheet.SlideFromRight();
  }

  @SimpleProperty(description = "Tipe animasi: fade.",
      category = PropertyCategory.BEHAVIOR)
  public String FadeAnim() {
    return sheet.FadeAnim();
  }

  @SimpleProperty(description = "Tipe animasi: tanpa animasi.",
      category = PropertyCategory.BEHAVIOR)
  public String NoAnim() {
    return sheet.NoAnim();
  }

  @SimpleFunction(description = "Buat id sheet unik baru (Number, siap pakai "
      + "ke RegisterSheet). 90% kasus cukup 2 blok id: CreateSheetId + RemoveSheetId.")
  public int CreateSheetId() {
    return sheet.CreateSheetId();
  }

  @SimpleFunction(description = "Tinggi jendela sheet ber-id (pixel; -2 = isi penuh, "
      + "-1 = otomatis/belum tampil). -1 juga bila sheet tidak ada. Isinya = "
      + "Arrangement yang kamu daftarkan (pegang referensinya di variabel; "
      + "tidak ada blok pengambil karena komponen MIT tidak bisa dibungkus "
      + "jadi nilai). Pasangan SetSheetStyle.")
  public int GetSheetHeight(int id) {
    return sheet.GetSheetHeight(id);
  }

  @SimpleFunction(description = "Semua id sheet yang terdaftar (URUT PEMBUATAN, "
      + "index 1 = pertama dibuat). Termasuk yang sedang tidak tampil; beda "
      + "dengan GetActiveSheetIds.")
  public List<Integer> GetSheetIds() {
    return sheet.GetSheetIds();
  }

  @SimpleFunction(description = "Id sheet yang sedang aktif/tampil (URUT PEMBUATAN). "
      + "Beda dengan GetSheetIds yang mencatat semua yang terdaftar. "
      + "Periksa 1 id via IsSheetIdActive.")
  public List<Integer> GetActiveSheetIds() {
    return sheet.GetActiveSheetIds();
  }

  @SimpleFunction(description = "Id sheet terakhir dibuat. -1 bila kosong.")
  public int CurrentSheetId() {
    return sheet.CurrentSheetId();
  }

  @SimpleFunction(description = "Id index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstSheetId() {
    return sheet.GetFirstSheetId();
  }

  @SimpleFunction(description = "Id TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastSheetId() {
    return sheet.GetLastSheetId();
  }

  @SimpleFunction(description = "Id yang dibuat TEPAT SEBELUM id ini. "
      + "-1 bila tidak ada.")
  public int GetPreviousSheetId(int id) {
    return sheet.GetPreviousSheetId(id);
  }

  @SimpleFunction(description = "True bila id sheet ini masih terdaftar. Kontras: "
      + "baru Register tapi belum Show -> Active=true, Showing=false.")
  public boolean IsSheetIdActive(int id) {
    return sheet.IsSheetIdActive(id);
  }

  @SimpleFunction(description = "SET: Hapus SEMUA sheet Tools_Sheet (tutup "
      + "diam-diam, konten dikembalikan, id dilepas). Clear = buang semua; beda "
      + "dengan CallDismissSheet (registrasi tetap) dan RemoveSheetId (satu id).")
  public void SetClearSheets() {
    sheet.SetClearSheets();
  }

  @SimpleEvent(description = "Sheet ber-id tertutup (back/luar/Dismiss): id. "
      + "Registrasi tetap, tampilkan lagi via CallShowSheet.")
  public void SheetHidden(int id) {
    EventDispatcher.dispatchEvent(this, "SheetHidden", id);
  }

  @SimpleEvent(description = "Sheet ber-id tampil (hasil CallShowSheet/ShowSimpleSheet): id.")
  public void SheetShown(int id) {
    EventDispatcher.dispatchEvent(this, "SheetShown", id);
  }

  @SimpleEvent(description = "Toast tampil (hasil CallShowToast).")
  public void ToastShown() {
    EventDispatcher.dispatchEvent(this, "ToastShown");
  }

  @SimpleEvent(description = "Notifikasi tampil (hasil CallSetNotification): id.")
  public void NotificationShown(int id) {
    EventDispatcher.dispatchEvent(this, "NotificationShown", id);
  }

  @SimpleProperty(description = "Nama operasi Sheet (untuk if di "
      + "ErrorOccurred: cth if component = OpSheet berarti error sheet).",
      category = PropertyCategory.BEHAVIOR)
  public String OpSheet() {
    return sheet.OpSheet();
  }

  @SimpleProperty(description = "Nama operasi Toast (untuk if di "
      + "ErrorOccurred: cth if component = OpToast berarti error toast).",
      category = PropertyCategory.BEHAVIOR)
  public String OpToast() {
    return sheet.OpToast();
  }

  @SimpleProperty(description = "Nama operasi Notification (untuk if di "
      + "ErrorOccurred: cth if component = OpNotification berarti error notifikasi).",
      category = PropertyCategory.BEHAVIOR)
  public String OpNotification() {
    return sheet.OpNotification();
  }

  // ================= Style =================

  @SimpleFunction(description = "SET: Atur sudut membulat (radius, pixel). Background "
      + "bawaan diganti.")
  public void SetCornerRadius(AndroidViewComponent comp, float radiusPx) {
    style.SetCornerRadius(comp, radiusPx);
  }

  @SimpleFunction(description = "SET: Atur garis tepi: warna + tebal (pixel). "
      + "Background bawaan diganti.")
  public void SetBorder(AndroidViewComponent comp, int color, float widthPx) {
    style.SetBorder(comp, color, widthPx);
  }

  @SimpleFunction(description = "SET: Atur gradien banyak warna (list warna, "
      + "minimal 2). arah = colok OrientationTopBottom/dll; "
      + "angka 0-7 = cara lama (0=atas-bawah, 1=kananATAS-kiriBAWAH, "
      + "2=kanan-kiri, 3=kananBAWAH-kiriATAS, 4=bawah-atas, 5=kiriBAWAH-"
      + "kananATAS, 6=kiri-kanan, 7=kiriATAS-kananBAWAH). Background bawaan "
      + "diganti (menimpa warna polos).")
  public void SetGradient(AndroidViewComponent comp, Object colors, int orientation) {
    style.SetGradient(comp, colors, orientation);
  }

  @SimpleProperty(description = "BLOK KUNING, arah gradien: atas-bawah (= 0).",
      category = PropertyCategory.BEHAVIOR)
  public int OrientationTopBottom() {
    return style.OrientationTopBottom();
  }

  @SimpleProperty(description = "BLOK KUNING, arah gradien: kananATAS-"
      + "kiriBAWAH (= 1).",
      category = PropertyCategory.BEHAVIOR)
  public int OrientationTopRightBottomLeft() {
    return style.OrientationTopRightBottomLeft();
  }

  @SimpleProperty(description = "BLOK KUNING, arah gradien: kanan-kiri (= 2).",
      category = PropertyCategory.BEHAVIOR)
  public int OrientationRightLeft() {
    return style.OrientationRightLeft();
  }

  @SimpleProperty(description = "BLOK KUNING, arah gradien: kananBAWAH-"
      + "kiriATAS (= 3).",
      category = PropertyCategory.BEHAVIOR)
  public int OrientationBottomRightTopLeft() {
    return style.OrientationBottomRightTopLeft();
  }

  @SimpleProperty(description = "BLOK KUNING, arah gradien: bawah-atas (= 4).",
      category = PropertyCategory.BEHAVIOR)
  public int OrientationBottomTop() {
    return style.OrientationBottomTop();
  }

  @SimpleProperty(description = "BLOK KUNING, arah gradien: kiriBAWAH-"
      + "kananATAS (= 5).",
      category = PropertyCategory.BEHAVIOR)
  public int OrientationBottomLeftTopRight() {
    return style.OrientationBottomLeftTopRight();
  }

  @SimpleProperty(description = "BLOK KUNING, arah gradien: kiri-kanan (= 6).",
      category = PropertyCategory.BEHAVIOR)
  public int OrientationLeftRight() {
    return style.OrientationLeftRight();
  }

  @SimpleProperty(description = "BLOK KUNING, arah gradien: kiriATAS-"
      + "kananBAWAH (= 7).",
      category = PropertyCategory.BEHAVIOR)
  public int OrientationTopLeftBottomRight() {
    return style.OrientationTopLeftBottomRight();
  }

  @SimpleFunction(description = "SET: Atur gradien radial: warna tengah, warna tepi, "
      + "radius (pixel). Background bawaan diganti.")
  public void SetRadialGradient(AndroidViewComponent comp, int centerColor, int edgeColor, float radiusPx) {
    style.SetRadialGradient(comp, centerColor, edgeColor, radiusPx);
  }

  @SimpleFunction(description = "SET: Atur bentuk background: colok "
      + "ShapeRectangle/ShapeOval/ShapeLine/ShapeRing .")
  public void SetShape(AndroidViewComponent comp, int shape) {
    style.SetShape(comp, shape);
  }

  @SimpleProperty(description = "BLOK KUNING, bentuk: persegi.",
      category = PropertyCategory.BEHAVIOR)
  public int ShapeRectangle() {
    return style.ShapeRectangle();
  }

  @SimpleProperty(description = "BLOK KUNING, bentuk: oval/lingkaran.",
      category = PropertyCategory.BEHAVIOR)
  public int ShapeOval() {
    return style.ShapeOval();
  }

  @SimpleProperty(description = "BLOK KUNING, bentuk: garis.",
      category = PropertyCategory.BEHAVIOR)
  public int ShapeLine() {
    return style.ShapeLine();
  }

  @SimpleProperty(description = "BLOK KUNING, bentuk: cincin.",
      category = PropertyCategory.BEHAVIOR)
  public int ShapeRing() {
    return style.ShapeRing();
  }

  @SimpleFunction(description = "SET: Atur visibilitas: colok "
      + "VisibilityVisible/VisibilityInvisible/VisibilityGone . "
      + "Gone = hilang + tempat ikut hilang (≈ Visible=false MIT); Invisible "
      + "= tak terlihat tapi tempat tetap ada.")
  public void SetVisibility(AndroidViewComponent comp, int visibility) {
    style.SetVisibility(comp, visibility);
  }

  @SimpleProperty(description = "BLOK KUNING, visibilitas: terlihat.",
      category = PropertyCategory.BEHAVIOR)
  public int VisibilityVisible() {
    return style.VisibilityVisible();
  }

  @SimpleProperty(description = "BLOK KUNING, visibilitas: tak terlihat "
      + "(tempat tetap ada).",
      category = PropertyCategory.BEHAVIOR)
  public int VisibilityInvisible() {
    return style.VisibilityInvisible();
  }

  @SimpleProperty(description = "BLOK KUNING, visibilitas: hilang "
      + "(tempat ikut hilang).",
      category = PropertyCategory.BEHAVIOR)
  public int VisibilityGone() {
    return style.VisibilityGone();
  }

  @SimpleFunction(description = "SET: Atur putaran komponen (derajat, searah jarum jam).")
  public void SetRotation(AndroidViewComponent comp, float degrees) {
    style.SetRotation(comp, degrees);
  }

  @SimpleFunction(description = "SET: Efek ripple (gelombang sentuh) warna "
      + "tertentu. Menimpa SEMUA background — pasang PALING AKHIR.")
  public void SetRipple(AndroidViewComponent comp, int color) {
    style.SetRipple(comp, color);
  }

  @SimpleFunction(description = "SET: Garis tepi: warna + tebal (pixel) dulu, "
      + "lalu bila dashed=true tambah panjang garis (pixel) + jarak garis (pixel). "
      + "dashLengthPx/dashGapPx DIABAIKAN bila dashed=false (= garis penuh).")
  public void SetStroke(AndroidViewComponent comp, int color, float widthPx, boolean dashed, float dashLengthPx, float dashGapPx) {
    style.SetStroke(comp, color, widthPx, dashed, dashLengthPx, dashGapPx);
  }

  @SimpleFunction(description = "SET: Atur spasi teks (khusus Label/TextBox/Button): "
      + "jarak baris (pixel) + jarak huruf (em). Selain teks: error.")
  public void SetTextSpace(AndroidViewComponent comp, float lineSpacePx, float letterSpace) {
    style.SetTextSpace(comp, lineSpacePx, letterSpace);
  }

  @SimpleFunction(description = "SET: Jadikan lingkaran/oval (cocok untuk foto "
      + "profil). Untuk lingkaran sempurna pakai komponen persegi.")
  public void SetCircular(AndroidViewComponent comp) {
    style.SetCircular(comp);
  }

  @SimpleFunction(description = "SET: Lingkaran + garis tepi warna (cocok untuk foto "
      + "profil).")
  public void SetCircularBorder(AndroidViewComponent comp, int color, float widthPx) {
    style.SetCircularBorder(comp, color, widthPx);
  }

  @SimpleFunction(description = "SET RESET: hapus animasi; kembalikan "
      + "opacity/alpha, rotasi, skala, posisi (translation) ke normal. yang "
      + "TIDAK di-reset: background, elevation, padding, margin.")
  public void SetResetEffects(AndroidViewComponent comp) {
    style.SetResetEffects(comp);
  }

  @SimpleFunction(description = "SET: Atur warna background polos. "
      + "Background bawaan diganti. Bertarung dengan SetDynamicBackgroundColor "
      + "(Core): yang dipanggil TERAKHIR menang.")
  public void SetBackgroundColor(AndroidViewComponent comp, int color) {
    style.SetBackgroundColor(comp, color);
  }

  @SimpleFunction(description = "SET: Atur gambar background dari file LOKAL "
      + "atau asset (path di-root /storage/emulated/0; asset diawali \"//\"). "
      + "KHUSUS lokal — BUKAN URL (URL: unduh dulu pakai Tools_Image/Tools_"
      + "Downloader). Izin baca storage otomatis dibawa komponen ini. Cocok "
      + "untuk wadah. Menimpa background bawaan.")
  public void SetBackgroundImage(AndroidViewComponent comp, final String path) {
    style.SetBackgroundImage(comp, path);
  }

  @SimpleFunction(description = "SET: Atur bayangan/elevasi (pixel). Butuh "
      + "background tidak transparan + ruang kosong di sekitar agar bayangan "
      + "terlihat; di tema gelap bayangan samar.")
  public void SetElevation(AndroidViewComponent comp, float px) {
    style.SetElevation(comp, px);
  }

  @SimpleFunction(description = "SET: Atur jarak dalam/padding (pixel): "
      + "kiri, atas, kanan, bawah.")
  public void SetPadding(AndroidViewComponent comp, float leftPx, float topPx, float rightPx, float bottomPx) {
    style.SetPadding(comp, leftPx, topPx, rightPx, bottomPx);
  }

  @SimpleFunction(description = "SET: Atur jarak luar/margin (pixel): kiri, atas, "
      + "kanan, bawah. Hanya bila induk mendukung margin (Vertical/Horizontal/"
      + "ScrollArrangement BISA; TableArrangement TIDAK — baris tabel "
      + "mengabaikan margin). Induk tak mendukung = ErrorOccurred.")
  public void SetMargin(AndroidViewComponent comp, float leftPx, float topPx, float rightPx, float bottomPx) {
    style.SetMargin(comp, leftPx, topPx, rightPx, bottomPx);
  }

  @SimpleFunction(description = "SET: Atur transparansi 0-100 (100 = penuh).")
  public void SetOpacity(AndroidViewComponent comp, int percent) {
    style.SetOpacity(comp, percent);
  }

  @SimpleFunction(description = "CALL: Animasi fade: startPercent ke endPercent "
      + "(0-100). Mis. 0->100 muncul, 100->0 hilang. durationMs mis. 300; "
      + "durationMs<=0 = tanpa set durasi (jalan instan). Menembak "
      + "AnimationStarted/AnimationEnded.")
  public void CallAnimateFade(AndroidViewComponent comp, int durationMs, int startPercent, int endPercent) {
    style.CallAnimateFade(comp, durationMs, startPercent, endPercent);
  }

  @SimpleProperty(description = "BLOK KUNING, arah geser: dari kiri.",
      category = PropertyCategory.BEHAVIOR)
  public String DirectionLeft() {
    return style.DirectionLeft();
  }

  @SimpleProperty(description = "BLOK KUNING, arah geser: dari kanan.",
      category = PropertyCategory.BEHAVIOR)
  public String DirectionRight() {
    return style.DirectionRight();
  }

  @SimpleProperty(description = "BLOK KUNING, arah geser: dari atas.",
      category = PropertyCategory.BEHAVIOR)
  public String DirectionTop() {
    return style.DirectionTop();
  }

  @SimpleProperty(description = "BLOK KUNING, arah geser: dari bawah.",
      category = PropertyCategory.BEHAVIOR)
  public String DirectionBottom() {
    return style.DirectionBottom();
  }

  @SimpleFunction(description = "CALL: Animasi geser-masuk: direction = colok "
      + "DirectionLeft/DirectionRight/DirectionTop/DirectionBottom ; teks left/right/top/bottom = cara lama. durationMs mis. "
      + "300; durationMs<=0 = tanpa set durasi (jalan instan).")
  public void CallAnimateSlideIn(AndroidViewComponent comp, String direction, int durationMs) {
    style.CallAnimateSlideIn(comp, direction, durationMs);
  }

  @SimpleFunction(description = "CALL: Animasi skala: ke toXPercent/toYPercent "
      + "(100 = normal). durationMs mis. 300; durationMs<=0 = tanpa set durasi "
      + "(jalan instan).")
  public void CallAnimateScale(AndroidViewComponent comp, int durationMs, int toXPercent, int toYPercent) {
    style.CallAnimateScale(comp, durationMs, toXPercent, toYPercent);
  }

  @SimpleFunction(description = "CALL: Animasi putar (derajat, searah jarum jam). "
      + "durationMs mis. 300; durationMs<=0 = tanpa set durasi (jalan "
      + "instan).")
  public void CallAnimateRotation(AndroidViewComponent comp, int durationMs, float degrees) {
    style.CallAnimateRotation(comp, durationMs, degrees);
  }

  @SimpleFunction(description = "Fallback polling untuk event animasi: true "
      + "bila komponen sedang dianimasikan Tools_Style.")
  public boolean IsAnimationRunning(AndroidViewComponent comp) {
    return style.IsAnimationRunning(comp);
  }

  @SimpleEvent(description = "Animasi Tools_Style mulai: komponen + tipe "
      + "(fade/slide/scale/rotation). Fallback: IsAnimationRunning.")
  public void AnimationStarted(AndroidViewComponent comp, String animationType) {
    EventDispatcher.dispatchEvent(this, "AnimationStarted", comp, animationType);
  }

  @SimpleEvent(description = "Animasi Tools_Style selesai: komponen + tipe. "
      + "Fallback: IsAnimationRunning.")
  public void AnimationEnded(AndroidViewComponent comp, String animationType) {
    EventDispatcher.dispatchEvent(this, "AnimationEnded", comp, animationType);
  }

  @SimpleProperty(description = "Nama operasi Style (untuk if di "
      + "ErrorOccurred: cth if component = OpStyle berarti error Style).",
      category = PropertyCategory.BEHAVIOR)
  public String OpStyle() {
    return style.OpStyle();
  }

  // ================= Task =================

  @SimpleFunction(description = "Buat id tugas unik baru (Number, siap pakai "
      + "ke CallSchedule*/CallScheduleAlarm). Urutan pembuatan menentukan urutan list.")
  public int CreateTaskId() {
    return task.CreateTaskId();
  }

  @SimpleFunction(description = "Semua id tugas/alarm (URUT PEMBUATAN, "
      + "index 1 = pertama dibuat).")
  public List<Integer> GetTaskIds() {
    return task.GetTaskIds();
  }

  @SimpleFunction(description = "Id tugas/alarm yang aktif (jalan/jeda/terpasang, "
      + "URUT PEMBUATAN).")
  public List<Integer> GetActiveTaskIds() {
    return task.GetActiveTaskIds();
  }

  @SimpleFunction(description = "Id tugas terakhir dibuat/di-start. -1 bila kosong.")
  public int CurrentTaskId() {
    return task.CurrentTaskId();
  }

  @SimpleFunction(description = "Id index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstTaskId() {
    return task.GetFirstTaskId();
  }

  @SimpleFunction(description = "Id TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastTaskId() {
    return task.GetLastTaskId();
  }

  @SimpleFunction(description = "Id yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.")
  public int GetPreviousTaskId(int id) {
    return task.GetPreviousTaskId(id);
  }

  @SimpleFunction(description = "True bila id tugas ini aktif (jalan/jeda/alarm terpasang).")
  public boolean IsTaskIdActive(int id) {
    return task.IsTaskIdActive(id);
  }

  @SimpleFunction(description = "Hapus id tugas: batalkan jadwal + cabut alarm "
      + "sistem + lepas id (otomatis beres-beres).")
  public void RemoveTaskId(int id) {
    task.RemoveTaskId(id);
  }

  @SimpleFunction(description = "CALL: Batalkan SEMUA tugas dalam-aplikasi (alarm "
      + "sistem tidak ikut; pakai SetCancelAllAlarms bila perlu). Tiap tugas "
      + "yang jalan menembak TaskCancelled.")
  public void CallCancelAllTasks() {
    task.CallCancelAllTasks();
  }

  @SimpleFunction(description = "CALL: Jadwalkan tugas sekali jalan: id + jeda "
      + "mili-detik + payload teks. Jadwal diterima = menembak TaskStarted, lalu "
      + "TaskFinished. Payload kompleks = JSON via Tools_Base (kirim JsonEncode, "
      + "terima JsonDecode). Gagal via ErrorOccurred.")
  public void CallScheduleOnce(int id, long delayMillis, String payload) {
    task.CallScheduleOnce(id, delayMillis, payload);
  }

  @SimpleFunction(description = "CALL: Jadwalkan tugas berulang: id + jeda awal + "
      + "interval + jumlah ulang (mis. 5; -1/0 = selamanya; 1 = sekali) + payload. "
      + "Tiap tick menembak TaskTick; selesai menembak TaskFinished. Payload "
      + "kompleks = JSON via Tools_Base (kirim JsonEncode, terima JsonDecode).")
  public void CallScheduleRepeating(int id, long initialDelayMillis, long intervalMillis, int repeatCount, String payload) {
    task.CallScheduleRepeating(id, initialDelayMillis, intervalMillis, repeatCount, payload);
  }

  @SimpleFunction(description = "CALL: Jadwalkan tugas pada jam epoch mili-detik "
      + "(lihat NowMillis; mau jam harian tanpa epoch = CallScheduleDailyAt). "
      + "Lewat = langsung jalan.")
  public void CallScheduleAt(int id, long epochMillis, String payload) {
    task.CallScheduleAt(id, epochMillis, payload);
  }

  @SimpleFunction(description = "SET: Jeda tugas ber-id (berlaku untuk once/repeating "
      + "yang belum selesai). Lanjut via SetResumeTask. Bukan jeda/jalan = ErrorOccurred. "
      + "Sukses diam.")
  public void SetPauseTask(int id) {
    task.SetPauseTask(id);
  }

  @SimpleFunction(description = "SET: Lanjutkan tugas yang dijeda (SetPauseTask). "
      + "Bukan jeda = ErrorOccurred. Sukses diam.")
  public void SetResumeTask(int id) {
    task.SetResumeTask(id);
  }

  @SimpleFunction(description = "CALL: Batalkan tugas ber-id (jadwal dalam-aplikasi "
      + "saja). Hasil lewat TaskCancelled.")
  public void CallCancelTask(int id) {
    task.CallCancelTask(id);
  }

  @SimpleFunction(description = "True bila tugas ber-id sedang dijeda (SetPauseTask, "
      + "belum resume/batal). False = jalan/selesai/batal/tidak ada (ragu? cek "
      + "GetTaskStatus).")
  public boolean IsTaskPaused(int id) {
    return task.IsTaskPaused(id);
  }

  @SimpleFunction(description = "Sisa mili-detik sampai bunyi berikutnya (jalan = "
      + "nextFire - sekarang; jeda = sisa saat dijeda; selesai/batal/tidak ada = 0). "
      + "Pasangan GetTaskProgress (index 4 = nextFireAt).")
  public long GetTaskRemaining(int id) {
    return task.GetTaskRemaining(id);
  }

  @SimpleFunction(description = "Mili-detik sejak tick terakhir (jalan; jeda = nilai "
      + "saat dijeda; belum pernah bunyi = sejak dijadwalkan; selesai/batal/tidak "
      + "ada = 0).")
  public long GetTaskElapsed(int id) {
    return task.GetTaskElapsed(id);
  }

  @SimpleFunction(description = "Waktu sekarang epoch mili-detik (untuk CallScheduleAt/CallScheduleAlarm).")
  public long NowMillis() {
    return task.NowMillis();
  }

  @SimpleFunction(description = "Fallback polling untuk event Task*: status "
      + "tugas ber-id. Salah satu: antri, jalan, jeda, selesai, batal, gagal, "
      + "alarm, atau tidak ada. Jeda = cek IsTaskPaused; sisa = GetTaskRemaining.")
  public String GetTaskStatus(int id) {
    return task.GetTaskStatus(id);
  }

  @SimpleFunction(description = "Fallback polling untuk TaskFinished: list "
      + "[index 1=success, 2=payload, 3=message]. Tetap ada sampai RemoveTaskId.")
  public List<Object> GetTaskResult(int id) {
    return task.GetTaskResult(id);
  }

  @SimpleFunction(description = "Fallback polling untuk TaskTick: list "
      + "[index 1=tickCount, 2=repeatCount, 3=lastTickAt, 4=nextFireAt].")
  public List<Object> GetTaskProgress(int id) {
    return task.GetTaskProgress(id);
  }

  @SimpleFunction(description = "Id tugas dalam-aplikasi terakhir yang selesai/bunyi "
      + "(-1 bila belum ada). Terakhir-menang: dua selesai berdekatan = kedua "
      + "menimpa; butuh pasti = TaskFinished.")
  public int GetLastFiredTaskId() {
    return task.GetLastFiredTaskId();
  }

  @SimpleFunction(description = "Payload tugas terakhir yang selesai/bunyi "
      + "(terakhir-menang; butuh pasti = TaskFinished).")
  public String GetLastFiredPayload() {
    return task.GetLastFiredPayload();
  }

  @SimpleFunction(description = "Waktu epoch tugas terakhir selesai/bunyi "
      + "(0 bila belum ada; terakhir-menang; butuh pasti = TaskFinished).")
  public long GetLastFiredTime() {
    return task.GetLastFiredTime();
  }

  @SimpleFunction(description = "SET: Simpan extra teks untuk tugas ber-id (mis. "
      + "url, nama file). Angka -> teks dulu. Menimpa kunci yang sama. "
      + "Sukses diam; id tidak ada = ErrorOccurred.")
  public void SetTaskExtra(int id, String key, String value) {
    task.SetTaskExtra(id, key, value);
  }

  @SimpleFunction(description = "Baca extra tugas ber-id (default bila tidak ada).")
  public String GetTaskExtra(int id, String key, String defaultValue) {
    return task.GetTaskExtra(id, key, defaultValue);
  }

  @SimpleFunction(description = "Semua kunci extra tugas ber-id.")
  public List<String> GetTaskExtraKeys(int id) {
    return task.GetTaskExtraKeys(id);
  }

  @SimpleFunction(description = "Hapus satu extra tugas ber-id.")
  public void RemoveTaskExtra(int id, String key) {
    task.RemoveTaskExtra(id, key);
  }

  @SimpleFunction(description = "Hapus SEMUA extra tugas ber-id.")
  public void SetClearTaskExtras(int id) {
    task.SetClearTaskExtras(id);
  }

  @SimpleFunction(description = "SET: Simpan variabel teks global (bisa dibaca tugas "
      + "mana pun + blok lain; tersimpan di HP = survive reboot + terbaca saat "
      + "alarm menyalakan aplikasi). Angka -> teks dulu. Sukses diam.")
  public void SetVar(String name, String value) {
    task.SetVar(name, value);
  }

  @SimpleFunction(description = "Baca variabel global (default bila tidak ada).")
  public String GetVar(String name, String defaultValue) {
    return task.GetVar(name, defaultValue);
  }

  @SimpleFunction(description = "Hapus satu variabel global.")
  public void RemoveVar(String name) {
    task.RemoveVar(name);
  }

  @SimpleFunction(description = "SET: Hapus SEMUA variabel global (tersimpan ikut terhapus).")
  public void SetClearVars() {
    task.SetClearVars();
  }

  @SimpleFunction(description = "Semua nama variabel global (URUT DIBUAT).")
  public List<String> GetAllVarNames() {
    return task.GetAllVarNames();
  }

  @SimpleFunction(description = "CALL: SET izin exact-alarm (spesifik alarm, "
      + "tanpa soket). Sudah boleh = langsung GotPermission(true) tanpa buka "
      + "Settings; belum = bukakan Settings + GotPermission(false + pesan). "
      + "Hasil akhir (user mengaktifkan atau tidak) cek via IsExactAlarmAllowed "
      + "sepulang dari Settings. Tanpa return.")
  public void CallSetExactAlarmPermission() {
    task.CallSetExactAlarmPermission();
  }

  @SimpleFunction(description = "True bila boleh pasang exact-alarm "
      + "(Android <12 selalu true).")
  public boolean IsExactAlarmAllowed() {
    return task.IsExactAlarmAllowed();
  }

  @SimpleFunction(description = "CALL: Pasang alarm SISTEM sekali bunyi: id + epoch "
      + "mili-detik + payload + exact (true = tepat waktu bila izin ada). "
      + "Bunyi = membuka aplikasi; baca via CallCheckStartupAlarm/AlarmFired. "
      + "Mau jam harian tanpa epoch = CallScheduleDailyAt. Tersimpan di HP; "
      + "untuk hidup lagi habis reboot, panggil CallRestoreAlarms di Initialize.")
  public void CallScheduleAlarm(int id, long epochMillis, String payload, boolean exact) {
    task.CallScheduleAlarm(id, epochMillis, payload, exact);
  }

  @SimpleFunction(description = "CALL: Pasang alarm SISTEM berulang: id + epoch "
      + "pertama + interval + payload. Interval <1 detik dibulatkan ke 1 detik "
      + "(hemat baterai, aturan Android). Tersimpan di HP; untuk hidup lagi "
      + "habis reboot, panggil CallRestoreAlarms di Initialize.")
  public void CallScheduleRepeatingAlarm(int id, long firstEpochMillis, long intervalMillis, String payload) {
    task.CallScheduleRepeatingAlarm(id, firstEpochMillis, intervalMillis, payload);
  }

  @SimpleFunction(description = "CALL: alarm HARIAN jam dinding tanpa mikir epoch: "
      + "id + jam 0-23 + menit 0-59 + payload. Bunyi tiap hari jam itu (interval "
      + "24 jam, mulai kejadian terdekat: lewat jam hari ini = besok). Salah "
      + "jam/menit = ErrorOccurred. Batal via SetCancelAlarm.")
  public void CallScheduleDailyAt(int id, int hour, int minute, String payload) {
    task.CallScheduleDailyAt(id, hour, minute, payload);
  }

  @SimpleFunction(description = "SET: Cabut alarm sistem ber-id (tugas dalam-aplikasi "
      + "dengan id sama tidak ikut; pakai CallCancelTask bila perlu).")
  public void SetCancelAlarm(int id) {
    task.SetCancelAlarm(id);
  }

  @SimpleFunction(description = "SET: Cabut SEMUA alarm sistem Tools_Task.")
  public void SetCancelAllAlarms() {
    task.SetCancelAllAlarms();
  }

  @SimpleFunction(description = "True bila alarm sistem ber-id terpasang.")
  public boolean IsAlarmSet(int id) {
    return task.IsAlarmSet(id);
  }

  @SimpleFunction(description = "Semua id alarm sistem yang terpasang "
      + "(URUT PEMBUATAN).")
  public List<Integer> GetPendingAlarmIds() {
    return task.GetPendingAlarmIds();
  }

  @SimpleFunction(description = "Fallback polling alarm: list [index 1=epochMillis, "
      + "2=intervalMillis (0 = sekali), 3=payload]. Kosong = tidak ada.")
  public List<Object> GetAlarmInfo(int id) {
    return task.GetAlarmInfo(id);
  }

  @SimpleFunction(description = "CALL: WAJIB dipanggil di Screen.Initialize (dan "
      + "timer bila perlu): cek apakah aplikasi dibuka oleh alarm Tools_Task. "
      + "Tanpa return: bila dibuka oleh alarm dan datanya baru, menembak AlarmFired.")
  public void CallCheckStartupAlarm() {
    task.CallCheckStartupAlarm();
  }

  @SimpleFunction(description = "CALL: Pasang ulang SEMUA alarm tersimpan (habis "
      + "reboot: WAJIB panggil di Screen.Initialize; alarm TIDAK hidup sendiri). "
      + "Selesai menembak AlarmsRestored (jumlah dipasang ulang; 0 = tidak ada "
      + "tersimpan).")
  public void CallRestoreAlarms() {
    task.CallRestoreAlarms();
  }

  @SimpleFunction(description = "SET: Tampilkan notifikasi ongoing (tetap) agar tugas "
      + "penting tidak dibunuh sistem. Tap membuka aplikasi.")
  public void SetShowOngoingNotification(int id, String title, String text) {
    task.SetShowOngoingNotification(id, title, text);
  }

  @SimpleFunction(description = "SET: Hapus notifikasi ongoing ber-id.")
  public void SetHideOngoingNotification(int id) {
    task.SetHideOngoingNotification(id);
  }

  @SimpleFunction(description = "SET: Jaga CPU tetap hidup (wake-lock parsial) agar "
      + "tugas tidak tidur. timeoutMillis 0 = sampai SetKeepAwake(false). "
      + "Pakai seperlunya (boros baterai).")
  public void SetKeepAwake(boolean on, long timeoutMillis) {
    task.SetKeepAwake(on, timeoutMillis);
  }

  @SimpleFunction(description = "True bila wake-lock sedang dipegang.")
  public boolean IsAwake() {
    return task.IsAwake();
  }

  @SimpleEvent(description = "Tugas dalam-aplikasi dimulai: id + payload.")
  public void TaskStarted(int id, String payload) {
    EventDispatcher.dispatchEvent(this, "TaskStarted", id, payload);
  }

  @SimpleEvent(description = "Tugas berulang tick ke-n: id + tick + payload.")
  public void TaskTick(int id, int tick, String payload) {
    EventDispatcher.dispatchEvent(this, "TaskTick", id, tick, payload);
  }

  @SimpleEvent(description = "Tugas selesai: success + payload + message.")
  public void TaskFinished(int id, boolean success, String payload, String message) {
    EventDispatcher.dispatchEvent(this, "TaskFinished", id, success, payload, message);
  }

  @SimpleEvent(description = "Tugas dibatalkan: id.")
  public void TaskCancelled(int id) {
    EventDispatcher.dispatchEvent(this, "TaskCancelled", id);
  }

  @SimpleEvent(description = "Alarm sistem bunyi (aplikasi dibuka oleh alarm): "
      + "id + payload. Selalu cek juga via CallCheckStartupAlarm di Initialize.")
  public void AlarmFired(int id, String payload) {
    EventDispatcher.dispatchEvent(this, "AlarmFired", id, payload);
  }

  @SimpleEvent(description = "Restore alarm selesai (hasil CallRestoreAlarms): "
      + "jumlah alarm dipasang ulang (0 = tidak ada tersimpan). Habis reboot = "
      + "panggil CallRestoreAlarms di Initialize, alarm TIDAK hidup sendiri.")
  public void AlarmsRestored(int count) {
    EventDispatcher.dispatchEvent(this, "AlarmsRestored", count);
  }

  @SimpleProperty(description = "Nama operasi Task (untuk if di ErrorOccurred: "
      + "cth if component = OpTask berarti error tugas).",
      category = PropertyCategory.BEHAVIOR)
  public String OpTask() {
    return task.OpTask();
  }

  @SimpleProperty(description = "Nama operasi Alarm (untuk if di ErrorOccurred: "
      + "cth if component = OpAlarm berarti error alarm).",
      category = PropertyCategory.BEHAVIOR)
  public String OpAlarm() {
    return task.OpAlarm();
  }

  @SimpleProperty(description = "Nama operasi Var (untuk if di ErrorOccurred: "
      + "cth if component = OpVar berarti error variabel).",
      category = PropertyCategory.BEHAVIOR)
  public String OpVar() {
    return task.OpVar();
  }

  // ================= Video =================

  @SimpleFunction(description = "CALL: Putar video ber-id di dalam layout (Arrangement MIT / "
      + "dinamis). Bisa jalan bareng (satu id = satu video). path file (/Download/... atau "
      + "absolut apa adanya), atau http(s) URL (streaming, izin INTERNET otomatis dibawa). "
      + "id dari CreateVideoId atau CreateUniqueId Tools_Base. "
      + "Atur tinggi wadah (mis. 300px) agar video kelihatan. Id yang masih main tidak bisa "
      + "dipakai ulang (Stop/Pause dulu). Id yang sama diputar di wadah lain = PINDAH "
      + "(lepas dari wadah lama). Stop lalu Play = mulai dari awal. Double-tap "
      + "mati secara default (nyalakan via DoubleTapSeek bila butuh). Video mengikuti ukuran "
      + "wadah (bisa gepeng bila rasio beda; samakan rasio wadah dengan video). Controller "
      + "bawaan + slider sendiri bisa rebutan: pilih satu. Sukses menembak VideoStarted; "
      + "gagal via ErrorOccurred.")
  public void CallPlayVideoIn(final AndroidViewComponent container, final String path, final int id) {
    video.CallPlayVideoIn(container, path, id);
  }

  @SimpleFunction(description = "SET: Jeda video yang sedang main. Tanpa feedback. "
      + "Cek status via IsVideoPaused.")
  public void SetPauseVideo(int id) {
    video.SetPauseVideo(id);
  }

  @SimpleFunction(description = "SET: Lanjutkan video yang dijeda. Tanpa feedback. "
      + "Cek status via IsVideoPaused.")
  public void SetResumeVideo(int id) {
    video.SetResumeVideo(id);
  }

  @SimpleFunction(description = "SET: Hentikan video + lepas dari layout. "
      + "Id tetap tercatat (Play lagi langsung bisa, mulai dari awal). Tanpa event. "
      + "Bersih total via RemoveVideoId.")
  public void SetStopVideo(int id) {
    video.SetStopVideo(id);
  }

  @SimpleFunction(description = "SET: Lompat video ke posisi milidetik. Bisa dipasang ke "
      + "Slider (matikan controller bawaan bila rebutan). Tanpa feedback.")
  public void SetSeekVideo(int id, int position) {
    video.SetSeekVideo(id, position);
  }

  @SimpleFunction(description = "SET: Maju video sejauh SkipSeconds DETIK. "
      + "Tanpa feedback.")
  public void SetForwardVideo(int id) {
    video.SetForwardVideo(id);
  }

  @SimpleFunction(description = "SET: Mundur video sejauh SkipSeconds DETIK. "
      + "Tanpa feedback.")
  public void SetBackwardVideo(int id) {
    video.SetBackwardVideo(id);
  }

  @SimpleFunction(description = "Posisi video sekarang dalam milidetik. 0 bila belum siap.")
  public int GetVideoPosition(int id) {
    return video.GetVideoPosition(id);
  }

  @SimpleFunction(description = "Durasi video dalam milidetik. 0 bila belum siap.")
  public int GetVideoDuration(int id) {
    return video.GetVideoDuration(id);
  }

  @SimpleFunction(description = "SET: Hapus SEMUA video Tools_Video dari layout itu (id ikut dilepas, "
      + "komponen lain tidak diusik). Video ikut di-pause.")
  public void SetClearVideo(final AndroidViewComponent container) {
    video.SetClearVideo(container);
  }

  @SimpleFunction(description = "Buat id video unik baru (Number, siap pakai ke CallPlayVideoIn).")
  public int CreateVideoId() {
    return video.CreateVideoId();
  }

  @SimpleFunction(description = "Semua id video yang dipegang (URUT PEMBUATAN, "
      + "index 1 = pertama dibuat).")
  public List<Integer> GetVideoIds() {
    return video.GetVideoIds();
  }

  @SimpleFunction(description = "Id video yang sedang main (URUT PEMBUATAN). "
      + "Yang dijeda/berhenti TIDAK masuk sini.")
  public List<Integer> GetActiveVideoIds() {
    return video.GetActiveVideoIds();
  }

  @SimpleFunction(description = "Id video yang terakhir disentuh (dibuat atau di-start). "
      + "-1 bila kosong. Beda dengan GetLastVideoId (terakhir dibuat).")
  public int CurrentVideoId() {
    return video.CurrentVideoId();
  }

  @SimpleFunction(description = "Id index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstVideoId() {
    return video.GetFirstVideoId();
  }

  @SimpleFunction(description = "Id video TERAKHIR DIBUAT. -1 bila kosong. Beda dengan "
      + "CurrentVideoId (terakhir disentuh).")
  public int GetLastVideoId() {
    return video.GetLastVideoId();
  }

  @SimpleFunction(description = "Id yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.")
  public int GetPreviousVideoId(int id) {
    return video.GetPreviousVideoId(id);
  }

  @SimpleFunction(description = "True bila id video ini sedang main. False = jeda, "
      + "berhenti, atau belum ada (bedakan jeda vs berhenti via IsVideoPaused).")
  public boolean IsVideoIdActive(int id) {
    return video.IsVideoIdActive(id);
  }

  @SimpleFunction(description = "True bila id video ini sedang dijeda (Pause tapi "
      + "belum Stop/selesai). False = main/berhenti/belum ada.")
  public boolean IsVideoPaused(int id) {
    return video.IsVideoPaused(id);
  }

  @SimpleFunction(description = "Volume khusus satu id (0-100). = volume global "
      + "bila belum diatur via SetVideoVolume.")
  public int GetVideoVolume(int id) {
    return video.GetVideoVolume(id);
  }

  @SimpleFunction(description = "SET: Volume khusus satu id (0-100). Menimpa "
      + "volume global untuk id ini. Tanpa feedback.")
  public void SetVideoVolume(int id, int volume) {
    video.SetVideoVolume(id, volume);
  }

  @SimpleFunction(description = "Loop khusus satu id. = loop global bila belum "
      + "diatur via SetVideoLooping.")
  public boolean GetVideoLooping(int id) {
    return video.GetVideoLooping(id);
  }

  @SimpleFunction(description = "SET: Loop khusus satu id (true = mengulang bila "
      + "selesai). Menimpa loop global untuk id ini. Berlaku untuk putaran "
      + "berikutnya. Tanpa feedback.")
  public void SetVideoLooping(int id, boolean loop) {
    video.SetVideoLooping(id, loop);
  }

  @SimpleFunction(description = "True bila id video ini sedang dibisukan via "
      + "SetVideoMuted.")
  public boolean IsVideoMuted(int id) {
    return video.IsVideoMuted(id);
  }

  @SimpleFunction(description = "SET: Bisukan/suarakan satu id. Bisu = volume 0 "
      + "sementara (nilai lama disimpan, pulih saat unmute). Tanpa feedback.")
  public void SetVideoMuted(int id, boolean muted) {
    video.SetVideoMuted(id, muted);
  }

  @SimpleFunction(description = "SET: Maju video sejauh seconds DETIK "
      + "(tanpa ubah SkipSeconds). Tanpa feedback.")
  public void SetForwardVideoBy(int id, int seconds) {
    video.SetForwardVideoBy(id, seconds);
  }

  @SimpleFunction(description = "SET: Mundur video sejauh seconds DETIK "
      + "(tanpa ubah SkipSeconds). Tanpa feedback.")
  public void SetBackwardVideoBy(int id, int seconds) {
    video.SetBackwardVideoBy(id, seconds);
  }

  @SimpleFunction(description = "SET: Hapus SEMUA video (semua layout) + buang "
      + "semua id. Tanpa feedback.")
  public void SetClearVideos() {
    video.SetClearVideos();
  }

  @SimpleFunction(description = "Hapus id video: hentikan + lepas dari layout + lepas id "
      + "(otomatis beres-beres).")
  public void RemoveVideoId(int id) {
    video.RemoveVideoId(id);
  }

  @SimpleProperty(description = "Tampilkan kontrol bawaan video (ada tombol play + geser bawaan). "
      + "Default true. Matikan bila pakai slider sendiri (bisa rebutan).",
      category = PropertyCategory.BEHAVIOR)
  public boolean ShowVideoController() {
    return video.ShowVideoController();
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
      defaultValue = "True")
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void ShowVideoController(boolean show) {
    video.ShowVideoController(show);
  }

  @SimpleProperty(description = "Loop global (default false). Id yang sudah diatur via "
      + "SetVideoLooping memakai nilainya sendiri.",
      category = PropertyCategory.BEHAVIOR)
  public boolean VideoLooping() {
    return video.VideoLooping();
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
      defaultValue = "False")
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void VideoLooping(boolean loop) {
    video.VideoLooping(loop);
  }

  @SimpleProperty(description = "Double-tap video: kiri = mundur, kanan = maju, "
      + "tengah = pause/resume. Default false (matikan perilaku tak terduga; "
      + "nyalakan bila butuh).",
      category = PropertyCategory.BEHAVIOR)
  public boolean DoubleTapSeek() {
    return video.DoubleTapSeek();
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
      defaultValue = "False")
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void DoubleTapSeek(boolean enabled) {
    video.DoubleTapSeek(enabled);
  }

  @SimpleEvent(description = "Video mulai main: id + durasi (ms) diketahui.")
  public void VideoStarted(int id, int duration) {
    EventDispatcher.dispatchEvent(this, "VideoStarted", id, duration);
  }

  @SimpleEvent(description = "Progres video berubah: id + posisi + durasi (ms) + persen 0-100.")
  public void VideoProgressChanged(int id, int position, int duration, int percent) {
    EventDispatcher.dispatchEvent(this, "VideoProgressChanged", id, position, duration, percent);
  }

  @SimpleEvent(description = "Video selesai (tidak loop). Putar lagi via CallPlayVideoIn.")
  public void VideoFinished(int id) {
    EventDispatcher.dispatchEvent(this, "VideoFinished", id);
  }

  @SimpleProperty(description = "Nama operasi Video untuk if di ErrorOccurred. Contoh: "
      + "colok ke perbandingan = dengan component dari event ErrorOccurred.",
      category = PropertyCategory.BEHAVIOR)
  public String OpVideo() {
    return video.OpVideo();
  }

}
