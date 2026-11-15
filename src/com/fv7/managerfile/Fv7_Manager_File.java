package com.fv7.managerfile;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Base64;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.FileScope;
import com.google.appinventor.components.runtime.ActivityResultListener;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.OnDestroyListener;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.JsonUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@DesignerComponent(
    version = 1,
    description = "File manager + download manager mandiri: unduh (pause/resume/cancel per ID), "
        + "salin/pindah dengan progres, baca/tulis/list, file picker, open media, base64, uri/json. "
        + "Path pendek per scope (Legacy, App, Private, Cache, Shared, Asset). Satu error global.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    androidMinSdk = 21)
@SimpleObject(external = true)
@UsesPermissions({"android.permission.INTERNET",
    "android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE"})
public class Fv7_Manager_File extends AndroidNonvisibleComponent
    implements ActivityResultListener, OnDestroyListener {

  private final Context context;
  private final Activity activity;
  private final Object lock = new Object();
  private int nextId = 1;

  private final Map<Integer, DlOp> downloads = new HashMap<Integer, DlOp>();
  private final Map<Integer, CpOp> copies = new HashMap<Integer, CpOp>();
  private final Map<Integer, CpOp> moves = new HashMap<Integer, CpOp>();
  private final Map<Integer, Integer> picks = new HashMap<Integer, Integer>();

  private static final long PROGRESS_MIN_MS = 400;

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

  private static class DlOp {
    int id;
    String url;
    File dest;
    String fileName;
    volatile boolean cancel = false;
    volatile boolean paused = false;
    volatile boolean running = false;
    final Object pauseLock = new Object();
  }

  private static class CpOp {
    int id;
    String kind;
    File src;
    File dst;
    long total = 0;
    volatile boolean cancel = false;
    volatile boolean running = false;
  }

  public Fv7_Manager_File(ComponentContainer container) {
    super(container.$form());
    this.context = container.$context();
    this.activity = container.$context();
  }

  // ================= scope + path =================

  private File externalRoot() {
    try {
      File f = Environment.getExternalStorageDirectory();
      if (f != null) {
        return f;
      }
    } catch (Exception e) {
      // abaikan, pakai fallback
    }
    return new File("/storage/emulated/0");
  }

  private File rootFor(FileScope scope) {
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

  private static String clean(String s) {
    return s == null ? "" : s.trim();
  }

  private static String stripLeadingSlash(String p) {
    String s = p;
    while (s.startsWith("/")) {
      s = s.substring(1);
    }
    return s;
  }

  private static boolean isAssetPath(String p) {
    return p.startsWith("//");
  }

  private static String assetName(String p) {
    return stripLeadingSlash(p);
  }

  /**
   * Aturan path: file:/// = absolut. // = asset (baca saja, kembali null).
   * Legacy + diawali / = root kartu SD. Lainnya = relatif terhadap root scope.
   * Kembali null untuk asset (ditangani jalur khusus).
   */
  private File resolve(FileScope scope, String path) {
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
    File root = rootFor(scope);
    if (p.length() == 0) {
      return root;
    }
    return new File(root, stripLeadingSlash(p));
  }

  // ================= event helper =================

  private void fire(final String name, final Object[] args) {
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          EventDispatcher.dispatchEvent(Fv7_Manager_File.this, name, args);
        }
      });
    } catch (Exception e) {
      // terakhir: coba langsung
      try {
        EventDispatcher.dispatchEvent(this, name, args);
      } catch (Exception e2) {
        // abaikan
      }
    }
  }

  private void error(String component, int id, String message) {
    String m = message == null ? "gagal" : message;
    fire("ErrorOccurred", new Object[] { component, Integer.valueOf(id), m });
  }

  private int takeId() {
    synchronized (lock) {
      int id = nextId;
      nextId = nextId + 1;
      return id;
    }
  }

  // ================= ID =================

  @SimpleFunction(description = "Buat angka ID unik baru (1, 2, 3, ...). Dipakai untuk operasi async.")
  public int CreateUniqueId() {
    return takeId();
  }

  // ================= DOWNLOAD =================

  @SimpleFunction(description = "Unduh URL ke folder scope+path. fileName kosong = nama otomatis "
      + "(content-disposition + mime + URL). path = folder tujuan. Kembali: id yang dipakai.")
  public int Download(String url, String fileName, FileScope scope, String path, int id) {
    String u = clean(url);
    if (u.length() == 0) {
      error("Download", id, "URL kosong");
      return id;
    }
    synchronized (lock) {
      if (downloads.containsKey(Integer.valueOf(id))) {
        error("Download", id, "ID " + id + " sedang dipakai unduhan lain");
        return id;
      }
    }
    File dir = resolve(scope, path);
    if (dir == null) {
      error("Download", id, "tidak bisa unduh ke Asset");
      return id;
    }
    try {
      if (!dir.exists()) {
        dir.mkdirs();
      }
    } catch (Exception e) {
      error("Download", id, "folder tujuan tidak bisa dibuat");
      return id;
    }
    final DlOp op = new DlOp();
    op.id = id;
    op.url = u;
    op.dest = dir;
    op.fileName = clean(fileName);
    synchronized (lock) {
      downloads.put(Integer.valueOf(id), op);
    }
    op.running = true;
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        runDownload(op);
      }
    });
    return id;
  }

  private void runDownload(DlOp op) {
    HttpURLConnection c = null;
    File outFile = null;
    try {
      c = (HttpURLConnection) new URL(op.url).openConnection();
      c.setConnectTimeout(20000);
      c.setReadTimeout(20000);
      c.setInstanceFollowRedirects(true);
      c.setRequestProperty("Accept-Encoding", "identity");
      c.setRequestProperty("User-Agent", "Mozilla/5.0");
      int code = c.getResponseCode();
      if (code >= 400) {
        finishDownload(op, false, "", "", "HTTP " + code);
        return;
      }
      String disp = c.getHeaderField("Content-Disposition");
      String mime = c.getContentType();
      String name = op.fileName;
      if (name.length() == 0) {
        name = GuessFileName(op.url, mime == null ? "" : mime, disp == null ? "" : disp);
      } else {
        name = sanitize(name);
      }
      op.fileName = name;
      long total = contentLen(c);
      outFile = uniqueFile(op.dest, name);
      FileOutputStream out = new FileOutputStream(outFile);
      try {
        InputStream in = c.getInputStream();
        try {
          byte[] buf = new byte[32768];
          long done = 0;
          int lastPct = -1;
          long lastFire = 0;
          int n;
          while ((n = in.read(buf)) > 0) {
            if (op.cancel) {
              finishDownload(op, false, "", "", "Dibatalkan");
              try {
                outFile.delete();
              } catch (Exception e) {
                // abaikan
              }
              return;
            }
            if (op.paused) {
              synchronized (op.pauseLock) {
                while (op.paused && !op.cancel) {
                  try {
                    op.pauseLock.wait();
                  } catch (InterruptedException e) {
                    // lanjut cek flag
                  }
                }
              }
              if (op.cancel) {
                finishDownload(op, false, "", "", "Dibatalkan");
                try {
                  outFile.delete();
                } catch (Exception e) {
                  // abaikan
                }
                return;
              }
            }
            out.write(buf, 0, n);
            done = done + n;
            int pct = total > 0 ? (int) (done * 100L / total) : 0;
            long now = System.currentTimeMillis();
            if (pct != lastPct && (now - lastFire >= PROGRESS_MIN_MS || pct == 100)) {
              lastPct = pct;
              lastFire = now;
              final int fId = op.id;
              final int fPct = pct;
              final long fDone = done;
              final long fTotal = total;
              form.runOnUiThread(new Runnable() {
                public void run() {
                  DownloadProgress(fId, fPct, fDone, fTotal);
                }
              });
            }
          }
          out.flush();
          finishDownload(op, true, outFile.getAbsolutePath(), op.fileName, "OK");
        } finally {
          try {
            in.close();
          } catch (Exception e) {
            // abaikan
          }
        }
      } finally {
        try {
          out.close();
        } catch (Exception e) {
          // abaikan
        }
      }
    } catch (Exception e) {
      String m = e.getMessage();
      finishDownload(op, false, "", "", m == null ? "unduh gagal" : m);
      if (outFile != null) {
        try {
          outFile.delete();
        } catch (Exception e2) {
          // abaikan
        }
      }
    } finally {
      if (c != null) {
        try {
          c.disconnect();
        } catch (Exception e) {
          // abaikan
        }
      }
    }
  }

  private void finishDownload(final DlOp op, final boolean success, final String path,
      final String name, final String msg) {
    synchronized (lock) {
      downloads.remove(Integer.valueOf(op.id));
    }
    op.running = false;
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          DownloadFinished(op.id, success, path, name, msg);
        }
      });
    } catch (Exception e) {
      DownloadFinished(op.id, success, path, name, msg);
    }
    if (!success && !"Dibatalkan".equals(msg)) {
      error("Download", op.id, msg);
    }
  }

  @SimpleFunction(description = "Batalkan unduhan berjalan. File sebagian dihapus.")
  public void CancelDownload(int id) {
    DlOp op;
    synchronized (lock) {
      op = downloads.get(Integer.valueOf(id));
    }
    if (op == null) {
      error("Cancel", id, "tidak ada unduhan ID " + id);
      return;
    }
    op.cancel = true;
    synchronized (op.pauseLock) {
      op.paused = false;
      op.pauseLock.notifyAll();
    }
  }

  @SimpleFunction(description = "Jeda unduhan berjalan (dilanjut dengan ResumeDownload).")
  public void PauseDownload(int id) {
    DlOp op;
    synchronized (lock) {
      op = downloads.get(Integer.valueOf(id));
    }
    if (op == null) {
      error("Pause", id, "tidak ada unduhan ID " + id);
      return;
    }
    op.paused = true;
  }

  @SimpleFunction(description = "Lanjutkan unduhan yang dijeda.")
  public void ResumeDownload(int id) {
    DlOp op;
    synchronized (lock) {
      op = downloads.get(Integer.valueOf(id));
    }
    if (op == null) {
      error("Resume", id, "tidak ada unduhan ID " + id);
      return;
    }
    synchronized (op.pauseLock) {
      op.paused = false;
      op.pauseLock.notifyAll();
    }
  }

  @SimpleFunction(description = "Daftar ID unduhan yang sedang berjalan.")
  public List<Integer> GetDownloadIds() {
    synchronized (lock) {
      return new ArrayList<Integer>(downloads.keySet());
    }
  }

  @SimpleFunction(description = "Tebak nama file dari URL, mime, dan content-disposition "
      + "(dipakai otomatis bila fileName Download dikosongkan).")
  public String GuessFileName(String url, String mimeType, String contentDisposition) {
    String name = null;
    if (contentDisposition != null && contentDisposition.length() > 0) {
      int star = contentDisposition.indexOf("filename*=");
      if (star >= 0) {
        int q1 = contentDisposition.indexOf("''", star);
        if (q1 > 0) {
          int end = contentDisposition.indexOf(';', q1 + 2);
          String enc = end > 0 ? contentDisposition.substring(q1 + 2, end)
              : contentDisposition.substring(q1 + 2);
          name = urlDecode(enc.trim());
        }
      }
      if (name == null) {
        int fn = contentDisposition.indexOf("filename=");
        if (fn >= 0) {
          int s = fn + 9;
          while (s < contentDisposition.length() && contentDisposition.charAt(s) == ' ') {
            s = s + 1;
          }
          if (s < contentDisposition.length() && contentDisposition.charAt(s) == '\"') {
            int e = contentDisposition.indexOf('\"', s + 1);
            if (e > 0) {
              name = contentDisposition.substring(s + 1, e);
            }
          } else {
            int e = contentDisposition.indexOf(';', s);
            String tok = e > 0 ? contentDisposition.substring(s, e)
                : contentDisposition.substring(s);
            name = tok.trim();
          }
        }
      }
    }
    if (name == null || name.length() == 0) {
      if (url != null) {
        String u = url;
        int q = u.indexOf('?');
        if (q >= 0) {
          u = u.substring(0, q);
        }
        int h = u.indexOf('#');
        if (h >= 0) {
          u = u.substring(0, h);
        }
        int s = u.lastIndexOf('/');
        if (s >= 0 && s + 1 < u.length()) {
          name = urlDecode(u.substring(s + 1));
        }
      }
    }
    if (name == null || name.length() == 0) {
      name = "downloadfile";
    }
    name = sanitize(name);
    if (name.indexOf('.') < 0) {
      String ext = extForMime(mimeType);
      if (ext.length() > 0) {
        name = name + "." + ext;
      }
    }
    return name;
  }

  // ================= COPY / MOVE =================

  @SimpleFunction(description = "Salin file dari fromScope+fromPath ke toScope+toPath. "
      + "Ada progres + cancel. Asset hanya bisa sebagai sumber.")
  public void CopyFile(FileScope fromScope, String fromPath, FileScope toScope, String toPath,
      int id) {
    synchronized (lock) {
      if (copies.containsKey(Integer.valueOf(id))) {
        error("Copy", id, "ID " + id + " sedang dipakai salinan lain");
        return;
      }
    }
    if (toScope == FileScope.Asset || (toPath != null && isAssetPath(clean(toPath)))) {
      error("Copy", id, "tidak bisa salin ke Asset");
      return;
    }
    final CpOp op = new CpOp();
    op.id = id;
    op.kind = "Copy";
    synchronized (lock) {
      copies.put(Integer.valueOf(id), op);
    }
    op.running = true;
    final FileScope fS = fromScope;
    final String fP = fromPath;
    final FileScope tS = toScope;
    final String tP = toPath;
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        runCopy(op, fS, fP, tS, tP, false);
      }
    });
  }

  @SimpleFunction(description = "Pindah file dari fromScope+fromPath ke toScope+toPath. "
      + "Ada progres + cancel. Asset hanya bisa sebagai sumber (jadi salin).")
  public void MoveFile(FileScope fromScope, String fromPath, FileScope toScope, String toPath,
      int id) {
    synchronized (lock) {
      if (moves.containsKey(Integer.valueOf(id))) {
        error("Move", id, "ID " + id + " sedang dipakai pindahan lain");
        return;
      }
    }
    if (toScope == FileScope.Asset || (toPath != null && isAssetPath(clean(toPath)))) {
      error("Move", id, "tidak bisa pindah ke Asset");
      return;
    }
    final CpOp op = new CpOp();
    op.id = id;
    op.kind = "Move";
    synchronized (lock) {
      moves.put(Integer.valueOf(id), op);
    }
    op.running = true;
    final FileScope fS = fromScope;
    final String fP = fromPath;
    final FileScope tS = toScope;
    final String tP = toPath;
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        runCopy(op, fS, fP, tS, tP, true);
      }
    });
  }

  private void runCopy(CpOp op, FileScope fS, String fP, FileScope tS, String tP, boolean move) {
    String evP = op.kind.equals("Move") ? "MoveProgress" : "CopyProgress";
    InputStream in = null;
    boolean fromAsset = false;
    String aName = "";
    File src = resolve(fS, fP);
    try {
      if (src == null) {
        String p = clean(fP);
        if (fS == FileScope.Asset || isAssetPath(p)) {
          fromAsset = true;
          aName = assetName(p);
        } else {
          finishCopy(op, false, "", "path sumber tidak valid", move);
          return;
        }
      } else if (src.isDirectory()) {
        finishCopy(op, false, "", "sumber adalah folder (pakai path file)", move);
        return;
      } else if (!src.exists()) {
        finishCopy(op, false, "", "file sumber tidak ada", move);
        return;
      }
      File dst = resolve(tS, tP);
      if (dst == null) {
        finishCopy(op, false, "", "tujuan tidak valid", move);
        return;
      }
      if (dst.exists() && dst.isDirectory()) {
        finishCopy(op, false, "", "tujuan adalah folder", move);
        return;
      }
      if (dst.getParentFile() != null) {
        dst.getParentFile().mkdirs();
      }
      long total;
      if (fromAsset) {
        try {
          total = context.getAssets().openFd(aName).getLength();
        } catch (Exception e) {
          total = -1;
        }
        in = context.getAssets().open(aName);
      } else {
        if (move && src.renameTo(dst)) {
          progressCopy(op, evP, 100, 1, 1);
          finishCopy(op, true, dst.getAbsolutePath(), "OK", move);
          return;
        }
        total = src.length();
        in = new FileInputStream(src);
      }
      op.total = total;
      FileOutputStream out = new FileOutputStream(dst);
      try {
        byte[] buf = new byte[32768];
        long done = 0;
        int lastPct = -1;
        long lastFire = 0;
        int n;
        while ((n = in.read(buf)) > 0) {
          if (op.cancel) {
            finishCopy(op, false, "", "Dibatalkan", move);
            try {
              dst.delete();
            } catch (Exception e) {
              // abaikan
            }
            return;
          }
          out.write(buf, 0, n);
          done = done + n;
          int pct = total > 0 ? (int) (done * 100L / total) : 0;
          long now = System.currentTimeMillis();
          if (pct != lastPct && (now - lastFire >= PROGRESS_MIN_MS || pct == 100)) {
            lastPct = pct;
            lastFire = now;
            progressCopy(op, evP, pct, done, total);
          }
        }
        out.flush();
        if (move && !fromAsset) {
          try {
            src.delete();
          } catch (Exception e) {
            // abaikan, salinan sudah jadi
          }
        }
        finishCopy(op, true, dst.getAbsolutePath(), "OK", move);
      } finally {
        try {
          out.close();
        } catch (Exception e) {
          // abaikan
        }
      }
    } catch (Exception e) {
      String m = e.getMessage();
      finishCopy(op, false, "", m == null ? "salin gagal" : m, move);
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

  private void progressCopy(CpOp op, String ev, final int pct, final long done,
      final long total) {
    final int fId = op.id;
    final boolean isMove = "MoveProgress".equals(ev);
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          if (isMove) {
            MoveProgress(fId, pct, done, total);
          } else {
            CopyProgress(fId, pct, done, total);
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  private void finishCopy(final CpOp op, final boolean success, final String destPath,
      final String msg, final boolean move) {
    synchronized (lock) {
      if (move) {
        moves.remove(Integer.valueOf(op.id));
      } else {
        copies.remove(Integer.valueOf(op.id));
      }
    }
    op.running = false;
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          if (move) {
            MoveFinished(op.id, success, destPath, msg);
          } else {
            CopyFinished(op.id, success, destPath, msg);
          }
        }
      });
    } catch (Exception e) {
      if (move) {
        MoveFinished(op.id, success, destPath, msg);
      } else {
        CopyFinished(op.id, success, destPath, msg);
      }
    }
    if (!success && !"Dibatalkan".equals(msg)) {
      error(op.kind, op.id, msg);
    }
  }

  @SimpleFunction(description = "Batalkan salinan berjalan. File sebagian dihapus.")
  public void CancelCopy(int id) {
    CpOp op;
    synchronized (lock) {
      op = copies.get(Integer.valueOf(id));
    }
    if (op == null) {
      error("Cancel", id, "tidak ada salinan ID " + id);
      return;
    }
    op.cancel = true;
  }

  @SimpleFunction(description = "Batalkan pindahan berjalan. File sebagian dihapus.")
  public void CancelMove(int id) {
    CpOp op;
    synchronized (lock) {
      op = moves.get(Integer.valueOf(id));
    }
    if (op == null) {
      error("Cancel", id, "tidak ada pindahan ID " + id);
      return;
    }
    op.cancel = true;
  }

  @SimpleFunction(description = "Daftar ID salinan yang sedang berjalan.")
  public List<Integer> GetCopyIds() {
    synchronized (lock) {
      return new ArrayList<Integer>(copies.keySet());
    }
  }

  @SimpleFunction(description = "Daftar ID pindahan yang sedang berjalan.")
  public List<Integer> GetMoveIds() {
    synchronized (lock) {
      return new ArrayList<Integer>(moves.keySet());
    }
  }

  // ================= READ / WRITE =================

  @SimpleFunction(description = "Baca file teks (UTF-8) di background. Hasil lewat GotText. "
      + "Asset bisa dibaca.")
  public void ReadText(FileScope scope, String path, int id) {
    final FileScope fS = scope;
    final String fP = path;
    final int fId = id;
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        InputStream in = null;
        try {
          String p = clean(fP);
          if (fS == FileScope.Asset || isAssetPath(p)) {
            in = context.getAssets().open(assetName(p));
          } else {
            File f = resolve(fS, fP);
            if (f == null || !f.exists() || f.isDirectory()) {
              error("Read", fId, "file tidak ada");
              return;
            }
            in = new FileInputStream(f);
          }
          BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
          StringBuilder sb = new StringBuilder();
          char[] buf = new char[8192];
          int n;
          while ((n = r.read(buf)) > 0) {
            sb.append(buf, 0, n);
          }
          final String text = sb.toString();
          final String sc = fS == null ? "" : fS.name();
          try {
            form.runOnUiThread(new Runnable() {
              public void run() {
                GotText(fId, sc, fP == null ? "" : fP, text);
              }
            });
          } catch (Exception e) {
            GotText(fId, sc, fP == null ? "" : fP, text);
          }
        } catch (Exception e) {
          String m = e.getMessage();
          error("Read", fId, m == null ? "baca gagal" : m);
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
    });
  }

  @SimpleFunction(description = "Tulis teks ke file (UTF-8). append=true = sambung di akhir. "
      + "Folder dibuat otomatis. Asset tidak bisa ditulis.")
  public boolean SaveFile(FileScope scope, String path, String content, boolean append) {
    Writer w = null;
    try {
      File f = resolve(scope, path);
      if (f == null) {
        error("Write", 0, "tidak bisa tulis ke Asset");
        return false;
      }
      if (f.getParentFile() != null) {
        f.getParentFile().mkdirs();
      }
      w = new OutputStreamWriter(new FileOutputStream(f, append), "UTF-8");
      w.write(content == null ? "" : content);
      w.flush();
      return true;
    } catch (Exception e) {
      String m = e.getMessage();
      error("Write", 0, m == null ? "tulis gagal" : m);
      return false;
    } finally {
      if (w != null) {
        try {
          w.close();
        } catch (Exception e) {
          // abaikan
        }
      }
    }
  }

  // ================= LIST / INFO =================

  @SimpleFunction(description = "Apakah file/folder ada (Asset bisa dicek).")
  public boolean Exists(FileScope scope, String path) {
    try {
      String p = clean(path);
      if (scope == FileScope.Asset || isAssetPath(p)) {
        try {
          InputStream in = context.getAssets().open(assetName(p));
          try {
            in.close();
          } catch (Exception e) {
            // abaikan
          }
          return true;
        } catch (Exception e) {
          try {
            String[] l = context.getAssets().list(assetName(p));
            return l != null && l.length > 0;
          } catch (Exception e2) {
            return false;
          }
        }
      }
      File f = resolve(scope, path);
      return f != null && f.exists();
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleFunction(description = "Apakah path adalah folder.")
  public boolean IsDirectory(FileScope scope, String path) {
    try {
      String p = clean(path);
      if (scope == FileScope.Asset || isAssetPath(p)) {
        try {
          String[] l = context.getAssets().list(assetName(p));
          return l != null && l.length > 0;
        } catch (Exception e) {
          return false;
        }
      }
      File f = resolve(scope, path);
      return f != null && f.isDirectory();
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleFunction(description = "Ukuran file dalam byte. Folder = total isi rekursif. "
      + "-1 bila tidak ada.")
  public long FileSize(FileScope scope, String path) {
    try {
      String p = clean(path);
      if (scope == FileScope.Asset || isAssetPath(p)) {
        try {
          return context.getAssets().openFd(assetName(p)).getLength();
        } catch (Exception e) {
          return -1L;
        }
      }
      File f = resolve(scope, path);
      if (f == null || !f.exists()) {
        return -1L;
      }
      return dirSize(f);
    } catch (Exception e) {
      return -1L;
    }
  }

  private long dirSize(File f) {
    if (!f.isDirectory()) {
      return f.length();
    }
    long total = 0;
    File[] kids = f.listFiles();
    if (kids != null) {
      for (int i = 0; i < kids.length; i = i + 1) {
        total = total + dirSize(kids[i]);
      }
    }
    return total;
  }

  @SimpleFunction(description = "Hapus file/folder (folder rekursif). Asset tidak bisa dihapus.")
  public boolean Delete(FileScope scope, String path) {
    try {
      File f = resolve(scope, path);
      if (f == null) {
        error("Delete", 0, "tidak bisa hapus Asset");
        return false;
      }
      if (!f.exists()) {
        return true;
      }
      return deleteRecursive(f);
    } catch (Exception e) {
      String m = e.getMessage();
      error("Delete", 0, m == null ? "hapus gagal" : m);
      return false;
    }
  }

  @SimpleFunction(description = "Buat folder (termasuk induknya). Sudah ada = true.")
  public boolean MakeDirectory(FileScope scope, String path) {
    try {
      File d = resolve(scope, path);
      if (d == null) {
        error("Mkdir", 0, "tidak bisa buat folder di Asset");
        return false;
      }
      if (d.exists()) {
        return true;
      }
      return d.mkdirs();
    } catch (Exception e) {
      String m = e.getMessage();
      error("Mkdir", 0, m == null ? "buat folder gagal" : m);
      return false;
    }
  }

  @SimpleFunction(description = "Ganti nama FILE (tanpa pindah folder). newName = nama saja.")
  public boolean RenameFile(FileScope scope, String path, String newName) {
    try {
      File src = resolve(scope, path);
      if (src == null || !src.exists() || src.isDirectory()) {
        error("RenameFile", 0, "file tidak ada");
        return false;
      }
      String nn = sanitize(clean(newName));
      if (nn.length() == 0 || nn.indexOf('/') >= 0) {
        error("RenameFile", 0, "nama baru tidak valid");
        return false;
      }
      return src.renameTo(new File(src.getParentFile(), nn));
    } catch (Exception e) {
      String m = e.getMessage();
      error("RenameFile", 0, m == null ? "rename gagal" : m);
      return false;
    }
  }

  @SimpleFunction(description = "Ganti nama FOLDER (tanpa pindah). newName = nama saja.")
  public boolean RenameFolder(FileScope scope, String path, String newName) {
    try {
      File src = resolve(scope, path);
      if (src == null || !src.exists() || !src.isDirectory()) {
        error("RenameFolder", 0, "folder tidak ada");
        return false;
      }
      String nn = sanitize(clean(newName));
      if (nn.length() == 0 || nn.indexOf('/') >= 0) {
        error("RenameFolder", 0, "nama baru tidak valid");
        return false;
      }
      return src.renameTo(new File(src.getParentFile(), nn));
    } catch (Exception e) {
      String m = e.getMessage();
      error("RenameFolder", 0, m == null ? "rename gagal" : m);
      return false;
    }
  }

  @SimpleFunction(description = "Daftar file (path lengkap). filter = ekstensi tanpa titik "
      + "(kosong = semua). includeSubfolders = sertakan isi subfolder.")
  public List<String> ListFiles(FileScope scope, String path, String filter, boolean includeSubfolders) {
    ArrayList<String> out = new ArrayList<String>();
    try {
      String p = clean(path);
      if (scope == FileScope.Asset || isAssetPath(p)) {
        String[] l = context.getAssets().list(assetName(p));
        if (l != null) {
          String f = normExt(filter);
          for (int i = 0; i < l.length; i = i + 1) {
            if (matchExt(l[i], f)) {
              out.add("//" + assetName(p) + "/" + l[i]);
            }
          }
        }
        Collections.sort(out);
        return out;
      }
      File d = resolve(scope, path);
      if (d == null || !d.isDirectory()) {
        error("List", 0, "folder tidak ada");
        return out;
      }
      listFilesWalk(d, normExt(filter), includeSubfolders, out);
      Collections.sort(out);
    } catch (Exception e) {
      String m = e.getMessage();
      error("List", 0, m == null ? "list gagal" : m);
    }
    return out;
  }

  @SimpleFunction(description = "Daftar subfolder (path lengkap, tanpa rekursif).")
  public List<String> ListFolders(FileScope scope, String path) {
    ArrayList<String> out = new ArrayList<String>();
    try {
      String p = clean(path);
      if (scope == FileScope.Asset || isAssetPath(p)) {
        error("List", 0, "list folder Asset tidak didukung");
        return out;
      }
      File d = resolve(scope, path);
      if (d == null || !d.isDirectory()) {
        error("List", 0, "folder tidak ada");
        return out;
      }
      File[] fs = d.listFiles();
      if (fs != null) {
        for (int i = 0; i < fs.length; i = i + 1) {
          if (fs[i].isDirectory()) {
            out.add(fs[i].getAbsolutePath());
          }
        }
      }
      Collections.sort(out);
    } catch (Exception e) {
      String m = e.getMessage();
      error("List", 0, m == null ? "list gagal" : m);
    }
    return out;
  }

  private void listFilesWalk(File dir, String ext, boolean recursive, ArrayList<String> out) {
    File[] fs = dir.listFiles();
    if (fs == null) {
      return;
    }
    for (int i = 0; i < fs.length; i = i + 1) {
      File g = fs[i];
      if (g.isDirectory()) {
        if (recursive) {
          listFilesWalk(g, ext, true, out);
        }
      } else if (matchExt(g.getName(), ext)) {
        out.add(g.getAbsolutePath());
      }
    }
  }

  // ================= PATH / MIME =================

  @SimpleFunction(description = "Tebak mime dari nama file/ekstensi. Tidak perlu file-nya ada.")
  public String GetMimeType(String fileName) {
    try {
      String n = clean(fileName).toLowerCase();
      int dot = n.lastIndexOf('.');
      String ext = dot >= 0 ? n.substring(dot + 1) : n;
      int q = ext.indexOf('?');
      if (q >= 0) {
        ext = ext.substring(0, q);
      }
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

  @SimpleFunction(description = "Nama file dari sebuah path (buang foldernya).")
  public String GetFileName(String path) {
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

  @SimpleFunction(description = "Path absolut ASD aplikasi (tulis tanpa izin).")
  public String GetASD() {
    try {
      File f = context.getExternalFilesDir(null);
      if (f != null) {
        return f.getAbsolutePath();
      }
    } catch (Exception e) {
      // abaikan
    }
    return "";
  }

  @SimpleFunction(description = "Path root kartu SD (/storage/emulated/0).")
  public String GetStoragePath() {
    try {
      return externalRoot().getAbsolutePath();
    } catch (Exception e) {
      return "/storage/emulated/0";
    }
  }

  @SimpleFunction(description = "Path folder cache aplikasi (untuk file sementara).")
  public String GetCacheDir() {
    try {
      File f = context.getCacheDir();
      if (f != null) {
        return f.getAbsolutePath();
      }
    } catch (Exception e) {
      // abaikan
    }
    return "";
  }

  @SimpleFunction(description = "Ubah angka byte jadi teks (B/KB/MB/GB).")
  public String ConvertBytes(long bytes) {
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

  // ================= URI / JSON =================

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

  @SimpleFunction(description = "Decode teks URL-encoded (pengganti hasil FilePicker uri).")
  public String UriDecode(String text) {
    try {
      return URLDecoder.decode(text == null ? "" : text, "UTF-8");
    } catch (Exception e) {
      return text == null ? "" : text;
    }
  }

  @SimpleFunction(description = "Encode list/dictionary/teks jadi JSON (seperti Web JsonObjectEncode).")
  public String JsonEncode(Object value) {
    try {
      return JsonUtil.getJsonRepresentation(value);
    } catch (Exception e) {
      String m = e.getMessage();
      error("Json", 0, m == null ? "encode gagal" : m);
      return "";
    }
  }

  @SimpleFunction(description = "Decode teks JSON jadi dictionary/list (seperti Web JsonTextDecode).")
  public Object JsonDecode(String json) {
    try {
      return JsonUtil.getObjectFromJson(json == null ? "" : json);
    } catch (Exception e) {
      String m = e.getMessage();
      error("Json", 0, m == null ? "decode gagal" : m);
      return "";
    }
  }

  // ================= FILE PICKER =================

  @SimpleFunction(description = "Buka pemilih file. mimeType contoh: image/* (kosong = semua). "
      + "Hasil lewat AfterPicked (uri + path).")
  public void OpenFilePicker(String mimeType, int id) {
    try {
      Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
      intent.addCategory(Intent.CATEGORY_OPENABLE);
      String mt = clean(mimeType);
      intent.setType(mt.length() == 0 ? "*/*" : mt);
      int rc = form.registerForActivityResult(this);
      synchronized (lock) {
        picks.put(Integer.valueOf(rc), Integer.valueOf(id));
      }
      activity.startActivityForResult(intent, rc);
    } catch (Exception e) {
      String m = e.getMessage();
      error("Pick", id, m == null ? "picker gagal dibuka" : m);
      AfterPicked(id, false, "", "");
    }
  }

  public void resultReturned(int requestCode, int resultCode, Intent data) {
    Integer idObj;
    synchronized (lock) {
      idObj = picks.remove(Integer.valueOf(requestCode));
    }
    if (idObj == null) {
      return;
    }
    final int id = idObj.intValue();
    try {
      if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
        final Uri uri = data.getData();
        final String uriStr = uri.toString();
        final String path = resolveUriToPath(uri, id);
        AfterPicked(id, true, uriStr, path);
      } else {
        AfterPicked(id, false, "", "");
      }
    } catch (Exception e) {
      String m = e.getMessage();
      error("Pick", id, m == null ? "picker gagal" : m);
      AfterPicked(id, false, "", "");
    }
  }

  private String resolveUriToPath(Uri uri, int id) {
    try {
      String scheme = uri.getScheme();
      if (scheme != null && scheme.equalsIgnoreCase("file")) {
        String p = uri.getPath();
        return p == null ? "" : p;
      }
      Cursor c = null;
      try {
        ContentResolver cr = context.getContentResolver();
        c = cr.query(uri, new String[] { "_data" }, null, null, null);
        if (c != null && c.moveToFirst()) {
          int idx = c.getColumnIndex("_data");
          if (idx >= 0) {
            String p = c.getString(idx);
            if (p != null && p.length() > 0) {
              return p;
            }
          }
        }
      } catch (Exception e) {
        // jatuh ke salinan cache
      } finally {
        if (c != null) {
          try {
            c.close();
          } catch (Exception e) {
            // abaikan
          }
        }
      }
      String guess = uri.getLastPathSegment();
      if (guess == null || guess.length() == 0) {
        guess = "picked";
      }
      guess = sanitize(guess);
      File cache = context.getCacheDir();
      if (!cache.exists()) {
        cache.mkdirs();
      }
      File dst = uniqueFile(cache, "pick_" + id + "_" + guess);
      InputStream in = context.getContentResolver().openInputStream(uri);
      try {
        FileOutputStream out = new FileOutputStream(dst);
        try {
          byte[] buf = new byte[32768];
          int n;
          while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
          }
          out.flush();
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
      return dst.getAbsolutePath();
    } catch (Exception e) {
      return "";
    }
  }

  // ================= OPEN FILE =================

  @SimpleFunction(description = "Buka file dengan aplikasi lain (musik, gambar, PDF, ...). "
      + "Hasil lewat AfterOpen. Asset disalin ke cache dulu.")
  public void OpenFile(FileScope scope, String path, int id) {
    try {
      String p = clean(path);
      File f = resolve(scope, path);
      if (f == null) {
        if (scope == FileScope.Asset || isAssetPath(p)) {
          File cache = context.getCacheDir();
          if (!cache.exists()) {
            cache.mkdirs();
          }
          File dst = new File(cache, sanitize(GetFileName(assetName(p))));
          InputStream in = context.getAssets().open(assetName(p));
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
          f = dst;
        } else {
          error("Open", id, "path tidak valid");
          AfterOpen(id, false, "path tidak valid");
          return;
        }
      }
      if (!f.exists()) {
        error("Open", id, "file tidak ada");
        AfterOpen(id, false, "file tidak ada");
        return;
      }
      Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.setDataAndType(Uri.fromFile(f), GetMimeType(f.getName()));
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      activity.startActivity(intent);
      AfterOpen(id, true, "");
    } catch (Exception e) {
      String m = e.getMessage();
      if (m == null) {
        m = "tidak ada aplikasi pembuka / file tidak bisa dibuka";
      }
      error("Open", id, m);
      AfterOpen(id, false, m);
    }
  }

  // ================= BASE64 =================

  @SimpleFunction(description = "Simpan base64 jadi file. Hasil lewat AfterBase64Saved.")
  public void Base64ToFile(String base64, FileScope scope, String path, int id) {
    final String b64 = base64 == null ? "" : base64;
    final FileScope fS = scope;
    final String fP = path;
    final int fId = id;
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        boolean ok = false;
        String outPath = "";
        String outMsg = "";
        try {
          byte[] data = Base64.decode(b64, Base64.DEFAULT);
          File f = resolve(fS, fP);
          if (f == null) {
            outMsg = "tidak bisa tulis ke Asset";
          } else {
            if (f.getParentFile() != null) {
              f.getParentFile().mkdirs();
            }
            FileOutputStream out = new FileOutputStream(f);
            try {
              out.write(data);
              out.flush();
            } finally {
              try {
                out.close();
              } catch (Exception e) {
                // abaikan
              }
            }
            ok = true;
            outPath = f.getAbsolutePath();
            outMsg = "OK";
          }
        } catch (Exception e) {
          String m = e.getMessage();
          outMsg = m == null ? "base64 gagal disimpan" : m;
        }
        final boolean fOk = ok;
        final String fPath = outPath;
        final String fMsg = outMsg;
        if (!fOk) {
          error("Base64File", fId, fMsg);
        }
        try {
          form.runOnUiThread(new Runnable() {
            public void run() {
              AfterBase64Saved(fId, fOk, fPath, fMsg);
            }
          });
        } catch (Exception e) {
          AfterBase64Saved(fId, fOk, fPath, fMsg);
        }
      }
    });
  }

  @SimpleFunction(description = "Decode base64 jadi gambar di cache. Path hasil langsung "
      + "bisa dipasang ke Image.Picture. Hasil lewat AfterPictureReady.")
  public void Base64ToPicture(String base64, int id) {
    final String b64 = base64 == null ? "" : base64;
    final int fId = id;
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        boolean ok = false;
        String outPath = "";
        String outMsg = "";
        try {
          byte[] data = Base64.decode(b64, Base64.DEFAULT);
          String ext = "png";
          if (data.length > 3 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8) {
            ext = "jpg";
          } else if (data.length > 3 && data[0] == 'G' && data[1] == 'I' && data[2] == 'F') {
            ext = "gif";
          }
          File cache = context.getCacheDir();
          if (!cache.exists()) {
            cache.mkdirs();
          }
          File dst = new File(cache, "pic_" + fId + "." + ext);
          FileOutputStream out = new FileOutputStream(dst);
          try {
            out.write(data);
            out.flush();
          } finally {
            try {
              out.close();
            } catch (Exception e) {
              // abaikan
            }
          }
          ok = true;
          outPath = dst.getAbsolutePath();
          outMsg = "OK";
        } catch (Exception e) {
          String m = e.getMessage();
          outMsg = m == null ? "base64 gagal di-decode" : m;
        }
        final boolean fOk = ok;
        final String fPath = outPath;
        final String fMsg = outMsg;
        if (!fOk) {
          error("Base64Picture", fId, fMsg);
        }
        try {
          form.runOnUiThread(new Runnable() {
            public void run() {
              AfterPictureReady(fId, fOk, fPath, fMsg);
            }
          });
        } catch (Exception e) {
          AfterPictureReady(fId, fOk, fPath, fMsg);
        }
      }
    });
  }

  // ================= EVENTS =================

  @SimpleEvent(description = "Progres unduhan: persen 0-100 + byte.")
  public void DownloadProgress(int id, int percent, long receivedBytes, long totalBytes) {
    EventDispatcher.dispatchEvent(this, "DownloadProgress",
        Integer.valueOf(id), Integer.valueOf(percent), Long.valueOf(receivedBytes),
        Long.valueOf(totalBytes));
  }

  @SimpleEvent(description = "Unduhan selesai. success=false berarti gagal/dibatalkan "
      + "(lihat message + ErrorOccurred).")
  public void DownloadFinished(int id, boolean success, String savePath, String fileName,
      String message) {
    EventDispatcher.dispatchEvent(this, "DownloadFinished",
        Integer.valueOf(id), Boolean.valueOf(success), savePath, fileName, message);
  }

  @SimpleEvent(description = "Progres salinan: persen 0-100 + byte.")
  public void CopyProgress(int id, int percent, long copiedBytes, long totalBytes) {
    EventDispatcher.dispatchEvent(this, "CopyProgress",
        Integer.valueOf(id), Integer.valueOf(percent), Long.valueOf(copiedBytes),
        Long.valueOf(totalBytes));
  }

  @SimpleEvent(description = "Salinan selesai.")
  public void CopyFinished(int id, boolean success, String destPath, String message) {
    EventDispatcher.dispatchEvent(this, "CopyFinished",
        Integer.valueOf(id), Boolean.valueOf(success), destPath, message);
  }

  @SimpleEvent(description = "Progres pindahan: persen 0-100 + byte.")
  public void MoveProgress(int id, int percent, long movedBytes, long totalBytes) {
    EventDispatcher.dispatchEvent(this, "MoveProgress",
        Integer.valueOf(id), Integer.valueOf(percent), Long.valueOf(movedBytes),
        Long.valueOf(totalBytes));
  }

  @SimpleEvent(description = "Pindahan selesai.")
  public void MoveFinished(int id, boolean success, String destPath, String message) {
    EventDispatcher.dispatchEvent(this, "MoveFinished",
        Integer.valueOf(id), Boolean.valueOf(success), destPath, message);
  }

  @SimpleEvent(description = "Hasil ReadText: id + scope + path + isi file.")
  public void GotText(int id, String scope, String path, String text) {
    EventDispatcher.dispatchEvent(this, "GotText",
        Integer.valueOf(id), scope, path, text);
  }

  @SimpleEvent(description = "Hasil OpenFilePicker: uri + path (decoded). "
      + "success=false = dibatalkan user.")
  public void AfterPicked(int id, boolean success, String uri, String path) {
    EventDispatcher.dispatchEvent(this, "AfterPicked",
        Integer.valueOf(id), Boolean.valueOf(success), uri, path);
  }

  @SimpleEvent(description = "Hasil OpenFile.")
  public void AfterOpen(int id, boolean success, String message) {
    EventDispatcher.dispatchEvent(this, "AfterOpen",
        Integer.valueOf(id), Boolean.valueOf(success), message);
  }

  @SimpleEvent(description = "Hasil Base64ToFile.")
  public void AfterBase64Saved(int id, boolean success, String path, String message) {
    EventDispatcher.dispatchEvent(this, "AfterBase64Saved",
        Integer.valueOf(id), Boolean.valueOf(success), path, message);
  }

  @SimpleEvent(description = "Hasil Base64ToPicture: path cache siap ke Image.Picture.")
  public void AfterPictureReady(int id, boolean success, String cachePath, String message) {
    EventDispatcher.dispatchEvent(this, "AfterPictureReady",
        Integer.valueOf(id), Boolean.valueOf(success), cachePath, message);
  }

  @SimpleEvent(description = "SATU-SATUNYA error global. component = nama operasi "
      + "(bandingkan dengan blok Op...). id = 0 untuk method sync.")
  public void ErrorOccurred(String component, int id, String message) {
    EventDispatcher.dispatchEvent(this, "ErrorOccurred",
        component, Integer.valueOf(id), message);
  }

  // ================= OP (read-only) =================

  @SimpleProperty(description = "Nama operasi Download (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpDownload() {
    return "Download";
  }

  @SimpleProperty(description = "Nama operasi Copy (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpCopy() {
    return "Copy";
  }

  @SimpleProperty(description = "Nama operasi Move (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpMove() {
    return "Move";
  }

  @SimpleProperty(description = "Nama operasi Read (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpRead() {
    return "Read";
  }

  @SimpleProperty(description = "Nama operasi Write (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpWrite() {
    return "Write";
  }

  @SimpleProperty(description = "Nama operasi Delete (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpDelete() {
    return "Delete";
  }

  @SimpleProperty(description = "Nama operasi Mkdir (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpMkdir() {
    return "Mkdir";
  }

  @SimpleProperty(description = "Nama operasi RenameFile (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpRenameFile() {
    return "RenameFile";
  }

  @SimpleProperty(description = "Nama operasi RenameFolder (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpRenameFolder() {
    return "RenameFolder";
  }

  @SimpleProperty(description = "Nama operasi List (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpList() {
    return "List";
  }

  @SimpleProperty(description = "Nama operasi Open (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpOpen() {
    return "Open";
  }

  @SimpleProperty(description = "Nama operasi Pick (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpPick() {
    return "Pick";
  }

  @SimpleProperty(description = "Nama operasi Base64File (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpBase64File() {
    return "Base64File";
  }

  @SimpleProperty(description = "Nama operasi Base64Picture (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpBase64Picture() {
    return "Base64Picture";
  }

  @SimpleProperty(description = "Nama operasi Pause (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpPause() {
    return "Pause";
  }

  @SimpleProperty(description = "Nama operasi Resume (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpResume() {
    return "Resume";
  }

  @SimpleProperty(description = "Nama operasi Cancel (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpCancel() {
    return "Cancel";
  }

  @SimpleProperty(description = "Nama operasi Json (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpJson() {
    return "Json";
  }

  // ================= destroy =================

  public void onDestroy() {
    try {
      form.unregisterForActivityResult(this);
    } catch (Exception e) {
      // abaikan
    }
    synchronized (lock) {
      for (DlOp op : downloads.values()) {
        op.cancel = true;
        synchronized (op.pauseLock) {
          op.paused = false;
          op.pauseLock.notifyAll();
        }
      }
      for (CpOp op : copies.values()) {
        op.cancel = true;
      }
      for (CpOp op : moves.values()) {
        op.cancel = true;
      }
    }
  }

  // ================= util internal =================

  private long contentLen(HttpURLConnection c) {
    try {
      String h = c.getHeaderField("Content-Length");
      if (h != null) {
        try {
          return Long.parseLong(h.trim());
        } catch (Exception e2) {
          // jatuh ke getContentLength
        }
      }
    } catch (Exception e) {
      // abaikan
    }
    try {
      int n = c.getContentLength();
      return n < 0 ? -1L : (long) n;
    } catch (Exception e) {
      return -1L;
    }
  }

  private boolean deleteRecursive(File f) {
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

  private void copyStream(InputStream in, FileOutputStream out) throws Exception {
    byte[] buf = new byte[32768];
    int n;
    while ((n = in.read(buf)) > 0) {
      out.write(buf, 0, n);
    }
    out.flush();
  }

  private File uniqueFile(File dir, String name) {
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

  private String sanitize(String name) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < name.length(); i = i + 1) {
      char ch = name.charAt(i);
      if (ch == '\\' || ch == '/' || ch == ':' || ch == '*' || ch == '?' || ch == '\"'
          || ch == '<' || ch == '>' || ch == '|' || ch < 32) {
        sb.append('_');
      } else {
        sb.append(ch);
      }
    }
    String s = sb.toString().trim();
    if (s.length() == 0) {
      return "downloadfile";
    }
    return s;
  }

  private String extForMime(String mime) {
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

  private String urlDecode(String s) {
    try {
      return URLDecoder.decode(s, "UTF-8");
    } catch (Exception e) {
      return s;
    }
  }

  private static String normExt(String filter) {
    String f = filter == null ? "" : filter.trim().toLowerCase();
    if (f.equals("*")) {
      return "";
    }
    if (f.startsWith(".")) {
      f = f.substring(1);
    }
    return f;
  }

  private static boolean matchExt(String name, String ext) {
    if (ext.length() == 0) {
      return true;
    }
    return name.toLowerCase().endsWith("." + ext);
  }
}
