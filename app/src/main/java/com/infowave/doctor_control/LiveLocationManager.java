package com.infowave.doctor_control;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.os.Build;
import android.os.HandlerThread;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.HashMap;
import java.util.Map;

/**
 * LiveLocationManager is a simple singleton facade that starts/stops a robust
 * foreground location service (LocationForegroundService) which:
 *  - Keeps sending location even if the app UI is closed.
 *  - Self-heals: if missing prefs/permissions it stops; callers can start again.
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

    public void startLocationUpdates(Context context) {
        if (isStarted) return;

        SharedPreferences prefs = context.getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
        int doctorId = prefs.getInt("doctor_id", -1);
        String appointmentId = prefs.getString("ongoing_appointment_id", null);

        if (doctorId == -1 || appointmentId == null) return; // nothing to track

        isStarted = true;
        Intent intent = new Intent(context, LocationForegroundService.class);
        ContextCompat.startForegroundService(context, intent);
    }

    public void stopLocationUpdates(Context context) {
        if (!isStarted) return;
        Intent intent = new Intent(context, LocationForegroundService.class);
        context.stopService(intent);
        isStarted = false;
    }

    public boolean isTracking() {
        return isStarted;
    }

    /**
     * Foreground service that keeps pushing GPS updates to the server.
     * Runs independently of UI; safe to start from anywhere.
     */
    public static class LocationForegroundService extends Service {

        private FusedLocationProviderClient locationClient;
        private LocationCallback locationCallback;
        private HandlerThread handlerThread;

        @Override
        public void onCreate() {
            super.onCreate();

            createNotificationChannel();
            startForeground(NOTIF_ID, buildNotification("Live location tracking active"));

            SharedPreferences prefs = getSharedPreferences("DoctorPrefs", MODE_PRIVATE);
            int doctorId = prefs.getInt("doctor_id", -1);
            String appointmentId = prefs.getString("ongoing_appointment_id", null);

            if (doctorId == -1 || appointmentId == null) {
                stopSelf();
                return;
            }

            locationClient = LocationServices.getFusedLocationProviderClient(this);

            LocationRequest request = LocationRequest.create()
                    .setInterval(15_000)           // 15s
                    .setFastestInterval(8_000)     // 8s
                    .setSmallestDisplacement(10f)  // 10m
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult result) {
                    if (result == null || result.getLastLocation() == null) return;
                    double lat = result.getLastLocation().getLatitude();
                    double lon = result.getLastLocation().getLongitude();
                    sendLocationToServer(getApplicationContext(), String.valueOf(doctorId), appointmentId, lat, lon);
                }
            };

            handlerThread = new HandlerThread("LiveLocationHandler");
            handlerThread.start();

            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        ContextCompat.checkSelfPermission(this, "android.permission.FOREGROUND_SERVICE_LOCATION")
                                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    stopSelf();
                    return;
                }

                locationClient.requestLocationUpdates(request, locationCallback, handlerThread.getLooper());
            } else {
                stopSelf();
            }
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            // Keep running unless explicitly stopped
            return START_STICKY;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (locationClient != null && locationCallback != null) {
                locationClient.removeLocationUpdates(locationCallback);
            }
            if (handlerThread != null) handlerThread.quitSafely();
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        private void sendLocationToServer(Context context, String doctorId, String appointmentId, double lat, double lon) {
            StringRequest request = new StringRequest(Request.Method.POST, LIVE_LOCATION_URL,
                    response -> { /* silent */ },
                    error -> { /* silent */ }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("doctor_id", doctorId);
                    params.put("appointment_id", appointmentId);
                    params.put("latitude", String.valueOf(lat));
                    params.put("longitude", String.valueOf(lon));
                    return params;
                }
            };
            Volley.newRequestQueue(context).add(request);
        }

        private void createNotificationChannel() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID, "Live Location Tracking", NotificationManager.IMPORTANCE_LOW);
                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) manager.createNotificationChannel(channel);
            }
        }

        private Notification buildNotification(String content) {
            Intent openApp = new Intent(this, MainActivity.class);
            openApp.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pi = PendingIntent.getActivity(this, 0, openApp,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

            return new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Doctor At Home — Live Tracking")
                    .setContentText(content)
                    .setSmallIcon(R.drawable.location)
                    .setContentIntent(pi)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(true)
                    .build();
        }
    }
}
