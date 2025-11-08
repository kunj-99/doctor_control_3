package com.infowave.doctor_control.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.infowave.doctor_control.ApiConfig;
import com.infowave.doctor_control.DistanceCalculator;
import com.infowave.doctor_control.R;
import com.infowave.doctor_control.adapter.apendingAdapter;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class aPendingFragment extends Fragment {
    // 🔁 Smooth refresh every 1.5s
    private static final int REFRESH_INTERVAL_MS = 1500;

    private RecyclerView recyclerView;
    private TextView emptyStateView;
    private apendingAdapter adapter;
    private final ArrayList<apendingAdapter.Appointment> appointments = new ArrayList<>();

    private String doctorId;

    private double doctorLat, doctorLon;
    private FusedLocationProviderClient fusedClient;

    private RequestQueue queue;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pending, container, false);

        queue = Volley.newRequestQueue(requireContext());
        fusedClient = LocationServices.getFusedLocationProviderClient(requireContext());

        recyclerView = view.findViewById(R.id.rv_pending_appointments);
        emptyStateView = view.findViewById(R.id.tv_empty_state_pending);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new apendingAdapter(getContext(), appointments);
        recyclerView.setAdapter(adapter);

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
        doctorId = String.valueOf(prefs.getInt("doctor_id", 0));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        startAutoRefresh();

        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1
            );
        } else {
            fusedClient.getLastLocation()
                    .addOnSuccessListener(loc -> {
                        if (loc != null) {
                            doctorLat = loc.getLatitude();
                            doctorLon = loc.getLongitude();
                        }
                        fetchPendingAppointments();
                    })
                    .addOnFailureListener(e -> fetchPendingAppointments());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAutoRefresh();
    }

    private void startAutoRefresh() {
        stopAutoRefresh();
        refreshRunnable = () -> {
            if (isAdded()) {
                fetchPendingAppointments();
                handler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS);
            }
        };
        handler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS);
    }

    private void stopAutoRefresh() {
        if (refreshRunnable != null) handler.removeCallbacks(refreshRunnable);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fetchPendingAppointments() {
        String url = ApiConfig.endpoint("Doctors/getPendingappointment.php", "doctor_id", doctorId);

        queue.add(new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        boolean success = response.optBoolean("success", false);
                        appointments.clear();

                        if (success && response.has("data")) {
                            JSONArray arr = response.getJSONArray("data");

                            List<String> links = new ArrayList<>(arr.length());

                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject o = arr.getJSONObject(i);

                                String id            = o.optString("appointment_id");
                                String name          = o.optString("patient_name");
                                String reason        = o.optString("reason_for_visit");
                                String link          = o.optString("patient_map_link", "");
                                String amount        = o.optString("amount", "0.00");
                                String paymentMethod = o.optString("payment_method", "Unknown");

                                // Vet fields
                                String animalCategoryName = clean(o.optString("animal_category_name", ""));
                                String animalBreed        = clean(o.optString("animal_breed", ""));
                                String vaccinationName    = clean(o.optString("vaccination_name", ""));

                                appointments.add(new apendingAdapter.Appointment(
                                        id,
                                        name,
                                        reason,
                                        "Calculating...",
                                        link,
                                        amount,
                                        paymentMethod,
                                        animalCategoryName,
                                        animalBreed,
                                        vaccinationName
                                ));
                                links.add(link);
                            }

                            adapter.notifyDataSetChanged();

                            // Toggle empty state
                            toggleEmptyState(appointments.isEmpty());

                            // Distance batch (only if we have rows)
                            if (!appointments.isEmpty()) {
                                DistanceCalculator.calculateDistanceBatch(
                                        requireActivity(),
                                        queue,
                                        doctorLat, doctorLon,
                                        links,
                                        results -> {
                                            for (int i = 0; i < results.size() && i < appointments.size(); i++) {
                                                appointments.get(i).setDistance(results.get(i));
                                            }
                                            adapter.notifyDataSetChanged();
                                        }
                                );
                            }
                        } else {
                            // No data
                            adapter.notifyDataSetChanged();
                            toggleEmptyState(true);
                        }
                    } catch (JSONException ignored) {
                        toggleEmptyState(appointments.isEmpty());
                    }
                },
                error -> {
                    // Keep UI smooth, just reflect current list
                    toggleEmptyState(appointments.isEmpty());
                }
        ));
    }

    private void toggleEmptyState(boolean isEmpty) {
        if (emptyStateView == null || recyclerView == null) return;
        if (isEmpty) {
            recyclerView.setVisibility(View.GONE);
            emptyStateView.setVisibility(View.VISIBLE);
        } else {
            emptyStateView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    // Normalize backend oddities like "null", "N/A", "undefined", or whitespace to empty string
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
