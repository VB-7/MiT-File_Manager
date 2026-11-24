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
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.appinventor.components.annotations.DesignerComponent;
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@DesignerComponent(
    version = 1,
    description = "Tools_Webview: webview ber-id ala Sunny Gupta v13 (blueprint "
        + "dicuri + disempurnakan, bukan fork). ID NUMBER dua arah (masuk/keluar "
        + "number, tanpa +0), simpan URUT PEMBUATAN + Current/Previous/First/"
        + "Last/List. Pembuatan ringan (taro view + daftar id). Isi Sunny tetap: "
        + "WebViewClient/WebChromeClient, JS bridge AppInventor, upload file, "
        + "download event, izin, geolokasi, dialog JS, SSL, HTTP-auth, deep-link, "
        + "asset MIT. AdBlock OTOMATIS via Tools_AdBlock (network + whitelist + "
        + "cosmetic injeksi). Input fleksibel: list ATAU teks. Konvensi: Is = "
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
    "android.permission.VIBRATE"})
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
    boolean adBlock = true;
    String webViewString = "";
  }

  private static final Map<Integer, Holder> holders =
      new HashMap<Integer, Holder>();
  private static final Object viewLock = new Object();
  private static String mobileUa = "";

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
    if (mobileUa.length() == 0) {
      try {
        mobileUa = w.getSettings().getUserAgentString();
      } catch (Exception e) {
        mobileUa = "";
      }
    }
    WebSettings s = w.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setDatabaseEnabled(true);
    s.setLoadWithOverviewMode(true);
    s.setUseWideViewPort(true);
    s.setBuiltInZoomControls(true);
    s.setDisplayZoomControls(false);
    s.setSupportZoom(true);
    s.setAllowFileAccess(true);
    s.setAllowFileAccessFromFileURLs(true);
    s.setAllowUniversalAccessFromFileURLs(true);
    s.setAllowContentAccess(true);
    s.setMediaPlaybackRequiresUserGesture(false);
    s.setJavaScriptCanOpenWindowsAutomatically(true);
    s.setSupportMultipleWindows(false);
    s.setGeolocationEnabled(true);
    s.setCacheMode(WebSettings.LOAD_DEFAULT);
    s.setDefaultFontSize(16);
    s.setBlockNetworkImage(false);
    s.setLoadsImagesAutomatically(true);
    s.setBlockNetworkLoads(false);
    try {
      CookieManager.getInstance().setAcceptThirdPartyCookies(w, true);
    } catch (Exception e) {
      // abaikan
    }
    w.setFocusable(true);
    w.setLongClickable(false);
    w.setVerticalScrollBarEnabled(true);
    w.setHorizontalScrollBarEnabled(true);
    w.setWebViewClient(new WClient());
    w.setWebChromeClient(new WChrome());
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
        DownloadNeeded(wid, url == null ? "" : url,
            mimeType == null ? "" : mimeType, name);
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
        w.loadUrl("https://" + src);
      } else {
        w.loadUrl(src.indexOf("://") > 0 ? src : "https://" + src);
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
    public void setWebViewString(final String s) {
      Holder h = holderOf(wid);
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
  }

  private final class WClient extends WebViewClient {
    private static final String ASSET_PREFIX = "file:///appinventor_asset/";

    private boolean handleDeep(String url) {
      if (url == null) {
        return false;
      }
      if (url.startsWith("http://") || url.startsWith("https://")
          || url.startsWith("file://") || url.startsWith("content://")
          || url.startsWith("data:") || url.startsWith("javascript:")) {
        return false;
      }
      try {
        Intent it = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        activity.startActivity(it);
        return true;
      } catch (Exception e) {
        return false;
      }
    }

    public boolean shouldOverrideUrlLoading(WebView view, String url) {
      return handleDeep(url);
    }

    public boolean shouldOverrideUrlLoading(WebView view,
        WebResourceRequest request) {
      try {
        return handleDeep(request.getUrl().toString());
      } catch (Exception e) {
        return false;
      }
    }

    public WebResourceResponse shouldInterceptRequest(WebView view,
        String url) {
      if (url != null && (url.startsWith("http://")
          || url.startsWith("https://"))) {
        int wid = holderId(view);
        Holder h = holderOf(wid);
        boolean useAb = h == null ? true : h.adBlock;
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
        if (url.startsWith("http://") || url.startsWith("https://")) {
          int wid = holderId(view);
          Holder h = holderOf(wid);
          boolean useAb = h == null ? true : h.adBlock;
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

    private WebResourceResponse emptyBlock() {
      try {
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
        PageStarted(wid, url == null ? "" : url);
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
        ErrorReceived(wid, desc == null ? "" : desc, code,
            url == null ? "" : url);
      }
    }

    public void onReceivedError(WebView view, WebResourceRequest req,
        WebResourceError err) {
      try {
        if (req.isForMainFrame()) {
          int wid = holderId(view);
          if (wid > 0) {
            ErrorReceived(wid, err.getDescription().toString(),
                err.getErrorCode(), req.getUrl().toString());
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
            ErrorReceived(wid, resp.getReasonPhrase(),
                resp.getStatusCode(), req.getUrl().toString());
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
    }

    public void onReceivedHttpAuthRequest(WebView view,
        HttpAuthHandler handler, String host, String realm) {
      int wid = holderId(view);
      Holder h = holderOf(wid);
      if (h != null) {
        h.authHandler = handler;
      }
      OnHttpAuthRequest(wid, host == null ? "" : host,
          realm == null ? "" : realm);
    }
  }

  private final class WChrome extends WebChromeClient {
    public void onProgressChanged(WebView view, int progress) {
      int wid = holderId(view);
      if (wid > 0) {
        ProgressChanged(wid, progress);
      }
    }

    public boolean onConsoleMessage(ConsoleMessage msg) {
      try {
        WebView v = null;
        // console tidak bawa view; kirim ke current
        int cur = Ids.currentOf(Ids.WEBVIEW);
        Tools_Webview.this.ConsoleMessage(msg.getMessage() == null ? "" : msg.getMessage(),
            cur, msg.lineNumber(),
            msg.sourceId() == null ? "" : msg.sourceId());
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
    }

    public void onGeolocationPermissionsShowPrompt(String origin,
        GeolocationPermissions.Callback cb) {
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
      + "ke CallShowWebview). URUT PEMBUATAN.")
  public int CreateWebviewId() {
    int id = Ids.createFor(Ids.WEBVIEW);
    if (id < 1) {
      Shared.err(this, "Webview", 0, "gagal buat id");
    }
    return id;
  }

  @SimpleFunction(description = "CALL: tampilkan webview ber-id ke layout. "
      + "source fleksibel: URL http(s), www..., //asset, file path, "
      + "atau string HTML (<... = loadData). width/height angka MIT (-2 isi, "
      + "-1 otomatis, positif pixel). id NUMBER (pakai CreateWebviewId, "
      + "reservasi otomatis diadopsi). Sukses menembak WebviewShown; gagal "
      + "via ErrorOccurred.")
  public void CallShowWebview(final AndroidViewComponent container,
      final String source, final int id, final int width, final int height) {
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
            WebView w = new WebView(context);
            Holder h = new Holder();
            h.view = w;
            synchronized (viewLock) {
              holders.put(Integer.valueOf(id), h);
            }
            setupDefaults(w, id);
            detach(w);
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
                mitToAndroidSize(width), mitToAndroidSize(height));
            vg.addView(w, lp);
            Ids.setActive(Ids.WEBVIEW, id, true);
            loadSource(w, source == null ? "" : source);
            String u = "";
            try {
              u = w.getUrl() == null ? "" : w.getUrl();
            } catch (Exception e) {
              u = "";
            }
            WebviewShown(id, source == null ? "" : source);
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

  @SimpleFunction(description = "Hapus webview ber-id (lepas view + lepas id). "
      + "Sukses menembak WebviewRemoved; tidak ada = diam.")
  public void RemoveWebviewId(int id) {
    boolean existed;
    synchronized (viewLock) {
      existed = holders.containsKey(Integer.valueOf(id));
    }
    clearStatic(id);
    if (existed) {
      WebviewRemoved(id);
    }
  }

  @SimpleFunction(description = "SET: hapus SEMUA webview Tools_Webview "
      + "(id ikut dilepas). Menembak WebviewRemoved satu per satu.")
  public void SetClearWebviews() {
    ArrayList<Integer> all;
    synchronized (viewLock) {
      all = new ArrayList<Integer>(holders.keySet());
    }
    for (int i = 0; i < all.size(); i = i + 1) {
      RemoveWebviewId(all.get(i).intValue());
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
      RemoveWebviewId(gone.get(i).intValue());
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

  @SimpleFunction(description = "SET: ganti sumber webview ber-id (format sama "
      + "seperti CallShowWebview). Tanpa feedback; id tidak ada via "
      + "ErrorOccurred.")
  public void SetWebviewSource(final int id, final String source) {
    final WebView w = viewOf(id);
    if (w == null) {
      Shared.err(this, "Webview", id, "id tidak ada");
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            loadSource(w, source == null ? "" : source);
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

  @SimpleFunction(description = "SET: mundur 1 halaman. Tanpa feedback.")
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

  @SimpleFunction(description = "SET: maju 1 halaman. Tanpa feedback.")
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

  @SimpleFunction(description = "SET: muat ulang halaman. Tanpa feedback.")
  public void ReloadWebview(final int id) {
    final WebView w = viewOf(id);
    if (w == null) {
      Shared.err(this, "Webview", id, "id tidak ada");
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
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

  @SimpleFunction(description = "SET: hentikan load. Tanpa feedback.")
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
  public boolean CanGoBack(int id) {
    try {
      WebView w = viewOf(id);
      return w != null && w.canGoBack();
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleFunction(description = "True bila bisa maju.")
  public boolean CanGoForward(int id) {
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

  @SimpleFunction(description = "Tutup dialog alert webview ber-id.")
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

  @SimpleFunction(description = "Jawab izin web (kamera/mic) webview aktif. "
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
