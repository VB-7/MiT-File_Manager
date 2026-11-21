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
    version = 23,
    description = "Tools_Task: penjadwal full-stack ber-id. Tugas dalam-aplikasi "
        + "(sekali/berulang/terjadwal) + alarm sistem via AlarmManager (buka "
        + "aplikasi saat bunyi) + notifikasi ongoing + wake-lock + variabel/extra "
        + "per tugas + persistensi alarm. Gudang Var/Extra tinggal di sini karena "
        + "tersimpan di HP (survive reboot) + terbaca saat alarm menyalakan aplikasi. "
        + "Id dikelola CreateTaskId. Tanpa Service/receiver khusus (aman "
        + "manifest, lolos Play). Setiap event ada fallback polling "
        + "(GetTaskStatus/Result/Progress, GetAlarmInfo, CheckStartupAlarm). "
        + "List id URUT PEMBUATAN. WAJIB di Screen.Initialize: CallCheckStartupAlarm "
        + "lalu CallRestoreAlarms (alarm TIDAK hidup sendiri habis reboot). DENAH "
        + "BERHENTI: jeda sementara -> SetPauseTask; batalkan jadwal tapi alarm "
        + "tetap -> CallCancelTask; cabut alarm tapi jadwal tetap -> SetCancelAlarm; "
        + "buang selamanya -> RemoveTaskId. Konvensi awalan: Is = boolean (colok ke "
        + "if), Call = async + hasil lewat event, Set = tanpa feedback sukses "
        + "(gagal via ErrorOccurred), Get = nilai balik sinkron.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    androidMinSdk = 21,
    iconName = "aiwebres/tools_task.jpg",
    helpUrl = "https://github.com/VB-7/MiT-File_Manager")
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