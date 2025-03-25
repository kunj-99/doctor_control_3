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
import com.example.doctor_control.adapter.apendingAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Objects;

public class aPendingFragment extends Fragment {

    private static final String TAG = "aPendingFragment";

    private RecyclerView recyclerView;
    private apendingAdapter adapter;
    private final ArrayList<apendingAdapter.Appointment> appointments = new ArrayList<>();
    private String doctorId;
    private final Handler refreshHandler = new Handler();
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Auto-refresh triggered");
            // Clear existing data before fetching fresh data
            appointments.clear();
            fetchDataFromServer();
            // Schedule next refresh in 20 seconds (20000 milliseconds)
            refreshHandler.postDelayed(this, 20000);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        View view = inflater.inflate(R.layout.fragment_pending, container, false);

        // Retrieve doctor_id from SharedPreferences using "DoctorPrefs"
        SharedPreferences prefs = requireActivity().getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
        doctorId = String.valueOf(prefs.getInt("doctor_id", 0)); // Default to "1" if not found
        Log.d(TAG, "Doctor ID retrieved: " + doctorId);

        recyclerView = view.findViewById(R.id.rv_pending_appointments);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize the adapter with the appointments list
        adapter = new apendingAdapter(getContext(), appointments);
        recyclerView.setAdapter(adapter);

        // Fetch data from the server initially
        fetchDataFromServer();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        // Start periodic refresh only if the fragment is visible
        if (getUserVisibleHint()) {
            Log.d(TAG, "Fragment is visible in onResume. Starting refresh runnable.");
            refreshHandler.postDelayed(refreshRunnable, 20000);
        } else {
            Log.d(TAG, "Fragment is not visible in onResume.");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called. Removing refresh callbacks.");
        // Stop periodic refresh when fragment is not in view
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        Log.d(TAG, "setUserVisibleHint: isVisibleToUser = " + isVisibleToUser);
        if (isVisibleToUser && isResumed()) {
            Log.d(TAG, "Fragment visible and resumed in setUserVisibleHint. Starting refresh runnable.");
            refreshHandler.postDelayed(refreshRunnable, 20000);
        } else {
            Log.d(TAG, "Fragment not visible in setUserVisibleHint. Removing refresh runnable.");
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }

    private void fetchDataFromServer() {
        // Replace this URL with your actual API endpoint
        String url = "http://sxm.a58.mytemp.website/Doctors/getPendingappointment.php?doctor_id=" + doctorId;
        Log.d(TAG, "Fetching data from URL: " + url);

        RequestQueue queue = Volley.newRequestQueue(requireContext());
        @SuppressLint("NotifyDataSetChanged") JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            Log.d(TAG, "Response: " + response.toString());
            try {
                boolean success = response.optBoolean("success", false);
                Log.d(TAG, "Success flag: " + success);
                if (success) {
                    JSONArray dataArray = response.optJSONArray("data");
                    if (dataArray != null) {
                        // Clear existing data before updating
                        appointments.clear();
                        Log.d(TAG, "Data array length: " + dataArray.length());
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject appointmentObj = dataArray.getJSONObject(i);
                            // Adjust these keys as per your JSON structure
                            String appointmentId = appointmentObj.optString("appointment_id", "0");
                            String name = appointmentObj.optString("patient_name", "N/A");
                            String problem = appointmentObj.optString("reason_for_visit", "N/A");
                            String distance = appointmentObj.optString("distance", "N/A");

                            Log.d(TAG, "Parsed appointment: " + appointmentId + ", " + name + ", " + problem + ", " + distance);

                            // Create and add an Appointment object
                            appointments.add(new apendingAdapter.Appointment(appointmentId, name, problem, distance));
                        }
                        adapter.notifyDataSetChanged();
                        Log.d(TAG, "Adapter updated. Total appointments: " + appointments.size());
                    } else {
                        Toast.makeText(getContext(), "No pending appointments found.", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Data array is null");
                    }
                } else {
                    String message = response.optString("message", "Failed to load data.");
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Server returned failure: " + message);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing JSON", e);
                Toast.makeText(getContext(), "Error parsing data.", Toast.LENGTH_SHORT).show();
            }
        }, error -> {
            Log.e(TAG, "Error fetching data from server", error);
            Toast.makeText(getContext(), "Error fetching data from server.", Toast.LENGTH_SHORT).show();
        });

        queue.add(request);
    }
}
