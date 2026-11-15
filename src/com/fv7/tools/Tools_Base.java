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
    description = "Fondasi Tools: id pool lintas extension + util dasar "
        + "(JSON, URI, waktu, byte, mime). Dipakai bareng Tools_File/Video/Audio/"
        + "Image/Compress/Downloader.",
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

  // ================= ID POOL =================

  @SimpleFunction(description = "Id yang disarankan: angka terkecil >= 1 yang belum dipakai "
      + "siapa pun. Cocok untuk Download / ShowImage.")
  public int SuggestId() {
    return Ids.suggest();
  }

  @SimpleFunction(description = "True bila id sedang dipakai operasi lain.")
  public boolean IsIdBusy(int id) {
    return Ids.isBusy(id);
  }

  @SimpleFunction(description = "Daftar id yang sedang dipakai (kosong = tidak ada).")
  public List<Integer> BusyIds() {
    return Ids.busyList();
  }

  @SimpleFunction(description = "Lepas paksa id yang macet. Jangan dipakai saat operasinya "
      + "masih jalan (pakai Cancel dulu).")
  public void ReleaseId(int id) {
    Ids.release(id);
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
    return 3;
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

  @SimpleEvent(description = "SATU-SATUNYA error Tools_Base. component = nama operasi "
      + "(bandingkan dengan blok Op...).")
  public void ErrorOccurred(String component, int id, String message) {
    EventDispatcher.dispatchEvent(this, "ErrorOccurred",
        component, Integer.valueOf(id), message);
  }
}
