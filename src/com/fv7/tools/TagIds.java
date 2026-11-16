package com.fv7.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Registry id generik ber-Tag untuk komponen LUAR (mis. WebView pihak ketiga
 * yang olah id-nya kacau). Pengganti tracking manual.
 *
 * AUTO-DETEKSI: semua input id menerima Number MAUPUN teks angka ("5" = 5),
 * karena di blok perbandingan string vs number tidak akan pernah cocok.
 * Id 0 boleh (beberapa komponen luar pakai 0); negatif / bukan angka ditolak.
 *
 * Namespace SENDIRI: tidak berebut dengan id Download/Image/Audio/Video.
 * Hanya mencatat (tracking), tidak menghancurkan komponen luar.
 * Semua list URUT MENYIMPAN (pertama disimpan = index 1).
 * Java 7 only (tanpa diamond, tanpa lambda).
 */
public final class TagIds {

  private static final Map<String, LinkedHashSet<Integer>> tags =
      new LinkedHashMap<String, LinkedHashSet<Integer>>();

  private TagIds() {
  }

  private static String norm(String tag) {
    return tag == null ? "" : tag.trim();
  }

  /**
   * Ubah input blok jadi id. Number langsung dipakai; teks angka di-parse
   * (" 12 " = 12, "7.0" = 7); sisanya = -1 (tidak valid).
   */
  public static int toId(Object v) {
    try {
      if (v == null) {
        return -1;
      }
      if (v instanceof Number) {
        int n = ((Number) v).intValue();
        return n < 0 ? -1 : n;
      }
      String s = v.toString().trim();
      if (s.length() == 0) {
        return -1;
      }
      try {
        int n = Integer.parseInt(s);
        return n < 0 ? -1 : n;
      } catch (NumberFormatException e) {
        double d = Double.parseDouble(s.trim());
        if (Double.isNaN(d) || Double.isInfinite(d)) {
          return -1;
        }
        int n = (int) d;
        return n < 0 ? -1 : n;
      }
    } catch (Exception e) {
      return -1;
    }
  }

  /** Buat id unik baru di tag ini (angka terkecil >= 0 yang belum dipakai,
   *  langsung di-reservasi). Untuk saat kamulah yang menentukan id-nya. */
  public static synchronized int create(String tag) {
    String t = norm(tag);
    if (t.length() == 0) {
      return -1;
    }
    LinkedHashSet<Integer> s = tags.get(t);
    if (s == null) {
      s = new LinkedHashSet<Integer>();
      tags.put(t, s);
    }
    int id = 0;
    while (s.contains(Integer.valueOf(id))) {
      id = id + 1;
      if (id < 0) {
        return -1;
      }
    }
    s.add(Integer.valueOf(id));
    return id;
  }

  /** Simpan id ke tag. Id dobel = tetap satu (urutan pertama menang). */
  public static synchronized boolean save(Object idIn, String tag) {    int id = toId(idIn);
    String t = norm(tag);
    if (t.length() == 0 || id < 0) {
      return false;
    }
    LinkedHashSet<Integer> s = tags.get(t);
    if (s == null) {
      s = new LinkedHashSet<Integer>();
      tags.put(t, s);
    }
    Integer k = Integer.valueOf(id);
    if (s.contains(k)) {
      return true;
    }
    s.add(k);
    return true;
  }

  /** Semua id di tag ini, URUT MENYIMPAN (index 1 = pertama disimpan). */
  public static synchronized List<Integer> list(String tag) {
    ArrayList<Integer> out = new ArrayList<Integer>();
    LinkedHashSet<Integer> s = tags.get(norm(tag));
    if (s != null) {
      out.addAll(s);
    }
    return out;
  }

  /** Id index 1 = PERTAMA DISIMPAN di tag ini. -1 bila kosong. */
  public static synchronized int first(String tag) {
    LinkedHashSet<Integer> s = tags.get(norm(tag));
    if (s == null || s.isEmpty()) {
      return -1;
    }
    return s.iterator().next().intValue();
  }

  /** Id TERAKHIR DISIMPAN di tag ini. -1 bila kosong. */
  public static synchronized int last(String tag) {
    LinkedHashSet<Integer> s = tags.get(norm(tag));
    if (s == null || s.isEmpty()) {
      return -1;
    }
    Integer last = null;
    for (Integer k : s) {
      last = k;
    }
    return last.intValue();
  }

  /** Id yang disimpan TEPAT SEBELUM id ini di tag itu. -1 bila tidak ada. */
  public static synchronized int previous(Object idIn, String tag) {
    int id = toId(idIn);
    if (id < 0) {
      return -1;
    }
    LinkedHashSet<Integer> s = tags.get(norm(tag));
    if (s == null) {
      return -1;
    }
    Integer prev = null;
    for (Integer k : s) {
      if (k.intValue() == id) {
        return prev == null ? -1 : prev.intValue();
      }
      prev = k;
    }
    return -1;
  }

  /** True bila id ini tercatat di tag itu (teks angka ikut cocok). */
  public static synchronized boolean contains(Object idIn, String tag) {
    int id = toId(idIn);
    if (id < 0) {
      return false;
    }
    LinkedHashSet<Integer> s = tags.get(norm(tag));
    if (s == null) {
      return false;
    }
    return s.contains(Integer.valueOf(id));
  }

  /** Jumlah id tercatat di tag ini. */
  public static synchronized int count(String tag) {
    LinkedHashSet<Integer> s = tags.get(norm(tag));
    return s == null ? 0 : s.size();
  }

  /** Hapus satu id dari tag. Tag yang kosong ikut dibuang. */
  public static synchronized boolean remove(Object idIn, String tag) {
    int id = toId(idIn);
    if (id < 0) {
      return false;
    }
    String t = norm(tag);
    LinkedHashSet<Integer> s = tags.get(t);
    if (s == null) {
      return false;
    }
    boolean gone = s.remove(Integer.valueOf(id));
    if (s.isEmpty()) {
      tags.remove(t);
    }
    return gone;
  }

  /** Buang SELURUH isi satu tag. */
  public static synchronized void clear(String tag) {
    tags.remove(norm(tag));
  }

  /** Semua nama tag yang masih punya isi, URUT DIBUAT. */
  public static synchronized List<String> allTags() {
    return new ArrayList<String>(tags.keySet());
  }
}
