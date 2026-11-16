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
import com.google.appinventor.components.common.FileScope;
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
    version = 1,
    description = "Tools_Image: tampilkan BANYAK gambar ber-id ke layout (path/URL/base64 mandiri) + "
        + "gambar dari base64 + info ukuran. Id dikelola CreateImageId / Tools_Base. "
        + "Sukses tampil tanpa event (langsung kelihatan); gagal via ErrorOccurred. "
        + "List id URUT PEMBUATAN.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    androidMinSdk = 21,
    iconName = "aiwebres/tools_image.jpg")
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

  @SimpleFunction(description = "Tampilkan gambar ber-id ke layout (Arrangement MIT / dinamis). "
      + "Bisa jalan bareng (satu id = satu gambar). source kosong = pakai scope+path. "
      + "source bisa: path file, file:/// absolut, http(s) URL (di-cache otomatis), "
      + "atau base64 (mentah / data:...). id = penanda (minta ke CreateImageId / "
      + "Tools_Base CreateUniqueId, reservasi otomatis diadopsi). Sukses tidak menembak "
      + "event (gambar langsung kelihatan); gagal via ErrorOccurred.")
  public void ShowImage(final AndroidViewComponent container, final String source,
      final FileScope scope, final String path, final int id) {
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
    final FileScope fS = scope;
    final String fP = path == null ? "" : path;
    final AndroidViewComponent fContainer = container;
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
          } else {
            String usePath = src.length() > 0 ? src : fP;
            String p = Shared.clean(usePath);
            if (p.length() == 0) {
              err = "path kosong";
            } else if (fS == FileScope.Asset || Shared.isAssetPath(p)) {
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
                disp = usePath;
              }
            } else {
              File f = Shared.resolve(context, scope, usePath);
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
                    iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    iv.setAdjustViewBounds(true);
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
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
                iv.setImageBitmap(fBmp);
                vg.addView(iv, lp);
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

  @SimpleFunction(description = "Hapus SEMUA gambar Tools_Image dari layout itu "
      + "(komponen lain tidak diusik, id ikut dilepas).")
  public void ClearImage(final AndroidViewComponent container) {
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

  @SimpleFunction(description = "Aktif/matikan cubit-zoom (pinch) + geser "
      + "gambar ber-id. Aktif memakai mode MATRIX (klik tetap jalan untuk tap "
      + "tanpa zoom). Perubahan skala menembak ZoomChanged; baca kapan saja "
      + "via GetZoom.")
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

  @SimpleFunction(description = "Kembalikan zoom gambar ber-id ke pas (1.0).")
  public void ResetZoom(final int id) {
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

  // ================= SLIDER =================

  private Bitmap loadBitmap(String src, FileScope scope, String fallbackPath) {
    try {
      String s = Shared.clean(src == null ? "" : src);
      String usePath = s.length() > 0 ? s : Shared.clean(fallbackPath);
      if (s.regionMatches(true, 0, "http", 0, 4)) {
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
      String p = usePath;
      if (p.length() == 0) {
        return null;
      }
      byte[] data = null;
      if (scope == FileScope.Asset || Shared.isAssetPath(p)) {
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
        File f = Shared.resolve(context, scope, p);
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

  @SimpleFunction(description = "Bikin slider gambar ber-id (geser kanan-kiri "
      + "bila horizontal=true, atas-bawah bila false — lebih dari Iago/Boson "
      + "yang cuma kiri-kanan). Isi via SetSliderImages. Geser menembak "
      + "SliderChanged; baca kapan saja via GetSliderIndex.")
  public void CreateImageSlider(final AndroidViewComponent container,
      final int id, final boolean horizontal) {
    if (id < 1) {
      Shared.err(this, "Slider", id, "id harus >= 1 (pakai CreateSliderId)");
      return;
    }
    if (!Ids.register(Ids.SLIDER, id)) {
      Shared.err(this, "Slider", id, "ID " + id + " sedang dipakai slider lain");
      return;
    }
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

  @SimpleFunction(description = "Isi slider ber-id dengan daftar gambar "
      + "(list teks: path file/asset, URL http, atau base64 — sama fleksibelnya "
      + "dengan ShowImage; scope+path dipakai bila item kosong). Maks 20 "
      + "gambar. Gagal via ErrorOccurred.")
  public void SetSliderImages(final int sliderId, final FileScope scope,
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
    final List<String> use = list.size() > 20 ? list.subList(0, 20) : list;
    final FileScope fS = scope;
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        final ArrayList<Bitmap> bmps = new ArrayList<Bitmap>();
        int fail = 0;
        for (int i = 0; i < use.size(); i = i + 1) {
          Bitmap b = loadBitmap(use.get(i), fS, "");
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

  @SimpleFunction(description = "Geser slider ke halaman berikut (berputar).")
  public void SliderNext(final int id) {
    slideTo(id, 1, false);
  }

  @SimpleFunction(description = "Geser slider ke halaman sebelum (berputar).")
  public void SliderPrevious(final int id) {
    slideTo(id, -1, false);
  }

  @SimpleFunction(description = "Lompat ke halaman index (0 = pertama).")
  public void SliderGoTo(final int id, final int index) {
    Slider s;
    synchronized (viewLock) {
      s = sliders.get(Integer.valueOf(id));
    }
    if (s == null) {
      Shared.err(this, "Slider", id, "slider tidak ada");
      return;
    }
    final Slider fS = s;
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          goToLocked(fS, id, index, true);
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  private void slideTo(final int id, final int delta, final boolean silent) {
    Slider s;
    synchronized (viewLock) {
      s = sliders.get(Integer.valueOf(id));
    }
    if (s == null || s.pages.isEmpty()) {
      return;
    }
    final Slider fS = s;
    final int target = (s.index + delta + s.pages.size()) % s.pages.size();
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          goToLocked(fS, id, target, !silent);
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "Auto-putar slider tiap intervalMs "
      + "(0 = mati).")
  public void SetSliderAutoPlay(final int id, final long intervalMs) {
    Slider s;
    synchronized (viewLock) {
      s = sliders.get(Integer.valueOf(id));
    }
    if (s == null) {
      Shared.err(this, "Slider", id, "slider tidak ada");
      return;
    }
    final Slider fS = s;
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
                      cur = sliders.get(Integer.valueOf(id));
                    }
                    if (cur == null || cur != fS) {
                      return;
                    }
                    if (!cur.pages.isEmpty()) {
                      int t = (cur.index + 1) % cur.pages.size();
                      goToLocked(cur, id, t, true);
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
            Shared.err(Tools_Image.this, "Slider", id,
                m == null ? "autoplay gagal" : m);
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "Fallback polling untuk event SliderChanged: "
      + "index halaman aktif (0 = pertama).")
  public int GetSliderIndex(int id) {
    synchronized (viewLock) {
      Slider s = sliders.get(Integer.valueOf(id));
      return s == null ? -1 : s.index;
    }
  }

  @SimpleFunction(description = "Jumlah halaman slider ber-id.")
  public int GetSliderCount(int id) {
    synchronized (viewLock) {
      Slider s = sliders.get(Integer.valueOf(id));
      return s == null ? 0 : s.pages.size();
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

  @SimpleFunction(description = "Buat id gambar unik baru (Number, siap pakai ke ShowImage).")
  public int CreateImageId() {
    int id = Ids.createFor(Ids.IMAGE);
    if (id < 1) {
      Shared.err(this, "Image", 0, "gagal buat id");
    }
    return id;
  }

  @SimpleFunction(description = "Semua id gambar yang tampil (URUT PEMBUATAN, "
      + "index 1 = pertama dibuat).")
  public List<Integer> GetImageIds() {
    return Ids.idsOf(Ids.IMAGE);
  }

  @SimpleFunction(description = "Id gambar yang sedang tampil/loading (URUT PEMBUATAN).")
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
      + "ke CreateImageSlider).")
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
  public void RemoveSliderId(int id) {
    clearSliderStatic(id);
  }

  @SimpleFunction(description = "Fallback polling untuk event ErrorOccurred: "
      + "pesan error terakhir operasi ini (Image/Base64/Slider, kosong bila "
      + "belum ada).")
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

  @SimpleFunction(description = "Decode base64 jadi gambar di cache. Path hasil langsung bisa "
      + "dipasang ke Image.Picture. Hasil lewat AfterPictureBase64Ready. "
      + "Satu proses per saat.")
  public boolean Base64ToPicture(final String base64) {
    if (!Shared.claimSlot("b64pic")) {
      Shared.err(this, "Base64", 0, "masih ada proses gambar berjalan");
      return false;
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
              AfterPictureBase64Ready(fOk, "Cache", fPath, fName, fMsg);
            }
          });
        } catch (Exception e) {
          if (fOk) {
            synchronized (viewLock) {
              lastPicturePath = fPath;
            }
          }
          AfterPictureBase64Ready(fOk, "Cache", fPath, fName, fMsg);
        }
        if (!fOk) {
          Shared.err(Tools_Image.this, "Base64", 0, fMsg);
        }
      }
    });
    return true;
  }

  @SimpleFunction(description = "Lebar dan tinggi gambar (pixel). Kembali: list [lebar, tinggi]. "
      + "[0, 0] bila bukan gambar / tidak ada.")
  public List<Integer> GetImageSize(FileScope scope, String path) {
    ArrayList<Integer> out = new ArrayList<Integer>();
    out.add(Integer.valueOf(0));
    out.add(Integer.valueOf(0));
    try {
      String p = Shared.clean(path);
      byte[] data = null;
      if (scope == FileScope.Asset || Shared.isAssetPath(p)) {
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
        File f = Shared.resolve(context, scope, path);
        if (f == null || !f.exists()) {
          Shared.err(this, "Image", 0, "file tidak ada");
          return out;
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

  // ================= EVENTS =================

  @SimpleEvent(description = "Gambar ber-id di-klik: id.")
  public void ImageClicked(int id) {
    EventDispatcher.dispatchEvent(this, "ImageClicked", Integer.valueOf(id));
  }

  @SimpleEvent(description = "Gambar ber-id di-long-klik: id.")
  public void ImageLongClicked(int id) {
    EventDispatcher.dispatchEvent(this, "ImageLongClicked", Integer.valueOf(id));
  }

  @SimpleEvent(description = "Skala zoom gambar berubah (akhir gesture): "
      + "id + skala (1.0 = pas). Fallback: GetZoom.")
  public void ZoomChanged(int id, float scale) {
    EventDispatcher.dispatchEvent(this, "ZoomChanged", Integer.valueOf(id),
        new Float(scale));
  }

  @SimpleEvent(description = "Slider jadi: id + arah (true = kanan-kiri).")
  public void SliderCreated(int id, boolean horizontal) {
    EventDispatcher.dispatchEvent(this, "SliderCreated", Integer.valueOf(id),
        Boolean.valueOf(horizontal));
  }

  @SimpleEvent(description = "Halaman slider pindah: id slider + index "
      + "(0 = pertama). Fallback: GetSliderIndex.")
  public void SliderChanged(int sliderId, int index) {
    EventDispatcher.dispatchEvent(this, "SliderChanged",
        Integer.valueOf(sliderId), Integer.valueOf(index));
  }

  @SimpleEvent(description = "Halaman slider di-klik: id slider + index.")
  public void SliderClicked(int sliderId, int index) {
    EventDispatcher.dispatchEvent(this, "SliderClicked",
        Integer.valueOf(sliderId), Integer.valueOf(index));
  }

  @SimpleEvent(description = "Proses base64-gambar dimulai.")
  public void PictureStarted() {
    EventDispatcher.dispatchEvent(this, "PictureStarted");
  }

  @SimpleEvent(description = "Hasil Base64ToPicture: success pertama, scope menemani path "
      + "(selalu Cache), fileName paling belakang. Path siap ke Image.Picture.")
  public void AfterPictureBase64Ready(boolean success, String scope, String path,
      String fileName, String message) {
    EventDispatcher.dispatchEvent(this, "AfterPictureBase64Ready",
        Boolean.valueOf(success), scope, path, fileName, message);
  }

  @SimpleEvent(description = "SATU-SATUNYA error Tools_Image. component = nama operasi "
      + "(bandingkan dengan blok Op...).")
  public void ErrorOccurred(String component, int id, String message) {
    EventDispatcher.dispatchEvent(this, "ErrorOccurred",
        component, Integer.valueOf(id), message);
  }

  // ================= OP =================

  @SimpleProperty(description = "Nama operasi Slider (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpSlider() {
    return "Slider";
  }

  @SimpleProperty(description = "Nama operasi Image (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpImage() {
    return "Image";
  }

  @SimpleProperty(description = "Nama operasi Base64 (untuk if di ErrorOccurred).",
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
