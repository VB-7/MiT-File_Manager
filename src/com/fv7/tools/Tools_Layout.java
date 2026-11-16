package com.fv7.tools;

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
import com.google.appinventor.components.runtime.HorizontalArrangement;
import com.google.appinventor.components.runtime.HorizontalScrollArrangement;
import com.google.appinventor.components.runtime.OnDestroyListener;
import com.google.appinventor.components.runtime.VerticalArrangement;
import com.google.appinventor.components.runtime.VerticalScrollArrangement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@DesignerComponent(
    version = 1,
    description = "Tools_Layout: bikin layout dinamis ber-id (Vertical, "
        + "Horizontal, VerticalScroll, HorizontalScroll) di dalam layout yang "
        + "sudah ada atau buatan Tools_Core/Tools_Layout. Id dikelola "
        + "CreateLayoutId. List id URUT PEMBUATAN.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    androidMinSdk = 21,
    iconName = "aiwebres/tools_layout.jpg")
@SimpleObject(external = true)
public class Tools_Layout extends AndroidNonvisibleComponent
    implements OnDestroyListener {

  private static final Map<Integer, Component> layouts =
      new HashMap<Integer, Component>();
  private static final Object layoutLock = new Object();
  private static final Map<Integer, String> layoutTypes =
      new HashMap<Integer, String>();

  public Tools_Layout(ComponentContainer container) {
    super(container.$form());
  }

  /** Lepas satu layout dinamis + lepas id. Dipakai komponen lain bila perlu. */
  public static void clearStatic(int id) {
    Component c = null;
    synchronized (layoutLock) {
      c = layouts.remove(Integer.valueOf(id));
      layoutTypes.remove(Integer.valueOf(id));
    }
    View root = null;
    if (c instanceof AndroidViewComponent) {
      try {
        root = ((AndroidViewComponent) c).getView();
      } catch (Exception e) {
        // abaikan
      }
    }
    if (root != null) {
      // Bereskan anak dinamis di dalamnya dulu (Core/Image/Sheet/nested).
      try {
        Tools_Core.clearByView(root);
      } catch (Exception e) {
        // abaikan
      }
      try {
        Tools_Image.clearByView(root);
      } catch (Exception e) {
        // abaikan
      }
      try {
        Tools_Sheet.clearByView(root);
      } catch (Exception e) {
        // abaikan
      }
      clearNestedByView(root, id);
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
    Ids.setActive(Ids.LAYOUT, id, false);
    Ids.unregister(Ids.LAYOUT, id);
  }

  /** Hapus layout dinamis bersarang yang view-nya ada di bawah root. */
  private static void clearNestedByView(View root, int exceptId) {
    if (root == null) {
      return;
    }
    ArrayList<Integer> gone = new ArrayList<Integer>();
    synchronized (layoutLock) {
      for (Map.Entry<Integer, Component> en : layouts.entrySet()) {
        if (en.getKey().intValue() == exceptId) {
          continue;
        }
        Component cc = en.getValue();
        if (cc instanceof AndroidViewComponent) {
          try {
            View v = ((AndroidViewComponent) cc).getView();
            if (v != null && v != root && isUnder(v, root)) {
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

  /** Hapus semua layout dinamis yang view-nya ada di bawah root (untuk induk luar). */
  public static void clearByView(View root) {
    if (root == null) {
      return;
    }
    ArrayList<Integer> gone = new ArrayList<Integer>();
    synchronized (layoutLock) {
      for (Map.Entry<Integer, Component> en : layouts.entrySet()) {
        Component cc = en.getValue();
        if (cc instanceof AndroidViewComponent) {
          try {
            View v = ((AndroidViewComponent) cc).getView();
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

  private void make(AndroidViewComponent container, int id, String type) {
    if (container == null || !(container instanceof ComponentContainer)) {
      Shared.err(this, "Layout", id, "layout induk tidak valid "
          + "(pakai Arrangement)");
      return;
    }
    if (id < 1) {
      Shared.err(this, "Layout", id, "id harus >= 1 (pakai CreateLayoutId)");
      return;
    }
    if (!Ids.register(Ids.LAYOUT, id)) {
      Shared.err(this, "Layout", id, "ID " + id + " sedang dipakai layout lain");
      return;
    }
    synchronized (layoutLock) {
      if (layouts.containsKey(Integer.valueOf(id))) {
        Shared.err(this, "Layout", id, "ID " + id + " masih ada "
            + "(Remove dulu)");
        return;
      }
    }
    try {
      Component made;
      if ("HorizontalArrangement".equals(type)) {
        made = new HorizontalArrangement((ComponentContainer) container);
      } else if ("VerticalScrollArrangement".equals(type)) {
        made = new VerticalScrollArrangement((ComponentContainer) container);
      } else if ("HorizontalScrollArrangement".equals(type)) {
        made = new HorizontalScrollArrangement((ComponentContainer) container);
      } else {
        made = new VerticalArrangement((ComponentContainer) container);
      }
      synchronized (layoutLock) {
        layouts.put(Integer.valueOf(id), made);
        layoutTypes.put(Integer.valueOf(id), type);
      }
      Ids.setActive(Ids.LAYOUT, id, true);
      LayoutCreated(id, type);
    } catch (Exception e) {
      Ids.setActive(Ids.LAYOUT, id, false);
      Ids.unregister(Ids.LAYOUT, id);
      String m = e.getMessage();
      Shared.err(this, "Layout", id,
          m == null ? "gagal bikin layout" : "gagal bikin layout: " + m);
    }
  }

  @SimpleFunction(description = "CALL: Bikin VerticalArrangement dinamis ber-id di "
      + "dalam layout induk. Sukses menembak LayoutCreated; gagal via "
      + "ErrorOccurred.")
  public void CallCreateVerticalLayout(AndroidViewComponent container, int id) {
    make(container, id, "VerticalArrangement");
  }

  @SimpleFunction(description = "CALL: Bikin HorizontalArrangement dinamis ber-id di "
      + "dalam layout induk.")
  public void CallCreateHorizontalLayout(AndroidViewComponent container, int id) {
    make(container, id, "HorizontalArrangement");
  }

  @SimpleFunction(description = "CALL: Bikin VerticalScrollArrangement dinamis "
      + "ber-id (bisa scroll atas-bawah) di dalam layout induk.")
  public void CallCreateVerticalScrollLayout(AndroidViewComponent container,
      int id) {
    make(container, id, "VerticalScrollArrangement");
  }

  @SimpleFunction(description = "CALL: Bikin HorizontalScrollArrangement dinamis "
      + "ber-id (bisa scroll kiri-kanan) di dalam layout induk.")
  public void CallCreateHorizontalScrollLayout(AndroidViewComponent container,
      int id) {
    make(container, id, "HorizontalScrollArrangement");
  }

  @SimpleFunction(description = "Ambil layout dinamis ber-id (untuk dipakai "
      + "sebagai wadah blok lain). null bila tidak ada.")
  public Object GetLayoutComponent(int id) {
    synchronized (layoutLock) {
      Component c = layouts.get(Integer.valueOf(id));
      if (c == null) {
        Shared.err(this, "Layout", id, "id tidak ada");
        return null;
      }
      return c;
    }
  }

  // ================= ID =================

  @SimpleFunction(description = "Buat id layout unik baru (Number, siap pakai).")
  public int CreateLayoutId() {
    int id = Ids.createFor(Ids.LAYOUT);
    if (id < 1) {
      Shared.err(this, "Layout", 0, "gagal buat id");
    }
    return id;
  }

  @SimpleFunction(description = "Semua id layout (URUT PEMBUATAN, "
      + "index 1 = pertama dibuat).")
  public List<Integer> GetLayoutIds() {
    return Ids.idsOf(Ids.LAYOUT);
  }

  @SimpleFunction(description = "Id layout yang aktif (URUT PEMBUATAN).")
  public List<Integer> GetActiveLayoutIds() {
    return Ids.activeIdsOf(Ids.LAYOUT);
  }

  @SimpleFunction(description = "Id layout terakhir dibuat. -1 bila kosong.")
  public int CurrentLayoutId() {
    return Ids.currentOf(Ids.LAYOUT);
  }

  @SimpleFunction(description = "Id index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstLayoutId() {
    return Ids.firstOf(Ids.LAYOUT);
  }

  @SimpleFunction(description = "Id TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastLayoutId() {
    return Ids.lastOf(Ids.LAYOUT);
  }

  @SimpleFunction(description = "Id yang dibuat TEPAT SEBELUM id ini. "
      + "-1 bila tidak ada.")
  public int GetPreviousLayoutId(int id) {
    return Ids.previousOf(Ids.LAYOUT, id);
  }

  @SimpleFunction(description = "True bila id layout ini masih ada.")
  public boolean IsLayoutIdActive(int id) {
    return Ids.isActive(Ids.LAYOUT, id);
  }

  @SimpleFunction(description = "Hapus layout dinamis: lepas dari induk + "
      + "lepas id (isi ikut hilang dari layar).")
  public void RemoveLayoutId(int id) {
    clearStatic(id);
  }

  @SimpleFunction(description = "SET: Hapus SEMUA layout dinamis Tools_Layout "
      + "(id ikut dilepas).")
  public void SetClearLayouts() {
    ArrayList<Integer> all;
    synchronized (layoutLock) {
      all = new ArrayList<Integer>(layouts.keySet());
    }
    for (int i = 0; i < all.size(); i = i + 1) {
      clearStatic(all.get(i).intValue());
    }
  }

  // ================= TYPE (pembanding, read-only) =================

  @SimpleFunction(description = "Tipe layout ber-id (\"VerticalArrangement\" "
      + "dkk, sama dengan typeName di LayoutCreated). Bandingkan dengan blok "
      + "TypeVertical/TypeHorizontal/TypeVerticalScroll/TypeHorizontalScroll. "
      + "Kosong bila id tidak ada.")
  public String GetLayoutType(int id) {
    synchronized (layoutLock) {
      String t = layoutTypes.get(Integer.valueOf(id));
      return t == null ? "" : t;
    }
  }

  @SimpleProperty(description = "Pembanding read-only: tipe VerticalArrangement.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeVertical() {
    return "VerticalArrangement";
  }

  @SimpleProperty(description = "Pembanding read-only: tipe HorizontalArrangement.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeHorizontal() {
    return "HorizontalArrangement";
  }

  @SimpleProperty(description = "Pembanding read-only: tipe VerticalScrollArrangement.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeVerticalScroll() {
    return "VerticalScrollArrangement";
  }

  @SimpleProperty(description = "Pembanding read-only: tipe HorizontalScrollArrangement.",
      category = PropertyCategory.BEHAVIOR)
  public String TypeHorizontalScroll() {
    return "HorizontalScrollArrangement";
  }

  // ================= EVENTS =================

  @SimpleEvent(description = "Layout dinamis jadi: id + nama tipe.")
  public void LayoutCreated(int id, String typeName) {
    EventDispatcher.dispatchEvent(this, "LayoutCreated",
        Integer.valueOf(id), typeName);
  }

  @SimpleEvent(description = "SATU-SATUNYA error Tools_Layout. component = nama "
      + "operasi (bandingkan dengan blok OpLayout).")
  public void ErrorOccurred(String component, int id, String message) {
    EventDispatcher.dispatchEvent(this, "ErrorOccurred",
        component, Integer.valueOf(id), message);
  }

  @SimpleFunction(description = "Fallback polling untuk event ErrorOccurred: "
      + "pesan error terakhir operasi ini (kosong bila belum ada).")
  public String LastError(String operation) {
    return Shared.lastError(operation);
  }

  // ================= OP =================

  @SimpleProperty(description = "Nama operasi Layout (untuk if di "
      + "ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpLayout() {
    return "Layout";
  }

  // ================= destroy =================

  public void onDestroy() {
    try {
      ArrayList<Integer> all;
      synchronized (layoutLock) {
        all = new ArrayList<Integer>(layouts.keySet());
      }
      for (int i = 0; i < all.size(); i = i + 1) {
        clearStatic(all.get(i).intValue());
      }
    } catch (Exception e) {
      // abaikan
    }
  }
}
