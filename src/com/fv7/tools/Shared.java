package com.fv7.tools;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;

import com.google.appinventor.components.runtime.Component;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.Form;
import com.google.appinventor.components.runtime.PermissionResultHandler;

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

/** Logika bareng semua Tools_* (path, file, bitmap, arsip, slot, error). */
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
      fire(comp, "ErrorOccurred", component,
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

  // ================= cermin All-in-One =================

  /** Instance Tools_All_in_One yang menerima salinan semua event (boleh null). */
  private static volatile Component allInOneSink = null;

  /** Daftarkan fasad All-in-One sebagai penerima salinan event. Terakhir menang. */
  public static void registerAllInOne(Component c) {
    allInOneSink = c;
  }

  /** Kirim event ke komponen asal + salinannya ke All-in-One (bila ada & beda). */
  public static boolean fire(Component origin, String event, Object... args) {
    boolean r = false;
    try {
      r = EventDispatcher.dispatchEvent(origin, event, args);
    } catch (Exception e) {
      // abaikan
    }
    try {
      Component s = allInOneSink;
      if (s != null && s != origin) {
        EventDispatcher.dispatchEvent(s, event, args);
      }
    } catch (Exception e) {
      // abaikan
    }
    return r;
  }

  // ================= izin sadar-OS =================

  /** Izin tulis storage (dipakai gate otomatis + blok manual). */
  public static final String PERM_WRITE_STORAGE =
      "android.permission.WRITE_EXTERNAL_STORAGE";
  /** Izin baca storage (dipakai gate operasi baca murni). */
  public static final String PERM_READ_STORAGE =
      "android.permission.READ_EXTERNAL_STORAGE";

  /** Petakan izin ke bentuk EFEKTIF di OS ini. null = tak perlu diminta
   *  (install-time / belum ada di OS ini) -> anggap granted. Berantai
   *  (mis. visual-selected -> media-images -> storage) sampai stabil. */
  public static String effectivePermission(String permission, int sdk) {
    String p = permission == null ? "" : permission.trim();
    if (p.length() == 0) {
      return null;
    }
    for (int i = 0; i < 4 && p != null; i++) {
      String q = effectiveOnce(p, sdk);
      if (q == null || q.equals(p)) {
        return q;
      }
      p = q;
    }
    return p;
  }

  private static String effectiveOnce(String p, int sdk) {
    if (p.equals("android.permission.POST_NOTIFICATIONS")) {
      return sdk >= 33 ? p : null;
    }
    if (p.equals("android.permission.READ_MEDIA_IMAGES")
        || p.equals("android.permission.READ_MEDIA_VIDEO")
        || p.equals("android.permission.READ_MEDIA_AUDIO")) {
      return sdk >= 33 ? p : PERM_READ_STORAGE;
    }
    if (p.equals("android.permission.READ_MEDIA_VISUAL_USER_SELECTED")) {
      return sdk >= 34 ? p : "android.permission.READ_MEDIA_IMAGES";
    }
    if (p.equals("android.permission.NEARBY_WIFI_DEVICES")) {
      return sdk >= 33 ? p : "android.permission.ACCESS_FINE_LOCATION";
    }
    if (p.equals("android.permission.BLUETOOTH_SCAN")
        || p.equals("android.permission.BLUETOOTH_CONNECT")
        || p.equals("android.permission.BLUETOOTH_ADVERTISE")) {
      return sdk >= 31 ? p : null;
    }
    return p;
  }

  /** True bila izin ini keluarga storage (cara ask-nya khusus per OS). */
  public static boolean isStoragePerm(String p) {
    return PERM_WRITE_STORAGE.equals(p) || PERM_READ_STORAGE.equals(p)
        || "android.permission.MANAGE_EXTERNAL_STORAGE".equals(p);
  }

  private static final Object ASK_LOCK = new Object();
  private static java.util.concurrent.CountDownLatch askLatch = null;
  private static boolean askResult = false;
  private static long lastSettingsOpen = 0;

  /** Buka layar Settings 'Akses semua file' (throttle 10 dtk agar batch
   *  tidak membuka berkali-kali). Aman di semua API. */
  public static void openAllFilesSettings(final Activity activity) {
    if (activity == null) {
      return;
    }
    synchronized (ASK_LOCK) {
      long now = System.currentTimeMillis();
      if (now - lastSettingsOpen < 10000) {
        return;
      }
      lastSettingsOpen = now;
    }
    try {
      if (android.os.Build.VERSION.SDK_INT >= 30) {
        try {
          android.content.Intent it = new android.content.Intent(
              "android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION");
          it.setData(android.net.Uri.parse("package:"
              + activity.getPackageName()));
          it.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
          activity.startActivity(it);
          return;
        } catch (Exception e) {
          // fallback di bawah
        }
        try {
          android.content.Intent it2 = new android.content.Intent(
              "android.settings.MANAGE_ALL_FILES_ACCESS_PERMISSION");
          it2.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
          activity.startActivity(it2);
          return;
        } catch (Exception e) {
          // abaikan
        }
      }
    } catch (Exception e) {
      // abaikan
    }
    try {
      android.content.Intent it3 = new android.content.Intent(
          "android.settings.APPLICATION_DETAILS_SETTINGS");
      it3.setData(android.net.Uri.parse("package:"
          + activity.getPackageName()));
      it3.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
      activity.startActivity(it3);
    } catch (Exception e) {
      // abaikan
    }
  }

  /** Gate otomatis: pastikan izin beres SEBELUM operasi storage jalan.
   *  WAJIB dipanggil dari background thread (memblokir sampai user
   *  menjawab dialog). Di UI thread: hanya cek status, tanpa aksi.
   *  Kembali true = lanjut; false = operasi akan gagal downstream lewat
   *  event gagalnya masing-masing (pesan + needPermission seperti biasa).
   *  Sudah granted = cuma 1x cek, tanpa dialog/settings. */
  public static boolean ensurePermission(final Form form,
      final Activity activity, String permission) {
    try {
      int sdk = android.os.Build.VERSION.SDK_INT;
      final String p = effectivePermission(permission, sdk);
      if (p == null) {
        return true;
      }
      boolean ui = android.os.Looper.myLooper()
          == android.os.Looper.getMainLooper();
      if (isStoragePerm(p) && sdk >= 30) {
        boolean mgr = false;
        try {
          mgr = android.os.Environment.isExternalStorageManager();
        } catch (Exception e) {
          mgr = false;
        }
        if (mgr) {
          return true;
        }
        if (!ui) {
          openAllFilesSettings(activity);
        }
        return false;
      }
      if (sdk < 23) {
        return true;
      }
      if (activity != null) {
        try {
          if (activity.checkSelfPermission(p)
              == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return true;
          }
        } catch (Exception e) {
          // lanjut ke dialog
        }
      }
      if (ui || form == null || activity == null) {
        return false;
      }
      // single-flight: 1 dialog untuk semua op paralel (mis. batch).
      java.util.concurrent.CountDownLatch latch;
      synchronized (ASK_LOCK) {
        if (askLatch == null) {
          askLatch = new java.util.concurrent.CountDownLatch(1);
          askResult = false;
          final java.util.concurrent.CountDownLatch mine = askLatch;
          try {
            activity.runOnUiThread(new Runnable() {
              public void run() {
                try {
                  form.askPermission(p, new PermissionResultHandler() {
                    public void HandlePermissionResponse(String perm,
                        boolean granted) {
                      synchronized (ASK_LOCK) {
                        askResult = granted;
                        askLatch = null;
                        mine.countDown();
                      }
                    }
                  });
                } catch (Exception e) {
                  synchronized (ASK_LOCK) {
                    askLatch = null;
                    mine.countDown();
                  }
                }
              }
            });
          } catch (Exception e) {
            synchronized (ASK_LOCK) {
              askLatch = null;
              mine.countDown();
            }
          }
        }
        latch = askLatch;
      }
      if (latch != null) {
        try {
          latch.await(60, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
          // abaikan
        }
      }
      synchronized (ASK_LOCK) {
        return askResult;
      }
    } catch (Exception e) {
      return false;
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
    return normalizePath(p).startsWith("//");
  }

  public static String assetName(String p) {
    String s = normalizePath(p);
    if (s.startsWith("//")) {
      s = s.substring(2);
    }
    return stripLeadingSlash(s);
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


  /** Root penyimpanan: kartu SD utama ("/storage/emulated/0"). */
  public static String rootPath() {
    try {
      return normalizePath(externalRoot().getAbsolutePath());
    } catch (Exception e) {
      return "/storage/emulated/0";
    }
  }

  /** Root kartu SD LEPASAN (removable, mis. "/storage/XXXX-XXXX").
   *  Kosong bila tidak ada / tidak terbaca. */
  public static String removableRoot(Context context) {
    try {
      File[] dirs = context.getExternalFilesDirs(null);
      String prim = "";
      try {
        File p = android.os.Environment.getExternalStorageDirectory();
        if (p != null) {
          prim = p.getAbsolutePath();
        }
      } catch (Exception e) {
        // abaikan
      }
      if (dirs != null) {
        for (int i = 0; i < dirs.length; i = i + 1) {
          if (dirs[i] == null) {
            continue;
          }
          String a = dirs[i].getAbsolutePath();
          int cut = a.indexOf("/Android/data/");
          String root = cut > 0 ? a.substring(0, cut) : a;
          if (root.length() > 0 && !root.equals(prim)) {
            return root;
          }
        }
      }
    } catch (Exception e) {
      // abaikan
    }
    try {
      File st = new File("/storage");
      File[] kids = st.listFiles();
      if (kids != null) {
        for (int i = 0; i < kids.length; i = i + 1) {
          String n = kids[i].getName();
          if (n.equals("emulated") || n.equals("self") || n.startsWith(".")) {
            continue;
          }
          if (kids[i].isDirectory()) {
            return kids[i].getAbsolutePath();
          }
        }
      }
    } catch (Exception e) {
      // abaikan
    }
    return "";
  }

  /** Resolve SEMUA path ke root kartu SD (STRATEGI TANPA SCOPE):
   *  - "" = root; "/Download/" -> "/storage/emulated/0/Download";
   *  - "/storage/emulated/0/Download/" dipakai apa adanya (anti ganda);
   *  - relatif "Download/a.jpg" ditempel ke root;
   *  - "/data/...", "/sdcard/..." absolut lain dipakai apa adanya;
   *  - "file://..." dibuka bungkusnya (absolut); "//..." = asset -> null;
   *  - URL http/https/ftp, data:, content:// = bukan file -> null. */
  public static File resolveRooted(String path) {
    String p = normalizePath(path);
    if (p.length() == 0) {
      return externalRoot();
    }
    if (isRemotePath(p)) {
      return null;
    }
    if (p.startsWith("//")) {
      return null;
    }
    if (p.startsWith("file://")) {
      return new File("/" + stripLeadingSlash(p.substring(7)));
    }
    String root = rootPath();
    String withSlash = p.startsWith("/") ? p : "/" + p;
    if (withSlash.equals(root) || withSlash.startsWith(root + "/")) {
      return new File(withSlash);
    }
    if (isAbsoluteDevicePath(withSlash)) {
      return new File(withSlash);
    }
    return new File(root + withSlash);
  }

  /** Path absolut tampilan/event dari input user (asset/remote/file://
   *  dikembalikan apa adanya, sudah dinormalkan). */
  public static String fullPath(String path) {
    String p = normalizePath(path);
    if (p.length() == 0) {
      return rootPath();
    }
    if (isRemotePath(p) || p.startsWith("//") || p.startsWith("file://")) {
      return p;
    }
    try {
      File f = resolveRooted(p);
      if (f != null) {
        return f.getAbsolutePath();
      }
    } catch (Exception e) {
      // abaikan
    }
    return p;
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
   *  tanpa ditempeli root lagi). */
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
      String p = normalizePath(path);
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
      String p = normalizePath(path);
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
    } else if (t.equals("pdf")) {
      return "pdf";
    } else if (t.equals("apk")) {
      return "apk";
    } else if (t.equals("unknown") || t.equals("lain")
        || t.equals("lainnya") || t.equals("other")) {
      return "unknown";
    }
    return "";
  }

  /** True bila nama file cocok untuk filter kategori kanonik.
   *  pdf = sub-tipe document (hanya ekstensi pdf); kategori lain =
   *  fileCategory biasa (document tetap mencakup pdf seperti dulu). */
  public static boolean fileMatchesCategory(String category, String fileName) {
    if ("pdf".equals(category)) {
      return "pdf".equals(extensionOf(fileName == null ? "" : fileName));
    }
    return category != null && category.equals(fileCategory(fileName));
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
    } else if ("pdf".equals(category)) {
      src = new String[] {"pdf"};
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

  // ================= satuan ukuran (B..YB, basis 1024) =================

  public static final String[] SIZE_UNITS = {"B", "KB", "MB", "GB", "TB",
      "PB", "EB", "ZB", "YB"};

  /** Index satuan kanonik (case-insensitive, trim). Tak dikenal = -1. */
  public static int sizeUnitIndex(String u) {
    try {
      String s = u == null ? "" : u.trim().toUpperCase();
      for (int i = 0; i < SIZE_UNITS.length; i = i + 1) {
        if (SIZE_UNITS[i].equals(s)) {
          return i;
        }
      }
    } catch (Exception e) {
      // abaikan
    }
    return -1;
  }

  /** Index satuan terbesar yang pas untuk bytes (auto). bytes<=0 = 0 (B). */
  public static int autoSizeUnitIndex(long bytes) {
    try {
      if (bytes <= 0) {
        return 0;
      }
      double v = (double) bytes;
      int i = 0;
      while (v >= 1024.0 && i < SIZE_UNITS.length - 1) {
        v = v / 1024.0;
        i = i + 1;
      }
      return i;
    } catch (Exception e) {
      return 0;
    }
  }

  /** Konversi value dari satuan fromIdx ke toIdx. Dibulatkan 2 desimal. */
  public static double convertSizeValue(double value, int fromIdx, int toIdx) {
    try {
      double r = value * Math.pow(1024.0, (double) (fromIdx - toIdx));
      return Math.round(r * 100.0) / 100.0;
    } catch (Exception e) {
      return -1;
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
