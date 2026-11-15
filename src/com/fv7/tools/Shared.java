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

  public static void err(Component comp, String component, int id, String message) {
    String m = message == null ? "gagal" : message;
    try {
      EventDispatcher.dispatchEvent(comp, "ErrorOccurred", component,
          Integer.valueOf(id), m);
    } catch (Exception e) {
      // abaikan
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
    return p.startsWith("//");
  }

  public static String assetName(String p) {
    return stripLeadingSlash(p);
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

  /**
   * Aturan path: file:/// = absolut. // = asset (baca saja, kembali null).
   * Legacy + diawali / = root kartu SD. Legacy tanpa / = privat.
   */
  public static File resolve(Context context, FileScope scope, String path) {
    String p = clean(path);
    if (p.startsWith("file://")) {
      return new File(stripLeadingSlash(p.substring(7)));
    }
    if (isAssetPath(p) || scope == FileScope.Asset) {
      return null;
    }
    if (scope == FileScope.Legacy && p.startsWith("/")) {
      return new File(externalRoot(), stripLeadingSlash(p));
    }
    if (scope == FileScope.Legacy && p.length() > 0 && !p.startsWith("/")) {
      try {
        return new File(context.getFilesDir(), p);
      } catch (Exception e) {
        return new File(externalRoot(), p);
      }
    }
    File root = rootFor(context, scope);
    if (p.length() == 0) {
      return root;
    }
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
      String p = clean(path);
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
      String n = clean(name);
      int q = n.indexOf('?');
      if (q >= 0) {
        n = n.substring(0, q);
      }
      int dot = n.lastIndexOf('.');
      if (dot >= 0 && dot + 1 < n.length()) {
        return n.substring(dot + 1).toLowerCase();
      }
      return "";
    } catch (Exception e) {
      return "";
    }
  }

  public static String parentOf(String path) {
    try {
      String p = clean(path);
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
