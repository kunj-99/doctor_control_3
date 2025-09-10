package com.infowave.doctor_control;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;


public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "requests_channel";

    // ───────────────────────────────
    //  TOKEN HANDLING
    // ───────────────────────────────

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        uploadTokenToServer(token);
    }

    /** Call once from your login / splash too (optional) */
    public static void refreshTokenIfNeeded(Context ctx) {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    new MyFirebaseMessagingService().uploadTokenToServer(ctx, token);
                });
    }

    private void uploadTokenToServer(String token) {
        uploadTokenToServer(getApplicationContext(), token);
    }

    private void uploadTokenToServer(Context ctx, String token) {
        SharedPreferences sp = ctx.getSharedPreferences("DoctorPrefs", MODE_PRIVATE);
        int doctorId = sp.getInt("doctor_id", -1);
        if (doctorId == -1) return; // not logged in yet

        RequestQueue queue = Volley.newRequestQueue(ctx);

        String url = ApiConfig.endpoint("Doctors/save_token.php");


        StringRequest req = new StringRequest(Request.Method.POST, url,
                resp -> { /* no log */ },
                err  -> { /* no log */ })
        {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> m = new HashMap<>();
                m.put("doctor_id", String.valueOf(doctorId));
                m.put("fcm_token", token);
                return m;
            }
        };
        queue.add(req);
    }

    // ───────────────────────────────
    //  NOTIFICATION HANDLING
    // ───────────────────────────────

    @Override
    public void onMessageReceived(RemoteMessage rm) {
        super.onMessageReceived(rm);

        createChannelIfNeeded();

        String title = "New Appointment Request";
        String body  = "You have a new request. Tap to view.";

        // Check for data payload first
        if (rm.getData().size() > 0) {
            Map<String, String> data = rm.getData();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (data.get("title") != null) title = data.get("title");
                if (data.get("body") != null) body = data.get("body");
            }
        }
        // Fallback to notification payload (optional)
        else if (rm.getNotification() != null) {
            if (rm.getNotification().getTitle() != null) title = rm.getNotification().getTitle();
            if (rm.getNotification().getBody() != null) body = rm.getNotification().getBody();
        }

        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("open_requests", true);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                this, 0, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_download)   // your app icon
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setSound(sound)
                .setContentIntent(pi);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            // permission not granted, skip notification
            return;
        }

        NotificationManagerCompat.from(this).notify(1001, nb.build());
    }

    private void createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel ch = new NotificationChannel(
                        CHANNEL_ID,
                        "Appointment Requests",
                        NotificationManager.IMPORTANCE_HIGH);
                ch.setDescription("Alerts when a patient requests an appointment");
                nm.createNotificationChannel(ch);
            }
        }
    }
}
