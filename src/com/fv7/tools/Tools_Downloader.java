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
import com.google.appinventor.components.runtime.OnDestroyListener;
import com.google.appinventor.components.runtime.util.AsynchUtil;

import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@DesignerComponent(
    version = 23,
    description = "Tools_Downloader: unduh paralel ber-id (pause/resume/cancel per id, "
        + "Started/ProgressChanged/Finished) + unduh batch list URL. Id dikelola "
        + "CreateDownloadId (atau CreateUniqueId Tools_Base). List id URUT PEMBUATAN "
        + "(index 1 = pertama dibuat). Konvensi awalan: Is = cek boolean; Call = async "
        + "+ hasil lewat event; Set = langsung tanpa feedback; Get = nilai balik sinkron.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    androidMinSdk = 21,
    iconName = "aiwebres/tools_downloader.jpg",
    helpUrl = "https://github.com/VB-7/MiT-File_Manager")
@SimpleObject(external = true)
@UsesPermissions({"android.permission.INTERNET",
    "android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE",
    "android.permission.MANAGE_EXTERNAL_STORAGE",
    "android.permission.POST_NOTIFICATIONS"})
public class Tools_Downloader extends AndroidNonvisibleComponent
    implements OnDestroyListener {

  private final Context context;
  private final Activity activity;

  private static final Map<Integer, DlOp> downloads = new HashMap<Integer, DlOp>();
  private static final Object lock = new Object();
  private static final long PROGRESS_MIN_MS = 400;

  private static boolean dlBatchActive = false;
  private static int dlBatchTotal = 0;
  private static int dlBatchDone = 0;
  private static int dlBatchOk = 0;
  private static String dlBatchFolder = "";

  private static class DlOp {
    int id;
    String url;
    File dest;
    String fileName;
    volatile boolean cancel = false;
    volatile boolean paused = false;
    final Object pauseLock = new Object();
  }

  /** Rekam status terakhir per id = fallback polling untuk semua event unduhan. */
  private static final class DlInfo {
    String status = "antri";
    int percent = 0;
    long received = 0;
    long total = 0;
    boolean success = false;
    String savePath = "";
    String fileName = "";
    String message = "";
    String url = "";
    String folder = "";
    int attempts = 0;
  }

  private static final Map<Integer, DlInfo> infos = new HashMap<Integer, DlInfo>();

  private static DlInfo infoOf(int id) {
    synchronized (lock) {
      DlInfo i = infos.get(Integer.valueOf(id));
      if (i == null) {
        i = new DlInfo();
        infos.put(Integer.valueOf(id), i);
      }
      return i;
    }
  }

  public Tools_Downloader(ComponentContainer container) {
    super(container.$form());
    this.context = container.$context();
    this.activity = container.$context();
  }

  @SimpleFunction(description = "CALL: unduh ke folder. url = http(s) ATAU data:URL "
      + "(base64/url-encoded, mis. data:image/png;base64,...) ATAU content: URI "
      + "Android (dari file picker) ATAU file: URL / path lokal (disalin). "
      + "blob: TIDAK didukung (itu objek browser; ubah dulu jadi base64 via JavaScript, "
      + "lalu kirim sebagai data:URL atau ke CallSaveBase64ToFile Tools_File). "
      + "fileName kosong = nama otomatis (seperti CallGuessFileName). folder: boleh nama "
      + "relatif (Download/ = aman tanpa izin khusus); folder di luar itu butuh izin "
      + "MANAGE_EXTERNAL_STORAGE. Contoh aman: Download/. id dari CreateDownloadId atau "
      + "CreateUniqueId Tools_Base. Hasil lewat DownloadStarted/ProgressChanged/Finished "
      + "(bawa id). Gagal mulai (URL/id/folder salah) = Finished(success=false) + "
      + "ErrorOccurred.")
  public void CallDownload(String url, String fileName, String folder, int id) {
    String u = Shared.clean(url);
    if (u.length() == 0) {
      Shared.err(this, "Download", id, "URL kosong");
      failDownloadStart(id, "URL kosong");
      return;
    }
    if (id < 1) {
      Shared.err(this, "Download", id, "id harus >= 1 (pakai CreateDownloadId)");
      failDownloadStart(id, "id harus >= 1");
      return;
    }
    if (!Ids.register(Ids.DOWNLOAD, id)) {
      Shared.err(this, "Download", id, "ID " + id + " sedang dipakai operasi lain");
      failDownloadStart(id, "ID sedang dipakai");
      return;
    }
    synchronized (lock) {
      if (downloads.containsKey(Integer.valueOf(id))) {
        Shared.err(this, "Download", id, "ID " + id + " masih ada unduhan jalan "
            + "(SetCancel/Remove dulu)");
        failDownloadStart(id, "masih ada unduhan jalan");
        return;
      }
    }
    File dir = Shared.resolveRooted(folder);
    if (dir == null) {
      Ids.unregister(Ids.DOWNLOAD, id);
      Shared.err(this, "Download", id, "tidak bisa unduh ke Asset");
      failDownloadStart(id, "tidak bisa unduh ke Asset");
      return;
    }
    try {
      if (!dir.exists()) {
        dir.mkdirs();
      }
    } catch (Exception e) {
      Ids.unregister(Ids.DOWNLOAD, id);
      Shared.err(this, "Download", id, "folder tujuan tidak bisa dibuat");
      failDownloadStart(id, "folder tujuan tidak bisa dibuat");
      return;
    }
    final DlOp op = new DlOp();
    op.id = id;
    op.url = u;
    op.dest = dir;
    op.fileName = Shared.clean(fileName);
    synchronized (lock) {
      downloads.put(Integer.valueOf(id), op);
    }
    Ids.setActive(Ids.DOWNLOAD, id, true);
    DlInfo ni = infoOf(id);
    ni.status = "jalan";
    ni.attempts = ni.attempts + 1;
    ni.percent = 0;
    ni.received = 0;
    ni.total = 0;
    ni.success = false;
    ni.savePath = "";
    ni.fileName = Shared.clean(fileName);
    ni.message = "";
    ni.url = u;
    ni.folder = dir.getAbsolutePath();
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        if (!Shared.ensurePermission(form, activity, Shared.PERM_WRITE_STORAGE)) {
          final String permMsg = "izin belum granted (Android 11+: aktifkan 'Akses semua file')";
          Shared.err(Tools_Downloader.this, "Download", op.id, permMsg);
          failDownloadStart(op.id, permMsg);
          return;
        }
        File srcLocal = localSourceFile(op.url);
        if (isDataUrl(op.url)) {
          runDataDownload(op);
        } else if (isContentUri(op.url)) {
          runContentDownload(op);
        } else if (srcLocal != null && srcLocal.isFile()) {
          runFileCopyDownload(op, srcLocal);
        } else {
          runDownload(op);
        }
      }
    });
  }

  /** Gagal mulai: beri feedback Finished(false) agar Call selalu bersuara. */
  private void failDownloadStart(final int fId, final String msg) {
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          DownloadFinished(fId, false, "", "", msg);
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  /** True bila url adalah data: URL (base64 / teks url-encoded). */
  private static boolean isDataUrl(String u) {
    return u != null && u.regionMatches(true, 0, "data:", 0, 5);
  }

  /** True bila url adalah content: URI Android (mis. dari file picker). */
  private static boolean isContentUri(String u) {
    return u != null && u.regionMatches(true, 0, "content:", 0, 8);
  }

  /** Sumber file lokal untuk CallDownload: dukung file: URL dan path lokal
   *  (relatif di-root /storage/emulated/0, absolut apa adanya).
   *  Kembali null bila bukan pola lokal (lanjut ke unduh http). */
  private static File localSourceFile(String u) {
    if (u == null) {
      return null;
    }
    if (u.regionMatches(true, 0, "file:", 0, 5)) {
      String p = u.substring(5);
      while (p.startsWith("//")) {
        p = p.substring(2);
      }
      if (p.regionMatches(true, 0, "localhost/", 0, 10)) {
        p = p.substring(10);
      }
      if (p.length() == 0) {
        return null;
      }
      try {
        if (p.indexOf('%') >= 0) {
          p = URLDecoder.decode(p, "UTF-8");
        }
      } catch (Exception e) {
        // pakai mentah
      }
      return new File(p);
    }
    if (u.indexOf("://") < 0) {
      try {
        return Shared.resolveRooted(u);
      } catch (Exception e) {
        return null;
      }
    }
    return null;
  }

  /** Salin file lokal (file: URL / path) ke folder tujuan.
   *  Alur event/notif sama seperti unduh http (Started -> Finished). */
  private void runFileCopyDownload(DlOp op, File src) {
    File outFile = null;
    InputStream in = null;
    try {
      String name = op.fileName;
      if (name.length() == 0) {
        name = src.getName();
      } else {
        name = Shared.sanitize(name);
      }
      if (op.cancel) {
        finishDownload(op, false, "", "", "Dibatalkan");
        return;
      }
      op.fileName = name;
      outFile = Shared.uniqueFile(op.dest, name);
      final long total = src.length();
      final int fId = op.id;
      final String fName = name;
      final String fPath = outFile.getAbsolutePath();
      try {
        form.runOnUiThread(new Runnable() {
          public void run() {
            DownloadStarted(fId, fName, fPath);
          }
        });
      } catch (Exception e) {
        // abaikan
      }
      updateNotifFor(fId);
      DlInfo si = infoOf(fId);
      si.status = "jalan";
      si.fileName = fName;
      si.savePath = fPath;
      in = new FileInputStream(src);
      FileOutputStream out = new FileOutputStream(outFile);
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
            final int pPct = pct;
            final long pDone = done;
            try {
              form.runOnUiThread(new Runnable() {
                public void run() {
                  DownloadProgressChanged(fId, pPct, pDone, total);
                }
              });
            } catch (Exception e) {
              // abaikan
            }
            updateNotifFor(fId);
            DlInfo pi = infoOf(fId);
            pi.percent = pPct;
            pi.received = pDone;
            pi.total = total;
          }
        }
        out.flush();
      } finally {
        try {
          in.close();
        } catch (Exception e) {
          // abaikan
        }
        try {
          out.close();
        } catch (Exception e) {
          // abaikan
        }
      }
      finishDownload(op, true, fPath, fName, "OK");
    } catch (Exception e) {
      String m = e.getMessage();
      finishDownload(op, false, "", "", m == null ? "salin file gagal" : m);
      if (outFile != null) {
        try {
          outFile.delete();
        } catch (Exception e2) {
          // abaikan
        }
      }
    }
  }

  /** Simpan data: URL (mis. data:image/png;base64,...) ke folder tujuan.
   *  Alur event/notif sama seperti unduh http (Started -> Finished). */
  private void runDataDownload(DlOp op) {
    File outFile = null;
    try {
      String u = op.url;
      int comma = u.indexOf(',');
      if (comma < 0) {
        finishDownload(op, false, "", "", "data URL tidak valid");
        return;
      }
      String meta = u.substring(5, comma);
      String payload = u.substring(comma + 1);
      boolean isBase64 = meta.toLowerCase().indexOf(";base64") >= 0;
      String mime = meta;
      int sc = meta.indexOf(';');
      if (sc >= 0) {
        mime = meta.substring(0, sc);
      }
      mime = mime.trim().toLowerCase();
      byte[] data;
      if (isBase64) {
        String b = payload.replaceAll("\\s", "");
        if (b.length() == 0) {
          finishDownload(op, false, "", "", "data base64 kosong");
          return;
        }
        try {
          data = android.util.Base64.decode(b, android.util.Base64.DEFAULT);
        } catch (Exception e) {
          finishDownload(op, false, "", "", "base64 tidak valid");
          return;
        }
      } else {
        try {
          String dec = URLDecoder.decode(payload, "UTF-8");
          data = dec.getBytes("UTF-8");
        } catch (Exception e) {
          finishDownload(op, false, "", "", "data URL tidak valid");
          return;
        }
      }
      if (op.cancel) {
        finishDownload(op, false, "", "", "Dibatalkan");
        return;
      }
      String name = op.fileName;
      if (name.length() == 0) {
        String ext = Shared.extForMime(mime);
        name = ext.length() == 0 ? "datafile" : "datafile." + ext;
      } else {
        name = Shared.sanitize(name);
      }
      op.fileName = name;
      outFile = Shared.uniqueFile(op.dest, name);
      final int fId = op.id;
      final String fName = name;
      final String fPath = outFile.getAbsolutePath();
      final byte[] fData = data;
      try {
        form.runOnUiThread(new Runnable() {
          public void run() {
            DownloadStarted(fId, fName, fPath);
          }
        });
      } catch (Exception e) {
        // abaikan
      }
      updateNotifFor(fId);
      DlInfo si = infoOf(fId);
      si.status = "jalan";
      si.fileName = fName;
      si.savePath = fPath;
      FileOutputStream out = new FileOutputStream(outFile);
      try {
        out.write(fData);
        out.flush();
      } finally {
        try {
          out.close();
        } catch (Exception e) {
          // abaikan
        }
      }
      DlInfo pi = infoOf(fId);
      pi.percent = 100;
      pi.received = fData.length;
      pi.total = fData.length;
      try {
        form.runOnUiThread(new Runnable() {
          public void run() {
            DownloadProgressChanged(fId, 100, fData.length, fData.length);
          }
        });
      } catch (Exception e) {
        // abaikan
      }
      updateNotifFor(fId);
      finishDownload(op, true, fPath, fName, "OK");
    } catch (Exception e) {
      String m = e.getMessage();
      finishDownload(op, false, "", "", m == null ? "simpan data gagal" : m);
      if (outFile != null) {
        try {
          outFile.delete();
        } catch (Exception e2) {
          // abaikan
        }
      }
    }
  }

  /** Salin content: URI (mis. dari file picker/galeri) ke folder tujuan.
   *  Alur event/notif sama seperti unduh http (Started -> Finished). */
  private void runContentDownload(DlOp op) {
    File outFile = null;
    InputStream in = null;
    try {
      Uri uri = Uri.parse(op.url);
      String name = op.fileName;
      if (name.length() == 0) {
        String display = null;
        try {
          Cursor cu = context.getContentResolver().query(uri, null, null,
              null, null);
          if (cu != null) {
            try {
              int idx = cu.getColumnIndex(OpenableColumns.DISPLAY_NAME);
              if (idx >= 0 && cu.moveToFirst()) {
                display = cu.getString(idx);
              }
            } finally {
              cu.close();
            }
          }
        } catch (Exception e) {
          // abaikan, pakai tebakan
        }
        if (display != null && display.length() > 0) {
          name = Shared.sanitize(display);
        } else {
          String mime = null;
          try {
            mime = context.getContentResolver().getType(uri);
          } catch (Exception e) {
            mime = null;
          }
          String ext = Shared.extForMime(mime);
          name = ext.length() == 0 ? "contentfile" : "contentfile." + ext;
        }
      } else {
        name = Shared.sanitize(name);
      }
      if (op.cancel) {
        finishDownload(op, false, "", "", "Dibatalkan");
        return;
      }
      op.fileName = name;
      outFile = Shared.uniqueFile(op.dest, name);
      final int fId = op.id;
      final String fName = name;
      final String fPath = outFile.getAbsolutePath();
      try {
        form.runOnUiThread(new Runnable() {
          public void run() {
            DownloadStarted(fId, fName, fPath);
          }
        });
      } catch (Exception e) {
        // abaikan
      }
      updateNotifFor(fId);
      DlInfo si = infoOf(fId);
      si.status = "jalan";
      si.fileName = fName;
      si.savePath = fPath;
      try {
        in = context.getContentResolver().openInputStream(uri);
      } catch (Exception e) {
        in = null;
      }
      if (in == null) {
        finishDownload(op, false, "", "", "tidak bisa buka content URI");
        try {
          outFile.delete();
        } catch (Exception e) {
          // abaikan
        }
        return;
      }
      FileOutputStream out = new FileOutputStream(outFile);
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
          long now = System.currentTimeMillis();
          if (now - lastFire >= PROGRESS_MIN_MS) {
            lastFire = now;
            final long pDone = done;
            try {
              form.runOnUiThread(new Runnable() {
                public void run() {
                  DownloadProgressChanged(fId, 0, pDone, -1);
                }
              });
            } catch (Exception e) {
              // abaikan
            }
            updateNotifFor(fId);
            DlInfo pi = infoOf(fId);
            pi.received = pDone;
          }
        }
        out.flush();
      } finally {
        try {
          in.close();
        } catch (Exception e) {
          // abaikan
        }
        try {
          out.close();
        } catch (Exception e) {
          // abaikan
        }
      }
      finishDownload(op, true, fPath, fName, "OK");
    } catch (Exception e) {
      String m = e.getMessage();
      finishDownload(op, false, "", "", m == null ? "salin content gagal" : m);
      if (outFile != null) {
        try {
          outFile.delete();
        } catch (Exception e2) {
          // abaikan
        }
      }
    }
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
        name = guessFileName(op.url, mime == null ? "" : mime,
            disp == null ? "" : disp);
      } else {
        name = Shared.sanitize(name);
      }
      op.fileName = name;
      long total = contentLen(c);
      outFile = Shared.uniqueFile(op.dest, name);
      final int fId = op.id;
      final String fName = name;
      final String fPath = outFile.getAbsolutePath();
      try {
        form.runOnUiThread(new Runnable() {
          public void run() {
            DownloadStarted(fId, fName, fPath);
          }
        });
      } catch (Exception e) {
        // abaikan
      }
      updateNotifFor(fId);
      DlInfo si = infoOf(fId);
      si.status = "jalan";
      si.fileName = fName;
      si.savePath = fPath;
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
              final int pPct = pct;
              final long pDone = done;
              final long pTotal = total;
              try {
                form.runOnUiThread(new Runnable() {
                  public void run() {
                    DownloadProgressChanged(fId, pPct, pDone, pTotal);
                  }
                });
              } catch (Exception e) {
                // abaikan
              }
              updateNotifFor(fId);
              DlInfo pi = infoOf(fId);
              pi.percent = pPct;
              pi.received = pDone;
              pi.total = pTotal;
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
    Ids.setActive(Ids.DOWNLOAD, op.id, false);
    DlInfo fi = infoOf(op.id);
    fi.success = success;
    fi.savePath = path;
    fi.fileName = name;
    fi.message = msg;
    fi.status = success ? "selesai" : ("Dibatalkan".equals(msg) ? "batal" : "gagal");
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          DownloadFinished(op.id, success, path, name, msg);
        }
      });
    } catch (Exception e) {
      DownloadFinished(op.id, success, path, name, msg);
    }
    updateNotifFor(op.id);
    if (!success && !"Dibatalkan".equals(msg)) {
      Shared.err(this, "Download", op.id, msg);
    }
    batchCount(success);
  }

  private void batchCount(boolean ok) {
    boolean fire = false;
    int total = 0;
    int okCount = 0;
    String folder = "";
    synchronized (lock) {
      if (dlBatchActive) {
        dlBatchDone = dlBatchDone + 1;
        if (ok) {
          dlBatchOk = dlBatchOk + 1;
        }
        if (dlBatchDone >= dlBatchTotal) {
          fire = true;
          total = dlBatchTotal;
          okCount = dlBatchOk;
          folder = dlBatchFolder;
          dlBatchActive = false;
        }
      }
    }
    if (fire) {
      final int fTotal = total;
      final int fOk = okCount;
      final String fFolder = folder;
      try {
        form.runOnUiThread(new Runnable() {
          public void run() {
            DownloadBatchFinished(fTotal, fOk, fFolder);
          }
        });
      } catch (Exception e) {
        DownloadBatchFinished(fTotal, fOk, fFolder);
      }
    }
  }

  @SimpleFunction(description = "CALL: unduh BANYAK URL sekaligus (tanpa loop manual). "
      + "urls = list teks URL (atau satu teks URL); fileNames = list nama (atau satu teks; "
      + "boleh kosong = otomatis semua; yang kurang dari jumlah URL = otomatis). "
      + "Contoh: urls = [url1, url2], fileNames = kosongkan. folder seperti CallDownload. "
      + "Tanpa return: id tiap URL ketahuan saat DownloadStarted menyala (cocokkan via "
      + "fileName/savePath yang dibawanya). Pantau tiap id lewat "
      + "DownloadStarted/DownloadProgressChanged/DownloadFinished. Ringkasan lewat "
      + "DownloadBatchFinished. Satu batch per saat. Tiap id batch bisa dipasang "
      + "SetDownloadNotification satu-satu (notif per file).")
  public void CallDownloadList(Object urls, Object fileNames,
      String folder) {
    int started = 0;
    synchronized (lock) {
      if (dlBatchActive) {
        Shared.err(this, "Download", 0, "masih ada batch unduhan berjalan");
        return;
      }
    }
    List<String> us = toStrList(urls);
    List<String> ns = toStrList(fileNames);
    if (us.isEmpty()) {
      Shared.err(this, "Download", 0, "list URL kosong");
      return;
    }
    File dir = Shared.resolveRooted(folder);
    if (dir == null) {
      Shared.err(this, "Download", 0, "tidak bisa unduh ke Asset");
      return;
    }
    synchronized (lock) {
      dlBatchActive = true;
      dlBatchTotal = us.size();
      dlBatchDone = 0;
      dlBatchOk = 0;
      dlBatchFolder = dir.getAbsolutePath();
    }
    final int fTotal = us.size();
    final String fFolder = dir.getAbsolutePath();
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          DownloadBatchStarted(fTotal, fFolder);
        }
      });
    } catch (Exception e) {
      // abaikan
    }
    for (int i = 0; i < us.size(); i = i + 1) {
      String fn = i < ns.size() ? ns.get(i) : "";
      int id = Ids.createFor(Ids.DOWNLOAD);
      if (id < 1) {
        synchronized (lock) {
          dlBatchTotal = dlBatchTotal - 1;
          if (dlBatchDone >= dlBatchTotal) {
            dlBatchActive = false;
          }
        }
        continue;
      }
      Ids.unregister(Ids.DOWNLOAD, id);
      started = started + 1;
      CallDownload(us.get(i), fn, folder, id);
    }
    if (started == 0) {
      synchronized (lock) {
        dlBatchActive = false;
      }
      Shared.err(this, "Download", 0, "tidak ada URL yang bisa mulai");
    }
  }

  private List<String> toStrList(Object o) {
    ArrayList<String> out = new ArrayList<String>();
    try {
      if (o == null) {
        return out;
      }
      if (o instanceof List) {
        List<?> l = (List<?>) o;
        for (int i = 0; i < l.size(); i = i + 1) {
          Object x = l.get(i);
          if (x != null) {
            out.add(x.toString());
          }
        }
      } else {
        out.add(o.toString());
      }
    } catch (Exception e) {
      // abaikan
    }
    return out;
  }

  @SimpleFunction(description = "SET: Batalkan unduhan berjalan. File sebagian dihapus. "
      + "Umpan balik via DownloadFinished(success=false). Tanpa event sendiri.")
  public void SetCancelDownload(int id) {
    cancelStatic(id);
  }

  /** Batalkan unduhan (id tetap terdaftar, bisa dipakai lagi). Dipakai juga
   *  oleh Tools_Base RemoveDownloadId. */
  public static void cancelStatic(int id) {
    DlOp op;
    synchronized (lock) {
      op = downloads.get(Integer.valueOf(id));
    }
    if (op == null) {
      return;
    }
    op.cancel = true;
    synchronized (op.pauseLock) {
      op.paused = false;
      op.pauseLock.notifyAll();
    }
  }

  @SimpleFunction(description = "SET: Jeda unduhan berjalan. Unduhan berhenti menulis, "
      + "lanjut dari posisi sama saat Resume (BUKAN ulang dari 0). Verifikasi via "
      + "IsDownloadPaused / GetDownloadStatus (status jadi jeda). Tanpa feedback.")
  public void SetPauseDownload(int id) {
    DlOp op;
    synchronized (lock) {
      op = downloads.get(Integer.valueOf(id));
    }
    if (op == null) {
      return;
    }
    op.paused = true;
    Ids.setActive(Ids.DOWNLOAD, id, false);
    infoOf(id).status = "jeda";
  }

  @SimpleFunction(description = "SET: Lanjutkan unduhan yang dijeda (lanjut dari posisi "
      + "jeda, tanpa unduh ulang). Verifikasi via IsDownloadPaused / GetDownloadStatus "
      + "(status kembali jalan). Tanpa feedback.")
  public void SetResumeDownload(int id) {
    DlOp op;
    synchronized (lock) {
      op = downloads.get(Integer.valueOf(id));
    }
    if (op == null) {
      return;
    }
    synchronized (op.pauseLock) {
      op.paused = false;
      op.pauseLock.notifyAll();
    }
    Ids.setActive(Ids.DOWNLOAD, id, true);
    infoOf(id).status = "jalan";
  }

  @SimpleFunction(description = "True bila id unduhan ini sedang dijeda (Pause tapi "
      + "belum Cancel/selesai). False = jalan/selesai/gagal/batal/belum ada.")
  public boolean IsDownloadPaused(int id) {
    synchronized (lock) {
      DlInfo i = infos.get(Integer.valueOf(id));
      return i != null && "jeda".equals(i.status);
    }
  }

  @SimpleFunction(description = "Persen unduhan id ini (0-100). = index 1 dari "
      + "GetDownloadProgress.")
  public int GetDownloadPercent(int id) {
    synchronized (lock) {
      DlInfo i = infos.get(Integer.valueOf(id));
      return i == null ? 0 : i.percent;
    }
  }

  @SimpleFunction(description = "Path file tujuan id ini (kosong bila belum mulai). "
      + "= index 2 dari GetDownloadResult.")
  public String GetDownloadSavePath(int id) {
    synchronized (lock) {
      DlInfo i = infos.get(Integer.valueOf(id));
      return i == null ? "" : i.savePath;
    }
  }

  @SimpleFunction(description = "Nama file id ini (kosong bila belum mulai). "
      + "= index 3 dari GetDownloadResult.")
  public String GetDownloadFileName(int id) {
    synchronized (lock) {
      DlInfo i = infos.get(Integer.valueOf(id));
      return i == null ? "" : i.fileName;
    }
  }

  @SimpleFunction(description = "Jumlah percobaan unduh id ini (tiap mulai/ulang +1, "
      + "termasuk CallRetryDownload; 0 bila id belum pernah mengunduh).")
  public int GetDownloadAttempts(int id) {
    synchronized (lock) {
      DlInfo i = infos.get(Integer.valueOf(id));
      return i == null ? 0 : i.attempts;
    }
  }

  @SimpleFunction(description = "CALL: Unduh ulang id ini dari awal (byte dihitung ulang "
      + "dari 0). Untuk id yang gagal/dibatalkan/selesai. Id yang masih jalan harus "
      + "di-Cancel dulu. Hasil lewat DownloadFinished seperti biasa.")
  public void CallRetryDownload(int id) {
    String u;
    String fn;
    String folder;
    synchronized (lock) {
      DlInfo i = infos.get(Integer.valueOf(id));
      if (i == null || i.url == null || i.url.length() == 0) {
        Shared.err(this, "Download", id, "id belum pernah mengunduh");
        return;
      }
      u = i.url;
      fn = i.fileName;
      folder = i.folder;
    }
    synchronized (lock) {
      if (downloads.containsKey(Integer.valueOf(id))) {
        Shared.err(this, "Download", id, "masih ada unduhan jalan (Cancel dulu)");
        return;
      }
    }
    cancelStatic(id);
    Ids.setActive(Ids.DOWNLOAD, id, false);
    CallDownload(u, fn, folder, id);
  }

  @SimpleFunction(description = "Total URL batch yang sedang jalan (0 bila tidak ada batch).")
  public int GetBatchTotal() {
    synchronized (lock) {
      return dlBatchTotal;
    }
  }

  @SimpleFunction(description = "Jumlah URL batch yang sudah selesai (sukses/gagal).")
  public int GetBatchDone() {
    synchronized (lock) {
      return dlBatchDone;
    }
  }

  @SimpleFunction(description = "Jumlah URL batch yang sukses.")
  public int GetBatchOk() {
    synchronized (lock) {
      return dlBatchOk;
    }
  }

  // ================= ID =================

  @SimpleFunction(description = "Buat id unduhan unik baru (Number, siap pakai ke CallDownload). "
      + "Urutan pembuatan menentukan urutan list.")
  public int CreateDownloadId() {
    int id = Ids.createFor(Ids.DOWNLOAD);
    if (id < 1) {
      Shared.err(this, "Download", 0, "gagal buat id");
    }
    return id;
  }

  @SimpleFunction(description = "Semua id unduhan yang dipegang (URUT PEMBUATAN, "
      + "index 1 = pertama dibuat). Termasuk yang dijeda.")
  public List<Integer> GetDownloadIds() {
    return Ids.idsOf(Ids.DOWNLOAD);
  }

  @SimpleFunction(description = "Id unduhan yang sedang jalan (URUT PEMBUATAN). "
      + "Yang dijeda TIDAK masuk sini.")
  public List<Integer> GetActiveDownloadIds() {
    return Ids.activeIdsOf(Ids.DOWNLOAD);
  }

  @SimpleFunction(description = "Id unduhan yang terakhir disentuh (dibuat atau di-start). "
      + "-1 bila kosong. Beda dengan GetLastDownloadId (terakhir dibuat).")
  public int CurrentDownloadId() {
    return Ids.currentOf(Ids.DOWNLOAD);
  }

  @SimpleFunction(description = "Id index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstDownloadId() {
    return Ids.firstOf(Ids.DOWNLOAD);
  }

  @SimpleFunction(description = "Id unduhan TERAKHIR DIBUAT. -1 bila kosong. Beda dengan "
      + "CurrentDownloadId (terakhir disentuh).")
  public int GetLastDownloadId() {
    return Ids.lastOf(Ids.DOWNLOAD);
  }

  @SimpleFunction(description = "Id yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.")
  public int GetPreviousDownloadId(int id) {
    return Ids.previousOf(Ids.DOWNLOAD, id);
  }

  @SimpleFunction(description = "True bila id unduhan ini sedang jalan (tidak dijeda). "
      + "Bedakan jeda vs berhenti via IsDownloadPaused.")
  public boolean IsDownloadIdActive(int id) {
    return Ids.isActive(Ids.DOWNLOAD, id);
  }

  @SimpleFunction(description = "Hapus id unduhan: batalkan bila jalan + lepas id "
      + "(otomatis beres-beres).")
  public void RemoveDownloadId(int id) {
    cancelStatic(id);
    Ids.setActive(Ids.DOWNLOAD, id, false);
    Ids.unregister(Ids.DOWNLOAD, id);
    synchronized (lock) {
      infos.remove(Integer.valueOf(id));
      notifOn.remove(Integer.valueOf(id));
    }
    cancelNotif(id);
  }

  // ================= NOTIFIKASI =================

  private static final Map<Integer, Boolean> notifOn = new HashMap<Integer, Boolean>();

  private static boolean notifEnabled(int id) {
    synchronized (lock) {
      Boolean b = notifOn.get(Integer.valueOf(id));
      return b != null && b.booleanValue();
    }
  }

  @SimpleFunction(description = "SET: tampilkan progres unduhan di bar notifikasi "
      + "Android (otomatis: nama file + persen). show=true = tampil + update "
      + "tiap Started/Progress/Finished; show=false = matikan + hapus notif id itu. "
      + "Pasang sesudah CallDownload, atau di dalam event DownloadStarted/"
      + "ProgressChanged (id sudah terdaftar; di Finished kurang relevan). "
      + "Tiap id hasil CallDownloadList bisa dipasang satu-satu (notif per file). "
      + "Android 13+ butuh izin POST_NOTIFICATIONS (minta via blok CallSetNotificationPermission Tools_Sheet "
      + "ATAU CallRequestPermission Tools_Base). Tanpa feedback.")
  public void SetDownloadNotification(int id, boolean show) {
    synchronized (lock) {
      if (show) {
        notifOn.put(Integer.valueOf(id), Boolean.valueOf(true));
      } else {
        notifOn.remove(Integer.valueOf(id));
      }
    }
    if (!show) {
      cancelNotif(id);
      return;
    }
    updateNotifFor(id);
  }

  private void updateNotifFor(int id) {
    if (!notifEnabled(id)) {
      return;
    }
    String status;
    int pct;
    long recv;
    long tot;
    String fn;
    String msg;
    synchronized (lock) {
      DlInfo i = infos.get(Integer.valueOf(id));
      if (i == null) {
        return;
      }
      status = i.status;
      pct = i.percent;
      recv = i.received;
      tot = i.total;
      fn = i.fileName;
      msg = i.message;
    }
    String title = (fn == null || fn.length() == 0) ? "Mengunduh..." : fn;
    if ("jalan".equals(status)) {
      String txt = pct + "%";
      if (tot > 0) {
        txt = txt + " - " + fmtBytes(recv) + "/" + fmtBytes(tot);
      }
      postNotif(id, title, txt, pct, false);
    } else if ("jeda".equals(status)) {
      postNotif(id, title, "Dijeda - " + pct + "%", pct, false);
    } else if ("selesai".equals(status)) {
      postNotif(id, title, "Unduhan selesai", 100, true);
    } else if ("batal".equals(status)) {
      postNotif(id, title, "Dibatalkan", pct, true);
    } else if ("gagal".equals(status)) {
      postNotif(id, title, "Gagal: " + (msg == null ? "" : msg), pct, true);
    }
  }

  private static String fmtBytes(long n) {
    if (n < 1024L) {
      return n + " B";
    }
    if (n < 1048576L) {
      return String.format("%.1f KB", Double.valueOf(n / 1024.0));
    }
    if (n < 1073741824L) {
      return String.format("%.1f MB", Double.valueOf(n / 1048576.0));
    }
    return String.format("%.2f GB", Double.valueOf(n / 1073741824.0));
  }

  private void postNotif(int id, String title, String text, int pct, boolean done) {
    try {
      android.app.NotificationManager nm = (android.app.NotificationManager)
          context.getSystemService(Context.NOTIFICATION_SERVICE);
      if (nm == null) {
        return;
      }
      String ch = "fv7_download";
      if (android.os.Build.VERSION.SDK_INT >= 26) {
        try {
          android.app.NotificationChannel c = new android.app.NotificationChannel(
              ch, "Unduhan", android.app.NotificationManager.IMPORTANCE_LOW);
          nm.createNotificationChannel(c);
        } catch (Exception e) {
          // abaikan
        }
      }
      android.app.Notification.Builder b;
      if (android.os.Build.VERSION.SDK_INT >= 26) {
        b = new android.app.Notification.Builder(context, ch);
      } else {
        b = new android.app.Notification.Builder(context);
      }
      b.setContentTitle(title).setContentText(text).setOngoing(!done)
          .setAutoCancel(done).setOnlyAlertOnce(true);
      try {
        b.setSmallIcon(done ? android.R.drawable.stat_sys_download_done
            : android.R.drawable.stat_sys_download);
      } catch (Exception e) {
        // abaikan
      }
      if (!done) {
        try {
          int p = pct < 0 ? 0 : (pct > 100 ? 100 : pct);
          b.setProgress(100, p, pct <= 0);
        } catch (Exception e) {
          // abaikan
        }
      }
      nm.notify(id, b.build());
    } catch (Exception e) {
      // abaikan (mis. izin notifikasi belum diberikan)
    }
  }

  private void cancelNotif(int id) {
    try {
      android.app.NotificationManager nm = (android.app.NotificationManager)
          context.getSystemService(Context.NOTIFICATION_SERVICE);
      if (nm != null) {
        nm.cancel(id);
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "Fallback polling untuk event Started/Progress/"
      + "Finished: status unduhan ber-id. Salah satu: jalan, jeda, selesai, "
      + "gagal, batal, atau tidak ada.")
  public String GetDownloadStatus(int id) {
    synchronized (lock) {
      DlInfo i = infos.get(Integer.valueOf(id));
      return i == null ? "tidak ada" : i.status;
    }
  }

  @SimpleFunction(description = "Fallback polling untuk event "
      + "DownloadProgressChanged: list [persen 0-100, byte diterima, byte total]. "
      + "Contoh ambil persen: select list item (hasil blok ini) index 1. "
      + "Jalan pintas: pakai GetDownloadPercent.")
  public List<Object> GetDownloadProgress(int id) {
    ArrayList<Object> out = new ArrayList<Object>();
    synchronized (lock) {
      DlInfo i = infos.get(Integer.valueOf(id));
      if (i == null) {
        out.add(Integer.valueOf(0));
        out.add(Long.valueOf(0));
        out.add(Long.valueOf(0));
        return out;
      }
      out.add(Integer.valueOf(i.percent));
      out.add(Long.valueOf(i.received));
      out.add(Long.valueOf(i.total));
    }
    return out;
  }

  @SimpleFunction(description = "Fallback polling untuk event DownloadFinished: "
      + "list [success, savePath, fileName, message]. Contoh ambil nama: select list item "
      + "(hasil blok ini) index 3. Jalan pintas: GetDownloadSavePath/GetDownloadFileName. "
      + "Tetap ada setelah selesai sampai RemoveDownloadId.")
  public List<Object> GetDownloadResult(int id) {
    ArrayList<Object> out = new ArrayList<Object>();
    synchronized (lock) {
      DlInfo i = infos.get(Integer.valueOf(id));
      if (i == null) {
        out.add(Boolean.valueOf(false));
        out.add("");
        out.add("");
        out.add("tidak ada");
        return out;
      }
      out.add(Boolean.valueOf(i.success));
      out.add(i.savePath);
      out.add(i.fileName);
      out.add(i.message);
    }
    return out;
  }

  @SimpleFunction(description = "Fallback polling untuk event batch: list "
      + "[batchJalan, total, selesai, ok]. Contoh ambil total: select list item "
      + "(hasil blok ini) index 2. Jalan pintas: GetBatchTotal/GetBatchDone/GetBatchOk.")
  public List<Object> GetBatchProgress() {
    ArrayList<Object> out = new ArrayList<Object>();
    synchronized (lock) {
      out.add(Boolean.valueOf(dlBatchActive));
      out.add(Integer.valueOf(dlBatchTotal));
      out.add(Integer.valueOf(dlBatchDone));
      out.add(Integer.valueOf(dlBatchOk));
    }
    return out;
  }

  @SimpleFunction(description = "Fallback polling untuk event ErrorOccurred: pesan error "
      + "terakhir operasi ini (teks kosong bila belum ada). Isi operation dengan blok OpDownload.")
  public String LastError(String operation) {
    return Shared.lastError(operation);
  }

  @SimpleFunction(description = "CALL: Tebak nama file dari URL saja (1 input, "
      + "mandiri — bisa disandingkan komponen lain, mis. Web bawaan MIT untuk "
      + "rename). http(s) = ambil header asli server (ikuti redirect); "
      + "data:/content:/file:/path = tebak lokal tanpa internet. Hasil lewat "
      + "FileNameGuessed (url + fileName + mimeType). Butuh potong path jadi nama? "
      + "pakai GetFileName Tools_File.")
  public void CallGuessFileName(final String url) {
    final String u = Shared.clean(url);
    if (u.length() == 0) {
      Shared.err(this, "Download", 0, "URL kosong");
      fireGuess(u, "", "");
      return;
    }
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        if (isDataUrl(u)) {
          int comma = u.indexOf(',');
          String meta = comma >= 0 ? u.substring(5, comma) : "";
          String mime = meta;
          int sc = meta.indexOf(';');
          if (sc >= 0) {
            mime = meta.substring(0, sc);
          }
          mime = mime.trim().toLowerCase();
          String ext = Shared.extForMime(mime);
          String name = ext.length() == 0 ? "datafile" : "datafile." + ext;
          fireGuess(u, name, mime);
          return;
        }
        if (isContentUri(u)) {
          String display = null;
          try {
            Cursor cu = context.getContentResolver().query(Uri.parse(u),
                null, null, null, null);
            if (cu != null) {
              try {
                int idx = cu.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0 && cu.moveToFirst()) {
                  display = cu.getString(idx);
                }
              } finally {
                cu.close();
              }
            }
          } catch (Exception e) {
            // abaikan
          }
          String mime = null;
          try {
            mime = context.getContentResolver().getType(Uri.parse(u));
          } catch (Exception e) {
            mime = null;
          }
          if (mime == null) {
            mime = "";
          }
          String name;
          if (display != null && display.length() > 0) {
            name = Shared.sanitize(display);
          } else {
            String ext = Shared.extForMime(mime);
            name = ext.length() == 0 ? "contentfile" : "contentfile." + ext;
          }
          fireGuess(u, name, mime);
          return;
        }
        File lf = localSourceFile(u);
        if (lf != null && lf.isFile()) {
          fireGuess(u, lf.getName(), Shared.mimeFor(lf.getName()));
          return;
        }
        HttpURLConnection c = null;
        try {
          c = (HttpURLConnection) new URL(u).openConnection();
          c.setConnectTimeout(15000);
          c.setReadTimeout(15000);
          c.setInstanceFollowRedirects(true);
          c.setRequestProperty("Accept-Encoding", "identity");
          c.setRequestProperty("User-Agent", "Mozilla/5.0");
          c.connect();
          String mime = c.getContentType();
          if (mime == null) {
            mime = "";
          }
          String disp = c.getHeaderField("Content-Disposition");
          if (disp == null) {
            disp = "";
          }
          String finalUrl = u;
          try {
            URL fu = c.getURL();
            if (fu != null) {
              finalUrl = fu.toString();
            }
          } catch (Exception e) {
            // pakai url asal
          }
          fireGuess(u, guessFileName(finalUrl, mime, disp), mime);
        } catch (Exception e) {
          String m = e.getMessage();
          if (m == null) {
            m = "tebak nama gagal";
          }
          Shared.err(Tools_Downloader.this, "Download", 0, m);
          fireGuess(u, guessFileName(u, "", ""), "");
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
    });
  }

  private void fireGuess(final String url, final String fileName,
      final String mime) {
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          FileNameGuessed(url, fileName, mime);
        }
      });
    } catch (Exception e) {
      FileNameGuessed(url, fileName, mime);
    }
  }

  @SimpleEvent(description = "Hasil CallGuessFileName: url asal + fileName "
      + "tebakan + mimeType (kosong bila tak diketahui).")
  public void FileNameGuessed(String url, String fileName, String mimeType) {
    Shared.fire(this, "FileNameGuessed", url, fileName,
        mimeType);
  }

  /** Tebak nama file dari URL/mime/disposition. Dipakai otomatis bila fileName
   *  CallDownload dikosongkan + oleh CallGuessFileName. */
  private static String guessFileName(String url, String mimeType, String contentDisposition) {
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
          while (s < contentDisposition.length()
              && contentDisposition.charAt(s) == ' ') {
            s = s + 1;
          }
          if (s < contentDisposition.length()
              && contentDisposition.charAt(s) == '"') {
            int e = contentDisposition.indexOf('"', s + 1);
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
    name = Shared.sanitize(name);
    if (name.indexOf('.') < 0) {
      String ext = Shared.extForMime(mimeType);
      if (ext.length() > 0) {
        name = name + "." + ext;
      }
    }
    return name;
  }

  private static String urlDecode(String s) {
    try {
      return URLDecoder.decode(s, "UTF-8");
    } catch (Exception e) {
      return s;
    }
  }

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

  // ================= EVENTS =================

  @SimpleEvent(description = "Unduhan dimulai: siapkan tampilan (nama + path tujuan).")
  public void DownloadStarted(int id, String fileName, String savePath) {
    Shared.fire(this, "DownloadStarted",
        Integer.valueOf(id), fileName, savePath);
  }

  @SimpleEvent(description = "Progres unduhan berubah: persen 0-100 + byte.")
  public void DownloadProgressChanged(int id, int percent, long receivedBytes,
      long totalBytes) {
    Shared.fire(this, "DownloadProgressChanged",
        Integer.valueOf(id), Integer.valueOf(percent), Long.valueOf(receivedBytes),
        Long.valueOf(totalBytes));
  }

  @SimpleEvent(description = "Unduhan selesai: success pertama, fileName paling belakang. "
      + "success=false = gagal/dibatalkan (lihat message + ErrorOccurred).")
  public void DownloadFinished(int id, boolean success, String savePath, String fileName,
      String message) {
    Shared.fire(this, "DownloadFinished",
        Integer.valueOf(id), Boolean.valueOf(success), savePath, fileName, message);
  }

  @SimpleEvent(description = "Batch unduhan dimulai: total URL + folder tujuan.")
  public void DownloadBatchStarted(int total, String folder) {
    Shared.fire(this, "DownloadBatchStarted",
        Integer.valueOf(total), folder);
  }

  @SimpleEvent(description = "Batch unduhan selesai semua: total + jumlah ok + folder.")
  public void DownloadBatchFinished(int total, int okCount, String folder) {
    Shared.fire(this, "DownloadBatchFinished",
        Integer.valueOf(total), Integer.valueOf(okCount), folder);
  }

  @SimpleEvent(description = "SATU-SATUNYA error Tools_Downloader. component = nama operasi "
      + "(isi dengan OpDownload untuk if). Contoh: jika component = OpDownload "
      + "maka tampilkan message.")
  public void ErrorOccurred(String component, int id, String message) {
    Shared.fire(this, "ErrorOccurred",
        component, Integer.valueOf(id), message);
  }

  // ================= OP =================

  @SimpleProperty(description = "Nama operasi Download untuk if di ErrorOccurred. Contoh: "
      + "colok ke perbandingan = dengan component dari event ErrorOccurred.",
      category = PropertyCategory.BEHAVIOR)
  public String OpDownload() {
    return "Download";
  }

  // ================= destroy =================

  public void onDestroy() {
    synchronized (lock) {
      for (DlOp op : downloads.values()) {
        op.cancel = true;
        synchronized (op.pauseLock) {
          op.paused = false;
          op.pauseLock.notifyAll();
        }
      }
    }
  }
}
