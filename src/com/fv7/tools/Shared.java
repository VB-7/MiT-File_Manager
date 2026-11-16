package com.fv7.tools;

import android.content.Context;
import android.os.Environment;

import com.google.appinventor.components.common.FileScope;
import com.google.appinventor.components.runtime.Component;
import com.google.appinventor.components.runtime.EventDispatcher;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Logika bareng semua Tools_* (scope/path, file, bitmap, arsip, slot, error). */
public final class Shared {

  private Shared() {
  }

  // ================= slot single-job =================

  private static final Map<String, Boolean> slots = new HashMap<String, Boolean>();

  public static synchronized boolean claimSlot(String kind) {
    Boolean b = slots.get(kind);
    if (b != null && b.booleanValue()) {
      return false;
    }
    slots.put(kind, Boolean.valueOf(true));
    return true;
  }

  public static synchronized void releaseSlot(String kind) {
    slots.put(kind, Boolean.valueOf(false));
  }

  public static synchronized boolean slotBusy(String kind) {
    Boolean b = slots.get(kind);
    return b != null && b.booleanValue();
  }

  // ================= error =================

  private static final Map<String, String> lastErr =
      new HashMap<String, String>();

  public static void err(Component comp, String component, int id, String message) {
    String m = message == null ? "gagal" : message;
    try {
      String key = component == null ? "" : component;
      synchronized (lastErr) {
        lastErr.put(key, m);
      }
    } catch (Exception e) {
      // abaikan
    }
    try {
      EventDispatcher.dispatchEvent(comp, "ErrorOccurred", component,
          Integer.valueOf(id), m);
    } catch (Exception e) {
      // abaikan
    }
  }

  /** Fallback polling untuk event ErrorOccurred: pesan error terakhir per operasi. */
  public static String lastError(String component) {
    try {
      synchronized (lastErr) {
        String m = lastErr.get(component == null ? "" : component);
        return m == null ? "" : m;
      }
    } catch (Exception e) {
      return "";
    }
  }

  // ================= string/path =================

  public static String clean(String s) {
    return s == null ? "" : s.trim();
  }

  public static String stripLeadingSlash(String p) {
    String s = p;
    while (s.startsWith("/")) {
      s = s.substring(1);
    }
    return s;
  }

  public static boolean isAssetPath(String p) {
    String s = normalizePath(p);
    if (s.startsWith("//")) {
      return true;
    }
    return "Asset".equals(scopeOf(p));
  }

  public static String assetName(String p) {
    return stripLeadingSlash(normalizePath(stripScopePrefix(p)));
  }

  /** Salin satu file asset ke cache (untuk operasi yang butuh java.io.File,
   *  mis. ZIP). Kembali null bila gagal. */
  public static File assetToCache(Context context, String assetPath) {
    try {
      String name = assetName(assetPath);
      File cache = context.getCacheDir();
      if (cache != null && !cache.exists()) {
        cache.mkdirs();
      }
      File dst = new File(cache, "asset_" + sanitize(fileNameOf(name)));
      InputStream in = context.getAssets().open(name);
      try {
        FileOutputStream out = new FileOutputStream(dst);
        try {
          copyStream(in, out);
        } finally {
          try {
            out.close();
          } catch (Exception e) {
            // abaikan
          }
        }
      } finally {
        try {
          in.close();
        } catch (Exception e) {
          // abaikan
        }
      }
      return dst;
    } catch (Exception e) {
      return null;
    }
  }

  public static File externalRoot() {
    try {
      File f = Environment.getExternalStorageDirectory();
      if (f != null) {
        return f;
      }
    } catch (Exception e) {
      // abaikan
    }
    return new File("/storage/emulated/0");
  }

  public static File rootFor(Context context, FileScope scope) {
    if (scope == null) {
      return externalRoot();
    }
    switch (scope) {
      case App:
        try {
          File f = context.getExternalFilesDir(null);
          if (f != null) {
            return f;
          }
        } catch (Exception e) {
          // abaikan
        }
        return new File(externalRoot(), "Android/data/files");
      case Private:
        try {
          return context.getFilesDir();
        } catch (Exception e) {
          return new File(externalRoot(), "Android/data/files");
        }
      case Cache:
        try {
          return context.getCacheDir();
        } catch (Exception e) {
          return new File(externalRoot(), "Android/data/cache");
        }
      case Shared:
      case Legacy:
      default:
        return externalRoot();
    }
  }

  /** Scope bawaan ambient: dipakai semua operasi file bila path tidak bawa
   *  prefix scope sendiri (blok Scope) dan bila komponen tidak pasang scope
   *  eksplisit. Diatur dari Designer (properti DefaultScope) atau blok
   *  Tools_Base SetScope. Default App (selalu bisa tulis, tanpa izin). */
  private static FileScope defaultScope = FileScope.App;

  public static synchronized void setDefaultScope(FileScope s) {
    if (s != null) {
      defaultScope = s;
      defaultScopeName = s.name();
      defaultAbsolute = false;
    }
  }

  public static synchronized FileScope getDefaultScope() {
    if (defaultAbsolute) {
      return null;
    }
    return defaultScope;
  }

  private static final String[] SCOPE_NAMES = {"App", "Private", "Cache",
      "Shared", "Legacy", "Asset"};

  private static String defaultScopeName = "App";
  private static boolean defaultAbsolute = false;

  /** Atur scope bawaan pakai NAMA TEKS ("App"/"Private"/"Cache"/"Shared"/
   *  "Legacy"/"Asset", bebas huruf besar-kecil) atau "" = absolut (berlaku
   *  apa adanya). Nama tak dikenal = diabaikan. Kembali true bila diubah. */
  public static synchronized boolean setDefaultScopeName(String name) {
    String s = name == null ? "" : name.trim();
    if (s.length() == 0) {
      defaultAbsolute = true;
      defaultScopeName = "";
      return true;
    }
    for (int i = 0; i < SCOPE_NAMES.length; i = i + 1) {
      if (s.equalsIgnoreCase(SCOPE_NAMES[i])) {
        defaultScopeName = SCOPE_NAMES[i];
        defaultAbsolute = false;
        defaultScope = scopeByName(SCOPE_NAMES[i]);
        return true;
      }
    }
    return false;
  }

  /** Baca nama scope bawaan ("App"/.../"Asset", atau "" = absolut). */
  public static synchronized String getDefaultScopeName() {
    return defaultAbsolute ? "" : defaultScopeName;
  }

  /** Nama scope dari prefix "NamaScope:path" (blok Scope Tools_Base).
   *  Kembali "" bila tidak ada prefix valid. Case-insensitive. */
  public static String scopeOf(String path) {
    String p = path == null ? "" : path.trim();
    int ci = p.indexOf(':');
    if (ci <= 0) {
      return "";
    }
    String head = p.substring(0, ci);
    for (int i = 0; i < SCOPE_NAMES.length; i = i + 1) {
      if (head.equalsIgnoreCase(SCOPE_NAMES[i])) {
        return SCOPE_NAMES[i];
      }
    }
    return "";
  }

  /** Buang prefix "NamaScope:" bila ada (untuk display/nama file). */
  public static String stripScopePrefix(String path) {
    String p = path == null ? "" : path.trim();
    if (scopeOf(p).length() > 0) {
      return p.substring(p.indexOf(':') + 1);
    }
    return p;
  }

  private static FileScope scopeByName(String name) {
    if ("App".equals(name)) {
      return FileScope.App;
    } else if ("Private".equals(name)) {
      return FileScope.Private;
    } else if ("Cache".equals(name)) {
      return FileScope.Cache;
    } else if ("Shared".equals(name)) {
      return FileScope.Shared;
    } else if ("Legacy".equals(name)) {
      return FileScope.Legacy;
    } else if ("Asset".equals(name)) {
      return FileScope.Asset;
    }
    return null;
  }

  /** Scope TEKS ala draft user: "App"/"Private"/"Cache"/"Shared"/"Legacy"/
   *  "Asset" (bebas huruf besar-kecil) atau "" = path absolut / root kartu
   *  SD. Tak dikenal = defaultScope. Kembali null untuk "" (ditangani
   *  khusus di resolveScope). */
  public static FileScope scopeToScope(String scope) {
    String s = scope == null ? "" : scope.trim();
    if (s.length() == 0) {
      return null;
    }
    if (s.equalsIgnoreCase("App")) {
      return FileScope.App;
    } else if (s.equalsIgnoreCase("Private")) {
      return FileScope.Private;
    } else if (s.equalsIgnoreCase("Cache")) {
      return FileScope.Cache;
    } else if (s.equalsIgnoreCase("Shared")) {
      return FileScope.Shared;
    } else if (s.equalsIgnoreCase("Legacy")) {
      return FileScope.Legacy;
    } else if (s.equalsIgnoreCase("Asset")) {
      return FileScope.Asset;
    }
    if (defaultAbsolute) {
      return null;
    }
    return getDefaultScope();
  }

  /** Scope efektif: prefix "NamaScope:" di path (keluaran helper Scope)
   *  MENANG bila ada; tanpa prefix pakai scope param. */
  public static FileScope effScope(String scope, String path) {
    String tagged = scopeOf(path);
    if (tagged.length() > 0) {
      FileScope f = scopeByName(tagged);
      if (f != null) {
        return f;
      }
    }
    return scopeToScope(scope);
  }

  /** Resolve path pakai scope TEKS (helper Scope ATAU string manual).
   *  Aturan:
   *  - Prefix "NamaScope:" di path (keluaran helper Scope) MENANG bila ada.
   *  - Tanpa prefix: pakai parameter scope ("App"/.../"").
   *  - scope "" = absolut dipakai apa adanya, relatif = root kartu SD
   *    (ala draft point 1). Tak dikenal = scope bawaan. */
  public static File resolveScope(Context context, String scope,
      String path) {
    String raw = path == null ? "" : path.trim();
    String tagged = scopeOf(raw);
    FileScope s;
    String body = raw;
    if (tagged.length() > 0) {
      s = scopeToScope(tagged);
      body = raw.substring(raw.indexOf(':') + 1);
    } else {
      s = scopeToScope(scope);
    }
    if (s == null) {
      String p = normalizePath(body);
      if (p.length() == 0) {
        return externalRoot();
      }
      if (isRemotePath(p)) {
        return null;
      }
      if (p.startsWith("file://")) {
        return new File(stripLeadingSlash(p.substring(7)));
      }
      if (isAssetPath(p)) {
        return null;
      }
      if (isAbsoluteDevicePath(p)) {
        return new File(p);
      }
      return new File(externalRoot(), stripLeadingSlash(p));
    }
    return resolve(context, s, body);
  }

  /** CLEANER path sentral (dipakai SEMUA komponen): rapikan input user
   *  sebelum dipakai. Aturan:
   *  - spasi depan/belakang dibuang, backslash jadi slash;
   *  - slash ganda diruntuhkan (kecuali awalan skema "://" dan "//" asset);
   *  - trailing slash dibuang.
   *  Kembali "" bila input kosong/null. */
  public static String normalizePath(String path) {
    String p = path == null ? "" : path.trim();
    if (p.length() == 0) {
      return "";
    }
    String scheme = "";
    String rest = p;
    if (p.startsWith("//")) {
      scheme = "//";
      rest = p.substring(2);
    } else {
      int ci = p.indexOf("://");
      if (ci > 0) {
        boolean ok = true;
        for (int i = 0; i < ci; i = i + 1) {
          char ch = p.charAt(i);
          boolean letter = (ch >= 'a' && ch <= 'z')
              || (ch >= 'A' && ch <= 'Z');
          boolean tail = (ch >= '0' && ch <= '9') || ch == '+'
              || ch == '-' || ch == '.';
          if (!letter && !(i > 0 && tail)) {
            ok = false;
            break;
          }
        }
        if (ok) {
          scheme = p.substring(0, ci + 3);
          rest = p.substring(ci + 3);
        }
      }
    }
    StringBuilder sb = new StringBuilder();
    boolean lastSlash = false;
    for (int i = 0; i < rest.length(); i = i + 1) {
      char ch = rest.charAt(i);
      if (ch == '\\') {
        ch = '/';
      }
      if (ch == '/') {
        if (!lastSlash) {
          sb.append('/');
        }
        lastSlash = true;
      } else {
        sb.append(ch);
        lastSlash = false;
      }
    }
    String r = sb.toString();
    while (r.length() > 1 && r.endsWith("/")) {
      r = r.substring(0, r.length() - 1);
    }
    return scheme + r;
  }

  /** True bila path adalah URL jaringan / data / content — bukan file lokal,
   *  jadi tidak bisa di-resolve ke File. */
  public static boolean isRemotePath(String p) {
    String s = p == null ? "" : p.trim().toLowerCase();
    return s.startsWith("http://") || s.startsWith("https://")
        || s.startsWith("ftp://") || s.startsWith("ftps://")
        || s.startsWith("data:") || s.startsWith("content://");
  }

  /** True bila path adalah path absolut perangkat (dipakai APA ADANYA,
   *  tanpa ditempeli scope-root). */
  public static boolean isAbsoluteDevicePath(String p) {
    String s = p == null ? "" : p.trim();
    if (s.startsWith("file://")) {
      return true;
    }
    if (s.equals("/sdcard") || s.equals("/storage") || s.equals("/data")) {
      return true;
    }
    String[] roots = {"/storage/", "/sdcard/", "/mnt/", "/data/",
        "/system/", "/vendor/", "/product/", "/cache/", "/odm/", "/oem/"};
    for (int i = 0; i < roots.length; i = i + 1) {
      if (s.startsWith(roots[i])) {
        return true;
      }
    }
    return false;
  }

  /**
   * Aturan path (CLEANER sentral, auto-correct, dipakai semua komponen):
   * - Prefix "NamaScope:path" (dari blok Scope Tools_Base, mis. "App:a.txt")
   *   MENANG atas scope parameter — hanya path itu yang pakai scope itu.
   * - URL (http/https/ftp), data:, content:// = bukan file -> null.
   * - // = asset (baca saja) -> null. file:/// = absolut langsung.
   * - Path absolut perangkat (/storage/..., /sdcard/..., /data/...) dipakai
   *   APA ADANYA untuk SEMUA scope (tidak ditempeli root lagi — anti ganda).
   * - Legacy + diawali / (selain absolut di atas) = root kartu SD.
   * - Legacy tanpa / = privat. Scope lain: relatif ke root scope itu.
   * - Slash ganda/backslash/trailing slash dirapikan otomatis.
   */
  public static File resolve(Context context, FileScope scope, String path) {
    String raw = path == null ? "" : path.trim();
    FileScope eff = scope;
    String tagged = scopeOf(raw);
    String rest = raw;
    if (tagged.length() > 0) {
      FileScope f = scopeByName(tagged);
      if (f != null) {
        eff = f;
      }
      rest = raw.substring(raw.indexOf(':') + 1);
    }
    String p = normalizePath(rest);
    if (p.length() == 0) {
      if (eff == FileScope.Asset) {
        return null;
      }
      return rootFor(context, eff);
    }
    if (isRemotePath(p)) {
      return null;
    }
    if (p.startsWith("file://")) {
      return new File(stripLeadingSlash(p.substring(7)));
    }
    if (isAssetPath(p) || eff == FileScope.Asset) {
      return null;
    }
    if (isAbsoluteDevicePath(p)) {
      return new File(p);
    }
    if (eff == FileScope.Legacy && p.startsWith("/")) {
      return new File(externalRoot(), stripLeadingSlash(p));
    }
    if (eff == FileScope.Legacy && p.length() > 0 && !p.startsWith("/")) {
      try {
        return new File(context.getFilesDir(), p);
      } catch (Exception e) {
        return new File(externalRoot(), p);
      }
    }
    File root = rootFor(context, eff);
    return new File(root, stripLeadingSlash(p));
  }

  public static String sanitize(String name) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < name.length(); i = i + 1) {
      char ch = name.charAt(i);
      if (ch == '\\' || ch == '/' || ch == ':' || ch == '*' || ch == '?'
          || ch == '"' || ch == '<' || ch == '>' || ch == '|' || ch < 32) {
        sb.append('_');
      } else {
        sb.append(ch);
      }
    }
    String s = sb.toString().trim();
    if (s.length() == 0) {
      return "file";
    }
    return s;
  }

  public static File uniqueFile(File dir, String name) {
    File f = new File(dir, name);
    if (!f.exists()) {
      return f;
    }
    int dot = name.lastIndexOf('.');
    String base = dot > 0 ? name.substring(0, dot) : name;
    String ext = dot > 0 ? name.substring(dot) : "";
    int n = 1;
    while (true) {
      File g = new File(dir, base + "_" + n + ext);
      if (!g.exists()) {
        return g;
      }
      n = n + 1;
    }
  }

  public static void copyStream(InputStream in, FileOutputStream out) throws Exception {
    byte[] buf = new byte[32768];
    int n;
    while ((n = in.read(buf)) > 0) {
      out.write(buf, 0, n);
    }
    out.flush();
  }

  public static byte[] readAll(InputStream in) throws Exception {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    byte[] buf = new byte[32768];
    int n;
    while ((n = in.read(buf)) > 0) {
      bos.write(buf, 0, n);
    }
    bos.flush();
    return bos.toByteArray();
  }

  public static boolean deleteRecursive(File f) {
    if (f == null || !f.exists()) {
      return true;
    }
    if (f.isDirectory()) {
      File[] kids = f.listFiles();
      if (kids != null) {
        for (int i = 0; i < kids.length; i = i + 1) {
          deleteRecursive(kids[i]);
        }
      }
    }
    return f.delete();
  }

  public static long dirSize(File f) {
    if (f == null || !f.exists()) {
      return -1L;
    }
    if (!f.isDirectory()) {
      return f.length();
    }
    long total = 0;
    File[] kids = f.listFiles();
    if (kids != null) {
      for (int i = 0; i < kids.length; i = i + 1) {
        long s = dirSize(kids[i]);
        if (s > 0) {
          total = total + s;
        }
      }
    }
    return total;
  }

  // ================= nama file =================

  public static String fileNameOf(String path) {
    try {
      String p = normalizePath(stripScopePrefix(path));
      int s = p.lastIndexOf('/');
      if (s >= 0 && s + 1 < p.length()) {
        return p.substring(s + 1);
      }
      return p;
    } catch (Exception e) {
      return "";
    }
  }

  public static String extensionOf(String name) {
    try {
      String n = normalizePath(name);
      int q = n.indexOf('?');
      if (q >= 0) {
        n = n.substring(0, q);
      }
      int dot = n.lastIndexOf('.');
      if (dot >= 0 && dot + 1 < n.length()) {
        String tail = n.substring(dot + 1);
        int sl = tail.indexOf('/');
        if (sl >= 0) {
          return "";
        }
        return tail.toLowerCase();
      }
      return "";
    } catch (Exception e) {
      return "";
    }
  }

  public static String parentOf(String path) {
    try {
      String p = normalizePath(stripScopePrefix(path));
      while (p.endsWith("/") && p.length() > 1) {
        p = p.substring(0, p.length() - 1);
      }
      int s = p.lastIndexOf('/');
      if (s > 0) {
        return p.substring(0, s);
      }
      return "";
    } catch (Exception e) {
      return "";
    }
  }

  // ================= mime =================

  private static final Map<String, String> MIME_EXT = new HashMap<String, String>();
  static {
    MIME_EXT.put("image/jpeg", "jpg");
    MIME_EXT.put("image/png", "png");
    MIME_EXT.put("image/webp", "webp");
    MIME_EXT.put("image/gif", "gif");
    MIME_EXT.put("video/mp4", "mp4");
    MIME_EXT.put("video/3gpp", "3gp");
    MIME_EXT.put("audio/mpeg", "mp3");
    MIME_EXT.put("audio/x-wav", "wav");
    MIME_EXT.put("application/pdf", "pdf");
    MIME_EXT.put("application/zip", "zip");
    MIME_EXT.put("application/json", "json");
    MIME_EXT.put("text/plain", "txt");
    MIME_EXT.put("text/html", "html");
    MIME_EXT.put("text/csv", "csv");
    MIME_EXT.put("application/vnd.android.package-archive", "apk");
  }

  public static String mimeFor(String fileName) {
    try {
      String ext = extensionOf(fileName);
      if (ext.equals("jpg") || ext.equals("jpeg")) {
        return "image/jpeg";
      } else if (ext.equals("png")) {
        return "image/png";
      } else if (ext.equals("webp")) {
        return "image/webp";
      } else if (ext.equals("gif")) {
        return "image/gif";
      } else if (ext.equals("mp4") || ext.equals("3gp")) {
        return "video/mp4";
      } else if (ext.equals("mp3")) {
        return "audio/mpeg";
      } else if (ext.equals("wav")) {
        return "audio/x-wav";
      } else if (ext.equals("ogg")) {
        return "audio/ogg";
      } else if (ext.equals("pdf")) {
        return "application/pdf";
      } else if (ext.equals("zip")) {
        return "application/zip";
      } else if (ext.equals("apk")) {
        return "application/vnd.android.package-archive";
      } else if (ext.equals("json")) {
        return "application/json";
      } else if (ext.equals("txt")) {
        return "text/plain";
      } else if (ext.equals("html") || ext.equals("htm")) {
        return "text/html";
      } else if (ext.equals("csv")) {
        return "text/csv";
      }
      return "application/octet-stream";
    } catch (Exception e) {
      return "application/octet-stream";
    }
  }

  public static String extForMime(String mime) {
    if (mime == null) {
      return "";
    }
    String m = mime.trim().toLowerCase();
    int s = m.indexOf(';');
    if (s >= 0) {
      m = m.substring(0, s).trim();
    }
    String e = MIME_EXT.get(m);
    return e == null ? "" : e;
  }

  // ================= kategori file =================

  public static final String[] IMAGE_EXTS = {"bmp", "gif", "heic", "heif",
      "ico", "jpg", "jpeg", "png", "svg", "tif", "tiff", "wbmp", "webp"};
  public static final String[] AUDIO_EXTS = {"aac", "amr", "flac", "m4a",
      "mid", "midi", "mp3", "oga", "ogg", "opus", "wav", "weba"};
  public static final String[] VIDEO_EXTS = {"avi", "flv", "m2ts", "mkv",
      "mov", "mp4", "mpeg", "mpg", "ts", "webm", "wmv", "3gp"};
  public static final String[] TEXT_EXTS = {"css", "csv", "htm", "html",
      "ini", "js", "json", "log", "md", "txt", "xml", "yaml", "yml"};
  public static final String[] ARCHIVE_EXTS = {"7z", "bz2", "gz", "rar",
      "tar", "xz", "zip"};
  public static final String[] DOC_EXTS = {"doc", "docx", "epub", "odp",
      "ods", "odt", "pdf", "ppt", "pptx", "rtf", "xls", "xlsx"};

  private static final Map<String, String> EXT_TO_CAT =
      new HashMap<String, String>();
  static {
    putExts(IMAGE_EXTS, "image");
    putExts(AUDIO_EXTS, "audio");
    putExts(VIDEO_EXTS, "video");
    putExts(TEXT_EXTS, "text");
    putExts(ARCHIVE_EXTS, "archive");
    putExts(DOC_EXTS, "document");
    EXT_TO_CAT.put("apk", "apk");
  }

  private static void putExts(String[] exts, String cat) {
    for (int i = 0; i < exts.length; i = i + 1) {
      EXT_TO_CAT.put(exts[i], cat);
    }
  }

  /** Kategori file dari nama/path: image/audio/video/text/archive/document/
   *  apk/unknown. Berdasarkan ekstensi (tak peduli huruf besar). */
  public static String fileCategory(String fileName) {
    String ext = extensionOf(fileName == null ? "" : fileName);
    if (ext.length() == 0) {
      return "unknown";
    }
    String c = EXT_TO_CAT.get(ext);
    return c == null ? "unknown" : c;
  }

  /** Normalisasi input tipe ke kanonik (terima alias Indonesia/Inggris).
   *  Kembali "" bila tidak dikenal. */
  public static String normCategory(String type) {
    String t = type == null ? "" : type.trim().toLowerCase();
    if (t.equals("image") || t.equals("images") || t.equals("gambar")
        || t.equals("foto") || t.equals("picture")) {
      return "image";
    } else if (t.equals("audio") || t.equals("audios") || t.equals("suara")
        || t.equals("musik") || t.equals("lagu") || t.equals("sound")) {
      return "audio";
    } else if (t.equals("video") || t.equals("videos") || t.equals("vidio")
        || t.equals("film")) {
      return "video";
    } else if (t.equals("text") || t.equals("teks")) {
      return "text";
    } else if (t.equals("archive") || t.equals("archives")
        || t.equals("arsip") || t.equals("kompres")) {
      return "archive";
    } else if (t.equals("document") || t.equals("documents")
        || t.equals("dokumen")) {
      return "document";
    } else if (t.equals("apk")) {
      return "apk";
    } else if (t.equals("unknown") || t.equals("lain")
        || t.equals("lainnya") || t.equals("other")) {
      return "unknown";
    }
    return "";
  }

  /** Daftar ekstensi (tanpa titik, urut) untuk satu kategori kanonik. */
  public static List<String> extensionsFor(String category) {
    ArrayList<String> out = new ArrayList<String>();
    String[] src = null;
    if ("image".equals(category)) {
      src = IMAGE_EXTS;
    } else if ("audio".equals(category)) {
      src = AUDIO_EXTS;
    } else if ("video".equals(category)) {
      src = VIDEO_EXTS;
    } else if ("text".equals(category)) {
      src = TEXT_EXTS;
    } else if ("archive".equals(category)) {
      src = ARCHIVE_EXTS;
    } else if ("document".equals(category)) {
      src = DOC_EXTS;
    } else if ("apk".equals(category)) {
      src = new String[] {"apk"};
    }
    if (src != null) {
      for (int i = 0; i < src.length; i = i + 1) {
        out.add(src[i]);
      }
    }
    return out;
  }

  // ================= format bantu =================

  public static String formatTime(int millis) {
    try {
      int total = millis / 1000;
      if (total < 0) {
        total = 0;
      }
      int sec = total % 60;
      int min = (total / 60) % 60;
      int hr = total / 3600;
      String ss = sec < 10 ? "0" + sec : String.valueOf(sec);
      if (hr > 0) {
        String mm = min < 10 ? "0" + min : String.valueOf(min);
        return hr + ":" + mm + ":" + ss;
      }
      return min + ":" + ss;
    } catch (Exception e) {
      return "0:00";
    }
  }

  public static String convertBytes(long bytes) {
    if (bytes < 0) {
      return "?";
    }
    if (bytes < 1024) {
      return bytes + " B";
    }
    double v = bytes / 1024.0;
    String unit = "KB";
    if (v >= 1024.0) {
      v = v / 1024.0;
      unit = "MB";
      if (v >= 1024.0) {
        v = v / 1024.0;
        unit = "GB";
      }
    }
    long r = Math.round(v * 100.0);
    String dec = (r % 100) < 10 ? "0" + (r % 100) : String.valueOf(r % 100);
    return (r / 100) + "." + dec + " " + unit;
  }

  // ================= deteksi arsip (magic bytes, tanpa library) =================

  /**
   * Baca magic bytes sebuah file. Kembali: zip, rar, 7z, gz, tar, bz2, xz,
   * apk, pdf, png, jpg, gif, mp3, mp4, atau unknown.
   */
  public static String detectKind(File f) {
    FileInputStream in = null;
    try {
      if (f == null || !f.exists() || f.isDirectory()) {
        return "unknown";
      }
      in = new FileInputStream(f);
      byte[] h = new byte[12];
      int n = 0;
      while (n < 12) {
        int r = in.read(h, n, 12 - n);
        if (r < 0) {
          break;
        }
        n = n + r;
      }
      if (n >= 4 && h[0] == 0x50 && h[1] == 0x4B && h[2] == 0x03 && h[3] == 0x04) {
        return "zip";
      }
      if (n >= 7 && h[0] == 0x52 && h[1] == 0x61 && h[2] == 0x72 && h[3] == 0x21
          && h[4] == 0x1A && h[5] == 0x07
          && (h[6] == 0x00 || h[6] == 0x01)) {
        return "rar";
      }
      if (n >= 6 && h[0] == 0x37 && h[1] == 0x7A && h[2] == (byte) 0xBC
          && h[3] == (byte) 0xAF && h[4] == 0x27 && h[5] == 0x1C) {
        return "7z";
      }
      if (n >= 3 && h[0] == 0x1F && h[1] == (byte) 0x8B && h[2] == 0x08) {
        return "gz";
      }
      if (n >= 3 && h[0] == 0x42 && h[1] == 0x5A && h[2] == 0x68) {
        return "bz2";
      }
      if (n >= 6 && h[0] == (byte) 0xFD && h[1] == 0x37 && h[2] == 0x7A
          && h[3] == 0x58 && h[4] == 0x5A && h[5] == 0x00) {
        return "xz";
      }
      if (n >= 8 && h[0] == (byte) 0x89 && h[1] == 0x50 && h[2] == 0x4E
          && h[3] == 0x47) {
        return "png";
      }
      if (n >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8
          && (h[2] & 0xFF) == 0xFF) {
        return "jpg";
      }
      if (n >= 6 && h[0] == 0x47 && h[1] == 0x49 && h[2] == 0x46) {
        return "gif";
      }
      if (n >= 5 && h[0] == 0x25 && h[1] == 0x50 && h[2] == 0x44
          && h[3] == 0x46 && h[4] == 0x2D) {
        return "pdf";
      }
      if (n >= 3 && h[0] == 0x49 && h[1] == 0x44 && h[2] == 0x33) {
        return "mp3";
      }
      if (n >= 12 && h[4] == 0x66 && h[5] == 0x74 && h[6] == 0x79
          && h[7] == 0x70) {
        return "mp4";
      }
      return "unknown";
    } catch (Exception e) {
      return "unknown";
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (Exception e) {
          // abaikan
        }
      }
    }
  }

  /** Cek entri tar (ustar) tanpa library: baca blok 512, cocokkan "ustar". */
  public static boolean looksLikeTar(File f) {
    FileInputStream in = null;
    try {
      if (f == null || !f.exists() || f.isDirectory()) {
        return false;
      }
      in = new FileInputStream(f);
      byte[] b = new byte[512];
      int n = 0;
      while (n < 512) {
        int r = in.read(b, n, 512 - n);
        if (r < 0) {
          break;
        }
        n = n + r;
      }
      if (n < 512) {
        return false;
      }
      return b[257] == 0x75 && b[258] == 0x73 && b[259] == 0x74
          && b[260] == 0x61 && b[261] == 0x72;
    } catch (Exception e) {
      return false;
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (Exception e) {
          // abaikan
        }
      }
    }
  }

  public static List<String> sortedDirs(File dir) {
    ArrayList<String> out = new ArrayList<String>();
    try {
      File[] fs = dir.listFiles();
      if (fs != null) {
        for (int i = 0; i < fs.length; i = i + 1) {
          if (fs[i].isDirectory()) {
            out.add(fs[i].getAbsolutePath());
          }
        }
      }
      Collections.sort(out);
    } catch (Exception e) {
      // abaikan
    }
    return out;
  }
}
