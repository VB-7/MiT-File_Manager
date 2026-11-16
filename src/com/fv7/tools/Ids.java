package com.fv7.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Satu pintu ID lintas extension (Download / Image / Audio / Video).
 * Id unik global (>= 1). Operasi multi-jalan wajib ber-id.
 *
 * URUTAN: semua list mengikuti urutan PEMBUATAN (pertama dibuat = index 1),
 * bukan urutan angka. Jadi GetDownloadIds / GetImageIds / GetAudioIds /
 * GetVideoIds / BusyIds / GetActive*Ids selalu creation-order.
 * Java 7 only (tanpa diamond, tanpa lambda).
 */
public final class Ids {

  public static final String DOWNLOAD = "Download";
  public static final String IMAGE = "Image";
  public static final String AUDIO = "Audio";
  public static final String VIDEO = "Video";
  public static final String CORE = "Core";
  public static final String LAYOUT = "Layout";
  public static final String SHEET = "Sheet";
  public static final String SLIDER = "Slider";
  public static final String RESERVED = "Reserved";

  private static final Set<Integer> claimed = new LinkedHashSet<Integer>();
  private static final Map<String, LinkedHashSet<Integer>> sets =
      new HashMap<String, LinkedHashSet<Integer>>();
  private static final Map<String, LinkedHashSet<Integer>> actives =
      new HashMap<String, LinkedHashSet<Integer>>();
  private static final Map<String, Integer> currents =
      new HashMap<String, Integer>();

  static {
    sets.put(DOWNLOAD, new LinkedHashSet<Integer>());
    sets.put(IMAGE, new LinkedHashSet<Integer>());
    sets.put(AUDIO, new LinkedHashSet<Integer>());
    sets.put(VIDEO, new LinkedHashSet<Integer>());
    sets.put(CORE, new LinkedHashSet<Integer>());
    sets.put(LAYOUT, new LinkedHashSet<Integer>());
    sets.put(SHEET, new LinkedHashSet<Integer>());
    sets.put(SLIDER, new LinkedHashSet<Integer>());
    sets.put(RESERVED, new LinkedHashSet<Integer>());
    actives.put(DOWNLOAD, new LinkedHashSet<Integer>());
    actives.put(IMAGE, new LinkedHashSet<Integer>());
    actives.put(AUDIO, new LinkedHashSet<Integer>());
    actives.put(VIDEO, new LinkedHashSet<Integer>());
    actives.put(CORE, new LinkedHashSet<Integer>());
    actives.put(LAYOUT, new LinkedHashSet<Integer>());
    actives.put(SHEET, new LinkedHashSet<Integer>());
    actives.put(SLIDER, new LinkedHashSet<Integer>());
    currents.put(DOWNLOAD, Integer.valueOf(-1));
    currents.put(IMAGE, Integer.valueOf(-1));
    currents.put(AUDIO, Integer.valueOf(-1));
    currents.put(VIDEO, Integer.valueOf(-1));
    currents.put(CORE, Integer.valueOf(-1));
    currents.put(LAYOUT, Integer.valueOf(-1));
    currents.put(SHEET, Integer.valueOf(-1));
    currents.put(SLIDER, Integer.valueOf(-1));
  }

  private Ids() {
  }

  private static boolean badId(int id) {
    return id < 1;
  }

  private static boolean badType(String type) {
    return !DOWNLOAD.equals(type) && !IMAGE.equals(type)
        && !AUDIO.equals(type) && !VIDEO.equals(type)
        && !CORE.equals(type) && !LAYOUT.equals(type)
        && !SHEET.equals(type) && !SLIDER.equals(type);
  }

  private static Integer lastOfSet(LinkedHashSet<Integer> s) {
    Integer last = null;
    for (Integer k : s) {
      last = k;
    }
    return last;
  }

  private static void fixCurrent(String type) {
    LinkedHashSet<Integer> s = sets.get(type);
    Integer cur = currents.get(type);
    if (cur != null && cur.intValue() > 0 && s.contains(cur)) {
      return;
    }
    if (s.isEmpty()) {
      currents.put(type, Integer.valueOf(-1));
    } else {
      currents.put(type, lastOfSet(s));
    }
  }

  // ================= global (kompatibel lama) =================

  /** Id yang disarankan: angka terkecil >= 1 yang belum dipakai siapa pun. */
  public static synchronized int suggest() {
    int id = 1;
    while (claimed.contains(Integer.valueOf(id))) {
      id = id + 1;
      if (id < 1) {
        return -1;
      }
    }
    return id;
  }

  /** Klaim id tanpa tipe (lama). False = sudah dipakai. */
  public static synchronized boolean claim(int id) {
    if (badId(id)) {
      return false;
    }
    Integer k = Integer.valueOf(id);
    if (claimed.contains(k)) {
      return false;
    }
    claimed.add(k);
    return true;
  }

  /** Lepas id apa pun (global + semua tipe + reserved). */
  public static synchronized void release(int id) {
    if (badId(id)) {
      return;
    }
    Integer k = Integer.valueOf(id);
    claimed.remove(k);
    sets.get(DOWNLOAD).remove(k);
    sets.get(IMAGE).remove(k);
    sets.get(AUDIO).remove(k);
    sets.get(VIDEO).remove(k);
    sets.get(RESERVED).remove(k);
    actives.get(DOWNLOAD).remove(k);
    actives.get(IMAGE).remove(k);
    actives.get(AUDIO).remove(k);
    actives.get(VIDEO).remove(k);
    fixCurrent(DOWNLOAD);
    fixCurrent(IMAGE);
    fixCurrent(AUDIO);
    fixCurrent(VIDEO);
  }

  public static synchronized boolean isBusy(int id) {
    if (badId(id)) {
      return false;
    }
    return claimed.contains(Integer.valueOf(id));
  }

  /** Semua id sibuk global, URUT PEMBUATAN (pertama dibuat = index 1). */
  public static synchronized List<Integer> busyList() {
    return new ArrayList<Integer>(claimed);
  }

  // ================= buat baru =================

  /**
   * Buat id unik global (di-reservasi). Pakai untuk tipe apa pun:
   * fungsi Start akan mengadopsinya otomatis.
   */
  public static synchronized int createUnique() {
    int id = suggest();
    if (id < 1) {
      return -1;
    }
    Integer k = Integer.valueOf(id);
    claimed.add(k);
    sets.get(RESERVED).add(k);
    return id;
  }

  /** Buat id unik untuk satu tipe (masuk daftar tipe itu, belum aktif). */
  public static synchronized int createFor(String type) {
    if (badType(type)) {
      return -1;
    }
    int id = suggest();
    if (id < 1) {
      return -1;
    }
    Integer k = Integer.valueOf(id);
    claimed.add(k);
    sets.get(type).add(k);
    currents.put(type, k);
    return id;
  }

  /**
   * Daftarkan id ke satu tipe. True = boleh dipakai.
   * Id reservasi (CreateUniqueId / CreateXId lain yang belum start) diadopsi.
   * Id milik tipe lain / klaim mentah = ditolak (false).
   */
  public static synchronized boolean register(String type, int id) {
    if (badType(type) || badId(id)) {
      return false;
    }
    Integer k = Integer.valueOf(id);
    LinkedHashSet<Integer> mine = sets.get(type);
    if (mine.contains(k)) {
      currents.put(type, k);
      return true;
    }
    if (claimed.contains(k)) {
      if (sets.get(RESERVED).contains(k)) {
        sets.get(RESERVED).remove(k);
        mine.add(k);
        currents.put(type, k);
        return true;
      }
      return false;
    }
    claimed.add(k);
    mine.add(k);
    currents.put(type, k);
    return true;
  }

  /** Lepas id dari satu tipe (sekaligus dari global bila milik tipe ini). */
  public static synchronized void unregister(String type, int id) {
    if (badType(type) || badId(id)) {
      return;
    }
    Integer k = Integer.valueOf(id);
    sets.get(type).remove(k);
    actives.get(type).remove(k);
    if (!sets.get(DOWNLOAD).contains(k) && !sets.get(IMAGE).contains(k)
        && !sets.get(AUDIO).contains(k) && !sets.get(VIDEO).contains(k)
        && !sets.get(RESERVED).contains(k)) {
      claimed.remove(k);
    }
    fixCurrent(type);
  }

  // ================= aktif =================

  public static synchronized void setActive(String type, int id, boolean active) {
    if (badType(type) || badId(id)) {
      return;
    }
    Integer k = Integer.valueOf(id);
    if (!sets.get(type).contains(k)) {
      return;
    }
    LinkedHashSet<Integer> a = actives.get(type);
    if (active) {
      a.remove(k);
      a.add(k);
    } else {
      a.remove(k);
    }
  }

  public static synchronized boolean isActive(String type, int id) {
    if (badType(type) || badId(id)) {
      return false;
    }
    return actives.get(type).contains(Integer.valueOf(id));
  }

  // ================= baca (semua URUT PEMBUATAN) =================

  /** Semua id tipe ini, URUT PEMBUATAN (index 1 = pertama dibuat). */
  public static synchronized List<Integer> idsOf(String type) {
    ArrayList<Integer> out = new ArrayList<Integer>();
    if (badType(type)) {
      return out;
    }
    out.addAll(sets.get(type));
    return out;
  }

  /** Id aktif tipe ini saja, URUT PEMBUATAN. */
  public static synchronized List<Integer> activeIdsOf(String type) {
    ArrayList<Integer> out = new ArrayList<Integer>();
    if (badType(type)) {
      return out;
    }
    out.addAll(actives.get(type));
    return out;
  }

  /** Id yang terakhir dibuat/di-start untuk tipe ini. -1 bila kosong. */
  public static synchronized int currentOf(String type) {
    if (badType(type)) {
      return -1;
    }
    Integer c = currents.get(type);
    return c == null ? -1 : c.intValue();
  }

  /** Id index 1 = PERTAMA DIBUAT untuk tipe ini. -1 bila kosong. */
  public static synchronized int firstOf(String type) {
    if (badType(type)) {
      return -1;
    }
    LinkedHashSet<Integer> s = sets.get(type);
    if (s.isEmpty()) {
      return -1;
    }
    return s.iterator().next().intValue();
  }

  /** Id TERAKHIR DIBUAT untuk tipe ini. -1 bila kosong. */
  public static synchronized int lastOf(String type) {
    if (badType(type)) {
      return -1;
    }
    LinkedHashSet<Integer> s = sets.get(type);
    if (s.isEmpty()) {
      return -1;
    }
    return lastOfSet(s).intValue();
  }

  /**
   * Id yang dibuat TEPAT SEBELUM id ini (dalam urutan pembuatan).
   * -1 bila id tidak ada / tidak ada pendahulu.
   */
  public static synchronized int previousOf(String type, int id) {
    if (badType(type) || badId(id)) {
      return -1;
    }
    Integer prev = null;
    for (Integer k : sets.get(type)) {
      if (k.intValue() == id) {
        return prev == null ? -1 : prev.intValue();
      }
      prev = k;
    }
    return -1;
  }
}
