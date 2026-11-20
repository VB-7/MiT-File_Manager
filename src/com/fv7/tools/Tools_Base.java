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
    version = 20,
    description = "Fondasi Tools: id pool global + helper Tag untuk id komponen "
        + "LUAR (auto-konversi teks angka: \"5\" = 5) + util dasar (JSON, URI, "
        + "waktu, byte, mime). Butuh id? CreateUniqueId (global, reservasi "
        + "otomatis). Id komponen luar? RegisterTagId (catat) / CreateTagId "
        + "(buatkan). Angka cepat tanpa booking? SuggestId (tanpa reservasi, "
        + "JANGAN dipakai dua tempat). Id per tipe (Download/Image/dll) ada "
        + "di komponen pemiliknya masing-masing, bukan di sini. Konvensi "
        + "awalan: Is = boolean (colok ke if), Call = async + hasil lewat "
        + "event, Set = tanpa feedback, Get = nilai balik sinkron.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    androidMinSdk = 21,
    iconName = "aiwebres/tools_base.jpg",
    helpUrl = "https://github.com/VB-7/MiT-File_Manager")
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

  @SimpleFunction(description = "Buat id unik global baru (Number, reservasi otomatis). "
      + "Siap pakai ke blok Call... pemiliknya (Download/Image/Audio/Video/dll; "
      + "reservasi otomatis diadopsi). Urutan pembuatan = urutan list.")
  public int CreateUniqueId() {
    int id = Ids.createUnique();
    if (id < 1) {
      Shared.err(this, "Id", 0, "gagal buat id");
    }
    return id;
  }

  @SimpleFunction(description = "BAHAYA: angka terkecil >= 1 yang belum dipakai siapa "
      + "pun, TAPI tidak di-reservasi (tidak dibooking). Dua pemanggil bisa "
      + "dapat angka SAMA = tabrakan. JANGAN dipakai dua tempat bersamaan; "
      + "kalau ragu pakai CreateUniqueId (reservasi otomatis).")
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

  @SimpleFunction(description = "SET: lepas paksa id yang macet (ruang UGD). "
      + "Resep urut: Cancel/Stop/Remove pemiliknya DULU, SetForceReleaseId "
      + "TERAKHIR. Id macet bila cancel tidak melepas / crash di tengah. "
      + "Tanpa feedback.")
  public void SetForceReleaseId(int id) {
    Ids.release(id);
  }

  // ================= ID TAG (helper komponen LUAR) =================

  @SimpleFunction(description = "Buat id unik baru di tag ini (Number, angka terkecil yang belum "
      + "dipakai, langsung di-reservasi). Untuk saat kamulah yang menentukan id-nya "
      + "(mis. bikin tab sendiri). Beda dengan RegisterTagId yang mencatat id dari luar.")
  public int CreateTagId(String tag) {
    int id = TagIds.create(tag);
    if (id < 0) {
      Shared.err(this, "Id", 0, "tag kosong");
    }
    return id;
  }

  @SimpleFunction(description = "Catat id komponen LUAR ke tag (mis. WebView pihak ketiga). "
      + "id boleh Number ATAU teks angka (\"5\" = 5, otomatis dikonversi). "
      + "Beda dengan CreateTagId yang MEMBUATKAN id baru. Id dobel = tetap "
      + "satu (idempoten). Gagal (tag kosong / bukan angka) via ErrorOccurred.")
  public void RegisterTagId(Object id, String tag) {
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

  @SimpleFunction(description = "Id pada nomor urut index di tag ini "
      + "(1 = pertama disimpan). -1 bila tag kosong / index di luar. "
      + "Cth: GetTagIds lalu GetTagIdAt(tag, 2) = id kedua.")
  public int GetTagIdAt(String tag, int index) {
    return TagIds.at(tag, index);
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

  @SimpleFunction(description = "True bila tag ini ADA (masih menyimpan "
      + "id). Beda dengan IsTagIdSaved yang cek id di dalam satu tag.")
  public boolean IsTagIdExists(String tag) {
    return TagIds.tagExists(tag);
  }

  @SimpleFunction(description = "Jumlah id tercatat di tag ini.")
  public int GetTagIdCount(String tag) {
    return TagIds.count(tag);
  }

  @SimpleFunction(description = "Hapus satu id dari tag (hanya lepas catatan, komponen luar "
      + "tidak diusik). Tag yang kosong ikut dibuang. Id yang memang tidak "
      + "tercatat = idempoten, TAPI tetap lapor via ErrorOccurred (\"tidak tercatat\") "
      + "agar salah ketik tag ketahuan; bukan diam.")
  public void RemoveTagId(Object id, String tag) {
    boolean gone = TagIds.remove(id, tag);
    if (!gone) {
      Shared.err(this, "Id", 0, "id tidak tercatat di tag itu");
    }
  }

  @SimpleFunction(description = "SET: hapus SELURUH isi satu tag. Tanpa feedback.")
  public void SetClearTag(String tag) {
    TagIds.clear(tag);
  }

  @SimpleFunction(description = "Semua nama tag yang masih punya isi (URUT DIBUAT).")
  public List<String> GetAllTags() {
    return TagIds.allTags();
  }

  // ================= JSON =================

  @SimpleFunction(description = "Encode list/dictionary/teks/angka/boolean jadi JSON.")
  public String JsonEncode(Object value) {
    try {
      return JsonUtil.getJsonRepresentation(value);
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Json", 0, m == null ? "encode gagal" : m);
      return "";
    }
  }

  @SimpleFunction(description = "Decode teks JSON jadi dictionary/list. "
      + "Gagal = \"\" + error (cek LastError(\"Json\"); pola guard: if hasil = \"\" berarti gagal).")
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

  @SimpleFunction(description = "Versi paket Tools (sama untuk semua Tools_* dalam satu .aix: "
      + "wajib SAMA semua per rilis; beda = bug kemasan).")
  public int ToolsVersion() {
    return 20;
  }

  @SimpleFunction(description = "Catatan update paket Tools (dibaca lewat blok, "
      + "karena panel info extension tidak bisa diubah manual).")
  public String GetWhatsNew() {
    return "v20 2026-11-20: RILIS BREAKING (pengguna tunggal = pemilik; "
        + "proyek lama wajib pasang ulang blok yang di-rename). HAPUS: 72 "
        + "cermin per-tipe Tools_Base (diet 99->31 blok); IsAudioPlaying/"
        + "IsVideoPlaying; GetPendingTaskIds; Downloader.GetFileName (pakai "
        + "CallGuessFileName); SaveTagId->RegisterTagId; ReleaseId->"
        + "SetForceReleaseId; ClearTag/ClearVars/ClearTaskExtras->SetClearTag/"
        + "SetClearVars/SetClearTaskExtras; File GetPermission->"
        + "CallRequestPermission; CallSetAlarm/CallSetRepeatingAlarm->"
        + "CallScheduleAlarm/CallScheduleRepeatingAlarm (+CallScheduleDailyAt); "
        + "Sheet SetShowToast/SetShowNotification->CallShowToast/"
        + "CallSetNotification (+event ToastShown/NotificationShown); "
        + "RegisterSheet pecah: RegisterSheet(id,content)+SetSheetStyle (+"
        + "ShowSimpleSheet, CreateNotificationId); Layout GetLayout(id)+"
        + "GetLayoutAs(type,id), param layout->type di 9 blok (+IsLayoutGone, "
        + "LayoutRemoved, SetClearLayoutsIn); Style SetStroke urutan baru "
        + "(comp,color,widthDp,dashed,dashLen,dashGap) + Orientation*/"
        + "Direction* kuning + background KHUSUS lokal/asset (BUKAN URL); "
        + "Compress ListZipContents->GetZipEntries + percent 0-100 + cancel "
        + "hapus ZIP sebagian; File success jadi #1 di 10 event + "
        + "IsCopyOrMoveBusy + CallCopyFile/Move/AppendFile + GetLastModified/"
        + "IsFileReadable; Core Set* diam->lapor ErrorOccurred + "
        + "InvokeDynamic->CallInvokeDynamic + GetDynamicType/16 Type*/"
        + "IsDynamicVisible; Image slider TANPA batas jumlah + cache URL 50MB "
        + "MD5 (+SetClearImageCache) + IsSliderAutoPlay/GetSliderInterval/"
        + "GetImageCount; Task IsTaskPaused/GetTaskElapsed/GetTaskRemaining + "
        + "AlarmsRestored; Audio/Video IsAudioPaused/IsVideoPaused + "
        + "SetForward/Backward*By + volume/loop/speed per-id + 4 designer "
        + "props; Downloader IsDownloadPaused + Get* + CallRetryDownload. "
        + "ALIAS pindah (tetap jalan): tipe file 'vidio'->video. RENCANA v21: "
        + "paket migrasi event besar (CoreClicked dkk); reserves kamera/mic/"
        + "lokasi di File (dicadangkan 2026-11-20) jadi blok nyata. "
        + "v19: Image ShowImage +width/height/fit, slider +paths, +"
        + "SetImageSource; fasad satu pintu. "
        + "v18 2026-11-18: 3 blok duplikat 1-input "
        + "(IsDuplicateFileOrDirectory boolean, GetManyDuplicateFileOr-"
        + "Directory number, GetListDuplicateFileOrDirectory list; keluarga "
        + "counter _N/(N), 1 laci tanpa subfolder; + IsTagIdExists(tag); "
        + "+ SetLayoutVisible/IsLayoutExists/IsLayoutVisible (tipe+id wajib "
        + "cocok); + GetLayout/SetLayoutSize/BackgroundColor/Alignment/"
        + "GetLayoutWidth/Height; create layout +lebar/tinggi/visible/warna; "
        + "Core create Button/Label/TextBox/Image +lebar/tinggi + "
        + "SetDynamicSize/Visible/Text/TextColor/BackgroundColor + "
        + "GetDynamicWidth/Height; Image +SetImageSize/Visible/Fit + "
        + "GetImageWidth/Height +IsImageVisible (BREAKING: proyek lama yang "
        + "pakai create di atas wajib pasang ulang bloknya); + Tools_All_in_One "
        + "(fasad 460 blok 12 tools + ikon, event dicerminkan via bus)). "
        + "v17 2026-11-17: CallDownload 4 pintu (http/data:/content:/file:); "
        + "CallGuessFileName + FileNameGuessed; versi anotasi 17 + catatan "
        + "update per komponen. v16: gate izin otomatis 16 operasi storage; "
        + "IsAllFilesAccessGranted/OpenAllFilesAccessSettings; "
        + "Is/CallEnsure*Permission + 10 Helper Permission; "
        + "MANAGE_EXTERNAL_STORAGE. v15: aturan Is/Call/Set final. "
        + "v14: direktori Android + notifikasi download. v13: hapus scope, "
        + "root /storage/emulated/0.";
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
      + "pesan error terakhir operasi ini (colok OpId/OpJson, kosong bila belum ada). "
      + "Cth: JsonDecode gagal -> LastError(OpJson) berisi sebabnya.")
  public String LastError(String operation) {
    return Shared.lastError(operation);
  }

  @SimpleEvent(description = "SATU-SATUNYA error Tools_Base. component = nama operasi "
      + "(bandingkan dengan blok OpId/OpJson; cth: if component = OpJson berarti error JSON). "
      + "id = id terkait (0 bila umum).")
  public void ErrorOccurred(String component, int id, String message) {
    Shared.fire(this, "ErrorOccurred",
        component, Integer.valueOf(id), message);
  }
}
