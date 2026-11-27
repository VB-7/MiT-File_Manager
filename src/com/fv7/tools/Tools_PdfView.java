package com.fv7.tools;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ScrollView;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.AndroidViewComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.OnDestroyListener;
import com.google.appinventor.components.runtime.util.AsynchUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@DesignerComponent(
    version = 1,
    description = "Tools_PdfView: penampil PDF ber-id (satu paket dgn Tools_Pdf "
        + "non-visible). CreatePdfView(container, id) taruh viewer ke wadah "
        + "(mengikuti wadah) atau langsung; OpenPdf(id, path) tampil otomatis. "
        + "Navigasi: swipe (saat zoom 100) + NextPage/PrevPage/GoToPage. Zoom: "
        + "pinch + SetZoom(id, persen) + SetZoomEnabled. Multi-id jalan bareng. "
        + "Semua event opsional (tanpa handler tetap jalan default). Mesin "
        + "PdfRenderer bawaan Android (offline, tanpa library).",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    androidMinSdk = 21,
    iconName = "aiwebres/tools_pdf.jpg",
    helpUrl = "https://github.com/VB-7/MiT-File_Manager")
@SimpleObject(external = true)
@UsesPermissions({"android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE",
    "android.permission.MANAGE_EXTERNAL_STORAGE"})
public class Tools_PdfView extends AndroidNonvisibleComponent
    implements OnDestroyListener {

  private static final class Holder {
    FrameLayout box;
    ScrollView vScroll;
    HorizontalScrollView hScroll;
    ImageView page;
    Bitmap bitmap;
    PdfRenderer renderer;
    ParcelFileDescriptor fd;
    File cacheFile;
    int pageCount = 0;
    int curPage = 0;
    int zoom = 100;
    boolean zoomEnabled = true;
    String path = "";
    GestureDetector fling;
    ScaleGestureDetector pinch;
    boolean scaling = false;
  }

  private static final Map<Integer, Holder> holders =
      new HashMap<Integer, Holder>();
  private static final Object viewLock = new Object();
  private static final int ZOOM_MIN = 50;
  private static final int ZOOM_MAX = 400;
  private static final int BITMAP_MAX = 4096;

  private final Context context;
  private final Activity activity;

  public Tools_PdfView(ComponentContainer container) {
    super(container.$form());
    this.context = container.$context();
    this.activity = container.$context();
    try {
      container.$form().registerForOnDestroy(this);
    } catch (Exception e) {
      // abaikan
    }
  }

  public void onDestroy() {
    ArrayList<Integer> all;
    synchronized (viewLock) {
      all = new ArrayList<Integer>(holders.keySet());
    }
    for (int i = 0; i < all.size(); i = i + 1) {
      try {
        closeHolder(all.get(i).intValue(), true);
      } catch (Exception e) {
        // abaikan
      }
    }
  }

  // ================= internal =================

  private Holder holderOf(int id) {
    synchronized (viewLock) {
      return holders.get(Integer.valueOf(id));
    }
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

  private boolean wantAsset(String path) {
    return Shared.isAssetPath(Shared.clean(path));
  }

  private File rootedFile(String path) {
    return Shared.resolveRooted(path);
  }

  private int clampZoom(int z) {
    if (z < ZOOM_MIN) {
      return ZOOM_MIN;
    }
    if (z > ZOOM_MAX) {
      return ZOOM_MAX;
    }
    return z;
  }

  private void closeRenderer(Holder h) {
    if (h == null) {
      return;
    }
    try {
      if (h.renderer != null) {
        h.renderer.close();
      }
    } catch (Exception e) {
      // abaikan
    }
    h.renderer = null;
    try {
      if (h.fd != null) {
        h.fd.close();
      }
    } catch (Exception e) {
      // abaikan
    }
    h.fd = null;
    h.pageCount = 0;
    h.curPage = 0;
    h.path = "";
    if (h.cacheFile != null) {
      try {
        h.cacheFile.delete();
      } catch (Exception e) {
        // abaikan
      }
      h.cacheFile = null;
    }
  }

  private void closeHolder(int id, boolean silent) {
    Holder h = holderOf(id);
    if (h == null) {
      return;
    }
    final View box = h.box;
    try {
      if (box != null) {
        detach(box);
      }
    } catch (Exception e) {
      // abaikan
    }
    try {
      if (h.bitmap != null && !h.bitmap.isRecycled()) {
        h.bitmap.recycle();
      }
    } catch (Exception e) {
      // abaikan
    }
    h.bitmap = null;
    closeRenderer(h);
    synchronized (viewLock) {
      holders.remove(Integer.valueOf(id));
    }
    Ids.setActive(Ids.PDFVIEW, id, false);
    Ids.unregister(Ids.PDFVIEW, id);
  }

  private int baseWidth(Holder h) {
    try {
      int w = h.box.getWidth();
      if (w > 0) {
        return w;
      }
    } catch (Exception e) {
      // abaikan
    }
    try {
      int w = context.getResources().getDisplayMetrics().widthPixels;
      return w > 0 ? w : 1080;
    } catch (Exception e) {
      return 1080;
    }
  }

  private void renderPage(final int id, final boolean fireEvent) {
    final Holder h = holderOf(id);
    if (h == null) {
      return;
    }
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        final Holder hh = holderOf(id);
        if (hh == null || hh.renderer == null) {
          return;
        }
        final int pg;
        final int zm;
        synchronized (viewLock) {
          pg = hh.curPage;
          zm = hh.zoom;
        }
        if (pg < 1) {
          return;
        }
        PdfRenderer.Page page = null;
        Bitmap bmp = null;
        try {
          if (pg - 1 >= hh.renderer.getPageCount()) {
            return;
          }
          page = hh.renderer.openPage(pg - 1);
          int baseW = baseWidth(hh);
          int targetW = Math.max(1, baseW * zm / 100);
          float scale = (float) targetW / (float) Math.max(1, page.getWidth());
          int targetH = Math.max(1, (int) (page.getHeight() * scale));
          float max = (float) BITMAP_MAX;
          if (targetW > BITMAP_MAX || targetH > BITMAP_MAX) {
            float down = Math.min(max / targetW, max / targetH);
            targetW = Math.max(1, (int) (targetW * down));
            targetH = Math.max(1, (int) (targetH * down));
          }
          bmp = Bitmap.createBitmap(targetW, targetH,
              Bitmap.Config.ARGB_8888);
          page.render(bmp, null, null,
              PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        } catch (Exception e) {
          if (page != null) {
            try {
              page.close();
            } catch (Exception e2) {
              // abaikan
            }
          }
          if (bmp != null) {
            try {
              bmp.recycle();
            } catch (Exception e2) {
              // abaikan
            }
          }
          return;
        } finally {
          if (page != null) {
            try {
              page.close();
            } catch (Exception e) {
              // abaikan
            }
          }
        }
        final Bitmap fBmp = bmp;
        try {
          form.runOnUiThread(new Runnable() {
            public void run() {
              Holder h2 = holderOf(id);
              if (h2 == null || h2.renderer == null) {
                if (fBmp != null) {
                  try {
                    fBmp.recycle();
                  } catch (Exception e) {
                    // abaikan
                  }
                }
                return;
              }
              try {
                if (h2.bitmap != null && !h2.bitmap.isRecycled()) {
                  h2.bitmap.recycle();
                }
              } catch (Exception e) {
                // abaikan
              }
              h2.bitmap = fBmp;
              try {
                h2.page.setImageBitmap(fBmp);
                ViewGroup.LayoutParams lp = h2.page.getLayoutParams();
                if (lp != null) {
                  lp.width = fBmp.getWidth();
                  lp.height = fBmp.getHeight();
                  h2.page.setLayoutParams(lp);
                }
              } catch (Exception e) {
                // abaikan
              }
              if (fireEvent) {
                PageChanged(id, h2.curPage, h2.pageCount);
              }
            }
          });
        } catch (Exception e) {
          // abaikan
        }
      }
    });
  }

  private void goTo(final int id, final int page) {
    final Holder h = holderOf(id);
    if (h == null) {
      Shared.err(this, "PdfView", id, "id tidak ada");
      return;
    }
    if (h.renderer == null || h.pageCount < 1) {
      Shared.err(this, "PdfView", id, "belum buka PDF");
      return;
    }
    int p = page < 1 ? 1 : page;
    if (p > h.pageCount) {
      p = h.pageCount;
    }
    synchronized (viewLock) {
      if (h.curPage == p) {
        return;
      }
      h.curPage = p;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            Holder h2 = holderOf(id);
            if (h2 != null && h2.vScroll != null) {
              h2.vScroll.scrollTo(0, 0);
              h2.hScroll.scrollTo(0, 0);
            }
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
    renderPage(id, true);
  }

  // ================= blok: id + tampil =================

  @SimpleFunction(description = "Buat id PdfView unik baru (Number, siap pakai "
      + "ke CreatePdfView). URUT PEMBUATAN.")
  public int CreatePdfViewId() {
    int id = Ids.createFor(Ids.PDFVIEW);
    if (id < 1) {
      Shared.err(this, "PdfView", 0, "gagal buat id");
    }
    return id;
  }

  @SimpleFunction(description = "Buat + tampilkan viewer PDF kosong ber-id ke "
      + "layout ( wadah = arrangement; langsung di Screen = mandiri). Id NUMBER "
      + "(pakai CreatePdfViewId). Buka file via OpenPdf(id, path). Sukses "
      + "menembak PdfViewCreated; gagal via ErrorOccurred.")
  public void CreatePdfView(final AndroidViewComponent container, final int id) {
    if (id < 1) {
      Shared.err(this, "PdfView", id, "id harus >= 1 (pakai CreatePdfViewId)");
      return;
    }
    if (!Ids.register(Ids.PDFVIEW, id)) {
      Shared.err(this, "PdfView", id, "ID " + id + " sedang dipakai viewer lain");
      return;
    }
    synchronized (viewLock) {
      Holder old = holders.get(Integer.valueOf(id));
      if (old != null && old.box != null) {
        Shared.err(this, "PdfView", id, "ID " + id + " masih tampil "
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
              Ids.setActive(Ids.PDFVIEW, id, false);
              Ids.unregister(Ids.PDFVIEW, id);
              Shared.err(Tools_PdfView.this, "PdfView", id,
                  "layout tidak valid");
              return;
            }
            final Holder h = new Holder();
            final FrameLayout box = new FrameLayout(context);
            final ScrollView sv = new ScrollView(context);
            final HorizontalScrollView hsv = new HorizontalScrollView(context);
            final ImageView iv = new ImageView(context);
            try {
              iv.setScaleType(ImageView.ScaleType.FIT_XY);
              iv.setAdjustViewBounds(true);
            } catch (Exception e) {
              // abaikan
            }
            hsv.addView(iv, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
            sv.addView(hsv, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
            box.addView(sv, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
            h.box = box;
            h.vScroll = sv;
            h.hScroll = hsv;
            h.page = iv;
            final int fid = id;
            GestureDetector fling = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener() {
                  public boolean onDown(MotionEvent e) {
                    return true;
                  }
                  public boolean onFling(MotionEvent e1, MotionEvent e2,
                      float vx, float vy) {
                    try {
                      Holder h2 = holderOf(fid);
                      if (h2 == null || h2.renderer == null) {
                        return false;
                      }
                      if (!h2.zoomEnabled || h2.zoom != 100) {
                        return false;
                      }
                      if (e1 == null || e2 == null) {
                        return false;
                      }
                      float dx = e2.getX() - e1.getX();
                      float dy = e2.getY() - e1.getY();
                      if (Math.abs(dx) < 120
                          || Math.abs(dx) < Math.abs(dy) * 1.5f
                          || Math.abs(vx) < 400) {
                        return false;
                      }
                      if (dx < 0) {
                        goTo(fid, h2.curPage + 1);
                      } else {
                        goTo(fid, h2.curPage - 1);
                      }
                      return true;
                    } catch (Exception e) {
                      return false;
                    }
                  }
                });
            h.fling = fling;
            ScaleGestureDetector pinch = new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                  public boolean onScaleBegin(ScaleGestureDetector d) {
                    Holder h2 = holderOf(fid);
                    if (h2 == null || !h2.zoomEnabled) {
                      return false;
                    }
                    h2.scaling = true;
                    return true;
                  }
                  public boolean onScale(ScaleGestureDetector d) {
                    return true;
                  }
                  public void onScaleEnd(ScaleGestureDetector d) {
                    try {
                      Holder h2 = holderOf(fid);
                      if (h2 == null || !h2.zoomEnabled) {
                        if (h2 != null) {
                          h2.scaling = false;
                        }
                        return;
                      }
                      h2.scaling = false;
                      int nz = clampZoom((int) (h2.zoom * d.getScaleFactor()));
                      if (nz != h2.zoom) {
                        synchronized (viewLock) {
                          h2.zoom = nz;
                        }
                        renderPage(fid, false);
                      }
                    } catch (Exception e) {
                      // abaikan
                    }
                  }
                });
            h.pinch = pinch;
            iv.setOnTouchListener(new View.OnTouchListener() {
              public boolean onTouch(View v, MotionEvent e) {
                try {
                  Holder h2 = holderOf(fid);
                  if (h2 == null) {
                    return false;
                  }
                  if (h2.pinch != null) {
                    h2.pinch.onTouchEvent(e);
                  }
                  if (h2.scaling) {
                    return true;
                  }
                  if (h2.zoomEnabled && h2.zoom == 100
                      && h2.fling != null
                      && h2.fling.onTouchEvent(e)) {
                    return true;
                  }
                } catch (Exception ex) {
                  // abaikan
                }
                return false;
              }
            });
            synchronized (viewLock) {
              holders.put(Integer.valueOf(fid), h);
            }
            detach(box);
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
            vg.addView(box, lp);
            Ids.setActive(Ids.PDFVIEW, fid, true);
            PdfViewCreated(true, fid, "");
          } catch (Exception e) {
            String m = e.getMessage();
            Ids.setActive(Ids.PDFVIEW, id, false);
            Ids.unregister(Ids.PDFVIEW, id);
            Shared.err(Tools_PdfView.this, "PdfView", id,
                m == null ? "gagal tampil" : m);
          }
        }
      });
    } catch (Exception e) {
      String m = e.getMessage();
      Ids.setActive(Ids.PDFVIEW, id, false);
      Ids.unregister(Ids.PDFVIEW, id);
      Shared.err(this, "PdfView", id, m == null ? "gagal tampil" : m);
    }
  }

  @SimpleFunction(description = "SET: hapus viewer ber-id (tutup dokumen + lepas "
      + "tampilan + lepas id). Menembak PdfViewClosed.")
  public void RemovePdfView(int id) {
    Holder h = holderOf(id);
    if (h == null) {
      Shared.err(this, "PdfView", id, "id tidak ada");
      final int fid = id;
      fireClosed(false, fid, "id tidak ada");
      return;
    }
    final int fid = id;
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          closeHolder(fid, true);
          fireClosed(true, fid, "");
        }
      });
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "PdfView", id, m == null ? "gagal hapus" : m);
    }
  }

  private void fireClosed(final boolean success, final int id,
      final String msg) {
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          PdfViewClosed(success, id, msg);
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "SET: hapus SEMUA viewer PdfView "
      + "(id ikut dilepas). Menembak PdfViewClosed satu per satu.")
  public void SetClearPdfViews() {
    ArrayList<Integer> all;
    synchronized (viewLock) {
      all = new ArrayList<Integer>(holders.keySet());
    }
    for (int i = 0; i < all.size(); i = i + 1) {
      RemovePdfView(all.get(i).intValue());
    }
  }

  @SimpleFunction(description = "Semua id PdfView (URUT PEMBUATAN, index 1 = "
      + "pertama dibuat). Number semua.")
  public List<Integer> GetPdfViewIds() {
    return Ids.idsOf(Ids.PDFVIEW);
  }

  @SimpleFunction(description = "Id PdfView terakhir dibuat/ditampil. -1 bila "
      + "kosong. Number.")
  public int CurrentPdfViewId() {
    return Ids.currentOf(Ids.PDFVIEW);
  }

  @SimpleFunction(description = "Id index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstPdfViewId() {
    return Ids.firstOf(Ids.PDFVIEW);
  }

  @SimpleFunction(description = "Id TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastPdfViewId() {
    return Ids.lastOf(Ids.PDFVIEW);
  }

  @SimpleFunction(description = "Id yang dibuat TEPAT SEBELUM id ini. -1 bila "
      + "tidak ada. Number.")
  public int GetPreviousPdfViewId(int id) {
    return Ids.previousOf(Ids.PDFVIEW, id);
  }

  @SimpleFunction(description = "True bila id viewer ini masih ada. Number.")
  public boolean IsPdfViewIdActive(int id) {
    return Ids.isActive(Ids.PDFVIEW, id);
  }

  // ================= blok: dokumen + navigasi + zoom =================

  @SimpleFunction(description = "CALL: buka PDF ke viewer ber-id (path / //asset "
      + "/ file:; content:/http: ditolak, unduh dulu). Mulai di halaman 1, "
      + "zoom 100. Hasil lewat PdfOpened(id...).")
  public void OpenPdf(final int id, final String path) {
    final Holder h = holderOf(id);
    if (h == null) {
      Shared.err(this, "PdfView", id, "id tidak ada");
      return;
    }
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        Holder hh = holderOf(id);
        if (hh == null) {
          return;
        }
        String p = Shared.clean(path);
        File src = null;
        File cache = null;
        if (wantAsset(p)) {
          src = Shared.assetToCache(context, p);
          cache = src;
          if (src == null) {
            fireOpened(false, id, 0, "aset tidak ditemukan");
            Shared.err(Tools_PdfView.this, "PdfView", id,
                "aset tidak ditemukan");
            return;
          }
        } else {
          src = rootedFile(p);
          if (src == null || !src.isFile()) {
            fireOpened(false, id, 0, "file tidak ada");
            Shared.err(Tools_PdfView.this, "PdfView", id, "file tidak ada");
            return;
          }
        }
        ParcelFileDescriptor fd = null;
        PdfRenderer r = null;
        try {
          fd = ParcelFileDescriptor.open(src,
              ParcelFileDescriptor.MODE_READ_ONLY);
          r = new PdfRenderer(fd);
        } catch (Exception e) {
          try {
            if (fd != null) {
              fd.close();
            }
          } catch (Exception e2) {
            // abaikan
          }
          if (cache != null) {
            try {
              cache.delete();
            } catch (Exception e2) {
              // abaikan
            }
          }
          String m = e.getMessage();
          if (m == null || m.length() == 0) {
            m = "gagal buka PDF (kunci/korup?)";
          }
          fireOpened(false, id, 0, m);
          Shared.err(Tools_PdfView.this, "PdfView", id, m);
          return;
        }
        Holder h2 = holderOf(id);
        if (h2 == null) {
          try {
            r.close();
          } catch (Exception e) {
            // abaikan
          }
          try {
            fd.close();
          } catch (Exception e) {
            // abaikan
          }
          return;
        }
        closeRenderer(h2);
        synchronized (viewLock) {
          h2.fd = fd;
          h2.renderer = r;
          h2.cacheFile = cache;
          h2.pageCount = r.getPageCount();
          h2.curPage = h2.pageCount > 0 ? 1 : 0;
          h2.path = Shared.clean(path);
          h2.zoom = 100;
        }
        Ids.setActive(Ids.PDFVIEW, id, true);
        if (h2.pageCount < 1) {
          fireOpened(false, id, 0, "PDF kosong");
          Shared.err(Tools_PdfView.this, "PdfView", id, "PDF kosong");
          return;
        }
        renderPage(id, false);
        fireOpened(true, id, h2.pageCount, "");
      }
    });
  }

  private void fireOpened(final boolean success, final int id,
      final int count, final String msg) {
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          PdfOpened(success, id, count, msg);
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "SET: tutup dokumen di viewer ber-id (tampilan "
      + "dikosongkan, viewer tetap ada). Tanpa event.")
  public void ClosePdf(int id) {
    Holder h = holderOf(id);
    if (h == null) {
      Shared.err(this, "PdfView", id, "id tidak ada");
      return;
    }
    closeRenderer(h);
    final ImageView iv = h.page;
    final Bitmap old;
    synchronized (viewLock) {
      old = h.bitmap;
      h.bitmap = null;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            iv.setImageBitmap(null);
          } catch (Exception e) {
            // abaikan
          }
          if (old != null) {
            try {
              if (!old.isRecycled()) {
                old.recycle();
              }
            } catch (Exception e) {
              // abaikan
            }
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "True bila viewer ber-id sedang membuka dokumen.")
  public boolean IsPdfOpen(int id) {
    Holder h = holderOf(id);
    return h != null && h.renderer != null && h.pageCount > 0;
  }

  @SimpleFunction(description = "Jumlah halaman dokumen di viewer ber-id. "
      + "0 bila belum buka.")
  public int GetPageCount(int id) {
    Holder h = holderOf(id);
    return h == null ? 0 : h.pageCount;
  }

  @SimpleFunction(description = "Halaman aktif viewer ber-id (1-based). "
      + "0 bila belum buka.")
  public int GetCurrentPage(int id) {
    Holder h = holderOf(id);
    return h == null ? 0 : h.curPage;
  }

  @SimpleProperty(description = "SET: halaman berikut (swipe kiri). Tanpa "
      + "feedback; pindah via PageChanged.")
  public void NextPage(int id) {
    Holder h = holderOf(id);
    if (h == null || h.renderer == null) {
      return;
    }
    goTo(id, h.curPage + 1);
  }

  @SimpleProperty(description = "SET: halaman sebelum (swipe kanan). Tanpa "
      + "feedback; pindah via PageChanged.")
  public void PrevPage(int id) {
    Holder h = holderOf(id);
    if (h == null || h.renderer == null) {
      return;
    }
    goTo(id, h.curPage - 1);
  }

  @SimpleFunction(description = "CALL: lompat ke halaman page (1-based, dijepit "
      + "1..pageCount). Pindah via PageChanged.")
  public void GoToPage(int id, int page) {
    goTo(id, page);
  }

  @SimpleFunction(description = "SET zoom viewer ber-id (persen, 50..400; "
      + "100 = pas-lebar). Render ulang tanpa PageChanged. Diabaikan bila "
      + "zoom dimatikan.")
  public void SetZoom(int id, int percent) {
    Holder h = holderOf(id);
    if (h == null || h.renderer == null) {
      return;
    }
    if (!h.zoomEnabled) {
      return;
    }
    int nz = clampZoom(percent);
    synchronized (viewLock) {
      if (h.zoom == nz) {
        return;
      }
      h.zoom = nz;
    }
    renderPage(id, false);
  }

  @SimpleFunction(description = "Zoom viewer ber-id sekarang (persen).")
  public int GetZoom(int id) {
    Holder h = holderOf(id);
    return h == null ? 100 : h.zoom;
  }

  @SimpleFunction(description = "SET: boleh zoom atau tidak (pinch + SetZoom). "
      + "Dimatikan = zoom dikunci 100.")
  public void SetZoomEnabled(int id, boolean enabled) {
    Holder h = holderOf(id);
    if (h == null) {
      return;
    }
    synchronized (viewLock) {
      h.zoomEnabled = enabled;
      if (!enabled) {
        h.zoom = 100;
      }
    }
    if (h.renderer != null) {
      renderPage(id, false);
    }
  }

  @SimpleFunction(description = "True bila zoom viewer ber-id dibolehkan.")
  public boolean IsZoomEnabled(int id) {
    Holder h = holderOf(id);
    return h != null && h.zoomEnabled;
  }

  // ================= event =================

  @SimpleEvent(description = "Viewer ber-id jadi (boleh OpenPdf). Gagal "
      + "(wadah/id tak valid) = success false + pesan.")
  public void PdfViewCreated(boolean success, int id, String message) {
    Shared.fire(this, "PdfViewCreated", Boolean.valueOf(success),
        Integer.valueOf(id), message);
  }

  @SimpleEvent(description = "Dokumen kebuka di viewer ber-id: success + id + "
      + "jumlah halaman + pesan.")
  public void PdfOpened(boolean success, int id, int pageCount,
      String message) {
    Shared.fire(this, "PdfOpened", Boolean.valueOf(success),
        Integer.valueOf(id), Integer.valueOf(pageCount), message);
  }

  @SimpleEvent(description = "Halaman aktif pindah (blok/swipe): id + halaman "
      + "1-based + total halaman. Untuk label '3/20'.")
  public void PageChanged(int id, int page, int pageCount) {
    Shared.fire(this, "PageChanged", Integer.valueOf(id),
        Integer.valueOf(page), Integer.valueOf(pageCount));
  }

  @SimpleEvent(description = "Viewer ber-id dihapus (Remove/Clear): success + "
      + "id + pesan.")
  public void PdfViewClosed(boolean success, int id, String message) {
    Shared.fire(this, "PdfViewClosed", Boolean.valueOf(success),
        Integer.valueOf(id), message);
  }
}
