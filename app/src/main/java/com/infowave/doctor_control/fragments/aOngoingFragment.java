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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.infowave.doctor_control.ApiConfig;
import com.infowave.doctor_control.BackgroundService;
import com.infowave.doctor_control.DistanceCalculator;
import com.infowave.doctor_control.LiveLocationManager;
import com.infowave.doctor_control.R;
import com.infowave.doctor_control.adapter.aOngoingAdapter;
import com.infowave.doctor_control.medical_report;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class aOngoingFragment extends Fragment {

    // 🔁 Faster & smooth refresh every 2s
    private static final long APPT_REFRESH_MS = 1500;

    private RecyclerView recyclerView;
    private TextView emptyStateView;
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

    // Vet flags and data
    private final ArrayList<Boolean> vetCases           = new ArrayList<>();
    private final ArrayList<String> animalCategoryNames = new ArrayList<>();
    private final ArrayList<String> animalBreeds        = new ArrayList<>();
    private final ArrayList<String> vaccinationNames    = new ArrayList<>();

    private String doctorId;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable refresher;

    private ActivityResultLauncher<Intent> reportLauncher;
    private ActivityResultLauncher<String> singlePermLauncher;

    private boolean askedFineOnce = false;
    private boolean askedNotifOnce = false;
    private boolean askedBgOnce = false;

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ongoing, container, false);

        queue = Volley.newRequestQueue(requireContext());

        reportLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        adapter.notifyDataSetChanged();
                    }
                });

        singlePermLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> requestFineThenNotifThenBackgroundIfNeeded()
        );

        recyclerView = view.findViewById(R.id.rv_ongoing_appointments);
        emptyStateView = view.findViewById(R.id.tv_empty_state);

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
        requestFineThenNotifThenBackgroundIfNeeded();
        startAppointmentRefresh();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAppointmentRefresh();
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

                        recyclerView.post(() -> {
                            clearAllLists();

                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject o = arr.optJSONObject(i);
                                if (o == null) continue;

                                String status = o.optString("status", "Unknown");
                                if (!"Confirmed".equalsIgnoreCase(status)) continue;

                                String apptId = o.optString("appointment_id", "");
                                appointmentIds.add(apptId);
                                patientNames.add(o.optString("patient_name", ""));
                                problems.add(o.optString("reason_for_visit", ""));
                                mapLinks.add(o.optString("patient_map_link", ""));
                                hasReport.add(o.optInt("has_report", 0) == 1);
                                distances.add("Calculating...");
                                amounts.add(o.optString("amount", "0.00"));
                                paymentMethods.add(o.optString("payment_method", "Unknown"));

                                boolean isVet = o.optInt("is_vet_case", 0) == 1;
                                vetCases.add(isVet);
                                animalCategoryNames.add(clean(o.optString("animal_category_name", "")));
                                animalBreeds.add(clean(o.optString("animal_breed", "")));
                                vaccinationNames.add(clean(o.optString("vaccination_name", "")));

                                if (appointmentIds.size() == 1) {
                                    requireContext().getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE)
                                            .edit()
                                            .putString("ongoing_appointment_id", apptId)
                                            .apply();
                                }
                            }

                            adapter.setVetCases(vetCases);
                            adapter.setVetData(animalCategoryNames, animalBreeds, vaccinationNames);
                            adapter.notifyDataSetChanged();

                            // Toggle empty state vs list
                            toggleEmptyState(appointmentIds.isEmpty());

                            ensureServicesBasedOnAppointments();

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

    @SuppressLint("NotifyDataSetChanged")
    private void applyEmptyAppointments() {
        recyclerView.post(() -> {
            clearAllLists();
            adapter.setVetCases(vetCases);
            adapter.setVetData(animalCategoryNames, animalBreeds, vaccinationNames);
            adapter.notifyDataSetChanged();
            toggleEmptyState(true);
            stopTrackingServices();
            requireContext().getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE)
                    .edit().remove("ongoing_appointment_id").apply();
        });
    }

    private void toggleEmptyState(boolean isEmpty) {
        if (isEmpty) {
            recyclerView.setVisibility(View.GONE);
            emptyStateView.setVisibility(View.VISIBLE);
        } else {
            emptyStateView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
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

        vetCases.clear();
        animalCategoryNames.clear();
        animalBreeds.clear();
        vaccinationNames.clear();
    }

    private void completeAppointment(String appointmentId, int position) {
        String url = ApiConfig.endpoint("Doctors/completeAppointment.php");

        queue.add(new StringRequest(
                Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject root = new JSONObject(response);
                        if (root.optBoolean("success", false)) {

                            appointmentIds.remove(position);
                            patientNames.remove(position);
                            problems.remove(position);
                            distances.remove(position);
                            mapLinks.remove(position);
                            hasReport.remove(position);
                            amounts.remove(position);
                            paymentMethods.remove(position);

                            if (position < vetCases.size())            vetCases.remove(position);
                            if (position < animalCategoryNames.size()) animalCategoryNames.remove(position);
                            if (position < animalBreeds.size())        animalBreeds.remove(position);
                            if (position < vaccinationNames.size())    vaccinationNames.remove(position);

                            adapter.setVetCases(vetCases);
                            adapter.setVetData(animalCategoryNames, animalBreeds, vaccinationNames);
                            adapter.notifyItemRemoved(position);
                            adapter.notifyItemRangeChanged(position, appointmentIds.size());

                            Toast.makeText(getContext(), "Appointment completed!", Toast.LENGTH_SHORT).show();

                            if (appointmentIds.isEmpty()) {
                                toggleEmptyState(true);
                                requireContext().getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE)
                                        .edit().remove("ongoing_appointment_id").apply();
                                stopTrackingServices();
                            } else {
                                toggleEmptyState(false);
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

    private void requestFineThenNotifThenBackgroundIfNeeded() {
        if (!askedFineOnce) {
            askedFineOnce = true;
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                singlePermLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                return;
            }
        }

        if (!askedNotifOnce && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            askedNotifOnce = true;
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                singlePermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                return;
            }
        }

        if (!askedBgOnce && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            askedBgOnce = true;
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                singlePermLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
                return;
            }
        }

        ensureServicesBasedOnAppointments();
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

    // Normalize backend oddities
    private static String clean(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.isEmpty()) return "";
        String v = t.toLowerCase();
        if (v.equals("null") || v.equals("none") || v.equals("n/a") || v.equals("na") || v.equals("undefined"))
            return "";
        return t;
    }
}
