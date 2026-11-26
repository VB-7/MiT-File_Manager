package com.fv7.tools;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.HttpAuthHandler;
import android.webkit.JavascriptInterface;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.URLUtil;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebHistoryItem;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.pm.ActivityInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.net.http.SslCertificate;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Base64;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.common.PropertyTypeConstants;
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
import com.google.appinventor.components.runtime.OnDestroyListener;
import com.google.appinventor.components.runtime.util.YailList;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.JsonUtil;
import com.google.appinventor.components.runtime.util.YailDictionary;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@DesignerComponent(
    version = 7,
    description = "Tools_Webview: webview ber-id ala Sunny Gupta v13 (blueprint "
        + "dicuri + disempurnakan, bukan fork). ID NUMBER dua arah (masuk/keluar "
        + "number, tanpa +0), simpan URUT PEMBUATAN + Current/Previous/First/"
        + "Last/List. Alias + properti + event Sunny v13 dipulihkan penuh "
        + "(current-based tetap current-based). WebViewClient/WebChromeClient, "
        + "JS bridge AppInventor, upload file, download event, izin, "
        + "geolokasi, dialog JS, SSL, HTTP-auth, deep-link, asset MIT. "
        + "AdBlock OTOMATIS via Tools_AdBlock (network + whitelist + "
        + "cosmetic injeksi + scriptlet popup/antiadblock/overlay/redirect). "
        + "Input fleksibel: list ATAU teks. Konvensi: Is = "
        + "boolean, Call = async + event, Set = diam (gagal via ErrorOccurred), "
        + "Get = sync.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    androidMinSdk = 21,
    iconName = "aiwebres/tools_webview.jpg",
    helpUrl = "https://github.com/VB-7/MiT-File_Manager")
@SimpleObject(external = true)
@UsesPermissions({"android.permission.INTERNET",
    "android.permission.ACCESS_FINE_LOCATION",
    "android.permission.ACCESS_COARSE_LOCATION",
    "android.permission.CAMERA",
    "android.permission.RECORD_AUDIO",
    "android.permission.MODIFY_AUDIO_SETTINGS",
    "android.permission.VIBRATE",
    "android.permission.WRITE_EXTERNAL_STORAGE"})
public class Tools_Webview extends AndroidNonvisibleComponent
    implements OnDestroyListener {

  private static final class Holder {
    WebView view;
    ValueCallback<Uri[]> fileCb;
    PermissionRequest permReq;
    GeolocationPermissions.Callback geoCb;
    String geoOrigin = "";
    SslErrorHandler sslHandler;
    HttpAuthHandler authHandler;
    JsResult jsAlert;
    JsResult jsConfirm;
    JsPromptResult jsPrompt;
    Message newWindowMsg;
    Message reSendMsg;
    Message dontSendMsg;
    boolean adBlock = true;
    String webViewString = "";
  }

  private static final Map<Integer, Holder> holders =
      new HashMap<Integer, Holder>();
  private static final Object viewLock = new Object();
  private static String mobileUa = "";

  // ---- konfigurasi global ala Sunny (berlaku semua webview) ----
  private static boolean followLinksOn = true;
  private static boolean deepLinksOn = false;
  private static boolean promptForPermission = true;
  private static boolean vibrationOn = true;
  private static boolean scrollEnabledGlobal = true;
  private static boolean supportMultipleDef = false;
  private static int newWindowApproach = 2; // 1 = samakan, 2 = event
  private static boolean zoomEnabledDef = true;
  private static int zoomPercentDef = 100;
  private static boolean desktopDefault = false;
  private static String userAgentOverride = "";
  private static String screenName = "";
  private static final ArrayList<String> customDeepSchemes =
      new ArrayList<String>();
  private static final HashMap<String, String> customHeaders =
      new HashMap<String, String>();
  private static final ArrayList<String> headerDomains =
      new ArrayList<String>();
  private static String lastMediaUrl = "";
  private static long lastMediaTime = 0;
  private static String blobBase64 = "";
  private static String blobMime = "";
  private static boolean blobPending = false;
  private static String blobFileName = "";
  private static String blobDir = "";

  private final Context context;
  private final Activity activity;

  public Tools_Webview(ComponentContainer container) {
    super(container.$form());
    this.context = container.$context();
    this.activity = container.$context();
  }

  // ================= internal =================

  private ViewGroup groupOf(AndroidViewComponent container) {
    if (container == null) {
      return null;
    }
    try {
      View v = container.getView();
      if (v instanceof ViewGroup) {
        return (ViewGroup) v;
      }
    } catch (Exception e) {
      // abaikan
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

  private int mitToAndroidSize(int s) {
    if (s == -2) {
      return ViewGroup.LayoutParams.MATCH_PARENT;
    }
    if (s == -1 || s <= -1000) {
      return ViewGroup.LayoutParams.WRAP_CONTENT;
    }
    if (s > 0) {
      return s;
    }
    return ViewGroup.LayoutParams.WRAP_CONTENT;
  }

  private int androidToMitSize(int s) {
    if (s == ViewGroup.LayoutParams.MATCH_PARENT) {
      return -2;
    }
    if (s == ViewGroup.LayoutParams.WRAP_CONTENT) {
      return -1;
    }
    return s;
  }

  private Holder holderOf(int id) {
    synchronized (viewLock) {
      return holders.get(Integer.valueOf(id));
    }
  }

  private WebView viewOf(int id) {
    Holder h = holderOf(id);
    return h == null ? null : h.view;
  }

  private void setupDefaults(final WebView w, final int id) {
    w.addJavascriptInterface(new JsBridge(id), "AppInventor");
    try {
      if (mobileUa.length() == 0) {
        String d = w.getSettings().getUserAgentString();
        mobileUa = d == null ? "" : d;
      }
    } catch (Exception e) {
      mobileUa = "";
    }
    WebSettings s = w.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setDatabaseEnabled(true);
    s.setLoadWithOverviewMode(true);
    s.setUseWideViewPort(true);
    s.setBuiltInZoomControls(zoomEnabledDef);
    s.setDisplayZoomControls(false); // UI +/- tidak pernah ditampilkan
    s.setSupportZoom(zoomEnabledDef);
    s.setTextZoom(zoomPercentDef);
    s.setAllowFileAccess(true);
    s.setAllowFileAccessFromFileURLs(true);
    s.setAllowUniversalAccessFromFileURLs(true);
    s.setAllowContentAccess(true);
    s.setMediaPlaybackRequiresUserGesture(false);
    s.setJavaScriptCanOpenWindowsAutomatically(true);
    s.setSupportMultipleWindows(supportMultipleDef);
    s.setGeolocationEnabled(true);
    s.setCacheMode(WebSettings.LOAD_DEFAULT);
    s.setDefaultFontSize(16);
    s.setBlockNetworkImage(false);
    s.setLoadsImagesAutomatically(true);
    s.setBlockNetworkLoads(false);
    try {
      String ua = userAgentOverride.length() > 0 ? userAgentOverride
          : (mobileUa.length() > 0 ? mobileUa : s.getUserAgentString());
      if (ua == null) {
        ua = "";
      }
      if (desktopDefault && ua.length() > 0) {
        ua = ua.replace("Android", "diordnA").replace("Mobile", "eliboM");
      }
      if (ua.length() > 0) {
        s.setUserAgentString(ua);
      }
    } catch (Exception e) {
      // abaikan
    }
    try {
      CookieManager.getInstance().setAcceptThirdPartyCookies(w, true);
    } catch (Exception e) {
      // abaikan
    }
    w.setFocusable(true);
    w.setLongClickable(false);
    try {
      w.setHapticFeedbackEnabled(vibrationOn);
    } catch (Exception e) {
      // abaikan
    }
    w.setVerticalScrollBarEnabled(true);
    w.setHorizontalScrollBarEnabled(true);
    w.setWebViewClient(new WClient());
    w.setWebChromeClient(new WChrome());
    try {
      w.setFindListener(new WebView.FindListener() {
        public void onFindResultReceived(int active, int count,
            boolean done) {
          FindResultReceived(id, active, count, done);
        }
      });
    } catch (Exception e) {
      // abaikan
    }
    w.setOnTouchListener(new View.OnTouchListener() {
      public boolean onTouch(View v, MotionEvent ev) {
        try {
          if (!scrollEnabledGlobal) {
            return ev != null
                && ev.getAction() == MotionEvent.ACTION_MOVE;
          }
          if (ev != null && (ev.getAction() == MotionEvent.ACTION_DOWN
              || ev.getAction() == MotionEvent.ACTION_UP)) {
            if (!v.hasFocus()) {
              v.requestFocus();
            }
          }
        } catch (Exception e) {
          // abaikan
        }
        return false;
      }
    });
    w.setOnLongClickListener(new View.OnLongClickListener() {
      public boolean onLongClick(View v) {
        try {
          WebView vw = (WebView) v;
          if (!vw.isLongClickable()) {
            return true;
          }
          WebView.HitTestResult hr = vw.getHitTestResult();
          if (hr == null) {
            return false;
          }
          String item = hr.getExtra();
          int type = hr.getType();
          if (type == WebView.HitTestResult.UNKNOWN_TYPE) {
            return false;
          }
          if (item == null) {
            item = "";
          }
          String second = "";
          if (type == 8) {
            try {
              Message m = new Handler().obtainMessage();
              vw.requestFocusNodeHref(m);
              Object u = m.getData().get("url");
              if (u != null) {
                second = u.toString();
              }
            } catch (Exception e) {
              second = "";
            }
          }
          if (vibrationOn) {
            try {
              Vibrator vb = (Vibrator) context.getSystemService(
                  Context.VIBRATOR_SERVICE);
              if (vb != null) {
                vb.vibrate(50);
              }
            } catch (Exception e) {
              // abaikan
            }
          }
          LongClicked(id, item, second, type);
          return false;
        } catch (Exception e) {
          return false;
        }
      }
    });
    w.setDownloadListener(new DownloadListener() {
      public void onDownloadStart(String url, String userAgent,
          String contentDisposition, String mimeType, long length) {
        int wid = holderId(w);
        String name = "";
        try {
          name = URLUtil.guessFileName(url, contentDisposition,
              mimeType);
        } catch (Exception e) {
          name = "";
        }
        if (name == null) {
          name = "";
        }
        String u = url == null ? "" : url;
        String mt = mimeType == null ? "" : mimeType;
        String cd = contentDisposition == null ? "" : contentDisposition;
        int sz = length > 2147483647L ? 2147483647 : (int) length;
        DownloadNeeded(wid, u, mt, name);
        OnDownloadNeeded(wid, u, cd, mt, sz);
      }
    });
  }

  private int holderId(WebView w) {
    synchronized (viewLock) {
      for (Map.Entry<Integer, Holder> en : holders.entrySet()) {
        if (en.getValue() != null && en.getValue().view == w) {
          return en.getKey().intValue();
        }
      }
    }
    return -1;
  }

  private int curId() {
    return Ids.currentOf(Ids.WEBVIEW);
  }

  private Holder curHolder() {
    return holderOf(curId());
  }

  private WebView curView() {
    return viewOf(curId());
  }

  private int d2p(int px) {
    try {
      float d = form.deviceDensity();
      if (d <= 0) {
        return px;
      }
      return Math.round(px / d);
    } catch (Exception e) {
      return px;
    }
  }

  private int p2d(int dp) {
    try {
      float d = form.deviceDensity();
      if (d <= 0) {
        return dp;
      }
      return Math.round(dp * d);
    } catch (Exception e) {
      return dp;
    }
  }

  private static ArrayList<String> splitFlex(String s) {
    ArrayList<String> out = new ArrayList<String>();
    if (s == null) {
      return out;
    }
    try {
      String t = s.replace("\r\n", "\n").replace("\r", "\n");
      String[] lines = t.split("[\n;]");
      for (int i = 0; i < lines.length; i = i + 1) {
        String[] parts = lines[i].split(",");
        for (int j = 0; j < parts.length; j = j + 1) {
          String x = parts[j].trim();
          if (x.length() > 0) {
            out.add(x);
          }
        }
      }
    } catch (Exception e) {
      // abaikan
    }
    return out;
  }

  /** Input fleksibel: list ATAU teks ("," utama, newline/";" ikut). */
  private static ArrayList<String> strListOf(Object o) {
    ArrayList<String> out = new ArrayList<String>();
    try {
      if (o instanceof YailList) {
        Object[] arr = ((YailList) o).toArray();
        for (int i = 0; i < arr.length; i = i + 1) {
          if (arr[i] == null) {
            continue;
          }
          ArrayList<String> sub = splitFlex(arr[i].toString());
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
          ArrayList<String> sub = splitFlex(x.toString());
          for (int j = 0; j < sub.size(); j = j + 1) {
            out.add(sub.get(j));
          }
        }
      } else if (o != null) {
        ArrayList<String> sub = splitFlex(o.toString());
        for (int j = 0; j < sub.size(); j = j + 1) {
          out.add(sub.get(j));
        }
      }
    } catch (Exception e) {
      // abaikan
    }
    return out;
  }

  /** Header dari YailDictionary ATAU teks "Key: Value" per baris/koma. */
  private static HashMap<String, String> headersMapOf(Object o) {
    HashMap<String, String> m = new HashMap<String, String>();
    try {
      if (o instanceof YailDictionary) {
        YailDictionary d = (YailDictionary) o;
        for (Object k : d.keySet()) {
          if (k == null) {
            continue;
          }
          Object v = null;
          try {
            v = d.get(k);
          } catch (Exception e) {
            v = null;
          }
          m.put(k.toString(), v == null ? "" : v.toString());
        }
      } else if (o != null) {
        ArrayList<String> lines = splitFlex(o.toString());
        for (int i = 0; i < lines.size(); i = i + 1) {
          String ln = lines.get(i);
          int c = ln.indexOf(':');
          if (c > 0) {
            m.put(ln.substring(0, c).trim(),
                ln.substring(c + 1).trim());
          }
        }
      }
    } catch (Exception e) {
      // abaikan
    }
    return m;
  }

  /** True bila customHeaders boleh dipakai untuk url ini (filter domain). */
  private static boolean headersApplyTo(String url) {
    try {
      if (customHeaders.isEmpty()) {
        return false;
      }
      if (headerDomains.isEmpty()) {
        return true;
      }
      String u = url == null ? "" : url.trim().toLowerCase();
      int p = u.indexOf("://");
      if (p >= 0) {
        u = u.substring(p + 3);
      }
      int s = u.indexOf('/');
      if (s >= 0) {
        u = u.substring(0, s);
      }
      int c = u.indexOf(':');
      if (c >= 0) {
        u = u.substring(0, c);
      }
      for (int i = 0; i < headerDomains.size(); i = i + 1) {
        String d = headerDomains.get(i).trim().toLowerCase();
        if (d.length() == 0) {
          continue;
        }
        if (u.equals(d) || u.endsWith("." + d)) {
          return true;
        }
      }
    } catch (Exception e) {
      // abaikan
    }
    return false;
  }

  /** loadUrl biasa, tapi sertakan customHeaders bila domain cocok. */
  private static void loadUrlWithHeaders(WebView w, String url) {
    if (w == null || url == null) {
      return;
    }
    try {
      if (headersApplyTo(url)) {
        w.loadUrl(url, new HashMap<String, String>(customHeaders));
      } else {
        w.loadUrl(url);
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  /** Parser deep-link ala Sunny (tel/mailto/sms/whatsapp/geo/intent/custom). */
  private boolean deepLinkParse(String url) {
    try {
      if (url == null) {
        return false;
      }
      if (url.startsWith("tel:")) {
        activity.startActivity(new Intent(Intent.ACTION_DIAL,
            Uri.parse(url)));
        return true;
      } else if (url.startsWith("mailto:") || url.startsWith("sms:")) {
        activity.startActivity(new Intent(Intent.ACTION_VIEW,
            Uri.parse(url)));
        return true;
      } else if (url.startsWith("whatsapp:")) {
        Intent it = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        it.setPackage("com.whatsapp");
        activity.startActivity(it);
        return true;
      } else if (url.startsWith("geo:")) {
        Intent it = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        it.setPackage("com.google.android.apps.maps");
        if (it.resolveActivity(context.getPackageManager()) != null) {
          activity.startActivity(it);
          return true;
        }
        return false;
      } else if (url.startsWith("intent:")) {
        try {
          Intent it = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
          if (it.resolveActivity(context.getPackageManager()) != null) {
            activity.startActivity(it);
            return true;
          }
          String fb = it.getStringExtra("browser_fallback_url");
          if (fb != null && fb.length() > 0) {
            WebView cv = curView();
            if (cv != null) {
              loadUrlWithHeaders(cv, fb);
            }
          }
          if (it.getPackage() != null) {
            Intent mk = new Intent(Intent.ACTION_VIEW, Uri.parse(
                "market://details?id=" + it.getPackage()));
            if (mk.resolveActivity(context.getPackageManager()) != null) {
              activity.startActivity(mk);
              return true;
            }
          }
          return false;
        } catch (Exception e) {
          return false;
        }
      } else if (!customDeepSchemes.isEmpty()) {
        int c = url.indexOf(':');
        if (c > 0 && customDeepSchemes.contains(url.substring(0, c))) {
          activity.startActivity(new Intent(Intent.ACTION_VIEW,
              Uri.parse(url)));
          return true;
        }
      }
    } catch (Exception e) {
      return false;
    }
    return false;
  }

  /** WebView + fling (Swiped) + scroll-event + kunci scroll ala Sunny. */
  private final class SWView extends WebView {
    final int wid;
    final GestureDetector gd;

    SWView(Context ctx, int id) {
      super(ctx);
      wid = id;
      gd = new GestureDetector(ctx,
          new GestureDetector.SimpleOnGestureListener() {
            public boolean onDown(MotionEvent e) {
              return false;
            }
            public boolean onFling(MotionEvent e1, MotionEvent e2,
                float velocityX, float velocityY) {
              try {
                if (e1 == null || e2 == null) {
                  return false;
                }
                float dx = Math.abs(e1.getRawX() - e2.getRawX());
                float dy = Math.abs(e1.getRawY() - e2.getRawY());
                if (dx > 200 && dy < 90 && Math.abs(velocityX) > 350) {
                  Swiped(wid, e1.getRawX() > e2.getRawX() ? 2 : 1);
                }
              } catch (Exception e) {
                // abaikan
              }
              return false;
            }
          });
    }

    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
      super.onScrollChanged(l, t, oldl, oldt);
      try {
        OnScrollChanged(wid, d2p(l), d2p(t), d2p(oldl), d2p(oldt),
            canScrollHorizontally(-1), canScrollHorizontally(1));
      } catch (Exception e) {
        // abaikan
      }
    }

    public boolean onTouchEvent(MotionEvent ev) {
      try {
        if (!scrollEnabledGlobal && ev != null
            && ev.getAction() == MotionEvent.ACTION_MOVE) {
          return true;
        }
      } catch (Exception e) {
        // abaikan
      }
      boolean g = false;
      try {
        g = gd.onTouchEvent(ev);
      } catch (Exception e) {
        // abaikan
      }
      boolean s = false;
      try {
        s = super.onTouchEvent(ev);
      } catch (Exception e) {
        // abaikan
      }
      return g || s;
    }
  }

  private void loadSource(WebView w, String source) {
    String src = source == null ? "" : source.trim();
    if (src.length() == 0) {
      return;
    }
    if (src.startsWith("<") || src.startsWith("<!")) {
      w.loadDataWithBaseURL(null, src, "text/html", "UTF-8", null);
      return;
    }
    if (src.startsWith("//")) {
      w.loadUrl("file:///appinventor_asset/" + src.substring(2));
      return;
    }
    if (src.startsWith("file://") || src.startsWith("http://")
        || src.startsWith("https://") || src.startsWith("content://")) {
      if (src.startsWith("http://") || src.startsWith("https://")) {
        loadUrlWithHeaders(w, src);
      } else {
        w.loadUrl(src);
      }
      return;
    }
    if (src.startsWith("data:")) {
      w.loadUrl(src);
      return;
    }
    FileProbe f = FileProbe.of(src);
    if (f != null && f.exists) {
      w.loadUrl("file://" + f.abs);
      return;
    }
    if (src.indexOf('.') >= 0 && src.indexOf(' ') < 0) {
      if (src.startsWith("www.")) {
        loadUrlWithHeaders(w, "https://" + src);
      } else {
        loadUrlWithHeaders(w,
            src.indexOf("://") > 0 ? src : "https://" + src);
      }
      return;
    }
    w.loadDataWithBaseURL(null, src, "text/html", "UTF-8", null);
  }

  private static final class FileProbe {
    String abs;
    boolean exists;
    static FileProbe of(String p) {
      FileProbe r = new FileProbe();
      try {
        java.io.File f;
        if (p.startsWith("/")) {
          f = new java.io.File(p);
        } else {
          f = new java.io.File("/storage/emulated/0/" + p);
        }
        r.abs = f.getAbsolutePath();
        r.exists = f.exists();
      } catch (Exception e) {
        r.abs = p;
        r.exists = false;
      }
      return r;
    }
  }

  public final class JsBridge {
    final int wid;
    JsBridge(int id) {
      wid = id;
    }
    @JavascriptInterface
    public String getWebViewString() {
      Holder h = holderOf(wid);
      return h == null ? "" : h.webViewString;
    }
    @JavascriptInterface
    public void setWebViewString(final String s) {      Holder h = holderOf(wid);
      if (h != null) {
        h.webViewString = s == null ? "" : s;
      }
      final String v = s == null ? "" : s;
      try {
        activity.runOnUiThread(new Runnable() {
          public void run() {
            WebViewStringChanged(wid, v);
          }
        });
      } catch (Exception e) {
        // abaikan
      }
    }
    @JavascriptInterface
    public void submitBlob(final String base64, final String mime) {
      blobBase64 = base64 == null ? "" : base64;
      blobMime = mime == null ? "" : mime;
      if (blobPending) {
        blobPending = false;
        final String b64 = blobBase64;
        final String fn = blobFileName;
        final String dd = blobDir;
        blobFileName = "";
        blobDir = "";
        try {
          form.runOnUiThread(new Runnable() {
            public void run() {
              writeBlobFile(b64, fn, dd);
            }
          });
        } catch (Exception e) {
          // abaikan
        }
      }
    }
  }

  /** Tulis base64 blob ke file lalu tembak BlobFileDownloaded (UI thread). */
  private void writeBlobFile(String b64, String fileName, String dir) {
    String path = "";
    try {
      byte[] data = android.util.Base64.decode(b64,
          android.util.Base64.DEFAULT);
      java.io.File out = resolveDownloadFile(
          fileName == null ? "" : fileName, dir == null ? "" : dir);
      if (out == null) {
        Shared.err(this, "Webview", 0, "folder blob tidak valid");
        return;
      }
      java.io.FileOutputStream fos = new java.io.FileOutputStream(out);
      try {
        fos.write(data);
        fos.flush();
      } finally {
        try {
          fos.close();
        } catch (Exception e) {
          // abaikan
        }
      }
      path = out.getAbsolutePath();
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Webview", 0,
          m == null ? "tulis blob gagal" : m);
      return;
    }
    BlobFileDownloaded(path);
  }

  /** Resolve target file DownloadBlob ("~" = privat app, else publik). */
  private java.io.File resolveDownloadFile(String fileName, String dir) {
    try {
      String fn = fileName == null ? "" : fileName.trim();
      if (fn.length() == 0) {
        fn = "blob.bin";
      }
      String dd = dir == null ? "" : dir.trim();
      if (dd.length() == 0) {
        dd = "Download";
      }
      java.io.File base;
      if (dd.startsWith("~")) {
        String sub = dd.substring(1);
        if (sub.startsWith("/")) {
          sub = sub.substring(1);
        }
        base = context.getExternalFilesDir(
            sub.length() > 0 ? sub : null);
      } else {
        base = Environment.getExternalStoragePublicDirectory(dd);
      }
      if (base == null) {
        return null;
      }
      if (!base.exists()) {
        base.mkdirs();
      }
      return new java.io.File(base, fn);
    } catch (Exception e) {
      return null;
    }
  }

  private final class WClient extends WebViewClient {
    private static final String ASSET_PREFIX = "file:///appinventor_asset/";

    private boolean handleDeep(String url) {
      if (url == null) {
        return false;
      }
      if (url.startsWith("http://") || url.startsWith("https://")) {
        return !followLinksOn;
      }
      if (url.startsWith("file://") || url.startsWith("content://")
          || url.startsWith("data:") || url.startsWith("javascript:")) {
        return false;
      }
      if (deepLinksOn) {
        return deepLinkParse(url);
      }
      return false;
    }

    public boolean shouldOverrideUrlLoading(WebView view, String url) {
      if (blockedTap(view, url)) {
        return true;
      }
      return handleDeep(url);
    }

    public boolean shouldOverrideUrlLoading(WebView view,
        WebResourceRequest request) {
      try {
        String url = request.getUrl().toString();
        if (blockedTap(view, url)) {
          return true;
        }
        return handleDeep(url);
      } catch (Exception e) {
        return false;
      }
    }

    /** Pencegat tautan iklan ala 1DM: link yang ditekan bila cocok filter
        AdBlock DIBATALKAN (return true = tetap di url sekarang, tidak load).
        Statistik ikut tercatat via shouldBlock (GetBlockedRequestsCount). */
    private boolean blockedTap(WebView view, String url) {
      try {
        if (url == null || view == null) {
          return false;
        }
        if (!(url.startsWith("http://") || url.startsWith("https://"))) {
          return false;
        }
        int wid = holderId(view);
        Holder h = holderOf(wid);
        boolean useAb = (h == null ? true : h.adBlock)
            && Tools_AdBlock.isEnabled();
        return useAb && Tools_AdBlock.shouldBlock(url);
      } catch (Exception e) {
        return false;
      }
    }

    public WebResourceResponse shouldInterceptRequest(WebView view,
        String url) {
      sniffMedia(view, url);
      if (url != null && (url.startsWith("http://")
          || url.startsWith("https://"))) {
        int wid = holderId(view);
        Holder h = holderOf(wid);
        boolean useAb = (h == null ? true : h.adBlock)
            && Tools_AdBlock.isEnabled();
        if (useAb && Tools_AdBlock.shouldBlock(url)) {
          return emptyBlock();
        }
      }
      if (url != null && (url.startsWith("http://localhost/")
          || url.startsWith(ASSET_PREFIX))) {
        WebResourceResponse r = handleAppRequest(url);
        if (r != null) {
          return r;
        }
      }
      return super.shouldInterceptRequest(view, url);
    }

    public WebResourceResponse shouldInterceptRequest(WebView view,
        WebResourceRequest request) {
      try {
        String url = request.getUrl().toString();
        sniffMedia(view, url);
        if (url.startsWith("http://") || url.startsWith("https://")) {
          int wid = holderId(view);
          Holder h = holderOf(wid);
          boolean useAb = (h == null ? true : h.adBlock)
            && Tools_AdBlock.isEnabled();
          if (useAb && Tools_AdBlock.shouldBlock(url)) {
            return emptyBlock();
          }
        }
        if (url.startsWith("http://localhost/")
            || url.startsWith(ASSET_PREFIX)) {
          WebResourceResponse r = handleAppRequest(url);
          if (r != null) {
            return r;
          }
        }
      } catch (Exception e) {
        // abaikan
      }
      return super.shouldInterceptRequest(view, request);
    }

    /** Cium URL media (video/audio) ala 1DM video-grabber; dedupe 3 detik. */
    private void sniffMedia(WebView view, String url) {
      try {
        if (url == null || view == null) {
          return;
        }
        String u = url.trim();
        String low = u.toLowerCase();
        if (!(low.startsWith("http://") || low.startsWith("https://"))) {
          return;
        }
        int q = low.indexOf('?');
        String path = q >= 0 ? low.substring(0, q) : low;
        int hsh = path.indexOf('#');
        if (hsh >= 0) {
          path = path.substring(0, hsh);
        }
        boolean media = path.endsWith(".mp4") || path.endsWith(".webm")
            || path.endsWith(".m3u8") || path.endsWith(".mpd")
            || path.endsWith(".mp3") || path.endsWith(".m4a")
            || path.endsWith(".aac") || path.endsWith(".ogg")
            || path.endsWith(".ogv") || path.endsWith(".oga")
            || path.endsWith(".wav") || path.endsWith(".flac")
            || path.endsWith(".mov") || path.endsWith(".m4v")
            || path.endsWith(".avi") || path.endsWith(".flv")
            || path.endsWith(".3gp") || path.endsWith(".mkv")
            || path.endsWith(".ts");
        if (!media) {
          return;
        }
        synchronized (viewLock) {
          long now = System.currentTimeMillis();
          if (u.equals(lastMediaUrl) && (now - lastMediaTime) < 3000) {
            return;
          }
          lastMediaUrl = u;
          lastMediaTime = now;
        }
        final int wid = holderId(view);
        final String fu = u;
        if (wid > 0) {
          try {
            activity.runOnUiThread(new Runnable() {
              public void run() {
                MediaFound(wid, fu);
              }
            });
          } catch (Exception e) {
            // abaikan
          }
        }
      } catch (Exception e) {
        // abaikan
      }
    }

    private WebResourceResponse emptyBlock() {      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          return new WebResourceResponse("text/plain", "utf-8", 200, "OK",
              null, new ByteArrayInputStream(new byte[0]));
        }
        return new WebResourceResponse("text/plain", "utf-8",
            new ByteArrayInputStream(new byte[0]));
      } catch (Exception e) {
        return null;
      }
    }

    private WebResourceResponse handleAppRequest(String url) {
      String prefix = "file:///appinventor_asset/";
      String path;
      if (url.startsWith(prefix)) {
        path = url.substring(prefix.length());
      } else {
        int p = url.indexOf("//localhost/");
        if (p < 0) {
          return null;
        }
        path = url.substring(p + 12);
      }
      InputStream in = null;
      try {
        in = form.openAsset(path);
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Access-Control-Allow-Origin", "*");
        String mime = URLConnection.getFileNameMap().getContentTypeFor(path);
        String enc = "utf-8";
        if (mime == null || (!mime.startsWith("text/")
            && !mime.equals("application/javascript"))) {
          enc = null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          return new WebResourceResponse(mime, enc, 200, "OK", headers, in);
        }
        return new WebResourceResponse(mime, enc, in);
      } catch (Exception e) {
        try {
          if (in != null) {
            in.close();
          }
        } catch (Exception ex) {
          // abaikan
        }
        return null;
      }
    }

    public void onPageStarted(WebView view, String url, Bitmap favicon) {
      int wid = holderId(view);
      if (wid > 0) {
        String u = url == null ? "" : url;
        PageStarted(wid, u);
        try {
          String js = Tools_AdBlock.scriptletJsFor(u);
          if (js.length() > 0 && Build.VERSION.SDK_INT >= 19) {
            view.evaluateJavascript(js, null);
          }
        } catch (Exception e) {
          // abaikan
        }
      }
    }

    public void onPageFinished(WebView view, String url) {
      int wid = holderId(view);
      if (wid > 0) {
        String u = url == null ? "" : url;
        PageLoaded(wid, u);
        try {
          String js = Tools_AdBlock.cosmeticJsFor(u);
          if (js.length() > 0 && Build.VERSION.SDK_INT >= 19) {
            view.evaluateJavascript(js, null);
          }
        } catch (Exception e) {
          // abaikan
        }
      }
    }

    public void onReceivedError(WebView view, int code, String desc,
        String url) {
      int wid = holderId(view);
      if (wid > 0) {
        String m = desc == null ? "" : desc;
        String u = url == null ? "" : url;
        ErrorReceived(wid, m, code, u);
        OnErrorReceived(wid, m, code, u);
      }
    }

    public void onReceivedError(WebView view, WebResourceRequest req,
        WebResourceError err) {
      try {
        if (req.isForMainFrame()) {
          int wid = holderId(view);
          if (wid > 0) {
            String m = err.getDescription().toString();
            int c = err.getErrorCode();
            String u = req.getUrl().toString();
            ErrorReceived(wid, m, c, u);
            OnErrorReceived(wid, m, c, u);
          }
        }
      } catch (Exception e) {
        // abaikan
      }
    }

    public void onReceivedHttpError(WebView view, WebResourceRequest req,
        WebResourceResponse resp) {
      try {
        if (req.isForMainFrame()) {
          int wid = holderId(view);
          if (wid > 0) {
            String m = resp.getReasonPhrase();
            int c = resp.getStatusCode();
            String u = req.getUrl().toString();
            ErrorReceived(wid, m, c, u);
            OnErrorReceived(wid, m, c, u);
          }
        }
      } catch (Exception e) {
        // abaikan
      }
    }

    public void onReceivedSslError(WebView view, SslErrorHandler handler,
        SslError error) {
      int wid = holderId(view);
      Holder h = holderOf(wid);
      if (h != null) {
        h.sslHandler = handler;
      }
      int code = 0;
      try {
        code = error.getPrimaryError();
      } catch (Exception e) {
        code = -1;
      }
      OnSslError(wid, code);
      OnReceivedSslError(code);
    }

    public void onReceivedHttpAuthRequest(WebView view,
        HttpAuthHandler handler, String host, String realm) {
      int wid = holderId(view);
      Holder h = holderOf(wid);
      if (h != null) {
        h.authHandler = handler;
      }
      String hh = host == null ? "" : host;
      String rr = realm == null ? "" : realm;
      OnHttpAuthRequest(wid, hh, rr);
      OnReceivedHttpAuthRequest(wid, hh, rr);
    }

    public boolean onCreateWindow(WebView view, boolean isDialog,
        boolean isUserGesture, Message resultMsg) {
      if (!supportMultipleDef) {
        return false;
      }
      try {
        int wid = holderId(view);
        Holder h = holderOf(wid);
        if (h == null || resultMsg == null) {
          return false;
        }
        if (newWindowApproach == 1) {
          WebView.WebViewTransport t =
              (WebView.WebViewTransport) resultMsg.obj;
          t.setWebView(view);
          resultMsg.sendToTarget();
          return true;
        }
        h.newWindowMsg = resultMsg;
        String u = "";
        try {
          u = view.getUrl() == null ? "" : view.getUrl();
        } catch (Exception e) {
          u = "";
        }
        OnNewWindowRequest(wid, u, isDialog, isUserGesture);
        return true;
      } catch (Exception e) {
        return false;
      }
    }

    public void onCloseWindow(WebView view) {
      int wid = holderId(view);
      if (wid > 0) {
        OnCloseWindowRequest(wid);
      }
    }

    public void onFormResubmission(WebView view, Message dontResend,
        Message resend) {
      int wid = holderId(view);
      Holder h = holderOf(wid);
      if (h != null) {
        h.dontSendMsg = dontResend;
        h.reSendMsg = resend;
      }
      if (wid > 0) {
        OnFormResubmission(wid);
      }
    }
  }

  private final class WChrome extends WebChromeClient {
    private View mCustomView;
    private WebChromeClient.CustomViewCallback mCustomCb;
    private int mOrigOrientation;
    private int mOrigUi;

    public void onProgressChanged(WebView view, int progress) {
      int wid = holderId(view);
      if (wid > 0) {
        ProgressChanged(wid, progress);
        OnProgressChanged(wid, progress);
      }
    }

    public boolean onConsoleMessage(ConsoleMessage msg) {
      try {
        String m = msg.message() == null ? "" : msg.message();
        String src = msg.sourceId() == null ? "" : msg.sourceId();
        int line = msg.lineNumber();
        String lvl = "";
        try {
          lvl = msg.messageLevel().name();
        } catch (Exception e) {
          lvl = "";
        }
        int cur = Ids.currentOf(Ids.WEBVIEW);
        Tools_Webview.this.ConsoleMessage(m, cur, line, src);
        Tools_Webview.this.OnConsoleMessage(m, line, src, lvl);
      } catch (Exception e) {
        // abaikan
      }
      return true;
    }

    public boolean onShowFileChooser(WebView view,
        ValueCallback<Uri[]> cb, FileChooserParams params) {
      int wid = holderId(view);
      Holder h = holderOf(wid);
      if (h != null) {
        if (h.fileCb != null) {
          try {
            h.fileCb.onReceiveValue(null);
          } catch (Exception e) {
            // abaikan
          }
        }
        h.fileCb = cb;
      }
      String mime = "";
      try {
        String[] acc = params.getAcceptTypes();
        if (acc != null && acc.length > 0 && acc[0] != null) {
          mime = acc[0];
        }
      } catch (Exception e) {
        mime = "";
      }
      FileUploadNeeded(wid, mime);
      return true;
    }

    public void onPermissionRequest(PermissionRequest request) {
      if (!promptForPermission) {
        try {
          request.grant(request.getResources());
        } catch (Exception e) {
          // abaikan
        }
        return;
      }
      int cur = Ids.currentOf(Ids.WEBVIEW);
      Holder h = holderOf(cur);
      if (h != null) {
        h.permReq = request;
      }
      ArrayList<String> perms = new ArrayList<String>();
      try {
        String[] rs = request.getResources();
        for (int i = 0; i < rs.length; i = i + 1) {
          perms.add(rs[i]);
        }
      } catch (Exception e) {
        // abaikan
      }
      OnWebPermissionRequest(cur, perms);
      OnPermissionRequest(perms);
    }

    public void onGeolocationPermissionsShowPrompt(String origin,
        GeolocationPermissions.Callback cb) {
      if (!promptForPermission) {
        try {
          cb.invoke(origin == null ? "" : origin, true, true);
        } catch (Exception e) {
          // abaikan
        }
        return;
      }
      int cur = Ids.currentOf(Ids.WEBVIEW);
      Holder h = holderOf(cur);
      if (h != null) {
        h.geoCb = cb;
        h.geoOrigin = origin == null ? "" : origin;
      }
      OnGeolocationRequested(cur, origin == null ? "" : origin);
    }

    public boolean onJsAlert(WebView view, String url, String message,
        JsResult result) {
      int wid = holderId(view);
      Holder h = holderOf(wid);
      if (h != null) {
        h.jsAlert = result;
      }
      OnJsAlert(wid, url == null ? "" : url,
          message == null ? "" : message);
      return true;
    }

    public boolean onJsConfirm(WebView view, String url, String message,
        JsResult result) {
      int wid = holderId(view);
      Holder h = holderOf(wid);
      if (h != null) {
        h.jsConfirm = result;
      }
      OnJsConfirm(wid, url == null ? "" : url,
          message == null ? "" : message);
      return true;
    }

    public boolean onJsPrompt(WebView view, String url, String message,
        String def, JsPromptResult result) {
      int wid = holderId(view);
      Holder h = holderOf(wid);
      if (h != null) {
        h.jsPrompt = result;
      }
      OnJsPrompt(wid, url == null ? "" : url,
          message == null ? "" : message, def == null ? "" : def);
      return true;
    }

    public void onShowCustomView(View v,
        WebChromeClient.CustomViewCallback cb) {
      OnShowCustomView();
      try {
        if (mCustomView != null) {
          onHideCustomView();
          return;
        }
        mCustomView = v;
        mCustomCb = cb;
        mOrigUi = activity.getWindow().getDecorView()
            .getSystemUiVisibility();
        mOrigOrientation = activity.getRequestedOrientation();
        ((FrameLayout) activity.getWindow().getDecorView()).addView(
            mCustomView, new FrameLayout.LayoutParams(-1, -1));
        int full = View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE;
        activity.getWindow().getDecorView().setSystemUiVisibility(full);
        activity.setRequestedOrientation(
            ActivityInfo.SCREEN_ORIENTATION_USER);
      } catch (Exception e) {
        // abaikan
      }
    }

    public void onHideCustomView() {
      OnHideCustomView();
      try {
        if (mCustomView != null) {
          ((FrameLayout) activity.getWindow().getDecorView())
              .removeView(mCustomView);
          mCustomView = null;
        }
        activity.getWindow().getDecorView()
            .setSystemUiVisibility(mOrigUi);
        activity.setRequestedOrientation(mOrigOrientation);
        if (mCustomCb != null) {
          mCustomCb.onCustomViewHidden();
          mCustomCb = null;
        }
        activity.setRequestedOrientation(
            ActivityInfo.SCREEN_ORIENTATION_USER);
      } catch (Exception e) {
        // abaikan
      }
    }
  }

  public static void clearStatic(int id) {
    Holder h;
    synchronized (viewLock) {
      h = holders.remove(Integer.valueOf(id));
    }
    if (h != null) {
      try {
        if (h.fileCb != null) {
          h.fileCb.onReceiveValue(null);
          h.fileCb = null;
        }
      } catch (Exception e) {
        // abaikan
      }
      try {
        WebView v = h.view;
        if (v != null) {
          v.stopLoading();
          v.setWebViewClient(null);
          v.setWebChromeClient(null);
          v.setDownloadListener(null);
          if (v.getParent() instanceof ViewGroup) {
            ((ViewGroup) v.getParent()).removeView(v);
          }
          v.destroy();
        }
      } catch (Exception e) {
        // abaikan
      }
    }
    Ids.setActive(Ids.WEBVIEW, id, false);
    Ids.unregister(Ids.WEBVIEW, id);
  }

  public static void clearByView(View root) {
    if (root == null) {
      return;
    }
    ArrayList<Integer> gone = new ArrayList<Integer>();
    synchronized (viewLock) {
      for (Map.Entry<Integer, Holder> en : holders.entrySet()) {
        try {
          View v = en.getValue() == null ? null : en.getValue().view;
          if (v != null && (v == root || isUnder(v, root))) {
            gone.add(en.getKey());
          }
        } catch (Exception e) {
          // abaikan
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

  // ================= blok: tampil + id =================

  @SimpleFunction(description = "Buat id webview unik baru (Number, siap pakai "
      + "ke CreateWebView). URUT PEMBUATAN.")
  public int CreateWebviewId() {
    int id = Ids.createFor(Ids.WEBVIEW);
    if (id < 1) {
      Shared.err(this, "Webview", 0, "gagal buat id");
    }
    return id;
  }

  @SimpleFunction(description = "SET: hapus SEMUA webview Tools_Webview "
      + "(id ikut dilepas). Menembak WebviewRemoved satu per satu.")
  public void SetClearWebviews() {
    ArrayList<Integer> all;
    synchronized (viewLock) {
      all = new ArrayList<Integer>(holders.keySet());
    }
    for (int i = 0; i < all.size(); i = i + 1) {
      RemoveWebView(all.get(i).intValue());
    }
  }

  @SimpleFunction(description = "SET: hapus webview yang ada di dalam container "
      + "ini saja. Menembak WebviewRemoved satu per satu.")
  public void SetClearWebviewsIn(AndroidViewComponent container) {
    View root = null;
    try {
      if (container != null) {
        root = container.getView();
      }
    } catch (Exception e) {
      root = null;
    }
    if (root == null) {
      Shared.err(this, "Webview", 0, "container tidak valid");
      return;
    }
    ArrayList<Integer> gone = new ArrayList<Integer>();
    synchronized (viewLock) {
      for (Map.Entry<Integer, Holder> en : holders.entrySet()) {
        try {
          View v = en.getValue() == null ? null : en.getValue().view;
          if (v != null && (v == root || isUnder(v, root))) {
            gone.add(en.getKey());
          }
        } catch (Exception e) {
          // abaikan
        }
      }
    }
    for (int i = 0; i < gone.size(); i = i + 1) {
      RemoveWebView(gone.get(i).intValue());
    }
  }

  @SimpleFunction(description = "Semua id webview (URUT PEMBUATAN, index 1 = "
      + "pertama dibuat). Number semua.")
  public List<Integer> GetWebviewIds() {
    return Ids.idsOf(Ids.WEBVIEW);
  }

  @SimpleFunction(description = "Id webview yang aktif (URUT PEMBUATAN).")
  public List<Integer> GetActiveWebviewIds() {
    return Ids.activeIdsOf(Ids.WEBVIEW);
  }

  @SimpleFunction(description = "Id webview terakhir dibuat/ditampil. -1 bila "
      + "kosong. Number.")
  public int CurrentWebviewId() {
    return Ids.currentOf(Ids.WEBVIEW);
  }

  @SimpleFunction(description = "Id index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstWebviewId() {
    return Ids.firstOf(Ids.WEBVIEW);
  }

  @SimpleFunction(description = "Id TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastWebviewId() {
    return Ids.lastOf(Ids.WEBVIEW);
  }

  @SimpleFunction(description = "Id yang dibuat TEPAT SEBELUM id ini. -1 bila "
      + "tidak ada. Number.")
  public int GetPreviousWebviewId(int id) {
    return Ids.previousOf(Ids.WEBVIEW, id);
  }

  @SimpleFunction(description = "True bila id webview ini masih ada. Number.")
  public boolean IsWebviewIdActive(int id) {
    return Ids.isActive(Ids.WEBVIEW, id);
  }

  // ================= blok: sumber + navigasi =================

  @SimpleFunction(description = "CALL: load string HTML mentah ke webview "
      + "ber-id. Tanpa return; selesai via PageLoaded.")
  public void CallLoadHtml(final int id, final String html) {
    final WebView w = viewOf(id);
    if (w == null) {
      Shared.err(this, "Webview", id, "id tidak ada");
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.loadDataWithBaseURL(null, html == null ? "" : html,
                "text/html", "UTF-8", null);
          } catch (Exception e) {
            String m = e.getMessage();
            Shared.err(Tools_Webview.this, "Webview", id,
                m == null ? "gagal load" : m);
          }
        }
      });
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Webview", id, m == null ? "gagal load" : m);
    }
  }

  @SimpleProperty(description = "SET: mundur 1 halaman. Tanpa feedback.")
  public void GoBack(final int id) {
    final WebView w = viewOf(id);
    if (w == null) {
      Shared.err(this, "Webview", id, "id tidak ada");
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            if (w.canGoBack()) {
              w.goBack();
            }
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "SET: maju 1 halaman. Tanpa feedback.")
  public void GoForward(final int id) {
    final WebView w = viewOf(id);
    if (w == null) {
      Shared.err(this, "Webview", id, "id tidak ada");
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            if (w.canGoForward()) {
              w.goForward();
            }
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "SET: muat ulang halaman. Tanpa feedback.")
  public void ReloadWebview(final int id) {
    final Holder h = holderOf(id);
    final WebView w = viewOf(id);
    if (w == null) {
      Shared.err(this, "Webview", id, "id tidak ada");
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            cancelJsOf(h);
            w.reload();
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "SET: hentikan load. Tanpa feedback.")
  public void StopWebview(final int id) {
    final WebView w = viewOf(id);
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.stopLoading();
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "True bila bisa mundur. Number in, boolean out.")
  public boolean IsGoBackAvailable(int id) {
    try {
      WebView w = viewOf(id);
      return w != null && w.canGoBack();
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleFunction(description = "True bila bisa maju.")
  public boolean IsGoForwardAvailable(int id) {
    try {
      WebView w = viewOf(id);
      return w != null && w.canGoForward();
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleFunction(description = "URL sekarang. Kosong bila id tidak ada.")
  public String GetWebviewUrl(int id) {
    try {
      WebView w = viewOf(id);
      if (w == null) {
        return "";
      }
      String u = w.getUrl();
      return u == null ? "" : u;
    } catch (Exception e) {
      return "";
    }
  }

  @SimpleFunction(description = "Judul halaman sekarang. Kosong bila tidak ada.")
  public String GetWebviewTitle(int id) {
    try {
      WebView w = viewOf(id);
      if (w == null) {
        return "";
      }
      String t = w.getTitle();
      return t == null ? "" : t;
    } catch (Exception e) {
      return "";
    }
  }

  @SimpleFunction(description = "Progres load 0-100. -1 bila id tidak ada.")
  public int GetWebviewProgress(int id) {
    try {
      WebView w = viewOf(id);
      if (w == null) {
        return -1;
      }
      return w.getProgress();
    } catch (Exception e) {
      return -1;
    }
  }

  // ================= blok: JS + string =================

  @SimpleFunction(description = "CALL: jalankan JavaScript di webview ber-id. "
      + "Hasil string (JSON-quoted) lewat AfterJavascriptEvaluated(id, "
      + "result).")
  public void CallEvaluateJavascript(final int id, final String script) {
    final WebView w = viewOf(id);
    if (w == null) {
      Shared.err(this, "Webview", id, "id tidak ada");
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            if (Build.VERSION.SDK_INT >= 19) {
              w.evaluateJavascript(script == null ? "" : script,
                  new ValueCallback<String>() {
                    public void onReceiveValue(String s) {
                      AfterJavascriptEvaluated(id, s == null ? "" : s);
                    }
                  });
            } else {
              Shared.err(Tools_Webview.this, "Webview", id,
                  "butuh Android 4.4+");
            }
          } catch (Exception e) {
            String m = e.getMessage();
            Shared.err(Tools_Webview.this, "Webview", id,
                m == null ? "JS gagal" : m);
          }
        }
      });
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Webview", id, m == null ? "JS gagal" : m);
    }
  }

  @SimpleFunction(description = "SET: tulis WebViewString webview ber-id "
      + "(dibaca JS via AppInventor.getWebViewString). Tanpa feedback.")
  public void SetWebviewString(int id, String text) {
    Holder h = holderOf(id);
    if (h == null) {
      Shared.err(this, "Webview", id, "id tidak ada");
      return;
    }
    h.webViewString = text == null ? "" : text;
  }

  @SimpleFunction(description = "Baca WebViewString webview ber-id (ditulis JS "
      + "via AppInventor.setWebViewString).")
  public String GetWebviewString(int id) {
    Holder h = holderOf(id);
    return h == null ? "" : h.webViewString;
  }

  // ================= blok: tampil/ukuran =================

  @SimpleFunction(description = "SET: tampil/sembunyi webview ber-id. false = "
      + "GONE. Tanpa feedback.")
  public void SetWebviewVisible(final int id, final boolean visible) {
    final WebView w = viewOf(id);
    if (w == null) {
      Shared.err(this, "Webview", id, "id tidak ada");
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.setVisibility(visible ? View.VISIBLE : View.GONE);
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "True bila webview ber-id sedang tampil.")
  public boolean IsWebviewVisible(int id) {
    try {
      WebView w = viewOf(id);
      return w != null && w.getVisibility() == View.VISIBLE;
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleFunction(description = "SET: atur lebar+tinggi webview ber-id. Angka "
      + "MIT: -2 isi, -1 otomatis, positif pixel. Tanpa feedback.")
  public void SetWebviewSize(final int id, final int width,
      final int height) {
    final WebView w = viewOf(id);
    if (w == null) {
      Shared.err(this, "Webview", id, "id tidak ada");
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            ViewGroup.LayoutParams lp = w.getLayoutParams();
            if (lp == null) {
              lp = new ViewGroup.LayoutParams(
                  ViewGroup.LayoutParams.MATCH_PARENT,
                  ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            lp.width = mitToAndroidSize(width);
            lp.height = mitToAndroidSize(height);
            w.setLayoutParams(lp);
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "Lebar webview ber-id (angka MIT). -3 bila "
      + "tidak ada.")
  public int GetWebviewWidth(int id) {
    try {
      WebView w = viewOf(id);
      if (w == null || w.getLayoutParams() == null) {
        return -3;
      }
      return androidToMitSize(w.getLayoutParams().width);
    } catch (Exception e) {
      return -3;
    }
  }

  @SimpleFunction(description = "Tinggi webview ber-id (angka MIT). -3 bila "
      + "tidak ada.")
  public int GetWebviewHeight(int id) {
    try {
      WebView w = viewOf(id);
      if (w == null || w.getLayoutParams() == null) {
        return -3;
      }
      return androidToMitSize(w.getLayoutParams().height);
    } catch (Exception e) {
      return -3;
    }
  }

  // ================= blok: setting ringan =================

  @SimpleFunction(description = "SET: nyala/mati JavaScript webview ber-id. "
      + "Tanpa feedback.")
  public void SetWebviewJavaScript(final int id, final boolean enabledNow) {
    final WebView w = viewOf(id);
    if (w == null) {
      Shared.err(this, "Webview", id, "id tidak ada");
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.getSettings().setJavaScriptEnabled(enabledNow);
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "True bila JavaScript webview ber-id nyala.")
  public boolean IsWebviewJavaScript(int id) {
    try {
      WebView w = viewOf(id);
      return w != null && w.getSettings().getJavaScriptEnabled();
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleFunction(description = "SET: mode desktop webview ber-id (tukar "
      + "Android/Mobile di user-agent). Tanpa feedback.")
  public void SetWebviewDesktop(final int id, final boolean desktop) {
    final WebView w = viewOf(id);
    if (w == null) {
      Shared.err(this, "Webview", id, "id tidak ada");
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            String ua = w.getSettings().getUserAgentString();
            if (ua == null || ua.length() == 0) {
              ua = mobileUa;
            }
            if (desktop) {
              ua = ua.replace("Android", "diordnA").replace("Mobile",
                  "eliboM");
            } else {
              ua = ua.replace("diordnA", "Android").replace("eliboM",
                  "Mobile");
            }
            w.getSettings().setUserAgentString(ua);
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "SET: AdBlock webview ber-id (true = ikut "
      + "Tools_AdBlock global). Tanpa feedback.")
  public void SetWebviewAdBlock(int id, boolean useAdBlock) {
    Holder h = holderOf(id);
    if (h == null) {
      Shared.err(this, "Webview", id, "id tidak ada");
      return;
    }
    h.adBlock = useAdBlock;
  }

  @SimpleFunction(description = "True bila AdBlock webview ber-id nyala.")
  public boolean IsWebviewAdBlock(int id) {
    Holder h = holderOf(id);
    return h != null && h.adBlock;
  }

  // ================= blok: upload + dialog + izin =================

  @SimpleFunction(description = "Jawab file chooser webview ber-id. contentUri "
      + "kosong = batal. Dipanggil dari event FileUploadNeeded.")
  public void UploadFile(int id, String contentUri) {
    Holder h = holderOf(id);
    if (h == null) {
      Shared.err(this, "Webview", id, "id tidak ada");
      return;
    }
    try {
      ValueCallback<Uri[]> cb = h.fileCb;
      h.fileCb = null;
      if (cb == null) {
        return;
      }
      if (contentUri == null || contentUri.trim().length() == 0) {
        cb.onReceiveValue(null);
      } else {
        cb.onReceiveValue(new Uri[]{Uri.parse(contentUri.trim())});
      }
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Webview", id, m == null ? "upload gagal" : m);
    }
  }

  @SimpleFunction(description = "Jawab dialog confirm webview ber-id.")
  public void ConfirmJsDialog(int id, boolean confirm) {
    Holder h = holderOf(id);
    if (h == null || h.jsConfirm == null) {
      return;
    }
    try {
      if (confirm) {
        h.jsConfirm.confirm();
      } else {
        h.jsConfirm.cancel();
      }
    } catch (Exception e) {
      // abaikan
    } finally {
      h.jsConfirm = null;
    }
  }

  @SimpleFunction(description = "Jawab dialog prompt webview ber-id dengan teks.")
  public void ContinueJsPrompt(int id, String text) {
    Holder h = holderOf(id);
    if (h == null || h.jsPrompt == null) {
      return;
    }
    try {
      h.jsPrompt.confirm(text == null ? "" : text);
    } catch (Exception e) {
      // abaikan
    } finally {
      h.jsPrompt = null;
    }
  }

  @SimpleProperty(description = "Tutup dialog alert webview ber-id.")
  public void DismissJsAlert(int id) {
    Holder h = holderOf(id);
    if (h == null || h.jsAlert == null) {
      return;
    }
    try {
      h.jsAlert.cancel();
    } catch (Exception e) {
      // abaikan
    } finally {
      h.jsAlert = null;
    }
  }

  @SimpleProperty(description = "Jawab izin web (kamera/mic) webview aktif. "
      + "true = grant, false = deny.")
  public void GrantWebPermission(boolean grant) {
    try {
      int cur = Ids.currentOf(Ids.WEBVIEW);
      Holder h = holderOf(cur);
      if (h == null || h.permReq == null) {
        return;
      }
      if (grant) {
        h.permReq.grant(h.permReq.getResources());
      } else {
        h.permReq.deny();
      }
      h.permReq = null;
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "Jawab geolokasi webview aktif.")
  public void AllowGeolocation(boolean allow, boolean remember) {
    try {
      int cur = Ids.currentOf(Ids.WEBVIEW);
      Holder h = holderOf(cur);
      if (h == null || h.geoCb == null) {
        return;
      }
      h.geoCb.invoke(h.geoOrigin, allow, remember);
      h.geoCb = null;
      h.geoOrigin = "";
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "Jawab SSL error webview ber-id.")
  public void ProceedSslError(int id, boolean proceed) {
    Holder h = holderOf(id);
    if (h == null || h.sslHandler == null) {
      return;
    }
    try {
      if (proceed) {
        h.sslHandler.proceed();
      } else {
        h.sslHandler.cancel();
      }
    } catch (Exception e) {
      // abaikan
    } finally {
      h.sslHandler = null;
    }
  }

  @SimpleFunction(description = "Jawab HTTP-auth webview ber-id. Kosong semua "
      + "= batal.")
  public void ProceedHttpAuth(int id, String user, String pass) {
    Holder h = holderOf(id);
    if (h == null || h.authHandler == null) {
      return;
    }
    try {
      String u = user == null ? "" : user;
      String p = pass == null ? "" : pass;
      if (u.length() == 0 && p.length() == 0) {
        h.authHandler.cancel();
      } else {
        h.authHandler.proceed(u, p);
      }
    } catch (Exception e) {
      // abaikan
    } finally {
      h.authHandler = null;
    }
  }

  // ================= blok Sunny: navigasi + riwayat + current =================

  private void cancelJsOf(Holder h) {
    if (h == null) {
      return;
    }
    try {
      if (h.jsAlert != null) {
        h.jsAlert.cancel();
        h.jsAlert = null;
      }
    } catch (Exception e) {
      // abaikan
    }
    try {
      if (h.jsConfirm != null) {
        h.jsConfirm.cancel();
        h.jsConfirm = null;
      }
    } catch (Exception e) {
      // abaikan
    }
    try {
      if (h.jsPrompt != null) {
        h.jsPrompt.cancel();
        h.jsPrompt = null;
      }
    } catch (Exception e) {
      // abaikan
    }
    try {
      if (h.fileCb != null) {
        h.fileCb.onReceiveValue(null);
        h.fileCb = null;
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "Buat + tampilkan webview kosong ber-id ke layout "
      + "(MATCH_PARENT, about:blank). Id NUMBER (pakai CreateWebviewId). "
      + "Ukuran via SetWebviewSize; muat link via GoToUrl(id, url). "
      + "Sukses menembak WebviewShown; gagal via ErrorOccurred.")
  public void CreateWebView(final AndroidViewComponent container, final int id) {
    if (id < 1) {
      Shared.err(this, "Webview", id, "id harus >= 1 (pakai CreateWebviewId)");
      return;
    }
    if (!Ids.register(Ids.WEBVIEW, id)) {
      Shared.err(this, "Webview", id, "ID " + id + " sedang dipakai webview lain");
      return;
    }
    synchronized (viewLock) {
      Holder old = holders.get(Integer.valueOf(id));
      if (old != null && old.view != null) {
        Shared.err(this, "Webview", id, "ID " + id + " masih tampil "
            + "(Remove dulu)");
        return;
      }
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            ViewGroup vg = groupOf(container);
            if (vg == null) {
              Ids.setActive(Ids.WEBVIEW, id, false);
              Ids.unregister(Ids.WEBVIEW, id);
              Shared.err(Tools_Webview.this, "Webview", id,
                  "layout tidak valid");
              return;
            }
            WebView w = new SWView(context, id);
            Holder h = new Holder();
            h.view = w;
            synchronized (viewLock) {
              holders.put(Integer.valueOf(id), h);
            }
            setupDefaults(w, id);
            detach(w);
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
                mitToAndroidSize(-2), mitToAndroidSize(-2));
            vg.addView(w, lp);
            Ids.setActive(Ids.WEBVIEW, id, true);
            loadSource(w, "about:blank");
            WebviewShown(id, "about:blank");
          } catch (Exception e) {
            String m = e.getMessage();
            Ids.setActive(Ids.WEBVIEW, id, false);
            Ids.unregister(Ids.WEBVIEW, id);
            Shared.err(Tools_Webview.this, "Webview", id,
                m == null ? "gagal tampil" : m);
          }
        }
      });
    } catch (Exception e) {
      String m = e.getMessage();
      Ids.setActive(Ids.WEBVIEW, id, false);
      Ids.unregister(Ids.WEBVIEW, id);
      Shared.err(this, "Webview", id, m == null ? "gagal tampil" : m);
    }
  }

  @SimpleProperty(description = "Id current (dipakai semua blok tanpa id). "
      + "-1 bila kosong.", category = PropertyCategory.BEHAVIOR)
  public int WebView() {
    return curId();
  }

  @SimpleProperty(description = "SET hijau: jadikan webview ber-id sebagai "
      + "current (dipakai semua blok tanpa id). Id tidak ada via "
      + "ErrorOccurred.", category = PropertyCategory.BEHAVIOR)
  public void WebView(final int id) {
    final WebView w = viewOf(id);
    if (w == null) {
      Shared.err(this, "Webview", id, "id tidak ada");
      return;
    }
    Ids.setCurrent(Ids.WEBVIEW, id);
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.setVisibility(View.VISIBLE);
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "Kembalikan objek webview ber-id (untuk "
      + "blok lanjut). Null bila tidak ada.")
  public Object GetWebView(int id) {
    return viewOf(id);
  }

  @SimpleFunction(description = "CALL: muat APA PUN ke webview ber-id: URL "
      + "http(s), www..., //asset, path file, string HTML (<...), atau "
      + "data:/base64. Header custom ikut bila http(s). Selesai via "
      + "PageLoaded; id tidak ada via ErrorOccurred.")
  public void GoToUrl(final int id, String url) {
    final WebView w = viewOf(id);
    if (w == null) {
      Shared.err(this, "Webview", id, "id tidak ada");
      return;
    }
    final Holder h = holderOf(id);
    final String u = url == null ? "" : url;
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            cancelJsOf(h);
            loadSource(w, u);
          } catch (Exception e) {
            String m = e.getMessage();
            Shared.err(Tools_Webview.this, "Webview", id,
                m == null ? "gagal load" : m);
          }
        }
      });
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Webview", id, m == null ? "gagal load" : m);
    }
  }

  @SimpleFunction(description = "CALL: muat URL + header tambahan sekali jalan "
      + "ke webview ber-id (dictionary ATAU teks \"Key: Value\"). Header "
      + "kosong = sama dengan GoToUrl. Selesai via PageLoaded; id tidak ada "
      + "via ErrorOccurred.")
  public void GoToUrlWithHeaders(final int id, String url, Object headers) {
    final WebView w = viewOf(id);
    if (w == null) {
      Shared.err(this, "Webview", id, "id tidak ada");
      return;
    }
    final Holder h = holderOf(id);
    final String u = url == null ? "" : url;
    final HashMap<String, String> m = headersMapOf(headers);
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            cancelJsOf(h);
            if (m.isEmpty()) {
              loadSource(w, u);
            } else {
              w.loadUrl(u, m);
            }
          } catch (Exception e) {
            String e2 = e.getMessage();
            Shared.err(Tools_Webview.this, "Webview", id,
                e2 == null ? "gagal load" : e2);
          }
        }
      });
    } catch (Exception e) {
      String m2 = e.getMessage();
      Shared.err(this, "Webview", id, m2 == null ? "gagal load" : m2);
    }
  }


  @SimpleFunction(description = "CALL: load string HTML ke CURRENT. Selesai "
      + "via PageLoaded.")
  public void LoadHtml(String html) {
    final Holder h = curHolder();
    final WebView w = h == null ? null : h.view;
    if (w == null) {
      Shared.err(this, "Webview", 0, "tidak ada current (SetWebView dulu)");
      return;
    }
    final String s = html == null ? "" : html;
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            cancelJsOf(h);
            w.loadDataWithBaseURL(null, s, "text/html", "UTF-8", null);
          } catch (Exception e) {
            String m = e.getMessage();
            Shared.err(Tools_Webview.this, "Webview", curId(),
                m == null ? "gagal load" : m);
          }
        }
      });
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Webview", curId(), m == null ? "gagal load" : m);
    }
  }

  @SimpleFunction(description = "CALL: jalankan JavaScript di CURRENT. Hasil "
      + "string (JSON-quoted) via AfterJavascriptEvaluated(id, result).")
  public void EvaluateJavaScript(final String script) {
    final int id = curId();
    final WebView w = viewOf(id);
    if (w == null) {
      Shared.err(this, "Webview", 0, "tidak ada current (SetWebView dulu)");
      return;
    }
    CallEvaluateJavascript(id, script);
  }

  @SimpleFunction(description = "True bila CURRENT bisa mundur sekian "
      + "langkah (negatif = mundur, positif = maju).")
  public boolean IsGoBackOrForwardAvailable(int steps) {
    try {
      WebView w = curView();
      return w != null && w.canGoBackOrForward(steps);
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleProperty(description = "SET: lompat riwayat CURRENT sejauh steps. "
      + "Tanpa feedback.")
  public void GoBackOrForward(final int steps) {
    final WebView w = curView();
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            if (w.canGoBackOrForward(steps)) {
              w.goBackOrForward(steps);
            }
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }


  @SimpleFunction(description = "Riwayat internal (URL per baris waktu) "
      + "webview ber-id. Kosong bila tidak ada.")
  public List<String> GetInternalHistory(int id) {
    ArrayList<String> out = new ArrayList<String>();
    try {
      WebView w = viewOf(id);
      if (w == null) {
        return out;
      }
      WebBackForwardList bl = w.copyBackForwardList();
      for (int i = 0; i < bl.getSize(); i = i + 1) {
        WebHistoryItem it = bl.getItemAtIndex(i);
        out.add(it == null || it.getUrl() == null ? "" : it.getUrl());
      }
    } catch (Exception e) {
      // abaikan
    }
    return out;
  }

  @SimpleFunction(description = "Hapus webview ber-id (lepas view + lepas id "
      + "dari pool, bisa dipakai lagi). Sukses menembak WebviewRemoved; "
      + "tidak ada = diam.")
  public void RemoveWebView(int id) {
    boolean existed;
    synchronized (viewLock) {
      existed = holders.containsKey(Integer.valueOf(id));
    }
    clearStatic(id);
    if (existed) {
      WebviewRemoved(id);
    }
  }


  // ================= blok Sunny: operasi tampilan =================

  @SimpleFunction(description = "CALL: cari teks di CURRENT (async). Hasil "
      + "via FindResultReceived.")
  public void Find(String string) {
    final WebView w = curView();
    if (w == null) {
      Shared.err(this, "Webview", 0, "tidak ada current (SetWebView dulu)");
      return;
    }
    final String s = string == null ? "" : string;
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            if (Build.VERSION.SDK_INT >= 16) {
              w.findAllAsync(s);
            }
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "SET: pindah ke hasil cari berikut (true) "
      + "atau sebelum (false) di CURRENT. Tanpa feedback.")
  public void FindNext(final boolean forward) {
    final WebView w = curView();
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.findNext(forward);
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "SET: hapus highlight hasil cari di CURRENT. "
      + "Tanpa feedback.")
  public void ClearMatches() {
    final WebView w = curView();
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.clearMatches();
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "SET: hapus data form webview ber-id. Tanpa "
      + "feedback.")
  public void ClearFormData(final int id) {
    final WebView w = viewOf(id);
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.clearFormData();
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "SET: hapus riwayat internal webview ber-id. "
      + "Tanpa feedback.")
  public void ClearInternalHistory(final int id) {
    final WebView w = viewOf(id);
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.clearHistory();
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "SET: hapus cache resource CURRENT. Tanpa "
      + "feedback.")
  public void ClearCache() {
    final WebView w = curView();
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.clearCache(true);
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "SET: hapus semua preferensi lokasi. Tanpa "
      + "feedback.")
  public void ClearLocation() {
    try {
      GeolocationPermissions.getInstance().clearAll();
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "SET: gulir CURRENT ke posisi x,y (dp). "
      + "Tanpa feedback.")
  public void ScrollTo(final int x, final int y) {
    final WebView w = curView();
    if (w == null) {
      return;
    }
    try {
      final int px = p2d(x);
      final int py = p2d(y);
      w.postDelayed(new Runnable() {
        public void run() {
          try {
            w.scrollTo(px, py);
          } catch (Exception e) {
            // abaikan
          }
        }
      }, 300);
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "Posisi gulir kiri CURRENT (dp). -1 bila "
      + "tidak ada current.")
  public int GetScrollX() {
    try {
      WebView w = curView();
      return w == null ? -1 : d2p(w.getScrollX());
    } catch (Exception e) {
      return -1;
    }
  }

  @SimpleFunction(description = "Posisi gulir atas CURRENT (dp). -1 bila "
      + "tidak ada current.")
  public int GetScrollY() {
    try {
      WebView w = curView();
      return w == null ? -1 : d2p(w.getScrollY());
    } catch (Exception e) {
      return -1;
    }
  }

  @SimpleFunction(description = "Tinggi konten HTML CURRENT (dp). -1 bila "
      + "tidak ada current.")
  public int ContentHeight() {
    try {
      WebView w = curView();
      return w == null ? -1 : d2p(w.getContentHeight());
    } catch (Exception e) {
      return -1;
    }
  }

  @SimpleProperty(description = "SET: gulir CURRENT setengah layar ke bawah "
      + "(true = sampai bawah). Tanpa feedback.")
  public void PageDown(final boolean bottom) {
    final WebView w = curView();
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.pageDown(bottom);
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "SET: gulir CURRENT setengah layar ke atas "
      + "(true = sampai atas). Tanpa feedback.")
  public void PageUp(final boolean top) {
    final WebView w = curView();
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.pageUp(top);
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "SET: zoom-in CURRENT. Tanpa feedback.")
  public void ZoomIn() {
    final WebView w = curView();
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.zoomIn();
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "SET: zoom-out CURRENT. Tanpa feedback.")
  public void ZoomOut() {
    final WebView w = curView();
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.zoomOut();
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "SET: zoom CURRENT sejauh persen. Tanpa "
      + "feedback.")
  public void ZoomBy(final int zoomP) {
    final WebView w = curView();
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.zoomBy((float) zoomP);
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "SET: jeda rendering webview ber-id "
      + "(animasi/geolokasi). Tanpa feedback.")
  public void PauseWebView(final int id) {
    final WebView w = viewOf(id);
    if (w == null) {
      Shared.err(this, "Webview", id, "id tidak ada");
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.onPause();
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "SET: lanjutkan webview ber-id yang di-pause. "
      + "Tanpa feedback.")
  public void ResumeWebView(final int id) {
    final WebView w = viewOf(id);
    if (w == null) {
      Shared.err(this, "Webview", id, "id tidak ada");
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.onResume();
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "Jawab confirm JS di CURRENT (true = OK). "
      + "Sama logika dengan ConfirmJsDialog current.")
  public void ConfirmJs(boolean confirm) {
    int id = curId();
    Holder h = holderOf(id);
    if (h == null || h.jsConfirm == null) {
      return;
    }
    try {
      if (confirm) {
        h.jsConfirm.confirm();
      } else {
        h.jsConfirm.cancel();
      }
    } catch (Exception e) {
      // abaikan
    } finally {
      h.jsConfirm = null;
    }
  }

  @SimpleProperty(description = "Jawab prompt JS di CURRENT dengan teks. Sama "
      + "logika dengan ContinueJsPrompt current.")
  public void ContinueJs(String input) {
    Holder h = curHolder();
    if (h == null || h.jsPrompt == null) {
      return;
    }
    try {
      h.jsPrompt.confirm(input == null ? "" : input);
    } catch (Exception e) {
      // abaikan
    } finally {
      h.jsPrompt = null;
    }
  }

  @SimpleFunction(description = "Jawab geolokasi CURRENT (allow + remember). "
      + "Sama logika dengan AllowGeolocation.")
  public void AllowGeolocationAccess(boolean allow, boolean remember) {
    AllowGeolocation(allow, remember);
  }

  @SimpleProperty(description = "SET: beri/tolak izin web CURRENT. Kosong = "
      + "tolak; isi apa pun = beri semua yang diminta. Tanpa feedback.")
  public void GrantPermission(final String permissions) {
    final Holder h = curHolder();
    if (h == null || h.permReq == null) {
      return;
    }
    final PermissionRequest req = h.permReq;
    final boolean deny = permissions == null
        || permissions.trim().length() == 0;
    h.permReq = null;
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            if (deny) {
              req.deny();
            } else {
              req.grant(req.getResources());
            }
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "Jawab HTTP-auth CURRENT. Kosong semua = "
      + "batal.")
  public void ProceedHttpAuthRequest(String username, String password) {
    ProceedHttpAuth(curId(), username, password);
  }

  // ================= blok Sunny: cookie + arsip + shortcut + post =================

  @SimpleFunction(description = "Cookie untuk URL ini (kosong bila tidak "
      + "ada).")
  public String GetCookies(String url) {
    try {
      String c = CookieManager.getInstance().getCookie(
          url == null ? "" : url);
      return c == null ? "" : c;
    } catch (Exception e) {
      return "";
    }
  }

  @SimpleFunction(description = "SET: tulis cookie untuk URL ini. Tanpa "
      + "feedback.")
  public void SetCookies(String url, String cookieString) {
    try {
      CookieManager.getInstance().setCookie(url == null ? "" : url,
          cookieString == null ? "" : cookieString);
      try {
        CookieManager.getInstance().flush();
      } catch (Exception e) {
        // abaikan
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "CALL: hapus semua cookie. Hasil via "
      + "CookiesRemoved(successful).")
  public void ClearCookies() {
    try {
      CookieManager cm = CookieManager.getInstance();
      if (Build.VERSION.SDK_INT >= 21) {
        cm.removeAllCookies(new ValueCallback<Boolean>() {
          public void onReceiveValue(Boolean ok) {
            CookiesRemoved(ok != null && ok.booleanValue());
          }
        });
      } else {
        cm.removeAllCookie();
        CookiesRemoved(true);
      }
      try {
        cm.flush();
      } catch (Exception e) {
        // abaikan
      }
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Webview", 0, m == null ? "cookie gagal" : m);
    }
  }

  @SimpleFunction(description = "CALL: simpan halaman CURRENT sebagai arsip "
      + "web (dir = folder). Hasil via AfterArchiveSaved(success, filePath).")
  public void SaveArchive(final String dir) {
    final WebView w = curView();
    if (w == null) {
      Shared.err(this, "Webview", 0, "tidak ada current (SetWebView dulu)");
      return;
    }
    final String d = dir == null ? "" : dir.trim();
    if (d.length() == 0) {
      Shared.err(this, "Webview", 0, "dir kosong");
      return;
    }
    if (Build.VERSION.SDK_INT < 19) {
      Shared.err(this, "Webview", 0, "butuh Android 4.4+");
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.saveWebArchive(d, true, new ValueCallback<String>() {
              public void onReceiveValue(String s) {
                if (s == null) {
                  AfterArchiveSaved(false, "");
                } else {
                  AfterArchiveSaved(true, s);
                }
              }
            });
          } catch (Exception e) {
            String m = e.getMessage();
            Shared.err(Tools_Webview.this, "Webview", curId(),
                m == null ? "arsip gagal" : m);
          }
        }
      });
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Webview", curId(), m == null ? "arsip gagal" : m);
    }
  }

  @SimpleProperty(description = "SET: catat nama screen untuk shortcut "
      + "(dipakai CreateShortcut). Tanpa feedback.")
  public void RegisterScreenForShortcut(String screen) {
    screenName = screen == null ? "" : screen.trim();
  }

  @SimpleFunction(description = "CALL: buat shortcut situs di layar utama "
      + "(iconPath = file gambar). Tanpa return; gagal via ErrorOccurred.")
  public void CreateShortcut(String url, String iconPath, String title) {
    try {
      final String u = url == null ? "" : url.trim();
      final String t = title == null ? "" : title;
      Bitmap img = null;
      try {
        img = BitmapFactory.decodeFile(iconPath == null ? "" : iconPath);
      } catch (Exception e) {
        img = null;
      }
      if (u.length() == 0 || img == null) {
        Shared.err(this, "Webview", 0, "url/icon tidak valid");
        return;
      }
      String scr = "";
      try {
        scr = context.getSharedPreferences("TinyDB1",
            Context.MODE_PRIVATE).getString("ssn", "");
      } catch (Exception e) {
        scr = "";
      }
      String target = "Screen1";
      if (scr != null && scr.length() > 0 && screenName.length() > 0) {
        try {
          Object o = JsonUtil.getObjectFromJson(scr, true);
          if (o != null && o.toString().length() > 0) {
            target = o.toString();
          }
        } catch (Exception e) {
          // abaikan
        }
      } else if (screenName.length() > 0) {
        target = screenName;
      }
      String pkg = context.getPackageName();
      Intent base = context.getPackageManager()
          .getLaunchIntentForPackage(pkg);
      String cls = "";
      try {
        cls = context.getPackageManager().resolveActivity(base, 0)
            .activityInfo.name.replaceAll("Screen1", target);
      } catch (Exception e) {
        cls = "";
      }
      if (cls.length() == 0) {
        Shared.err(this, "Webview", 0, "activity tidak ketemu");
        return;
      }
      Intent intent = new Intent();
      intent.setClassName(context, cls);
      ArrayList<String> sv = new ArrayList<String>();
      sv.add(u);
      sv.add("2");
      try {
        intent.putExtra("APP_INVENTOR_START",
            JsonUtil.getJsonRepresentation(sv));
      } catch (Exception e) {
        // abaikan
      }
      if (Build.VERSION.SDK_INT < 26) {
        Intent installer = new Intent(
            "com.android.launcher.action.INSTALL_SHORTCUT");
        installer.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
        installer.putExtra(Intent.EXTRA_SHORTCUT_NAME, t);
        installer.putExtra(Intent.EXTRA_SHORTCUT_ICON, img);
        installer.putExtra("duplicate", false);
        context.sendBroadcast(installer);
      } else {
        ShortcutManager sm = (ShortcutManager) context.getSystemService(
            Context.SHORTCUT_SERVICE);
        if (sm == null || !sm.isRequestPinShortcutSupported()) {
          Shared.err(this, "Webview", 0, "pin shortcut tak didukung");
          return;
        }
        ShortcutInfo info = new ShortcutInfo.Builder(context, t)
            .setShortLabel(t).setIcon(Icon.createWithBitmap(img))
            .setIntent(intent).build();
        Intent cb = sm.createShortcutResultIntent(info);
        int fl = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
          fl |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, cb, fl);
        sm.requestPinShortcut(info, pi.getIntentSender());
      }
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Webview", 0, m == null ? "shortcut gagal" : m);
    }
  }

  @SimpleFunction(description = "CALL: muat URL dengan POST (data teks) ke "
      + "CURRENT. Selesai via PageLoaded.")
  public void PostData(String url, String data) {
    final WebView w = curView();
    if (w == null) {
      Shared.err(this, "Webview", 0, "tidak ada current (SetWebView dulu)");
      return;
    }
    final String u = url == null ? "" : url;
    final String d = data == null ? "" : data;
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.postUrl(u, d.getBytes("UTF-8"));
          } catch (Exception e) {
            String m = e.getMessage();
            Shared.err(Tools_Webview.this, "Webview", curId(),
                m == null ? "post gagal" : m);
          }
        }
      });
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Webview", curId(), m == null ? "post gagal" : m);
    }
  }

  // ================= blok Sunny: header + darkmode + deeplink + form =================

  @SimpleProperty(description = "SET hijau: simpan custom headers "
      + "(dictionary ATAU teks \"Key: Value\"). Dipakai tiap load http(s) "
      + "bila domain cocok.", category = PropertyCategory.BEHAVIOR)
  public void CustomHeaders(Object headers) {
    synchronized (viewLock) {
      customHeaders.clear();
      customHeaders.putAll(headersMapOf(headers));
    }
  }

  @SimpleFunction(description = "SET: buang semua custom headers. Tanpa "
      + "feedback.")
  public void ClearCustomHeaders() {
    synchronized (viewLock) {
      customHeaders.clear();
    }
  }

  @SimpleProperty(description = "SET hijau: batasi custom headers ke domain "
      + "ini saja (list ATAU teks). Kosong = semua domain.",
      category = PropertyCategory.BEHAVIOR)
  public void HeaderDomains(Object domains) {
    synchronized (viewLock) {
      headerDomains.clear();
      ArrayList<String> l = strListOf(domains);
      for (int i = 0; i < l.size(); i = i + 1) {
        headerDomains.add(l.get(i));
      }
    }
  }

  @SimpleFunction(description = "SET: hapus filter domain header. Tanpa "
      + "feedback.")
  public void ClearHeaderDomains() {
    synchronized (viewLock) {
      headerDomains.clear();
    }
  }

  @SimpleProperty(description = "True bila mode gelap CURRENT nyala "
      + "(Android 9+).", category = PropertyCategory.BEHAVIOR)
  public boolean DarkMode() {
    try {
      WebView w = curView();
      if (w == null || Build.VERSION.SDK_INT < 29) {
        return false;
      }
      return w.getSettings().getForceDark() == 2;
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleProperty(description = "SET hijau: mode gelap CURRENT (Android 9+; "
      + "di bawah itu diam).", category = PropertyCategory.BEHAVIOR)
  public void DarkMode(final boolean enable) {
    final WebView w = curView();
    if (w == null) {
      return;
    }
    if (Build.VERSION.SDK_INT < 29) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.getSettings().setForceDark(enable ? 2 : 0);
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "SET: daftarkan skema deep-link custom "
      + "(contoh \"myapp\"). Tanpa feedback; duplikat diabaikan.")
  public void RegisterDeepLink(String scheme) {
    try {
      String s = scheme == null ? "" : scheme.trim();
      if (s.length() == 0) {
        return;
      }
      int c = s.indexOf(':');
      if (c > 0) {
        s = s.substring(0, c);
      }
      synchronized (viewLock) {
        if (!customDeepSchemes.contains(s)) {
          customDeepSchemes.add(s);
        }
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "SET: jawab resubmission form CURRENT (true = "
      + "kirim ulang). Dipanggil dari event OnFormResubmission. Tanpa "
      + "feedback bila tidak ada yang menunggu.")
  public void ResubmitForm(boolean reSubmit) {
    Holder h = curHolder();
    if (h == null) {
      return;
    }
    Message rs = h.reSendMsg;
    Message ds = h.dontSendMsg;
    h.reSendMsg = null;
    h.dontSendMsg = null;
    try {
      if (reSubmit) {
        if (rs != null) {
          rs.sendToTarget();
        }
      } else {
        if (ds != null) {
          ds.sendToTarget();
        }
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "CALL: sambungkan popup yang tertunda ke "
      + "webview ber-id (dipanggil dari OnNewWindowRequest). Gagal via "
      + "ErrorOccurred.")
  public void LoadInNewWindow(int id) {
    WebView w = viewOf(id);
    Holder h = holderOf(id);
    if (w == null || h == null) {
      Shared.err(this, "Webview", id, "id tidak ada");
      return;
    }
    Message m = h.newWindowMsg;
    if (m == null) {
      Shared.err(this, "Webview", id, "tidak ada popup tertunda");
      return;
    }
    try {
      WebView.WebViewTransport t = (WebView.WebViewTransport) m.obj;
      t.setWebView(w);
      m.sendToTarget();
    } catch (Exception e) {
      String s = e.getMessage();
      Shared.err(this, "Webview", id, s == null ? "gagal" : s);
    } finally {
      h.newWindowMsg = null;
    }
  }

  @SimpleFunction(description = "SET: tutup tampilan fullscreen (video) di "
      + "CURRENT. Tanpa feedback.")
  public void HideCustomView() {
    try {
      final WebView w = curView();
      if (w == null) {
        return;
      }
      WebChromeClient cc = w.getWebChromeClient();
      if (cc != null) {
        cc.onHideCustomView();
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "CALL: unduh file ke folder. url blob: = "
      + "diekstrak via JS lalu disimpan; http(s) = DownloadManager. "
      + "downloadDir \"~...\" = privat app, else publik. Hasil via "
      + "BlobFileDownloaded(filePath); gagal via ErrorOccurred.")
  public void DownloadBlob(final String url, final String mimeType,
      final String fileName, final String downloadDir) {
    final String u = url == null ? "" : url.trim();
    final String fn = fileName == null ? "" : fileName.trim();
    final String dd = downloadDir == null ? "" : downloadDir.trim();
    if (u.length() == 0) {
      Shared.err(this, "Webview", 0, "url kosong");
      return;
    }
    if (u.startsWith("blob:")) {
      final WebView w = curView();
      if (w == null) {
        Shared.err(this, "Webview", 0, "tidak ada current (SetWebView dulu)");
        return;
      }
      blobPending = true;
      blobFileName = fn.length() > 0 ? fn : "blob.bin";
      blobDir = dd;
      final String js = "(function(){try{fetch('" + u
          + "').then(function(r){return r.blob();}).then(function(b){"
          + "var fr=new FileReader();fr.onload=function(){try{var s=''"
          + "+fr.result;var c=s.indexOf(',');"
          + "AppInventor.submitBlob(c>=0?s.substring(c+1):s,b.type||'');"
          + "}catch(e){}};fr.readAsDataURL(b);}).catch(function(){"
          + "AppInventor.submitBlob('','');});}catch(e){try{"
          + "AppInventor.submitBlob('','');}catch(_){}}})()";
      try {
        form.runOnUiThread(new Runnable() {
          public void run() {
            try {
              if (Build.VERSION.SDK_INT >= 19) {
                w.evaluateJavascript(js, null);
              } else {
                Shared.err(Tools_Webview.this, "Webview", 0,
                    "butuh Android 4.4+");
              }
            } catch (Exception e) {
              String m = e.getMessage();
              Shared.err(Tools_Webview.this, "Webview", 0,
                  m == null ? "blob gagal" : m);
            }
          }
        });
      } catch (Exception e) {
        String m = e.getMessage();
        Shared.err(this, "Webview", 0, m == null ? "blob gagal" : m);
      }
      return;
    }
    if (!(u.startsWith("http://") || u.startsWith("https://"))) {
      Shared.err(this, "Webview", 0, "skema tak didukung");
      return;
    }
    final String mt = mimeType == null ? "" : mimeType;
    try {
      AsynchUtil.runAsynchronously(new Runnable() {
        public void run() {
          String fail = "";
          String path = "";
          try {
            DownloadManager dm = (DownloadManager) context.getSystemService(
                Context.DOWNLOAD_SERVICE);
            if (dm == null) {
              fail = "DownloadManager mati";
            } else {
              String name = fn.length() > 0 ? fn : "download.bin";
              String dir = dd.length() > 0 ? dd : "Download";
              DownloadManager.Request rq = new DownloadManager.Request(
                  Uri.parse(u));
              if (mt.length() > 0) {
                rq.setMimeType(mt);
              }
              try {
                String ck = CookieManager.getInstance().getCookie(u);
                if (ck != null && ck.length() > 0) {
                  rq.addRequestHeader("cookie", ck);
                }
              } catch (Exception e) {
                // abaikan
              }
              rq.setDescription("Downloading file...");
              rq.setTitle(name);
              rq.setNotificationVisibility(DownloadManager.Request
                  .VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
              java.io.File expect;
              if (dir.startsWith("~")) {
                String sub = dir.substring(1);
                if (sub.startsWith("/")) {
                  sub = sub.substring(1);
                }
                rq.setDestinationInExternalFilesDir(context,
                    sub.length() > 0 ? sub : null, name);
                java.io.File base = context.getExternalFilesDir(
                    sub.length() > 0 ? sub : null);
                expect = base == null ? null : new java.io.File(base, name);
              } else {
                rq.setDestinationInExternalPublicDir(dir, name);
                expect = new java.io.File(
                    Environment.getExternalStoragePublicDirectory(dir),
                    name);
              }
              long did = dm.enqueue(rq);
              boolean run = true;
              while (run) {
                try {
                  Thread.sleep(500);
                } catch (Exception e) {
                  // abaikan
                }
                DownloadManager.Query q = new DownloadManager.Query();
                q.setFilterById(did);
                android.database.Cursor cu = null;
                try {
                  cu = dm.query(q);
                  if (cu != null && cu.moveToFirst()) {
                    int st = cu.getInt(cu.getColumnIndex(
                        DownloadManager.COLUMN_STATUS));
                    if (st == DownloadManager.STATUS_SUCCESSFUL) {
                      run = false;
                      path = expect == null ? "" : expect.getAbsolutePath();
                    } else if (st == DownloadManager.STATUS_FAILED) {
                      run = false;
                      fail = "unduhan gagal";
                    }
                  } else {
                    run = false;
                    fail = "unduhan hilang";
                  }
                } catch (Exception e) {
                  run = false;
                  String m = e.getMessage();
                  fail = m == null ? "unduhan gagal" : m;
                } finally {
                  try {
                    if (cu != null) {
                      cu.close();
                    }
                  } catch (Exception e) {
                    // abaikan
                  }
                }
              }
            }
          } catch (Exception e) {
            String m = e.getMessage();
            fail = m == null ? "unduhan gagal" : m;
          }
          final String fPath = path;
          final String fFail = fail;
          try {
            form.runOnUiThread(new Runnable() {
              public void run() {
                if (fFail.length() > 0) {
                  Shared.err(Tools_Webview.this, "Webview", 0, fFail);
                } else {
                  BlobFileDownloaded(fPath);
                }
              }
            });
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Webview", 0, m == null ? "unduhan gagal" : m);
    }
  }

  // ================= blok Sunny: jembatan filter AdBlock =================


  @SimpleFunction(description = "CALL: muat 1 daftar filter (otomatis: " +
      "http(s) = download, //nama = asset, kosong/default = EasyList, " +
      "lainnya = file). Event OnFilterListProgress/Loaded/Error.")
  public void LoadFilterList(final String source) {
    String s = source == null ? "" : source.trim();
    if (s.length() == 0 || s.equalsIgnoreCase("default")) {
      loadFilterFromUrl("https://easylist.to/easylist/easylist.txt");
      return;
    }
    if (s.startsWith("http://") || s.startsWith("https://")) {
      loadFilterFromUrl(s);
      return;
    }
    String p = s;
    if (p.startsWith("//")) {
      p = p.substring(2);
    }
    if (p.startsWith("/")) {
      loadFilterFromFile(new java.io.File(p));
      return;
    }
    loadFilterFromAsset(p);
  }

  private void loadFilterFromUrl(final String u) {
    if (u.length() == 0) {
      Shared.err(this, "Webview", 0, "url kosong");
      return;
    }
    try {
      AsynchUtil.runAsynchronously(new Runnable() {
        public void run() {
          String t = Tools_AdBlock.downloadText(u);
          finishFilterLoad(u, t);
        }
      });
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Webview", 0, m == null ? "gagal" : m);
    }
  }

  private void loadFilterFromAsset(final String assetPath) {
    String p = assetPath == null ? "" : assetPath.trim();
    if (p.startsWith("//")) {
      p = p.substring(2);
    }
    if (p.length() == 0) {
      Shared.err(this, "Webview", 0, "asset kosong");
      return;
    }
    final String fp = p;
    try {
      AsynchUtil.runAsynchronously(new Runnable() {
        public void run() {
          String t = null;
          java.io.InputStream in = null;
          try {
            in = form.openAsset(fp);
            byte[] d = readAllBytes(in);
            t = new String(d, "UTF-8");
          } catch (Exception e) {
            t = null;
          } finally {
            try {
              if (in != null) {
                in.close();
              }
            } catch (Exception e) {
              // abaikan
            }
          }
          finishFilterLoad(fp, t);
        }
      });
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Webview", 0, m == null ? "gagal" : m);
    }
  }

  private void loadFilterFromFile(final java.io.File f) {
    if (!f.exists()) {
      Shared.err(this, "Webview", 0, "file tidak ada");
      return;
    }
    try {
      AsynchUtil.runAsynchronously(new Runnable() {
        public void run() {
          String t = null;
          java.io.FileInputStream in = null;
          try {
            in = new java.io.FileInputStream(f);
            byte[] d = readAllBytes(in);
            t = new String(d, "UTF-8");
          } catch (Exception e) {
            t = null;
          } finally {
            try {
              if (in != null) {
                in.close();
              }
            } catch (Exception e) {
              // abaikan
            }
          }
          finishFilterLoad(f.getAbsolutePath(), t);
        }
      });
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Webview", 0, m == null ? "gagal" : m);
    }
  }

  private void finishFilterLoad(final String source, final String text) {
    final String s = source == null ? "" : source;
    if (text == null) {
      try {
        form.runOnUiThread(new Runnable() {
          public void run() {
            OnFilterListError("gagal baca", s);
          }
        });
      } catch (Exception e) {
        // abaikan
      }
      return;
    }
    final String t = text;
    final int added = Tools_AdBlock.addFilterText(t);
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          OnFilterListProgress(t.length(), s);
          OnFilterListLoaded(added, s);
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  private static byte[] readAllBytes(java.io.InputStream in)
      throws Exception {
    java.io.ByteArrayOutputStream bos =
        new java.io.ByteArrayOutputStream();
    byte[] buf = new byte[32768];
    int n;
    while ((n = in.read(buf)) > 0) {
      bos.write(buf, 0, n);
    }
    bos.flush();
    return bos.toByteArray();
  }


  @SimpleFunction(description = "CALL: baca sertifikat SSL halaman CURRENT. "
      + "Hasil via GotCertificate(isSecure, issuedBy, issuedTo, validTill).")
  public void GetSslCertificate() {
    final WebView w = curView();
    if (w == null) {
      Shared.err(this, "Webview", 0, "tidak ada current (SetWebView dulu)");
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            SslCertificate c = w.getCertificate();
            if (c == null) {
              GotCertificate(false, "", "", "");
            } else {
              String by = "";
              String to = "";
              String till = "";
              try {
                by = c.getIssuedBy().getDName();
              } catch (Exception e) {
                by = "";
              }
              try {
                to = c.getIssuedTo().getDName();
              } catch (Exception e) {
                to = "";
              }
              try {
                till = c.getValidNotAfterDate().toString();
              } catch (Exception e) {
                till = "";
              }
              GotCertificate(true, by == null ? "" : by,
                  to == null ? "" : to, till == null ? "" : till);
            }
          } catch (Exception e) {
            String m = e.getMessage();
            Shared.err(Tools_Webview.this, "Webview", curId(),
                m == null ? "sertifikat gagal" : m);
          }
        }
      });
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Webview", curId(),
          m == null ? "sertifikat gagal" : m);
    }
  }

  // ================= properti Sunny (current-based) =================

  @SimpleProperty(description = "SET: teks host iklan yang diblokir "
      + "(diteruskan ke mesin AdBlock global). Tulis saja.",
      category = PropertyCategory.BEHAVIOR)
  public void AdHosts(String hosts) {
    try {
      Tools_AdBlock.addFilterText(hosts == null ? "" : hosts);
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "True bila CURRENT memuat gambar.",
      category = PropertyCategory.BEHAVIOR)
  public boolean AutoLoadImages() {
    try {
      WebView w = curView();
      return w != null && w.getSettings().getLoadsImagesAutomatically();
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleProperty(description = "SET: CURRENT boleh/tidak memuat gambar.",
      category = PropertyCategory.BEHAVIOR)
  public void AutoLoadImages(boolean allow) {
    final boolean v = allow;
    final WebView w = curView();
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.getSettings().setBlockNetworkImage(!v);
            w.getSettings().setLoadsImagesAutomatically(v);
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "True bila CURRENT butuh gestur user untuk "
      + "media.", category = PropertyCategory.BEHAVIOR)
  public boolean AutoplayMedia() {
    try {
      WebView w = curView();
      return w != null
          && w.getSettings().getMediaPlaybackRequiresUserGesture();
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleProperty(description = "SET: gestur user untuk media di CURRENT.",
      category = PropertyCategory.BEHAVIOR)
  public void AutoplayMedia(boolean need) {
    final boolean v = need;
    final WebView w = curView();
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.getSettings().setMediaPlaybackRequiresUserGesture(v);
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "SET: warna latar CURRENT (int ARGB). Tulis "
      + "saja.", category = PropertyCategory.BEHAVIOR)
  public void BackgroundColor(int color) {
    final int c = color;
    final WebView w = curView();
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.setBackgroundColor(c);
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "True bila gerbang AdBlock global nyala.",
      category = PropertyCategory.BEHAVIOR)
  public boolean BlockAds() {
    return Tools_AdBlock.isEnabled();
  }

  @SimpleProperty(description = "SET: nyala/mati gerbang AdBlock global "
      + "(AND dengan SetWebviewAdBlock per-id).",
      category = PropertyCategory.BEHAVIOR)
  public void BlockAds(boolean block) {
    Tools_AdBlock.setEnabled(block);
  }

  @SimpleProperty(description = "True bila CURRENT tidak boleh load dari "
      + "network.", category = PropertyCategory.BEHAVIOR)
  public boolean BlockNetworkLoads() {
    try {
      WebView w = curView();
      return w != null && w.getSettings().getBlockNetworkLoads();
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleProperty(description = "SET: blokir load network di CURRENT.",
      category = PropertyCategory.BEHAVIOR)
  public void BlockNetworkLoads(boolean block) {
    final boolean v = block;
    final WebView w = curView();
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.getSettings().setBlockNetworkLoads(v);
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "Mode cache CURRENT (int WebSettings).",
      category = PropertyCategory.BEHAVIOR)
  public int CacheMode() {
    try {
      WebView w = curView();
      return w == null ? -1 : w.getSettings().getCacheMode();
    } catch (Exception e) {
      return -1;
    }
  }

  @SimpleProperty(description = "SET: mode cache CURRENT.",
      category = PropertyCategory.BEHAVIOR)
  public void CacheMode(int mode) {
    final int v = mode;
    final WebView w = curView();
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.getSettings().setCacheMode(v);
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "Judul halaman CURRENT. Kosong bila tidak "
      + "ada.", category = PropertyCategory.BEHAVIOR)
  public String CurrentPageTitle() {
    try {
      WebView w = curView();
      if (w == null || w.getTitle() == null) {
        return "";
      }
      return w.getTitle();
    } catch (Exception e) {
      return "";
    }
  }

  @SimpleProperty(description = "URL halaman CURRENT. Kosong bila tidak ada.",
      category = PropertyCategory.BEHAVIOR)
  public String CurrentUrl() {
    try {
      WebView w = curView();
      if (w == null || w.getUrl() == null) {
        return "";
      }
      return w.getUrl();
    } catch (Exception e) {
      return "";
    }
  }

  @SimpleProperty(description = "Custom headers tersimpan (dictionary).",
      category = PropertyCategory.BEHAVIOR)
  public YailDictionary CustomHeaders() {
    YailDictionary d = new YailDictionary();
    try {
      synchronized (viewLock) {
        for (Map.Entry<String, String> en : customHeaders.entrySet()) {
          d.put(en.getKey(), en.getValue());
        }
      }
    } catch (Exception e) {
      // abaikan
    }
    return d;
  }

  @SimpleProperty(description = "True bila deep-link (tel/whatsapp/dll) "
      + "dibuka ke aplikasi luar.", category = PropertyCategory.BEHAVIOR)
  public boolean DeepLinks() {
    return deepLinksOn;
  }

  @SimpleProperty(description = "SET: deep-link ke aplikasi luar nyala/mati.",
      category = PropertyCategory.BEHAVIOR)
  public void DeepLinks(boolean on) {
    deepLinksOn = on;
  }

  @SimpleProperty(description = "True bila CURRENT mode desktop.",
      category = PropertyCategory.BEHAVIOR)
  public boolean DesktopMode() {
    return desktopDefault;
  }

  @SimpleProperty(description = "SET: mode desktop CURRENT + default webview "
      + "baru.", category = PropertyCategory.BEHAVIOR)
  public void DesktopMode(final boolean desktop) {
    desktopDefault = desktop;
    final WebView w = curView();
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            String ua = w.getSettings().getUserAgentString();
            if (ua == null || ua.length() == 0) {
              ua = mobileUa;
            }
            if (desktop) {
              ua = ua.replace("Android", "diordnA").replace("Mobile",
                  "eliboM");
            } else {
              ua = ua.replace("diordnA", "Android").replace("eliboM",
                  "Mobile");
            }
            w.getSettings().setUserAgentString(ua);
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }
  @SimpleProperty(description = "True bila JavaScript CURRENT nyala.",
      category = PropertyCategory.BEHAVIOR)
  public boolean EnableJS() {
    try {
      WebView w = curView();
      return w != null && w.getSettings().getJavaScriptEnabled();
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleProperty(description = "SET: JavaScript CURRENT nyala/mati.",
      category = PropertyCategory.BEHAVIOR)
  public void EnableJS(final boolean js) {
    final WebView w = curView();
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.getSettings().setJavaScriptEnabled(js);
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "True bila CURRENT boleh akses file lokal.",
      category = PropertyCategory.BEHAVIOR)
  public boolean FileAccess() {
    try {
      WebView w = curView();
      return w != null && w.getSettings().getAllowFileAccess();
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleProperty(description = "SET: akses file lokal CURRENT (4 flag "
      + "sekaligus).", category = PropertyCategory.BEHAVIOR)
  public void FileAccess(final boolean allow) {
    final WebView w = curView();
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.getSettings().setAllowFileAccess(allow);
            w.getSettings().setAllowFileAccessFromFileURLs(allow);
            w.getSettings().setAllowUniversalAccessFromFileURLs(allow);
            w.getSettings().setAllowContentAccess(allow);
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "True bila link http CURRENT diikuti.",
      category = PropertyCategory.BEHAVIOR)
  public boolean FollowLinks() {
    return followLinksOn;
  }

  @SimpleProperty(description = "SET: link http diikuti (false = blokir "
      + "navigasi).", category = PropertyCategory.BEHAVIOR)
  public void FollowLinks(boolean follow) {
    followLinksOn = follow;
  }

  @SimpleProperty(description = "Ukuran font default CURRENT.",
      category = PropertyCategory.BEHAVIOR)
  public int FontSize() {
    try {
      WebView w = curView();
      return w == null ? 16 : w.getSettings().getDefaultFontSize();
    } catch (Exception e) {
      return 16;
    }
  }

  @SimpleProperty(description = "SET: ukuran font default CURRENT.",
      category = PropertyCategory.BEHAVIOR)
  public void FontSize(final int size) {
    final WebView w = curView();
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.getSettings().setDefaultFontSize(size);
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "Daftar domain filter header (list).",
      category = PropertyCategory.BEHAVIOR)
  public List<String> HeaderDomains() {
    synchronized (viewLock) {
      return new ArrayList<String>(headerDomains);
    }
  }

  @SimpleProperty(description = "SET: skala awal CURRENT (0 = default). "
      + "Tulis saja.", category = PropertyCategory.BEHAVIOR)
  public void InitialScale(final int scale) {
    final WebView w = curView();
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.setInitialScale(scale);
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "Tipe layer CURRENT (int).",
      category = PropertyCategory.BEHAVIOR)
  public int LayerType() {
    try {
      WebView w = curView();
      return w == null ? 0 : w.getLayerType();
    } catch (Exception e) {
      return 0;
    }
  }

  @SimpleProperty(description = "SET: tipe layer CURRENT.",
      category = PropertyCategory.BEHAVIOR)
  public void LayerType(final int type) {
    final WebView w = curView();
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.setLayerType(type, null);
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "True bila CURRENT load overview-mode.",
      category = PropertyCategory.BEHAVIOR)
  public boolean LoadWithOverviewMode() {
    try {
      WebView w = curView();
      return w != null && w.getSettings().getLoadWithOverviewMode();
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleProperty(description = "SET: overview-mode CURRENT.",
      category = PropertyCategory.BEHAVIOR)
  public void LoadWithOverviewMode(final boolean mode) {
    final WebView w = curView();
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.getSettings().setLoadWithOverviewMode(mode);
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "True bila long-click CURRENT aktif.",
      category = PropertyCategory.BEHAVIOR)
  public boolean LongClickable() {
    try {
      WebView w = curView();
      return w != null && w.isLongClickable();
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleProperty(description = "SET: long-click CURRENT (menu + event "
      + "LongClicked).", category = PropertyCategory.BEHAVIOR)
  public void LongClickable(final boolean clickable) {
    final WebView w = curView();
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.setLongClickable(clickable);
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "SET: pendekatan window baru (1 = samakan, "
      + "2 = event). Tulis saja.", category = PropertyCategory.BEHAVIOR)
  public void NewWindowApproach(int approach) {
    newWindowApproach = approach;
  }

  @SimpleProperty(description = "Mode over-scroll CURRENT (int).",
      category = PropertyCategory.BEHAVIOR)
  public int OverScrollMode() {
    try {
      WebView w = curView();
      return w == null ? 1 : w.getOverScrollMode();
    } catch (Exception e) {
      return 1;
    }
  }

  @SimpleProperty(description = "SET: mode over-scroll CURRENT.",
      category = PropertyCategory.BEHAVIOR)
  public void OverScrollMode(final int mode) {
    final WebView w = curView();
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.setOverScrollMode(mode);
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "True bila izin web/geolokasi ditanyakan "
      + "dulu (false = beri otomatis).", category = PropertyCategory.BEHAVIOR)
  public boolean PromptForPermission() {
    return promptForPermission;
  }

  @SimpleProperty(description = "SET: tanya izin dulu atau beri otomatis.",
      category = PropertyCategory.BEHAVIOR)
  public void PromptForPermission(boolean ask) {
    promptForPermission = ask;
  }

  @SimpleProperty(description = "Sudut rotasi CURRENT.",
      category = PropertyCategory.BEHAVIOR)
  public float RotationAngle() {
    try {
      WebView w = curView();
      return w == null ? 0f : w.getRotation();
    } catch (Exception e) {
      return 0f;
    }
  }

  @SimpleProperty(description = "SET: sudut rotasi CURRENT.",
      category = PropertyCategory.BEHAVIOR)
  public void RotationAngle(final float rotation) {
    final WebView w = curView();
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.setRotation(rotation);
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "SET: scrollbar CURRENT nyala/mati. Tulis "
      + "saja.", category = PropertyCategory.BEHAVIOR)
  public void ScrollBar(final boolean show) {
    final WebView w = curView();
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.setVerticalScrollBarEnabled(show);
            w.setHorizontalScrollBarEnabled(show);
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "Gaya scrollbar CURRENT (int).",
      category = PropertyCategory.BEHAVIOR)
  public int ScrollBarStyle() {
    try {
      WebView w = curView();
      return w == null ? 0 : w.getScrollBarStyle();
    } catch (Exception e) {
      return 0;
    }
  }

  @SimpleProperty(description = "SET: gaya scrollbar CURRENT.",
      category = PropertyCategory.BEHAVIOR)
  public void ScrollBarStyle(final int style) {
    final WebView w = curView();
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.setScrollBarStyle(style);
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "SET: kunci gulir sentuh semua webview. "
      + "Tulis saja.", category = PropertyCategory.BEHAVIOR)
  public void Scrollable(boolean scroll) {
    scrollEnabledGlobal = scroll;
  }

  @SimpleProperty(description = "True bila CURRENT dukung multi-window.",
      category = PropertyCategory.BEHAVIOR)
  public boolean SupportMultipleWindows() {
    return supportMultipleDef;
  }

  @SimpleProperty(description = "SET: multi-window (berlaku CURRENT + "
      + "webview baru).", category = PropertyCategory.BEHAVIOR)
  public void SupportMultipleWindows(final boolean support) {
    supportMultipleDef = support;
    final WebView w = curView();
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.getSettings()
                .setJavaScriptCanOpenWindowsAutomatically(support);
            w.getSettings().setSupportMultipleWindows(support);
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "True bila CURRENT pakai viewport lebar.",
      category = PropertyCategory.BEHAVIOR)
  public boolean UseWideViewPort() {
    try {
      WebView w = curView();
      return w != null && w.getSettings().getUseWideViewPort();
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleProperty(description = "SET: viewport lebar CURRENT.",
      category = PropertyCategory.BEHAVIOR)
  public void UseWideViewPort(final boolean wide) {
    final WebView w = curView();
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.getSettings().setUseWideViewPort(wide);
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "User-agent CURRENT.",
      category = PropertyCategory.BEHAVIOR)
  public String UserAgent() {
    try {
      WebView w = curView();
      if (w == null) {
        return userAgentOverride.length() > 0 ? userAgentOverride
            : mobileUa;
      }
      String u = w.getSettings().getUserAgentString();
      return u == null ? "" : u;
    } catch (Exception e) {
      return "";
    }
  }

  @SimpleProperty(description = "SET: user-agent CURRENT + default webview "
      + "baru (kosong = bawaan).", category = PropertyCategory.BEHAVIOR)
  public void UserAgent(final String agent) {
    final String a = agent == null ? "" : agent;
    userAgentOverride = a.length() > 0 ? a : mobileUa;
    final WebView w = curView();
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            if (a.length() > 0) {
              w.getSettings().setUserAgentString(a);
            } else if (mobileUa.length() > 0) {
              w.getSettings().setUserAgentString(mobileUa);
            }
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "SET: beri akses geolokasi JS di CURRENT. "
      + "Tulis saja.", category = PropertyCategory.BEHAVIOR)
  public void UsesLocation(final boolean uses) {
    final WebView w = curView();
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.getSettings().setGeolocationEnabled(uses);
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "True bila getar long-click nyala.",
      category = PropertyCategory.BEHAVIOR)
  public boolean VibrationEnabled() {
    return vibrationOn;
  }

  @SimpleProperty(description = "SET: getar + haptic saat long-click.",
      category = PropertyCategory.BEHAVIOR)
  public void VibrationEnabled(boolean vibrate) {
    vibrationOn = vibrate;
  }

  @SimpleProperty(description = "True bila CURRENT sedang tampil.",
      category = PropertyCategory.BEHAVIOR)
  public boolean Visible() {
    try {
      WebView w = curView();
      return w != null && w.getVisibility() == View.VISIBLE;
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleProperty(description = "WebViewString CURRENT (tulis JS via "
      + "AppInventor.setWebViewString).", category = PropertyCategory.BEHAVIOR)
  public String WebViewString() {
    Holder h = curHolder();
    return h == null ? "" : h.webViewString;
  }

  @SimpleProperty(description = "SET: tulis WebViewString CURRENT.",
      category = PropertyCategory.BEHAVIOR)
  public void WebViewString(String text) {
    Holder h = curHolder();
    if (h == null) {
      return;
    }
    h.webViewString = text == null ? "" : text;
  }

  @SimpleProperty(description = "True bila zoom gestur CURRENT nyala.",
      category = PropertyCategory.BEHAVIOR)
  public boolean ZoomEnabled() {
    return zoomEnabledDef;
  }

  @SimpleProperty(description = "SET: zoom gestur (berlaku CURRENT + webview "
      + "baru).", category = PropertyCategory.BEHAVIOR)
  public void ZoomEnabled(final boolean enable) {
    zoomEnabledDef = enable;
    final WebView w = curView();
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.getSettings().setSupportZoom(enable);
            w.getSettings().setBuiltInZoomControls(enable);
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "Zoom halaman persen (default 100).",
      category = PropertyCategory.BEHAVIOR)
  public int ZoomPercent() {
    return zoomPercentDef;
  }

  @SimpleProperty(description = "SET: zoom persen (berlaku CURRENT + webview "
      + "baru).", category = PropertyCategory.BEHAVIOR)
  public void ZoomPercent(final int zoom) {
    zoomPercentDef = zoom;
    final WebView w = curView();
    if (w == null) {
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            w.getSettings().setTextZoom(zoom);
          } catch (Exception e) {
            // abaikan
          }
        }
      });
    } catch (Exception e) {
      // abaikan
    }
  }

  // ================= events =================

  @SimpleEvent(description = "Webview jadi: id + sumber. Number.")
  public void WebviewShown(int id, String source) {
    Shared.fire(this, "WebviewShown", Integer.valueOf(id), source);
  }

  @SimpleEvent(description = "Webview dilepas: id yang hilang. Number.")
  public void WebviewRemoved(int id) {
    Shared.fire(this, "WebviewRemoved", Integer.valueOf(id));
  }

  @SimpleEvent(description = "Load mulai: id + url.")
  public void PageStarted(int id, String url) {
    Shared.fire(this, "PageStarted", Integer.valueOf(id), url);
  }

  @SimpleEvent(description = "Load selesai: id + url (cosmetic sudah diinjeksi).")
  public void PageLoaded(int id, String url) {
    Shared.fire(this, "PageLoaded", Integer.valueOf(id), url);
  }

  @SimpleEvent(description = "Progres load berubah: id + 0-100.")
  public void ProgressChanged(int id, int progress) {
    Shared.fire(this, "ProgressChanged", Integer.valueOf(id),
        Integer.valueOf(progress));
  }

  @SimpleEvent(description = "Error load: id + pesan + kode + url.")
  public void ErrorReceived(int id, String message, int code, String url) {
    Shared.fire(this, "ErrorReceived", Integer.valueOf(id), message,
        Integer.valueOf(code), url);
  }

  @SimpleEvent(description = "Hasil EvaluateJavascript: id + string "
      + "(JSON-quoted).")
  public void AfterJavascriptEvaluated(int id, String result) {
    Shared.fire(this, "AfterJavascriptEvaluated", Integer.valueOf(id),
        result);
  }

  @SimpleEvent(description = "JS memanggil AppInventor.setWebViewString: id + "
      + "nilai.")
  public void WebViewStringChanged(int id, String value) {
    Shared.fire(this, "WebViewStringChanged", Integer.valueOf(id), value);
  }

  @SimpleEvent(description = "Halaman minta upload file: id + mime. Jawab via "
      + "UploadFile (kosong = batal).")
  public void FileUploadNeeded(int id, String mimeType) {
    Shared.fire(this, "FileUploadNeeded", Integer.valueOf(id), mimeType);
  }

  @SimpleEvent(description = "Halaman minta download: id + url + mime + nama. "
      + "Teruskan ke Tools_Downloader.")
  public void DownloadNeeded(int id, String url, String mimeType,
      String fileName) {
    Shared.fire(this, "DownloadNeeded", Integer.valueOf(id), url, mimeType,
        fileName);
  }

  @SimpleEvent(description = "Console halaman: id + pesan + baris + sumber.")
  public void ConsoleMessage(String message, int id, int line,
      String source) {
    Shared.fire(this, "ConsoleMessage", Integer.valueOf(id), message,
        Integer.valueOf(line), source);
  }

  @SimpleEvent(description = "Halaman minta alert JS: id + url + pesan. Tutup "
      + "via DismissJsAlert.")
  public void OnJsAlert(int id, String url, String message) {
    Shared.fire(this, "OnJsAlert", Integer.valueOf(id), url, message);
  }

  @SimpleEvent(description = "Halaman minta confirm JS: id + url + pesan. "
      + "Jawab via ConfirmJsDialog.")
  public void OnJsConfirm(int id, String url, String message) {
    Shared.fire(this, "OnJsConfirm", Integer.valueOf(id), url, message);
  }

  @SimpleEvent(description = "Halaman minta prompt JS: id + url + pesan + "
      + "default. Jawab via ContinueJsPrompt.")
  public void OnJsPrompt(int id, String url, String message,
      String defaultValue) {
    Shared.fire(this, "OnJsPrompt", Integer.valueOf(id), url, message,
        defaultValue);
  }

  @SimpleEvent(description = "Situs minta izin web (kamera/mic): id + list "
      + "izin. Jawab via GrantWebPermission.")
  public void OnWebPermissionRequest(int id, List<String> permissions) {
    YailList y = YailList.makeList(permissions);
    Shared.fire(this, "OnWebPermissionRequest", Integer.valueOf(id), y);
  }

  @SimpleEvent(description = "Situs minta lokasi: id + origin. Jawab via "
      + "AllowGeolocation.")
  public void OnGeolocationRequested(int id, String origin) {
    Shared.fire(this, "OnGeolocationRequested", Integer.valueOf(id), origin);
  }

  @SimpleEvent(description = "SSL error: id + kode. Jawab via ProceedSslError.")
  public void OnSslError(int id, int code) {
    Shared.fire(this, "OnSslError", Integer.valueOf(id),
        Integer.valueOf(code));
  }

  @SimpleEvent(description = "HTTP-auth diminta: id + host + realm. Jawab via "
      + "ProceedHttpAuth.")
  public void OnHttpAuthRequest(int id, String host, String realm) {
    Shared.fire(this, "OnHttpAuthRequest", Integer.valueOf(id), host, realm);
  }

  // ================= event Sunny tambahan + alias =================

  @SimpleEvent(description = "Arsip selesai: success + path file (kosong bila "
      + "gagal).")
  public void AfterArchiveSaved(boolean success, String filePath) {
    Shared.fire(this, "AfterArchiveSaved", Boolean.valueOf(success),
        filePath);
  }

  @SimpleEvent(description = "Unduhan DownloadBlob selesai: path file.")
  public void BlobFileDownloaded(String filePath) {
    Shared.fire(this, "BlobFileDownloaded", filePath);
  }

  @SimpleEvent(description = "Hasil ClearCookies: true bila cookie dibuang.")
  public void CookiesRemoved(boolean successful) {
    Shared.fire(this, "CookiesRemoved", Boolean.valueOf(successful));
  }

  @SimpleEvent(description = "Hasil Find: id + ordinal aktif + total cocok + "
      + "selesai-hitung.")
  public void FindResultReceived(int id, int activeMatchOrdinal,
      int numberOfMatches, boolean isDoneCounting) {
    Shared.fire(this, "FindResultReceived", Integer.valueOf(id),
        Integer.valueOf(activeMatchOrdinal),
        Integer.valueOf(numberOfMatches),
        Boolean.valueOf(isDoneCounting));
  }

  @SimpleEvent(description = "Sertifikat CURRENT: aman + penerbit + untuk + "
      + "berlaku-sampai.")
  public void GotCertificate(boolean isSecure, String issuedBy,
      String issuedTo, String validTill) {
    Shared.fire(this, "GotCertificate", Boolean.valueOf(isSecure),
        issuedBy, issuedTo, validTill);
  }

  @SimpleEvent(description = "Long-click: id + item + url simpul (type 8) + "
      + "tipe HitTest.")
  public void LongClicked(int id, String item, String secondaryUrl,
      int type) {
    Shared.fire(this, "LongClicked", Integer.valueOf(id), item,
        secondaryUrl, Integer.valueOf(type));
  }

  @SimpleEvent(description = "Halaman minta tutup window: id.")
  public void OnCloseWindowRequest(int id) {
    Shared.fire(this, "OnCloseWindowRequest", Integer.valueOf(id));
  }

  @SimpleEvent(description = "Loader daftar filter gagal: pesan + sumber.")
  public void OnFilterListError(String error, String source) {
    Shared.fire(this, "OnFilterListError", error, source);
  }

  @SimpleEvent(description = "Loader daftar filter selesai: aturan BARU + "
      + "sumber.")
  public void OnFilterListLoaded(int rulesCount, String source) {
    Shared.fire(this, "OnFilterListLoaded", Integer.valueOf(rulesCount),
        source);
  }

  @SimpleEvent(description = "Progres loader daftar filter: ukuran byte + "
      + "sumber.")
  public void OnFilterListProgress(int loaded, String source) {
    Shared.fire(this, "OnFilterListProgress", Integer.valueOf(loaded),
        source);
  }

  @SimpleEvent(description = "Form minta kirim ulang: id. Jawab via "
      + "ResubmitForm.")
  public void OnFormResubmission(int id) {
    Shared.fire(this, "OnFormResubmission", Integer.valueOf(id));
  }

  @SimpleEvent(description = "Halaman keluar fullscreen.")
  public void OnHideCustomView() {
    Shared.fire(this, "OnHideCustomView");
  }

  @SimpleEvent(description = "Halaman minta window baru: id + url peminta + "
      + "dialog + popup. Sambung via LoadInNewWindow(id).")
  public void OnNewWindowRequest(int id, String url, boolean isDialog,
      boolean isPopup) {
    Shared.fire(this, "OnNewWindowRequest", Integer.valueOf(id), url,
        Boolean.valueOf(isDialog), Boolean.valueOf(isPopup));
  }

  @SimpleEvent(description = "Webview digulir: id + x/y + x/y lama (dp) + "
      + "bisa-kiri/kanan.")
  public void OnScrollChanged(int id, int scrollX, int scrollY,
      int oldScrollX, int oldScrollY, boolean canGoLeft,
      boolean canGoRight) {
    Shared.fire(this, "OnScrollChanged", Integer.valueOf(id),
        Integer.valueOf(scrollX), Integer.valueOf(scrollY),
        Integer.valueOf(oldScrollX), Integer.valueOf(oldScrollY),
        Boolean.valueOf(canGoLeft), Boolean.valueOf(canGoRight));
  }

  @SimpleEvent(description = "Halaman masuk fullscreen (video). Tutup via "
      + "HideCustomView.")
  public void OnShowCustomView() {
    Shared.fire(this, "OnShowCustomView");
  }

  @SimpleEvent(description = "Usap cepat: id + arah (1 kanan, 2 kiri).")
  public void Swiped(int id, int direction) {
    Shared.fire(this, "Swiped", Integer.valueOf(id),
        Integer.valueOf(direction));
  }

  @SimpleEvent(description = "ALIAS Sunny: error load (id + pesan + kode + "
      + "url). Sama isi dengan ErrorReceived.")
  public void OnErrorReceived(int id, String message, int errorCode,
      String url) {
    Shared.fire(this, "OnErrorReceived", Integer.valueOf(id), message,
        Integer.valueOf(errorCode), url);
  }

  @SimpleEvent(description = "ALIAS Sunny: progres load (id + 0-100). Sama "
      + "isi dengan ProgressChanged.")
  public void OnProgressChanged(int id, int progress) {
    Shared.fire(this, "OnProgressChanged", Integer.valueOf(id),
        Integer.valueOf(progress));
  }

  @SimpleEvent(description = "ALIAS Sunny: halaman minta download (id + url "
      + "+ disposition + mime + size). Sama momen dengan DownloadNeeded.")
  public void OnDownloadNeeded(int id, String url,
      String contentDisposition, String mimeType, int size) {
    Shared.fire(this, "OnDownloadNeeded", Integer.valueOf(id), url,
        contentDisposition, mimeType, Integer.valueOf(size));
  }

  @SimpleEvent(description = "ALIAS Sunny: console halaman (pesan + baris + "
      + "sumber + level). Tanpa id (lihat ConsoleMessage untuk versi "
      + "ber-id).")
  public void OnConsoleMessage(String message, int lineNumber,
      String sourceId, String level) {
    Shared.fire(this, "OnConsoleMessage", message,
        Integer.valueOf(lineNumber), sourceId, level);
  }

  @SimpleEvent(description = "ALIAS Sunny: situs minta izin (list saja, tanpa "
      + "id). Sama momen dengan OnWebPermissionRequest.")
  public void OnPermissionRequest(List<String> permissionsList) {
    YailList y = YailList.makeList(permissionsList);
    Shared.fire(this, "OnPermissionRequest", y);
  }

  @SimpleEvent(description = "ALIAS Sunny: SSL error (kode saja, tanpa id). "
      + "Sama momen dengan OnSslError.")
  public void OnReceivedSslError(int errorCode) {
    Shared.fire(this, "OnReceivedSslError", Integer.valueOf(errorCode));
  }

  @SimpleEvent(description = "ALIAS Sunny: HTTP-auth (id + host + realm). "
      + "Sama isi dengan OnHttpAuthRequest.")
  public void OnReceivedHttpAuthRequest(int id, String host, String realm) {
    Shared.fire(this, "OnReceivedHttpAuthRequest", Integer.valueOf(id),
        host, realm);
  }

  @SimpleEvent(description = "Media (video/audio) tercium dari traffic "
      + "halaman: id + url. Teruskan ke Tools_Downloader. Ala 1DM "
      + "video-grabber (dedupe 3 detik).")
  public void MediaFound(int id, String url) {
    Shared.fire(this, "MediaFound", Integer.valueOf(id), url);
  }

  @SimpleEvent(description = "SATU-SATUNYA error Tools_Webview. component = "
      + "nama operasi (bandingkan blok OpWebview). id number terkait (0 umum).")
  public void ErrorOccurred(String component, int id, String message) {
    Shared.fire(this, "ErrorOccurred", component,
        Integer.valueOf(id), message);
  }

  @SimpleFunction(description = "Fallback polling error terakhir (kosong bila "
      + "belum ada). Isi operation dengan OpWebview.")
  public String LastError(String operation) {
    return Shared.lastError(operation);
  }

  @SimpleProperty(description = "Nama operasi Webview (untuk if di "
      + "ErrorOccurred).", category = PropertyCategory.BEHAVIOR)
  public String OpWebview() {
    return "Webview";
  }

  public void onDestroy() {
    try {
      ArrayList<Integer> all;
      synchronized (viewLock) {
        all = new ArrayList<Integer>(holders.keySet());
      }
      for (int i = 0; i < all.size(); i = i + 1) {
        clearStatic(all.get(i).intValue());
      }
    } catch (Exception e) {
      // abaikan
    }
  }
}
