package com.infowave.doctor_control;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Shows a leave-app confirmation and pushes online/offline to server.
 * Call from MainActivity.onBackPressed() and onUserLeaveHint().
 */
public final class ActiveStatusManager {

    public enum Trigger { BACK_EXIT, HOME_OR_APP_SWITCH }

    private boolean dialogShowing = false;
    private long lastShownAt = 0L; // debounce for HOME/app switch

    public void promptAndExit(Activity activity, Trigger trigger, @Nullable Runnable afterExit) {
        if (activity == null || activity.isFinishing()) return;

        // Debounce HOME/app switch so it won’t fire twice
        long now = System.currentTimeMillis();
        if (trigger == Trigger.HOME_OR_APP_SWITCH && (now - lastShownAt) < 1500) return;
        lastShownAt = now;

        if (dialogShowing) return;
        dialogShowing = true;

        final int doctorId = getDoctorId(activity);
        if (doctorId <= 0) {
            dialogShowing = false;
            // No doctor logged in – just exit.
            exitApp(activity, afterExit);
            return;
        }

        AlertDialog dlg = new AlertDialog.Builder(activity)
                .setTitle("Exit and set your status?")
                .setMessage("Choose whether patients should see you as Active or Inactive after you close the app.")
                .setCancelable(true)
                .setPositiveButton("Keep Active & Exit", (d, w) -> {
                    setActiveStatus(activity.getApplicationContext(), doctorId, true);
                    dialogShowing = false;
                    exitApp(activity, afterExit);
                })
                .setNegativeButton("Go Inactive & Exit", (d, w) -> {
                    setActiveStatus(activity.getApplicationContext(), doctorId, false);
                    dialogShowing = false;
                    exitApp(activity, afterExit);
                })
                .setOnDismissListener(di -> dialogShowing = false)
                .create();

        // Make sure it appears even when HOME is pressed
        if (dlg.getWindow() != null) {
            dlg.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
        dlg.show();
    }

    private int getDoctorId(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
        int id = sp.getInt("doctor_id", -1);
        if (id > 0) return id;
        id = sp.getInt("DoctorId", -1);
        if (id > 0) return id;
        return sp.getInt("doc_id", -1);
    }

    private void setActiveStatus(Context appCtx, int doctorId, boolean active) {
        // Store preference locally (optional)
        appCtx.getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE)
                .edit().putBoolean("keep_active", active).apply();

        try {
            // 🟢 New Code Block (using your ApiConfig.endpoint format)
            String url = ApiConfig.endpoint(
                    "Doctors/set_online_status.php",
                    "doctor_id",
                    URLEncoder.encode(String.valueOf(doctorId), StandardCharsets.UTF_8.name())
            ) + "&is_online=" + (active ? "1" : "0");

            StringRequest req = new StringRequest(Request.Method.GET, url,
                    resp -> {
                        // Optional toast; safe because app is about to close anyway
                        Toast.makeText(appCtx,
                                active ? "Status set to Active." : "Status set to Inactive.",
                                Toast.LENGTH_SHORT).show();
                    },
                    err -> {
                        String msg = (err != null && !TextUtils.isEmpty(err.getMessage()))
                                ? err.getMessage() : "Network error";
                        Toast.makeText(appCtx, "Status update failed: " + msg, Toast.LENGTH_SHORT).show();
                    });
            req.setRetryPolicy(new DefaultRetryPolicy(10000, 1, 1.0f));
            Volley.newRequestQueue(appCtx).add(req);
        } catch (Exception e) {
            Toast.makeText(appCtx, "Status update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void exitApp(Activity activity, @Nullable Runnable afterExit) {
        try {
            // Finish this task and move to back; this is the safest “close”
            activity.moveTaskToBack(true);
            activity.finishAffinity();
        } catch (Exception ignored) {}
        if (afterExit != null) afterExit.run();
    }
}
