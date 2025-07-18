package com.infowave.doctor_control.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
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
import com.infowave.doctor_control.R;
import com.infowave.doctor_control.adapter.HistoryAdapter;
import com.infowave.doctor_control.HistoryItem;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class HistoryFragment extends Fragment {

    private RecyclerView rvHistory;
    private HistoryAdapter historyAdapter;

    private final ArrayList<HistoryItem> historyItems = new ArrayList<>();
    private final ArrayList<String> appointmentIds = new ArrayList<>();

    private RequestQueue requestQueue;
    private static final String BASE_URL = "https://thedoctorathome.in/Doctors/gethistory.php?doctor_id=";

    private final Handler refreshHandler = new Handler();
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            fetchHistoryData(getDoctorId());
            refreshHandler.postDelayed(this, 2000);
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

        requestQueue = Volley.newRequestQueue(requireContext());

        fetchHistoryData(getDoctorId());

        return view;
    }

    private String getDoctorId() {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
        int id = prefs.getInt("doctor_id", 0);
        return String.valueOf(id);
    }

    private void fetchHistoryData(String doctorId) {
        String url = BASE_URL + doctorId;

        @SuppressLint("NotifyDataSetChanged")
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    ArrayList<HistoryItem> tmpItems = new ArrayList<>();
                    ArrayList<String> tmpIds = new ArrayList<>();

                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);

                            String apptId = obj.getString("appointment_id");
                            String patientName = obj.getString("patient_name");
                            String appointmentDate = obj.getString("appointment_date");
                            String symptoms = obj.getString("reason_for_visit");
                            boolean flag = obj.optBoolean("flag", false);
                            String patientId = obj.getString("patient_id");
                            String status = obj.optString("status", "");

                            tmpIds.add(apptId);
                            tmpItems.add(new HistoryItem(
                                    patientName,
                                    appointmentDate,
                                    symptoms,
                                    flag,
                                    patientId,
                                    apptId,
                                    status
                            ));
                        }

                        historyItems.clear();
                        historyItems.addAll(tmpItems);

                        appointmentIds.clear();
                        appointmentIds.addAll(tmpIds);

                        historyAdapter.notifyDataSetChanged();

                        if (historyItems.isEmpty()) {
                            Toast.makeText(getContext(),
                                    "No appointment history found.",
                                    Toast.LENGTH_SHORT).show();
                        }

                    } catch (JSONException e) {
                        Toast.makeText(getContext(),
                                "Parse error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(getContext(),
                            "Error fetching data from server.",
                            Toast.LENGTH_SHORT).show();
                }
        );

        requestQueue.add(jsonArrayRequest);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshHandler.postDelayed(refreshRunnable, 2000);
    }

    @Override
    public void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(refreshRunnable);
    }
}
