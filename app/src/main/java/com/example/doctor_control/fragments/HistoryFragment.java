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

    // Main lists used by adapter
    private final ArrayList<HistoryItem> historyItems = new ArrayList<>();
    private final ArrayList<String> appointmentIds = new ArrayList<>();

    private RequestQueue requestQueue;
    private static final String BASE_URL = "http://sxm.a58.mytemp.website/Doctors/gethistory.php?doctor_id=";

    private final Handler refreshHandler = new Handler();
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Auto-refresh triggered");
            fetchHistoryData(getDoctorId());
            refreshHandler.postDelayed(this, 2000); // Fast refresh every 2 seconds
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        rvHistory = view.findViewById(R.id.rv_history);
        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));

        historyAdapter = new HistoryAdapter(historyItems, appointmentIds);
        rvHistory.setAdapter(historyAdapter);

        requestQueue = Volley.newRequestQueue(getContext());

        fetchHistoryData(getDoctorId()); // Initial load

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

                    // Use temporary lists to prevent RecyclerView inconsistency
                    ArrayList<HistoryItem> tempHistoryItems = new ArrayList<>();
                    ArrayList<String> tempAppointmentIds = new ArrayList<>();

                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject object = response.getJSONObject(i);

                            String apptId = object.getString("appointment_id");
                            String patientName = object.getString("patient_name");
                            String appointmentDate = object.getString("appointment_date");
                            String symptoms = object.getString("reason_for_visit");
                            boolean flag = object.optBoolean("flag", false);
                            String patientId = object.getString("patient_id");

                            tempAppointmentIds.add(apptId);
                            tempHistoryItems.add(new HistoryItem(patientName, appointmentDate, symptoms, flag, patientId, apptId, ""));

                            Log.d(TAG, "Parsed item: " + patientName + " | " + apptId);
                        }

                        // Apply updates safely after all parsing is done
                        historyItems.clear();
                        historyItems.addAll(tempHistoryItems);

                        appointmentIds.clear();
                        appointmentIds.addAll(tempAppointmentIds);

                        historyAdapter.notifyDataSetChanged();
                        Log.d(TAG, "Adapter updated. Total history items: " + historyItems.size());

                        if (historyItems.isEmpty()) {
                            Toast.makeText(getContext(), "No appointment history found.", Toast.LENGTH_SHORT).show();
                        }

                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error", e);
                        Toast.makeText(getContext(), "Parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Volley error fetching history data", error);
                    Toast.makeText(getContext(), "Error fetching data from server.", Toast.LENGTH_SHORT).show();
                }
        );

        requestQueue.add(jsonArrayRequest);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Starting auto-refresh");
        refreshHandler.postDelayed(refreshRunnable, 2000);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Stopping auto-refresh");
        refreshHandler.removeCallbacks(refreshRunnable);
    }
}
