package com.fv7.tools;

import android.app.Activity;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@DesignerComponent(
    version = 21,
    description = "Tools_Sheet: bottom-sheet dialog ber-id (pakai Arrangement "
        + "sebagai konten), toast, dan notifikasi bar status Android. Sheet: "
        + "RegisterSheet (+SetSheetStyle) / ShowSimpleSheet -> CallShowSheet / "
        + "CallDismissSheet + event SheetHidden, animasi geser otomatis. Notifikasi: "
        + "channel otomatis, butuh izin POST_NOTIFICATIONS di Android 13+ (HANYA "
        + "notifikasi; toast/sheet TIDAK butuh izin ini; pakai "
        + "SetRequestNotificationPermission atau izinkan manual). Id dikelola "
        + "CreateSheetId (notifikasi: CreateNotificationId). List id URUT "
        + "PEMBUATAN. Konvensi awalan: Is = boolean (colok ke if), Call = async + "
        + "hasil lewat event, Set = tanpa feedback sukses (gagal via ErrorOccurred), "
        + "Get = nilai balik sinkron. Blok kuning = pilihan tetap (colok ke soket "
        + "yang cocok).",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    androidMinSdk = 21,
    iconName = "aiwebres/tools_sheet.jpg",
    helpUrl = "https://github.com/VB-7/MiT-File_Manager")
@SimpleObject(external = true)
@UsesPermissions({"android.permission.POST_NOTIFICATIONS"})
public class Tools_Sheet extends AndroidNonvisibleComponent
    implements OnDestroyListener {

  private static final String CHANNEL_ID = "tools_sheet_channel";

  private final Activity activity;
  private final Context context;
  private final float density;

  private static final class Sheet {
    Dialog dialog;
    View content;
    ViewGroup parent;
    int index;
    ViewGroup.LayoutParams lp;
    boolean silent;
    int gravity;
    String animType;
    long animMs;
    boolean swipe;
  }

  private static final Map<Integer, Sheet> sheets =
      new HashMap<Integer, Sheet>();
  private static final Object sheetLock = new Object();
  private static final Set<Integer> notifIds = new HashSet<Integer>();

  public Tools_Sheet(ComponentContainer container) {
    super(container.$form());
    this.activity = container.$context();
    this.context = container.$context();
    float d = 1.0f;
    try {
      d = context.getResources().getDisplayMetrics().density;
    } catch (Exception e) {
      // abaikan
    }
    this.density = d <= 0 ? 1.0f : d;
  }

  private int dp(float v) {
    return Math.round(v * density);
  }

  private static void restore(Sheet s) {
    try {
      if (s.content != null && s.parent != null) {
        if (s.content.getParent() instanceof ViewGroup) {
          ((ViewGroup) s.content.getParent()).removeView(s.content);
        }
        ViewGroup.LayoutParams use = s.lp;
        if (use == null) {
          use = new ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        if (s.index >= 0 && s.index <= s.parent.getChildCount()) {
          s.parent.addView(s.content, s.index, use);
        } else {
          s.parent.addView(s.content, use);
        }
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  private static void forget(int id) {
    synchronized (sheetLock) {
      sheets.remove(Integer.valueOf(id));
    }
    Ids.setActive(Ids.SHEET, id, false);
    Ids.unregister(Ids.SHEET, id);
  }

  @SimpleFunction(description = "Daftarkan bottom-sheet ber-id dengan gaya BAKU "
      + "(bawah, lebar penuh, dim 50, geser-bawah 250ms, bisa tutup back/luar, "
      + "geser-tutup aktif). content = Arrangement (dipindah ke dialog saat "
      + "tampil, dikembalikan saat tutup). Gaya cukup = langsung CallShowSheet; "
      + "mau gaya lain = SetSheetStyle dulu. Sukses tanpa event; gagal via "
      + "ErrorOccurred.")
  public void RegisterSheet(final int id, final AndroidViewComponent content) {
    doRegister(id, content, 80, 0, 0, 50, "slide_bottom", 250, true, true);
  }

  @SimpleFunction(description = "SET: ubah gaya sheet TERDAFTAR (boleh sebelum/sesudah "
      + "tampil; langsung kelihatan bila sedang tampil). gravity angka: 80=bawah, "
      + "48=atas, 17=tengah, 3=kiri, 5=kanan. widthPx/heightPx <= 0 = menyesuaikan "
      + "(lebar penuh bila gravity atas/bawah). dimPercent 0-100 (kegelapan latar). "
      + "animationType teks: pakai blok kuning SlideFromBottom/SlideFromTop/"
      + "SlideFromLeft/SlideFromRight/FadeAnim/NoAnim "
      + "(salah ketik = slide-bawah). animationDurationMs 0 = 250. "
      + "cancelable = bisa tutup via tombol back/luar. swipeToDismiss = geser "
      + "ke arah tutup untuk menutup. Sukses diam; id belum register via "
      + "ErrorOccurred.")
  public void SetSheetStyle(final int id, final int gravity, final int widthPx,
      final int heightPx, final int dimPercent, final String animationType,
      final int animationDurationMs, final boolean cancelable,
      final boolean swipeToDismiss) {
    final Sheet s = need(id, "Style");
    if (s == null) {
      return;
    }
    if (gravity == 48 || gravity == 17 || gravity == 3 || gravity == 5) {
      s.gravity = gravity;
    } else {
      s.gravity = 80;
    }
    String at = animationType == null ? "" : animationType.trim().toLowerCase();
    if (!"slide_top".equals(at) && !"slide_left".equals(at)
        && !"slide_right".equals(at) && !"fade".equals(at)
        && !"none".equals(at)) {
      at = "slide_bottom";
    }
    s.animType = at;
    s.animMs = animationDurationMs > 0 ? animationDurationMs : 250;
    s.swipe = swipeToDismiss;
    final boolean fCancel = cancelable;
    final int fW = widthPx;
    final int fH = heightPx;
    final int fDim = Math.max(0, Math.min(100, dimPercent));
    try {
      activity.runOnUiThread(new Runnable() {
        public void run() {
          try {
            Dialog d = s.dialog;
            if (d == null) {
              return;
            }
            d.setCancelable(fCancel);
            d.setCanceledOnTouchOutside(fCancel);
            Window w = d.getWindow();
            if (w != null) {
              w.setGravity(s.gravity);
              w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
              WindowManager.LayoutParams lp = w.getAttributes();
              lp.width = fW > 0 ? fW
                  : WindowManager.LayoutParams.MATCH_PARENT;
              lp.height = fH > 0 ? fH
                  : WindowManager.LayoutParams.WRAP_CONTENT;
              lp.dimAmount = fDim / 100.0f;
              w.setAttributes(lp);
            }
          } catch (Exception e) {
            String m = e.getMessage();
            Shared.err(Tools_Sheet.this, "Sheet", id,
                m == null ? "style gagal" : m);
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "CALL: tampilkan sheet SEDERHANA dari Arrangement "
      + "dalam 1 blok (tanpa Register dulu): gaya baku bawah + full-width + "
      + "dim 50 + geser-bawah, id dibuat otomatis. Kembali: id sheet (tampil via "
      + "event SheetShown; tutup via CallDismissSheet; -1 bila gagal).")
  public int ShowSimpleSheet(final AndroidViewComponent content) {
    int id = Ids.createFor(Ids.SHEET);
    if (id < 1) {
      Shared.err(this, "Sheet", 0, "gagal buat id");
      return -1;
    }
    doRegister(id, content, 80, 0, 0, 50, "slide_bottom", 250, true, true);
    boolean ok;
    synchronized (sheetLock) {
      ok = sheets.containsKey(Integer.valueOf(id));
    }
    if (!ok) {
      return -1;
    }
    CallShowSheet(id);
    return id;
  }

  /** Daftarkan sheet dengan gaya eksplisit (dipakai RegisterSheet/ShowSimpleSheet). */
  private void doRegister(final int id, final AndroidViewComponent content,
      final int gravity, final int widthPx, final int heightPx,
      final int dimPercent, final String animationType,
      final int animationDurationMs, final boolean cancelable,
      final boolean swipeToDismiss) {
    if (content == null) {
      Shared.err(this, "Sheet", id, "konten tidak valid");
      return;
    }
    if (id < 1) {
      Shared.err(this, "Sheet", id, "id harus >= 1 (pakai CreateSheetId)");
      return;
    }
    if (!Ids.register(Ids.SHEET, id)) {
      Shared.err(this, "Sheet", id, "ID " + id + " sedang dipakai sheet lain");
      return;
    }
    synchronized (sheetLock) {
      if (sheets.containsKey(Integer.valueOf(id))) {
        Shared.err(this, "Sheet", id, "ID " + id + " sudah terdaftar "
            + "(Remove dulu)");
        return;
      }
    }
    final int g;
    if (gravity == 48 || gravity == 17 || gravity == 3 || gravity == 5) {
      g = gravity;
    } else {
      g = 80;
    }
    String at = animationType == null ? "" : animationType.trim().toLowerCase();
    if (!"slide_top".equals(at) && !"slide_left".equals(at)
        && !"slide_right".equals(at) && !"fade".equals(at)
        && !"none".equals(at)) {
      at = "slide_bottom";
    }
    final String fAt = at;
    final long fMs = animationDurationMs > 0 ? animationDurationMs : 250;
    try {
      final View v = content.getView();
      if (v == null) {
        Ids.setActive(Ids.SHEET, id, false);
        Ids.unregister(Ids.SHEET, id);
        Shared.err(this, "Sheet", id, "view konten tidak ada");
        return;
      }
      final Sheet s = new Sheet();
      s.content = v;
      s.silent = false;
      s.gravity = g;
      s.animType = fAt;
      s.animMs = fMs;
      s.swipe = swipeToDismiss;
      try {
        if (v.getParent() instanceof ViewGroup) {
          s.parent = (ViewGroup) v.getParent();
          s.index = s.parent.indexOfChild(v);
          s.lp = v.getLayoutParams();
        }
      } catch (Exception e) {
        // abaikan
      }
      final Dialog d = new Dialog(activity);
      try {
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
      } catch (Exception e) {
        // abaikan
      }
      s.dialog = d;
      d.setCancelable(cancelable);
      d.setCanceledOnTouchOutside(cancelable);
      d.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
        public void onDismiss(android.content.DialogInterface di) {
          try {
            restore(s);
          } catch (Exception e) {
            // abaikan
          }
          if (s.silent) {
            forget(id);
          } else {
            Ids.setActive(Ids.SHEET, id, false);
            SheetHidden(id);
          }
        }
      });
      try {
        Window w = d.getWindow();
        if (w != null) {
          w.setGravity(g);
          int dim = Math.max(0, Math.min(100, dimPercent));
          w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
          WindowManager.LayoutParams lp = w.getAttributes();
          lp.width = widthPx > 0 ? widthPx
              : WindowManager.LayoutParams.MATCH_PARENT;
          lp.height = heightPx > 0 ? heightPx
              : WindowManager.LayoutParams.WRAP_CONTENT;
          lp.dimAmount = dim / 100.0f;
          w.setAttributes(lp);
          try {
            w.setBackgroundDrawableResource(android.R.color.transparent);
          } catch (Exception e) {
            // abaikan
          }
        }
      } catch (Exception e) {
        // abaikan
      }
      synchronized (sheetLock) {
        sheets.put(Integer.valueOf(id), s);
      }
      Ids.setActive(Ids.SHEET, id, true);
    } catch (Exception e) {
      Ids.setActive(Ids.SHEET, id, false);
      Ids.unregister(Ids.SHEET, id);
      String m = e.getMessage();
      Shared.err(this, "Sheet", id,
          m == null ? "register gagal" : "register gagal: " + m);
    }
  }

  private Sheet need(int id, String op) {
    Sheet s;
    synchronized (sheetLock) {
      s = sheets.get(Integer.valueOf(id));
    }
    if (s == null) {
      Shared.err(this, "Sheet", id, op + ": id belum diregister");
      return null;
    }
    return s;
  }

  private void slideIn(final Sheet s, final View v) {
    try {
      v.post(new Runnable() {
        public void run() {
          try {
            if ("none".equals(s.animType)) {
              return;
            }
            if ("fade".equals(s.animType)) {
              v.setAlpha(0.0f);
              v.animate().alpha(1.0f).setDuration(s.animMs).start();
              return;
            }
            int w = v.getWidth();
            int h = v.getHeight();
            if (w <= 0) {
              w = dp(300);
            }
            if (h <= 0) {
              h = dp(300);
            }
            if ("slide_top".equals(s.animType)) {
              v.setTranslationY(-h);
            } else if ("slide_left".equals(s.animType)) {
              v.setTranslationX(-w);
            } else if ("slide_right".equals(s.animType)) {
              v.setTranslationX(w);
            } else {
              v.setTranslationY(h);
            }
            v.animate().translationX(0).translationY(0)
                .setDuration(s.animMs).start();
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  private void slideOut(final Sheet s, final View v, final Runnable after) {
    try {
      if (s.dialog == null || !s.dialog.isShowing()
          || "none".equals(s.animType)) {
        after.run();
        return;
      }
      if ("fade".equals(s.animType)) {
        v.animate().alpha(0.0f).setDuration(Math.min(s.animMs, 200))
            .withEndAction(after).start();
        return;
      }
      int w = v.getWidth();
      int h = v.getHeight();
      if (w <= 0) {
        w = dp(300);
      }
      if (h <= 0) {
        h = dp(300);
      }
      float tx = 0;
      float ty = 0;
      if ("slide_top".equals(s.animType)) {
        ty = -h;
      } else if ("slide_left".equals(s.animType)) {
        tx = -w;
      } else if ("slide_right".equals(s.animType)) {
        tx = w;
      } else {
        ty = h;
      }
      final float fTx = tx;
      final float fTy = ty;
      v.post(new Runnable() {
        public void run() {
          try {
            v.animate().translationX(fTx).translationY(fTy)
                .setDuration(Math.min(s.animMs, 200)).withEndAction(after)
                .start();
          } catch (Exception e) {
            after.run();
          }
        }
      });
    } catch (Exception e) {
      after.run();
    }
  }

  private void wireSwipe(final int id, final Sheet s, final View box) {
    if (!s.swipe) {
      return;
    }
    try {
      box.setOnTouchListener(new View.OnTouchListener() {
        float dx = 0;
        float dy = 0;
        boolean track = false;
        public boolean onTouch(View vv, MotionEvent e) {
          int a = e.getActionMasked();
          if (a == MotionEvent.ACTION_DOWN) {
            dx = e.getRawX();
            dy = e.getRawY();
            track = true;
          } else if (a == MotionEvent.ACTION_UP && track) {
            track = false;
            try {
              float ddx = e.getRawX() - dx;
              float ddy = e.getRawY() - dy;
              int w = box.getWidth();
              int h = box.getHeight();
              boolean go = false;
              if (s.gravity == 48) {
                go = -ddy > h / 3.0f;
              } else if (s.gravity == 3) {
                go = -ddx > w / 3.0f;
              } else if (s.gravity == 5) {
                go = ddx > w / 3.0f;
              } else if (s.gravity == 17) {
                go = Math.abs(ddx) + Math.abs(ddy) > Math.max(w, h) / 2.0f;
              } else {
                go = ddy > h / 3.0f;
              }
              if (go) {
                CallDismissSheet(id);
                return true;
              }
            } catch (Exception ex) {
              // abaikan
            }
          } else if (a == MotionEvent.ACTION_CANCEL) {
            track = false;
          }
          return false;
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "CALL: Tampilkan sheet ber-id (animasi geser naik "
      + "otomatis). Hasil lewat SheetShown.")
  public void CallShowSheet(final int id) {
    final Sheet s = need(id, "Show");
    if (s == null) {
      return;
    }
    try {
      activity.runOnUiThread(new Runnable() {
        public void run() {
          try {
            if (s.content.getParent() instanceof ViewGroup) {
              ((ViewGroup) s.content.getParent()).removeView(s.content);
            }
            FrameLayout box = new FrameLayout(activity);
            box.addView(s.content, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));
            s.dialog.setContentView(box);
            s.dialog.show();
            wireSwipe(id, s, box);
            slideIn(s, box);
            Ids.setActive(Ids.SHEET, id, true);
            SheetShown(id);
          } catch (Exception e) {
            String m = e.getMessage();
            Shared.err(Tools_Sheet.this, "Sheet", id,
                m == null ? "show gagal" : "show gagal: " + m);
          }
        }
      });
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Sheet", id, m == null ? "show gagal" : m);
    }
  }

  @SimpleFunction(description = "CALL: Tutup sheet ber-id (animasi keluar). "
      + "Hasil lewat SheetHidden. Dismiss = tutup TAPI registrasi tetap (bisa "
      + "CallShowSheet lagi); beda dengan RemoveSheetId (tutup + id dibuang) dan "
      + "SetClearSheets (buang semua).")
  public void CallDismissSheet(final int id) {
    final Sheet s = need(id, "Dismiss");
    if (s == null) {
      return;
    }
    try {
      activity.runOnUiThread(new Runnable() {
        public void run() {
          try {
            if (s.dialog == null || !s.dialog.isShowing()) {
              Ids.setActive(Ids.SHEET, id, false);
              SheetHidden(id);
              return;
            }
            View dec = null;
            try {
              Window w = s.dialog.getWindow();
              dec = w == null ? null : w.getDecorView();
            } catch (Exception e) {
              // abaikan
            }
            final Runnable fin = new Runnable() {
              public void run() {
                try {
                  s.dialog.dismiss();
                } catch (Exception e) {
                  // abaikan
                }
              }
            };
            if (dec != null) {
              slideOut(s, dec, fin);
            } else {
              fin.run();
            }
          } catch (Exception e) {
            String m = e.getMessage();
            Shared.err(Tools_Sheet.this, "Sheet", id,
                m == null ? "dismiss gagal" : "dismiss gagal: " + m);
          }
        }
      });
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Sheet", id, m == null ? "dismiss gagal" : m);
    }
  }

  @SimpleFunction(description = "True bila sheet ber-id sedang tampil. Kontras: "
      + "baru Register tapi belum Show -> IsSheetIdActive=true, IsSheetShowing=false.")
  public boolean IsSheetShowing(int id) {
    Sheet s;
    synchronized (sheetLock) {
      s = sheets.get(Integer.valueOf(id));
    }
    if (s == null || s.dialog == null) {
      return false;
    }
    try {
      return s.dialog.isShowing();
    } catch (Exception e) {
      return false;
    }
  }

  /** Tutup diam-diam + lepas id. Dipakai Tools_Base dan cascade. */
  public static void clearStatic(int id) {
    final Sheet s;
    synchronized (sheetLock) {
      s = sheets.get(Integer.valueOf(id));
    }
    if (s == null) {
      Ids.setActive(Ids.SHEET, id, false);
      Ids.unregister(Ids.SHEET, id);
      return;
    }
    s.silent = true;
    try {
      if (s.dialog != null && s.dialog.isShowing()) {
        s.dialog.dismiss();
      } else {
        restore(s);
        forget(id);
      }
    } catch (Exception e) {
      restore(s);
      forget(id);
    }
  }

  @SimpleFunction(description = "Hapus sheet ber-id: tutup diam-diam (tanpa "
      + "SheetHidden), kembalikan konten ke layout asal, lepas id. Remove = tutup "
      + "+ id dibuang; beda dengan CallDismissSheet (registrasi tetap) dan "
      + "SetClearSheets (buang semua).")
  public void RemoveSheetId(final int id) {
    try {
      activity.runOnUiThread(new Runnable() {
        public void run() {
          clearStatic(id);
        }
      });
    } catch (Exception e) {
      clearStatic(id);
    }
  }

  // ================= TOAST =================

  @SimpleFunction(description = "CALL: tampilkan toast (pesan singkat bawah layar). "
      + "longDuration = tampil lebih lama. Sukses menembak ToastShown; gagal via "
      + "ErrorOccurred. Toast TIDAK butuh izin notifikasi.")
  public void CallShowToast(final String message, final boolean longDuration) {
    final String m = message == null ? "" : message;
    try {
      activity.runOnUiThread(new Runnable() {
        public void run() {
          try {
            Toast.makeText(activity, m, longDuration ? Toast.LENGTH_LONG
                : Toast.LENGTH_SHORT).show();
            ToastShown();
          } catch (Exception e) {
            String em = e.getMessage();
            Shared.err(Tools_Sheet.this, "Toast", 0,
                em == null ? "toast gagal" : em);
          }
        }
      });
    } catch (Exception e) {
      String em = e.getMessage();
      Shared.err(this, "Toast", 0, em == null ? "toast gagal" : em);
    }
  }

  // ================= NOTIFIKASI =================

  private void ensureChannel() {
    try {
      if (Build.VERSION.SDK_INT >= 26) {
        NotificationManager nm = (NotificationManager) context
            .getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
          try {
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
              NotificationChannel ch = new NotificationChannel(CHANNEL_ID,
                  "Tools", NotificationManager.IMPORTANCE_DEFAULT);
              nm.createNotificationChannel(ch);
            }
          } catch (Exception e) {
            // abaikan
          }
        }
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "SET: Minta izin notifikasi (Android 13+). "
      + "Best-effort: bila ditolak, izinkan manual di Setelan aplikasi.")
  public void SetRequestNotificationPermission() {
    try {
      if (Build.VERSION.SDK_INT >= 33) {
        activity.requestPermissions(
            new String[] { "android.permission.POST_NOTIFICATIONS" }, 9001);
      }
    } catch (Exception e) {
      String m = e.getMessage();
    }
  }

  @SimpleFunction(description = "Buat id notifikasi unik baru (Number; minta ke sini, "
      + "JANGAN angka manual — id dobel = notifikasi lama tertimpa).")
  public int CreateNotificationId() {
    synchronized (sheetLock) {
      int id = 1;
      while (notifIds.contains(Integer.valueOf(id))) {
        id = id + 1;
        if (id < 1) {
          return -1;
        }
      }
      notifIds.add(Integer.valueOf(id));
      return id;
    }
  }

  @SimpleFunction(description = "CALL: tampilkan notifikasi bar status: id (minta ke "
      + "CreateNotificationId; manual boleh tapi dobel = tertimpa), judul, isi. "
      + "Tap notifikasi membuka aplikasi. Hanya NOTIFIKASI yang butuh izin "
      + "POST_NOTIFICATIONS (toast/sheet tidak). Sukses menembak NotificationShown; "
      + "gagal (mis. izin belum ada) via ErrorOccurred.")
  public void CallSetNotification(final int id, final String title,
      final String text) {
    ensureChannel();
    final String t = title == null ? "" : title;
    final String x = text == null ? "" : text;
    try {
      activity.runOnUiThread(new Runnable() {
        public void run() {
          try {
            int icon = android.R.drawable.ic_dialog_info;
            try {
              int appIcon = context.getApplicationInfo().icon;
              if (appIcon != 0) {
                icon = appIcon;
              }
            } catch (Exception e) {
              // pakai bawaan
            }
            Notification.Builder b;
            if (Build.VERSION.SDK_INT >= 26) {
              b = new Notification.Builder(activity, CHANNEL_ID);
            } else {
              b = new Notification.Builder(activity);
            }
            b.setContentTitle(t).setContentText(x).setSmallIcon(icon)
                .setAutoCancel(true);
            try {
              Intent launch = context.getPackageManager()
                  .getLaunchIntentForPackage(context.getPackageName());
              if (launch != null) {
                int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                if (Build.VERSION.SDK_INT >= 23) {
                  flags = flags | PendingIntent.FLAG_IMMUTABLE;
                }
                PendingIntent pi = PendingIntent.getActivity(activity, id,
                    launch, flags);
                b.setContentIntent(pi);
              }
            } catch (Exception e) {
              // tanpa tap-action
            }
            NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) {
              Shared.err(Tools_Sheet.this, "Notification", id,
                  "layanan notifikasi tidak ada");
              return;
            }
            if (Build.VERSION.SDK_INT >= 16) {
              nm.notify(id, b.build());
            } else {
              nm.notify(id, b.getNotification());
            }
            synchronized (sheetLock) {
              notifIds.add(Integer.valueOf(id));
            }
            NotificationShown(id);
          } catch (SecurityException se) {
            Shared.err(Tools_Sheet.this, "Notification", id,
                "izin notifikasi belum diberikan");
          } catch (Exception e) {
            String m = e.getMessage();
            Shared.err(Tools_Sheet.this, "Notification", id,
                m == null ? "notifikasi gagal" : m);
          }
        }
      });
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Notification", id, m == null ? "notifikasi gagal" : m);
    }
  }

  @SimpleFunction(description = "SET: Hapus notifikasi ber-id.")
  public void SetCancelNotification(int id) {
    try {
      NotificationManager nm = (NotificationManager) context
          .getSystemService(Context.NOTIFICATION_SERVICE);
      if (nm != null) {
        nm.cancel(id);
      }
      synchronized (sheetLock) {
        notifIds.remove(Integer.valueOf(id));
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "SET: Hapus SEMUA notifikasi Tools_Sheet.")
  public void SetCancelAllNotifications() {
    try {
      NotificationManager nm = (NotificationManager) context
          .getSystemService(Context.NOTIFICATION_SERVICE);
      if (nm != null) {
        nm.cancelAll();
      }
      synchronized (sheetLock) {
        notifIds.clear();
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "Tipe animasi: geser dari bawah.",
      category = PropertyCategory.BEHAVIOR)
  public String SlideFromBottom() {
    return "slide_bottom";
  }

  @SimpleProperty(description = "Tipe animasi: geser dari atas.",
      category = PropertyCategory.BEHAVIOR)
  public String SlideFromTop() {
    return "slide_top";
  }

  @SimpleProperty(description = "Tipe animasi: geser dari kiri.",
      category = PropertyCategory.BEHAVIOR)
  public String SlideFromLeft() {
    return "slide_left";
  }

  @SimpleProperty(description = "Tipe animasi: geser dari kanan.",
      category = PropertyCategory.BEHAVIOR)
  public String SlideFromRight() {
    return "slide_right";
  }

  @SimpleProperty(description = "Tipe animasi: fade.",
      category = PropertyCategory.BEHAVIOR)
  public String FadeAnim() {
    return "fade";
  }

  @SimpleProperty(description = "Tipe animasi: tanpa animasi.",
      category = PropertyCategory.BEHAVIOR)
  public String NoAnim() {
    return "none";
  }

  // ================= ID =================

  @SimpleFunction(description = "Buat id sheet unik baru (Number, siap pakai "
      + "ke RegisterSheet). 90% kasus cukup 2 blok id: CreateSheetId + RemoveSheetId.")
  public int CreateSheetId() {
    int id = Ids.createFor(Ids.SHEET);
    if (id < 1) {
      Shared.err(this, "Sheet", 0, "gagal buat id");
    }
    return id;
  }

  @SimpleFunction(description = "Tinggi jendela sheet ber-id (pixel; -2 = isi penuh, "
      + "-1 = otomatis/belum tampil). -1 juga bila sheet tidak ada. Isinya = "
      + "Arrangement yang kamu daftarkan (pegang referensinya di variabel; "
      + "tidak ada blok pengambil karena komponen MIT tidak bisa dibungkus "
      + "jadi nilai). Pasangan SetSheetStyle.")
  public int GetSheetHeight(int id) {
    try {
      Sheet s;
      synchronized (sheetLock) {
        s = sheets.get(Integer.valueOf(id));
      }
      if (s == null || s.dialog == null) {
        return -1;
      }
      Window w = s.dialog.getWindow();
      if (w == null) {
        return -1;
      }
      return w.getAttributes().height;
    } catch (Exception e) {
      return -1;
    }
  }

  @SimpleFunction(description = "Semua id sheet yang terdaftar (URUT PEMBUATAN, "
      + "index 1 = pertama dibuat). Termasuk yang sedang tidak tampil; beda "
      + "dengan GetActiveSheetIds.")
  public List<Integer> GetSheetIds() {
    return Ids.idsOf(Ids.SHEET);
  }

  @SimpleFunction(description = "Id sheet yang sedang aktif/tampil (URUT PEMBUATAN). "
      + "Beda dengan GetSheetIds yang mencatat semua yang terdaftar. "
      + "Periksa 1 id via IsSheetIdActive.")
  public List<Integer> GetActiveSheetIds() {
    return Ids.activeIdsOf(Ids.SHEET);
  }

  @SimpleFunction(description = "Id sheet terakhir dibuat. -1 bila kosong.")
  public int CurrentSheetId() {
    return Ids.currentOf(Ids.SHEET);
  }

  @SimpleFunction(description = "Id index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstSheetId() {
    return Ids.firstOf(Ids.SHEET);
  }

  @SimpleFunction(description = "Id TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastSheetId() {
    return Ids.lastOf(Ids.SHEET);
  }

  @SimpleFunction(description = "Id yang dibuat TEPAT SEBELUM id ini. "
      + "-1 bila tidak ada.")
  public int GetPreviousSheetId(int id) {
    return Ids.previousOf(Ids.SHEET, id);
  }

  @SimpleFunction(description = "True bila id sheet ini masih terdaftar. Kontras: "
      + "baru Register tapi belum Show -> Active=true, Showing=false.")
  public boolean IsSheetIdActive(int id) {
    return Ids.isActive(Ids.SHEET, id);
  }

  @SimpleFunction(description = "SET: Hapus SEMUA sheet Tools_Sheet (tutup "
      + "diam-diam, konten dikembalikan, id dilepas). Clear = buang semua; beda "
      + "dengan CallDismissSheet (registrasi tetap) dan RemoveSheetId (satu id).")
  public void SetClearSheets() {
    ArrayList<Integer> all;
    synchronized (sheetLock) {
      all = new ArrayList<Integer>(sheets.keySet());
    }
    for (int i = 0; i < all.size(); i = i + 1) {
      RemoveSheetId(all.get(i).intValue());
    }
  }

  @SimpleFunction(description = "Fallback polling untuk event ErrorOccurred: "
      + "pesan error terakhir operasi ini (colok OpSheet/OpToast/OpNotification; "
      + "kosong bila belum ada).")
  public String LastError(String operation) {
    return Shared.lastError(operation);
  }

  /** Tutup diam-diam semua sheet yang kontennya ada di bawah root. */
  public static void clearByView(View root) {
    if (root == null) {
      return;
    }
    ArrayList<Integer> gone = new ArrayList<Integer>();
    synchronized (sheetLock) {
      for (Map.Entry<Integer, Sheet> en : sheets.entrySet()) {
        try {
          View v = en.getValue().content;
          if (v != null && (v == root || isUnder(v, root))) {
            gone.add(en.getKey());
          }
        } catch (Exception e) {
          // abaikan
        }
      }
    }
    for (int i = 0; i < gone.size(); i = i + 1) {
      int sid = gone.get(i).intValue();
      Sheet s;
      synchronized (sheetLock) {
        s = sheets.get(Integer.valueOf(sid));
      }
      if (s != null) {
        s.silent = true;
        try {
          if (s.dialog != null && s.dialog.isShowing()) {
            s.dialog.dismiss();
          } else {
            restore(s);
            forget(sid);
          }
        } catch (Exception e) {
          restore(s);
          forget(sid);
        }
      }
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

  // ================= EVENTS =================

  @SimpleEvent(description = "Sheet ber-id tertutup (back/luar/Dismiss): id. "
      + "Registrasi tetap, tampilkan lagi via CallShowSheet.")
  public void SheetHidden(int id) {
    Shared.fire(this, "SheetHidden", Integer.valueOf(id));
  }

  @SimpleEvent(description = "Sheet ber-id tampil (hasil CallShowSheet/ShowSimpleSheet): id.")
  public void SheetShown(int id) {
    Shared.fire(this, "SheetShown", Integer.valueOf(id));
  }

  @SimpleEvent(description = "Toast tampil (hasil CallShowToast).")
  public void ToastShown() {
    Shared.fire(this, "ToastShown");
  }

  @SimpleEvent(description = "Notifikasi tampil (hasil CallSetNotification): id.")
  public void NotificationShown(int id) {
    Shared.fire(this, "NotificationShown", Integer.valueOf(id));
  }

  @SimpleEvent(description = "SATU-SATUNYA error Tools_Sheet (Sheet, Toast, "
      + "Notification). component = nama operasi (bandingkan dengan blok "
      + "OpSheet/OpToast/OpNotification; cth: if component = OpToast berarti "
      + "error toast). id = id terkait (0 bila umum: toast/id-pool).")
  public void ErrorOccurred(String component, int id, String message) {
    Shared.fire(this, "ErrorOccurred",
        component, Integer.valueOf(id), message);
  }

  // ================= OP =================

  @SimpleProperty(description = "Nama operasi Sheet (untuk if di "
      + "ErrorOccurred: cth if component = OpSheet berarti error sheet).",
      category = PropertyCategory.BEHAVIOR)
  public String OpSheet() {
    return "Sheet";
  }

  @SimpleProperty(description = "Nama operasi Toast (untuk if di "
      + "ErrorOccurred: cth if component = OpToast berarti error toast).",
      category = PropertyCategory.BEHAVIOR)
  public String OpToast() {
    return "Toast";
  }

  @SimpleProperty(description = "Nama operasi Notification (untuk if di "
      + "ErrorOccurred: cth if component = OpNotification berarti error notifikasi).",
      category = PropertyCategory.BEHAVIOR)
  public String OpNotification() {
    return "Notification";
  }

  // ================= destroy =================

  public void onDestroy() {
    try {
      ArrayList<Integer> all;
      synchronized (sheetLock) {
        all = new ArrayList<Integer>(sheets.keySet());
      }
      for (int i = 0; i < all.size(); i = i + 1) {
        int sid = all.get(i).intValue();
        Sheet s;
        synchronized (sheetLock) {
          s = sheets.get(Integer.valueOf(sid));
        }
        if (s != null) {
          s.silent = true;
          try {
            if (s.dialog != null && s.dialog.isShowing()) {
              s.dialog.dismiss();
            } else {
              restore(s);
              forget(sid);
            }
          } catch (Exception e) {
            restore(s);
            forget(sid);
          }
        }
      }
    } catch (Exception e) {
      // abaikan
    }
  }
}
