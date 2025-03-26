package com.example.doctor_control.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.doctor_control.R;
import com.example.doctor_control.adapter.aRequestAdapeter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

public class aRequestFragment extends Fragment {

    private static final String TAG = "aRequestFragment";

    private RecyclerView recyclerView;
    private aRequestAdapeter adapter;
    private final ArrayList<aRequestAdapeter.Appointment> appointments = new ArrayList<>();
    private String doctorId;

    // Handler for periodic refresh
    private final Handler refreshHandler = new Handler();
    // Auto-refresh runnable with interval of 5 seconds (5000 milliseconds)
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Auto-refresh triggered");
            // Clear the list before fetching fresh data
            appointments.clear();
            fetchDataFromServer();
            // Schedule next refresh in 5 seconds
            refreshHandler.postDelayed(this, 5000);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        View view = inflater.inflate(R.layout.fragment_request, container, false);

        // Retrieve doctor_id from SharedPreferences (using getInt() because it's stored as an Integer)
        SharedPreferences prefs = requireActivity().getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
        doctorId = String.valueOf(prefs.getInt("doctor_id", 1)); // Default to 1 if not found
        Log.d(TAG, "Doctor ID retrieved: " + doctorId);

        recyclerView = view.findViewById(R.id.rv_pending_appointments);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize the adapter with the appointments list
        adapter = new aRequestAdapeter(getContext(), appointments);
        recyclerView.setAdapter(adapter);

        // Fetch data from the server initially
        fetchDataFromServer();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        // Start periodic refresh every 5 seconds when fragment is visible
        refreshHandler.postDelayed(refreshRunnable, 5000);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called. Removing refresh callbacks.");
        // Stop periodic refresh when fragment is not visible
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    // For fragments in a ViewPager: start/stop refresh based on visibility
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

    private void fetchDataFromServer() {
        String url = "http://sxm.a58.mytemp.website/Doctors/getRequestappointment.php?doctor_id=" + doctorId;
        Log.d(TAG, "Fetching data from server with URL: " + url);

        RequestQueue queue = Volley.newRequestQueue(requireContext());
        @SuppressLint("NotifyDataSetChanged")
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            Log.d(TAG, "Response: " + response.toString());
            try {
                boolean success = response.optBoolean("success", false);
                Log.d(TAG, "Success flag in response: " + success);
                if (success) {
                    JSONArray dataArray = response.optJSONArray("data");
                    if (dataArray != null) {
                        // Clear current appointments before updating
                        appointments.clear();
                        Log.d(TAG, "Data array length: " + dataArray.length());
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject appointmentObj = dataArray.getJSONObject(i);
                            // Extract appointment details (adjust keys as per your JSON structure)
                            String appointmentId = appointmentObj.optString("appointment_id", "0");
                            String name = appointmentObj.optString("patient_name", "N/A");
                            String problem = appointmentObj.optString("reason_for_visit", "N/A");
                            String distance = appointmentObj.optString("distance", "N/A");

                            Log.d(TAG, "Parsed appointment: " + appointmentId + ", " + name + ", " + problem + ", " + distance);

                            // Create a new Appointment object and add to the list
                            aRequestAdapeter.Appointment appointment =
                                    new aRequestAdapeter.Appointment(appointmentId, name, problem, distance);
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
                Map<String, String> params = new HashMap<>();
                params.put("doctor_id", doctorId);
                return params;
            }
        };

        queue.add(request);
    }
}
