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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class aPendingFragment extends Fragment {

    private static final String TAG = "aPendingFragment";

    private RecyclerView recyclerView;
    private apendingAdapter adapter;
    // Ensure ArrayList is imported from java.util
    private final ArrayList<apendingAdapter.Appointment> appointments = new ArrayList<>();
    private String doctorId;

    // Handler for periodic refresh
    private final Handler refreshHandler = new Handler();
    // Auto-refresh runnable set to 5 seconds (5000 ms)
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Auto-refresh triggered");
            // Clear existing data before fetching fresh data
            appointments.clear();
            fetchDataFromServer();
            // Schedule next refresh in 5 seconds
            refreshHandler.postDelayed(this, 5000);
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
        String url = "http://sxm.a58.mytemp.website/Doctors/getPendingappointment.php?doctor_id=" + doctorId;
        Log.d(TAG, "Fetching data from URL: " + url);

        RequestQueue queue = Volley.newRequestQueue(requireContext());
        @SuppressLint("NotifyDataSetChanged")
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

                // Clear current data and repopulate
                appointments.clear();

                // If the array is empty, show a toast and update adapter
                if (arr.length() == 0) {
                    Toast.makeText(getContext(), "No pending appointments found.", Toast.LENGTH_SHORT).show();
                    adapter.notifyDataSetChanged();
                    return;
                }

                // Optionally load locally stored completed report IDs
                SharedPreferences prefs = requireContext().getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
                HashSet<String> completedSet = new HashSet<>(prefs.getStringSet("completed_reports", new HashSet<>()));

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String apptId = obj.getString("appointment_id");

                    // Create an Appointment object (ensure your adapter's Appointment class is defined)
                    apendingAdapter.Appointment appointment = new apendingAdapter.Appointment(
                            apptId,
                            obj.getString("patient_name"),
                            obj.getString("reason_for_visit"),
                            obj.getString("time_slot")
                    );
                    appointments.add(appointment);
                }
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
