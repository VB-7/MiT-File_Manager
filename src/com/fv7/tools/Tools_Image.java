package com.fv7.tools;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@DesignerComponent(
    version = 1,
    description = "Tools_Image: tampilkan gambar ke layout (path/URL/base64 mandiri) + "
        + "gambar dari base64 + info ukuran. Sukses tampil tanpa event (langsung "
        + "kelihatan); gagal via ErrorOccurred.",
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
  private ImageView imageView = null;

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

  @SimpleFunction(description = "Tampilkan gambar ke layout (Arrangement MIT / dinamis). "
      + "source kosong = pakai scope+path. source bisa: path file, file:/// absolut, "
      + "http(s) URL (di-cache otomatis), atau base64 (mentah / data:...). "
      + "id = penanda (minta ke Tools_Base) — dipakai di ErrorOccurred bila gagal. "
      + "Sukses tidak menembak event (gambar langsung kelihatan).")
  public void ShowImage(final AndroidViewComponent container, final String source,
      final FileScope scope, final String path, final int id) {
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
              File f = Shared.resolve(context, fS, usePath);
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
                  Shared.err(Tools_Image.this, "Image", id, "layout tidak valid");
                  return;
                }
                if (imageView == null) {
                  imageView = new ImageView(activity);
                  imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                  imageView.setAdjustViewBounds(true);
                }
                detach(imageView);
                ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
                imageView.setImageBitmap(fBmp);
                vg.addView(imageView, lp);
              } catch (Exception e) {
                String m = e.getMessage();
                Shared.err(Tools_Image.this, "Image", id,
                    m == null ? "gambar gagal ditampilkan" : m);
              }
            }
          });
        } catch (Exception e) {
          String m = e.getMessage();
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

  @SimpleFunction(description = "Hapus gambar Tools_Image dari layout itu (komponen lain "
      + "tidak diusik).")
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
            if (imageView != null && imageView.getParent() == vg) {
              vg.removeView(imageView);
            }
          } catch (Exception e) {
            String m = e.getMessage();
            Shared.err(Tools_Image.this, "Image", 0, m == null ? "clear gagal" : m);
          }
        }
      });
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(Tools_Image.this, "Image", 0, m == null ? "clear gagal" : m);
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
              AfterPictureBase64Ready(fOk, "Cache", fPath, fName, fMsg);
            }
          });
        } catch (Exception e) {
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

  @SimpleEvent(description = "Proses base64→gambar dimulai.")
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
      if (imageView != null) {
        detach(imageView);
        imageView = null;
      }
    } catch (Exception e) {
      // abaikan
    }
    Shared.releaseSlot("b64pic");
  }
}
