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
  public int OrientationBottomRightTopLeft() {
    return 3;
  }

  @SimpleProperty(description = "BLOK KUNING, arah gradien: bawah-atas (= 4).",
      category = PropertyCategory.BEHAVIOR)
  public int OrientationBottomTop() {
    return 4;
  }

  @SimpleProperty(description = "BLOK KUNING, arah gradien: kiriBAWAH-"
      + "kananATAS (= 5).",
      category = PropertyCategory.BEHAVIOR)
  public int OrientationBottomLeftTopRight() {
    return 5;
  }

  @SimpleProperty(description = "BLOK KUNING, arah gradien: kiri-kanan (= 6).",
      category = PropertyCategory.BEHAVIOR)
  public int OrientationLeftRight() {
    return 6;
  }

  @SimpleProperty(description = "BLOK KUNING, arah gradien: kiriATAS-"
      + "kananBAWAH (= 7).",
      category = PropertyCategory.BEHAVIOR)
  public int OrientationTopLeftBottomRight() {
    return 7;
  }

  @SimpleFunction(description = "SET: Atur gradien radial: warna tengah, warna tepi, "
      + "radius (pixel). Background bawaan diganti.")
  public void SetRadialGradient(AndroidViewComponent comp, int centerColor,
      int edgeColor, float radiusPx) {
    View v = viewOf(comp, "RadialGradient");
    if (v == null) {
      return;
    }
    try {
      GradientDrawable g = currentBg(v);
      g.setColors(new int[] { centerColor, edgeColor });
      g.setGradientType(GradientDrawable.RADIAL_GRADIENT);
      g.setGradientRadius(Math.max(0, Math.round(radiusPx)));
      v.setBackgroundDrawable(g);
    } catch (Exception e) {
    }
  }

  @SimpleFunction(description = "SET: Atur bentuk background: colok "
      + "ShapeRectangle/ShapeOval/ShapeLine/ShapeRing .")
  public void SetShape(AndroidViewComponent comp, int shape) {
    View v = viewOf(comp, "Shape");
    if (v == null) {
      return;
    }
    if (shape < 0 || shape > 3) {
      Shared.err(this, "Style", 0,
          "Shape: pakai ShapeRectangle/ShapeOval/ShapeLine/ShapeRing");
      return;
    }
    try {
      GradientDrawable g = currentBg(v);
      g.setShape(shape);
      v.setBackgroundDrawable(g);
    } catch (Exception e) {
    }
  }

  @SimpleProperty(description = "BLOK KUNING, bentuk: persegi.",
      category = PropertyCategory.BEHAVIOR)
  public int ShapeRectangle() {
    return GradientDrawable.RECTANGLE;
  }

  @SimpleProperty(description = "BLOK KUNING, bentuk: oval/lingkaran.",
      category = PropertyCategory.BEHAVIOR)
  public int ShapeOval() {
    return GradientDrawable.OVAL;
  }

  @SimpleProperty(description = "BLOK KUNING, bentuk: garis.",
      category = PropertyCategory.BEHAVIOR)
  public int ShapeLine() {
    return GradientDrawable.LINE;
  }

  @SimpleProperty(description = "BLOK KUNING, bentuk: cincin.",
      category = PropertyCategory.BEHAVIOR)
  public int ShapeRing() {
    return GradientDrawable.RING;
  }

  @SimpleFunction(description = "SET: Atur visibilitas: colok "
      + "VisibilityVisible/VisibilityInvisible/VisibilityGone . "
      + "Gone = hilang + tempat ikut hilang (≈ Visible=false MIT); Invisible "
      + "= tak terlihat tapi tempat tetap ada.")
  public void SetVisibility(AndroidViewComponent comp, int visibility) {
    View v = viewOf(comp, "Visibility");
    if (v == null) {
      return;
    }
    try {
      v.setVisibility(visibility);
    } catch (Exception e) {
    }
  }

  @SimpleProperty(description = "BLOK KUNING, visibilitas: terlihat.",
      category = PropertyCategory.BEHAVIOR)
  public int VisibilityVisible() {
    return View.VISIBLE;
  }

  @SimpleProperty(description = "BLOK KUNING, visibilitas: tak terlihat "
      + "(tempat tetap ada).",
      category = PropertyCategory.BEHAVIOR)
  public int VisibilityInvisible() {
    return View.INVISIBLE;
  }

  @SimpleProperty(description = "BLOK KUNING, visibilitas: hilang "
      + "(tempat ikut hilang).",
      category = PropertyCategory.BEHAVIOR)
  public int VisibilityGone() {
    return View.GONE;
  }

  @SimpleFunction(description = "SET: Atur putaran komponen (derajat, searah jarum jam).")
  public void SetRotation(AndroidViewComponent comp, float degrees) {
    View v = viewOf(comp, "Rotation");
    if (v == null) {
      return;
    }
    try {
      v.setRotation(degrees);
    } catch (Exception e) {
    }
  }

  @SimpleFunction(description = "SET: Efek ripple (gelombang sentuh) warna "
      + "tertentu. Menimpa SEMUA background — pasang PALING AKHIR.")
  public void SetRipple(AndroidViewComponent comp, int color) {
    View v = viewOf(comp, "Ripple");
    if (v == null) {
      return;
    }
    try {
      android.graphics.drawable.Drawable content = v.getBackground();
      android.content.res.ColorStateList cs =
          android.content.res.ColorStateList.valueOf(color);
      android.graphics.drawable.RippleDrawable r =
          new android.graphics.drawable.RippleDrawable(cs, content, null);
      v.setBackgroundDrawable(r);
      try {
        v.setClickable(true);
      } catch (Exception e) {
        // abaikan
      }
    } catch (Exception e) {
    }
  }

  @SimpleFunction(description = "SET: Garis tepi: warna + tebal (pixel) dulu, "
      + "lalu bila dashed=true tambah panjang garis (pixel) + jarak garis (pixel). "
      + "dashLengthPx/dashGapPx DIABAIKAN bila dashed=false (= garis penuh).")
  public void SetStroke(AndroidViewComponent comp, int color,
      float widthPx, boolean dashed, float dashLengthPx, float dashGapPx) {
    View v = viewOf(comp, "Stroke");
    if (v == null) {
      return;
    }
    try {
      GradientDrawable g = currentBg(v);
      if (dashed) {
        g.setStroke(Math.max(0, Math.round(widthPx)), color,
            Math.max(0, dashLengthPx), Math.max(0, dashGapPx));
      } else {
        g.setStroke(Math.max(0, Math.round(widthPx)), color);
      }
      v.setBackgroundDrawable(g);
    } catch (Exception e) {
    }
  }

  @SimpleFunction(description = "SET: Atur spasi teks (khusus Label/TextBox/Button): "
      + "jarak baris (pixel) + jarak huruf (em). Selain teks: error.")
  public void SetTextSpace(AndroidViewComponent comp, float lineSpacePx,
      float letterSpace) {
    View v = viewOf(comp, "TextSpace");
    if (v == null) {
      return;
    }
    try {
      if (!(v instanceof android.widget.TextView)) {
        Shared.err(this, "Style", 0,
            "TextSpace: khusus komponen teks (Label/TextBox/Button)");
        return;
      }
      android.widget.TextView tv = (android.widget.TextView) v;
      tv.setLineSpacing(Math.max(0, lineSpacePx), 1.0f);
      try {
        tv.setLetterSpacing(letterSpace);
      } catch (Exception e) {
        // abaikan (API lama)
      }
    } catch (Exception e) {
    }
  }

  @SimpleFunction(description = "SET: Jadikan lingkaran/oval (cocok untuk foto "
      + "profil). Untuk lingkaran sempurna pakai komponen persegi.")
  public void SetCircular(AndroidViewComponent comp) {
    View v = viewOf(comp, "Circular");
    if (v == null) {
      return;
    }
    try {
      GradientDrawable g = currentBg(v);
      g.setShape(GradientDrawable.OVAL);
      v.setBackgroundDrawable(g);
    } catch (Exception e) {
    }
  }

  @SimpleFunction(description = "SET: Lingkaran + garis tepi warna (cocok untuk foto "
      + "profil).")
  public void SetCircularBorder(AndroidViewComponent comp, int color,
      float widthPx) {
    View v = viewOf(comp, "CircularBorder");
    if (v == null) {
      return;
    }
    try {
      GradientDrawable g = currentBg(v);
      g.setShape(GradientDrawable.OVAL);
      g.setStroke(Math.max(0, Math.round(widthPx)), color);
      v.setBackgroundDrawable(g);
    } catch (Exception e) {
    }
  }

  @SimpleFunction(description = "SET RESET: hapus animasi; kembalikan "
      + "opacity/alpha, rotasi, skala, posisi (translation) ke normal. yang "
      + "TIDAK di-reset: background, elevation, padding, margin.")
  public void SetResetEffects(AndroidViewComponent comp) {
    View v = viewOf(comp, "Reset");
    if (v == null) {
      return;
    }
    try {
      v.clearAnimation();
      v.setAlpha(1.0f);
      v.setRotation(0.0f);
      v.setScaleX(1.0f);
      v.setScaleY(1.0f);
      v.setTranslationX(0.0f);
      v.setTranslationY(0.0f);
    } catch (Exception e) {
    }
  }

  @SimpleFunction(description = "SET: Atur warna background polos. "
      + "Background bawaan diganti. Bertarung dengan SetDynamicBackgroundColor "
      + "(Core): yang dipanggil TERAKHIR menang.")
  public void SetBackgroundColor(AndroidViewComponent comp, int color) {
    View v = viewOf(comp, "BackgroundColor");
    if (v == null) {
      return;
    }
    try {
      GradientDrawable g = currentBg(v);
      g.setColor(color);
      v.setBackgroundDrawable(g);
    } catch (Exception e) {
    }
  }

  @SimpleFunction(description = "SET: Atur gambar background dari file LOKAL "
      + "atau asset (path di-root /storage/emulated/0; asset diawali \"//\"). "
      + "KHUSUS lokal — BUKAN URL (URL: unduh dulu pakai Tools_Image/Tools_"
      + "Downloader). Izin baca storage otomatis dibawa komponen ini. Cocok "
      + "untuk wadah. Menimpa background bawaan.")
  public void SetBackgroundImage(AndroidViewComponent comp,
      final String path) {
    final View v = viewOf(comp, "BackgroundImage");
    if (v == null) {
      return;
    }
    final String p = Shared.clean(path == null ? "" : path);
    if (p.length() == 0) {
      Shared.err(this, "Style", 0, "BackgroundImage: path kosong");
      return;
    }
    if (Shared.isRemotePath(p)) {
      Shared.err(this, "Style", 0, "BackgroundImage: URL tidak didukung "
          + "(KHUSUS file lokal/asset; unduh dulu pakai Tools_Image/"
          + "Downloader)");
      return;
    }
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        Bitmap bmp = null;
        try {
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
            if (!Shared.ensurePermission(form, activity, Shared.PERM_READ_STORAGE)) {
              Shared.err(Tools_Style.this, "Style", 0,
                  "BackgroundImage: butuh izin baca storage");
              return;
            }
            File f = Shared.resolveRooted(p);
            if (f == null || !f.exists()) {
              Shared.err(Tools_Style.this, "Style", 0, "file tidak ada");
              return;
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
          if (data == null) {
            Shared.err(Tools_Style.this, "Style", 0, "gambar tidak terbaca");
            return;
          }
          BitmapFactory.Options o = new BitmapFactory.Options();
          o.inJustDecodeBounds = true;
          BitmapFactory.decodeByteArray(data, 0, data.length, o);
          int sample = 1;
          while (((o.outWidth / sample) > 2048
              || (o.outHeight / sample) > 2048) && sample < 16) {
            sample = sample * 2;
          }
          BitmapFactory.Options o2 = new BitmapFactory.Options();
          o2.inSampleSize = sample;
          bmp = BitmapFactory.decodeByteArray(data, 0, data.length, o2);
        } catch (Exception e) {
          String m = e.getMessage();
          Shared.err(Tools_Style.this, "Style", 0,
              m == null ? "background gagal" : m);
          return;
        }
        if (bmp == null) {
          Shared.err(Tools_Style.this, "Style", 0, "bukan gambar yang valid");
          return;
        }
        final Bitmap fBmp = bmp;
        try {
          v.post(new Runnable() {
            public void run() {
              try {
                v.setBackgroundDrawable(new BitmapDrawable(
                    context.getResources(), fBmp));
              } catch (Exception e) {
                Shared.err(Tools_Style.this, "Style", 0, "background gagal");
              }
            }
          });
        } catch (Exception e) {
          Shared.err(Tools_Style.this, "Style", 0, "background gagal");
        }
      }
    });
  }

  @SimpleFunction(description = "SET: Atur bayangan/elevasi (pixel). Butuh "
      + "background tidak transparan + ruang kosong di sekitar agar bayangan "
      + "terlihat; di tema gelap bayangan samar.")
  public void SetElevation(AndroidViewComponent comp, float px) {
    View v = viewOf(comp, "Elevation");
    if (v == null) {
      return;
    }
    try {
      v.setElevation(Math.max(0, px));
    } catch (Exception e) {
    }
  }

  @SimpleFunction(description = "SET: Atur jarak dalam/padding (pixel): "
      + "kiri, atas, kanan, bawah.")
  public void SetPadding(AndroidViewComponent comp, float leftPx,
      float topPx, float rightPx, float bottomPx) {
    View v = viewOf(comp, "Padding");
    if (v == null) {
      return;
    }
    try {
      v.setPadding(Math.round(leftPx), Math.round(topPx), Math.round(rightPx),
          Math.round(bottomPx));
    } catch (Exception e) {
    }
  }

  @SimpleFunction(description = "SET: Atur jarak luar/margin (pixel): kiri, atas, "
      + "kanan, bawah. Hanya bila induk mendukung margin (Vertical/Horizontal/"
      + "ScrollArrangement BISA; TableArrangement TIDAK — baris tabel "
      + "mengabaikan margin). Induk tak mendukung = ErrorOccurred.")
  public void SetMargin(AndroidViewComponent comp, float leftPx,
      float topPx, float rightPx, float bottomPx) {
    View v = viewOf(comp, "Margin");
    if (v == null) {
      return;
    }
    try {
      ViewGroup.LayoutParams lp = v.getLayoutParams();
      if (lp instanceof ViewGroup.MarginLayoutParams) {
        ((ViewGroup.MarginLayoutParams) lp).setMargins(Math.round(leftPx),
            Math.round(topPx), Math.round(rightPx), Math.round(bottomPx));
        v.requestLayout();
      } else {
        Shared.err(this, "Style", 0,
            "Margin: induk tidak mendukung margin (mis. TableArrangement)");
      }
    } catch (Exception e) {
    }
  }

  @SimpleFunction(description = "SET: Atur transparansi 0-100 (100 = penuh).")
  public void SetOpacity(AndroidViewComponent comp, int percent) {
    View v = viewOf(comp, "Opacity");
    if (v == null) {
      return;
    }
    try {
      int p = Math.max(0, Math.min(100, percent));
      v.setAlpha(p / 100.0f);
    } catch (Exception e) {
    }
  }

  private void play(final AndroidViewComponent comp, final View v,
      final Animation a, final int durationMs, final String op,
      final String type) {
    if (v == null) {
      return;
    }
    try {
      if (durationMs > 0) {
        a.setDuration(durationMs);
      }
      if (type != null && !"".equals(type)) {
        try {
          a.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationStart(Animation an) {
              AnimationStarted(comp, type);
            }
            public void onAnimationEnd(Animation an) {
              AnimationEnded(comp, type);
            }
            public void onAnimationRepeat(Animation an) {
              // abaikan
            }
          });
        } catch (Exception e) {
          // abaikan
        }
      }
      try {
        v.post(new Runnable() {
          public void run() {
            try {
              v.startAnimation(a);
            } catch (Exception e) {
              Shared.err(Tools_Style.this, "Style", 0, "animasi gagal");
            }
          }
        });
      } catch (Exception e) {
        v.startAnimation(a);
      }
    } catch (Exception e) {
      Shared.err(this, "Style", 0, op + " gagal");
    }
  }

  @SimpleFunction(description = "CALL: Animasi fade: startPercent ke endPercent "
      + "(0-100). Mis. 0->100 muncul, 100->0 hilang. durationMs mis. 300; "
      + "durationMs<=0 = tanpa set durasi (jalan instan). Menembak "
      + "AnimationStarted/AnimationEnded.")
  public void CallAnimateFade(AndroidViewComponent comp, int durationMs,
      int startPercent, int endPercent) {
    View v = viewOf(comp, "Fade");
    if (v == null) {
      return;
    }
    float s = Math.max(0, Math.min(100, startPercent)) / 100.0f;
    float e = Math.max(0, Math.min(100, endPercent)) / 100.0f;
    AlphaAnimation a = new AlphaAnimation(s, e);
    a.setFillAfter(true);
    play(comp, v, a, durationMs, "fade", "fade");
  }

  @SimpleProperty(description = "BLOK KUNING, arah geser: dari kiri.",
      category = PropertyCategory.BEHAVIOR)
  public String DirectionLeft() {
    return "left";
  }

  @SimpleProperty(description = "BLOK KUNING, arah geser: dari kanan.",
      category = PropertyCategory.BEHAVIOR)
  public String DirectionRight() {
    return "right";
  }

  @SimpleProperty(description = "BLOK KUNING, arah geser: dari atas.",
      category = PropertyCategory.BEHAVIOR)
  public String DirectionTop() {
    return "top";
  }

  @SimpleProperty(description = "BLOK KUNING, arah geser: dari bawah.",
      category = PropertyCategory.BEHAVIOR)
  public String DirectionBottom() {
    return "bottom";
  }

  @SimpleFunction(description = "CALL: Animasi geser-masuk: direction = colok "
      + "DirectionLeft/DirectionRight/DirectionTop/DirectionBottom ; teks left/right/top/bottom = cara lama. durationMs mis. "
      + "300; durationMs<=0 = tanpa set durasi (jalan instan).")
  public void CallAnimateSlideIn(AndroidViewComponent comp, String direction,
      int durationMs) {
    View v = viewOf(comp, "SlideIn");
    if (v == null) {
      return;
    }
    String d = direction == null ? "" : direction.trim().toLowerCase();
    Animation a;
    if ("left".equals(d)) {
      a = new TranslateAnimation(Animation.RELATIVE_TO_SELF, -1.0f,
          Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
          Animation.RELATIVE_TO_SELF, 0.0f);
    } else if ("right".equals(d)) {
      a = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 1.0f,
          Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
          Animation.RELATIVE_TO_SELF, 0.0f);
    } else if ("top".equals(d)) {
      a = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
          Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, -1.0f,
          Animation.RELATIVE_TO_SELF, 0.0f);
    } else if ("bottom".equals(d)) {
      a = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
          Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 1.0f,
          Animation.RELATIVE_TO_SELF, 0.0f);
    } else {
      Shared.err(this, "Style", 0,
          "arah harus left/right/top/bottom");
      return;
    }
    play(comp, v, a, durationMs, "slidein", "slide");
  }

  @SimpleFunction(description = "CALL: Animasi skala: ke toXPercent/toYPercent "
      + "(100 = normal). durationMs mis. 300; durationMs<=0 = tanpa set durasi "
      + "(jalan instan).")
  public void CallAnimateScale(AndroidViewComponent comp, int durationMs,
      int toXPercent, int toYPercent) {
    View v = viewOf(comp, "Scale");
    if (v == null) {
      return;
    }
    float x = Math.max(0, toXPercent) / 100.0f;
    float y = Math.max(0, toYPercent) / 100.0f;
    play(comp, v, new ScaleAnimation(0.0f, x, 0.0f, y,
        Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f),
        durationMs, "scale", "scale");
  }

  @SimpleFunction(description = "CALL: Animasi putar (derajat, searah jarum jam). "
      + "durationMs mis. 300; durationMs<=0 = tanpa set durasi (jalan "
      + "instan).")
  public void CallAnimateRotation(AndroidViewComponent comp, int durationMs,
      float degrees) {
    View v = viewOf(comp, "RotationAnim");
    if (v == null) {
      return;
    }
    play(comp, v, new android.view.animation.RotateAnimation(0.0f, degrees,
        Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f),
        durationMs, "rotation", "rotation");
  }

  @SimpleFunction(description = "Fallback polling untuk event animasi: true "
      + "bila komponen sedang dianimasikan Tools_Style.")
  public boolean IsAnimationRunning(AndroidViewComponent comp) {
    View v = comp == null ? null : viewOf(comp, "AnimCheck");
    if (v == null) {
      return false;
    }
    try {
      Animation a = v.getAnimation();
      return a != null && a.hasStarted() && !a.hasEnded();
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleFunction(description = "Fallback polling untuk event ErrorOccurred: "
      + "pesan error terakhir operasi ini (kosong bila belum ada).")
  public String LastError(String operation) {
    return Shared.lastError(operation);
  }

  // ================= EVENTS =================

  @SimpleEvent(description = "Animasi Tools_Style mulai: komponen + tipe "
      + "(fade/slide/scale/rotation). Fallback: IsAnimationRunning.")
  public void AnimationStarted(AndroidViewComponent comp, String animationType) {
    Shared.fire(this, "AnimationStarted", comp,
        animationType == null ? "" : animationType);
  }

  @SimpleEvent(description = "Animasi Tools_Style selesai: komponen + tipe. "
      + "Fallback: IsAnimationRunning.")
  public void AnimationEnded(AndroidViewComponent comp, String animationType) {
    Shared.fire(this, "AnimationEnded", comp,
        animationType == null ? "" : animationType);
  }

  // ================= EVENTS =================

  @SimpleEvent(description = "SATU-SATUNYA error Tools_Style. component = nama "
      + "operasi (bandingkan dengan blok OpStyle; cth: if component = OpStyle "
      + "berarti error Style). Id selalu 0 (stateless).")
  public void ErrorOccurred(String component, int id, String message) {
    Shared.fire(this, "ErrorOccurred",
        component, Integer.valueOf(id), message);
  }

  // ================= OP =================

  @SimpleProperty(description = "Nama operasi Style (untuk if di "
      + "ErrorOccurred: cth if component = OpStyle berarti error Style).",
      category = PropertyCategory.BEHAVIOR)
  public String OpStyle() {
    return "Style";
  }
}
