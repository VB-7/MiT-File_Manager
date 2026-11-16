package com.fv7.tools;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.AndroidViewComponent;
import com.google.appinventor.components.runtime.Component;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.OnDestroyListener;
import com.google.appinventor.components.runtime.util.YailList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@DesignerComponent(
    version = 1,
    description = "Tools_Core: mesin komponen dinamis generik ber-id. Bikin komponen "
        + "MIT apa pun saat aplikasi jalan (Button, Label, TextBox, Image, CheckBox, "
        + "Switch, Slider, Spinner, ListView, DatePicker, TimePicker, "
        + "Horizontal/VerticalArrangement, Horizontal/VerticalScrollArrangement) ke "
        + "dalam layout, lengkap dengan event Click/LongClick/ComponentCreated ber-id, "
        + "plus SetProperty/GetProperty/Invoke generik. Id dikelola CreateDynamicId. "
        + "List id URUT PEMBUATAN. Catatan: event klik komponen dinamis dilayani "
        + "Tools_Core (blok Any generik MIT tidak ikut terpicu untuknya).",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    androidMinSdk = 21,
    iconName = "aiwebres/tools_core.jpg")
@SimpleObject(external = true)
public class Tools_Core extends AndroidNonvisibleComponent
    implements OnDestroyListener {

  private static final String BASE_PKG =
      "com.google.appinventor.components.runtime.";

  private static final String[] ALLOWED = {
      "Button", "Label", "TextBox", "Image", "CheckBox", "Switch",
      "Slider", "Spinner", "ListView", "DatePicker", "TimePicker", "Space",
      "HorizontalArrangement", "VerticalArrangement",
      "HorizontalScrollArrangement", "VerticalScrollArrangement"
  };
  private static final Set<String> allowedSet = new HashSet<String>();
  static {
    for (int i = 0; i < ALLOWED.length; i = i + 1) {
      allowedSet.add(ALLOWED[i]);
    }
  }

  private static final Map<Integer, Component> comps =
      new HashMap<Integer, Component>();
  private static final Object compLock = new Object();
  private static final Map<Integer, Long> lastTap =
      new HashMap<Integer, Long>();

  public Tools_Core(ComponentContainer container) {
    super(container.$form());
  }

  private static String canonical(String name) {
    if (name == null) {
      return null;
    }
    String t = name.trim();
    for (int i = 0; i < ALLOWED.length; i = i + 1) {
      if (ALLOWED[i].equalsIgnoreCase(t)) {
        return ALLOWED[i];
      }
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

  /** Lepas satu komponen dinamis + lepas id. Dipakai komponen lain bila perlu. */
  public static void clearStatic(int id) {
    Component c = null;
    synchronized (compLock) {
      c = comps.remove(Integer.valueOf(id));
      lastTap.remove(Integer.valueOf(id));
    }
    if (c instanceof AndroidViewComponent) {
      try {
        View v = ((AndroidViewComponent) c).getView();
        if (v != null && v.getParent() instanceof ViewGroup) {
          ((ViewGroup) v.getParent()).removeView(v);
        }
      } catch (Exception e) {
        // abaikan
      }
    }
    Ids.setActive(Ids.CORE, id, false);
    Ids.unregister(Ids.CORE, id);
  }

  @SimpleFunction(description = "Bikin komponen MIT dinamis ber-id ke dalam layout "
      + "(Arrangement MIT / dinamis). typeName: Button, Label, TextBox, Image, "
      + "CheckBox, Switch, Slider, Spinner, ListView, DatePicker, TimePicker, "
      + "HorizontalArrangement, VerticalArrangement, HorizontalScrollArrangement, "
      + "VerticalScrollArrangement. id = penanda (minta ke CreateDynamicId, "
      + "reservasi otomatis diadopsi). Sukses menembak ComponentCreated; "
      + "klik/longklik menembak CoreClicked/CoreLongClicked; gagal via "
      + "ErrorOccurred.")
  public void CallCreateDynamicComponent(String typeName,
      AndroidViewComponent container, final int id) {
    final String canon = canonical(typeName);
    if (canon == null) {
      Shared.err(this, "Core", id, "tipe tidak didukung: " + typeName);
      return;
    }
    if (container == null) {
      Shared.err(this, "Core", id, "layout tidak valid");
      return;
    }
    if (id < 1) {
      Shared.err(this, "Core", id, "id harus >= 1 (pakai CreateDynamicId)");
      return;
    }
    if (!Ids.register(Ids.CORE, id)) {
      Shared.err(this, "Core", id, "ID " + id + " sedang dipakai komponen lain");
      return;
    }
    synchronized (compLock) {
      if (comps.containsKey(Integer.valueOf(id))) {
        Shared.err(this, "Core", id, "ID " + id + " masih ada "
            + "(Remove dulu)");
        return;
      }
    }
    try {
      Class<?> cls = Class.forName(BASE_PKG + canon);
      Constructor<?> ctor = cls.getConstructor(ComponentContainer.class);
      final Component comp = (Component) ctor.newInstance(container);
      synchronized (compLock) {
        comps.put(Integer.valueOf(id), comp);
      }
      Ids.setActive(Ids.CORE, id, true);
      if (comp instanceof AndroidViewComponent) {
        try {
          View v = ((AndroidViewComponent) comp).getView();
          if (v != null) {
            v.setOnClickListener(new View.OnClickListener() {
              public void onClick(View view) {
                CoreClicked(id);
                try {
                  long now = System.currentTimeMillis();
                  Long prev;
                  synchronized (compLock) {
                    prev = lastTap.get(Integer.valueOf(id));
                    lastTap.put(Integer.valueOf(id), Long.valueOf(now));
                  }
                  if (prev != null && (now - prev.longValue()) < 350) {
                    CoreDoubleClicked(id);
                  }
                } catch (Exception e) {
                  // abaikan
                }
              }
            });
            v.setOnLongClickListener(new View.OnLongClickListener() {
              public boolean onLongClick(View view) {
                CoreLongClicked(id);
                return true;
              }
            });
          }
        } catch (Exception e) {
          // abaikan, komponen tetap jadi
        }
      }
      ComponentCreated(id, canon);
    } catch (Exception e) {
      Ids.setActive(Ids.CORE, id, false);
      Ids.unregister(Ids.CORE, id);
      String m = e.getMessage();
      Shared.err(this, "Core", id,
          m == null ? "gagal bikin komponen" : "gagal bikin komponen: " + m);
    }
  }

  @SimpleFunction(description = "Ambil komponen dinamis ber-id (untuk blok Any "
      + "generik MIT / blok yang butuh soket komponen). null bila tidak ada.")
  public Object GetDynamicComponent(int id) {
    synchronized (compLock) {
      Component c = comps.get(Integer.valueOf(id));
      if (c == null) {
        Shared.err(this, "Core", id, "id tidak ada");
        return null;
      }
      return c;
    }
  }

  private static Object coerce(Class<?> target, Object value) {
    if (value == null) {
      return null;
    }
    try {
      if (target == String.class) {
        return value.toString();
      }
      if (target == boolean.class || target == Boolean.class) {
        if (value instanceof Boolean) {
          return value;
        }
        String s = value.toString().trim().toLowerCase();
        return Boolean.valueOf(s.equals("true") || s.equals("1"));
      }
      if (value instanceof Number) {
        Number n = (Number) value;
        if (target == int.class || target == Integer.class) {
          return Integer.valueOf(n.intValue());
        }
        if (target == long.class || target == Long.class) {
          return Long.valueOf(n.longValue());
        }
        if (target == float.class || target == Float.class) {
          return Float.valueOf(n.floatValue());
        }
        if (target == double.class || target == Double.class) {
          return Double.valueOf(n.doubleValue());
        }
      } else {
        String s = value.toString().trim();
        if (target == int.class || target == Integer.class) {
          return Integer.valueOf(Integer.parseInt(s));
        }
        if (target == long.class || target == Long.class) {
          return Long.valueOf(Long.parseLong(s));
        }
        if (target == float.class || target == Float.class) {
          return Float.valueOf(Float.parseFloat(s));
        }
        if (target == double.class || target == Double.class) {
          return Double.valueOf(Double.parseDouble(s));
        }
      }
      if (target.isInstance(value)) {
        return value;
      }
    } catch (Exception e) {
      return null;
    }
    return null;
  }

  private static Method findSetter(Class<?> cls, String name) {
    // Konvensi MIT: setter properti bernama persis propertinya (Text, Visible).
    for (Method m : cls.getMethods()) {
      if (m.getName().equals(name) && m.getParameterTypes().length == 1) {
        return m;
      }
    }
    String want = "set" + name;
    for (Method m : cls.getMethods()) {
      if (m.getName().equalsIgnoreCase(want)
          && m.getParameterTypes().length == 1) {
        return m;
      }
    }
    return null;
  }

  private static Method findGetter(Class<?> cls, String name) {
    // Konvensi MIT: getter properti bernama persis propertinya (Text).
    for (Method m : cls.getMethods()) {
      if (m.getName().equals(name) && m.getParameterTypes().length == 0
          && m.getReturnType() != void.class) {
        return m;
      }
    }
    String g = "get" + name;
    String b = "is" + name;
    for (Method m : cls.getMethods()) {
      if ((m.getName().equalsIgnoreCase(g) || m.getName().equalsIgnoreCase(b))
          && m.getParameterTypes().length == 0
          && m.getReturnType() != void.class) {
        return m;
      }
    }
    return null;
  }

  @SimpleFunction(description = "SET: Set properti komponen dinamis ber-id "
      + "(nama properti MIT, mis. Text, BackgroundColor, FontSize). "
      + "Gagal via ErrorOccurred.")
  public void SetDynamicProperty(int id, String name, Object value) {
    String e = applyProperty(id, name, value);
    if (e != null) {
      Shared.err(this, "Core", id, e);
    }
  }

  /** Terapkan properti. Kembali null bila sukses, pesan error bila gagal. */
  private String applyProperty(int id, String name, Object value) {
    Component comp;
    synchronized (compLock) {
      comp = comps.get(Integer.valueOf(id));
    }
    if (comp == null) {
      return "id tidak ada";
    }
    if (name == null || name.trim().length() == 0) {
      return "nama properti kosong";
    }
    try {
      Method setter = findSetter(comp.getClass(), name.trim());
      if (setter == null) {
        return "properti tidak ada: " + name;
      }
      Object arg = coerce(setter.getParameterTypes()[0], value);
      if (arg == null && value != null) {
        return "nilai tidak cocok untuk " + name;
      }
      setter.invoke(comp, arg);
      return null;
    } catch (Exception e) {
      String m = e.getMessage();
      return m == null ? "set properti gagal" : "set properti gagal: " + m;
    }
  }

  private void build(String canon, AndroidViewComponent container, int id,
      String[] names, Object[] values) {
    CallCreateDynamicComponent(canon, container, id);
    boolean ok;
    synchronized (compLock) {
      ok = comps.containsKey(Integer.valueOf(id));
    }
    if (!ok) {
      return;
    }
    for (int i = 0; i < names.length && i < values.length; i = i + 1) {
      String e = applyProperty(id, names[i], values[i]);
      if (e != null) {
        Shared.err(this, "Core", id, e);
      }
    }
  }

  @SimpleFunction(description = "CALL: Jalan pintas: bikin Button dinamis ber-id "
      + "lengkap dengan teks, warna, ukuran font, tebal, visibilitas. "
      + "Sama seperti Create + Set berulang, tapi satu blok.")
  public void CallCreateDynamicButton(AndroidViewComponent container, int id,
      String text, int textColor, int backgroundColor, float fontSize,
      boolean bold, boolean visible) {
    build("Button", container, id,
        new String[] { "Text", "TextColor", "BackgroundColor", "FontSize",
            "FontBold", "Visible" },
        new Object[] { text, Integer.valueOf(textColor),
            Integer.valueOf(backgroundColor), new Float(fontSize),
            Boolean.valueOf(bold), Boolean.valueOf(visible) });
  }

  @SimpleFunction(description = "CALL: Jalan pintas: bikin Label dinamis ber-id "
      + "lengkap dengan teks, warna, ukuran font, tebal, visibilitas.")
  public void CallCreateDynamicLabel(AndroidViewComponent container, int id,
      String text, int textColor, int backgroundColor, float fontSize,
      boolean bold, boolean visible) {
    build("Label", container, id,
        new String[] { "Text", "TextColor", "BackgroundColor", "FontSize",
            "FontBold", "Visible" },
        new Object[] { text, Integer.valueOf(textColor),
            Integer.valueOf(backgroundColor), new Float(fontSize),
            Boolean.valueOf(bold), Boolean.valueOf(visible) });
  }

  @SimpleFunction(description = "CALL: Jalan pintas: bikin TextBox dinamis ber-id "
      + "lengkap dengan hint, warna, ukuran font, tebal, visibilitas.")
  public void CallCreateDynamicTextBox(AndroidViewComponent container, int id,
      String hint, int textColor, int backgroundColor, float fontSize,
      boolean bold, boolean visible) {
    build("TextBox", container, id,
        new String[] { "Hint", "TextColor", "BackgroundColor", "FontSize",
            "FontBold", "Visible" },
        new Object[] { hint, Integer.valueOf(textColor),
            Integer.valueOf(backgroundColor), new Float(fontSize),
            Boolean.valueOf(bold), Boolean.valueOf(visible) });
  }

  @SimpleFunction(description = "CALL: Jalan pintas: bikin Image (gambar MIT) "
      + "dinamis ber-id dengan picture + visibilitas. Isi picture seperti "
      + "Image.Picture biasa (path/asset/URL).")
  public void CallCreateDynamicImage(AndroidViewComponent container, int id,
      String picturePath, boolean visible) {
    build("Image", container, id,
        new String[] { "Picture", "Visible" },
        new Object[] { picturePath, Boolean.valueOf(visible) });
  }

  @SimpleFunction(description = "Baca properti komponen dinamis ber-id "
      + "(teks). Kosong bila tidak ada/gagal (lihat ErrorOccurred).")
  public String GetDynamicProperty(int id, String name) {
    Component comp;
    synchronized (compLock) {
      comp = comps.get(Integer.valueOf(id));
    }
    if (comp == null) {
      Shared.err(this, "Core", id, "id tidak ada");
      return "";
    }
    if (name == null || name.trim().length() == 0) {
      Shared.err(this, "Core", id, "nama properti kosong");
      return "";
    }
    try {
      Method getter = findGetter(comp.getClass(), name.trim());
      if (getter == null) {
        Shared.err(this, "Core", id, "properti tidak ada: " + name);
        return "";
      }
      Object r = getter.invoke(comp);
      return r == null ? "" : r.toString();
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Core", id,
          m == null ? "baca properti gagal" : "baca properti gagal: " + m);
      return "";
    }
  }

  @SimpleFunction(description = "Panggil fungsi komponen dinamis ber-id "
      + "(nama fungsi MIT + list argumen). Sinkron: hasil langsung dikembalikan "
      + "sebagai teks (kosong bila void/gagal; gagal via ErrorOccurred).")
  public String InvokeDynamic(int id, String name, YailList args) {
    Component comp;
    synchronized (compLock) {
      comp = comps.get(Integer.valueOf(id));
    }
    if (comp == null) {
      Shared.err(this, "Core", id, "id tidak ada");
      return "";
    }
    if (name == null || name.trim().length() == 0) {
      Shared.err(this, "Core", id, "nama fungsi kosong");
      return "";
    }
    try {
      Object[] in = args == null ? new Object[0] : args.toArray();
      Method best = null;
      Object[] bestArgs = null;
      for (Method m : comp.getClass().getMethods()) {
        if (!m.getName().equalsIgnoreCase(name.trim())) {
          continue;
        }
        Class<?>[] ps = m.getParameterTypes();
        if (ps.length != in.length) {
          continue;
        }
        Object[] ca = new Object[ps.length];
        boolean ok = true;
        for (int i = 0; i < ps.length; i = i + 1) {
          ca[i] = coerce(ps[i], in[i]);
          if (ca[i] == null && in[i] != null) {
            ok = false;
            break;
          }
        }
        if (ok) {
          best = m;
          bestArgs = ca;
          break;
        }
      }
      if (best == null) {
        Shared.err(this, "Core", id, "fungsi tidak cocok: " + name);
        return "";
      }
      Object r = best.invoke(comp, bestArgs);
      return r == null ? "" : r.toString();
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Core", id,
          m == null ? "invoke gagal" : "invoke gagal: " + m);
      return "";
    }
  }

  // ================= ID =================

  @SimpleFunction(description = "Buat id dinamis unik baru (Number, siap pakai "
      + "ke CreateDynamicComponent).")
  public int CreateDynamicId() {
    int id = Ids.createFor(Ids.CORE);
    if (id < 1) {
      Shared.err(this, "Core", 0, "gagal buat id");
    }
    return id;
  }

  @SimpleFunction(description = "Semua id dinamis (URUT PEMBUATAN, "
      + "index 1 = pertama dibuat).")
  public List<Integer> GetDynamicIds() {
    return Ids.idsOf(Ids.CORE);
  }

  @SimpleFunction(description = "Id dinamis yang aktif (URUT PEMBUATAN).")
  public List<Integer> GetActiveDynamicIds() {
    return Ids.activeIdsOf(Ids.CORE);
  }

  @SimpleFunction(description = "Id dinamis terakhir dibuat. -1 bila kosong.")
  public int CurrentDynamicId() {
    return Ids.currentOf(Ids.CORE);
  }

  @SimpleFunction(description = "Id index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstDynamicId() {
    return Ids.firstOf(Ids.CORE);
  }

  @SimpleFunction(description = "Id TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastDynamicId() {
    return Ids.lastOf(Ids.CORE);
  }

  @SimpleFunction(description = "Id yang dibuat TEPAT SEBELUM id ini. "
      + "-1 bila tidak ada.")
  public int GetPreviousDynamicId(int id) {
    return Ids.previousOf(Ids.CORE, id);
  }

  @SimpleFunction(description = "True bila id dinamis ini masih ada.")
  public boolean IsDynamicIdActive(int id) {
    return Ids.isActive(Ids.CORE, id);
  }

  @SimpleFunction(description = "Hapus komponen dinamis: lepas view dari layout "
      + "+ lepas id (otomatis beres-beres).")
  public void RemoveDynamicId(int id) {
    clearStatic(id);
  }

  @SimpleFunction(description = "SET: Hapus SEMUA komponen dinamis Tools_Core "
      + "(id ikut dilepas).")
  public void SetClearDynamicComponents() {
    ArrayList<Integer> all;
    synchronized (compLock) {
      all = new ArrayList<Integer>(comps.keySet());
    }
    for (int i = 0; i < all.size(); i = i + 1) {
      clearStatic(all.get(i).intValue());
    }
  }

  @SimpleFunction(description = "Fallback polling untuk event ErrorOccurred: "
      + "pesan error terakhir operasi ini (kosong bila belum ada).")
  public String LastError(String operation) {
    return Shared.lastError(operation);
  }

  /** Hapus semua komponen dinamis yang view-nya ada di dalam/di bawah root. */
  public static void clearByView(View root) {
    if (root == null) {
      return;
    }
    ArrayList<Integer> gone = new ArrayList<Integer>();
    synchronized (compLock) {
      for (Map.Entry<Integer, Component> en : comps.entrySet()) {
        Component c = en.getValue();
        if (c instanceof AndroidViewComponent) {
          try {
            View v = ((AndroidViewComponent) c).getView();
            if (v != null && (v == root || isUnder(v, root))) {
              gone.add(en.getKey());
            }
          } catch (Exception e) {
            // abaikan
          }
        }
      }
    }
    for (int i = 0; i < gone.size(); i = i + 1) {
      clearStatic(gone.get(i).intValue());
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

  @SimpleEvent(description = "Komponen dinamis jadi: id + nama tipe.")
  public void ComponentCreated(int id, String typeName) {
    EventDispatcher.dispatchEvent(this, "ComponentCreated",
        Integer.valueOf(id), typeName);
  }

  @SimpleEvent(description = "Komponen dinamis di-klik: id.")
  public void CoreClicked(int id) {
    EventDispatcher.dispatchEvent(this, "CoreClicked", Integer.valueOf(id));
  }

  @SimpleEvent(description = "Komponen dinamis di-long-klik: id.")
  public void CoreLongClicked(int id) {
    EventDispatcher.dispatchEvent(this, "CoreLongClicked", Integer.valueOf(id));
  }

  @SimpleEvent(description = "Komponen dinamis di-klik 2x cepat: id.")
  public void CoreDoubleClicked(int id) {
    EventDispatcher.dispatchEvent(this, "CoreDoubleClicked", Integer.valueOf(id));
  }

  @SimpleEvent(description = "SATU-SATUNYA error Tools_Core. component = nama "
      + "operasi (bandingkan dengan blok OpCore).")
  public void ErrorOccurred(String component, int id, String message) {
    EventDispatcher.dispatchEvent(this, "ErrorOccurred",
        component, Integer.valueOf(id), message);
  }

  // ================= OP =================

  @SimpleProperty(description = "Nama operasi Core (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpCore() {
    return "Core";
  }

  // ================= destroy =================

  public void onDestroy() {
    try {
      ArrayList<Integer> all;
      synchronized (compLock) {
        all = new ArrayList<Integer>(comps.keySet());
      }
      for (int i = 0; i < all.size(); i = i + 1) {
        clearStatic(all.get(i).intValue());
      }
    } catch (Exception e) {
      // abaikan
    }
  }
}
