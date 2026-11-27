package com.fv7.tools;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;

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
import com.google.appinventor.components.runtime.OnDestroyListener;
import com.google.appinventor.components.runtime.util.AsynchUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@DesignerComponent(
    version = 1,
    description = "Tools_Pdf: baca PDF tanpa library luar (xref tabel/stream + "
        + "object stream, Flate/ASCII85/ASCIIHex/RunLength, ToUnicode, susunan "
        + "per baris) + render halaman jadi gambar via PdfRenderer. "
        + "Alur: OpenPdf(path) -> PdfOpened(success, pageCount, title, author). "
        + "Lalu GetPageText/GetAllText/GetPageCount/GetPdfTitle/GetPdfAuthor/ "
        + "GetPageSize sinkron, RenderPage(page, imageName, folder) async -> "
        + "PageRendered. path: relatif di-root ke /storage/emulated/0, absolut "
        + "apa adanya, file:/... dibuka bungkusnya, //... = asset (disalin ke "
        + "cache). PDF dari internet? unduh dulu pakai Download Tools_Downloader. "
        + "PDF terkunci (encrypted) ditolak dengan pesan jelas. Tutup via "
        + "ClosePdf (buka PDF lain otomatis menutup yang lama). Konvensi awalan: "
        + "Is = boolean (colok ke if), async + hasil lewat event, "
        + "Set = tanpa feedback sukses (gagal via ErrorOccurred), "
        + "Get = nilai balik sinkron.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    androidMinSdk = 21,
    iconName = "aiwebres/tools_pdf.jpg",
    helpUrl = "https://github.com/VB-7/MiT-File_Manager")
@SimpleObject(external = true)
@UsesPermissions({"android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE"})
public class Tools_Pdf extends AndroidNonvisibleComponent
    implements OnDestroyListener {

  private final Context context;
  private final Activity activity;

  private final Object docLock = new Object();
  private PdfParser.Doc doc = null;
  private PdfRenderer renderer = null;
  private ParcelFileDescriptor pfd = null;
  private File tempCache = null;

  /** Skala render (persen, 100 = 72 dpi). Default 200. */
  private int renderScale = 200;

  public Tools_Pdf(ComponentContainer container) {
    super(container.$form());
    this.context = container.$context();
    this.activity = container.$context();
  }

  private File resolvePdfFile(String path) {
    try {
      String p = Shared.clean(path);
      if (p.length() == 0) {
        return null;
      }
      if (Shared.isAssetPath(p)) {
        File c = Shared.assetToCache(context, p);
        if (c != null && c.isFile()) {
          tempCache = c;
        }
        return c;
      }
      if (p.regionMatches(true, 0, "file:", 0, 5)) {
        String q = p.substring(5);
        while (q.startsWith("//")) {
          q = q.substring(2);
        }
        return new File(q);
      }
      if (p.regionMatches(true, 0, "content:", 0, 8)
          || p.regionMatches(true, 0, "http://", 0, 7)
          || p.regionMatches(true, 0, "https://", 0, 8)) {
        return null;
      }
      return Shared.resolveRooted(p);
    } catch (Exception e) {
      return null;
    }
  }

  private void closeLocked() {
    if (renderer != null) {
      try {
        renderer.close();
      } catch (Exception e) {
        // abaikan
      }
      renderer = null;
    }
    if (pfd != null) {
      try {
        pfd.close();
      } catch (Exception e) {
        // abaikan
      }
      pfd = null;
    }
    doc = null;
    if (tempCache != null) {
      try {
        tempCache.delete();
      } catch (Exception e) {
        // abaikan
      }
      tempCache = null;
    }
  }

  @SimpleFunction(description = "Buka PDF (path/asset/file:). Hasil lewat "
      + "PdfOpened(success, pageCount, title, author, message). PDF terkunci "
      + "(encrypted) = gagal + ErrorOccurred. Buka PDF lain otomatis menutup "
      + "yang lama. Butuh izin tulis (buka 'Akses semua file' di Android 11+).")
  public void OpenPdf(String path) {
    final String p = Shared.clean(path);
    if (p.length() == 0) {
      Shared.err(this, "Pdf", 0, "path kosong");
      PdfOpened(false, 0, "", "", "path kosong");
      return;
    }
    if (p.regionMatches(true, 0, "content:", 0, 8)
        || p.regionMatches(true, 0, "http://", 0, 7)
        || p.regionMatches(true, 0, "https://", 0, 8)) {
      Shared.err(this, "Pdf", 0,
          "content:/http: tidak didukung - unduh dulu pakai Download");
      PdfOpened(false, 0, "", "", "unduh dulu pakai Download");
      return;
    }
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        if (!Shared.ensurePermission(form, activity,
            Shared.PERM_WRITE_STORAGE)) {
          String msg = "izin belum granted (Android 11+: aktifkan 'Akses semua file')";
          Shared.err(Tools_Pdf.this, "Pdf", 0, msg);
          PdfOpened(false, 0, "", "", msg);
          return;
        }
        File f = resolvePdfFile(p);
        if (f == null || !f.isFile()) {
          Shared.err(Tools_Pdf.this, "Pdf", 0, "file tidak ada");
          PdfOpened(false, 0, "", "", "file tidak ada");
          return;
        }
        InputStream in = null;
        try {
          in = new FileInputStream(f);
          byte[] data = Shared.readAll(in);
          PdfParser.Doc d = PdfParser.parse(data);
          ParcelFileDescriptor fd = ParcelFileDescriptor.open(f,
              ParcelFileDescriptor.MODE_READ_ONLY);
          PdfRenderer r = null;
          try {
            r = new PdfRenderer(fd);
          } catch (Exception e) {
            try {
              fd.close();
            } catch (Exception ex) {
              // abaikan
            }
            throw e;
          }
          int n;
          String title;
          String author;
          synchronized (docLock) {
            closeLocked();
            doc = d;
            pfd = fd;
            renderer = r;
            n = d.pages.size();
            title = d.title;
            author = d.author;
          }
          final int fN = n;
          final String fT = title;
          final String fA = author;
          try {
            form.runOnUiThread(new Runnable() {
              public void run() {
                PdfOpened(true, fN, fT, fA, "OK");
              }
            });
          } catch (Exception e) {
            PdfOpened(true, fN, fT, fA, "OK");
          }
        } catch (Exception e) {
          String m = e.getMessage();
          if (m == null) {
            m = "buka PDF gagal";
          }
          Shared.err(Tools_Pdf.this, "Pdf", 0, m);
          PdfOpened(false, 0, "", "", m);
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
    });
  }

  @SimpleFunction(description = "Tutup PDF yang terbuka + lepas file descriptor "
      + "(bebaskan memori). Tanpa event; cek via IsPdfOpen.")
  public void ClosePdf() {
    synchronized (docLock) {
      closeLocked();
    }
  }

  @SimpleFunction(description = "True bila ada PDF terbuka (siap dibaca/"
      + "render). Colok ke if.")
  public boolean IsPdfOpen() {
    synchronized (docLock) {
      return doc != null;
    }
  }

  @SimpleFunction(description = "Jumlah halaman PDF (0 bila belum buka).")
  public int GetPageCount() {
    synchronized (docLock) {
      return doc == null ? 0 : doc.pages.size();
    }
  }

  @SimpleFunction(description = "Judul PDF /Title (kosong bila tak ada/belum "
      + "buka).")
  public String GetPdfTitle() {
    synchronized (docLock) {
      return doc == null ? "" : doc.title;
    }
  }

  @SimpleFunction(description = "Penulis PDF /Author (kosong bila tak ada/belum "
      + "buka).")
  public String GetPdfAuthor() {
    synchronized (docLock) {
      return doc == null ? "" : doc.author;
    }
  }

  @SimpleFunction(description = "Teks halaman ke-page (1 = pertama). Baris "
      + "diurut atas-bawah, kiri-kanan. Gagal (belum buka/halaman salah) = "
      + "teks kosong + ErrorOccurred.")
  public String GetPageText(int page) {
    synchronized (docLock) {
      if (doc == null) {
        Shared.err(this, "Pdf", 0, "belum buka PDF (pakai OpenPdf dulu)");
        return "";
      }
      if (page < 1 || page > doc.pages.size()) {
        Shared.err(this, "Pdf", 0,
            "halaman 1.." + doc.pages.size());
        return "";
      }
      String t = doc.pages.get(page - 1).text;
      return t == null ? "" : t;
    }
  }

  @SimpleFunction(description = "Teks SEMUA halaman digabung (separator di "
      + "antaranya, mis. \"\\n\\n\" atau \" \"). Gagal = teks kosong + "
      + "ErrorOccurred.")
  public String GetAllText(String separator) {
    synchronized (docLock) {
      if (doc == null) {
        Shared.err(this, "Pdf", 0, "belum buka PDF (pakai OpenPdf dulu)");
        return "";
      }
      String sep = separator == null ? "\n" : separator;
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < doc.pages.size(); i = i + 1) {
        if (i > 0) {
          sb.append(sep);
        }
        String t = doc.pages.get(i).text;
        if (t != null) {
          sb.append(t);
        }
      }
      return sb.toString();
    }
  }

  @SimpleFunction(description = "Ukuran halaman ke-page: list [lebarPt, "
      + "tinggiPt] (point 1/72 inci). Gagal = list kosong + ErrorOccurred.")
  public List<Object> GetPageSize(int page) {
    ArrayList<Object> out = new ArrayList<Object>();
    synchronized (docLock) {
      if (doc == null) {
        Shared.err(this, "Pdf", 0, "belum buka PDF (pakai OpenPdf dulu)");
        return out;
      }
      if (page < 1 || page > doc.pages.size()) {
        Shared.err(this, "Pdf", 0,
            "halaman 1.." + doc.pages.size());
        return out;
      }
      out.add(Double.valueOf(doc.pages.get(page - 1).widthPt));
      out.add(Double.valueOf(doc.pages.get(page - 1).heightPt));
    }
    return out;
  }

  @SimpleFunction(description = "Render halaman ke-page jadi gambar "
      + "(PNG/JPG ikut ekstensi imageName; kosong = halaman-N.png) ke folder "
      + "(relatif aman: Download/). Skala ikut RenderScale. Hasil lewat "
      + "PageRendered(success, page, savePath, message).")
  public void RenderPage(int page, String imageName, String folder) {
    final int fPage = page;
    final String fName = Shared.clean(imageName);
    final String fFolder = Shared.clean(folder);
    synchronized (docLock) {
      if (doc == null) {
        Shared.err(this, "Pdf", 0, "belum buka PDF (pakai OpenPdf dulu)");
        PageRendered(false, fPage, "", "belum buka PDF");
        return;
      }
      if (fPage < 1 || fPage > doc.pages.size()) {
        Shared.err(this, "Pdf", 0, "halaman 1.." + doc.pages.size());
        PageRendered(false, fPage, "", "halaman 1.." + doc.pages.size());
        return;
      }
    }
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        if (!Shared.ensurePermission(form, activity,
            Shared.PERM_WRITE_STORAGE)) {
          String msg = "izin belum granted (Android 11+: aktifkan 'Akses semua file')";
          Shared.err(Tools_Pdf.this, "Pdf", 0, msg);
          PageRendered(false, fPage, "", msg);
          return;
        }
        File dir = Shared.resolveRooted(fFolder);
        if (dir == null) {
          Shared.err(Tools_Pdf.this, "Pdf", 0, "tidak bisa render ke Asset");
          PageRendered(false, fPage, "", "tidak bisa render ke Asset");
          return;
        }
        try {
          if (!dir.exists()) {
            dir.mkdirs();
          }
        } catch (Exception e) {
          Shared.err(Tools_Pdf.this, "Pdf", 0, "folder tujuan tidak bisa dibuat");
          PageRendered(false, fPage, "", "folder tujuan tidak bisa dibuat");
          return;
        }
        PdfRenderer.Page pg = null;
        try {
          PdfRenderer r;
          double wPt;
          double hPt;
          int scale;
          synchronized (docLock) {
            r = renderer;
            if (doc == null || r == null || fPage < 1
                || fPage > doc.pages.size()) {
              PageRendered(false, fPage, "", "PDF sudah ditutup");
              return;
            }
            wPt = doc.pages.get(fPage - 1).widthPt;
            hPt = doc.pages.get(fPage - 1).heightPt;
            scale = renderScale;
          }
          pg = r.openPage(fPage - 1);
          int w = (int) Math.round(wPt * scale / 100.0);
          int h = (int) Math.round(hPt * scale / 100.0);
          if (w < 1) {
            w = 1;
          }
          if (h < 1) {
            h = 1;
          }
          int big = w > h ? w : h;
          if (big > 4096) {
            double k = 4096.0 / big;
            w = Math.max(1, (int) Math.round(w * k));
            h = Math.max(1, (int) Math.round(h * k));
          }
          Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
          try {
            Canvas cv = new Canvas(bmp);
            cv.drawColor(0xFFFFFFFF);
            pg.render(bmp, null, null,
                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
          } finally {
            try {
              pg.close();
            } catch (Exception e) {
              // abaikan
            }
            pg = null;
          }
          String name = fName;
          if (name.length() == 0) {
            name = "halaman-" + fPage + ".png";
          } else {
            name = Shared.sanitize(name);
            if (name.indexOf('.') < 0) {
              name = name + ".png";
            }
          }
          boolean jpg = name.toLowerCase().endsWith(".jpg")
              || name.toLowerCase().endsWith(".jpeg");
          File out = Shared.uniqueFile(dir, name);
          FileOutputStream fos = new FileOutputStream(out);
          try {
            if (jpg) {
              bmp.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            } else {
              bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            }
            fos.flush();
          } finally {
            try {
              fos.close();
            } catch (Exception e) {
              // abaikan
            }
            try {
              bmp.recycle();
            } catch (Exception e) {
              // abaikan
            }
          }
          PageRendered(true, fPage, out.getAbsolutePath(), "OK");
        } catch (Exception e) {
          if (pg != null) {
            try {
              pg.close();
            } catch (Exception ex) {
              // abaikan
            }
          }
          String m = e.getMessage();
          if (m == null) {
            m = "render gagal";
          }
          Shared.err(Tools_Pdf.this, "Pdf", 0, m);
          PageRendered(false, fPage, "", m);
        }
      }
    });
  }

  @SimpleProperty(description = "SET: skala render halaman (persen, 100 = "
      + "72 dpi; default 200). Dijepit 25..800. Sukses diam.")
  public void RenderScale(int percent) {
    int v = percent;
    if (v < 25) {
      v = 25;
    }
    if (v > 800) {
      v = 800;
    }
    renderScale = v;
  }

  @SimpleFunction(description = "Skala render sekarang (persen, default 200).")
  public int GetRenderScale() {
    return renderScale;
  }

  // ================= EVENTS =================

  @SimpleEvent(description = "PDF selesai dibuka: success + jumlah halaman + "
      + "judul + penulis. Gagal = success false (lihat message + "
      + "ErrorOccurred).")
  public void PdfOpened(boolean success, int pageCount, String title,
      String author, String message) {
    Shared.fire(this, "PdfOpened", Boolean.valueOf(success),
        Integer.valueOf(pageCount), title, author, message);
  }

  @SimpleEvent(description = "Render halaman selesai: success + nomor halaman "
      + "(1-based) + path gambar + message.")
  public void PageRendered(boolean success, int page, String savePath,
      String message) {
    Shared.fire(this, "PageRendered", Boolean.valueOf(success),
        Integer.valueOf(page), savePath, message);
  }

  @SimpleEvent(description = "SATU-SATUNYA error Tools_Pdf. component = nama "
      + "operasi (isi dengan OpPdf untuk if). id selalu 0 (tanpa id).")
  public void ErrorOccurred(String component, int id, String message) {
    Shared.fire(this, "ErrorOccurred",
        component, Integer.valueOf(id), message);
  }

  // ================= OP =================

  @SimpleProperty(description = "Nama operasi Pdf untuk if di ErrorOccurred. "
      + "Cth: jika component = OpPdf maka tampilkan message.",
      category = PropertyCategory.BEHAVIOR)
  public String OpPdf() {
    return "Pdf";
  }

  @SimpleFunction(description = "Fallback polling untuk event ErrorOccurred: "
      + "pesan error terakhir operasi ini (teks kosong bila belum ada). Isi "
      + "operation dengan blok OpPdf.")
  public String LastError(String operation) {
    return Shared.lastError(operation);
  }

  // ================= destroy =================

  public void onDestroy() {
    synchronized (docLock) {
      closeLocked();
    }
  }
}
