package com.infowave.doctor_control;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public final class DoctorFcmTokenHelper {

    private static final String TAG = "DOCTOR_FCM";

    private DoctorFcmTokenHelper() {}

    public static void ensureTokenSynced(Context ctx) {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> upload(ctx.getApplicationContext(), token))
                .addOnFailureListener(e -> Log.w(TAG, "getToken failed", e));
    }

    public static void upload(Context ctx, String token) {
        SharedPreferences sp = ctx.getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
        int doctorId = sp.getInt("doctor_id", -1);
        if (doctorId <= 0) {
            Log.d(TAG, "doctor_id missing; skip upload");
            return;
        }

        String url = ApiConfig.endpoint("Doctors/save_token.php"); // we’ll create this PHP next

        RequestQueue q = Volley.newRequestQueue(ctx);
        StringRequest req = new StringRequest(Request.Method.POST, url,
                resp -> Log.d(TAG, "token saved"),
                err  -> Log.w(TAG, "token save error", err)) {
            @Override protected Map<String, String> getParams() {
                Map<String, String> m = new HashMap<>();
                m.put("doctor_id", String.valueOf(doctorId));
                m.put("fcm_token", token);
                return m;
            }
        };
        q.add(req);
    }
}
