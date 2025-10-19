package com.infowave.doctor_control;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

/**
 * BackgroundService:
 * - Periodically pings your server to mark doctor as active (stubbed).
 * - Periodically re-checks that LiveLocationManager tracking is running.
 *   If not, it attempts to start it (only if doctor_id and appointment_id exist).
 *
 * Start this service when doctor goes online / accepts an appointment.
 */
public class BackgroundService extends Service {

    public static final String TAG = BackgroundService.class.getSimpleName();

    private Handler handler;
    private Runnable activeTick;
    private Runnable trackingGuard;

    // Intervals (ms)
    private static final long ACTIVE_PING_EVERY = 60L * 60L * 1000L; // 1 hour
    private static final long TRACK_GUARD_EVERY = 60L * 1000L;       // 1 minute

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(getMainLooper());

        // 1) Hourly "doctor is active" ping
        activeTick = new Runnable() {
            @Override
            public void run() {
                try {
                    keepDoctorActive();
                } finally {
                    handler.postDelayed(this, ACTIVE_PING_EVERY);
                }
            }
        };

        // 2) Guard to ensure tracking is still on (self-heal)
        trackingGuard = new Runnable() {
            @Override
            public void run() {
                try {
                    ensureLocationTrackingAlive();
                } finally {
                    handler.postDelayed(this, TRACK_GUARD_EVERY);
                }
            }
        };

        handler.post(activeTick);
        handler.post(trackingGuard);
        Log.d(TAG, "BackgroundService created: guards scheduled.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Keep running
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacks(activeTick);
            handler.removeCallbacks(trackingGuard);
        }
        setDoctorInactive();
        Log.d(TAG, "BackgroundService destroyed; set doctor inactive.");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /** Stub: replace with real server call if needed */
    private void keepDoctorActive() {
        Log.d(TAG, "Keeping doctor active (ping server).");
        // TODO: Implement your real ping → e.g., Volley/Retrofit call.
    }

    /** Stub: replace with real server call if needed */
    private void setDoctorInactive() {
        Log.d(TAG, "Setting doctor inactive (server update).");
        // TODO: Implement your real inactive call.
    }

    /** Ensures live tracking remains running if context is valid. */
    private void ensureLocationTrackingAlive() {
        Context ctx = getApplicationContext();
        SharedPreferences prefs = ctx.getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
        int doctorId = prefs.getInt("doctor_id", -1);
        String appointmentId = prefs.getString("ongoing_appointment_id", null);

        if (doctorId != -1 && appointmentId != null) {
            if (!LiveLocationManager.getInstance().isTracking()) {
                LiveLocationManager.getInstance().startLocationUpdates(ctx);
                Log.d(TAG, "Tracking guard: restarted LiveLocationManager.");
            }
        } else {
            // If not in an active appointment, ensure tracking is off.
            if (LiveLocationManager.getInstance().isTracking()) {
                LiveLocationManager.getInstance().stopLocationUpdates(ctx);
                Log.d(TAG, "Tracking guard: no active appointment; stopped tracking.");
            }
        }
    }
}
