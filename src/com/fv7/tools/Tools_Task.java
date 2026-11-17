package com.fv7.tools;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

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
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.OnDestroyListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@DesignerComponent(
    version = 1,
    description = "Tools_Task: penjadwal full-stack ber-id. Tugas dalam-aplikasi "
        + "(sekali/berulang/terjadwal) + alarm sistem via AlarmManager (buka "
        + "aplikasi saat bunyi) + notifikasi ongoing + wake-lock + variabel/extra "
        + "per tugas + persistensi alarm (RestoreAlarms). Id dikelola "
        + "CreateTaskId / Tools_Base. Tanpa Service/receiver khusus (aman "
        + "manifest, lolos Play). Setiap event ada fallback polling "
        + "(GetTaskStatus/Result/Progress, GetAlarmInfo, CheckStartupAlarm). "
        + "List id URUT PEMBUATAN.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    androidMinSdk = 21,
    iconName = "aiwebres/tools_task.jpg")
@SimpleObject(external = true)
@UsesPermissions({"android.permission.WAKE_LOCK",
    "android.permission.RECEIVE_BOOT_COMPLETED",
    "android.permission.SCHEDULE_EXACT_ALARM",
    "android.permission.POST_NOTIFICATIONS"})
public class Tools_Task extends AndroidNonvisibleComponent
    implements OnDestroyListener {

  private static final String PREFS = "Fv7ToolsTask";
  private static final String CHANNEL_ID = "tools_task_channel";
  private static final long MIN_INTERVAL = 1000L;

  private final Context context;
  private final Activity activity;

  private static Tools_Task last = null;
  private static Handler mainHandler = null;
  private static final Object lock = new Object();

  private static final class Task {
    int id;
    String payload = "";
    String kind = "once";
    long delay = 0;
    long interval = 0;
    int repeatCount = 1;
    int tick = 0;
    String status = "antri";
    boolean success = false;
    String message = "";
    long nextFire = 0;
    long lastTickAt = 0;
    long remaining = 0;
    boolean paused = false;
    Runnable runner = null;
    Map<String, String> extras = new HashMap<String, String>();
  }

  private static final class AlarmSpec {
    int id;
    long epoch = 0;
    long interval = 0;
    String payload = "";
    boolean exact = false;
  }

  private static final Map<Integer, Task> tasks = new HashMap<Integer, Task>();
  private static final Map<Integer, AlarmSpec> alarms =
      new HashMap<Integer, AlarmSpec>();
  private static final Map<String, String> vars = new HashMap<String, String>();

  private static int lastFiredTaskId = -1;
  private static String lastFiredPayload = "";
  private static long lastFiredAt = 0;
  private static int lastDeliveredAlarmId = -1;

  private static PowerManager.WakeLock wakeLock = null;

  public Tools_Task(ComponentContainer container) {
    super(container.$form());
    this.context = container.$context();
    this.activity = container.$context();
    last = this;
  }

  // ================= internal =================

  private static Handler main() {
    synchronized (lock) {
      if (mainHandler == null) {
        try {
          mainHandler = new Handler(Looper.getMainLooper());
        } catch (Exception e) {
          mainHandler = null;
        }
      }
      return mainHandler;
    }
  }

  private static void updateActive(int id) {
    boolean on = false;
    synchronized (lock) {
      Task t = tasks.get(Integer.valueOf(id));
      if (t != null && ("jalan".equals(t.status) || "jeda".equals(t.status))) {
        on = true;
      }
      if (alarms.containsKey(Integer.valueOf(id))) {
        on = true;
      }
    }
    Ids.setActive(Ids.TASK, id, on);
  }

  private static SharedPreferences prefs(Context ctx) {
    try {
      return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    } catch (Exception e) {
      return null;
    }
  }

  private void persistAlarm(AlarmSpec a) {
    try {
      SharedPreferences p = prefs(context);
      if (p == null) {
        return;
      }
      SharedPreferences.Editor e = p.edit();
      e.putLong("epoch_" + a.id, a.epoch);
      e.putLong("interval_" + a.id, a.interval);
      e.putString("payload_" + a.id, a.payload == null ? "" : a.payload);
      e.putBoolean("exact_" + a.id, a.exact);
      Set<String> ids = new HashSet<String>();
      try {
        ids.addAll(p.getStringSet("alarm_ids", new HashSet<String>()));
      } catch (Exception ex) {
        // abaikan
      }
      ids.add(String.valueOf(a.id));
      if (Build.VERSION.SDK_INT >= 11) {
        e.putStringSet("alarm_ids", ids);
      }
      e.commit();
    } catch (Exception e) {
      // abaikan
    }
  }

  private void unpersistAlarm(int id) {
    try {
      SharedPreferences p = prefs(context);
      if (p == null) {
        return;
      }
      SharedPreferences.Editor e = p.edit();
      e.remove("epoch_" + id);
      e.remove("interval_" + id);
      e.remove("payload_" + id);
      e.remove("exact_" + id);
      Set<String> ids = new HashSet<String>();
      try {
        ids.addAll(p.getStringSet("alarm_ids", new HashSet<String>()));
      } catch (Exception ex) {
        // abaikan
      }
      ids.remove(String.valueOf(id));
      if (Build.VERSION.SDK_INT >= 11) {
        e.putStringSet("alarm_ids", ids);
      }
      e.commit();
    } catch (Exception e) {
      // abaikan
    }
  }

  private AlarmManager alarmManager() {
    try {
      Object s = context.getSystemService(Context.ALARM_SERVICE);
      if (s instanceof AlarmManager) {
        return (AlarmManager) s;
      }
    } catch (Exception e) {
      // abaikan
    }
    return null;
  }

  private PendingIntent pendingFor(int id, String payload) {
    try {
      Intent launch = null;
      try {
        launch = context.getPackageManager()
            .getLaunchIntentForPackage(context.getPackageName());
      } catch (Exception e) {
        launch = null;
      }
      if (launch == null) {
        launch = new Intent(context, activity.getClass());
      }
      launch.setAction("com.fv7.tools.TASK_ALARM_" + id);
      launch.putExtra("taskId", id);
      launch.putExtra("payload", payload == null ? "" : payload);
      launch.putExtra("firedAt", System.currentTimeMillis());
      launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
          | Intent.FLAG_ACTIVITY_SINGLE_TOP
          | Intent.FLAG_ACTIVITY_CLEAR_TOP);
      int flags = PendingIntent.FLAG_UPDATE_CURRENT;
      if (Build.VERSION.SDK_INT >= 23) {
        flags = flags | PendingIntent.FLAG_IMMUTABLE;
      }
      return PendingIntent.getActivity(context, id, launch, flags);
    } catch (Exception e) {
      return null;
    }
  }

  private boolean armAlarm(AlarmSpec a) {
    try {
      AlarmManager am = alarmManager();
      PendingIntent pi = pendingFor(a.id, a.payload);
      if (am == null || pi == null) {
        return false;
      }
      long now = System.currentTimeMillis();
      if (a.interval > 0) {
        long first = a.epoch <= 0 ? now + a.interval : a.epoch;
        if (first < now) {
          first = now + 1000;
        }
        if (Build.VERSION.SDK_INT >= 19) {
          try {
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, first,
                Math.max(MIN_INTERVAL, a.interval), pi);
          } catch (Exception e) {
            am.setRepeating(AlarmManager.RTC_WAKEUP, first,
                Math.max(MIN_INTERVAL, a.interval), pi);
          }
        } else {
          am.setRepeating(AlarmManager.RTC_WAKEUP, first,
              Math.max(MIN_INTERVAL, a.interval), pi);
        }
      } else {
        long at = a.epoch <= 0 ? now + 1000 : a.epoch;
        if (at < now) {
          at = now + 500;
        }
        boolean wantExact = a.exact && canExact();
        if (wantExact) {
          if (Build.VERSION.SDK_INT >= 23) {
            try {
              am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            } catch (Exception e) {
              am.setExact(AlarmManager.RTC_WAKEUP, at, pi);
            }
          } else if (Build.VERSION.SDK_INT >= 19) {
            am.setExact(AlarmManager.RTC_WAKEUP, at, pi);
          } else {
            am.set(AlarmManager.RTC_WAKEUP, at, pi);
          }
        } else {
          if (Build.VERSION.SDK_INT >= 23) {
            try {
              am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            } catch (Exception e) {
              am.set(AlarmManager.RTC_WAKEUP, at, pi);
            }
          } else {
            am.set(AlarmManager.RTC_WAKEUP, at, pi);
          }
        }
      }
      return true;
    } catch (SecurityException se) {
      Shared.err(this, "Alarm", a.id, "izin alarm exact belum diberikan");
      return false;
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Alarm", a.id, m == null ? "alarm gagal" : m);
      return false;
    }
  }

  private boolean canExact() {
    try {
      if (Build.VERSION.SDK_INT >= 31) {
        AlarmManager am = alarmManager();
        if (am != null) {
          try {
            return am.canScheduleExactAlarms();
          } catch (Exception e) {
            return false;
          }
        }
        return false;
      }
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private void ensureChannel() {
    try {
      if (Build.VERSION.SDK_INT >= 26) {
        NotificationManager nm = (NotificationManager) context
            .getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
          NotificationChannel ch = new NotificationChannel(CHANNEL_ID,
              "Tools Task", NotificationManager.IMPORTANCE_DEFAULT);
          nm.createNotificationChannel(ch);
        }
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  // ================= ID =================

  @SimpleFunction(description = "Buat id tugas unik baru (Number, siap pakai "
      + "ke CallSchedule*/CallSetAlarm). Urutan pembuatan menentukan urutan list.")
  public int CreateTaskId() {
    int id = Ids.createFor(Ids.TASK);
    if (id < 1) {
      Shared.err(this, "Task", 0, "gagal buat id");
    }
    return id;
  }

  @SimpleFunction(description = "Semua id tugas/alarm (URUT PEMBUATAN, "
      + "index 1 = pertama dibuat).")
  public List<Integer> GetTaskIds() {
    return Ids.idsOf(Ids.TASK);
  }

  @SimpleFunction(description = "Id tugas/alarm yang aktif (jalan/jeda/terpasang, "
      + "URUT PEMBUATAN).")
  public List<Integer> GetActiveTaskIds() {
    return Ids.activeIdsOf(Ids.TASK);
  }

  @SimpleFunction(description = "Alias GetActiveTaskIds: id yang masih pending.")
  public List<Integer> GetPendingTaskIds() {
    return Ids.activeIdsOf(Ids.TASK);
  }

  @SimpleFunction(description = "Id tugas terakhir dibuat/di-start. -1 bila kosong.")
  public int CurrentTaskId() {
    return Ids.currentOf(Ids.TASK);
  }

  @SimpleFunction(description = "Id index 1 = PERTAMA DIBUAT. -1 bila kosong.")
  public int GetFirstTaskId() {
    return Ids.firstOf(Ids.TASK);
  }

  @SimpleFunction(description = "Id TERAKHIR DIBUAT. -1 bila kosong.")
  public int GetLastTaskId() {
    return Ids.lastOf(Ids.TASK);
  }

  @SimpleFunction(description = "Id yang dibuat TEPAT SEBELUM id ini. -1 bila tidak ada.")
  public int GetPreviousTaskId(int id) {
    return Ids.previousOf(Ids.TASK, id);
  }

  @SimpleFunction(description = "True bila id tugas ini aktif (jalan/jeda/alarm terpasang).")
  public boolean IsTaskIdActive(int id) {
    return Ids.isActive(Ids.TASK, id);
  }

  @SimpleFunction(description = "Hapus id tugas: batalkan jadwal + cabut alarm "
      + "sistem + lepas id (otomatis beres-beres).")
  public void RemoveTaskId(int id) {
    cancelStatic(context, id);
    synchronized (lock) {
      tasks.remove(Integer.valueOf(id));
    }
    unpersistAlarm(id);
    synchronized (lock) {
      alarms.remove(Integer.valueOf(id));
    }
    Ids.setActive(Ids.TASK, id, false);
    Ids.unregister(Ids.TASK, id);
  }

  @SimpleFunction(description = "CALL: Batalkan SEMUA tugas dalam-aplikasi (alarm "
      + "sistem tidak ikut; pakai CallCancelAllAlarms bila perlu). Tiap tugas "
      + "yang jalan menembak TaskCancelled.")
  public void CallCancelAllTasks() {
    ArrayList<Integer> all;
    synchronized (lock) {
      all = new ArrayList<Integer>(tasks.keySet());
    }
    for (int i = 0; i < all.size(); i = i + 1) {
      CallCancelTask(all.get(i).intValue());
    }
  }

  /** Batalkan jadwal + alarm sistem ber-id. Dipakai Tools_Base juga. */
  public static void cancelStatic(Context ctx, int id) {
    Task t = null;
    synchronized (lock) {
      t = tasks.get(Integer.valueOf(id));
      if (t != null) {
        t.status = "batal";
        t.paused = false;
        try {
          Handler h = mainHandler;
          if (h != null && t.runner != null) {
            h.removeCallbacks(t.runner);
          }
        } catch (Exception e) {
          // abaikan
        }
      }
    }
    try {
      if (ctx != null) {
        Object s = ctx.getSystemService(Context.ALARM_SERVICE);
        if (s instanceof AlarmManager) {
          AlarmManager am = (AlarmManager) s;
          try {
            Intent launch = null;
            try {
              launch = ctx.getPackageManager()
                  .getLaunchIntentForPackage(ctx.getPackageName());
            } catch (Exception e) {
              launch = null;
            }
            if (launch != null) {
              launch.setAction("com.fv7.tools.TASK_ALARM_" + id);
              int flags = PendingIntent.FLAG_UPDATE_CURRENT;
              if (Build.VERSION.SDK_INT >= 23) {
                flags = flags | PendingIntent.FLAG_IMMUTABLE;
              }
              PendingIntent pi = PendingIntent.getActivity(ctx, id, launch,
                  flags | PendingIntent.FLAG_NO_CREATE);
              if (pi != null) {
                try {
                  am.cancel(pi);
                } catch (Exception e) {
                  // abaikan
                }
                try {
                  pi.cancel();
                } catch (Exception e) {
                  // abaikan
                }
              }
            }
          } catch (Exception e) {
            // abaikan
          }
        }
      }
    } catch (Exception e) {
      // abaikan
    }
    updateActive(id);
  }

  /** Bersihkan total ber-id: batalkan jadwal + cabut alarm + buang info. */
  public static void clearStaticFull(Context ctx, int id) {
    cancelStatic(ctx, id);
    synchronized (lock) {
      tasks.remove(Integer.valueOf(id));
    }
    synchronized (lock) {
      alarms.remove(Integer.valueOf(id));
    }
    try {
      if (ctx != null) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS,
            Context.MODE_PRIVATE);
        if (p != null) {
          SharedPreferences.Editor e = p.edit();
          e.remove("epoch_" + id);
          e.remove("interval_" + id);
          e.remove("payload_" + id);
          e.remove("exact_" + id);
          try {
            Set<String> ids = new HashSet<String>(p.getStringSet("alarm_ids",
                new HashSet<String>()));
            ids.remove(String.valueOf(id));
            if (Build.VERSION.SDK_INT >= 11) {
              e.putStringSet("alarm_ids", ids);
            }
          } catch (Exception ex) {
            // abaikan
          }
          e.commit();
        }
      }
    } catch (Exception e) {
      // abaikan
    }
    Ids.setActive(Ids.TASK, id, false);
    Ids.unregister(Ids.TASK, id);
  }

  // ================= scheduler dalam-aplikasi =================

  private boolean startLocked(final Task t) {
    Handler h = main();
    if (h == null) {
      return false;
    }
    final Tools_Task self = this;
    if ("repeating".equals(t.kind)) {
      t.runner = new Runnable() {
        public void run() {
          boolean fireDone = false;
          boolean fireTick = false;
          int tickNow = 0;
          synchronized (lock) {
            if (!"jalan".equals(t.status)) {
              return;
            }
            t.tick = t.tick + 1;
            t.lastTickAt = System.currentTimeMillis();
            tickNow = t.tick;
            fireTick = true;
            if (t.repeatCount > 0 && t.tick >= t.repeatCount) {
              t.status = "selesai";
              t.success = true;
              t.message = "OK";
              fireDone = true;
            } else {
              t.nextFire = System.currentTimeMillis() + t.interval;
            }
          }
          if (fireTick && last != null) {
            try {
              last.TaskTick(t.id, tickNow, t.payload);
            } catch (Exception e) {
              // abaikan
            }
          }
          if (fireDone) {
            updateActive(t.id);
            if (last != null) {
              try {
                last.TaskFinished(t.id, true, t.payload, "OK");
              } catch (Exception e) {
                // abaikan
              }
            }
            return;
          }
          try {
            Handler hh = main();
            if (hh != null) {
              hh.postDelayed(this, t.interval);
            }
          } catch (Exception e) {
            // abaikan
          }
        }
      };
      t.nextFire = System.currentTimeMillis() + t.delay;
      try {
        h.postDelayed(t.runner, Math.max(0, t.delay));
      } catch (Exception e) {
        return false;
      }
    } else {
      t.runner = new Runnable() {
        public void run() {
          synchronized (lock) {
            if (!"jalan".equals(t.status)) {
              return;
            }
            t.status = "selesai";
            t.success = true;
            t.message = "OK";
            t.lastTickAt = System.currentTimeMillis();
          }
          updateActive(t.id);
          if (last != null) {
            try {
              last.TaskFinished(t.id, true, t.payload, "OK");
            } catch (Exception e) {
              // abaikan
            }
          }
        }
      };
      t.nextFire = System.currentTimeMillis() + t.delay;
      try {
        h.postDelayed(t.runner, Math.max(0, t.delay));
      } catch (Exception e) {
        return false;
      }
    }
    try {
      self.TaskStarted(t.id, t.payload);
    } catch (Exception e) {
      // abaikan
    }
    return true;
  }

  private boolean scheduleKind(int id, String kind, long delay,
      long interval, int repeatCount, String payload) {
    if (id < 1) {
      Shared.err(this, "Task", id, "id harus >= 1 (pakai CreateTaskId)");
      return false;
    }
    if (!Ids.register(Ids.TASK, id)) {
      Shared.err(this, "Task", id, "ID " + id + " sedang dipakai tugas lain");
      return false;
    }
    synchronized (lock) {
      Task old = tasks.get(Integer.valueOf(id));
      if (old != null && ("jalan".equals(old.status)
          || "jeda".equals(old.status))) {
        Shared.err(this, "Task", id, "ID " + id + " masih jalan "
            + "(Cancel/Remove dulu)");
        return false;
      }
      if (old != null) {
        try {
          if (mainHandler != null && old.runner != null) {
            mainHandler.removeCallbacks(old.runner);
          }
        } catch (Exception e) {
          // abaikan
        }
      }
      Task t = new Task();
      t.id = id;
      t.kind = kind;
      t.payload = payload == null ? "" : payload;
      t.delay = Math.max(0, delay);
      t.interval = Math.max(0, interval);
      t.repeatCount = repeatCount;
      t.tick = 0;
      t.status = "jalan";
      t.success = false;
      t.message = "";
      t.paused = false;
      tasks.put(Integer.valueOf(id), t);
      Ids.setActive(Ids.TASK, id, true);
      boolean ok = startLocked(t);
      if (!ok) {
        t.status = "gagal";
        t.message = "handler tidak ada";
        Shared.err(this, "Task", id, "penjadwal tidak tersedia");
        updateActive(id);
        return false;
      }
      return true;
    }
  }

  @SimpleFunction(description = "CALL: Jadwalkan tugas sekali jalan: id + jeda "
      + "mili-detik + payload teks (JSON via Tools_Base bila perlu). "
      + "Menembak TaskStarted lalu TaskFinished. Sukses menembak Started; gagal via ErrorOccurred.")
  public void CallScheduleOnce(int id, long delayMillis, String payload) {
    scheduleKind(id, "once", delayMillis, 0, 1, payload);
  }

  @SimpleFunction(description = "CALL: Jadwalkan tugas berulang: id + jeda awal + "
      + "interval + jumlah ulang (mis. 5) atau -1 = selamanya + payload. "
      + "Tiap tick menembak TaskTick; selesai menembak TaskFinished.")
  public void CallScheduleRepeating(int id, long initialDelayMillis,
      long intervalMillis, int repeatCount, String payload) {
    long iv = intervalMillis < MIN_INTERVAL ? MIN_INTERVAL : intervalMillis;
    scheduleKind(id, "repeating", initialDelayMillis, iv, repeatCount,
        payload);
  }

  @SimpleFunction(description = "CALL: Jadwalkan tugas pada jam epoch mili-detik "
      + "(lihat NowMillis). Lewat = langsung jalan.")
  public void CallScheduleAt(int id, long epochMillis, String payload) {
    long delay = epochMillis - System.currentTimeMillis();
    if (delay < 0) {
      delay = 0;
    }
    scheduleKind(id, "once", delay, 0, 1, payload);
  }

  @SimpleFunction(description = "SET: Jeda tugas ber-id (berlaku untuk once/repeating "
      + "yang belum selesai). Lanjut via SetResumeTask.")
  public void SetPauseTask(int id) {
    synchronized (lock) {
      Task t = tasks.get(Integer.valueOf(id));
      if (t == null || !"jalan".equals(t.status)) {
        return;
      }
      try {
        if (mainHandler != null && t.runner != null) {
          mainHandler.removeCallbacks(t.runner);
        }
      } catch (Exception e) {
        // abaikan
      }
      long left = t.nextFire - System.currentTimeMillis();
      t.remaining = left > 0 ? left : 0;
      t.status = "jeda";
      t.paused = true;
    }
    updateActive(id);
  }

  @SimpleFunction(description = "SET: Lanjutkan tugas yang dijeda (SetPauseTask).")
  public void SetResumeTask(int id) {
    Task t;
    synchronized (lock) {
      t = tasks.get(Integer.valueOf(id));
      if (t == null || !"jeda".equals(t.status)) {
        return;
      }
      t.status = "jalan";
      t.paused = false;
      long wait = t.remaining > 0 ? t.remaining : t.interval;
      if ("once".equals(t.kind) && t.remaining <= 0) {
        wait = 0;
      }
      if ("repeating".equals(t.kind) && t.remaining <= 0) {
        wait = t.interval;
      }
      t.nextFire = System.currentTimeMillis() + wait;
      t.remaining = 0;
      Handler h = main();
      if (h == null || t.runner == null) {
        t.status = "gagal";
        updateActive(id);
        return;
      }
      final Task ft = t;
      final long fWait = wait;
      Runnable wrap;
      if ("repeating".equals(ft.kind)) {
        wrap = new Runnable() {
          public void run() {
            synchronized (lock) {
              if (!"jalan".equals(ft.status)) {
                return;
              }
              ft.tick = ft.tick + 1;
              ft.lastTickAt = System.currentTimeMillis();
              if (ft.repeatCount > 0 && ft.tick >= ft.repeatCount) {
                ft.status = "selesai";
                ft.success = true;
                ft.message = "OK";
              } else {
                ft.nextFire = System.currentTimeMillis() + ft.interval;
              }
            }
            if (last != null) {
              try {
                last.TaskTick(ft.id, ft.tick, ft.payload);
              } catch (Exception e) {
                // abaikan
              }
            }
            boolean done;
            synchronized (lock) {
              done = "selesai".equals(ft.status);
            }
            if (done) {
              updateActive(ft.id);
              if (last != null) {
                try {
                  last.TaskFinished(ft.id, true, ft.payload, "OK");
                } catch (Exception e) {
                  // abaikan
                }
              }
              return;
            }
            try {
              Handler hh = main();
              if (hh != null) {
                hh.postDelayed(this, ft.interval);
              }
            } catch (Exception e) {
              // abaikan
            }
          }
        };
        ft.runner = wrap;
      } else {
        wrap = new Runnable() {
          public void run() {
            synchronized (lock) {
              if (!"jalan".equals(ft.status)) {
                return;
              }
              ft.status = "selesai";
              ft.success = true;
              ft.message = "OK";
              ft.lastTickAt = System.currentTimeMillis();
            }
            updateActive(ft.id);
            if (last != null) {
              try {
                last.TaskFinished(ft.id, true, ft.payload, "OK");
              } catch (Exception e) {
                // abaikan
              }
            }
          }
        };
        ft.runner = wrap;
      }
      try {
        h.postDelayed(wrap, Math.max(0, fWait));
      } catch (Exception e) {
        ft.status = "gagal";
        Shared.err(this, "Task", id, "resume gagal");
      }
    }
    updateActive(id);
  }

  @SimpleFunction(description = "CALL: Batalkan tugas ber-id (jadwal dalam-aplikasi "
      + "saja). Hasil lewat TaskCancelled.")
  public void CallCancelTask(int id) {
    boolean had = false;
    synchronized (lock) {
      Task t = tasks.get(Integer.valueOf(id));
      if (t != null && ("jalan".equals(t.status)
          || "jeda".equals(t.status))) {
        had = true;
        t.status = "batal";
        t.paused = false;
        try {
          if (mainHandler != null && t.runner != null) {
            mainHandler.removeCallbacks(t.runner);
          }
        } catch (Exception e) {
          // abaikan
        }
      }
    }
    updateActive(id);
    if (had) {
      try {
        TaskCancelled(id);
      } catch (Exception e) {
        // abaikan
      }
    } else {
      Shared.err(this, "Task", id, "tidak ada tugas jalan ID " + id);
    }
  }

  @SimpleFunction(description = "Waktu sekarang epoch mili-detik (untuk CallScheduleAt/SetAlarm).")
  public long NowMillis() {
    return System.currentTimeMillis();
  }

  // ================= polling tugas =================

  @SimpleFunction(description = "Fallback polling untuk event Task*: status "
      + "tugas ber-id. Salah satu: antri, jalan, jeda, selesai, batal, gagal, "
      + "alarm, atau tidak ada.")
  public String GetTaskStatus(int id) {
    synchronized (lock) {
      Task t = tasks.get(Integer.valueOf(id));
      if (t != null) {
        return t.status;
      }
      if (alarms.containsKey(Integer.valueOf(id))) {
        return "alarm";
      }
      return "tidak ada";
    }
  }

  @SimpleFunction(description = "Fallback polling untuk TaskFinished: list "
      + "[success, payload, message]. Tetap ada sampai RemoveTaskId.")
  public List<Object> GetTaskResult(int id) {
    ArrayList<Object> out = new ArrayList<Object>();
    synchronized (lock) {
      Task t = tasks.get(Integer.valueOf(id));
      if (t == null) {
        out.add(Boolean.valueOf(false));
        out.add("");
        out.add("tidak ada");
        return out;
      }
      out.add(Boolean.valueOf(t.success));
      out.add(t.payload == null ? "" : t.payload);
      out.add(t.message == null ? "" : t.message);
    }
    return out;
  }

  @SimpleFunction(description = "Fallback polling untuk TaskTick: list "
      + "[tickCount, repeatCount, lastTickAt, nextFireAt].")
  public List<Object> GetTaskProgress(int id) {
    ArrayList<Object> out = new ArrayList<Object>();
    synchronized (lock) {
      Task t = tasks.get(Integer.valueOf(id));
      if (t == null) {
        out.add(Integer.valueOf(0));
        out.add(Integer.valueOf(0));
        out.add(Long.valueOf(0));
        out.add(Long.valueOf(0));
        return out;
      }
      out.add(Integer.valueOf(t.tick));
      out.add(Integer.valueOf(t.repeatCount));
      out.add(Long.valueOf(t.lastTickAt));
      out.add(Long.valueOf(t.nextFire));
    }
    return out;
  }

  @SimpleFunction(description = "Id tugas dalam-aplikasi terakhir yang selesai/bunyi. -1 bila belum ada.")
  public int GetLastFiredTaskId() {
    synchronized (lock) {
      return lastFiredTaskId;
    }
  }

  @SimpleFunction(description = "Payload tugas terakhir yang selesai/bunyi.")
  public String GetLastFiredPayload() {
    synchronized (lock) {
      return lastFiredPayload;
    }
  }

  @SimpleFunction(description = "Waktu epoch tugas terakhir selesai/bunyi. 0 bila belum ada.")
  public long GetLastFiredTime() {
    synchronized (lock) {
      return lastFiredAt;
    }
  }

  // ================= extra per tugas =================

  @SimpleFunction(description = "SET: Simpan extra teks untuk tugas ber-id (mis. "
      + "url, nama file). Menimpa kunci yang sama. Tanpa feedback.")
  public void SetTaskExtra(int id, String key, String value) {
    synchronized (lock) {
      Task t = tasks.get(Integer.valueOf(id));
      if (t == null) {
        return;
      }
      String k = key == null ? "" : key.trim();
      if (k.length() == 0) {
        return;
      }
      t.extras.put(k, value == null ? "" : value);
    }
  }

  @SimpleFunction(description = "Baca extra tugas ber-id (default bila tidak ada).")
  public String GetTaskExtra(int id, String key, String defaultValue) {
    synchronized (lock) {
      Task t = tasks.get(Integer.valueOf(id));
      if (t == null) {
        return defaultValue == null ? "" : defaultValue;
      }
      String k = key == null ? "" : key.trim();
      String v = t.extras.get(k);
      if (v == null) {
        return defaultValue == null ? "" : defaultValue;
      }
      return v;
    }
  }

  @SimpleFunction(description = "Semua kunci extra tugas ber-id.")
  public List<String> GetTaskExtraKeys(int id) {
    ArrayList<String> out = new ArrayList<String>();
    synchronized (lock) {
      Task t = tasks.get(Integer.valueOf(id));
      if (t != null) {
        out.addAll(t.extras.keySet());
      }
    }
    return out;
  }

  @SimpleFunction(description = "Hapus satu extra tugas ber-id.")
  public void RemoveTaskExtra(int id, String key) {
    synchronized (lock) {
      Task t = tasks.get(Integer.valueOf(id));
      if (t == null) {
        return;
      }
      String k = key == null ? "" : key.trim();
      t.extras.remove(k);
    }
  }

  @SimpleFunction(description = "Hapus SEMUA extra tugas ber-id.")
  public void ClearTaskExtras(int id) {
    synchronized (lock) {
      Task t = tasks.get(Integer.valueOf(id));
      if (t != null) {
        t.extras.clear();
      }
    }
  }

  // ================= variabel global =================

  @SimpleFunction(description = "SET: Simpan variabel teks global (bisa dibaca tugas "
      + "mana pun + blok lain).")
  public void SetVar(String name, String value) {
    String n = name == null ? "" : name.trim();
    if (n.length() == 0) {
      return;
    }
    synchronized (lock) {
      vars.put(n, value == null ? "" : value);
    }
    try {
      SharedPreferences p = prefs(context);
      if (p != null) {
        p.edit().putString("var_" + n, value == null ? "" : value).commit();
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "Baca variabel global (default bila tidak ada).")
  public String GetVar(String name, String defaultValue) {
    String n = name == null ? "" : name.trim();
    synchronized (lock) {
      String v = vars.get(n);
      if (v != null) {
        return v;
      }
    }
    try {
      SharedPreferences p = prefs(context);
      if (p != null) {
        String v = p.getString("var_" + n, null);
        if (v != null) {
          return v;
        }
      }
    } catch (Exception e) {
      // abaikan
    }
    return defaultValue == null ? "" : defaultValue;
  }

  @SimpleFunction(description = "Hapus satu variabel global.")
  public void RemoveVar(String name) {
    String n = name == null ? "" : name.trim();
    synchronized (lock) {
      vars.remove(n);
    }
    try {
      SharedPreferences p = prefs(context);
      if (p != null) {
        p.edit().remove("var_" + n).commit();
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "Hapus SEMUA variabel global.")
  public void ClearVars() {
    synchronized (lock) {
      vars.clear();
    }
    try {
      SharedPreferences p = prefs(context);
      if (p != null) {
        Map<String, ?> all = p.getAll();
        SharedPreferences.Editor e = p.edit();
        for (Map.Entry<String, ?> en : all.entrySet()) {
          if (en.getKey().startsWith("var_")) {
            e.remove(en.getKey());
          }
        }
        e.commit();
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "Semua nama variabel global (URUT DIBUAT).")
  public List<String> GetAllVarNames() {
    ArrayList<String> out = new ArrayList<String>();
    synchronized (lock) {
      out.addAll(vars.keySet());
    }
    try {
      SharedPreferences p = prefs(context);
      if (p != null) {
        Map<String, ?> all = p.getAll();
        for (Map.Entry<String, ?> en : all.entrySet()) {
          String k = en.getKey();
          if (k.startsWith("var_")) {
            String n = k.substring(4);
            if (!out.contains(n)) {
              out.add(n);
            }
          }
        }
      }
    } catch (Exception e) {
      // abaikan
    }
    return out;
  }

  // ================= alarm sistem =================

  @SimpleFunction(description = "SET: Minta izin exact-alarm (Android 12+). Best-effort: "
      + "bila ditolak, alarm jalan inexact (tetap bunyi, bisa meleset).")
  public void SetRequestExactAlarmPermission() {
    try {
      if (Build.VERSION.SDK_INT >= 31) {
        Intent it = new Intent(
            android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
        try {
          it.setData(android.net.Uri.parse("package:"
              + context.getPackageName()));
        } catch (Exception e) {
          // abaikan
        }
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(it);
      }
    } catch (Exception e) {
      String m = e.getMessage();
    }
  }

  @SimpleFunction(description = "True bila boleh pasang exact-alarm "
      + "(Android <12 selalu true).")
  public boolean IsExactAlarmAllowed() {
    return canExact();
  }

  @SimpleFunction(description = "CALL: Pasang alarm SISTEM sekali bunyi: id + epoch "
      + "mili-detik + payload + exact (true = tepat waktu bila izin ada). "
      + "Bunyi = membuka aplikasi; baca via CheckStartupAlarm/AlarmFired. "
      + "Tersimpan otomatis (survive reboot via RestoreAlarms).")
  public void CallSetAlarm(int id, long epochMillis, String payload,
      boolean exact) {
    if (id < 1) {
      Shared.err(this, "Alarm", id, "id harus >= 1 (pakai CreateTaskId)");
      return;
    }
    if (!Ids.register(Ids.TASK, id)) {
      synchronized (lock) {
        if (!alarms.containsKey(Integer.valueOf(id))) {
          Shared.err(this, "Alarm", id, "ID " + id
              + " sedang dipakai tugas lain");
          return;
        }
      }
    }
    AlarmSpec a = new AlarmSpec();
    a.id = id;
    a.epoch = epochMillis;
    a.interval = 0;
    a.payload = payload == null ? "" : payload;
    a.exact = exact;
    boolean ok = armAlarm(a);
    if (!ok) {
      return;
    }
    synchronized (lock) {
      alarms.put(Integer.valueOf(id), a);
    }
    persistAlarm(a);
    Ids.setActive(Ids.TASK, id, true);
    return;
  }

  @SimpleFunction(description = "CALL: Pasang alarm SISTEM berulang: id + epoch "
      + "pertama + interval + payload. Interval <1 detik dibulatkan ke 1 detik "
      + "(hemat baterai, aturan Android).")
  public void CallSetRepeatingAlarm(int id, long firstEpochMillis,
      long intervalMillis, String payload) {
    if (id < 1) {
      Shared.err(this, "Alarm", id, "id harus >= 1 (pakai CreateTaskId)");
      return;
    }
    if (intervalMillis < MIN_INTERVAL) {
      Shared.err(this, "Alarm", id, "interval minimal 1000 ms");
      return;
    }
    if (!Ids.register(Ids.TASK, id)) {
      synchronized (lock) {
        if (!alarms.containsKey(Integer.valueOf(id))) {
          Shared.err(this, "Alarm", id, "ID " + id
              + " sedang dipakai tugas lain");
          return;
        }
      }
    }
    AlarmSpec a = new AlarmSpec();
    a.id = id;
    a.epoch = firstEpochMillis;
    a.interval = intervalMillis;
    a.payload = payload == null ? "" : payload;
    a.exact = false;
    boolean ok = armAlarm(a);
    if (!ok) {
      return;
    }
    synchronized (lock) {
      alarms.put(Integer.valueOf(id), a);
    }
    persistAlarm(a);
    Ids.setActive(Ids.TASK, id, true);
    return;
  }

  @SimpleFunction(description = "SET: Cabut alarm sistem ber-id (tugas dalam-aplikasi "
      + "dengan id sama tidak ikut; pakai CancelTask bila perlu).")
  public void SetCancelAlarm(int id) {
    cancelStatic(context, id);
    unpersistAlarm(id);
    synchronized (lock) {
      alarms.remove(Integer.valueOf(id));
    }
    updateActive(id);
  }

  @SimpleFunction(description = "SET: Cabut SEMUA alarm sistem Tools_Task.")
  public void SetCancelAllAlarms() {
    ArrayList<Integer> all;
    synchronized (lock) {
      all = new ArrayList<Integer>(alarms.keySet());
    }
    for (int i = 0; i < all.size(); i = i + 1) {
      SetCancelAlarm(all.get(i).intValue());
    }
  }

  @SimpleFunction(description = "True bila alarm sistem ber-id terpasang.")
  public boolean IsAlarmSet(int id) {
    synchronized (lock) {
      return alarms.containsKey(Integer.valueOf(id));
    }
  }

  @SimpleFunction(description = "Semua id alarm sistem yang terpasang "
      + "(URUT PEMBUATAN).")
  public List<Integer> GetPendingAlarmIds() {
    ArrayList<Integer> out = new ArrayList<Integer>();
    synchronized (lock) {
      for (Map.Entry<Integer, AlarmSpec> en : alarms.entrySet()) {
        out.add(en.getKey());
      }
    }
    return out;
  }

  @SimpleFunction(description = "Fallback polling alarm: list [epochMillis, "
      + "intervalMillis (0 = sekali), payload]. Kosong = tidak ada.")
  public List<Object> GetAlarmInfo(int id) {
    ArrayList<Object> out = new ArrayList<Object>();
    synchronized (lock) {
      AlarmSpec a = alarms.get(Integer.valueOf(id));
      if (a == null) {
        out.add(Long.valueOf(0));
        out.add(Long.valueOf(0));
        out.add("");
        return out;
      }
      out.add(Long.valueOf(a.epoch));
      out.add(Long.valueOf(a.interval));
      out.add(a.payload == null ? "" : a.payload);
    }
    return out;
  }

  @SimpleFunction(description = "CALL: WAJIB dipanggil di Screen.Initialize (dan "
      + "timer bila perlu): cek apakah aplikasi dibuka oleh alarm Tools_Task. "
      + "Tanpa return: bila dibuka oleh alarm dan datanya baru, menembak AlarmFired.")
  public void CallCheckStartupAlarm() {
    int found = -1;
    String pay = "";
    try {
      Intent it = activity.getIntent();
      if (it != null && it.hasExtra("taskId")) {
        found = it.getIntExtra("taskId", -1);
        try {
          pay = it.getStringExtra("payload");
        } catch (Exception e) {
          pay = "";
        }
        if (pay == null) {
          pay = "";
        }
      }
    } catch (Exception e) {
      found = -1;
    }
    if (found > 0) {
      synchronized (lock) {
        lastFiredTaskId = found;
        lastFiredPayload = pay;
        lastFiredAt = System.currentTimeMillis();
      }
      boolean fresh;
      synchronized (lock) {
        fresh = found != lastDeliveredAlarmId;
        if (fresh) {
          lastDeliveredAlarmId = found;
        }
      }
      if (fresh) {
        try {
          AlarmFired(found, pay);
        } catch (Exception e) {
          // abaikan
        }
      }
      synchronized (lock) {
        AlarmSpec a = alarms.get(Integer.valueOf(found));
        if (a != null && a.interval <= 0) {
          alarms.remove(Integer.valueOf(found));
          updateActive(found);
        }
      }
      try {
        unpersistOneShot(found);
      } catch (Exception e) {
        // abaikan
      }
      return;
    }
  }

  private void unpersistOneShot(int id) {
    synchronized (lock) {
      AlarmSpec a = alarms.get(Integer.valueOf(id));
      if (a != null && a.interval > 0) {
        return;
      }
    }
    unpersistAlarm(id);
  }

  @SimpleFunction(description = "CALL: Pasang ulang SEMUA alarm tersimpan (mis. "
      + "habis reboot: panggil di Screen.Initialize). Tanpa return.")
  public void CallRestoreAlarms() {
    try {
      SharedPreferences p = prefs(context);
      if (p == null) {
        return;
      }
      Set<String> ids = null;
      try {
        ids = p.getStringSet("alarm_ids", new HashSet<String>());
      } catch (Exception e) {
        ids = new HashSet<String>();
      }
      if (ids == null) {
        return;
      }
      ArrayList<String> copy = new ArrayList<String>(ids);
      for (int i = 0; i < copy.size(); i = i + 1) {
        int id = -1;
        try {
          id = Integer.parseInt(copy.get(i));
        } catch (Exception e) {
          continue;
        }
        if (id < 1) {
          continue;
        }
        long epoch = p.getLong("epoch_" + id, 0);
        long interval = p.getLong("interval_" + id, 0);
        String pay = p.getString("payload_" + id, "");
        boolean exact = p.getBoolean("exact_" + id, false);
        if (epoch <= 0 && interval <= 0) {
          continue;
        }
        long now = System.currentTimeMillis();
        if (interval <= 0 && epoch < now) {
          unpersistAlarm(id);
          continue;
        }
        if (interval > 0 && epoch < now) {
          epoch = now + interval;
        }
        AlarmSpec a = new AlarmSpec();
        a.id = id;
        a.epoch = epoch;
        a.interval = interval;
        a.payload = pay == null ? "" : pay;
        a.exact = exact;
        Ids.register(Ids.TASK, id);
        boolean ok = armAlarm(a);
        if (ok) {
          synchronized (lock) {
            alarms.put(Integer.valueOf(id), a);
          }
          persistAlarm(a);
          Ids.setActive(Ids.TASK, id, true);
        }
      }
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Alarm", 0, m == null ? "restore gagal" : m);
    }
  }

  // ================= notifikasi + wake =================

  @SimpleFunction(description = "SET: Tampilkan notifikasi ongoing (tetap) agar tugas "
      + "penting tidak dibunuh sistem. Tap membuka aplikasi.")
  public void SetShowOngoingNotification(int id, String title, String text) {
    ensureChannel();
    final String t = title == null ? "" : title;
    final String x = text == null ? "" : text;
    final int nid = id;
    try {
      activity.runOnUiThread(new Runnable() {
        public void run() {
          try {
            int icon = android.R.drawable.ic_dialog_info;
            try {
              int appIcon = context.getApplicationInfo().icon;
              if (appIcon != 0) {
                icon = appIcon;
              }
            } catch (Exception e) {
              // pakai bawaan
            }
            Notification.Builder b;
            if (Build.VERSION.SDK_INT >= 26) {
              b = new Notification.Builder(activity, CHANNEL_ID);
            } else {
              b = new Notification.Builder(activity);
            }
            b.setContentTitle(t).setContentText(x).setSmallIcon(icon)
                .setOngoing(true).setAutoCancel(false);
            try {
              Intent launch = context.getPackageManager()
                  .getLaunchIntentForPackage(context.getPackageName());
              if (launch != null) {
                int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                if (Build.VERSION.SDK_INT >= 23) {
                  flags = flags | PendingIntent.FLAG_IMMUTABLE;
                }
                PendingIntent pi = PendingIntent.getActivity(activity, nid,
                    launch, flags);
                b.setContentIntent(pi);
              }
            } catch (Exception e) {
              // tanpa tap-action
            }
            NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) {
              Shared.err(Tools_Task.this, "Notification", nid,
                  "layanan notifikasi tidak ada");
              return;
            }
            if (Build.VERSION.SDK_INT >= 16) {
              nm.notify(nid, b.build());
            } else {
              nm.notify(nid, b.getNotification());
            }
          } catch (SecurityException se) {
            Shared.err(Tools_Task.this, "Notification", nid,
                "izin notifikasi belum diberikan");
          } catch (Exception e) {
            String m = e.getMessage();
            Shared.err(Tools_Task.this, "Notification", nid,
                m == null ? "notifikasi gagal" : m);
          }
        }
      });
    } catch (Exception e) {
      String m = e.getMessage();
      Shared.err(this, "Notification", nid, m == null ? "notifikasi gagal" : m);
    }
  }

  @SimpleFunction(description = "SET: Hapus notifikasi ongoing ber-id.")
  public void SetHideOngoingNotification(int id) {
    try {
      NotificationManager nm = (NotificationManager) context
          .getSystemService(Context.NOTIFICATION_SERVICE);
      if (nm != null) {
        nm.cancel(id);
      }
    } catch (Exception e) {
      // abaikan
    }
  }

  @SimpleFunction(description = "SET: Jaga CPU tetap hidup (wake-lock parsial) agar "
      + "tugas tidak tidur. timeoutMillis 0 = sampai SetKeepAwake(false). "
      + "Pakai seperlunya (boros baterai).")
  public void SetKeepAwake(boolean on, long timeoutMillis) {
    try {
      if (on) {
        PowerManager pm = null;
        try {
          Object s = context.getSystemService(Context.POWER_SERVICE);
          if (s instanceof PowerManager) {
            pm = (PowerManager) s;
          }
        } catch (Exception e) {
          pm = null;
        }
        if (pm == null) {
          return;
        }
        synchronized (lock) {
          try {
            if (wakeLock != null && wakeLock.isHeld()) {
              try {
                wakeLock.release();
              } catch (Exception e) {
                // abaikan
              }
            }
          } catch (Exception e) {
            // abaikan
          }
          try {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "Fv7ToolsTask:lock");
            wakeLock.setReferenceCounted(false);
            if (timeoutMillis > 0) {
              wakeLock.acquire(timeoutMillis);
            } else {
              wakeLock.acquire();
            }
          } catch (Exception e) {
            String m = e.getMessage();
          }
        }
      } else {
        synchronized (lock) {
          try {
            if (wakeLock != null && wakeLock.isHeld()) {
              wakeLock.release();
            }
          } catch (Exception e) {
            // abaikan
          }
          wakeLock = null;
        }
      }
    } catch (Exception e) {
      String m = e.getMessage();
    }
  }

  @SimpleFunction(description = "True bila wake-lock sedang dipegang.")
  public boolean IsAwake() {
    synchronized (lock) {
      try {
        return wakeLock != null && wakeLock.isHeld();
      } catch (Exception e) {
        return false;
      }
    }
  }

  @SimpleFunction(description = "Fallback polling untuk event ErrorOccurred: "
      + "pesan error terakhir operasi ini (kosong bila belum ada).")
  public String LastError(String operation) {
    return Shared.lastError(operation);
  }

  // ================= EVENTS =================

  @SimpleEvent(description = "Tugas dalam-aplikasi dimulai: id + payload.")
  public void TaskStarted(int id, String payload) {
    EventDispatcher.dispatchEvent(this, "TaskStarted",
        Integer.valueOf(id), payload == null ? "" : payload);
  }

  @SimpleEvent(description = "Tugas berulang tick ke-n: id + tick + payload.")
  public void TaskTick(int id, int tick, String payload) {
    EventDispatcher.dispatchEvent(this, "TaskTick",
        Integer.valueOf(id), Integer.valueOf(tick),
        payload == null ? "" : payload);
    synchronized (lock) {
      lastFiredTaskId = id;
      lastFiredPayload = payload == null ? "" : payload;
      lastFiredAt = System.currentTimeMillis();
    }
  }

  @SimpleEvent(description = "Tugas selesai: success + payload + message.")
  public void TaskFinished(int id, boolean success, String payload,
      String message) {
    EventDispatcher.dispatchEvent(this, "TaskFinished",
        Integer.valueOf(id), Boolean.valueOf(success),
        payload == null ? "" : payload, message == null ? "" : message);
    synchronized (lock) {
      lastFiredTaskId = id;
      lastFiredPayload = payload == null ? "" : payload;
      lastFiredAt = System.currentTimeMillis();
    }
  }

  @SimpleEvent(description = "Tugas dibatalkan: id.")
  public void TaskCancelled(int id) {
    EventDispatcher.dispatchEvent(this, "TaskCancelled",
        Integer.valueOf(id));
  }

  @SimpleEvent(description = "Alarm sistem bunyi (aplikasi dibuka oleh alarm): "
      + "id + payload. Selalu cek juga via CheckStartupAlarm di Initialize.")
  public void AlarmFired(int id, String payload) {
    EventDispatcher.dispatchEvent(this, "AlarmFired",
        Integer.valueOf(id), payload == null ? "" : payload);
  }

  @SimpleEvent(description = "SATU-SATUNYA error Tools_Task. component = nama "
      + "operasi (bandingkan dengan blok OpTask/OpAlarm/OpVar/OpNotification).")
  public void ErrorOccurred(String component, int id, String message) {
    EventDispatcher.dispatchEvent(this, "ErrorOccurred",
        component, Integer.valueOf(id), message);
  }

  // ================= OP =================

  @SimpleProperty(description = "Nama operasi Task (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpTask() {
    return "Task";
  }

  @SimpleProperty(description = "Nama operasi Alarm (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpAlarm() {
    return "Alarm";
  }

  @SimpleProperty(description = "Nama operasi Var (untuk if di ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpVar() {
    return "Var";
  }

  @SimpleProperty(description = "Nama operasi Notification (untuk if di "
      + "ErrorOccurred).",
      category = PropertyCategory.BEHAVIOR)
  public String OpNotification() {
    return "Notification";
  }

  // ================= destroy =================

  public void onDestroy() {
    try {
      synchronized (lock) {
        for (Map.Entry<Integer, Task> en : tasks.entrySet()) {
          try {
            Task t = en.getValue();
            if (t != null && t.runner != null && mainHandler != null) {
              mainHandler.removeCallbacks(t.runner);
            }
          } catch (Exception e) {
            // abaikan per-id
          }
        }
      }
    } catch (Exception e) {
      // abaikan
    }
    try {
      synchronized (lock) {
        if (wakeLock != null && wakeLock.isHeld()) {
          try {
            wakeLock.release();
          } catch (Exception e) {
            // abaikan
          }
        }
        wakeLock = null;
      }
    } catch (Exception e) {
      // abaikan
    }
  }
}
