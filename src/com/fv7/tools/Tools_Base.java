package com.fv7.tools;

import android.app.Activity;
import android.content.Context;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.PermissionResultHandler;
import com.google.appinventor.components.runtime.util.YailList;
import com.google.appinventor.components.runtime.util.JsonUtil;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

@DesignerComponent(
    version = 25,
    description = "Fondasi Tools: id pool global + util Tag untuk id komponen "
        + "LUAR (auto-konversi teks angka: \"5\" = 5) + util dasar (JSON, URI, "
        + "waktu, byte, mime). Butuh id? CreateUniqueId (global, reservasi "
        + "otomatis). Id komponen luar? RegisterTagId (catat) / CreateTagId "
        + "(buatkan). Angka cepat tanpa booking? SuggestId (tanpa reservasi, "
        + "JANGAN dipakai dua tempat). Id per tipe (Download/Image/dll) ada "
        + "di komponen pemiliknya masing-masing, bukan di sini. Konvensi "
        + "awalan: Is = boolean (colok ke if), Call = async + hasil lewat "
        + "event, Set = tanpa feedback, Get = nilai balik sinkron. Izin generik "
        + "(manual + luar): CallRequestPermission(s) + IsPermissionGranted + "
        + "GetGranted/DeniedPermissions + 10 Pilihan izin + GotPermission.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    androidMinSdk = 21,
    iconName = "aiwebres/tools_base.jpg",
    helpUrl = "https://github.com/VB-7/MiT-File_Manager")
@SimpleObject(external = true)
@UsesPermissions({"android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE",
    "android.permission.MANAGE_EXTERNAL_STORAGE",
    "android.permission.CAMERA",
    "android.permission.RECORD_AUDIO",
    "android.permission.POST_NOTIFICATIONS",
    "android.permission.READ_MEDIA_IMAGES",
    "android.permission.READ_MEDIA_VIDEO",
    "android.permission.READ_MEDIA_AUDIO",
    "android.permission.ACCESS_FINE_LOCATION",
    "android.permission.ACCESS_COARSE_LOCATION"})
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

  @SimpleProperty(description = "SET: lepas paksa id yang macet (ruang UGD). "
      + "Resep urut: Cancel/Stop/Remove pemiliknya DULU, SetForceReleaseId "
      + "TERAKHIR. Id macet bila cancel tidak melepas / crash di tengah. "
      + "Tanpa feedback.")
  public void ForceReleaseId(int id) {
    Ids.release(id);
  }

  // ================= ID TAG (util komponen LUAR) =================

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

  @SimpleProperty(description = "SET: hapus SELURUH isi satu tag. Tanpa feedback.")
  public void ClearTag(String tag) {
    TagIds.clear(tag);
  }

  @SimpleFunction(description = "Semua nama tag yang masih punya isi (URUT DIBUAT).")
  public List<String> GetAllTags() {
    return TagIds.allTags();
  }


  // ================= IZIN GENERIK (manual + luar) =================

  private void fireOnUi(final Runnable r) {
    try {
      form.runOnUiThread(r);
    } catch (Exception e) {
      try {
        activity.runOnUiThread(r);
      } catch (Exception e2) {
        // abaikan
      }
    }
  }

  private boolean grantedNow(String permission) {
    String p = Shared.clean(permission);
    if (p.length() == 0) {
      return false;
    }
    try {
      int sdk = android.os.Build.VERSION.SDK_INT;
      String eff = Shared.effectivePermission(p, sdk);
      if (eff == null) {
        return true;
      }
      if (Shared.isStoragePerm(eff) && sdk >= 30) {
        return android.os.Environment.isExternalStorageManager();
      }
      if (sdk >= 23) {
        return activity.checkSelfPermission(eff)
            == android.content.pm.PackageManager.PERMISSION_GRANTED;
      }
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  private void requestOne(final String p) {
    if (p.length() == 0) {
      Shared.err(this, "Permission", 0, "izin kosong");
      fireOnUi(new Runnable() {
        public void run() {
          GotPermission(false, "", "izin kosong", false);
        }
      });
      return;
    }
    boolean granted = false;
    try {
      granted = grantedNow(p);
    } catch (Exception e) {
      granted = false;
    }
    if (granted) {
      fireOnUi(new Runnable() {
        public void run() {
          GotPermission(true, p, "", false);
        }
      });
      return;
    }
    int sdk = android.os.Build.VERSION.SDK_INT;
    final String eff = Shared.effectivePermission(p, sdk);
    if (eff == null) {
      fireOnUi(new Runnable() {
        public void run() {
          GotPermission(true, p, "tak perlu diminta di OS ini", false);
        }
      });
      return;
    }
    if (Shared.isStoragePerm(eff) && sdk >= 30) {
      Shared.err(this, "Permission", 0, "Android 11+: butuh "
          + "akses semua file - membuka Settings");
      Shared.openAllFilesSettings(activity);
      fireOnUi(new Runnable() {
        public void run() {
          GotPermission(false, p, "Android 11+: aktifkan akses semua file "
              + "di Setelan aplikasi", false);
        }
      });
      return;
    }
    try {
      form.askPermission(eff, new PermissionResultHandler() {
        public void HandlePermissionResponse(String perm, boolean granted) {
          final boolean g = granted;
          fireOnUi(new Runnable() {
            public void run() {
              if (g) {
                GotPermission(true, p, "", false);
                return;
              }
              boolean perm2 = false;
              try {
                if (android.os.Build.VERSION.SDK_INT >= 23) {
                  perm2 = !activity.shouldShowRequestPermissionRationale(eff);
                }
              } catch (Exception e) {
                perm2 = false;
              }
              if (perm2) {
                Shared.err(Tools_Base.this, "Permission", 0,
                    "izin ditolak permanen: " + p);
                GotPermission(false, p, "izin ditolak permanen: " + p
                    + " - aktifkan manual di Setelan aplikasi", true);
              } else {
                Shared.err(Tools_Base.this, "Permission", 0,
                    "izin ditolak user: " + p);
                GotPermission(false, p, "izin ditolak user: " + p, false);
              }
            }
          });
        }
      });
    } catch (Exception e) {
      String m = e.getMessage();
      final String msg = m == null ? "minta izin gagal" : m;
      Shared.err(this, "Permission", 0, msg);
      fireOnUi(new Runnable() {
        public void run() {
          GotPermission(false, p, msg, false);
        }
      });
    }
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
    requestOne(Shared.clean(permission));
  }

  @SimpleFunction(description = "CALL: minta BANYAK izin sekaligus (list; contoh "
      + "langsung colok list permission dari event luar). GotPermission "
      + "ditembak PER IZIN (satu bentuk hasil). Entri kosong dilewati.")
  public void CallRequestPermissions(final YailList permissions) {
    if (permissions == null) {
      return;
    }
    Object[] arr = permissions.toArray();
    for (int i = 0; i < arr.length; i = i + 1) {
      Object o = arr[i];
      String p = Shared.clean(o == null ? "" : String.valueOf(o));
      if (p.length() == 0) {
        continue;
      }
      requestOne(p);
    }
  }

  @SimpleFunction(description = "True bila 1 izin ini sudah granted (teks ATAU "
      + "blok Permission...). Storage Android 11+ = cek akses semua file. "
      + "Android 6 ke bawah selalu true (izin sejak install).")
  public boolean IsPermissionGranted(String permission) {
    return grantedNow(permission);
  }

  @SimpleFunction(description = "Daftar izin dari list yang SUDAH granted "
      + "(untuk global/var/alur). Kosong = tak ada yang beres.")
  public List<String> GetGrantedPermissions(YailList permissions) {
    ArrayList<String> out = new ArrayList<String>();
    if (permissions == null) {
      return out;
    }
    Object[] arr = permissions.toArray();
    for (int i = 0; i < arr.length; i = i + 1) {
      Object o = arr[i];
      String p = Shared.clean(o == null ? "" : String.valueOf(o));
      if (p.length() == 0) {
        continue;
      }
      if (grantedNow(p)) {
        out.add(p);
      }
    }
    return out;
  }

  @SimpleFunction(description = "Daftar izin dari list yang BELUM granted / "
      + "ditolak (untuk global/var/alur). Kosong = semua beres.")
  public List<String> GetDeniedPermissions(YailList permissions) {
    ArrayList<String> out = new ArrayList<String>();
    if (permissions == null) {
      return out;
    }
    Object[] arr = permissions.toArray();
    for (int i = 0; i < arr.length; i = i + 1) {
      Object o = arr[i];
      String p = Shared.clean(o == null ? "" : String.valueOf(o));
      if (p.length() == 0) {
        continue;
      }
      if (!grantedNow(p)) {
        out.add(p);
      }
    }
    return out;
  }

  @SimpleEvent(description = "Hasil CallRequestPermission(s) Base (HANYA untuk "
      + "izin yang diminta via Base - tidak menembak untuk izin tools lain): "
      + "success + izin + pesan + permanentlyDenied. success=true = beres "
      + "(lanjut). false + permanentlyDenied=false = user menolak (boleh "
      + "tanya lagi) ATAU kasus sistem (storage 11+ / input salah - baca "
      + "message). false + true = permanen (aktifkan manual di Setelan).")
  public void GotPermission(boolean success, String permission,
      String message, boolean permanentlyDenied) {
    Shared.fire(this, "GotPermission", Boolean.valueOf(success), permission,
        message, Boolean.valueOf(permanentlyDenied));
  }

  @SimpleProperty(description = "Pilihan read-only: izin kamera (CADANGAN - belum ada "
      + "bloknya). Colok ke CallRequestPermission(s) / IsPermissionGranted "
      + "di sini (tanpa ketik string).",
      category = PropertyCategory.BEHAVIOR)
  public String PermissionCamera() {
    return "android.permission.CAMERA";
  }

  @SimpleProperty(description = "Pilihan read-only: izin mikrofon, rekam "
      + "suara (CADANGAN - belum ada bloknya). Colok ke "
      + "CallRequestPermission(s) / IsPermissionGranted di sini.",
      category = PropertyCategory.BEHAVIOR)
  public String PermissionMicrophone() {
    return "android.permission.RECORD_AUDIO";
  }

  @SimpleProperty(description = "Pilihan read-only: izin baca storage. "
      + "Colok ke CallRequestPermission(s) / IsPermissionGranted di sini.",
      category = PropertyCategory.BEHAVIOR)
  public String PermissionReadStorage() {
    return "android.permission.READ_EXTERNAL_STORAGE";
  }

  @SimpleProperty(description = "Pilihan read-only: izin tulis storage. "
      + "Colok ke CallRequestPermission(s) / IsPermissionGranted di sini.",
      category = PropertyCategory.BEHAVIOR)
  public String PermissionWriteStorage() {
    return "android.permission.WRITE_EXTERNAL_STORAGE";
  }

  @SimpleProperty(description = "Pilihan read-only: izin notifikasi "
      + "(Android 13+). Colok ke CallRequestPermission(s) / "
      + "IsPermissionGranted di sini.",
      category = PropertyCategory.BEHAVIOR)
  public String PermissionNotifications() {
    return "android.permission.POST_NOTIFICATIONS";
  }

  @SimpleProperty(description = "Pilihan read-only: izin baca gambar "
      + "(Android 13+). Colok ke CallRequestPermission(s) / "
      + "IsPermissionGranted di sini.",
      category = PropertyCategory.BEHAVIOR)
  public String PermissionReadImages() {
    return "android.permission.READ_MEDIA_IMAGES";
  }

  @SimpleProperty(description = "Pilihan read-only: izin baca video "
      + "(Android 13+). Colok ke CallRequestPermission(s) / "
      + "IsPermissionGranted di sini.",
      category = PropertyCategory.BEHAVIOR)
  public String PermissionReadVideo() {
    return "android.permission.READ_MEDIA_VIDEO";
  }

  @SimpleProperty(description = "Pilihan read-only: izin baca audio "
      + "(Android 13+). Colok ke CallRequestPermission(s) / "
      + "IsPermissionGranted di sini.",
      category = PropertyCategory.BEHAVIOR)
  public String PermissionReadAudio() {
    return "android.permission.READ_MEDIA_AUDIO";
  }

  @SimpleProperty(description = "Pilihan read-only: izin lokasi akurat (CADANGAN - "
      + "belum ada bloknya). Colok ke CallRequestPermission(s) / "
      + "IsPermissionGranted di sini.",
      category = PropertyCategory.BEHAVIOR)
  public String PermissionLocationFine() {
    return "android.permission.ACCESS_FINE_LOCATION";
  }

  @SimpleProperty(description = "Pilihan read-only: izin lokasi kasar (CADANGAN - "
      + "belum ada bloknya). Colok ke CallRequestPermission(s) / "
      + "IsPermissionGranted di sini.",
      category = PropertyCategory.BEHAVIOR)
  public String PermissionLocationCoarse() {
    return "android.permission.ACCESS_COARSE_LOCATION";
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
    return 23;
  }

  @SimpleFunction(description = "Catatan update paket Tools (dibaca lewat blok, "
      + "karena panel info extension tidak bisa diubah manual).")
  public String GetWhatsNew() {
    return "v23 2026-11-21: REDESAIN IZIN (breaking, blok lama merah): Base = hub "
        + "generik (CallRequestPermission(s) + IsPermissionGranted + "
        + "GetGranted/DeniedPermissions + 10 Pilihan izin + GotPermission); File = "
        + "CallSetStoragePermission; Sheet = CallSetNotificationPermission; Task = "
        + "CallSetExactAlarmPermission; Call lama + helper File dihapus; GotPermission "
        + "4-param (permanentlyDenied = user vs permanen); operasi File/Compress/"
        + "Downloader/Image/Style gate mandiri. "
        + "v22 2026-11-20: File +GetFileHash(path, algorithm) hash isi "
        + "(MD5/SHA-1/SHA-224/SHA-256/SHA-384/SHA-512) + Helper merah Type "
        + "(dropdown; soket terima merah/teks manual) + IsDuplicateFileOrDirectory "
        + "jadi cek nama PERSIS (counter _N/(N) hanya untuk hitungan Many) + "
        + "sapuan kata: Helper = blok merah. "
        + "v21 2026-11-20: BREAKING nilai ukuran: SEMUA soket dp->pixel "
        + "mentah (Style 9 blok + Sheet + slider; angka dp lama kali ~2-3). "
        + "Image: slider setara ListView (create +width/height/fit; SetSliderSize/"
        + "Fit/Spacing/Selected/Visible; Get Width/Height/Fit/Spacing/Visible/"
        + "Selected/Image/Failed/Preload; lazy SetSliderPreload OTOMATIS + ingat "
        + "posisi SetSliderRememberPosition; IsImageLoading; SliderCreated pakai "
        + "sliderId; batas Exif dicatat; cache URL 80MB). Downloader "
        + "+GetDownloadAttempts. Sheet +GetSheetHeight + beda daftar/Aktif. File "
        + "GetFileName rujuk CallGuessFileName. Fasad LastError gabungan 12 "
        + "pemilik. RENCANA: reserves kamera/mic/lokasi jadi blok nyata; 6 uji "
        + "HP (pemilik). "
        + "v20 2026-11-20: RILIS BREAKING (pengguna tunggal = pemilik; "
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
        + "Direction* + background KHUSUS lokal/asset (BUKAN URL); "
        + "Compress ListZipContents->GetZipEntries + percent 0-100 + cancel "
        + "hapus ZIP sebagian; File success jadi #1 di 10 event + "
        + "IsCopyOrMoveBusy + CopyFile/MoveFile + CallAppendFile + GetLastModified/"
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
        + "counter _N/(N), 1 folder tanpa subfolder; + IsTagIdExists(tag); "
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
        + "Is/CallEnsure*Permission + 10 blok Permission; "
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

  @SimpleProperty(description = "Nama operasi Permission (untuk if di "
      + "ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpPermission() {
    return "Permission";
  }

  @SimpleFunction(description = "Fallback polling untuk event ErrorOccurred: "
      + "pesan error terakhir operasi ini (colok OpId/OpJson, kosong bila belum ada). "
      + "Cth: JsonDecode gagal -> LastError(OpJson) berisi sebabnya.")
  public String LastError(String operation) {
    return Shared.lastError(operation);
  }

  @SimpleEvent(description = "SATU-SATUNYA error Tools_Base. component = nama operasi "
      + "(bandingkan dengan blok OpId/OpJson/OpPermission; cth: if component = OpJson berarti error JSON). "
      + "id = id terkait (0 bila umum).")
  public void ErrorOccurred(String component, int id, String message) {
    Shared.fire(this, "ErrorOccurred",
        component, Integer.valueOf(id), message);
  }
}
