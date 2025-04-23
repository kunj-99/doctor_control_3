package com.example.doctor_control.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
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
import com.example.doctor_control.adapter.apendingAdapter;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class aPendingFragment extends Fragment {

    private static final String TAG = "aPendingFragment";

    private RecyclerView recyclerView;
    private apendingAdapter adapter;
    // List of Appointment objects for the adapter
    private final ArrayList<apendingAdapter.Appointment> appointments = new ArrayList<>();
    private String doctorId;

    // Doctor’s current location variables
    private double doctorLat, doctorLon;
    private FusedLocationProviderClient fusedLocationClient;

    // Handler for periodic refresh
    private final Handler refreshHandler = new Handler();
    // Auto-refresh runnable set to refresh every 5 seconds (5000 ms)
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Auto-refresh triggered");
            appointments.clear();
            fetchDataFromServer();
            refreshHandler.postDelayed(this, 1000); // Adjust the delay here if needed
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        View view = inflater.inflate(R.layout.fragment_pending, container, false);

        // Retrieve doctor_id from SharedPreferences using "DoctorPrefs"
        SharedPreferences prefs = requireActivity().getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
        doctorId = String.valueOf(prefs.getInt("doctor_id", 0)); // Defaults to 0 if not found
        Log.d(TAG, "Doctor ID retrieved: " + doctorId);

        // Setup RecyclerView and Adapter
        recyclerView = view.findViewById(R.id.rv_pending_appointments);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new apendingAdapter(getContext(), appointments);
        recyclerView.setAdapter(adapter);

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        // Check for location permission and fetch location
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Request permission if not granted
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            // Use default location values if permission is not granted
            doctorLat = 0;
            doctorLon = 0;
            fetchDataFromServer();
        } else {
            // Get the last known location
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    doctorLat = location.getLatitude();
                    doctorLon = location.getLongitude();
                    Log.d(TAG, "Doctor current location: " + doctorLat + ", " + doctorLon);
                } else {
                    Log.d(TAG, "Doctor location is null; using default values 0,0");
                    doctorLat = 0;
                    doctorLon = 0;
                }
                fetchDataFromServer();
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching doctor location", e);
                doctorLat = 0;
                doctorLon = 0;
                fetchDataFromServer();
            });
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        // Start the periodic refresh when the fragment resumes
        refreshHandler.postDelayed(refreshRunnable, 1000);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called. Removing refresh callbacks.");
        // Remove callbacks to stop refresh when the fragment is paused
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    /**
     * Note: setUserVisibleHint is deprecated in newer Fragment versions.
     * If using a ViewPager, consider using setMaxLifecycle instead.
     */
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

    /**
     * This method fetches pending appointments from the server and calculates the distance
     * between the doctor and the patient's location using the map link.
     */
    @SuppressLint("NotifyDataSetChanged")
    private void fetchDataFromServer() {
        String url = "http://sxm.a58.mytemp.website/Doctors/getPendingappointment.php?doctor_id=" + doctorId;
        Log.d(TAG, "Fetching data from URL: " + url);

        RequestQueue queue = Volley.newRequestQueue(requireContext());
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            Log.d(TAG, "Response: " + response.toString());
            try {
                JSONObject root = new JSONObject(response.toString());
                boolean success = root.getBoolean("success");

                if (!success) {
                    Toast.makeText(getContext(), "No pending appointments found.", Toast.LENGTH_SHORT).show();
                    adapter.notifyDataSetChanged();
                    return;
                }

                JSONArray arr = root.getJSONArray("appointments");

                // Clear current data and repopulate the appointments list
                appointments.clear();

                if (arr.length() == 0) {
                    Toast.makeText(getContext(), "No pending appointments found.", Toast.LENGTH_SHORT).show();
                    adapter.notifyDataSetChanged();
                    return;
                }

                // Load any locally stored completed report IDs (optional)
                SharedPreferences prefs = requireContext().getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
                HashSet<String> completedSet = new HashSet<>(prefs.getStringSet("completed_reports", new HashSet<>()));

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String apptId = obj.getString("appointment_id");

                    // Extract the patient_map_link if available
                    String patientMapLink = obj.optString("patient_map_link", "");
                    String distanceStr = "N/A";
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

                    Log.d(TAG, "Parsed appointment: " + apptId + ", " +
                            obj.getString("patient_name") + ", " +
                            obj.getString("reason_for_visit") + ", " + distanceStr);

                    // Create and add an Appointment object using the computed distance and map link
                    apendingAdapter.Appointment appointment = new apendingAdapter.Appointment(
                            apptId,
                            obj.getString("patient_name"),
                            obj.getString("reason_for_visit"),
                            distanceStr,
                            patientMapLink  // Pass the map link into the Appointment object
                    );
                    appointments.add(appointment);
                }
                // Notify adapter about data changes
                adapter.notifyDataSetChanged();
                Log.d(TAG, "Adapter updated. Total appointments: " + appointments.size());
            } catch (JSONException e) {
                Log.e(TAG, "JSON parsing error: ", e);
                Toast.makeText(getContext(), "Parse error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, error -> {
            Log.e(TAG, "Volley error: ", error);
            Toast.makeText(getContext(), "Network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
        }) {
            // Although this is a GET request, you can override getParams() if you ever switch to POST
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("doctor_id", doctorId);
                return params;
            }
        };

        queue.add(request);
    }
}
