package com.example.doctor_control.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.doctor_control.R;
import com.example.doctor_control.adapter.aOngoingAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class aOngoingFragment extends Fragment {

    private static final String TAG = "aOngoingFragment";

    private RecyclerView recyclerView;
    private aOngoingAdapter adapter;
    private ArrayList<String> patientNames, problems, distances, appointmentIds;
    private ArrayList<Boolean> hasReport;
    private String doctorId;

    // Handler for periodic refresh
    private final Handler refreshHandler = new Handler();
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Auto-refresh triggered");
            fetchAppointmentsData(doctorId);
            // Schedule next refresh in 5 seconds (5000 milliseconds)
            refreshHandler.postDelayed(this, 5000);
        }
    };

    private ActivityResultLauncher<Intent> reportLauncher;

    // Declare a variable to track fragment visibility
    private boolean isActivityVisible = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        View view = inflater.inflate(R.layout.fragment_ongoing, container, false);

        recyclerView = view.findViewById(R.id.rv_ongoing_appointments);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        patientNames = new ArrayList<>();
        problems = new ArrayList<>();
        distances = new ArrayList<>();
        appointmentIds = new ArrayList<>();
        hasReport = new ArrayList<>();

        // Activity result for when report is completed
        reportLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getBooleanExtra("report_submitted", false)) {
                            String apptId = data.getStringExtra("appointment_id");

                            // Save to SharedPreferences
                            SharedPreferences prefs = requireContext().getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
                            HashSet<String> completedSet = new HashSet<>(prefs.getStringSet("completed_reports", new HashSet<>()));
                            completedSet.add(apptId);
                            prefs.edit().putStringSet("completed_reports", completedSet).apply();

                            // Update in list and adapter
                            int index = appointmentIds.indexOf(apptId);
                            if (index != -1) {
                                hasReport.set(index, true);
                                adapter.notifyItemChanged(index);
                            }
                        }
                    }
                });

        // Get doctorId from SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
        int id = prefs.getInt("doctor_id", -1);

        if (id == -1) {
            Toast.makeText(getContext(), "Doctor ID not found!", Toast.LENGTH_SHORT).show();
        } else {
            doctorId = String.valueOf(id);
            fetchAppointmentsData(doctorId);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        isActivityVisible = true;
        refreshHandler.postDelayed(refreshRunnable, 5000);
    }

    @Override
    public void onPause() {
        super.onPause();
        isActivityVisible = false;
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    // For fragments in a ViewPager: start/stop refresh based on visibility
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && isResumed()) {
            isActivityVisible = true;
            refreshHandler.postDelayed(refreshRunnable, 5000);
        } else {
            isActivityVisible = false;
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }

    private void fetchAppointmentsData(String doctorId) {
        String url = "http://sxm.a58.mytemp.website/Doctors/getOngoingAppointment.php";
        RequestQueue queue = Volley.newRequestQueue(requireContext());

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject root = new JSONObject(response);
                        boolean success = root.getBoolean("success");

                        if (!success) {
                            Toast.makeText(getContext(), "No ongoing appointments.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        JSONArray arr = root.getJSONArray("appointments");

                        patientNames.clear();
                        problems.clear();
                        distances.clear();
                        appointmentIds.clear();
                        hasReport.clear();

                        // Load locally stored completed report IDs
                        SharedPreferences prefs = requireContext().getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
                        HashSet<String> completedSet = new HashSet<>(prefs.getStringSet("completed_reports", new HashSet<>()));

                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            String apptId = obj.getString("appointment_id");

                            patientNames.add(obj.getString("patient_name"));
                            problems.add(obj.getString("reason_for_visit"));
                            distances.add(obj.getString("time_slot"));
                            appointmentIds.add(apptId);

                            // Enable "Complete" button if report already saved
                            hasReport.add(completedSet.contains(apptId));
                        }

                        // Initialize or update adapter
                        if (adapter == null) {
                            adapter = new aOngoingAdapter(
                                    getContext(),
                                    appointmentIds,
                                    patientNames,
                                    problems,
                                    distances,
                                    hasReport,
                                    reportLauncher
                            );
                            recyclerView.setAdapter(adapter);
                        } else {
                            adapter.notifyDataSetChanged();
                        }

                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: ", e);
                        Toast.makeText(getContext(), "Parse error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
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

    private void updateDoctorAutoStatus() {
        String updateUrl = "http://sxm.a58.mytemp.website/update_doctor_status.php";
        StringRequest updateRequest = new StringRequest(Request.Method.GET, updateUrl,
                response -> {
                    // Minimal logging
                },
                error -> {
                    // Minimal logging
                }
        );
        RequestQueue queue = Volley.newRequestQueue(requireContext());
        queue.add(updateRequest);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        refreshHandler.removeCallbacks(refreshRunnable);
    }
}
