package com.fv7.tools;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Mesin BitTorrent murni-Java untuk Tools_Downloader (tanpa library luar,
 * Java 7, tanpa blok baru — dipanggil otomatis dari Download bila url-nya
 * magnet: atau link http(s) file .torrent).
 *
 * <p>Dukungan v1: magnet ber-tracker (param tr=) + file .torrent (http(s)).
 * Tracker HTTP(S) + UDP. Magnet TANPA tracker (murni DHT) belum didukung.
 * File .torrent LOKAL (path/file:/content:) tetap disalin apa adanya oleh
 * Download (bukan diunduh isinya).</p>
 */
final class TorrentEngine {

  private TorrentEngine() {
  }

  /** Batas unduh file .torrent / metadata (10 MB). */
  static final int MAX_META_BYTES = 10 * 1024 * 1024;
  /** Ukuran satu block permintaan P2P (standar 16 KB). */
  static final int BLOCK = 16384;
  /** Maksimum koneksi peer bersamaan. */
  static final int MAX_PEERS = 16;

  /** Dilempar bila user membatalkan (pause/resume/cancel lewat Listener). */
  static final class Cancelled extends IOException {
    Cancelled() {
      super("Dibatalkan");
    }
  }

  /** Jembatan ke status operasi Download (pause/cancel + event progres). */
  interface Listener {
    boolean isCancelled();
    void waitIfPaused() throws Cancelled;
    void onStarted(String name, String savePath, long total);
    void onProgress(long done, long total);
  }

  /** Hasil unduh torrent: nama + path akhir (file / folder) + total byte. */
  static final class Result {
    String name = "";
    String savePath = "";
    long total = 0;
  }

  // ================= deteksi input =================

  static boolean isMagnetLink(String s) {
    if (s == null) {
      return false;
    }
    String t = s.trim();
    return t.regionMatches(true, 0, "magnet:", 0, 7);
  }

  /** Path URL tanpa query/fragment, huruf kecil. */
  static String urlPathOf(String url) {
    try {
      String u = url == null ? "" : url.trim();
      int q = u.indexOf('?');
      if (q >= 0) {
        u = u.substring(0, q);
      }
      int h = u.indexOf('#');
      if (h >= 0) {
        u = u.substring(0, h);
      }
      return u.toLowerCase();
    } catch (Exception e) {
      return "";
    }
  }

  /** True bila link http(s) ini kandidat torrent (ekstensi / mime). */
  static boolean isTorrentTarget(String url, String mime) {
    String u = url == null ? "" : url.trim().toLowerCase();
    if (!u.startsWith("http://") && !u.startsWith("https://")) {
      return false;
    }
    if (urlPathOf(u).endsWith(".torrent")) {
      return true;
    }
    if (mime != null && mime.toLowerCase().indexOf("torrent") >= 0) {
      return true;
    }
    return false;
  }

  // ================= magnet =================

  static final class Magnet {
    byte[] infoHash = null;
    String name = "";
    ArrayList<String> trackers = new ArrayList<String>();
  }

  /** Ambil nilai param query mentah (belum di-decode) — boleh muncul berkali-kali. */
  private static ArrayList<String> queryParams(String q, String key) {
    ArrayList<String> out = new ArrayList<String>();
    try {
      String[] ps = q.split("&");
      for (int i = 0; i < ps.length; i = i + 1) {
        String p = ps[i];
        int e = p.indexOf('=');
        String k = e >= 0 ? p.substring(0, e) : p;
        String v = e >= 0 ? p.substring(e + 1) : "";
        if (k.equalsIgnoreCase(key)) {
          out.add(v);
        }
      }
    } catch (Exception e) {
      // abaikan
    }
    return out;
  }

  private static String urlDecode(String s) {
    try {
      return URLDecoder.decode(s, "UTF-8");
    } catch (Exception e) {
      return s;
    }
  }

  private static byte[] hexToBytes(String h) {
    try {
      String s = h.trim();
      if (s.length() != 40) {
        return null;
      }
      byte[] out = new byte[20];
      for (int i = 0; i < 20; i = i + 1) {
        int v = Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        out[i] = (byte) v;
      }
      return out;
    } catch (Exception e) {
      return null;
    }
  }

  private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

  private static byte[] base32ToBytes(String s) {
    try {
      String t = s.trim().toUpperCase().replace("=", "");
      if (t.length() != 32) {
        return null;
      }
      byte[] out = new byte[20];
      int bits = 0;
      int val = 0;
      int pos = 0;
      for (int i = 0; i < t.length(); i = i + 1) {
        int v = BASE32.indexOf(t.charAt(i));
        if (v < 0) {
          return null;
        }
        val = (val << 5) | v;
        bits = bits + 5;
        if (bits >= 8) {
          bits = bits - 8;
          out[pos] = (byte) ((val >> bits) & 0xFF);
          pos = pos + 1;
        }
      }
      return pos == 20 ? out : null;
    } catch (Exception e) {
      return null;
    }
  }

  static Magnet parseMagnet(String uri) throws IOException {
    Magnet m = new Magnet();
    try {
      String u = uri == null ? "" : uri.trim();
      int q = u.indexOf('?');
      String query = q >= 0 ? u.substring(q + 1) : "";
      ArrayList<String> xts = queryParams(query, "xt");
      for (int i = 0; i < xts.size(); i = i + 1) {
        String xt = urlDecode(xts.get(i)).trim();
        String low = xt.toLowerCase();
        if (low.startsWith("urn:btih:")) {
          String h = xt.substring(9);
          byte[] b = h.length() == 40 ? hexToBytes(h) : base32ToBytes(h);
          if (b != null) {
            m.infoHash = b;
            break;
          }
        }
      }
      if (m.infoHash == null) {
        throw new IOException("magnet tanpa info-hash (xt=urn:btih:...)");
      }
      ArrayList<String> dns = queryParams(query, "dn");
      if (!dns.isEmpty()) {
        m.name = urlDecode(dns.get(0)).trim();
      }
      ArrayList<String> trs = queryParams(query, "tr");
      for (int i = 0; i < trs.size(); i = i + 1) {
        String t = urlDecode(trs.get(i)).trim();
        if (t.length() > 0) {
          m.trackers.add(t);
        }
      }
      return m;
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException("magnet tidak valid");
    }
  }

  /** Nama tampilan magnet tanpa internet (param dn=), kosong bila tak ada. */
  static String magnetDisplayName(String uri) {
    try {
      String u = uri == null ? "" : uri.trim();
      int q = u.indexOf('?');
      if (q < 0) {
        return "";
      }
      ArrayList<String> dns = queryParams(u.substring(q + 1), "dn");
      if (!dns.isEmpty()) {
        return urlDecode(dns.get(0)).trim();
      }
    } catch (Exception e) {
      // abaikan
    }
    return "";
  }

  // ================= bencode =================

  /** Hasil decode: nilai + posisi akhir (untuk potong info dict / pesan). */
  private static final class BVal {
    Object val;
    int end;
  }

  private static BVal decode(byte[] b, int pos) throws IOException {
    if (pos >= b.length) {
      throw new IOException("bencode terpotong");
    }
    byte c = b[pos];
    if (c == 'i') {
      int e = indexOf(b, (byte) 'e', pos + 1);
      if (e < 0) {
        throw new IOException("bencode int rusak");
      }
      long v;
      try {
        v = Long.parseLong(new String(b, pos + 1, e - pos - 1, "UTF-8"));
      } catch (Exception ex) {
        throw new IOException("bencode int rusak");
      }
      BVal r = new BVal();
      r.val = Long.valueOf(v);
      r.end = e + 1;
      return r;
    }
    if (c == 'l' || c == 'd') {
      boolean isList = c == 'l';
      Object box = isList ? (Object) new ArrayList<Object>()
          : (Object) new HashMap<String, Object>();
      int p = pos + 1;
      while (true) {
        if (p >= b.length) {
          throw new IOException("bencode terpotong");
        }
        if (b[p] == 'e') {
          BVal r = new BVal();
          r.val = box;
          r.end = p + 1;
          return r;
        }
        if (isList) {
          BVal v = decode(b, p);
          ((ArrayList<Object>) box).add(v.val);
          p = v.end;
        } else {
          BVal k = decode(b, p);
          if (!(k.val instanceof byte[])) {
            throw new IOException("kunci dict bukan string");
          }
          BVal v = decode(b, k.end);
          String ks;
          try {
            ks = new String((byte[]) k.val, "UTF-8");
          } catch (Exception ex) {
            ks = "";
          }
          ((HashMap<String, Object>) box).put(ks, v.val);
          p = v.end;
        }
      }
    }
    if (c >= '0' && c <= '9') {
      int colon = indexOf(b, (byte) ':', pos);
      if (colon < 0) {
        throw new IOException("bencode string rusak");
      }
      int len;
      try {
        len = Integer.parseInt(new String(b, pos, colon - pos, "UTF-8"));
      } catch (Exception ex) {
        throw new IOException("bencode string rusak");
      }
      if (len < 0 || colon + 1 + len > b.length) {
        throw new IOException("bencode string rusak");
      }
      byte[] s = new byte[len];
      System.arraycopy(b, colon + 1, s, 0, len);
      BVal r = new BVal();
      r.val = s;
      r.end = colon + 1 + len;
      return r;
    }
    throw new IOException("bencode tidak dikenal");
  }

  private static int indexOf(byte[] b, byte v, int from) {
    for (int i = from; i < b.length; i = i + 1) {
      if (b[i] == v) {
        return i;
      }
    }
    return -1;
  }

  private static void bencode(Object o, ByteArrayOutputStream out) throws IOException {
    if (o instanceof Long) {
      out.write('i');
      byte[] n = ((Long) o).toString().getBytes("UTF-8");
      out.write(n, 0, n.length);
      out.write('e');
    } else if (o instanceof Integer) {
      out.write('i');
      byte[] n = ((Integer) o).toString().getBytes("UTF-8");
      out.write(n, 0, n.length);
      out.write('e');
    } else if (o instanceof byte[]) {
      byte[] s = (byte[]) o;
      byte[] n = Integer.toString(s.length).getBytes("UTF-8");
      out.write(n, 0, n.length);
      out.write(':');
      out.write(s, 0, s.length);
    } else if (o instanceof String) {
      byte[] s = ((String) o).getBytes("UTF-8");
      byte[] n = Integer.toString(s.length).getBytes("UTF-8");
      out.write(n, 0, n.length);
      out.write(':');
      out.write(s, 0, s.length);
    } else if (o instanceof List) {
      out.write('l');
      List<Object> l = (List<Object>) o;
      for (int i = 0; i < l.size(); i = i + 1) {
        bencode(l.get(i), out);
      }
      out.write('e');
    } else if (o instanceof Map) {
      out.write('d');
      Map<String, Object> m = (Map<String, Object>) o;
      ArrayList<String> ks = new ArrayList<String>(m.keySet());
      Collections.sort(ks);
      for (int i = 0; i < ks.size(); i = i + 1) {
        bencode(ks.get(i).getBytes("UTF-8"), out);
        bencode(m.get(ks.get(i)), out);
      }
      out.write('e');
    } else {
      throw new IOException("bencode: tipe tak didukung");
    }
  }

  private static byte[] bencodeBytes(Object o) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    bencode(o, bos);
    bos.flush();
    return bos.toByteArray();
  }

  private static String asString(Object o) {
    try {
      if (o instanceof byte[]) {
        return new String((byte[]) o, "UTF-8");
      }
    } catch (Exception e) {
      // abaikan
    }
    return "";
  }

  private static long asLong(Object o, long def) {
    try {
      if (o instanceof Long) {
        return ((Long) o).longValue();
      }
      if (o instanceof Integer) {
        return ((Integer) o).intValue();
      }
    } catch (Exception e) {
      // abaikan
    }
    return def;
  }

  // ================= struktur torrent =================

  static final class TorrentFile {
    String path = "";
    long length = 0;
    long start = 0;
  }

  static final class Torrent {
    byte[] infoHash = null;
    String name = "";
    long pieceLength = 0;
    byte[] pieceHashes = new byte[0];
    int pieceCount = 0;
    long totalLength = 0;
    boolean multi = false;
    ArrayList<TorrentFile> files = new ArrayList<TorrentFile>();
    ArrayList<String> trackers = new ArrayList<String>();
  }

  /** Parse byte file .torrent (validasi + potong info dict asli untuk info-hash). */
  static Torrent parseTorrent(byte[] data) throws IOException {
    if (data == null || data.length < 10) {
      throw new IOException("bukan file torrent");
    }
    try {
      BVal top = decode(data, 0);
      if (!(top.val instanceof Map)) {
        throw new IOException("bukan file torrent");
      }
      Map<String, Object> m = (Map<String, Object>) top.val;
      Object infoObj = m.get("info");
      if (!(infoObj instanceof Map)) {
        throw new IOException("bukan file torrent (tanpa info)");
      }
      // Cari offset mentah "4:info" di top-level untuk info-hash yang tepat.
      int infoStart = -1;
      int infoEnd = -1;
      int p = 1; // lewati 'd'
      while (p < data.length && data[p] != 'e') {
        BVal k = decode(data, p);
        BVal v = decode(data, k.end);
        if (k.val instanceof byte[]) {
          String ks = new String((byte[]) k.val, "UTF-8");
          if (ks.equals("info")) {
            infoStart = k.end;
            infoEnd = v.end;
            break;
          }
        }
        p = v.end;
      }
      if (infoStart < 0) {
        throw new IOException("bukan file torrent (info rusak)");
      }
      byte[] infoBytes = new byte[infoEnd - infoStart];
      System.arraycopy(data, infoStart, infoBytes, 0, infoBytes.length);

      Map<String, Object> info = (Map<String, Object>) infoObj;
      Torrent t = new Torrent();
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      t.infoHash = md.digest(infoBytes);
      t.name = asString(info.get("name"));
      if (t.name.length() == 0) {
        t.name = "torrent";
      }
      t.pieceLength = asLong(info.get("piece length"), 0);
      Object pieces = info.get("pieces");
      if (!(pieces instanceof byte[])) {
        throw new IOException("torrent tanpa daftar piece");
      }
      t.pieceHashes = (byte[]) pieces;
      if (t.pieceHashes.length % 20 != 0 || t.pieceHashes.length == 0) {
        throw new IOException("daftar piece rusak");
      }
      if (t.pieceLength <= 0) {
        throw new IOException("panjang piece rusak");
      }
      t.pieceCount = t.pieceHashes.length / 20;
      Object lenObj = info.get("length");
      Object filesObj = info.get("files");
      if (lenObj instanceof Long || lenObj instanceof Integer) {
        t.multi = false;
        TorrentFile f = new TorrentFile();
        f.path = t.name;
        f.length = asLong(lenObj, 0);
        t.files.add(f);
      } else if (filesObj instanceof List) {
        t.multi = true;
        List<Object> fl = (List<Object>) filesObj;
        for (int i = 0; i < fl.size(); i = i + 1) {
          if (!(fl.get(i) instanceof Map)) {
            continue;
          }
          Map<String, Object> fm = (Map<String, Object>) fl.get(i);
          long flen = asLong(fm.get("length"), -1);
          Object pathObj = fm.get("path");
          if (flen < 0 || !(pathObj instanceof List)) {
            continue;
          }
          List<Object> pc = (List<Object>) pathObj;
          StringBuilder sb = new StringBuilder();
          for (int j = 0; j < pc.size(); j = j + 1) {
            String comp = Shared.sanitize(asString(pc.get(j)));
            if (comp.length() == 0 || comp.equals(".") || comp.equals("..")) {
              continue;
            }
            if (sb.length() > 0) {
              sb.append('/');
            }
            sb.append(comp);
          }
          if (sb.length() == 0) {
            continue;
          }
          TorrentFile f = new TorrentFile();
          f.path = sb.toString();
          f.length = flen;
          t.files.add(f);
        }
        if (t.files.isEmpty()) {
          throw new IOException("torrent tanpa file");
        }
      } else {
        throw new IOException("torrent tanpa panjang file");
      }
      long total = 0;
      for (int i = 0; i < t.files.size(); i = i + 1) {
        t.files.get(i).start = total;
        total = total + t.files.get(i).length;
      }
      t.totalLength = total;
      if (t.totalLength <= 0) {
        throw new IOException("torrent kosong (0 byte)");
      }
      // Tracker: announce + announce-list.
      Object an = m.get("announce");
      if (an instanceof byte[]) {
        String s = asString(an).trim();
        if (s.length() > 0) {
          t.trackers.add(s);
        }
      }
      Object al = m.get("announce-list");
      if (al instanceof List) {
        List<Object> tier = (List<Object>) al;
        for (int i = 0; i < tier.size(); i = i + 1) {
          if (tier.get(i) instanceof List) {
            List<Object> inner = (List<Object>) tier.get(i);
            for (int j = 0; j < inner.size(); j = j + 1) {
              String s = asString(inner.get(j)).trim();
              if (s.length() > 0 && !t.trackers.contains(s)) {
                t.trackers.add(s);
              }
            }
          } else {
            String s = asString(tier.get(i)).trim();
            if (s.length() > 0 && !t.trackers.contains(s)) {
              t.trackers.add(s);
            }
          }
        }
      }
      return t;
    } catch (IOException e) {
      String msg = e.getMessage();
      throw new IOException(msg == null ? "torrent rusak" : msg);
    } catch (Exception e) {
      throw new IOException("torrent rusak");
    }
  }

  // ================= unduh byte http (file .torrent) =================

  static byte[] fetchHttpBytes(String url) throws IOException {
    HttpURLConnection c = null;
    try {
      c = (HttpURLConnection) new URL(url).openConnection();
      c.setConnectTimeout(20000);
      c.setReadTimeout(20000);
      c.setInstanceFollowRedirects(true);
      c.setRequestProperty("Accept-Encoding", "identity");
      c.setRequestProperty("User-Agent", "Mozilla/5.0");
      int code = c.getResponseCode();
      if (code >= 400) {
        throw new IOException("HTTP " + code);
      }
      long len = -1;
      try {
        String h = c.getHeaderField("Content-Length");
        if (h != null) {
          len = Long.parseLong(h.trim());
        }
      } catch (Exception e) {
        len = -1;
      }
      if (len > MAX_META_BYTES) {
        throw new IOException("file terlalu besar untuk torrent");
      }
      InputStream in = c.getInputStream();
      try {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[32768];
        int n;
        int total = 0;
        while ((n = in.read(buf)) > 0) {
          total = total + n;
          if (total > MAX_META_BYTES) {
            throw new IOException("file terlalu besar untuk torrent");
          }
          bos.write(buf, 0, n);
        }
        bos.flush();
        return bos.toByteArray();
      } finally {
        try {
          in.close();
        } catch (Exception e) {
          // abaikan
        }
      }
    } finally {
      if (c != null) {
        try {
          c.disconnect();
        } catch (Exception e) {
          // abaikan
        }
      }
    }
  }

  // ================= peer id =================

  private static final Random RAND = new Random();

  static byte[] makePeerId() {
    try {
      String head = "-FV7001-";
      String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
      StringBuilder sb = new StringBuilder(head);
      synchronized (RAND) {
        for (int i = 0; i < 12; i = i + 1) {
          sb.append(chars.charAt(RAND.nextInt(chars.length())));
        }
      }
      return sb.toString().getBytes("UTF-8");
    } catch (Exception e) {
      return "01234567890123456789".getBytes();
    }
  }

  // ================= tracker =================

  private static String encBytes(byte[] b) {
    try {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < b.length; i = i + 1) {
        int v = b[i] & 0xFF;
        if ((v >= '0' && v <= '9') || (v >= 'A' && v <= 'Z')
            || (v >= 'a' && v <= 'z') || v == '-' || v == '_'
            || v == '.' || v == '~') {
          sb.append((char) v);
        } else {
          sb.append('%');
          String h = Integer.toHexString(v).toUpperCase();
          if (h.length() < 2) {
            sb.append('0');
          }
          sb.append(h);
        }
      }
      return sb.toString();
    } catch (Exception e) {
      return "";
    }
  }

  /** Minta daftar peer ke SEMUA tracker yang bisa dihubungi (http + udp). */
  static ArrayList<String> announceAll(List<String> trackers, byte[] infoHash,
      byte[] peerId, long downloaded, long left, Listener l) {
    ArrayList<String> out = new ArrayList<String>();
    HashSet<String> seen = new HashSet<String>();
    if (trackers == null) {
      return out;
    }
    int tried = 0;
    for (int i = 0; i < trackers.size(); i = i + 1) {
      if (l.isCancelled()) {
        return out;
      }
      if (tried >= 8) {
        break;
      }
      String tr = trackers.get(i);
      if (tr == null) {
        continue;
      }
      String low = tr.trim().toLowerCase();
      ArrayList<String> got = null;
      if (low.startsWith("http://") || low.startsWith("https://")) {
        tried = tried + 1;
        got = announceHttp(tr, infoHash, peerId, downloaded, left);
      } else if (low.startsWith("udp://")) {
        tried = tried + 1;
        got = announceUdp(tr, infoHash, peerId, downloaded, left);
      }
      if (got != null) {
        for (int j = 0; j < got.size(); j = j + 1) {
          if (!seen.contains(got.get(j))) {
            seen.add(got.get(j));
            out.add(got.get(j));
          }
        }
      }
      if (out.size() >= 100) {
        break;
      }
    }
    return out;
  }

  private static ArrayList<String> announceHttp(String tracker, byte[] infoHash,
      byte[] peerId, long downloaded, long left) {
    ArrayList<String> out = new ArrayList<String>();
    HttpURLConnection c = null;
    try {
      String sep = tracker.indexOf('?') >= 0 ? "&" : "?";
      String url = tracker + sep + "info_hash=" + encBytes(infoHash)
          + "&peer_id=" + encBytes(peerId) + "&port=6881"
          + "&uploaded=0&downloaded=" + downloaded + "&left=" + left
          + "&compact=1&no_peer_id=1&event=started&numwant=100";
      c = (HttpURLConnection) new URL(url).openConnection();
      c.setConnectTimeout(12000);
      c.setReadTimeout(12000);
      c.setInstanceFollowRedirects(true);
      c.setRequestProperty("User-Agent", "Mozilla/5.0");
      int code = c.getResponseCode();
      if (code >= 400) {
        return out;
      }
      InputStream in = c.getInputStream();
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      try {
        byte[] buf = new byte[32768];
        int n;
        int total = 0;
        while ((n = in.read(buf)) > 0) {
          total = total + n;
          if (total > 1048576) {
            break;
          }
          bos.write(buf, 0, n);
        }
        bos.flush();
      } finally {
        try {
          in.close();
        } catch (Exception e) {
          // abaikan
        }
      }
      BVal r = decode(bos.toByteArray(), 0);
      if (!(r.val instanceof Map)) {
        return out;
      }
      Map<String, Object> m = (Map<String, Object>) r.val;
      if (m.get("failure reason") instanceof byte[]) {
        return out;
      }
      Object peers = m.get("peers");
      if (peers instanceof byte[]) {
        byte[] pb = (byte[]) peers;
        for (int i = 0; i + 6 <= pb.length; i = i + 6) {
          String ip = (pb[i] & 0xFF) + "." + (pb[i + 1] & 0xFF) + "."
              + (pb[i + 2] & 0xFF) + "." + (pb[i + 3] & 0xFF);
          int port = ((pb[i + 4] & 0xFF) << 8) | (pb[i + 5] & 0xFF);
          if (port > 0 && !ip.equals("0.0.0.0")) {
            out.add(ip + ":" + port);
          }
        }
      } else if (peers instanceof List) {
        List<Object> pl = (List<Object>) peers;
        for (int i = 0; i < pl.size(); i = i + 1) {
          if (pl.get(i) instanceof Map) {
            Map<String, Object> pm = (Map<String, Object>) pl.get(i);
            String ip = asString(pm.get("ip")).trim();
            long port = asLong(pm.get("port"), 0);
            if (ip.length() > 0 && port > 0 && port < 65536) {
              out.add(ip + ":" + port);
            }
          }
        }
      }
    } catch (Exception e) {
      // tracker mati = coba berikutnya
    } finally {
      if (c != null) {
        try {
          c.disconnect();
        } catch (Exception e) {
          // abaikan
        }
      }
    }
    return out;
  }

  private static final long UDP_MAGIC = 0x41727101980L;

  private static ArrayList<String> announceUdp(String tracker, byte[] infoHash,
      byte[] peerId, long downloaded, long left) {
    ArrayList<String> out = new ArrayList<String>();
    DatagramSocket ds = null;
    try {
      String t = tracker.trim();
      int p0 = t.indexOf("://") + 3;
      int p1 = t.indexOf('/', p0);
      String hostPort = p1 >= 0 ? t.substring(p0, p1) : t.substring(p0);
      String host = hostPort;
      int port = 80;
      int c = hostPort.lastIndexOf(':');
      if (c >= 0) {
        host = hostPort.substring(0, c);
        try {
          port = Integer.parseInt(hostPort.substring(c + 1));
        } catch (Exception e) {
          port = 80;
        }
      }
      InetAddress addr = InetAddress.getByName(host);
      ds = new DatagramSocket();
      ds.setSoTimeout(8000);
      int trans;
      synchronized (RAND) {
        trans = RAND.nextInt(Integer.MAX_VALUE);
      }
      // connect
      java.io.ByteArrayOutputStream cbos = new java.io.ByteArrayOutputStream();
      DataOutputStream cdos = new DataOutputStream(cbos);
      cdos.writeLong(UDP_MAGIC);
      cdos.writeInt(0);
      cdos.writeInt(trans);
      cdos.flush();
      byte[] creq = cbos.toByteArray();
      long connId = -1;
      for (int a = 0; a < 2; a = a + 1) {
        DatagramPacket p = new DatagramPacket(creq, creq.length, addr, port);
        ds.send(p);
        byte[] rb = new byte[16];
        DatagramPacket rp = new DatagramPacket(rb, rb.length);
        try {
          ds.receive(rp);
        } catch (Exception e) {
          continue;
        }
        if (rp.getLength() < 16) {
          continue;
        }
        DataInputStream din = new DataInputStream(
            new java.io.ByteArrayInputStream(rb, 0, rp.getLength()));
        int action = din.readInt();
        int tr = din.readInt();
        if (action == 0 && tr == trans) {
          connId = din.readLong();
          break;
        }
      }
      if (connId == -1) {
        return out;
      }
      // announce
      int trans2;
      synchronized (RAND) {
        trans2 = RAND.nextInt(Integer.MAX_VALUE);
        if (trans2 == trans) {
          trans2 = trans + 1;
        }
      }
      java.io.ByteArrayOutputStream abos = new java.io.ByteArrayOutputStream();
      DataOutputStream ados = new DataOutputStream(abos);
      ados.writeLong(connId);
      ados.writeInt(1);
      ados.writeInt(trans2);
      ados.write(infoHash, 0, 20);
      ados.write(peerId, 0, 20);
      ados.writeLong(downloaded);
      ados.writeLong(left);
      ados.writeLong(0);
      ados.writeInt(2);
      ados.writeInt(0);
      synchronized (RAND) {
        ados.writeInt(RAND.nextInt());
      }
      ados.writeInt(100);
      ados.writeShort(6881);
      ados.flush();
      byte[] areq = abos.toByteArray();
      for (int a = 0; a < 2; a = a + 1) {
        DatagramPacket p = new DatagramPacket(areq, areq.length, addr, port);
        ds.send(p);
        byte[] rb = new byte[4096];
        DatagramPacket rp = new DatagramPacket(rb, rb.length);
        try {
          ds.receive(rp);
        } catch (Exception e) {
          continue;
        }
        if (rp.getLength() < 20) {
          continue;
        }
        DataInputStream din = new DataInputStream(
            new java.io.ByteArrayInputStream(rb, 0, rp.getLength()));
        int action = din.readInt();
        int tr = din.readInt();
        if (action != 1 || tr != trans2) {
          continue;
        }
        din.readInt(); // interval
        din.readInt(); // leechers
        din.readInt(); // seeders
        int rest = rp.getLength() - 20;
        int off = 20;
        while (off + 6 <= rp.getLength()) {
          String ip = (rb[off] & 0xFF) + "." + (rb[off + 1] & 0xFF) + "."
              + (rb[off + 2] & 0xFF) + "." + (rb[off + 3] & 0xFF);
          int pp = ((rb[off + 4] & 0xFF) << 8) | (rb[off + 5] & 0xFF);
          if (pp > 0 && !ip.equals("0.0.0.0")) {
            out.add(ip + ":" + pp);
          }
          off = off + 6;
          rest = rest - 6;
        }
        break;
      }
    } catch (Exception e) {
      // tracker mati = coba berikutnya
    } finally {
      if (ds != null) {
        try {
          ds.close();
        } catch (Exception e) {
          // abaikan
        }
      }
    }
    return out;
  }

  // ================= pesan peer =================

  private static void readFully(InputStream in, byte[] b, int off, int len)
      throws IOException {
    int got = 0;
    while (got < len) {
      int n = in.read(b, off + got, len - got);
      if (n < 0) {
        throw new IOException("peer putus");
      }
      got = got + n;
    }
  }

  private static void writeInt(OutputStream out, int v) throws IOException {
    out.write((v >> 24) & 0xFF);
    out.write((v >> 16) & 0xFF);
    out.write((v >> 8) & 0xFF);
    out.write(v & 0xFF);
  }

  private static int readInt(byte[] b, int off) {
    return ((b[off] & 0xFF) << 24) | ((b[off + 1] & 0xFF) << 16)
        | ((b[off + 2] & 0xFF) << 8) | (b[off + 3] & 0xFF);
  }

  private static void sendSimple(OutputStream out, int id) throws IOException {
    writeInt(out, 1);
    out.write(id);
    out.flush();
  }

  private static void sendHave(OutputStream out, int idx) throws IOException {
    writeInt(out, 5);
    out.write(4);
    writeInt(out, idx);
    out.flush();
  }

  private static void sendRequest(OutputStream out, int idx, int begin, int len)
      throws IOException {
    writeInt(out, 13);
    out.write(6);
    writeInt(out, idx);
    writeInt(out, begin);
    writeInt(out, len);
    out.flush();
  }

  private static void sendPiece(OutputStream out, int idx, int begin, byte[] data)
      throws IOException {
    writeInt(out, 9 + data.length);
    out.write(7);
    writeInt(out, idx);
    writeInt(out, begin);
    out.write(data, 0, data.length);
    out.flush();
  }

  // ================= sesi unduh (file + peer) =================

  /** Status bersama: file output, piece yang sudah ada, progres. */
  private static final class Session {
    final Torrent t;
    final File root;
    final boolean multi;
    final RandomAccessFile[] rafs;
    final boolean[] have;
    final boolean[] busy;
    int donePieces = 0;
    long doneBytes = 0;
    final Object pickLock = new Object();
    final Object fileLock = new Object();
    final Object progLock = new Object();
    long lastFire = 0;
    int lastPct = -1;
    final Listener listener;
    final byte[] peerId;

    Session(Torrent t, File root, boolean multi, RandomAccessFile[] rafs,
        Listener listener, byte[] peerId) {
      this.t = t;
      this.root = root;
      this.multi = multi;
      this.rafs = rafs;
      this.have = new boolean[t.pieceCount];
      this.busy = new boolean[t.pieceCount];
      this.listener = listener;
      this.peerId = peerId;
    }

    long pieceLen(int idx) {
      if (idx == t.pieceCount - 1) {
        long r = t.totalLength - t.pieceLength * (t.pieceCount - 1);
        return r > 0 ? r : t.pieceLength;
      }
      return t.pieceLength;
    }

    int takePiece(boolean[] peerHas) {
      synchronized (pickLock) {
        for (int i = 0; i < have.length; i = i + 1) {
          if (!have[i] && !busy[i]
              && (peerHas == null || (i < peerHas.length && peerHas[i]))) {
            busy[i] = true;
            return i;
          }
        }
        return -1;
      }
    }

    void releasePiece(int idx) {
      synchronized (pickLock) {
        if (idx >= 0 && idx < busy.length) {
          busy[idx] = false;
        }
      }
    }

    void completePiece(int idx) {
      synchronized (pickLock) {
        if (!have[idx]) {
          have[idx] = true;
          donePieces = donePieces + 1;
          doneBytes = doneBytes + pieceLen(idx);
        }
        busy[idx] = false;
      }
    }

    boolean allDone() {
      synchronized (pickLock) {
        return donePieces >= have.length;
      }
    }

    void fireProgress() {
      long done;
      synchronized (pickLock) {
        done = doneBytes;
      }
      long now = System.currentTimeMillis();
      int pct = t.totalLength > 0
          ? (int) (done * 100L / t.totalLength) : 0;
      synchronized (progLock) {
        if (pct == lastPct && now - lastFire < 400 && pct != 100) {
          return;
        }
        lastPct = pct;
        lastFire = now;
      }
      listener.onProgress(done, t.totalLength);
    }

    /** Baca byte global [offset, offset+len) dari file-file output. */
    void readBytes(long offset, byte[] buf, int off, int len) throws IOException {
      synchronized (fileLock) {
        int left = len;
        int bo = off;
        long pos = offset;
        for (int f = 0; f < t.files.size() && left > 0; f = f + 1) {
          TorrentFile tf = t.files.get(f);
          if (pos >= tf.start + tf.length) {
            continue;
          }
          if (pos + left <= tf.start) {
            break;
          }
          long inFile = pos - tf.start;
          if (inFile < 0) {
            int skip = (int) (-inFile);
            bo = bo + skip;
            left = left - skip;
            pos = pos + skip;
            inFile = 0;
          }
          int can = (int) Math.min(left, tf.length - inFile);
          if (can <= 0) {
            continue;
          }
          rafs[f].seek(inFile);
          int got = 0;
          while (got < can) {
            int n = rafs[f].read(buf, bo + got, can - got);
            if (n < 0) {
              break;
            }
            got = got + n;
          }
          if (got < can) {
            throw new IOException("baca file gagal");
          }
          bo = bo + can;
          left = left - can;
          pos = pos + can;
        }
        if (left != 0) {
          throw new IOException("baca file gagal");
        }
      }
    }

    /** Tulis byte global [offset, ...) ke file-file output. */
    void writeBytes(long offset, byte[] buf, int off, int len) throws IOException {
      synchronized (fileLock) {
        int left = len;
        int bo = off;
        long pos = offset;
        for (int f = 0; f < t.files.size() && left > 0; f = f + 1) {
          TorrentFile tf = t.files.get(f);
          if (pos >= tf.start + tf.length) {
            continue;
          }
          if (pos + left <= tf.start) {
            break;
          }
          long inFile = pos - tf.start;
          if (inFile < 0) {
            int skip = (int) (-inFile);
            bo = bo + skip;
            left = left - skip;
            pos = pos + skip;
            inFile = 0;
          }
          int can = (int) Math.min(left, tf.length - inFile);
          if (can <= 0) {
            continue;
          }
          rafs[f].seek(inFile);
          rafs[f].write(buf, bo, can);
          bo = bo + can;
          left = left - can;
          pos = pos + can;
        }
        if (left != 0) {
          throw new IOException("tulis file gagal");
        }
      }
    }

    boolean verifyPiece(int idx) throws IOException {
      long len = pieceLen(idx);
      if (len > 16777216L) {
        return false;
      }
      byte[] buf = new byte[(int) len];
      try {
        readBytes(t.pieceLength * idx, buf, 0, (int) len);
      } catch (IOException e) {
        return false;
      }
      try {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] h = md.digest(buf);
        for (int i = 0; i < 20; i = i + 1) {
          if (h[i] != t.pieceHashes[idx * 20 + i]) {
            return false;
          }
        }
        return true;
      } catch (Exception e) {
        return false;
      }
    }

    void close() {
      for (int i = 0; i < rafs.length; i = i + 1) {
        try {
          if (rafs[i] != null) {
            rafs[i].close();
          }
        } catch (Exception e) {
          // abaikan
        }
      }
    }
  }

  // ================= worker peer =================

  private static final class PeerWorker extends Thread {
    final Session s;
    final String ip;
    final int port;
    final boolean needMetaExt;

    PeerWorker(Session s, String ip, int port, boolean needMetaExt) {
      this.s = s;
      this.ip = ip;
      this.port = port;
      this.needMetaExt = needMetaExt;
      setDaemon(true);
    }

    public void run() {
      Socket sock = null;
      try {
        s.listener.waitIfPaused();
        if (s.listener.isCancelled() || s.allDone()) {
          return;
        }
        sock = new Socket();
        sock.connect(new InetSocketAddress(ip, port), 12000);
        sock.setSoTimeout(30000);
        InputStream in = sock.getInputStream();
        OutputStream out = sock.getOutputStream();
        // handshake
        ByteArrayOutputStream hbos = new ByteArrayOutputStream();
        hbos.write(19);
        byte[] proto = "BitTorrent protocol".getBytes("UTF-8");
        hbos.write(proto, 0, proto.length);
        byte[] resv = new byte[8];
        if (needMetaExt) {
          resv[5] = 0x10;
        }
        hbos.write(resv, 0, 8);
        hbos.write(s.t.infoHash, 0, 20);
        hbos.write(s.peerId, 0, 20);
        hbos.flush();
        out.write(hbos.toByteArray());
        out.flush();
        byte[] hs = new byte[68];
        readFully(in, hs, 0, 68);
        if (hs[0] != 19) {
          return;
        }
        for (int i = 0; i < 20; i = i + 1) {
          if (hs[48 + i] != s.t.infoHash[i]) {
            return;
          }
        }
        // bitfield milik sendiri (bila resume punya piece)
        sendBitfieldIfAny(out);
        sendSimple(out, 2); // interested
        sendSimple(out, 1); // unchoke (kami layani upload)
        boolean[] peerHas = new boolean[s.t.pieceCount];
        boolean peerHasAny = false;
        boolean choked = true;
        long lastNet = System.currentTimeMillis();
        int idleRounds = 0;
        while (true) {
          s.listener.waitIfPaused();
          if (s.listener.isCancelled() || s.allDone()) {
            return;
          }
          if (System.currentTimeMillis() - lastNet > 120000) {
            return; // peer diam 2 menit
          }
          int idx = -1;
          if (!choked && peerHasAny) {
            idx = s.takePiece(peerHas);
          }
          if (idx < 0) {
            // baca pesan (tunggu unchoke/have/bitfield)
            int got = readOneMessage(in, out, peerHas, null, -1, null);
            if (got == 1) {
              choked = false;
              lastNet = System.currentTimeMillis();
            } else if (got == 0) {
              choked = true;
            } else if (got == 2) {
              peerHasAny = true;
              lastNet = System.currentTimeMillis();
            } else if (got == 3) {
              lastNet = System.currentTimeMillis();
            }
            idleRounds = idleRounds + 1;
            if (idleRounds > 60) {
              return; // 60x baca tanpa tugas = peer tak berguna
            }
            continue;
          }
          idleRounds = 0;
          boolean ok;
          try {
            ok = downloadPiece(in, out, peerHas, idx);
          } catch (Cancelled e) {
            throw e;
          } catch (Exception e) {
            s.releasePiece(idx);
            return;
          }
          lastNet = System.currentTimeMillis();
          if (!ok) {
            s.releasePiece(idx);
            return;
          }
        }
      } catch (Cancelled e) {
        // keluar diam-diam, main yang meneruskan
      } catch (Exception e) {
        // peer jelek = mati sendiri, main ganti peer lain
      } finally {
        if (sock != null) {
          try {
            sock.close();
          } catch (Exception e) {
            // abaikan
          }
        }
      }
    }

    private void sendBitfieldIfAny(OutputStream out) {
      try {
        int n = s.t.pieceCount;
        boolean any = false;
        synchronized (s.pickLock) {
          for (int i = 0; i < n; i = i + 1) {
            if (s.have[i]) {
              any = true;
              break;
            }
          }
        }
        if (!any) {
          return;
        }
        int blen = (n + 7) / 8;
        byte[] bf = new byte[blen];
        synchronized (s.pickLock) {
          for (int i = 0; i < n; i = i + 1) {
            if (s.have[i]) {
              bf[i / 8] = (byte) (bf[i / 8] | (0x80 >> (i % 8)));
            }
          }
        }
        writeInt(out, 1 + blen);
        out.write(5);
        out.write(bf, 0, blen);
        out.flush();
      } catch (Exception e) {
        // abaikan
      }
    }

    /**
     * Baca SATU pesan peer. Kembali 1 = unchoke, 0 = choke, 2 = have/bitfield
     * (peerHas berubah), 3 = lain (piece tak terduga / keepalive / request
     * terlayani), -1 = timeout tanpa pesan.
     */
    private int readOneMessage(InputStream in, OutputStream out,
        boolean[] peerHas, byte[] pieceBuf, int wantIdx,
        int[] pieceOffHolder) throws Exception {
      byte[] lb = new byte[4];
      try {
        readFully(in, lb, 0, 4);
      } catch (IOException e) {
        return -1;
      }
      int len = readInt(lb, 0);
      if (len == 0) {
        return 3; // keepalive
      }
      if (len < 1 || len > 262144) {
        throw new IOException("pesan peer rusak");
      }
      byte[] m = new byte[len];
      readFully(in, m, 0, len);
      int id = m[0] & 0xFF;
      if (id == 0) {
        return 0;
      }
      if (id == 1) {
        return 1;
      }
      if (id == 4 && len == 5) {
        int idx = readInt(m, 1);
        if (idx >= 0 && idx < peerHas.length) {
          peerHas[idx] = true;
        }
        return 2;
      }
      if (id == 5) {
        for (int i = 0; i < peerHas.length; i = i + 1) {
          int bi = 1 + i / 8;
          if (bi < m.length && (m[bi] & (0x80 >> (i % 8))) != 0) {
            peerHas[i] = true;
          }
        }
        return 2;
      }
      if (id == 6 && len == 13) {
        // peer minta block ke kami = layani (hindari kami di-choke)
        int rIdx = readInt(m, 1);
        int rBegin = readInt(m, 5);
        int rLen = readInt(m, 9);
        serveRequest(out, rIdx, rBegin, rLen);
        return 3;
      }
      if (id == 7 && pieceBuf != null && wantIdx >= 0) {
        int pIdx = readInt(m, 1);
        int pBegin = readInt(m, 5);
        if (pIdx == wantIdx && pBegin >= 0
            && pBegin + len - 9 <= pieceBuf.length) {
          System.arraycopy(m, 9, pieceBuf, pBegin, len - 9);
          if (pieceOffHolder != null && pieceOffHolder.length > 0) {
            pieceOffHolder[0] = pieceOffHolder[0] + (len - 9);
          }
        }
        return 3;
      }
      return 3;
    }

    private void serveRequest(OutputStream out, int idx, int begin, int len) {
      try {
        boolean has;
        synchronized (s.pickLock) {
          has = idx >= 0 && idx < s.have.length && s.have[idx];
        }
        if (!has) {
          return;
        }
        long plen = s.pieceLen(idx);
        if (begin < 0 || len <= 0 || len > 131072
            || (long) begin + len > plen) {
          return;
        }
        byte[] buf = new byte[len];
        s.readBytes(s.t.pieceLength * idx + begin, buf, 0, len);
        synchronized (out) {
          sendPiece(out, idx, begin, buf);
        }
      } catch (Exception e) {
        // abaikan
      }
    }

    /** Unduh satu piece penuh (pipeline maks 4 block). */
    private boolean downloadPiece(InputStream in, OutputStream out,
        boolean[] peerHas, int idx) throws Exception {
      long plen = s.pieceLen(idx);
      if (plen <= 0 || plen > 16777216L) {
        return false;
      }
      byte[] buf = new byte[(int) plen];
      int[] got = new int[1];
      got[0] = 0;
      int blocks = (int) ((plen + BLOCK - 1) / BLOCK);
      int inFlight = 0;
      int nextBlock = 0;
      while (got[0] < plen) {
        s.listener.waitIfPaused();
        if (s.listener.isCancelled() || s.allDone()) {
          throw new Cancelled();
        }
        while (inFlight < 4 && nextBlock < blocks) {
          int begin = nextBlock * BLOCK;
          int blen = (int) Math.min(BLOCK, plen - begin);
          synchronized (out) {
            sendRequest(out, idx, begin, blen);
          }
          nextBlock = nextBlock + 1;
          inFlight = inFlight + 1;
        }
        int before = got[0];
        long t0 = System.currentTimeMillis();
        while (got[0] == before) {
          s.listener.waitIfPaused();
          if (s.listener.isCancelled()) {
            throw new Cancelled();
          }
          if (System.currentTimeMillis() - t0 > 30000) {
            return false; // peer macet
          }
          int r = readOneMessage(in, out, peerHas, buf, idx, got);
          if (r == 0) {
            return false; // di-choke
          }
          if (r == -1) {
            continue;
          }
        }
        inFlight = inFlight - 1;
        if (inFlight < 0) {
          inFlight = 0;
        }
      }
      // verifikasi hash
      try {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] h = md.digest(buf);
        for (int i = 0; i < 20; i = i + 1) {
          if (h[i] != s.t.pieceHashes[idx * 20 + i]) {
            return false; // hash beda (peer jahil/rusak)
          }
        }
      } catch (Exception e) {
        return false;
      }
      s.writeBytes(s.t.pieceLength * idx, buf, 0, (int) plen);
      s.completePiece(idx);
      s.fireProgress();
      // kabari peer lain? cukup have ke peer ini
      try {
        synchronized (out) {
          sendHave(out, idx);
        }
      } catch (Exception e) {
        // abaikan
      }
      return true;
    }
  }

  // ================= metadata via ut_metadata (magnet) =================

  /** Ambil info dict dari SATU peer lewat ut_metadata. Null bila gagal. */
  private static byte[] fetchMetaFromPeer(String ip, int port, byte[] infoHash,
      byte[] peerId, Listener l, long deadline) {
    Socket sock = null;
    try {
      sock = new Socket();
      sock.connect(new InetSocketAddress(ip, port), 10000);
      sock.setSoTimeout(15000);
      InputStream in = sock.getInputStream();
      OutputStream out = sock.getOutputStream();
      // handshake + bit ekstensi
      ByteArrayOutputStream hbos = new ByteArrayOutputStream();
      hbos.write(19);
      byte[] proto = "BitTorrent protocol".getBytes("UTF-8");
      hbos.write(proto, 0, proto.length);
      byte[] resv = new byte[8];
      resv[5] = 0x10;
      hbos.write(resv, 0, 8);
      hbos.write(infoHash, 0, 20);
      hbos.write(peerId, 0, 20);
      hbos.flush();
      out.write(hbos.toByteArray());
      out.flush();
      byte[] hs = new byte[68];
      readFully(in, hs, 0, 68);
      if (hs[0] != 19) {
        return null;
      }
      for (int i = 0; i < 20; i = i + 1) {
        if (hs[48 + i] != infoHash[i]) {
          return null;
        }
      }
      if ((hs[20 + 5] & 0x10) == 0) {
        return null; // peer tanpa ekstensi
      }
      // ext handshake kami: ut_metadata id lokal = 1
      HashMap<String, Object> mm = new HashMap<String, Object>();
      mm.put("ut_metadata", Integer.valueOf(1));
      HashMap<String, Object> hsMap = new HashMap<String, Object>();
      hsMap.put("m", mm);
      byte[] hsB = bencodeBytes(hsMap);
      writeInt(out, 2 + hsB.length);
      out.write(20);
      out.write(0);
      out.write(hsB, 0, hsB.length);
      out.flush();
      int peerMetaId = -1;
      int metaSize = -1;
      long t0 = System.currentTimeMillis();
      byte[] lenB = new byte[4];
      while (peerMetaId < 0 || metaSize < 0) {
        if (l.isCancelled() || System.currentTimeMillis() > deadline) {
          return null;
        }
        if (System.currentTimeMillis() - t0 > 30000) {
          return null;
        }
        readFully(in, lenB, 0, 4);
        int len = readInt(lenB, 0);
        if (len == 0) {
          continue;
        }
        if (len < 2 || len > 262144) {
          return null;
        }
        byte[] m = new byte[len];
        readFully(in, m, 0, len);
        if ((m[0] & 0xFF) != 20 || (m[1] & 0xFF) != 0) {
          continue;
        }
        byte[] dictPart = new byte[len - 2];
        System.arraycopy(m, 2, dictPart, 0, dictPart.length);
        BVal dv;
        try {
          dv = decode(dictPart, 0);
        } catch (Exception e) {
          continue;
        }
        if (!(dv.val instanceof Map)) {
          continue;
        }
        Map<String, Object> dm = (Map<String, Object>) dv.val;
        Object mo = dm.get("m");
        if (mo instanceof Map) {
          Object ut = ((Map<String, Object>) mo).get("ut_metadata");
          long u = asLong(ut, 0);
          if (u > 0 && u < 256) {
            peerMetaId = (int) u;
          }
        }
        Object sz = dm.get("metadata_size");
        long s = asLong(sz, -1);
        if (s > 0 && s <= MAX_META_BYTES) {
          metaSize = (int) s;
        }
      }
      if (peerMetaId < 0 || metaSize < 0) {
        return null;
      }
      byte[] meta = new byte[metaSize];
      boolean[] gotPiece = new boolean[(metaSize + BLOCK - 1) / BLOCK];
      int gotCount = 0;
      // minta semua potongan metadata
      for (int i = 0; i < gotPiece.length; i = i + 1) {
        HashMap<String, Object> rq = new HashMap<String, Object>();
        rq.put("msg_type", Integer.valueOf(0));
        rq.put("piece", Integer.valueOf(i));
        byte[] rqB = bencodeBytes(rq);
        writeInt(out, 2 + rqB.length);
        out.write(20);
        out.write(peerMetaId);
        out.write(rqB, 0, rqB.length);
      }
      out.flush();
      t0 = System.currentTimeMillis();
      while (gotCount < gotPiece.length) {
        if (l.isCancelled() || System.currentTimeMillis() > deadline) {
          return null;
        }
        if (System.currentTimeMillis() - t0 > 30000) {
          return null;
        }
        readFully(in, lenB, 0, 4);
        int len = readInt(lenB, 0);
        if (len == 0) {
          continue;
        }
        if (len < 2 || len > BLOCK + 65536) {
          return null;
        }
        byte[] m = new byte[len];
        readFully(in, m, 0, len);
        if ((m[0] & 0xFF) != 20 || (m[1] & 0xFF) != 1) {
          continue;
        }
        byte[] dictPart = new byte[len - 2];
        System.arraycopy(m, 2, dictPart, 0, dictPart.length);
        BVal dv;
        try {
          dv = decode(dictPart, 0);
        } catch (Exception e) {
          continue;
        }
        if (!(dv.val instanceof Map)) {
          continue;
        }
        Map<String, Object> dm = (Map<String, Object>) dv.val;
        if (asLong(dm.get("msg_type"), -1) != 1) {
          continue;
        }
        int piece = (int) asLong(dm.get("piece"), -1);
        if (piece < 0 || piece >= gotPiece.length || gotPiece[piece]) {
          continue;
        }
        int dataOff = 2 + dv.end;
        int dataLen = len - dataOff;
        if (dataLen <= 0) {
          continue;
        }
        int wOff = piece * BLOCK;
        int wLen = Math.min(dataLen, metaSize - wOff);
        System.arraycopy(m, dataOff, meta, wOff, wLen);
        gotPiece[piece] = true;
        gotCount = gotCount + 1;
        t0 = System.currentTimeMillis();
      }
      // verifikasi: SHA1(metadata) == info-hash magnet
      try {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] h = md.digest(meta);
        for (int i = 0; i < 20; i = i + 1) {
          if (h[i] != infoHash[i]) {
            return null;
          }
        }
      } catch (Exception e) {
        return null;
      }
      return meta;
    } catch (Exception e) {
      return null;
    } finally {
      if (sock != null) {
        try {
          sock.close();
        } catch (Exception e) {
          // abaikan
        }
      }
    }
  }

  /** Bangun Torrent dari info dict mentah (hasil ut_metadata). */
  private static Torrent torrentFromInfo(byte[] infoBytes, Magnet m)
      throws IOException {
    BVal iv = decode(infoBytes, 0);
    if (!(iv.val instanceof Map)) {
      throw new IOException("metadata rusak");
    }
    Map<String, Object> info = (Map<String, Object>) iv.val;
    Torrent t = new Torrent();
    t.infoHash = m.infoHash;
    t.name = asString(info.get("name"));
    if (t.name.length() == 0) {
      t.name = m.name.length() > 0 ? m.name : "torrent";
    }
    t.pieceLength = asLong(info.get("piece length"), 0);
    Object pieces = info.get("pieces");
    if (!(pieces instanceof byte[])) {
      throw new IOException("metadata tanpa daftar piece");
    }
    t.pieceHashes = (byte[]) pieces;
    if (t.pieceHashes.length % 20 != 0 || t.pieceHashes.length == 0) {
      throw new IOException("metadata rusak");
    }
    if (t.pieceLength <= 0) {
      throw new IOException("metadata rusak");
    }
    t.pieceCount = t.pieceHashes.length / 20;
    Object lenObj = info.get("length");
    Object filesObj = info.get("files");
    if (lenObj instanceof Long || lenObj instanceof Integer) {
      t.multi = false;
      TorrentFile f = new TorrentFile();
      f.path = t.name;
      f.length = asLong(lenObj, 0);
      t.files.add(f);
    } else if (filesObj instanceof List) {
      t.multi = true;
      List<Object> fl = (List<Object>) filesObj;
      for (int i = 0; i < fl.size(); i = i + 1) {
        if (!(fl.get(i) instanceof Map)) {
          continue;
        }
        Map<String, Object> fm = (Map<String, Object>) fl.get(i);
        long flen = asLong(fm.get("length"), -1);
        Object pathObj = fm.get("path");
        if (flen < 0 || !(pathObj instanceof List)) {
          continue;
        }
        List<Object> pc = (List<Object>) pathObj;
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < pc.size(); j = j + 1) {
          String comp = Shared.sanitize(asString(pc.get(j)));
          if (comp.length() == 0 || comp.equals(".") || comp.equals("..")) {
            continue;
          }
          if (sb.length() > 0) {
            sb.append('/');
          }
          sb.append(comp);
        }
        if (sb.length() == 0) {
          continue;
        }
        TorrentFile f = new TorrentFile();
        f.path = sb.toString();
        f.length = flen;
        t.files.add(f);
      }
      if (t.files.isEmpty()) {
        throw new IOException("metadata tanpa file");
      }
    } else {
      throw new IOException("metadata tanpa panjang file");
    }
    long total = 0;
    for (int i = 0; i < t.files.size(); i = i + 1) {
      t.files.get(i).start = total;
      total = total + t.files.get(i).length;
    }
    t.totalLength = total;
    if (t.totalLength <= 0) {
      throw new IOException("torrent kosong (0 byte)");
    }
    for (int i = 0; i < m.trackers.size(); i = i + 1) {
      t.trackers.add(m.trackers.get(i));
    }
    return t;
  }

  // ================= pintu utama =================

  /**
   * Unduh ISI torrent ke folder tujuan. Input: magnet: ATAU url http(s)
   * file .torrent ATAU byte file .torrent (bila preTorrent != null).
   * Kembali Result berisi nama + path akhir. Lempar Cancelled bila user
   * membatalkan (output sebagian ikut dihapus).
   */
  static Result downloadAuto(String input, byte[] preTorrent, File destDir,
      Listener l) throws IOException, Cancelled {
    String u = input == null ? "" : input.trim();
    if (l.isCancelled()) {
      throw new Cancelled();
    }
    Torrent t;
    if (preTorrent != null) {
      t = parseTorrent(preTorrent);
    } else if (isMagnetLink(u)) {
      Magnet m = parseMagnet(u);
      if (m.trackers.isEmpty()) {
        throw new IOException("magnet tanpa tracker (tr=...) belum didukung "
            + "- pakai magnet yang memuat tracker atau link file .torrent");
      }
      byte[] peerId = makePeerId();
      ArrayList<String> peers = announceAll(m.trackers, m.infoHash, peerId,
          0, 1, l);
      if (peers.isEmpty()) {
        throw new IOException("tidak ada peer/tracker yang menjawab magnet ini");
      }
      long deadline = System.currentTimeMillis() + 120000;
      byte[] meta = null;
      for (int i = 0; i < peers.size() && meta == null; i = i + 1) {
        if (l.isCancelled()) {
          throw new Cancelled();
        }
        l.waitIfPaused();
        if (System.currentTimeMillis() > deadline) {
          break;
        }
        String[] hp = splitHostPort(peers.get(i));
        if (hp == null) {
          continue;
        }
        int port;
        try {
          port = Integer.parseInt(hp[1]);
        } catch (Exception e) {
          continue;
        }
        meta = fetchMetaFromPeer(hp[0], port, m.infoHash, peerId, l, deadline);
      }
      if (meta == null) {
        if (l.isCancelled()) {
          throw new Cancelled();
        }
        throw new IOException("gagal ambil metadata magnet (peer tak memberi info)");
      }
      t = torrentFromInfo(meta, m);
    } else if (u.toLowerCase().startsWith("http://")
        || u.toLowerCase().startsWith("https://")) {
      byte[] tb = fetchHttpBytes(u);
      t = parseTorrent(tb);
    } else {
      throw new IOException("untuk torrent isi magnet: atau link http(s) file .torrent");
    }
    return downloadTorrent(t, destDir, l);
  }

  private static String[] splitHostPort(String s) {
    try {
      int c = s.lastIndexOf(':');
      if (c <= 0 || c + 1 >= s.length()) {
        return null;
      }
      String[] out = new String[2];
      out[0] = s.substring(0, c);
      out[1] = s.substring(c + 1);
      return out;
    } catch (Exception e) {
      return null;
    }
  }

  /** Unduh isi Torrent yang metadatanya sudah diketahui. */
  static Result downloadTorrent(Torrent t, File destDir, final Listener l)
      throws IOException, Cancelled {
    String safeName = Shared.sanitize(t.name);
    if (safeName.length() == 0) {
      safeName = "torrent";
    }
    final File root;
    if (t.multi) {
      root = new File(destDir, safeName);
    } else {
      root = new File(destDir, safeName);
    }
    // Siapkan file output (resume: file yang sudah ada + cocok diverifikasi).
    RandomAccessFile[] rafs = new RandomAccessFile[t.files.size()];
    try {
      for (int i = 0; i < t.files.size(); i = i + 1) {
        TorrentFile tf = t.files.get(i);
        File f = t.multi ? new File(root, tf.path.replace('/', File.separatorChar))
            : root;
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) {
          parent.mkdirs();
        }
        RandomAccessFile raf = new RandomAccessFile(f, "rw");
        rafs[i] = raf;
        if (raf.length() != tf.length) {
          raf.setLength(tf.length);
        }
      }
    } catch (IOException e) {
      for (int i = 0; i < rafs.length; i = i + 1) {
        try {
          if (rafs[i] != null) {
            rafs[i].close();
          }
        } catch (Exception ex) {
          // abaikan
        }
      }
      throw new IOException("folder tujuan tidak bisa ditulis");
    }
    final byte[] peerId = makePeerId();
    Session s = new Session(t, root, t.multi, rafs, l, peerId);
    try {
      // Verifikasi resume (piece yang sudah lengkap di disk).
      for (int i = 0; i < t.pieceCount; i = i + 1) {
        if (l.isCancelled()) {
          throw new Cancelled();
        }
        boolean ok = false;
        try {
          ok = s.verifyPiece(i);
        } catch (Exception e) {
          ok = false;
        }
        if (ok) {
          synchronized (s.pickLock) {
            s.have[i] = true;
            s.donePieces = s.donePieces + 1;
            s.doneBytes = s.doneBytes + s.pieceLen(i);
          }
        }
      }
      String savePath = root.getAbsolutePath();
      l.onStarted(safeName, savePath, t.totalLength);
      l.onProgress(s.doneBytes, t.totalLength);
      if (s.allDone()) {
        Result r = new Result();
        r.name = safeName;
        r.savePath = savePath;
        r.total = t.totalLength;
        return r;
      }
      if (t.trackers.isEmpty()) {
        throw new IOException("torrent tanpa tracker - tidak ada sumber peer");
      }
      ArrayList<String> peers = announceAll(t.trackers, t.infoHash, peerId,
          s.doneBytes, t.totalLength - s.doneBytes, l);
      if (peers.isEmpty()) {
        throw new IOException("tidak ada peer/tracker yang menjawab");
      }
      if (l.isCancelled()) {
        throw new Cancelled();
      }
      // Kolam worker: isi ulang dari daftar peer sampai semua piece dapat.
      ArrayList<PeerWorker> workers = new ArrayList<PeerWorker>();
      int peerPos = 0;
      while (!s.allDone()) {
        l.waitIfPaused();
        if (l.isCancelled()) {
          throw new Cancelled();
        }
        // buang worker mati
        for (int i = workers.size() - 1; i >= 0; i = i - 1) {
          if (!workers.get(i).isAlive()) {
            workers.remove(i);
          }
        }
        while (workers.size() < MAX_PEERS && peerPos < peers.size()
            && !s.allDone()) {
          String[] hp = splitHostPort(peers.get(peerPos));
          peerPos = peerPos + 1;
          if (hp == null) {
            continue;
          }
          int port;
          try {
            port = Integer.parseInt(hp[1]);
          } catch (Exception e) {
            continue;
          }
          if (port <= 0 || port > 65535) {
            continue;
          }
          PeerWorker w = new PeerWorker(s, hp[0], port, false);
          workers.add(w);
          w.start();
        }
        if (workers.isEmpty()) {
          // semua worker mati. peer habis -> coba announce ulang sekali.
          if (peerPos >= peers.size()) {
            ArrayList<String> more = announceAll(t.trackers, t.infoHash,
                peerId, s.doneBytes, t.totalLength - s.doneBytes, l);
            HashSet<String> seen = new HashSet<String>(peers);
            peers.clear();
            for (int i = 0; i < more.size(); i = i + 1) {
              if (!seen.contains(more.get(i))) {
                peers.add(more.get(i));
              }
            }
            peerPos = 0;
            if (peers.isEmpty()) {
              throw new IOException("peer berhenti di tengah jalan ("
                  + s.donePieces + "/" + t.pieceCount + " piece)");
            }
            continue;
          }
        }
        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
          // cek flag lagi
        }
      }
      // tunggu worker selesai
      for (int i = 0; i < workers.size(); i = i + 1) {
        try {
          workers.get(i).join(5000);
        } catch (Exception e) {
          // abaikan
        }
      }
      l.onProgress(t.totalLength, t.totalLength);
      Result r = new Result();
      r.name = safeName;
      r.savePath = savePath;
      r.total = t.totalLength;
      return r;
    } catch (Cancelled e) {
      try {
        Shared.deleteRecursive(root);
      } catch (Exception ex) {
        // abaikan
      }
      throw e;
    } finally {
      s.close();
    }
  }
}
