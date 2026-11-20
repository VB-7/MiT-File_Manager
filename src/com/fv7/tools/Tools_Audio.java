package com.fv7.tools;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.OnDestroyListener;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UsesPermissions("android.permission.INTERNET")
@DesignerComponent(
    version = 22,
    description = "Tools_Audio: putar BANYAK audio ber-id (background, tanpa tampilan), kontrol "
        + "pause/resume/seek/maju/mundur, loop, volume. Satu id = satu audio, bisa jalan bareng. "
        + "Id dikelola CreateAudioId (atau CreateUniqueId Tools_Base). "
        + "List id URUT PEMBUATAN (index 1 = pertama dibuat). Streaming URL butuh "
        + "izin INTERNET (otomatis dibawa komponen ini). Konvensi awalan: Is = cek "
        + "boolean; Call = async + hasil lewat event; Set = langsung tanpa feedback; "
        + "Get = nilai balik sinkron.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    androidMinSdk = 21,
    iconName = "aiwebres/tools_audio.jpg",
    helpUrl = "https://github.com/VB-7/MiT-File_Manager")
@SimpleObject(external = true)
public class Tools_Audio extends AndroidNonvisibleComponent
    implements OnDestroyListener {

  private final Context context;
  private final Activity activity;

  private int progressInterval = 500;
  private int skipSeconds = 10;
  private boolean audioLooping = false;
  private int mediaVolume = 100;
  private final Map<Integer, Integer> volumes =
      new HashMap<Integer, Integer>();
  private final Map<Integer, Boolean> loops =
      new HashMap<Integer, Boolean>();
  private final Map<Integer, Float> speeds =
      new HashMap<Integer, Float>();

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

  private float volFloatV(int v) {
    if (v < 0) {
      v = 0;
    }
    if (v > 100) {
      v = 100;
    }
    return v / 100.0f;
  }

  private float effVolFloat(int id) {
    Integer o;
    synchronized (audioLock) {
      o = volumes.get(Integer.valueOf(id));
    }
    return volFloatV(o == null ? mediaVolume : o.intValue());
  }

  private boolean effLoop(int id) {
    Boolean o;
    synchronized (audioLock) {
      o = loops.get(Integer.valueOf(id));
    }
    return o == null ? audioLooping : o.booleanValue();
  }

  private void applySpeed(int id, MediaPlayer mp) {
    try {
      if (android.os.Build.VERSION.SDK_INT < 23) {
        return;
      }
      Float s;
      synchronized (audioLock) {
        s = speeds.get(Integer.valueOf(id));
      }
      if (s != null && mp != null) {
        try {
          mp.setPlaybackParams(mp.getPlaybackParams().setSpeed(s.floatValue()));
        } catch (Exception e) {
          // abaikan
        }
      }
    } catch (Exception e) {
      // abaikan
    }
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
      + "(satu id = satu audio). path file (/Download/... atau absolut apa adanya), "
      + "file:/// absolut, atau http(s) URL (streaming, izin INTERNET otomatis dibawa). "
      + "id dari CreateAudioId atau CreateUniqueId Tools_Base (reservasi diadopsi). "
      + "Id yang masih bunyi tidak bisa dipakai ulang (Stop/Pause dulu). Stop lalu Play "
      + "= mulai dari awal. Tampilan (Slider/Label) bikin sendiri dari "
      + "AudioStarted/ProgressChanged/Finished. Sukses menembak AudioStarted; gagal via "
      + "ErrorOccurred.")
  public void CallPlayAudio(final String path, final int id) {
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
              File ff = Shared.resolveRooted(sa);
              if (ff == null || !ff.exists()) {
                Ids.unregister(Ids.AUDIO, id);
                Shared.err(Tools_Audio.this, "Audio", id, "file tidak ada");
                return;
              }
              mp.setDataSource(ff.getAbsolutePath());
            } else if (Shared.isAssetPath(sa)) {
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
              File f = Shared.resolveRooted(sa);
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
                  p.setLooping(effLoop(fId));
                  float v = effVolFloat(fId);
                  p.setVolume(v, v);
                  applySpeed(fId, p);
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

  @SimpleFunction(description = "SET: Jeda audio yang sedang bunyi. Tanpa feedback. "
      + "Cek status via IsAudioPaused.")
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
      }
    } catch (Exception e) {
      String m = e.getMessage();
    }
  }

  @SimpleFunction(description = "SET: Lanjutkan audio yang dijeda. Tanpa feedback. "
      + "Cek status via IsAudioPaused.")
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
      }
    } catch (Exception e) {
      String m = e.getMessage();
    }
  }

  @SimpleFunction(description = "SET: Hentikan audio (posisi kembali ke 0, tanpa event). "
      + "Putar lagi via CallPlayAudio (mulai dari awal). Cek via IsAudioIdActive.")
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
      }
    } catch (Exception e) {
      String m = e.getMessage();
    }
  }

  @SimpleFunction(description = "SET: Lompat audio ke posisi milidetik. Bisa dipasang ke "
      + "Slider. Tanpa feedback.")
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
      }
    } catch (Exception e) {
      String m = e.getMessage();
    }
  }

  private void seekBySeconds(int id, int seconds) {
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
        int np = pos + seconds * 1000;
        if (np < 0) {
          np = 0;
        }
        if (dur > 0 && np > dur) {
          np = dur;
        }
        mp.seekTo(np);
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "SET: Maju audio sejauh SkipSeconds DETIK. "
      + "Tanpa feedback.")
  public void SetForwardAudio(int id) {
    seekBySeconds(id, skipSeconds);
  }

  @SimpleFunction(description = "SET: Maju audio sejauh seconds DETIK "
      + "(tanpa ubah SkipSeconds). Tanpa feedback.")
  public void SetForwardAudioBy(int id, int seconds) {
    seekBySeconds(id, seconds);
  }

  @SimpleFunction(description = "SET: Mundur audio sejauh SkipSeconds DETIK. "
      + "Tanpa feedback.")
  public void SetBackwardAudio(int id) {
    seekBySeconds(id, -skipSeconds);
  }

  @SimpleFunction(description = "SET: Mundur audio sejauh seconds DETIK "
      + "(tanpa ubah SkipSeconds). Tanpa feedback.")
  public void SetBackwardAudioBy(int id, int seconds) {
    seekBySeconds(id, -seconds);
  }

  @SimpleFunction(description = "Posisi audio sekarang dalam milidetik. 0 bila belum siap.")
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

  @SimpleFunction(description = "Durasi audio dalam milidetik. 0 bila belum siap.")
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

  @SimpleFunction(description = "Id audio yang terakhir disentuh (dibuat atau di-start). "
      + "-1 bila kosong. Beda dengan GetLastAudioId (terakhir dibuat).")
  public int CurrentAudioId() {
    return Ids.currentOf(Ids.AUDIO);
  }

  @SimpleFunction(description = "Id index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstAudioId() {
    return Ids.firstOf(Ids.AUDIO);
  }

  @SimpleFunction(description = "Id audio TERAKHIR DIBUAT. -1 bila kosong. Beda dengan "
      + "CurrentAudioId (terakhir disentuh).")
  public int GetLastAudioId() {
    return Ids.lastOf(Ids.AUDIO);
  }

  @SimpleFunction(description = "Id yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.")
  public int GetPreviousAudioId(int id) {
    return Ids.previousOf(Ids.AUDIO, id);
  }

  @SimpleFunction(description = "True bila id audio ini sedang bunyi. False = jeda, "
      + "berhenti, atau belum ada (bedakan jeda vs berhenti via IsAudioPaused).")
  public boolean IsAudioIdActive(int id) {
    return Ids.isActive(Ids.AUDIO, id);
  }


  @SimpleFunction(description = "True bila id audio ini sedang dijeda (Pause tapi "
      + "belum Stop/selesai). False = bunyi/berhenti/belum ada.")
  public boolean IsAudioPaused(int id) {
    try {
      synchronized (audioLock) {
        MediaPlayer mp = players.get(Integer.valueOf(id));
        Boolean r = readies.get(Integer.valueOf(id));
        return mp != null && r != null && r.booleanValue() && !mp.isPlaying();
      }
    } catch (Exception e) {
      return false;
    }
  }

  @SimpleFunction(description = "Volume khusus satu id (0-100). = volume global "
      + "bila belum diatur via SetAudioVolume.")
  public int GetAudioVolume(int id) {
    synchronized (audioLock) {
      Integer o = volumes.get(Integer.valueOf(id));
      if (o != null) {
        return o.intValue();
      }
    }
    return mediaVolume;
  }

  @SimpleFunction(description = "SET: Volume khusus satu id (0-100). Menimpa "
      + "volume global untuk id ini. Tanpa feedback.")
  public void SetAudioVolume(int id, int volume) {
    if (volume < 0) {
      volume = 0;
    }
    if (volume > 100) {
      volume = 100;
    }
    synchronized (audioLock) {
      volumes.put(Integer.valueOf(id), Integer.valueOf(volume));
    }
    try {
      MediaPlayer mp;
      synchronized (audioLock) {
        mp = players.get(Integer.valueOf(id));
      }
      if (mp != null) {
        float v = effVolFloat(id);
        mp.setVolume(v, v);
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "Loop khusus satu id. = loop global bila belum "
      + "diatur via SetAudioLooping.")
  public boolean GetAudioLooping(int id) {
    synchronized (audioLock) {
      Boolean o = loops.get(Integer.valueOf(id));
      if (o != null) {
        return o.booleanValue();
      }
    }
    return audioLooping;
  }

  @SimpleFunction(description = "SET: Loop khusus satu id (true = mengulang bila "
      + "selesai). Menimpa loop global untuk id ini. Tanpa feedback.")
  public void SetAudioLooping(int id, boolean loop) {
    synchronized (audioLock) {
      loops.put(Integer.valueOf(id), Boolean.valueOf(loop));
    }
    try {
      MediaPlayer mp;
      synchronized (audioLock) {
        mp = players.get(Integer.valueOf(id));
      }
      if (mp != null) {
        mp.setLooping(loop);
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "Kecepatan id ini (1.0 = normal). 1.0 bila belum "
      + "diatur via SetAudioSpeed.")
  public float GetAudioSpeed(int id) {
    synchronized (audioLock) {
      Float o = speeds.get(Integer.valueOf(id));
      if (o != null) {
        return o.floatValue();
      }
    }
    return 1.0f;
  }

  @SimpleFunction(description = "SET: Kecepatan khusus satu id (1.0 = normal, "
      + "0.5 = setengah, 2.0 = ganda). Butuh Android 6+. Tanpa feedback.")
  public void SetAudioSpeed(int id, float speed) {
    if (speed < 0.25f) {
      speed = 0.25f;
    }
    if (speed > 4.0f) {
      speed = 4.0f;
    }
    synchronized (audioLock) {
      speeds.put(Integer.valueOf(id), Float.valueOf(speed));
    }
    try {
      MediaPlayer mp;
      synchronized (audioLock) {
        mp = players.get(Integer.valueOf(id));
      }
      if (mp != null) {
        applySpeed(id, mp);
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "Hapus id audio: hentikan bila bunyi + lepas id "
      + "(otomatis beres-beres).")
  public void RemoveAudioId(int id) {
    stopStatic(id);
  }


  @SimpleFunction(description = "SET: Hentikan SEMUA audio + buang semua id. "
      + "Tanpa feedback.")
  public void SetClearAudios() {
    try {
      List<Integer> ids = Ids.idsOf(Ids.AUDIO);
      for (int i = 0; i < ids.size(); i = i + 1) {
        try {
          stopStatic(ids.get(i).intValue());
        } catch (Exception e) {
          // abaikan per-id
        }
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  // ================= pengaturan =================

  @SimpleProperty(description = "Jeda event AudioProgressChanged dalam milidetik (default 500). "
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

  @SimpleProperty(description = "Lompatan Maju/Mundur dalam DETIK (bukan ms). Default 10. "
      + "Untuk lompatan sekali-sekali pakai SetForwardAudioBy/SetBackwardAudioBy.",
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

  @SimpleProperty(description = "Loop global (default false). Id yang sudah diatur via "
      + "SetAudioLooping memakai nilainya sendiri.",
      category = PropertyCategory.BEHAVIOR)
  public boolean AudioLooping() {
    return audioLooping;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
      defaultValue = "False")
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void AudioLooping(boolean loop) {
    audioLooping = loop;
  }

  @SimpleProperty(description = "Volume global 0-100 (default 100, awal semua id). Id yang "
      + "sudah diatur via SetAudioVolume memakai nilainya sendiri.",
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
      synchronized (audioLock) {
        keys = new ArrayList<Integer>(players.keySet());
      }
      for (int i = 0; i < keys.size(); i = i + 1) {
        int aid = keys.get(i).intValue();
        try {
          MediaPlayer p2;
          synchronized (audioLock) {
            p2 = players.get(Integer.valueOf(aid));
          }
          if (p2 != null) {
            float v = effVolFloat(aid);
            p2.setVolume(v, v);
          }
        } catch (Exception e) {
          // abaikan per-id
        }
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  // ================= events =================

  @SimpleEvent(description = "Audio mulai bunyi: id + durasi (ms) diketahui.")
  public void AudioStarted(int id, int duration) {
    Shared.fire(this, "AudioStarted",
        Integer.valueOf(id), Integer.valueOf(duration));
  }

  @SimpleEvent(description = "Progres audio berubah: id + posisi + durasi (ms) + persen 0-100.")
  public void AudioProgressChanged(int id, int position, int duration, int percent) {
    Shared.fire(this, "AudioProgressChanged",
        Integer.valueOf(id), Integer.valueOf(position), Integer.valueOf(duration),
        Integer.valueOf(percent));
  }

  @SimpleEvent(description = "Audio selesai (tidak loop). Putar lagi via CallPlayAudio.")
  public void AudioFinished(int id) {
    Shared.fire(this, "AudioFinished", Integer.valueOf(id));
  }

  @SimpleEvent(description = "SATU-SATUNYA error Tools_Audio. component = nama operasi "
      + "(isi dengan OpAudio untuk if). Contoh: jika component = OpAudio "
      + "maka tampilkan message.")
  public void ErrorOccurred(String component, int id, String message) {
    Shared.fire(this, "ErrorOccurred",
        component, Integer.valueOf(id), message);
  }

  @SimpleProperty(description = "Nama operasi Audio untuk if di ErrorOccurred. Contoh: "
      + "colok ke perbandingan = dengan component dari event ErrorOccurred.",
      category = PropertyCategory.BEHAVIOR)
  public String OpAudio() {
    return "Audio";
  }

  @SimpleFunction(description = "Fallback polling untuk event ErrorOccurred: pesan error "
      + "terakhir operasi ini (teks kosong bila belum ada). Isi operation dengan blok OpAudio.")
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
