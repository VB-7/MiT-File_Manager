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
import com.google.appinventor.components.runtime.PermissionResultHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@DesignerComponent(
    version = 23,
    description = "Tools_Sheet: bottom-sheet dialog ber-id (pakai Arrangement "
        + "sebagai konten), toast, dan notifikasi bar status Android. Sheet: "
        + "RegisterSheet (+SetSheetStyle) / ShowSimpleSheet -> CallShowSheet / "
        + "CallDismissSheet + event SheetHidden, animasi geser otomatis. Notifikasi: "
        + "channel otomatis, butuh izin POST_NOTIFICATIONS di Android 13+ (HANYA "
        + "notifikasi; toast/sheet TIDAK butuh izin ini; pakai "
        + "CallSetNotificationPermission atau izinkan manual). Id dikelola "
        + "CreateSheetId (notifikasi: CreateNotificationId). List id URUT "
        + "PEMBUATAN. Konvensi awalan: Is = boolean (colok ke if), Call = async + "
        + "hasil lewat event, Set = tanpa feedback sukses (gagal via ErrorOccurred), "
        + "Get = nilai balik sinkron. Pilihan tetap (colok ke soket "
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
      + "animationType teks: pakai blok SlideFromBottom/SlideFromTop/"
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
            Shared.err(Tools_Sheet.t