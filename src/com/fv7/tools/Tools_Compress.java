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
import com.google.appinventor.components.runtime.util.AsynchUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@DesignerComponent(
    version = 22,
    description = "Tools_Compress: bikin ZIP, ekstrak ZIP, intip isi ZIP, dan deteksi "
        + "jenis arsip (zip/rar/7z/tar/gz/...) dari magic-bytes. RAR/7z terdeteksi "
        + "namun belum bisa dibuka (butuh library luar) — ZIP penuh bisa. "
        + "Pola event = template paket: Started -> ProgressChanged -> Finished + "
        + "satu ErrorOccurred. Konvensi awalan: Is = boolean (colok ke if), "
        + "Call = async + hasil lewat event, Set = tanpa feedback, Get = nilai "
        + "balik sinkron.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    androidMinSdk = 21,
    iconName = "aiwebres/tools_compress.jpg",
    helpUrl = "https://github.com/VB-7/MiT-File_Manager")
@SimpleObject(external = true)
@UsesPermissions({"android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE",
    "android.permission.MANAGE_EXTERNAL_STORAGE"})
public class Tools_Compress extends AndroidNonvisibleComponent {

  private final Context context;
  private final Activity activity;
  private volatile boolean zipCancel = false;

  public Tools_Compress(ComponentContainer container) {
    super(container.$form());
    this.context = container.$context();
    this.activity = container.$context();
  }

  @SimpleFunction(description = "Deteksi jenis file dari ISINYA (bukan ekstensi). Kembali: zip, "
      + "rar, 7z, gz, bz2, xz, tar, apk, pdf, png, jpg, gif, mp3, mp4, atau unknown. "
      + "Instan, tanpa id. Pasangan GetZipEntries: hasil \"kosong\" + Detect = bukan-ZIP "
      + "berarti zip-nya kosong (bukan rusak).")
  public String DetectArchiveType(String path) {
    File tmpAsset = null;
    try {
      File f = Shared.resolveRooted(path);
      if (f == null && Shared.isAssetPath(path)) {
        tmpAsset = Shared.assetToCache(context, path);
        f = tmpAsset;
      }
      if (f == null || !f.exists() || f.isDirectory()) {
        Shared.err(this, "Zip", 0, "file tidak ada");
        return "unknown";
      }
      String kind = Shared.detectKind(f);
      if (kind.equals("unknown") && Shared.looksLikeTar(f)) {
        return "tar";
      }
      return kind;
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Zip", 0, m == null ? "deteksi gagal" : m);
      return "unknown";
    } finally {
      if (tmpAsset != null) {
        try {
          tmpAsset.delete();
        } catch (Exception e2) {
          // abaikan
        }
      }
    }
  }

  @SimpleFunction(description = "Daftar isi ZIP (nama entri). Instan. Kosong = bukan ZIP "
      + "ATAU zip kosong (bedakan via DetectArchiveType dulu).")
  public List<String> GetZipEntries(String zipPath) {
    ArrayList<String> out = new ArrayList<String>();
    ZipFile zf = null;
    File tmpAsset = null;
    try {
      File f = Shared.resolveRooted(zipPath);
      if (f == null && Shared.isAssetPath(zipPath)) {
        tmpAsset = Shared.assetToCache(context, zipPath);
        f = tmpAsset;
      }
      if (f == null || !f.exists()) {
        Shared.err(this, "Zip", 0, "file tidak ada");
        return out;
      }
      zf = new ZipFile(f);
      Enumeration<? extends ZipEntry> en = zf.entries();
      while (en.hasMoreElements()) {
        out.add(en.nextElement().getName());
      }
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Zip", 0, m == null ? "bukan ZIP yang valid" : m);
    } finally {
      if (zf != null) {
        try {
          zf.close();
        } catch (Exception e) {
          // abaikan
        }
      }
      if (tmpAsset != null) {
        try {
          tmpAsset.delete();
        } catch (Exception e2) {
          // abaikan
        }
      }
    }
    return out;
  }

  @SimpleFunction(description = "CALL: bikin ZIP dari BANYAK file sekaligus (tanpa loop). paths = "
      + "list path FILE (folder dilewati, tidak rekursif; list kosong = Finished gagal "
      + "\"list kosong\"). destZip = path file zip tujuan (sudah ada = DITIMPA). "
      + "Mulai = menembak Started; gagal via ErrorOccurred. "
      + "Pantau via ZipStarted/ProgressChanged/Finished. Satu proses ZIP per saat.")
  public void CallZipFiles(Object paths, String destZip) {
    if (!Shared.claimSlot("zip")) {
      Shared.err(this, "Zip", 0, "masih ada proses arsip berjalan");
      ZipFinished(false, "", "", 0, 0, "masih ada proses arsip berjalan");
      return;
    }
    List<String> list = toStrList(paths);
    if (list.isEmpty()) {
      Shared.releaseSlot("zip");
      Shared.err(this, "Zip", 0, "list kosong");
      ZipFinished(false, "", "", 0, 0, "list kosong");
      return;
    }
    File dst = Shared.resolveRooted(destZip);
    if (dst == null) {
      Shared.releaseSlot("zip");
      Shared.err(this, "Zip", 0, "tujuan ZIP tidak valid (Asset?)");
      ZipFinished(false, "", "", 0, 0, "tujuan ZIP tidak valid");
      return;
    }
    if (dst.getParentFile() != null) {
      dst.getParentFile().mkdirs();
    }
    final List<String> fPaths = list;
    final File fDst = dst;
    zipCancel = false;
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        if (!Shared.ensurePermission(form, activity, Shared.PERM_WRITE_STORAGE)) {
          final String permMsg = "izin belum granted (Android 11+: aktifkan 'Akses semua file')";
          Shared.releaseSlot("zip");
          zipFinished(false, "", "", 0, 0, permMsg);
          return;
        }
        runZip(fPaths, fDst);
      }
    });
  }

  private void runZip(List<String> paths, File dst) {
    ZipOutputStream zos = null;
    int total = paths.size();
    int done = 0;
    final ArrayList<File> tmpAssets = new ArrayList<File>();
    try {
      final int fTotal = total;
      final String fZip = dst.getAbsolutePath();
      try {
        form.runOnUiThread(new Runnable() {
          public void run() {
            ZipStarted(fTotal, fZip);
          }
        });
      } catch (Exception e) {
        // abaikan
      }
      zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(dst)));
      Set<String> used = new HashSet<String>();
      byte[] buf = new byte[32768];
      for (int i = 0; i < paths.size(); i = i + 1) {
        if (zipCancel) {
          deleteTemps(tmpAssets);
          try {
            dst.delete();
          } catch (Exception e0) {
            // abaikan
          }
          zipFinished(false, "", "", 0, 0, "Dibatalkan");
          return;
        }
        String srcP = paths.get(i);
        File src = Shared.resolveRooted(srcP);
        if (src == null && Shared.isAssetPath(srcP)) {
          File tmp = Shared.assetToCache(context, srcP);
          if (tmp != null) {
            tmpAssets.add(tmp);
            src = tmp;
          }
        }
        String entryName;
        if (src != null && src.exists() && !src.isDirectory()) {
          entryName = uniqueEntry(used, src.getName());
        } else {
          continue;
        }
        final int idx = i + 1;
        final String fName = entryName;
        try {
          form.runOnUiThread(new Runnable() {
            public void run() {
              ZipProgressChanged(idx, fTotal, fName, 0);
            }
          });
        } catch (Exception e) {
          // abaikan
        }
        try {
          FileInputStream fin = new FileInputStream(src);
          try {
            BufferedInputStream bin = new BufferedInputStream(fin);
            try {
              zos.putNextEntry(new ZipEntry(entryName));
              int n;
              while ((n = bin.read(buf)) > 0) {
                if (zipCancel) {
                  deleteTemps(tmpAssets);
                  try {
                    dst.delete();
                  } catch (Exception e0) {
                    // abaikan
                  }
                  zipFinished(false, "", "", 0, 0, "Dibatalkan");
                  return;
                }
                zos.write(buf, 0, n);
              }
              zos.closeEntry();
              done = done + 1;
            } finally {
              try {
                bin.close();
              } catch (Exception e) {
                // abaikan
              }
            }
          } finally {
            try {
              fin.close();
            } catch (Exception e) {
              // abaikan
            }
          }
        } catch (Exception e) {
          // file ini dilewati, lanjut berikutnya
        }
        final int dIdx = i + 1;
        try {
          form.runOnUiThread(new Runnable() {
            public void run() {
              ZipProgressChanged(dIdx, fTotal, fName, 100);
            }
          });
        } catch (Exception e) {
          // abaikan
        }
      }
      deleteTemps(tmpAssets);
      try {
        zos.finish();
        zos.flush();
      } catch (Exception e) {
        // abaikan
      }
      zipFinished(true, dst.getAbsolutePath(), Shared.fileNameOf(dst.getName()),
          done, total - done, "OK");
    } catch (Exception e) {
      String m = e.getMessage();
      zipFinished(false, "", "", done, total - done,
          m == null ? "bikin ZIP gagal" : m);
      try {
        dst.delete();
      } catch (Exception e2) {
        // abaikan
      }
    } finally {
      if (zos != null) {
        try {
          zos.close();
        } catch (Exception e) {
          // abaikan
        }
      }
      deleteTemps(tmpAssets);
      Shared.releaseSlot("zip");
    }
  }

  private void deleteTemps(List<File> tmps) {
    try {
      for (int i = 0; i < tmps.size(); i = i + 1) {
        try {
          tmps.get(i).delete();
        } catch (Exception e) {
          // abaikan
        }
      }
      tmps.clear();
    } catch (Exception e) {
      // abaikan
    }
  }

  private String uniqueEntry(Set<String> used, String name) {
    String n = Shared.sanitize(name.length() == 0 ? "file" : name);
    if (!used.contains(n)) {
      used.add(n);
      return n;
    }
    int dot = n.lastIndexOf('.');
    String base = dot > 0 ? n.substring(0, dot) : n;
    String ext = dot > 0 ? n.substring(dot) : "";
    int k = 1;
    while (true) {
      String g = base + "_" + k + ext;
      if (!used.contains(g)) {
        used.add(g);
        return g;
      }
      k = k + 1;
    }
  }

  private void zipFinished(final boolean success, final String zipPath,
      final String fileName, final int doneCount, final int failCount,
      final String msg) {
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          ZipFinished(success, zipPath, fileName, doneCount, failCount, msg);
        }
      });
    } catch (Exception e) {
      ZipFinished(success, zipPath, fileName, doneCount, failCount, msg);
    }
    if (!success && !"Dibatalkan".equals(msg)) {
      Shared.err(this, "Zip", 0, msg);
    }
  }

  @SimpleFunction(description = "CALL: ekstrak ZIP ke folder. Struktur folder di dalam ZIP dijaga. "
      + "destFolder sudah ada = DIGABUNG (file sama nama ditimpa). Bukan ZIP valid = "
      + "Finished gagal \"ekstrak gagal (bukan ZIP valid?)\". Batalkan via SetCancelZip "
      + "(file yang sudah terekstrak tetap ada). Pantau via "
      + "UnzipStarted/ProgressChanged/Finished.")
  public void CallUnzip(String zipPath, String destFolder) {
    if (!Shared.claimSlot("zip")) {
      Shared.err(this, "Zip", 0, "masih ada proses arsip berjalan");
      UnzipFinished(false, "", 0, 0, "masih ada proses arsip berjalan");
      return;
    }
    File zf = Shared.resolveRooted(zipPath);
    File tmpZip = null;
    if (zf == null && Shared.isAssetPath(zipPath)) {
      tmpZip = Shared.assetToCache(context, zipPath);
      zf = tmpZip;
    }
    if (zf == null || !zf.exists()) {
      Shared.releaseSlot("zip");
      Shared.err(this, "Zip", 0, "file ZIP tidak ada");
      UnzipFinished(false, "", 0, 0, "file ZIP tidak ada");
      return;
    }
    if (destFolder == null || Shared.clean(destFolder).length() == 0) {
      Shared.releaseSlot("zip");
      Shared.err(this, "Zip", 0, "folder tujuan kosong");
      UnzipFinished(false, "", 0, 0, "folder tujuan kosong");
      return;
    }
    File dir = Shared.resolveRooted(destFolder);
    if (dir == null) {
      Shared.releaseSlot("zip");
      Shared.err(this, "Zip", 0, "folder tujuan tidak valid (Asset?)");
      UnzipFinished(false, "", 0, 0, "folder tujuan tidak valid");
      return;
    }
    if (!dir.exists()) {
      dir.mkdirs();
    }
    final File fZf = zf;
    final File fDir = dir;
    final File fTmp = tmpZip;
    zipCancel = false;
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        if (!Shared.ensurePermission(form, activity, Shared.PERM_WRITE_STORAGE)) {
          final String permMsg = "izin belum granted (Android 11+: aktifkan 'Akses semua file')";
          Shared.releaseSlot("zip");
          try {
            if (fTmp != null) {
              fTmp.delete();
            }
          } catch (Exception ex0) {
            // abaikan
          }
          unzipFinished(false, "", 0, 0, permMsg);
          return;
        }
        try {
          runUnzip(fZf, fDir);
        } finally {
          if (fTmp != null) {
            try {
              fTmp.delete();
            } catch (Exception e) {
              // abaikan
            }
          }
        }
      }
    });
  }

  private void runUnzip(File zf, File dir) {
    ZipFile zip = null;
    int done = 0;
    int fail = 0;
    try {
      zip = new ZipFile(zf);
      int total = zip.size();
      final String zName = zf.getName();
      final int fTotal = total;
      final String fDir = dir.getAbsolutePath();
      try {
        form.runOnUiThread(new Runnable() {
          public void run() {
            UnzipStarted(zName, fTotal, fDir);
          }
        });
      } catch (Exception e) {
        // abaikan
      }
      String basePath = dir.getCanonicalPath();
      Enumeration<? extends ZipEntry> en = zip.entries();
      int idx = 0;
      byte[] buf = new byte[32768];
      while (en.hasMoreElements()) {
        if (zipCancel) {
          unzipFinished(false, fDir, done, total - done, "Dibatalkan");
          return;
        }
        ZipEntry e = en.nextElement();
        idx = idx + 1;
        final int fIdx = idx;
        final String fEntry = e.getName();
        try {
          form.runOnUiThread(new Runnable() {
            public void run() {
              UnzipProgressChanged(fIdx, fTotal, fEntry, 0);
            }
          });
        } catch (Exception ex) {
          // abaikan
        }
        try {
          File out = new File(dir, e.getName());
          String canon = out.getCanonicalPath();
          if (!canon.startsWith(basePath + File.separator)
              && !canon.equals(basePath)) {
            fail = fail + 1;
            continue;
          }
          if (e.isDirectory()) {
            out.mkdirs();
            done = done + 1;
          } else {
            if (out.getParentFile() != null) {
              out.getParentFile().mkdirs();
            }
            java.io.InputStream in = zip.getInputStream(e);
            try {
              FileOutputStream fout = new FileOutputStream(out);
              try {
                int n;
                while ((n = in.read(buf)) > 0) {
                  if (zipCancel) {
                    unzipFinished(false, fDir, done, total - done, "Dibatalkan");
                    return;
                  }
                  fout.write(buf, 0, n);
                }
                fout.flush();
              } finally {
                try {
                  fout.close();
                } catch (Exception ex) {
                  // abaikan
                }
              }
            } finally {
              try {
                in.close();
              } catch (Exception ex) {
                // abaikan
              }
            }
            done = done + 1;
          }
        } catch (Exception ex) {
          fail = fail + 1;
        }
        try {
          form.runOnUiThread(new Runnable() {
            public void run() {
              UnzipProgressChanged(fIdx, fTotal, fEntry, 100);
            }
          });
        } catch (Exception ex) {
          // abaikan
        }
      }
      unzipFinished(true, fDir, done, fail, "OK");
    } catch (Exception e) {
      String m = e.getMessage();
      unzipFinished(false, dir.getAbsolutePath(), done, 0,
          m == null ? "ekstrak gagal (bukan ZIP valid?)" : m);
    } finally {
      if (zip != null) {
        try {
          zip.close();
        } catch (Exception e) {
          // abaikan
        }
      }
      Shared.releaseSlot("zip");
    }
  }

  private void unzipFinished(final boolean success,
      final String destFolder, final int doneCount, final int failCount,
      final String msg) {
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          UnzipFinished(success, destFolder, doneCount, failCount, msg);
        }
      });
    } catch (Exception e) {
      UnzipFinished(success, destFolder, doneCount, failCount, msg);
    }
    if (!success && !"Dibatalkan".equals(msg)) {
      Shared.err(this, "Zip", 0, msg);
    }
  }

  @SimpleFunction(description = "SET: batalkan proses ZIP/Unzip berjalan. Tanpa feedback "
      + "(tidak ada event Cancelled: cancel = Set yang pasti). Efek: berhenti; file ZIP "
      + "sebagian DIHAPUS, file yang sudah terekstrak tetap ada. Verifikasi via IsZipBusy "
      + "(false = sudah berhenti) lalu coba-lagi.")
  public void SetCancelZip() {
    if (!Shared.slotBusy("zip")) {
      return;
    }
    zipCancel = true;
  }

  @SimpleFunction(description = "True bila ada proses ZIP/Unzip berjalan.")
  public boolean IsZipBusy() {
    return Shared.slotBusy("zip");
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

  // ================= EVENTS =================

  @SimpleEvent(description = "Bikin ZIP dimulai: total file + path ZIP tujuan.")
  public void ZipStarted(int total, String zipPath) {
    Shared.fire(this, "ZipStarted",
        Integer.valueOf(total), zipPath);
  }

  @SimpleEvent(description = "Progres bikin ZIP berubah: index/total + nama file + percent (0-100).")
  public void ZipProgressChanged(int index, int total, String fileName, int percent) {
    Shared.fire(this, "ZipProgressChanged",
        Integer.valueOf(index), Integer.valueOf(total), fileName,
        Integer.valueOf(percent));
  }

  @SimpleEvent(description = "Bikin ZIP selesai: success + path ZIP + fileName + "
      + "jumlah ok/gagal.")
  public void ZipFinished(boolean success, String zipPath, String fileName,
      int doneCount, int failCount, String message) {
    Shared.fire(this, "ZipFinished", Boolean.valueOf(success), zipPath, fileName, Integer.valueOf(doneCount),
        Integer.valueOf(failCount), message);
  }

  @SimpleEvent(description = "Ekstrak ZIP dimulai: nama ZIP + total entri + folder tujuan.")
  public void UnzipStarted(String zipName, int total, String destFolder) {
    Shared.fire(this, "UnzipStarted", zipName,
        Integer.valueOf(total), destFolder);
  }

  @SimpleEvent(description = "Progres ekstrak berubah: index/total + nama entri + percent (0-100).")
  public void UnzipProgressChanged(int index, int total, String entryName,
      int percent) {
    Shared.fire(this, "UnzipProgressChanged",
        Integer.valueOf(index), Integer.valueOf(total), entryName,
        Integer.valueOf(percent));
  }

  @SimpleEvent(description = "Ekstrak selesai: success + folder + jumlah ok/gagal.")
  public void UnzipFinished(boolean success, String destFolder,
      int doneCount, int failCount, String message) {
    Shared.fire(this, "UnzipFinished", Boolean.valueOf(success), destFolder, Integer.valueOf(doneCount), Integer.valueOf(failCount),
        message);
  }

  @SimpleEvent(description = "SATU-SATUNYA error Tools_Compress. component = nama operasi "
      + "(bandingkan dengan blok OpZip/OpUnzip; cth: if component = OpUnzip berarti error "
      + "ekstrak). id = id terkait (0 bila umum).")
  public void ErrorOccurred(String component, int id, String message) {
    Shared.fire(this, "ErrorOccurred",
        component, Integer.valueOf(id), message);
  }

  @SimpleFunction(description = "Fallback polling untuk event ErrorOccurred: "
      + "pesan error terakhir operasi ini (colok OpZip/OpUnzip: isi dengan "
      + "itu, JANGAN ketik manual; kosong bila belum ada).")
  public String LastError(String operation) {
    return Shared.lastError(operation);
  }

  // ================= OP =================

  @SimpleProperty(description = "Nama operasi Zip (untuk if di ErrorOccurred: "
      + "cth if component = OpZip berarti error bikin-ZIP).",
      category = PropertyCategory.BEHAVIOR)
  public String OpZip() {
    return "Zip";
  }

  @SimpleProperty(description = "Nama operasi Unzip (untuk if di ErrorOccurred: "
      + "cth if component = OpUnzip berarti error ekstrak).",
      category = PropertyCategory.BEHAVIOR)
  public String OpUnzip() {
    return "Unzip";
  }
}
