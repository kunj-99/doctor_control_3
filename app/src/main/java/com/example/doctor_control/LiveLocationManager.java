package com.example.doctor_control;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.*;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class LiveLocationManager {

    private static final String TAG = "LiveLocationManager";
    private static final String LIVE_LOCATION_URL = "http://sxm.a58.mytemp.website/update_live_location.php";
    private static final String CHANNEL_ID = "LiveTrackingChannel";

    private static LiveLocationManager instance;
    private boolean isStarted = false;

    private LiveLocationManager() {}

    public static LiveLocationManager getInstance() {
        if (instance == null) instance = new LiveLocationManager();
        return instance;
    }

    public void startLocationUpdates(Context context) {
        if (isStarted) {
            Log.d(TAG, "[INFO] Location tracking already running.");
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
        int doctorId = prefs.getInt("doctor_id", -1);
        String appointmentId = prefs.getString("ongoing_appointment_id", null);

        Log.d(TAG, "[START] doctor_id: " + doctorId + ", appointment_id: " + appointmentId);

        if (doctorId == -1 || appointmentId == null) {
            Log.w(TAG, "[ERROR] Cannot start tracking. Missing doctorId or appointmentId.");
            return;
        }

        isStarted = true;
        Intent intent = new Intent(context, LocationForegroundService.class);
        ContextCompat.startForegroundService(context, intent);
        Log.d(TAG, "[START] Foreground location service started.");
    }

    public void stopLocationUpdates(Context context) {
        if (!isStarted) {
            Log.d(TAG, "[INFO] No location tracking active.");
            return;
        }
        Intent intent = new Intent(context, LocationForegroundService.class);
        context.stopService(intent);
        isStarted = false;
        Log.d(TAG, "[STOP] Foreground location service stopped.");
    }

    public boolean isTracking() {
        return isStarted;
    }

    public String getLocationLogs(Context context) {
        SharedPreferences sp = context.getSharedPreferences("LocationLogs", Context.MODE_PRIVATE);
        return sp.getString("history", "No logs yet.");
    }

    public void clearLogs(Context context) {
        context.getSharedPreferences("LocationLogs", Context.MODE_PRIVATE).edit().clear().apply();
        Log.d(TAG, "[LOG] Location logs cleared.");
    }

    // 🔥 Embedded Foreground Service
    public static class LocationForegroundService extends Service {

        private FusedLocationProviderClient locationClient;
        private LocationCallback locationCallback;

        @Override
        public void onCreate() {
            super.onCreate();
            Log.d(TAG, "[SERVICE] onCreate triggered");

            createNotificationChannel();
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Tracking Location")
                    .setContentText("Live location tracking active")
                    .setSmallIcon(R.drawable.location) // replace with your icon
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build();

            startForeground(101, notification);

            SharedPreferences prefs = getSharedPreferences("DoctorPrefs", MODE_PRIVATE);
            int doctorId = prefs.getInt("doctor_id", -1);
            String appointmentId = prefs.getString("ongoing_appointment_id", null);

            Log.d(TAG, "[SERVICE] doctorId=" + doctorId + ", appointmentId=" + appointmentId);

            if (doctorId == -1 || appointmentId == null) {
                Log.w(TAG, "[SERVICE] Missing doctor ID or appointment ID. Stopping service.");
                stopSelf();
                return;
            }

            locationClient = LocationServices.getFusedLocationProviderClient(this);

            LocationRequest request = LocationRequest.create()
                    .setInterval(15000)
                    .setFastestInterval(10000)
                    .setSmallestDisplacement(20)
                    .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult result) {
                    if (result != null && result.getLastLocation() != null) {
                        double lat = result.getLastLocation().getLatitude();
                        double lon = result.getLastLocation().getLongitude();

                        Log.d(TAG, "[LOCATION] Lat: " + lat + ", Lon: " + lon);
                        sendLocationToServer(getApplicationContext(), String.valueOf(doctorId), appointmentId, lat, lon);
                    } else {
                        Log.w(TAG, "[LOCATION] Location result is null.");
                    }
                }
            };

            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                locationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
                Log.d(TAG, "[SERVICE] Location updates started.");
            } else {
                Log.w(TAG, "[SERVICE] Location permission not granted.");
            }
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            Log.d(TAG, "[SERVICE] onStartCommand received");
            return START_STICKY;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (locationClient != null && locationCallback != null) {
                locationClient.removeLocationUpdates(locationCallback);
                Log.d(TAG, "[SERVICE] Location updates removed.");
            }
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        private void sendLocationToServer(Context context, String doctorId, String appointmentId, double lat, double lon) {
            StringRequest request = new StringRequest(Request.Method.POST, LIVE_LOCATION_URL,
                    response -> {
                        Log.d(TAG, "[SEND] Server Response: " + response);
                        logLocation(context, appointmentId, lat, lon);
                    },
                    error -> Log.e(TAG, "[SEND] Volley error: " + error.getMessage())) {
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

        private void logLocation(Context context, String appointmentId, double lat, double lon) {
            SharedPreferences sp = context.getSharedPreferences("LocationLogs", Context.MODE_PRIVATE);
            String oldLogs = sp.getString("history", "");
            String timestamp = DateFormat.getDateTimeInstance().format(new Date());

            String newLog = timestamp + " | Appt: " + appointmentId + " | Lat: " + lat + " | Lon: " + lon;
            sp.edit().putString("history", oldLogs + newLog + "\n").apply();

            Log.d(TAG, "[LOG] " + newLog);
        }

        private void createNotificationChannel() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Live Location Tracking",
                        NotificationManager.IMPORTANCE_LOW
                );
                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                }
            }
        }
    }
}
