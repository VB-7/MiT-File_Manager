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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@DesignerComponent(
    version = 1,
    description = "Tools_Audio: putar BANYAK audio ber-id (background, tanpa tampilan), kontrol "
        + "pause/resume/seek/maju/mundur, loop, volume. Satu id = satu audio, bisa jalan bareng. "
        + "Id dikelola CreateAudioId / Tools_Base. List id URUT PEMBUATAN.",
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

  private static final Map<Integer, MediaPlayer> players =
      new HashMap<Integer, MediaPlayer>();
  private static final Map<Integer, Boolean> readies =
      new HashMap<Integer, Boolean>();
  private static final Object audioLock = new Object();
  private static Handler mediaTicks = null;

  private final Runnable mediaTick = new Runnable() {
    public void run() {
      try {
        ArrayList<Integer> keys;
        synchronized (audioLock) {
          keys = new ArrayList<Integer>(players.keySet());
        }
        for (int i = 0; i < keys.size(); i = i + 1) {
          int aid = keys.get(i).intValue();
          try {
            MediaPlayer mp;
            boolean ready;
            synchronized (audioLock) {
              mp = players.get(Integer.valueOf(aid));
              Boolean r = readies.get(Integer.valueOf(aid));
              ready = r != null && r.booleanValue();
            }
            if (mp != null && ready && mp.isPlaying()) {
              int pos = mp.getCurrentPosition();
              int dur = mp.getDuration();
              int pct = dur > 0 ? ((int) (pos * 100L / dur)) : 0;
              AudioProgressChanged(aid, pos, dur, pct);
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

  public Tools_Audio(ComponentContainer container) {
    super(container.$form());
    this.context = container.$context();
    this.activity = container.$context();
  }

  private void updateTickState() {
    try {
      boolean need = false;
      try {
        synchronized (audioLock) {
          for (Map.Entry<Integer, MediaPlayer> en : players.entrySet()) {
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
      synchronized (audioLock) {
        if (mediaTicks == null) {
          try {
            mediaTicks = new Handler(context.getMainLooper());
          } catch (Exception e) {
            return;
          }
        }
        mediaTicks.removeCallbacks(mediaTick);
        if (need) {
          int every = progressInterval > 0 ? progressInterval : 500;
          mediaTicks.postDelayed(mediaTick, every);
        }
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

  private void stopInternal(int id) {
    MediaPlayer mp;
    synchronized (audioLock) {
      mp = players.remove(Integer.valueOf(id));
      readies.remove(Integer.valueOf(id));
    }
    try {
      if (mp != null) {
        try {
          mp.stop();
        } catch (Exception e) {
          // abaikan
        }
        try {
          mp.reset();
        } catch (Exception e) {
          // abaikan
        }
        try {
          mp.release();
        } catch (Exception e) {
          // abaikan
        }
      }
    } catch (Exception e) {
      // abaikan
    }
    Ids.setActive(Ids.AUDIO, id, false);
  }

  /** Hentikan + lepas id. Dipakai komponen + Tools_Base RemoveAudioId. */
  public static void stopStatic(int id) {
    MediaPlayer mp;
    synchronized (audioLock) {
      mp = players.remove(Integer.valueOf(id));
      readies.remove(Integer.valueOf(id));
    }
    try {
      if (mp != null) {
        try {
          mp.stop();
        } catch (Exception e) {
          // abaikan
        }
        try {
          mp.reset();
        } catch (Exception e) {
          // abaikan
        }
        try {
          mp.release();
        } catch (Exception e) {
          // abaikan
        }
      }
    } catch (Exception e) {
      // abaikan
    }
    Ids.setActive(Ids.AUDIO, id, false);
    Ids.unregister(Ids.AUDIO, id);
  }

  @SimpleFunction(description = "CALL: Putar audio ber-id (background, tanpa tampilan). Bisa jalan bareng "
      + "(satu id = satu audio). path bisa file scope, file:/// absolut, atau http(s) URL "
      + "(streaming). id dari CreateAudioId / Tools_Base CreateUniqueId (reservasi diadopsi). "
      + "Tampilan (Slider/Label) bikin sendiri dari AudioStarted/ProgressChanged/Finished. "
      + "Sukses menembak AudioStarted; gagal via ErrorOccurred. scope: helper "
      + "Scope, teks, atau absolut.")
  public void CallPlayAudio(final String path, final String scope, final int id) {
    if (id < 1) {
      Shared.err(this, "Audio", id, "id harus >= 1 (pakai CreateAudioId)");
      return;
    }
    if (!Ids.register(Ids.AUDIO, id)) {
      Shared.err(this, "Audio", id, "ID " + id + " sedang dipakai audio lain");
      return;
    }
    synchronized (audioLock) {
      if (players.containsKey(Integer.valueOf(id))) {
        if (Ids.isActive(Ids.AUDIO, id)) {
          Shared.err(this, "Audio", id, "ID " + id + " masih bunyi "
              + "(tunggu selesai / Pause / Stop dulu)");
          return;
        }
      }
    }
    stopInternal(id);
    try {
      form.runOnUiThread(new Runnable() {
        public void run() {
          try {
            final MediaPlayer mp = new MediaPlayer();
            String sa = Shared.clean(path);
            if (sa.length() == 0) {
              Ids.unregister(Ids.AUDIO, id);
              Shared.err(Tools_Audio.this, "Audio", id, "path kosong");
              return;
            }
            if (sa.regionMatches(true, 0, "http", 0, 4)) {
              mp.setDataSource(sa);
            } else if (sa.startsWith("file://")) {
              mp.setDataSource(Shared.normalizePath(
                  Shared.stripLeadingSlash(sa.substring(7))));
            } else if (Shared.effScope(scope, sa) == FileScope.Asset || Shared.isAssetPath(sa)) {
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
              File f = Shared.resolve(context, Shared.effScope(scope, sa), sa);
              if (f == null || !f.exists()) {
                Ids.unregister(Ids.AUDIO, id);
                Shared.err(Tools_Audio.this, "Audio", id, "file tidak ada");
                return;
              }
              mp.setDataSource(f.getAbsolutePath());
            }
            synchronized (audioLock) {
              players.put(Integer.valueOf(id), mp);
              readies.put(Integer.valueOf(id), Boolean.valueOf(false));
            }
            final int fId = id;
            mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
              public void onPrepared(MediaPlayer p) {
                synchronized (audioLock) {
                  if (players.get(Integer.valueOf(fId)) != p) {
                    return;
                  }
                  readies.put(Integer.valueOf(fId), Boolean.valueOf(true));
                }
                try {
                  p.setLooping(audioLooping);
                  float v = volFloat();
                  p.setVolume(v, v);
                } catch (Exception e) {
                  // abaikan
                }
                Ids.setActive(Ids.AUDIO, fId, true);
                int dur = 0;
                try {
                  dur = p.getDuration();
                  p.start();
                } catch (Exception e) {
                  // abaikan
                }
                AudioStarted(fId, dur);
                updateTickState();
              }
            });
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
              public void onCompletion(MediaPlayer p) {
                synchronized (audioLock) {
                  if (players.get(Integer.valueOf(fId)) != p) {
                    return;
                  }
                }
                if (audioLooping) {
                  try {
                    p.start();
                  } catch (Exception e) {
                    // abaikan
                  }
                  return;
                }
                synchronized (audioLock) {
                  readies.put(Integer.valueOf(fId), Boolean.valueOf(false));
                }
                Ids.setActive(Ids.AUDIO, fId, false);
                AudioFinished(fId);
                updateTickState();
              }
            });
            mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
              public boolean onError(MediaPlayer p, int what, int extra) {
                synchronized (audioLock) {
                  if (players.get(Integer.valueOf(fId)) == p) {
                    readies.put(Integer.valueOf(fId), Boolean.valueOf(false));
                  } else {
                    return true;
                  }
                }
                Ids.setActive(Ids.AUDIO, fId, false);
                Shared.err(Tools_Audio.this, "Audio", fId,
                    "format tidak didukung / file rusak");
                updateTickState();
                return true;
              }
            });
            mp.prepareAsync();
          } catch (Exception e) {
            Ids.unregister(Ids.AUDIO, id);
            String m = e.getMessage();
            Shared.err(Tools_Audio.this, "Audio", id,
                m == null ? "audio gagal diputar" : m);
          }
        }
      });
    } catch (Exception e) {
      Ids.unregister(Ids.AUDIO, id);
      String m = e.getMessage();
      Shared.err(this, "Audio", id, m == null ? "audio gagal diputar" : m);
      return;
    }
  }

  @SimpleFunction(description = "SET: jeda audio yang sedang bunyi (per id). Tanpa feedback.")
  public void SetPauseAudio(int id) {
    try {
      MediaPlayer mp;
      boolean ready;
      synchronized (audioLock) {
        mp = players.get(Integer.valueOf(id));
        Boolean r = readies.get(Integer.valueOf(id));
        ready = r != null && r.booleanValue();
      }
      if (mp != null && ready && mp.isPlaying()) {
        mp.pause();
        Ids.setActive(Ids.AUDIO, id, false);
        updateTickState();
      } else {
        Shared.err(this, "Audio", id, "tidak ada audio bunyi ID " + id);
      }
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Audio", id, m == null ? "pause gagal" : m);
    }
  }

  @SimpleFunction(description = "SET: lanjutkan audio yang dijeda (per id). Tanpa feedback.")
  public void SetResumeAudio(int id) {
    try {
      MediaPlayer mp;
      boolean ready;
      synchronized (audioLock) {
        mp = players.get(Integer.valueOf(id));
        Boolean r = readies.get(Integer.valueOf(id));
        ready = r != null && r.booleanValue();
      }
      if (mp != null && ready && !mp.isPlaying()) {
        mp.start();
        Ids.setActive(Ids.AUDIO, id, true);
        updateTickState();
      } else {
        Shared.err(this, "Audio", id, "tidak ada audio jeda ID " + id);
      }
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Audio", id, m == null ? "resume gagal" : m);
    }
  }

  @SimpleFunction(description = "SET: Hentikan audio (per id). Putar lagi via CallPlayAudio.")
  public void SetStopAudio(int id) {
    try {
      boolean has;
      synchronized (audioLock) {
        has = players.containsKey(Integer.valueOf(id));
      }
      if (has) {
        stopInternal(id);
        updateTickState();
      } else {
        Shared.err(this, "Audio", id, "tidak ada audio ID " + id);
      }
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Audio", id, m == null ? "stop gagal" : m);
    }
  }

  @SimpleFunction(description = "SET: Lompat audio ke posisi (ms). Bisa dipasang ke Slider. (per id)")
  public void SetSeekAudio(int id, int position) {
    try {
      MediaPlayer mp;
      boolean ready;
      synchronized (audioLock) {
        mp = players.get(Integer.valueOf(id));
        Boolean r = readies.get(Integer.valueOf(id));
        ready = r != null && r.booleanValue();
      }
      if (mp != null && ready) {
        mp.seekTo(position < 0 ? 0 : position);
      } else {
        Shared.err(this, "Audio", id, "tidak ada audio bunyi ID " + id);
      }
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Audio", id, m == null ? "seek gagal" : m);
    }
  }

  @SimpleFunction(description = "SET: Maju audio sejauh SkipSeconds detik. (per id)")
  public void SetForwardAudio(int id) {
    try {
      MediaPlayer mp;
      boolean ready;
      synchronized (audioLock) {
        mp = players.get(Integer.valueOf(id));
        Boolean r = readies.get(Integer.valueOf(id));
        ready = r != null && r.booleanValue();
      }
      if (mp != null && ready) {
        int pos = mp.getCurrentPosition();
        int dur = 0;
        try {
          dur = mp.getDuration();
        } catch (Exception e) {
          // abaikan
        }
        int skip = skipSeconds * 1000;
        if (dur > 0) {
          mp.seekTo(Math.min(dur, pos + skip));
        } else {
          mp.seekTo(pos + skip);
        }
      } else {
        Shared.err(this, "Audio", id, "tidak ada audio bunyi ID " + id);
      }
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Audio", id, m == null ? "maju gagal" : m);
    }
  }

  @SimpleFunction(description = "SET: Mundur audio sejauh SkipSeconds detik. (per id)")
  public void SetBackwardAudio(int id) {
    try {
      MediaPlayer mp;
      boolean ready;
      synchronized (audioLock) {
        mp = players.get(Integer.valueOf(id));
        Boolean r = readies.get(Integer.valueOf(id));
        ready = r != null && r.booleanValue();
      }
      if (mp != null && ready) {
        int pos = mp.getCurrentPosition();
        mp.seekTo(Math.max(0, pos - skipSeconds * 1000));
      } else {
        Shared.err(this, "Audio", id, "tidak ada audio bunyi ID " + id);
      }
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Audio", id, m == null ? "mundur gagal" : m);
    }
  }

  @SimpleFunction(description = "Posisi audio sekarang (ms). (per id)")
  public int GetAudioPosition(int id) {
    try {
      synchronized (audioLock) {
        MediaPlayer mp = players.get(Integer.valueOf(id));
        Boolean r = readies.get(Integer.valueOf(id));
        if (mp != null && r != null && r.booleanValue()) {
          return mp.getCurrentPosition();
        }
      }
    } catch (Exception e) {
      // abaikan
    }
    return 0;
  }

  @SimpleFunction(description = "Durasi audio (ms). 0 bila belum siap. (per id)")
  public int GetAudioDuration(int id) {
    try {
      synchronized (audioLock) {
        MediaPlayer mp = players.get(Integer.valueOf(id));
        Boolean r = readies.get(Integer.valueOf(id));
        if (mp != null && r != null && r.booleanValue()) {
          int d = mp.getDuration();
          return d < 0 ? 0 : d;
        }
      }
    } catch (Exception e) {
      // abaikan
    }
    return 0;
  }

  @SimpleFunction(description = "True bila audio id ini sedang bunyi.")
  public boolean IsAudioPlaying(int id) {
    try {
      synchronized (audioLock) {
        MediaPlayer mp = players.get(Integer.valueOf(id));
        Boolean r = readies.get(Integer.valueOf(id));
        return mp != null && r != null && r.booleanValue() && mp.isPlaying();
      }
    } catch (Exception e) {
      return false;
    }
  }

  // ================= ID =================

  @SimpleFunction(description = "Buat id audio unik baru (Number, siap pakai ke CallPlayAudio).")
  public int CreateAudioId() {
    int id = Ids.createFor(Ids.AUDIO);
    if (id < 1) {
      Shared.err(this, "Audio", 0, "gagal buat id");
    }
    return id;
  }

  @SimpleFunction(description = "Semua id audio yang dipegang (URUT PEMBUATAN, "
      + "index 1 = pertama dibuat).")
  public List<Integer> GetAudioIds() {
    return Ids.idsOf(Ids.AUDIO);
  }

  @SimpleFunction(description = "Id audio yang sedang bunyi (URUT PEMBUATAN). "
      + "Yang dijeda/berhenti TIDAK masuk sini.")
  public List<Integer> GetActiveAudioIds() {
    return Ids.activeIdsOf(Ids.AUDIO);
  }

  @SimpleFunction(description = "Id audio yang terakhir dibuat/di-start. -1 bila kosong.")
  public int CurrentAudioId() {
    return Ids.currentOf(Ids.AUDIO);
  }

  @SimpleFunction(description = "Id index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstAudioId() {
    return Ids.firstOf(Ids.AUDIO);
  }

  @SimpleFunction(description = "Id TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastAudioId() {
    return Ids.lastOf(Ids.AUDIO);
  }

  @SimpleFunction(description = "Id yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.")
  public int GetPreviousAudioId(int id) {
    return Ids.previousOf(Ids.AUDIO, id);
  }

  @SimpleFunction(description = "True bila id audio ini sedang bunyi.")
  public boolean IsAudioIdActive(int id) {
    return Ids.isActive(Ids.AUDIO, id);
  }

  @SimpleFunction(description = "Hapus id audio: hentikan bila bunyi + lepas id "
      + "(otomatis beres-beres).")
  public void RemoveAudioId(int id) {
    stopStatic(id);
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

  @SimpleProperty(description = "Volume audio 0-100 (default 100, berlaku semua id).",
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
      float v = volFloat();
      synchronized (audioLock) {
        for (MediaPlayer mp : players.values()) {
          try {
            mp.setVolume(v, v);
          } catch (Exception e) {
            // abaikan per-id
          }
        }
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  // ================= events =================

  @SimpleEvent(description = "Audio mulai bunyi: id + durasi (ms) diketahui.")
  public void AudioStarted(int id, int duration) {
    EventDispatcher.dispatchEvent(this, "AudioStarted",
        Integer.valueOf(id), Integer.valueOf(duration));
  }

  @SimpleEvent(description = "Progres audio berubah: id + posisi + durasi (ms) + persen 0-100.")
  public void AudioProgressChanged(int id, int position, int duration, int percent) {
    EventDispatcher.dispatchEvent(this, "AudioProgressChanged",
        Integer.valueOf(id), Integer.valueOf(position), Integer.valueOf(duration),
        Integer.valueOf(percent));
  }

  @SimpleEvent(description = "Audio selesai (tidak loop). Putar lagi via CallPlayAudio.")
  public void AudioFinished(int id) {
    EventDispatcher.dispatchEvent(this, "AudioFinished", Integer.valueOf(id));
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

  @SimpleFunction(description = "Fallback polling untuk event ErrorOccurred: "
      + "pesan error terakhir operasi ini (kosong bila belum ada).")
  public String LastError(String operation) {
    return Shared.lastError(operation);
  }

  // ================= destroy =================

  public void onDestroy() {
    try {
      synchronized (audioLock) {
        if (mediaTicks != null) {
          mediaTicks.removeCallbacks(mediaTick);
        }
      }
    } catch (Exception e) {
      // abaikan
    }
    try {
      ArrayList<Integer> keys;
      synchronized (audioLock) {
        keys = new ArrayList<Integer>(players.keySet());
      }
      for (int i = 0; i < keys.size(); i = i + 1) {
        stopInternal(keys.get(i).intValue());
        Ids.unregister(Ids.AUDIO, keys.get(i).intValue());
      }
    } catch (Exception e) {
      // abaikan
    }
  }
}
