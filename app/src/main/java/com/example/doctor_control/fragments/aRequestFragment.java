package com.example.doctor_control.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.doctor_control.R;
import com.example.doctor_control.adapter.aRequestAdapeter;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class aRequestFragment extends Fragment {

    private static final String TAG = "aRequestFragment";
    private static final int REQUEST_LOCATION_PERMISSION = 100;

    private RecyclerView recyclerView;
    private aRequestAdapeter adapter;
    private final ArrayList<aRequestAdapeter.Appointment> appointments = new ArrayList<>();
    private String doctorId;

    // Doctor's current location (fetched on the spot)
    private double doctorLat, doctorLon;
    private FusedLocationProviderClient fusedLocationProviderClient;

    // Handler for periodic refresh
    private final Handler refreshHandler = new Handler();
    // Auto-refresh runnable with interval of 5 seconds (5000 milliseconds)
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Auto-refresh triggered");
            appointments.clear();
            fetchDataFromServer();
            refreshHandler.postDelayed(this, 5000);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        View view = inflater.inflate(R.layout.fragment_request, container, false);

        // Retrieve doctor_id from SharedPreferences (for identifier only)
        doctorId = String.valueOf(requireActivity()
                .getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE)
                .getInt("doctor_id", 1)); // Default to 1 if not found
        Log.d(TAG, "Doctor ID retrieved: " + doctorId);

        recyclerView = view.findViewById(R.id.rv_pending_appointments);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new aRequestAdapeter(getContext(), appointments);
        recyclerView.setAdapter(adapter);

        // Initialize the fused location client to get current location
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Check location permissions
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request location permissions if not granted
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
            // Return the view; location fetching will occur after permissions are granted.
            return view;
        }

        // Fetch doctor's current location and then data from server
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                doctorLat = location.getLatitude();
                doctorLon = location.getLongitude();
                Log.d(TAG, "Doctor current location: " + doctorLat + ", " + doctorLon);
            } else {
                Log.d(TAG, "Doctor location is null; using default values 0,0");
                doctorLat = 0;
                doctorLon = 0;
            }
            // Now fetch the appointments data from the server
            fetchDataFromServer();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching doctor location", e);
            doctorLat = 0;
            doctorLon = 0;
            fetchDataFromServer();
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        refreshHandler.postDelayed(refreshRunnable, 5000);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called. Removing refresh callbacks.");
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        Log.d(TAG, "setUserVisibleHint: isVisibleToUser = " + isVisibleToUser);
        if (isVisibleToUser && isResumed()) {
            Log.d(TAG, "Fragment visible and resumed in setUserVisibleHint. Starting refresh runnable.");
            refreshHandler.postDelayed(refreshRunnable, 5000);
        } else {
            Log.d(TAG, "Fragment not visible in setUserVisibleHint. Removing refresh runnable.");
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fetchDataFromServer() {
        String url = "http://sxm.a58.mytemp.website/Doctors/getRequestappointment.php?doctor_id=" + doctorId;
        Log.d(TAG, "Fetching data from server with URL: " + url);

        RequestQueue queue = Volley.newRequestQueue(requireContext());
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            Log.d(TAG, "Response: " + response.toString());
            try {
                boolean success = response.optBoolean("success", false);
                Log.d(TAG, "Success flag in response: " + success);
                if (success) {
                    JSONArray dataArray = response.optJSONArray("data");
                    if (dataArray != null) {
                        appointments.clear();
                        Log.d(TAG, "Data array length: " + dataArray.length());
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject appointmentObj = dataArray.getJSONObject(i);
                            String appointmentId = appointmentObj.optString("appointment_id", "0");
                            String name = appointmentObj.optString("patient_name", "N/A");
                            String problem = appointmentObj.optString("reason_for_visit", "N/A");
                            // The response now gives a Google Maps URL
                            String patientMapLink = appointmentObj.optString("patient_map_link", "");
                            String distanceStr = "N/A";

                            // Parse the URL to extract coordinates from the query parameter
                            if (!patientMapLink.isEmpty() && patientMapLink.contains("query=")) {
                                String[] splitArr = patientMapLink.split("query=");
                                if (splitArr.length > 1) {
                                    String coordStr = splitArr[1];
                                    // Remove any trailing parameters if present
                                    int ampIndex = coordStr.indexOf("&");
                                    if (ampIndex != -1) {
                                        coordStr = coordStr.substring(0, ampIndex);
                                    }
                                    String[] parts = coordStr.split(",");
                                    if (parts.length == 2) {
                                        try {
                                            double patientLat = Double.parseDouble(parts[0].trim());
                                            double patientLon = Double.parseDouble(parts[1].trim());
                                            float[] results = new float[1];
                                            Location.distanceBetween(doctorLat, doctorLon, patientLat, patientLon, results);
                                            float distanceInKm = results[0] / 1000.0f;
                                            distanceStr = String.format("%.2f km", distanceInKm);
                                        } catch (NumberFormatException e) {
                                            Log.e(TAG, "Error parsing patient coordinates: " + e.getMessage());
                                        }
                                    }
                                }
                            }

                            Log.d(TAG, "Parsed appointment: " + appointmentId + ", " + name + ", " + problem + ", " + distanceStr);
                            aRequestAdapeter.Appointment appointment =
                                    new aRequestAdapeter.Appointment(appointmentId, name, problem, distanceStr);
                            appointments.add(appointment);
                        }
                        adapter.notifyDataSetChanged();
                        Log.d(TAG, "Adapter notified. Appointments list size: " + appointments.size());
                    } else {
                        Log.d(TAG, "Data array is null");
                        Toast.makeText(getContext(), "No appointments found.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String message = response.optString("message", "Failed to load data.");
                    Log.d(TAG, "Server returned failure: " + message);
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing JSON", e);
                Toast.makeText(getContext(), "Error parsing data.", Toast.LENGTH_SHORT).show();
            }
        }, error -> {
            Log.e(TAG, "Error fetching data from server", error);
            Toast.makeText(getContext(), "Error fetching data from server.", Toast.LENGTH_SHORT).show();
        }) {
            @Override
            protected Map<String, String> getParams() {
                // Although this is a GET request, parameters can be added if needed.
                Map<String, String> params = new HashMap<>();
                params.put("doctor_id", doctorId);
                return params;
            }
        };

        queue.add(request);
    }
}
