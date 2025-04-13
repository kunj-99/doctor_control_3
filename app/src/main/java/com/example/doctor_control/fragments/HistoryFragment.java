package com.example.doctor_control.fragments;

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
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.doctor_control.R;
import com.example.doctor_control.adapter.HistoryAdapter;
import com.example.doctor_control.HistoryItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class HistoryFragment extends Fragment {

    private static final String TAG = "HistoryFragment";
    private RecyclerView rvHistory;
    private HistoryAdapter historyAdapter;
    // List of HistoryItem objects (assumed to include all appointment details)
    private final ArrayList<HistoryItem> historyItems = new ArrayList<>();
    // Separate list to hold appointment IDs
    private final ArrayList<String> appointmentIds = new ArrayList<>();
    private RequestQueue requestQueue;

    // Updated URL with doctor_id parameter
    private static final String BASE_URL = "http://sxm.a58.mytemp.website/Doctors/gethistory.php?doctor_id=";
    private final Handler refreshHandler = new Handler();
    // Auto-refresh every 5 seconds (5000 ms)
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Auto-refresh triggered");
            historyItems.clear();
            appointmentIds.clear();
            fetchHistoryData(getDoctorId());
            refreshHandler.postDelayed(this, 5000);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        rvHistory = view.findViewById(R.id.rv_history);
        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));

        // Clear lists and initialize adapter with both history items and appointment IDs
        historyItems.clear();
        appointmentIds.clear();
        historyAdapter = new HistoryAdapter(historyItems, appointmentIds);
        rvHistory.setAdapter(historyAdapter);

        requestQueue = Volley.newRequestQueue(getContext());
        fetchHistoryData(getDoctorId());

        return view;
    }

    private String getDoctorId() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
        int doctorIdInt = sharedPreferences.getInt("doctor_id", 0);
        String doctorId = String.valueOf(doctorIdInt);
        Log.d(TAG, "Retrieved doctor_id: " + doctorId);
        return doctorId;
    }

    private void fetchHistoryData(String doctorId) {
        String url = BASE_URL + doctorId;
        Log.d(TAG, "Fetching history data from URL: " + url);

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    Log.d(TAG, "Received response with length: " + response.length());
                    try {
                        // Clear current lists before adding new data
                        historyItems.clear();
                        appointmentIds.clear();

                        for (int i = 0; i < response.length(); i++) {
                            JSONObject object = response.getJSONObject(i);

                            // Extract appointment ID and add it to the separate list
                            String apptId = object.getString("appointment_id");
                            appointmentIds.add(apptId);

                            // Extract other values from JSON
                            String patientName = object.getString("patient_name");
                            String appointmentDate = object.getString("appointment_date");
                            String symptoms = object.getString("reason_for_visit");
                            boolean flag = object.optBoolean("flag", false);
                            String patientId = object.getString("patient_id");

                            // Extract the status field
                            String status = object.optString("status", "Unknown");

                            // Create a HistoryItem including the new status field.
                            HistoryItem item = new HistoryItem(patientName, appointmentDate, symptoms, flag, patientId, apptId, status);
                            historyItems.add(item);
                            Log.d(TAG, "Added history item: " + patientName + ", appointmentId: " + apptId + ", status: " + status);
                        }
                        historyAdapter.notifyDataSetChanged();
                        Log.d(TAG, "History adapter notified. Total items: " + historyItems.size());
                        if (historyItems.isEmpty()) {
                            Toast.makeText(getContext(), "No pending appointments found.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing JSON response", e);
                        Toast.makeText(getContext(), "Parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Volley error in fetching history data", error);
                    Toast.makeText(getContext(), "Error fetching data from server.", Toast.LENGTH_SHORT).show();
                }
        );
        requestQueue.add(jsonArrayRequest);
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
}
