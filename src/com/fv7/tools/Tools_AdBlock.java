package com.fv7.tools;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.YailList;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@DesignerComponent(
    version = 3,
    description = "Tools_AdBlock: mesin AdBlock AUTO ala 1DM — banyak list beda "
        + "tipe dipakai SEKALIGUS dalam satu mesin gabungan (bukan per-id). "
        + "Sumber bawaan 27 URL (EasyList, uAssets, AdGuard, Fanboy, ABPindo, "
        + "oisd, 1Hosts, yoyo, anti-adblock). Download + parse + pisah ke 4 "
        + "ember OTOMATIS: hosts, network rules, whitelist (@@), cosmetic (##). "
        + "Event AdBlockUpdated membawa var TERPISAH yang sudah tersaring — "
        + "var adhost = pasti host, tidak campur rules. Keluaran teks newline "
        + "langsung colok Sunny (AdHosts / AddFilterRules) + format umum list. "
        + "Update: Manual / TiapBuka / Harian / Mingguan / Auto-ikut-pembuat "
        + "(! Expires:, fallback Harian). Global tanpa id (dipakai bareng "
        + "semua webview). Konvensi: Is = boolean, Call = async + event, "
        + "Set = diam (gagal via ErrorOccurred), Get = sync. Input fleksibel: "
        + "list ATAU teks (\",\" utama, newline/\";\" ikut diterima).",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    androidMinSdk = 21,
    iconName = "aiwebres/tools_adblock.jpg",
    helpUrl = "https://github.com/VB-7/MiT-File_Manager")
@SimpleObject(external = true)
@UsesPermissions({"android.permission.INTERNET",
    "android.permission.ACCESS_NETWORK_STATE"})
public class Tools_AdBlock extends AndroidNonvisibleComponent {

  private static final String PREFS = "Fv7ToolsAdBlock";
  private static final String K_MODE = "mode";
  private static final String K_LAST = "lastUpdate";
  private static final String K_CUSTOM = "customSources";

  private static final String MODE_MANUAL = "Manual";
  private static final String MODE_TIAPBUKA = "TiapBuka";
  private static final String MODE_HARIAN = "Harian";
  private static final String MODE_MINGGUAN = "Mingguan";
  private static final String MODE_AUTO = "Auto";

  private static final String[] DEFAULT_SOURCES = {
    "https://www.apps2sd.info/idm/filters.txt",
    "https://easylist.to/easylist/easylist.txt",
    "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters.txt",
    "https://filters.adtidy.org/extension/ublock/filters/2_without_easylist.txt",
    "https://easylist.to/easylist/easyprivacy.txt",
    "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=hosts&showintro=1&mimetype=plaintext",
    "https://raw.githubusercontent.com/reek/anti-adblock-killer/master/anti-adblock-killer-filters.txt",
    "https://easylist-downloads.adblockplus.org/antiadblockfilters.txt",
    "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/unbreak.txt",
    "https://filters.adtidy.org/extension/ublock/filters/11.txt",
    "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/badware.txt",
    "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/privacy.txt",
    "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/resource-abuse.txt",
    "https://malware-filter.gitlab.io/malware-filter/urlhaus-filter-online.txt",
    "https://filters.adtidy.org/extension/ublock/filters/14.txt",
    "https://secure.fanboy.co.nz/fanboy-annoyance.txt",
    "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/annoyances.txt",
    "https://secure.fanboy.co.nz/fanboy-cookiemonster.txt",
    "https://badmojr.github.io/1Hosts/Lite/hosts.txt",
    "https://secure.fanboy.co.nz/fanboy-indian.txt",
    "https://filters.adtidy.org/windows/filters/13.txt?id=13",
    "https://raw.githubusercontent.com/ABPindo/indonesianadblockrules/master/subscriptions/abpindo.txt",
    "https://filters.adtidy.org/windows/filters/1.txt?id=1",
    "https://easylist-downloads.adblockplus.org/easylistgermany.txt",
    "https://abp.oisd.nl",
    "https://raw.githubusercontent.com/bogachenko/fuckfuckadblock/master/fuckfuckadblock.txt",
    "https://pgl.yoyo.org/as/serverlist.php?hostformat=nohtml&showintro=0&mimetype=plaintext"
  };

  private static final Object lock = new Object();
  private static final LinkedHashSet<String> hosts = new LinkedHashSet<String>();
  private static final ArrayList<String> netRules = new ArrayList<String>();
  private static final ArrayList<String> whitelist = new ArrayList<String>();
  private static final ArrayList<String> cosmetics = new ArrayList<String>();
  private static boolean enabled = false;
  private static long blockedCount = 0;
  private static long allowedCount = 0;
  private static String updateMode = MODE_HARIAN;
  private static long lastUpdate = 0;
  private static boolean refreshing = false;
  private static Tools_AdBlock last = null;

  private final Context context;
  private final Activity activity;

  public Tools_AdBlock(ComponentContainer container) {
    super(container.$form());
    this.context = container.$context();
    this.activity = container.$context();
    last = this;
    loadPrefs();
    loadCache();
    maybeAutoRefresh();
  }

  // ================= internal: prefs + cache =================

  private void loadPrefs() {
    try {
      SharedPreferences p = context.getSharedPreferences(PREFS,
          Context.MODE_PRIVATE);
      updateMode = normMode(p.getString(K_MODE, MODE_HARIAN));
      lastUpdate = p.getLong(K_LAST, 0);
    } catch (Exception e) {
      updateMode = MODE_HARIAN;
      lastUpdate = 0;
    }
  }

  private void savePrefs() {
    try {
      SharedPreferences p = context.getSharedPreferences(PREFS,
          Context.MODE_PRIVATE);
      p.edit().putString(K_MODE, updateMode)
        .putLong(K_LAST, lastUpdate).apply();
    } catch (Exception e) {
      // abaikan
    }
  }

  private File cacheDir() {
    try {
      File d = new File(context.getFilesDir(), "fv7_adblock");
      if (!d.exists()) {
        d.mkdirs();
      }
      return d;
    } catch (Exception e) {
      return null;
    }
  }

  private void writeCache(String name, List<String> lines) {
    try {
      File d = cacheDir();
      if (d == null) {
        return;
      }
      File f = new File(d, name);
      FileOutputStream out = new FileOutputStream(f);
      try {
        for (int i = 0; i < lines.size(); i = i + 1) {
          String s = lines.get(i);
          if (s == null) {
            continue;
          }
          out.write((s + "\n").getBytes("UTF-8"));
        }
        out.flush();
      } finally {
        try {
          out.close();
        } catch (Exception e) {
          // abaikan
        }
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  private ArrayList<String> readCache(String name) {
    ArrayList<String> out = new ArrayList<String>();
    try {
      File d = cacheDir();
      if (d == null) {
        return out;
      }
      File f = new File(d, name);
      if (!f.exists()) {
        return out;
      }
      FileInputStream in = new FileInputStream(f);
      try {
        byte[] data = readAll(in);
        String t = new String(data, "UTF-8");
        String[] parts = t.split("\n");
        for (int i = 0; i < parts.length; i = i + 1) {
          String s = parts[i].trim();
          if (s.length() > 0) {
            out.add(s);
          }
        }
      } finally {
        try {
          in.close();
        } catch (Exception e) {
          // abaikan
        }
      }
    } catch (Exception e) {
      // abaikan
    }
    return out;
  }

  private void loadCache() {
    synchronized (lock) {
      ArrayList<String> h = readCache("hosts.txt");
      ArrayList<String> r = readCache("rules.txt");
      ArrayList<String> w = readCache("white.txt");
      ArrayList<String> c = readCache("cosmetic.txt");
      if (!h.isEmpty() || !r.isEmpty() || !w.isEmpty() || !c.isEmpty()) {
        hosts.clear();
        for (int i = 0; i < h.size(); i = i + 1) {
          hosts.add(h.get(i));
        }
        netRules.clear();
        netRules.addAll(r);
        whitelist.clear();
        whitelist.addAll(w);
        cosmetics.clear();
        cosmetics.addAll(c);
      }
    }
  }

  private void persistCache() {
    synchronized (lock) {
      ArrayList<String> h = new ArrayList<String>(hosts);
      writeCache("hosts.txt", h);
      writeCache("rules.txt", netRules);
      writeCache("white.txt", whitelist);
      writeCache("cosmetic.txt", cosmetics);
    }
  }

  private void maybeAutoRefresh() {
    try {
      String m = updateMode;
      if (MODE_MANUAL.equals(m)) {
        return;
      }
      long now = System.currentTimeMillis();
      boolean stale = false;
      if (MODE_TIAPBUKA.equals(m)) {
        stale = true;
      } else if (MODE_MINGGUAN.equals(m)) {
        stale = (now - lastUpdate) > 7L * 24L * 3600L * 1000L;
      } else {
        stale = (now - lastUpdate) > 24L * 3600L * 1000L;
      }
      if (stale && !isEmpty()) {
        return;
      }
      if (stale) {
        final Tools_AdBlock me = this;
        AsynchUtil.runAsynchronously(new Runnable() {
          public void run() {
            doRefresh(me, false);
          }
        });
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  private static boolean isEmpty() {
    synchronized (lock) {
      return hosts.isEmpty() && netRules.isEmpty();
    }
  }

  // ================= internal: input fleksibel =================

  private static ArrayList<String> splitFlexible(String s) {
    ArrayList<String> out = new ArrayList<String>();
    if (s == null) {
      return out;
    }
    String t = s.replace("\r\n", "\n").replace("\r", "\n");
    String[] lines = t.split("[\n;,]");
    for (int i = 0; i < lines.length; i = i + 1) {
      String[] parts = lines[i].split(",");
      for (int j = 0; j < parts.length; j = j + 1) {
        String x = parts[j].trim();
        if (x.length() > 0) {
          out.add(x);
        }
      }
    }
    return out;
  }

  private static ArrayList<String> toStrList(Object o) {
    ArrayList<String> out = new ArrayList<String>();
    try {
      if (o instanceof YailList) {
        Object[] arr = ((YailList) o).toArray();
        for (int i = 0; i < arr.length; i = i + 1) {
          if (arr[i] == null) {
            continue;
          }
          ArrayList<String> sub = splitFlexible(arr[i].toString());
          for (int j = 0; j < sub.size(); j = j + 1) {
            out.add(sub.get(j));
          }
        }
      } else if (o instanceof List) {
        List<?> l = (List<?>) o;
        for (int i = 0; i < l.size(); i = i + 1) {
          Object x = l.get(i);
          if (x == null) {
            continue;
          }
          ArrayList<String> sub = splitFlexible(x.toString());
          for (int j = 0; j < sub.size(); j = j + 1) {
            out.add(sub.get(j));
          }
        }
      } else if (o != null) {
        ArrayList<String> sub = splitFlexible(o.toString());
        for (int j = 0; j < sub.size(); j = j + 1) {
          out.add(sub.get(j));
        }
      }
    } catch (Exception e) {
      // abaikan
    }
    return out;
  }

  private static String normMode(String m) {
    String t = m == null ? "" : m.trim().toLowerCase();
    if (t.equals("manual")) {
      return MODE_MANUAL;
    } else if (t.equals("tiapbuka") || t.equals("tiap buka")
        || t.equals("setiap buka") || t.equals("onopen")) {
      return MODE_TIAPBUKA;
    } else if (t.equals("harian") || t.equals("daily") || t.equals("hari")) {
      return MODE_HARIAN;
    } else if (t.equals("mingguan") || t.equals("weekly")
        || t.equals("minggu")) {
      return MODE_MINGGUAN;
    } else if (t.equals("auto") || t.equals("otomatis")
        || t.equals("ikut pembuat")) {
      return MODE_AUTO;
    }
    return MODE_HARIAN;
  }

  private static byte[] readAll(InputStream in) throws Exception {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    byte[] buf = new byte[32768];
    int n;
    while ((n = in.read(buf)) > 0) {
      bos.write(buf, 0, n);
    }
    bos.flush();
    return bos.toByteArray();
  }

  // ================= internal: parse =================

  private static String lowerHost(String h) {
    if (h == null) {
      return "";
    }
    return h.trim().toLowerCase();
  }

  private static boolean looksDomain(String s) {
    if (s == null) {
      return false;
    }
    String t = s.trim().toLowerCase();
    if (t.length() < 4 || t.length() > 253) {
      return false;
    }
    if (t.indexOf(' ') >= 0 || t.indexOf('/') >= 0
        || t.indexOf(':') >= 0 || t.indexOf('@') >= 0) {
      return false;
    }
    int dot = t.lastIndexOf('.');
    if (dot <= 0 || dot == t.length() - 1) {
      return false;
    }
    for (int i = 0; i < t.length(); i = i + 1) {
      char c = t.charAt(i);
      boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
          || c == '.' || c == '-' || c == '_';
      if (!ok) {
        return false;
      }
    }
    return true;
  }

  private static String hostOfUrl(String url) {
    if (url == null) {
      return "";
    }
    String u = url.trim();
    if (u.length() == 0) {
      return "";
    }
    try {
      if (u.indexOf("://") < 0 && looksDomain(u)) {
        return lowerHost(u);
      }
      android.net.Uri uri = android.net.Uri.parse(u);
      String h = uri.getHost();
      if (h != null && h.length() > 0) {
        return lowerHost(h);
      }
    } catch (Exception e) {
      // jatuh ke bawah
    }
    try {
      String t = u.toLowerCase();
      int p = t.indexOf("://");
      if (p >= 0) {
        t = t.substring(p + 3);
      }
      int s = t.indexOf('/');
      if (s >= 0) {
        t = t.substring(0, s);
      }
      int c = t.indexOf(':');
      if (c >= 0) {
        t = t.substring(0, c);
      }
      if (looksDomain(t)) {
        return t;
      }
    } catch (Exception e) {
      // abaikan
    }
    return "";
  }

  private static String domainOfRule(String rule) {
    try {
      String r = rule.trim();
      if (r.startsWith("@@")) {
        r = r.substring(2);
      }
      if (r.startsWith("||")) {
        r = r.substring(2);
        int i = 0;
        while (i < r.length()) {
          char c = r.charAt(i);
          if (c == '^' || c == '/' || c == '|' || c == '$'
              || c == ',' || c == ' ') {
            break;
          }
          i = i + 1;
        }
        String d = r.substring(0, i).toLowerCase();
        if (looksDomain(d) || (d.startsWith("*.") && looksDomain(d.substring(2)))) {
          return d.startsWith("*.") ? d.substring(2) : d;
        }
        return d;
      }
    } catch (Exception e) {
      // abaikan
    }
    return "";
  }

  private static void classifyLine(String line) {
    if (line == null) {
      return;
    }
    String t = line.trim();
    if (t.length() == 0) {
      return;
    }
    if (t.startsWith("!") || t.startsWith("[Adblock")) {
      return;
    }
    if (t.startsWith("#") && !t.startsWith("##")
        && !t.startsWith("#$#") && !t.startsWith("#?#")) {
      return;
    }
    synchronized (lock) {
      if (t.startsWith("@@")) {
        if (!whitelist.contains(t)) {
          whitelist.add(t);
        }
        String d = domainOfRule(t);
        if (d.length() > 0 && !hosts.contains("@@:" + d)) {
          hosts.add("@@:" + d);
        }
        return;
      }
      if (t.indexOf("##") >= 0 || t.indexOf("#$#") >= 0
          || t.indexOf("#?#") >= 0) {
        if (!cosmetics.contains(t)) {
          cosmetics.add(t);
        }
        return;
      }
      if (t.startsWith("||") || t.startsWith("|http")
          || t.startsWith("|ws") || t.indexOf("*") >= 0
          || t.endsWith("^") || t.indexOf("$") >= 0) {
        if (!netRules.contains(t)) {
          netRules.add(t);
        }
        String d = domainOfRule(t);
        if (d.length() > 2 && looksDomain(d) && !hosts.contains(d)) {
          hosts.add(d);
        }
        return;
      }
      if (t.startsWith("0.0.0.0") || t.startsWith("127.0.0.1")
          || t.startsWith("::1") || t.startsWith("0 ")) {
        String[] parts = t.split("\\s+");
        for (int i = parts.length - 1; i >= 0; i = i - 1) {
          String d = lowerHost(parts[i]);
          if (looksDomain(d) && !d.equals("localhost")) {
            if (!hosts.contains(d)) {
              hosts.add(d);
            }
            return;
          }
        }
        return;
      }
      if (looksDomain(t)) {
        String d = lowerHost(t);
        if (!hosts.contains(d)) {
          hosts.add(d);
        }
        return;
      }
      if (t.startsWith("http://") || t.startsWith("https://")) {
        String h = hostOfUrl(t);
        if (h.length() > 0) {
          if (!hosts.contains(h)) {
            hosts.add(h);
          }
        } else if (!netRules.contains(t)) {
          netRules.add(t);
        }
        return;
      }
      if (!netRules.contains(t)) {
        netRules.add(t);
      }
    }
  }

  private static boolean hostBlocked(String host) {
    if (host.length() == 0) {
      return false;
    }
    synchronized (lock) {
      if (hosts.contains(host)) {
        return true;
      }
      for (String h : hosts) {
        if (h.startsWith("@@:")) {
          continue;
        }
        if (host.endsWith("." + h)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean hostWhitelisted(String host) {
    if (host.length() == 0) {
      return false;
    }
    synchronized (lock) {
      if (hosts.contains("@@:" + host)) {
        return true;
      }
      for (String h : hosts) {
        if (h.startsWith("@@:")) {
          String d = h.substring(3);
          if (host.equals(d) || host.endsWith("." + d)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /** Cek gabungan (dipakai Webview tiap request): whitelist dulu, baru blokir. */
  public static boolean shouldBlock(String url) {
    boolean r = decideBlock(url);
    if (r) {
      synchronized (lock) {
        blockedCount = blockedCount + 1;
      }
    } else {
      try {
        String u = url == null ? "" : url.trim();
        if (enabled && (u.startsWith("http://")
            || u.startsWith("https://"))) {
          synchronized (lock) {
            allowedCount = allowedCount + 1;
          }
        }
      } catch (Exception e) {
        // abaikan
      }
    }
    return r;
  }

  private static boolean decideBlock(String url) {
    try {
      if (!enabled) {
        return false;
      }
      if (url == null || url.trim().length() == 0) {
        return false;
      }
      String u = url.trim();
      if (!(u.startsWith("http://") || u.startsWith("https://"))) {
        return false;
      }
      String host = hostOfUrl(u);
      if (host.length() == 0) {
        return false;
      }
      if (hostWhitelisted(host)) {
        return false;
      }
      synchronized (lock) {
        for (int i = 0; i < whitelist.size(); i = i + 1) {
          String w = whitelist.get(i);
          if (w.length() > 2 && matchRule(w.substring(2), u, host)) {
            return false;
          }
        }
        if (hostBlocked(host)) {
          return true;
        }
        for (int i = 0; i < netRules.size(); i = i + 1) {
          if (matchRule(netRules.get(i), u, host)) {
            return true;
          }
        }
      }
    } catch (Exception e) {
      // abaikan = jangan blokir
    }
    return false;
  }

  // ================= jembatan statik (dipakai Tools_Webview) =================

  /** Nyala/mati global dari luar (properti BlockAds Webview). */
  public static void setEnabled(boolean on) {
    enabled = on;
  }

  public static boolean isEnabled() {
    return enabled;
  }

  /** Jumlah host (tanpa marker internal @@:). */
  public static int hostCount() {
    synchronized (lock) {
      int n = 0;
      for (String h : hosts) {
        if (!h.startsWith("@@:")) {
          n = n + 1;
        }
      }
      return n;
    }
  }

  public static int ruleCount() {
    synchronized (lock) {
      return netRules.size();
    }
  }

  public static int whiteCount() {
    synchronized (lock) {
      return whitelist.size();
    }
  }

  /** True bila URL/host ini di-whitelist (dipakai blok IsWhitelisted Webview). */
  public static boolean isWhitelistedUrl(String url) {
    String h = hostOfUrl(url == null ? "" : url);
    return hostWhitelisted(h);
  }

  // ================= scriptlet: popup/antiadblock/overlay/redirect =================

  private static final String[] SCRIPTLET_NAMES = {"popup", "antiadblock",
      "overlay", "redirect", "privacy"};
  private static final LinkedHashSet<String> scriptletsOn =
      new LinkedHashSet<String>();

  static {
    scriptletsOn.add("popup");
    scriptletsOn.add("antiadblock");
    scriptletsOn.add("overlay");
    scriptletsOn.add("redirect");
    scriptletsOn.add("privacy");
  }

  /** Nyalakan/matikan satu scriptlet (nama bebas kapital, tidak dikenal = false). */
  public static boolean setScriptlet(String name, boolean on) {
    try {
      String n = name == null ? "" : name.trim().toLowerCase();
      boolean known = false;
      for (int i = 0; i < SCRIPTLET_NAMES.length; i = i + 1) {
        if (SCRIPTLET_NAMES[i].equals(n)) {
          known = true;
        }
      }
      if (!known) {
        return false;
      }
      synchronized (lock) {
        if (on) {
          scriptletsOn.add(n);
        } else {
          scriptletsOn.remove(n);
        }
      }
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public static boolean isScriptletOn(String name) {
    try {
      String n = name == null ? "" : name.trim().toLowerCase();
      synchronized (lock) {
        return scriptletsOn.contains(n);
      }
    } catch (Exception e) {
      return false;
    }
  }

  private static boolean scOn(String name) {
    synchronized (lock) {
      return scriptletsOn.contains(name);
    }
  }

  /** JS injeksi awal-load (dipanggil Webview di onPageStarted). Kosong = mati. */
  public static String scriptletJsFor(String url) {
    try {
      if (!enabled) {
        return "";
      }
      StringBuilder js = new StringBuilder();
      js.append("(function(){try{");
      if (scOn("popup") || scOn("redirect")) {
        js.append("if(!window.__fv7pop){window.__fv7pop=1;window.__fv7g=0;");
        js.append("['click','touchstart','keydown'].forEach(function(e){");
        js.append("document.addEventListener(e,function(){window.__fv7g=Date.now();},true);});");
        js.append("var __open=window.open;window.open=function(){");
        js.append("try{if(Date.now()-window.__fv7g>1500){return null;}}catch(e){}");
        js.append("try{return __open.apply(window,arguments);}catch(e){return null;}};}");
      }
      if (scOn("antiadblock")) {
        js.append("try{if(typeof window.adsbygoogle==='undefined'){window.adsbygoogle={loaded:true};}");
        js.append("if(typeof window.canRunAds==='undefined'){window.canRunAds=true;}");
        js.append("if(typeof window.fuckAdBlock==='undefined'){window.fuckAdBlock={on:function(){},check:function(){return false;}};}");
        js.append("if(typeof window.BlockAdBlock==='undefined'){window.BlockAdBlock={};}catch(e){}");
      }
      if (scOn("redirect")) {
        js.append("try{var __m=document.querySelectorAll('meta[http-equiv]');");
        js.append("for(var i=0;i<__m.length;i++){try{var h=__m[i].getAttribute('http-equiv');");
        js.append("if(h&&h.toLowerCase()==='refresh'){__m[i].parentNode.removeChild(__m[i]);}}catch(e){}}}catch(e){}");
      }
      if (scOn("overlay")) {
        js.append("if(!window.__fv7ov){window.__fv7ov=1;");
        js.append("var __kill=function(){try{var els=document.querySelectorAll('body div');");
        js.append("for(var i=0;i<els.length;i++){try{var s=window.getComputedStyle(els[i]);");
        js.append("if(s&&(s.position==='fixed'||s.position==='sticky')){");
        js.append("var r=els[i].getBoundingClientRect();");
        js.append("if(r.width>innerWidth*0.8&&r.height>innerHeight*0.5&&parseInt(s.zIndex||'0',10)>=999){");
        js.append("els[i].style.setProperty('display','none','important');}}}}catch(e){}};");
        js.append("try{new MutationObserver(function(){__kill();}).observe(document.documentElement,{childList:true,subtree:true});}catch(e){}");
        js.append("__kill();}");
      }
      if (scOn("privacy")) {
        js.append("try{var __c=window.CanvasRenderingContext2D;");
        js.append("if(__c&&__c.prototype){var __g=__c.prototype.getImageData;");
        js.append("__c.prototype.getImageData=function(){");
        js.append("var d=__g.apply(this,arguments);try{var a=d.data;");
        js.append("for(var i=0;i<a.length;i+=401){a[i]=(a[i]+(i%5))%256;}}");
        js.append("catch(e){}return d;};}}catch(e){}");
        js.append("try{Object.defineProperty(window.navigator,'webdriver',");
        js.append("{get:function(){return false;},configurable:true});}");
        js.append("catch(e){}");
        js.append("try{Object.defineProperty(window.navigator,");
        js.append("'hardwareConcurrency',{get:function(){return 4;},");
        js.append("configurable:true});}catch(e){}");
        js.append("try{Object.defineProperty(window.navigator,'deviceMemory',");
        js.append("{get:function(){return 4;},configurable:true});}");
        js.append("catch(e){}");
      }
      js.append("}catch(e){}}") ;
      String out = js.toString();
      if (out.length() < 60) {
        return "";
      }
      return out;
    } catch (Exception e) {
      return "";
    }
  }

  /** Tulis cache bila ada instance (dipanggil tiap ada perubahan ember). */
  private static void persistIfReady() {
    try {
      Tools_AdBlock m = last;
      if (m != null) {
        m.persistCache();
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  /** Muat cache bila ember kosong (dipakai InitAdBlocker). */
  public static void ensureLoaded() {
    try {
      synchronized (lock) {
        if (!hosts.isEmpty() || !netRules.isEmpty()
            || !whitelist.isEmpty() || !cosmetics.isEmpty()) {
          return;
        }
      }
      Tools_AdBlock m = last;
      if (m != null) {
        m.loadCache();
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  /** Masukkan teks filter campuran (ABP/hosts/whitelist/cosmetic).
   *  Kembali: jumlah entri BARU yang masuk ember. */
  public static int addFilterText(String text) {
    int before = 0;
    int after = 0;
    try {
      synchronized (lock) {
        before = hosts.size() + netRules.size() + whitelist.size()
            + cosmetics.size();
      }
      if (text != null) {
        String t = text.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = t.split("\n");
        for (int i = 0; i < lines.length; i = i + 1) {
          classifyLine(lines[i]);
        }
      }
      synchronized (lock) {
        after = hosts.size() + netRules.size() + whitelist.size()
            + cosmetics.size();
      }
      persistIfReady();
    } catch (Exception e) {
      // abaikan
    }
    return after - before;
  }

  /** Tambah 1 host whitelist (ala Sunny AddToWhitelist). */
  public static boolean addWhiteHost(String host) {
    boolean added = false;
    try {
      String h = lowerHost(host == null ? "" : host);
      if (!looksDomain(h)) {
        return false;
      }
      synchronized (lock) {
        if (!hosts.contains("@@:" + h)) {
          hosts.add("@@:" + h);
          added = true;
        }
        String rule = "@@||" + h + "^";
        if (!whitelist.contains(rule)) {
          whitelist.add(rule);
          added = true;
        }
      }
      if (added) {
        persistIfReady();
      }
    } catch (Exception e) {
      // abaikan
    }
    return added;
  }

  /** Buang 1 host dari whitelist (ala Sunny RemoveFromWhitelist). */
  public static boolean removeWhiteHost(String host) {
    boolean out = false;
    try {
      String h = lowerHost(host == null ? "" : host);
      if (h.length() == 0) {
        return false;
      }
      synchronized (lock) {
        if (hosts.remove("@@:" + h)) {
          out = true;
        }
        for (int i = whitelist.size() - 1; i >= 0; i = i - 1) {
          String w = whitelist.get(i);
          if (w.equals("@@||" + h + "^") || w.equals("@@" + h)
              || w.indexOf(h) >= 0 && w.startsWith("@@")) {
            whitelist.remove(i);
            out = true;
          }
        }
      }
      if (out) {
        persistIfReady();
      }
    } catch (Exception e) {
      // abaikan
    }
    return out;
  }

  /** Buang SEMUA network rule (whitelist + setting tetap). */
  public static void clearRulesOnly() {
    try {
      synchronized (lock) {
        netRules.clear();
      }
      persistIfReady();
    } catch (Exception e) {
      // abaikan
    }
  }

  /** Hapus file cache saja (ember tetap). */
  public static void clearCachesOnly() {
    try {
      Tools_AdBlock m = last;
      if (m == null) {
        return;
      }
      File d = m.cacheDir();
      if (d != null) {
        File[] fs = d.listFiles();
        if (fs != null) {
          for (int i = 0; i < fs.length; i = i + 1) {
            try {
              fs[i].delete();
            } catch (Exception e) {
              // abaikan
            }
          }
        }
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  /** Buang SEMUA hasil + cache (ala Sunny ClearAdBlockerData). */
  public static void clearAllData() {
    try {
      synchronized (lock) {
        hosts.clear();
        netRules.clear();
        whitelist.clear();
        cosmetics.clear();
      }
      persistIfReady();
      clearCachesOnly();
    } catch (Exception e) {
      // abaikan
    }
  }

  private static boolean matchRule(String rule, String url, String host) {
    try {
      String r = rule.trim();
      if (r.length() == 0) {
        return false;
      }
      if (r.startsWith("@@")) {
        r = r.substring(2);
      }
      String low = url.toLowerCase();
      if (r.startsWith("||")) {
        String d = domainOfRule("||" + r.substring(2));
        if (d.length() > 0) {
          if (host.equals(d) || host.endsWith("." + d)) {
            return true;
          }
          return false;
        }
        String tail = r.substring(2).toLowerCase();
        int cut = tail.indexOf('^');
        if (cut >= 0) {
          tail = tail.substring(0, cut);
        }
        cut = tail.indexOf('$');
        if (cut >= 0) {
          tail = tail.substring(0, cut);
        }
        cut = tail.indexOf('/');
        if (cut >= 0) {
          tail = tail.substring(0, cut);
        }
        return tail.length() > 3 && host.indexOf(tail) >= 0;
      }
      if (r.indexOf("##") >= 0 || r.indexOf("#$#") >= 0) {
        return false;
      }
      StringBuilder rx = new StringBuilder();
      for (int i = 0; i < r.length(); i = i + 1) {
        char c = r.charAt(i);
        if (c == '*') {
          rx.append(".*");
        } else if (c == '^') {
          rx.append("[^a-zA-Z0-9_.%-]");
        } else if (c == '|') {
          // jangkar, abaikan di pencocokan sederhana
        } else {
          if ("\\.[]{}()+-?".indexOf(c) >= 0) {
            rx.append('\\');
          }
          rx.append(Character.toLowerCase(c));
        }
      }
      String pat = rx.toString();
      if (pat.length() < 4) {
        return false;
      }
      return low.matches(".*" + pat + ".*");
    } catch (Exception e) {
      return false;
    }
  }

  // ================= download + refresh =================

  private List<String> activeSources() {
    ArrayList<String> out = new ArrayList<String>();
    try {
      SharedPreferences p = context.getSharedPreferences(PREFS,
          Context.MODE_PRIVATE);
      String c = p.getString(K_CUSTOM, "");
      if (c != null && c.trim().length() > 0) {
        ArrayList<String> sub = splitFlexible(c);
        for (int i = 0; i < sub.size(); i = i + 1) {
          out.add(sub.get(i));
        }
      }
    } catch (Exception e) {
      // abaikan
    }
    if (out.isEmpty()) {
      for (int i = 0; i < DEFAULT_SOURCES.length; i = i + 1) {
        out.add(DEFAULT_SOURCES[i]);
      }
    }
    return out;
  }

  private static void doRefresh(final Tools_AdBlock me, boolean fireEvent) {
    boolean ok = false;
    String msg = "";
    int nHost = 0;
    int nRule = 0;
    int nWhite = 0;
    int nCos = 0;
    try {
      synchronized (lock) {
        if (refreshing) {
          if (fireEvent && me != null) {
            Shared.err(me, "AdBlock", 0, "sedang refresh, coba lagi nanti");
          }
          return;
        }
        refreshing = true;
      }
      List<String> src = me.activeSources();
      int done = 0;
      for (int i = 0; i < src.size(); i = i + 1) {
        String u = src.get(i);
        try {
          String txt = downloadText(u);
          if (txt != null) {
            String[] lines = txt.split("\n");
            for (int j = 0; j < lines.length; j = j + 1) {
              classifyLine(lines[j]);
            }
          }
        } catch (Exception e) {
          // satu sumber gagal = lanjut, tidak batalkan semua
        }
        done = done + 1;
        if (me != null) {
          final int fDone = done;
          final int fTotal = src.size();
          try {
            me.form.runOnUiThread(new Runnable() {
              public void run() {
                Shared.fire(me, "AdBlockProgressChanged",
                    Integer.valueOf(fDone), Integer.valueOf(fTotal));
              }
            });
          } catch (Exception e) {
            // abaikan
          }
        }
      }
      synchronized (lock) {
        nHost = hosts.size();
        nRule = netRules.size();
        nWhite = whitelist.size();
        nCos = cosmetics.size();
      }
      me.persistCache();
      synchronized (lock) {
        lastUpdate = System.currentTimeMillis();
      }
      me.savePrefs();
      ok = (nHost + nRule) > 0;
      msg = ok ? "ok" : "hasil kosong (cek koneksi)";
    } catch (Exception e) {
      String m = e.getMessage();
      msg = m == null ? "refresh gagal" : m;
      ok = false;
    } finally {
      synchronized (lock) {
        refreshing = false;
      }
    }
    if (fireEvent && me != null) {
      final boolean fOk = ok;
      final int fH = nHost;
      final int fR = nRule;
      final int fW = nWhite;
      final int fC = nCos;
      final String fM = msg;
      try {
        me.form.runOnUiThread(new Runnable() {
          public void run() {
            Shared.fire(me, "AdBlockUpdated", Boolean.valueOf(fOk),
                Integer.valueOf(fH), Integer.valueOf(fR),
                Integer.valueOf(fW), Integer.valueOf(fC), fM);
          }
        });
      } catch (Exception e) {
        // abaikan
      }
    }
  }

  public static String downloadText(String urlStr) {
    HttpURLConnection c = null;
    try {
      URL url = new URL(urlStr);
      c = (HttpURLConnection) url.openConnection();
      c.setConnectTimeout(20000);
      c.setReadTimeout(30000);
      c.setInstanceFollowRedirects(true);
      c.setRequestProperty("User-Agent", "Mozilla/5.0");
      int code = c.getResponseCode();
      if (code >= 400) {
        return null;
      }
      InputStream in = c.getInputStream();
      try {
        byte[] data = readAll(in);
        if (data.length > 8 * 1024 * 1024) {
          return null;
        }
        return new String(data, "UTF-8");
      } finally {
        try {
          in.close();
        } catch (Exception e) {
          // abaikan
        }
      }
    } catch (Exception e) {
      return null;
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

  // ================= blok: mode + refresh =================

  @SimpleProperty(description = "Mode update (Manual/TiapBuka/Harian/"
      + "Mingguan/Auto).")
  public String AdBlockUpdateMode() {
    return updateMode;
  }

  @SimpleProperty(description = "SET hijau: mode update (Manual / TiapBuka / "
      + "Harian / Mingguan / Auto-ikut-pembuat). Auto = hormati `! Expires:` "
      + "tiap list, fallback Harian. Salah via ErrorOccurred.")
  public void AdBlockUpdateMode(String mode) {
    String t = mode == null ? "" : mode.trim().toLowerCase();
    boolean known = t.equals("manual") || t.equals("tiapbuka")
        || t.equals("tiap buka") || t.equals("setiap buka")
        || t.equals("onopen") || t.equals("harian") || t.equals("daily")
        || t.equals("hari") || t.equals("mingguan") || t.equals("weekly")
        || t.equals("minggu") || t.equals("auto") || t.equals("otomatis")
        || t.equals("ikut pembuat");
    if (!known) {
      Shared.err(this, "AdBlock", 0,
          "mode tidak dikenal: " + (mode == null ? "" : mode));
      return;
    }
    updateMode = normMode(mode);
    savePrefs();
  }

  @SimpleFunction(description = "Mode update sekarang (Manual/TiapBuka/Harian/"
      + "Mingguan/Auto). Sama isi dengan properti AdBlockUpdateMode.")
  public String GetAdBlockUpdateMode() {
    return updateMode;
  }

  @SimpleProperty(description = "True bila mesin AdBlock global sedang nyala. "
      + "Default MATI — set true untuk aktif.")
  public boolean AdBlock() {
    return enabled;
  }

  @SimpleProperty(description = "SET hijau: nyala/mati mesin AdBlock global. "
      + "Mati = shouldBlock selalu false (webview tidak blokir).")
  public void AdBlock(boolean on) {
    enabled = on;
  }

  @SimpleFunction(description = "True bila mesin AdBlock global sedang "
      + "nyala. Sama isi dengan properti AdBlock.")
  public boolean IsAdBlockEnabled() {
    return enabled;
  }

  @SimpleFunction(description = "CALL: download + gabung SEMUA sumber (27 bawaan "
      + "atau custom via SetAdBlockSources) + pisah ke 4 ember. Hasil lewat "
      + "AdBlockUpdated(success, totalHost, totalRule, totalWhite, "
      + "totalCosmetic, message). Progres lewat AdBlockProgressChanged "
      + "(done, total). Satu refresh per saat; sibuk via ErrorOccurred.")
  public void CallRefreshAdBlock() {
    final Tools_AdBlock me = this;
    synchronized (lock) {
      if (refreshing) {
        Shared.err(this, "AdBlock", 0, "sedang refresh, coba lagi nanti");
        return;
      }
    }
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        doRefresh(me, true);
      }
    });
  }

  @SimpleFunction(description = "True bila ada refresh yang sedang jalan.")
  public boolean IsAdBlockRefreshing() {
    synchronized (lock) {
      return refreshing;
    }
  }

  @SimpleProperty(description = "Sumber download aktif (list URL). Bawaan = "
      + "27 URL bila belum di-custom.")
  public List<String> AdBlockSources() {
    return activeSources();
  }

  @SimpleProperty(description = "SET hijau: ganti sumber download (list ATAU "
      + "teks multi-separator \",\"/newline/\";\"). Kosong = kembali ke 27 "
      + "bawaan. Lalu panggil CallRefreshAdBlock.")
  public void AdBlockSources(Object sources) {
    try {
      ArrayList<String> list = toStrList(sources);
      SharedPreferences p = context.getSharedPreferences(PREFS,
          Context.MODE_PRIVATE);
      if (list.isEmpty()) {
        p.edit().putString(K_CUSTOM, "").apply();
      } else {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i = i + 1) {
          if (i > 0) {
            sb.append('\n');
          }
          sb.append(list.get(i));
        }
        p.edit().putString(K_CUSTOM, sb.toString()).apply();
      }
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "AdBlock", 0, m == null ? "sumber gagal" : m);
    }
  }

  @SimpleFunction(description = "Sumber download aktif (list URL). Sama isi "
      + "dengan properti AdBlockSources.")
  public List<String> GetAdBlockSources() {
    return activeSources();
  }

  private static long updateIntervalMs(String m) {
    if (MODE_TIAPBUKA.equals(m)) {
      return 0;
    }
    if (MODE_MINGGUAN.equals(m)) {
      return 7L * 24L * 3600L * 1000L;
    }
    return 24L * 3600L * 1000L;
  }

  private static String ageText(long ms) {
    long s = ms / 1000L;
    if (s < 60L) {
      return s + " dtk";
    }
    long m = s / 60L;
    if (m < 60L) {
      return m + " mnt";
    }
    long h = m / 60L;
    if (h < 48L) {
      return h + " jam";
    }
    return (h / 24L) + " hari";
  }

  @SimpleFunction(description = "Refresh terakhir (ms epoch). 0 = belum "
      + "pernah. Tersimpan permanen (tahan tutup aplikasi).")
  public long GetAdBlockLastUpdate() {
    synchronized (lock) {
      return lastUpdate;
    }
  }

  @SimpleFunction(description = "Sisa ms menuju refresh terjadwal. -1 = "
      + "Manual/TiapBuka (tak terjadwal); 0 = jatuh tempo / belum pernah.")
  public long GetAdBlockNextUpdateIn() {
    synchronized (lock) {
      if (MODE_MANUAL.equals(updateMode)
          || MODE_TIAPBUKA.equals(updateMode)) {
        return -1;
      }
      if (lastUpdate <= 0) {
        return 0;
      }
      long rem = (lastUpdate + updateIntervalMs(updateMode))
          - System.currentTimeMillis();
      return rem < 0 ? 0 : rem;
    }
  }

  @SimpleFunction(description = "Ringkasan jadwal update sebagai teks "
      + "(mode, terakhir, berikutnya). Jadwal ikut tersimpan walau aplikasi "
      + "ditutup.")
  public String GetAdBlockUpdateInfo() {
    synchronized (lock) {
      StringBuilder sb = new StringBuilder();
      sb.append("mode=").append(updateMode).append("; ");
      if (lastUpdate <= 0) {
        sb.append("terakhir=belum pernah; ");
      } else {
        sb.append("terakhir=").append(ageText(
            System.currentTimeMillis() - lastUpdate)).append(" lalu; ");
      }
      if (MODE_MANUAL.equals(updateMode)) {
        sb.append("berikutnya=-");
      } else if (MODE_TIAPBUKA.equals(updateMode)) {
        sb.append("berikutnya=tiap buka");
      } else {
        long rem = (lastUpdate + updateIntervalMs(updateMode))
            - System.currentTimeMillis();
        if (rem <= 0) {
          sb.append("berikutnya=jatuh tempo");
        } else {
          sb.append("berikutnya=").append(ageText(rem)).append(" lagi");
        }
      }
      return sb.toString();
    }
  }

  @SimpleFunction(description = "SET: hapus SEMUA hasil (hosts/rules/whitelist/"
      + "cosmetic + cache file). Tanpa feedback.")
  public void SetClearAdBlock() {
    synchronized (lock) {
      hosts.clear();
      netRules.clear();
      whitelist.clear();
      cosmetics.clear();
    }
    try {
      File d = cacheDir();
      if (d != null) {
        File[] fs = d.listFiles();
        if (fs != null) {
          for (int i = 0; i < fs.length; i = i + 1) {
            try {
              fs[i].delete();
            } catch (Exception e) {
              // abaikan
            }
          }
        }
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  // ================= blok: hosts =================

  @SimpleFunction(description = "SET: tambah host manual (list ATAU teks "
      + "multi-separator). Blok yang normalisasi. Tanpa feedback.")
  public void AddAdBlockHosts(Object hostsIn) {
    ArrayList<String> list = toStrList(hostsIn);
    synchronized (lock) {
      for (int i = 0; i < list.size(); i = i + 1) {
        String d = lowerHost(list.get(i));
        if (looksDomain(d) && !hosts.contains(d)) {
          hosts.add(d);
        }
      }
    }
    persistCache();
  }

  @SimpleFunction(description = "Semua host (URUT MASUK, duplikat dibuang). "
      + "Kosong = belum refresh.")
  public List<String> GetAdBlockHosts() {
    synchronized (lock) {
      ArrayList<String> out = new ArrayList<String>();
      for (String h : hosts) {
        if (!h.startsWith("@@:")) {
          out.add(h);
        }
      }
      return out;
    }
  }

  @SimpleFunction(description = "Host sebagai 1 teks newline (langsung colok "
      + "Sunny.AdHosts + format umum download internet).")
  public String GetAdBlockHostsText() {
    synchronized (lock) {
      StringBuilder sb = new StringBuilder();
      for (String h : hosts) {
        if (h.startsWith("@@:")) {
          continue;
        }
        if (sb.length() > 0) {
          sb.append('\n');
        }
        sb.append(h);
      }
      return sb.toString();
    }
  }

  @SimpleFunction(description = "Jumlah host (tanpa marker internal).")
  public int GetAdBlockHostCount() {
    synchronized (lock) {
      int n = 0;
      for (String h : hosts) {
        if (!h.startsWith("@@:")) {
          n = n + 1;
        }
      }
      return n;
    }
  }

  @SimpleFunction(description = "Host pada nomor urut index (1 = pertama masuk). "
      + "Kosong bila di luar.")
  public String GetAdBlockHostAt(int index) {
    synchronized (lock) {
      int i = 1;
      for (String h : hosts) {
        if (h.startsWith("@@:")) {
          continue;
        }
        if (i == index) {
          return h;
        }
        i = i + 1;
      }
    }
    return "";
  }

  @SimpleFunction(description = "True bila host ini diblokir (cocok persis "
      + "atau subdomain).")
  public boolean IsAdBlockHost(String host) {
    return hostBlocked(lowerHost(host == null ? "" : host));
  }

  // ================= blok: rules =================

  @SimpleFunction(description = "Semua network rule mentah (URUT MASUK).")
  public List<String> GetAdBlockRules() {
    synchronized (lock) {
      return new ArrayList<String>(netRules);
    }
  }

  @SimpleFunction(description = "Rules sebagai 1 teks newline (langsung colok "
      + "Sunny.AddFilterRules).")
  public String GetAdBlockRulesText() {
    synchronized (lock) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < netRules.size(); i = i + 1) {
        if (i > 0) {
          sb.append('\n');
        }
        sb.append(netRules.get(i));
      }
      return sb.toString();
    }
  }

  @SimpleFunction(description = "Total request yang DIBLOKIR sejak nyala.")
  public long GetBlockedRequestsCount() {
    synchronized (lock) {
      return blockedCount;
    }
  }

  @SimpleFunction(description = "Total request yang DIIZINKAN sejak nyala.")
  public long GetAllowedRequestsCount() {
    synchronized (lock) {
      return allowedCount;
    }
  }

  @SimpleFunction(description = "Ringkasan cache: hosts/rules/white/cosmetic "
      + "+ blocked/allowed.")
  public String GetAdBlockerStats() {
    synchronized (lock) {
      return "hosts=" + hosts.size() + " rules=" + netRules.size()
          + " white=" + whitelist.size() + " cos=" + cosmetics.size()
          + " blocked=" + blockedCount + " allowed=" + allowedCount;
    }
  }

  @SimpleFunction(description = "Rincian cache per baris (hosts, rules, "
      + "whitelist, cosmetic, blocked, allowed).")
  public String GetAdBlockerDetailedStats() {
    synchronized (lock) {
      return "hosts: " + hosts.size() + "\n"
          + "rules: " + netRules.size() + "\n"
          + "whitelist: " + whitelist.size() + "\n"
          + "cosmetic: " + cosmetics.size() + "\n"
          + "blocked: " + blockedCount + "\n"
          + "allowed: " + allowedCount;
    }
  }

  @SimpleFunction(description = "Jumlah network rule.")
  public int GetAdBlockRuleCount() {
    synchronized (lock) {
      return netRules.size();
    }
  }

  @SimpleFunction(description = "Rule pada nomor urut index (1 = pertama). "
      + "Kosong bila di luar.")
  public String GetAdBlockRuleAt(int index) {
    synchronized (lock) {
      if (index >= 1 && index <= netRules.size()) {
        return netRules.get(index - 1);
      }
    }
    return "";
  }

  @SimpleFunction(description = "True bila URL ini DIBLOKIR mesin gabungan "
      + "(whitelist dicek dulu). Ini yang dipakai webview tiap request.")
  public boolean IsAdBlocked(String url) {
    return shouldBlock(url == null ? "" : url);
  }

  // ================= blok: whitelist =================

  @SimpleFunction(description = "Semua whitelist/exception mentah (@@...).")
  public List<String> GetAdBlockWhitelist() {
    synchronized (lock) {
      return new ArrayList<String>(whitelist);
    }
  }

  @SimpleFunction(description = "Whitelist sebagai 1 teks newline.")
  public String GetAdBlockWhitelistText() {
    synchronized (lock) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < whitelist.size(); i = i + 1) {
        if (i > 0) {
          sb.append('\n');
        }
        sb.append(whitelist.get(i));
      }
      return sb.toString();
    }
  }

  @SimpleFunction(description = "Jumlah whitelist.")
  public int GetAdBlockWhitelistCount() {
    synchronized (lock) {
      return whitelist.size();
    }
  }

  @SimpleFunction(description = "True bila URL/host ini di-whitelist (@@).")
  public boolean IsWhitelisted(String url) {
    String h = hostOfUrl(url == null ? "" : url);
    return hostWhitelisted(h);
  }

  // ================= blok: cosmetic =================

  @SimpleFunction(description = "Semua cosmetic selector mentah (domain##sel). "
      + "Dipakai webview injeksi JS saat PageLoaded.")
  public List<String> GetAdBlockCosmetics() {
    synchronized (lock) {
      return new ArrayList<String>(cosmetics);
    }
  }

  @SimpleFunction(description = "Cosmetic sebagai 1 teks newline.")
  public String GetAdBlockCosmeticsText() {
    synchronized (lock) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < cosmetics.size(); i = i + 1) {
        if (i > 0) {
          sb.append('\n');
        }
        sb.append(cosmetics.get(i));
      }
      return sb.toString();
    }
  }

  @SimpleFunction(description = "Jumlah cosmetic selector.")
  public int GetAdBlockCosmeticCount() {
    synchronized (lock) {
      return cosmetics.size();
    }
  }

  /** Cosmetic JS untuk satu host (dipanggil Tools_Webview saat PageLoaded). */
  public static String cosmeticJsFor(String url) {
    try {
      String host = hostOfUrl(url == null ? "" : url);
      if (host.length() == 0) {
        return "";
      }
      ArrayList<String> sels = new ArrayList<String>();
      synchronized (lock) {
        for (int i = 0; i < cosmetics.size(); i = i + 1) {
          String r = cosmetics.get(i);
          int p = r.indexOf("##");
          if (p <= 0) {
            continue;
          }
          String dom = r.substring(0, p).trim().toLowerCase();
          String sel = r.substring(p + 2).trim();
          if (sel.length() == 0) {
            continue;
          }
          if (dom.length() == 0 || host.equals(dom)
              || host.endsWith("." + dom)) {
            sels.add(sel);
          }
        }
      }
      if (sels.isEmpty()) {
        return "";
      }
      StringBuilder js = new StringBuilder();
      js.append("(function(){try{var s=[");
      for (int i = 0; i < sels.size(); i = i + 1) {
        if (i > 0) {
          js.append(",");
        }
        js.append("\"");
        String s = sels.get(i).replace("\\", "\\\\").replace("\"", "\\\"");
        js.append(s);
        js.append("\"");
        if (i >= 200) {
          break;
        }
      }
      js.append("];for(var i=0;i<s.length;i++){try{var els=document.querySelectorAll(s[i]);for(var j=0;j<els.length;j++){els[j].style.setProperty('display','none','important');}}catch(e){}}return s.length;}catch(e){return 0}}") ;
      return js.toString();
    } catch (Exception e) {
      return "";
    }
  }

  // ================= blok: scriptlet =================

  @SimpleFunction(description = "SET: nyala/mati satu scriptlet global (nama: "
      + "popup, antiadblock, overlay, redirect, privacy. bebas kapital). "
      + "popup = cegat window.open tanpa gestur; antiadblock = jinakkan "
      + "detektor generik; overlay = sapu div fixed raksasa; redirect = "
      + "cabut meta-refresh; privacy = acak sidik kanvas + samarkan "
      + "navigator (ala Brave Shields). Disuntik tiap halaman mulai dimuat. "
      + "Nama tidak dikenal via ErrorOccurred.")
  public void SetScriptletEnabled(String name, boolean on) {
    if (!setScriptlet(name, on)) {
      Shared.err(this, "AdBlock", 0,
          "scriptlet tidak dikenal: " + (name == null ? "" : name));
    }
  }

  @SimpleFunction(description = "True bila scriptlet ini sedang nyala "
      + "(popup/antiadblock/overlay/redirect).")
  public boolean IsScriptletEnabled(String name) {
    return isScriptletOn(name);
  }

  @SimpleFunction(description = "Daftar nama scriptlet yang tersedia "
      + "(popup, antiadblock, overlay, redirect). URUT TETAP.")
  public List<String> GetScriptletNames() {
    ArrayList<String> out = new ArrayList<String>();
    for (int i = 0; i < SCRIPTLET_NAMES.length; i = i + 1) {
      out.add(SCRIPTLET_NAMES[i]);
    }
    return out;
  }

  // ================= events =================

  @SimpleEvent(description = "Refresh selesai: success + total tiap ember "
      + "(host, rule, white, cosmetic) + message. Var sudah tersaring, "
      + "tinggal colok (adhost/rules/whitelist/cosmetic).")
  public void AdBlockUpdated(boolean success, int totalHost, int totalRule,
      int totalWhite, int totalCosmetic, String message) {
    Shared.fire(this, "AdBlockUpdated", Boolean.valueOf(success),
        Integer.valueOf(totalHost), Integer.valueOf(totalRule),
        Integer.valueOf(totalWhite), Integer.valueOf(totalCosmetic), message);
  }

  @SimpleEvent(description = "Progres refresh: done/total sumber (1-based).")
  public void AdBlockProgressChanged(int done, int total) {
    Shared.fire(this, "AdBlockProgressChanged",
        Integer.valueOf(done), Integer.valueOf(total));
  }

  @SimpleEvent(description = "SATU-SATUNYA error Tools_AdBlock. component = "
      + "nama operasi (bandingkan blok OpAdBlock). id selalu 0 (global).")
  public void ErrorOccurred(String component, int id, String message) {
    Shared.fire(this, "ErrorOccurred", component,
        Integer.valueOf(id), message);
  }

  @SimpleFunction(description = "Fallback polling error terakhir (kosong bila "
      + "belum ada). Isi operation dengan OpAdBlock.")
  public String LastError(String operation) {
    return Shared.lastError(operation);
  }

  @SimpleProperty(description = "Nama operasi AdBlock (untuk if di "
      + "ErrorOccurred).", category = PropertyCategory.BEHAVIOR)
  public String OpAdBlock() {
    return "AdBlock";
  }
}
