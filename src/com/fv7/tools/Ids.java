package com.fv7.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Satu pintu ID lintas extension (dipakai Tools_Downloader + Tools_Image).
 * Id diklaim saat operasi mulai, dilepas saat selesai/batal.
 */
public final class Ids {

  private static final Set<Integer> claimed = new HashSet<Integer>();

  private Ids() {
  }

  /** Id yang disarankan: angka terkecil >= 1 yang belum dipakai (tidak diklaim). */
  public static synchronized int suggest() {
    int id = 1;
    while (claimed.contains(Integer.valueOf(id))) {
      id = id + 1;
    }
    return id;
  }

  /** Klaim id. False = sudah dipakai orang. */
  public static synchronized boolean claim(int id) {
    Integer k = Integer.valueOf(id);
    if (claimed.contains(k)) {
      return false;
    }
    claimed.add(k);
    return true;
  }

  /** Lepas id (selesai/batal/paksa). */
  public static synchronized void release(int id) {
    claimed.remove(Integer.valueOf(id));
  }

  public static synchronized boolean isBusy(int id) {
    return claimed.contains(Integer.valueOf(id));
  }

  public static synchronized List<Integer> busyList() {
    ArrayList<Integer> out = new ArrayList<Integer>(claimed);
    Collections.sort(out);
    return out;
  }
}
