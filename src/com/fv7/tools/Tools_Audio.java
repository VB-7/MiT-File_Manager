package com.fv7.tools;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.FileScope;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.OnDestroyListener;

import java.io.File;

@DesignerComponent(
    version = 1,
    description = "Tools_Audio: putar audio background (tanpa tampilan), kontrol "
        + "pause/resume/seek/maju/mundur, loop, volume. Satu audio per saat (Play baru "
        + "hentikan yang lama). Tampilan (Slider/Label) bikin sendiri dari event. "
        + "Tanpa id.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    androidMinSdk = 21,
    iconName = "aiwebres/tools_audio.jpg")
@SimpleObject(external = true)
public class Tools_Audio extends AndroidNonvisibleComponent
    implements OnDestroyListener {

  private final Context context;
  private final Activity activity;

  private int progressInterval = 500;
  private int skipSeconds = 10;
  private boolean audioLooping = false;
  private int mediaVolume = 100;

  private MediaPlayer audioPlayer = null;
  private boolean audioReady = false;
  private Handler mediaTicks = null;

  private final Runnable mediaTick = new Runnable() {
    public void run() {
      try {
        if (audioPlayer != null && audioReady && audioPlayer.isPlaying()) {
          int pos = audioPlayer.getCurrentPosition();
          int dur = audioPlayer.getDuration();
          int pct = dur > 0 ? ((int) (pos * 100L / dur)) : 0;
          AudioProgressChanged(pos, dur, pct);
        }
      } catch (Exception e) {
        // abaikan
      }
      updateTickState();
    }
  };

  public Tools_Audio(ComponentContainer container) {
    super(container.$form());
    this.context = container.$context();
    this.activity = container.$context();
  }

  private void updateTickState() {
    try {
      boolean need = false;
      try {
        if (audioPlayer != null && audioReady && audioPlayer.isPlaying()) {
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

  private void stopInternal() {
    try {
      if (audioPlayer != null) {
        try {
          audioPlayer.stop();
        } catch (Exception e) {
          // abaikan
        }
        try {
          audioPlayer.reset();
        } catch (Exception e) {
          // abaikan
        }
        try {
          audioPlayer.release();
        } catch (Exception e) {
          // abaikan
        }
        audioPlayer = null;
      }
    } catch (Exception e) {
      // abaikan
    }
    audioReady = false;
  }

  @SimpleFunction(description = "Putar audio (background, tanpa tampilan). path bisa file scope, "
      + "file:/// absolut, atau http(s) URL (streaming). Tampilan (Slider/Label) bikin "
      + "sendiri dari AudioStarted/ProgressChanged/Finished.")
  public void PlayAudio(final FileScope scope, final String path) {
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            stopInternal();
            final MediaPlayer mp = new MediaPlayer();
            String sa = Shared.clean(path);
            if (sa.length() == 0) {
              Shared.err(Tools_Audio.this, "Audio", 0, "path kosong");
              return;
            }
            if (sa.regionMatches(true, 0, "http", 0, 4)) {
              mp.setDataSource(sa);
            } else if (sa.startsWith("file://")) {
              mp.setDataSource(Shared.stripLeadingSlash(sa.substring(7)));
            } else if (scope == FileScope.Asset || Shared.isAssetPath(sa)) {
              android.content.res.AssetFileDescriptor afd =
                  context.getAssets().openFd(Shared.assetName(sa));
              try {
                mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),
                    afd.getLength());
              } finally {
                try {
                  afd.close();
                } catch (Exception e) {
                  // abaikan
                }
              }
            } else {
              File f = Shared.resolve(context, scope, sa);
              if (f == null || !f.exists()) {
                Shared.err(Tools_Audio.this, "Audio", 0, "file tidak ada");
                return;
              }
              mp.setDataSource(f.getAbsolutePath());
            }
            audioPlayer = mp;
            audioReady = false;
            mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
              public void onPrepared(MediaPlayer p) {
                if (audioPlayer != mp) {
                  return;
                }
                try {
                  p.setLooping(audioLooping);
                  float v = volFloat();
                  p.setVolume(v, v);
                } catch (Exception e) {
                  // abaikan
                }
                audioReady = true;
                int dur = 0;
                try {
                  dur = p.getDuration();
                  p.start();
                } catch (Exception e) {
                  // abaikan
                }
                AudioStarted(dur);
                updateTickState();
              }
            });
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
              public void onCompletion(MediaPlayer p) {
                if (audioPlayer != mp) {
                  return;
                }
                if (audioLooping) {
                  try {
                    p.start();
                  } catch (Exception e) {
                    // abaikan
                  }
                  return;
                }
                audioReady = false;
                AudioFinished();
                updateTickState();
              }
            });
            mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
              public boolean onError(MediaPlayer p, int what, int extra) {
                if (audioPlayer == mp) {
                  audioReady = false;
                  Shared.err(Tools_Audio.this, "Audio", 0,
                      "format tidak didukung / file rusak");
                  updateTickState();
                }
                return true;
              }
            });
            mp.prepareAsync();
          } catch (Exception e) {
            String m = e.getMessage();
            Shared.err(Tools_Audio.this, "Audio", 0,
                m == null ? "audio gagal diputar" : m);
          }
        }
      });
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Audio", 0, m == null ? "audio gagal diputar" : m);
    }
  }

  @SimpleFunction(description = "Jeda audio yang sedang bunyi.")
  public void PauseAudio() {
    try {
      if (audioPlayer != null && audioReady && audioPlayer.isPlaying()) {
        audioPlayer.pause();
        updateTickState();
      } else {
        Shared.err(this, "Audio", 0, "tidak ada audio yang bunyi");
      }
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Audio", 0, m == null ? "pause gagal" : m);
    }
  }

  @SimpleFunction(description = "Lanjutkan audio yang dijeda.")
  public void ResumeAudio() {
    try {
      if (audioPlayer != null && audioReady && !audioPlayer.isPlaying()) {
        audioPlayer.start();
        updateTickState();
      } else {
        Shared.err(this, "Audio", 0, "tidak ada audio yang dijeda");
      }
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Audio", 0, m == null ? "resume gagal" : m);
    }
  }

  @SimpleFunction(description = "Hentikan audio (putar lagi via PlayAudio).")
  public void StopAudio() {
    try {
      if (audioPlayer != null) {
        stopInternal();
        updateTickState();
      } else {
        Shared.err(this, "Audio", 0, "tidak ada audio");
      }
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Audio", 0, m == null ? "stop gagal" : m);
    }
  }

  @SimpleFunction(description = "Lompat audio ke posisi (ms). Bisa dipasang ke Slider.")
  public void SeekAudio(int position) {
    try {
      if (audioPlayer != null && audioReady) {
        audioPlayer.seekTo(position < 0 ? 0 : position);
      } else {
        Shared.err(this, "Audio", 0, "tidak ada audio yang bunyi");
      }
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Audio", 0, m == null ? "seek gagal" : m);
    }
  }

  @SimpleFunction(description = "Maju audio sejauh SkipSeconds detik.")
  public void ForwardAudio() {
    try {
      if (audioPlayer != null && audioReady) {
        int pos = audioPlayer.getCurrentPosition();
        int dur = 0;
        try {
          dur = audioPlayer.getDuration();
        } catch (Exception e) {
          // abaikan
        }
        int skip = skipSeconds * 1000;
        if (dur > 0) {
          audioPlayer.seekTo(Math.min(dur, pos + skip));
        } else {
          audioPlayer.seekTo(pos + skip);
        }
      } else {
        Shared.err(this, "Audio", 0, "tidak ada audio yang bunyi");
      }
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Audio", 0, m == null ? "maju gagal" : m);
    }
  }

  @SimpleFunction(description = "Mundur audio sejauh SkipSeconds detik.")
  public void BackwardAudio() {
    try {
      if (audioPlayer != null && audioReady) {
        int pos = audioPlayer.getCurrentPosition();
        audioPlayer.seekTo(Math.max(0, pos - skipSeconds * 1000));
      } else {
        Shared.err(this, "Audio", 0, "tidak ada audio yang bunyi");
      }
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Audio", 0, m == null ? "mundur gagal" : m);
    }
  }

  @SimpleFunction(description = "Posisi audio sekarang (ms).")
  public int AudioPosition() {
    try {
      if (audioPlayer != null && audioReady) {
        return audioPlayer.getCurrentPosition();
      }
    } catch (Exception e) {
      // abaikan
    }
    return 0;
  }

  @SimpleFunction(description = "Durasi audio (ms). 0 bila belum siap.")
  public int AudioDuration() {
    try {
      if (audioPlayer != null && audioReady) {
        int d = audioPlayer.getDuration();
        return d < 0 ? 0 : d;
      }
    } catch (Exception e) {
      // abaikan
    }
    return 0;
  }

  @SimpleFunction(description = "True bila audio sedang bunyi.")
  public boolean IsAudioPlaying() {
    try {
      return audioPlayer != null && audioReady && audioPlayer.isPlaying();
    } catch (Exception e) {
      return false;
    }
  }

  // ================= pengaturan =================

  @SimpleProperty(description = "Jeda event AudioProgressChanged dalam milidetik (default 500).",
      category = PropertyCategory.BEHAVIOR)
  public int ProgressInterval() {
    return progressInterval;
  }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void ProgressInterval(int interval) {
    progressInterval = interval <= 0 ? 500 : interval;
    updateTickState();
  }

  @SimpleProperty(description = "Lompatan Maju/Mundur dalam detik (default 10).",
      category = PropertyCategory.BEHAVIOR)
  public int SkipSeconds() {
    return skipSeconds;
  }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void SkipSeconds(int seconds) {
    skipSeconds = seconds <= 0 ? 10 : seconds;
  }

  @SimpleProperty(description = "Audio mengulang otomatis bila selesai. Default false.",
      category = PropertyCategory.BEHAVIOR)
  public boolean AudioLooping() {
    return audioLooping;
  }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void AudioLooping(boolean loop) {
    audioLooping = loop;
  }

  @SimpleProperty(description = "Volume audio 0-100 (default 100).",
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
    try {
      if (audioPlayer != null && audioReady) {
        float v = volFloat();
        audioPlayer.setVolume(v, v);
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  // ================= events =================

  @SimpleEvent(description = "Audio mulai bunyi: persiapan selesai, durasi (ms) diketahui.")
  public void AudioStarted(int duration) {
    EventDispatcher.dispatchEvent(this, "AudioStarted",
        Integer.valueOf(duration));
  }

  @SimpleEvent(description = "Progres audio berubah: posisi + durasi (ms) + persen 0-100.")
  public void AudioProgressChanged(int position, int duration, int percent) {
    EventDispatcher.dispatchEvent(this, "AudioProgressChanged",
        Integer.valueOf(position), Integer.valueOf(duration),
        Integer.valueOf(percent));
  }

  @SimpleEvent(description = "Audio selesai (tidak loop). Putar lagi via PlayAudio.")
  public void AudioFinished() {
    EventDispatcher.dispatchEvent(this, "AudioFinished");
  }

  @SimpleEvent(description = "SATU-SATUNYA error Tools_Audio.")
  public void ErrorOccurred(String component, int id, String message) {
    EventDispatcher.dispatchEvent(this, "ErrorOccurred",
        component, Integer.valueOf(id), message);
  }

  @SimpleProperty(description = "Nama operasi Audio (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpAudio() {
    return "Audio";
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
    stopInternal();
  }
}
