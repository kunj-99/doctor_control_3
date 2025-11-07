package com.infowave.doctor_control;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.installations.FirebaseInstallations;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public final class DoctorFcmTokenHelper {

    private static final String TAG = "DOCTOR_FCM";
    private DoctorFcmTokenHelper() {}

    /** Ensure latest token is fetched and uploaded with a per-device identity */
    public static void ensureTokenSynced(Context ctx) {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    Log.d(TAG, "getToken() success | len=" + (token == null ? 0 : token.length()));
                    resolveDeviceIdAndUpload(ctx.getApplicationContext(), token);
                })
                .addOnFailureListener(e -> Log.w(TAG, "getToken() failed", e));
    }

    /** Backward-compatible entry: will also attach device_id before uploading */
    public static void upload(Context ctx, String token) {
        resolveDeviceIdAndUpload(ctx.getApplicationContext(), token);
    }

    // -------------------- internal helpers --------------------

    private static void resolveDeviceIdAndUpload(Context ctx, String token) {
        try {
            FirebaseInstallations.getInstance().getId()
                    .addOnSuccessListener(fid -> {
                        String deviceId = (fid != null && !fid.isEmpty())
                                ? fid : getAndroidIdFallback(ctx);
                        doUpload(ctx, token, deviceId);
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "FID fetch failed, using ANDROID_ID fallback", e);
                        doUpload(ctx, token, getAndroidIdFallback(ctx));
                    });
        } catch (Throwable t) {
            Log.w(TAG, "FID fetch exception, using ANDROID_ID fallback", t);
            doUpload(ctx, token, getAndroidIdFallback(ctx));
        }
    }

    private static String getAndroidIdFallback(Context ctx) {
        try {
            String id = Settings.Secure.getString(
                    ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
            return (id != null && !id.isEmpty()) ? id : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    /** Final POST including device_id + minimal metadata */
    private static void doUpload(Context ctx, String token, String deviceId) {
        if (token == null || token.isEmpty()) {
            Log.w(TAG, "doUpload: empty token, skip");
            return;
        }

        SharedPreferences sp = ctx.getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);

        // --- SAFELY READ doctor_id (handles both int and legacy string) ---
        int doctorId = -1;
        try {
            doctorId = sp.getInt("doctor_id", -1);
            if (doctorId <= 0) {
                // legacy path (if it was stored as String earlier)
                String doctorIdStr = sp.getString("doctor_id", "");
                if (doctorIdStr != null && !doctorIdStr.isEmpty()) {
                    try { doctorId = Integer.parseInt(doctorIdStr); } catch (Exception ignore) {}
                }
            }
        } catch (ClassCastException cce) {
            // If it was saved as String originally
            try {
                String doctorIdStr = sp.getString("doctor_id", "");
                if (doctorIdStr != null && !doctorIdStr.isEmpty()) {
                    doctorId = Integer.parseInt(doctorIdStr);
                }
            } catch (Exception ignored) { /* keep -1 */ }
        }

        if (doctorId <= 0) {
            Log.e(TAG, "Invalid/missing doctor_id; not uploading token");
            return;
        }

        final String url = ApiConfig.endpoint("Doctors/save_token.php");
        final RequestQueue queue = Volley.newRequestQueue(ctx);

        final String model = Build.MANUFACTURER + " " + Build.MODEL;

        // ---- Safe appVersion lookup (API 33+ compatible) ----
        String appVersion = "unknown";
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appVersion = ctx.getPackageManager()
                        .getPackageInfo(ctx.getPackageName(),
                                PackageManager.PackageInfoFlags.of(0))
                        .versionName;
            } else {
                appVersion = ctx.getPackageManager()
                        .getPackageInfo(ctx.getPackageName(), 0)
                        .versionName;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "App version lookup failed", e);
        }
        final String finalAppVersion = appVersion;

        Log.d(TAG, "POST " + url + " | doctor_id=" + doctorId
                + " | device_id=" + deviceId
                + " | token_len=" + token.length()
                + " | model=" + model
                + " | app_version=" + finalAppVersion);

        int finalDoctorId = doctorId;
        StringRequest req = new StringRequest(Request.Method.POST, url,
                resp -> Log.d(TAG, "Token saved OK: " + trim800(resp)),
                err  -> Log.e(TAG, "Token save ERROR", err)) {

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("Accept", "application/json");
                return h;
            }

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> m = new HashMap<>();
                m.put("doctor_id", String.valueOf(finalDoctorId));
                m.put("fcm_token", token);
                m.put("device_id", deviceId);
                m.put("platform", "android");
                m.put("model", model);
                m.put("app_version", finalAppVersion);
                return m;
            }
        };

        req.setRetryPolicy(new DefaultRetryPolicy(
                10000, // 10s
                1,     // one retry
                1.0f
        ));
        queue.add(req);
    }

    private static String trim800(String s) {
        if (s == null) return "null";
        s = s.trim();
        return s.length() > 800 ? s.substring(0, 800) + "...(truncated)" : s;
    }
}
