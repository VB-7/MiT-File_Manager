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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@DesignerComponent(
    version = 1,
    description = "Tools_File (sistem draft): awalan Is = boolean (colok ke if), "
        + "awalan Call = async + hasil lewat event, awalan Set = tanpa feedback. "
        + "Semua blok path bawa scoop teks: App/Private/Cache/Shared/Legacy/"
        + "Asset (bebas huruf) atau \"\" = path absolut dipakai apa adanya, "
        + "relatif = root kartu SD. Salin/pindah digabung satu blok "
        + "CallCopyOrMoveFile (isMove) + info duplikat.",
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

  // ================= bantu scoop =================

  private File scoopFile(String scoop, String path) {
    return Shared.resolveScoop(context, scoop, path);
  }

  private boolean wantAsset(String scoop, String path) {
    if (Shared.scoopToScope(scoop) == null) {
      return false;
    }
    try {
      if (Shared.scoopToScope(scoop).name().equals("Asset")) {
        return true;
      }
    } catch (Exception e) {
      // abaikan
    }
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
  private File findDir(String scoop, String folder) {
    File d = scoopFile(scoop, folder);
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
        File g = scoopFile(scoop, alt);
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

  @SimpleFunction(description = "Apakah file ada di jalur. scoop: App/Private/"
      + "Cache/Shared/Legacy/Asset (bebas huruf) atau \"\" = absolut. "
      + "Asset bisa dicek.")
  public boolean IsFileExists(String fileName, String scoop) {
    try {
      String p = Shared.clean(fileName);
      if (wantAsset(scoop, p)) {
        return existsInAssets(p);
      }
      File f = scoopFile(scoop, p);
      return f != null && f.isFile();
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleFunction(description = "Apakah directory/folder ada di jalur. "
      + "scoop seperti IsFileExists.")
  public boolean IsDirectoryExists(String directory, String scoop) {
    try {
      String p = Shared.clean(directory);
      if (wantAsset(scoop, p)) {
        try {
          String[] l = context.getAssets().list(Shared.assetName(p));
          return l != null && l.length > 0;
        } catch (Exception e) {
          return false;
        }
      }
      File d = scoopFile(scoop, p);
      return d != null && d.isDirectory();
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleFunction(description = "Apakah file .nomedia ada di dalam folder. "
      + "scoop seperti IsFileExists.")
  public boolean IsNomediaExists(String folder, String scoop) {
    try {
      String p = Shared.clean(folder);
      if (wantAsset(scoop, p)) {
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
      File d = scoopFile(scoop, p);
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
      + "scoop seperti IsFileExists.")
  public boolean IsHiddenDirectory(String directory, String scoop) {
    try {
      String p = Shared.clean(directory);
      if (wantAsset(scoop, p)) {
        return Shared.fileNameOf(Shared.assetName(p)).startsWith(".");
      }
      File d = findDir(scoop, p);
      if (d == null || !d.isDirectory()) {
        return false;
      }
      return d.getName().startsWith(".");
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleFunction(description = "True bila ada salinan/pindahan "
      + "CallCopyOrMoveFile yang masih berjalan.")
  public boolean IsBusy() {
    return Shared.slotBusy("copyormove");
  }

  // ================= CALL: direktori & nomedia =================

  @SimpleFunction(description = "CALL: jadikan directory hidden (nama jadi "
      + ".nama). Sudah hidden = success. Hasil lewat DirectoryHidden.")
  public void CallSetHiddenDirectory(final String directory,
      final String scoop) {
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        String need = "";
        try {
          File d = findDir(scoop, directory);
          if (d == null || !d.isDirectory()) {
            final String msg = "directory tidak ada";
            Shared.err(Tools_File.this, "Directory", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                DirectoryHidden(Shared.clean(directory), false, msg, "");
              }
            });
            return;
          }
          final String abs = d.getAbsolutePath();
          if (d.getName().startsWith(".")) {
            fireOnUi(new Runnable() {
              public void run() {
                DirectoryHidden(abs, true, "sudah hidden", "");
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
                DirectoryHidden(abs, false, msg, "");
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
              DirectoryHidden(np, ok, msg, needF);
            }
          });
        } catch (SecurityException se) {
          final String msg = "ditolak sistem (izin)";
          Shared.err(Tools_File.this, "Directory", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              DirectoryHidden(Shared.clean(directory), false, msg,
                  WRITE_PERM);
            }
          });
        } catch (Exception e) {
          final String m = e.getMessage();
          final String msg = m == null ? "gagal hidden" : m;
          Shared.err(Tools_File.this, "Directory", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              DirectoryHidden(Shared.clean(directory), false, msg, "");
            }
          });
        }
      }
    });
  }

  @SimpleFunction(description = "CALL: buat file .nomedia di dalam folder "
      + "(sembunyikan dari galeri/musik). Hasil lewat NomediaFileCreated.")
  public void CallMakeNomediaFile(final String folder, final String scoop) {
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        try {
          if (wantAsset(scoop, folder)) {
            final String msg = "tidak bisa tulis ke Asset";
            Shared.err(Tools_File.this, "Directory", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                NomediaFileCreated(Shared.clean(folder), false, msg, "");
              }
            });
            return;
          }
          File d = scoopFile(scoop, folder);
          if (d == null) {
            final String msg = "folder tidak valid";
            Shared.err(Tools_File.this, "Directory", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                NomediaFileCreated(Shared.clean(folder), false, msg, "");
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
                  NomediaFileCreated(abs, false, msg, WRITE_PERM);
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
              NomediaFileCreated(abs, okF, msg, okF ? "" : WRITE_PERM);
            }
          });
        } catch (Exception e) {
          final String m = e.getMessage();
          final String msg = m == null ? "gagal buat .nomedia" : m;
          Shared.err(Tools_File.this, "Directory", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              NomediaFileCreated(Shared.clean(folder), false, msg, "");
            }
          });
        }
      }
    });
  }

  @SimpleFunction(description = "CALL: hapus file .nomedia di folder. "
      + "Tidak ada = success. Hasil lewat NomediaFileDeleted.")
  public void CallDeleteNomediaFile(final String folder,
      final String scoop) {
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        try {
          if (wantAsset(scoop, folder)) {
            final String msg = "tidak bisa tulis ke Asset";
            Shared.err(Tools_File.this, "Directory", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                NomediaFileDeleted(Shared.clean(folder), false, msg, "");
              }
            });
            return;
          }
          File d = scoopFile(scoop, folder);
          if (d == null || !d.isDirectory()) {
            final String msg = "folder tidak ada";
            Shared.err(Tools_File.this, "Directory", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                NomediaFileDeleted(Shared.clean(folder), false, msg, "");
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
              NomediaFileDeleted(abs, okF, msg, okF ? "" : WRITE_PERM);
            }
          });
        } catch (SecurityException se) {
          final String msg = "ditolak sistem (izin)";
          Shared.err(Tools_File.this, "Directory", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              NomediaFileDeleted(Shared.clean(folder), false, msg,
                  WRITE_PERM);
            }
          });
        } catch (Exception e) {
          final String m = e.getMessage();
          final String msg = m == null ? "gagal hapus .nomedia" : m;
          Shared.err(Tools_File.this, "Directory", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              NomediaFileDeleted(Shared.clean(folder), false, msg, "");
            }
          });
        }
      }
    });
  }

  @SimpleFunction(description = "CALL: buat folder (termasuk induknya). "
      + "Sudah ada = success. Hasil lewat DirectoryCreated.")
  public void CallMakeDirectory(final String folder, final String scoop) {
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        try {
          if (wantAsset(scoop, folder)) {
            final String msg = "tidak bisa tulis ke Asset";
            Shared.err(Tools_File.this, "Directory", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                DirectoryCreated(Shared.clean(folder), false, msg, "");
              }
            });
            return;
          }
          File d = scoopFile(scoop, folder);
          if (d == null) {
            final String msg = "folder tidak valid";
            Shared.err(Tools_File.this, "Directory", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                DirectoryCreated(Shared.clean(folder), false, msg, "");
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
              DirectoryCreated(abs, ok, msg, ok ? "" : WRITE_PERM);
            }
          });
        } catch (SecurityException se) {
          final String msg = "ditolak sistem (izin)";
          Shared.err(Tools_File.this, "Directory", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              DirectoryCreated(Shared.clean(folder), false, msg, WRITE_PERM);
            }
          });
        } catch (Exception e) {
          final String m = e.getMessage();
          final String msg = m == null ? "gagal buat folder" : m;
          Shared.err(Tools_File.this, "Directory", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              DirectoryCreated(Shared.clean(folder), false, msg, "");
            }
          });
        }
      }
    });
  }

  // ================= CALL: hapus & rename =================

  @SimpleFunction(description = "CALL: hapus file/folder (folder rekursif). "
      + "Tidak ada = success. Hasil lewat FileDeleted.")
  public void CallDeleteFile(final String path, final String scoop) {
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        try {
          if (wantAsset(scoop, path)) {
            final String msg = "tidak bisa hapus dari Asset";
            Shared.err(Tools_File.this, "File", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                FileDeleted(Shared.clean(path), false, msg, "");
              }
            });
            return;
          }
          File f = scoopFile(scoop, path);
          if (f == null) {
            final String msg = "path tidak valid";
            Shared.err(Tools_File.this, "File", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                FileDeleted(Shared.clean(path), false, msg, "");
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
              FileDeleted(abs, okF, msg, okF ? "" : WRITE_PERM);
            }
          });
        } catch (SecurityException se) {
          final String msg = "ditolak sistem (izin)";
          Shared.err(Tools_File.this, "File", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              FileDeleted(Shared.clean(path), false, msg, WRITE_PERM);
            }
          });
        } catch (Exception e) {
          final String m = e.getMessage();
          final String msg = m == null ? "gagal hapus" : m;
          Shared.err(Tools_File.this, "File", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              FileDeleted(Shared.clean(path), false, msg, "");
            }
          });
        }
      }
    });
  }

  @SimpleFunction(description = "CALL: hapus BANYAK path sekaligus (tanpa loop "
      + "manual). paths = list / teks koma. Hasil lewat FileListDeleted "
      + "(total + terhapus).")
  public void CallDeleteList(final Object paths, final String scoop) {
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
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
              if (wantAsset(scoop, l.get(i))) {
                continue;
              }
              File f = scoopFile(scoop, l.get(i));
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
  public void CallRenameFile(final String path, final String newName,
      final String scoop) {
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        try {
          if (wantAsset(scoop, path)) {
            final String msg = "tidak bisa rename di Asset";
            Shared.err(Tools_File.this, "File", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                FileRenamed(Shared.clean(path), false, msg, "");
              }
            });
            return;
          }
          File f = scoopFile(scoop, path);
          if (f == null || !f.isFile()) {
            final String msg = "file tidak ada";
            Shared.err(Tools_File.this, "File", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                FileRenamed(Shared.clean(path), false, msg, "");
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
                FileRenamed(abs, false, msg, "");
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
              FileRenamed(np, ok, msg, ok ? "" : WRITE_PERM);
            }
          });
        } catch (SecurityException se) {
          final String msg = "ditolak sistem (izin)";
          Shared.err(Tools_File.this, "File", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              FileRenamed(Shared.clean(path), false, msg, WRITE_PERM);
            }
          });
        } catch (Exception e) {
          final String m = e.getMessage();
          final String msg = m == null ? "rename gagal" : m;
          Shared.err(Tools_File.this, "File", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              FileRenamed(Shared.clean(path), false, msg, "");
            }
          });
        }
      }
    });
  }

  @SimpleFunction(description = "CALL: ganti nama FOLDER (tanpa pindah). "
      + "newName = nama saja. Hasil lewat FolderRenamed.")
  public void CallRenameFolder(final String path, final String newName,
      final String scoop) {
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        try {
          if (wantAsset(scoop, path)) {
            final String msg = "tidak bisa rename di Asset";
            Shared.err(Tools_File.this, "File", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                FolderRenamed(Shared.clean(path), false, msg, "");
              }
            });
            return;
          }
          File f = findDir(scoop, path);
          if (f == null || !f.isDirectory()) {
            final String msg = "folder tidak ada";
            Shared.err(Tools_File.this, "File", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                FolderRenamed(Shared.clean(path), false, msg, "");
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
                FolderRenamed(abs, false, msg, "");
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
              FolderRenamed(np, ok, msg, ok ? "" : WRITE_PERM);
            }
          });
        } catch (SecurityException se) {
          final String msg = "ditolak sistem (izin)";
          Shared.err(Tools_File.this, "File", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              FolderRenamed(Shared.clean(path), false, msg, WRITE_PERM);
            }
          });
        } catch (Exception e) {
          final String m = e.getMessage();
          final String msg = m == null ? "rename gagal" : m;
          Shared.err(Tools_File.this, "File", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              FolderRenamed(Shared.clean(path), false, msg, "");
            }
          });
        }
      }
    });
  }

  // ================= CALL: tulis =================

  @SimpleFunction(description = "CALL: buat file kosong (folder induk dibuat "
      + "otomatis). Sudah ada = success. Hasil lewat EmptyFileCreated.")
  public void CallCreateEmptyFile(final String path, final String scoop) {
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        try {
          if (wantAsset(scoop, path)) {
            final String msg = "tidak bisa tulis ke Asset";
            Shared.err(Tools_File.this, "File", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                EmptyFileCreated(Shared.clean(path), false, msg, "");
              }
            });
            return;
          }
          File f = scoopFile(scoop, path);
          if (f == null) {
            final String msg = "path tidak valid";
            Shared.err(Tools_File.this, "File", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                EmptyFileCreated(Shared.clean(path), false, msg, "");
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
                  EmptyFileCreated(Shared.clean(path), false, msg,
                      WRITE_PERM);
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
              EmptyFileCreated(abs, okF, msg, okF ? "" : WRITE_PERM);
            }
          });
        } catch (Exception e) {
          final String m = e.getMessage();
          final String msg = m == null ? "gagal buat file" : m;
          Shared.err(Tools_File.this, "File", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              EmptyFileCreated(Shared.clean(path), false, msg, "");
            }
          });
        }
      }
    });
  }

  @SimpleFunction(description = "CALL: tulis teks ke file (UTF-8). "
      + "append=true = sambung di akhir. Hasil lewat FileSaved.")
  public void CallSaveFile(final String path, final String content,
      final boolean append, final String scoop) {
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        try {
          if (wantAsset(scoop, path)) {
            final String msg = "tidak bisa tulis ke Asset";
            Shared.err(Tools_File.this, "File", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                FileSaved(Shared.clean(path), false, msg, "");
              }
            });
            return;
          }
          File f = scoopFile(scoop, path);
          if (f == null) {
            final String msg = "path tidak valid";
            Shared.err(Tools_File.this, "File", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                FileSaved(Shared.clean(path), false, msg, "");
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
              FileSaved(abs, true, "", "");
            }
          });
        } catch (SecurityException se) {
          final String msg = "ditolak sistem (izin)";
          Shared.err(Tools_File.this, "File", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              FileSaved(Shared.clean(path), false, msg, WRITE_PERM);
            }
          });
        } catch (Exception e) {
          final String m = e.getMessage();
          final String msg = m == null ? "gagal simpan" : m;
          Shared.err(Tools_File.this, "File", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              FileSaved(Shared.clean(path), false, msg, "");
            }
          });
        }
      }
    });
  }

  @SimpleFunction(description = "CALL: simpan base64 LANGSUNG ke storage "
      + "(tanpa validasi gambar, lebih cepat). Hasil lewat Base64FileSaved. "
      + "Polling: GetLastBase64FilePath.")
  public void CallSaveBase64ToFile(final String base64, final String path,
      final String scoop) {
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        try {
          if (wantAsset(scoop, path)) {
            lastBase64Path = "";
            final String msg = "tidak bisa tulis ke Asset";
            Shared.err(Tools_File.this, "Base64", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                Base64FileSaved(Shared.clean(path), false, msg, "");
              }
            });
            return;
          }
          File f = scoopFile(scoop, path);
          if (f == null) {
            lastBase64Path = "";
            final String msg = "path tidak valid";
            Shared.err(Tools_File.this, "Base64", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                Base64FileSaved(Shared.clean(path), false, msg, "");
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
                Base64FileSaved(Shared.clean(path), false, msg, "");
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
              Base64FileSaved(abs, true, "", "");
            }
          });
        } catch (SecurityException se) {
          lastBase64Path = "";
          final String msg = "ditolak sistem (izin)";
          Shared.err(Tools_File.this, "Base64", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              Base64FileSaved(Shared.clean(path), false, msg, WRITE_PERM);
            }
          });
        } catch (Exception e) {
          lastBase64Path = "";
          final String m = e.getMessage();
          final String msg = m == null ? "gagal simpan base64" : m;
          Shared.err(Tools_File.this, "Base64", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              Base64FileSaved(Shared.clean(path), false, msg, "");
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
      + "Hasil lewat GotText (content = isi file). Mulai lewat ReadStarted.")
  public void CallReadText(final String path, final String scoop) {
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        final String p = Shared.clean(path);
        final String sc = scoop == null ? "" : scoop.trim();
        fireOnUi(new Runnable() {
          public void run() {
            ReadStarted(sc, p);
          }
        });
        try {
          if (wantAsset(scoop, p)) {
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
                GotText(true, sc, "//" + Shared.assetName(p), nm, txt);
              }
            });
            return;
          }
          File f = scoopFile(scoop, p);
          if (f == null || !f.isFile()) {
            final String msg = "file tidak ada";
            Shared.err(Tools_File.this, "Read", 0, msg);
            fireOnUi(new Runnable() {
              public void run() {
                GotText(false, sc, p, "", "");
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
              GotText(true, sc, abs, nm, txt);
            }
          });
        } catch (SecurityException se) {
          final String msg = "ditolak sistem (izin)";
          Shared.err(Tools_File.this, "Read", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              GotText(false, sc, p, "", "");
            }
          });
        } catch (Exception e) {
          final String m = e.getMessage();
          final String msg = m == null ? "baca gagal" : m;
          Shared.err(Tools_File.this, "Read", 0, msg);
          fireOnUi(new Runnable() {
            public void run() {
              GotText(false, sc, p, "", "");
            }
          });
        }
      }
    });
  }

  // ================= CALL: salin / pindah =================

  @SimpleFunction(description = "CALL: salin ATAU pindah file (isMove=true = "
      + "pindah). from/to bawa scoop sendiri-sendiri. Nama tujuan boleh beda. "
      + "Bila tujuan SUDAH ADA: tidak disalin, Finished(success=true, "
      + "isDuplicated=true) -> rename manual ala contoh. Asset hanya bisa "
      + "jadi sumber. Satu job per saat.")
  public void CallCopyOrMoveFile(final String fromFileName,
      final String fromScoop, final String toFileName, final String toScoop,
      final boolean isMove) {
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
    if (wantAsset(toScoop, toFileName)) {
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
        runCopyOrMove(isMove, fromScoop, fromFileName, toScoop, toFileName);
      }
    });
  }

  @SimpleFunction(description = "SET: batalkan salinan/pindahan berjalan. "
      + "Tanpa feedback (file sebagian dihapus).")
  public void SetCancelCopyOrMove() {
    copyMoveCancel = true;
  }

  private void runCopyOrMove(boolean move, String fS, String fP, String tS,
      String tP) {
    String comp = "CopyOrMove";
    InputStream in = null;
    boolean fromAsset = false;
    String aName = "";
    File src = scoopFile(fS, fP);
    long total = 0;
    try {
      if (src == null) {
        String p = Shared.clean(fP);
        if (wantAsset(fS, p)) {
          fromAsset = true;
          aName = Shared.assetName(p);
        } else {
          finishCopyOrMove(move, false, "", "",
              Shared.fileNameOf(fP), false, "path sumber tidak valid");
          return;
        }
      } else if (src.isDirectory()) {
        finishCopyOrMove(move, false, "", "", src.getName(), false,
            "sumber adalah folder");
        return;
      } else if (!src.exists()) {
        finishCopyOrMove(move, false, "", "", src.getName(), false,
            "file sumber tidak ada");
        return;
      }
      File dst = scoopFile(tS, tP);
      if (dst == null) {
        finishCopyOrMove(move, false, "", "",
            Shared.fileNameOf(tP), false, "tujuan tidak valid");
        return;
      }
      if (dst.exists() && dst.isDirectory()) {
        finishCopyOrMove(move, false, "", "", dst.getName(), false,
            "tujuan adalah folder");
        return;
      }
      if (dst.exists() && dst.isFile()) {
        // Sudah ada: JANGAN timpa. Lapor duplikat agar user rename manual.
        finishCopyOrMove(move, true, dst.getAbsolutePath(), tS == null ? ""
            : tS.trim(), dst.getName(), true, "");
        return;
      }
      if (dst.getParentFile() != null) {
        dst.getParentFile().mkdirs();
      }
      final String dAbs = dst.getAbsolutePath();
      final String dName = dst.getName();
      final String tSc = tS == null ? "" : tS.trim();
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
          finishCopyOrMove(true, true, dAbs, tSc, dName, false, "");
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
            finishCopyOrMove(move, false, "", "", "", false, "dibatalkan");
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
        finishCopyOrMove(move, false, "", "", "", false, "dibatalkan");
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
      finishCopyOrMove(move, true, dAbs, tSc, dName, false, "");
    } catch (SecurityException se) {
      finishCopyOrMove(move, false, "", "", Shared.fileNameOf(tP), false,
          "ditolak sistem (izin)");
    } catch (Exception e) {
      String m = e.getMessage();
      finishCopyOrMove(move, false, "", "", Shared.fileNameOf(tP), false,
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
      final String absPath, final String scope, final String fileName,
      final boolean duplicated, final String msg) {
    if (!success && msg.length() > 0) {
      Shared.err(this, "CopyOrMove", 0, msg);
    }
    final boolean s = success;
    final String p = absPath;
    final String sc = scope;
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
      + "Olah string saja, tanpa scoop.")
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

  @SimpleFunction(description = "Path absolut dari scoop+path. URL/base64/"
      + "Asset = kosong. scoop \"\" = absolut apa adanya.")
  public String GetFullPath(String path, String scoop) {
    try {
      if (wantAsset(scoop, path)) {
        return "//" + Shared.assetName(Shared.clean(path));
      }
      File f = scoopFile(scoop, path);
      return f == null ? "" : f.getAbsolutePath();
    } catch (Exception e) {
      return "";
    }
  }

  @SimpleFunction(description = "Ukuran file dalam byte. Folder = total isi "
      + "rekursif. scoop seperti IsFileExists.")
  public long FileSize(String path, String scoop) {
    try {
      if (wantAsset(scoop, path)) {
        try {
          return context.getAssets().openFd(
              Shared.assetName(Shared.clean(path))).getLength();
        } catch (Exception e) {
          return -1L;
        }
      }
      File f = scoopFile(scoop, path);
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

  @SimpleFunction(description = "Sisa ruang kosong (byte) di penyimpanan "
      + "scoop itu. scoop \"\" = kartu SD.")
  public long FreeSpace(String scoop) {
    try {
      File r = scoopFile(scoop, "");
      if (r == null) {
        return -1L;
      }
      return r.getFreeSpace();
    } catch (Exception e) {
      return -1L;
    }
  }

  @SimpleFunction(description = "Total kapasitas (byte) penyimpanan scoop "
      + "itu. -1 bila gagal.")
  public long TotalSpace(String scoop) {
    try {
      File r = scoopFile(scoop, "");
      if (r == null) {
        return -1L;
      }
      return r.getTotalSpace();
    } catch (Exception e) {
      return -1L;
    }
  }

  @SimpleFunction(description = "Daftar file (path lengkap). filter = "
      + "ekstensi tanpa titik (boleh koma: jpg,png; kosong = semua). "
      + "recursive = sertakan isi subfolder. scoop seperti IsFileExists.")
  public List<String> ListFiles(String path, String filter, boolean recursive,
      String scoop) {
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
      if (wantAsset(scoop, p)) {
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
      File d = scoopFile(scoop, p);
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

  @SimpleFunction(description = "Daftar subfolder (path lengkap, tanpa "
      + "rekursif). scoop seperti IsFileExists.")
  public List<String> ListFolders(String folder, String scoop) {
    ArrayList<String> out = new ArrayList<String>();
    try {
      String p = Shared.clean(folder);
      if (wantAsset(scoop, p)) {
        return out;
      }
      File d = scoopFile(scoop, p);
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

  @SimpleFunction(description = "Kategori file dari nama/path: image, audio, "
      + "video, text, archive, document, apk, atau unknown. Berdasarkan "
      + "ekstensi (tak peduli huruf besar). Bandingkan dengan blok "
      + "TypeImage/dll. Olah string saja.")
  public String FileTypeOf(String pathOrName) {
    return Shared.fileCategory(pathOrName);
  }

  @SimpleFunction(description = "Daftar file (path lengkap) yang kategorinya "
      + "cocok. type = image/audio/video/text/archive/document/apk (boleh "
      + "alias: gambar/musik/vidio/teks/arsip/dokumen) atau salah satu blok "
      + "TypeImage/dll. recursive = sertakan isi subfolder. "
      + "scoop seperti IsFileExists.")
  public List<String> ListFilesByType(String folder, String type,
      boolean recursive, String scoop) {
    ArrayList<String> out = new ArrayList<String>();
    String cat = Shared.normCategory(type);
    if (cat.length() == 0) {
      Shared.err(this, "File", 0, "tipe tidak dikenal: "
          + (type == null ? "" : type));
      return out;
    }
    try {
      String p = Shared.clean(folder);
      if (wantAsset(scoop, p)) {
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
      File d = scoopFile(scoop, p);
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

  @SimpleFunction(description = "Minta izin Android (cth: "
      + "android.permission.READ_EXTERNAL_STORAGE). Hasil lewat "
      + "GotPermission. Pasang need_permission dari event lain ke sini.")
  public void GetPermission(final String permission) {
    final String p = Shared.clean(permission);
    if (p.length() == 0) {
      Shared.err(this, "Permission", 0, "izin kosong");
      fireOnUi(new Runnable() {
        public void run() {
          GotPermission(false, "", "izin kosong");
        }
      });
      return;
    }
    try {
      form.askForPermission(p, new PermissionResultHandler() {
        public void HandlePermissionResponse(String perm, boolean granted) {
          final boolean g = granted;
          fireOnUi(new Runnable() {
            public void run() {
              if (!g) {
                Shared.err(Tools_File.this, "Permission", 0,
                    "izin ditolak: " + p);
              }
              GotPermission(g, p, g ? "" : "izin ditolak: " + p);
            }
          });
        }
      });
    } catch (Exception e) {
      String m = e.getMessage();
      if (m == null) {
        m = "minta izin gagal";
      }
      final String msg = m;
      Shared.err(this, "Permission", 0, msg);
      fireOnUi(new Runnable() {
        public void run() {
          GotPermission(false, p, msg);
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
      if (resultCode == Activity.RESULT_OK && data != null
          && data.getData() != null) {
        final Uri uri = data.getData();
        final String uriStr = uri.toString();
        final String path = resolveUriToPath(uri);
        AfterFilePicked(true, "Cache", uriStr, path,
            Shared.fileNameOf(path));
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

  @SimpleFunction(description = "Buka file dengan aplikasi lain (musik, "
      + "gambar, PDF, ...). scoop seperti IsFileExists. Hasil lewat "
      + "AfterOpenFile. Asset disalin ke cache dulu.")
  public void OpenFile(String path, String scoop) {
    try {
      String p = Shared.clean(path);
      File f = scoopFile(scoop, path);
      if (f == null) {
        if (wantAsset(scoop, p)) {
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

  @SimpleEvent(description = "Hasil CallSetHiddenDirectory: path + success + "
      + "message + needPermission (isi = izin yang harus diminta bila "
      + "gagal karena izin).")
  public void DirectoryHidden(String path, boolean success, String message,
      String needPermission) {
    EventDispatcher.dispatchEvent(this, "DirectoryHidden", path,
        Boolean.valueOf(success), message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallMakeNomediaFile: path + success + "
      + "message + needPermission.")
  public void NomediaFileCreated(String path, boolean success, String message,
      String needPermission) {
    EventDispatcher.dispatchEvent(this, "NomediaFileCreated", path,
        Boolean.valueOf(success), message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallDeleteNomediaFile: path + success + "
      + "message + needPermission.")
  public void NomediaFileDeleted(String path, boolean success, String message,
      String needPermission) {
    EventDispatcher.dispatchEvent(this, "NomediaFileDeleted", path,
        Boolean.valueOf(success), message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallMakeDirectory: path + success + "
      + "message + needPermission.")
  public void DirectoryCreated(String path, boolean success, String message,
      String needPermission) {
    EventDispatcher.dispatchEvent(this, "DirectoryCreated", path,
        Boolean.valueOf(success), message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallDeleteFile: path + success + "
      + "message + needPermission.")
  public void FileDeleted(String path, boolean success, String message,
      String needPermission) {
    EventDispatcher.dispatchEvent(this, "FileDeleted", path,
        Boolean.valueOf(success), message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallDeleteList: success + total + "
      + "terhapus + message + needPermission.")
  public void FileListDeleted(boolean success, int total, int deleted,
      String message, String needPermission) {
    EventDispatcher.dispatchEvent(this, "FileListDeleted",
        Boolean.valueOf(success), Integer.valueOf(total),
        Integer.valueOf(deleted), message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallRenameFile: path (nama baru) + "
      + "success + message + needPermission.")
  public void FileRenamed(String path, boolean success, String message,
      String needPermission) {
    EventDispatcher.dispatchEvent(this, "FileRenamed", path,
        Boolean.valueOf(success), message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallRenameFolder: path (nama baru) + "
      + "success + message + needPermission.")
  public void FolderRenamed(String path, boolean success, String message,
      String needPermission) {
    EventDispatcher.dispatchEvent(this, "FolderRenamed", path,
        Boolean.valueOf(success), message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallCreateEmptyFile: path + success + "
      + "message + needPermission.")
  public void EmptyFileCreated(String path, boolean success, String message,
      String needPermission) {
    EventDispatcher.dispatchEvent(this, "EmptyFileCreated", path,
        Boolean.valueOf(success), message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallSaveFile: path + success + "
      + "message + needPermission.")
  public void FileSaved(String path, boolean success, String message,
      String needPermission) {
    EventDispatcher.dispatchEvent(this, "FileSaved", path,
        Boolean.valueOf(success), message, needPermission);
  }

  @SimpleEvent(description = "Hasil CallSaveBase64ToFile: path + success + "
      + "message + needPermission.")
  public void Base64FileSaved(String path, boolean success, String message,
      String needPermission) {
    EventDispatcher.dispatchEvent(this, "Base64FileSaved", path,
        Boolean.valueOf(success), message, needPermission);
  }

  @SimpleEvent(description = "Bacaan dimulai (CallReadText): persiapan "
      + "(tampilkan loading).")
  public void ReadStarted(String scope, String path) {
    EventDispatcher.dispatchEvent(this, "ReadStarted", scope, path);
  }

  @SimpleEvent(description = "Hasil CallReadText: success pertama, scoop "
      + "menemani path, fileName paling belakang, content = isi file.")
  public void GotText(boolean success, String scope, String path,
      String fileName, String content) {
    EventDispatcher.dispatchEvent(this, "GotText", Boolean.valueOf(success),
        scope, path, fileName, content);
  }

  @SimpleEvent(description = "Salin/pindah dimulai: path sumber + tujuan + "
      + "nama file + isMoved.")
  public void CopyOrMoveStarted(String fromPath, String toPath,
      String fileName, boolean isMoved) {
    EventDispatcher.dispatchEvent(this, "CopyOrMoveStarted", fromPath,
        toPath, fileName, Boolean.valueOf(isMoved));
  }

  @SimpleEvent(description = "Progres salin/pindah: persen 0-100 + byte "
      + "total & berjalan.")
  public void CopyOrMoveProgressChanged(int progress, long byteTotal,
      long byteProgress) {
    EventDispatcher.dispatchEvent(this, "CopyOrMoveProgressChanged",
        Integer.valueOf(progress), Long.valueOf(byteTotal),
        Long.valueOf(byteProgress));
  }

  @SimpleEvent(description = "Salin/pindah selesai: success + path + nama "
      + "file + isDuplicated (true = tujuan sudah ada, tidak ditimpa, "
      + "rename manual).")
  public void CopyOrMoveFinished(boolean success, String path,
      String fileName, boolean isDuplicated) {
    EventDispatcher.dispatchEvent(this, "CopyOrMoveFinished",
        Boolean.valueOf(success), path, fileName,
        Boolean.valueOf(isDuplicated));
  }

  @SimpleEvent(description = "Hasil GetPermission: success + izin + pesan. "
      + "Pasang need_permission dari event lain ke GetPermission.")
  public void GotPermission(boolean success, String permission,
      String message) {
    EventDispatcher.dispatchEvent(this, "GotPermission",
        Boolean.valueOf(success), permission, message);
  }

  @SimpleEvent(description = "Hasil OpenFilePicker: success pertama, scope "
      + "menemani path, fileName paling belakang. success=false = dibatalkan "
      + "user.")
  public void AfterFilePicked(boolean success, String scope, String uri,
      String path, String fileName) {
    EventDispatcher.dispatchEvent(this, "AfterFilePicked",
        Boolean.valueOf(success), scope, uri, path, fileName);
  }

  @SimpleEvent(description = "Hasil OpenFile: success + pesan.")
  public void AfterOpenFile(boolean success, String message) {
    EventDispatcher.dispatchEvent(this, "AfterOpenFile",
        Boolean.valueOf(success), message);
  }

  @SimpleEvent(description = "SATU-SATUNYA error Tools_File. component = nama "
      + "operasi (bandingkan dengan blok Op...). id selalu 0 (tanpa id).")
  public void ErrorOccurred(String component, int id, String message) {
    EventDispatcher.dispatchEvent(this, "ErrorOccurred", component,
        Integer.valueOf(id), message);
  }

  @SimpleFunction(description = "Fallback polling untuk event ErrorOccurred: "
      + "pesan error terakhir operasi ini (Directory/File/Read/Base64/"
      + "CopyOrMove/Pick/Open/Permission, kosong bila belum ada).")
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
