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
  public int GetLastAudi