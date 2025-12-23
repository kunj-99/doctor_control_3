package com.infowave.doctor_control;

import android.annotation.SuppressLint;
import android.content.Intent;
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

public class PendingPaymentActivity extends AppCompatActivity {

    // Extras (SettlementDetailsActivity will read these)
    public static final String EXTRA_SUMMARY_ID = "extra_summary_id";
    public static final String EXTRA_DOCTOR_ID = "extra_doctor_id";
    public static final String EXTRA_AMOUNT_TO_PAY = "extra_amount_to_pay";           // Doctor -> Admin
    public static final String EXTRA_AMOUNT_TO_RECEIVE = "extra_amount_to_receive";   // Admin -> Doctor
    public static final String EXTRA_CREATED_AT = "extra_created_at";
    public static final String EXTRA_NOTES = "extra_notes";
    public static final String EXTRA_SETTLEMENT_STATUS = "extra_settlement_status";
    public static final String EXTRA_APPOINTMENT_IDS_CSV = "extra_appointment_ids_csv";
    public static final String EXTRA_APPOINTMENT_COUNT = "extra_appointment_count";
    public static final String EXTRA_ONLINE_COUNT = "extra_online_count";
    public static final String EXTRA_OFFLINE_COUNT = "extra_offline_count";

    private RecyclerView recyclerView;
    private PaymentSummaryAdapter adapter;
    private int doctorId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_payment);

        setupSystemBarScrims();

        View back = findViewById(R.id.btnBack);
        if (back != null) back.setOnClickListener(v -> finish());

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
                this::openSettlementDetails,          // Button click (Pay OR Receive)
                this::showSettlementAppointmentsBottomSheet // Card click (Appointments sheet)
        );

        recyclerView.setAdapter(adapter);

        fetchPendingSummaries(doctorId);
    }

    private void openSettlementDetails(PaymentSummary summary) {
        if (summary == null) return;

        double amountToPay = summary.receivedFromDoctor;   // Doctor -> Admin
        double amountToReceive = summary.givenToDoctor;     // Admin -> Doctor

        if (amountToPay <= 0 && amountToReceive <= 0) {
            Toast.makeText(this, "No dues found for this settlement.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent i = new Intent(PendingPaymentActivity.this, SettlementDetailsActivity.class);

        i.putExtra(EXTRA_SUMMARY_ID, summary.summaryId);
        i.putExtra(EXTRA_DOCTOR_ID, summary.doctorId);
        i.putExtra(EXTRA_AMOUNT_TO_PAY, amountToPay);
        i.putExtra(EXTRA_AMOUNT_TO_RECEIVE, amountToReceive);

        i.putExtra(EXTRA_CREATED_AT, summary.createdAt != null ? summary.createdAt : "");
        i.putExtra(EXTRA_NOTES, summary.notes != null ? summary.notes : "");
        i.putExtra(EXTRA_SETTLEMENT_STATUS, summary.settlementStatus != null ? summary.settlementStatus : "Pending");

        i.putExtra(EXTRA_APPOINTMENT_IDS_CSV, summary.appointmentIdsCsv != null ? summary.appointmentIdsCsv : "");
        i.putExtra(EXTRA_APPOINTMENT_COUNT, summary.appointmentCount);
        i.putExtra(EXTRA_ONLINE_COUNT, summary.onlineAppointments);
        i.putExtra(EXTRA_OFFLINE_COUNT, summary.offlineAppointments);

        startActivity(i);
    }

    private void setupSystemBarScrims() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        final View root = findViewById(R.id.root_container);
        final View topScrim = findViewById(R.id.system_top_scrim);
        final View bottomScrim = findViewById(R.id.system_bottom_scrim);
        final View header = findViewById(R.id.layoutHeader);
        final RecyclerView list = findViewById(R.id.recyclerViewPaymentSummary);

        WindowInsetsControllerCompat ctrl = ViewCompat.getWindowInsetsController(root);
        if (ctrl != null) {
            ctrl.setAppearanceLightStatusBars(false);
            ctrl.setAppearanceLightNavigationBars(false);
        }

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int types = WindowInsetsCompat.Type.statusBars()
                    | WindowInsetsCompat.Type.navigationBars()
                    | WindowInsetsCompat.Type.displayCutout();
            Insets bars = insets.getInsets(types);

            int top = bars.top;
            if (top == 0) {
                @SuppressLint("InternalInsetResource") int resId =
                        getResources().getIdentifier("status_bar_height", "dimen", "android");
                if (resId > 0) top = getResources().getDimensionPixelSize(resId);
            }

            int bottom = bars.bottom;
            if (bottom == 0) {
                @SuppressLint("InternalInsetResource") int resId =
                        getResources().getIdentifier("navigation_bar_height", "dimen", "android");
                if (resId > 0) bottom = getResources().getDimensionPixelSize(resId);
            }

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

            if (header != null) {
                int padTop = Math.max(header.getPaddingTop(), top);
                header.setPadding(header.getPaddingLeft(), padTop,
                        header.getPaddingRight(), header.getPaddingBottom());
            }

            if (list != null) {
                int padBottom = Math.max(list.getPaddingBottom(), bottom);
                list.setPadding(list.getPaddingLeft(), list.getPaddingTop(),
                        list.getPaddingRight(), padBottom);
                list.setClipToPadding(false);
            }

            return insets;
        });

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

                        s.givenToDoctor         = o.optDouble("given_to_doctor", 0);       // Admin -> Doctor
                        s.receivedFromDoctor    = o.optDouble("received_from_doctor", 0);  // Doctor -> Admin

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

    @SuppressLint("SetTextI18n")
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
