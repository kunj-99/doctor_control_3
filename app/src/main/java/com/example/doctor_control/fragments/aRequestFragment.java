package com.example.doctor_control.fragments;

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
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.doctor_control.R;
import com.example.doctor_control.adapter.aRequestAdapeter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class aRequestFragment extends Fragment {

    private RecyclerView recyclerView;
    private aRequestAdapeter adapter;
    // Use a single ArrayList of Appointment objects
    private ArrayList<aRequestAdapeter.Appointment> appointments = new ArrayList<>();

    // Doctor ID loaded from SharedPreferences
    private String doctorId;

    // Handler for periodic refresh
    private Handler refreshHandler = new Handler();
    private Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            // Clear the list before refreshing data
            appointments.clear();
            fetchDataFromServer();
            // Schedule next refresh in 30 seconds (30000 milliseconds)
            refreshHandler.postDelayed(this, 30000);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_request, container, false);

        // Retrieve doctor_id from SharedPreferences (using getInt() because it's stored as an Integer)
        SharedPreferences prefs = getActivity().getSharedPreferences("MY_PREFS_NAME", Context.MODE_PRIVATE);
        doctorId = String.valueOf(prefs.getInt("doctor_id", 1)); // Default to 1 if not found

        recyclerView = view.findViewById(R.id.rv_pending_appointments);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize the adapter with context and appointments list
        adapter = new aRequestAdapeter(getContext(), appointments);
        recyclerView.setAdapter(adapter);

        // Fetch data from the server initially
        fetchDataFromServer();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Start periodic refresh only if the fragment is visible
        if (getUserVisibleHint()) {
            refreshHandler.postDelayed(refreshRunnable, 30000);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Stop periodic refresh when fragment is not in view
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    // This method is useful if the fragment is used within a ViewPager
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        // Start refresh if the fragment becomes visible and is resumed
        if (isVisibleToUser && isResumed()) {
            refreshHandler.postDelayed(refreshRunnable, 30000);
        } else {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }

    private void fetchDataFromServer() {
        // Replace this URL with your actual endpoint
        String url = "http://sxm.a58.mytemp.website/Doctors/getRequestappointment.php?doctor_id=" + doctorId;

        RequestQueue queue = Volley.newRequestQueue(getContext());
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            try {
                boolean success = response.optBoolean("success", false);
                if (success) {
                    JSONArray dataArray = response.optJSONArray("data");
                    if (dataArray != null) {
                        // Clear current appointments before updating
                        appointments.clear();
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject appointmentObj = dataArray.getJSONObject(i);
                            // Extract appointment details (adjust keys as per your JSON)
                            String appointmentId = appointmentObj.optString("appointment_id", "0");
                            String name = appointmentObj.optString("full_name", "N/A");
                            String problem = appointmentObj.optString("problem", "N/A");
                            String distance = appointmentObj.optString("distance", "N/A");

                            // Create a new Appointment object and add to the list
                            aRequestAdapeter.Appointment appointment =
                                    new aRequestAdapeter.Appointment(appointmentId, name, problem, distance);
                            appointments.add(appointment);
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "No appointments found.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String message = response.optString("message", "Failed to load data.");
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Error parsing data.", Toast.LENGTH_SHORT).show();
            }
        }, error -> {
            error.printStackTrace();
            Toast.makeText(getContext(), "Error fetching data from server.", Toast.LENGTH_SHORT).show();
        });

        queue.add(request);
    }
}
