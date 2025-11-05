package com.infowave.doctor_control;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.infowave.doctor_control.adapter.PaymentSummaryAdapter;
import com.infowave.doctor_control.adapter.SettlementAppointmentAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PendingPaymentActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PaymentSummaryAdapter adapter;
    private int doctorId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_payment);

        // Edge-to-edge, but we supply our own black scrims for BOTH bars.
        setupSystemBarScrims();

        doctorId = getDoctorIdFromPrefs();
        if (doctorId <= 0) {
            Toast.makeText(this, "Doctor ID not found. Please login again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        recyclerView = findViewById(R.id.recyclerViewPaymentSummary);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new PaymentSummaryAdapter(
                new ArrayList<>(),
                summary -> {
                    double amtDoctorToAdmin = summary.receivedFromDoctor;
                    if (amtDoctorToAdmin > 0) {
                        String payText = "Pay Admin ₹" + String.format(Locale.ROOT, "%.2f", amtDoctorToAdmin);
                        Toast.makeText(this, payText + " (Summary #" + summary.summaryId + ")", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "No dues to pay.", Toast.LENGTH_SHORT).show();
                    }
                },
                this::showSettlementAppointmentsBottomSheet
        );
        recyclerView.setAdapter(adapter);

        fetchPendingSummaries(doctorId);
    }

    /** Edge-to-edge + black status/nav bars via overlay Views sized from insets with reliable fallbacks. */
    private void setupSystemBarScrims() {
        // Draw behind system bars
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        final View root = findViewById(R.id.root_container);
        final View topScrim = findViewById(R.id.system_top_scrim);
        final View bottomScrim = findViewById(R.id.system_bottom_scrim);
        final View header = findViewById(R.id.layoutHeader);
        final RecyclerView list = findViewById(R.id.recyclerViewPaymentSummary);

        // White icons on black bars
        WindowInsetsControllerCompat ctrl = ViewCompat.getWindowInsetsController(root);
        if (ctrl != null) {
            ctrl.setAppearanceLightStatusBars(false);
            ctrl.setAppearanceLightNavigationBars(false);
        }

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            // Include cutouts for devices with notches
            int types = WindowInsetsCompat.Type.statusBars()
                    | WindowInsetsCompat.Type.navigationBars()
                    | WindowInsetsCompat.Type.displayCutout();
            Insets bars = insets.getInsets(types);

            // --- Robust status-bar height with fallback ---
            int top = bars.top;
            if (top == 0) {
                // Some OEMs return 0 on the first pass; fall back to status_bar_height dimen
                int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
                if (resId > 0) top = getResources().getDimensionPixelSize(resId);
            }

            // --- Robust nav-bar height with fallback (gesture nav may be 0) ---
            int bottom = bars.bottom;
            if (bottom == 0) {
                int resId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
                if (resId > 0) bottom = getResources().getDimensionPixelSize(resId);
            }

            // Size/Show scrims
            if (topScrim != null) {
                topScrim.getLayoutParams().height = top;
                topScrim.requestLayout();
                topScrim.setVisibility(top > 0 ? View.VISIBLE : View.GONE);
                topScrim.bringToFront();
            }
            if (bottomScrim != null) {
                bottomScrim.getLayoutParams().height = bottom;
                bottomScrim.requestLayout();
                bottomScrim.setVisibility(bottom > 0 ? View.VISIBLE : View.GONE);
                bottomScrim.bringToFront();
            }

            // Keep header content below the status area
            if (header != null) {
                int padTop = Math.max(header.getPaddingTop(), top);
                header.setPadding(header.getPaddingLeft(), padTop,
                        header.getPaddingRight(), header.getPaddingBottom());
            }

            // Keep list above the bottom gesture/nav area
            if (list != null) {
                int padBottom = Math.max(list.getPaddingBottom(), bottom);
                list.setPadding(list.getPaddingLeft(), list.getPaddingTop(),
                        list.getPaddingRight(), padBottom);
                list.setClipToPadding(false);
            }

            // Do NOT consume; keeps bars visible on OEM variants.
            return insets;
        });

        // Ensure the very first layout receives insets
        ViewCompat.requestApplyInsets(root);
    }

    private int getDoctorIdFromPrefs() {
        SharedPreferences sp = getSharedPreferences("DoctorPrefs", MODE_PRIVATE);
        int id = sp.getInt("doctor_id", -1);
        if (id > 0) return id;
        id = sp.getInt("DoctorId", -1);
        if (id > 0) return id;
        id = sp.getInt("doc_id", -1);
        return id;
    }

    private void fetchPendingSummaries(int doctorId) {
        try {
            String base = ApiConfig.endpoint(
                    "Doctors/get_doctor_settlements.php",
                    "doctor_id",
                    URLEncoder.encode(String.valueOf(doctorId), StandardCharsets.UTF_8.name())
            );
            String url = base + "&status=Pending";

            StringRequest req = new StringRequest(Request.Method.GET, url, response -> {
                try {
                    JSONObject root = new JSONObject(response);
                    if (!root.optBoolean("success", false)) {
                        Toast.makeText(this, root.optString("message", "Failed"), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    JSONArray arr = root.optJSONArray("data");
                    if (arr == null) arr = new JSONArray();

                    List<PaymentSummary> list = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.getJSONObject(i);
                        PaymentSummary s = new PaymentSummary();
                        s.summaryId             = o.optInt("summary_id");
                        s.doctorId              = o.optInt("doctor_id");

                        s.appointmentIdsCsv     = o.optString("appointment_ids_csv", "");
                        s.appointmentCount      = o.optInt("appointment_count", 0);
                        s.onlineAppointments    = o.optInt("online_appointments", 0);
                        s.offlineAppointments   = o.optInt("offline_appointments", 0);

                        s.totalBaseExGst        = o.optDouble("total_base_ex_gst", 0);
                        s.totalGst              = o.optDouble("total_gst", 0);
                        s.adminCollectedTotal   = o.optDouble("admin_collected_total", 0);
                        s.doctorCollectedTotal  = o.optDouble("doctor_collected_total", 0);

                        s.adminCut              = o.optDouble("admin_cut", 0);
                        s.doctorCut             = o.optDouble("doctor_cut", 0);
                        s.adjustmentAmount      = o.optDouble("adjustment_amount", 0);

                        s.givenToDoctor         = o.optDouble("given_to_doctor", 0);
                        s.receivedFromDoctor    = o.optDouble("received_from_doctor", 0);

                        s.settlementStatus      = o.optString("settlement_status", "Pending");
                        s.notes                 = o.optString("notes", "");
                        s.createdAt             = o.optString("created_at", "");
                        s.updatedAt             = o.optString("updated_at", "");

                        if ("Pending".equalsIgnoreCase(s.settlementStatus)) {
                            list.add(s);
                        }
                    }
                    adapter.setData(list);

                    if (list.isEmpty()) {
                        Toast.makeText(this, "No pending settlements.", Toast.LENGTH_SHORT).show();
                    }

                } catch (Exception e) {
                    Toast.makeText(this, "Parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }, error -> Toast.makeText(this, "Network error: " + (error.getMessage()!=null?error.getMessage():""), Toast.LENGTH_SHORT).show());

            req.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.0f));
            Volley.newRequestQueue(this).add(req);

        } catch (Exception e) {
            Toast.makeText(this, "Build URL failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showSettlementAppointmentsBottomSheet(PaymentSummary summary) {
        try {
            View sheetView = LayoutInflater.from(this)
                    .inflate(R.layout.bottomsheet_settlement_appointments, null);

            TextView tvTitle = sheetView.findViewById(R.id.tvSheetTitle);
            TextView tvMeta  = sheetView.findViewById(R.id.tvMeta);
            RecyclerView rv  = sheetView.findViewById(R.id.rvSettlementAppointments);
            rv.setLayoutManager(new LinearLayoutManager(this));

            tvTitle.setText("Settlement #" + summary.summaryId + " • " + summary.settlementStatus);
            tvMeta.setText(
                    "Appointments: " + summary.appointmentCount +
                            "  |  Online: " + summary.onlineAppointments +
                            "  |  Offline: " + summary.offlineAppointments
            );

            BottomSheetDialog dialog = new BottomSheetDialog(this);
            dialog.setContentView(sheetView);
            dialog.show();

            String base = ApiConfig.endpoint(
                    "Doctors/get_settlement_appointments.php",
                    "doctor_id",
                    URLEncoder.encode(String.valueOf(summary.doctorId), StandardCharsets.UTF_8.name())
            );
            String url = base + "&summary_id=" + summary.summaryId;

            StringRequest req = new StringRequest(Request.Method.GET, url, response -> {
                try {
                    JSONObject root = new JSONObject(response);
                    if (!root.optBoolean("success", false)) {
                        Toast.makeText(this, root.optString("message", "Failed"), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    JSONArray arr = root.optJSONArray("data");
                    if (arr == null) arr = new JSONArray();

                    List<SettlementAppointment> data = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.getJSONObject(i);

                        SettlementAppointment a = new SettlementAppointment();
                        a.appointmentId   = o.optInt("appointment_id");
                        a.patientId       = o.optInt("patient_id");
                        a.patientName     = o.optString("patient_name", "");
                        a.paymentMethod   = o.optString("payment_method", "");
                        a.deposit         = o.optDouble("deposit", 0);
                        a.depositStatus   = o.optString("deposit_status", "");
                        a.amountTotal     = o.optDouble("amount_total", 0);
                        a.gst             = o.optDouble("gst", 0);
                        a.baseExGst       = o.optDouble("base_ex_gst", 0);
                        a.adminCommission = o.optDouble("admin_commission", 0);
                        a.doctorEarning   = o.optDouble("doctor_earning", 0);
                        a.paymentStatus   = o.optString("payment_status", "");
                        a.createdAt       = o.optString("created_at", "");

                        data.add(a);
                    }
                    rv.setAdapter(new SettlementAppointmentAdapter(data));

                } catch (Exception e) {
                    Toast.makeText(this, "Parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }, error -> Toast.makeText(this, "Network error: " + (error.getMessage()!=null?error.getMessage():""), Toast.LENGTH_SHORT).show());

            req.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.0f));
            Volley.newRequestQueue(this).add(req);

        } catch (Exception e) {
            Toast.makeText(this, "Bottom sheet error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
