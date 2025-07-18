package com.infowave.doctor_control.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.infowave.doctor_control.DistanceCalculator;
import com.infowave.doctor_control.R;
import com.infowave.doctor_control.adapter.aRequestAdapeter;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class aRequestFragment extends Fragment {
    private static final int REFRESH_INTERVAL = 5000;

    private RecyclerView recyclerView;
    private aRequestAdapeter adapter;
    private final ArrayList<aRequestAdapeter.Appointment> appointments = new ArrayList<>();
    private String doctorId;
    private RequestQueue queue;

    private FusedLocationProviderClient fusedClient;
    private double doctorLat, doctorLon;

    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_request, container, false);
        queue = Volley.newRequestQueue(requireContext());
        fusedClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        recyclerView = view.findViewById(R.id.rv_pending_appointments);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new aRequestAdapeter(getContext(), appointments);
        recyclerView.setAdapter(adapter);

        doctorId = String.valueOf(getActivity()
                .getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE)
                .getInt("doctor_id", 1));

        // Prime doctor location once
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    getActivity(),
                    new String[]{ Manifest.permission.ACCESS_FINE_LOCATION },
                    0
            );
        } else {
            fusedClient.getLastLocation()
                    .addOnSuccessListener(loc -> {
                        if (loc != null) {
                            doctorLat = loc.getLatitude();
                            doctorLon = loc.getLongitude();
                        }
                        // kick off first load
                        fetchDataFromServer();
                    });
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        startAutoRefresh();
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
                fetchDataFromServer();
                refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
            }
        };
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
    }

    private void stopAutoRefresh() {
        if (refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fetchDataFromServer() {
        String url = "https://thedoctorathome.in/Doctors/getRequestappointment.php"
                + "?doctor_id=" + doctorId;

        queue.add(new StringRequest(
                com.android.volley.Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject root = new JSONObject(response);
                        boolean success = root.optBoolean("success", false);
                        JSONArray data = root.optJSONArray("data");

                        appointments.clear();
                        if (!success || data == null || data.length() == 0) {
                            adapter.notifyDataSetChanged();
                            return;
                        }

                        // Collect map links
                        List<String> links = new ArrayList<>();
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject obj = data.getJSONObject(i);
                            String apptId        = obj.optString("appointment_id", "0");
                            String name          = obj.optString("patient_name", "N/A");
                            String problem       = obj.optString("reason_for_visit", "N/A");
                            String link          = obj.optString("patient_map_link", "");
                            String totalPayment  = obj.optString("amount", "0.00");
                            String paymentMethod = obj.optString("payment_method", "Unknown");

                            appointments.add(new aRequestAdapeter.Appointment(
                                    apptId, name, problem, "…", totalPayment, paymentMethod
                            ));
                            links.add(link);
                        }

                        adapter.notifyDataSetChanged();

                        // Single batch distance lookup
                        DistanceCalculator.calculateDistanceBatch(
                                requireActivity(),
                                queue,
                                doctorLat,
                                doctorLon,
                                links,
                                distanceList -> {
                                    for (int i = 0; i < distanceList.size(); i++) {
                                        appointments.get(i).setDistance(distanceList.get(i));
                                    }
                                    adapter.notifyDataSetChanged();
                                }
                        );

                    } catch (JSONException je) {
                        Toast.makeText(getContext(),
                                "Error parsing data.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(getContext(),
                            "Error fetching data.", Toast.LENGTH_SHORT).show();
                }
        ));
    }
}
