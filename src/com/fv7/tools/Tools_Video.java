package com.fv7.tools;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.VideoView;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.AndroidViewComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.OnDestroyListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UsesPermissions("android.permission.INTERNET")
@DesignerComponent(
    version = 22,
    description = "Tools_Video: putar BANYAK video ber-id di dalam layout (Arrangement MIT / dinamis), "
        + "kontrol ala VLC (pause/resume/seek/maju/mundur/double-tap), loop, volume. "
        + "Satu id = satu video, bisa jalan bareng. Id dikelola CreateVideoId (atau "
        + "CreateUniqueId Tools_Base). List id URUT PEMBUATAN (index 1 = pertama dibuat). "
        + "Streaming URL butuh izin INTERNET (otomatis dibawa komponen ini). Akhiran In = "
        + "butuh wadah layout. Konvensi awalan: Is = cek boolean; Call = async + hasil "
        + "lewat event; Set = langsung tanpa feedback; Get = nilai balik sinkron.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    androidMinSdk = 21,
    iconName = "aiwebres/tools_video.jpg",
    helpUrl = "https://github.com/VB-7/MiT-File_Manager")
@SimpleObject(external = true)
public class Tools_Video extends AndroidNonvisibleComponent
    implements OnDestroyListener {

  private final Context context;
  private final Activity activity;

  private int progressInterval = 500;
  private int skipSeconds = 10;
  private boolean showVideoController = true;
  private boolean videoLooping = false;
  private int mediaVolume = 100;
  private boolean doubleTapSeek = false;
  private final Map<Integer, Integer> volumes =
      new HashMap<Integer, Integer>();
  private final Map<Integer, Boolean> loops =
      new HashMap<Integer, Boolean>();
  private static final Map<Integer, MediaPlayer> mps =
      new HashMap<Integer, MediaPlayer>();
  private static final Map<Integer, Integer> mutedPrev =
      new HashMap<Integer, Integer>();

  private static final Map<Integer, VideoView> views = new HashMap<Integer, VideoView>();
  private static final Map<Integer, Boolean> readies = new HashMap<Integer, Boolean>();
  private static final Object videoLock = new Object();
  private Handler mediaTicks = null;

  private final Runnable mediaTick = new Runnable() {
    public void run() {
      try {
        ArrayList<Integer> keys;
        synchronized (videoLock) {
          keys = new ArrayList<Integer>(views.keySet());
        }
        for (int i = 0; i < keys.size(); i = i + 1) {
          int vid = keys.get(i).intValue();
          try {
            VideoView vv;
            boolean ready;
            synchronized (videoLock) {
              vv = views.get(Integer.valueOf(vid));
              Boolean r = readies.get(Integer.valueOf(vid));
              ready = r != null && r.booleanValue();
            }
            if (vv != null && ready && vv.isPlaying()) {
              int pos = vv.getCurrentPosition();
              int dur = vv.getDuration();
              int pct = dur > 0 ? ((int) (pos * 100L / dur)) : 0;
              VideoProgressChanged(vid, pos, dur, pct);
            }
          } catch (Exception e) {
            // abaikan per-id
          }
        }
      } catch (Exception e) {
        // abaikan
      }
      updateTickState();
    }
  };

  public Tools_Video(ComponentContainer container) {
    super(container.$form());
    this.context = container.$context();
    this.activity = container.$context();
  }

  private float effVolFloat(int id) {
    Integer o;
    synchronized (videoLock) {
      o = volumes.get(Integer.valueOf(id));
    }
    return volFloatV(o == null ? mediaVolume : o.intValue());
  }

  private int effVolInt(int id) {
    Integer o;
    synchronized (videoLock) {
      o = volumes.get(Integer.valueOf(id));
    }
    int v = o == null ? mediaVolume : o.intValue();
    if (v < 0) {
      v = 0;
    }
    if (v > 100) {
      v = 100;
    }
    return v;
  }

  private boolean effLoop(int id) {
    Boolean o;
    synchronized (videoLock) {
      o = loops.get(Integer.valueOf(id));
    }
    return o == null ? videoLooping : o.booleanValue();
  }

  private void applyVolId(int id, int volume) {
    try {
      MediaPlayer mp;
      synchronized (videoLock) {
        mp = mps.get(Integer.valueOf(id));
      }
      if (mp != null) {
        float v = volFloatV(volume);
        mp.setVolume(v, v);
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  private void updateTickState() {
    try {
      boolean need = false;
      try {
        synchronized (videoLock) {
          for (Map.Entry<Integer, VideoView> en : views.entrySet()) {
            try {
              Boolean r = readies.get(en.getKey());
              if (r != null && r.booleanValue() && en.getValue().isPlaying()) {
                need = true;
                break;
              }
            } catch (Exception e) {
              // abaikan per-id
            }
          }
        }
      } catch (Exception e) {
        // abaikan
      }
      if (mediaTicks == null) {
        mediaTicks = new Handler(context.getMainLooper());
      }
      mediaTicks.removeCallbacks(mediaTick);
      if (need) {
        int every = progressInterval > 0 ? progressInterval : 500;
        mediaTicks.postDelayed(mediaTick, every);
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  private float volFloatV(int v) {
    if (v < 0) {
      v = 0;
    }
    if (v > 100) {
      v = 100;
    }
    return v / 100.0f;
  }

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

  @SimpleFunction(description = "CALL: Putar video ber-id di dalam layout (Arrangement MIT / "
      + "dinamis). Bisa jalan bareng (satu id = satu video). path file (/Download/... atau "
      + "absolut apa adanya), atau http(s) URL (streaming, izin INTERNET otomatis dibawa). "
      + "id dari CreateVideoId atau CreateUniqueId Tools_Base. "
      + "Atur tinggi wadah (mis. 300px) agar video kelihatan. Id yang masih main tidak bisa "
      + "dipakai ulang (Stop/Pause dulu). Id yang sama diputar di wadah lain = PINDAH "
      + "(lepas dari wadah lama). Stop lalu Play = mulai dari awal. Double-tap "
      + "mati secara default (nyalakan via DoubleTapSeek bila butuh). Video mengikuti ukuran "
      + "wadah (bisa gepeng bila rasio beda; samakan rasio wadah dengan video). Controller "
      + "bawaan + slider sendiri bisa rebutan: pilih satu. Sukses menembak VideoStarted; "
      + "gagal via ErrorOccurred.")
  public void CallPlayVideoIn(final AndroidViewComponent container,
      final String path, final int id) {
    if (id < 1) {
      Shared.err(this, "Video", id, "id harus >= 1 (pakai CreateVideoId)");
      return;
    }
    if (!Ids.register(Ids.VIDEO, id)) {
      Shared.err(this, "Video", id, "ID " + id + " sedang dipakai video lain");
      return;
    }
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            ViewGroup vg = groupOf(container);
            if (vg == null) {
              Ids.unregister(Ids.VIDEO, id);
              Shared.err(Tools_Video.this, "Video", id, "layout tidak valid");
              return;
            }
            VideoView old;
            synchronized (videoLock) {
              old = views.get(Integer.valueOf(id));
            }
            if (old != null) {
              if (Ids.isActive(Ids.VIDEO, id)) {
                Shared.err(Tools_Video.this, "Video", id, "ID " + id + " masih main "
                    + "(tunggu selesai / Pause / Stop dulu)");
                return;
              }
              try {
                old.stopPlayback();
              } catch (Exception e) {
                // abaikan
              }
              detach(old);
              synchronized (videoLock) {
                if (views.get(Integer.valueOf(id)) == old) {
                  views.remove(Integer.valueOf(id));
                  readies.remove(Integer.valueOf(id));
                }
              }
              Ids.setActive(Ids.VIDEO, id, false);
            }
            VideoView vv;
            synchronized (videoLock) {
              vv = views.get(Integer.valueOf(id));
            }
            if (vv == null) {
              vv = buildView(id);
              synchronized (videoLock) {
                views.put(Integer.valueOf(id), vv);
                readies.put(Integer.valueOf(id), Boolean.valueOf(false));
              }
            }
            final VideoView fVv = vv;
            detach(fVv);
            if (showVideoController) {
              try {
                MediaController mc = new MediaController(activity);
                mc.setAnchorView(fVv);
                fVv.setMediaController(mc);
              } catch (Exception e) {
                // abaikan
              }
            } else {
              try {
                fVv.setMediaController(null);
              } catch (Exception e) {
                // abaikan
              }
            }
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
            vg.addView(fVv, lp);
            synchronized (videoLock) {
              readies.put(Integer.valueOf(id), Boolean.valueOf(false));
            }
            Ids.setActive(Ids.VIDEO, id, false);
            String sv = Shared.clean(path);
            if (sv.length() == 0) {
              Ids.unregister(Ids.VIDEO, id);
              Shared.err(Tools_Video.this, "Video", id, "path kosong");
              return;
            }
            if (sv.regionMatches(true, 0, "http", 0, 4)) {
              fVv.setVideoURI(Uri.parse(sv));
            } else if (sv.startsWith("file://")) {
              File fv = Shared.resolveRooted(sv);
              if (fv == null || !fv.exists()) {
                Ids.unregister(Ids.VIDEO, id);
                Shared.err(Tools_Video.this, "Video", id, "file tidak ada");
                return;
              }
              fVv.setVideoPath(fv.getAbsolutePath());
            } else if (Shared.isAssetPath(sv)) {
              File cache = context.getCacheDir();
              if (!cache.exists()) {
                cache.mkdirs();
              }
              File dst = new File(cache, "vid_" + id + "_"
                  + Shared.sanitize(Shared.fileNameOf(Shared.assetName(sv))));
              InputStream in = context.getAssets().open(Shared.assetName(sv));
              try {
                FileOutputStream out = new FileOutputStream(dst);
                try {
                  Shared.copyStream(in, out);
                } finally {
                  try {
                    out.close();
                  } catch (Exception e) {
                    // abaikan
                  }
                }
              } finally {
                try {
                  in.close();
                } catch (Exception e) {
                  // abaikan
                }
              }
              fVv.setVideoPath(dst.getAbsolutePath());
            } else {
              File f = Shared.resolveRooted(sv);
              if (f == null || !f.exists()) {
                Ids.unregister(Ids.VIDEO, id);
                Shared.err(Tools_Video.this, "Video", id, "file tidak ada");
                return;
              }
              fVv.setVideoPath(f.getAbsolutePath());
            }
            fVv.requestFocus();
            fVv.start();
            updateTickState();
          } catch (Exception e) {
            Ids.unregister(Ids.VIDEO, id);
            String m = e.getMessage();
            Shared.err(Tools_Video.this, "Video", id,
                m == null ? "video gagal diputar" : m);
          }
        }
      });
    } catch (Exception e) {
      Ids.unregister(Ids.VIDEO, id);
      String m = e.getMessage();
      Shared.err(this, "Video", id, m == null ? "video gagal diputar" : m);
      return;
    }
  }

  private VideoView buildView(final int id) {
    final VideoView vv = new VideoView(activity);
    vv.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
      public void onPrepared(MediaPlayer mp) {
        synchronized (videoLock) {
          if (views.get(Integer.valueOf(id)) != vv) {
            return;
          }
          readies.put(Integer.valueOf(id), Boolean.valueOf(true));
        }
        synchronized (videoLock) {
          mps.put(Integer.valueOf(id), mp);
        }
        try {
          mp.setLooping(effLoop(id));
          float v = effVolFloat(id);
          mp.setVolume(v, v);
        } catch (Exception e) {
          // abaikan
        }
        Ids.setActive(Ids.VIDEO, id, true);
        int dur = 0;
        try {
          dur = vv.getDuration();
        } catch (Exception e) {
          // abaikan
        }
        VideoStarted(id, dur);
        updateTickState();
      }
    });
    vv.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
      public void onCompletion(MediaPlayer mp) {
        synchronized (videoLock) {
          if (views.get(Integer.valueOf(id)) != vv) {
            return;
          }
        }
        if (effLoop(id)) {
          try {
            vv.start();
          } catch (Exception e) {
            // abaikan
          }
          return;
        }
        synchronized (videoLock) {
          readies.put(Integer.valueOf(id), Boolean.valueOf(false));
        }
        Ids.setActive(Ids.VIDEO, id, false);
        VideoFinished(id);
        updateTickState();
      }
    });
    vv.setOnErrorListener(new MediaPlayer.OnErrorListener() {
      public boolean onError(MediaPlayer mp, int what, int extra) {
        synchronized (videoLock) {
          if (views.get(Integer.valueOf(id)) == vv) {
            readies.put(Integer.valueOf(id), Boolean.valueOf(false));
          } else {
            return true;
          }
        }
        Ids.setActive(Ids.VIDEO, id, false);
        Shared.err(Tools_Video.this, "Video", id,
            "format tidak didukung / file rusak");
        updateTickState();
        return true;
      }
    });
    final GestureDetector gd = new GestureDetector(activity,
        new GestureDetector.SimpleOnGestureListener() {
          public boolean onDoubleTap(MotionEvent e) {
            if (!doubleTapSeek) {
              return false;
            }
            try {
              float x = e.getX();
              int w = vv.getWidth();
              int skip = skipSeconds * 1000;
              int pos = vv.getCurrentPosition();
              int dur = 0;
              try {
                dur = vv.getDuration();
              } catch (Exception ex) {
                // abaikan
              }
              if (x < w / 3.0f) {
                vv.seekTo(Math.max(0, pos - skip));
              } else if (x > w * 2.0f / 3.0f) {
                if (dur > 0) {
                  vv.seekTo(Math.min(dur, pos + skip));
                } else {
                  vv.seekTo(pos + skip);
                }
              } else {
                if (vv.isPlaying()) {
                  vv.pause();
                  Ids.setActive(Ids.VIDEO, id, false);
                } else {
                  vv.start();
                  Ids.setActive(Ids.VIDEO, id, true);
                }
                updateTickState();
              }
            } catch (Exception ex) {
              // abaikan
            }
            return true;
          }
        });
    vv.setOnTouchListener(new View.OnTouchListener() {
      public boolean onTouch(View v, MotionEvent e) {
        try {
          if (doubleTapSeek) {
            gd.onTouchEvent(e);
          }
        } catch (Exception ex) {
          // abaikan
        }
        return false;
      }
    });
    return vv;
  }

  private VideoView viewOf(int id) {
    synchronized (videoLock) {
      return views.get(Integer.valueOf(id));
    }
  }

  private boolean readyOf(int id) {
    synchronized (videoLock) {
      Boolean r = readies.get(Integer.valueOf(id));
      return r != null && r.booleanValue();
    }
  }

  @SimpleFunction(description = "SET: Jeda video yang sedang main. Tanpa feedback. "
      + "Cek status via IsVideoPaused.")
  public void SetPauseVideo(int id) {
    try {
      VideoView vv = viewOf(id);
      if (vv != null && readyOf(id) && vv.isPlaying()) {
        vv.pause();
        Ids.setActive(Ids.VIDEO, id, false);
        updateTickState();
      } else {
      }
    } catch (Exception e) {
      String m = e.getMessage();
    }
  }

  @SimpleFunction(description = "SET: Lanjutkan video yang dijeda. Tanpa feedback. "
      + "Cek status via IsVideoPaused.")
  public void SetResumeVideo(int id) {
    try {
      VideoView vv = viewOf(id);
      if (vv != null && readyOf(id) && !vv.isPlaying()) {
        vv.start();
        Ids.setActive(Ids.VIDEO, id, true);
        updateTickState();
      } else {
      }
    } catch (Exception e) {
      String m = e.getMessage();
    }
  }

  @SimpleFunction(description = "SET: Hentikan video + lepas dari layout. "
      + "Id tetap tercatat (Play lagi langsung bisa, mulai dari awal). Tanpa event. "
      + "Bersih total via RemoveVideoId.")
  public void SetStopVideo(int id) {
    try {
      VideoView vv = viewOf(id);
      if (vv != null) {
        try {
          vv.stopPlayback();
        } catch (Exception e) {
          // abaikan
        }
        try {
          detach(vv);
        } catch (Exception e) {
          // abaikan
        }
        synchronized (videoLock) {
          views.remove(Integer.valueOf(id));
          readies.remove(Integer.valueOf(id));
          mps.remove(Integer.valueOf(id));
          mutedPrev.remove(Integer.valueOf(id));
        }
        Ids.setActive(Ids.VIDEO, id, false);
        updateTickState();
      } else {
      }
    } catch (Exception e) {
      String m = e.getMessage();
    }
  }

  @SimpleFunction(description = "SET: Lompat video ke posisi milidetik. Bisa dipasang ke "
      + "Slider (matikan controller bawaan bila rebutan). Tanpa feedback.")
  public void SetSeekVideo(int id, int position) {
    try {
      VideoView vv = viewOf(id);
      if (vv != null && readyOf(id)) {
        vv.seekTo(position < 0 ? 0 : position);
      } else {
      }
    } catch (Exception e) {
      String m = e.getMessage();
    }
  }

  private void seekBySeconds(int id, int seconds) {
    try {
      VideoView vv = viewOf(id);
      if (vv != null && readyOf(id)) {
        int pos = vv.getCurrentPosition();
        int dur = 0;
        try {
          dur = vv.getDuration();
        } catch (Exception e) {
          // abaikan
        }
        int np = pos + seconds * 1000;
        if (np < 0) {
          np = 0;
        }
        if (dur > 0 && np > dur) {
          np = dur;
        }
        vv.seekTo(np);
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "SET: Maju video sejauh SkipSeconds DETIK. "
      + "Tanpa feedback.")
  public void SetForwardVideo(int id) {
    seekBySeconds(id, skipSeconds);
  }

  @SimpleFunction(description = "SET: Mundur video sejauh SkipSeconds DETIK. "
      + "Tanpa feedback.")
  public void SetBackwardVideo(int id) {
    seekBySeconds(id, -skipSeconds);
  }

  @SimpleFunction(description = "Posisi video sekarang dalam milidetik. 0 bila belum siap.")
  public int GetVideoPosition(int id) {
    try {
      VideoView vv = viewOf(id);
      if (vv != null && readyOf(id)) {
        return vv.getCurrentPosition();
      }
    } catch (Exception e) {
      // abaikan
    }
    return 0;
  }

  @SimpleFunction(description = "Durasi video dalam milidetik. 0 bila belum siap.")
  public int GetVideoDuration(int id) {
    try {
      VideoView vv = viewOf(id);
      if (vv != null && readyOf(id)) {
        int d = vv.getDuration();
        return d < 0 ? 0 : d;
      }
    } catch (Exception e) {
      // abaikan
    }
    return 0;
  }

  @SimpleFunction(description = "SET: Hapus SEMUA video Tools_Video dari layout itu (id ikut dilepas, "
      + "komponen lain tidak diusik). Video ikut di-pause.")
  public void SetClearVideo(final AndroidViewComponent container) {
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            ViewGroup vg = groupOf(container);
            if (vg == null) {
              Shared.err(Tools_Video.this, "Video", 0, "layout tidak valid");
              return;
            }
            ArrayList<Integer> gone = new ArrayList<Integer>();
            synchronized (videoLock) {
              for (Map.Entry<Integer, VideoView> en : views.entrySet()) {
                VideoView vv = en.getValue();
                if (vv != null && vv.getParent() == vg) {
                  try {
                    if (vv.isPlaying()) {
                      vv.pause();
                    }
                  } catch (Exception e) {
                    // abaikan
                  }
                  try {
                    vg.removeView(vv);
                  } catch (Exception e) {
                    // abaikan
                  }
                  gone.add(en.getKey());
                }
              }
              for (int i = 0; i < gone.size(); i = i + 1) {
                int gid = gone.get(i).intValue();
                views.remove(Integer.valueOf(gid));
                readies.remove(Integer.valueOf(gid));
                mps.remove(Integer.valueOf(gid));
                mutedPrev.remove(Integer.valueOf(gid));
                Ids.setActive(Ids.VIDEO, gid, false);
                Ids.unregister(Ids.VIDEO, gid);
              }
            }
            updateTickState();
          } catch (Exception e) {
            String m = e.getMessage();
            Shared.err(Tools_Video.this, "Video", 0,
                m == null ? "clear gagal" : m);
          }
        }
      });
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Video", 0, m == null ? "clear gagal" : m);
    }
  }

  /** Hentikan + lepas satu video ber-id. Dipakai komponen + Tools_Base. */
  public static void stopStatic(int id) {
    VideoView vv;
    synchronized (videoLock) {
      vv = views.remove(Integer.valueOf(id));
      readies.remove(Integer.valueOf(id));
      mps.remove(Integer.valueOf(id));
      mutedPrev.remove(Integer.valueOf(id));
    }
    if (vv != null) {
      try {
        try {
          if (vv.isPlaying()) {
            vv.pause();
          }
        } catch (Exception e) {
          // abaikan
        }
        try {
          vv.stopPlayback();
        } catch (Exception e) {
          // abaikan
        }
        try {
          if (vv.getParent() instanceof ViewGroup) {
            ((ViewGroup) vv.getParent()).removeView(vv);
          }
        } catch (Exception e) {
          // abaikan
        }
      } catch (Exception e) {
        // abaikan
      }
    }
    Ids.setActive(Ids.VIDEO, id, false);
    Ids.unregister(Ids.VIDEO, id);
  }

  // ================= ID =================

  @SimpleFunction(description = "Buat id video unik baru (Number, siap pakai ke CallPlayVideoIn).")
  public int CreateVideoId() {
    int id = Ids.createFor(Ids.VIDEO);
    if (id < 1) {
      Shared.err(this, "Video", 0, "gagal buat id");
    }
    return id;
  }

  @SimpleFunction(description = "Semua id video yang dipegang (URUT PEMBUATAN, "
      + "index 1 = pertama dibuat).")
  public List<Integer> GetVideoIds() {
    return Ids.idsOf(Ids.VIDEO);
  }

  @SimpleFunction(description = "Id video yang sedang main (URUT PEMBUATAN). "
      + "Yang dijeda/berhenti TIDAK masuk sini.")
  public List<Integer> GetActiveVideoIds() {
    return Ids.activeIdsOf(Ids.VIDEO);
  }

  @SimpleFunction(description = "Id video yang terakhir disentuh (dibuat atau di-start). "
      + "-1 bila kosong. Beda dengan GetLastVideoId (terakhir dibuat).")
  public int CurrentVideoId() {
    return Ids.currentOf(Ids.VIDEO);
  }

  @SimpleFunction(description = "Id index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstVideoId() {
    return Ids.firstOf(Ids.VIDEO);
  }

  @SimpleFunction(description = "Id video TERAKHIR DIBUAT. -1 bila kosong. Beda dengan "
      + "CurrentVideoId (terakhir disentuh).")
  public int GetLastVideoId() {
    return Ids.lastOf(Ids.VIDEO);
  }

  @SimpleFunction(description = "Id yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.")
  public int GetPreviousVideoId(int id) {
    return Ids.previousOf(Ids.VIDEO, id);
  }

  @SimpleFunction(description = "True bila id video ini sedang main. False = jeda, "
      + "berhenti, atau belum ada (bedakan jeda vs berhenti via IsVideoPaused).")
  public boolean IsVideoIdActive(int id) {
    return Ids.isActive(Ids.VIDEO, id);
  }

  @SimpleFunction(description = "True bila id video ini sedang dijeda (Pause tapi "
      + "belum Stop/selesai). False = main/berhenti/belum ada.")
  public boolean IsVideoPaused(int id) {
    try {
      VideoView vv = viewOf(id);
      return vv != null && readyOf(id) && !vv.isPlaying();
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleFunction(description = "Volume khusus satu id (0-100). = volume global "
      + "bila belum diatur via SetVideoVolume.")
  public int GetVideoVolume(int id) {
    synchronized (videoLock) {
      Integer o = volumes.get(Integer.valueOf(id));
      if (o != null) {
        return o.intValue();
      }
    }
    return mediaVolume;
  }

  @SimpleFunction(description = "SET: Volume khusus satu id (0-100). Menimpa "
      + "volume global untuk id ini. Tanpa feedback.")
  public void SetVideoVolume(int id, int volume) {
    if (volume < 0) {
      volume = 0;
    }
    if (volume > 100) {
      volume = 100;
    }
    synchronized (videoLock) {
      volumes.put(Integer.valueOf(id), Integer.valueOf(volume));
      mutedPrev.remove(Integer.valueOf(id));
    }
    applyVolId(id, volume);
  }

  @SimpleFunction(description = "Loop khusus satu id. = loop global bila belum "
      + "diatur via SetVideoLooping.")
  public boolean GetVideoLooping(int id) {
    synchronized (videoLock) {
      Boolean o = loops.get(Integer.valueOf(id));
      if (o != null) {
        return o.booleanValue();
      }
    }
    return videoLooping;
  }

  @SimpleFunction(description = "SET: Loop khusus satu id (true = mengulang bila "
      + "selesai). Menimpa loop global untuk id ini. Berlaku untuk putaran "
      + "berikutnya. Tanpa feedback.")
  public void SetVideoLooping(int id, boolean loop) {
    synchronized (videoLock) {
      loops.put(Integer.valueOf(id), Boolean.valueOf(loop));
    }
  }

  @SimpleFunction(description = "True bila id video ini sedang dibisukan via "
      + "SetVideoMuted.")
  public boolean IsVideoMuted(int id) {
    synchronized (videoLock) {
      return mutedPrev.containsKey(Integer.valueOf(id));
    }
  }

  @SimpleFunction(description = "SET: Bisukan/suarakan satu id. Bisu = volume 0 "
      + "sementara (nilai lama disimpan, pulih saat unmute). Tanpa feedback.")
  public void SetVideoMuted(int id, boolean muted) {
    if (muted) {
      synchronized (videoLock) {
        if (!mutedPrev.containsKey(Integer.valueOf(id))) {
          mutedPrev.put(Integer.valueOf(id), Integer.valueOf(effVolInt(id)));
        }
      }
      applyVolId(id, 0);
    } else {
      Integer p;
      synchronized (videoLock) {
        p = mutedPrev.remove(Integer.valueOf(id));
      }
      applyVolId(id, p == null ? effVolInt(id) : p.intValue());
    }
  }

  @SimpleFunction(description = "SET: Maju video sejauh seconds DETIK "
      + "(tanpa ubah SkipSeconds). Tanpa feedback.")
  public void SetForwardVideoBy(int id, int seconds) {
    seekBySeconds(id, seconds);
  }

  @SimpleFunction(description = "SET: Mundur video sejauh seconds DETIK "
      + "(tanpa ubah SkipSeconds). Tanpa feedback.")
  public void SetBackwardVideoBy(int id, int seconds) {
    seekBySeconds(id, -seconds);
  }

  @SimpleFunction(description = "SET: Hapus SEMUA video (semua layout) + buang "
      + "semua id. Tanpa feedback.")
  public void SetClearVideos() {
    try {
      List<Integer> ids = Ids.idsOf(Ids.VIDEO);
      for (int i = 0; i < ids.size(); i = i + 1) {
        try {
          stopStatic(ids.get(i).intValue());
        } catch (Exception e) {
          // abaikan per-id
        }
      }
      updateTickState();
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "Hapus id video: hentikan + lepas dari layout + lepas id "
      + "(otomatis beres-beres).")
  public void RemoveVideoId(int id) {
    stopStatic(id);
    updateTickState();
  }

  // ================= pengaturan =================

  @SimpleProperty(description = "Jeda event VideoProgressChanged dalam milidetik (default 500). "
      + "Lagu 3 menit = 360 event; HP kentang naikkan ke 1000.",
      category = PropertyCategory.BEHAVIOR)
  public int ProgressInterval() {
    return progressInterval;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER,
      defaultValue = "500")
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void ProgressInterval(int interval) {
    progressInterval = interval <= 0 ? 500 : interval;
    updateTickState();
  }

  @SimpleProperty(description = "Lompatan Maju/Mundur + double-tap dalam DETIK (bukan ms). "
      + "Default 10. Untuk lompatan sekali-sekali pakai SetForwardVideoBy/SetBackwardVideoBy.",
      category = PropertyCategory.BEHAVIOR)
  public int SkipSeconds() {
    return skipSeconds;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER,
      defaultValue = "10")
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void SkipSeconds(int seconds) {
    skipSeconds = seconds <= 0 ? 10 : seconds;
  }

  @SimpleProperty(description = "Tampilkan kontrol bawaan video (ada tombol play + geser bawaan). "
      + "Default true. Matikan bila pakai slider sendiri (bisa rebutan).",
      category = PropertyCategory.BEHAVIOR)
  public boolean ShowVideoController() {
    return showVideoController;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
      defaultValue = "True")
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void ShowVideoController(boolean show) {
    showVideoController = show;
  }

  @SimpleProperty(description = "Loop global (default false). Id yang sudah diatur via "
      + "SetVideoLooping memakai nilainya sendiri.",
      category = PropertyCategory.BEHAVIOR)
  public boolean VideoLooping() {
    return videoLooping;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
      defaultValue = "False")
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void VideoLooping(boolean loop) {
    videoLooping = loop;
  }

  @SimpleProperty(description = "Volume global 0-100 (default 100, awal semua id). Id yang "
      + "sudah diatur via SetVideoVolume memakai nilainya sendiri. Bisu sementara via "
      + "SetVideoMuted.",
      category = PropertyCategory.BEHAVIOR)
  public int MediaVolume() {
    return mediaVolume;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER,
      defaultValue = "100")
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void MediaVolume(int volume) {
    if (volume < 0) {
      volume = 0;
    }
    if (volume > 100) {
      volume = 100;
    }
    mediaVolume = volume;
    try {
      ArrayList<Integer> keys;
      synchronized (videoLock) {
        keys = new ArrayList<Integer>(mps.keySet());
      }
      for (int i = 0; i < keys.size(); i = i + 1) {
        int vid = keys.get(i).intValue();
        Integer mo;
        synchronized (videoLock) {
          mo = mutedPrev.get(Integer.valueOf(vid));
        }
        if (mo == null) {
          applyVolId(vid, effVolInt(vid));
        }
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleProperty(description = "Double-tap video: kiri = mundur, kanan = maju, "
      + "tengah = pause/resume. Default false (matikan perilaku tak terduga; "
      + "nyalakan bila butuh).",
      category = PropertyCategory.BEHAVIOR)
  public boolean DoubleTapSeek() {
    return doubleTapSeek;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
      defaultValue = "False")
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void DoubleTapSeek(boolean enabled) {
    doubleTapSeek = enabled;
  }

  // ================= events =================

  @SimpleEvent(description = "Video mulai main: id + durasi (ms) diketahui.")
  public void VideoStarted(int id, int duration) {
    Shared.fire(this, "VideoStarted",
        Integer.valueOf(id), Integer.valueOf(duration));
  }

  @SimpleEvent(description = "Progres video berubah: id + posisi + durasi (ms) + persen 0-100.")
  public void VideoProgressChanged(int id, int position, int duration, int percent) {
    Shared.fire(this, "VideoProgressChanged",
        Integer.valueOf(id), Integer.valueOf(position), Integer.valueOf(duration),
        Integer.valueOf(percent));
  }

  @SimpleEvent(description = "Video selesai (tidak loop). Putar lagi via CallPlayVideoIn.")
  public void VideoFinished(int id) {
    Shared.fire(this, "VideoFinished", Integer.valueOf(id));
  }

  @SimpleEvent(description = "SATU-SATUNYA error Tools_Video. component = nama operasi "
      + "(isi dengan OpVideo untuk if). Contoh: jika component = OpVideo "
      + "maka tampilkan message.")
  public void ErrorOccurred(String component, int id, String message) {
    Shared.fire(this, "ErrorOccurred",
        component, Integer.valueOf(id), message);
  }

  @SimpleProperty(description = "Nama operasi Video untuk if di ErrorOccurred. Contoh: "
      + "colok ke perbandingan = dengan component dari event ErrorOccurred.",
      category = PropertyCategory.BEHAVIOR)
  public String OpVideo() {
    return "Video";
  }

  @SimpleFunction(description = "Fallback polling untuk event ErrorOccurred: pesan error "
      + "terakhir operasi ini (teks kosong bila belum ada). Isi operation dengan blok OpVideo.")
  public String LastError(String operation) {
    return Shared.lastError(operation);
  }

  // ================= destroy =================

  public void onDestroy() {
    try {
      if (mediaTicks != null) {
        mediaTicks.removeCallbacks(mediaTick);
      }
    } catch (Exception e) {
      // abaikan
    }
    try {
      ArrayList<Integer> keys;
      synchronized (videoLock) {
        keys = new ArrayList<Integer>(views.keySet());
      }
      for (int i = 0; i < keys.size(); i = i + 1) {
        stopStatic(keys.get(i).intValue());
      }
    } catch (Exception e) {
      // abaikan
    }
  }
}
