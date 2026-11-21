package com.fv7.tools;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Base64;

import com.fv7.tools.helpers.Type;
import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.Options;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.ActivityResultListener;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.OnDestroyListener;
import com.google.appinventor.components.runtime.PermissionResultHandler;
import com.google.appinventor.components.runtime.util.AsynchUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@DesignerComponent(
    version = 23,
    description = "Tools_File: file/folder teks, salin/pindah/hapus/rename, direktori, izin "
        + "storage 1 pintu (CallSetStoragePermission + cek + GotPermission), "
        + "picker/opener, base64-ke-file, info ruang. Konvensi awalan: "
        + "Is = boolean (colok ke if), Call = async + hasil lewat event, Set = tanpa "
        + "feedback, Get = nilai balik sinkron. Semua path otomatis di-root ke "
        + "/storage/emulated/0 (\"/Download/a.jpg\" jadi "
        + "\"/storage/emulated/0/Download/a.jpg\"; absolut apa adanya). Salin/pindah "
        + "digabung satu blok CallCopyOrMoveFile (isMove) + info duplikat. "
        + "Izin kamera/mic/lokasi = CADANGAN RESMI untuk blok foto/rekam/lokasi (hadir "
        + "mulai v21); hapus bila batal. Keluarga Open = panggil dunia luar: OpenFile + "
        + "OpenFilePicker berperilaku Call (hasil lewat event), OpenAllFilesAccessSettings "
        + "berperilaku Set (berangkat saja, tanpa feedback).",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    androidMinSdk = 21,
    iconName = "aiwebres/tools_file.jpg",
    helpUrl = "https://github.com/VB-7/MiT-File_Manager")
@SimpleObject(external = true)
@UsesPermissions({"android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE",
    "android.permission.MANAGE_EXTERNAL_STORAGE",
    "android.permission.CAMERA",
    "android.permission.RECORD_AUDIO",
    "android.permission.POST_NOTIFICATIONS",
    "android.permission.READ_MEDIA_IMAGES",
    "android.permission.READ_MEDIA_VIDEO",
    "android.permission.READ_MEDIA_AUDIO",
    "android.permission.ACCESS_FINE_LOCATION",
    "android.permission.ACCESS_COARSE_LOCATION"})
public class Tools_File extends AndroidNonvisibleComponent
    implements ActivityResultListener, OnDestroyListener {

  private final Context context;
  private final Activity activity;

  private volatile boolean copyMoveCancel = false;
  private volatile String lastBase64Path = "";

  private final Map<Integer, Integer> picks = new HashMap<Integer, Integer>();
  private static final long PROGRESS_MIN_MS = 400;

  private static final String READ_PERM =
      "android.permission.READ_EXTERNAL_STORAGE";
  private static final String WRITE_PERM =
      "android.permission.WRITE_EXTERNAL_STORAGE";

  public Tools_File(ComponentContainer container) {
    super(container.$form());
    this.context = container.$context();
    this.activity = container.$context();
  }

  // ================= bantu path =================

  private File rootedFile(String path) {
    return Shared.resolveRooted(path);
  }

  private boolean wantAsset(String path) {
    return Shared.isAssetPath(Shared.clean(path));
  }

  private boolean existsInAssets(String assetPath) {
    try {
      InputStream in = context.getAssets().open(Shared.assetName(assetPath));
      try {
        in.close();
      } catch (Exception e) {
        // abaikan
      }
      return true;
    } catch (Exception e) {
      try {
        String[] l = context.getAssets().list(Shared.assetName(assetPath));
        return l != null && l.length > 0;
      } catch (Exception e2) {
        return false;
      }
    }
  }

  /** Cari folder dengan toleransi titik: "x" tidak ada -> coba ".x". */
  private File findDir(String folder) {
    File d = rootedFile(folder);
    if (d != null && d.isDirectory()) {
      return d;
    }
    try {
      String p = Shared.clean(folder);
      String name = Shared.fileNameOf(p);
      if (name.length() > 0 && !name.startsWith(".")) {
        String parent = Shared.parentOf(p);
        String alt = parent.length() == 0 ? "." + name
            : parent + "/." + name;
        File g = rootedFile(alt);
        if (g != null && g.isDirectory()) {
          return g;
        }
      }
    } catch (Exception e) {
      // abaikan
    }
    return d;
  }

  private void fireOnUi(final Runnable r) {
    try {
      form.runOnUiThread(r);
    } catch (Exception e) {
      try {
        activity.runOnUiThread(r);
      } catch (Exception e2) {
        // abaikan
      }
    }
  }

  private List<String> toStringList(Object paths) {
    ArrayList<String> out = new ArrayList<String>();
    try {
      if (paths instanceof List) {
        List<?> l = (List<?>) paths;
        for (int i = 0; i < l.size(); i = i + 1) {
          Object o = l.get(i);
          if (o != null) {
            String s = String.valueOf(o).trim();
            if (s.length() > 0) {
              out.add(s);
            }
          }
        }
      } else if (paths != null) {
        String s = String.valueOf(paths).trim();
        if (s.length() > 0) {
          String[] parts = s.split(",");
          for (int i = 0; i < parts.length; i = i + 1) {
            String t = parts[i].trim();
            if (t.length() > 0) {
              out.add(t);
            }
          }
        }
      }
    } catch (Exception e) {
      // abaikan
    }
    return out;
  }

  // ================= IS (boolean, colok ke if) =================

  @SimpleFunction(description = "Apakah file ATAU folder ada di jalur "
      + "(otomatis di-root /storage/emulated/0; absolut apa adanya). "
      + "Aset (//...) bisa dicek.")
  public boolean IsFileOrDirectoryExists(String path) {
    try {
      String p = Shared.clean(path);
      if (wantAsset(p)) {
        if (existsInAssets(p)) {
          return true;
        }
        try {
          String[] l = context.getAssets().list(Shared.assetName(p));
          return l != null && l.length > 0;
        } catch (Exception e) {
          return false;
        }
      }
      File f = rootedFile(p);
      return f != null && f.exists();
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleFunction(description = "Apakah file .nomedia ada di dalam folder. "
      + "path seperti IsFileOrDirectoryExists.")
  public boolean IsNomediaExists(String folder) {
    try {
      String p = Shared.clean(folder);
      if (wantAsset(p)) {
        try {
          String[] l = context.getAssets().list(Shared.assetName(p));
          if (l != null) {
            for (int i = 0; i < l.length; i = i + 1) {
              if (".nomedia".equals(l[i])) {
                return true;
              }
            }
          }
        } catch (Exception e) {
          // abaikan
        }
        return false;
      }
      File d = rootedFile(p);
      if (d == null || !d.isDirectory()) {
        return false;
      }
      return new File(d, ".nomedia").isFile();
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleFunction(description = "Apakah directory ada di jalur DAN hidden "
      + "(nama diawali titik). Boleh tulis dengan/tanpa titik. "
      + "path seperti IsFileOrDirectoryExists.")
  public boolean IsHiddenDirectory(String directory) {
    try {
      String p = Shared.clean(directory);
      if (wantAsset(p)) {
        return Shared.fileNameOf(Shared.assetName(p)).startsWith(".");
      }
      File d = findDir(p);
      if (d == null || !d.isDirectory()) {
        return false;
      }
      return d.getName().startsWith(".");
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleFunction(description = "True bila salinan/pindahan CallCopyOrMoveFile "
      + "yang masih berjalan. Khusus operasi itu (slot copyormove).")
  public boolean IsCopyOrMoveBusy() {
    return Shared.slotBusy("copyormove");
  }

  // ================= CALL: direktori & nomedia =================

  @SimpleFunction(description = "CALL: jadikan directory hidden (nama jadi "
      + ".nama). Sudah hidden = success. Hasil lewat DirectoryHidden.")
  public void CallSetHiddenDirectory(final String directory) {
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        if (!Shared.ensurePermission(form, activity, Shared.PERM_WRITE_STORAGE)) {
          final String permMsg = "izin belum granted (Android 11+: aktifkan 'Akses semua file')";
          Shared.err(Tools_File.this, "Directory", 0, permMsg);
          fireOnUi(new Runnable() {
            public void run() {
              DirectoryHidden(false, Shared.clean(directory), permMsg, WRITE_PERM);
            }
          });
          return;
        }
        String need = "";
        try {
          File d = findDir(directory);
          if (d == null || !d.isDirectory()) {
            final String msg = "directory tidak ada";
            Shared.err(Tools_File.this, "Directory", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                DirectoryHidden(false, Shared.clean(directory), msg, "");
              }
            });
            return;
          }
          final String abs = d.getAbsolutePath();
          if (d.getName().startsWith(".")) {
            fireOnUi(new Runnable() {
              public void run() {
                DirectoryHidden(true, abs, "sudah hidden", "");
              }
            });
            return;
          }
          File t = new File(d.getParentFile(), "." + d.getName());
          if (t.exists()) {
            final String msg = "nama hidden sudah dipakai";
            Shared.err(Tools_File.this, "Directory", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                DirectoryHidden(false, abs, msg, "");
              }
            });
            return;
          }
          final boolean ok = d.renameTo(t);
          final String np = ok ? t.getAbsolutePath() : abs;
          final String msg = ok ? "" : "rename gagal (mungkin izin)";
          if (!ok) {
            need = WRITE_PERM;
            Shared.err(Tools_File.this, "Directory", 0, msg);
          }
          final String needF = need;
          fireOnUi(new Runnable() {
            public void run() {
              DirectoryHidden(ok, np, msg, needF);
            }
          });
        } catch (SecurityException se) {
          final String msg = "ditolak sistem (izin)";
          Shared.err(Tools_File.this, "Directory", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              DirectoryHidden(false, Shared.clean(directory), msg, WRITE_PERM);
            }
          });
        } catch (Exception e) {
          final String m = e.getMessage();
          final String msg = m == null ? "gagal hidden" : m;
          Shared.err(Tools_File.this, "Directory", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              DirectoryHidden(false, Shared.clean(directory), msg, "");
            }
          });
        }
      }
    });
  }

  @SimpleFunction(description = "CALL: buat file .nomedia di dalam folder "
      + "(sembunyikan dari galeri/musik). Hasil lewat NomediaFileCreated.")
  public void CallMakeNomediaFile(final String folder) {
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        if (!Shared.ensurePermission(form, activity, Shared.PERM_WRITE_STORAGE)) {
          final String permMsg = "izin belum granted (Android 11+: aktifkan 'Akses semua file')";
          Shared.err(Tools_File.this, "Directory", 0, permMsg);
          fireOnUi(new Runnable() {
            public void run() {
              NomediaFileCreated(false, Shared.clean(folder), permMsg, WRITE_PERM);
            }
          });
          return;
        }
        try {
          if (wantAsset(folder)) {
            final String msg = "tidak bisa tulis ke Asset";
            Shared.err(Tools_File.this, "Directory", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                NomediaFileCreated(false, Shared.clean(folder), msg, "");
              }
            });
            return;
          }
          File d = rootedFile(folder);
          if (d == null) {
            final String msg = "folder tidak valid";
            Shared.err(Tools_File.this, "Directory", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                NomediaFileCreated(false, Shared.clean(folder), msg, "");
              }
            });
            return;
          }
          d.mkdirs();
          File n = new File(d, ".nomedia");
          boolean ok = n.isFile();
          if (!ok) {
            try {
              ok = n.createNewFile();
            } catch (SecurityException se) {
              final String msg = "ditolak sistem (izin)";
              Shared.err(Tools_File.this, "Directory", 0, msg);
              final String abs = d.getAbsolutePath();
              fireOnUi(new Runnable() {
                public void run() {
                  NomediaFileCreated(false, abs, msg, WRITE_PERM);
                }
              });
              return;
            }
          }
          final String abs = n.getAbsolutePath();
          final boolean okF = ok;
          final String msg = ok ? "" : "gagal buat .nomedia";
          if (!ok) {
            Shared.err(Tools_File.this, "Directory", 0, msg);
          }
          fireOnUi(new Runnable() {
            public void run() {
              NomediaFileCreated(okF, abs, msg, okF ? "" : WRITE_PERM);
            }
          });
        } catch (Exception e) {
          final String m = e.getMessage();
          final String msg = m == null ? "gagal buat .nomedia" : m;
          Shared.err(Tools_File.this, "Directory", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              NomediaFileCreated(false, Shared.clean(folder), msg, "");
            }
          });
        }
      }
    });
  }

  @SimpleFunction(description = "CALL: hapus file .nomedia di folder. "
      + "Tidak ada = success. Hasil lewat NomediaFileDeleted.")
  public void CallDeleteNomediaFile(final String folder) {
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        if (!Shared.ensurePermission(form, activity, Shared.PERM_WRITE_STORAGE)) {
          final String permMsg = "izin belum granted (Android 11+: aktifkan 'Akses semua file')";
          Shared.err(Tools_File.this, "Directory", 0, permMsg);
          fireOnUi(new Runnable() {
            public void run() {
              NomediaFileDeleted(false, Shared.clean(folder), permMsg, WRITE_PERM);
            }
          });
          return;
        }
        try {
          if (wantAsset(folder)) {
            final String msg = "tidak bisa tulis ke Asset";
            Shared.err(Tools_File.this, "Directory", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                NomediaFileDeleted(false, Shared.clean(folder), msg, "");
              }
            });
            return;
          }
          File d = rootedFile(folder);
          if (d == null || !d.isDirectory()) {
            final String msg = "folder tidak ada";
            Shared.err(Tools_File.this, "Directory", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                NomediaFileDeleted(false, Shared.clean(folder), msg, "");
              }
            });
            return;
          }
          File n = new File(d, ".nomedia");
          final String abs = n.getAbsolutePath();
          boolean ok = !n.exists() || n.delete();
          final boolean okF = ok;
          final String msg = ok ? "" : "gagal hapus .nomedia";
          if (!ok) {
            Shared.err(Tools_File.this, "Directory", 0, msg);
          }
          fireOnUi(new Runnable() {
            public void run() {
              NomediaFileDeleted(okF, abs, msg, okF ? "" : WRITE_PERM);
            }
          });
        } catch (SecurityException se) {
          final String msg = "ditolak sistem (izin)";
          Shared.err(Tools_File.this, "Directory", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              NomediaFileDeleted(false, Shared.clean(folder), msg, WRITE_PERM);
            }
          });
        } catch (Exception e) {
          final String m = e.getMessage();
          final String msg = m == null ? "gagal hapus .nomedia" : m;
          Shared.err(Tools_File.this, "Directory", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              NomediaFileDeleted(false, Shared.clean(folder), msg, "");
            }
          });
        }
      }
    });
  }

  @SimpleFunction(description = "CALL: buat folder (termasuk induknya). "
      + "Sudah ada = success. Hasil lewat DirectoryCreated.")
  public void CallMakeDirectory(final String folder) {
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        if (!Shared.ensurePermission(form, activity, Shared.PERM_WRITE_STORAGE)) {
          final String permMsg = "izin belum granted (Android 11+: aktifkan 'Akses semua file')";
          Shared.err(Tools_File.this, "Directory", 0, permMsg);
          fireOnUi(new Runnable() {
            public void run() {
              DirectoryCreated(false, Shared.clean(folder), permMsg, WRITE_PERM);
            }
          });
          return;
        }
        try {
          if (wantAsset(folder)) {
            final String msg = "tidak bisa tulis ke Asset";
            Shared.err(Tools_File.this, "Directory", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                DirectoryCreated(false, Shared.clean(folder), msg, "");
              }
            });
            return;
          }
          File d = rootedFile(folder);
          if (d == null) {
            final String msg = "folder tidak valid";
            Shared.err(Tools_File.this, "Directory", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                DirectoryCreated(false, Shared.clean(folder), msg, "");
              }
            });
            return;
          }
          if (!d.isDirectory()) {
            d.mkdirs();
          }
          final String abs = d.getAbsolutePath();
          final boolean ok = d.isDirectory();
          final String msg = ok ? "" : "gagal buat folder (mungkin izin)";
          if (!ok) {
            Shared.err(Tools_File.this, "Directory", 0, msg);
          }
          fireOnUi(new Runnable() {
            public void run() {
              DirectoryCreated(ok, abs, msg, ok ? "" : WRITE_PERM);
            }
          });
        } catch (SecurityException se) {
          final String msg = "ditolak sistem (izin)";
          Shared.err(Tools_File.this, "Directory", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              DirectoryCreated(false, Shared.clean(folder), msg, WRITE_PERM);
            }
          });
        } catch (Exception e) {
          final String m = e.getMessage();
          final String msg = m == null ? "gagal buat folder" : m;
          Shared.err(Tools_File.this, "Directory", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              DirectoryCreated(false, Shared.clean(folder), msg, "");
            }
          });
        }
      }
    });
  }

  // ================= CALL: hapus & rename =================

  @SimpleFunction(description = "CALL: hapus file/folder (folder rekursif). "
      + "Tidak ada = success. Hasil lewat FileDeleted.")
  public void CallDeleteFile(final String path) {
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        if (!Shared.ensurePermission(form, activity, Shared.PERM_WRITE_STORAGE)) {
          final String permMsg = "izin belum granted (Android 11+: aktifkan 'Akses semua file')";
          Shared.err(Tools_File.this, "File", 0, permMsg);
          fireOnUi(new Runnable() {
            public void run() {
              FileDeleted(false, Shared.clean(path), permMsg, WRITE_PERM);
            }
          });
          return;
        }
        try {
          if (wantAsset(path)) {
            final String msg = "tidak bisa hapus dari Asset";
            Shared.err(Tools_File.this, "File", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                FileDeleted(false, Shared.clean(path), msg, "");
              }
            });
            return;
          }
          File f = rootedFile(path);
          if (f == null) {
            final String msg = "path tidak valid";
            Shared.err(Tools_File.this, "File", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                FileDeleted(false, Shared.clean(path), msg, "");
              }
            });
            return;
          }
          final String abs = f.getAbsolutePath();
          boolean ok = Shared.deleteRecursive(f);
          final boolean okF = ok;
          final String msg = ok ? "" : "gagal hapus (mungkin izin)";
          if (!ok) {
            Shared.err(Tools_File.this, "File", 0, msg);
          }
          fireOnUi(new Runnable() {
            public void run() {
              FileDeleted(okF, abs, msg, okF ? "" : WRITE_PERM);
            }
          });
        } catch (SecurityException se) {
          final String msg = "ditolak sistem (izin)";
          Shared.err(Tools_File.this, "File", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              FileDeleted(false, Shared.clean(path), msg, WRITE_PERM);
            }
          });
        } catch (Exception e) {
          final String m = e.getMessage();
          final String msg = m == null ? "gagal hapus" : m;
          Shared.err(Tools_File.this, "File", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              FileDeleted(false, Shared.clean(path), msg, "");
            }
          });
        }
      }
    });
  }

  @SimpleFunction(description = "CALL: hapus BANYAK path sekaligus (tanpa loop "
      + "manual). paths = list, atau teks pisah koma (\"/a.jpg, /b.jpg\"; koma "
      + "boleh spasi). Hasil lewat FileListDeleted (total + terhapus).")
  public void CallDeleteList(final Object paths) {
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        if (!Shared.ensurePermission(form, activity, Shared.PERM_WRITE_STORAGE)) {
          final String permMsg = "izin belum granted (Android 11+: aktifkan 'Akses semua file')";
          Shared.err(Tools_File.this, "File", 0, permMsg);
          fireOnUi(new Runnable() {
            public void run() {
              FileListDeleted(false, 0, 0, permMsg, WRITE_PERM);
            }
          });
          return;
        }
        try {
          List<String> l = toStringList(paths);
          final int total = l.size();
          if (total == 0) {
            final String msg = "daftar kosong";
            Shared.err(Tools_File.this, "File", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                FileListDeleted(false, 0, 0, msg, "");
              }
            });
            return;
          }
          int done = 0;
          for (int i = 0; i < l.size(); i = i + 1) {
            try {
              if (wantAsset(l.get(i))) {
                continue;
              }
              File f = rootedFile(l.get(i));
              if (f != null && Shared.deleteRecursive(f)) {
                done = done + 1;
              }
            } catch (Exception e) {
              // lanjut ke berikutnya
            }
          }
          final int doneF = done;
          final boolean ok = doneF == total;
          final String msg = ok ? "" : "sebagian gagal (" + doneF
              + "/" + total + ")";
          if (!ok) {
            Shared.err(Tools_File.this, "File", 0, msg);
          }
          fireOnUi(new Runnable() {
            public void run() {
              FileListDeleted(ok, total, doneF, msg, ok ? "" : WRITE_PERM);
            }
          });
        } catch (Exception e) {
          final String m = e.getMessage();
          final String msg = m == null ? "gagal hapus list" : m;
          Shared.err(Tools_File.this, "File", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              FileListDeleted(false, 0, 0, msg, "");
            }
          });
        }
      }
    });
  }

  @SimpleFunction(description = "CALL: ganti nama FILE (tanpa pindah folder). "
      + "newName = nama saja. Hasil lewat FileRenamed (path = nama baru).")
  public void CallRenameFile(final String path, final String newName) {
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        if (!Shared.ensurePermission(form, activity, Shared.PERM_WRITE_STORAGE)) {
          final String permMsg = "izin belum granted (Android 11+: aktifkan 'Akses semua file')";
          Shared.err(Tools_File.this, "File", 0, permMsg);
          fireOnUi(new Runnable() {
            public void run() {
              FileRenamed(false, Shared.clean(path), permMsg, WRITE_PERM);
            }
          });
          return;
        }
        try {
          if (wantAsset(path)) {
            final String msg = "tidak bisa rename di Asset";
            Shared.err(Tools_File.this, "File", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                FileRenamed(false, Shared.clean(path), msg, "");
              }
            });
            return;
          }
          File f = rootedFile(path);
          if (f == null || !f.isFile()) {
            final String msg = "file tidak ada";
            Shared.err(Tools_File.this, "File", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                FileRenamed(false, Shared.clean(path), msg, "");
              }
            });
            return;
          }
          String nn = Shared.sanitize(Shared.clean(newName));
          File t = new File(f.getParentFile(), nn);
          if (t.exists()) {
            final String msg = "nama sudah dipakai";
            Shared.err(Tools_File.this, "File", 0, msg);
            final String abs = f.getAbsolutePath();
            fireOnUi(new Runnable() {
              public void run() {
                FileRenamed(false, abs, msg, "");
              }
            });
            return;
          }
          final boolean ok = f.renameTo(t);
          final String np = ok ? t.getAbsolutePath() : f.getAbsolutePath();
          final String msg = ok ? "" : "rename gagal (mungkin izin)";
          if (!ok) {
            Shared.err(Tools_File.this, "File", 0, msg);
          }
          fireOnUi(new Runnable() {
            public void run() {
              FileRenamed(ok, np, msg, ok ? "" : WRITE_PERM);
            }
          });
        } catch (SecurityException se) {
          final String msg = "ditolak sistem (izin)";
          Shared.err(Tools_File.this, "File", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              FileRenamed(false, Shared.clean(path), msg, WRITE_PERM);
            }
          });
        } catch (Exception e) {
          final String m = e.getMessage();
          final String msg = m == null ? "rename gagal" : m;
          Shared.err(Tools_File.this, "File", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              FileRenamed(false, Shared.clean(path), msg, "");
            }
          });
        }
      }
    });
  }

  @SimpleFunction(description = "CALL: ganti nama FOLDER (tanpa pindah). "
      + "newName = nama saja. Hasil lewat FolderRenamed.")
  public void CallRenameFolder(final String path, final String newName) {
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        if (!Shared.ensurePermission(form, activity, Shared.PERM_WRITE_STORAGE)) {
          final String permMsg = "izin belum granted (Android 11+: aktifkan 'Akses semua file')";
          Shared.err(Tools_File.this, "File", 0, permMsg);
          fireOnUi(new Runnable() {
            public void run() {
              FolderRenamed(false, Shared.clean(path), permMsg, WRITE_PERM);
            }
          });
          return;
        }
        try {
          if (wantAsset(path)) {
            final String msg = "tidak bisa rename di Asset";
            Shared.err(Tools_File.this, "File", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                FolderRenamed(false, Shared.clean(path), msg, "");
              }
            });
            return;
          }
          File f = findDir(path);
          if (f == null || !f.isDirectory()) {
            final String msg = "folder tidak ada";
            Shared.err(Tools_File.this, "File", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                FolderRenamed(false, Shared.clean(path), msg, "");
              }
            });
            return;
          }
          String nn = Shared.sanitize(Shared.clean(newName));
          File t = new File(f.getParentFile(), nn);
          if (t.exists()) {
            final String msg = "nama sudah dipakai";
            Shared.err(Tools_File.this, "File", 0, msg);
            final String abs = f.getAbsolutePath();
            fireOnUi(new Runnable() {
              public void run() {
                FolderRenamed(false, abs, msg, "");
              }
            });
            return;
          }
          final boolean ok = f.renameTo(t);
          final String np = ok ? t.getAbsolutePath() : f.getAbsolutePath();
          final String msg = ok ? "" : "rename gagal (mungkin izin)";
          if (!ok) {
            Shared.err(Tools_File.this, "File", 0, msg);
          }
          fireOnUi(new Runnable() {
            public void run() {
              FolderRenamed(ok, np, msg, ok ? "" : WRITE_PERM);
            }
          });
        } catch (SecurityException se) {
          final String msg = "ditolak sistem (izin)";
          Shared.err(Tools_File.this, "File", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              FolderRenamed(false, Shared.clean(path), msg, WRITE_PERM);
            }
          });
        } catch (Exception e) {
          final String m = e.getMessage();
          final String msg = m == null ? "rename gagal" : m;
          Shared.err(Tools_File.this, "File", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              FolderRenamed(false, Shared.clean(path), msg, "");
            }
          });
        }
      }
    });
  }

  // ================= CALL: tulis =================

  @SimpleFunction(description = "CALL: buat file kosong (folder induk dibuat "
      + "otomatis). Sudah ada = success. Hasil lewat EmptyFileCreated.")
  public void CallCreateEmptyFile(final String path) {
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        if (!Shared.ensurePermission(form, activity, Shared.PERM_WRITE_STORAGE)) {
          final String permMsg = "izin belum granted (Android 11+: aktifkan 'Akses semua file')";
          Shared.err(Tools_File.this, "File", 0, permMsg);
          fireOnUi(new Runnable() {
            public void run() {
              EmptyFileCreated(false, Shared.clean(path), permMsg, WRITE_PERM);
            }
          });
          return;
        }
        try {
          if (wantAsset(path)) {
            final String msg = "tidak bisa tulis ke Asset";
            Shared.err(Tools_File.this, "File", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                EmptyFileCreated(false, Shared.clean(path), msg, "");
              }
            });
            return;
          }
          File f = rootedFile(path);
          if (f == null) {
            final String msg = "path tidak valid";
            Shared.err(Tools_File.this, "File", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                EmptyFileCreated(false, Shared.clean(path), msg, "");
              }
            });
            return;
          }
          if (f.getParentFile() != null) {
            f.getParentFile().mkdirs();
          }
          boolean ok = f.isFile();
          if (!ok) {
            try {
              ok = f.createNewFile();
            } catch (SecurityException se) {
              final String msg = "ditolak sistem (izin)";
              Shared.err(Tools_File.this, "File", 0, msg);
              fireOnUi(new Runnable() {
                public void run() {
                  EmptyFileCreated(false, Shared.clean(path), msg, WRITE_PERM);
                }
              });
              return;
            }
          }
          final String abs = f.getAbsolutePath();
          final boolean okF = ok;
          final String msg = ok ? "" : "gagal buat file";
          if (!ok) {
            Shared.err(Tools_File.this, "File", 0, msg);
          }
          fireOnUi(new Runnable() {
            public void run() {
              EmptyFileCreated(okF, abs, msg, okF ? "" : WRITE_PERM);
            }
          });
        } catch (Exception e) {
          final String m = e.getMessage();
          final String msg = m == null ? "gagal buat file" : m;
          Shared.err(Tools_File.this, "File", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              EmptyFileCreated(false, Shared.clean(path), msg, "");
            }
          });
        }
      }
    });
  }

  @SimpleFunction(description = "CALL: tulis teks ke file (UTF-8; timpa isi "
      + "lama). append=true = sambung di akhir (sama seperti CallAppendFile). "
      + "Hanya untuk teks; file biner rusak bila lewat sini. Hasil lewat FileSaved.")
  public void CallSaveFile(final String path, final String content,
      final boolean append) {
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        if (!Shared.ensurePermission(form, activity, Shared.PERM_WRITE_STORAGE)) {
          final String permMsg = "izin belum granted (Android 11+: aktifkan 'Akses semua file')";
          Shared.err(Tools_File.this, "File", 0, permMsg);
          fireOnUi(new Runnable() {
            public void run() {
              FileSaved(false, Shared.clean(path), permMsg, WRITE_PERM);
            }
          });
          return;
        }
        try {
          if (wantAsset(path)) {
            final String msg = "tidak bisa tulis ke Asset";
            Shared.err(Tools_File.this, "File", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                FileSaved(false, Shared.clean(path), msg, "");
              }
            });
            return;
          }
          File f = rootedFile(path);
          if (f == null) {
            final String msg = "path tidak valid";
            Shared.err(Tools_File.this, "File", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                FileSaved(false, Shared.clean(path), msg, "");
              }
            });
            return;
          }
          if (f.getParentFile() != null) {
            f.getParentFile().mkdirs();
          }
          byte[] b = (content == null ? "" : content).getBytes("UTF-8");
          FileOutputStream out = new FileOutputStream(f, append);
          try {
            out.write(b);
            out.flush();
          } finally {
            try {
              out.close();
            } catch (Exception e) {
              // abaikan
            }
          }
          final String abs = f.getAbsolutePath();
          fireOnUi(new Runnable() {
            public void run() {
              FileSaved(true, abs, "", "");
            }
          });
        } catch (SecurityException se) {
          final String msg = "ditolak sistem (izin)";
          Shared.err(Tools_File.this, "File", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              FileSaved(false, Shared.clean(path), msg, WRITE_PERM);
            }
          });
        } catch (Exception e) {
          final String m = e.getMessage();
          final String msg = m == null ? "gagal simpan" : m;
          Shared.err(Tools_File.this, "File", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              FileSaved(false, Shared.clean(path), msg, "");
            }
          });
        }
      }
    });
  }

  @SimpleFunction(description = "CALL: sambung teks di AKHIR file (UTF-8; "
      + "tidak menimpa isi lama). Sama seperti CallSaveFile(append=true). "
      + "File belum ada = dibuat baru. Hasil lewat FileSaved.")
  public void CallAppendFile(final String path, final String content) {
    CallSaveFile(path, content, true);
  }

  @SimpleFunction(description = "CALL: simpan base64 LANGSUNG ke storage "
      + "(tanpa validasi gambar, lebih cepat). Hasil lewat Base64FileSaved. "
      + "Polling: GetLastBase64FilePath.")
  public void CallSaveBase64ToFile(final String base64, final String path) {
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        if (!Shared.ensurePermission(form, activity, Shared.PERM_WRITE_STORAGE)) {
          final String permMsg = "izin belum granted (Android 11+: aktifkan 'Akses semua file')";
          lastBase64Path = "";
          Shared.err(Tools_File.this, "Base64", 0, permMsg);
          fireOnUi(new Runnable() {
            public void run() {
              Base64FileSaved(false, Shared.clean(path), permMsg, WRITE_PERM);
            }
          });
          return;
        }
        try {
          if (wantAsset(path)) {
            lastBase64Path = "";
            final String msg = "tidak bisa tulis ke Asset";
            Shared.err(Tools_File.this, "Base64", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                Base64FileSaved(false, Shared.clean(path), msg, "");
              }
            });
            return;
          }
          File f = rootedFile(path);
          if (f == null) {
            lastBase64Path = "";
            final String msg = "path tidak valid";
            Shared.err(Tools_File.this, "Base64", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                Base64FileSaved(false, Shared.clean(path), msg, "");
              }
            });
            return;
          }
          String b = base64 == null ? "" : base64.trim();
          int ci = b.indexOf("base64,");
          if (ci >= 0) {
            b = b.substring(ci + 7);
          }
          b = b.replaceAll("\\s", "");
          if (b.length() == 0) {
            lastBase64Path = "";
            final String msg = "base64 kosong";
            Shared.err(Tools_File.this, "Base64", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                Base64FileSaved(false, Shared.clean(path), msg, "");
              }
            });
            return;
          }
          byte[] data = Base64.decode(b, Base64.DEFAULT);
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
          lastBase64Path = f.getAbsolutePath();
          final String abs = lastBase64Path;
          fireOnUi(new Runnable() {
            public void run() {
              Base64FileSaved(true, abs, "", "");
            }
          });
        } catch (SecurityException se) {
          lastBase64Path = "";
          final String msg = "ditolak sistem (izin)";
          Shared.err(Tools_File.this, "Base64", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              Base64FileSaved(false, Shared.clean(path), msg, WRITE_PERM);
            }
          });
        } catch (Exception e) {
          lastBase64Path = "";
          final String m = e.getMessage();
          final String msg = m == null ? "gagal simpan base64" : m;
          Shared.err(Tools_File.this, "Base64", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              Base64FileSaved(false, Shared.clean(path), msg, "");
            }
          });
        }
      }
    });
  }

  @SimpleFunction(description = "Fallback polling untuk Base64FileSaved: "
      + "path file base64 terakhir (kosong bila gagal/belum ada).")
  public String GetLastBase64FilePath() {
    return lastBase64Path == null ? "" : lastBase64Path;
  }

  // ================= CALL: baca teks =================

  @SimpleFunction(description = "CALL: baca file teks (UTF-8) di background. "
      + "Hanya untuk teks UTF-8; file biner/encoding lain isinya bisa kacau "
      + "(biner = baca via base64/pustaka lain). Hasil lewat GotText "
      + "(content = isi file). Mulai lewat ReadStarted.")
  public void CallReadText(final String path) {
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        if (!Shared.ensurePermission(form, activity, Shared.PERM_READ_STORAGE)) {
          final String permMsg = "izin belum granted (Android 11+: aktifkan 'Akses semua file')";
          Shared.err(Tools_File.this, "Read", 0, permMsg);
          fireOnUi(new Runnable() {
            public void run() {
              GotText(false, Shared.clean(path), "", "");
            }
          });
          return;
        }
        final String p = Shared.clean(path);
        fireOnUi(new Runnable() {
          public void run() {
            ReadStarted(p);
          }
        });
        try {
          if (wantAsset(p)) {
            InputStream in = context.getAssets().open(Shared.assetName(p));
            byte[] data;
            try {
              data = Shared.readAll(in);
            } finally {
              try {
                in.close();
              } catch (Exception e) {
                // abaikan
              }
            }
            final String txt = new String(data, "UTF-8");
            final String nm = Shared.fileNameOf(Shared.assetName(p));
            fireOnUi(new Runnable() {
              public void run() {
                GotText(true, "//" + Shared.assetName(p), nm, txt);
              }
            });
            return;
          }
          File f = rootedFile(p);
          if (f == null || !f.isFile()) {
            final String msg = "file tidak ada";
            Shared.err(Tools_File.this, "Read", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                GotText(false, p, "", "");
              }
            });
            return;
          }
          FileInputStream in = new FileInputStream(f);
          byte[] data;
          try {
            data = Shared.readAll(in);
          } finally {
            try {
              in.close();
            } catch (Exception e) {
              // abaikan
            }
          }
          final String txt = new String(data, "UTF-8");
          final String abs = f.getAbsolutePath();
          final String nm = f.getName();
          fireOnUi(new Runnable() {
            public void run() {
              GotText(true, abs, nm, txt);
            }
          });
        } catch (SecurityException se) {
          final String msg = "ditolak sistem (izin)";
          Shared.err(Tools_File.this, "Read", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              GotText(false, p, "", "");
            }
          });
        } catch (Exception e) {
          final String m = e.getMessage();
          final String msg = m == null ? "baca gagal" : m;
          Shared.err(Tools_File.this, "Read", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              GotText(false, p, "", "");
            }
          });
        }
      }
    });
  }

  // ================= CALL: salin / pindah =================

  @SimpleFunction(description = "CALL: Salin ATAU pindah file (isMove=true = "
      + "pindah; salah centang = file pindah padahal mau salin — untuk aman pakai "
      + "CallCopyFile/CallMoveFile bila ragu). Path otomatis di-root "
      + "/storage/emulated/0. Nama tujuan boleh beda. Bila tujuan SUDAH ADA: tidak "
      + "disalin, Finished(success=false, isDuplicated=true) + ErrorOccurred. "
      + "Asset hanya bisa jadi sumber. Satu job per saat.")
  public void CallCopyOrMoveFile(final String fromFileName, final String toFileName,
      final boolean isMove) {
    doCopyOrMove(isMove, fromFileName, toFileName);
  }

  @SimpleFunction(description = "CALL: Salin file (tidak pindah). Sama seperti "
      + "CallCopyOrMoveFile(isMove=false), tapi tanpa risiko salah centang. "
      + "Bila tujuan SUDAH ADA: Finished(success=false, isDuplicated=true). "
      + "Hasil lewat CopyOrMoveFinished.")
  public void CallCopyFile(final String fromFileName, final String toFileName) {
    doCopyOrMove(false, fromFileName, toFileName);
  }

  @SimpleFunction(description = "CALL: Pindah file (sumber ikut terhapus bila sukses). "
      + "Sama seperti CallCopyOrMoveFile(isMove=true), tapi tanpa risiko salah "
      + "centang. Bila tujuan SUDAH ADA: Finished(success=false, isDuplicated=true). "
      + "Hasil lewat CopyOrMoveFinished.")
  public void CallMoveFile(final String fromFileName, final String toFileName) {
    doCopyOrMove(true, fromFileName, toFileName);
  }

  private void doCopyOrMove(final boolean isMove, final String fromFileName,
      final String toFileName) {
    if (!Shared.claimSlot("copyormove")) {
      final String msg = "masih ada salinan/pindahan berjalan";
      Shared.err(this, "CopyOrMove", 0, msg);
      fireOnUi(new Runnable() {
        public void run() {
          CopyOrMoveFinished(false, Shared.clean(toFileName),
              Shared.fileNameOf(toFileName), false);
        }
      });
      return;
    }
    if (wantAsset(toFileName)) {
      Shared.releaseSlot("copyormove");
      final String msg = "tidak bisa salin ke Asset";
      Shared.err(this, "CopyOrMove", 0, msg);
      fireOnUi(new Runnable() {
        public void run() {
          CopyOrMoveFinished(false, Shared.clean(toFileName),
              Shared.fileNameOf(toFileName), false);
        }
      });
      return;
    }
    copyMoveCancel = false;
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        if (!Shared.ensurePermission(form, activity, Shared.PERM_WRITE_STORAGE)) {
          final String permMsg = "izin belum granted (Android 11+: aktifkan 'Akses semua file')";
          finishCopyOrMove(isMove, false, "", Shared.fileNameOf(toFileName), false, permMsg);
          Shared.releaseSlot("copyormove");
          return;
        }
        runCopyOrMove(isMove, fromFileName, toFileName);
      }
    });
  }

  @SimpleFunction(description = "SET: batalkan salinan/pindahan berjalan. "
      + "Tanpa feedback (file sebagian dihapus). Rantai: CopyOrMoveFinished "
      + "(success=false, need='dibatalkan').")
  public void SetCancelCopyOrMove() {
    copyMoveCancel = true;
  }

  private void runCopyOrMove(final boolean move, final String fP, final String tP) {
    String comp = "CopyOrMove";
    InputStream in = null;
    boolean fromAsset = false;
    String aName = "";
    File src = rootedFile(fP);
    long total = 0;
    try {
      if (src == null) {
        String p = Shared.clean(fP);
        if (wantAsset(p)) {
          fromAsset = true;
          aName = Shared.assetName(p);
        } else {
          finishCopyOrMove(move, false, "",
              Shared.fileNameOf(fP), false, "path sumber tidak valid");
          return;
        }
      } else if (src.isDirectory()) {
        finishCopyOrMove(move, false, "", src.getName(), false,
            "sumber adalah folder");
        return;
      } else if (!src.exists()) {
        finishCopyOrMove(move, false, "", src.getName(), false,
            "file sumber tidak ada");
        return;
      }
      File dst = rootedFile(tP);
      if (dst == null) {
        finishCopyOrMove(move, false, "",
            Shared.fileNameOf(tP), false, "tujuan tidak valid");
        return;
      }
      if (dst.exists() && dst.isDirectory()) {
        finishCopyOrMove(move, false, "", dst.getName(), false,
            "tujuan adalah folder");
        return;
      }
      if (dst.exists() && dst.isFile()) {
        // Sudah ada: JANGAN timpa. success=false + isDuplicated agar
        // logika if-success pemanggil tetap benar.
        finishCopyOrMove(move, false, dst.getAbsolutePath(), dst.getName(), true,
            "tujuan sudah ada, tidak ditimpa (rename manual / hapus dulu)");
        return;
      }
      if (dst.getParentFile() != null) {
        dst.getParentFile().mkdirs();
      }
      final String dAbs = dst.getAbsolutePath();
      final String dName = dst.getName();
      fireOnUi(new Runnable() {
        public void run() {
          CopyOrMoveStarted(Shared.clean(fP), dAbs, dName, move);
        }
      });
      if (fromAsset) {
        try {
          total = context.getAssets().openFd(aName).getLength();
        } catch (Exception e) {
          total = -1;
        }
        in = context.getAssets().open(aName);
      } else {
        if (move && src.renameTo(dst)) {
          fireCopyOrMoveProgress(100, 1, 1);
          finishCopyOrMove(true, true, dAbs, dName, false, "");
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
          if (copyMoveCancel) {
            finishCopyOrMove(move, false, "", "", false, "dibatalkan");
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
          if (pct != lastPct && now - lastFire >= PROGRESS_MIN_MS) {
            lastPct = pct;
            lastFire = now;
            fireCopyOrMoveProgress(pct, done, total);
          }
        }
        out.flush();
      } finally {
        try {
          out.close();
        } catch (Exception e) {
          // abaikan
        }
      }
      if (copyMoveCancel) {
        finishCopyOrMove(move, false, "", "", false, "dibatalkan");
        try {
          dst.delete();
        } catch (Exception e) {
          // abaikan
        }
        return;
      }
      fireCopyOrMoveProgress(100, total < 0 ? 0 : total,
          total < 0 ? 0 : total);
      if (move && !fromAsset) {
        try {
          src.delete();
        } catch (Exception e) {
          // abaikan
        }
      }
      finishCopyOrMove(move, true, dAbs, dName, false, "");
    } catch (SecurityException se) {
      finishCopyOrMove(move, false, "", Shared.fileNameOf(tP), false,
          "ditolak sistem (izin)");
    } catch (Exception e) {
      String m = e.getMessage();
      finishCopyOrMove(move, false, "", Shared.fileNameOf(tP), false,
          m == null ? "salin/pindah gagal" : m);
    } finally {
      try {
        if (in != null) {
          in.close();
        }
      } catch (Exception e) {
        // abaikan
      }
      Shared.releaseSlot("copyormove");
    }
  }

  private void fireCopyOrMoveProgress(final int pct, final long done,
      final long total) {
    fireOnUi(new Runnable() {
      public void run() {
        CopyOrMoveProgressChanged(pct, done, total);
      }
    });
  }

  private void finishCopyOrMove(final boolean move, final boolean success,
      final String absPath, final String fileName,
      final boolean duplicated, final String msg) {
    if (!success && msg.length() > 0) {
      Shared.err(this, "CopyOrMove", 0, msg);
    }
    final boolean s = success;
    final String p = absPath;
    final String fn = fileName;
    final boolean dup = duplicated;
    fireOnUi(new Runnable() {
      public void run() {
        CopyOrMoveFinished(s, p, fn, dup);
      }
    });
  }

  // ================= GET / query =================

  @SimpleFunction(description = "Nama file dari sebuah path (buang foldernya). "
      + "Olah string saja, tanpa scope. Butuh tebak nama dari URL? pakai "
      + "CallGuessFileName (Downloader).")
  public String GetFileName(String path) {
    return Shared.fileNameOf(path);
  }

  @SimpleFunction(description = "Ekstensi tanpa titik (huruf kecil). "
      + "Cth: foto.JPG -> jpg. Olah string saja.")
  public String GetExtension(String path) {
    return Shared.extensionOf(path);
  }

  @SimpleFunction(description = "Folder induk dari sebuah path. "
      + "Cth: /a/b/c.jpg -> /a/b. Olah string saja.")
  public String GetParentFolder(String path) {
    return Shared.parentOf(path);
  }

  @SimpleFunction(description = "Path absolut dari input (di-root ke "
      + "/storage/emulated/0; absolut/URL/aset apa adanya). Cth: "
      + "\"/Download/a.jpg\" -> \"/storage/emulated/0/Download/a.jpg\".")
  public String GetFullPath(String path) {
    try {
      if (wantAsset(path)) {
        return "//" + Shared.assetName(Shared.clean(path));
      }
      return Shared.fullPath(path);
    } catch (Exception e) {
      return "";
    }
  }

  @SimpleFunction(description = "Ukuran file dalam byte (-1 bila tidak ada "
      + "/ gagal). Folder = total isi rekursif; folder besar = bisa lambat "
      + "(hitung rekursif, UI beku sesaat). path seperti IsFileOrDirectoryExists.")
  public long FileSize(String path) {
    try {
      if (wantAsset(path)) {
        try {
          return context.getAssets().openFd(
              Shared.assetName(Shared.clean(path))).getLength();
        } catch (Exception e) {
          return -1L;
        }
      }
      File f = rootedFile(path);
      if (f == null || !f.exists()) {
        return -1L;
      }
      if (f.isDirectory()) {
        return Shared.dirSize(f);
      }
      return f.length();
    } catch (Exception e) {
      return -1L;
    }
  }

  @SimpleFunction(description = "Waktu ubah terakhir file/folder (milidetik "
      + "epoch; 0 bila tidak ada/gagal). Bandingkan 2 file = mana yang baru. "
      + "Ubah ke tanggal pakai blok tanggal App Inventor.")
  public long GetLastModified(String path) {
    try {
      File f = rootedFile(path);
      if (f == null || !f.exists()) {
        return 0L;
      }
      return f.lastModified();
    } catch (Exception e) {
      return 0L;
    }
  }

  @SimpleFunction(description = "True bila path ada DAN bisa dibaca aplikasi. "
      + "False = tidak ada, atau tidak ada izin baca (cek IsPermissionGranted "
      + "/ aktifkan 'Akses semua file').")
  public boolean IsFileReadable(String path) {
    try {
      File f = rootedFile(path);
      return f != null && f.exists() && f.canRead();
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleFunction(description = "Folder khusus aplikasi (tanpa izin tambahan, "
      + "aman Android 11+). Cth: /storage/emulated/0/Android/data/<paket>/"
      + "files. Hasilnya bisa langsung dipakai sebagai path blok lain. Pakai "
      + "ini agar TIDAK perlu izin storage sama sekali.")
  public String GetAndroidSpecificDirectory() {
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

  @SimpleFunction(description = "Root kartu SD utama (/storage/emulated/0) — awal "
      + "semua path strategi baru.")
  public String GetAndroidStorageDirectory() {
    try {
      return Shared.rootPath();
    } catch (Exception e) {
      return "/storage/emulated/0";
    }
  }

  @SimpleFunction(description = "Root kartu SD LEPASAN (removable, mis. "
      + "/storage/XXXX-XXXX). Kosong bila tidak ada / tidak terbaca. Path SD "
      + "dipakai apa adanya (tidak di-root ulang).")
  public String GetAndroidSdCardDirectory() {
    try {
      return Shared.removableRoot(context);
    } catch (Exception e) {
      return "";
    }
  }

  @SimpleFunction(description = "True bila kartu SD LEPASAN (removable) "
      + "terpasang dan terbaca.")
  public boolean IsSdCardExists() {
    try {
      return Shared.removableRoot(context).length() > 0;
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleFunction(description = "Path folder cache aplikasi (untuk file "
      + "sementara). Tanpa izin tambahan.")
  public String GetCacheDir() {
    try {
      File c = context.getCacheDir();
      if (c != null) {
        return c.getAbsolutePath();
      }
    } catch (Exception e) {
      // abaikan
    }
    return "";
  }

  @SimpleFunction(description = "Sisa ruang kosong (byte) di kartu SD "
      + "(/storage/emulated/0). -1 bila gagal dibaca.")
  public long FreeSpace() {
    try {
      File r = rootedFile("");
      if (r == null) {
        return -1L;
      }
      return r.getFreeSpace();
    } catch (Exception e) {
      return -1L;
    }
  }

  @SimpleFunction(description = "Total kapasitas (byte) kartu SD "
      + "(/storage/emulated/0). -1 bila gagal.")
  public long TotalSpace() {
    try {
      File r = rootedFile("");
      if (r == null) {
        return -1L;
      }
      return r.getTotalSpace();
    } catch (Exception e) {
      return -1L;
    }
  }

  @SimpleFunction(description = "GET: Daftar file (path lengkap). filter = "
      + "ekstensi tanpa titik (boleh koma: jpg,png; kosong = semua). "
      + "recursive = sertakan isi subfolder. path seperti IsFileOrDirectoryExists. "
      + "File besar + recursive = bisa lambat (UI beku sesaat); untuk daftar besar "
      + "pakai filter/tipe secukupnya.")
  public List<String> GetFiles(String path, String filter, boolean recursive) {
    ArrayList<String> out = new ArrayList<String>();
    try {
      String p = Shared.clean(path);
      String flt = Shared.clean(filter).toLowerCase().replace(" ", "");
      ArrayList<String> exts = new ArrayList<String>();
      if (flt.length() > 0) {
        String[] parts = flt.split(",");
        for (int i = 0; i < parts.length; i = i + 1) {
          String e = parts[i];
          if (e.startsWith(".")) {
            e = e.substring(1);
          }
          if (e.length() > 0) {
            exts.add(e);
          }
        }
      }
      if (wantAsset(p)) {
        String[] l = context.getAssets().list(Shared.assetName(p));
        if (l != null) {
          for (int i = 0; i < l.length; i = i + 1) {
            if (exts.isEmpty() || exts.contains(
                Shared.extensionOf(l[i]).toLowerCase())) {
              out.add("//" + Shared.assetName(p) + "/" + l[i]);
            }
          }
        }
        Collections.sort(out);
        return out;
      }
      File d = rootedFile(p);
      if (d == null || !d.isDirectory()) {
        Shared.err(this, "File", 0, "folder tidak ada");
        return out;
      }
      walkFilesByExt(d, exts, recursive, out);
      Collections.sort(out);
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "File", 0, m == null ? "list gagal" : m);
    }
    return out;
  }

  private void walkFilesByExt(File dir, ArrayList<String> exts,
      boolean recursive, ArrayList<String> out) {
    File[] fs = dir.listFiles();
    if (fs == null) {
      return;
    }
    for (int i = 0; i < fs.length; i = i + 1) {
      File g = fs[i];
      if (g.isDirectory()) {
        if (recursive) {
          walkFilesByExt(g, exts, true, out);
        }
      } else if (exts.isEmpty() || exts.contains(
          Shared.extensionOf(g.getName()).toLowerCase())) {
        out.add(g.getAbsolutePath());
      }
    }
  }

  @SimpleFunction(description = "GET: Daftar subfolder (path lengkap, tanpa "
      + "rekursif). path seperti IsFileOrDirectoryExists.")
  public List<String> GetFolders(String folder) {
    ArrayList<String> out = new ArrayList<String>();
    try {
      String p = Shared.clean(folder);
      if (wantAsset(p)) {
        return out;
      }
      File d = rootedFile(p);
      if (d == null || !d.isDirectory()) {
        Shared.err(this, "File", 0, "folder tidak ada");
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
      Shared.err(this, "File", 0, m == null ? "list gagal" : m);
    }
    return out;
  }

  // ================= DUPLIKAT FILE OR DIRECTORY =================

  /** True bila s tak kosong dan isinya digit semua. */
  private boolean dupIsDigits(String s) {
    if (s == null || s.length() == 0) {
      return false;
    }
    for (int i = 0; i < s.length(); i = i + 1) {
      char c = s.charAt(i);
      if (c < '0' || c > '9') {
        return false;
      }
    }
    return true;
  }

  /** Kunci keluarga nama: buang counter "_N" / " (N)", lalu huruf kecil semua.
   *  Mode file: ekstensi ikut jadi bagian kunci (image.jpg beda dengan
   *  image.png). Contoh: "image_2.jpg" dan "image (1).jpg" -> "image.jpg". */
  private String dupKey(String name, boolean isFile) {
    try {
      String n = Shared.sanitize(Shared.clean(name));
      String base = n;
      String ext = "";
      if (isFile) {
        int d = n.lastIndexOf('.');
        if (d > 0) {
          base = n.substring(0, d);
          ext = n.substring(d);
        }
      }
      if (base.endsWith(")")) {
        int o = base.lastIndexOf('(');
        if (o > 0 && dupIsDigits(base.substring(o + 1,
            base.length() - 1).trim())) {
          base = base.substring(0, o).trim();
        }
      }
      int u = base.lastIndexOf('_');
      if (u > 0 && dupIsDigits(base.substring(u + 1))) {
        base = base.substring(0, u);
      }
      String k = base + ext;
      if (k.length() == 0) {
        return "";
      }
      return k.toLowerCase(java.util.Locale.US);
    } catch (Exception e) {
      return "";
    }
  }

  /** Daftar path lengkap satu keluarga duplikat dalam 1 folder yang sama
   *  (tanpa masuk subfolder). Termasuk inputnya sendiri bila ada. */
  private ArrayList<String> collectDuplicates(String fileOrDirectory) {
    ArrayList<String> out = new ArrayList<String>();
    try {
      String p = Shared.clean(fileOrDirectory);
      if (p.length() == 0 || wantAsset(p)) {
        return out;
      }
      File t = rootedFile(p);
      if (t == null) {
        return out;
      }
      boolean folderMode = p.endsWith("/")
          || (t.exists() && t.isDirectory());
      String name = t.getName();
      if (name.length() == 0) {
        return out;
      }
      File drawer = t.getParentFile();
      if (drawer == null || !drawer.isDirectory()) {
        return out;
      }
      String key = dupKey(name, !folderMode);
      if (key.length() == 0) {
        return out;
      }
      File[] fs = drawer.listFiles();
      if (fs != null) {
        for (int i = 0; i < fs.length; i = i + 1) {
          String en = fs[i].getName();
          if (en.length() == 0) {
            continue;
          }
          if (key.equals(dupKey(en, fs[i].isFile()))) {
            out.add(fs[i].getAbsolutePath());
          }
        }
      }
      Collections.sort(out);
    } catch (Exception e) {
      // abaikan, pemanggil Is/Get yang memutuskan
    }
    return out;
  }

  @SimpleFunction(description = "True bila NAMA PERSIS input sudah dipakai di "
      + "folder yang sama (nama.jpg vs nama.jpg = duplikat; nama (1).jpg "
      + "BUKAN duplikatnya — akhiran counter _N/(N) hanya untuk hitungan "
      + "Many). Inputnya sendiri dihitung, jadi file yang sudah ada selalu "
      + "true; gunanya untuk cek SEBELUM simpan (true = nama sudah "
      + "terpakai). Huruf besar-kecil diabaikan. Untuk banding ISI pakai "
      + "GetFileHash + blok = di if. Hanya 1 level folder, tanpa subfolder.")
  public boolean IsDuplicateFileOrDirectory(String fileOrDirectory) {
    try {
      String p = Shared.clean(fileOrDirectory);
      if (p.length() == 0 || wantAsset(p)) {
        return false;
      }
      File t = rootedFile(p);
      if (t == null) {
        return false;
      }
      File drawer = t.getParentFile();
      String name = t.getName();
      if (drawer == null || !drawer.isDirectory() || name.length() == 0) {
        return false;
      }
      String key = Shared.sanitize(Shared.clean(name))
          .toLowerCase(java.util.Locale.US);
      File[] fs = drawer.listFiles();
      if (fs == null) {
        return false;
      }
      for (int i = 0; i < fs.length; i = i + 1) {
        String en = fs[i].getName();
        if (en.length() == 0) {
          continue;
        }
        if (key.equals(Shared.sanitize(Shared.clean(en))
            .toLowerCase(java.util.Locale.US))) {
          return true;
        }
      }
      return false;
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleFunction(description = "Berapa banyak keluarga counter nama input di "
      + "folder yang sama (nama.jpg, nama_1.jpg, nama (1).jpg, ...; termasuk "
      + "inputnya sendiri bila ada). 0 = tidak ada. Ini HITUNGAN Many — "
      + "untuk cek nama persis pakai IsDuplicateFileOrDirectory. Hanya 1 "
      + "level folder, tanpa subfolder.")
  public int GetManyDuplicateFileOrDirectory(String fileOrDirectory) {
    try {
      return collectDuplicates(fileOrDirectory).size();
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Duplicate", 0,
          m == null ? "hitung duplikat gagal" : m);
      return 0;
    }
  }

  @SimpleFunction(description = "Daftar path lengkap keluarga counter nama "
      + "input di folder yang sama, urut abjad (termasuk inputnya sendiri "
      + "bila ada; untuk cek nama persis lihat IsDuplicateFileOrDirectory). "
      + "Kosong = tidak ada; tanpa masuk subfolder.")
  public List<String> GetDuplicateFileOrDirectory(String fileOrDirectory) {
    try {
      return collectDuplicates(fileOrDirectory);
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Duplicate", 0,
          m == null ? "list duplikat gagal" : m);
      return new ArrayList<String>();
    }
  }

  @SimpleFunction(description = "Hash isi file (sidik jari konten ala ZArchiver): "
      + "dua file dgn hash SAMA = isi SAMA walau nama beda (foto (3).jpg vs "
      + "foto (4).jpg). algorithm: colok Helper merah Type (dropdown MD5/SHA-1/ "
      + "SHA-224/SHA-256/SHA-384/SHA-512), atau tulis manual dgn blok teks "
      + "(cth sha256 — soketnya menerima keduanya). Bandingkan hasilnya dgn "
      + "blok = di if. Path seperti IsFileOrDirectoryExists (asset "
      + "didukung). File tidak ada/algoritma salah/baca gagal = teks kosong "
      + "+ ErrorOccurred.")
  public String GetFileHash(String path, @Options(Type.class) String algorithm) {
    try {
      String algo = Shared.clean(algorithm).toUpperCase(java.util.Locale.US)
          .replace("-", "").replace(" ", "");
      if (algo.equals("MD5")) {
        algo = "MD5";
      } else if (algo.equals("SHA1") || algo.equals("SHA224")
          || algo.equals("SHA256") || algo.equals("SHA384")
          || algo.equals("SHA512")) {
        algo = "SHA-" + algo.substring(3);
      } else {
        Shared.err(this, "Hash", 0, "algoritma harus MD5 / SHA-1 / SHA-224 / "
            + "SHA-256 / SHA-384 / SHA-512");
        return "";
      }
      MessageDigest md = MessageDigest.getInstance(algo);
      String p = Shared.clean(path);
      InputStream in = null;
      try {
        if (wantAsset(p)) {
          in = context.getAssets().open(Shared.assetName(p));
        } else {
          File f = rootedFile(p);
          if (f == null || !f.isFile()) {
            Shared.err(this, "Hash", 0, "file tidak ada");
            return "";
          }
          in = new FileInputStream(f);
        }
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) >= 0) {
          md.update(buf, 0, n);
        }
      } finally {
        if (in != null) {
          try {
            in.close();
          } catch (Exception e) {
            // abaikan
          }
        }
      }
      byte[] d = md.digest();
      StringBuilder sb = new StringBuilder(d.length * 2);
      for (int i = 0; i < d.length; i = i + 1) {
        String h = Integer.toHexString(d[i] & 0xFF);
        if (h.length() < 2) {
          sb.append('0');
        }
        sb.append(h);
      }
      return sb.toString();
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Hash", 0, m == null ? "hash gagal" : m);
      return "";
    }
  }

  @SimpleFunction(description = "Kategori file dari nama/path: image, audio, "
      + "video, text, archive, document, apk, atau unknown (boleh alias Indonesia "
      + "seperti GetFilesByType). Berdasarkan ekstensi (tak peduli huruf besar). "
      + "Bandingkan dengan blok TypeImage/dll. Olah string saja. Nama bisa dibohongi "
      + "(jpg padahal zip) — bila akurasi penting pakai DetectArchiveType "
      + "Tools_Compress.")
  public String FileTypeOf(String pathOrName) {
    return Shared.fileCategory(pathOrName);
  }

  @SimpleFunction(description = "GET: Daftar file (path lengkap) yang kategorinya "
      + "cocok. type = image/audio/video/text/archive/document/apk (boleh alias "
      + "Indonesia: gambar/musik/video/teks/arsip/dokumen; alias lama vidio tetap "
      + "diterima) atau salah satu blok TypeImage/dll. recursive = sertakan isi "
      + "subfolder. path seperti IsFileOrDirectoryExists. Pilih: nama bisa dibohongi "
      + "(jpg padahal zip) — bila akurasi penting pakai DetectArchiveType "
      + "Tools_Compress (baca isi); bila kecepatan penting pakai FileTypeOf "
      + "(baca nama).")
  public List<String> GetFilesByType(String folder, String type,
      boolean recursive) {
    ArrayList<String> out = new ArrayList<String>();
    String cat = Shared.normCategory(type);
    if (cat.length() == 0) {
      Shared.err(this, "File", 0, "tipe tidak dikenal: "
          + (type == null ? "" : type));
      return out;
    }
    try {
      String p = Shared.clean(folder);
      if (wantAsset(p)) {
        String[] l = context.getAssets().list(Shared.assetName(p));
        if (l != null) {
          for (int i = 0; i < l.length; i = i + 1) {
            if (cat.equals(Shared.fileCategory(l[i]))) {
              out.add("//" + Shared.assetName(p) + "/" + l[i]);
            }
          }
        }
        Collections.sort(out);
        return out;
      }
      File d = rootedFile(p);
      if (d == null || !d.isDirectory()) {
        Shared.err(this, "File", 0, "folder tidak ada");
        return out;
      }
      walkFilesByCat(d, cat, recursive, out);
      Collections.sort(out);
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "File", 0, m == null ? "list gagal" : m);
    }
    return out;
  }

  private void walkFilesByCat(File dir, String cat, boolean recursive,
      ArrayList<String> out) {
    File[] fs = dir.listFiles();
    if (fs == null) {
      return;
    }
    for (int i = 0; i < fs.length; i = i + 1) {
      File g = fs[i];
      if (g.isDirectory()) {
        if (recursive) {
          walkFilesByCat(g, cat, true, out);
        }
      } else if (cat.equals(Shared.fileCategory(g.getName()))) {
        out.add(g.getAbsolutePath());
      }
    }
  }

  @SimpleFunction(description = "Daftar ekstensi (tanpa titik) yang masuk "
      + "kategori type. Tipe tak dikenal = list kosong + error. "
      + "Olah string saja.")
  public List<String> GetExtensionsForType(String type) {
    String cat = Shared.normCategory(type);
    if (cat.length() == 0) {
      Shared.err(this, "File", 0, "tipe tidak dikenal: "
          + (type == null ? "" : type));
      return new ArrayList<String>();
    }
    return Shared.extensionsFor(cat);
  }

  @SimpleProperty(description = "Pembanding read-only kategori: image.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeImage() {
    return "image";
  }

  @SimpleProperty(description = "Pembanding read-only kategori: audio.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeAudio() {
    return "audio";
  }

  @SimpleProperty(description = "Pembanding read-only kategori: video.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeVideo() {
    return "video";
  }

  @SimpleProperty(description = "Pembanding read-only kategori: text.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeText() {
    return "text";
  }

  @SimpleProperty(description = "Pembanding read-only kategori: archive.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeArchive() {
    return "archive";
  }

  @SimpleProperty(description = "Pembanding read-only kategori: document.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeDocument() {
    return "document";
  }

  @SimpleProperty(description = "Pembanding read-only kategori: apk.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeApk() {
    return "apk";
  }

  @SimpleProperty(description = "Pembanding read-only kategori: unknown.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeUnknown() {
    return "unknown";
  }

  // ================= GET PERMISSION =================



  @SimpleFunction(description = "True bila aplikasi boleh tulis SEMUA file "
      + "bersama. Android 11+ = 'Akses semua file' aktif; Android 10 ke "
      + "bawah selalu true (izin tulis biasa cukup via CallSetStoragePermission).")
  public boolean IsAllFilesAccessGranted() {
    try {
      if (android.os.Build.VERSION.SDK_INT >= 30) {
        return android.os.Environment.isExternalStorageManager();
      }
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  @SimpleFunction(description = "Buka layar Settings 'Akses semua file' "
      + "untuk aplikasi ini (Android 11+). WAJIB agar tulis ke "
      + "/storage/emulated/0 & SD card jalan. Setelah kembali, cek lewat "
      + "IsAllFilesAccessGranted. Tanpa feedback.")
  public void OpenAllFilesAccessSettings() {
    try {
      Shared.openAllFilesSettings(activity);
    } catch (Exception e) {
      Shared.err(this, "Permission", 0, "gagal buka settings");
    }
  }

  @SimpleFunction(description = "True bila izin ini sudah granted. Untuk "
      + "izin storage di Android 11+ = cek 'Akses semua file'. "
      + "Android 6 ke bawah selalu true (izin sejak install). Colok blok "
      + "teks ATAU blok Permission... di Tools_Base.")
  public boolean IsPermissionGranted(String permission) {
    String p = Shared.clean(permission);
    if (p.length() == 0) {
      return false;
    }
    try {
      int sdk = android.os.Build.VERSION.SDK_INT;
      String eff = Shared.effectivePermission(p, sdk);
      if (eff == null) {
        return true;
      }
      if (Shared.isStoragePerm(eff) && sdk >= 30) {
        return android.os.Environment.isExternalStorageManager();
      }
      if (sdk >= 23) {
        return activity.checkSelfPermission(eff)
            == android.content.pm.PackageManager.PERMISSION_GRANTED;
      }
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  @SimpleFunction(description = "True bila izin tulis storage sudah beres "
      + "(tanpa input apa pun). Android 11+ = 'Akses semua file' aktif; "
      + "Android 10 ke bawah = izin tulis granted.")
  public boolean IsStoragePermissionGranted() {
    return IsPermissionGranted(WRITE_PERM);
  }


  @SimpleFunction(description = "CALL: SATU PINTU izin File (spesifik storage, "
      + "tanpa soket). Pintar OS: sudah beres = langsung GotPermission(true) "
      + "tanpa dialog; Android 11+ belum = OTOMATIS buka Settings + "
      + "GotPermission(false + pesan); lainnya = dialog izin (end-user yang "
      + "mutusin) + GotPermission. permanentlyDenied=false = user menolak "
      + "(boleh tanya lagi); true = permanen (aktifkan manual di Setelan). "
      + "Tanpa return.")
  public void CallSetStoragePermission() {
    requestStorage();
  }

  private void requestStorage() {
    final String p = WRITE_PERM;
    boolean granted = false;
    try {
      granted = IsPermissionGranted(p);
    } catch (Exception e) {
      granted = false;
    }
    if (granted) {
      fireOnUi(new Runnable() {
        public void run() {
          GotPermission(true, p, "", false);
        }
      });
      return;
    }
    int sdk = android.os.Build.VERSION.SDK_INT;
    final String eff = Shared.effectivePermission(p, sdk);
    if (eff == null) {
      fireOnUi(new Runnable() {
        public void run() {
          GotPermission(true, p, "tak perlu diminta di OS ini", false);
        }
      });
      return;
    }
    if (Shared.isStoragePerm(eff) && sdk >= 30) {
      Shared.err(this, "Permission", 0, "Android 11+: butuh "
          + "akses semua file - membuka Settings");
      Shared.openAllFilesSettings(activity);
      fireOnUi(new Runnable() {
        public void run() {
          GotPermission(false, p, "Android 11+: aktifkan akses semua file "
              + "di Setelan aplikasi", false);
        }
      });
      return;
    }
    try {
      form.askPermission(eff, new PermissionResultHandler() {
        public void HandlePermissionResponse(String perm, boolean granted) {
          final boolean g = granted;
          fireOnUi(new Runnable() {
            public void run() {
              if (g) {
                GotPermission(true, p, "", false);
                return;
              }
              boolean perm2 = false;
              try {
                if (android.os.Build.VERSION.SDK_INT >= 23) {
                  perm2 = !activity.shouldShowRequestPermissionRationale(eff);
                }
              } catch (Exception e) {
                perm2 = false;
              }
              if (perm2) {
                Shared.err(Tools_File.this, "Permission", 0,
                    "izin ditolak permanen: " + p);
                GotPermission(false, p, "izin ditolak permanen: " + p
                    + " - aktifkan manual di Setelan aplikasi", true);
              } else {
                Shared.err(Tools_File.this, "Permission", 0,
                    "izin ditolak user: " + p);
                GotPermission(false, p, "izin ditolak user: " + p, false);
              }
            }
          });
        }
      });
    } catch (Exception e) {
      String m = e.getMessage();
      final String msg = m == null ? "minta izin gagal" : m;
      Shared.err(this, "Permission", 0, msg);
      fireOnUi(new Runnable() {
        public void run() {
          GotPermission(false, p, msg, false);
        }
      });
    }
  }




  // ================= PICKER & BUKA =================

  @SimpleFunction(description = "Buka pemilih file. mimeType contoh: image/* "
      + "(kosong = semua). Hasil lewat AfterFilePicked (uri + path + nama "
      + "file). Satu picker per saat.")
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
      AfterFilePicked(false, "", "", "");
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
      if (resultCode == Activity.RESULT_OK && data != null
          && data.getData() != null) {
        final Uri uri = data.getData();
        final String uriStr = uri.toString();
        final String path = resolveUriToPath(uri);
        AfterFilePicked(true, uriStr, path,
            Shared.fileNameOf(path));
      } else {
        AfterFilePicked(false, "", "", "");
      }
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Pick", 0, m == null ? "picker gagal" : m);
      AfterFilePicked(false, "", "", "");
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

  @SimpleFunction(description = "Buka file dengan aplikasi lain (musik, "
      + "gambar, PDF, ...). path seperti IsFileOrDirectoryExists. Hasil lewat "
      + "AfterOpenFile. Asset disalin ke cache dulu.")
  public void OpenFile(String path) {
    try {
      String p = Shared.clean(path);
      File f = rootedFile(path);
      if (f == null) {
        if (wantAsset(p)) {
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

  @SimpleEvent(description = "Hasil CallSetHiddenDirectory: success pertama, path + "
      + "message + needPermission (isi = izin yang harus diminta bila "
      + "gagal karena izin).")
  public void DirectoryHidden(boolean success, String path, String message,
      String needPermission) {
    Shared.fire(this, "DirectoryHidden", Boolean.valueOf(success), path,
        message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallMakeNomediaFile: success pertama, path + "
      + "message + needPermission.")
  public void NomediaFileCreated(boolean success, String path, String message,
      String needPermission) {
    Shared.fire(this, "NomediaFileCreated", Boolean.valueOf(success), path,
        message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallDeleteNomediaFile: success pertama, path + "
      + "message + needPermission.")
  public void NomediaFileDeleted(boolean success, String path, String message,
      String needPermission) {
    Shared.fire(this, "NomediaFileDeleted", Boolean.valueOf(success), path,
        message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallMakeDirectory: success pertama, path + "
      + "message + needPermission.")
  public void DirectoryCreated(boolean success, String path, String message,
      String needPermission) {
    Shared.fire(this, "DirectoryCreated", Boolean.valueOf(success), path,
        message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallDeleteFile: success pertama, path + "
      + "message + needPermission.")
  public void FileDeleted(boolean success, String path, String message,
      String needPermission) {
    Shared.fire(this, "FileDeleted", Boolean.valueOf(success), path,
        message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallDeleteList: success + total + "
      + "terhapus + message + needPermission.")
  public void FileListDeleted(boolean success, int total, int deleted,
      String message, String needPermission) {
    Shared.fire(this, "FileListDeleted",
        Boolean.valueOf(success), Integer.valueOf(total),
        Integer.valueOf(deleted), message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallRenameFile: success pertama, path (nama baru) + "
      + "message + needPermission.")
  public void FileRenamed(boolean success, String path, String message,
      String needPermission) {
    Shared.fire(this, "FileRenamed", Boolean.valueOf(success), path,
        message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallRenameFolder: success pertama, path (nama baru) + "
      + "message + needPermission.")
  public void FolderRenamed(boolean success, String path, String message,
      String needPermission) {
    Shared.fire(this, "FolderRenamed", Boolean.valueOf(success), path,
        message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallCreateEmptyFile: success pertama, path + "
      + "message + needPermission.")
  public void EmptyFileCreated(boolean success, String path, String message,
      String needPermission) {
    Shared.fire(this, "EmptyFileCreated", Boolean.valueOf(success), path,
        message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallSaveFile: success pertama, path + "
      + "message + needPermission.")
  public void FileSaved(boolean success, String path, String message,
      String needPermission) {
    Shared.fire(this, "FileSaved", Boolean.valueOf(success), path,
        message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallSaveBase64ToFile: success pertama, path + "
      + "message + needPermission.")
  public void Base64FileSaved(boolean success, String path, String message,
      String needPermission) {
    Shared.fire(this, "Base64FileSaved", Boolean.valueOf(success), path,
        message, needPermission);
  }

  @SimpleEvent(description = "Bacaan dimulai (CallReadText): persiapan "
      + "(tampilkan loading).")
  public void ReadStarted(String path) {
    Shared.fire(this, "ReadStarted", path);
  }

  @SimpleEvent(description = "Hasil CallReadText: success pertama, path "
      + "absolut, fileName paling belakang, content = isi file.")
  public void GotText(boolean success, String path,
      String fileName, String content) {
    Shared.fire(this, "GotText", Boolean.valueOf(success), path, fileName, content);
  }

  @SimpleEvent(description = "Salin/pindah dimulai: path sumber + tujuan + "
      + "nama file + isMoved.")
  public void CopyOrMoveStarted(String fromPath, String toPath,
      String fileName, boolean isMoved) {
    Shared.fire(this, "CopyOrMoveStarted", fromPath,
        toPath, fileName, Boolean.valueOf(isMoved));
  }

  @SimpleEvent(description = "Progres salin/pindah: persen 0-100 + byte "
      + "total & berjalan.")
  public void CopyOrMoveProgressChanged(int progress, long byteTotal,
      long byteProgress) {
    Shared.fire(this, "CopyOrMoveProgressChanged",
        Integer.valueOf(progress), Long.valueOf(byteTotal),
        Long.valueOf(byteProgress));
  }

  @SimpleEvent(description = "Salin/pindah selesai: success + path + nama "
      + "file + isDuplicated (true = tujuan sudah ada, tidak ditimpa, "
      + "rename manual).")
  public void CopyOrMoveFinished(boolean success, String path,
      String fileName, boolean isDuplicated) {
    Shared.fire(this, "CopyOrMoveFinished",
        Boolean.valueOf(success), path, fileName,
        Boolean.valueOf(isDuplicated));
  }

  @SimpleEvent(description = "Hasil CallSetStoragePermission File (HANYA izin "
      + "storage File - tidak menembak untuk izin tools lain): success + izin + "
      + "pesan + permanentlyDenied. success=true = beres (lanjut). false + "
      + "permanentlyDenied=false = user menolak (boleh tanya lagi) ATAU Settings "
      + "storage (baca message). false + true = permanen (aktifkan manual di "
      + "Setelan).")
  public void GotPermission(boolean success, String permission,
      String message, boolean permanentlyDenied) {
    Shared.fire(this, "GotPermission",
        Boolean.valueOf(success), permission, message,
        Boolean.valueOf(permanentlyDenied));
  }

  @SimpleEvent(description = "Hasil OpenFilePicker: success pertama, uri, "
      + "path absolut, fileName paling belakang. success=false = "
      + "dibatalkan user.")
  public void AfterFilePicked(boolean success, String uri,
      String path, String fileName) {
    Shared.fire(this, "AfterFilePicked", Boolean.valueOf(success), uri, path, fileName);
  }

  @SimpleEvent(description = "Hasil OpenFile: success + pesan.")
  public void AfterOpenFile(boolean success, String message) {
    Shared.fire(this, "AfterOpenFile",
        Boolean.valueOf(success), message);
  }

  @SimpleEvent(description = "SATU-SATUNYA error Tools_File. component = nama "
      + "operasi (isi dengan OpDirectory/OpFile/OpRead/OpBase64/"
      + "OpCopyOrMove/OpPick/OpOpen/OpPermission untuk if). Contoh: jika component "
      + "= OpFile maka tampilkan message. id selalu 0 (tanpa id — cek component saja).")
  public void ErrorOccurred(String component, int id, String message) {
    Shared.fire(this, "ErrorOccurred", component,
        Integer.valueOf(id), message);
  }

  @SimpleFunction(description = "Fallback polling untuk event ErrorOccurred: "
      + "pesan error terakhir operasi ini (Directory/File/Read/Base64/"
      + "CopyOrMove/Pick/Open/Permission, teks kosong bila belum ada). Isi operation "
      + "dengan blok OpFile. Contoh: simpan LastError(OpCopyOrMove) "
      + "ke variabel.")
  public String LastError(String operation) {
    return Shared.lastError(operation);
  }

  // ================= OP =================

  @SimpleProperty(description = "Nama operasi direktori (untuk if di "
      + "ErrorOccurred).", category = PropertyCategory.BEHAVIOR)
  public String OpDirectory() {
    return "Directory";
  }

  @SimpleProperty(description = "Nama operasi File umum (untuk if di "
      + "ErrorOccurred).", category = PropertyCategory.BEHAVIOR)
  public String OpFile() {
    return "File";
  }

  @SimpleProperty(description = "Nama operasi Read (untuk if di "
      + "ErrorOccurred).", category = PropertyCategory.BEHAVIOR)
  public String OpRead() {
    return "Read";
  }

  @SimpleProperty(description = "Nama operasi Base64 (untuk if di "
      + "ErrorOccurred).", category = PropertyCategory.BEHAVIOR)
  public String OpBase64() {
    return "Base64";
  }

  @SimpleProperty(description = "Nama operasi salin/pindah (untuk if di "
      + "ErrorOccurred).", category = PropertyCategory.BEHAVIOR)
  public String OpCopyOrMove() {
    return "CopyOrMove";
  }

  @SimpleProperty(description = "Nama operasi Pick (untuk if di "
      + "ErrorOccurred).", category = PropertyCategory.BEHAVIOR)
  public String OpPick() {
    return "Pick";
  }

  @SimpleProperty(description = "Nama operasi Open (untuk if di "
      + "ErrorOccurred).", category = PropertyCategory.BEHAVIOR)
  public String OpOpen() {
    return "Open";
  }

  @SimpleProperty(description = "Nama operasi Permission (untuk if di "
      + "ErrorOccurred).", category = PropertyCategory.BEHAVIOR)
  public String OpPermission() {
    return "Permission";
  }

  // ================= destroy =================

  public void onDestroy() {
    try {
      form.unregisterForActivityResult(this);
    } catch (Exception e) {
      // abaikan
    }
    copyMoveCancel = true;
    Shared.releaseSlot("copyormove");
    Shared.releaseSlot("read");
    Shared.releaseSlot("pick");
    Shared.releaseSlot("b64file");
  }
}
