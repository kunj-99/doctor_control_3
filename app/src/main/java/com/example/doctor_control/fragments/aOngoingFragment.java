package com.example.doctor_control.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.doctor_control.R;
import com.example.doctor_control.adapter.aOngoingAdapter;
import com.example.doctor_control.LiveLocationManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class aOngoingFragment extends Fragment {

    private static final String TAG = "aOngoingFragment";
    private static final String LIVE_LOCATION_URL = "http://sxm.a58.mytemp.website/update_live_location.php";
    private static final int REFRESH_INTERVAL = 5000; // 5 seconds

    private RecyclerView recyclerView;
    private aOngoingAdapter adapter;
    private ArrayList<String> patientNames, problems, distances, appointmentIds, mapLinks;
    private ArrayList<Boolean> hasReport;
    private String doctorId;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    // Handler and Runnable for auto-refresh
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;
    // These variables store the doctor's latest location for distance computation.
    private double doctorLat, doctorLon ;

    private ActivityResultLauncher<Intent> reportLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ongoing, container, false);
        recyclerView = view.findViewById(R.id.rv_ongoing_appointments);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        patientNames = new ArrayList<>();
        problems = new ArrayList<>();
        distances = new ArrayList<>();
        appointmentIds = new ArrayList<>();
        hasReport = new ArrayList<>();
        mapLinks = new ArrayList<>();

        reportLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                String apptId = result.getData().getStringExtra("appointment_id");
                SharedPreferences prefs = requireContext().getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
                HashSet<String> completedSet = new HashSet<>(prefs.getStringSet("completed_reports", new HashSet<>()));
                completedSet.add(apptId);
                prefs.edit().putStringSet("completed_reports", completedSet).apply();

                // STOP LOCATION IF APPOINTMENT COMPLETED
                String ongoingId = prefs.getString("ongoing_appointment_id", null);
                if (ongoingId != null && ongoingId.equals(apptId)) {
                    prefs.edit().remove("ongoing_appointment_id").apply();
                    LiveLocationManager.getInstance().stopLocationUpdates(requireContext());
                    Log.d("LiveLocationManager", "Stopped tracking for completed appointment: " + apptId);
                }

                int index = appointmentIds.indexOf(apptId);
                if (index != -1) {
                    hasReport.set(index, true);
                    adapter.notifyItemChanged(index);
                }
            }
        });

        String logs = LiveLocationManager.getInstance().getLocationLogs(requireContext());
        Log.d("TrackingHistory", logs);

        SharedPreferences prefs = requireActivity().getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
        int id = prefs.getInt("doctor_id", -1);
        if (id == -1) {
            Toast.makeText(getContext(), "Doctor ID not found!", Toast.LENGTH_SHORT).show();
        } else {
            doctorId = String.valueOf(id);
            // We'll fetch appointments data later (after location is updated)
            fetchAppointmentsData(doctorId);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                if (!appointmentIds.isEmpty()) {
                    double lat = result.getLastLocation().getLatitude();
                    double lon = result.getLastLocation().getLongitude();
                    // Update doctor's location variables so distance calculations are accurate.
                    doctorLat = lat;
                    doctorLon = lon;
                    Log.d("LiveLocationCheck", "Doctor's current location updated: lat=" + lat + ", lon=" + lon);
                    sendLiveLocationToServer(doctorId, appointmentIds.get(0), lat, lon);
                } else {
                    Log.d("LiveLocationCheck", "No location or no appointments");
                }
            }
        };

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        startAutoRefresh();

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        // Request location updates.
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(15000)
                .setFastestInterval(10000)
                .setSmallestDisplacement(20)
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        Log.d(TAG, "Requesting optimized location updates...");
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

        // Get last known location (if available) to update doctor's location variables immediately.
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                doctorLat = location.getLatitude();
                doctorLon = location.getLongitude();
                Log.d(TAG, "Fetched last known location: lat=" + doctorLat + ", lon=" + doctorLon);
            } else {
                Log.d(TAG, "No last known location available.");
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAutoRefresh();
        // We intentionally do not remove location updates here to allow background tracking.
    }

    private void startAutoRefresh() {
        stopAutoRefresh(); // Remove any pending callbacks first
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAdded()) {
                    fetchAppointmentsData(doctorId);
                    refreshHandler.postDelayed(this, REFRESH_INTERVAL);
                }
            }
        };
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
    }

    private void stopAutoRefresh() {
        if (refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }

    private void fetchAppointmentsData(String doctorId) {
        String url = "http://sxm.a58.mytemp.website/Doctors/getOngoingAppointment.php";
        RequestQueue queue = Volley.newRequestQueue(requireContext());
        @SuppressLint("NotifyDataSetChanged")
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject root = new JSONObject(response);
                        if (!root.getBoolean("success")) return;
                        JSONArray arr = root.getJSONArray("appointments");

                        patientNames.clear();
                        problems.clear();
                        distances.clear();
                        appointmentIds.clear();
                        hasReport.clear();
                        mapLinks.clear();

                        SharedPreferences prefs = requireContext().getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
                        HashSet<String> completedSet = new HashSet<>(prefs.getStringSet("completed_reports", new HashSet<>()));

                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            String apptId = obj.getString("appointment_id");
                            appointmentIds.add(apptId);
                            patientNames.add(obj.getString("patient_name"));
                            problems.add(obj.getString("reason_for_visit"));

                            // Compute distance using patient's map link and the doctor's current location.
                            String distanceStr = obj.getString("time_slot"); // fallback value
                            String patientMapLink = obj.getString("patient_map_link");
                            Log.d(TAG, "Patient map link: " + patientMapLink);

                            if (!patientMapLink.isEmpty() && patientMapLink.contains("query=")) {
                                String[] splitArr = patientMapLink.split("query=");
                                if (splitArr.length > 1) {
                                    String coordStr = splitArr[1];
                                    int ampIndex = coordStr.indexOf("&");
                                    if (ampIndex != -1) {
                                        coordStr = coordStr.substring(0, ampIndex);
                                    }
                                    String[] parts = coordStr.split(",");
                                    if (parts.length == 2) {
                                        try {
                                            double patientLat = Double.parseDouble(parts[0].trim());
                                            double patientLon = Double.parseDouble(parts[1].trim());
                                            Log.d(TAG, "Parsed patient coordinates: lat=" + patientLat + ", lon=" + patientLon);
                                            float[] results = new float[1];
                                            Log.d(TAG, "Doctor location for distance calculation: lat=" + doctorLat + ", lon=" + doctorLon);
                                            Location.distanceBetween(doctorLat, doctorLon, patientLat, patientLon, results);
                                            Log.d(TAG, "Raw distance between (in meters): " + results[0]);
                                            float distanceInKm = results[0] / 1000.0f;
                                            distanceStr = String.format("%.2f km", distanceInKm);
                                            Log.d(TAG, "Computed distance: " + distanceStr);
                                        } catch (NumberFormatException e) {
                                            Log.e(TAG, "Error parsing patient coordinates: " + e.getMessage());
                                        }
                                    }
                                }
                            }
                            distances.add(distanceStr);
                            mapLinks.add(obj.getString("patient_map_link"));
                            hasReport.add(completedSet.contains(apptId));

                            // Save first ongoing appointment ID and start location updates if not already started.
                            if (i == 0) {
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString("ongoing_appointment_id", apptId);
                                editor.apply();
                                LiveLocationManager.getInstance().startLocationUpdates(requireContext().getApplicationContext());
                            }
                        }

                        if (adapter == null) {
                            adapter = new aOngoingAdapter(getContext(), appointmentIds, patientNames, problems, distances, hasReport, mapLinks, reportLauncher);
                            recyclerView.setAdapter(adapter);
                        } else {
                            adapter.notifyDataSetChanged();
                        }
                        Log.d(TAG, "Adapter updated. Total appointments: " + appointmentIds.size());
                    } catch (JSONException e) {
                        Log.e(TAG, "Parse error", e);
                    }
                }, error -> Log.e(TAG, "Network error", error)) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("doctor_id", doctorId);
                return params;
            }
        };
        queue.add(request);
    }

    private void sendLiveLocationToServer(String doctorId, String appointmentId, double lat, double lon) {
        StringRequest request = new StringRequest(Request.Method.POST, LIVE_LOCATION_URL,
                response -> Log.d("LiveLocationResponse", "Server response: " + response),
                error -> Log.e("LiveLocationError", "Volley error: " + error.getMessage())
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("doctor_id", doctorId);
                params.put("appointment_id", appointmentId);
                params.put("latitude", String.valueOf(lat));
                params.put("longitude", String.valueOf(lon));
                Log.d("LiveLocationSend", "Sending to server: " + params);
                return params;
            }
        };

        Volley.newRequestQueue(requireContext()).add(request);
    }
}
