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
    version = 20,
    description = "Tools_Image: tampilkan BANYAK gambar ber-id ke layout (path/URL/base64 mandiri) + "
        + "gambar dari base64 + info ukuran. Id dikelola CreateImageId / Tools_Base. "
        + "Sukses menembak ImageShown (id + path); gagal via ErrorOccurred. "
        + "List id URUT PEMBUATAN (index 1 = pertama dibuat). INGAT DUA KALENDER: "
        + "daftar id mulai dari 1, TAPI halaman slider mulai dari 0. Slider TANPA "
        + "batas jumlah; URL gambar diunduh sekali lalu di-cache otomatis (maks "
        + "50MB, terlama dihapus dulu; bersihkan manual via SetClearImageCache). "
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

  private static final long IMG_CACHE_MAX = 50L * 1024L * 1024L;

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

  /** Ambil byte URL dari cache disk. null bila belum ada/gagal. */
  private byte[] readUrlCache(String url) {
    try {
      File d = imgCacheDir();
      String k = md5hex(url);
      if (d == null || k == null) {
        return null;
      }
      File f = new File(d, k + urlExt(url));
      if (!f.exists()) {
        return null;
      }
      FileInputStream fin = new FileInputStream(f);
      try {
        return Shared.readAll(fin);
      } finally {
        try {
          fin.close();
        } catch (Exception e) {
          // abaikan
        }
      }
    } catch (Exception e) {
      return null;
    }
  }

  /** Simpan byte URL ke cache disk (gagal diam-diam = unduh ulang lain kali). */
  private void writeUrlCache(String url, byte[] data) {
    try {
      File d = imgCacheDir();
      String k = md5hex(url);
      if (d == null || k == null || data == null) {
        return;
      }
      File f = new File(d, k + urlExt(url));
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
      pruneImgCache(d);
    } catch (Exception e) {
      // abaikan
    }
  }

  private static void pruneImgCache(File dir) {
    try {
      File[] fs = dir.listFiles();
      if (fs == null || fs.length == 0) {
        return;
      }
      long total = 0;
      for (int i = 0; i < fs.length; i = i + 1) {
        total = total + fs[i].length();
      }
      if (total <= IMG_CACHE_MAX) {
        return;
      }
      for (int i = 0; i < fs.length; i = i + 1) {
        for (int j = i + 1; j < fs.length; j = j + 1) {
          if (fs[j].lastModified() < fs[i].lastModified()) {
            File t = fs[i];
            fs[i] = fs[j];
            fs[j] = t;
          }
        }
      }
      for (int i = 0; i < fs.length && total > IMG_CACHE_MAX; i = i + 1) {
        total = total - fs[i].length();
        try {
          fs[i].delete();
        } catch (Exception e) {
          // abaikan
        }
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "CALL: tampilkan gambar ber-id ke layout (Arrangement MIT / dinamis). "
      + "SATU pintu input: path file/asset, file:/// absolut, http(s) URL "
      + "(diunduh sekali lalu di-cache otomatis — tampil ulang = tanpa unduh), "
      + "base64 (mentah / data:...), atau content:// — "
      + "terdeteksi otomatis. Bisa jalan bareng (satu id = satu gambar). "
      + "id = penanda (minta ke CreateImageId / "
      + "Tools_Base CreateUniqueId, reservasi otomatis diadopsi). "
      + "width/height angka MIT: -2 = isi penuh, -1 = otomatis, positif = pixel. "
      + "fit true = seluruh gambar muat (tidak kepotong); false = penuhi tampilan "
      + "(bisa kepotong). Sukses menembak "
      + "ImageShown; gagal via ErrorOccurred.")
  public void CallShowImage(final AndroidViewComponent container, final String source,
      final int id, final int width, final int height,
      final boolean fit) {
    if (id < 1) {
      Shared.err(this, "Image", id, "id harus >= 1 (pakai CreateImageId)");
      return;
    }
    if (!Ids.register(Ids.IMAGE, id)) {
      Shared.err(this, "Image", id, "ID " + id + " sedang dipakai gambar lain");
      return;
    }
    synchronized (viewLock) {
      if (views.containsKey(Integer.valueOf(id))) {
        Shared.err(this, "Image", id, "ID " + id + " masih tampil "
            + "(Remove/Clear dulu)");
        return;
      }
    }
    Ids.setActive(Ids.IMAGE, id, true);
    final String src = Shared.clean(source == null ? "" : source);
    final AndroidViewComponent fContainer = container;
    final int fW = width;
    final int fH = height;
    final boolean fFit = fit;
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        Bitmap bmp = null;
        String disp = "";
        String err = "";
        try {
          if (src.regionMatches(true, 0, "http", 0, 4)) {
            String name = Shared.sanitize(guessName(src));
            File cache = context.getCacheDir();
            if (!cache.exists()) {
              cache.mkdirs();
            }
            File dst = Shared.uniqueFile(cache, "img_" + id + "_" + name);
            byte[] data = readUrlCache(src);
            if (data == null) {
              HttpURLConnection c = (HttpURLConnection) new URL(src).openConnection();
              c.setConnectTimeout(20000);
              c.setReadTimeout(20000);
              c.setInstanceFollowRedirects(true);
              c.setRequestProperty("User-Agent", "Mozilla/5.0");
              int code = c.getResponseCode();
              if (code >= 400) {
                err = "HTTP " + code;
              } else {
                InputStream in = c.getInputStream();
                try {
                  data = Shared.readAll(in);
                } finally {
                  try {
                    in.close();
                  } catch (Exception e) {
                    // abaikan
                  }
                }
                try {
                  c.disconnect();
                } catch (Exception e) {
                  // abaikan
                }
                if (data != null) {
                  writeUrlCache(src, data);
                }
              }
            }
            if (err.length() == 0 && data != null) {
              bmp = boundedDecode(data);
              if (bmp == null) {
                err = "URL bukan gambar yang valid";
              } else {
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
                disp = dst.getAbsolutePath();
              }
            } else if (err.length() == 0) {
              err = "URL tidak bisa diunduh";
            }
          } else if (src.startsWith("data:") || looksBase64(src)) {
            String b64 = src;
            int comma = src.indexOf(',');
            if (src.startsWith("data:") && comma >= 0) {
              b64 = src.substring(comma + 1);
            }
            byte[] data = Base64.decode(b64.trim(), Base64.DEFAULT);
            bmp = boundedDecode(data);
            if (bmp == null) {
              err = "base64 bukan gambar yang valid";
            } else {
              File cache = context.getCacheDir();
              if (!cache.exists()) {
                cache.mkdirs();
              }
              File dst = new File(cache, "img_" + id + ".png");
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
              disp = dst.getAbsolutePath();
            }
          } else if (src.regionMatches(true, 0, "content://", 0, 10)) {
            InputStream in = null;
            try {
              in = context.getContentResolver().openInputStream(
                  android.net.Uri.parse(src));
            } catch (Exception e) {
              in = null;
            }
            if (in == null) {
              err = "content tidak bisa dibuka";
            } else {
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
              bmp = boundedDecode(data);
              if (bmp == null) {
                err = "content bukan gambar yang valid";
              } else {
                File cache = context.getCacheDir();
                if (!cache.exists()) {
                  cache.mkdirs();
                }
                File dst = Shared.uniqueFile(cache, "img_" + id + "_content.png");
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
                disp = dst.getAbsolutePath();
              }
            }
          } else {
            String p = src;
            if (p.length() == 0) {
              err = "gambar kosong";
            } else if (Shared.isAssetPath(p)) {
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
              bmp = boundedDecode(data);
              if (bmp == null) {
                err = "asset bukan gambar yang valid";
              } else {
                disp = p;
              }
            } else {
              File f = Shared.resolveRooted(p);
              if (f == null || !f.exists()) {
                err = "file tidak ada";
              } else {
                FileInputStream fin = new FileInputStream(f);
                byte[] data;
                try {
                  data = Shared.readAll(fin);
                } finally {
                  try {
                    fin.close();
                  } catch (Exception e) {
                    // abaikan
                  }
                }
                bmp = boundedDecode(data);
                if (bmp == null) {
                  err = "file bukan gambar yang valid";
                } else {
                  disp = f.getAbsolutePath();
                }
              }
            }
          }
        } catch (Exception e) {
          String m = e.getMessage();
          err = m == null ? "gambar gagal ditampilkan" : m;
        }
        final Bitmap fBmp = bmp;
        final String fErr = err;
        final String fDisp = disp;
        if (fBmp == null) {
          Ids.setActive(Ids.IMAGE, id, false);
          Ids.unregister(Ids.IMAGE, id);
          Shared.err(Tools_Image.this, "Image", id,
              fErr.length() > 0 ? fErr : "gambar gagal ditampilkan");
          return;
        }
        try {
          form.runOnUiThread(new Runnable() {
            public void run() {
              try {
                ViewGroup vg = groupOf(fContainer);
                if (vg == null) {
                  Ids.setActive(Ids.IMAGE, id, false);
                  Ids.unregister(Ids.IMAGE, id);
                  Shared.err(Tools_Image.this, "Image", id, "layout tidak valid");
                  return;
                }
                ImageView iv;
                synchronized (viewLock) {
                  iv = views.get(Integer.valueOf(id));
                  if (iv == null) {
                    iv = new ImageView(activity);
                    iv.setScaleType(fFit ? ImageView.ScaleType.FIT_CENTER
                        : ImageView.ScaleType.CENTER_CROP);
                    iv.setAdjustViewBounds(fFit);
                    views.put(Integer.valueOf(id), iv);
                  }
                }
                detach(iv);
                final ImageView fIv = iv;
                iv.setOnClickListener(new View.OnClickListener() {
                  public void onClick(View vv) {
                    ImageClicked(id);
                  }
                });
                iv.setOnLongClickListener(new View.OnLongClickListener() {
                  public boolean onLongClick(View vv) {
                    ImageLongClicked(id);
                    return true;
                  }
                });
                ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
                    mitToAndroidSize(fW),
                    mitToAndroidSize(fH));
                iv.setImageBitmap(fBmp);
                vg.addView(iv, lp);
                ImageShown(id, fDisp);
              } catch (Exception e) {
                String m = e.getMessage();
                Ids.setActive(Ids.IMAGE, id, false);
                Ids.unregister(Ids.IMAGE, id);
                Shared.err(Tools_Image.this, "Image", id,
                    m == null ? "gambar gagal ditampilkan" : m);
              }
            }
          });
        } catch (Exception e) {
          String m = e.getMessage();
          Ids.setActive(Ids.IMAGE, id, false);
          Ids.unregister(Ids.IMAGE, id);
          Shared.err(Tools_Image.this, "Image", id,
              m == null ? "gambar gagal ditampilkan" : m);
        }
      }
    });
  }

  private String guessName(String url) {
    try {
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
      String n = (s >= 0 && s + 1 < u.length()) ? u.substring(s + 1) : "image";
      n = Shared.sanitize(n);
      if (n.indexOf('.') < 0) {
        n = n + ".png";
      }
      return n;
    } catch (Exception e) {
      return "image.png";
    }
  }

  @SimpleFunction(description = "SET: hapus SEMUA gambar Tools_Image dari layout itu "
      + "(komponen lain tidak diusik, id ikut dilepas). Tanpa feedback.")
  public void SetClearImage(final AndroidViewComponent container) {
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            ViewGroup vg = groupOf(container);
            if (vg == null) {
              Shared.err(Tools_Image.this, "Image", 0, "layout tidak valid");
              return;
            }
            synchronized (viewLock) {
              ArrayList<Integer> gone = new ArrayList<Integer>();
              for (Map.Entry<Integer, ImageView> en : views.entrySet()) {
                ImageView iv = en.getValue();
                if (iv != null && iv.getParent() == vg) {
                  try {
                    vg.removeView(iv);
                  } catch (Exception e) {
                    // abaikan
                  }
                  gone.add(en.getKey());
                }
              }
              for (int i = 0; i < gone.size(); i = i + 1) {
                int gid = gone.get(i).intValue();
                views.remove(Integer.valueOf(gid));
                Ids.setActive(Ids.IMAGE, gid, false);
                Ids.unregister(Ids.IMAGE, gid);
              }
            }
          } catch (Exception e) {
            String m = e.getMessage();
            Shared.err(Tools_Image.this, "Image", 0, m == null ? "clear gagal" : m);
          }
        }
      });
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Image", 0, m == null ? "clear gagal" : m);
    }
  }

  /** Lepas satu gambar ber-id (lepas view + lepas id). Dipakai komponen + Base. */
  public static void clearStatic(int id) {
    synchronized (viewLock) {
      ImageView iv = views.remove(Integer.valueOf(id));
      if (iv != null) {
        try {
          iv.setOnClickListener(null);
          iv.setOnLongClickListener(null);
          iv.setOnTouchListener(null);
          if (iv.getParent() instanceof ViewGroup) {
            ((ViewGroup) iv.getParent()).removeView(iv);
          }
        } catch (Exception e) {
          // abaikan
        }
      }
      zoomHandlers.remove(Integer.valueOf(id));
    }
    Ids.setActive(Ids.IMAGE, id, false);
    Ids.unregister(Ids.IMAGE, id);
  }

  /** Hapus semua gambar/slider yang view-nya ada di dalam/di bawah root. */
  public static void clearByView(View root) {
    if (root == null) {
      return;
    }
    ArrayList<Integer> gone = new ArrayList<Integer>();
    ArrayList<Integer> goneSliders = new ArrayList<Integer>();
    synchronized (viewLock) {
      for (Map.Entry<Integer, ImageView> en : views.entrySet()) {
        try {
          View v = en.getValue();
          if (v != null && (v == root || isUnder(v, root))) {
            gone.add(en.getKey());
          }
        } catch (Exception e) {
          // abaikan
        }
      }
      for (Map.Entry<Integer, Slider> en : sliders.entrySet()) {
        try {
          View v = en.getValue() == null ? null : en.getValue().root;
          if (v != null && (v == root || isUnder(v, root))) {
            goneSliders.add(en.getKey());
          }
        } catch (Exception e) {
          // abaikan
        }
      }
    }
    for (int i = 0; i < gone.size(); i = i + 1) {
      clearStatic(gone.get(i).intValue());
    }
    for (int i = 0; i < goneSliders.size(); i = i + 1) {
      clearSliderStatic(goneSliders.get(i).intValue());
    }
  }

  private static boolean isUnder(View v, View root) {
    try {
      Object p = v.getParent();
      while (p instanceof View) {
        if (p == root) {
          return true;
        }
        p = ((View) p).getParent();
      }
    } catch (Exception e) {
      // abaikan
    }
    return false;
  }

  // ================= ZOOM =================

  private final class ZoomHandler implements View.OnTouchListener {
    final int zid;
    final ImageView ziv;
    final Matrix matrix = new Matrix();
    final Matrix base = new Matrix();
    float scale = 1.0f;
    float lastFired = 1.0f;
    final float maxScale = 6.0f;
    ScaleGestureDetector detector;
    int mode = 0;
    float lastX = 0;
    float lastY = 0;
    float downX = 0;
    float downY = 0;
    long downT = 0;
    boolean ready = false;

    ZoomHandler(final int zid, final ImageView ziv) {
      this.zid = zid;
      this.ziv = ziv;
      detector = new ScaleGestureDetector(context,
          new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            public boolean onScale(ScaleGestureDetector d) {
              if (!ready) {
                return true;
              }
              float f = d.getScaleFactor();
              float ns = scale * f;
              if (ns < 1.0f) {
                f = 1.0f / scale;
              } else if (ns > maxScale) {
                f = maxScale / scale;
              }
              scale = scale * f;
              matrix.postScale(f, f, d.getFocusX(), d.getFocusY());
              clamp();
              ziv.setImageMatrix(matrix);
              return true;
            }
            public void onScaleEnd(ScaleGestureDetector d) {
              fireZoom();
            }
          });
    }

    void start() {
      ziv.setScaleType(ImageView.ScaleType.MATRIX);
      ziv.post(new Runnable() {
        public void run() {
          try {
            if (ziv.getDrawable() == null) {
              return;
            }
            int dw = ziv.getDrawable().getIntrinsicWidth();
            int dh = ziv.getDrawable().getIntrinsicHeight();
            int vw = ziv.getWidth();
            int vh = ziv.getHeight();
            if (dw <= 0 || dh <= 0 || vw <= 0 || vh <= 0) {
              return;
            }
            float s = Math.min((float) vw / dw, (float) vh / dh);
            base.setScale(s, s);
            base.postTranslate((vw - dw * s) / 2.0f, (vh - dh * s) / 2.0f);
            matrix.set(base);
            ziv.setImageMatrix(matrix);
            ready = true;
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    }

    void reset() {
      try {
        matrix.set(base);
        ziv.setImageMatrix(matrix);
        scale = 1.0f;
        lastFired = 1.0f;
      } catch (Exception e) {
        // abaikan
      }
    }

    public boolean onTouch(View v, MotionEvent e) {
      if (!ready) {
        return false;
      }
      try {
        detector.onTouchEvent(e);
      } catch (Exception ex) {
        // abaikan
      }
      int a = e.getActionMasked();
      if (a == MotionEvent.ACTION_DOWN) {
        mode = 1;
        lastX = e.getX();
        lastY = e.getY();
        downX = lastX;
        downY = lastY;
        downT = System.currentTimeMillis();
      } else if (a == MotionEvent.ACTION_MOVE) {
        if (mode == 1 && !detector.isInProgress() && scale > 1.01f
            && e.getPointerCount() == 1) {
          float dx = e.getX() - lastX;
          float dy = e.getY() - lastY;
          lastX = e.getX();
          lastY = e.getY();
          matrix.postTranslate(dx, dy);
          clamp();
          ziv.setImageMatrix(matrix);
        }
      } else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
        mode = 0;
        fireZoom();
        try {
          float dist = Math.abs(e.getX() - downX) + Math.abs(e.getY() - downY);
          long dt = System.currentTimeMillis() - downT;
          if (dist < 24 && dt < 400 && !detector.isInProgress()
              && scale <= 1.01f) {
            return false;
          }
        } catch (Exception ex) {
          // abaikan
        }
      } else if (a == MotionEvent.ACTION_POINTER_UP) {
        mode = 0;
        lastX = e.getX();
        lastY = e.getY();
      }
      return true;
    }

    void clamp() {
      try {
        if (ziv.getDrawable() == null) {
          return;
        }
        RectF r = new RectF(0, 0,
            ziv.getDrawable().getIntrinsicWidth(),
            ziv.getDrawable().getIntrinsicHeight());
        matrix.mapRect(r);
        float vw = ziv.getWidth();
        float vh = ziv.getHeight();
        float dx = 0;
        float dy = 0;
        if (r.width() <= vw) {
          dx = (vw - r.width()) / 2.0f - r.left;
        } else {
          if (r.left > 0) {
            dx = -r.left;
          } else if (r.right < vw) {
            dx = vw - r.right;
          }
        }
        if (r.height() <= vh) {
          dy = (vh - r.height()) / 2.0f - r.top;
        } else {
          if (r.top > 0) {
            dy = -r.top;
          } else if (r.bottom < vh) {
            dy = vh - r.bottom;
          }
        }
        matrix.postTranslate(dx, dy);
      } catch (Exception e) {
        // abaikan
      }
    }

    void fireZoom() {
      if (Math.abs(scale - lastFired) > 0.005f) {
        lastFired = scale;
        ZoomChanged(zid, scale);
      }
    }
  }

  @SimpleFunction(description = "SET: Aktif/matikan cubit-zoom (pinch) + geser "
      + "gambar ber-id (mode cubit-geser; tap biasa tetap dihitung klik). "
      + "Perubahan skala menembak ZoomChanged; baca kapan saja "
      + "via GetZoom. Sukses diam; id tidak ada via ErrorOccurred.")
  public void SetZoomEnabled(final int id, final boolean enabled) {
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          ImageView iv;
          synchronized (viewLock) {
            iv = views.get(Integer.valueOf(id));
          }
          if (iv == null) {
            Shared.err(Tools_Image.this, "Image", id, "id gambar tidak ada");
            return;
          }
          try {
            if (enabled) {
              ZoomHandler h;
              synchronized (viewLock) {
                h = zoomHandlers.get(Integer.valueOf(id));
                if (h == null) {
                  h = new ZoomHandler(id, iv);
                  zoomHandlers.put(Integer.valueOf(id), h);
                }
              }
              iv.setOnTouchListener(h);
              h.start();
            } else {
              synchronized (viewLock) {
                zoomHandlers.remove(Integer.valueOf(id));
              }
              iv.setOnTouchListener(null);
              iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            }
          } catch (Exception e) {
            String m = e.getMessage();
            Shared.err(Tools_Image.this, "Image", id,
                m == null ? "zoom gagal" : m);
          }
        }
      });
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Image", id, m == null ? "zoom gagal" : m);
    }
  }

  @SimpleFunction(description = "Fallback polling untuk event ZoomChanged: "
      + "skala zoom gambar ber-id (1.0 = pas).")
  public float GetZoom(int id) {
    synchronized (viewLock) {
      ZoomHandler h = zoomHandlers.get(Integer.valueOf(id));
      return h == null ? 1.0f : h.scale;
    }
  }

  @SimpleFunction(description = "SET: kembalikan zoom gambar ber-id ke pas (1.0). Tanpa feedback.")
  public void SetResetZoom(final int id) {
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          ZoomHandler h;
          synchronized (viewLock) {
            h = zoomHandlers.get(Integer.valueOf(id));
          }
          if (h == null) {
            Shared.err(Tools_Image.this, "Image", id, "zoom belum aktif");
            return;
          }
          try {
            h.reset();
          } catch (Exception e) {
            String m = e.getMessage();
            Shared.err(Tools_Image.this, "Image", id,
                m == null ? "reset gagal" : m);
          }
        }
      });
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Image", id, m == null ? "reset gagal" : m);
    }
  }

  // ================= UKURAN + TAMPIL (angka MIT: -2 isi, -1 otomatis) =================

  private ImageView imageOf(int id) {
    synchronized (viewLock) {
      return views.get(Integer.valueOf(id));
    }
  }

  /** MIT -> android: -2 isi penuh, -1 otomatis, positif pixel. Negatif lain
   *  (persen) tak didukung view mentah -> otomatis. */
  private int mitToAndroidSize(int s) {
    if (s == -2) {
      return ViewGroup.LayoutParams.MATCH_PARENT;
    }
    if (s == -1 || s <= -1000) {
      return ViewGroup.LayoutParams.WRAP_CONTENT;
    }
    if (s > 0) {
      return s;
    }
    return ViewGroup.LayoutParams.WRAP_CONTENT;
  }

  private int androidToMitSize(int s) {
    if (s == ViewGroup.LayoutParams.MATCH_PARENT) {
      return -2;
    }
    if (s == ViewGroup.LayoutParams.WRAP_CONTENT) {
      return -1;
    }
    return s;
  }

  @SimpleFunction(description = "SET: atur lebar + tinggi gambar ber-id. Angka "
      + "SAMA seperti blok bawaan MIT: -2 = isi penuh, -1 = otomatis, positif "
      + "= pixel. Persen tidak didukung blok ini; resep setengah layar: bungkus "
      + "dalam 2 Arrangement sejajar (masing-masing -2 = terbagi rata), gambar di "
      + "dalamnya -2. Sukses diam; id tidak ada via ErrorOccurred.")
  public void SetImageSize(final int id, final int width, final int height) {
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            ImageView iv = imageOf(id);
            if (iv == null) {
              Shared.err(Tools_Image.this, "Image", id, "id gambar tidak ada");
              return;
            }
            ViewGroup.LayoutParams lp = iv.getLayoutParams();
            if (lp == null) {
              lp = new ViewGroup.LayoutParams(
                  ViewGroup.LayoutParams.MATCH_PARENT,
                  ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            lp.width = mitToAndroidSize(width);
            lp.height = mitToAndroidSize(height);
            iv.setLayoutParams(lp);
          } catch (Exception e) {
            // Set = tanpa feedback
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "SET: tampil/sembunyi gambar ber-id. "
      + "false = GONE (hilang total). Sukses diam; id tidak ada via ErrorOccurred.")
  public void SetImageVisible(final int id, final boolean visible) {
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            ImageView iv = imageOf(id);
            if (iv == null) {
              Shared.err(Tools_Image.this, "Image", id, "id gambar tidak ada");
              return;
            }
            iv.setVisibility(visible ? View.VISIBLE : View.GONE);
          } catch (Exception e) {
            String m = e.getMessage();
            Shared.err(Tools_Image.this, "Image", id,
                m == null ? "visible gagal" : m);
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "SET: mode pas gambar ber-id. fit true = "
      + "seluruh gambar muat di dalam (tidak kepotong); false = penuhi "
      + "tampilan (bisa kepotong). Mematikan zoom bila sedang aktif. "
      + "Sukses diam; id tidak ada via ErrorOccurred.")
  public void SetImageFit(final int id, final boolean fit) {
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            ImageView iv = imageOf(id);
            if (iv == null) {
              Shared.err(Tools_Image.this, "Image", id, "id gambar tidak ada");
              return;
            }
            synchronized (viewLock) {
              zoomHandlers.remove(Integer.valueOf(id));
            }
            iv.setOnTouchListener(null);
            iv.setScaleType(fit ? ImageView.ScaleType.FIT_CENTER
                : ImageView.ScaleType.CENTER_CROP);
            iv.setAdjustViewBounds(fit);
          } catch (Exception e) {
            String m = e.getMessage();
            Shared.err(Tools_Image.this, "Image", id,
                m == null ? "fit gagal" : m);
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "SET: ganti gambar ber-id yang SUDAH tampil "
      + "dengan sumber baru. SATU pintu input sama seperti CallShowImage: "
      + "path file/asset, file:/// absolut, http(s) URL (di-cache otomatis), "
      + "base64 (mentah / data:...), atau content:// — terdeteksi otomatis. "
      + "View, ukuran & mode fit TETAP (tidak dibuat ulang); zoom bila aktif "
      + "di-reset ke pas. Sukses menembak ImageShown; gagal via ErrorOccurred "
      + "(gambar lama tetap tampil).")
  public void SetImageSource(final int id, final String source) {
    ImageView cur;
    synchronized (viewLock) {
      cur = views.get(Integer.valueOf(id));
    }
    if (cur == null) {
      Shared.err(this, "Image", id,
          "id gambar tidak ada (tampilkan dulu via CallShowImage)");
      return;
    }
    final String src = Shared.clean(source == null ? "" : source);
    if (src.length() == 0) {
      Shared.err(this, "Image", id, "gambar kosong");
      return;
    }
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        byte[] data = null;
        Bitmap bmp = null;
        try {
          data = loadRawBytes(src);
          if (data != null) {
            bmp = boundedDecode(data);
          }
        } catch (Exception e) {
          bmp = null;
        }
        if (bmp == null) {
          Shared.err(Tools_Image.this, "Image", id,
              "gambar tidak terbaca (path/URL/base64/content tidak valid)");
          return;
        }
        String disp = "";
        try {
          if (src.regionMatches(true, 0, "http", 0, 4)
              || src.startsWith("data:") || looksBase64(src)
              || src.regionMatches(true, 0, "content://", 0, 10)) {
            File cache = context.getCacheDir();
            if (!cache.exists()) {
              cache.mkdirs();
            }
            File dst = new File(cache, "img_" + id + "_set.png");
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
            disp = dst.getAbsolutePath();
          } else if (Shared.isAssetPath(src)) {
            disp = src;
          } else {
            File f = Shared.resolveRooted(src);
            disp = (f == null) ? src : f.getAbsolutePath();
          }
        } catch (Exception e) {
          disp = src;
        }
        final Bitmap fBmp = bmp;
        final String fDisp = disp;
        try {
          form.runOnUiThread(new Runnable() {
            public void run() {
              try {
                ImageView iv = imageOf(id);
                if (iv == null) {
                  Shared.err(Tools_Image.this, "Image", id,
                      "id gambar tidak ada (tampilkan dulu via CallShowImage)");
                  return;
                }
                iv.setImageBitmap(fBmp);
                synchronized (viewLock) {
                  ZoomHandler h = zoomHandlers.get(Integer.valueOf(id));
                  if (h != null) {
                    try {
                      h.scale = 1.0f;
                      h.lastFired = 1.0f;
                      h.ready = false;
                      h.start();
                    } catch (Exception e) {
                      // abaikan
                    }
                  }
                }
                ImageShown(id, fDisp);
              } catch (Exception e) {
                String m = e.getMessage();
                Shared.err(Tools_Image.this, "Image", id,
                    m == null ? "ganti gambar gagal" : m);
              }
            }
          });
        } catch (Exception e) {
          String m = e.getMessage();
          Shared.err(Tools_Image.this, "Image", id,
              m == null ? "ganti gambar gagal" : m);
        }
      }
    });
  }

  @SimpleFunction(description = "Lebar gambar ber-id (angka MIT: -2 isi, "
      + "-1 otomatis, positif pixel). -3 bila id tidak ada. PENERJEMAH SATUAN: "
      + "untuk dipasang ke SetImageSize pakai blok ini (satuan MIT); GetImageSize "
      + "(pixel) hanya untuk hitung-hitungan.")
  public int GetImageWidth(int id) {
    try {
      ImageView iv = imageOf(id);
      if (iv == null) {
        return -3;
      }
      ViewGroup.LayoutParams lp = iv.getLayoutParams();
      if (lp == null) {
        return -3;
      }
      return androidToMitSize(lp.width);
    } catch (Exception e) {
      return -3;
    }
  }

  @SimpleFunction(description = "Tinggi gambar ber-id (angka MIT). "
      + "-3 bila id tidak ada. Lihat penerjemah satuan di GetImageWidth.")
  public int GetImageHeight(int id) {
    try {
      ImageView iv = imageOf(id);
      if (iv == null) {
        return -3;
      }
      ViewGroup.LayoutParams lp = iv.getLayoutParams();
      if (lp == null) {
        return -3;
      }
      return androidToMitSize(lp.height);
    } catch (Exception e) {
      return -3;
    }
  }

  @SimpleFunction(description = "True bila gambar ber-id sedang tampil "
      + "(visible). False bila tidak ada / sedang GONE.")
  public boolean IsImageVisible(int id) {
    try {
      ImageView iv = imageOf(id);
      return iv != null && iv.getVisibility() == View.VISIBLE;
    } catch (Exception e) {
      return false;
    }
  }

  // ================= SLIDER =================

  /** Satu pintu baca byte gambar: http(s) URL, base64 (mentah / data:...),
   *  content://, asset, atau path file. null bila gagal. */
  private byte[] loadRawBytes(String s) {
    if (s == null || s.length() == 0) {
      return null;
    }
    try {
      if (s.regionMatches(true, 0, "http", 0, 4)) {
        byte[] hit = readUrlCache(s);
        if (hit != null) {
          return hit;
        }
        HttpURLConnection c = (HttpURLConnection) new java.net.URL(s)
            .openConnection();
        c.setConnectTimeout(20000);
        c.setReadTimeout(20000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", "Mozilla/5.0");
        int code = c.getResponseCode();
        if (code >= 400) {
          try {
            c.disconnect();
          } catch (Exception e) {
            // abaikan
          }
          return null;
        }
        InputStream in = c.getInputStream();
        try {
          byte[] data = Shared.readAll(in);
          if (data != null) {
            writeUrlCache(s, data);
          }
          return data;
        } finally {
          try {
            in.close();
          } catch (Exception e) {
            // abaikan
          }
          try {
            c.disconnect();
          } catch (Exception e) {
            // abaikan
          }
        }
      }
      if (s.startsWith("data:") || looksBase64(s)) {
        String b64 = s;
        int comma = s.indexOf(',');
        if (s.startsWith("data:") && comma >= 0) {
          b64 = s.substring(comma + 1);
        }
        return Base64.decode(b64.trim(), Base64.DEFAULT);
      }
      if (s.regionMatches(true, 0, "content://", 0, 10)) {
        InputStream in = null;
        try {
          in = context.getContentResolver().openInputStream(
              android.net.Uri.parse(s));
          if (in == null) {
            return null;
          }
          return Shared.readAll(in);
        } catch (Exception e) {
          return null;
        } finally {
          try {
            if (in != null) {
              in.close();
            }
          } catch (Exception e) {
            // abaikan
          }
        }
      }
      if (Shared.isAssetPath(s)) {
        InputStream in = context.getAssets().open(Shared.assetName(s));
        try {
          return Shared.readAll(in);
        } finally {
          try {
            in.close();
          } catch (Exception e) {
            // abaikan
          }
        }
      }
      File f = Shared.resolveRooted(s);
      if (f == null || !f.exists()) {
        return null;
      }
      FileInputStream fin = new FileInputStream(f);
      try {
        return Shared.readAll(fin);
      } finally {
        try {
          fin.close();
        } catch (Exception e) {
          // abaikan
        }
      }
    } catch (Exception e) {
      return null;
    }
  }

  private Bitmap loadBitmap(String src, String fallbackPath) {
    try {
      String s = Shared.clean(src == null ? "" : src);
      String usePath = s.length() > 0 ? s : Shared.clean(fallbackPath);
      if (s.regionMatches(true, 0, "http", 0, 4)) {
        byte[] hit = readUrlCache(s);
        if (hit != null) {
          return boundedDecode(hit);
        }
        HttpURLConnection c = (HttpURLConnection) new java.net.URL(s)
            .openConnection();
        c.setConnectTimeout(20000);
        c.setReadTimeout(20000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", "Mozilla/5.0");
        int code = c.getResponseCode();
        if (code >= 400) {
          try {
            c.disconnect();
          } catch (Exception e) {
            // abaikan
          }
          return null;
        }
        InputStream in = c.getInputStream();
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
        try {
          c.disconnect();
        } catch (Exception e) {
          // abaikan
        }
        if (data != null) {
          writeUrlCache(s, data);
        }
        return boundedDecode(data);
      }
      if (s.startsWith("data:") || looksBase64(s)) {
        String b64 = s;
        int comma = s.indexOf(',');
        if (s.startsWith("data:") && comma >= 0) {
          b64 = s.substring(comma + 1);
        }
        return boundedDecode(Base64.decode(b64.trim(), Base64.DEFAULT));
      }
      if (s.regionMatches(true, 0, "content://", 0, 10)) {
        byte[] raw = loadRawBytes(s);
        return raw == null ? null : boundedDecode(raw);
      }
      String p = usePath;
      if (p.length() == 0) {
        return null;
      }
      byte[] data = null;
      if (Shared.isAssetPath(p)) {
        InputStream in = context.getAssets().open(Shared.assetName(p));
        try {
          data = Shared.readAll(in);
        } finally {
          try {
            in.close();
          } catch (Exception e) {
            // abaikan
          }
        }
      } else {
        File f = Shared.resolveRooted(p);
        if (f == null || !f.exists()) {
          return null;
        }
        FileInputStream fin = new FileInputStream(f);
        try {
          data = Shared.readAll(fin);
        } finally {
          try {
            fin.close();
          } catch (Exception e) {
            // abaikan
          }
        }
      }
      return data == null ? null : boundedDecode(data);
    } catch (Exception e) {
      return null;
    }
  }

  private List<String> toStrList(Object o) {
    ArrayList<String> out = new ArrayList<String>();
    try {
      if (o instanceof YailList) {
        for (Object x : ((YailList) o).toArray()) {
          if (x != null) {
            String s = x.toString();
            if (s.length() > 0) {
              out.add(s);
            }
          }
        }
      } else if (o instanceof List) {
        List<?> l = (List<?>) o;
        for (int i = 0; i < l.size(); i = i + 1) {
          Object x = l.get(i);
          if (x != null) {
            out.add(x.toString());
          }
        }
      } else if (o != null) {
        String s = o.toString();
        if (s.length() > 0) {
          out.add(s);
        }
      }
    } catch (Exception e) {
      // abaikan
    }
    return out;
  }

  @SimpleFunction(description = "CALL: bikin slider gambar ber-id (geser kanan-kiri "
      + "bila horizontal=true, atas-bawah bila false). paths = list gambar langsung "
      + "(path file/asset, URL http, atau base64; boleh list kosong = isi belakangan "
      + "via SetSliderImages). TANPA batas jumlah (dulu maks 20); tiap gambar "
      + "dikecilkan otomatis bila > 2048px, tapi puluhan gambar besar tetap makan "
      + "RAM — sesuaikan. URL diunduh sekali lalu di-cache (tak perlu unduh ulang; "
      + "bersihkan via SetClearImageCache). INGAT: halaman slider mulai dari 0 "
      + "(bukan 1 seperti daftar id). Geser menembak "
      + "SliderChanged; baca kapan saja via GetSliderIndex.")
  public void CallCreateImageSlider(final AndroidViewComponent container,
      final int id, final boolean horizontal, final Object paths) {
    if (id < 1) {
      Shared.err(this, "Slider", id, "id harus >= 1 (pakai CreateSliderId)");
      return;
    }
    if (!Ids.register(Ids.SLIDER, id)) {
      Shared.err(this, "Slider", id, "ID " + id + " sedang dipakai slider lain");
      return;
    }
    final List<String> init = toStrList(paths);
    final List<String> first = new ArrayList<String>(init);
    synchronized (viewLock) {
      if (sliders.containsKey(Integer.valueOf(id))) {
        Shared.err(this, "Slider", id, "ID " + id + " masih ada "
            + "(Remove dulu)");
        return;
      }
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            ViewGroup vg = groupOf(container);
            if (vg == null) {
              Ids.setActive(Ids.SLIDER, id, false);
              Ids.unregister(Ids.SLIDER, id);
              Shared.err(Tools_Image.this, "Slider", id, "layout tidak valid");
              return;
            }
            final ViewGroup root;
            final LinearLayout box = new LinearLayout(activity);
            if (horizontal) {
              HorizontalScrollView hsv = new HorizontalScrollView(activity);
              hsv.setHorizontalScrollBarEnabled(false);
              hsv.setFillViewport(true);
              box.setOrientation(LinearLayout.HORIZONTAL);
              root = hsv;
            } else {
              ScrollView sv = new ScrollView(activity);
              sv.setVerticalScrollBarEnabled(false);
              sv.setFillViewport(true);
              box.setOrientation(LinearLayout.VERTICAL);
              root = sv;
            }
            root.addView(box, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
            vg.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
            root.setOnTouchListener(new View.OnTouchListener() {
              public boolean onTouch(View vv, MotionEvent e) {
                int a = e.getActionMasked();
                if (a == MotionEvent.ACTION_UP
                    || a == MotionEvent.ACTION_CANCEL) {
                  root.post(new Runnable() {
                    public void run() {
                      snapSlider(id);
                    }
                  });
                }
                return false;
              }
            });
            Slider s = new Slider();
            s.root = root;
            s.box = box;
            s.horizontal = horizontal;
            synchronized (viewLock) {
              sliders.put(Integer.valueOf(id), s);
            }
            Ids.setActive(Ids.SLIDER, id, true);
            SliderCreated(id, horizontal);
            if (!first.isEmpty()) {
              fillSliderImages(id, first);
            }
          } catch (Exception e) {
            Ids.setActive(Ids.SLIDER, id, false);
            Ids.unregister(Ids.SLIDER, id);
            String m = e.getMessage();
            Shared.err(Tools_Image.this, "Slider", id,
                m == null ? "slider gagal dibuat" : m);
          }
        }
      });
    } catch (Exception e) {
      Ids.setActive(Ids.SLIDER, id, false);
      Ids.unregister(Ids.SLIDER, id);
      String m = e.getMessage();
      Shared.err(this, "Slider", id, m == null ? "slider gagal dibuat" : m);
    }
  }

  private void snapSlider(int id) {
    Slider s;
    synchronized (viewLock) {
      s = sliders.get(Integer.valueOf(id));
    }
    if (s == null || s.box.getChildCount() == 0) {
      return;
    }
    try {
      int target = s.index;
      if (s.horizontal && s.root instanceof HorizontalScrollView) {
        int pageW = s.root.getWidth();
        if (pageW <= 0 && s.box.getChildCount() > 0) {
          pageW = s.box.getChildAt(0).getWidth();
        }
        if (pageW > 0) {
          int sx = ((HorizontalScrollView) s.root).getScrollX();
          target = Math.round((float) sx / pageW);
        }
      } else if (!s.horizontal && s.root instanceof ScrollView) {
        View first = s.box.getChildCount() > 0 ? s.box.getChildAt(s.index < s.box
            .getChildCount() ? s.index : 0) : null;
        int pageH = first == null ? 0 : first.getHeight();
        if (pageH > 0) {
          int sy = ((ScrollView) s.root).getScrollY();
          target = Math.round((float) sy / pageH);
        }
      }
      if (target < 0) {
        target = 0;
      }
      if (target > s.pages.size() - 1) {
        target = s.pages.size() - 1;
      }
      goToLocked(s, id, target, true);
    } catch (Exception e) {
      // abaikan
    }
  }

  private void goToLocked(Slider s, int id, int target, boolean fire) {
    try {
      if (target < 0) {
        target = 0;
      }
      if (target > s.pages.size() - 1) {
        target = s.pages.size() - 1;
      }
      if (s.horizontal && s.root instanceof HorizontalScrollView) {
        int pageW = s.root.getWidth();
        if (pageW <= 0 && s.box.getChildCount() > 0) {
          pageW = s.box.getChildAt(0).getWidth();
        }
        if (pageW > 0) {
          ((HorizontalScrollView) s.root).smoothScrollTo(target * pageW, 0);
        }
      } else if (!s.horizontal && s.root instanceof ScrollView) {
        int y = 0;
        for (int i = 0; i < target && i < s.box.getChildCount(); i = i + 1) {
          View ch = s.box.getChildAt(i);
          if (ch != null) {
            y = y + ch.getHeight();
          }
        }
        ((ScrollView) s.root).smoothScrollTo(0, y);
      }
      if (fire && target != s.index) {
        s.index = target;
        SliderChanged(id, target);
      } else {
        s.index = target;
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "SET: isi slider ber-id dengan daftar gambar "
      + "(list teks: path file/asset, URL http, atau base64 — sama fleksibelnya "
      + "dengan ShowImage; path dipakai bila item kosong). TANPA batas jumlah. "
      + "URL diunduh sekali lalu di-cache (tak perlu unduh ulang). "
      + "List kosong / slider tidak ada = ErrorOccurred. Gagal via ErrorOccurred.")
  public void SetSliderImages(final int sliderId,
      final Object paths) {
    final List<String> list = toStrList(paths);
    if (list.isEmpty()) {
      Shared.err(this, "Slider", sliderId, "list gambar kosong");
      return;
    }
    Slider s;
    synchronized (viewLock) {
      s = sliders.get(Integer.valueOf(sliderId));
    }
    if (s == null) {
      Shared.err(this, "Slider", sliderId, "slider tidak ada");
      return;
    }
    final List<String> use = new ArrayList<String>(list);
    fillSliderImages(sliderId, use);
  }

  private void fillSliderImages(final int sliderId, final List<String> use) {
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        final ArrayList<Bitmap> bmps = new ArrayList<Bitmap>();
        int fail = 0;
        for (int i = 0; i < use.size(); i = i + 1) {
          Bitmap b = loadBitmap(use.get(i), "");
          if (b == null) {
            fail = fail + 1;
          } else {
            bmps.add(b);
          }
        }
        final int fFail = fail;
        try {
          form.runOnUiThread(new Runnable() {
            public void run() {
              try {
                Slider sl;
                synchronized (viewLock) {
                  sl = sliders.get(Integer.valueOf(sliderId));
                }
                if (sl == null) {
                  return;
                }
                sl.box.removeAllViews();
                sl.pages.clear();
                sl.index = 0;
                for (int i = 0; i < bmps.size(); i = i + 1) {
                  final int idx = i;
                  ImageView iv = new ImageView(activity);
                  iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                  iv.setAdjustViewBounds(true);
                  iv.setImageBitmap(bmps.get(i));
                  iv.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View vv) {
                      SliderClicked(sliderId, idx);
                    }
                  });
                  LinearLayout.LayoutParams lp;
                  if (sl.horizontal) {
                    lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                  } else {
                    lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                  }
                  sl.box.addView(iv, lp);
                  sl.pages.add(iv);
                }
                if (sl.horizontal && sl.root instanceof HorizontalScrollView) {
                  ((HorizontalScrollView) sl.root).scrollTo(0, 0);
                } else if (sl.root instanceof ScrollView) {
                  ((ScrollView) sl.root).scrollTo(0, 0);
                }
                if (fFail > 0) {
                  Shared.err(Tools_Image.this, "Slider", sliderId, fFail
                      + " gambar gagal dimuat");
                }
              } catch (Exception e) {
                String m = e.getMessage();
                Shared.err(Tools_Image.this, "Slider", sliderId,
                    m == null ? "isi slider gagal" : m);
              }
            }
          });
        } catch (Exception e) {
          String m = e.getMessage();
          Shared.err(Tools_Image.this, "Slider", sliderId,
              m == null ? "isi slider gagal" : m);
        }
      }
    });
  }

  @SimpleFunction(description = "SET: geser slider ke halaman berikut (berputar). "
      + "Memicu SliderChanged bila halaman berubah (bukan tanpa feedback).")
  public void SetSliderNext(final int sliderId) {
    slideTo(sliderId, 1, false);
  }

  @SimpleFunction(description = "SET: geser slider ke halaman sebelum (berputar). "
      + "Memicu SliderChanged bila halaman berubah (bukan tanpa feedback).")
  public void SetSliderPrevious(final int sliderId) {
    slideTo(sliderId, -1, false);
  }

  @SimpleFunction(description = "SET: lompat ke halaman index (INGAT: 0 = pertama, "
      + "bukan 1). Memicu SliderChanged bila halaman berubah.")
  public void SetSliderGoTo(final int sliderId, final int index) {
    Slider s;
    synchronized (viewLock) {
      s = sliders.get(Integer.valueOf(sliderId));
    }
    if (s == null) {
      Shared.err(this, "Slider", sliderId, "slider tidak ada");
      return;
    }
    final Slider fS = s;
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          goToLocked(fS, sliderId, index, true);
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  private void slideTo(final int sliderId, final int delta, final boolean silent) {
    Slider s;
    synchronized (viewLock) {
      s = sliders.get(Integer.valueOf(sliderId));
    }
    if (s == null || s.pages.isEmpty()) {
      return;
    }
    final Slider fS = s;
    final int fId = sliderId;
    final int target = (s.index + delta + s.pages.size()) % s.pages.size();
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          goToLocked(fS, fId, target, !silent);
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "SET: Auto-putar slider tiap intervalMs "
      + "(0 = mati). Tiap putaran memicu SliderChanged.")
  public void SetSliderAutoPlay(final int sliderId, final long intervalMs) {
    Slider s;
    synchronized (viewLock) {
      s = sliders.get(Integer.valueOf(sliderId));
    }
    if (s == null) {
      Shared.err(this, "Slider", sliderId, "slider tidak ada");
      return;
    }
    final Slider fS = s;
    final int fId = sliderId;
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            if (fS.auto != null && fS.autoTask != null) {
              fS.auto.removeCallbacks(fS.autoTask);
              fS.autoTask = null;
            }
            fS.interval = intervalMs;
            if (intervalMs > 0) {
              if (fS.auto == null) {
                fS.auto = new Handler();
              }
              fS.autoTask = new Runnable() {
                public void run() {
                  try {
                    Slider cur;
                    synchronized (viewLock) {
                      cur = sliders.get(Integer.valueOf(fId));
                    }
                    if (cur == null || cur != fS) {
                      return;
                    }
                    if (!cur.pages.isEmpty()) {
                      int t = (cur.index + 1) % cur.pages.size();
                      goToLocked(cur, fId, t, true);
                    }
                    if (fS.autoTask != null && fS.interval > 0) {
                      fS.auto.postDelayed(fS.autoTask, fS.interval);
                    }
                  } catch (Exception e) {
                    // abaikan
                  }
                }
              };
              fS.auto.postDelayed(fS.autoTask, intervalMs);
            }
          } catch (Exception e) {
            String m = e.getMessage();
            Shared.err(Tools_Image.this, "Slider", fId,
                m == null ? "autoplay gagal" : m);
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "Fallback polling untuk event SliderChanged: "
      + "index halaman aktif (INGAT: 0 = pertama, bukan 1). -1 bila slider tidak ada.")
  public int GetSliderIndex(int sliderId) {
    synchronized (viewLock) {
      Slider s = sliders.get(Integer.valueOf(sliderId));
      return s == null ? -1 : s.index;
    }
  }

  @SimpleFunction(description = "Jumlah halaman slider ber-id (0 bila slider tidak ada).")
  public int GetSliderCount(int sliderId) {
    synchronized (viewLock) {
      Slider s = sliders.get(Integer.valueOf(sliderId));
      return s == null ? 0 : s.pages.size();
    }
  }

  @SimpleFunction(description = "True bila autoplay slider ber-id sedang jalan "
      + "(interval > 0 dan belum dimatikan).")
  public boolean IsSliderAutoPlay(int sliderId) {
    synchronized (viewLock) {
      Slider s = sliders.get(Integer.valueOf(sliderId));
      return s != null && s.autoTask != null && s.interval > 0;
    }
  }

  @SimpleFunction(description = "Interval autoplay slider ber-id (milidetik; "
      + "0 = mati / slider tidak ada).")
  public long GetSliderInterval(int sliderId) {
    synchronized (viewLock) {
      Slider s = sliders.get(Integer.valueOf(sliderId));
      return s == null ? 0L : s.interval;
    }
  }

  /** Lepas satu slider + lepas id. Dipakai komponen + Base. */
  public static void clearSliderStatic(int id) {
    Slider s = null;
    synchronized (viewLock) {
      s = sliders.remove(Integer.valueOf(id));
    }
    if (s != null) {
      try {
        if (s.auto != null && s.autoTask != null) {
          s.auto.removeCallbacks(s.autoTask);
        }
      } catch (Exception e) {
        // abaikan
      }
      s.autoTask = null;
      try {
        if (s.root != null && s.root.getParent() instanceof ViewGroup) {
          ((ViewGroup) s.root.getParent()).removeView(s.root);
        }
      } catch (Exception e) {
        // abaikan
      }
    }
    Ids.setActive(Ids.SLIDER, id, false);
    Ids.unregister(Ids.SLIDER, id);
  }

  // ================= ID =================

  @SimpleFunction(description = "Buat id gambar unik baru (Number, siap pakai ke CallShowImage).")
  public int CreateImageId() {
    int id = Ids.createFor(Ids.IMAGE);
    if (id < 1) {
      Shared.err(this, "Image", 0, "gagal buat id");
    }
    return id;
  }

  @SimpleFunction(description = "Semua id gambar yang tampil (URUT PEMBUATAN, "
      + "index 1 = pertama dibuat). Beda dengan GetActiveImageIds: yang ini HANYA "
      + "yang sudah tampil; Aktif = termasuk yang masih loading.")
  public List<Integer> GetImageIds() {
    return Ids.idsOf(Ids.IMAGE);
  }

  @SimpleFunction(description = "Jumlah gambar ber-id yang sedang tampil "
      + "(0 bila tidak ada).")
  public int GetImageCount() {
    synchronized (viewLock) {
      return views.size();
    }
  }

  @SimpleFunction(description = "Id gambar yang sedang tampil/loading (URUT PEMBUATAN). "
      + "Beda dengan GetImageIds: yang ini termasuk yang masih loading; Ids = "
      + "hanya yang sudah tampil.")
  public List<Integer> GetActiveImageIds() {
    return Ids.activeIdsOf(Ids.IMAGE);
  }

  @SimpleFunction(description = "Id gambar yang terakhir dibuat/di-start. -1 bila kosong.")
  public int CurrentImageId() {
    return Ids.currentOf(Ids.IMAGE);
  }

  @SimpleFunction(description = "Id index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstImageId() {
    return Ids.firstOf(Ids.IMAGE);
  }

  @SimpleFunction(description = "Id TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastImageId() {
    return Ids.lastOf(Ids.IMAGE);
  }

  @SimpleFunction(description = "Id yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.")
  public int GetPreviousImageId(int id) {
    return Ids.previousOf(Ids.IMAGE, id);
  }

  @SimpleFunction(description = "True bila id gambar ini sedang tampil/loading.")
  public boolean IsImageIdActive(int id) {
    return Ids.isActive(Ids.IMAGE, id);
  }

  @SimpleFunction(description = "Hapus id gambar: lepas gambar dari layout + lepas id "
      + "(otomatis beres-beres).")
  public void RemoveImageId(int id) {
    clearStatic(id);
  }

  @SimpleFunction(description = "Buat id slider unik baru (Number, siap pakai "
      + "ke CallCreateImageSlider).")
  public int CreateSliderId() {
    int id = Ids.createFor(Ids.SLIDER);
    if (id < 1) {
      Shared.err(this, "Slider", 0, "gagal buat id");
    }
    return id;
  }

  @SimpleFunction(description = "Semua id slider (URUT PEMBUATAN, "
      + "index 1 = pertama dibuat).")
  public List<Integer> GetSliderIds() {
    return Ids.idsOf(Ids.SLIDER);
  }

  @SimpleFunction(description = "Id slider yang aktif (URUT PEMBUATAN).")
  public List<Integer> GetActiveSliderIds() {
    return Ids.activeIdsOf(Ids.SLIDER);
  }

  @SimpleFunction(description = "Id slider terakhir dibuat. -1 bila kosong.")
  public int CurrentSliderId() {
    return Ids.currentOf(Ids.SLIDER);
  }

  @SimpleFunction(description = "Id index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstSliderId() {
    return Ids.firstOf(Ids.SLIDER);
  }

  @SimpleFunction(description = "Id TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastSliderId() {
    return Ids.lastOf(Ids.SLIDER);
  }

  @SimpleFunction(description = "Id yang dibuat TEPAT SEBELUM id ini. "
      + "-1 bila tidak ada.")
  public int GetPreviousSliderId(int id) {
    return Ids.previousOf(Ids.SLIDER, id);
  }

  @SimpleFunction(description = "True bila id slider ini masih ada.")
  public boolean IsSliderIdActive(int id) {
    return Ids.isActive(Ids.SLIDER, id);
  }

  @SimpleFunction(description = "Hapus slider: lepas dari layout + lepas id "
      + "(otomatis beres-beres, autoplay mati).")
  public void RemoveSliderId(int sliderId) {
    clearSliderStatic(sliderId);
  }

  @SimpleFunction(description = "Fallback polling untuk event ErrorOccurred: "
      + "pesan error terakhir operasi ini (colok OpImage/OpSlider/OpBase64; "
      + "kosong bila belum ada).")
  public String LastError(String operation) {
    return Shared.lastError(operation);
  }

  @SimpleFunction(description = "Fallback polling untuk event "
      + "AfterPictureBase64Ready: path gambar terakhir yang jadi "
      + "(kosong bila belum ada).")
  public String GetLastPicturePath() {
    synchronized (viewLock) {
      return lastPicturePath;
    }
  }

  @SimpleFunction(description = "CALL: decode base64 jadi gambar di cache. Path hasil langsung bisa "
      + "dipasang ke Image.Picture. Hasil lewat AfterPictureBase64Ready. "
      + "Satu proses per saat: panggilan kedua saat sibuk = DITOLAK "
      + "(AfterPictureBase64Ready success=false + ErrorOccurred).")
  public void CallBase64ToPicture(final String base64) {
    if (!Shared.claimSlot("b64pic")) {
      Shared.err(this, "Base64", 0, "masih ada proses gambar berjalan");
      AfterPictureBase64Ready(false, "", "", "masih ada proses gambar berjalan");
      return;
    }
    final String b64 = base64 == null ? "" : base64;
    PictureStarted();
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        boolean ok = false;
        String outPath = "";
        String msg = "";
        try {
          String s = b64.trim();
          int comma = s.indexOf(',');
          if (s.startsWith("data:") && comma >= 0) {
            s = s.substring(comma + 1);
          }
          byte[] data = Base64.decode(s, Base64.DEFAULT);
          Bitmap bmp = boundedDecode(data);
          if (bmp == null) {
            msg = "base64 bukan gambar yang valid";
          } else {
            String ext = "png";
            if (data.length > 3 && (data[0] & 0xFF) == 0xFF
                && (data[1] & 0xFF) == 0xD8) {
              ext = "jpg";
            } else if (data.length > 3 && data[0] == 'G' && data[1] == 'I'
                && data[2] == 'F') {
              ext = "gif";
            }
            File cache = context.getCacheDir();
            if (!cache.exists()) {
              cache.mkdirs();
            }
            File dst = new File(cache, "pic_" + System.currentTimeMillis() + "." + ext);
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
            msg = "OK";
          }
        } catch (Exception e) {
          String m = e.getMessage();
          msg = m == null ? "base64 gagal di-decode" : m;
        } finally {
          Shared.releaseSlot("b64pic");
        }
        final boolean fOk = ok;
        final String fPath = outPath;
        final String fMsg = msg;
        final String fName = Shared.fileNameOf(outPath);
        try {
          form.runOnUiThread(new Runnable() {
            public void run() {
              if (fOk) {
                synchronized (viewLock) {
                  lastPicturePath = fPath;
                }
              }
              AfterPictureBase64Ready(fOk, fPath, fName, fMsg);
            }
          });
        } catch (Exception e) {
          if (fOk) {
            synchronized (viewLock) {
              lastPicturePath = fPath;
            }
          }
          AfterPictureBase64Ready(fOk, fPath, fName, fMsg);
        }
        if (!fOk) {
          Shared.err(Tools_Image.this, "Base64", 0, fMsg);
        }
      }
    });
  }

  @SimpleFunction(description = "Lebar dan tinggi gambar (pixel). SATU pintu: "
      + "path file/asset, http(s) URL (pakai cache bila pernah diunduh), base64, "
      + "atau content:// — terdeteksi "
      + "otomatis. Kembali: list [lebar, tinggi] (index 1 = lebar, 2 = tinggi). "
      + "[0, 0] bila bukan gambar / tidak ada. Pixel hanya untuk hitung-hitungan; "
      + "untuk dipasang ke SetImageSize pakai GetImageWidth/Height (satuan MIT).")
  public List<Integer> GetImageSize(String path) {
    ArrayList<Integer> out = new ArrayList<Integer>();
    out.add(Integer.valueOf(0));
    out.add(Integer.valueOf(0));
    try {
      String p = Shared.clean(path == null ? "" : path);
      byte[] data = loadRawBytes(p);
      if (data == null) {
        Shared.err(this, "Image", 0, "gambar tidak terbaca");
        return out;
      }
      BitmapFactory.Options o = new BitmapFactory.Options();
      o.inJustDecodeBounds = true;
      BitmapFactory.decodeByteArray(data, 0, data.length, o);
      if (o.outWidth > 0 && o.outHeight > 0) {
        out.set(0, Integer.valueOf(o.outWidth));
        out.set(1, Integer.valueOf(o.outHeight));
      } else {
        Shared.err(this, "Image", 0, "bukan gambar yang valid");
      }
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Image", 0, m == null ? "baca ukuran gagal" : m);
    }
    return out;
  }

  @SimpleFunction(description = "SET: hapus SEMUA cache unduhan URL gambar "
      + "(hemat unduh-ulang; maks 50MB, terlama terhapus otomatis). Pakai ini "
      + "bila gambar URL berubah tapi alamatnya sama (paksa unduh baru). "
      + "Sukses diam; gagal via ErrorOccurred.")
  public void SetClearImageCache() {
    try {
      File d = imgCacheDir();
      if (d == null || !d.exists()) {
        return;
      }
      File[] fs = d.listFiles();
      if (fs == null) {
        return;
      }
      for (int i = 0; i < fs.length; i = i + 1) {
        try {
          fs[i].delete();
        } catch (Exception e) {
          // abaikan
        }
      }
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Image", 0,
          m == null ? "hapus cache gagal" : m);
    }
  }

  // ================= EVENTS =================

  @SimpleEvent(description = "Hasil CallShowImage/SetImageSource: gambar ber-id tampil. "
      + "id + path YANG JADI (URL/base64/content -> file cache; asset/file -> path itu; "
      + "penting untuk simpan/bagi gambar).")
  public void ImageShown(int id, String path) {
    Shared.fire(this, "ImageShown", Integer.valueOf(id), path);
  }

  @SimpleEvent(description = "Gambar ber-id di-klik: id.")
  public void ImageClicked(int id) {
    Shared.fire(this, "ImageClicked", Integer.valueOf(id));
  }

  @SimpleEvent(description = "Gambar ber-id di-long-klik: id.")
  public void ImageLongClicked(int id) {
    Shared.fire(this, "ImageLongClicked", Integer.valueOf(id));
  }

  @SimpleEvent(description = "Skala zoom gambar berubah (akhir gesture): "
      + "id + skala (1.0 = pas). Fallback: GetZoom.")
  public void ZoomChanged(int id, float scale) {
    Shared.fire(this, "ZoomChanged", Integer.valueOf(id),
        new Float(scale));
  }

  @SimpleEvent(description = "Slider jadi: id + arah (true = kanan-kiri).")
  public void SliderCreated(int id, boolean horizontal) {
    Shared.fire(this, "SliderCreated", Integer.valueOf(id),
        Boolean.valueOf(horizontal));
  }

  @SimpleEvent(description = "Halaman slider pindah: id slider + index "
      + "(INGAT: 0 = pertama, bukan 1). Dipicu geser manual, GoTo/Next/Previous, "
      + "dan autoplay. Fallback: GetSliderIndex.")
  public void SliderChanged(int sliderId, int index) {
    Shared.fire(this, "SliderChanged",
        Integer.valueOf(sliderId), Integer.valueOf(index));
  }

  @SimpleEvent(description = "Halaman slider di-klik: id slider + index (0 = pertama).")
  public void SliderClicked(int sliderId, int index) {
    Shared.fire(this, "SliderClicked",
        Integer.valueOf(sliderId), Integer.valueOf(index));
  }

  @SimpleEvent(description = "Proses base64-gambar dimulai (tanpa parameter "
      + "karena antrean serial: satu proses per saat, id tidak perlu).")
  public void PictureStarted() {
    Shared.fire(this, "PictureStarted");
  }

  @SimpleEvent(description = "Hasil CallBase64ToPicture: success pertama, path absolut "
      + "(cache), fileName paling belakang. Path siap ke Image.Picture.")
  public void AfterPictureBase64Ready(boolean success, String path,
      String fileName, String message) {
    Shared.fire(this, "AfterPictureBase64Ready", Boolean.valueOf(success), path, fileName, message);
  }

  @SimpleEvent(description = "SATU-SATUNYA error Tools_Image. component = nama operasi "
      + "(bandingkan dengan blok OpImage/OpSlider/OpBase64; cth: if component = OpSlider "
      + "berarti error slider). id = id terkait (0 bila umum: Base64/id-pool).")
  public void ErrorOccurred(String component, int id, String message) {
    Shared.fire(this, "ErrorOccurred",
        component, Integer.valueOf(id), message);
  }

  // ================= OP =================

  @SimpleProperty(description = "Nama operasi Slider (untuk if di ErrorOccurred: "
      + "cth if component = OpSlider berarti error slider).",
      category = PropertyCategory.BEHAVIOR)
  public String OpSlider() {
    return "Slider";
  }

  @SimpleProperty(description = "Nama operasi Image (untuk if di ErrorOccurred: "
      + "cth if component = OpImage berarti error gambar).",
      category = PropertyCategory.BEHAVIOR)
  public String OpImage() {
    return "Image";
  }

  @SimpleProperty(description = "Nama operasi Base64 (untuk if di ErrorOccurred: "
      + "cth if component = OpBase64 berarti error decode base64).",
      category = PropertyCategory.BEHAVIOR)
  public String OpBase64() {
    return "Base64";
  }

  // ================= destroy =================

  public void onDestroy() {
    try {
      synchronized (viewLock) {
        for (ImageView iv : views.values()) {
          try {
            detach(iv);
          } catch (Exception e) {
            // abaikan
          }
        }
        views.clear();
        zoomHandlers.clear();
        for (Slider s : sliders.values()) {
          try {
            if (s.auto != null && s.autoTask != null) {
              s.auto.removeCallbacks(s.autoTask);
            }
          } catch (Exception e) {
            // abaikan
          }
        }
        sliders.clear();
      }
    } catch (Exception e) {
      // abaikan
    }
    Shared.releaseSlot("b64pic");
  }
}
