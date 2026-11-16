package com.fv7.tools;

import android.app.Activity;
import android.content.Context;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.util.JsonUtil;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;

@DesignerComponent(
    version = 1,
    description = "Fondasi Tools: id pool lintas extension + helper Tag untuk id "
        + "komponen LUAR (auto-konversi teks angka) + util dasar "
        + "(JSON, URI, waktu, byte, mime). Id unik global via CreateUniqueId, "
        + "atau per tipe via CreateDownloadId/CreateImageId/CreateAudioId/CreateVideoId. "
        + "Semua list id URUT PEMBUATAN (index 1 = pertama dibuat).",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    androidMinSdk = 21,
    iconName = "aiwebres/tools_base.jpg")
@SimpleObject(external = true)
public class Tools_Base extends AndroidNonvisibleComponent {

  private final Context context;
  private final Activity activity;

  public Tools_Base(ComponentContainer container) {
    super(container.$form());
    this.context = container.$context();
    this.activity = container.$context();
  }

  // ================= ID POOL (global) =================

  @SimpleFunction(description = "Buat id unik global baru (Number). Reservasi otomatis, "
      + "siap pakai ke Download / ShowImage / PlayAudio / PlayVideoIn "
      + "(fungsi Start mengadopsi reservasi). Urutan pembuatan = urutan list.")
  public int CreateUniqueId() {
    int id = Ids.createUnique();
    if (id < 1) {
      Shared.err(this, "Id", 0, "gagal buat id");
    }
    return id;
  }

  @SimpleFunction(description = "Id yang disarankan: angka terkecil >= 1 yang belum dipakai "
      + "siapa pun (tidak di-reservasi, langsung pakai).")
  public int SuggestId() {
    return Ids.suggest();
  }

  @SimpleFunction(description = "True bila id sedang dipakai operasi lain.")
  public boolean IsIdBusy(int id) {
    return Ids.isBusy(id);
  }

  @SimpleFunction(description = "Semua id sibuk lintas tipe (URUT PEMBUATAN, "
      + "index 1 = pertama dibuat). Kosong = tidak ada.")
  public List<Integer> BusyIds() {
    return Ids.busyList();
  }

  @SimpleFunction(description = "Lepas paksa id yang macet. Jangan dipakai saat operasinya "
      + "masih jalan (pakai Cancel/Stop/Remove dulu).")
  public void ReleaseId(int id) {
    Ids.release(id);
  }

  // ================= ID: Download =================

  @SimpleFunction(description = "Semua id unduhan (URUT PEMBUATAN, index 1 = pertama dibuat).")
  public List<Integer> GetDownloadIds() {
    return Ids.idsOf(Ids.DOWNLOAD);
  }

  @SimpleFunction(description = "Id unduhan yang sedang jalan (URUT PEMBUATAN). "
      + "Yang dijeda TIDAK masuk sini.")
  public List<Integer> GetActiveDownloadIds() {
    return Ids.activeIdsOf(Ids.DOWNLOAD);
  }

  @SimpleFunction(description = "Id unduhan terakhir dibuat/di-start. -1 bila kosong.")
  public int CurrentDownloadId() {
    return Ids.currentOf(Ids.DOWNLOAD);
  }

  @SimpleFunction(description = "Id unduhan index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstDownloadId() {
    return Ids.firstOf(Ids.DOWNLOAD);
  }

  @SimpleFunction(description = "Id unduhan TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastDownloadId() {
    return Ids.lastOf(Ids.DOWNLOAD);
  }

  @SimpleFunction(description = "Id unduhan yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.")
  public int GetPreviousDownloadId(int id) {
    return Ids.previousOf(Ids.DOWNLOAD, id);
  }

  @SimpleFunction(description = "True bila id unduhan ini sedang jalan.")
  public boolean IsDownloadIdActive(int id) {
    return Ids.isActive(Ids.DOWNLOAD, id);
  }

  @SimpleFunction(description = "Hapus id unduhan: batalkan bila jalan + lepas id.")
  public void RemoveDownloadId(int id) {
    Tools_Downloader.cancelStatic(id);
    Ids.setActive(Ids.DOWNLOAD, id, false);
    Ids.unregister(Ids.DOWNLOAD, id);
  }

  // ================= ID: Image =================

  @SimpleFunction(description = "Semua id gambar (URUT PEMBUATAN, index 1 = pertama dibuat).")
  public List<Integer> GetImageIds() {
    return Ids.idsOf(Ids.IMAGE);
  }

  @SimpleFunction(description = "Id gambar yang sedang tampil/loading (URUT PEMBUATAN).")
  public List<Integer> GetActiveImageIds() {
    return Ids.activeIdsOf(Ids.IMAGE);
  }

  @SimpleFunction(description = "Id gambar terakhir dibuat/di-start. -1 bila kosong.")
  public int CurrentImageId() {
    return Ids.currentOf(Ids.IMAGE);
  }

  @SimpleFunction(description = "Id gambar index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstImageId() {
    return Ids.firstOf(Ids.IMAGE);
  }

  @SimpleFunction(description = "Id gambar TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastImageId() {
    return Ids.lastOf(Ids.IMAGE);
  }

  @SimpleFunction(description = "Id gambar yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.")
  public int GetPreviousImageId(int id) {
    return Ids.previousOf(Ids.IMAGE, id);
  }

  @SimpleFunction(description = "True bila id gambar ini sedang tampil/loading.")
  public boolean IsImageIdActive(int id) {
    return Ids.isActive(Ids.IMAGE, id);
  }

  @SimpleFunction(description = "Hapus id gambar: lepas dari layout + lepas id.")
  public void RemoveImageId(int id) {
    Tools_Image.clearStatic(id);
  }

  // ================= ID: Audio =================

  @SimpleFunction(description = "Semua id audio (URUT PEMBUATAN, index 1 = pertama dibuat).")
  public List<Integer> GetAudioIds() {
    return Ids.idsOf(Ids.AUDIO);
  }

  @SimpleFunction(description = "Id audio yang sedang bunyi (URUT PEMBUATAN).")
  public List<Integer> GetActiveAudioIds() {
    return Ids.activeIdsOf(Ids.AUDIO);
  }

  @SimpleFunction(description = "Id audio terakhir dibuat/di-start. -1 bila kosong.")
  public int CurrentAudioId() {
    return Ids.currentOf(Ids.AUDIO);
  }

  @SimpleFunction(description = "Id audio index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstAudioId() {
    return Ids.firstOf(Ids.AUDIO);
  }

  @SimpleFunction(description = "Id audio TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastAudioId() {
    return Ids.lastOf(Ids.AUDIO);
  }

  @SimpleFunction(description = "Id audio yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.")
  public int GetPreviousAudioId(int id) {
    return Ids.previousOf(Ids.AUDIO, id);
  }

  @SimpleFunction(description = "True bila id audio ini sedang bunyi.")
  public boolean IsAudioIdActive(int id) {
    return Ids.isActive(Ids.AUDIO, id);
  }

  @SimpleFunction(description = "Hapus id audio: hentikan bila bunyi + lepas id.")
  public void RemoveAudioId(int id) {
    Tools_Audio.stopStatic(id);
  }

  // ================= ID: Video =================

  @SimpleFunction(description = "Semua id video (URUT PEMBUATAN, index 1 = pertama dibuat).")
  public List<Integer> GetVideoIds() {
    return Ids.idsOf(Ids.VIDEO);
  }

  @SimpleFunction(description = "Id video yang sedang main (URUT PEMBUATAN).")
  public List<Integer> GetActiveVideoIds() {
    return Ids.activeIdsOf(Ids.VIDEO);
  }

  @SimpleFunction(description = "Id video terakhir dibuat/di-start. -1 bila kosong.")
  public int CurrentVideoId() {
    return Ids.currentOf(Ids.VIDEO);
  }

  @SimpleFunction(description = "Id video index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstVideoId() {
    return Ids.firstOf(Ids.VIDEO);
  }

  @SimpleFunction(description = "Id video TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastVideoId() {
    return Ids.lastOf(Ids.VIDEO);
  }

  @SimpleFunction(description = "Id video yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.")
  public int GetPreviousVideoId(int id) {
    return Ids.previousOf(Ids.VIDEO, id);
  }

  @SimpleFunction(description = "True bila id video ini sedang main.")
  public boolean IsVideoIdActive(int id) {
    return Ids.isActive(Ids.VIDEO, id);
  }

  @SimpleFunction(description = "Hapus id video: hentikan + lepas layout + lepas id.")
  public void RemoveVideoId(int id) {
    Tools_Video.stopStatic(id);
  }

  // ================= ID: Core (dinamis) =================

  @SimpleFunction(description = "Semua id dinamis Tools_Core (URUT PEMBUATAN, "
      + "index 1 = pertama dibuat).")
  public List<Integer> GetDynamicIds() {
    return Ids.idsOf(Ids.CORE);
  }

  @SimpleFunction(description = "Id dinamis yang aktif (URUT PEMBUATAN).")
  public List<Integer> GetActiveDynamicIds() {
    return Ids.activeIdsOf(Ids.CORE);
  }

  @SimpleFunction(description = "Id dinamis terakhir dibuat. -1 bila kosong.")
  public int CurrentDynamicId() {
    return Ids.currentOf(Ids.CORE);
  }

  @SimpleFunction(description = "Id dinamis index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstDynamicId() {
    return Ids.firstOf(Ids.CORE);
  }

  @SimpleFunction(description = "Id dinamis TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastDynamicId() {
    return Ids.lastOf(Ids.CORE);
  }

  @SimpleFunction(description = "Id dinamis yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.")
  public int GetPreviousDynamicId(int id) {
    return Ids.previousOf(Ids.CORE, id);
  }

  @SimpleFunction(description = "True bila id dinamis ini masih ada.")
  public boolean IsDynamicIdActive(int id) {
    return Ids.isActive(Ids.CORE, id);
  }

  @SimpleFunction(description = "Hapus id dinamis: lepas view + lepas id.")
  public void RemoveDynamicId(int id) {
    Tools_Core.clearStatic(id);
  }

  // ================= ID: Layout (dinamis) =================

  @SimpleFunction(description = "Semua id layout Tools_Layout (URUT PEMBUATAN, "
      + "index 1 = pertama dibuat).")
  public List<Integer> GetLayoutIds() {
    return Ids.idsOf(Ids.LAYOUT);
  }

  @SimpleFunction(description = "Id layout yang aktif (URUT PEMBUATAN).")
  public List<Integer> GetActiveLayoutIds() {
    return Ids.activeIdsOf(Ids.LAYOUT);
  }

  @SimpleFunction(description = "Id layout terakhir dibuat. -1 bila kosong.")
  public int CurrentLayoutId() {
    return Ids.currentOf(Ids.LAYOUT);
  }

  @SimpleFunction(description = "Id layout index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstLayoutId() {
    return Ids.firstOf(Ids.LAYOUT);
  }

  @SimpleFunction(description = "Id layout TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastLayoutId() {
    return Ids.lastOf(Ids.LAYOUT);
  }

  @SimpleFunction(description = "Id layout yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.")
  public int GetPreviousLayoutId(int id) {
    return Ids.previousOf(Ids.LAYOUT, id);
  }

  @SimpleFunction(description = "True bila id layout ini masih ada.")
  public boolean IsLayoutIdActive(int id) {
    return Ids.isActive(Ids.LAYOUT, id);
  }

  @SimpleFunction(description = "Hapus id layout: lepas dari induk (anak dinamis "
      + "di dalamnya ikut beres) + lepas id.")
  public void RemoveLayoutId(int id) {
    Tools_Layout.clearStatic(id);
  }

  // ================= ID: Sheet =================

  @SimpleFunction(description = "Semua id sheet Tools_Sheet (URUT PEMBUATAN, "
      + "index 1 = pertama dibuat).")
  public List<Integer> GetSheetIds() {
    return Ids.idsOf(Ids.SHEET);
  }

  @SimpleFunction(description = "Id sheet yang aktif (URUT PEMBUATAN).")
  public List<Integer> GetActiveSheetIds() {
    return Ids.activeIdsOf(Ids.SHEET);
  }

  @SimpleFunction(description = "Id sheet terakhir dibuat. -1 bila kosong.")
  public int CurrentSheetId() {
    return Ids.currentOf(Ids.SHEET);
  }

  @SimpleFunction(description = "Id sheet index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstSheetId() {
    return Ids.firstOf(Ids.SHEET);
  }

  @SimpleFunction(description = "Id sheet TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastSheetId() {
    return Ids.lastOf(Ids.SHEET);
  }

  @SimpleFunction(description = "Id sheet yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.")
  public int GetPreviousSheetId(int id) {
    return Ids.previousOf(Ids.SHEET, id);
  }

  @SimpleFunction(description = "True bila id sheet ini masih terdaftar.")
  public boolean IsSheetIdActive(int id) {
    return Ids.isActive(Ids.SHEET, id);
  }

  @SimpleFunction(description = "Hapus id sheet: tutup diam-diam + lepas id.")
  public void RemoveSheetId(int id) {
    Tools_Sheet.clearStatic(id);
  }

  // ================= ID: Slider =================

  @SimpleFunction(description = "Semua id slider Tools_Image (URUT PEMBUATAN, "
      + "index 1 = pertama dibuat).")
  public List<Integer> GetSliderIds() {
    return Ids.idsOf(Ids.SLIDER);
  }

  @SimpleFunction(description = "Id slider yang aktif (URUT PEMBUATAN).")
  public List<Integer> GetActiveSliderIds() {
    return Ids.activeIdsOf(Ids.SLIDER);
  }

  @SimpleFunction(description = "Id slider terakhir dibuat. -1 bila kosong.")
  public int CurrentSliderId() {
    return Ids.currentOf(Ids.SLIDER);
  }

  @SimpleFunction(description = "Id slider index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstSliderId() {
    return Ids.firstOf(Ids.SLIDER);
  }

  @SimpleFunction(description = "Id slider TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastSliderId() {
    return Ids.lastOf(Ids.SLIDER);
  }

  @SimpleFunction(description = "Id slider yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.")
  public int GetPreviousSliderId(int id) {
    return Ids.previousOf(Ids.SLIDER, id);
  }

  @SimpleFunction(description = "True bila id slider ini masih ada.")
  public boolean IsSliderIdActive(int id) {
    return Ids.isActive(Ids.SLIDER, id);
  }

  @SimpleFunction(description = "Hapus id slider: lepas dari layout (autoplay "
      + "mati) + lepas id.")
  public void RemoveSliderId(int id) {
    Tools_Image.clearSliderStatic(id);
  }

  // ================= ID: Task =================

  @SimpleFunction(description = "Semua id tugas/alarm Tools_Task (URUT PEMBUATAN, "
      + "index 1 = pertama dibuat).")
  public List<Integer> GetTaskIds() {
    return Ids.idsOf(Ids.TASK);
  }

  @SimpleFunction(description = "Id tugas/alarm yang aktif (URUT PEMBUATAN).")
  public List<Integer> GetActiveTaskIds() {
    return Ids.activeIdsOf(Ids.TASK);
  }

  @SimpleFunction(description = "Id tugas terakhir dibuat. -1 bila kosong.")
  public int CurrentTaskId() {
    return Ids.currentOf(Ids.TASK);
  }

  @SimpleFunction(description = "Id tugas index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstTaskId() {
    return Ids.firstOf(Ids.TASK);
  }

  @SimpleFunction(description = "Id tugas TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastTaskId() {
    return Ids.lastOf(Ids.TASK);
  }

  @SimpleFunction(description = "Id tugas yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.")
  public int GetPreviousTaskId(int id) {
    return Ids.previousOf(Ids.TASK, id);
  }

  @SimpleFunction(description = "True bila id tugas ini aktif (jalan/jeda/alarm terpasang).")
  public boolean IsTaskIdActive(int id) {
    return Ids.isActive(Ids.TASK, id);
  }

  @SimpleFunction(description = "Hapus id tugas: batalkan jadwal + cabut alarm "
      + "sistem + lepas id.")
  public void RemoveTaskId(int id) {
    Tools_Task.clearStaticFull(context, id);
  }

  // ================= ID TAG (helper komponen LUAR) =================

  @SimpleFunction(description = "Buat id unik baru di tag ini (Number, angka terkecil yang belum "
      + "dipakai, langsung di-reservasi). Untuk saat kamulah yang menentukan id-nya "
      + "(mis. bikin tab sendiri). Beda dengan SaveTagId yang mencatat id dari luar.")
  public int CreateTagId(String tag) {
    int id = TagIds.create(tag);
    if (id < 0) {
      Shared.err(this, "Id", 0, "tag kosong");
    }
    return id;
  }

  @SimpleFunction(description = "Simpan id komponen LUAR ke tag (mis. WebView pihak ketiga). "
      + "id boleh Number ATAU teks angka (\"5\" = 5, otomatis dikonversi). "
      + "Id dobel = tetap satu. Gagal (tag kosong / bukan angka) via ErrorOccurred.")
  public void SaveTagId(Object id, String tag) {
    boolean ok = TagIds.save(id, tag);
    if (!ok) {
      Shared.err(this, "Id", 0, "tag kosong / id bukan angka");
    }
  }

  @SimpleFunction(description = "Semua id di tag ini (Number semua, URUT MENYIMPAN, "
      + "index 1 = pertama disimpan).")
  public List<Integer> GetTagIds(String tag) {
    return TagIds.list(tag);
  }

  @SimpleFunction(description = "Id index 1 = PERTAMA DISIMPAN di tag ini. -1 bila kosong.")
  public int GetFirstTagId(String tag) {
    return TagIds.first(tag);
  }

  @SimpleFunction(description = "Id TERAKHIR DISIMPAN di tag ini. -1 bila kosong.")
  public int GetLastTagId(String tag) {
    return TagIds.last(tag);
  }

  @SimpleFunction(description = "Id yang disimpan TEPAT SEBELUM id ini di tag itu. "
      + "id boleh Number / teks angka. -1 bila tidak ada.")
  public int GetPreviousTagId(Object id, String tag) {
    return TagIds.previous(id, tag);
  }

  @SimpleFunction(description = "True bila id ini tercatat di tag itu "
      + "(teks angka ikut cocok: \"5\" = 5).")
  public boolean IsTagIdSaved(Object id, String tag) {
    return TagIds.contains(id, tag);
  }

  @SimpleFunction(description = "Jumlah id tercatat di tag ini.")
  public int GetTagIdCount(String tag) {
    return TagIds.count(tag);
  }

  @SimpleFunction(description = "Hapus satu id dari tag (hanya lepas catatan, komponen luar "
      + "tidak diusik). Tag yang kosong ikut dibuang. Tidak tercatat via ErrorOccurred.")
  public void RemoveTagId(Object id, String tag) {
    boolean gone = TagIds.remove(id, tag);
    if (!gone) {
      Shared.err(this, "Id", 0, "id tidak tercatat di tag itu");
    }
  }

  @SimpleFunction(description = "Buang SELURUH isi satu tag.")
  public void ClearTag(String tag) {
    TagIds.clear(tag);
  }

  @SimpleFunction(description = "Semua nama tag yang masih punya isi (URUT DIBUAT).")
  public List<String> GetAllTags() {
    return TagIds.allTags();
  }

  // ================= JSON =================

  @SimpleFunction(description = "Encode list/dictionary/teks jadi JSON.")
  public String JsonEncode(Object value) {
    try {
      return JsonUtil.getJsonRepresentation(value);
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Json", 0, m == null ? "encode gagal" : m);
      return "";
    }
  }

  @SimpleFunction(description = "Decode teks JSON jadi dictionary/list. Gagal = \"\" + error.")
  public Object JsonDecode(String json) {
    try {
      return JsonUtil.getObjectFromJson(json == null ? "" : json);
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Json", 0, m == null ? "decode gagal" : m);
      return "";
    }
  }

  // ================= URI =================

  @SimpleFunction(description = "Encode teks untuk URL (spasi jadi %20).")
  public String UriEncode(String text) {
    try {
      String s = URLEncoder.encode(text == null ? "" : text, "UTF-8");
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < s.length(); i = i + 1) {
        char ch = s.charAt(i);
        if (ch == '+') {
          sb.append("%20");
        } else {
          sb.append(ch);
        }
      }
      return sb.toString();
    } catch (Exception e) {
      return text == null ? "" : text;
    }
  }

  @SimpleFunction(description = "Decode teks URL-encoded.")
  public String UriDecode(String text) {
    try {
      return URLDecoder.decode(text == null ? "" : text, "UTF-8");
    } catch (Exception e) {
      return text == null ? "" : text;
    }
  }

  // ================= FORMAT =================

  @SimpleFunction(description = "Ubah milidetik jadi teks jam (m:ss / h:mm:ss) untuk Label.")
  public String FormatTime(int millis) {
    return Shared.formatTime(millis);
  }

  @SimpleFunction(description = "Ubah angka byte jadi teks (B/KB/MB/GB).")
  public String ConvertBytes(long bytes) {
    return Shared.convertBytes(bytes);
  }

  @SimpleFunction(description = "Tebak mime dari nama file/ekstensi. Tidak perlu file-nya ada.")
  public String GetMimeType(String fileName) {
    return Shared.mimeFor(fileName);
  }

  @SimpleFunction(description = "Versi paket Tools (sama untuk semua Tools_* dalam satu .aix).")
  public int ToolsVersion() {
    return 8;
  }

  // ================= OP + ERROR =================

  @SimpleProperty(description = "Nama operasi Json (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpJson() {
    return "Json";
  }

  @SimpleProperty(description = "Nama operasi Id (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpId() {
    return "Id";
  }

  @SimpleFunction(description = "Fallback polling untuk event ErrorOccurred: "
      + "pesan error terakhir operasi ini (Id/Json/Tag, kosong bila belum ada).")
  public String LastError(String operation) {
    return Shared.lastError(operation);
  }

  @SimpleEvent(description = "SATU-SATUNYA error Tools_Base. component = nama operasi "
      + "(bandingkan dengan blok Op...).")
  public void ErrorOccurred(String component, int id, String message) {
    EventDispatcher.dispatchEvent(this, "ErrorOccurred",
        component, Integer.valueOf(id), message);
  }
}
