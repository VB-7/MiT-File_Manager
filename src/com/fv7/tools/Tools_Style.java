package com.fv7.tools;

import android.app.Activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;

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
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.YailList;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;

@DesignerComponent(
    version = 23,
    description = "Tools_Style: percantik komponen/wadah yang kelihatan "
        + "(Button, Label, Arrangement, gambar Tools_Image, komponen dinamis, "
        + "dsb). Radius, border, gradien, elevation, padding, margin, opacity, "
        + "background warna/gambar, plus animasi fade/slide/scale/rotation. "
        + "Tanpa id (stateless, langsung tempel ke komponen — TIDAK perlu "
        + "CreateId). Semua prosedur void; gagal via ErrorOccurred (id selalu "
        + "0). PERANG CAT (satu tembok, yang terakhir menang): radius+border = "
        + "AKUR (satu drawable, boleh digabung); warna polos menimpa "
        + "gradien/ripple; gradien menimpa warna; ripple menimpa SEMUA "
        + "background — pasang PALING AKHIR. Vs SetDynamicBackgroundColor "
        + "(Core, untuk komponen dinamis): dua-duanya menulis background view "
        + "— yang DIPANGGIL TERAKHIR menang. Konvensi awalan: Is = boolean "
        + "(colok ke if), Call = async + hasil lewat event, Set = tanpa "
        + "feedback sukses (gagal via ErrorOccurred), Get = nilai balik "
        + "sinkron. Pilihan tetap (colok ke soket yang cocok). "
        + "Kamus MakeViewUp: SetRadius = SetCornerRadius di sini; SetImage = "
        + "SetBackgroundImage; SetSpace = SetTextSpace.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    androidMinSdk = 21,
    iconName = "aiwebres/tools_style.jpg",
    helpUrl = "https://github.com/VB-7/MiT-File_Manager")
@SimpleObject(external = true)
@UsesPermissions({"android.permission.READ_EXTERNAL_STORAGE"})
public class Tools_Style extends AndroidNonvisibleComponent {

  private final Context context;
  private final Activity activity;

  public Tools_Style(ComponentContainer container) {
    super(container.$form());
    this.context = container.$context();
    this.activity = container.$context();
  }

  private View viewOf(AndroidViewComponent comp, String op) {
    if (comp == null) {
      Shared.err(this, "Style", 0, op + ": komponen tidak valid");
      return null;
    }
    try {
      View v = comp.getView();
      if (v == null) {
        Shared.err(this, "Style", 0, op + ": view tidak ada");
        return null;
      }
      return v;
    } catch (Exception e) {
      Shared.err(this, "Style", 0, op + ": view tidak bisa diakses");
      return null;
    }
  }

  private GradientDrawable currentBg(View v) {
    try {
      if (v.getBackground() instanceof GradientDrawable) {
        return (GradientDrawable) v.getBackground();
      }
    } catch (Exception e) {
      // abaikan
    }
    return new GradientDrawable();
  }

  @SimpleFunction(description = "SET: Atur sudut membulat (radius, pixel). Background "
      + "bawaan diganti.")
  public void SetCornerRadius(AndroidViewComponent comp, float radiusPx) {
    View v = viewOf(comp, "Radius");
    if (v == null) {
      return;
    }
    try {
      GradientDrawable g = currentBg(v);
      g.setCornerRadius(Math.max(0, Math.round(radiusPx)));
      v.setBackgroundDrawable(g);
    } catch (Exception e) {
    }
  }

  @SimpleFunction(description = "SET: Atur garis tepi: warna + tebal (pixel). "
      + "Background bawaan diganti.")
  public void SetBorder(AndroidViewComponent comp, int color, float widthPx) {
    View v = viewOf(comp, "Border");
    if (v == null) {
      return;
    }
    try {
      GradientDrawable g = currentBg(v);
      g.setStroke(Math.max(0, Math.round(widthPx)), color);
      v.setBackgroundDrawable(g);
    } catch (Exception e) {
    }
  }

  @SimpleFunction(description = "SET: Atur gradien banyak warna (list warna, "
      + "minimal 2). arah = colok OrientationTopBottom/dll; "
      + "angka 0-7 = cara lama (0=atas-bawah, 1=kananATAS-kiriBAWAH, "
      + "2=kanan-kiri, 3=kananBAWAH-kiriATAS, 4=bawah-atas, 5=kiriBAWAH-"
      + "kananATAS, 6=kiri-kanan, 7=kiriATAS-kananBAWAH). Background bawaan "
      + "diganti (menimpa warna polos).")
  public void SetGradient(AndroidViewComponent comp, Object colors,
      int orientation) {
    View v = viewOf(comp, "Gradient");
    if (v == null) {
      return;
    }
    if (orientation < 0 || orientation > 7) {
      Shared.err(this, "Style", 0,
          "Gradient: orientation 0-7 (pakai OrientationTopBottom/dll)");
      return;
    }
    ArrayList<Integer> list = new ArrayList<Integer>();
    try {
      if (colors instanceof YailList) {
        for (Object x : ((YailList) colors).toArray()) {
          if (x instanceof Number) {
            list.add(Integer.valueOf(((Number) x).intValue()));
          } else if (x != null) {
            try {
              list.add(Integer.valueOf(Integer.parseInt(x.toString().trim())));
            } catch (Exception e) {
              // abaikan item
            }
          }
        }
      } else if (colors instanceof Number) {
        list.add(Integer.valueOf(((Number) colors).intValue()));
      }
    } catch (Exception e) {
      // abaikan
    }
    if (list.size() < 2) {
      Shared.err(this, "Style", 0, "Gradient: butuh minimal 2 warna");
      return;
    }
    try {
      GradientDrawable g = currentBg(v);
      int[] arr = new int[list.size()];
      for (int i = 0; i < list.size(); i = i + 1) {
        arr[i] = list.get(i).intValue();
      }
      g.setColors(arr);
      GradientDrawable.Orientation[] all = {
          GradientDrawable.Orientation.TOP_BOTTOM,
          GradientDrawable.Orientation.TR_BL,
          GradientDrawable.Orientation.RIGHT_LEFT,
          GradientDrawable.Orientation.BR_TL,
          GradientDrawable.Orientation.BOTTOM_TOP,
          GradientDrawable.Orientation.BL_TR,
          GradientDrawable.Orientation.LEFT_RIGHT,
          GradientDrawable.Orientation.TL_BR
      };
      g.setOrientation(all[orientation]);
      v.setBackgroundDrawable(g);
    } catch (Exception e) {
    }
  }

  @SimpleProperty(description = "BLOK KUNING, arah gradien: atas-bawah (= 0).",
      category = PropertyCategory.BEHAVIOR)
  public int OrientationTopBottom() {
    return 0;
  }

  @SimpleProperty(description = "BLOK KUNING, arah gradien: kananATAS-"
      + "kiriBAWAH (= 1).",
      category = PropertyCategory.BEHAVIOR)
  public int OrientationTopRightBottomLeft() {
    return 1;
  }

  @SimpleProperty(description = "BLOK KUNING, arah gradien: kanan-kiri (= 2).",
      category = PropertyCategory.BEHAVIOR)
  public int OrientationRightLeft() {
    return 2;
  }

  @SimpleProperty(description = "BLOK KUNING, arah gradien: kananBAWAH-"
      + "kiriATAS (= 3).",
      category = PropertyCategory.BEHAVIOR)
  public int OrientationBo