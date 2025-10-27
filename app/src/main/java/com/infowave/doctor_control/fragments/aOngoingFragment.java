package com.infowave.doctor_control.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.*;
import com.infowave.doctor_control.*;
import com.infowave.doctor_control.adapter.aOngoingAdapter;
import com.infowave.doctor_control.medical_report;

import org.json.*;

import java.util.*;

public class aOngoingFragment extends Fragment {

    private static final long APPT_REFRESH_MS = 5000;

    private RecyclerView recyclerView;
    private aOngoingAdapter adapter;
    private RequestQueue queue;

    private final ArrayList<String> appointmentIds = new ArrayList<>();
    private final ArrayList<String> patientNames   = new ArrayList<>();
    private final ArrayList<String> problems       = new ArrayList<>();
    private final ArrayList<String> distances      = new ArrayList<>();
    private final ArrayList<String> mapLinks       = new ArrayList<>();
    private final ArrayList<Boolean> hasReport     = new ArrayList<>();
    private final ArrayList<String> amounts        = new ArrayList<>();
    private final ArrayList<String> paymentMethods = new ArrayList<>();

    private String doctorId;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable refresher;

    private ActivityResultLauncher<Intent> reportLauncher;

    // ---- Permission launcher for all required runtime permissions
    private ActivityResultLauncher<String[]> permLauncher;

    // Track whether we've already requested permissions this session
    private boolean permissionsAskedOnce = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ongoing, container, false);

        queue = Volley.newRequestQueue(requireContext());

        reportLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // If needed, refresh UI, we already refresh periodically
                        adapter.notifyDataSetChanged();
                    }
                });

        // Request multiple permissions in one go
        permLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    // If FINE granted, and (if required) BACKGROUND granted on Android 10+
                    if (hasAllCriticalPermissions()) {
                        // If we already have an ongoing appointment, ensure services are running
                        ensureServicesBasedOnAppointments();
                    } else {
                        Toast.makeText(requireContext(),
                                "Location permissions not granted. Live tracking disabled.",
                                Toast.LENGTH_LONG).show();
                        stopTrackingServices();
                    }
                });

        recyclerView = view.findViewById(R.id.rv_ongoing_appointments);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new aOngoingAdapter(
                getContext(),
                appointmentIds,
                patientNames,
                problems,
                distances,
                hasReport,
                mapLinks,
                amounts,
                paymentMethods,
                position -> {
                    if (position >= 0 && position < appointmentIds.size()) {
                        String appointmentId = appointmentIds.get(position);
                        completeAppointment(appointmentId, position);
                    }
                },
                (appointmentId, position) -> {
                    Intent intent = new Intent(getContext(), medical_report.class);
                    intent.putExtra("appointment_id", appointmentId);
                    reportLauncher.launch(intent);
                }
        );

        recyclerView.setAdapter(adapter);

        doctorId = String.valueOf(
                requireActivity()
                        .getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE)
                        .getInt("doctor_id", -1)
        );

        if ("-1".equals(doctorId)) {
            Toast.makeText(getContext(), "Doctor ID not found!", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        requestAllPermissionsIfNeeded();   // ask once on resume
        startAppointmentRefresh();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAppointmentRefresh();
        // NOTE: Foreground service keeps running; no need to stop here.
    }

    private void startAppointmentRefresh() {
        stopAppointmentRefresh();
        fetchAppointments();
        refresher = () -> {
            if (isAdded()) {
                fetchAppointments();
                handler.postDelayed(refresher, APPT_REFRESH_MS);
            }
        };
        handler.postDelayed(refresher, APPT_REFRESH_MS);
    }

    private void stopAppointmentRefresh() {
        if (refresher != null) handler.removeCallbacks(refresher);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fetchAppointments() {
        String url = ApiConfig.endpoint("Doctors/getOngoingAppointment.php");

        queue.add(new StringRequest(
                Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject root = new JSONObject(response);
                        if (!root.optBoolean("success", false) || !root.has("appointments")) {
                            applyEmptyAppointments();
                            return;
                        }

                        JSONArray arr = root.getJSONArray("appointments");

                        // Update UI lists on main thread
                        recyclerView.post(() -> {
                            clearAllLists();

                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject o = arr.optJSONObject(i);
                                if (o == null) continue;

                                String status = o.optString("status", "Unknown");
                                if (!"Confirmed".equalsIgnoreCase(status)) {
                                    continue;
                                }

                                String apptId = o.optString("appointment_id", "");
                                appointmentIds.add(apptId);
                                patientNames.add(o.optString("patient_name", ""));
                                problems.add(o.optString("reason_for_visit", ""));
                                mapLinks.add(o.optString("patient_map_link", ""));
                                hasReport.add(o.optInt("has_report", 0) == 1);
                                distances.add("Calculating...");
                                amounts.add(o.optString("amount", "0.00"));
                                paymentMethods.add(o.optString("payment_method", "Unknown"));

                                // First confirmed appointment → mark as ongoing for tracking
                                if (appointmentIds.size() == 1) {
                                    requireContext().getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE)
                                            .edit()
                                            .putString("ongoing_appointment_id", apptId)
                                            .apply();
                                }
                            }

                            adapter.notifyDataSetChanged();

                            // Start/stop services based on list
                            ensureServicesBasedOnAppointments();

                            // Distance calculation (async)
                            for (int i = 0; i < mapLinks.size(); i++) {
                                final int idx = i;
                                DistanceCalculator.calculateDistance(
                                        requireActivity(),
                                        queue,
                                        mapLinks.get(idx),
                                        dist -> {
                                            if (idx < distances.size()) {
                                                distances.set(idx, dist);
                                                adapter.notifyItemChanged(idx);
                                            }
                                        }
                                );
                            }
                        });

                    } catch (JSONException e) {
                        Toast.makeText(getContext(), "Parse error", Toast.LENGTH_SHORT).show();
                        applyEmptyAppointments();
                    }
                },
                error -> {
                    Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
                    // keep previous list; do not force stop
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("doctor_id", doctorId);
                return p;
            }
        });
    }

    private void applyEmptyAppointments() {
        recyclerView.post(() -> {
            clearAllLists();
            adapter.notifyDataSetChanged();
            // No ongoing appointments → stop services
            stopTrackingServices();
            // Also clear the saved ongoing id
            requireContext().getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE)
                    .edit().remove("ongoing_appointment_id").apply();
        });
    }

    private void clearAllLists() {
        appointmentIds.clear();
        patientNames.clear();
        problems.clear();
        distances.clear();
        mapLinks.clear();
        hasReport.clear();
        amounts.clear();
        paymentMethods.clear();
    }

    private void completeAppointment(String appointmentId, int position) {
        String url = ApiConfig.endpoint("Doctors/completeAppointment.php");

        queue.add(new StringRequest(
                Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject root = new JSONObject(response);
                        if (root.optBoolean("success", false)) {

                            // Remove from lists
                            appointmentIds.remove(position);
                            patientNames.remove(position);
                            problems.remove(position);
                            distances.remove(position);
                            mapLinks.remove(position);
                            hasReport.remove(position);
                            amounts.remove(position);
                            paymentMethods.remove(position);

                            adapter.notifyItemRemoved(position);
                            adapter.notifyItemRangeChanged(position, appointmentIds.size());

                            Toast.makeText(getContext(), "Appointment completed!", Toast.LENGTH_SHORT).show();

                            // If now list empty → stop tracking
                            if (appointmentIds.isEmpty()) {
                                requireContext().getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE)
                                        .edit().remove("ongoing_appointment_id").apply();
                                stopTrackingServices();
                            } else {
                                // Next appointment becomes ongoing
                                String nextAppt = appointmentIds.get(0);
                                requireContext().getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE)
                                        .edit().putString("ongoing_appointment_id", nextAppt).apply();
                                ensureServicesBasedOnAppointments();
                            }
                        } else {
                            Toast.makeText(getContext(), "Error updating appointment status", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(getContext(), "Error updating appointment status", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(getContext(), "Error completing appointment", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("appointment_id", appointmentId);
                return params;
            }
        });
    }

    /* ===================== Permissions & Services ===================== */

    private void requestAllPermissionsIfNeeded() {
        if (permissionsAskedOnce) return;
        permissionsAskedOnce = true;

        ArrayList<String> perms = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!perms.isEmpty()) {
            permLauncher.launch(perms.toArray(new String[0]));
        } else {
            // Already granted
            ensureServicesBasedOnAppointments();
        }
    }

    private boolean hasAllCriticalPermissions() {
        boolean fine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean bgOk = true;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            bgOk = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
        }
        boolean notifOk = true;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notifOk = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return fine && bgOk && notifOk;
    }

    private void ensureServicesBasedOnAppointments() {
        if (!isAdded()) return;
        if (!hasAllCriticalPermissions()) return;

        Context ctx = requireContext().getApplicationContext();

        if (!appointmentIds.isEmpty()) {
            // Ensure ongoing_appointment_id exists (already set in fetch)
            // Start foreground tracking + guard
            LiveLocationManager.getInstance().startLocationUpdates(ctx);
            ctx.startService(new Intent(ctx, BackgroundService.class));
        } else {
            stopTrackingServices();
        }
    }

    private void stopTrackingServices() {
        if (!isAdded()) return;
        Context ctx = requireContext().getApplicationContext();
        LiveLocationManager.getInstance().stopLocationUpdates(ctx);
        ctx.stopService(new Intent(ctx, BackgroundService.class));
    }
}
