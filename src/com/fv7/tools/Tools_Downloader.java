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

  @SimpleFunction(description = "CALL: unduh URL ke folder scope. scope: keluaran helper "
      + "Scope (\"App:folder\"), teks (\"App\"/\"Shared\"/\"\"), atau absolut. fileName kosong = "
      + "nama otomatis. id = angka bebas yang belum dipakai (minta ke Tools_Base "
      + "CreateUniqueId atau CreateDownloadId). Hasil lewat DownloadStarted/"
      + "ProgressChanged/Finished (bawa id).")
  public void CallDownload(String url, String fileName, String folder,
      String scope, int id) {
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
    File dir = Shared.resolveScope(context, scope, folder);
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
    ni.percent = 0;
    ni.received = 0;
    ni.total = 0;
    ni.success = false;
    ni.savePath = "";
    ni.fileName = Shared.clean(fileName);
    ni.message = "";
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        runDownload(op);
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
        name = GetFileName(op.url, mime == null ? "" : mime,
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

  @SimpleFunction(description = "CALL: unduh BANYAK URL sekaligus (tanpa loop manual). fileNames "
      + "boleh kosong (= otomatis semua). scope seperti CallDownload. Kembali: list id "
      + "URUT PEMBUATAN (pantau per id). Ringkasan lewat DownloadBatchFinished. "
      + "Satu batch per saat.")
  public List<Integer> CallDownloadList(Object urls, Object fileNames,
      String folder, String scope) {
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
    File dir = Shared.resolveScope(context, scope, folder);
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
      out.add(Integer.valueOf(id));
      CallDownload(us.get(i), fn, folder, scope, id);
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

  @SimpleFunction(description = "SET: batalkan unduhan berjalan (per id). File sebagian dihapus. Tanpa feedback.")
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

  @SimpleFunction(description = "SET: jeda unduhan berjalan (per id). Lanjut via SetResumeDownload. Tanpa feedback.")
  public void SetPauseDownload(int id) {
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
    infoOf(id).status = "jeda";
  }

  @SimpleFunction(description = "SET: lanjutkan unduhan yang dijeda (per id). Tanpa feedback.")
  public void SetResumeDownload(int id) {
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
    infoOf(id).status = "jalan";
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
    synchronized (lock) {
      infos.remove(Integer.valueOf(id));
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
      + "DownloadProgressChanged: list [persen 0-100, byte diterima, byte total].")
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
      + "list [success, savePath, fileName, message]. Tetap ada setelah selesai "
      + "sampai RemoveDownloadId.")
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
      + "[batchJalan, total, selesai, ok].")
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

  @SimpleFunction(description = "Fallback polling untuk event ErrorOccurred: "
      + "pesan error terakhir operasi ini (kosong bila belum ada).")
  public String LastError(String operation) {
    return Shared.lastError(operation);
  }

  @SimpleFunction(description = "GET: nama file dari URL, mime, dan content-disposition "
      + "(dipakai otomatis bila fileName CallDownload dikosongkan).")
  public String GetFileName(String url, String mimeType, String contentDisposition) {
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
