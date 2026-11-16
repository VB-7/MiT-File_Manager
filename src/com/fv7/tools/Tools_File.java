package com.fv7.tools;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@DesignerComponent(
    version = 1,
    description = "Tools_File: semua operasi file instan (tanpa id) + salin/pindah "
        + "(Started/ProgressChanged/Finished, single-slot) + batch list + file picker "
        + "+ buka file + nomedia + hidden + simpan base64 langsung ke storage "
        + "(SaveBase64ToFile, tanpa validasi gambar, lebih cepat).",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    androidMinSdk = 21,
    iconName = "aiwebres/tools_file.jpg")
@SimpleObject(external = true)
@UsesPermissions({"android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE"})
public class Tools_File extends AndroidNonvisibleComponent
    implements ActivityResultListener, OnDestroyListener {

  private final Context context;
  private final Activity activity;

  private volatile boolean copyCancel = false;
  private volatile boolean moveCancel = false;
  private volatile boolean batchCancel = false;

  private final Map<Integer, Integer> picks = new HashMap<Integer, Integer>();
  private static final long PROGRESS_MIN_MS = 400;

  public Tools_File(ComponentContainer container) {
    super(container.$form());
    this.context = container.$context();
    this.activity = container.$context();
  }

  // ================= INSTAN: cek =================

  @SimpleFunction(description = "Apakah file/folder ada (Asset bisa dicek).")
  public boolean Exists(FileScope scope, String path) {
    try {
      String p = Shared.clean(path);
      if (scope == FileScope.Asset || Shared.isAssetPath(p)) {
        try {
          InputStream in = context.getAssets().open(Shared.assetName(p));
          try {
            in.close();
          } catch (Exception e) {
            // abaikan
          }
          return true;
        } catch (Exception e) {
          try {
            String[] l = context.getAssets().list(Shared.assetName(p));
            return l != null && l.length > 0;
          } catch (Exception e2) {
            return false;
          }
        }
      }
      File f = Shared.resolve(context, scope, path);
      return f != null && f.exists();
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleFunction(description = "Apakah path adalah folder.")
  public boolean IsDirectory(FileScope scope, String path) {
    try {
      String p = Shared.clean(path);
      if (scope == FileScope.Asset || Shared.isAssetPath(p)) {
        try {
          String[] l = context.getAssets().list(Shared.assetName(p));
          return l != null && l.length > 0;
        } catch (Exception e) {
          return false;
        }
      }
      File f = Shared.resolve(context, scope, path);
      return f != null && f.isDirectory();
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleFunction(description = "Apakah nama file/folder diawali titik (hidden ala Android/Linux).")
  public boolean IsHidden(FileScope scope, String path) {
    try {
      String p = Shared.clean(path);
      String n;
      if (scope == FileScope.Asset || Shared.isAssetPath(p)) {
        n = Shared.fileNameOf(Shared.assetName(p));
      } else {
        File f = Shared.resolve(context, scope, path);
        if (f == null) {
          return false;
        }
        n = f.getName();
      }
      return n.startsWith(".");
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleFunction(description = "Sembunyikan (hidden=true, nama jadi .nama) atau tampilkan lagi. "
      + "Sudah sesuai kondisi = true.")
  public boolean SetHidden(FileScope scope, String path, boolean hidden) {
    try {
      File f = Shared.resolve(context, scope, path);
      if (f == null || !f.exists()) {
        Shared.err(this, "File", 0, "file tidak ada");
        return false;
      }
      String n = f.getName();
      boolean now = n.startsWith(".");
      if (now == hidden) {
        return true;
      }
      String target = hidden ? "." + n : n.substring(1);
      if (target.length() == 0) {
        Shared.err(this, "File", 0, "nama tidak valid");
        return false;
      }
      return f.renameTo(new File(f.getParentFile(), target));
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "File", 0, m == null ? "gagal ubah hidden" : m);
      return false;
    }
  }

  // ================= INSTAN: buat/hapus/rename =================

  @SimpleFunction(description = "Buat folder (termasuk induknya). Sudah ada = true.")
  public boolean MakeDirectory(FileScope scope, String path) {
    try {
      File d = Shared.resolve(context, scope, path);
      if (d == null) {
        Shared.err(this, "File", 0, "tidak bisa buat folder di Asset");
        return false;
      }
      if (d.exists()) {
        return true;
      }
      return d.mkdirs();
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "File", 0, m == null ? "buat folder gagal" : m);
      return false;
    }
  }

  @SimpleFunction(description = "Pastikan folder ada (buat bila belum). Selalu true bila beres. "
      + "Pasangan pola: if IsDirectory(...) then ... (tidak perlu, cukup blok ini).")
  public boolean EnsureDirectory(FileScope scope, String path) {
    return MakeDirectory(scope, path);
  }

  @SimpleFunction(description = "Hapus file/folder (folder rekursif). Tidak ada = true. "
      + "Asset tidak bisa dihapus.")
  public boolean Delete(FileScope scope, String path) {
    try {
      File f = Shared.resolve(context, scope, path);
      if (f == null) {
        Shared.err(this, "File", 0, "tidak bisa hapus Asset");
        return false;
      }
      if (!f.exists()) {
        return true;
      }
      return Shared.deleteRecursive(f);
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "File", 0, m == null ? "hapus gagal" : m);
      return false;
    }
  }

  @SimpleFunction(description = "Hapus BANYAK path sekaligus (tanpa loop manual). "
      + "Kembali: jumlah yang berhasil terhapus.")
  public int DeleteList(FileScope scope, Object paths) {
    int done = 0;
    try {
      List<String> list = toStringList(paths);
      for (int i = 0; i < list.size(); i = i + 1) {
        try {
          File f = Shared.resolve(context, scope, list.get(i));
          if (f != null && f.exists() && Shared.deleteRecursive(f)) {
            done = done + 1;
          } else if (f != null && !f.exists()) {
            done = done + 1;
          }
        } catch (Exception e) {
          // lanjut ke berikutnya, dihitung gagal
        }
      }
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "File", 0, m == null ? "hapus batch gagal" : m);
    }
    return done;
  }

  @SimpleFunction(description = "Ganti nama FILE (tanpa pindah folder). newName = nama saja.")
  public boolean RenameFile(FileScope scope, String path, String newName) {
    try {
      File src = Shared.resolve(context, scope, path);
      if (src == null || !src.exists() || src.isDirectory()) {
        Shared.err(this, "File", 0, "file tidak ada");
        return false;
      }
      String nn = Shared.sanitize(Shared.clean(newName));
      if (nn.length() == 0 || nn.indexOf('/') >= 0) {
        Shared.err(this, "File", 0, "nama baru tidak valid");
        return false;
      }
      return src.renameTo(new File(src.getParentFile(), nn));
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "File", 0, m == null ? "rename gagal" : m);
      return false;
    }
  }

  @SimpleFunction(description = "Ganti nama FOLDER (tanpa pindah). newName = nama saja.")
  public boolean RenameFolder(FileScope scope, String path, String newName) {
    try {
      File src = Shared.resolve(context, scope, path);
      if (src == null || !src.exists() || !src.isDirectory()) {
        Shared.err(this, "File", 0, "folder tidak ada");
        return false;
      }
      String nn = Shared.sanitize(Shared.clean(newName));
      if (nn.length() == 0 || nn.indexOf('/') >= 0) {
        Shared.err(this, "File", 0, "nama baru tidak valid");
        return false;
      }
      return src.renameTo(new File(src.getParentFile(), nn));
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "File", 0, m == null ? "rename gagal" : m);
      return false;
    }
  }

  @SimpleFunction(description = "Tulis teks ke file (UTF-8). append=true = sambung di akhir. "
      + "Folder dibuat otomatis. Asset tidak bisa ditulis.")
  public boolean SaveFile(FileScope scope, String path, String content, boolean append) {
    java.io.Writer w = null;
    try {
      File f = Shared.resolve(context, scope, path);
      if (f == null) {
        Shared.err(this, "File", 0, "tidak bisa tulis ke Asset");
        return false;
      }
      if (f.getParentFile() != null) {
        f.getParentFile().mkdirs();
      }
      w = new java.io.OutputStreamWriter(new FileOutputStream(f, append), "UTF-8");
      w.write(content == null ? "" : content);
      w.flush();
      return true;
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "File", 0, m == null ? "tulis gagal" : m);
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

  @SimpleFunction(description = "Buat file kosong (folder induk dibuat otomatis). "
      + "Sudah ada = true.")
  public boolean CreateEmptyFile(FileScope scope, String path) {
    try {
      File f = Shared.resolve(context, scope, path);
      if (f == null) {
        Shared.err(this, "File", 0, "tidak bisa buat file di Asset");
        return false;
      }
      if (f.exists()) {
        return true;
      }
      if (f.getParentFile() != null) {
        f.getParentFile().mkdirs();
      }
      return f.createNewFile();
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "File", 0, m == null ? "buat file gagal" : m);
      return false;
    }
  }

  // ================= BASE64 KE FILE =================

  @SimpleFunction(description = "Simpan base64 LANGSUNG ke storage (tanpa validasi gambar, "
      + "lebih cepat dari Base64ToPicture). base64 boleh mentah atau data:.../...;base64,.... "
      + "path = lokasi TUJUAN termasuk nama file (mis. folder/hasil.bin). Folder dibuat "
      + "otomatis. Asset tidak bisa jadi tujuan. Hasil lewat AfterBase64FileReady. "
      + "Satu proses per saat; sibuk = false + error. Kembali: true = mulai.")
  public boolean SaveBase64ToFile(final String base64, final FileScope scope,
      final String path) {
    if (!Shared.claimSlot("b64file")) {
      Shared.err(this, "Base64", 0, "masih ada proses base64-file berjalan");
      return false;
    }
    final String b64 = base64 == null ? "" : base64;
    final FileScope fS = scope;
    final String fP = path == null ? "" : path;
    Base64FileStarted();
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        boolean ok = false;
        String outPath = "";
        String outName = "";
        String outScope = "";
        String msg = "";
        try {
          String s = Shared.clean(b64);
          int comma = s.indexOf(',');
          if (s.startsWith("data:") && comma >= 0) {
            s = s.substring(comma + 1).trim();
          }
          if (s.length() == 0) {
            msg = "base64 kosong";
          } else {
            byte[] data = Base64.decode(s, Base64.DEFAULT);
            File dst = Shared.resolve(context, fS, fP);
            if (dst == null) {
              msg = "tidak bisa tulis ke Asset";
            } else if (dst.isDirectory()) {
              msg = "path adalah folder (tambah nama file)";
            } else {
              if (dst.getParentFile() != null) {
                dst.getParentFile().mkdirs();
              }
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
              outName = dst.getName();
              outScope = fS == null ? "" : fS.name();
              msg = "OK";
            }
          }
        } catch (IllegalArgumentException e) {
          msg = "base64 tidak valid";
        } catch (Exception e) {
          String m = e.getMessage();
          msg = m == null ? "simpan base64 gagal" : m;
        } finally {
          Shared.releaseSlot("b64file");
        }
        final boolean fOk = ok;
        final String fPath = outPath;
        final String fName = outName;
        final String fSc = outScope;
        final String fMsg = msg;
        try {
          form.runOnUiThread(new Runnable() {
            public void run() {
              AfterBase64FileReady(fOk, fSc, fPath, fName, fMsg);
            }
          });
        } catch (Exception e) {
          AfterBase64FileReady(fOk, fSc, fPath, fName, fMsg);
        }
        if (!fOk) {
          Shared.err(Tools_File.this, "Base64", 0, fMsg);
        }
      }
    });
    return true;
  }

  // ================= INSTAN: nomedia =================

  @SimpleFunction(description = "Buat file .nomedia di folder (sembunyikan dari galeri/musik). "
      + "Sudah ada = true.")
  public boolean CreateNomedia(FileScope scope, String folder) {
    try {
      File d = Shared.resolve(context, scope, folder);
      if (d == null) {
        Shared.err(this, "File", 0, "tidak bisa tulis ke Asset");
        return false;
      }
      if (!d.exists()) {
        d.mkdirs();
      }
      File n = new File(d, ".nomedia");
      if (n.exists()) {
        return true;
      }
      return n.createNewFile();
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "File", 0, m == null ? "buat .nomedia gagal" : m);
      return false;
    }
  }

  @SimpleFunction(description = "Hapus file .nomedia di folder. Tidak ada = true.")
  public boolean DeleteNomedia(FileScope scope, String folder) {
    try {
      File d = Shared.resolve(context, scope, folder);
      if (d == null) {
        Shared.err(this, "File", 0, "tidak bisa hapus di Asset");
        return false;
      }
      File n = new File(d, ".nomedia");
      if (!n.exists()) {
        return true;
      }
      return n.delete();
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "File", 0, m == null ? "hapus .nomedia gagal" : m);
      return false;
    }
  }

  @SimpleFunction(description = "Apakah folder punya file .nomedia.")
  public boolean NomediaExists(FileScope scope, String folder) {
    try {
      File d = Shared.resolve(context, scope, folder);
      if (d == null || !d.isDirectory()) {
        return false;
      }
      return new File(d, ".nomedia").exists();
    } catch (Exception e) {
      return false;
    }
  }

  // ================= INSTAN: info =================

  @SimpleFunction(description = "Ukuran file dalam byte. Folder = total isi rekursif. "
      + "-1 bila tidak ada.")
  public long FileSize(FileScope scope, String path) {
    try {
      String p = Shared.clean(path);
      if (scope == FileScope.Asset || Shared.isAssetPath(p)) {
        try {
          return context.getAssets().openFd(Shared.assetName(p)).getLength();
        } catch (Exception e) {
          return -1L;
        }
      }
      File f = Shared.resolve(context, scope, path);
      if (f == null || !f.exists()) {
        return -1L;
      }
      return Shared.dirSize(f);
    } catch (Exception e) {
      return -1L;
    }
  }

  @SimpleFunction(description = "Sisa ruang kosong (byte) di penyimpanan scope itu. "
      + "Cek sebelum unduh/salin besar. -1 bila gagal.")
  public long FreeSpace(FileScope scope) {
    try {
      File root = Shared.rootFor(context, scope);
      android.os.StatFs st = new android.os.StatFs(root.getAbsolutePath());
      return st.getAvailableBytes();
    } catch (Exception e) {
      return -1L;
    }
  }

  @SimpleFunction(description = "Total kapasitas (byte) penyimpanan scope itu. -1 bila gagal.")
  public long TotalSpace(FileScope scope) {
    try {
      File root = Shared.rootFor(context, scope);
      android.os.StatFs st = new android.os.StatFs(root.getAbsolutePath());
      return st.getTotalBytes();
    } catch (Exception e) {
      return -1L;
    }
  }

  @SimpleFunction(description = "Daftar file (path lengkap). filter = ekstensi tanpa titik "
      + "(kosong = semua). includeSubfolders = sertakan isi subfolder.")
  public List<String> ListFiles(FileScope scope, String path, String filter,
      boolean includeSubfolders) {
    ArrayList<String> out = new ArrayList<String>();
    try {
      String p = Shared.clean(path);
      if (scope == FileScope.Asset || Shared.isAssetPath(p)) {
        String[] l = context.getAssets().list(Shared.assetName(p));
        if (l != null) {
          String f = normExt(filter);
          for (int i = 0; i < l.length; i = i + 1) {
            if (matchExt(l[i], f)) {
              out.add("//" + Shared.assetName(p) + "/" + l[i]);
            }
          }
        }
        Collections.sort(out);
        return out;
      }
      File d = Shared.resolve(context, scope, path);
      if (d == null || !d.isDirectory()) {
        Shared.err(this, "File", 0, "folder tidak ada");
        return out;
      }
      walkFiles(d, normExt(filter), includeSubfolders, out);
      Collections.sort(out);
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "File", 0, m == null ? "list gagal" : m);
    }
    return out;
  }

  @SimpleFunction(description = "Daftar subfolder (path lengkap, tanpa rekursif).")
  public List<String> ListFolders(FileScope scope, String path) {
    try {
      String p = Shared.clean(path);
      if (scope == FileScope.Asset || Shared.isAssetPath(p)) {
        Shared.err(this, "File", 0, "list folder Asset tidak didukung");
        return new ArrayList<String>();
      }
      File d = Shared.resolve(context, scope, path);
      if (d == null || !d.isDirectory()) {
        Shared.err(this, "File", 0, "folder tidak ada");
        return new ArrayList<String>();
      }
      return Shared.sortedDirs(d);
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "File", 0, m == null ? "list gagal" : m);
      return new ArrayList<String>();
    }
  }

  private void walkFiles(File dir, String ext, boolean recursive, ArrayList<String> out) {
    File[] fs = dir.listFiles();
    if (fs == null) {
      return;
    }
    for (int i = 0; i < fs.length; i = i + 1) {
      File g = fs[i];
      if (g.isDirectory()) {
        if (recursive) {
          walkFiles(g, ext, true, out);
        }
      } else if (matchExt(g.getName(), ext)) {
        out.add(g.getAbsolutePath());
      }
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

  // ================= INSTAN: path =================

  @SimpleFunction(description = "Path absolut dari scope+path. URL/base64/Asset = "
      + "string kosong + error.")
  public String GetFullPath(FileScope scope, String path) {
    try {
      String p = Shared.clean(path);
      if (p.length() == 0) {
        Shared.err(this, "File", 0, "path kosong");
        return "";
      }
      File f = Shared.resolve(context, scope, path);
      if (f == null) {
        Shared.err(this, "File", 0, "Asset tidak punya path file");
        return "";
      }
      return f.getAbsolutePath();
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "File", 0, m == null ? "path gagal di-resolve" : m);
      return "";
    }
  }

  @SimpleFunction(description = "Nama file dari sebuah path (buang foldernya).")
  public String GetFileName(String path) {
    return Shared.fileNameOf(path);
  }

  @SimpleFunction(description = "Ekstensi tanpa titik (huruf kecil). Cth: foto.JPG -> jpg.")
  public String GetExtension(String path) {
    return Shared.extensionOf(path);
  }

  @SimpleFunction(description = "Folder induk dari sebuah path. Cth: /a/b/c.jpg -> /a/b.")
  public String GetParentFolder(String path) {
    return Shared.parentOf(path);
  }

  // ================= BACA TEKS =================

  @SimpleFunction(description = "Baca file teks (UTF-8) di background. Hasil lewat GotText. "
      + "Satu bacaan per saat; sibuk = false + error.")
  public boolean ReadText(FileScope scope, String path) {
    if (!Shared.claimSlot("read")) {
      Shared.err(this, "Read", 0, "masih ada bacaan berjalan");
      return false;
    }
    final FileScope fS = scope;
    final String fP = path == null ? "" : path;
    ReadStarted(fS == null ? "" : fS.name(), fP);
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        InputStream in = null;
        boolean ok = false;
        String text = "";
        String err = "";
        String sc = "";
        try {
          sc = fS == null ? "" : fS.name();
          String p = Shared.clean(fP);
          if (fS == FileScope.Asset || Shared.isAssetPath(p)) {
            in = context.getAssets().open(Shared.assetName(p));
          } else {
            File f = Shared.resolve(context, fS, fP);
            if (f == null || !f.exists() || f.isDirectory()) {
              err = "file tidak ada";
            } else {
              in = new FileInputStream(f);
            }
          }
          if (in != null) {
            java.io.BufferedReader r = new java.io.BufferedReader(
                new java.io.InputStreamReader(in, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[8192];
            int n;
            while ((n = r.read(buf)) > 0) {
              sb.append(buf, 0, n);
            }
            text = sb.toString();
            ok = true;
          }
        } catch (Exception e) {
          String m = e.getMessage();
          err = m == null ? "baca gagal" : m;
        } finally {
          if (in != null) {
            try {
              in.close();
            } catch (Exception e) {
              // abaikan
            }
          }
          Shared.releaseSlot("read");
        }
        final boolean fOk = ok;
        final String fText = text;
        final String fErr = err;
        final String fSc = sc;
        try {
          form.runOnUiThread(new Runnable() {
            public void run() {
              GotText(fOk, fSc, fP, Shared.fileNameOf(fP), fText);
            }
          });
        } catch (Exception e) {
          GotText(fOk, fSc, fP, Shared.fileNameOf(fP), fText);
        }
        if (!fOk) {
          Shared.err(Tools_File.this, "Read", 0, fErr);
        }
      }
    });
    return true;
  }

  // ================= SALIN / PINDAH =================

  @SimpleFunction(description = "Salin file (Started/ProgressChanged/Finished). Asset hanya bisa "
      + "jadi sumber. Satu salinan per saat; sibuk = false + error. Kembali: true = mulai.")
  public boolean CopyFile(FileScope fromScope, String fromPath, FileScope toScope,
      String toPath) {
    if (!Shared.claimSlot("copy")) {
      Shared.err(this, "Copy", 0, "masih ada salinan berjalan");
      return false;
    }
    if (toScope == FileScope.Asset
        || (toPath != null && Shared.isAssetPath(Shared.clean(toPath)))) {
      Shared.releaseSlot("copy");
      Shared.err(this, "Copy", 0, "tidak bisa salin ke Asset");
      return false;
    }
    copyCancel = false;
    final FileScope fS = fromScope;
    final String fP = fromPath;
    final FileScope tS = toScope;
    final String tP = toPath;
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        runCopyMove(false, fS, fP, tS, tP);
      }
    });
    return true;
  }

  @SimpleFunction(description = "Pindah file (Started/ProgressChanged/Finished). Asset hanya bisa "
      + "jadi sumber (jadi salinan). Satu pindahan per saat. Kembali: true = mulai.")
  public boolean MoveFile(FileScope fromScope, String fromPath, FileScope toScope,
      String toPath) {
    if (!Shared.claimSlot("move")) {
      Shared.err(this, "Move", 0, "masih ada pindahan berjalan");
      return false;
    }
    if (toScope == FileScope.Asset
        || (toPath != null && Shared.isAssetPath(Shared.clean(toPath)))) {
      Shared.releaseSlot("move");
      Shared.err(this, "Move", 0, "tidak bisa pindah ke Asset");
      return false;
    }
    moveCancel = false;
    final FileScope fS = fromScope;
    final String fP = fromPath;
    final FileScope tS = toScope;
    final String tP = toPath;
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        runCopyMove(true, fS, fP, tS, tP);
      }
    });
    return true;
  }

  private void runCopyMove(boolean move, FileScope fS, String fP, FileScope tS, String tP) {
    String kind = move ? "move" : "copy";
    String scName = tS == null ? "" : tS.name();
    String comp = move ? "Move" : "Copy";
    InputStream in = null;
    boolean fromAsset = false;
    String aName = "";
    File src = Shared.resolve(context, fS, fP);
    long total = 0;
    try {
      if (src == null) {
        String p = Shared.clean(fP);
        if (fS == FileScope.Asset || Shared.isAssetPath(p)) {
          fromAsset = true;
          aName = Shared.assetName(p);
        } else {
          finishCopyMove(move, scName, false, "", "path sumber tidak valid");
          return;
        }
      } else if (src.isDirectory()) {
        finishCopyMove(move, scName, false, "", "sumber adalah folder");
        return;
      } else if (!src.exists()) {
        finishCopyMove(move, scName, false, "", "file sumber tidak ada");
        return;
      }
      File dst = Shared.resolve(context, tS, tP);
      if (dst == null) {
        finishCopyMove(move, scName, false, "", "tujuan tidak valid");
        return;
      }
      if (dst.exists() && dst.isDirectory()) {
        finishCopyMove(move, scName, false, "", "tujuan adalah folder");
        return;
      }
      if (dst.getParentFile() != null) {
        dst.getParentFile().mkdirs();
      }
      String dispName = Shared.fileNameOf(dst.getName());
      fireStarted(move, dispName, dst.getAbsolutePath());
      if (fromAsset) {
        try {
          total = context.getAssets().openFd(aName).getLength();
        } catch (Exception e) {
          total = -1;
        }
        in = context.getAssets().open(aName);
      } else {
        if (move && src.renameTo(dst)) {
          fireProgress(move, 100, 1, 1);
          finishCopyMove(move, scName, true, dst.getAbsolutePath(), "OK");
          return;
        }
        total = src.length();
        in = new FileInputStream(src);
      }
      FileOutputStream out = new FileOutputStream(dst);
      try {
        byte[] buf = new byte[32768];
        long done = 0;
        int lastPct = -1;
        long lastFire = 0;
        int n;
        while ((n = in.read(buf)) > 0) {
          if ((move && moveCancel) || (!move && copyCancel)) {
            finishCopyMove(move, scName, false, "", "Dibatalkan");
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
            fireProgress(move, pct, done, total);
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
        finishCopyMove(move, scName, true, dst.getAbsolutePath(), "OK");
      } finally {
        try {
          out.close();
        } catch (Exception e) {
          // abaikan
        }
      }
    } catch (Exception e) {
      String m = e.getMessage();
      finishCopyMove(move, scName, false, "", m == null ? "salin gagal" : m);
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (Exception e) {
          // abaikan
        }
      }
      Shared.releaseSlot(kind);
    }
  }

  private void fireStarted(final boolean move, final String fileName, final String destPath) {
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          if (move) {
            MoveStarted(fileName, destPath);
          } else {
            CopyStarted(fileName, destPath);
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  private void fireProgress(final boolean move, final int pct, final long done, final long total) {
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          if (move) {
            MoveProgressChanged(pct, done, total);
          } else {
            CopyProgressChanged(pct, done, total);
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  private void finishCopyMove(final boolean move, final String sc,
      final boolean success, final String destPath, final String msg) {
    final String fn = Shared.fileNameOf(destPath);
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          if (move) {
            MoveFinished(success, sc, destPath, fn, msg);
          } else {
            CopyFinished(success, sc, destPath, fn, msg);
          }
        }
      });
    } catch (Exception e) {
      if (move) {
        MoveFinished(success, sc, destPath, fn, msg);
      } else {
        CopyFinished(success, sc, destPath, fn, msg);
      }
    }
    if (!success && !"Dibatalkan".equals(msg)) {
      Shared.err(this, move ? "Move" : "Copy", 0, msg);
    }
  }

  @SimpleFunction(description = "Batalkan salinan berjalan. File sebagian dihapus.")
  public void CancelCopy() {
    if (!Shared.slotBusy("copy")) {
      Shared.err(this, "Copy", 0, "tidak ada salinan berjalan");
      return;
    }
    copyCancel = true;
  }

  @SimpleFunction(description = "Batalkan pindahan berjalan. File sebagian dihapus.")
  public void CancelMove() {
    if (!Shared.slotBusy("move")) {
      Shared.err(this, "Move", 0, "tidak ada pindahan berjalan");
      return;
    }
    moveCancel = true;
  }

  @SimpleFunction(description = "True bila ada salinan berjalan.")
  public boolean IsCopyBusy() {
    return Shared.slotBusy("copy");
  }

  @SimpleFunction(description = "True bila ada pindahan berjalan.")
  public boolean IsMoveBusy() {
    return Shared.slotBusy("move");
  }

  // ================= BATCH: list =================

  @SimpleFunction(description = "Salin BANYAK file sekaligus ke satu folder (tanpa loop manual). "
      + "fromPaths = list path sumber. Kembali: true = mulai. "
      + "Pantau via CopyBatchStarted/ProgressChanged/Finished.")
  public boolean CopyList(FileScope fromScope, Object fromPaths, FileScope toScope,
      String toFolder) {
    if (!Shared.claimSlot("copy")) {
      Shared.err(this, "Copy", 0, "masih ada salinan berjalan");
      return false;
    }
    if (toScope == FileScope.Asset
        || (toFolder != null && Shared.isAssetPath(Shared.clean(toFolder)))) {
      Shared.releaseSlot("copy");
      Shared.err(this, "Copy", 0, "tidak bisa salin ke Asset");
      return false;
    }
    final FileScope fS = fromScope;
    final List<String> paths = toStringList(fromPaths);
    final FileScope tS = toScope;
    final String tF = toFolder == null ? "" : toFolder;
    if (paths.isEmpty()) {
      Shared.releaseSlot("copy");
      Shared.err(this, "Copy", 0, "list kosong");
      return false;
    }
    batchCancel = false;
    copyCancel = false;
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        runBatch(false, fS, paths, tS, tF);
      }
    });
    return true;
  }

  @SimpleFunction(description = "Pindah BANYAK file sekaligus ke satu folder. "
      + "Pantau via MoveBatchStarted/ProgressChanged/Finished.")
  public boolean MoveList(FileScope fromScope, Object fromPaths, FileScope toScope,
      String toFolder) {
    if (!Shared.claimSlot("move")) {
      Shared.err(this, "Move", 0, "masih ada pindahan berjalan");
      return false;
    }
    if (toScope == FileScope.Asset
        || (toFolder != null && Shared.isAssetPath(Shared.clean(toFolder)))) {
      Shared.releaseSlot("move");
      Shared.err(this, "Move", 0, "tidak bisa pindah ke Asset");
      return false;
    }
    final FileScope fS = fromScope;
    final List<String> paths = toStringList(fromPaths);
    final FileScope tS = toScope;
    final String tF = toFolder == null ? "" : toFolder;
    if (paths.isEmpty()) {
      Shared.releaseSlot("move");
      Shared.err(this, "Move", 0, "list kosong");
      return false;
    }
    batchCancel = false;
    moveCancel = false;
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        runBatch(true, fS, paths, tS, tF);
      }
    });
    return true;
  }

  private void runBatch(final boolean move, FileScope fS, List<String> paths, FileScope tS,
      String folder) {
    String kind = move ? "move" : "copy";
    String bSc = tS == null ? "" : tS.name();
    String comp = move ? "Move" : "Copy";
    int total = paths.size();
    int done = 0;
    int fail = 0;
    try {
      File dir = Shared.resolve(context, tS, folder);
      if (dir == null) {
        batchFinished(move, false, bSc, "", 0, total, "folder tujuan tidak valid");
        return;
      }
      if (!dir.exists()) {
        dir.mkdirs();
      }
      final int fTotal = total;
      final String fFolder = dir.getAbsolutePath();
      try {
        form.runOnUiThread(new Runnable() {
          public void run() {
            if (move) {
              MoveBatchStarted(fTotal, fFolder);
            } else {
              CopyBatchStarted(fTotal, fFolder);
            }
          }
        });
      } catch (Exception e) {
        // abaikan
      }
      for (int i = 0; i < paths.size(); i = i + 1) {
        if (batchCancel || (move && moveCancel) || (!move && copyCancel)) {
          batchFinished(move, false, bSc, fFolder, done, total - done,
              "Dibatalkan");
          return;
        }
        String srcP = paths.get(i);
        String name = Shared.fileNameOf(Shared.clean(srcP));
        if (name.length() == 0) {
          name = "file_" + i;
        }
        final int idx = i + 1;
        final int fT = total;
        final String fName = name;
        final int lastPct = 0;
        try {
          form.runOnUiThread(new Runnable() {
            public void run() {
              if (move) {
                MoveBatchProgressChanged(idx, fT, fName, lastPct);
              } else {
                CopyBatchProgressChanged(idx, fT, fName, lastPct);
              }
            }
          });
        } catch (Exception e) {
          // abaikan
        }
        if (copyOneFile(move, fS, srcP, tS, folder + "/" + name)) {
          done = done + 1;
        } else {
          fail = fail + 1;
        }
        final int dIdx = i + 1;
        try {
          form.runOnUiThread(new Runnable() {
            public void run() {
              if (move) {
                MoveBatchProgressChanged(dIdx, fT, fName, 100);
              } else {
                CopyBatchProgressChanged(dIdx, fT, fName, 100);
              }
            }
          });
        } catch (Exception e) {
          // abaikan
        }
      }
      batchFinished(move, fail == 0, bSc, fFolder, done, fail, "OK");
    } catch (Exception e) {
      String m = e.getMessage();
      batchFinished(move, false, bSc, "", done, total - done,
          m == null ? "batch gagal" : m);
    } finally {
      Shared.releaseSlot(kind);
    }
  }

  private boolean copyOneFile(boolean move, FileScope fS, String fP, FileScope tS,
      String tP) {
    InputStream in = null;
    try {
      File src = Shared.resolve(context, fS, fP);
      File dst = Shared.resolve(context, tS, tP);
      if (dst == null) {
        return false;
      }
      if (dst.getParentFile() != null) {
        dst.getParentFile().mkdirs();
      }
      if (src == null) {
        String p = Shared.clean(fP);
        if (fS == FileScope.Asset || Shared.isAssetPath(p)) {
          in = context.getAssets().open(Shared.assetName(p));
        } else {
          return false;
        }
      } else if (src.isDirectory() || !src.exists()) {
        return false;
      } else {
        if (move && src.renameTo(dst)) {
          return true;
        }
        in = new FileInputStream(src);
      }
      FileOutputStream out = new FileOutputStream(dst);
      try {
        byte[] buf = new byte[32768];
        int n;
        while ((n = in.read(buf)) > 0) {
          if (batchCancel || (move && moveCancel) || (!move && copyCancel)) {
            return false;
          }
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
      if (move && src != null && !Shared.isAssetPath(Shared.clean(fP))
          && fS != FileScope.Asset) {
        try {
          src.delete();
        } catch (Exception e) {
          // abaikan
        }
      }
      return true;
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

  private void batchFinished(final boolean move, final boolean success, final String sc,
      final String folder, final int doneCount, final int failCount, final String msg) {
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          if (move) {
            MoveBatchFinished(success, sc, folder, doneCount, failCount, msg);
          } else {
            CopyBatchFinished(success, sc, folder, doneCount, failCount, msg);
          }
        }
      });
    } catch (Exception e) {
      if (move) {
        MoveBatchFinished(success, sc, folder, doneCount, failCount, msg);
      } else {
        CopyBatchFinished(success, sc, folder, doneCount, failCount, msg);
      }
    }
    if (!success && !"Dibatalkan".equals(msg)) {
      Shared.err(this, move ? "Move" : "Copy", 0, msg);
    }
  }

  @SuppressWarnings("unchecked")
  private List<String> toStringList(Object paths) {
    ArrayList<String> out = new ArrayList<String>();
    try {
      if (paths == null) {
        return out;
      }
      if (paths instanceof List) {
        List<Object> l = (List<Object>) paths;
        for (int i = 0; i < l.size(); i = i + 1) {
          Object o = l.get(i);
          if (o != null) {
            String s = o.toString();
            if (s.length() > 0) {
              out.add(s);
            }
          }
        }
      } else {
        String s = paths.toString();
        if (s.length() > 0) {
          out.add(s);
        }
      }
    } catch (Exception e) {
      // abaikan
    }
    return out;
  }

  // ================= FILE PICKER =================

  @SimpleFunction(description = "Buka pemilih file. mimeType contoh: image/* (kosong = semua). "
      + "Hasil lewat AfterFilePicked (uri + path + nama file). Satu picker per saat.")
  public void OpenFilePicker(String mimeType) {
    if (!Shared.claimSlot("pick")) {
      Shared.err(this, "Pick", 0, "picker sudah dibuka");
      return;
    }
    try {
      Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
      intent.addCategory(Intent.CATEGORY_OPENABLE);
      String mt = Shared.clean(mimeType);
      intent.setType(mt.length() == 0 ? "*/*" : mt);
      int rc = form.registerForActivityResult(this);
      synchronized (picks) {
        picks.put(Integer.valueOf(rc), Integer.valueOf(1));
      }
      activity.startActivityForResult(intent, rc);
    } catch (Exception e) {
      Shared.releaseSlot("pick");
      String m = e.getMessage();
      Shared.err(this, "Pick", 0, m == null ? "picker gagal dibuka" : m);
      AfterFilePicked(false, "", "", "", "");
    }
  }

  public void resultReturned(int requestCode, int resultCode, Intent data) {
    Integer hit;
    synchronized (picks) {
      hit = picks.remove(Integer.valueOf(requestCode));
    }
    if (hit == null) {
      return;
    }
    Shared.releaseSlot("pick");
    try {
      if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
        final Uri uri = data.getData();
        final String uriStr = uri.toString();
        final String path = resolveUriToPath(uri);
        AfterFilePicked(true, "Cache", uriStr, path, Shared.fileNameOf(path));
      } else {
        AfterFilePicked(false, "", "", "", "");
      }
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Pick", 0, m == null ? "picker gagal" : m);
      AfterFilePicked(false, "", "", "", "");
    }
  }

  private String resolveUriToPath(Uri uri) {
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
      guess = Shared.sanitize(guess);
      File cache = context.getCacheDir();
      if (!cache.exists()) {
        cache.mkdirs();
      }
      File dst = Shared.uniqueFile(cache, "pick_" + guess);
      InputStream in = context.getContentResolver().openInputStream(uri);
      try {
        FileOutputStream out = new FileOutputStream(dst);
        try {
          Shared.copyStream(in, out);
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
      + "Hasil lewat AfterOpenFile. Asset disalin ke cache dulu.")
  public void OpenFile(FileScope scope, String path) {
    try {
      String p = Shared.clean(path);
      File f = Shared.resolve(context, scope, path);
      if (f == null) {
        if (scope == FileScope.Asset || Shared.isAssetPath(p)) {
          File cache = context.getCacheDir();
          if (!cache.exists()) {
            cache.mkdirs();
          }
          File dst = new File(cache, Shared.sanitize(
              Shared.fileNameOf(Shared.assetName(p))));
          InputStream in = context.getAssets().open(Shared.assetName(p));
          try {
            FileOutputStream out = new FileOutputStream(dst);
            try {
              Shared.copyStream(in, out);
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
          Shared.err(this, "Open", 0, "path tidak valid");
          AfterOpenFile(false, "path tidak valid");
          return;
        }
      }
      if (!f.exists()) {
        Shared.err(this, "Open", 0, "file tidak ada");
        AfterOpenFile(false, "file tidak ada");
        return;
      }
      Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.setDataAndType(Uri.fromFile(f), Shared.mimeFor(f.getName()));
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      activity.startActivity(intent);
      AfterOpenFile(true, "");
    } catch (Exception e) {
      String m = e.getMessage();
      if (m == null) {
        m = "tidak ada aplikasi pembuka / file tidak bisa dibuka";
      }
      Shared.err(this, "Open", 0, m);
      AfterOpenFile(false, m);
    }
  }

  // ================= EVENTS =================

  @SimpleEvent(description = "Bacaan dimulai: persiapan (tampilkan loading).")
  public void ReadStarted(String scope, String path) {
    EventDispatcher.dispatchEvent(this, "ReadStarted", scope, path);
  }

  @SimpleEvent(description = "Hasil ReadText: success pertama, scope menemani path, "
      + "fileName paling belakang, content = isi file.")
  public void GotText(boolean success, String scope, String path, String fileName,
      String content) {
    EventDispatcher.dispatchEvent(this, "GotText", Boolean.valueOf(success), scope,
        path, fileName, content);
  }

  @SimpleEvent(description = "Simpan base64 ke file dimulai: siapkan loading.")
  public void Base64FileStarted() {
    EventDispatcher.dispatchEvent(this, "Base64FileStarted");
  }

  @SimpleEvent(description = "Hasil SaveBase64ToFile: success pertama, scope menemani path, "
      + "fileName paling belakang.")
  public void AfterBase64FileReady(boolean success, String scope, String path,
      String fileName, String message) {
    EventDispatcher.dispatchEvent(this, "AfterBase64FileReady",
        Boolean.valueOf(success), scope, path, fileName, message);
  }

  @SimpleEvent(description = "Salinan dimulai: siapkan tampilan progres.")
  public void CopyStarted(String fileName, String destPath) {
    EventDispatcher.dispatchEvent(this, "CopyStarted", fileName, destPath);
  }

  @SimpleEvent(description = "Progres salinan berubah: persen 0-100 + byte.")
  public void CopyProgressChanged(int percent, long copiedBytes, long totalBytes) {
    EventDispatcher.dispatchEvent(this, "CopyProgressChanged",
        Integer.valueOf(percent), Long.valueOf(copiedBytes), Long.valueOf(totalBytes));
  }

  @SimpleEvent(description = "Salinan selesai: success pertama, scope menemani destPath, "
      + "fileName paling belakang.")
  public void CopyFinished(boolean success, String scope, String destPath, String fileName,
      String message) {
    EventDispatcher.dispatchEvent(this, "CopyFinished", Boolean.valueOf(success), scope,
        destPath, fileName, message);
  }

  @SimpleEvent(description = "Pindahan dimulai: siapkan tampilan progres.")
  public void MoveStarted(String fileName, String destPath) {
    EventDispatcher.dispatchEvent(this, "MoveStarted", fileName, destPath);
  }

  @SimpleEvent(description = "Progres pindahan berubah: persen 0-100 + byte.")
  public void MoveProgressChanged(int percent, long movedBytes, long totalBytes) {
    EventDispatcher.dispatchEvent(this, "MoveProgressChanged",
        Integer.valueOf(percent), Long.valueOf(movedBytes), Long.valueOf(totalBytes));
  }

  @SimpleEvent(description = "Pindahan selesai: success pertama, scope menemani destPath, "
      + "fileName paling belakang.")
  public void MoveFinished(boolean success, String scope, String destPath, String fileName,
      String message) {
    EventDispatcher.dispatchEvent(this, "MoveFinished", Boolean.valueOf(success), scope,
        destPath, fileName, message);
  }

  @SimpleEvent(description = "Batch salinan dimulai: total file + folder tujuan.")
  public void CopyBatchStarted(int total, String destFolder) {
    EventDispatcher.dispatchEvent(this, "CopyBatchStarted",
        Integer.valueOf(total), destFolder);
  }

  @SimpleEvent(description = "Progres batch salinan berubah: index/total + nama file + "
      + "persen file itu.")
  public void CopyBatchProgressChanged(int index, int total, String fileName,
      int filePercent) {
    EventDispatcher.dispatchEvent(this, "CopyBatchProgressChanged",
        Integer.valueOf(index), Integer.valueOf(total), fileName,
        Integer.valueOf(filePercent));
  }

  @SimpleEvent(description = "Batch salinan selesai: success + scope + folder + "
      + "jumlah ok/gagal.")
  public void CopyBatchFinished(boolean success, String scope, String destFolder,
      int doneCount, int failCount, String message) {
    EventDispatcher.dispatchEvent(this, "CopyBatchFinished", Boolean.valueOf(success),
        scope, destFolder, Integer.valueOf(doneCount), Integer.valueOf(failCount),
        message);
  }

  @SimpleEvent(description = "Batch pindahan dimulai: total file + folder tujuan.")
  public void MoveBatchStarted(int total, String destFolder) {
    EventDispatcher.dispatchEvent(this, "MoveBatchStarted",
        Integer.valueOf(total), destFolder);
  }

  @SimpleEvent(description = "Progres batch pindahan berubah: index/total + nama file + "
      + "persen file itu.")
  public void MoveBatchProgressChanged(int index, int total, String fileName,
      int filePercent) {
    EventDispatcher.dispatchEvent(this, "MoveBatchProgressChanged",
        Integer.valueOf(index), Integer.valueOf(total), fileName,
        Integer.valueOf(filePercent));
  }

  @SimpleEvent(description = "Batch pindahan selesai: success + scope + folder + "
      + "jumlah ok/gagal.")
  public void MoveBatchFinished(boolean success, String scope, String destFolder,
      int doneCount, int failCount, String message) {
    EventDispatcher.dispatchEvent(this, "MoveBatchFinished", Boolean.valueOf(success),
        scope, destFolder, Integer.valueOf(doneCount), Integer.valueOf(failCount),
        message);
  }

  @SimpleEvent(description = "Hasil OpenFilePicker: success pertama, scope menemani path, "
      + "fileName paling belakang. success=false = dibatalkan user.")
  public void AfterFilePicked(boolean success, String scope, String uri, String path,
      String fileName) {
    EventDispatcher.dispatchEvent(this, "AfterFilePicked", Boolean.valueOf(success),
        scope, uri, path, fileName);
  }

  @SimpleEvent(description = "Hasil OpenFile: success + pesan.")
  public void AfterOpenFile(boolean success, String message) {
    EventDispatcher.dispatchEvent(this, "AfterOpenFile", Boolean.valueOf(success),
        message);
  }

  @SimpleEvent(description = "SATU-SATUNYA error Tools_File. component = nama operasi "
      + "(bandingkan dengan blok Op...). id selalu 0 (tanpa id).")
  public void ErrorOccurred(String component, int id, String message) {
    EventDispatcher.dispatchEvent(this, "ErrorOccurred",
        component, Integer.valueOf(id), message);
  }

  // ================= OP =================

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

  @SimpleProperty(description = "Nama operasi File umum/instan (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpFile() {
    return "File";
  }

  @SimpleProperty(description = "Nama operasi Pick (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpPick() {
    return "Pick";
  }

  @SimpleProperty(description = "Nama operasi Open (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpOpen() {
    return "Open";
  }

  @SimpleProperty(description = "Nama operasi Base64 (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpBase64() {
    return "Base64";
  }

  // ================= destroy =================

  public void onDestroy() {
    try {
      form.unregisterForActivityResult(this);
    } catch (Exception e) {
      // abaikan
    }
    copyCancel = true;
    moveCancel = true;
    batchCancel = true;
    Shared.releaseSlot("copy");
    Shared.releaseSlot("move");
    Shared.releaseSlot("read");
    Shared.releaseSlot("pick");
    Shared.releaseSlot("b64file");
  }
}
