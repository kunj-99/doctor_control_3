package com.example.doctor_control.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.doctor_control.R;
import com.example.doctor_control.adapter.aOngoingAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class aOngoingFragment extends Fragment {
    private static final String TAG = "aOngoingFragment";

    private RecyclerView recyclerView;
    private aOngoingAdapter adapter;
    private ArrayList<String> patientNames, problems, distances, appointmentIds;
    ArrayList<Boolean> hasReport = new ArrayList<>();
    private String doctorId;

    private final Handler refreshHandler = new Handler();
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Auto-refresh triggered");
            fetchAppointmentsData(doctorId);
            refreshHandler.postDelayed(this, 10000);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        View view = inflater.inflate(R.layout.fragment_ongoing, container, false);

        // Retrieve doctor_id from SharedPreferences
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
        int id = prefs.getInt("doctor_id", -1);
        if (id == -1) {
            Log.e(TAG, "Doctor ID not found in SharedPreferences!");
            Toast.makeText(getContext(), "Doctor ID missing — cannot load appointments.", Toast.LENGTH_LONG).show();
            doctorId = "";
        } else {
            doctorId = String.valueOf(id);
            Log.d(TAG, "Fetched doctorId=" + doctorId + " from SharedPreferences");
        }

        recyclerView = view.findViewById(R.id.rv_ongoing_appointments);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        patientNames  = new ArrayList<>();
        problems      = new ArrayList<>();
        distances     = new ArrayList<>();
        appointmentIds = new ArrayList<>();

        // Start periodic refresh only if we have a valid doctorId
        if (!doctorId.isEmpty()) {
            refreshHandler.post(refreshRunnable);
        }

        return view;
    }

    private void fetchAppointmentsData(final String doctorId) {
        Log.d(TAG, "fetchAppointmentsData() → doctorId=" + doctorId);
        String url = "http://sxm.a58.mytemp.website/Doctors/getOngoingAppointment.php";
        RequestQueue queue = Volley.newRequestQueue(requireContext());

        StringRequest request = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "API response: " + response);
                        try {
                            JSONObject root = new JSONObject(response);
                            boolean success = root.getBoolean("success");
                            if (!success) {
                                Log.w(TAG, "No confirmed appointments found");
                                Toast.makeText(getContext(), "No confirmed appointments.", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            JSONArray arr = root.getJSONArray("appointments");
                            Log.d(TAG, "Parsed JSON array length=" + arr.length());

                            patientNames.clear();
                            problems.clear();
                            distances.clear();
                            appointmentIds.clear();
                            hasReport.clear();

                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.getJSONObject(i);

                                patientNames.add(obj.getString("patient_name"));
                                problems.add(obj.getString("reason_for_visit"));
                                distances.add(obj.getString("time_slot"));
                                appointmentIds.add(obj.getString("appointment_id"));

                                // Extract the new has_report field (default = false if missing)
                                hasReport.add(obj.optBoolean("has_report", false));

                                Log.d(TAG, "Loaded appointment #" + obj.getString("appointment_id") +
                                        " → " + obj.getString("patient_name") +
                                        " | hasReport=" + hasReport.get(i));
                            }

                            // After the loop, pass hasReport into your adapter constructor:
                            adapter = new aOngoingAdapter(
                                    getContext(),
                                    appointmentIds,
                                    patientNames,
                                    problems,
                                    distances,
                                    hasReport
                            );
                            recyclerView.setAdapter(adapter);
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error", e);
                            Toast.makeText(getContext(), "Parse error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Volley error", error);
                        Toast.makeText(getContext(), "Network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Log.d(TAG, "POST params: doctor_id=" + doctorId);
                Map<String, String> params = new HashMap<>();
                params.put("doctor_id", doctorId);
                return params;
            }
        };

        queue.add(request);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView() — removing callbacks");
        refreshHandler.removeCallbacks(refreshRunnable);
    }
}
