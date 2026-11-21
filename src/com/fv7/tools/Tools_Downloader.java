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
    return u != null && u.regionMat