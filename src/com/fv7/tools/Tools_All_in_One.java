package com.fv7.tools;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.AndroidViewComponent;
import com.google.appinventor.components.runtime.Component;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.util.YailList;

import java.util.List;

@DesignerComponent(
    version = 18,
    description = "Tools_All_in_One: SATU komponen berisi SEMUA blok 12 Tools_* "
        + "(Audio/Base/Compress/Core/Downloader/File/Image/Layout/Sheet/Style/Task/Video). "
        + "Fasad: semua blok diteruskan ke mesin aslinya, semua event dicerminkan ke sini. "
        + "Cocok bila tidak mau banyak komponen di daftar blok. "
        + "(v18 2026-11-18: komponen baru.)",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    androidMinSdk = 21,
    iconName = "aiwebres/tools_all_in_one.jpg")
@SimpleObject(external = true)
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

  @SimpleFunction(description = "CALL: Putar audio ber-id (background, tanpa tampilan). Bisa jalan bareng "
      + "(satu id = satu audio). path file (/Download/..., absolut apa adanya), "
      + "file:/// absolut, atau http(s) URL (streaming). id dari CreateAudioId / "
      + "Tools_Base CreateUniqueId (reservasi diadopsi). "
      + "Tampilan (Slider/Label) bikin sendiri dari AudioStarted/ProgressChanged/Finished. "
      + "Sukses menembak AudioStarted; gagal via ErrorOccurred.")
  public void CallPlayAudio(String path, int id) {
    audio.CallPlayAudio(path, id);
  }

  @SimpleFunction(description = "SET: jeda audio yang sedang bunyi (per id). Tanpa feedback.")
  public void SetPauseAudio(int id) {
    audio.SetPauseAudio(id);
  }

  @SimpleFunction(description = "SET: lanjutkan audio yang dijeda (per id). Tanpa feedback.")
  public void SetResumeAudio(int id) {
    audio.SetResumeAudio(id);
  }

  @SimpleFunction(description = "SET: Hentikan audio (per id). Putar lagi via CallPlayAudio.")
  public void SetStopAudio(int id) {
    audio.SetStopAudio(id);
  }

  @SimpleFunction(description = "SET: Lompat audio ke posisi (ms). Bisa dipasang ke Slider. (per id)")
  public void SetSeekAudio(int id, int position) {
    audio.SetSeekAudio(id, position);
  }

  @SimpleFunction(description = "SET: Maju audio sejauh SkipSeconds detik. (per id)")
  public void SetForwardAudio(int id) {
    audio.SetForwardAudio(id);
  }

  @SimpleFunction(description = "SET: Mundur audio sejauh SkipSeconds detik. (per id)")
  public void SetBackwardAudio(int id) {
    audio.SetBackwardAudio(id);
  }

  @SimpleFunction(description = "Posisi audio sekarang (ms). (per id)")
  public int GetAudioPosition(int id) {
    return audio.GetAudioPosition(id);
  }

  @SimpleFunction(description = "Durasi audio (ms). 0 bila belum siap. (per id)")
  public int GetAudioDuration(int id) {
    return audio.GetAudioDuration(id);
  }

  @SimpleFunction(description = "True bila audio id ini sedang bunyi.")
  public boolean IsAudioPlaying(int id) {
    return audio.IsAudioPlaying(id);
  }

  @SimpleFunction(description = "Buat id audio unik baru (Number, siap pakai ke CallPlayAudio).")
  public int CreateAudioId() {
    return audio.CreateAudioId();
  }

  @SimpleFunction(description = "Semua id audio (URUT PEMBUATAN, index 1 = pertama dibuat).")
  public List<Integer> GetAudioIds() {
    return base.GetAudioIds();
  }

  @SimpleFunction(description = "Id audio yang sedang bunyi (URUT PEMBUATAN).")
  public List<Integer> GetActiveAudioIds() {
    return base.GetActiveAudioIds();
  }

  @SimpleFunction(description = "Id audio terakhir dibuat/di-start. -1 bila kosong.")
  public int CurrentAudioId() {
    return base.CurrentAudioId();
  }

  @SimpleFunction(description = "Id audio index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstAudioId() {
    return base.GetFirstAudioId();
  }

  @SimpleFunction(description = "Id audio TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastAudioId() {
    return base.GetLastAudioId();
  }

  @SimpleFunction(description = "Id audio yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.")
  public int GetPreviousAudioId(int id) {
    return base.GetPreviousAudioId(id);
  }

  @SimpleFunction(description = "True bila id audio ini sedang bunyi.")
  public boolean IsAudioIdActive(int id) {
    return base.IsAudioIdActive(id);
  }

  @SimpleFunction(description = "Hapus id audio: hentikan bila bunyi + lepas id.")
  public void RemoveAudioId(int id) {
    base.RemoveAudioId(id);
  }

  @SimpleProperty(description = "Audio mengulang otomatis bila selesai. Default false.",
      category = PropertyCategory.BEHAVIOR)
  public boolean AudioLooping() {
    return audio.AudioLooping();
  }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void AudioLooping(boolean loop) {
    audio.AudioLooping(loop);
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

  @SimpleEvent(description = "SATU-SATUNYA error Tools_Audio.")
  public void ErrorOccurred(String component, int id, String message) {
    EventDispatcher.dispatchEvent(this, "ErrorOccurred", component, id, message);
  }

  @SimpleProperty(description = "Nama operasi Audio (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpAudio() {
    return audio.OpAudio();
  }

  @SimpleFunction(description = "Fallback polling untuk event ErrorOccurred: "
      + "pesan error terakhir operasi ini (Id/Json/Tag, kosong bila belum ada).")
  public String LastError(String operation) {
    return base.LastError(operation);
  }

  @SimpleFunction(description = "Buat id unik global baru (Number). Reservasi otomatis, "
      + "siap pakai ke CallDownload / CallShowImage / CallPlayAudio / CallPlayVideoIn "
      + "(reservasi otomatis diadopsi). Urutan pembuatan = urutan list.")
  public int CreateUniqueId() {
    return base.CreateUniqueId();
  }

  @SimpleFunction(description = "Id yang disarankan: angka terkecil >= 1 yang belum dipakai "
      + "siapa pun (tidak di-reservasi, langsung pakai).")
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

  @SimpleFunction(description = "Lepas paksa id yang macet. Jangan dipakai saat operasinya "
      + "masih jalan (pakai Cancel/Stop/Remove dulu).")
  public void ReleaseId(int id) {
    base.ReleaseId(id);
  }

  @SimpleFunction(description = "Semua id unduhan (URUT PEMBUATAN, index 1 = pertama dibuat).")
  public List<Integer> GetDownloadIds() {
    return base.GetDownloadIds();
  }

  @SimpleFunction(description = "Id unduhan yang sedang jalan (URUT PEMBUATAN). "
      + "Yang dijeda TIDAK masuk sini.")
  public List<Integer> GetActiveDownloadIds() {
    return base.GetActiveDownloadIds();
  }

  @SimpleFunction(description = "Id unduhan terakhir dibuat/di-start. -1 bila kosong.")
  public int CurrentDownloadId() {
    return base.CurrentDownloadId();
  }

  @SimpleFunction(description = "Id unduhan index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstDownloadId() {
    return base.GetFirstDownloadId();
  }

  @SimpleFunction(description = "Id unduhan TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastDownloadId() {
    return base.GetLastDownloadId();
  }

  @SimpleFunction(description = "Id unduhan yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.")
  public int GetPreviousDownloadId(int id) {
    return base.GetPreviousDownloadId(id);
  }

  @SimpleFunction(description = "True bila id unduhan ini sedang jalan.")
  public boolean IsDownloadIdActive(int id) {
    return base.IsDownloadIdActive(id);
  }

  @SimpleFunction(description = "Hapus id unduhan: batalkan bila jalan + lepas id.")
  public void RemoveDownloadId(int id) {
    base.RemoveDownloadId(id);
  }

  @SimpleFunction(description = "Semua id gambar (URUT PEMBUATAN, index 1 = pertama dibuat).")
  public List<Integer> GetImageIds() {
    return base.GetImageIds();
  }

  @SimpleFunction(description = "Id gambar yang sedang tampil/loading (URUT PEMBUATAN).")
  public List<Integer> GetActiveImageIds() {
    return base.GetActiveImageIds();
  }

  @SimpleFunction(description = "Id gambar terakhir dibuat/di-start. -1 bila kosong.")
  public int CurrentImageId() {
    return base.CurrentImageId();
  }

  @SimpleFunction(description = "Id gambar index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstImageId() {
    return base.GetFirstImageId();
  }

  @SimpleFunction(description = "Id gambar TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastImageId() {
    return base.GetLastImageId();
  }

  @SimpleFunction(description = "Id gambar yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.")
  public int GetPreviousImageId(int id) {
    return base.GetPreviousImageId(id);
  }

  @SimpleFunction(description = "True bila id gambar ini sedang tampil/loading.")
  public boolean IsImageIdActive(int id) {
    return base.IsImageIdActive(id);
  }

  @SimpleFunction(description = "Hapus id gambar: lepas dari layout + lepas id.")
  public void RemoveImageId(int id) {
    base.RemoveImageId(id);
  }

  @SimpleFunction(description = "Semua id video (URUT PEMBUATAN, index 1 = pertama dibuat).")
  public List<Integer> GetVideoIds() {
    return base.GetVideoIds();
  }

  @SimpleFunction(description = "Id video yang sedang main (URUT PEMBUATAN).")
  public List<Integer> GetActiveVideoIds() {
    return base.GetActiveVideoIds();
  }

  @SimpleFunction(description = "Id video terakhir dibuat/di-start. -1 bila kosong.")
  public int CurrentVideoId() {
    return base.CurrentVideoId();
  }

  @SimpleFunction(description = "Id video index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstVideoId() {
    return base.GetFirstVideoId();
  }

  @SimpleFunction(description = "Id video TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastVideoId() {
    return base.GetLastVideoId();
  }

  @SimpleFunction(description = "Id video yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.")
  public int GetPreviousVideoId(int id) {
    return base.GetPreviousVideoId(id);
  }

  @SimpleFunction(description = "True bila id video ini sedang main.")
  public boolean IsVideoIdActive(int id) {
    return base.IsVideoIdActive(id);
  }

  @SimpleFunction(description = "Hapus id video: hentikan + lepas layout + lepas id.")
  public void RemoveVideoId(int id) {
    base.RemoveVideoId(id);
  }

  @SimpleFunction(description = "Semua id dinamis Tools_Core (URUT PEMBUATAN, "
      + "index 1 = pertama dibuat).")
  public List<Integer> GetDynamicIds() {
    return base.GetDynamicIds();
  }

  @SimpleFunction(description = "Id dinamis yang aktif (URUT PEMBUATAN).")
  public List<Integer> GetActiveDynamicIds() {
    return base.GetActiveDynamicIds();
  }

  @SimpleFunction(description = "Id dinamis terakhir dibuat. -1 bila kosong.")
  public int CurrentDynamicId() {
    return base.CurrentDynamicId();
  }

  @SimpleFunction(description = "Id dinamis index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstDynamicId() {
    return base.GetFirstDynamicId();
  }

  @SimpleFunction(description = "Id dinamis TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastDynamicId() {
    return base.GetLastDynamicId();
  }

  @SimpleFunction(description = "Id dinamis yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.")
  public int GetPreviousDynamicId(int id) {
    return base.GetPreviousDynamicId(id);
  }

  @SimpleFunction(description = "True bila id dinamis ini masih ada.")
  public boolean IsDynamicIdActive(int id) {
    return base.IsDynamicIdActive(id);
  }

  @SimpleFunction(description = "Hapus id dinamis: lepas view + lepas id.")
  public void RemoveDynamicId(int id) {
    base.RemoveDynamicId(id);
  }

  @SimpleFunction(description = "Semua id layout Tools_Layout (URUT PEMBUATAN, "
      + "index 1 = pertama dibuat).")
  public List<Integer> GetLayoutIds() {
    return base.GetLayoutIds();
  }

  @SimpleFunction(description = "Id layout yang aktif (URUT PEMBUATAN).")
  public List<Integer> GetActiveLayoutIds() {
    return base.GetActiveLayoutIds();
  }

  @SimpleFunction(description = "Id layout terakhir dibuat. -1 bila kosong.")
  public int CurrentLayoutId() {
    return base.CurrentLayoutId();
  }

  @SimpleFunction(description = "Id layout index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstLayoutId() {
    return base.GetFirstLayoutId();
  }

  @SimpleFunction(description = "Id layout TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastLayoutId() {
    return base.GetLastLayoutId();
  }

  @SimpleFunction(description = "Id layout yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.")
  public int GetPreviousLayoutId(int id) {
    return base.GetPreviousLayoutId(id);
  }

  @SimpleFunction(description = "True bila id layout ini masih ada.")
  public boolean IsLayoutIdActive(int id) {
    return base.IsLayoutIdActive(id);
  }

  @SimpleFunction(description = "Hapus id layout: lepas dari induk (anak dinamis "
      + "di dalamnya ikut beres) + lepas id.")
  public void RemoveLayoutId(int id) {
    base.RemoveLayoutId(id);
  }

  @SimpleFunction(description = "Semua id sheet Tools_Sheet (URUT PEMBUATAN, "
      + "index 1 = pertama dibuat).")
  public List<Integer> GetSheetIds() {
    return base.GetSheetIds();
  }

  @SimpleFunction(description = "Id sheet yang aktif (URUT PEMBUATAN).")
  public List<Integer> GetActiveSheetIds() {
    return base.GetActiveSheetIds();
  }

  @SimpleFunction(description = "Id sheet terakhir dibuat. -1 bila kosong.")
  public int CurrentSheetId() {
    return base.CurrentSheetId();
  }

  @SimpleFunction(description = "Id sheet index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstSheetId() {
    return base.GetFirstSheetId();
  }

  @SimpleFunction(description = "Id sheet TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastSheetId() {
    return base.GetLastSheetId();
  }

  @SimpleFunction(description = "Id sheet yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.")
  public int GetPreviousSheetId(int id) {
    return base.GetPreviousSheetId(id);
  }

  @SimpleFunction(description = "True bila id sheet ini masih terdaftar.")
  public boolean IsSheetIdActive(int id) {
    return base.IsSheetIdActive(id);
  }

  @SimpleFunction(description = "Hapus id sheet: tutup diam-diam + lepas id.")
  public void RemoveSheetId(int id) {
    base.RemoveSheetId(id);
  }

  @SimpleFunction(description = "Semua id slider Tools_Image (URUT PEMBUATAN, "
      + "index 1 = pertama dibuat).")
  public List<Integer> GetSliderIds() {
    return base.GetSliderIds();
  }

  @SimpleFunction(description = "Id slider yang aktif (URUT PEMBUATAN).")
  public List<Integer> GetActiveSliderIds() {
    return base.GetActiveSliderIds();
  }

  @SimpleFunction(description = "Id slider terakhir dibuat. -1 bila kosong.")
  public int CurrentSliderId() {
    return base.CurrentSliderId();
  }

  @SimpleFunction(description = "Id slider index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstSliderId() {
    return base.GetFirstSliderId();
  }

  @SimpleFunction(description = "Id slider TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastSliderId() {
    return base.GetLastSliderId();
  }

  @SimpleFunction(description = "Id slider yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.")
  public int GetPreviousSliderId(int id) {
    return base.GetPreviousSliderId(id);
  }

  @SimpleFunction(description = "True bila id slider ini masih ada.")
  public boolean IsSliderIdActive(int id) {
    return base.IsSliderIdActive(id);
  }

  @SimpleFunction(description = "Hapus id slider: lepas dari layout (autoplay "
      + "mati) + lepas id.")
  public void RemoveSliderId(int id) {
    base.RemoveSliderId(id);
  }

  @SimpleFunction(description = "Semua id tugas/alarm Tools_Task (URUT PEMBUATAN, "
      + "index 1 = pertama dibuat).")
  public List<Integer> GetTaskIds() {
    return base.GetTaskIds();
  }

  @SimpleFunction(description = "Id tugas/alarm yang aktif (URUT PEMBUATAN).")
  public List<Integer> GetActiveTaskIds() {
    return base.GetActiveTaskIds();
  }

  @SimpleFunction(description = "Id tugas terakhir dibuat. -1 bila kosong.")
  public int CurrentTaskId() {
    return base.CurrentTaskId();
  }

  @SimpleFunction(description = "Id tugas index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstTaskId() {
    return base.GetFirstTaskId();
  }

  @SimpleFunction(description = "Id tugas TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastTaskId() {
    return base.GetLastTaskId();
  }

  @SimpleFunction(description = "Id tugas yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.")
  public int GetPreviousTaskId(int id) {
    return base.GetPreviousTaskId(id);
  }

  @SimpleFunction(description = "True bila id tugas ini aktif (jalan/jeda/alarm terpasang).")
  public boolean IsTaskIdActive(int id) {
    return base.IsTaskIdActive(id);
  }

  @SimpleFunction(description = "Hapus id tugas: batalkan jadwal + cabut alarm "
      + "sistem + lepas id.")
  public void RemoveTaskId(int id) {
    base.RemoveTaskId(id);
  }

  @SimpleFunction(description = "Buat id unik baru di tag ini (Number, angka terkecil yang belum "
      + "dipakai, langsung di-reservasi). Untuk saat kamulah yang menentukan id-nya "
      + "(mis. bikin tab sendiri). Beda dengan SaveTagId yang mencatat id dari luar.")
  public int CreateTagId(String tag) {
    return base.CreateTagId(tag);
  }

  @SimpleFunction(description = "Simpan id komponen LUAR ke tag (mis. WebView pihak ketiga). "
      + "id boleh Number ATAU teks angka (\"5\" = 5, otomatis dikonversi). "
      + "Id dobel = tetap satu. Gagal (tag kosong / bukan angka) via ErrorOccurred.")
  public void SaveTagId(Object id, String tag) {
    base.SaveTagId(id, tag);
  }

  @SimpleFunction(description = "Semua id di tag ini (Number semua, URUT MENYIMPAN, "
      + "index 1 = pertama disimpan).")
  public List<Integer> GetTagIds(String tag) {
    return base.GetTagIds(tag);
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
      + "tidak diusik). Tag yang kosong ikut dibuang. Tidak tercatat via ErrorOccurred.")
  public void RemoveTagId(Object id, String tag) {
    base.RemoveTagId(id, tag);
  }

  @SimpleFunction(description = "Buang SELURUH isi satu tag.")
  public void ClearTag(String tag) {
    base.ClearTag(tag);
  }

  @SimpleFunction(description = "Semua nama tag yang masih punya isi (URUT DIBUAT).")
  public List<String> GetAllTags() {
    return base.GetAllTags();
  }

  @SimpleFunction(description = "Encode list/dictionary/teks jadi JSON.")
  public String JsonEncode(Object value) {
    return base.JsonEncode(value);
  }

  @SimpleFunction(description = "Decode teks JSON jadi dictionary/list. Gagal = \"\" + error.")
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

  @SimpleFunction(description = "Versi paket Tools (sama untuk semua Tools_* dalam satu .aix).")
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

  @SimpleFunction(description = "Deteksi jenis file dari ISINYA (bukan ekstensi). Kembali: zip, "
      + "rar, 7z, gz, bz2, xz, tar, apk, pdf, png, jpg, gif, mp3, mp4, atau unknown. "
      + "Instan, tanpa id.")
  public String DetectArchiveType(String path) {
    return compress.DetectArchiveType(path);
  }

  @SimpleFunction(description = "Daftar isi ZIP (nama entri). Instan. Kosong bila bukan ZIP.")
  public List<String> ListZipContents(String zipPath) {
    return compress.ListZipContents(zipPath);
  }

  @SimpleFunction(description = "CALL: bikin ZIP dari BANYAK file sekaligus (tanpa loop). paths = "
      + "list path sumber. destZip = path file zip tujuan. Mulai = menembak Started; gagal via ErrorOccurred. "
      + "Pantau via ZipStarted/ProgressChanged/Finished. Satu proses ZIP per saat.")
  public void CallZipFiles(Object paths, String destZip) {
    compress.CallZipFiles(paths, destZip);
  }

  @SimpleFunction(description = "CALL: ekstrak ZIP ke folder. Struktur folder di dalam ZIP dijaga. "
      + "Batalkan via SetCancelZip. Pantau via UnzipStarted/ProgressChanged/Finished.")
  public void CallUnzip(String zipPath, String destFolder) {
    compress.CallUnzip(zipPath, destFolder);
  }

  @SimpleFunction(description = "SET: batalkan proses ZIP/Unzip berjalan. Tanpa feedback.")
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

  @SimpleEvent(description = "Progres bikin ZIP berubah: index/total + nama file + persen.")
  public void ZipProgressChanged(int index, int total, String fileName, int filePercent) {
    EventDispatcher.dispatchEvent(this, "ZipProgressChanged", index, total, fileName, filePercent);
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

  @SimpleEvent(description = "Progres ekstrak berubah: index/total + nama entri + persen.")
  public void UnzipProgressChanged(int index, int total, String entryName, int entryPercent) {
    EventDispatcher.dispatchEvent(this, "UnzipProgressChanged", index, total, entryName, entryPercent);
  }

  @SimpleEvent(description = "Ekstrak selesai: success + folder + jumlah ok/gagal.")
  public void UnzipFinished(boolean success, String destFolder, int doneCount, int failCount, String message) {
    EventDispatcher.dispatchEvent(this, "UnzipFinished", success, destFolder, doneCount, failCount, message);
  }

  @SimpleProperty(description = "Nama operasi Zip (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpZip() {
    return compress.OpZip();
  }

  @SimpleProperty(description = "Nama operasi Unzip (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpUnzip() {
    return compress.OpUnzip();
  }

  @SimpleFunction(description = "Bikin komponen MIT dinamis ber-id ke dalam layout "
      + "(Arrangement MIT / dinamis). typeName: Button, Label, TextBox, Image, "
      + "CheckBox, Switch, Slider, Spinner, ListView, DatePicker, TimePicker, "
      + "HorizontalArrangement, VerticalArrangement, HorizontalScrollArrangement, "
      + "VerticalScrollArrangement. id = penanda (minta ke CreateDynamicId, "
      + "reservasi otomatis diadopsi). Sukses menembak ComponentCreated; "
      + "klik/longklik menembak CoreClicked/CoreLongClicked; gagal via "
      + "ErrorOccurred.")
  public void CallCreateDynamicComponent(String typeName, AndroidViewComponent container, int id) {
    core.CallCreateDynamicComponent(typeName, container, id);
  }

  @SimpleFunction(description = "Ambil komponen dinamis ber-id (untuk blok Any "
      + "generik MIT / blok yang butuh soket komponen). null bila tidak ada.")
  public Object GetDynamicComponent(int id) {
    return core.GetDynamicComponent(id);
  }

  @SimpleFunction(description = "SET: Set properti komponen dinamis ber-id "
      + "(nama properti MIT, mis. Text, BackgroundColor, FontSize). "
      + "Gagal via ErrorOccurred.")
  public void SetDynamicProperty(int id, String name, Object value) {
    core.SetDynamicProperty(id, name, value);
  }

  @SimpleFunction(description = "CALL: Jalan pintas: bikin Button dinamis ber-id "
      + "lengkap dengan teks, warna, ukuran font, tebal, visibilitas, "
      + "LEBAR + TINGGI (angka MIT: -2 isi, -1 otomatis, positif pixel). "
      + "Sama seperti Create + Set berulang, tapi satu blok.")
  public void CallCreateDynamicButton(AndroidViewComponent container, int id, String text, int textColor, int backgroundColor, float fontSize, boolean bold, boolean visible, int width, int height) {
    core.CallCreateDynamicButton(container, id, text, textColor, backgroundColor, fontSize, bold, visible, width, height);
  }

  @SimpleFunction(description = "CALL: Jalan pintas: bikin Label dinamis ber-id "
      + "lengkap dengan teks, warna, ukuran font, tebal, visibilitas, "
      + "LEBAR + TINGGI (angka MIT).")
  public void CallCreateDynamicLabel(AndroidViewComponent container, int id, String text, int textColor, int backgroundColor, float fontSize, boolean bold, boolean visible, int width, int height) {
    core.CallCreateDynamicLabel(container, id, text, textColor, backgroundColor, fontSize, bold, visible, width, height);
  }

  @SimpleFunction(description = "CALL: Jalan pintas: bikin TextBox dinamis ber-id "
      + "lengkap dengan hint, warna, ukuran font, tebal, visibilitas, "
      + "LEBAR + TINGGI (angka MIT).")
  public void CallCreateDynamicTextBox(AndroidViewComponent container, int id, String hint, int textColor, int backgroundColor, float fontSize, boolean bold, boolean visible, int width, int height) {
    core.CallCreateDynamicTextBox(container, id, hint, textColor, backgroundColor, fontSize, bold, visible, width, height);
  }

  @SimpleFunction(description = "CALL: Jalan pintas: bikin Image (gambar MIT) "
      + "dinamis ber-id dengan picture + visibilitas + LEBAR + TINGGI "
      + "(angka MIT). Isi picture seperti Image.Picture biasa "
      + "(path/asset/URL).")
  public void CallCreateDynamicImage(AndroidViewComponent container, int id, String picturePath, boolean visible, int width, int height) {
    core.CallCreateDynamicImage(container, id, picturePath, visible, width, height);
  }

  @SimpleFunction(description = "Baca properti komponen dinamis ber-id "
      + "(teks). Kosong bila tidak ada/gagal (lihat ErrorOccurred).")
  public String GetDynamicProperty(int id, String name) {
    return core.GetDynamicProperty(id, name);
  }

  @SimpleFunction(description = "Panggil fungsi komponen dinamis ber-id "
      + "(nama fungsi MIT + list argumen). Sinkron: hasil langsung dikembalikan "
      + "sebagai teks (kosong bila void/gagal; gagal via ErrorOccurred).")
  public String InvokeDynamic(int id, String name, YailList args) {
    return core.InvokeDynamic(id, name, args);
  }

  @SimpleFunction(description = "SET: atur lebar + tinggi komponen dinamis ber-id "
      + "(semua tipe: Button/Label/TextBox/Image/dll). Angka SAMA seperti blok "
      + "bawaan MIT: -2 = isi penuh, -1 = otomatis, positif = pixel. "
      + "Tanpa feedback.")
  public void SetDynamicSize(int id, int width, int height) {
    core.SetDynamicSize(id, width, height);
  }

  @SimpleFunction(description = "SET: tampil/sembunyi komponen dinamis ber-id. "
      + "false = GONE (hilang total). Tanpa feedback.")
  public void SetDynamicVisible(int id, boolean visible) {
    core.SetDynamicVisible(id, visible);
  }

  @SimpleFunction(description = "SET: teks komponen dinamis ber-id "
      + "(Button/Label/TextBox/Hint). Diam bila tipe tak punya teks. "
      + "Tanpa feedback.")
  public void SetDynamicText(int id, String text) {
    core.SetDynamicText(id, text);
  }

  @SimpleFunction(description = "SET: warna teks komponen dinamis ber-id. "
      + "Colok blok warna MIT. Diam bila tipe tak punya warna teks. "
      + "Tanpa feedback.")
  public void SetDynamicTextColor(int id, int color) {
    core.SetDynamicTextColor(id, color);
  }

  @SimpleFunction(description = "SET: warna latar komponen dinamis ber-id. "
      + "Colok blok warna MIT. Tanpa feedback.")
  public void SetDynamicBackgroundColor(int id, int color) {
    core.SetDynamicBackgroundColor(id, color);
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

  @SimpleFunction(description = "SET: Hapus SEMUA komponen dinamis Tools_Core "
      + "(id ikut dilepas).")
  public void SetClearDynamicComponents() {
    core.SetClearDynamicComponents();
  }

  @SimpleEvent(description = "Komponen dinamis jadi: id + nama tipe.")
  public void ComponentCreated(int id, String typeName) {
    EventDispatcher.dispatchEvent(this, "ComponentCreated", id, typeName);
  }

  @SimpleEvent(description = "Komponen dinamis di-klik: id.")
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

  @SimpleProperty(description = "Nama operasi Core (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpCore() {
    return core.OpCore();
  }

  @SimpleFunction(description = "CALL: unduh ke folder (otomatis di-root "
      + "/storage/emulated/0; absolut apa adanya). url = http(s) ATAU data:URL "
      + "(base64/url-encoded, mis. data:image/png;base64,...) ATAU content: URI "
      + "Android (dari file picker) ATAU file: URL / path lokal (disalin). "
      + "blob: TIDAK didukung (itu objek browser; "
      + "ubah dulu jadi base64 via JavaScript, lalu kirim sebagai data:URL "
      + "atau ke CallSaveBase64ToFile). fileName kosong = nama otomatis. "
      + "id = angka bebas yang belum dipakai (minta ke Tools_Base "
      + "CreateUniqueId atau CreateDownloadId). Hasil lewat DownloadStarted/"
      + "ProgressChanged/Finished (bawa id).")
  public void CallDownload(String url, String fileName, String folder, int id) {
    downloader.CallDownload(url, fileName, folder, id);
  }

  @SimpleFunction(description = "CALL: unduh BANYAK URL sekaligus (tanpa loop manual). fileNames "
      + "boleh kosong (= otomatis semua). folder seperti CallDownload. Tanpa return: "
      + "pantau tiap id lewat DownloadStarted/DownloadProgressChanged/DownloadFinished. "
      + "Ringkasan lewat DownloadBatchFinished. Satu batch per saat.")
  public void CallDownloadList(Object urls, Object fileNames, String folder) {
    downloader.CallDownloadList(urls, fileNames, folder);
  }

  @SimpleFunction(description = "SET: batalkan unduhan berjalan (per id). File sebagian dihapus. Tanpa feedback.")
  public void SetCancelDownload(int id) {
    downloader.SetCancelDownload(id);
  }

  @SimpleFunction(description = "SET: jeda unduhan berjalan (per id). Lanjut via SetResumeDownload. Tanpa feedback.")
  public void SetPauseDownload(int id) {
    downloader.SetPauseDownload(id);
  }

  @SimpleFunction(description = "SET: lanjutkan unduhan yang dijeda (per id). Tanpa feedback.")
  public void SetResumeDownload(int id) {
    downloader.SetResumeDownload(id);
  }

  @SimpleFunction(description = "Buat id unduhan unik baru (Number, siap pakai ke CallDownload). "
      + "Urutan pembuatan menentukan urutan list.")
  public int CreateDownloadId() {
    return downloader.CreateDownloadId();
  }

  @SimpleFunction(description = "SET: tampilkan progres unduhan di bar notifikasi "
      + "Android (per id, otomatis: nama file + persen). show=true = tampil + update "
      + "tiap Started/Progress/Finished; show=false = matikan + hapus notif id itu. "
      + "Pasang sesudah CallDownload, atau di dalam event DownloadStarted/"
      + "ProgressChanged (id sudah terdaftar; di Finished kurang relevan). "
      + "Tiap id hasil CallDownloadList bisa dipasang satu-satu, agar sistem bisa "
      + "bedakan Download vs DownloadList. Android 13+ butuh izin "
      + "POST_NOTIFICATIONS (minta via blok GetPermission Tools_File). "
      + "Tanpa feedback.")
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
      + "DownloadProgressChanged: list [persen 0-100, byte diterima, byte total].")
  public List<Object> GetDownloadProgress(int id) {
    return downloader.GetDownloadProgress(id);
  }

  @SimpleFunction(description = "Fallback polling untuk event DownloadFinished: "
      + "list [success, savePath, fileName, message]. Tetap ada setelah selesai "
      + "sampai RemoveDownloadId.")
  public List<Object> GetDownloadResult(int id) {
    return downloader.GetDownloadResult(id);
  }

  @SimpleFunction(description = "Fallback polling untuk event batch: list "
      + "[batchJalan, total, selesai, ok].")
  public List<Object> GetBatchProgress() {
    return downloader.GetBatchProgress();
  }

  @SimpleFunction(description = "CALL: tebak nama file dari URL saja (1 input, "
      + "mandiri — bisa disandingkan komponen lain, mis. Web bawaan MIT untuk "
      + "rename). http(s) = ambil header asli server (ikuti redirect); "
      + "data:/content:/file:/path = tebak lokal tanpa internet. Hasil lewat "
      + "FileNameGuessed (url + fileName + mimeType).")
  public void CallGuessFileName(String url) {
    downloader.CallGuessFileName(url);
  }

  @SimpleEvent(description = "Hasil CallGuessFileName: url asal + fileName "
      + "tebakan + mimeType (kosong bila tak diketahui).")
  public void FileNameGuessed(String url, String fileName, String mimeType) {
    EventDispatcher.dispatchEvent(this, "FileNameGuessed", url, fileName, mimeType);
  }

  @SimpleFunction(description = "GET: nama file dari URL, mime, dan content-disposition "
      + "(dipakai otomatis bila fileName CallDownload dikosongkan).")
  public String GetFileName(String url, String mimeType, String contentDisposition) {
    return downloader.GetFileName(url, mimeType, contentDisposition);
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

  @SimpleProperty(description = "Nama operasi Download (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpDownload() {
    return downloader.OpDownload();
  }

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

  @SimpleFunction(description = "True bila ada salinan/pindahan "
      + "CallCopyOrMoveFile yang masih berjalan.")
  public boolean IsBusy() {
    return file.IsBusy();
  }

  @SimpleFunction(description = "CALL: jadikan directory hidden (nama jadi "
      + ".nama). Sudah hidden = success. Hasil lewat DirectoryHidden.")
  public void CallSetHiddenDirectory(String directory) {
    file.CallSetHiddenDirectory(directory);
  }

  @SimpleFunction(description = "CALL: buat file .nomedia di dalam folder "
      + "(sembunyikan dari galeri/musik). Hasil lewat NomediaFileCreated.")
  public void CallMakeNomediaFile(String folder) {
    file.CallMakeNomediaFile(folder);
  }

  @SimpleFunction(description = "CALL: hapus file .nomedia di folder. "
      + "Tidak ada = success. Hasil lewat NomediaFileDeleted.")
  public void CallDeleteNomediaFile(String folder) {
    file.CallDeleteNomediaFile(folder);
  }

  @SimpleFunction(description = "CALL: buat folder (termasuk induknya). "
      + "Sudah ada = success. Hasil lewat DirectoryCreated.")
  public void CallMakeDirectory(String folder) {
    file.CallMakeDirectory(folder);
  }

  @SimpleFunction(description = "CALL: hapus file/folder (folder rekursif). "
      + "Tidak ada = success. Hasil lewat FileDeleted.")
  public void CallDeleteFile(String path) {
    file.CallDeleteFile(path);
  }

  @SimpleFunction(description = "CALL: hapus BANYAK path sekaligus (tanpa loop "
      + "manual). paths = list / teks koma. Hasil lewat FileListDeleted "
      + "(total + terhapus).")
  public void CallDeleteList(Object paths) {
    file.CallDeleteList(paths);
  }

  @SimpleFunction(description = "CALL: ganti nama FILE (tanpa pindah folder). "
      + "newName = nama saja. Hasil lewat FileRenamed (path = nama baru).")
  public void CallRenameFile(String path, String newName) {
    file.CallRenameFile(path, newName);
  }

  @SimpleFunction(description = "CALL: ganti nama FOLDER (tanpa pindah). "
      + "newName = nama saja. Hasil lewat FolderRenamed.")
  public void CallRenameFolder(String path, String newName) {
    file.CallRenameFolder(path, newName);
  }

  @SimpleFunction(description = "CALL: buat file kosong (folder induk dibuat "
      + "otomatis). Sudah ada = success. Hasil lewat EmptyFileCreated.")
  public void CallCreateEmptyFile(String path) {
    file.CallCreateEmptyFile(path);
  }

  @SimpleFunction(description = "CALL: tulis teks ke file (UTF-8). "
      + "append=true = sambung di akhir. Hasil lewat FileSaved.")
  public void CallSaveFile(String path, String content, boolean append) {
    file.CallSaveFile(path, content, append);
  }

  @SimpleFunction(description = "CALL: simpan base64 LANGSUNG ke storage "
      + "(tanpa validasi gambar, lebih cepat). Hasil lewat Base64FileSaved. "
      + "Polling: GetLastBase64FilePath.")
  public void CallSaveBase64ToFile(String base64, String path) {
    file.CallSaveBase64ToFile(base64, path);
  }

  @SimpleFunction(description = "Fallback polling untuk Base64FileSaved: "
      + "path file base64 terakhir (kosong bila gagal/belum ada).")
  public String GetLastBase64FilePath() {
    return file.GetLastBase64FilePath();
  }

  @SimpleFunction(description = "CALL: baca file teks (UTF-8) di background. "
      + "Hasil lewat GotText (content = isi file). Mulai lewat ReadStarted.")
  public void CallReadText(String path) {
    file.CallReadText(path);
  }

  @SimpleFunction(description = "CALL: salin ATAU pindah file (isMove=true = "
      + "pindah). Path otomatis di-root /storage/emulated/0. Nama tujuan boleh beda. "
      + "Bila tujuan SUDAH ADA: tidak disalin, Finished(success=true, "
      + "isDuplicated=true) -> rename manual ala contoh. Asset hanya bisa "
      + "jadi sumber. Satu job per saat.")
  public void CallCopyOrMoveFile(String fromFileName, String toFileName, boolean isMove) {
    file.CallCopyOrMoveFile(fromFileName, toFileName, isMove);
  }

  @SimpleFunction(description = "SET: batalkan salinan/pindahan berjalan. "
      + "Tanpa feedback (file sebagian dihapus).")
  public void SetCancelCopyOrMove() {
    file.SetCancelCopyOrMove();
  }

  @SimpleFunction(description = "Nama file dari sebuah path (buang foldernya). "
      + "Olah string saja, tanpa scope.")
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
      + "/storage/emulated/0; absolut/URL/aset apa adanya).")
  public String GetFullPath(String path) {
    return file.GetFullPath(path);
  }

  @SimpleFunction(description = "Ukuran file dalam byte. Folder = total isi "
      + "rekursif. path seperti IsFileOrDirectoryExists.")
  public long FileSize(String path) {
    return file.FileSize(path);
  }

  @SimpleFunction(description = "Folder khusus aplikasi (tanpa izin tambahan, "
      + "aman Android 11+). Cth: /storage/emulated/0/Android/data/<paket>/"
      + "files. Hasilnya bisa langsung dipakai sebagai path blok lain.")
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
      + "(/storage/emulated/0).")
  public long FreeSpace() {
    return file.FreeSpace();
  }

  @SimpleFunction(description = "Total kapasitas (byte) kartu SD "
      + "(/storage/emulated/0). -1 bila gagal.")
  public long TotalSpace() {
    return file.TotalSpace();
  }

  @SimpleFunction(description = "Daftar file (path lengkap). filter = "
      + "ekstensi tanpa titik (boleh koma: jpg,png; kosong = semua). "
      + "recursive = sertakan isi subfolder. path seperti IsFileOrDirectoryExists.")
  public List<String> ListFiles(String path, String filter, boolean recursive) {
    return file.ListFiles(path, filter, recursive);
  }

  @SimpleFunction(description = "Daftar subfolder (path lengkap, tanpa "
      + "rekursif). path seperti IsFileOrDirectoryExists.")
  public List<String> ListFolders(String folder) {
    return file.ListFolders(folder);
  }

  @SimpleFunction(description = "True bila file/folder input PUNYA DUPLIKAT "
      + "di dalam 1 laci yang sama: ada yang namanya sama persis, atau satu "
      + "keluarga counter (mis. image.jpg dengan image_1.jpg / "
      + "image (1).jpg). Input satu saja: \"/Download/image.jpg\" = mode "
      + "file, \"/Download/\" atau path folder = mode folder. Hanya level "
      + "laci itu, tanpa masuk subfolder. Cocok untuk validasi sebelum "
      + "simpan/rename.")
  public boolean IsDuplicateFileOrDirectory(String fileOrDirectory) {
    return file.IsDuplicateFileOrDirectory(fileOrDirectory);
  }

  @SimpleFunction(description = "Berapa banyak duplikat file/folder input di "
      + "dalam 1 laci yang sama (termasuk inputnya sendiri bila ada). 0 = "
      + "tidak ada duplikat. Keluarga nama sama seperti "
      + "IsDuplicateFileOrDirectory; hanya level laci itu, tanpa masuk "
      + "subfolder.")
  public int GetManyDuplicateFileOrDirectory(String fileOrDirectory) {
    return file.GetManyDuplicateFileOrDirectory(fileOrDirectory);
  }

  @SimpleFunction(description = "Daftar path lengkap duplikat file/folder "
      + "input di dalam 1 laci yang sama (termasuk inputnya sendiri bila "
      + "ada). Urut abjad. Kosong = tidak ada duplikat. Keluarga nama sama "
      + "seperti IsDuplicateFileOrDirectory; tanpa masuk subfolder.")
  public List<String> GetListDuplicateFileOrDirectory(String fileOrDirectory) {
    return file.GetListDuplicateFileOrDirectory(fileOrDirectory);
  }

  @SimpleFunction(description = "Kategori file dari nama/path: image, audio, "
      + "video, text, archive, document, apk, atau unknown. Berdasarkan "
      + "ekstensi (tak peduli huruf besar). Bandingkan dengan blok "
      + "TypeImage/dll. Olah string saja.")
  public String FileTypeOf(String pathOrName) {
    return file.FileTypeOf(pathOrName);
  }

  @SimpleFunction(description = "Daftar file (path lengkap) yang kategorinya "
      + "cocok. type = image/audio/video/text/archive/document/apk (boleh "
      + "alias: gambar/musik/vidio/teks/arsip/dokumen) atau salah satu blok "
      + "TypeImage/dll. recursive = sertakan isi subfolder. "
      + "path seperti IsFileOrDirectoryExists.")
  public List<String> ListFilesByType(String folder, String type, boolean recursive) {
    return file.ListFilesByType(folder, type, recursive);
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

  @SimpleFunction(description = "Minta izin Android (cth: "
      + "android.permission.READ_EXTERNAL_STORAGE). Hasil lewat "
      + "GotPermission. Pasang need_permission dari event lain ke sini. "
      + "Android 11+: izin storage dialihkan ke 'Akses semua file' "
      + "(pakai OpenAllFilesAccessSettings; cek IsAllFilesAccessGranted).")
  public void GetPermission(String permission) {
    file.GetPermission(permission);
  }

  @SimpleFunction(description = "True bila aplikasi boleh tulis SEMUA file "
      + "bersama. Android 11+ = 'Akses semua file' aktif; Android 10 ke "
      + "bawah selalu true (izin tulis biasa cukup via GetPermission).")
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

  @SimpleFunction(description = "True bila izin ini sudah granted. Untuk "
      + "izin storage di Android 11+ = cek 'Akses semua file'. "
      + "Android 6 ke bawah selalu true (izin sejak install). Colok blok "
      + "Permission... ATAU teks string izinnya.")
  public boolean IsPermissionGranted(String permission) {
    return file.IsPermissionGranted(permission);
  }

  @SimpleFunction(description = "True bila izin tulis storage sudah beres "
      + "(tanpa input apa pun). Android 11+ = 'Akses semua file' aktif; "
      + "Android 10 ke bawah = izin tulis granted.")
  public boolean IsStoragePermissionGranted() {
    return file.IsStoragePermissionGranted();
  }

  @SimpleFunction(description = "CALL: pastikan 1 izin beres TANPA rakit "
      + "manual — blok ini yang nentuin caranya (cek OS dulu): sudah "
      + "granted = langsung GotPermission(true); storage Android 11+ "
      + "belum = OTOMATIS buka Settings + GotPermission(false + pesan); "
      + "lainnya = dialog izin + GotPermission. Tanpa return. Colok blok "
      + "Permission... ATAU teks string izinnya.")
  public void CallEnsurePermission(String permission) {
    file.CallEnsurePermission(permission);
  }

  @SimpleFunction(description = "CALL: pastikan izin tulis storage beres "
      + "(tanpa input apa pun) — hasil lewat GotPermission. Storage "
      + "Android 11+ belum granted = otomatis buka Settings.")
  public void CallEnsureStoragePermission() {
    file.CallEnsureStoragePermission();
  }

  @SimpleProperty(description = "Helper read-only: izin kamera. Colok ke "
      + "IsPermissionGranted / CallEnsurePermission (tanpa ketik string).",
      category = PropertyCategory.BEHAVIOR)
  public String PermissionCamera() {
    return file.PermissionCamera();
  }

  @SimpleProperty(description = "Helper read-only: izin mikrofon (rekam "
      + "suara). Colok ke IsPermissionGranted / CallEnsurePermission.",
      category = PropertyCategory.BEHAVIOR)
  public String PermissionMicrophone() {
    return file.PermissionMicrophone();
  }

  @SimpleProperty(description = "Helper read-only: izin baca storage. "
      + "Colok ke IsPermissionGranted / CallEnsurePermission.",
      category = PropertyCategory.BEHAVIOR)
  public String PermissionReadStorage() {
    return file.PermissionReadStorage();
  }

  @SimpleProperty(description = "Helper read-only: izin tulis storage. "
      + "Colok ke IsPermissionGranted / CallEnsurePermission.",
      category = PropertyCategory.BEHAVIOR)
  public String PermissionWriteStorage() {
    return file.PermissionWriteStorage();
  }

  @SimpleProperty(description = "Helper read-only: izin notifikasi "
      + "(Android 13+). Colok ke IsPermissionGranted / CallEnsurePermission.",
      category = PropertyCategory.BEHAVIOR)
  public String PermissionNotifications() {
    return file.PermissionNotifications();
  }

  @SimpleProperty(description = "Helper read-only: izin baca gambar "
      + "(Android 13+). Colok ke IsPermissionGranted / CallEnsurePermission.",
      category = PropertyCategory.BEHAVIOR)
  public String PermissionReadImages() {
    return file.PermissionReadImages();
  }

  @SimpleProperty(description = "Helper read-only: izin baca video "
      + "(Android 13+). Colok ke IsPermissionGranted / CallEnsurePermission.",
      category = PropertyCategory.BEHAVIOR)
  public String PermissionReadVideo() {
    return file.PermissionReadVideo();
  }

  @SimpleProperty(description = "Helper read-only: izin baca audio "
      + "(Android 13+). Colok ke IsPermissionGranted / CallEnsurePermission.",
      category = PropertyCategory.BEHAVIOR)
  public String PermissionReadAudio() {
    return file.PermissionReadAudio();
  }

  @SimpleProperty(description = "Helper read-only: izin lokasi akurat. "
      + "Colok ke IsPermissionGranted / CallEnsurePermission.",
      category = PropertyCategory.BEHAVIOR)
  public String PermissionLocationFine() {
    return file.PermissionLocationFine();
  }

  @SimpleProperty(description = "Helper read-only: izin lokasi kasar. "
      + "Colok ke IsPermissionGranted / CallEnsurePermission.",
      category = PropertyCategory.BEHAVIOR)
  public String PermissionLocationCoarse() {
    return file.PermissionLocationCoarse();
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

  @SimpleEvent(description = "Hasil CallSetHiddenDirectory: path + success + "
      + "message + needPermission (isi = izin yang harus diminta bila "
      + "gagal karena izin).")
  public void DirectoryHidden(String path, boolean success, String message, String needPermission) {
    EventDispatcher.dispatchEvent(this, "DirectoryHidden", path, success, message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallMakeNomediaFile: path + success + "
      + "message + needPermission.")
  public void NomediaFileCreated(String path, boolean success, String message, String needPermission) {
    EventDispatcher.dispatchEvent(this, "NomediaFileCreated", path, success, message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallDeleteNomediaFile: path + success + "
      + "message + needPermission.")
  public void NomediaFileDeleted(String path, boolean success, String message, String needPermission) {
    EventDispatcher.dispatchEvent(this, "NomediaFileDeleted", path, success, message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallMakeDirectory: path + success + "
      + "message + needPermission.")
  public void DirectoryCreated(String path, boolean success, String message, String needPermission) {
    EventDispatcher.dispatchEvent(this, "DirectoryCreated", path, success, message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallDeleteFile: path + success + "
      + "message + needPermission.")
  public void FileDeleted(String path, boolean success, String message, String needPermission) {
    EventDispatcher.dispatchEvent(this, "FileDeleted", path, success, message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallDeleteList: success + total + "
      + "terhapus + message + needPermission.")
  public void FileListDeleted(boolean success, int total, int deleted, String message, String needPermission) {
    EventDispatcher.dispatchEvent(this, "FileListDeleted", success, total, deleted, message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallRenameFile: path (nama baru) + "
      + "success + message + needPermission.")
  public void FileRenamed(String path, boolean success, String message, String needPermission) {
    EventDispatcher.dispatchEvent(this, "FileRenamed", path, success, message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallRenameFolder: path (nama baru) + "
      + "success + message + needPermission.")
  public void FolderRenamed(String path, boolean success, String message, String needPermission) {
    EventDispatcher.dispatchEvent(this, "FolderRenamed", path, success, message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallCreateEmptyFile: path + success + "
      + "message + needPermission.")
  public void EmptyFileCreated(String path, boolean success, String message, String needPermission) {
    EventDispatcher.dispatchEvent(this, "EmptyFileCreated", path, success, message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallSaveFile: path + success + "
      + "message + needPermission.")
  public void FileSaved(String path, boolean success, String message, String needPermission) {
    EventDispatcher.dispatchEvent(this, "FileSaved", path, success, message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallSaveBase64ToFile: path + success + "
      + "message + needPermission.")
  public void Base64FileSaved(String path, boolean success, String message, String needPermission) {
    EventDispatcher.dispatchEvent(this, "Base64FileSaved", path, success, message, needPermission);
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

  @SimpleEvent(description = "Hasil GetPermission: success + izin + pesan. "
      + "Pasang need_permission dari event lain ke GetPermission.")
  public void GotPermission(boolean success, String permission, String message) {
    EventDispatcher.dispatchEvent(this, "GotPermission", success, permission, message);
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

  @SimpleProperty(description = "Nama operasi Permission (untuk if di "
      + "ErrorOccurred).", category = PropertyCategory.BEHAVIOR)
  public String OpPermission() {
    return file.OpPermission();
  }

  @SimpleFunction(description = "CALL: tampilkan gambar ber-id ke layout (Arrangement MIT / dinamis). "
      + "Bisa jalan bareng (satu id = satu gambar). source kosong = pakai path "
      + "(otomatis di-root /storage/emulated/0). "
      + "source bisa: path file, file:/// absolut, http(s) URL (di-cache otomatis), "
      + "atau base64 (mentah / data:...). id = penanda (minta ke CreateImageId / "
      + "Tools_Base CreateUniqueId, reservasi otomatis diadopsi). Sukses menembak "
      + "ImageShown; gagal via ErrorOccurred.")
  public void CallShowImage(AndroidViewComponent container, String source, String path, int id) {
    image.CallShowImage(container, source, path, id);
  }

  @SimpleFunction(description = "SET: hapus SEMUA gambar Tools_Image dari layout itu "
      + "(komponen lain tidak diusik, id ikut dilepas). Tanpa feedback.")
  public void SetClearImage(AndroidViewComponent container) {
    image.SetClearImage(container);
  }

  @SimpleFunction(description = "SET: Aktif/matikan cubit-zoom (pinch) + geser "
      + "gambar ber-id. Aktif memakai mode MATRIX (klik tetap jalan untuk tap "
      + "tanpa zoom). Perubahan skala menembak ZoomChanged; baca kapan saja "
      + "via GetZoom.")
  public void SetZoomEnabled(int id, boolean enabled) {
    image.SetZoomEnabled(id, enabled);
  }

  @SimpleFunction(description = "Fallback polling untuk event ZoomChanged: "
      + "skala zoom gambar ber-id (1.0 = pas).")
  public float GetZoom(int id) {
    return image.GetZoom(id);
  }

  @SimpleFunction(description = "SET: kembalikan zoom gambar ber-id ke pas (1.0). Tanpa feedback.")
  public void SetResetZoom(int id) {
    image.SetResetZoom(id);
  }

  @SimpleFunction(description = "SET: atur lebar + tinggi gambar ber-id. Angka "
      + "SAMA seperti blok bawaan MIT: -2 = isi penuh, -1 = otomatis, positif "
      + "= pixel. Persen tidak didukung (dianggap otomatis). Tanpa feedback.")
  public void SetImageSize(int id, int width, int height) {
    image.SetImageSize(id, width, height);
  }

  @SimpleFunction(description = "SET: tampil/sembunyi gambar ber-id. "
      + "false = GONE (hilang total). Tanpa feedback.")
  public void SetImageVisible(int id, boolean visible) {
    image.SetImageVisible(id, visible);
  }

  @SimpleFunction(description = "SET: mode pas gambar ber-id. fit true = "
      + "seluruh gambar muat di dalam (tidak kepotong); false = penuhi "
      + "tampilan (bisa kepotong). Mematikan zoom bila sedang aktif. "
      + "Tanpa feedback.")
  public void SetImageFit(int id, boolean fit) {
    image.SetImageFit(id, fit);
  }

  @SimpleFunction(description = "Lebar gambar ber-id (angka MIT: -2 isi, "
      + "-1 otomatis, positif pixel). -3 bila id tidak ada.")
  public int GetImageWidth(int id) {
    return image.GetImageWidth(id);
  }

  @SimpleFunction(description = "Tinggi gambar ber-id (angka MIT). "
      + "-3 bila id tidak ada.")
  public int GetImageHeight(int id) {
    return image.GetImageHeight(id);
  }

  @SimpleFunction(description = "True bila gambar ber-id sedang tampil "
      + "(visible). False bila tidak ada / sedang GONE.")
  public boolean IsImageVisible(int id) {
    return image.IsImageVisible(id);
  }

  @SimpleFunction(description = "CALL: bikin slider gambar ber-id (geser kanan-kiri "
      + "bila horizontal=true, atas-bawah bila false — lebih dari Iago/Boson "
      + "yang cuma kiri-kanan). Isi via SetSliderImages. Geser menembak "
      + "SliderChanged; baca kapan saja via GetSliderIndex.")
  public void CallCreateImageSlider(AndroidViewComponent container, int id, boolean horizontal) {
    image.CallCreateImageSlider(container, id, horizontal);
  }

  @SimpleFunction(description = "SET: isi slider ber-id dengan daftar gambar "
      + "(list teks: path file/asset, URL http, atau base64 — sama fleksibelnya "
      + "dengan ShowImage; path dipakai bila item kosong). Maks 20 "
      + "gambar. Gagal via ErrorOccurred.")
  public void SetSliderImages(int sliderId, Object paths) {
    image.SetSliderImages(sliderId, paths);
  }

  @SimpleFunction(description = "SET: geser slider ke halaman berikut (berputar). Tanpa feedback.")
  public void SetSliderNext(int id) {
    image.SetSliderNext(id);
  }

  @SimpleFunction(description = "SET: geser slider ke halaman sebelum (berputar). Tanpa feedback.")
  public void SetSliderPrevious(int id) {
    image.SetSliderPrevious(id);
  }

  @SimpleFunction(description = "SET: lompat ke halaman index (0 = pertama). Tanpa feedback.")
  public void SetSliderGoTo(int id, int index) {
    image.SetSliderGoTo(id, index);
  }

  @SimpleFunction(description = "SET: Auto-putar slider tiap intervalMs "
      + "(0 = mati).")
  public void SetSliderAutoPlay(int id, long intervalMs) {
    image.SetSliderAutoPlay(id, intervalMs);
  }

  @SimpleFunction(description = "Fallback polling untuk event SliderChanged: "
      + "index halaman aktif (0 = pertama).")
  public int GetSliderIndex(int id) {
    return image.GetSliderIndex(id);
  }

  @SimpleFunction(description = "Jumlah halaman slider ber-id.")
  public int GetSliderCount(int id) {
    return image.GetSliderCount(id);
  }

  @SimpleFunction(description = "Buat id gambar unik baru (Number, siap pakai ke CallShowImage).")
  public int CreateImageId() {
    return image.CreateImageId();
  }

  @SimpleFunction(description = "Buat id slider unik baru (Number, siap pakai "
      + "ke CallCreateImageSlider).")
  public int CreateSliderId() {
    return image.CreateSliderId();
  }

  @SimpleFunction(description = "Fallback polling untuk event "
      + "AfterPictureBase64Ready: path gambar terakhir yang jadi "
      + "(kosong bila belum ada).")
  public String GetLastPicturePath() {
    return image.GetLastPicturePath();
  }

  @SimpleFunction(description = "CALL: decode base64 jadi gambar di cache. Path hasil langsung bisa "
      + "dipasang ke Image.Picture. Hasil lewat AfterPictureBase64Ready. "
      + "Satu proses per saat.")
  public void CallBase64ToPicture(String base64) {
    image.CallBase64ToPicture(base64);
  }

  @SimpleFunction(description = "Lebar dan tinggi gambar (pixel). Kembali: list [lebar, tinggi]. "
      + "[0, 0] bila bukan gambar / tidak ada.")
  public List<Integer> GetImageSize(String path) {
    return image.GetImageSize(path);
  }

  @SimpleEvent(description = "Hasil CallShowImage: gambar ber-id tampil. id + path tampil.")
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

  @SimpleEvent(description = "Slider jadi: id + arah (true = kanan-kiri).")
  public void SliderCreated(int id, boolean horizontal) {
    EventDispatcher.dispatchEvent(this, "SliderCreated", id, horizontal);
  }

  @SimpleEvent(description = "Halaman slider pindah: id slider + index "
      + "(0 = pertama). Fallback: GetSliderIndex.")
  public void SliderChanged(int sliderId, int index) {
    EventDispatcher.dispatchEvent(this, "SliderChanged", sliderId, index);
  }

  @SimpleEvent(description = "Halaman slider di-klik: id slider + index.")
  public void SliderClicked(int sliderId, int index) {
    EventDispatcher.dispatchEvent(this, "SliderClicked", sliderId, index);
  }

  @SimpleEvent(description = "Proses base64-gambar dimulai.")
  public void PictureStarted() {
    EventDispatcher.dispatchEvent(this, "PictureStarted");
  }

  @SimpleEvent(description = "Hasil CallBase64ToPicture: success pertama, path absolut "
      + "(cache), fileName paling belakang. Path siap ke Image.Picture.")
  public void AfterPictureBase64Ready(boolean success, String path, String fileName, String message) {
    EventDispatcher.dispatchEvent(this, "AfterPictureBase64Ready", success, path, fileName, message);
  }

  @SimpleProperty(description = "Nama operasi Slider (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpSlider() {
    return image.OpSlider();
  }

  @SimpleProperty(description = "Nama operasi Image (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpImage() {
    return image.OpImage();
  }

  @SimpleFunction(description = "CALL: Bikin VerticalArrangement dinamis ber-id di "
      + "dalam layout induk. width/height angka MIT (-2 isi, -1 otomatis, "
      + "positif pixel); visible false = GONE. Sukses menembak LayoutCreated; "
      + "gagal via ErrorOccurred.")
  public void CallCreateVerticalLayout(AndroidViewComponent container, int id, int width, int height, boolean visible, int backgroundColor) {
    layout.CallCreateVerticalLayout(container, id, width, height, visible, backgroundColor);
  }

  @SimpleFunction(description = "CALL: Bikin HorizontalArrangement dinamis ber-id di "
      + "dalam layout induk. width/height angka MIT (-2 isi, -1 otomatis, "
      + "positif pixel); visible false = GONE.")
  public void CallCreateHorizontalLayout(AndroidViewComponent container, int id, int width, int height, boolean visible, int backgroundColor) {
    layout.CallCreateHorizontalLayout(container, id, width, height, visible, backgroundColor);
  }

  @SimpleFunction(description = "CALL: Bikin VerticalScrollArrangement dinamis "
      + "ber-id (bisa scroll atas-bawah) di dalam layout induk. width/height "
      + "angka MIT; visible false = GONE.")
  public void CallCreateVerticalScrollLayout(AndroidViewComponent container, int id, int width, int height, boolean visible, int backgroundColor) {
    layout.CallCreateVerticalScrollLayout(container, id, width, height, visible, backgroundColor);
  }

  @SimpleFunction(description = "CALL: Bikin HorizontalScrollArrangement dinamis "
      + "ber-id (bisa scroll kiri-kanan) di dalam layout induk. width/height "
      + "angka MIT; visible false = GONE.")
  public void CallCreateHorizontalScrollLayout(AndroidViewComponent container, int id, int width, int height, boolean visible, int backgroundColor) {
    layout.CallCreateHorizontalScrollLayout(container, id, width, height, visible, backgroundColor);
  }

  @SimpleFunction(description = "Ambil layout dinamis ber-id (untuk dipakai "
      + "sebagai wadah blok lain). null bila tidak ada.")
  public Object GetLayoutComponent(int id) {
    return layout.GetLayoutComponent(id);
  }

  @SimpleFunction(description = "Buat id layout unik baru (Number, siap pakai).")
  public int CreateLayoutId() {
    return layout.CreateLayoutId();
  }

  @SimpleFunction(description = "SET: Hapus SEMUA layout dinamis Tools_Layout "
      + "(id ikut dilepas).")
  public void SetClearLayouts() {
    layout.SetClearLayouts();
  }

  @SimpleFunction(description = "SET: tampil/sembunyi layout dinamis ber-id. "
      + "layout = colok TypeVertical/TypeHorizontal/TypeVerticalScroll/"
      + "TypeHorizontalScroll (WAJIB cocok dengan tipe aslinya, biar tidak "
      + "ketuker); id = id layout; visible false = GONE (hilang total, "
      + "ruang ikut ketutup). Tanpa feedback; cek dulu pakai IsLayoutExists "
      + "bila ragu.")
  public void SetLayoutVisible(String layout, int id, boolean visible) {
    this.layout.SetLayoutVisible(layout, id, visible);
  }

  @SimpleFunction(description = "True bila layout dinamis ber-id ini ADA dan "
      + "tipenya PERSIS sama dengan layout yang diminta (colok TypeVertical/"
      + "dll). False bila id tidak ada ATAU tipe beda.")
  public boolean IsLayoutExists(String layout, int id) {
    return this.layout.IsLayoutExists(layout, id);
  }

  @SimpleFunction(description = "True bila layout dinamis ber-id ini ADA, "
      + "tipenya cocok (seperti IsLayoutExists), DAN sedang tampil (visible). "
      + "False bila tidak ada / tipe beda / sedang GONE.")
  public boolean IsLayoutVisible(String layout, int id) {
    return this.layout.IsLayoutVisible(layout, id);
  }

  @SimpleFunction(description = "Tipe layout ber-id (\"VerticalArrangement\" "
      + "dkk, sama dengan typeName di LayoutCreated). Bandingkan dengan blok "
      + "TypeVertical/TypeHorizontal/TypeVerticalScroll/TypeHorizontalScroll. "
      + "Kosong bila id tidak ada.")
  public String GetLayoutType(int id) {
    return layout.GetLayoutType(id);
  }

  @SimpleProperty(description = "Pembanding read-only: tipe VerticalArrangement.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeVertical() {
    return layout.TypeVertical();
  }

  @SimpleProperty(description = "Pembanding read-only: tipe HorizontalArrangement.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeHorizontal() {
    return layout.TypeHorizontal();
  }

  @SimpleProperty(description = "Pembanding read-only: tipe VerticalScrollArrangement.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeVerticalScroll() {
    return layout.TypeVerticalScroll();
  }

  @SimpleProperty(description = "Pembanding read-only: tipe HorizontalScrollArrangement.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeHorizontalScroll() {
    return layout.TypeHorizontalScroll();
  }

  @SimpleFunction(description = "Ambil layout dinamis ber-id yang tipenya PERSIS "
      + "sama dengan layout yang diminta (untuk dicolok ke soket container, "
      + "mis. Create button). null bila id tidak ada ATAU tipe beda.")
  public Object GetLayout(String layout, int id) {
    return this.layout.GetLayout(layout, id);
  }

  @SimpleFunction(description = "SET: atur lebar + tinggi layout dinamis ber-id "
      + "(tipe wajib cocok). Angka SAMA seperti blok bawaan MIT: -2 = isi "
      + "penuh (fill parent), -1 = otomatis, positif = pixel. Tanpa feedback.")
  public void SetLayoutSize(String layout, int id, int width, int height) {
    this.layout.SetLayoutSize(layout, id, width, height);
  }

  @SimpleFunction(description = "SET: warna latar layout dinamis ber-id (tipe "
      + "wajib cocok). Colok blok warna MIT. Tanpa feedback.")
  public void SetLayoutBackgroundColor(String layout, int id, int color) {
    this.layout.SetLayoutBackgroundColor(layout, id, color);
  }

  @SimpleFunction(description = "SET: perataan isi layout dinamis ber-id (tipe "
      + "wajib cocok). Angka SAMA seperti blok bawaan MIT: horizontal 1=kiri, "
      + "2=tengah, 3=kanan; vertikal 1=atas, 2=tengah, 3=bawah. Tanpa feedback.")
  public void SetLayoutAlignment(String layout, int id, int horizontal, int vertical) {
    this.layout.SetLayoutAlignment(layout, id, horizontal, vertical);
  }

  @SimpleFunction(description = "Lebar layout dinamis ber-id (angka MIT: -2 isi, "
      + "-1 otomatis, positif pixel). -3 bila id tidak ada / tipe beda.")
  public int GetLayoutWidth(String layout, int id) {
    return this.layout.GetLayoutWidth(layout, id);
  }

  @SimpleFunction(description = "Tinggi layout dinamis ber-id (angka MIT). "
      + "-3 bila id tidak ada / tipe beda.")
  public int GetLayoutHeight(String layout, int id) {
    return this.layout.GetLayoutHeight(layout, id);
  }

  @SimpleEvent(description = "Layout dinamis jadi: id + nama tipe.")
  public void LayoutCreated(int id, String typeName) {
    EventDispatcher.dispatchEvent(this, "LayoutCreated", id, typeName);
  }

  @SimpleProperty(description = "Nama operasi Layout (untuk if di "
      + "ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpLayout() {
    return layout.OpLayout();
  }

  @SimpleFunction(description = "Daftarkan bottom-sheet ber-id. content = "
      + "Arrangement (dipindah ke dialog saat tampil, dikembalikan saat "
      + "tutup). gravity angka: 80=bawah, 48=atas, 17=tengah, 3=kiri, 5=kanan. "
      + "widthDp/heightDp <= 0 = menyesuaikan (lebar penuh bila gravity "
      + "atas/bawah). dimPercent 0-100 (kegelapan latar). animationType teks: "
      + "pakai properti SlideFromBottom/SlideFromTop/SlideFromLeft/"
      + "SlideFromRight/FadeAnim/NoAnim. animationDurationMs 0 = 250. "
      + "cancelable = bisa tutup via tombol back/luar. swipeToDismiss = geser "
      + "ke arah tutup untuk menutup. Sukses tanpa event; gagal via "
      + "ErrorOccurred.")
  public void RegisterSheet(int id, AndroidViewComponent content, int gravity, int widthDp, int heightDp, int dimPercent, String animationType, int animationDurationMs, boolean cancelable, boolean swipeToDismiss) {
    sheet.RegisterSheet(id, content, gravity, widthDp, heightDp, dimPercent, animationType, animationDurationMs, cancelable, swipeToDismiss);
  }

  @SimpleFunction(description = "CALL: Tampilkan sheet ber-id (animasi geser naik "
      + "otomatis). Hasil lewat SheetShown.")
  public void CallShowSheet(int id) {
    sheet.CallShowSheet(id);
  }

  @SimpleFunction(description = "CALL: Tutup sheet ber-id (animasi keluar). "
      + "Hasil lewat SheetHidden (registrasi tetap, bisa CallShowSheet lagi).")
  public void CallDismissSheet(int id) {
    sheet.CallDismissSheet(id);
  }

  @SimpleFunction(description = "True bila sheet ber-id sedang tampil.")
  public boolean IsSheetShowing(int id) {
    return sheet.IsSheetShowing(id);
  }

  @SimpleFunction(description = "SET: Tampilkan toast (pesan singkat bawah layar). "
      + "longDuration = tampil lebih lama. Gagal via ErrorOccurred.")
  public void SetShowToast(String message, boolean longDuration) {
    sheet.SetShowToast(message, longDuration);
  }

  @SimpleFunction(description = "SET: Minta izin notifikasi (Android 13+). "
      + "Best-effort: bila ditolak, izinkan manual di Setelan aplikasi.")
  public void SetRequestNotificationPermission() {
    sheet.SetRequestNotificationPermission();
  }

  @SimpleFunction(description = "SET: Tampilkan notifikasi bar status: id (angka "
      + "unik, mis. 1), judul, isi. Tap notifikasi membuka aplikasi. "
      + "Gagal (mis. izin belum ada) via ErrorOccurred.")
  public void SetShowNotification(int id, String title, String text) {
    sheet.SetShowNotification(id, title, text);
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
      + "ke RegisterSheet).")
  public int CreateSheetId() {
    return sheet.CreateSheetId();
  }

  @SimpleFunction(description = "SET: Hapus SEMUA sheet Tools_Sheet (tutup "
      + "diam-diam, konten dikembalikan, id dilepas).")
  public void SetClearSheets() {
    sheet.SetClearSheets();
  }

  @SimpleEvent(description = "Sheet ber-id tertutup (back/luar/Dismiss): id. "
      + "Registrasi tetap, tampilkan lagi via CallShowSheet.")
  public void SheetHidden(int id) {
    EventDispatcher.dispatchEvent(this, "SheetHidden", id);
  }

  @SimpleEvent(description = "Sheet ber-id tampil (hasil CallShowSheet): id.")
  public void SheetShown(int id) {
    EventDispatcher.dispatchEvent(this, "SheetShown", id);
  }

  @SimpleProperty(description = "Nama operasi Sheet (untuk if di "
      + "ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpSheet() {
    return sheet.OpSheet();
  }

  @SimpleProperty(description = "Nama operasi Toast (untuk if di "
      + "ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpToast() {
    return sheet.OpToast();
  }

  @SimpleProperty(description = "Nama operasi Notification (untuk if di "
      + "ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpNotification() {
    return sheet.OpNotification();
  }

  @SimpleFunction(description = "SET: Atur sudut membulat (radius, dp). Background "
      + "bawaan diganti.")
  public void SetCornerRadius(AndroidViewComponent comp, float radiusDp) {
    style.SetCornerRadius(comp, radiusDp);
  }

  @SimpleFunction(description = "SET: Atur garis tepi: warna + tebal (dp). "
      + "Background bawaan diganti.")
  public void SetBorder(AndroidViewComponent comp, int color, float widthDp) {
    style.SetBorder(comp, color, widthDp);
  }

  @SimpleFunction(description = "SET: Atur gradien banyak warna (list warna) + arah "
      + "angka: 0=atas-bawah, 1=kananATAS-kiriBAWAH, 2=kanan-kiri, 3=kananBAWAH-"
      + "kiriATAS, 4=bawah-atas, 5=kiriBAWAH-kananATAS, 6=kiri-kanan, "
      + "7=kiriATAS-kananBAWAH. Background bawaan diganti.")
  public void SetGradient(AndroidViewComponent comp, Object colors, int orientation) {
    style.SetGradient(comp, colors, orientation);
  }

  @SimpleFunction(description = "SET: Atur gradien radial: warna tengah, warna tepi, "
      + "radius (dp). Background bawaan diganti.")
  public void SetRadialGradient(AndroidViewComponent comp, int centerColor, int edgeColor, float radiusDp) {
    style.SetRadialGradient(comp, centerColor, edgeColor, radiusDp);
  }

  @SimpleFunction(description = "SET: Atur bentuk background angka: pakai properti "
      + "ShapeRectangle/ShapeOval/ShapeLine/ShapeRing.")
  public void SetShape(AndroidViewComponent comp, int shape) {
    style.SetShape(comp, shape);
  }

  @SimpleProperty(description = "Bentuk: persegi.",
      category = PropertyCategory.BEHAVIOR)
  public int ShapeRectangle() {
    return style.ShapeRectangle();
  }

  @SimpleProperty(description = "Bentuk: oval/lingkaran.",
      category = PropertyCategory.BEHAVIOR)
  public int ShapeOval() {
    return style.ShapeOval();
  }

  @SimpleProperty(description = "Bentuk: garis.",
      category = PropertyCategory.BEHAVIOR)
  public int ShapeLine() {
    return style.ShapeLine();
  }

  @SimpleProperty(description = "Bentuk: cincin.",
      category = PropertyCategory.BEHAVIOR)
  public int ShapeRing() {
    return style.ShapeRing();
  }

  @SimpleFunction(description = "SET: Atur visibilitas angka: pakai properti "
      + "VisibilityVisible/VisibilityInvisible/VisibilityGone.")
  public void SetVisibility(AndroidViewComponent comp, int visibility) {
    style.SetVisibility(comp, visibility);
  }

  @SimpleProperty(description = "Visibilitas: terlihat.",
      category = PropertyCategory.BEHAVIOR)
  public int VisibilityVisible() {
    return style.VisibilityVisible();
  }

  @SimpleProperty(description = "Visibilitas: tak terlihat (tempat tetap ada).",
      category = PropertyCategory.BEHAVIOR)
  public int VisibilityInvisible() {
    return style.VisibilityInvisible();
  }

  @SimpleProperty(description = "Visibilitas: hilang (tempat ikut hilang).",
      category = PropertyCategory.BEHAVIOR)
  public int VisibilityGone() {
    return style.VisibilityGone();
  }

  @SimpleFunction(description = "SET: Atur putaran komponen (derajat, searah jarum jam).")
  public void SetRotation(AndroidViewComponent comp, float degrees) {
    style.SetRotation(comp, degrees);
  }

  @SimpleFunction(description = "SET: Efek ripple (gelombang sentuh) warna tertentu. "
      + "Background bawaan diganti RippleDrawable.")
  public void SetRipple(AndroidViewComponent comp, int color) {
    style.SetRipple(comp, color);
  }

  @SimpleFunction(description = "SET: Garis tepi putus-putus: warna, tebal (dp), "
      + "panjang garis (dp), jarak garis (dp). dashed=false = garis penuh.")
  public void SetStroke(AndroidViewComponent comp, boolean dashed, float widthDp, int color, float dashLengthDp, float dashGapDp) {
    style.SetStroke(comp, dashed, widthDp, color, dashLengthDp, dashGapDp);
  }

  @SimpleFunction(description = "SET: Atur spasi teks (khusus Label/TextBox/Button): "
      + "jarak baris (dp) + jarak huruf (em). Selain teks: error.")
  public void SetTextSpace(AndroidViewComponent comp, float lineSpaceDp, float letterSpace) {
    style.SetTextSpace(comp, lineSpaceDp, letterSpace);
  }

  @SimpleFunction(description = "SET: Jadikan lingkaran/oval (cocok untuk foto "
      + "profil). Untuk lingkaran sempurna pakai komponen persegi.")
  public void SetCircular(AndroidViewComponent comp) {
    style.SetCircular(comp);
  }

  @SimpleFunction(description = "SET: Lingkaran + garis tepi warna (cocok untuk foto "
      + "profil).")
  public void SetCircularBorder(AndroidViewComponent comp, int color, float widthDp) {
    style.SetCircularBorder(comp, color, widthDp);
  }

  @SimpleFunction(description = "SET: Kembalikan efek sementara: hapus animasi, "
      + "alpha/rotasi/skala/posisi ke normal. Background tidak diubah.")
  public void SetResetEffects(AndroidViewComponent comp) {
    style.SetResetEffects(comp);
  }

  @SimpleFunction(description = "SET: Atur warna background polos. Background "
      + "bawaan diganti.")
  public void SetBackgroundColor(AndroidViewComponent comp, int color) {
    style.SetBackgroundColor(comp, color);
  }

  @SimpleFunction(description = "SET: Atur gambar background (file/asset, "
      + "path di-root /storage/emulated/0). Cocok untuk wadah.")
  public void SetBackgroundImage(AndroidViewComponent comp, String path) {
    style.SetBackgroundImage(comp, path);
  }

  @SimpleFunction(description = "SET: Atur bayangan/elevasi (dp). Efek terlihat di "
      + "Android 5.0+.")
  public void SetElevation(AndroidViewComponent comp, float dp) {
    style.SetElevation(comp, dp);
  }

  @SimpleFunction(description = "SET: Atur jarak dalam/padding (dp): "
      + "kiri, atas, kanan, bawah.")
  public void SetPadding(AndroidViewComponent comp, float leftDp, float topDp, float rightDp, float bottomDp) {
    style.SetPadding(comp, leftDp, topDp, rightDp, bottomDp);
  }

  @SimpleFunction(description = "SET: Atur jarak luar/margin (dp): kiri, atas, "
      + "kanan, bawah. Hanya bila induk mendukung margin.")
  public void SetMargin(AndroidViewComponent comp, float leftDp, float topDp, float rightDp, float bottomDp) {
    style.SetMargin(comp, leftDp, topDp, rightDp, bottomDp);
  }

  @SimpleFunction(description = "SET: Atur transparansi 0-100 (100 = penuh).")
  public void SetOpacity(AndroidViewComponent comp, int percent) {
    style.SetOpacity(comp, percent);
  }

  @SimpleFunction(description = "CALL: Animasi fade: startPercent ke endPercent "
      + "(0-100). Mis. 0->100 muncul, 100->0 hilang. Menembak "
      + "AnimationStarted/AnimationEnded.")
  public void CallAnimateFade(AndroidViewComponent comp, int durationMs, int startPercent, int endPercent) {
    style.CallAnimateFade(comp, durationMs, startPercent, endPercent);
  }

  @SimpleFunction(description = "CALL: Animasi geser-masuk: direction = "
      + "left/right/top/bottom. durationMs mis. 300.")
  public void CallAnimateSlideIn(AndroidViewComponent comp, String direction, int durationMs) {
    style.CallAnimateSlideIn(comp, direction, durationMs);
  }

  @SimpleFunction(description = "CALL: Animasi skala: ke toXPercent/toYPercent "
      + "(100 = normal). durationMs mis. 300.")
  public void CallAnimateScale(AndroidViewComponent comp, int durationMs, int toXPercent, int toYPercent) {
    style.CallAnimateScale(comp, durationMs, toXPercent, toYPercent);
  }

  @SimpleFunction(description = "CALL: Animasi putar (derajat, searah jarum jam). "
      + "durationMs mis. 300.")
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
      + "ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpStyle() {
    return style.OpStyle();
  }

  @SimpleFunction(description = "Buat id tugas unik baru (Number, siap pakai "
      + "ke CallSchedule*/CallSetAlarm). Urutan pembuatan menentukan urutan list.")
  public int CreateTaskId() {
    return task.CreateTaskId();
  }

  @SimpleFunction(description = "Alias GetActiveTaskIds: id yang masih pending.")
  public List<Integer> GetPendingTaskIds() {
    return task.GetPendingTaskIds();
  }

  @SimpleFunction(description = "CALL: Batalkan SEMUA tugas dalam-aplikasi (alarm "
      + "sistem tidak ikut; pakai CallCancelAllAlarms bila perlu). Tiap tugas "
      + "yang jalan menembak TaskCancelled.")
  public void CallCancelAllTasks() {
    task.CallCancelAllTasks();
  }

  @SimpleFunction(description = "CALL: Jadwalkan tugas sekali jalan: id + jeda "
      + "mili-detik + payload teks (JSON via Tools_Base bila perlu). "
      + "Menembak TaskStarted lalu TaskFinished. Sukses menembak Started; gagal via ErrorOccurred.")
  public void CallScheduleOnce(int id, long delayMillis, String payload) {
    task.CallScheduleOnce(id, delayMillis, payload);
  }

  @SimpleFunction(description = "CALL: Jadwalkan tugas berulang: id + jeda awal + "
      + "interval + jumlah ulang (mis. 5) atau -1 = selamanya + payload. "
      + "Tiap tick menembak TaskTick; selesai menembak TaskFinished.")
  public void CallScheduleRepeating(int id, long initialDelayMillis, long intervalMillis, int repeatCount, String payload) {
    task.CallScheduleRepeating(id, initialDelayMillis, intervalMillis, repeatCount, payload);
  }

  @SimpleFunction(description = "CALL: Jadwalkan tugas pada jam epoch mili-detik "
      + "(lihat NowMillis). Lewat = langsung jalan.")
  public void CallScheduleAt(int id, long epochMillis, String payload) {
    task.CallScheduleAt(id, epochMillis, payload);
  }

  @SimpleFunction(description = "SET: Jeda tugas ber-id (berlaku untuk once/repeating "
      + "yang belum selesai). Lanjut via SetResumeTask.")
  public void SetPauseTask(int id) {
    task.SetPauseTask(id);
  }

  @SimpleFunction(description = "SET: Lanjutkan tugas yang dijeda (SetPauseTask).")
  public void SetResumeTask(int id) {
    task.SetResumeTask(id);
  }

  @SimpleFunction(description = "CALL: Batalkan tugas ber-id (jadwal dalam-aplikasi "
      + "saja). Hasil lewat TaskCancelled.")
  public void CallCancelTask(int id) {
    task.CallCancelTask(id);
  }

  @SimpleFunction(description = "Waktu sekarang epoch mili-detik (untuk CallScheduleAt/SetAlarm).")
  public long NowMillis() {
    return task.NowMillis();
  }

  @SimpleFunction(description = "Fallback polling untuk event Task*: status "
      + "tugas ber-id. Salah satu: antri, jalan, jeda, selesai, batal, gagal, "
      + "alarm, atau tidak ada.")
  public String GetTaskStatus(int id) {
    return task.GetTaskStatus(id);
  }

  @SimpleFunction(description = "Fallback polling untuk TaskFinished: list "
      + "[success, payload, message]. Tetap ada sampai RemoveTaskId.")
  public List<Object> GetTaskResult(int id) {
    return task.GetTaskResult(id);
  }

  @SimpleFunction(description = "Fallback polling untuk TaskTick: list "
      + "[tickCount, repeatCount, lastTickAt, nextFireAt].")
  public List<Object> GetTaskProgress(int id) {
    return task.GetTaskProgress(id);
  }

  @SimpleFunction(description = "Id tugas dalam-aplikasi terakhir yang selesai/bunyi. -1 bila belum ada.")
  public int GetLastFiredTaskId() {
    return task.GetLastFiredTaskId();
  }

  @SimpleFunction(description = "Payload tugas terakhir yang selesai/bunyi.")
  public String GetLastFiredPayload() {
    return task.GetLastFiredPayload();
  }

  @SimpleFunction(description = "Waktu epoch tugas terakhir selesai/bunyi. 0 bila belum ada.")
  public long GetLastFiredTime() {
    return task.GetLastFiredTime();
  }

  @SimpleFunction(description = "SET: Simpan extra teks untuk tugas ber-id (mis. "
      + "url, nama file). Menimpa kunci yang sama. Tanpa feedback.")
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
  public void ClearTaskExtras(int id) {
    task.ClearTaskExtras(id);
  }

  @SimpleFunction(description = "SET: Simpan variabel teks global (bisa dibaca tugas "
      + "mana pun + blok lain).")
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

  @SimpleFunction(description = "Hapus SEMUA variabel global.")
  public void ClearVars() {
    task.ClearVars();
  }

  @SimpleFunction(description = "Semua nama variabel global (URUT DIBUAT).")
  public List<String> GetAllVarNames() {
    return task.GetAllVarNames();
  }

  @SimpleFunction(description = "SET: Minta izin exact-alarm (Android 12+). Best-effort: "
      + "bila ditolak, alarm jalan inexact (tetap bunyi, bisa meleset).")
  public void SetRequestExactAlarmPermission() {
    task.SetRequestExactAlarmPermission();
  }

  @SimpleFunction(description = "True bila boleh pasang exact-alarm "
      + "(Android <12 selalu true).")
  public boolean IsExactAlarmAllowed() {
    return task.IsExactAlarmAllowed();
  }

  @SimpleFunction(description = "CALL: Pasang alarm SISTEM sekali bunyi: id + epoch "
      + "mili-detik + payload + exact (true = tepat waktu bila izin ada). "
      + "Bunyi = membuka aplikasi; baca via CheckStartupAlarm/AlarmFired. "
      + "Tersimpan otomatis (survive reboot via RestoreAlarms).")
  public void CallSetAlarm(int id, long epochMillis, String payload, boolean exact) {
    task.CallSetAlarm(id, epochMillis, payload, exact);
  }

  @SimpleFunction(description = "CALL: Pasang alarm SISTEM berulang: id + epoch "
      + "pertama + interval + payload. Interval <1 detik dibulatkan ke 1 detik "
      + "(hemat baterai, aturan Android).")
  public void CallSetRepeatingAlarm(int id, long firstEpochMillis, long intervalMillis, String payload) {
    task.CallSetRepeatingAlarm(id, firstEpochMillis, intervalMillis, payload);
  }

  @SimpleFunction(description = "SET: Cabut alarm sistem ber-id (tugas dalam-aplikasi "
      + "dengan id sama tidak ikut; pakai CancelTask bila perlu).")
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

  @SimpleFunction(description = "Fallback polling alarm: list [epochMillis, "
      + "intervalMillis (0 = sekali), payload]. Kosong = tidak ada.")
  public List<Object> GetAlarmInfo(int id) {
    return task.GetAlarmInfo(id);
  }

  @SimpleFunction(description = "CALL: WAJIB dipanggil di Screen.Initialize (dan "
      + "timer bila perlu): cek apakah aplikasi dibuka oleh alarm Tools_Task. "
      + "Tanpa return: bila dibuka oleh alarm dan datanya baru, menembak AlarmFired.")
  public void CallCheckStartupAlarm() {
    task.CallCheckStartupAlarm();
  }

  @SimpleFunction(description = "CALL: Pasang ulang SEMUA alarm tersimpan (mis. "
      + "habis reboot: panggil di Screen.Initialize). Tanpa return.")
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
      + "id + payload. Selalu cek juga via CheckStartupAlarm di Initialize.")
  public void AlarmFired(int id, String payload) {
    EventDispatcher.dispatchEvent(this, "AlarmFired", id, payload);
  }

  @SimpleProperty(description = "Nama operasi Task (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpTask() {
    return task.OpTask();
  }

  @SimpleProperty(description = "Nama operasi Alarm (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpAlarm() {
    return task.OpAlarm();
  }

  @SimpleProperty(description = "Nama operasi Var (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpVar() {
    return task.OpVar();
  }

  @SimpleFunction(description = "Putar video ber-id di dalam layout (Arrangement MIT / dinamis). "
      + "Bisa jalan bareng (satu id = satu video). path file (/Download/..., absolut "
      + "apa adanya), atau http(s) URL (streaming). id dari CreateVideoId / "
      + "Tools_Base CreateUniqueId. "
      + "Atur tinggi arrangement (mis. 300px) agar video kelihatan. "
      + "Double-tap: kiri = mundur, kanan = maju, tengah = pause/resume. "
      + "Sukses menembak VideoStarted; gagal via ErrorOccurred.")
  public void CallPlayVideoIn(AndroidViewComponent container, String path, int id) {
    video.CallPlayVideoIn(container, path, id);
  }

  @SimpleFunction(description = "SET: Jeda video yang sedang main. (per id)")
  public void SetPauseVideo(int id) {
    video.SetPauseVideo(id);
  }

  @SimpleFunction(description = "SET: Lanjutkan video yang dijeda. (per id)")
  public void SetResumeVideo(int id) {
    video.SetResumeVideo(id);
  }

  @SimpleFunction(description = "SET: Hentikan video + lepas dari layout (per id). "
      + "Id tetap tercatat (Play lagi langsung bisa). Bersih total via RemoveVideoId.")
  public void SetStopVideo(int id) {
    video.SetStopVideo(id);
  }

  @SimpleFunction(description = "SET: Lompat video ke posisi (ms). Bisa dipasang ke Slider. (per id)")
  public void SetSeekVideo(int id, int position) {
    video.SetSeekVideo(id, position);
  }

  @SimpleFunction(description = "SET: Maju video sejauh SkipSeconds detik. (per id)")
  public void SetForwardVideo(int id) {
    video.SetForwardVideo(id);
  }

  @SimpleFunction(description = "SET: Mundur video sejauh SkipSeconds detik. (per id)")
  public void SetBackwardVideo(int id) {
    video.SetBackwardVideo(id);
  }

  @SimpleFunction(description = "Posisi video sekarang (ms). (per id)")
  public int GetVideoPosition(int id) {
    return video.GetVideoPosition(id);
  }

  @SimpleFunction(description = "Durasi video (ms). 0 bila belum siap. (per id)")
  public int GetVideoDuration(int id) {
    return video.GetVideoDuration(id);
  }

  @SimpleFunction(description = "True bila video id ini sedang main.")
  public boolean IsVideoPlaying(int id) {
    return video.IsVideoPlaying(id);
  }

  @SimpleFunction(description = "SET: Hapus SEMUA video Tools_Video dari layout itu (id ikut dilepas, "
      + "komponen lain tidak diusik). Video ikut di-pause.")
  public void SetClearVideo(AndroidViewComponent container) {
    video.SetClearVideo(container);
  }

  @SimpleFunction(description = "Buat id video unik baru (Number, siap pakai ke CallPlayVideoIn).")
  public int CreateVideoId() {
    return video.CreateVideoId();
  }

  @SimpleProperty(description = "Tampilkan kontrol bawaan video. Default true.",
      category = PropertyCategory.BEHAVIOR)
  public boolean ShowVideoController() {
    return video.ShowVideoController();
  }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void ShowVideoController(boolean show) {
    video.ShowVideoController(show);
  }

  @SimpleProperty(description = "Video mengulang otomatis bila selesai. Default false.",
      category = PropertyCategory.BEHAVIOR)
  public boolean VideoLooping() {
    return video.VideoLooping();
  }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void VideoLooping(boolean loop) {
    video.VideoLooping(loop);
  }

  @SimpleProperty(description = "Double-tap video: kiri = mundur, kanan = maju, "
      + "tengah = pause/resume. Default true.",
      category = PropertyCategory.BEHAVIOR)
  public boolean DoubleTapSeek() {
    return video.DoubleTapSeek();
  }

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

  @SimpleProperty(description = "Nama operasi Video (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpVideo() {
    return video.OpVideo();
  }

  @SimpleProperty(description = "Jeda event progres dalam milidetik. Satu kenop mengatur Audio+Video sekaligus; baca mengembalikan nilai Audio.",
      category = PropertyCategory.BEHAVIOR)
  public int ProgressInterval() {
    return audio.ProgressInterval();
  }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void ProgressInterval(int value) {
    audio.ProgressInterval(value);
    video.ProgressInterval(value);
  }

  @SimpleProperty(description = "Lompatan Maju/Mundur dalam detik. Satu kenop mengatur Audio+Video sekaligus; baca mengembalikan nilai Audio.",
      category = PropertyCategory.BEHAVIOR)
  public int SkipSeconds() {
    return audio.SkipSeconds();
  }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void SkipSeconds(int value) {
    audio.SkipSeconds(value);
    video.SkipSeconds(value);
  }

  @SimpleProperty(description = "Volume media 0-100. Satu kenop mengatur Audio+Video sekaligus; baca mengembalikan nilai Audio.",
      category = PropertyCategory.BEHAVIOR)
  public int MediaVolume() {
    return audio.MediaVolume();
  }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void MediaVolume(int value) {
    audio.MediaVolume(value);
    video.MediaVolume(value);
  }
}
