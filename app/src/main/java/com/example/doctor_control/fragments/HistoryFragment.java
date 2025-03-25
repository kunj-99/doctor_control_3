package com.example.doctor_control.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.doctor_control.R;
import com.example.doctor_control.adapter.HistoryAdapter;
import com.example.doctor_control.HistoryItem;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private static final String TAG = "HistoryFragment";
    private RecyclerView rvHistory;
    private HistoryAdapter historyAdapter;
    private List<HistoryItem> historyItems;
    private RequestQueue requestQueue;

    // Updated URL with doctor_id parameter
    private static final String BASE_URL = "http://sxm.a58.mytemp.website/Doctors/gethistory.php?doctor_id=";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        // Initialize RecyclerView and list
        rvHistory = view.findViewById(R.id.rv_history);
        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        historyItems = new ArrayList<>();
        historyAdapter = new HistoryAdapter(historyItems);
        rvHistory.setAdapter(historyAdapter);

        // Initialize Volley request queue
        requestQueue = Volley.newRequestQueue(getContext());

        // Get doctor_id from SharedPreferences as an integer and convert it to String
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
        int doctorIdInt = sharedPreferences.getInt("doctor_id", 0);
        String doctorId = String.valueOf(doctorIdInt);
        Log.d(TAG, "Retrieved doctor_id: " + doctorId);

        // Fetch history data using the doctor_id
        fetchHistoryData(doctorId);

        return view;
    }

    private void fetchHistoryData(String doctorId) {
        // Construct the URL using the doctor_id from SharedPreferences
        String url = BASE_URL + doctorId;
        Log.d(TAG, "Fetching history data from URL: " + url);

        // Create a JsonArrayRequest
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Log.d(TAG, "Received response with length: " + response.length());
                        try {
                            // Parse the JSON response and add items to the list
                            // Inside your onResponse loop:
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject object = response.getJSONObject(i);

                                // Extract values from JSON
                                String patientName = object.getString("patient_name");
                                String appointmentDate = object.getString("appointment_date");
                                String symptoms = object.getString("reason_for_visit");
                                // Use optBoolean to handle missing "flag"
                                boolean flag = object.optBoolean("flag", false);
                                String patientId = object.getString("patient_id");

                                HistoryItem item = new HistoryItem(patientName, appointmentDate, symptoms, flag, patientId);
                                historyItems.add(item);
                                Log.d(TAG, "Added history item: " + patientName);
                            }

                            // Notify adapter about data changes
                            historyAdapter.notifyDataSetChanged();
                            Log.d(TAG, "History adapter notified. Total items: " + historyItems.size());
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing JSON response", e);
                        }
                    }
                },
                new Response.ErrorListener(){
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Volley error in fetching history data", error);
                    }
                }
        );

        // Add the request to the RequestQueue.
        requestQueue.add(jsonArrayRequest);
    }
}
