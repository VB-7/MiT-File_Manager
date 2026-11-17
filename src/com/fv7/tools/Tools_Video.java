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

@DesignerComponent(
    version = 1,
    description = "Tools_Video: putar BANYAK video ber-id di dalam layout (Arrangement MIT / dinamis), "
        + "kontrol ala VLC (pause/resume/seek/maju/mundur/double-tap), loop, volume. "
        + "Satu id = satu video, bisa jalan bareng. Id dikelola CreateVideoId / Tools_Base. "
        + "List id URUT PEMBUATAN.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    androidMinSdk = 21,
    iconName = "aiwebres/tools_video.jpg")
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
  private boolean doubleTapSeek = true;

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

  private float volFloat() {
    int v = mediaVolume;
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

  @SimpleFunction(description = "Putar video ber-id di dalam layout (Arrangement MIT / dinamis). "
      + "Bisa jalan bareng (satu id = satu video). path file (/Download/..., absolut "
      + "apa adanya), atau http(s) URL (streaming). id dari CreateVideoId / "
      + "Tools_Base CreateUniqueId. "
      + "Atur tinggi arrangement (mis. 300px) agar video kelihatan. "
      + "Double-tap: kiri = mundur, kanan = maju, tengah = pause/resume. "
      + "Sukses menembak VideoStarted; gagal via ErrorOccurred.")
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
        try {
          mp.setLooping(videoLooping);
          float v = volFloat();
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
        if (videoLooping) {
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

  @SimpleFunction(description = "SET: Jeda video yang sedang main. (per id)")
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

  @SimpleFunction(description = "SET: Lanjutkan video yang dijeda. (per id)")
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

  @SimpleFunction(description = "SET: Hentikan video + lepas dari layout (per id). "
      + "Id tetap tercatat (Play lagi langsung bisa). Bersih total via RemoveVideoId.")
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
        }
        Ids.setActive(Ids.VIDEO, id, false);
        updateTickState();
      } else {
      }
    } catch (Exception e) {
      String m = e.getMessage();
    }
  }

  @SimpleFunction(description = "SET: Lompat video ke posisi (ms). Bisa dipasang ke Slider. (per id)")
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

  @SimpleFunction(description = "SET: Maju video sejauh SkipSeconds detik. (per id)")
  public void SetForwardVideo(int id) {
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
        int skip = skipSeconds * 1000;
        if (dur > 0) {
          vv.seekTo(Math.min(dur, pos + skip));
        } else {
          vv.seekTo(pos + skip);
        }
      } else {
      }
    } catch (Exception e) {
      String m = e.getMessage();
    }
  }

  @SimpleFunction(description = "SET: Mundur video sejauh SkipSeconds detik. (per id)")
  public void SetBackwardVideo(int id) {
    try {
      VideoView vv = viewOf(id);
      if (vv != null && readyOf(id)) {
        int pos = vv.getCurrentPosition();
        vv.seekTo(Math.max(0, pos - skipSeconds * 1000));
      } else {
      }
    } catch (Exception e) {
      String m = e.getMessage();
    }
  }

  @SimpleFunction(description = "Posisi video sekarang (ms). (per id)")
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

  @SimpleFunction(description = "Durasi video (ms). 0 bila belum siap. (per id)")
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

  @SimpleFunction(description = "True bila video id ini sedang main.")
  public boolean IsVideoPlaying(int id) {
    try {
      VideoView vv = viewOf(id);
      return vv != null && readyOf(id) && vv.isPlaying();
    } catch (Exception e) {
      return false;
    }
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

  @SimpleFunction(description = "Id video yang terakhir dibuat/di-start. -1 bila kosong.")
  public int CurrentVideoId() {
    return Ids.currentOf(Ids.VIDEO);
  }

  @SimpleFunction(description = "Id index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstVideoId() {
    return Ids.firstOf(Ids.VIDEO);
  }

  @SimpleFunction(description = "Id TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastVideoId() {
    return Ids.lastOf(Ids.VIDEO);
  }

  @SimpleFunction(description = "Id yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.")
  public int GetPreviousVideoId(int id) {
    return Ids.previousOf(Ids.VIDEO, id);
  }

  @SimpleFunction(description = "True bila id video ini sedang main.")
  public boolean IsVideoIdActive(int id) {
    return Ids.isActive(Ids.VIDEO, id);
  }

  @SimpleFunction(description = "Hapus id video: hentikan + lepas dari layout + lepas id "
      + "(otomatis beres-beres).")
  public void RemoveVideoId(int id) {
    stopStatic(id);
    updateTickState();
  }

  // ================= pengaturan =================

  @SimpleProperty(description = "Jeda event VideoProgressChanged dalam milidetik (default 500).",
      category = PropertyCategory.BEHAVIOR)
  public int ProgressInterval() {
    return progressInterval;
  }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void ProgressInterval(int interval) {
    progressInterval = interval <= 0 ? 500 : interval;
    updateTickState();
  }

  @SimpleProperty(description = "Lompatan Maju/Mundur + double-tap dalam detik (default 10).",
      category = PropertyCategory.BEHAVIOR)
  public int SkipSeconds() {
    return skipSeconds;
  }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void SkipSeconds(int seconds) {
    skipSeconds = seconds <= 0 ? 10 : seconds;
  }

  @SimpleProperty(description = "Tampilkan kontrol bawaan video. Default true.",
      category = PropertyCategory.BEHAVIOR)
  public boolean ShowVideoController() {
    return showVideoController;
  }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void ShowVideoController(boolean show) {
    showVideoController = show;
  }

  @SimpleProperty(description = "Video mengulang otomatis bila selesai. Default false.",
      category = PropertyCategory.BEHAVIOR)
  public boolean VideoLooping() {
    return videoLooping;
  }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void VideoLooping(boolean loop) {
    videoLooping = loop;
  }

  @SimpleProperty(description = "Volume video 0-100 (default 100).",
      category = PropertyCategory.BEHAVIOR)
  public int MediaVolume() {
    return mediaVolume;
  }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void MediaVolume(int volume) {
    if (volume < 0) {
      volume = 0;
    }
    if (volume > 100) {
      volume = 100;
    }
    mediaVolume = volume;
  }

  @SimpleProperty(description = "Double-tap video: kiri = mundur, kanan = maju, "
      + "tengah = pause/resume. Default true.",
      category = PropertyCategory.BEHAVIOR)
  public boolean DoubleTapSeek() {
    return doubleTapSeek;
  }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void DoubleTapSeek(boolean enabled) {
    doubleTapSeek = enabled;
  }

  // ================= events =================

  @SimpleEvent(description = "Video mulai main: id + durasi (ms) diketahui.")
  public void VideoStarted(int id, int duration) {
    EventDispatcher.dispatchEvent(this, "VideoStarted",
        Integer.valueOf(id), Integer.valueOf(duration));
  }

  @SimpleEvent(description = "Progres video berubah: id + posisi + durasi (ms) + persen 0-100.")
  public void VideoProgressChanged(int id, int position, int duration, int percent) {
    EventDispatcher.dispatchEvent(this, "VideoProgressChanged",
        Integer.valueOf(id), Integer.valueOf(position), Integer.valueOf(duration),
        Integer.valueOf(percent));
  }

  @SimpleEvent(description = "Video selesai (tidak loop). Putar lagi via CallPlayVideoIn.")
  public void VideoFinished(int id) {
    EventDispatcher.dispatchEvent(this, "VideoFinished", Integer.valueOf(id));
  }

  @SimpleEvent(description = "SATU-SATUNYA error Tools_Video.")
  public void ErrorOccurred(String component, int id, String message) {
    EventDispatcher.dispatchEvent(this, "ErrorOccurred",
        component, Integer.valueOf(id), message);
  }

  @SimpleProperty(description = "Nama operasi Video (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpVideo() {
    return "Video";
  }

  @SimpleFunction(description = "Fallback polling untuk event ErrorOccurred: "
      + "pesan error terakhir operasi ini (kosong bila belum ada).")
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
