package com.fv7.tools;

import android.view.View;
import android.view.ViewGroup;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.Options;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.fv7.tools.helpers.LayoutType;
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
    version = 23,
    description = "Tools_Layout: bikin layout dinamis ber-id (Vertical, "
        + "Horizontal, VerticalScroll, HorizontalScroll) di dalam layout yang "
        + "sudah ada atau buatan Tools_Core/Tools_Layout. Id dikelola "
        + "CreateLayoutId. List id URUT PEMBUATAN. Konvensi awalan: Is = boolean "
        + "(colok ke if), Call = async + hasil lewat event, Set = tanpa feedback "
        + "sukses (gagal via ErrorOccurred), Get = nilai balik sinkron. KAMUS "
        + "\"TIDAK ADA\": objek = null, angka = -3, id = -1, teks = kosong. "
        + "Tiga tukang: butuh wadah scroll = Layout; butuh tombol/label dinamis "
        + "= Core (atau komponen luar bila butuh lebih). Pilihan tetap (colok ke soket yang cocok).",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    androidMinSdk = 21,
    iconName = "aiwebres/tools_layout.jpg",
    helpUrl = "https://github.com/VB-7/MiT-File_Manager")
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

  private void make(AndroidViewComponent container, int id, String type,
      int width, int height, boolean visible, int backgroundColor) {
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
      try {
        if (made instanceof AndroidViewComponent) {
          ((AndroidViewComponent) made).Width(width);
          ((AndroidViewComponent) made).Height(height);
          ((AndroidViewComponent) made).Visible(visible);
        }
        if (made instanceof VerticalArrangement) {
          ((VerticalArrangement) made).BackgroundColor(backgroundColor);
        } else if (made instanceof HorizontalArrangement) {
          ((HorizontalArrangement) made).BackgroundColor(backgroundColor);
        } else if (made instanceof VerticalScrollArrangement) {
          ((VerticalScrollArrangement) made).BackgroundColor(backgroundColor);
        } else if (made instanceof HorizontalScrollArrangement) {
          ((HorizontalScrollArrangement) made).BackgroundColor(backgroundColor);
        }
      } catch (Exception e) {
        // abaikan, properti opsional
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
      + "dalam layout induk. width/height angka MIT (-2 isi, -1 otomatis, "
      + "positif pixel); visible false = GONE. Sukses menembak LayoutCreated; "
      + "gagal via ErrorOccurred.")
  public void CallCreateVerticalLayout(AndroidViewComponent container, int id,
      int width, int height, boolean visible, int backgroundColor) {
    make(container, id, "VerticalArrangement", width, height, visible,
        backgroundColor);
  }

  @SimpleFunction(description = "CALL: Bikin HorizontalArrangement dinamis ber-id di "
      + "dalam layout induk. width/height angka MIT (-2 isi, -1 otomatis, "
      + "positif pixel); visible false = GONE. Sukses menembak LayoutCreated; "
      + "gagal via ErrorOccurred.")
  public void CallCreateHorizontalLayout(AndroidViewComponent container, int id,
      int width, int height, boolean visible, int backgroundColor) {
    make(container, id, "HorizontalArrangement", width, height, visible,
        backgroundColor);
  }

  @SimpleFunction(description = "CALL: Bikin VerticalScrollArrangement dinamis "
      + "ber-id (bisa scroll atas-bawah) di dalam layout induk. width/height "
      + "angka MIT; visible false = GONE. Sukses menembak LayoutCreated; gagal "
      + "via ErrorOccurred.")
  public void CallCreateVerticalScrollLayout(AndroidViewComponent container,
      int id, int width, int height, boolean visible, int backgroundColor) {
    make(container, id, "VerticalScrollArrangement", width, height, visible,
        backgroundColor);
  }

  @SimpleFunction(description = "CALL: Bikin HorizontalScrollArrangement dinamis "
      + "ber-id (bisa scroll kiri-kanan) di dalam layout induk. width/height "
      + "angka MIT; visible false = GONE. Sukses menembak LayoutCreated; gagal "
      + "via ErrorOccurred.")
  public void CallCreateHorizontalScrollLayout(AndroidViewComponent container,
      int id, int width, int height, boolean visible, int backgroundColor) {
    make(container, id, "HorizontalScrollArrangement", width, height, visible,
        backgroundColor);
  }

  @SimpleFunction(description = "Ambil layout dinamis ber-id TANPA cek tipe "
      + "(untuk dibaca/dipegang; bebas). null bila tidak ada. Kontras: "
      + "GetLayoutAs(type, id) = AMAN (tipe wajib cocok; untuk dicolok ke soket "
      + "container yang menuntut tipe). Pakai As bila dicolok ke soket container.")
  public Object GetLayout(int id) {
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
      + "lepas id (isi ikut hilang dari layar). CASCADING: isi dinamis di "
      + "dalamnya (Core/Image/Sheet/nested Tools_Layout) ikut dilepas id-nya. "
      + "Sukses menembak LayoutRemoved; id tidak ada = diam (tanpa event).")
  public void RemoveLayoutId(int id) {
    boolean existed;
    synchronized (layoutLock) {
      existed = layouts.containsKey(Integer.valueOf(id));
    }
    clearStatic(id);
    if (existed) {
      LayoutRemoved(id);
    }
  }

  @SimpleFunction(description = "SET: Hapus SEMUA layout dinamis Tools_Layout "
      + "(id ikut dilepas; cascading seperti RemoveLayoutId). Menembak "
      + "LayoutRemoved SATU PER SATU id.")
  public void SetClearLayouts() {
    ArrayList<Integer> all;
    synchronized (layoutLock) {
      all = new ArrayList<Integer>(layouts.keySet());
    }
    for (int i = 0; i < all.size(); i = i + 1) {
      RemoveLayoutId(all.get(i).intValue());
    }
  }

  @SimpleFunction(description = "SET: Hapus HANYA layout dinamis yang ada di "
      + "dalam container ini (wadah luar/arrangement biasa boleh; isi dinamis "
      + "di dalamnya cascading seperti RemoveLayoutId). Menembak LayoutRemoved "
      + "satu per satu id. Kontras: SetClearLayouts = sapu SEMUA.")
  public void SetClearLayoutsIn(AndroidViewComponent container) {
    View root = null;
    try {
      if (container != null) {
        root = container.getView();
      }
    } catch (Exception e) {
      root = null;
    }
    if (root == null) {
      Shared.err(this, "Layout", 0, "container tidak valid");
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
      RemoveLayoutId(gone.get(i).intValue());
    }
  }

  // ================= TYPE (pembanding, read-only) =================

  /** Layout ber-id yang tipenya PERSIS sama dengan yang diminta.
   *  Null bila id tidak ada, tipe beda, atau input type kosong. */
  private Component findTypedLayout(String type, int id) {
    synchronized (layoutLock) {
      Component c = layouts.get(Integer.valueOf(id));
      if (c == null) {
        return null;
      }
      String want = Shared.clean(type);
      if (want.length() == 0) {
        return null;
      }
      String have = layoutTypes.get(Integer.valueOf(id));
      if (have == null || !have.equals(want)) {
        return null;
      }
      return c;
    }
  }

  @SimpleFunction(description = "SET: tampil/sembunyi layout dinamis ber-id. "
      + "type = colok TypeVertical/TypeHorizontal/TypeVerticalScroll/"
      + "TypeHorizontalScroll (WAJIB cocok dengan tipe aslinya, biar tidak "
      + "ketuker); id = id layout; visible false = GONE (hilang total, "
      + "ruang ikut ketutup). Tanpa feedback; cek dulu pakai IsLayoutExists "
      + "bila ragu.")
  public void SetLayoutVisible(@Options(LayoutType.class) String type, int id, boolean visible) {
    try {
      Component c = findTypedLayout(type, id);
      if (c instanceof AndroidViewComponent) {
        View v = ((AndroidViewComponent) c).getView();
        if (v != null) {
          v.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
      }
    } catch (Exception e) {
      // Set = tanpa feedback
    }
  }

  @SimpleFunction(description = "True bila layout dinamis ber-id ini ADA dan "
      + "tipenya PERSIS sama dengan type yang diminta (colok TypeVertical/"
      + "dll). False bila id tidak ada ATAU tipe beda.")
  public boolean IsLayoutExists(@Options(LayoutType.class) String type, int id) {
    try {
      return findTypedLayout(type, id) != null;
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleFunction(description = "True bila layout dinamis ber-id ini ADA, "
      + "tipenya cocok (seperti IsLayoutExists), DAN sedang tampil (visible). "
      + "False bila tidak ada / tipe beda / sedang GONE. Kebalikan: IsLayoutGone.")
  public boolean IsLayoutVisible(@Options(LayoutType.class) String type, int id) {
    try {
      Component c = findTypedLayout(type, id);
      if (c instanceof AndroidViewComponent) {
        View v = ((AndroidViewComponent) c).getView();
        return v != null && v.getVisibility() == View.VISIBLE;
      }
      return false;
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleFunction(description = "True bila layout dinamis ber-id ini sedang GONE/"
      + "sembunyi. Kebalikan IsLayoutVisible: true juga bila id tidak ada / tipe "
      + "beda (ragu? cek IsLayoutExists dulu).")
  public boolean IsLayoutGone(@Options(LayoutType.class) String type, int id) {
    try {
      Component c = findTypedLayout(type, id);
      if (c instanceof AndroidViewComponent) {
        View v = ((AndroidViewComponent) c).getView();
        return v == null || v.getVisibility() != View.VISIBLE;
      }
      return true;
    } catch (Exception e) {
      return true;
    }
  }

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

  @SimpleProperty(description = "BLOK KUNING, pembanding read-only: tipe "
      + "VerticalArrangement (colok ke soket type).",
      category = PropertyCategory.BEHAVIOR)
  public String TypeVertical() {
    return "VerticalArrangement";
  }

  @SimpleProperty(description = "BLOK KUNING, pembanding read-only: tipe "
      + "HorizontalArrangement (colok ke soket type).",
      category = PropertyCategory.BEHAVIOR)
  public String TypeHorizontal() {
    return "HorizontalArrangement";
  }

  @SimpleProperty(description = "BLOK KUNING, pembanding read-only: tipe "
      + "VerticalScrollArrangement (colok ke soket type).",
      category = PropertyCategory.BEHAVIOR)
  public String TypeVerticalScroll() {
    return "VerticalScrollArrangement";
  }

  @SimpleProperty(description = "BLOK KUNING, pembanding read-only: tipe "
      + "HorizontalScrollArrangement (colok ke soket type).",
      category = PropertyCategory.BEHAVIOR)
  public String TypeHorizontalScroll() {
    return "HorizontalScrollArrangement";
  }

  // ================= GET + PROPERTI (angka MIT: -2 isi, -1 otomatis) =================

  @SimpleFunction(description = "Ambil layout dinamis ber-id yang tipenya PERSIS "
      + "sama dengan type yang diminta (colok TypeVertical/dll; untuk dicolok ke "
      + "soket container, mis. Create button). AMAN vs GetLayout(id) yang bebas. "
      + "null bila id tidak ada ATAU tipe beda.")
  public Object GetLayoutAs(@Options(LayoutType.class) String type, int id) {
    try {
      return findTypedLayout(type, id);
    } catch (Exception e) {
      return null;
    }
  }

  @SimpleFunction(description = "SET: atur lebar + tinggi layout dinamis ber-id "
      + "(tipe wajib cocok). Angka SAMA seperti blok bawaan MIT: -2 = isi "
      + "penuh (fill parent), -1 = otomatis, positif = pixel. Tanpa feedback.")
  public void SetLayoutSize(@Options(LayoutType.class) String type, int id, int width, int height) {
    try {
      Component c = findTypedLayout(type, id);
      if (c instanceof AndroidViewComponent) {
        ((AndroidViewComponent) c).Width(width);
        ((AndroidViewComponent) c).Height(height);
      }
    } catch (Exception e) {
      // Set = tanpa feedback
    }
  }

  @SimpleFunction(description = "SET: warna latar layout dinamis ber-id (tipe "
      + "wajib cocok). Colok blok warna MIT. Tanpa feedback.")
  public void SetLayoutBackgroundColor(@Options(LayoutType.class) String type, int id, int color) {
    try {
      Component c = findTypedLayout(type, id);
      if (c instanceof VerticalArrangement) {
        ((VerticalArrangement) c).BackgroundColor(color);
      } else if (c instanceof HorizontalArrangement) {
        ((HorizontalArrangement) c).BackgroundColor(color);
      } else if (c instanceof VerticalScrollArrangement) {
        ((VerticalScrollArrangement) c).BackgroundColor(color);
      } else if (c instanceof HorizontalScrollArrangement) {
        ((HorizontalScrollArrangement) c).BackgroundColor(color);
      }
    } catch (Exception e) {
      // Set = tanpa feedback
    }
  }

  @SimpleFunction(description = "SET: perataan isi layout dinamis ber-id (tipe "
      + "wajib cocok). Angka SAMA seperti blok bawaan MIT: horizontal 1=kiri, "
      + "2=tengah, 3=kanan; vertikal 1=atas, 2=tengah, 3=bawah. Tanpa feedback.")
  public void SetLayoutAlignment(@Options(LayoutType.class) String type, int id, int horizontal,
      int vertical) {
    try {
      Component c = findTypedLayout(type, id);
      if (c instanceof VerticalArrangement) {
        ((VerticalArrangement) c).AlignHorizontal(horizontal);
        ((VerticalArrangement) c).AlignVertical(vertical);
      } else if (c instanceof HorizontalArrangement) {
        ((HorizontalArrangement) c).AlignHorizontal(horizontal);
        ((HorizontalArrangement) c).AlignVertical(vertical);
      } else if (c instanceof VerticalScrollArrangement) {
        ((VerticalScrollArrangement) c).AlignHorizontal(horizontal);
        ((VerticalScrollArrangement) c).AlignVertical(vertical);
      } else if (c instanceof HorizontalScrollArrangement) {
        ((HorizontalScrollArrangement) c).AlignHorizontal(horizontal);
        ((HorizontalScrollArrangement) c).AlignVertical(vertical);
      }
    } catch (Exception e) {
      // Set = tanpa feedback
    }
  }

  @SimpleFunction(description = "Lebar layout dinamis ber-id (angka MIT: -2 isi, "
      + "-1 otomatis, positif pixel). -3 bila id tidak ada / tipe beda.")
  public int GetLayoutWidth(@Options(LayoutType.class) String type, int id) {
    try {
      Component c = findTypedLayout(type, id);
      if (c instanceof AndroidViewComponent) {
        return ((AndroidViewComponent) c).Width();
      }
    } catch (Exception e) {
      // abaikan
    }
    return -3;
  }

  @SimpleFunction(description = "Tinggi layout dinamis ber-id (angka MIT). "
      + "-3 bila id tidak ada / tipe beda.")
  public int GetLayoutHeight(@Options(LayoutType.class) String type, int id) {
    try {
      Component c = findTypedLayout(type, id);
      if (c instanceof AndroidViewComponent) {
        return ((AndroidViewComponent) c).Height();
      }
    } catch (Exception e) {
      // abaikan
    }
    return -3;
  }

  // ================= EVENTS =================

  @SimpleEvent(description = "Layout dinamis jadi: id + nama tipe.")
  public void LayoutCreated(int id, String typeName) {
    Shared.fire(this, "LayoutCreated",
        Integer.valueOf(id), typeName);
  }

  @SimpleEvent(description = "Layout dinamis dilepas (RemoveLayoutId/ "
      + "SetClearLayouts/SetClearLayoutsIn): id yang hilang.")
  public void LayoutRemoved(int id) {
    Shared.fire(this, "LayoutRemoved", Integer.valueOf(id));
  }

  @SimpleEvent(description = "SATU-SATUNYA error Tools_Layout. component = nama "
      + "operasi (bandingkan dengan blok OpLayout; cth: if component = OpLayout "
      + "berarti error Layout). id = id terkait (0 bila umum).")
  public void ErrorOccurred(String component, int id, String message) {
    Shared.fire(this, "ErrorOccurred",
        component, Integer.valueOf(id), message);
  }

  @SimpleFunction(description = "Fallback polling untuk event ErrorOccurred: "
      + "pesan error terakhir operasi ini (kosong bila belum ada).")
  public String LastError(String operation) {
    return Shared.lastError(operation);
  }

  // ================= OP =================

  @SimpleProperty(description = "Nama operasi Layout (untuk if di "
      + "ErrorOccurred: cth if component = OpLayout berarti error Layout).",
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
