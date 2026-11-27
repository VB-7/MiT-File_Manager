package com.fv7.tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Inflater;

/**
 * Parser PDF murni-Java untuk Tools_Pdf (tanpa library luar, Java 7).
 *
 * <p>Mendukung: xref tabel + xref stream + object stream (+fallback pindai
 * mentah), FlateDecode (+prediktor PNG), ASCII85/ASCIIHex/RunLength,
 * encoding WinAnsi + Differences + ToUnicode (bfchar/bfrange), susunan teks
 * per baris via matriks teks (op Tm, Td, T*, Tf, Tj, TJ), metadata Title/Author,
 * deteksi file terkunci (/Encrypt).</p>
 *
 * <p>Batasan v1: LZWDecode tak didukung (stream itu dilewati, teks lain tetap
 * dibaca); MacRoman fallback ke WinAnsi; lebar glif tak dipakai (spasi dari
 * jarak x + angka kerning TJ).</p>
 */
final class PdfParser {

  private PdfParser() {
  }

  /** Gagal parse dengan pesan Indonesia siap tampil. */
  static final class PdfException extends IOException {
    PdfException(String m) {
      super(m);
    }
  }

  /** Referensi tak langsung "num gen R". */
  static final class Ref {
    int num;
    int gen;
  }

  /** Stream: kamus + byte mentah (belum di-decode). */
  static final class StreamObj {
    Map<String, Object> dict = new HashMap<String, Object>();
    byte[] raw = new byte[0];
  }

  /** Satu halaman: ukuran (point 1/72 inci) + teks hasil ekstraksi. */
  static final class Page {
    double widthPt;
    double heightPt;
    String text = "";
  }

  /** Dokumen hasil parse. */
  static final class Doc {
    String title = "";
    String author = "";
    ArrayList<Page> pages = new ArrayList<Page>();
  }

  /** Batas ukuran file (120 MB) agar HP kentang tidak OOM. */
  private static final int MAX_BYTES = 120 * 1024 * 1024;
  /** Batas token per content stream (anti hang). */
  private static final int MAX_TOKENS = 2000000;

  // ================= tabel WinAnsi (byte -> char) =================

  private static final char[] WINANSI = new char[256];

  static {
    for (int i = 0; i < 256; i = i + 1) {
      WINANSI[i] = (char) i;
    }
    WINANSI[0x80] = '\u20AC';
    WINANSI[0x82] = '\u201A';
    WINANSI[0x83] = '\u0192';
    WINANSI[0x84] = '\u201E';
    WINANSI[0x85] = '\u2026';
    WINANSI[0x86] = '\u2020';
    WINANSI[0x87] = '\u2021';
    WINANSI[0x88] = '\u02C6';
    WINANSI[0x89] = '\u2030';
    WINANSI[0x8A] = '\u0160';
    WINANSI[0x8B] = '\u2039';
    WINANSI[0x8C] = '\u0152';
    WINANSI[0x8E] = '\u017D';
    WINANSI[0x91] = '\u2018';
    WINANSI[0x92] = '\u2019';
    WINANSI[0x93] = '\u201C';
    WINANSI[0x94] = '\u201D';
    WINANSI[0x95] = '\u2022';
    WINANSI[0x96] = '\u2013';
    WINANSI[0x97] = '\u2014';
    WINANSI[0x98] = '\u02DC';
    WINANSI[0x99] = '\u2122';
    WINANSI[0x9A] = '\u0161';
    WINANSI[0x9B] = '\u203A';
    WINANSI[0x9C] = '\u0153';
    WINANSI[0x9E] = '\u017E';
    WINANSI[0x9F] = '\u0178';
  }

  // ================= mini Adobe Glyph List =================

  private static final HashMap<String, Character> AGL =
      new HashMap<String, Character>();

  private static void agl(String name, char c) {
    AGL.put(name, Character.valueOf(c));
  }

  static {
    agl("space", ' ');
    agl("nbspace", '\u00A0');
    agl("hyphen", '-');
    agl("minus", '\u2212');
    agl("endash", '\u2013');
    agl("emdash", '\u2014');
    agl("quoteleft", '\u2018');
    agl("quoteright", '\u2019');
    agl("quotesingle", '\'');
    agl("quotedblleft", '\u201C');
    agl("quotedblright", '\u201D');
    agl("quotedbl", '"');
    agl("quotesinglbase", '\u201A');
    agl("quotedblbase", '\u201E');
    agl("grave", '`');
    agl("acute", '\u00B4');
    agl("circumflex", '\u02C6');
    agl("tilde", '\u02DC');
    agl("dieresis", '\u00A8');
    agl("bullet", '\u2022');
    agl("ellipsis", '\u2026');
    agl("dagger", '\u2020');
    agl("daggerdbl", '\u2021');
    agl("perthousand", '\u2030');
    agl("guilsinglleft", '\u2039');
    agl("guilsinglright", '\u203A');
    agl("fi", '\uFB01');
    agl("fl", '\uFB02');
    agl("ffi", '\uFB00');
    agl("ffl", '\uFB03');
    agl("ff", '\uFB00');
    agl("germandbls", '\u00DF');
    agl("OE", '\u0152');
    agl("oe", '\u0153');
    agl("Scaron", '\u0160');
    agl("scaron", '\u0161');
    agl("Ydieresis", '\u0178');
    agl("Zcaron", '\u017D');
    agl("zcaron", '\u017E');
    agl("Euro", '\u20AC');
    agl("euro", '\u20AC');
    agl("trademark", '\u2122');
    agl("copyright", '\u00A9');
    agl("registered", '\u00AE');
    agl("degree", '\u00B0');
    agl("plusminus", '\u00B1');
    agl("multiply", '\u00D7');
    agl("divide", '\u00F7');
    agl("onesuperior", '\u00B9');
    agl("twosuperior", '\u00B2');
    agl("threesuperior", '\u00B3');
    agl("onehalf", '\u00BD');
    agl("onequarter", '\u00BC');
    agl("threequarters", '\u00BE');
    agl("florin", '\u0192');
    agl("exclamdown", '\u00A1');
    agl("questiondown", '\u00BF');
    agl("sterling", '\u00A3');
    agl("yen", '\u00A5');
    agl("cent", '\u00A2');
    agl("currency", '\u00A4');
    agl("brokenbar", '\u00A6');
    agl("section", '\u00A7');
    agl("dieresis2", '\u00A8');
    agl("ordfeminine", '\u00AA');
    agl("ordmasculine", '\u00BA');
    agl("paragraph", '\u00B6');
    agl("middot", '\u00B7');
    agl("cedilla", '\u00B8');
    agl("onesuperior2", '\u00B9');
    agl("AE", '\u00C6');
    agl("ae", '\u00E6');
    agl("Oslash", '\u00D8');
    agl("oslash", '\u00F8');
    agl("THORN", '\u00DE');
    agl("thorn", '\u00FE');
    agl("eth", '\u00F0');
    agl("Eth", '\u00D0');
    agl("dollar", '$');
    agl("percent", '%');
    agl("ampersand", '&');
    agl("asterisk", '*');
    agl("plus", '+');
    agl("comma", ',');
    agl("period", '.');
    agl("slash", '/');
    agl("colon", ':');
    agl("semicolon", ';');
    agl("less", '<');
    agl("equal", '=');
    agl("greater", '>');
    agl("question", '?');
    agl("at", '@');
    agl("backslash", '\\');
    agl("bracketleft", '[');
    agl("bracketright", ']');
    agl("asciicircum", '^');
    agl("underscore", '_');
    agl("braceleft", '{');
    agl("bar", '|');
    agl("braceright", '}');
    agl("asciitilde", '~');
    agl("parenleft", '(');
    agl("parenright", ')');
    agl("numbersign", '#');
    agl("exclam", '!');
  }

  /** Nama glif -> char. uniXXXX/uXXXX diurai; tak dikenal -> '?'. */
  private static char glyphToChar(String name) {
    if (name == null || name.length() == 0) {
      return '?';
    }
    if (name.length() == 1) {
      return name.charAt(0);
    }
    Character c = AGL.get(name);
    if (c != null) {
      return c.charValue();
    }
    try {
      if (name.startsWith("uni") && name.length() >= 7) {
        return (char) Integer.parseInt(name.substring(3, 7), 16);
      }
      if (name.charAt(0) == 'u' && name.length() >= 5) {
        int end = 5;
        while (end < name.length() && end < 9
            && Character.digit(name.charAt(end), 16) >= 0) {
          end = end + 1;
        }
        if (end >= 5) {
          int cp = Integer.parseInt(name.substring(1, end), 16);
          if (cp >= 0 && cp <= 0xFFFF) {
            return (char) cp;
          }
        }
      }
    } catch (Exception e) {
      // jatuh ke '?'
    }
    return '?';
  }

  // ================= pintu utama =================

  /** Parse byte PDF menjadi Doc (judul, penulis, halaman + teks). */
  static Doc parse(byte[] data) throws PdfException {
    if (data == null || data.length < 10) {
      throw new PdfException("bukan file PDF (terlalu kecil)");
    }
    if (data.length > MAX_BYTES) {
      throw new PdfException("PDF terlalu besar (>120 MB)");
    }
    Reader r = new Reader(data);
    r.readXrefs();
    Map<String, Object> trailer = r.trailer;
    if (trailer == null || trailer.isEmpty()) {
      throw new PdfException("PDF rusak (trailer tak terbaca)");
    }
    Object enc = r.resolve(trailer.get("Encrypt"));
    if (enc != null) {
      throw new PdfException("PDF terkunci (encrypted) - buka kuncinya dulu");
    }
    Map<String, Object> catalog = r.asDict(r.resolve(trailer.get("Root")));
    if (catalog == null) {
      throw new PdfException("PDF rusak (katalog tak terbaca)");
    }
    Map<String, Object> info = r.asDict(r.resolve(trailer.get("Info")));
    Doc doc = new Doc();
    if (info != null) {
      doc.title = textString(r.resolve(info.get("Title")));
      doc.author = textString(r.resolve(info.get("Author")));
    }
    Map<String, Object> pages = r.asDict(r.resolve(catalog.get("Pages")));
    if (pages == null) {
      throw new PdfException("PDF rusak (daftar halaman tak terbaca)");
    }
    ArrayList<Map<String, Object>> pageDicts =
        new ArrayList<Map<String, Object>>();
    collectPages(r, pages, null, pageDicts, 0);
    if (pageDicts.isEmpty()) {
      throw new PdfException("PDF tidak punya halaman");
    }
    if (pageDicts.size() > 10000) {
      throw new PdfException("PDF punya >10000 halaman (tak didukung)");
    }
    for (int i = 0; i < pageDicts.size(); i = i + 1) {
      Page pg = new Page();
      try {
        fillPage(r, pageDicts.get(i), pg);
      } catch (Exception e) {
        pg.widthPt = 0;
        pg.heightPt = 0;
        pg.text = "";
      }
      doc.pages.add(pg);
    }
    return doc;
  }

  /** TextString PDF (literal/hex): BOM UTF-16BE, else byte->WinAnsi. */
  private static String textString(Object o) {
    if (!(o instanceof byte[])) {
      return "";
    }
    byte[] b = (byte[]) o;
    if (b.length == 0) {
      return "";
    }
    try {
      if (b.length >= 2 && (b[0] & 0xFF) == 0xFE && (b[1] & 0xFF) == 0xFF) {
        return new String(b, 2, b.length - 2, "UTF-16BE");
      }
      if (b.length >= 2 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xFE) {
        return new String(b, 2, b.length - 2, "UTF-16LE");
      }
      StringBuilder sb = new StringBuilder(b.length);
      for (int i = 0; i < b.length; i = i + 1) {
        sb.append(WINANSI[b[i] & 0xFF]);
      }
      return sb.toString();
    } catch (Exception e) {
      return "";
    }
  }

  // ================= kumpul halaman =================

  private static void collectPages(Reader r, Map<String, Object> node,
      Map<String, Object> inheritedRes,
      ArrayList<Map<String, Object>> out, int depth) throws PdfException {
    if (depth > 60) {
      throw new PdfException("PDF rusak (pohon halaman terlalu dalam)");
    }
    Object resObj = node.get("Resources");
    Map<String, Object> res = r.asDict(r.resolve(resObj));
    if (res == null) {
      res = inheritedRes;
    }
    Object kidsObj = r.resolve(node.get("Kids"));
    if (kidsObj instanceof List) {
      List<Object> kids = (List<Object>) kidsObj;
      for (int i = 0; i < kids.size(); i = i + 1) {
        Map<String, Object> kid = r.asDict(r.resolve(kids.get(i)));
        if (kid != null) {
          collectPages(r, kid, res, out, depth + 1);
        }
      }
      return;
    }
    // simpul daun = halaman (punya MediaBox/Contents atau Type Page)
    node.put("__inheritedRes__", res);
    out.add(node);
  }

  private static void fillPage(Reader r, Map<String, Object> pg, Page out)
      throws PdfException {
    double[] box = r.asRect(r.resolve(pg.get("MediaBox")));
    if (box == null) {
      box = new double[] {0, 0, 595, 842};
    }
    double w = box[2] - box[0];
    double h = box[3] - box[1];
    if (w <= 0 || h <= 0) {
      w = 595;
      h = 842;
    }
    long rot = r.asLong(r.resolve(pg.get("Rotate")), 0);
    if (rot == 90 || rot == 270) {
      double t = w;
      w = h;
      h = t;
    }
    out.widthPt = w;
    out.heightPt = h;
    Map<String, Object> res = r.asDict(r.resolve(pg.get("Resources")));
    if (res == null) {
      res = (Map<String, Object>) pg.get("__inheritedRes__");
    }
    Object contents = r.resolve(pg.get("Contents"));
    ByteArrayOutputStream all = new ByteArrayOutputStream();
    appendContents(r, contents, all);
    byte[] bytes = all.toByteArray();
    if (bytes.length == 0 || res == null) {
      out.text = "";
      return;
    }
    Map<String, PdfFont> fonts = buildFonts(r, res);
    out.text = extractText(bytes, fonts);
  }

  private static void appendContents(Reader r, Object contents,
      ByteArrayOutputStream all) {
    try {
      if (contents instanceof Ref || contents instanceof Map
          || contents instanceof StreamObj) {
        Object o = r.resolve(contents);
        if (o instanceof StreamObj) {
          byte[] b = decodeStream((StreamObj) o);
          if (b != null) {
            all.write(b, 0, b.length);
            all.write('\n');
          }
        }
      } else if (contents instanceof List) {
        List<Object> l = (List<Object>) contents;
        for (int i = 0; i < l.size(); i = i + 1) {
          appendContents(r, l.get(i), all);
        }
      }
    } catch (Exception e) {
      // stream rusak = lewati, teks lain tetap dibaca
    }
  }

  // ================= font =================

  private static final class PdfFont {
    HashMap<Integer, String> toUni = new HashMap<Integer, String>();
    HashMap<Integer, String> diff = new HashMap<Integer, String>();
    String baseEnc = "WinAnsi";
    boolean wide = false;
  }

  private static Map<String, PdfFont> buildFonts(Reader r,
      Map<String, Object> res) {
    HashMap<String, PdfFont> out = new HashMap<String, PdfFont>();
    try {
      Map<String, Object> fontDict = r.asDict(r.resolve(res.get("Font")));
      if (fontDict == null) {
        return out;
      }
      for (String key : fontDict.keySet()) {
        try {
          Map<String, Object> f = r.asDict(r.resolve(fontDict.get(key)));
          if (f == null) {
            continue;
          }
          PdfFont pf = new PdfFont();
          String subtype = r.asName(r.resolve(f.get("Subtype")));
          String encName = r.asName(r.resolve(f.get("Encoding")));
          Object encObj = r.resolve(f.get("Encoding"));
          if (encObj instanceof Map) {
            Map<String, Object> enc = (Map<String, Object>) encObj;
            String base = r.asName(r.resolve(enc.get("BaseEncoding")));
            if (base.length() > 0) {
              pf.baseEnc = base;
            }
            Object dif = r.resolve(enc.get("Differences"));
            if (dif instanceof List) {
              List<Object> dl = (List<Object>) dif;
              int code = 0;
              for (int i = 0; i < dl.size(); i = i + 1) {
                Object it = dl.get(i);
                if (it instanceof Long || it instanceof Integer) {
                  code = ((Number) it).intValue();
                } else {
                  String nm = r.asName(it);
                  if (nm.startsWith("/")) {
                    nm = nm.substring(1);
                  }
                  pf.diff.put(Integer.valueOf(code & 0xFF),
                      String.valueOf(glyphToChar(nm)));
                  code = code + 1;
                }
              }
            }
          } else if (encName.length() > 0) {
            String e = encName.startsWith("/") ? encName.substring(1) : encName;
            pf.baseEnc = e;
          }
          if (subtype.indexOf("Type0") >= 0 || pf.baseEnc.equals("Identity-H")
              || pf.baseEnc.equals("Identity-V")) {
            pf.wide = true;
          }
          Object tu = r.resolve(f.get("ToUnicode"));
          if (tu instanceof StreamObj) {
            byte[] cb = decodeStream((StreamObj) tu);
            if (cb != null) {
              parseToUnicode(new String(cb, "US-ASCII"), pf);
              if (!pf.toUni.isEmpty()) {
                pf.wide = pf.wide || looksWide(pf);
              }
            }
          } else {
            // ToUnicode kadang di descendant (Type0)
            Object desc = r.resolve(f.get("DescendantFonts"));
            if (desc instanceof List && !((List<Object>) desc).isEmpty()) {
              Map<String, Object> dd = r.asDict(
                  r.resolve(((List<Object>) desc).get(0)));
              if (dd != null) {
                Object tu2 = r.resolve(dd.get("ToUnicode"));
                if (tu2 instanceof StreamObj) {
                  byte[] cb = decodeStream((StreamObj) tu2);
                  if (cb != null) {
                    parseToUnicode(new String(cb, "US-ASCII"), pf);
                  }
                }
              }
            }
          }
          String k = key.startsWith("/") ? key.substring(1) : key;
          out.put(k, pf);
        } catch (Exception e) {
          // font rusak = lewati
        }
      }
    } catch (Exception e) {
      // abaikan
    }
    return out;
  }

  private static boolean looksWide(PdfFont pf) {
    for (Integer k : pf.toUni.keySet()) {
      if (k.intValue() > 0xFF) {
        return true;
      }
    }
    return false;
  }

  /** Parse CMap ToUnicode: bfchar + bfrange (termasuk bentuk array). */
  private static void parseToUnicode(String cmap, PdfFont pf) {
    try {
      String[] lines = cmap.split("\n");
      boolean inChar = false;
      boolean inRange = false;
      for (int li = 0; li < lines.length; li = li + 1) {
        String ln = lines[li].trim();
        if (ln.indexOf("beginbfchar") >= 0) {
          inChar = true;
          continue;
        }
        if (ln.indexOf("endbfchar") >= 0) {
          inChar = false;
          continue;
        }
        if (ln.indexOf("beginbfrange") >= 0) {
          inRange = true;
          continue;
        }
        if (ln.indexOf("endbfrange") >= 0) {
          inRange = false;
          continue;
        }
        if (inChar) {
          int a = ln.indexOf('<');
          int b = ln.indexOf('>', a + 1);
          int c = ln.indexOf('<', b + 1);
          int d = ln.indexOf('>', c + 1);
          if (a >= 0 && b > a && c > b && d > c) {
            int src = (int) Long.parseLong(ln.substring(a + 1, b), 16);
            String dst = cmapBytesToString(ln.substring(c + 1, d));
            pf.toUni.put(Integer.valueOf(src), dst);
          }
        } else if (inRange) {
          int p1 = ln.indexOf('<');
          int p2 = ln.indexOf('>', p1 + 1);
          int p3 = ln.indexOf('<', p2 + 1);
          int p4 = ln.indexOf('>', p3 + 1);
          if (p1 < 0 || p2 <= p1 || p3 <= p2 || p4 <= p3) {
            continue;
          }
          int lo = (int) Long.parseLong(ln.substring(p1 + 1, p2), 16);
          int hi = (int) Long.parseLong(ln.substring(p3 + 1, p4), 16);
          String rest = ln.substring(p4 + 1).trim();
          if (rest.startsWith("[")) {
            // bentuk array: <lo> <hi> [<d0> <d1> ...]
            int q = 0;
            int cur = lo;
            while (cur <= hi && q < 512) {
              int s = rest.indexOf('<', q);
              int e = rest.indexOf('>', s + 1);
              if (s < 0 || e <= s) {
                break;
              }
              pf.toUni.put(Integer.valueOf(cur),
                  cmapBytesToString(rest.substring(s + 1, e)));
              cur = cur + 1;
              q = e + 1;
            }
          } else {
            int s = rest.indexOf('<');
            int e = rest.indexOf('>', s + 1);
            if (s >= 0 && e > s) {
              byte[] base = hexToBytes(ln.substring(p4 + 1).trim()
                  .replaceAll("[<>]", ""));
              // dst awal sebagai int (dukung 1-2 byte; >2 byte = pakai string)
              String first = cmapBytesToString(
                  rest.substring(s + 1, e));
              if (first.length() == 1 && base != null) {
                int start = first.charAt(0);
                for (int v = lo; v <= hi; v = v + 1) {
                  pf.toUni.put(Integer.valueOf(v),
                      String.valueOf((char) (start + (v - lo))));
                  if (v - lo > 4096) {
                    break;
                  }
                }
              } else {
                for (int v = lo; v <= hi; v = v + 1) {
                  pf.toUni.put(Integer.valueOf(v), first);
                  if (v - lo > 64) {
                    break;
                  }
                }
              }
            }
          }
        }
      }
    } catch (Exception e) {
      // CMap aneh = pakai yang sudah dapat
    }
  }

  private static byte[] hexToBytes(String h) {
    try {
      String t = h.replaceAll("\\s", "");
      if (t.length() == 0 || t.length() > 512) {
        return null;
      }
      if (t.length() % 2 == 1) {
        t = t + "0";
      }
      byte[] out = new byte[t.length() / 2];
      for (int i = 0; i < out.length; i = i + 1) {
        out[i] = (byte) Integer.parseInt(t.substring(i * 2, i * 2 + 2), 16);
      }
      return out;
    } catch (Exception e) {
      return null;
    }
  }

  /** Byte CMap -> String: BOM UTF-16BE, else per-byte WinAnsi. */
  private static String cmapBytesToString(String hex) {
    byte[] b = hexToBytes(hex);
    if (b == null || b.length == 0) {
      return "";
    }
    try {
      if (b.length >= 2 && (b[0] & 0xFF) == 0xFE && (b[1] & 0xFF) == 0xFF) {
        return new String(b, 2, b.length - 2, "UTF-16BE");
      }
      if (b.length % 2 == 0 && b.length >= 2) {
        // tebakan UTF-16BE tanpa BOM (umum di CMap): ada byte 0 di posisi genap?
        boolean looks16 = false;
        for (int i = 0; i < b.length; i = i + 2) {
          if (b[i] == 0) {
            looks16 = true;
            break;
          }
        }
        if (looks16) {
          return new String(b, 0, b.length, "UTF-16BE");
        }
      }
      StringBuilder sb = new StringBuilder(b.length);
      for (int i = 0; i < b.length; i = i + 1) {
        sb.append(WINANSI[b[i] & 0xFF]);
      }
      return sb.toString();
    } catch (Exception e) {
      return "";
    }
  }

  // ================= ekstraksi teks =================

  private static final class Run {
    double x;
    double y;
    double size;
    String text = "";
  }

  /** Jalankan content stream -> teks rapi per baris. */
  private static String extractText(byte[] bytes, Map<String, PdfFont> fonts) {
    ArrayList<Run> runs = new ArrayList<Run>();
    try {
      ArrayList<Object> stack = new ArrayList<Object>();
      double[] lm = new double[] {1, 0, 0, 1, 0, 0};
      String curFont = "";
      double curSize = 0;
      TokReader tr = new TokReader(bytes);
      Object t;
      int count = 0;
      while ((t = tr.next()) != null) {
        count = count + 1;
        if (count > MAX_TOKENS) {
          break;
        }
        if (t instanceof Op) {
          String op = ((Op) t).name;
          if (op.equals("BT")) {
            lm = new double[] {1, 0, 0, 1, 0, 0};
          } else if (op.equals("Tf") && stack.size() >= 2) {
            Object fn = stack.get(stack.size() - 2);
            Object sz = stack.get(stack.size() - 1);
            curFont = nameOf(fn);
            curSize = numOf(sz);
            stack.clear();
          } else if ((op.equals("Td") || op.equals("TD"))
              && stack.size() >= 2) {
            double tx = numOf(stack.get(stack.size() - 2));
            double ty = numOf(stack.get(stack.size() - 1));
            if (op.equals("TD")) {
              ty = -ty;
            }
            double e = lm[4] + tx;
            double f = lm[5] + ty;
            lm = new double[] {1, 0, 0, 1, e, f};
            stack.clear();
          } else if (op.equals("Tm") && stack.size() >= 6) {
            int n = stack.size();
            lm = new double[] {numOf(stack.get(n - 6)),
                numOf(stack.get(n - 5)), numOf(stack.get(n - 4)),
                numOf(stack.get(n - 3)), numOf(stack.get(n - 2)),
                numOf(stack.get(n - 1))};
            stack.clear();
          } else if (op.equals("T*")) {
            double e = lm[4];
            double f = lm[5] - 12;
            lm = new double[] {1, 0, 0, 1, e, f};
            stack.clear();
          } else if (op.equals("Tj") && stack.size() >= 1) {
            Object s = stack.get(stack.size() - 1);
            if (s instanceof byte[]) {
              addRun(runs, fonts, curFont, curSize, lm[4], lm[5],
                  decodeString((byte[]) s, fonts.get(curFont)));
            }
            stack.clear();
          } else if (op.equals("TJ") && stack.size() >= 1) {
            Object a = stack.get(stack.size() - 1);
            if (a instanceof List) {
              addRun(runs, fonts, curFont, curSize, lm[4], lm[5],
                  decodeTJ((List<Object>) a, fonts.get(curFont)));
            }
            stack.clear();
          } else if ((op.equals("'") || op.equals("\""))
              && stack.size() >= 1) {
            Object s = stack.get(stack.size() - 1);
            if (s instanceof byte[]) {
              double e = lm[4];
              double f = lm[5] - 12;
              lm = new double[] {1, 0, 0, 1, e, f};
              addRun(runs, fonts, curFont, curSize, lm[4], lm[5],
                  decodeString((byte[]) s, fonts.get(curFont)));
            }
            stack.clear();
          } else if (op.equals("[") || op.equals("]")) {
            if (op.equals("]")) {
              // tutup array TJ: kumpulkan operand sejak "[" jadi List
              ArrayList<Object> arr = new ArrayList<Object>();
              while (!stack.isEmpty()) {
                Object top = stack.remove(stack.size() - 1);
                if (top instanceof Op
                    && ((Op) top).name.equals("[")) {
                  break;
                }
                arr.add(0, top);
              }
              stack.add(arr);
            } else {
              stack.add(t); // penanda buka
            }
          } else {
            // Tc/Tw/Tz/TL/Ts/Trm/dll = abaikan operandnya
            stack.clear();
          }
        } else {
          stack.add(t);
          if (stack.size() > 32) {
            stack.remove(0);
          }
        }
      }
    } catch (Exception e) {
      // pakai run yang sudah dapat
    }
    return composeRuns(runs);
  }

  private static String nameOf(Object o) {
    if (o instanceof String) {
      String s = (String) o;
      return s.startsWith("/") ? s.substring(1) : s;
    }
    return "";
  }

  private static double numOf(Object o) {
    if (o instanceof Number) {
      return ((Number) o).doubleValue();
    }
    return 0;
  }

  private static void addRun(ArrayList<Run> runs, Map<String, PdfFont> fonts,
      String fontKey, double size, double x, double y, String text) {
    if (text == null || text.length() == 0) {
      return;
    }
    Run r = new Run();
    r.x = x;
    r.y = y;
    r.size = size <= 0 ? 12 : size;
    r.text = text;
    runs.add(r);
  }

  /** Decode byte string via font (ToUnicode dulu, lalu Encoding). */
  private static String decodeString(byte[] bs, PdfFont f) {
    if (bs.length == 0) {
      return "";
    }
    StringBuilder sb = new StringBuilder(bs.length);
    if (f != null && !f.toUni.isEmpty()) {
      int i = 0;
      while (i < bs.length) {
        int b0 = bs[i] & 0xFF;
        String s = null;
        if (f.wide && i + 1 < bs.length) {
          s = f.toUni.get(Integer.valueOf((b0 << 8) | (bs[i + 1] & 0xFF)));
          if (s != null) {
            sb.append(s);
            i = i + 2;
            continue;
          }
        }
        if (!f.wide && i + 1 < bs.length) {
          s = f.toUni.get(Integer.valueOf((b0 << 8) | (bs[i + 1] & 0xFF)));
          if (s != null) {
            sb.append(s);
            i = i + 2;
            continue;
          }
        }
        s = f.toUni.get(Integer.valueOf(b0));
        if (s != null) {
          sb.append(s);
        } else {
          sb.append(WINANSI[b0]);
        }
        i = i + 1;
      }
      return sb.toString();
    }
    for (int i = 0; i < bs.length; i = i + 1) {
      int b0 = bs[i] & 0xFF;
      String g = f == null ? null : f.diff.get(Integer.valueOf(b0));
      if (g != null) {
        sb.append(g);
      } else {
        sb.append(WINANSI[b0]);
      }
    }
    return sb.toString();
  }

  /** Decode array TJ: angka < -80 = jeda kata (spasi). */
  private static String decodeTJ(List<Object> arr, PdfFont f) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < arr.size(); i = i + 1) {
      Object o = arr.get(i);
      if (o instanceof byte[]) {
        sb.append(decodeString((byte[]) o, f));
      } else if (o instanceof Number) {
        if (((Number) o).doubleValue() < -80) {
          sb.append(' ');
        }
      }
    }
    return sb.toString();
  }

  /** Susun run jadi teks: kelompok baris (toleransi y), urut x, spasi dari gap. */
  private static String composeRuns(ArrayList<Run> runs) {
    if (runs.isEmpty()) {
      return "";
    }
    // urut y menurun
    for (int i = 1; i < runs.size(); i = i + 1) {
      Run k = runs.get(i);
      int j = i - 1;
      while (j >= 0 && runs.get(j).y < k.y) {
        runs.set(j + 1, runs.get(j));
        j = j - 1;
      }
      runs.set(j + 1, k);
    }
    StringBuilder out = new StringBuilder();
    int i = 0;
    while (i < runs.size()) {
      int j = i;
      double y0 = runs.get(i).y;
      double tol = runs.get(i).size * 0.5;
      if (tol < 1) {
        tol = 1;
      }
      while (j + 1 < runs.size()
          && Math.abs(runs.get(j + 1).y - y0) <= tol) {
        j = j + 1;
      }
      // urut x menaik dalam baris [i..j]
      for (int a = i + 1; a <= j; a = a + 1) {
        Run k = runs.get(a);
        int b = a - 1;
        while (b >= i && runs.get(b).x > k.x) {
          runs.set(b + 1, runs.get(b));
          b = b - 1;
        }
        runs.set(b + 1, k);
      }
      double endX = 0;
      boolean first = true;
      for (int k = i; k <= j; k = k + 1) {
        Run r = runs.get(k);
        if (!first) {
          double gap = r.x - endX;
          if (gap > r.size * 0.25) {
            out.append(' ');
          }
        }
        out.append(r.text);
        endX = r.x + r.text.length() * r.size * 0.5;
        first = false;
      }
      out.append('\n');
      i = j + 1;
    }
    // rapikan: buang spasi ujung baris + baris kosong berlebih
    String[] lines = out.toString().split("\n");
    StringBuilder fin = new StringBuilder();
    for (int k = 0; k < lines.length; k = k + 1) {
      String t = lines[k].trim();
      if (t.length() == 0) {
        continue;
      }
      if (fin.length() > 0) {
        fin.append('\n');
      }
      fin.append(t);
    }
    return fin.toString();
  }

  // ================= token content stream =================

  private static final class Op {
    String name = "";
  }

  /** Tokenizer kecil: byte[] mentah, nama /string (hex), array, angka, op. */
  private static final class TokReader {
    final byte[] b;
    int p;

    TokReader(byte[] b) {
      this.b = b;
      this.p = 0;
    }

    private void skipWs() {
      while (p < b.length) {
        byte c = b[p];
        if (c == 0 || c == 9 || c == 10 || c == 12 || c == 13 || c == 32) {
          p = p + 1;
        } else if (c == '%') {
          while (p < b.length && b[p] != 10 && b[p] != 13) {
            p = p + 1;
          }
        } else {
          break;
        }
      }
    }

    Object next() {
      skipWs();
      if (p >= b.length) {
        return null;
      }
      byte c = b[p];
      if (c == '(') {
        return readLiteral();
      }
      if (c == '<') {
        if (p + 1 < b.length && b[p + 1] == '<') {
          p = p + 2;
          Op o = new Op();
          o.name = "<<";
          return o;
        }
        return readHex();
      }
      if (c == '>') {
        if (p + 1 < b.length && b[p + 1] == '>') {
          p = p + 2;
          Op o = new Op();
          o.name = ">>";
          return o;
        }
        p = p + 1;
        return next();
      }
      if (c == '[' || c == ']') {
        p = p + 1;
        Op o = new Op();
        o.name = String.valueOf((char) c);
        return o;
      }
      if (c == '/') {
        int s = p;
        p = p + 1;
        while (p < b.length && !isDelim(b[p])) {
          p = p + 1;
        }
        try {
          return new String(b, s, p - s, "US-ASCII");
        } catch (Exception e) {
          return "/x";
        }
      }
      if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.') {
        int s = p;
        boolean dot = false;
        if (c == '-' || c == '+') {
          p = p + 1;
        }
        while (p < b.length) {
          byte d = b[p];
          if (d >= '0' && d <= '9') {
            p = p + 1;
          } else if (d == '.' && !dot) {
            dot = true;
            p = p + 1;
          } else {
            break;
          }
        }
        // cek R (referensi) — jarang di content, tapi tangani
        int q = p;
        while (q < b.length && (b[q] == 32 || b[q] == 10 || b[q] == 13
            || b[q] == 9)) {
          q = q + 1;
        }
        try {
          String ns = new String(b, s, p - s, "US-ASCII");
          if (dot) {
            return Double.valueOf(Double.parseDouble(ns));
          }
          return Long.valueOf(Long.parseLong(ns));
        } catch (Exception e) {
          p = s + 1;
          Op o = new Op();
          o.name = String.valueOf((char) b[s]);
          return o;
        }
      }
      // operator / kata kunci
      int s = p;
      while (p < b.length && !isDelim(b[p])) {
        p = p + 1;
      }
      if (p == s) {
        p = p + 1;
        return next();
      }
      Op o = new Op();
      try {
        o.name = new String(b, s, p - s, "US-ASCII");
      } catch (Exception e) {
        o.name = "?";
      }
      if (o.name.equals("ID")) {
        skipInlineImage();
        return next();
      }
      return o;
    }

    /** Lewati data biner gambar inline (ID ... EI) agar tak jadi teks sampah. */
    private void skipInlineImage() {
      if (p < b.length && (b[p] == 32 || b[p] == 10 || b[p] == 13
          || b[p] == 9 || b[p] == 12 || b[p] == 0)) {
        p = p + 1;
      }
      while (p + 3 < b.length) {
        if ((b[p] == 32 || b[p] == 10 || b[p] == 13 || b[p] == 9
            || b[p] == 12) && b[p + 1] == 'E' && b[p + 2] == 'I') {
          byte n = b[p + 3];
          if (n == 32 || n == 10 || n == 13 || n == 9 || n == 12 || n == 0) {
            p = p + 3;
            return;
          }
        }
        p = p + 1;
      }
      p = b.length;
    }

    private boolean isDelim(byte d) {
      return d == 0 || d == 9 || d == 10 || d == 12 || d == 13 || d == 32
          || d == '(' || d == ')' || d == '<' || d == '>' || d == '['
          || d == ']' || d == '{' || d == '}' || d == '/' || d == '%';
    }

    private Object readLiteral() {
      // p di '('
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      p = p + 1;
      int depth = 1;
      while (p < b.length && depth > 0) {
        byte c = b[p];
        if (c == '\\') {
          p = p + 1;
          if (p >= b.length) {
            break;
          }
          byte e = b[p];
          if (e == 'n') {
            bos.write('\n');
          } else if (e == 'r') {
            bos.write('\r');
          } else if (e == 't') {
            bos.write('\t');
          } else if (e == 'b') {
            bos.write('\b');
          } else if (e == 'f') {
            bos.write('\f');
          } else if (e == '(') {
            bos.write('(');
          } else if (e == ')') {
            bos.write(')');
          } else if (e == '\\') {
            bos.write('\\');
          } else if (e == 10) {
            // lanjut baris = buang
          } else if (e == 13) {
            if (p + 1 < b.length && b[p + 1] == 10) {
              p = p + 1;
            }
          } else if (e >= '0' && e <= '7') {
            int v = e - '0';
            for (int k = 0; k < 2; k = k + 1) {
              if (p + 1 < b.length && b[p + 1] >= '0' && b[p + 1] <= '7') {
                p = p + 1;
                v = v * 8 + (b[p] - '0');
              } else {
                break;
              }
            }
            bos.write(v & 0xFF);
          } else {
            bos.write(e);
          }
          p = p + 1;
        } else if (c == '(') {
          depth = depth + 1;
          bos.write('(');
          p = p + 1;
        } else if (c == ')') {
          depth = depth - 1;
          if (depth > 0) {
            bos.write(')');
          }
          p = p + 1;
        } else {
          bos.write(c);
          p = p + 1;
        }
      }
      return bos.toByteArray();
    }

    private Object readHex() {
      // p di '<'
      p = p + 1;
      StringBuilder sb = new StringBuilder();
      while (p < b.length && b[p] != '>') {
        byte c = b[p];
        if (c == 32 || c == 10 || c == 13 || c == 9 || c == 12 || c == 0) {
          p = p + 1;
          continue;
        }
        sb.append((char) c);
        p = p + 1;
      }
      if (p < b.length) {
        p = p + 1;
      }
      byte[] r = hexToBytes(sb.toString());
      return r == null ? new byte[0] : r;
    }
  }

  // ================= decode stream =================

  /** Decode semua filter sebuah stream. Null bila ada filter tak didukung. */
  static byte[] decodeStream(StreamObj s) {
    try {
      byte[] data = s.raw;
      Object f = s.dict.get("Filter");
      ArrayList<String> filters = new ArrayList<String>();
      if (f instanceof String) {
        String n = (String) f;
        filters.add(n.startsWith("/") ? n.substring(1) : n);
      } else if (f instanceof List) {
        List<Object> l = (List<Object>) f;
        for (int i = 0; i < l.size(); i = i + 1) {
          if (l.get(i) instanceof String) {
            String n = (String) l.get(i);
            filters.add(n.startsWith("/") ? n.substring(1) : n);
          }
        }
      }
      Object dp = s.dict.get("DecodeParms");
      ArrayList<Map<String, Object>> parms = new ArrayList<Map<String, Object>>();
      if (dp instanceof Map) {
        parms.add((Map<String, Object>) dp);
      } else if (dp instanceof List) {
        List<Object> l = (List<Object>) dp;
        for (int i = 0; i < l.size(); i = i + 1) {
          if (l.get(i) instanceof Map) {
            parms.add((Map<String, Object>) l.get(i));
          } else {
            parms.add(null);
          }
        }
      }
      for (int i = 0; i < filters.size(); i = i + 1) {
        String fn = filters.get(i);
        Map<String, Object> pm = i < parms.size() ? parms.get(i) : null;
        if (fn.equals("FlateDecode") || fn.equals("Fl")) {
          data = flateDecode(data, pm);
        } else if (fn.equals("ASCII85Decode") || fn.equals("A85")) {
          data = ascii85Decode(data);
        } else if (fn.equals("ASCIIHexDecode") || fn.equals("AHx")) {
          data = asciiHexDecode(data);
        } else if (fn.equals("RunLengthDecode") || fn.equals("RL")) {
          data = runLengthDecode(data);
        } else if (fn.equals("LZWDecode") || fn.equals("LZW")) {
          return null;
        } else if (fn.equals("Crypt")) {
          // sudah ditolak di level dokumen bila /Encrypt ada
        } else if (fn.equals("DCTDecode") || fn.equals("DCT")
            || fn.equals("JPXDecode") || fn.equals("JBIG2Decode")
            || fn.equals("CCITTFaxDecode") || fn.equals("CCF")) {
          return null; // gambar/fax: tak relevan untuk teks
        } else {
          return null;
        }
      }
      return data;
    } catch (Exception e) {
      return null;
    }
  }

  private static byte[] flateDecode(byte[] data, Map<String, Object> parms)
      throws IOException {
    byte[] out;
    try {
      out = inflate(data, false);
    } catch (Exception e) {
      out = inflate(data, true);
    }
    if (parms != null) {
      long pred = 1;
      Object po = parms.get("Predictor");
      if (po instanceof Number) {
        pred = ((Number) po).longValue();
      }
      if (pred >= 10) {
        long cols = 1;
        long colors = 1;
        Object co = parms.get("Columns");
        if (co instanceof Number) {
          cols = ((Number) co).longValue();
        }
        Object cl = parms.get("Colors");
        if (cl instanceof Number) {
          colors = ((Number) cl).longValue();
        }
        if (cols > 0 && cols < 10000 && colors >= 1 && colors <= 8) {
          out = pngUnfilter(out, (int) cols * (int) colors);
        }
      }
    }
    return out;
  }

  private static byte[] inflate(byte[] data, boolean raw) throws IOException {
    Inflater inf = new Inflater(raw);
    try {
      inf.setInput(data);
      ByteArrayOutputStream bos = new ByteArrayOutputStream(
          Math.min(data.length * 3, 8388608));
      byte[] buf = new byte[32768];
      while (!inf.finished()) {
        int n;
        try {
          n = inf.inflate(buf);
        } catch (java.util.zip.DataFormatException e) {
          throw new IOException("flate rusak");
        }
        if (n == 0) {
          if (inf.needsInput()) {
            break;
          }
          try {
            Thread.sleep(0);
          } catch (InterruptedException e) {
            // abaikan
          }
          break;
        }
        bos.write(buf, 0, n);
        if (bos.size() > MAX_BYTES) {
          throw new IOException("stream terlalu besar");
        }
      }
      bos.flush();
      return bos.toByteArray();
    } finally {
      try {
        inf.end();
      } catch (Exception e) {
        // abaikan
      }
    }
  }

  /** Balikkan filter baris PNG (None/Sub/Up/Average/Paeth). */
  private static byte[] pngUnfilter(byte[] data, int rowLen) {
    try {
      if (rowLen <= 0) {
        return data;
      }
      int stride = rowLen + 1;
      if (data.length % stride != 0) {
        return data;
      }
      int rows = data.length / stride;
      byte[] out = new byte[rows * rowLen];
      byte[] prev = new byte[rowLen];
      for (int r = 0; r < rows; r = r + 1) {
        int fo = r * stride;
        int type = data[fo] & 0xFF;
        byte[] cur = new byte[rowLen];
        System.arraycopy(data, fo + 1, cur, 0, rowLen);
        for (int i = 0; i < rowLen; i = i + 1) {
          int a = i > 0 ? (cur[i - 1] & 0xFF) : 0;
          int b = prev[i] & 0xFF;
          int c = i > 0 ? (prev[i - 1] & 0xFF) : 0;
          int v = cur[i] & 0xFF;
          if (type == 1) {
            v = (v + a) & 0xFF;
          } else if (type == 2) {
            v = (v + b) & 0xFF;
          } else if (type == 3) {
            v = (v + ((a + b) >> 1)) & 0xFF;
          } else if (type == 4) {
            int p = a + b - c;
            int pa = Math.abs(p - a);
            int pb = Math.abs(p - b);
            int pc = Math.abs(p - c);
            int pr = pa <= pb && pa <= pc ? a : (pb <= pc ? b : c);
            v = (v + pr) & 0xFF;
          }
          cur[i] = (byte) v;
        }
        System.arraycopy(cur, 0, out, r * rowLen, rowLen);
        prev = cur;
      }
      return out;
    } catch (Exception e) {
      return data;
    }
  }

  private static byte[] ascii85Decode(byte[] data) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    long tuple = 0;
    int count = 0;
    for (int i = 0; i < data.length; i = i + 1) {
      int c = data[i] & 0xFF;
      if (c == 'z' && count == 0) {
        bos.write(0);
        bos.write(0);
        bos.write(0);
        bos.write(0);
        continue;
      }
      if (c == '~') {
        break;
      }
      if (c <= 32) {
        continue;
      }
      if (c < '!' || c > 'u') {
        throw new IOException("ascii85 rusak");
      }
      tuple = tuple * 85 + (c - '!');
      count = count + 1;
      if (count == 5) {
        bos.write((int) ((tuple >> 24) & 0xFF));
        bos.write((int) ((tuple >> 16) & 0xFF));
        bos.write((int) ((tuple >> 8) & 0xFF));
        bos.write((int) (tuple & 0xFF));
        tuple = 0;
        count = 0;
      }
    }
    if (count > 0) {
      if (count == 1) {
        throw new IOException("ascii85 rusak");
      }
      for (int k = count; k < 5; k = k + 1) {
        tuple = tuple * 85 + 84;
      }
      for (int k = 0; k < count - 1; k = k + 1) {
        bos.write((int) ((tuple >> (24 - k * 8)) & 0xFF));
      }
    }
    bos.flush();
    return bos.toByteArray();
  }

  private static byte[] asciiHexDecode(byte[] data) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    int hi = -1;
    for (int i = 0; i < data.length; i = i + 1) {
      int c = data[i] & 0xFF;
      if (c == '>') {
        break;
      }
      int v;
      if (c >= '0' && c <= '9') {
        v = c - '0';
      } else if (c >= 'A' && c <= 'F') {
        v = c - 'A' + 10;
      } else if (c >= 'a' && c <= 'f') {
        v = c - 'a' + 10;
      } else {
        continue;
      }
      if (hi < 0) {
        hi = v;
      } else {
        bos.write((hi << 4) | v);
        hi = -1;
      }
    }
    if (hi >= 0) {
      bos.write(hi << 4);
    }
    bos.flush();
    return bos.toByteArray();
  }

  private static byte[] runLengthDecode(byte[] data) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    int i = 0;
    while (i < data.length) {
      int n = data[i] & 0xFF;
      i = i + 1;
      if (n == 128) {
        break;
      }
      if (n <= 127) {
        int len = n + 1;
        if (i + len > data.length) {
          throw new IOException("runlength rusak");
        }
        bos.write(data, i, len);
        i = i + len;
      } else {
        int len = 257 - n;
        if (i >= data.length) {
          throw new IOException("runlength rusak");
        }
        int v = data[i] & 0xFF;
        i = i + 1;
        for (int k = 0; k < len; k = k + 1) {
          bos.write(v);
        }
      }
    }
    bos.flush();
    return bos.toByteArray();
  }

  // ================= reader xref/objek =================

  /** State baca file: offset objek + cache + trailer. */
  private static final class Reader {
    final byte[] data;
    final HashMap<Integer, Long> offsets = new HashMap<Integer, Long>();
    final HashMap<Integer, int[]> compressed = new HashMap<Integer, int[]>();
    final HashMap<Integer, Object> cache = new HashMap<Integer, Object>();
    final HashMap<Integer, Map<String, Object>> objStms =
        new HashMap<Integer, Map<String, Object>>();
    Map<String, Object> trailer = new HashMap<String, Object>();

    Reader(byte[] data) {
      this.data = data;
    }

    // ---------- util teks ----------

    int skipWs(int p) {
      while (p < data.length) {
        byte c = data[p];
        if (c == 0 || c == 9 || c == 10 || c == 12 || c == 13 || c == 32) {
          p = p + 1;
        } else if (c == '%') {
          while (p < data.length && data[p] != 10 && data[p] != 13) {
            p = p + 1;
          }
        } else {
          break;
        }
      }
      return p;
    }

    boolean matchAt(int p, String s) {
      if (p < 0 || p + s.length() > data.length) {
        return false;
      }
      for (int i = 0; i < s.length(); i = i + 1) {
        if (data[p + i] != (byte) s.charAt(i)) {
          return false;
        }
      }
      return true;
    }

    String readWord(int p) {
      int s = p;
      while (p < data.length && data[p] > 32 && data[p] != '/'
          && data[p] != '<' && data[p] != '>' && data[p] != '['
          && data[p] != ']' && data[p] != '(' && data[p] != ')') {
        p = p + 1;
      }
      try {
        return new String(data, s, p - s, "US-ASCII");
      } catch (Exception e) {
        return "";
      }
    }

    long readLong(int p) {
      int s = p;
      if (p < data.length && (data[p] == '-' || data[p] == '+')) {
        p = p + 1;
      }
      while (p < data.length && data[p] >= '0' && data[p] <= '9') {
        p = p + 1;
      }
      try {
        return Long.parseLong(new String(data, s, p - s, "US-ASCII").trim());
      } catch (Exception e) {
        return 0;
      }
    }

    // ---------- resolve ----------

    Object resolve(Object o) {
      return resolve(o, 0);
    }

    Object resolve(Object o, int depth) {
      if (!(o instanceof Ref) || depth > 50) {
        return o;
      }
      Ref r = (Ref) o;
      Object hit = cache.get(Integer.valueOf(r.num));
      if (hit != null) {
        return hit;
      }
      if (compressed.containsKey(Integer.valueOf(r.num))) {
        int[] ci = compressed.get(Integer.valueOf(r.num));
        Map<String, Object> stm = objStm(ci[0]);
        Object v = stm == null ? null : stm.get("@" + ci[1]);
        if (v != null) {
          cache.put(Integer.valueOf(r.num), v);
        }
        return v;
      }
      Long off = offsets.get(Integer.valueOf(r.num));
      if (off == null) {
        return null;
      }
      try {
        Object v = readIndirect(off.longValue());
        if (v != null) {
          cache.put(Integer.valueOf(r.num), v);
        }
        return v;
      } catch (Exception e) {
        return null;
      }
    }

    Map<String, Object> asDict(Object o) {
      if (o instanceof Map) {
        return (Map<String, Object>) o;
      }
      return null;
    }

    String asName(Object o) {
      if (o instanceof String) {
        return (String) o;
      }
      return "";
    }

    long asLong(Object o, long def) {
      if (o instanceof Number) {
        return ((Number) o).longValue();
      }
      return def;
    }

    double[] asRect(Object o) {
      if (!(o instanceof List)) {
        return null;
      }
      List<Object> l = (List<Object>) o;
      if (l.size() < 4) {
        return null;
      }
      double[] r = new double[4];
      for (int i = 0; i < 4; i = i + 1) {
        Object v = l.get(i);
        r[i] = v instanceof Number ? ((Number) v).doubleValue() : 0;
      }
      return r;
    }

    // ---------- xref ----------

    void readXrefs() {
      try {
        int sx = lastIndexOf("startxref");
        if (sx < 0) {
          bruteScan();
          return;
        }
        int p = skipWs(sx + 9);
        StringBuilder sb = new StringBuilder();
        while (p < data.length && data[p] >= '0' && data[p] <= '9') {
          sb.append((char) data[p]);
          p = p + 1;
        }
        long off = 0;
        try {
          off = Long.parseLong(sb.toString());
        } catch (Exception e) {
          bruteScan();
          return;
        }
        int found = 0;
        for (int guard = 0; guard < 20 && off > 0 && off < data.length;
            guard = guard + 1) {
          int q = skipWs((int) off);
          if (matchAt(q, "xref")) {
            off = readXrefTable(q + 4);
            found = found + 1;
          } else {
            // objek = xref stream
            Object v = null;
            try {
              v = readIndirect(off);
            } catch (Exception e) {
              v = null;
            }
            if (v instanceof StreamObj) {
              off = readXrefStream((StreamObj) v);
              found = found + 1;
            } else {
              break;
            }
          }
          if (off <= 0) {
            break;
          }
        }
        if (found == 0 || trailer.isEmpty()) {
          bruteScan();
        }
      } catch (Exception e) {
        bruteScan();
      }
    }

    private int lastIndexOf(String s) {
      byte[] pat = null;
      try {
        pat = s.getBytes("US-ASCII");
      } catch (Exception e) {
        return -1;
      }
      int from = data.length - pat.length - 64;
      if (from < 0) {
        from = 0;
      }
      // cari di 8 KB terakhir dulu (startxref di ekor), lalu seluruh file
      for (int pass = 0; pass < 2; pass = pass + 1) {
        int lo = pass == 0 ? Math.max(0, data.length - 8192) : 0;
        for (int i = from; i >= lo; i = i - 1) {
          boolean ok = true;
          for (int j = 0; j < pat.length; j = j + 1) {
            if (data[i + j] != pat[j]) {
              ok = false;
              break;
            }
          }
          if (ok) {
            return i;
          }
        }
        from = data.length - pat.length;
      }
      return -1;
    }

    /** Parse seksi "xref" klasik. Kembali offset /Prev (0 bila tak ada). */
    private long readXrefTable(int p) {
      try {
        p = skipWs(p);
        while (p < data.length) {
          if (matchAt(p, "trailer")) {
            Val v = parseValue(data, p + 7, 0);
            if (v.val instanceof Map) {
              mergeTrailer((Map<String, Object>) v.val);
            }
            Object pv = trailer.get("Prev");
            if (pv instanceof Number) {
              return ((Number) pv).longValue();
            }
            return 0;
          }
          // "first count"
          long first = readLong(p);
          while (p < data.length && data[p] != 10 && data[p] != 13
              && data[p] != 32) {
            p = p + 1;
          }
          p = skipWs(p);
          long count = readLong(p);
          while (p < data.length && data[p] >= '0' && data[p] <= '9') {
            p = p + 1;
          }
          p = skipWs(p);
          for (long i = 0; i < count && p < data.length; i = i + 1) {
            // "oooooooooo ggggg n"
            long off = 0;
            int d = 0;
            while (d < 10 && p < data.length) {
              byte c = data[p];
              if (c >= '0' && c <= '9') {
                off = off * 10 + (c - '0');
              }
              p = p + 1;
              d = d + 1;
            }
            p = skipWs(p);
            while (p < data.length && data[p] >= '0' && data[p] <= '9') {
              p = p + 1;
            }
            p = skipWs(p);
            boolean used = p < data.length && data[p] == 'n';
            p = p + 1;
            // ke ujung baris
            while (p < data.length && data[p] != 10 && data[p] != 13) {
              p = p + 1;
            }
            p = skipWs(p);
            if (used) {
              int num = (int) (first + i);
              if (!offsets.containsKey(Integer.valueOf(num))) {
                offsets.put(Integer.valueOf(num), Long.valueOf(off));
              }
            }
          }
        }
      } catch (Exception e) {
        // abaikan
      }
      Object pv = trailer.get("Prev");
      if (pv instanceof Number) {
        return ((Number) pv).longValue();
      }
      return 0;
    }

    /** Parse xref stream. Kembali offset /Prev (0 bila tak ada). */
    private long readXrefStream(StreamObj s) {
      try {
        mergeDict(s.dict);
        byte[] raw = decodeStream(s);
        if (raw == null) {
          return 0;
        }
        int w0 = 1;
        int w1 = 4;
        int w2 = 2;
        Object w = resolve(s.dict.get("W"));
        if (w instanceof List && ((List<Object>) w).size() >= 3) {
          List<Object> wl = (List<Object>) w;
          w0 = (int) asLong(wl.get(0), 1);
          w1 = (int) asLong(wl.get(1), 4);
          w2 = (int) asLong(wl.get(2), 2);
        }
        int stride = w0 + w1 + w2;
        // Index (default 0 Size)
        ArrayList<int[]> ranges = new ArrayList<int[]>();
        Object ix = resolve(s.dict.get("Index"));
        if (ix instanceof List && ((List<Object>) ix).size() >= 2) {
          List<Object> il = (List<Object>) ix;
          for (int i = 0; i + 1 < il.size(); i = i + 2) {
            ranges.add(new int[] {(int) asLong(il.get(i), 0),
                (int) asLong(il.get(i + 1), 0)});
          }
        } else {
          long size = asLong(resolve(s.dict.get("Size")), 0);
          ranges.add(new int[] {0, (int) size});
        }
        int pos = 0;
        for (int ri = 0; ri < ranges.size(); ri = ri + 1) {
          int start = ranges.get(ri)[0];
          int count = ranges.get(ri)[1];
          for (int i = 0; i < count; i = i + 1) {
            if (pos + stride > raw.length) {
              break;
            }
            long f0 = readBig(raw, pos, w0);
            long f1 = readBig(raw, pos + w0, w1);
            long f2 = readBig(raw, pos + w0 + w1, w2);
            pos = pos + stride;
            int num = start + i;
            if (f0 == 0) {
              continue; // bebas
            } else if (f0 == 1) {
              if (!offsets.containsKey(Integer.valueOf(num))) {
                offsets.put(Integer.valueOf(num), Long.valueOf(f1));
              }
            } else if (f0 == 2) {
              compressed.put(Integer.valueOf(num),
                  new int[] {(int) f1, (int) f2});
            }
          }
        }
        Object pv = trailer.get("Prev");
        if (pv instanceof Number) {
          return ((Number) pv).longValue();
        }
      } catch (Exception e) {
        // abaikan
      }
      return 0;
    }

    private long readBig(byte[] b, int off, int w) {
      long v = 0;
      for (int i = 0; i < w && off + i < b.length; i = i + 1) {
        v = (v << 8) | (b[off + i] & 0xFF);
      }
      return v;
    }

    private void mergeTrailer(Map<String, Object> t) {
      mergeDict(t);
    }

    private void mergeDict(Map<String, Object> t) {
      for (String k : t.keySet()) {
        if (!trailer.containsKey(k)) {
          trailer.put(k, t.get(k));
        }
      }
      // Root/Info/Size/ID ikut; Prev ditimpa agar rantai jalan
      if (t.containsKey("Prev")) {
        trailer.put("Prev", t.get("Prev"));
      }
    }

    /** Object stream -> map index-dalam-stream ke objek. */
    private Map<String, Object> objStm(int num) {
      Integer key = Integer.valueOf(num);
      if (objStms.containsKey(key)) {
        return objStms.get(key);
      }
      HashMap<String, Object> m = new HashMap<String, Object>();
      objStms.put(key, m);
      try {
        Long off = offsets.get(key);
        if (off == null) {
          return m;
        }
        Object v = readIndirect(off.longValue());
        if (!(v instanceof StreamObj)) {
          return m;
        }
        StreamObj s = (StreamObj) v;
        byte[] raw = decodeStream(s);
        if (raw == null) {
          return m;
        }
        long n = asLong(resolve(s.dict.get("N")), 0);
        long first = asLong(resolve(s.dict.get("First")), 0);
        // header: N pasang angka dalam `first` byte pertama
        String head;
        try {
          head = new String(raw, 0, (int) Math.min(first, raw.length),
              "US-ASCII");
        } catch (Exception e) {
          return m;
        }
        String[] parts = head.trim().split("\\s+");
        for (long i = 0; i < n && i * 2 + 1 < parts.length; i = i + 1) {
          try {
            int idx = Integer.parseInt(parts[(int) (i * 2 + 1)]);
            Val vv = parseValue(raw, (int) (first + idx), 0);
            m.put("@" + parts[(int) (i * 2)], vv.val);
          } catch (Exception e) {
            // lewati objek rusak
          }
        }
      } catch (Exception e) {
        // abaikan
      }
      return m;
    }

    /** Pindai mentah semua "num gen obj" bila xref gagal total. */
    private void bruteScan() {
      try {
        int p = 0;
        while (p < data.length - 8) {
          if (data[p] >= '0' && data[p] <= '9') {
            int s = p;
            while (p < data.length && data[p] >= '0' && data[p] <= '9') {
              p = p + 1;
            }
            int q = skipWs(p);
            if (q < data.length && data[q] >= '0' && data[q] <= '9') {
              while (q < data.length && data[q] >= '0' && data[q] <= '9') {
                q = q + 1;
              }
              int r = skipWs(q);
              if (matchAt(r, "obj")) {
                try {
                  int num = Integer.parseInt(
                      new String(data, s, p - s, "US-ASCII"));
                  if (!offsets.containsKey(Integer.valueOf(num))) {
                    offsets.put(Integer.valueOf(num), Long.valueOf(s));
                  }
                } catch (Exception e) {
                  // abaikan
                }
                p = r + 3;
                continue;
              }
            }
          } else {
            p = p + 1;
          }
        }
        // trailer darurat: cari Root
        int c = lastIndexOf("/Root");
        if (c >= 0) {
          try {
            Val v = parseValue(data, c + 5, 0);
            trailer.put("Root", v.val);
          } catch (Exception e) {
            // abaikan
          }
        }
      } catch (Exception e) {
        // abaikan
      }
    }

    // ---------- baca objek tak langsung ----------

    Object readIndirect(long off) throws PdfException {
      int p = skipWs((int) off);
      Val num = parseValue(data, p, 0);
      Val gen = parseValue(data, num.end, 0);
      int q = skipWs(gen.end);
      if (!matchAt(q, "obj")) {
        throw new PdfException("obj rusak");
      }
      Val v = parseValue(data, q + 3, 0);
      // stream? (dict + stream...endstream)
      if (v.val instanceof Map) {
        int r = skipWs(v.end);
        if (matchAt(r, "stream")) {
          int bs = r + 6;
          if (bs < data.length && data[bs] == 13) {
            bs = bs + 1;
          }
          if (bs < data.length && data[bs] == 10) {
            bs = bs + 1;
          }
          Object lenObj = resolve(((Map<String, Object>) v.val).get("Length"));
          long len = lenObj instanceof Number
              ? ((Number) lenObj).longValue() : -1;
          byte[] raw;
          if (len >= 0 && bs + len <= data.length) {
            raw = new byte[(int) len];
            System.arraycopy(data, bs, raw, 0, (int) len);
          } else {
            // fallback: cari endstream
            int e = indexOf(data, "endstream", bs);
            if (e < 0) {
              throw new PdfException("stream rusak");
            }
            int ee = e;
            while (ee > bs && (data[ee - 1] == 10 || data[ee - 1] == 13
                || data[ee - 1] == 32)) {
              ee = ee - 1;
            }
            raw = new byte[ee - bs];
            System.arraycopy(data, bs, raw, 0, ee - bs);
          }
          StreamObj s = new StreamObj();
          s.dict = (Map<String, Object>) v.val;
          s.raw = raw;
          return s;
        }
      }
      return v.val;
    }

    private int indexOf(byte[] h, String needle, int from) {
      try {
        byte[] n = needle.getBytes("US-ASCII");
        for (int i = from; i + n.length <= h.length; i = i + 1) {
          boolean ok = true;
          for (int j = 0; j < n.length; j = j + 1) {
            if (h[i + j] != n[j]) {
              ok = false;
              break;
            }
          }
          if (ok) {
            return i;
          }
        }
      } catch (Exception e) {
        // abaikan
      }
      return -1;
    }

    // ---------- parse nilai ----------

    Val parseValue(byte[] h, int pos, int depth) throws PdfException {
      if (depth > 80) {
        throw new PdfException("PDF rusak (nesting terlalu dalam)");
      }
      int p = pos;
      while (p < h.length && (h[p] == 0 || h[p] == 9 || h[p] == 10
          || h[p] == 12 || h[p] == 13 || h[p] == 32)) {
        p = p + 1;
      }
      if (p >= h.length) {
        throw new PdfException("PDF terpotong");
      }
      byte c = h[p];
      if (c == '<' && p + 1 < h.length && h[p + 1] == '<') {
        HashMap<String, Object> m = new HashMap<String, Object>();
        p = p + 2;
        while (true) {
          int q = p;
          while (q < h.length && (h[q] == 0 || h[q] == 9 || h[q] == 10
              || h[q] == 12 || h[q] == 13 || h[q] == 32)) {
            q = q + 1;
          }
          if (q + 1 < h.length && h[q] == '>' && h[q + 1] == '>') {
            Val r = new Val();
            r.val = m;
            r.end = q + 2;
            return r;
          }
          if (q >= h.length || h[q] != '/') {
            throw new PdfException("dict rusak");
          }
          int ks = q;
          q = q + 1;
          while (q < h.length && h[q] > 32 && h[q] != '/' && h[q] != '<'
              && h[q] != '>' && h[q] != '[' && h[q] != ']' && h[q] != '('
              && h[q] != ')') {
            q = q + 1;
          }
          String key;
          try {
            key = new String(h, ks + 1, q - ks - 1, "US-ASCII");
          } catch (Exception e) {
            throw new PdfException("dict rusak");
          }
          Val v = parseValue(h, q, depth + 1);
          m.put(key, v.val);
          p = v.end;
        }
      }
      if (c == '[') {
        ArrayList<Object> l = new ArrayList<Object>();
        p = p + 1;
        while (true) {
          int q = p;
          while (q < h.length && (h[q] == 0 || h[q] == 9 || h[q] == 10
              || h[q] == 12 || h[q] == 13 || h[q] == 32)) {
            q = q + 1;
          }
          if (q >= h.length) {
            throw new PdfException("array rusak");
          }
          if (h[q] == ']') {
            Val r = new Val();
            r.val = l;
            r.end = q + 1;
            return r;
          }
          Val v = parseValue(h, q, depth + 1);
          l.add(v.val);
          p = v.end;
        }
      }
      if (c == '(') {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        p = p + 1;
        int lvl = 1;
        while (p < h.length && lvl > 0) {
          byte d = h[p];
          if (d == '\\') {
            p = p + 1;
            if (p >= h.length) {
              break;
            }
            byte e = h[p];
            if (e == 'n') {
              bos.write('\n');
            } else if (e == 'r') {
              bos.write('\r');
            } else if (e == 't') {
              bos.write('\t');
            } else if (e == 'b') {
              bos.write('\b');
            } else if (e == 'f') {
              bos.write('\f');
            } else if (e == '(') {
              bos.write('(');
            } else if (e == ')') {
              bos.write(')');
            } else if (e == '\\') {
              bos.write('\\');
            } else if (e == 10) {
              // lanjut baris
            } else if (e == 13) {
              if (p + 1 < h.length && h[p + 1] == 10) {
                p = p + 1;
              }
            } else if (e >= '0' && e <= '7') {
              int v = e - '0';
              for (int k = 0; k < 2; k = k + 1) {
                if (p + 1 < h.length && h[p + 1] >= '0' && h[p + 1] <= '7') {
                  p = p + 1;
                  v = v * 8 + (h[p] - '0');
                } else {
                  break;
                }
              }
              bos.write(v & 0xFF);
            } else {
              bos.write(e);
            }
            p = p + 1;
          } else if (d == '(') {
            lvl = lvl + 1;
            bos.write('(');
            p = p + 1;
          } else if (d == ')') {
            lvl = lvl - 1;
            if (lvl > 0) {
              bos.write(')');
            }
            p = p + 1;
          } else {
            bos.write(d);
            p = p + 1;
          }
        }
        Val r = new Val();
        r.val = bos.toByteArray();
        r.end = p;
        return r;
      }
      if (c == '<') {
        StringBuilder sb = new StringBuilder();
        p = p + 1;
        while (p < h.length && h[p] != '>') {
          byte d = h[p];
          if (d == 32 || d == 10 || d == 13 || d == 9 || d == 12 || d == 0) {
            p = p + 1;
            continue;
          }
          sb.append((char) d);
          p = p + 1;
        }
        if (p < h.length) {
          p = p + 1;
        }
        byte[] r = hexToBytes(sb.toString());
        Val v = new Val();
        v.val = r == null ? new byte[0] : r;
        v.end = p;
        return v;
      }
      if (c == '/') {
        int s = p;
        p = p + 1;
        while (p < h.length && h[p] > 32 && h[p] != '/' && h[p] != '<'
            && h[p] != '>' && h[p] != '[' && h[p] != ']' && h[p] != '('
            && h[p] != ')') {
          p = p + 1;
        }
        Val v = new Val();
        try {
          v.val = new String(h, s, p - s, "US-ASCII");
        } catch (Exception e) {
          v.val = "/x";
        }
        v.end = p;
        return v;
      }
      // true false null / angka / referensi
      if (matchWord(h, p, "true")) {
        Val v = new Val();
        v.val = Boolean.valueOf(true);
        v.end = p + 4;
        return v;
      }
      if (matchWord(h, p, "false")) {
        Val v = new Val();
        v.val = Boolean.valueOf(false);
        v.end = p + 5;
        return v;
      }
      if (matchWord(h, p, "null")) {
        Val v = new Val();
        v.val = null;
        v.end = p + 4;
        return v;
      }
      if (matchWord(h, p, "obj") || matchWord(h, p, "endobj")
          || matchWord(h, p, "stream") || matchWord(h, p, "endstream")
          || matchWord(h, p, "xref") || matchWord(h, p, "trailer")
          || matchWord(h, p, "startxref")) {
        throw new PdfException("token tak terduga");
      }
      // angka (mungkin referensi "num gen R")
      int s = p;
      if (p < h.length && (h[p] == '-' || h[p] == '+')) {
        p = p + 1;
      }
      boolean dot = false;
      boolean any = false;
      while (p < h.length && ((h[p] >= '0' && h[p] <= '9')
          || (h[p] == '.' && !dot))) {
        if (h[p] == '.') {
          dot = true;
        } else {
          any = true;
        }
        p = p + 1;
      }
      if (!any) {
        throw new PdfException("token tak dikenal");
      }
      String ns;
      try {
        ns = new String(h, s, p - s, "US-ASCII");
      } catch (Exception e) {
        throw new PdfException("angka rusak");
      }
      // referensi?
      if (!dot) {
        int q = p;
        while (q < h.length && (h[q] == 32 || h[q] == 10 || h[q] == 13
            || h[q] == 9 || h[q] == 0)) {
          q = q + 1;
        }
        int r = q;
        while (r < h.length && h[r] >= '0' && h[r] <= '9') {
          r = r + 1;
        }
        if (r > q) {
          int t = r;
          while (t < h.length && (h[t] == 32 || h[t] == 10 || h[t] == 13
              || h[t] == 9 || h[t] == 0)) {
            t = t + 1;
          }
          if (t < h.length && h[t] == 'R'
              && (t + 1 >= h.length || !isWordChar(h[t + 1]))) {
            try {
              Ref rf = new Ref();
              rf.num = Integer.parseInt(ns.trim());
              rf.gen = Integer.parseInt(
                  new String(h, q, r - q, "US-ASCII").trim());
              Val v = new Val();
              v.val = rf;
              v.end = t + 1;
              return v;
            } catch (Exception e) {
              // jatuh ke angka biasa
            }
          }
        }
      }
      Val v = new Val();
      try {
        if (dot) {
          v.val = Double.valueOf(Double.parseDouble(ns));
        } else {
          v.val = Long.valueOf(Long.parseLong(ns));
        }
      } catch (Exception e) {
        throw new PdfException("angka rusak");
      }
      v.end = p;
      return v;
    }

    private boolean matchWord(byte[] h, int p, String w) {
      try {
        byte[] n = w.getBytes("US-ASCII");
        if (p + n.length > h.length) {
          return false;
        }
        for (int i = 0; i < n.length; i = i + 1) {
          if (h[p + i] != n[i]) {
            return false;
          }
        }
        if (p + n.length < h.length && isWordChar(h[p + n.length])) {
          return false;
        }
        return true;
      } catch (Exception e) {
        return false;
      }
    }

    private boolean isWordChar(byte c) {
      return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
          || (c >= '0' && c <= '9');
    }
  }

  /** Nilai + posisi akhir (untuk objek mentah & pesan). */
  private static final class Val {
    Object val;
    int end;
  }
}
