package com.infowave.doctor_control;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;

import android.os.Build;
import android.os.HandlerThread;
import android.os.IBinder;
import android.content.pm.ServiceInfo;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Foreground service से continuous GPS भेजता है (Android नियमों के अनुसार).
 * - Notification non-dismissable (swipe से नहीं हटेगा)
 * - App swipe/kill पर onTaskRemoved से auto-restart
 * - Boot/Update के बाद auto-restart (inner BootRestartReceiver)
 * - Slow/No net पर offline queue + auto-flush
 */
public class LiveLocationManager {

    private static final String LIVE_LOCATION_URL = ApiConfig.endpoint("update_live_location.php");
    private static final String CHANNEL_ID = "LiveTrackingChannel";
    private static final int NOTIF_ID = 101;

    private static LiveLocationManager instance;
    private boolean isStarted = false;

    private LiveLocationManager() {}

    public static synchronized LiveLocationManager getInstance() {
        if (instance == null) instance = new LiveLocationManager();
        return instance;
    }

    /** Appointment accept/online होते ही call करें */
    public void startLocationUpdates(Context context) {
        if (isStarted) return;

        SharedPreferences prefs = context.getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
        int doctorId = prefs.getInt("doctor_id", -1);
        String appointmentId = prefs.getString("ongoing_appointment_id", null);
        if (doctorId == -1 || appointmentId == null) return;

        isStarted = true;
        Intent intent = new Intent(context, LocationForegroundService.class);
        ContextCompat.startForegroundService(context, intent);
    }

    /** Appointment complete/cancel होते ही call करें */
    public void stopLocationUpdates(Context context) {
        if (!isStarted) return;
        Intent intent = new Intent(context, LocationForegroundService.class);
        context.stopService(intent);
        isStarted = false;
    }

    public boolean isTracking() { return isStarted; }

    /** ===== Foreground Service ===== */
    public static class LocationForegroundService extends Service {

        private FusedLocationProviderClient locationClient;
        private LocationCallback locationCallback;
        private HandlerThread handlerThread;

        // Offline queue keys
        private static final String Q_PREF = "LL_QUEUE_PREF";
        private static final String Q_KEY  = "pending_updates";

        private ConnectivityManager connectivityManager;
        private ConnectivityManager.NetworkCallback netCallback;

        @Override
        public void onCreate() {
            super.onCreate();

            createNotificationChannel();
            Notification n = buildNotification("Live location tracking active");

            // Foreground with location type (Android 10+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                int type = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
                startForeground(NOTIF_ID, n, type);
            } else {
                startForeground(NOTIF_ID, n);
            }

            // Battery optimization prompt (OEM killers)
            BatteryOptHelper.requestIgnoreBatteryOptimizationsIfNeeded(this);

            // Validate tracking state
            SharedPreferences prefs = getSharedPreferences("DoctorPrefs", MODE_PRIVATE);
            int doctorId = prefs.getInt("doctor_id", -1);
            String appointmentId = prefs.getString("ongoing_appointment_id", null);
            if (doctorId == -1 || appointmentId == null) { stopSelf(); return; }

            locationClient = LocationServices.getFusedLocationProviderClient(this);

            LocationRequest request = LocationRequest.create()
                    .setInterval(30_000)           // 30s
                    .setFastestInterval(25_000)    // 25s
                    .setSmallestDisplacement(10f)  // 10m
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult result) {
                    if (result == null || result.getLastLocation() == null) return;
                    double lat = result.getLastLocation().getLatitude();
                    double lon = result.getLastLocation().getLongitude();
                    pushUpdateOrQueue(String.valueOf(doctorId), appointmentId, lat, lon);
                }
            };

            handlerThread = new HandlerThread("LiveLocationHandler");
            handlerThread.start();

            // Permissions
            boolean hasFine = ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED;
            if (!hasFine) { stopSelf(); return; }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                boolean hasFgLoc = ContextCompat.checkSelfPermission(
                        this, "android.permission.FOREGROUND_SERVICE_LOCATION"
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED;
                if (!hasFgLoc) { stopSelf(); return; }
            }

            // Start updates
            locationClient.requestLocationUpdates(request, locationCallback, handlerThread.getLooper());

            // Network callback: ऑनलाइन होते ही queue flush
            connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                netCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        flushQueuedUpdates();
                    }
                };
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    connectivityManager.registerDefaultNetworkCallback(netCallback);
                }
            }
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            return START_STICKY;
        }

        @Override
        public void onTaskRemoved(Intent rootIntent) {
            // Recents से हटाने पर restart
            Intent restart = new Intent(getApplicationContext(), LocationForegroundService.class);
            restart.setPackage(getPackageName());
            ContextCompat.startForegroundService(getApplicationContext(), restart);
            super.onTaskRemoved(rootIntent);
        }

        @Override
        public void onDestroy() {
            if (locationClient != null && locationCallback != null) {
                locationClient.removeLocationUpdates(locationCallback);
            }
            if (handlerThread != null) handlerThread.quitSafely();
            if (connectivityManager != null && netCallback != null) {
                try { connectivityManager.unregisterNetworkCallback(netCallback); } catch (Exception ignored) {}
            }
            super.onDestroy();
        }

        @Override
        public IBinder onBind(Intent intent) { return null; }

        /** --- Networking helpers --- */

        private boolean isNetworkUp() {
            try {
                ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm == null) return false;
                Network nw = cm.getActiveNetwork();
                if (nw == null) return false;
                NetworkCapabilities caps = cm.getNetworkCapabilities(nw);
                return caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
            } catch (Exception e) { return false; }
        }

        private void pushUpdateOrQueue(String doctorId, String appointmentId, double lat, double lon) {
            if (isNetworkUp()) {
                sendLocationToServer(getApplicationContext(), doctorId, appointmentId, lat, lon, 0);
            } else {
                queueUpdate(doctorId, appointmentId, lat, lon);
            }
        }

        private void sendLocationToServer(Context ctx, String doctorId, String appointmentId, double lat, double lon, int attempt) {
            StringRequest req = new StringRequest(
                    Request.Method.POST,
                    LIVE_LOCATION_URL,
                    response -> { /* success: do nothing */ },
                    error -> {
                        // Exponential backoff: 0.5s, 1s, 2s, 4s ... up to ~32s
                        if (attempt < 6) {
                            long delay = (long) (500 * Math.pow(2, attempt));
                            new android.os.Handler(getMainLooper()).postDelayed(
                                    () -> sendLocationToServer(ctx, doctorId, appointmentId, lat, lon, attempt + 1),
                                    delay
                            );
                        } else {
                            // Still failing → queue it
                            queueUpdate(doctorId, appointmentId, lat, lon);
                        }
                    }
            ) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> p = new HashMap<>();
                    p.put("doctor_id", doctorId);
                    p.put("appointment_id", appointmentId);
                    p.put("latitude", String.valueOf(lat));
                    p.put("longitude", String.valueOf(lon));
                    return p;
                }
            };
            // Reasonable retry policy for transient server errors
            req.setRetryPolicy(new DefaultRetryPolicy(
                    10_000, // timeout
                    0,      // volley-level retries (we do our own)
                    1.0f
            ));
            Volley.newRequestQueue(ctx.getApplicationContext()).add(req);
        }

        private void queueUpdate(String doctorId, String appointmentId, double lat, double lon) {
            try {
                SharedPreferences sp = getSharedPreferences(Q_PREF, MODE_PRIVATE);
                JSONArray arr = new JSONArray(sp.getString(Q_KEY, "[]"));
                JSONObject o = new JSONObject();
                o.put("doctor_id", doctorId);
                o.put("appointment_id", appointmentId);
                o.put("lat", lat);
                o.put("lon", lon);
                o.put("ts", System.currentTimeMillis());
                arr.put(o);

                // Keep queue bounded (e.g., last 200 entries)
                if (arr.length() > 200) {
                    JSONArray trimmed = new JSONArray();
                    for (int i = arr.length() - 200; i < arr.length(); i++) {
                        trimmed.put(arr.getJSONObject(i));
                    }
                    arr = trimmed;
                }
                sp.edit().putString(Q_KEY, arr.toString()).apply();
            } catch (Exception ignored) {}
        }

        private void flushQueuedUpdates() {
            try {
                SharedPreferences sp = getSharedPreferences(Q_PREF, MODE_PRIVATE);
                String s = sp.getString(Q_KEY, "[]");
                JSONArray arr = new JSONArray(s);
                if (arr.length() == 0) return;

                // Send sequentially to keep order
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    String d = o.getString("doctor_id");
                    String a = o.getString("appointment_id");
                    double lt = o.getDouble("lat");
                    double ln = o.getDouble("lon");
                    sendLocationToServer(getApplicationContext(), d, a, lt, ln, 0);
                }
                sp.edit().putString(Q_KEY, "[]").apply();
            } catch (JSONException ignored) {}
        }

        /** --- Notification helpers --- */

        private void createNotificationChannel() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Live Location Tracking",
                        NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("Keeps live location tracking running.");
                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) manager.createNotificationChannel(channel);
            }
        }

        private Notification buildNotification(String content) {
            Intent open = new Intent(this, MainActivity.class);
            open.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pi = PendingIntent.getActivity(
                    this, 0, open,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );

            NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.location)
                    .setContentTitle("Doctor At Home — Live Tracking")
                    .setContentText(content)
                    .setContentIntent(pi)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setOngoing(true)       // non-dismissable
                    .setAutoCancel(false);

            if (Build.VERSION.SDK_INT >= 34) {
                b.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
            }

            Notification n = b.build();
            n.flags |= Notification.FLAG_NO_CLEAR;      // swipe से clear न हो
            n.flags |= Notification.FLAG_ONGOING_EVENT; // ongoing दिखे
            return n;
        }
    }

    /** ===== Battery opt helper (same file) ===== */
    public static class BatteryOptHelper {
        public static void requestIgnoreBatteryOptimizationsIfNeeded(Context ctx) {
            try {
                PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
                String pkg = ctx.getPackageName();
                if (pm != null && !pm.isIgnoringBatteryOptimizations(pkg)) {
                    Intent i = new Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    i.setData(Uri.parse("package:" + pkg));
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(i);
                }
            } catch (Exception ignored) {}
        }
    }

    /** ===== Boot/Update auto-restart (inner receiver; no extra file) ===== */
    public static class BootRestartReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            SharedPreferences prefs = context.getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
            int doctorId = prefs.getInt("doctor_id", -1);
            String appointmentId = prefs.getString("ongoing_appointment_id", null);
            if (doctorId != -1 && appointmentId != null) {
                Intent svc = new Intent(context, LiveLocationManager.LocationForegroundService.class);
                ContextCompat.startForegroundService(context, svc);
            }
        }
    }
}
