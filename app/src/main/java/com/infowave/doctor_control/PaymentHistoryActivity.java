package com.infowave.doctor_control;

import android.annotation.SuppressLint;
import android.content.Intent; // <-- added
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
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
import com.google.android.material.chip.ChipGroup;
import com.infowave.doctor_control.adapter.PaymentHistoryAdapter;
import com.infowave.doctor_control.adapter.SettlementAppointmentAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class PaymentHistoryActivity extends AppCompatActivity {

    private static final String TAG = "PaymentHistoryActivity";

    private ListView listView;
    private ChipGroup chipsFilter;            // filter via chips
    private PaymentHistoryAdapter adapter;

    private int doctorId = -1;
    private final ArrayList<PaymentSummary> completedList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: starting PaymentHistoryActivity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_history);

        setupSystemBarScrims();

        doctorId = getDoctorIdFromPrefs();
        Log.d(TAG, "onCreate: resolved doctorId=" + doctorId);
        if (doctorId <= 0) {
            Log.w(TAG, "onCreate: doctorId missing, finishing activity");
            Toast.makeText(this, "Doctor ID not found. Please login again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        listView    = findViewById(R.id.list_payment);
        chipsFilter = findViewById(R.id.chips_filter); // must exist in XML if you want chip filtering

        // Card click: open bottom sheet (unchanged)
        // Show Proof click: redirect to SettlementDetailsActivity
        adapter = new PaymentHistoryAdapter(
                this,
                completedList,
                new PaymentHistoryAdapter.RowActionListener() {
                    @Override
                    public void onOpenSettlementDetails(PaymentSummary summary) {
                        if (summary == null) return;
                        // Whole row/card click -> BottomSheet (unchanged)
                        showSettlementAppointmentsBottomSheet(summary);
                    }

                    @Override
                    public void onShowProof(PaymentSummary summary) {
                        if (summary == null) return;
                        // Show Proof button -> redirect to SettlementDetailsActivity
                        openSettlementDetails(summary);
                    }
                }
        );

        listView.setDivider(null);
        listView.setAdapter(adapter);
        Log.d(TAG, "onCreate: adapter attached with initial size=" + completedList.size());

        // Chips -> adapter filter (guard if chips not in this layout)
        if (chipsFilter != null) {
            chipsFilter.setOnCheckedChangeListener((group, checkedId) -> {
                int mode = PaymentHistoryAdapter.FILTER_ALL;
                if (checkedId == R.id.chip_admin_to_doctor) {
                    mode = PaymentHistoryAdapter.FILTER_ADMIN_TO_DOCTOR;
                } else if (checkedId == R.id.chip_doctor_to_admin) {
                    mode = PaymentHistoryAdapter.FILTER_DOCTOR_TO_ADMIN;
                }
                Log.d(TAG, "chips: checkedId=" + checkedId + ", mode=" + mode);
                adapter.setDirectionFilter(mode);
            });
        }

        // Keep list item click -> bottom sheet (unchanged)
        listView.setOnItemClickListener((parent, view, position, id) -> {
            PaymentSummary summary = (PaymentSummary) adapter.getItem(position);
            if (summary == null) return;
            showSettlementAppointmentsBottomSheet(summary);
        });

        fetchCompletedSettlements(doctorId);
    }

    private void setupSystemBarScrims() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        final View root = findViewById(R.id.root_container);
        final View content = findViewById(R.id.content_container);
        final View topScrim = findViewById(R.id.system_top_scrim);
        final View bottomScrim = findViewById(R.id.system_bottom_scrim);
        final ListView list = findViewById(R.id.list_payment);

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
            int bottom = bars.bottom;

            if (top == 0) {
                @SuppressLint("InternalInsetResource")
                int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
                if (resId > 0) top = getResources().getDimensionPixelSize(resId);
            }
            if (bottom == 0) {
                @SuppressLint("InternalInsetResource")
                int resId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
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

            if (content != null) {
                int padTop = Math.max(content.getPaddingTop(), top);
                int padBottom = Math.max(content.getPaddingBottom(), bottom);
                content.setPadding(content.getPaddingLeft(), padTop,
                        content.getPaddingRight(), padBottom);
            }
            if (list != null) {
                int padBottom2 = Math.max(list.getPaddingBottom(), bottom);
                list.setPadding(list.getPaddingLeft(), list.getPaddingTop(),
                        list.getPaddingRight(), padBottom2);
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

    /** Loads only Settled items (history). Pending screen keeps using '&status=Pending'. */
    private void fetchCompletedSettlements(int doctorId) {
        try {
            String base = ApiConfig.endpoint(
                    "Doctors/get_doctor_settlements.php",
                    "doctor_id",
                    URLEncoder.encode(String.valueOf(doctorId), StandardCharsets.UTF_8.name())
            );
            String url = base + "&status=Settled";
            Log.d(TAG, "fetchCompletedSettlements: GET " + url);

            final long t0 = System.currentTimeMillis();
            StringRequest req = new StringRequest(Request.Method.GET, url, response -> {
                long dt = System.currentTimeMillis() - t0;
                try {
                    JSONObject root = new JSONObject(response);
                    boolean success = root.optBoolean("success", false);
                    if (!success) {
                        String msg = root.optString("message", "Failed");
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    JSONArray arr = root.optJSONArray("data");
                    completedList.clear();

                    if (arr != null) {
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
                            s.settlementStatus      = o.optString("settlement_status", "Settled");
                            s.notes                 = o.optString("notes", "");
                            s.createdAt             = o.optString("created_at", "");
                            s.updatedAt             = o.optString("updated_at", "");

                            if ("Settled".equalsIgnoreCase(s.settlementStatus)) {
                                completedList.add(s);
                            }
                        }
                    }

                    adapter.setData(completedList);

                    if (completedList.isEmpty()) {
                        Toast.makeText(this, "No settled settlements found.", Toast.LENGTH_SHORT).show();
                    }

                } catch (Exception e) {
                    Toast.makeText(this, "Parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }, error -> {
                String msg = (error != null && error.getMessage() != null) ? error.getMessage() : "Network error";
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            });

            req.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.0f));
            Volley.newRequestQueue(this).add(req);

        } catch (Exception e) {
            Toast.makeText(this, "Build URL failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show Proof button → SettlementDetailsActivity.
     */
    private void openSettlementDetails(PaymentSummary summary) {
        Intent i = new Intent(PaymentHistoryActivity.this, SettlementDetailsActivity.class);

        // Primary extras
        i.putExtra(SettlementDetailsActivity.EXTRA_DOCTOR_ID, summary.doctorId);
        i.putExtra(SettlementDetailsActivity.EXTRA_SUMMARY_ID, summary.summaryId);

        // Legacy keys for compatibility
        i.putExtra("doctor_id", summary.doctorId);
        i.putExtra("summary_id", summary.summaryId);

        startActivity(i);
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

            tvTitle.setText("Settlement #" + summary.summaryId + " • Settled");
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
                        String msg = root.optString("message", "Failed");
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    JSONArray arr = root.optJSONArray("data");

                    List<SettlementAppointment> data = new ArrayList<>();
                    if (arr != null) {
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
                    }
                    rv.setAdapter(new SettlementAppointmentAdapter(data));

                } catch (Exception e) {
                    Toast.makeText(this, "Parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }, error -> {
                String msg = (error != null && error.getMessage() != null) ? error.getMessage() : "Network error";
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            });

            req.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.0f));
            Volley.newRequestQueue(this).add(req);

        } catch (Exception e) {
            Toast.makeText(this, "Bottom sheet error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
