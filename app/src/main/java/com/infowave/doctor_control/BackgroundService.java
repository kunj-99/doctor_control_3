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
 * - Hourly "active" ping stub (implement API if needed).
 * - Every 60s guard to ensure LiveLocationManager is running during an active appointment.
 * - Start this service when doctor goes online / accepts an appointment.
 */
public class BackgroundService extends Service {

    public static final String TAG = "BackgroundService";

    private Handler handler;
    private Runnable activeTick;
    private Runnable trackingGuard;

    // Intervals
    private static final long ACTIVE_PING_EVERY = 60L * 60L * 1000L; // 1 hour
    private static final long TRACK_GUARD_EVERY = 60L * 1000L;       // 1 minute

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(getMainLooper());

        activeTick = new Runnable() {
            @Override
            public void run() {
                try { keepDoctorActive(); }
                finally { handler.postDelayed(this, ACTIVE_PING_EVERY); }
            }
        };

        trackingGuard = new Runnable() {
            @Override
            public void run() {
                try { ensureLocationTrackingAlive(); }
                finally { handler.postDelayed(this, TRACK_GUARD_EVERY); }
            }
        };

        handler.post(activeTick);
        handler.post(trackingGuard);
        Log.d(TAG, "Created → scheduled hourly active ping & tracking guard.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // keep alive
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacks(activeTick);
            handler.removeCallbacks(trackingGuard);
        }
        setDoctorInactive();
        Log.d(TAG, "Destroyed → removed callbacks; marked doctor inactive.");
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    /* ===== Stubs to integrate server status if you want ===== */

    private void keepDoctorActive() {
        Log.d(TAG, "Active ping → implement API if needed.");
        // TODO: Volley/Retrofit call to mark doctor active on server
    }

    private void setDoctorInactive() {
        Log.d(TAG, "Inactive ping → implement API if needed.");
        // TODO: Volley/Retrofit call to mark doctor inactive on server
    }

    /* ===== Guard logic ===== */

    private void ensureLocationTrackingAlive() {
        Context ctx = getApplicationContext();
        SharedPreferences prefs = ctx.getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
        int doctorId = prefs.getInt("doctor_id", -1);
        String appointmentId = prefs.getString("ongoing_appointment_id", null);

        if (doctorId != -1 && appointmentId != null) {
            if (!LiveLocationManager.getInstance().isTracking()) {
                LiveLocationManager.getInstance().startLocationUpdates(ctx);
                Log.d(TAG, "Guard: Tracking was off, restarted foreground tracking.");
            }
        } else {
            if (LiveLocationManager.getInstance().isTracking()) {
                LiveLocationManager.getInstance().stopLocationUpdates(ctx);
                Log.d(TAG, "Guard: No active appointment, stopped tracking.");
            }
        }
    }
}
