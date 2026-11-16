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
import com.google.appinventor.components.common.FileScope;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.OnDestroyListener;
import com.google.appinventor.components.runtime.util.AsynchUtil;

import java.io.File;
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
    version = 1,
    description = "Tools_Downloader: unduh paralel ber-id (pause/resume/cancel per id, "
        + "Started/ProgressChanged/Finished) + unduh batch list URL. Id dikelola "
        + "Tools_Base (CreateUniqueId) atau CreateDownloadId. List id URUT PEMBUATAN.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    androidMinSdk = 21,
    iconName = "aiwebres/tools_downloader.jpg")
@SimpleObject(external = true)
@UsesPermissions({"android.permission.INTERNET",
    "android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE"})
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

  public Tools_Downloader(ComponentContainer container) {
    super(container.$form());
    this.context = container.$context();
    this.activity = container.$context();
  }

  @SimpleFunction(description = "Unduh URL ke folder scope+path. fileName kosong = nama otomatis. "
      + "id = angka bebas yang belum dipakai (minta ke Tools_Base CreateUniqueId atau "
      + "CreateDownloadId). Id reservasi otomatis diadopsi. Kembali: id, atau -1 bila gagal mulai.")
  public int Download(String url, String fileName, FileScope scope, String path, int id) {
    String u = Shared.clean(url);
    if (u.length() == 0) {
      Shared.err(this, "Download", id, "URL kosong");
      return -1;
    }
    if (id < 1) {
      Shared.err(this, "Download", id, "id harus >= 1 (pakai CreateDownloadId)");
      return -1;
    }
    if (!Ids.register(Ids.DOWNLOAD, id)) {
      Shared.err(this, "Download", id, "ID " + id + " sedang dipakai operasi lain");
      return -1;
    }
    synchronized (lock) {
      if (downloads.containsKey(Integer.valueOf(id))) {
        Shared.err(this, "Download", id, "ID " + id + " masih ada unduhan jalan "
            + "(Cancel/Remove dulu)");
        return -1;
      }
    }
    File dir = Shared.resolve(context, scope, path);
    if (dir == null) {
      Ids.unregister(Ids.DOWNLOAD, id);
      Shared.err(this, "Download", id, "tidak bisa unduh ke Asset");
      return -1;
    }
    try {
      if (!dir.exists()) {
        dir.mkdirs();
      }
    } catch (Exception e) {
      Ids.unregister(Ids.DOWNLOAD, id);
      Shared.err(this, "Download", id, "folder tujuan tidak bisa dibuat");
      return -1;
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
        name = GuessFileName(op.url, mime == null ? "" : mime,
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
    Ids.unregister(Ids.DOWNLOAD, op.id);
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

  @SimpleFunction(description = "Unduh BANYAK URL sekaligus (tanpa loop manual). fileNames "
      + "boleh kosong (= otomatis semua). Kembali: list id URUT PEMBUATAN (pantau per id). "
      + "Ringkasan lewat DownloadBatchFinished. Satu batch per saat.")
  public List<Integer> DownloadList(Object urls, Object fileNames, FileScope scope,
      String folder) {
    ArrayList<Integer> out = new ArrayList<Integer>();
    synchronized (lock) {
      if (dlBatchActive) {
        Shared.err(this, "Download", 0, "masih ada batch unduhan berjalan");
        return out;
      }
    }
    List<String> us = toStrList(urls);
    List<String> ns = toStrList(fileNames);
    if (us.isEmpty()) {
      Shared.err(this, "Download", 0, "list URL kosong");
      return out;
    }
    File dir = Shared.resolve(context, scope, folder);
    if (dir == null) {
      Shared.err(this, "Download", 0, "tidak bisa unduh ke Asset");
      return out;
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
      int got = Download(us.get(i), fn, scope, folder, id);
      if (got >= 0) {
        out.add(Integer.valueOf(id));
      } else {
        synchronized (lock) {
          dlBatchTotal = dlBatchTotal - 1;
          if (dlBatchDone >= dlBatchTotal) {
            dlBatchActive = false;
          }
        }
      }
    }
    if (out.isEmpty()) {
      synchronized (lock) {
        dlBatchActive = false;
      }
      Shared.err(this, "Download", 0, "tidak ada URL yang bisa mulai");
    }
    return out;
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

  @SimpleFunction(description = "Batalkan unduhan berjalan (per id). File sebagian dihapus.")
  public void CancelDownload(int id) {
    cancelStatic(id);
  }

  /** Batalkan + lepas id. Dipakai juga oleh Tools_Base RemoveDownloadId. */
  public static void cancelStatic(int id) {
    DlOp op;
    synchronized (lock) {
      op = downloads.get(Integer.valueOf(id));
    }
    if (op == null) {
      Ids.unregister(Ids.DOWNLOAD, id);
      return;
    }
    op.cancel = true;
    synchronized (op.pauseLock) {
      op.paused = false;
      op.pauseLock.notifyAll();
    }
  }

  @SimpleFunction(description = "Jeda unduhan berjalan (per id). Lanjut via ResumeDownload.")
  public void PauseDownload(int id) {
    DlOp op;
    synchronized (lock) {
      op = downloads.get(Integer.valueOf(id));
    }
    if (op == null) {
      Shared.err(this, "Download", id, "tidak ada unduhan ID " + id);
      return;
    }
    op.paused = true;
    Ids.setActive(Ids.DOWNLOAD, id, false);
  }

  @SimpleFunction(description = "Lanjutkan unduhan yang dijeda (per id).")
  public void ResumeDownload(int id) {
    DlOp op;
    synchronized (lock) {
      op = downloads.get(Integer.valueOf(id));
    }
    if (op == null) {
      Shared.err(this, "Download", id, "tidak ada unduhan ID " + id);
      return;
    }
    synchronized (op.pauseLock) {
      op.paused = false;
      op.pauseLock.notifyAll();
    }
    Ids.setActive(Ids.DOWNLOAD, id, true);
  }

  // ================= ID =================

  @SimpleFunction(description = "Buat id unduhan unik baru (Number, siap pakai ke Download). "
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

  @SimpleFunction(description = "Id unduhan yang terakhir dibuat/di-start. -1 bila kosong.")
  public int CurrentDownloadId() {
    return Ids.currentOf(Ids.DOWNLOAD);
  }

  @SimpleFunction(description = "Id index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstDownloadId() {
    return Ids.firstOf(Ids.DOWNLOAD);
  }

  @SimpleFunction(description = "Id TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastDownloadId() {
    return Ids.lastOf(Ids.DOWNLOAD);
  }

  @SimpleFunction(description = "Id yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.")
  public int GetPreviousDownloadId(int id) {
    return Ids.previousOf(Ids.DOWNLOAD, id);
  }

  @SimpleFunction(description = "True bila id unduhan ini sedang jalan (tidak dijeda).")
  public boolean IsDownloadIdActive(int id) {
    return Ids.isActive(Ids.DOWNLOAD, id);
  }

  @SimpleFunction(description = "Hapus id unduhan: batalkan bila jalan + lepas id "
      + "(otomatis beres-beres).")
  public void RemoveDownloadId(int id) {
    cancelStatic(id);
    Ids.setActive(Ids.DOWNLOAD, id, false);
    Ids.unregister(Ids.DOWNLOAD, id);
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

  private String urlDecode(String s) {
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
    EventDispatcher.dispatchEvent(this, "DownloadStarted",
        Integer.valueOf(id), fileName, savePath);
  }

  @SimpleEvent(description = "Progres unduhan berubah: persen 0-100 + byte.")
  public void DownloadProgressChanged(int id, int percent, long receivedBytes,
      long totalBytes) {
    EventDispatcher.dispatchEvent(this, "DownloadProgressChanged",
        Integer.valueOf(id), Integer.valueOf(percent), Long.valueOf(receivedBytes),
        Long.valueOf(totalBytes));
  }

  @SimpleEvent(description = "Unduhan selesai: success pertama, fileName paling belakang. "
      + "success=false = gagal/dibatalkan (lihat message + ErrorOccurred).")
  public void DownloadFinished(int id, boolean success, String savePath, String fileName,
      String message) {
    EventDispatcher.dispatchEvent(this, "DownloadFinished",
        Integer.valueOf(id), Boolean.valueOf(success), savePath, fileName, message);
  }

  @SimpleEvent(description = "Batch unduhan dimulai: total URL + folder tujuan.")
  public void DownloadBatchStarted(int total, String folder) {
    EventDispatcher.dispatchEvent(this, "DownloadBatchStarted",
        Integer.valueOf(total), folder);
  }

  @SimpleEvent(description = "Batch unduhan selesai semua: total + jumlah ok + folder.")
  public void DownloadBatchFinished(int total, int okCount, String folder) {
    EventDispatcher.dispatchEvent(this, "DownloadBatchFinished",
        Integer.valueOf(total), Integer.valueOf(okCount), folder);
  }

  @SimpleEvent(description = "SATU-SATUNYA error Tools_Downloader. component = nama operasi "
      + "(bandingkan dengan blok OpDownload).")
  public void ErrorOccurred(String component, int id, String message) {
    EventDispatcher.dispatchEvent(this, "ErrorOccurred",
        component, Integer.valueOf(id), message);
  }

  // ================= OP =================

  @SimpleProperty(description = "Nama operasi Download (untuk if di ErrorOccurred).",
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
