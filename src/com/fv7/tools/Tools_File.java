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
      return new File(d, ".nom