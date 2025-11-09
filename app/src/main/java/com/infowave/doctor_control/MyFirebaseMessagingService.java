package com.infowave.doctor_control;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.installations.FirebaseInstallations;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Doctor-side FCM with patient-style custom sound channel (uses res/raw/sound).
 * Make sure you have a file at: app/src/main/res/raw/sound.mp3 (or .wav)
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "DOC_FCM";
    // Use a stable channel id. (You can keep your previous ID, but the sound is tied to the channel.)
    private static final String CHANNEL_ID = "requests_channel";

    // ───────────────────────── TOKEN HANDLING ─────────────────────────

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "onNewToken() -> token_len=" + (token == null ? 0 : token.length()));
        try {
            FirebaseInstallations.getInstance().getId()
                    .addOnSuccessListener(fid -> Log.d(TAG, "FID=" + fid))
                    .addOnFailureListener(e -> Log.w(TAG, "FID fetch failed", e));
        } catch (Throwable t) {
            Log.w(TAG, "FID fetch exception", t);
        }
        DoctorFcmTokenHelper.upload(getApplicationContext(), token);
    }

    /** Call once from Splash/Login after auth. */
    public static void refreshTokenIfNeeded(Context ctx) {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    Log.d(TAG, "refreshTokenIfNeeded() -> token_len=" + (token == null ? 0 : token.length()));
                    DoctorFcmTokenHelper.upload(ctx.getApplicationContext(), token);
                })
                .addOnFailureListener(e -> Log.e(TAG, "refreshTokenIfNeeded() getToken failed", e));
    }

    // ─────────────────────── NOTIFICATION HANDLING ───────────────────────

    @Override
    public void onMessageReceived(RemoteMessage rm) {
        super.onMessageReceived(rm);
        if (rm == null) {
            Log.w(TAG, "onMessageReceived() called with null RemoteMessage");
            return;
        }

        // Create channel (with custom sound) exactly like patient side
        createChannelIfNeeded();

        String title = "New Appointment Request";
        String body  = "You have a new request. Tap to view.";

        if (rm.getData() != null && !rm.getData().isEmpty()) {
            Map<String, String> data = rm.getData();
            if (data.containsKey("title") && data.get("title") != null) title = data.get("title");
            if (data.containsKey("body")  && data.get("body")  != null) body  = data.get("body");
        } else if (rm.getNotification() != null) {
            RemoteMessage.Notification n = rm.getNotification();
            if (n.getTitle() != null) title = n.getTitle();
            if (n.getBody()  != null) body  = n.getBody();
        }

        showNotification(title, body);
    }

    private void showNotification(String title, String body) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("open_requests", true);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                this,
                0,
                i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder nb = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_download)   // ensure this icon exists
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "POST_NOTIFICATIONS not granted; skipping notification");
            return;
        }

        NotificationManagerCompat.from(this).notify(1001, nb.build());
    }

    /**
     * Patient-style channel creation WITH custom sound (raw/sound).
     * This mirrors the patient app logic: setSound() on the channel with AudioAttributes.
     */
    private void createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) return;

        // If channel already exists, DO NOT recreate (sound is fixed per channel id on Android O+).
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return;

        // EXACTLY LIKE PATIENT: use res/raw/sound
        Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.sound);
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID,
                "Appointment Requests",
                NotificationManager.IMPORTANCE_HIGH
        );
        ch.setDescription("Alerts when a patient requests an appointment");
        ch.setSound(soundUri, attrs);  // <-- custom sound (same as patient)
        nm.createNotificationChannel(ch);
        Log.d(TAG, "Notification channel created with custom sound: " + CHANNEL_ID);
    }

    // ───────────────────── ANDROID 13+ PERMISSION ─────────────────────

    /** Call from your Activity (e.g., Splash/Login) on first launch. */
    public static void requestNotificationPermissionIfNeeded(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        activity,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        101
                );
                Log.d(TAG, "Requested POST_NOTIFICATIONS permission");
            }
        }
    }
}
