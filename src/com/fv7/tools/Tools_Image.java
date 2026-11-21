package com.fv7.tools;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Handler;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.AndroidViewComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.OnDestroyListener;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.YailList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@DesignerComponent(
    version = 23,
    description = "Tools_Image: tampilkan BANYAK gambar ber-id ke layout (path/URL/base64 mandiri) + "
        + "gambar dari base64 + info ukuran. Id dikelola CreateImageId / Tools_Base. "
        + "Sukses menembak ImageShown (id + path); gagal via ErrorOccurred. "
        + "List id URUT PEMBUATAN (index 1 = pertama dibuat). INGAT DUA KALENDER: "
        + "daftar id mulai dari 1, TAPI halaman slider mulai dari 0. Slider TANPA "
        + "batas jumlah; URL gambar diunduh sekali lalu di-cache otomatis (maks "
        + "80MB bawaan, atur via SetImageCacheMaxMB; terlama dihapus dulu; "
        + "bersihkan manual via SetClearImageCache). "
        + "Konvensi awalan: Is = boolean (colok ke if), Call = async + hasil lewat "
        + "event, Set = tanpa feedback sukses (gagal via ErrorOccurred), Get = nilai "
        + "balik sinkron.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    androidMinSdk = 21,
    iconName = "aiwebres/tools_image.jpg",
    helpUrl = "https://github.com/VB-7/MiT-File_Manager")
@SimpleObject(external = true)
@UsesPermissions({"android.permission.INTERNET",
    "android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE"})
public class Tools_Image extends AndroidNonvisibleComponent
    implements OnDestroyListener {

  private final Context context;
  private final Activity activity;

  private static final Map<Integer, ImageView> views = new HashMap<Integer, ImageView>();
  private static final Object viewLock = new Object();

  /** Status zoom per id gambar (1.0 = pas). */
  private static final Map<Integer, ZoomHandler> zoomHandlers =
      new HashMap<Integer, ZoomHandler>();

  /** Slider: root scroll + deret gambar. */
  private static final class Slider {
    ViewGroup root;
    LinearLayout box;
    ArrayList<ImageView> pages = new ArrayList<ImageView>();
    int index = 0;
    boolean horizontal = true;
    int width = ViewGroup.LayoutParams.MATCH_PARENT;
    int height = ViewGroup.LayoutParams.WRAP_CONTENT;
    boolean fit = true;
    ArrayList<String> sources = new ArrayList<String>();
    int spacingPx = 0;
    int selected = -1;
    int lastFail = 0;
    int preloadN = 0;
    int winGen = 0;
    boolean rememberPos = false;
    ArrayList<Boolean> loaded = new ArrayList<Boolean>();
    Handler auto;
    Runnable autoTask;
    long interval = 0;
  }

  private static final Map<Integer, Slider> sliders =
      new HashMap<Integer, Slider>();
  private static String lastPicturePath = "";

  public Tools_Image(ComponentContainer container) {
    super(container.$form());
    this.context = container.$context();
    this.activity = container.$context();
  }

  private ViewGroup groupOf(AndroidViewComponent container) {
    if (container == null) {
      return null;
    }
    try {
      View v = container.getView();
      if (v instanceof ViewGroup) {
        return (ViewGroup) v;
      }
    } catch (Exception e) {
      // abaikan
    }
    return null;
  }

  private void detach(View v) {
    try {
      if (v != null && v.getParent() instanceof ViewGroup) {
        ((ViewGroup) v.getParent()).removeView(v);
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  private static boolean looksBase64(String s) {
    if (s == null) {
      return false;
    }
    String t = s.trim();
    if (t.length() < 100) {
      return false;
    }
    if (t.indexOf('.') >= 0 || t.indexOf(' ') >= 0 || t.indexOf('\\') >= 0
        || t.indexOf(':') >= 0 || t.indexOf('/') == 0) {
      return false;
    }
    for (int i = 0; i < t.length(); i = i + 1) {
      char c = t.charAt(i);
      boolean ok = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
          || (c >= '0' && c <= '9') || c == '+' || c == '/' || c == '='
          || c == '\r' || c == '\n';
      if (!ok) {
        return false;
      }
    }
    return true;
  }

  private Bitmap boundedDecode(byte[] data) {
    try {
      BitmapFactory.Options o = new BitmapFactory.Options();
      o.inJustDecodeBounds = true;
      BitmapFactory.decodeByteArray(data, 0, data.length, o);
      int w = o.outWidth;
      int h = o.outHeight;
      int sample = 1;
      while (((w / sample) > 2048 || (h / sample) > 2048) && sample < 16) {
        sample = sample * 2;
      }
      BitmapFactory.Options o2 = new BitmapFactory.Options();
      o2.inSampleSize = sample;
      return BitmapFactory.decodeByteArray(data, 0, data.length, o2);
    } catch (Exception e) {
      return null;
    }
  }

  // ================= CACHE URL (hemat unduh-ulang) =================

  /** Batas cache URL (byte). Default 80MB; atur via SetImageCacheMaxMB. */
  private long imgCacheMaxBytes = 80L * 1024L * 1024L;

  private File imgCacheDir() {
    try {
      File base = context.getCacheDir();
      if (base == null) {
        return null;
      }
      File d = new File(base, "tools_img");
      if (!d.exists()) {
        d.mkdirs();
      }
      return d;
    } catch (Exception e) {
      return null;
    }
  }

  private static String md5hex(String s) {
    try {
      java.security.MessageDigest md =
          java.security.MessageDigest.getInstance("MD5");
      byte[] h = md.digest(s.getBytes("UTF-8"));
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < h.length; i = i + 1) {
        String hx = Integer.toHexString(h[i] & 0xFF);
        if (hx.length() == 1) {
          sb.append('0');
        }
        sb.append(hx);
      }
      return sb.toString();
    } catch (Exception e) {
      return null;
    }
  }

  private static String urlExt(String url) {
    try {
      String u = url;
      int q = u.indexOf('?');
      if (q >= 0) {
        u = u.substring(0, q);
      }
      int hsh = u.indexOf('#');
      if (hsh >= 0) {
        u = u.substring(0, hsh);
      }
      int dot = u.lastIndexOf('.');
      int slash = u.lastIndexOf('/');
      if (dot > slash && dot < u.length() - 1) {
        String e = u.substring(dot + 1).toLowerCase();
        if (e.equals("jpg") || e.equals("jpeg") || e.equals("png")
            || e.equals("gif") || e.equals("webp") || e.equals("bmp")) {
          return "." + e;
        }
      }
    } catch (Exception e) {
      // abaikan
    }
    return ".img";
  }

  /** Ambil byte URL dari cache disk. null bila be