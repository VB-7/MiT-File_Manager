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
import com.google.appinventor.components.common.FileScope;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.AndroidViewComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.OnDestroyListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

@DesignerComponent(
    version = 1,
    description = "Tools_Video: putar video di dalam layout (Arrangement MIT / dinamis), "
        + "kontrol ala VLC (pause/resume/seek/maju/mundur/double-tap), loop, volume. "
        + "Satu video per saat (Play baru hentikan yang lama). Tanpa id.",
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

  private VideoView videoView = null;
  private MediaController videoController = null;
  private boolean videoReadyFlag = false;
  private Handler mediaTicks = null;

  private final Runnable mediaTick = new Runnable() {
    public void run() {
      try {
        if (videoView != null && videoReadyFlag && videoView.isPlaying()) {
          int pos = videoView.getCurrentPosition();
          int dur = videoView.getDuration();
          int pct = dur > 0 ? ((int) (pos * 100L / dur)) : 0;
          VideoProgressChanged(pos, dur, pct);
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
        if (videoView != null && videoReadyFlag && videoView.isPlaying()) {
          need = true;
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

  @SimpleFunction(description = "Putar video di dalam layout (Arrangement MIT / dinamis). "
      + "path bisa file scope, file:/// absolut, atau http(s) URL (streaming). "
      + "Atur tinggi arrangement (mis. 300px) agar video kelihatan. "
      + "Double-tap: kiri = mundur, kanan = maju, tengah = pause/resume.")
  public void PlayVideoIn(final AndroidViewComponent container, final FileScope scope,
      final String path) {
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            ViewGroup vg = groupOf(container);
            if (vg == null) {
              Shared.err(Tools_Video.this, "Video", 0, "layout tidak valid");
              return;
            }
            if (videoView == null) {
              videoView = new VideoView(activity);
              videoView.setOnPreparedListener(
                  new MediaPlayer.OnPreparedListener() {
                    public void onPrepared(MediaPlayer mp) {
                      try {
                        mp.setLooping(videoLooping);
                        float v = volFloat();
                        mp.setVolume(v, v);
                      } catch (Exception e) {
                        // abaikan
                      }
                      videoReadyFlag = true;
                      int dur = 0;
                      try {
                        dur = videoView.getDuration();
                      } catch (Exception e) {
                        // abaikan
                      }
                      VideoStarted(dur);
                      updateTickState();
                    }
                  });
              videoView.setOnCompletionListener(
                  new MediaPlayer.OnCompletionListener() {
                    public void onCompletion(MediaPlayer mp) {
                      if (videoLooping) {
                        try {
                          videoView.start();
                        } catch (Exception e) {
                          // abaikan
                        }
                        return;
                      }
                      videoReadyFlag = false;
                      VideoFinished();
                      updateTickState();
                    }
                  });
              videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                public boolean onError(MediaPlayer mp, int what, int extra) {
                  videoReadyFlag = false;
                  Shared.err(Tools_Video.this, "Video", 0,
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
                        int w = videoView.getWidth();
                        int skip = skipSeconds * 1000;
                        int pos = videoView.getCurrentPosition();
                        int dur = 0;
                        try {
                          dur = videoView.getDuration();
                        } catch (Exception ex) {
                          // abaikan
                        }
                        if (x < w / 3.0f) {
                          videoView.seekTo(Math.max(0, pos - skip));
                        } else if (x > w * 2.0f / 3.0f) {
                          if (dur > 0) {
                            videoView.seekTo(Math.min(dur, pos + skip));
                          } else {
                            videoView.seekTo(pos + skip);
                          }
                        } else {
                          if (videoView.isPlaying()) {
                            videoView.pause();
                          } else {
                            videoView.start();
                          }
                          updateTickState();
                        }
                      } catch (Exception ex) {
                        // abaikan
                      }
                      return true;
                    }
                  });
              videoView.setOnTouchListener(new View.OnTouchListener() {
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
            }
            detach(videoView);
            if (showVideoController) {
              if (videoController == null) {
                videoController = new MediaController(activity);
              }
              videoController.setAnchorView(videoView);
              videoView.setMediaController(videoController);
            } else {
              videoView.setMediaController(null);
            }
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
            vg.addView(videoView, lp);
            videoReadyFlag = false;
            String sv = Shared.clean(path);
            if (sv.length() == 0) {
              Shared.err(Tools_Video.this, "Video", 0, "path kosong");
              return;
            }
            if (sv.regionMatches(true, 0, "http", 0, 4)) {
              videoView.setVideoURI(Uri.parse(sv));
            } else if (sv.startsWith("file://")) {
              videoView.setVideoPath(Shared.stripLeadingSlash(sv.substring(7)));
            } else if (scope == FileScope.Asset || Shared.isAssetPath(sv)) {
              File cache = context.getCacheDir();
              if (!cache.exists()) {
                cache.mkdirs();
              }
              File dst = new File(cache, "vid_"
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
              videoView.setVideoPath(dst.getAbsolutePath());
            } else {
              File f = Shared.resolve(context, scope, sv);
              if (f == null || !f.exists()) {
                Shared.err(Tools_Video.this, "Video", 0, "file tidak ada");
                return;
              }
              videoView.setVideoPath(f.getAbsolutePath());
            }
            videoView.requestFocus();
            videoView.start();
            updateTickState();
          } catch (Exception e) {
            String m = e.getMessage();
            Shared.err(Tools_Video.this, "Video", 0,
                m == null ? "video gagal diputar" : m);
          }
        }
      });
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Video", 0, m == null ? "video gagal diputar" : m);
    }
  }

  @SimpleFunction(description = "Jeda video yang sedang main.")
  public void PauseVideo() {
    try {
      if (videoView != null && videoReadyFlag && videoView.isPlaying()) {
        videoView.pause();
        updateTickState();
      } else {
        Shared.err(this, "Video", 0, "tidak ada video yang main");
      }
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Video", 0, m == null ? "pause gagal" : m);
    }
  }

  @SimpleFunction(description = "Lanjutkan video yang dijeda.")
  public void ResumeVideo() {
    try {
      if (videoView != null && videoReadyFlag && !videoView.isPlaying()) {
        videoView.start();
        updateTickState();
      } else {
        Shared.err(this, "Video", 0, "tidak ada video yang dijeda");
      }
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Video", 0, m == null ? "resume gagal" : m);
    }
  }

  @SimpleFunction(description = "Hentikan video (putar lagi via PlayVideoIn).")
  public void StopVideo() {
    try {
      if (videoView != null) {
        try {
          videoView.stopPlayback();
        } catch (Exception e) {
          // abaikan
        }
        videoReadyFlag = false;
        updateTickState();
      } else {
        Shared.err(this, "Video", 0, "tidak ada video");
      }
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Video", 0, m == null ? "stop gagal" : m);
    }
  }

  @SimpleFunction(description = "Lompat video ke posisi (ms). Bisa dipasang ke Slider.")
  public void SeekVideo(int position) {
    try {
      if (videoView != null && videoReadyFlag) {
        videoView.seekTo(position < 0 ? 0 : position);
      } else {
        Shared.err(this, "Video", 0, "tidak ada video yang main");
      }
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Video", 0, m == null ? "seek gagal" : m);
    }
  }

  @SimpleFunction(description = "Maju video sejauh SkipSeconds detik.")
  public void ForwardVideo() {
    try {
      if (videoView != null && videoReadyFlag) {
        int pos = videoView.getCurrentPosition();
        int dur = 0;
        try {
          dur = videoView.getDuration();
        } catch (Exception e) {
          // abaikan
        }
        int skip = skipSeconds * 1000;
        if (dur > 0) {
          videoView.seekTo(Math.min(dur, pos + skip));
        } else {
          videoView.seekTo(pos + skip);
        }
      } else {
        Shared.err(this, "Video", 0, "tidak ada video yang main");
      }
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Video", 0, m == null ? "maju gagal" : m);
    }
  }

  @SimpleFunction(description = "Mundur video sejauh SkipSeconds detik.")
  public void BackwardVideo() {
    try {
      if (videoView != null && videoReadyFlag) {
        int pos = videoView.getCurrentPosition();
        videoView.seekTo(Math.max(0, pos - skipSeconds * 1000));
      } else {
        Shared.err(this, "Video", 0, "tidak ada video yang main");
      }
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Video", 0, m == null ? "mundur gagal" : m);
    }
  }

  @SimpleFunction(description = "Posisi video sekarang (ms).")
  public int VideoPosition() {
    try {
      if (videoView != null && videoReadyFlag) {
        return videoView.getCurrentPosition();
      }
    } catch (Exception e) {
      // abaikan
    }
    return 0;
  }

  @SimpleFunction(description = "Durasi video (ms). 0 bila belum siap.")
  public int VideoDuration() {
    try {
      if (videoView != null && videoReadyFlag) {
        int d = videoView.getDuration();
        return d < 0 ? 0 : d;
      }
    } catch (Exception e) {
      // abaikan
    }
    return 0;
  }

  @SimpleFunction(description = "True bila video sedang main.")
  public boolean IsVideoPlaying() {
    try {
      return videoView != null && videoReadyFlag && videoView.isPlaying();
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleFunction(description = "Hapus video dari layout itu (komponen lain tidak diusik). "
      + "Video ikut di-pause.")
  public void ClearVideo(final AndroidViewComponent container) {
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            ViewGroup vg = groupOf(container);
            if (vg == null) {
              Shared.err(Tools_Video.this, "Video", 0, "layout tidak valid");
              return;
            }
            if (videoView != null && videoView.getParent() == vg) {
              try {
                if (videoView.isPlaying()) {
                  videoView.pause();
                }
              } catch (Exception e) {
                // abaikan
              }
              updateTickState();
              vg.removeView(videoView);
            }
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

  @SimpleEvent(description = "Video mulai main: persiapan selesai, durasi (ms) diketahui.")
  public void VideoStarted(int duration) {
    EventDispatcher.dispatchEvent(this, "VideoStarted",
        Integer.valueOf(duration));
  }

  @SimpleEvent(description = "Progres video berubah: posisi + durasi (ms) + persen 0-100.")
  public void VideoProgressChanged(int position, int duration, int percent) {
    EventDispatcher.dispatchEvent(this, "VideoProgressChanged",
        Integer.valueOf(position), Integer.valueOf(duration),
        Integer.valueOf(percent));
  }

  @SimpleEvent(description = "Video selesai (tidak loop). Putar lagi via PlayVideoIn.")
  public void VideoFinished() {
    EventDispatcher.dispatchEvent(this, "VideoFinished");
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
      if (videoView != null) {
        try {
          videoView.stopPlayback();
        } catch (Exception e) {
          // abaikan
        }
        detach(videoView);
        videoView = null;
      }
    } catch (Exception e) {
      // abaikan
    }
    videoReadyFlag = false;
  }
}
