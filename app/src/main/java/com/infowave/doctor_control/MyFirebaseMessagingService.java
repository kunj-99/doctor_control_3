package com.infowave.doctor_control;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "requests_channel";
    private static final String TAG = "MyFMS";

    // ───────────────────────────────
    //  TOKEN HANDLING
    // ───────────────────────────────

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token: " + token);
        DoctorFcmTokenHelper.upload(getApplicationContext(), token);
    }

    /** Call once from login/splash */
    public static void refreshTokenIfNeeded(Context ctx) {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    Log.d(TAG, "Refreshed FCM token: " + token);
                    DoctorFcmTokenHelper.upload(ctx, token);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch FCM token", e));
    }

    // ───────────────────────────────
    //  NOTIFICATION HANDLING
    // ───────────────────────────────

    @Override
    public void onMessageReceived(RemoteMessage rm) {
        super.onMessageReceived(rm);

        Log.d(TAG, "Message received: " + rm);

        createChannelIfNeeded();

        String title = "New Appointment Request";
        String body  = "You have a new request. Tap to view.";

        if (rm.getData().size() > 0) {
            Log.d(TAG, "Data payload: " + rm.getData().toString());
            Map<String, String> data = rm.getData();
            if (data.get("title") != null) title = data.get("title");
            if (data.get("body")  != null) body  = data.get("body");
        } else if (rm.getNotification() != null) {
            Log.d(TAG, "Notification payload: " + rm.getNotification().getBody());
            if (rm.getNotification().getTitle() != null) title = rm.getNotification().getTitle();
            if (rm.getNotification().getBody()  != null) body  = rm.getNotification().getBody();
        }

        showNotification(title, body);
    }

    private void showNotification(String title, String body) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("open_requests", true);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                this, 0, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_download)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setSound(sound)
                .setContentIntent(pi);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted, skipping notification");
            return;
        }

        NotificationManagerCompat.from(this).notify(1001, nb.build());
        Log.d(TAG, "Notification shown: " + title + " / " + body);
    }

    private void createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel ch = new NotificationChannel(
                        CHANNEL_ID,
                        "Appointment Requests",
                        NotificationManager.IMPORTANCE_HIGH
                );
                ch.setDescription("Alerts when a patient requests an appointment");
                nm.createNotificationChannel(ch);
                Log.d(TAG, "Notification channel created");
            }
        }
    }
}
