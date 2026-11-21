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
    version = 23,
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

  @SimpleFunction(description = "SET: lepas paksa id yang macet (ruang UGD). "
      + "Resep urut: Cancel/Stop/Remove pemiliknya DULU, SetForceReleaseId "
      + "TERAKHIR. Id macet bila cancel tidak melepas / crash di tengah. "
      + "Tanpa feedback.")
  public void SetForceReleaseId(int id) {
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

  @SimpleFunction(description = "SET: hapus SELURUH isi satu tag. Tanpa feedback.")
  public void SetClearTag(String tag) {
    TagIds.clear(tag);
  }

  @SimpleFunction(description = "Semua nama tag yang masih punya isi (URUT DIBUAT).")
  public List<String> GetAllTags() {
    return TagIds.allTags();
  }


  // ================= IZIN GENERIK (manual + luar) =================

  private void fireOnUi(final Runnable r) 